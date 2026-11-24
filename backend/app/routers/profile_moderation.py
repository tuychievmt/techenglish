from __future__ import annotations

import uuid
from typing import Any, Dict

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.anti_cheat import (
    AUTO_HIDE_REPORT_THRESHOLD,
    is_avatar_allowed,
    rank_title_for_xp,
    validate_username,
)
from app.deps import get_current_user, get_db, now_ms, require_moderator
from app.models import (
    CommentRow,
    CommunityDeckRow,
    ModerationReportRow,
    PeerReviewSubmissionRow,
    RoomMessageRow,
    Sm2StateRow,
    UserBlockRow,
    UserRow,
)
from app.schemas.social import ProfileUpdateRequest, ReportRequest

router = APIRouter(tags=['profile_moderation'])


def _serialize_profile(user: UserRow, db: Session) -> Dict[str, Any]:
    learned_count = db.query(Sm2StateRow).filter(Sm2StateRow.user_id == user.id, Sm2StateRow.repetitions >= 1).count()
    badges = [
        {"id": "first_sync", "title_ru": "Облачный синхронизатор", "unlocked": user.total_xp >= 10},
        {"id": "streak_7", "title_ru": "7 дней без пропусков", "unlocked": user.streak_days >= 7},
        {"id": "middle_rank", "title_ru": "Ранг Middle Developer", "unlocked": user.total_xp >= 600},
        {"id": "vocab_100", "title_ru": "100 изученных терминов", "unlocked": learned_count >= 100},
        {"id": "contributor", "title_ru": "Contributor (PR в словарь)", "unlocked": bool(user.is_contributor)},
    ]
    return {
        "user_id": user.id,
        "username": user.username,
        "display_name": user.display_name,
        "avatar_key": user.avatar_key,
        "status_text": user.status_text,
        "tech_stack": [t.strip() for t in user.tech_stack_csv.split(",") if t.strip()],
        "github_url": user.github_url,
        "telegram_handle": user.telegram_handle,
        "rank_title": rank_title_for_xp(user.total_xp),
        "total_xp": user.total_xp,
        "streak_days": user.streak_days,
        "league": user.league,
        "accepts_pings": user.accepts_pings,
        "badges": badges,
    }


@router.get("/v1/profile/me")
def get_my_profile(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    return _serialize_profile(user, db)


@router.patch("/v1/profile/me")
def update_my_profile(
    body: ProfileUpdateRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.username is not None:
        try:
            clean = validate_username(body.username)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc))
        clash = db.query(UserRow).filter(UserRow.username == clean, UserRow.id != user.id).first()
        if clash:
            raise HTTPException(status_code=409, detail="Username already taken")
        user.username = clean

    if body.avatar_key is not None:
        if not is_avatar_allowed(body.avatar_key):
            raise HTTPException(
                status_code=422,
                detail="Only preset avatars or verified GitHub/Google OAuth avatar URLs are allowed (Errata E2)",
            )
        user.avatar_key = body.avatar_key.strip()

    if body.display_name is not None:
        user.display_name = body.display_name.strip()[:120]
    if body.status_text is not None:
        user.status_text = body.status_text.strip()[:200]
    if body.tech_stack is not None:
        user.tech_stack_csv = ",".join(s.strip() for s in body.tech_stack[:8] if s.strip())
    if body.github_url is not None:
        user.github_url = body.github_url.strip()[:255]
    if body.telegram_handle is not None:
        user.telegram_handle = body.telegram_handle.strip()[:120]
    if body.accepts_pings is not None:
        user.accepts_pings = body.accepts_pings

    db.commit()
    return _serialize_profile(user, db)


@router.post("/v1/moderation/report", status_code=status.HTTP_201_CREATED)
def create_moderation_report(
    body: ReportRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    target_type = body.target_type.strip().upper()
    if target_type not in {"PROFILE", "COMMENT", "DECK", "ROOM_MESSAGE", "PEER_REVIEW", "DICTIONARY_PR"}:
        raise HTTPException(status_code=422, detail="Unsupported moderation target_type")
    report = ModerationReportRow(
        id=str(uuid.uuid4()),
        reporter_id=user.id,
        target_type=target_type,
        target_id=body.target_id,
        reason=body.reason.strip(),
        status="OPEN",
        created_at_ms=now_ms(),
    )
    db.add(report)
    db.flush()

    distinct_reporters = (
        db.query(func.count(func.distinct(ModerationReportRow.reporter_id)))
        .filter(
            ModerationReportRow.target_type == target_type,
            ModerationReportRow.target_id == body.target_id,
        )
        .scalar()
        or 0
    )
    auto_hidden = False
    if int(distinct_reporters) >= AUTO_HIDE_REPORT_THRESHOLD:
        if target_type == "COMMENT":
            comment = db.get(CommentRow, body.target_id)
            if comment:
                comment.is_hidden = True
                auto_hidden = True
        elif target_type == "DECK":
            deck = db.get(CommunityDeckRow, body.target_id)
            if deck:
                deck.is_hidden = True
                auto_hidden = True
        elif target_type == "ROOM_MESSAGE":
            msg = db.get(RoomMessageRow, body.target_id)
            if msg:
                msg.is_hidden = True
                auto_hidden = True
        elif target_type == "PEER_REVIEW":
            sub = db.get(PeerReviewSubmissionRow, body.target_id)
            if sub:
                sub.is_hidden = True
                auto_hidden = True

    db.commit()
    return {
        "report_id": report.id,
        "status": report.status,
        "target_type": target_type,
        "auto_hidden": auto_hidden,
    }


@router.get("/v1/moderation/queue")
def list_moderation_queue(user: UserRow = Depends(require_moderator), db: Session = Depends(get_db)):
    rows = (
        db.query(ModerationReportRow)
        .filter(ModerationReportRow.status == "OPEN")
        .order_by(ModerationReportRow.created_at_ms.desc())
        .limit(50)
        .all()
    )
    return {
        "items": [
            {
                "report_id": r.id,
                "target_type": r.target_type,
                "target_id": r.target_id,
                "reason": r.reason,
                "status": r.status,
                "created_at_ms": r.created_at_ms,
            }
            for r in rows
        ]
    }


@router.post("/v1/moderation/block/{target_user_id}")
def block_user(target_user_id: str, user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    if target_user_id == user.id:
        raise HTTPException(status_code=400, detail="Cannot block yourself")
    if not db.query(UserBlockRow).filter_by(blocker_id=user.id, blocked_id=target_user_id).first():
        db.add(UserBlockRow(blocker_id=user.id, blocked_id=target_user_id, created_at_ms=now_ms()))
        db.commit()
    return {"blocked": True, "target_user_id": target_user_id}


@router.get("/v1/moderation/blocks")
def list_blocked_users(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = db.query(UserBlockRow).filter_by(blocker_id=user.id).all()
    return {"blocked_user_ids": [r.blocked_id for r in rows]}
