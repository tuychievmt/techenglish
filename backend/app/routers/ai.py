from __future__ import annotations

import uuid
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.ai_proxy import (
    MAX_AI_REQUESTS_PER_DAY,
    GeminiAiClient,
    evaluate_interview_answer_async,
    get_daily_ai_limit,
    get_gemini_client,
    review_pr_comment_async,
)
from app.deps import get_current_user, get_db, now_ms, utc_day_str
from app.models import AiUsageLogRow, UserRow
from app.schemas.realtime import AiCodeReviewRequest, AiInterviewRequest

router = APIRouter(tags=['ai'])


def _ai_used_today(db: Session, user_id: str) -> int:
    day = utc_day_str()
    return int(
        db.query(func.count(AiUsageLogRow.id))
        .filter(AiUsageLogRow.user_id == user_id, AiUsageLogRow.utc_day == day)
        .scalar()
        or 0
    )


@router.get("/v1/ai/quota")
def get_ai_quota(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    limit = get_daily_ai_limit()
    used = _ai_used_today(db, user.id)
    return {
        "daily_limit": limit,
        "used_today": used,
        "remaining_today": max(0, limit - used),
        "audio_stored": False,
        "profile_pii_forwarded": False,
    }


@router.post("/v1/ai/interview")
async def ai_mock_interview(
    body: AiInterviewRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    gemini_client: Optional[GeminiAiClient] = Depends(get_gemini_client),
):
    if not body.consent_accepted:
        raise HTTPException(
            status_code=400,
            detail="Explicit user consent is required before sending text to the AI proxy (ФТ-8.3).",
        )
    if len(body.user_answer_text.strip()) < 5:
        raise HTTPException(status_code=422, detail="Answer must be at least 5 characters.")

    limit = get_daily_ai_limit()
    used = _ai_used_today(db, user.id)
    if used >= limit:
        raise HTTPException(status_code=429, detail="Daily AI request quota exceeded (ФТ-8.3).")

    result = await evaluate_interview_answer_async(
        role=body.role.strip().lower(),
        level=body.level.strip().lower(),
        question_en=body.question_en,
        user_answer_raw=body.user_answer_text,
        turn_index=body.turn_index,
        gemini_client=gemini_client,
    )
    db.add(
        AiUsageLogRow(
            id=str(uuid.uuid4()),
            user_id=user.id,
            feature="INTERVIEW",
            utc_day=utc_day_str(),
            created_at_ms=now_ms(),
        )
    )
    db.commit()
    result["remaining_daily_quota"] = max(0, limit - (used + 1))
    result["audio_stored"] = False
    return result


@router.post("/v1/ai/code-review")
async def ai_code_review(
    body: AiCodeReviewRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    gemini_client: Optional[GeminiAiClient] = Depends(get_gemini_client),
):
    if not body.consent_accepted:
        raise HTTPException(
            status_code=400,
            detail="Explicit user consent is required before sending text to the AI proxy (ФТ-8.3).",
        )
    if len(body.draft_text.strip()) < 5:
        raise HTTPException(status_code=422, detail="Draft text must be at least 5 characters.")

    limit = get_daily_ai_limit()
    used = _ai_used_today(db, user.id)
    if used >= limit:
        raise HTTPException(status_code=429, detail="Daily AI request quota exceeded (ФТ-8.3).")

    result = await review_pr_comment_async(
        raw_text=body.draft_text,
        context=body.context,
        gemini_client=gemini_client,
    )
    db.add(
        AiUsageLogRow(
            id=str(uuid.uuid4()),
            user_id=user.id,
            feature="CODE_REVIEW",
            utc_day=utc_day_str(),
            created_at_ms=now_ms(),
        )
    )
    db.commit()
    result["remaining_daily_quota"] = max(0, limit - (used + 1))
    return result
