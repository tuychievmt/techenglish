from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Body, Depends, HTTPException, Query
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.anti_cheat import PING_COOLDOWN_MS, rank_title_for_xp, week_start_utc5_ms
from app.deps import get_current_user, get_db, now_ms
from app.models import (
    ActivityFeedReactionRow,
    ActivityFeedRow,
    DevicePushTokenRow,
    FriendPingRow,
    FriendshipRow,
    SyncEventRow,
    UserBlockRow,
    UserRow,
)
from app.schemas.social import (
    AddFriendRequest,
    RegisterDeviceTokenRequest,
    UnregisterDeviceTokenRequest,
)
from app.services.push_gateway import PushMessage, PushNotificationGateway, get_push_gateway

router = APIRouter(tags=['social'])


@router.get("/v1/leaderboards/{tab}")
def get_leaderboard(
    tab: str,
    stack: Optional[str] = Query(default=None),
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    wk_start = week_start_utc5_ms(now_ms())
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}

    users_query = db.query(UserRow)
    if tab == "league":
        users_query = users_query.filter(UserRow.league == user.league)
    elif tab == "friends":
        friend_ids = [
            f.friend_id for f in db.query(FriendshipRow).filter(FriendshipRow.user_id == user.id).all()
        ] + [user.id]
        users_query = users_query.filter(UserRow.id.in_(friend_ids))
    elif tab == "tech_stack":
        target_stack = (stack or (user.tech_stack_csv.split(",")[0] if user.tech_stack_csv else "Kotlin")).strip()
        users_query = users_query.filter(UserRow.tech_stack_csv.ilike(f"%{target_stack}%"))

    candidates = [u for u in users_query.all() if u.id not in blocked_ids]

    weekly_xp_rows = (
        db.query(SyncEventRow.user_id, func.coalesce(func.sum(SyncEventRow.awarded_xp), 0))
        .filter(SyncEventRow.week_start_utc5_ms == wk_start)
        .group_by(SyncEventRow.user_id)
        .all()
    )
    weekly_map = {uid: int(xp) for uid, xp in weekly_xp_rows}

    ranked = sorted(candidates, key=lambda u: (-weekly_map.get(u.id, 0), -u.total_xp, u.username))
    entries = []
    pinned_me = None
    for idx, u in enumerate(ranked, start=1):
        row = {
            "rank": idx,
            "user_id": u.id,
            "username": u.username,
            "avatar_key": u.avatar_key,
            "rank_title": rank_title_for_xp(u.total_xp),
            "league": u.league,
            "tech_stack": u.tech_stack_csv,
            "weekly_xp": weekly_map.get(u.id, 0),
            "total_xp": u.total_xp,
            "is_current_user": u.id == user.id,
        }
        if idx <= 10:
            entries.append(row)
        if u.id == user.id:
            pinned_me = row

    return {
        "tab": tab,
        "week_start_utc5_ms": wk_start,
        "reset_timezone": "Asia/Dushanbe",
        "bots_used": False,
        "top_10": entries,
        "current_user_entry": pinned_me,
    }


# =========================================================================
# Friends, Activity Feed & Ping/Nudge (ФТ-12.1–12.3)
# =========================================================================


@router.get("/v1/friends/search")
def search_users(q: str = Query(min_length=2), user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    hits = (
        db.query(UserRow)
        .filter(UserRow.username.ilike(f"%{q.strip().lstrip('@').lower()}%"), UserRow.id != user.id)
        .limit(20)
        .all()
    )
    return {
        "results": [
            {
                "user_id": u.id,
                "username": u.username,
                "avatar_key": u.avatar_key,
                "rank_title": rank_title_for_xp(u.total_xp),
                "streak_days": u.streak_days,
            }
            for u in hits
            if u.id not in blocked_ids
        ]
    }


@router.post("/v1/friends/add")
def add_friend(body: AddFriendRequest, user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    target = db.query(UserRow).filter(UserRow.username == body.username.strip().lstrip("@").lower()).first()
    if not target or target.id == user.id:
        raise HTTPException(status_code=404, detail="Target user not found")
    ts = now_ms()
    for a, b in ((user.id, target.id), (target.id, user.id)):
        if not db.query(FriendshipRow).filter_by(user_id=a, friend_id=b).first():
            db.add(FriendshipRow(user_id=a, friend_id=b, created_at_ms=ts))
    db.commit()
    return {"added": True, "friend_id": target.id, "username": target.username}


@router.get("/v1/friends")
def list_friends(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    links = db.query(FriendshipRow).filter_by(user_id=user.id).all()
    friends = []
    for link in links:
        if link.friend_id in blocked_ids:
            continue
        f = db.get(UserRow, link.friend_id)
        if not f:
            continue
        last_ping = (
            db.query(func.max(FriendPingRow.sent_at_ms))
            .filter_by(sender_id=user.id, receiver_id=f.id)
            .scalar()
            or 0
        )
        friends.append(
            {
                "user_id": f.id,
                "username": f.username,
                "avatar_key": f.avatar_key,
                "rank_title": rank_title_for_xp(f.total_xp),
                "streak_days": f.streak_days,
                "accepts_pings": f.accepts_pings,
                "last_pinged_at_ms": int(last_ping),
            }
        )
    return {"friends": friends}


@router.get("/v1/friends/feed")
def friends_activity_feed(user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    friend_ids = [
        f.friend_id for f in db.query(FriendshipRow).filter_by(user_id=user.id).all() if f.friend_id not in blocked_ids
    ] + [user.id]
    rows = (
        db.query(ActivityFeedRow)
        .filter(ActivityFeedRow.actor_user_id.in_(friend_ids))
        .order_by(ActivityFeedRow.created_at_ms.desc())
        .limit(30)
        .all()
    )
    row_ids = [r.id for r in rows]
    reacted_ids = (
        {
            rx.event_id
            for rx in db.query(ActivityFeedReactionRow)
            .filter(
                ActivityFeedReactionRow.user_id == user.id,
                ActivityFeedReactionRow.event_id.in_(row_ids),
            )
            .all()
        }
        if row_ids
        else set()
    )
    items = []
    for r in rows:
        actor = db.get(UserRow, r.actor_user_id)
        items.append(
            {
                "id": r.id,
                "actor_user_id": r.actor_user_id,
                "actor_username": actor.username if actor else "dev",
                "summary_ru": r.summary_ru,
                "reaction_count": r.reaction_count,
                "user_reacted": r.id in reacted_ids,
                "created_at_ms": r.created_at_ms,
            }
        )
    return {"items": items}


@router.post("/v1/friends/feed/{event_id}/react")
def react_to_feed_item(event_id: str, user: UserRow = Depends(get_current_user), db: Session = Depends(get_db)):
    item = db.get(ActivityFeedRow, event_id)
    if not item:
        raise HTTPException(status_code=404, detail="Feed item not found")
    existing = db.query(ActivityFeedReactionRow).filter_by(event_id=event_id, user_id=user.id).first()
    if not existing:
        db.add(ActivityFeedReactionRow(event_id=event_id, user_id=user.id, created_at_ms=now_ms()))
        item.reaction_count += 1
        db.commit()
    return {"id": item.id, "reaction_count": item.reaction_count, "user_reacted": True}


@router.post("/v1/notifications/device-token")
def register_device_push_token(
    body: RegisterDeviceTokenRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    token = body.token.strip()
    platform = (body.platform or "ANDROID").strip().upper()[:24] or "ANDROID"
    ts = now_ms()
    existing = db.query(DevicePushTokenRow).filter_by(token=token).first()
    if existing:
        existing.user_id = user.id
        existing.platform = platform
        existing.updated_at_ms = ts
    else:
        db.add(
            DevicePushTokenRow(
                user_id=user.id,
                platform=platform,
                token=token,
                created_at_ms=ts,
                updated_at_ms=ts,
            )
        )
    db.commit()
    return {"registered": True, "platform": platform}


@router.delete("/v1/notifications/device-token")
def unregister_device_push_token(
    body: Optional[UnregisterDeviceTokenRequest] = Body(default=None),
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = db.query(DevicePushTokenRow).filter_by(user_id=user.id)
    if body and body.token and body.token.strip():
        query = query.filter_by(token=body.token.strip())
    removed = query.delete(synchronize_session=False)
    db.commit()
    return {"removed": int(removed)}


@router.post("/v1/friends/{friend_id}/ping")
def ping_friend(
    friend_id: str,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    push_gateway: PushNotificationGateway = Depends(get_push_gateway),
):
    target = db.get(UserRow, friend_id)
    if not target:
        raise HTTPException(status_code=404, detail="Friend not found")
    if not target.accepts_pings:
        raise HTTPException(status_code=403, detail="Friend has opted out of ping notifications")
    ts = now_ms()
    last_ping = (
        db.query(func.max(FriendPingRow.sent_at_ms))
        .filter_by(sender_id=user.id, receiver_id=friend_id)
        .scalar()
        or 0
    )
    if last_ping and (ts - int(last_ping)) < PING_COOLDOWN_MS:
        raise HTTPException(status_code=429, detail="Ping allowed at most once per 24 hours per friend (ФТ-12.3)")
    db.add(FriendPingRow(sender_id=user.id, receiver_id=friend_id, sent_at_ms=ts))
    db.commit()

    device_tokens = db.query(DevicePushTokenRow).filter_by(user_id=target.id).all()
    notified = 0
    provider = "none"
    for row in device_tokens:
        result = push_gateway.send(
            PushMessage(
                user_id=target.id,
                token=row.token,
                title=f"Пинг от @{user.username}",
                body="Друг напоминает закрыть дневную цель по техническому английскому!",
                data={
                    "type": "friend_ping",
                    "sender_id": user.id,
                    "sender_username": user.username,
                    "destination": "flashcards",
                },
            )
        )
        provider = result.provider
        if result.delivered:
            notified += 1

    return {
        "pinged": True,
        "friend_id": friend_id,
        "sent_at_ms": ts,
        "push_dispatched": notified > 0,
        "push_devices_notified": notified,
        "push_provider": provider,
    }
