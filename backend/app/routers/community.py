from __future__ import annotations

import uuid
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.anti_cheat import rank_title_for_xp, sanitize_deck_tags, validate_ugc_text
from app.deps import get_current_user, get_db, now_ms
from app.models import (
    ActivityFeedRow,
    CommentRow,
    CommentVoteRow,
    CommunityDeckRatingRow,
    CommunityDeckRow,
    CommunityDeckTermRow,
    UserBlockRow,
    UserRow,
)
from app.schemas.social import (
    CreateCommentRequest,
    PublishDeckRequest,
    RateDeckRequest,
    VoteCommentRequest,
)

router = APIRouter(tags=['community'])


@router.get("/v1/comments/{target_type}/{target_id}")
def list_comments(
    target_type: str,
    target_id: int,
    sort: str = Query(default="top"),
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    norm_type = target_type.strip().upper()
    if norm_type not in {"TERM", "SNIPPET"}:
        raise HTTPException(status_code=422, detail="target_type must be TERM or SNIPPET")
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    rows = (
        db.query(CommentRow)
        .filter(
            CommentRow.target_type == norm_type,
            CommentRow.target_id == target_id,
            CommentRow.is_hidden == False,  # noqa: E712
        )
        .all()
    )
    visible = [r for r in rows if r.author_user_id not in blocked_ids]
    votes_map = {
        v.comment_id: v.vote
        for v in db.query(CommentVoteRow).filter(CommentVoteRow.user_id == user.id).all()
    }
    if sort == "newest":
        visible.sort(key=lambda c: -c.created_at_ms)
    else:
        visible.sort(key=lambda c: (-(c.upvotes - c.downvotes), -c.upvotes, -c.created_at_ms))

    return {
        "target_type": norm_type,
        "target_id": target_id,
        "items": [
            {
                "id": c.id,
                "target_type": c.target_type,
                "target_id": c.target_id,
                "author_user_id": c.author_user_id,
                "author_username": c.author_username,
                "author_rank_title": c.author_rank_title,
                "body": c.body,
                "upvotes": c.upvotes,
                "downvotes": c.downvotes,
                "score": c.upvotes - c.downvotes,
                "user_vote": votes_map.get(c.id, 0),
                "created_at_ms": c.created_at_ms,
            }
            for c in visible
        ],
    }


@router.post("/v1/comments", status_code=status.HTTP_201_CREATED)
def create_comment(
    body: CreateCommentRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    norm_type = body.target_type.strip().upper()
    if norm_type not in {"TERM", "SNIPPET"}:
        raise HTTPException(status_code=422, detail="target_type must be TERM or SNIPPET")
    try:
        clean_body = validate_ugc_text(body.body, min_len=5, max_len=1000)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))

    row = CommentRow(
        id=str(uuid.uuid4()),
        target_type=norm_type,
        target_id=body.target_id,
        author_user_id=user.id,
        author_username=user.username,
        author_rank_title=rank_title_for_xp(user.total_xp),
        body=clean_body,
        upvotes=0,
        downvotes=0,
        is_hidden=False,
        created_at_ms=now_ms(),
    )
    db.add(row)
    db.commit()
    return {
        "id": row.id,
        "target_type": row.target_type,
        "target_id": row.target_id,
        "author_user_id": row.author_user_id,
        "author_username": row.author_username,
        "author_rank_title": row.author_rank_title,
        "body": row.body,
        "upvotes": 0,
        "downvotes": 0,
        "score": 0,
        "user_vote": 0,
        "created_at_ms": row.created_at_ms,
    }


@router.post("/v1/comments/{comment_id}/vote")
def vote_comment(
    comment_id: str,
    body: VoteCommentRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.vote not in (-1, 0, 1):
        raise HTTPException(status_code=422, detail="vote must be -1, 0, or 1")
    comment = db.get(CommentRow, comment_id)
    if not comment or comment.is_hidden:
        raise HTTPException(status_code=404, detail="Comment not found")

    existing = db.query(CommentVoteRow).filter_by(comment_id=comment_id, user_id=user.id).first()
    if body.vote == 0:
        if existing:
            db.delete(existing)
    elif existing:
        existing.vote = body.vote
    else:
        db.add(CommentVoteRow(comment_id=comment_id, user_id=user.id, vote=body.vote))
    db.flush()

    up = db.query(func.count(CommentVoteRow.id)).filter_by(comment_id=comment_id, vote=1).scalar() or 0
    down = db.query(func.count(CommentVoteRow.id)).filter_by(comment_id=comment_id, vote=-1).scalar() or 0
    comment.upvotes = int(up)
    comment.downvotes = int(down)
    db.commit()
    return {
        "id": comment.id,
        "upvotes": comment.upvotes,
        "downvotes": comment.downvotes,
        "score": comment.upvotes - comment.downvotes,
        "user_vote": body.vote,
    }


@router.get("/v1/decks")
def list_community_decks(
    tag: Optional[str] = Query(default=None),
    query: Optional[str] = Query(default=None),
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    rows = (
        db.query(CommunityDeckRow)
        .filter(CommunityDeckRow.is_hidden == False)  # noqa: E712
        .order_by(CommunityDeckRow.rating.desc(), CommunityDeckRow.downloads_count.desc(), CommunityDeckRow.created_at_ms.desc())
        .all()
    )
    items = []
    for d in rows:
        if d.author_user_id in blocked_ids:
            continue
        tags_list = [t for t in d.tags_csv.split(",") if t]
        if tag and tag.strip().lower() not in tags_list:
            continue
        if query and query.strip().lower() not in (d.title + " " + d.description + " " + d.tags_csv).lower():
            continue
        items.append(
            {
                "id": d.id,
                "title": d.title,
                "description": d.description,
                "tags": tags_list,
                "author_user_id": d.author_user_id,
                "author_username": d.author_username,
                "rating": round(float(d.rating), 2),
                "downloads_count": d.downloads_count,
                "term_count": d.term_count,
                "created_at_ms": d.created_at_ms,
            }
        )
    return {"decks": items}


@router.get("/v1/decks/{deck_id}")
def get_community_deck_detail(
    deck_id: str,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    deck = db.get(CommunityDeckRow, deck_id)
    if not deck or deck.is_hidden:
        raise HTTPException(status_code=404, detail="Community deck not found")
    terms = db.query(CommunityDeckTermRow).filter_by(deck_id=deck_id).all()
    return {
        "id": deck.id,
        "title": deck.title,
        "description": deck.description,
        "tags": [t for t in deck.tags_csv.split(",") if t],
        "author_user_id": deck.author_user_id,
        "author_username": deck.author_username,
        "rating": round(float(deck.rating), 2),
        "downloads_count": deck.downloads_count,
        "term_count": deck.term_count,
        "created_at_ms": deck.created_at_ms,
        "terms": [
            {
                "term": t.term,
                "transcription": t.transcription,
                "translation": t.translation,
                "example": t.example,
                "explanation_ru": t.explanation_ru,
            }
            for t in terms
        ],
    }


@router.post("/v1/decks", status_code=status.HTTP_201_CREATED)
def publish_community_deck(
    body: PublishDeckRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    try:
        clean_title = validate_ugc_text(body.title, min_len=4, max_len=80)
        clean_desc = validate_ugc_text(body.description, min_len=0, max_len=400) if body.description.strip() else ""
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))

    valid_terms = [t for t in body.terms if t.term.strip() and t.translation.strip()]
    if not valid_terms or len(valid_terms) > 200:
        raise HTTPException(status_code=422, detail="Deck must contain between 1 and 200 valid terms.")
    clean_tags = sanitize_deck_tags(body.tags)

    deck_id = str(uuid.uuid4())
    ts = now_ms()
    deck = CommunityDeckRow(
        id=deck_id,
        title=clean_title,
        description=clean_desc,
        tags_csv=",".join(clean_tags),
        author_user_id=user.id,
        author_username=user.username,
        rating=5.0,
        downloads_count=0,
        term_count=len(valid_terms),
        is_hidden=False,
        created_at_ms=ts,
    )
    db.add(deck)
    for t in valid_terms:
        db.add(
            CommunityDeckTermRow(
                deck_id=deck_id,
                term=t.term.strip(),
                transcription=t.transcription.strip(),
                translation=t.translation.strip(),
                example=t.example.strip(),
                explanation_ru=t.explanation_ru.strip(),
            )
        )
    db.add(
        ActivityFeedRow(
            id=str(uuid.uuid4()),
            actor_user_id=user.id,
            summary_ru=f"Опубликовал(а) колоду «{clean_title}» ({len(valid_terms)} слов)",
            reaction_count=0,
            created_at_ms=ts,
        )
    )
    db.commit()
    return {
        "id": deck.id,
        "title": deck.title,
        "description": deck.description,
        "tags": clean_tags,
        "author_user_id": deck.author_user_id,
        "author_username": deck.author_username,
        "rating": deck.rating,
        "downloads_count": 0,
        "term_count": deck.term_count,
        "created_at_ms": ts,
    }


@router.post("/v1/decks/{deck_id}/download")
def download_community_deck(
    deck_id: str,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    deck = db.get(CommunityDeckRow, deck_id)
    if not deck or deck.is_hidden:
        raise HTTPException(status_code=404, detail="Community deck not found")
    deck.downloads_count += 1
    db.commit()
    terms = db.query(CommunityDeckTermRow).filter_by(deck_id=deck_id).all()
    return {
        "deck_id": deck.id,
        "title": deck.title,
        "downloads_count": deck.downloads_count,
        "terms": [
            {
                "term": t.term,
                "transcription": t.transcription,
                "translation": t.translation,
                "example": t.example,
                "explanation_ru": t.explanation_ru,
            }
            for t in terms
        ],
    }


@router.post("/v1/decks/{deck_id}/rate")
def rate_community_deck(
    deck_id: str,
    body: RateDeckRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    deck = db.get(CommunityDeckRow, deck_id)
    if not deck or deck.is_hidden:
        raise HTTPException(status_code=404, detail="Community deck not found")
    existing = db.query(CommunityDeckRatingRow).filter_by(deck_id=deck_id, user_id=user.id).first()
    if existing:
        existing.stars = body.stars
    else:
        db.add(CommunityDeckRatingRow(deck_id=deck_id, user_id=user.id, stars=body.stars))
    db.flush()
    avg = db.query(func.avg(CommunityDeckRatingRow.stars)).filter_by(deck_id=deck_id).scalar() or 5.0
    deck.rating = round(float(avg), 2)
    db.commit()
    return {"deck_id": deck.id, "rating": deck.rating}
