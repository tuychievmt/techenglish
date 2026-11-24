from __future__ import annotations

import hashlib
import json

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.anti_cheat import rank_title_for_xp
from app.deps import get_current_user, get_db
from app.integrity import ClientIntegrityVerifier, get_integrity_verifier
from app.models import Sm2StateRow, UserCustomWordRow, UserFavoriteRow, UserRow
from app.schemas.sync import SyncBatchRequest
from app.services.sync_service import OTA_PACKS, _apply_sync_batch

router = APIRouter(tags=['sync'])


@router.post("/v1/sync/events")
def sync_events(
    body: SyncBatchRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    integrity_verifier: ClientIntegrityVerifier = Depends(get_integrity_verifier),
):
    return _apply_sync_batch(db, user, body, integrity_verifier=integrity_verifier)


@router.get("/v1/sync/state")
def get_sync_state(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    sm2 = db.query(Sm2StateRow).filter(Sm2StateRow.user_id == user.id).all()
    favs = [r.normalized_term for r in db.query(UserFavoriteRow).filter(UserFavoriteRow.user_id == user.id).all()]
    custom = {
        r.normalized_term: r.translation
        for r in db.query(UserCustomWordRow).filter(UserCustomWordRow.user_id == user.id).all()
    }
    return {
        "user_id": user.id,
        "server_total_xp": user.total_xp,
        "rank_title": rank_title_for_xp(user.total_xp),
        "league": user.league,
        "sm2_states": [
            {
                "normalized_term": s.normalized_term,
                "repetitions": s.repetitions,
                "ease_factor": s.ease_factor,
                "interval_days": s.interval_days,
                "next_review_at_ms": s.next_review_at_ms,
                "client_timestamp_ms": s.client_timestamp_ms,
                "server_received_ms": s.server_received_ms,
            }
            for s in sm2
        ],
        "favorite_terms": favs,
        "custom_words": custom,
    }


@router.get("/v1/ota/manifest")
def get_ota_manifest():
    packs = []
    for pack_id, payload in OTA_PACKS.items():
        raw_bytes = json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
        sha = hashlib.sha256(raw_bytes).hexdigest()
        item_count = (
            len(payload.get("terms", []))
            + len(payload.get("doc_articles", []))
            + len(payload.get("scenarios", []))
            + len(payload.get("work_phrases", []))
            + len(payload.get("code_snippets", []))
        )
        packs.append(
            {
                "pack_id": pack_id,
                "version": payload["version"],
                "title_ru": payload["title_ru"],
                "domain": payload["domain"],
                "item_count": item_count,
                "sha256_hex": sha,
                "data_only": True,
                "download_url": payload.get("download_url"),
            }
        )
    return {"packs": packs}


@router.get("/v1/ota/packs/{pack_id}")
def get_ota_pack(pack_id: str):
    payload = OTA_PACKS.get(pack_id)
    if not payload:
        raise HTTPException(status_code=404, detail="OTA pack not found")
    raw_json = json.dumps(payload, ensure_ascii=False, sort_keys=True)
    sha = hashlib.sha256(raw_json.encode("utf-8")).hexdigest()
    return {"pack_id": pack_id, "sha256_hex": sha, "payload_json": raw_json}
