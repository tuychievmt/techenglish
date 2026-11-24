from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.anti_cheat import rank_title_for_xp, validate_ugc_text
from app.deps import get_current_user, get_db, now_ms, utc_day_str
from app.models import PeerReviewFeedbackRow, PeerReviewSubmissionRow, UserBlockRow, UserRow
from app.schemas.realtime import PeerReviewFeedbackRequest, PeerReviewSubmitRequest
from app.voice_storage import MAX_VOICE_SECONDS, PEER_REVIEW_VOICE_TTL_DAYS, VoiceStorageBackend, get_voice_storage

router = APIRouter(tags=['peer_review'])


@router.get("/v1/peer-review/daily-case")
def get_daily_peer_review_case():
    day = utc_day_str()
    return {
        "case_id": f"case_{day}",
        "date_iso": day,
        "title_en": "Incident Communication: Production Deployment Rollback",
        "prompt_en": "Explain to your engineering team in 3 clear English sentences why the production deployment failed (missing DB migration index caused lock contention) and what mitigation steps are underway.",
        "prompt_ru": "Объясните команде в 3 чётких предложениях на английском, почему упал продакшн-деплой (отсутствующий индекс миграции вызвал блокировки БД) и какие шаги по восстановлению выполняются.",
        "target_sentences_count": 3,
        "voice_max_seconds": MAX_VOICE_SECONDS,
        "voice_ttl_days": PEER_REVIEW_VOICE_TTL_DAYS,
    }


@router.get("/v1/peer-review/submissions")
def list_peer_review_submissions(
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: VoiceStorageBackend = Depends(get_voice_storage),
):
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    now = now_ms()
    rows = (
        db.query(PeerReviewSubmissionRow)
        .filter(PeerReviewSubmissionRow.is_hidden == False)  # noqa: E712
        .order_by(PeerReviewSubmissionRow.created_at_ms.desc())
        .limit(40)
        .all()
    )
    items = []
    for s in rows:
        if s.author_user_id in blocked_ids:
            continue
        # Lazy TTL enforcement for expired voice clips (Errata E5)
        has_voice = bool(s.voice_object_key)
        if has_voice and s.voice_expires_at_ms and now > s.voice_expires_at_ms:
            storage.delete_object(s.voice_object_key)
            s.voice_object_key = None
            has_voice = False
            db.commit()
        fbs = (
            db.query(PeerReviewFeedbackRow)
            .filter_by(submission_id=s.id)
            .order_by(PeerReviewFeedbackRow.created_at_ms.desc())
            .all()
        )
        items.append(
            {
                "id": s.id,
                "case_id": s.case_id,
                "author_user_id": s.author_user_id,
                "author_username": s.author_username,
                "author_rank_title": s.author_rank_title,
                "text_response": s.text_response,
                "has_voice_clip": has_voice,
                "voice_duration_seconds": s.voice_duration_seconds if has_voice else 0,
                "voice_expires_at_ms": s.voice_expires_at_ms if has_voice else 0,
                "voice_url": (
                    storage.generate_presigned_get_url(s.voice_object_key, expires_in_seconds=300)
                    if has_voice and s.voice_object_key
                    else None
                ),
                "created_at_ms": s.created_at_ms,
                "feedback": [
                    {
                        "id": f.id,
                        "reviewer_username": f.reviewer_username,
                        "reviewer_rank_title": f.reviewer_rank_title,
                        "rating_stars": f.rating_stars,
                        "comment_text": f.comment_text,
                        "created_at_ms": f.created_at_ms,
                    }
                    for f in fbs
                ],
            }
        )
    return {"items": items}


@router.get("/v1/peer-review/submissions/{sub_id}/voice-url")
def get_peer_review_voice_presigned_url(
    sub_id: str,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: VoiceStorageBackend = Depends(get_voice_storage),
):
    sub = db.get(PeerReviewSubmissionRow, sub_id)
    if not sub or sub.is_hidden or not sub.voice_object_key:
        raise HTTPException(status_code=404, detail="Voice clip not found")
    if sub.voice_expires_at_ms and now_ms() > sub.voice_expires_at_ms:
        storage.delete_object(sub.voice_object_key)
        sub.voice_object_key = None
        db.commit()
        raise HTTPException(status_code=410, detail="Voice clip expired")
    if not storage.exists(sub.voice_object_key):
        raise HTTPException(status_code=404, detail="Voice object not found in storage")
    presigned_url = storage.generate_presigned_get_url(sub.voice_object_key, expires_in_seconds=300)
    return {
        "submission_id": sub.id,
        "voice_url": presigned_url,
        "download_url": presigned_url,
        "expires_in_seconds": 300,
        "voice_expires_at_ms": sub.voice_expires_at_ms,
    }


@router.post("/v1/peer-review/submissions", status_code=status.HTTP_201_CREATED)
def create_peer_review_submission(
    body: PeerReviewSubmitRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: VoiceStorageBackend = Depends(get_voice_storage),
):
    clean_text = body.text_response.strip()
    has_voice = bool(body.voice_base64)
    if not clean_text and not has_voice:
        raise HTTPException(status_code=422, detail="Provide either text_response or a voice clip (<= 20s).")
    if clean_text:
        try:
            clean_text = validate_ugc_text(clean_text, min_len=15, max_len=1200)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc))

    ts = now_ms()
    voice_key = None
    voice_expires = 0
    if has_voice:
        if body.voice_duration_seconds < 1 or body.voice_duration_seconds > MAX_VOICE_SECONDS:
            raise HTTPException(status_code=422, detail="Voice clip duration must be 1..20 seconds (ФТ-14.2).")
        try:
            voice_key = storage.put_voice_clip(user.id, body.voice_base64 or "", body.voice_content_type)
        except Exception as exc:
            raise HTTPException(status_code=422, detail=f"Invalid voice clip: {exc}")
        voice_expires = ts + PEER_REVIEW_VOICE_TTL_DAYS * 24 * 60 * 60 * 1000

    sub = PeerReviewSubmissionRow(
        id=str(uuid.uuid4()),
        case_id=body.case_id,
        author_user_id=user.id,
        author_username=user.username,
        author_rank_title=rank_title_for_xp(user.total_xp),
        text_response=clean_text,
        voice_object_key=voice_key,
        voice_duration_seconds=body.voice_duration_seconds if voice_key else 0,
        voice_expires_at_ms=voice_expires,
        is_hidden=False,
        created_at_ms=ts,
    )
    db.add(sub)
    db.commit()
    return {
        "id": sub.id,
        "case_id": sub.case_id,
        "has_voice_clip": bool(voice_key),
        "voice_object_key": voice_key,
        "voice_expires_at_ms": voice_expires,
        "voice_ttl_days": PEER_REVIEW_VOICE_TTL_DAYS,
    }


@router.post("/v1/peer-review/submissions/{sub_id}/feedback", status_code=status.HTTP_201_CREATED)
def post_peer_review_feedback(
    sub_id: str,
    body: PeerReviewFeedbackRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    sub = db.get(PeerReviewSubmissionRow, sub_id)
    if not sub or sub.is_hidden:
        raise HTTPException(status_code=404, detail="Submission not found")
    try:
        clean_comment = validate_ugc_text(body.comment_text, min_len=5, max_len=600)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))

    fb = PeerReviewFeedbackRow(
        id=str(uuid.uuid4()),
        submission_id=sub.id,
        reviewer_user_id=user.id,
        reviewer_username=user.username,
        reviewer_rank_title=rank_title_for_xp(user.total_xp),
        rating_stars=body.rating_stars,
        comment_text=clean_comment,
        created_at_ms=now_ms(),
    )
    db.add(fb)
    db.commit()
    return {"id": fb.id, "rating_stars": fb.rating_stars, "comment_text": fb.comment_text}
