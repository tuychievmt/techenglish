from __future__ import annotations

import secrets
import uuid
from typing import Any, Dict

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.anti_cheat import rank_title_for_xp, validate_ugc_text, week_start_utc5_ms
from app.deps import get_current_user, get_db, now_ms
from app.models import SquadMemberRow, SquadRow, SyncEventRow, UserRow
from app.schemas.social import CreateSquadRequest, JoinSquadRequest

router = APIRouter(tags=['squads'])


def _serialize_squad(squad: SquadRow, db: Session) -> Dict[str, Any]:
    wk_start = week_start_utc5_ms(now_ms())
    members_rows = db.query(SquadMemberRow).filter_by(squad_id=squad.id).all()
    member_ids = [m.user_id for m in members_rows]
    weekly_xp_rows = (
        db.query(SyncEventRow.user_id, func.coalesce(func.sum(SyncEventRow.awarded_xp), 0))
        .filter(SyncEventRow.user_id.in_(member_ids), SyncEventRow.week_start_utc5_ms == wk_start)
        .group_by(SyncEventRow.user_id)
        .all()
        if member_ids
        else []
    )
    weekly_map = {uid: int(xp) for uid, xp in weekly_xp_rows}
    members = []
    collective_xp = 0
    for uid in member_ids:
        u = db.get(UserRow, uid)
        if not u:
            continue
        wxp = weekly_map.get(uid, 0)
        collective_xp += wxp
        members.append(
            {
                "user_id": u.id,
                "username": u.username,
                "rank_title": rank_title_for_xp(u.total_xp),
                "weekly_xp": wxp,
            }
        )
    members.sort(key=lambda m: (-m["weekly_xp"], m["username"]))
    is_active = 5 <= len(members) <= 10
    goal_reached = is_active and collective_xp >= squad.weekly_goal_xp
    return {
        "id": squad.id,
        "name": squad.name,
        "invite_code": squad.invite_code,
        "weekly_goal_xp": squad.weekly_goal_xp,
        "current_week_xp": collective_xp,
        "member_count": len(members),
        "min_members": 5,
        "max_members": 10,
        "is_active_for_challenge": is_active,
        "goal_reached": goal_reached,
        "next_week_bonus_multiplier": 1.25 if goal_reached else 1.0,
        "affects_ranked_leaderboard_xp": False,
        "members": members,
    }


@router.post("/v1/squads", status_code=status.HTTP_201_CREATED)
def create_squad(
    body: CreateSquadRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if db.query(SquadMemberRow).filter_by(user_id=user.id).first():
        raise HTTPException(status_code=409, detail="Leave your current squad before creating a new one.")
    try:
        clean_name = validate_ugc_text(body.name, min_len=3, max_len=60)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))

    sq_id = str(uuid.uuid4())
    code = f"SQ-{secrets.token_hex(3).upper()}"
    ts = now_ms()
    squad = SquadRow(
        id=sq_id,
        name=clean_name,
        invite_code=code,
        creator_user_id=user.id,
        weekly_goal_xp=body.weekly_goal_xp,
        created_at_ms=ts,
    )
    db.add(squad)
    db.add(SquadMemberRow(squad_id=sq_id, user_id=user.id, joined_at_ms=ts))
    db.commit()
    return _serialize_squad(squad, db)


@router.post("/v1/squads/join")
def join_squad(
    body: JoinSquadRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if db.query(SquadMemberRow).filter_by(user_id=user.id).first():
        raise HTTPException(status_code=409, detail="You are already a member of a squad.")
    squad = db.query(SquadRow).filter_by(invite_code=body.invite_code.strip().upper()).first()
    if not squad:
        raise HTTPException(status_code=404, detail="Squad invite code not found.")
    count = db.query(func.count(SquadMemberRow.id)).filter_by(squad_id=squad.id).scalar() or 0
    if int(count) >= 10:
        raise HTTPException(status_code=409, detail="Squad is full (max 10 developers per ФТ-14.4).")
    db.add(SquadMemberRow(squad_id=squad.id, user_id=user.id, joined_at_ms=now_ms()))
    db.commit()
    return _serialize_squad(squad, db)


@router.get("/v1/squads/my")
def get_my_squad(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    membership = db.query(SquadMemberRow).filter_by(user_id=user.id).first()
    if not membership:
        return {"squad": None}
    squad = db.get(SquadRow, membership.squad_id)
    if not squad:
        return {"squad": None}
    return {"squad": _serialize_squad(squad, db)}


@router.post("/v1/squads/leave")
def leave_squad(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    membership = db.query(SquadMemberRow).filter_by(user_id=user.id).first()
    if not membership:
        raise HTTPException(status_code=404, detail="Not currently in a squad.")
    db.delete(membership)
    db.commit()
    return {"left": True}
