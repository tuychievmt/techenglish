from __future__ import annotations

import uuid
from typing import Any, Dict, Optional

from fastapi import HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.anti_cheat import (
    evaluate_ranked_event,
    rank_title_for_xp,
    should_incoming_sm2_win,
    week_start_utc5_ms,
)
from app.config import settings
from app.deps import now_ms
from app.integrity import ClientIntegrityVerifier, get_integrity_verifier
from app.models import (
    ActivityFeedRow,
    Sm2StateRow,
    SyncEventRow,
    UserCustomWordRow,
    UserFavoriteRow,
    UserRow,
)
from app.schemas.sync import SyncBatchRequest
from app.security import create_jwt


OTA_PACKS: Dict[str, Dict[str, Any]] = {
    "ota_docker_linux_week_v3": {
        "pack_id": "ota_docker_linux_week_v3",
        "version": 3,
        "title_ru": "Неделя Docker и Linux (OTA-пакет)",
        "domain": "DevOps & Infrastructure",
        "terms": [
            {
                "term": "cgroups v2 Unified Hierarchy",
                "normalized_term": "cgroups v2 unified hierarchy",
                "phonetic_transcription": "/ˈsiː.ɡruːps ˈjuː.nɪ.faɪd ˈhaɪə.rɑːr.ki/",
                "ipa_source_url": "https://github.com/open-dict-data/ipa-dict",
                "ipa_license": "MIT / CC-BY-SA-3.0",
                "part_of_speech": "noun phrase",
                "translation_ru": "единая иерархия контрольных групп cgroups v2",
                "definition_en": "Linux kernel resource isolation hierarchy enforcing unified CPU, memory, and I/O limits across container processes.",
                "definition_ru": "Единая иерархия контрольных групп ядра Linux для изоляции CPU, памяти и ввода-вывода контейнеров.",
                "major_category": "Software Engineering & Compiler Technology",
                "category_id": 108,
                "subcategory_id": 1,
                "domain": "DevOps & Infrastructure",
                "subdomain": "DevOps",
                "difficulty_level": "Advanced",
                "cefr_level": "C1",
                "source_reference": "Linux Kernel Documentation — Control Group v2",
                "verification_status": "verified",
            }
        ],
    }
}


def _issue_tokens(user: UserRow) -> Dict[str, Any]:
    return {
        "access_token": create_jwt(user.id, "access", settings.access_token_ttl_seconds),
        "refresh_token": create_jwt(user.id, "refresh", settings.refresh_token_ttl_seconds),
        "token_type": "bearer",
        "user_id": user.id,
        "username": user.username,
        "provider": user.provider,
        "is_email_verified": user.is_email_verified,
    }


def _apply_sync_batch(
    db: Session,
    user: UserRow,
    body: SyncBatchRequest,
    integrity_verifier: Optional[ClientIntegrityVerifier] = None,
) -> Dict[str, Any]:
    verifier = integrity_verifier or get_integrity_verifier()
    verdict = verifier.verify(body.play_integrity_token, expected_nonce=body.play_integrity_nonce)
    if not verdict.is_valid:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Play Integrity verification failed ({verdict.reason})",
        )

    srv_now = now_ms()
    wk_start = week_start_utc5_ms(srv_now)
    one_min_ago = srv_now - 60_000
    day_start = srv_now - (24 * 60 * 60 * 1000)

    xp_last_min = (
        db.query(func.coalesce(func.sum(SyncEventRow.awarded_xp), 0))
        .filter(SyncEventRow.user_id == user.id, SyncEventRow.server_received_ms >= one_min_ago)
        .scalar()
        or 0
    )
    xp_today = (
        db.query(func.coalesce(func.sum(SyncEventRow.awarded_xp), 0))
        .filter(SyncEventRow.user_id == user.id, SyncEventRow.server_received_ms >= day_start)
        .scalar()
        or 0
    )

    accepted_ids: List[str] = []
    duplicate_ids: List[str] = []
    rejected_events: List[Dict[str, str]] = []
    tampering_flags = 0
    awarded_batch_xp = 0

    for ev in body.events:
        if db.get(SyncEventRow, ev.event_id) is not None:
            duplicate_ids.append(ev.event_id)
            continue

        verdict = evaluate_ranked_event(
            event_type=ev.event_type,
            client_reported_xp=ev.client_reported_xp,
            client_timestamp_ms=ev.client_timestamp_ms,
            server_received_ms=srv_now,
            session_duration_ms=ev.session_duration_ms,
            is_correct=ev.is_correct,
            xp_last_minute=xp_last_min,
            xp_today=xp_today,
        )
        if verdict.tampering_detected:
            tampering_flags += 1
        if not verdict.accepted:
            rejected_events.append({"event_id": ev.event_id, "reason": verdict.reject_reason or "REJECTED"})
            continue

        db.add(
            SyncEventRow(
                event_id=ev.event_id,
                user_id=user.id,
                event_type=ev.event_type,
                payload_json=ev.payload_json,
                awarded_xp=verdict.awarded_xp,
                client_timestamp_ms=ev.client_timestamp_ms,
                server_received_ms=srv_now,
                week_start_utc5_ms=wk_start,
            )
        )
        xp_last_min += verdict.awarded_xp
        xp_today += verdict.awarded_xp
        awarded_batch_xp += verdict.awarded_xp
        accepted_ids.append(ev.event_id)

    # SM-2 conflict resolution per Errata E1
    sm2_updated = 0
    sm2_conflicts_kept_existing = 0
    for item in body.sm2_updates:
        norm = item.normalized_term.strip().lower()
        if not norm:
            continue
        existing = (
            db.query(Sm2StateRow)
            .filter(Sm2StateRow.user_id == user.id, Sm2StateRow.normalized_term == norm)
            .first()
        )
        if existing is None:
            from app.anti_cheat import clamp_client_timestamp
            sanitized_ts, _ = clamp_client_timestamp(item.client_timestamp_ms, srv_now)
            db.add(
                Sm2StateRow(
                    user_id=user.id,
                    normalized_term=norm,
                    repetitions=item.repetitions,
                    ease_factor=max(1.3, item.ease_factor),
                    interval_days=max(0, item.interval_days),
                    next_review_at_ms=item.next_review_at_ms,
                    client_timestamp_ms=sanitized_ts,
                    server_received_ms=srv_now,
                )
            )
            sm2_updated += 1
        else:
            inc_wins, sanitized_ts, _ = should_incoming_sm2_win(
                existing_client_ts=existing.client_timestamp_ms,
                existing_reps=existing.repetitions,
                existing_server_ts=existing.server_received_ms,
                incoming_client_ts=item.client_timestamp_ms,
                incoming_reps=item.repetitions,
                incoming_server_ts=srv_now,
            )
            if inc_wins:
                existing.repetitions = item.repetitions
                existing.ease_factor = max(1.3, item.ease_factor)
                existing.interval_days = max(0, item.interval_days)
                existing.next_review_at_ms = item.next_review_at_ms
                existing.client_timestamp_ms = sanitized_ts
                existing.server_received_ms = srv_now
                sm2_updated += 1
            else:
                sm2_conflicts_kept_existing += 1

    # Favorites & custom words merge by set union
    for fav in body.favorite_terms:
        norm = fav.strip().lower()
        if norm and not db.query(UserFavoriteRow).filter_by(user_id=user.id, normalized_term=norm).first():
            db.add(UserFavoriteRow(user_id=user.id, normalized_term=norm))

    for term, transl in body.custom_words.items():
        norm = term.strip().lower()
        if norm and not db.query(UserCustomWordRow).filter_by(user_id=user.id, normalized_term=norm).first():
            db.add(UserCustomWordRow(user_id=user.id, normalized_term=norm, translation=transl.strip()))

    # Recompute total XP strictly from accepted server sync_events
    db.flush()
    recalculated_total_xp = (
        db.query(func.coalesce(func.sum(SyncEventRow.awarded_xp), 0))
        .filter(SyncEventRow.user_id == user.id)
        .scalar()
        or 0
    )
    user.total_xp = int(recalculated_total_xp)
    if awarded_batch_xp > 0:
        user.streak_days = max(user.streak_days, 1)
        db.add(
            ActivityFeedRow(
                id=str(uuid.uuid4()),
                actor_user_id=user.id,
                summary_ru=f"Заработал +{awarded_batch_xp} XP (ранг: {rank_title_for_xp(user.total_xp)})",
                reaction_count=0,
                created_at_ms=srv_now,
            )
        )
    db.commit()

    return {
        "accepted_event_ids": accepted_ids,
        "duplicate_event_ids": duplicate_ids,
        "rejected_events": rejected_events,
        "awarded_xp": awarded_batch_xp,
        "server_total_xp": user.total_xp,
        "tampering_flags": tampering_flags,
        "sm2_updated": sm2_updated,
        "sm2_conflicts_kept_existing": sm2_conflicts_kept_existing,
    }
