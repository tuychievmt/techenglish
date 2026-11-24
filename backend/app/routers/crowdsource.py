from __future__ import annotations

import uuid
from typing import Any, Dict

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.anti_cheat import validate_ugc_text
from app.deps import get_current_user, get_db, now_ms, require_moderator
from app.models import DictionaryPrRow, DictionaryPrVoteRow, Sm2StateRow, UserBlockRow, UserRow
from app.schemas.social import SubmitDictionaryPrRequest, VoteCommentRequest

router = APIRouter(tags=['crowdsource'])


def _evaluate_crowdsource_eligibility(user: UserRow, db: Session) -> Dict[str, Any]:
    age_days = max(0, (now_ms() - int(user.created_at_ms)) // (24 * 60 * 60 * 1000))
    learned_count = int(
        db.query(func.count(Sm2StateRow.id))
        .filter(Sm2StateRow.user_id == user.id, Sm2StateRow.repetitions >= 1)
        .scalar()
        or 0
    )
    meets_rank = int(user.total_xp) >= 600
    meets_age = age_days >= 14
    meets_terms = learned_count >= 100
    meets_clean = int(user.confirmed_reports) == 0
    unmet = []
    if not meets_rank:
        unmet.append("Требуется ранг Middle Developer и выше (≥ 600 XP).")
    if not meets_age:
        unmet.append("Возраст аккаунта должен быть ≥ 14 дней.")
    if not meets_terms:
        unmet.append("Необходимо изучить ≥ 100 терминов.")
    if not meets_clean:
        unmet.append("Не должно быть подтверждённых жалоб модерации.")
    return {
        "is_eligible": len(unmet) == 0,
        "meets_rank_middle_plus": meets_rank,
        "meets_account_age_14_days": meets_age,
        "meets_learned_terms_100": meets_terms,
        "meets_zero_confirmed_reports": meets_clean,
        "total_xp": int(user.total_xp),
        "account_age_days": int(age_days),
        "learned_terms_count": learned_count,
        "confirmed_reports": int(user.confirmed_reports),
        "unmet_reasons_ru": unmet,
        "project_license": "CC BY-SA 4.0",
    }


@router.get("/v1/crowdsource/eligibility")
def get_crowdsource_eligibility(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    return _evaluate_crowdsource_eligibility(user, db)


@router.get("/v1/crowdsource/prs")
def list_dictionary_prs(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    rows = db.query(DictionaryPrRow).order_by(DictionaryPrRow.created_at_ms.desc()).limit(50).all()
    return {
        "items": [
            {
                "id": p.id,
                "term": p.term,
                "proposed_translation_ru": p.proposed_translation_ru,
                "proposed_translation_tg": p.proposed_translation_tg,
                "proposed_definition_ru": p.proposed_definition_ru,
                "source_reference": p.source_reference,
                "author_username": p.author_username,
                "status": p.status,
                "upvotes": p.upvotes,
                "downvotes": p.downvotes,
                "license_accepted": p.license_accepted,
                "created_at_ms": p.created_at_ms,
            }
            for p in rows
            if p.author_user_id not in blocked_ids
        ]
    }


@router.post("/v1/crowdsource/prs", status_code=status.HTTP_201_CREATED)
def submit_dictionary_pr(
    body: SubmitDictionaryPrRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    elig = _evaluate_crowdsource_eligibility(user, db)
    if not elig["is_eligible"]:
        raise HTTPException(
            status_code=403,
            detail="Crowdsourcing gate unmet (ФТ-14.3): " + "; ".join(elig["unmet_reasons_ru"]),
        )
    if not body.license_accepted:
        raise HTTPException(
            status_code=422,
            detail="Project open content license (CC BY-SA 4.0) must be accepted before submitting a dictionary PR.",
        )
    if not body.proposed_translation_ru.strip() and not body.proposed_translation_tg.strip():
        raise HTTPException(status_code=422, detail="Provide at least a Russian or Tajik translation.")
    src = body.source_reference.strip()
    if len(src) < 4:
        raise HTTPException(status_code=422, detail="Authoritative source reference is required.")

    pr = DictionaryPrRow(
        id=str(uuid.uuid4()),
        author_user_id=user.id,
        author_username=user.username,
        term=body.term.strip(),
        proposed_translation_ru=body.proposed_translation_ru.strip(),
        proposed_translation_tg=body.proposed_translation_tg.strip(),
        proposed_definition_ru=body.proposed_definition_ru.strip(),
        source_reference=src,
        license_accepted=True,
        status="OPEN",
        upvotes=0,
        downvotes=0,
        created_at_ms=now_ms(),
    )
    db.add(pr)
    db.commit()
    return {
        "id": pr.id,
        "term": pr.term,
        "status": pr.status,
        "license_notice": "Published under project license CC BY-SA 4.0 upon moderation approval.",
    }


@router.post("/v1/crowdsource/prs/{pr_id}/vote")
def vote_dictionary_pr(
    pr_id: str,
    body: VoteCommentRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.vote not in (-1, 0, 1):
        raise HTTPException(status_code=422, detail="vote must be -1, 0, or 1")
    pr = db.get(DictionaryPrRow, pr_id)
    if not pr:
        raise HTTPException(status_code=404, detail="Dictionary PR not found")
    existing = db.query(DictionaryPrVoteRow).filter_by(pr_id=pr_id, user_id=user.id).first()
    if body.vote == 0:
        if existing:
            db.delete(existing)
    elif existing:
        existing.vote = body.vote
    else:
        db.add(DictionaryPrVoteRow(pr_id=pr_id, user_id=user.id, vote=body.vote))
    db.flush()
    pr.upvotes = int(db.query(func.count(DictionaryPrVoteRow.id)).filter_by(pr_id=pr_id, vote=1).scalar() or 0)
    pr.downvotes = int(db.query(func.count(DictionaryPrVoteRow.id)).filter_by(pr_id=pr_id, vote=-1).scalar() or 0)
    db.commit()
    return {"id": pr.id, "upvotes": pr.upvotes, "downvotes": pr.downvotes}


@router.post("/v1/crowdsource/prs/{pr_id}/approve")
def approve_dictionary_pr(
    pr_id: str,
    user: UserRow = Depends(require_moderator),
    db: Session = Depends(get_db),
):
    pr = db.get(DictionaryPrRow, pr_id)
    if not pr:
        raise HTTPException(status_code=404, detail="Dictionary PR not found")
    if pr.author_user_id == user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Self-approval of your own Dictionary PR is forbidden",
        )
    pr.status = "APPROVED"
    author = db.get(UserRow, pr.author_user_id)
    if author:
        author.is_contributor = True
        author.squad_bonus_xp = int(author.squad_bonus_xp) + 50  # Bonus Contributor XP (non-ranked)
    db.commit()
    return {"id": pr.id, "status": pr.status, "contributor_badge_unlocked": True, "bonus_xp": 50}
