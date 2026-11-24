from __future__ import annotations

import asyncio
import json
import uuid
from typing import Any, List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, WebSocket, WebSocketDisconnect, status
from sqlalchemy.orm import Session

from app.anti_cheat import rank_title_for_xp, validate_ugc_text
from app.deps import get_current_user, get_db, now_ms
from app.models import PvpMatchRow, RoomMessageRow, SessionLocal, UserBlockRow, UserRow
from app.redis_client import WS_TICKET_TTL_SECONDS, consume_ws_ticket, get_redis_client, issue_ws_ticket
from app.schemas.realtime import (
    PostRoomMessageRequest,
    PvpDisconnectRequest,
    PvpMatchmakeRequest,
    PvpSubmitRequest,
)
from app.services.realtime_broker import RedisRealtimeBroker, get_realtime_broker

router = APIRouter(tags=['realtime'])


THEMATIC_ROOMS = {
    "code-review-practice": {
        "slug": "code-review-practice",
        "title": "#code-review-practice",
        "description_ru": "Разбор комментариев к Pull Request и вежливых формулировок",
    },
    "daily-standup": {
        "slug": "daily-standup",
        "title": "#daily-standup",
        "description_ru": "Ежедневные статус-апдейты, блокеры и синки команды",
    },
    "interviews": {
        "slug": "interviews",
        "title": "#interviews",
        "description_ru": "Подготовка к техническим и behavioural собеседованиям на английском",
    },
    "backend-english": {
        "slug": "backend-english",
        "title": "#backend-english",
        "description_ru": "Архитектура, базы данных, API-контракты и инциденты",
    },
}

_SERVER_PVP_QUESTIONS = [
    {
        "id": "pvp_q1",
        "prompt_en": "Which term describes an operation that produces the same result when repeated?",
        "options": ["Idempotency", "Sharding", "Starvation", "Deadlock"],
        "correct_option_index": 0,
        "category_tag": "API",
    },
    {
        "id": "pvp_q2",
        "prompt_en": "Log error: 'SQLSTATE 40P01: deadlock detected'. What happened?",
        "options": [
            "Two transactions are mutually waiting for locks",
            "Disk quota exceeded on WAL volume",
            "TLS certificate expired",
            "JSON syntax error in request body",
        ],
        "correct_option_index": 0,
        "category_tag": "LOGS",
    },
    {
        "id": "pvp_q3",
        "prompt_en": "Best polite PR comment to suggest extracting a helper function:",
        "options": [
            "Nit: could we extract this parsing block into a dedicated helper for readability?",
            "Bad code, rewrite immediately.",
            "Delete this file.",
            "Why did you write this?",
        ],
        "correct_option_index": 0,
        "category_tag": "PR",
    },
    {
        "id": "pvp_q4",
        "prompt_en": "What does 'backpressure' prevent in reactive streaming pipelines?",
        "options": [
            "Slow consumers being overwhelmed by fast producers",
            "DNS cache poisoning",
            "SQL injection in prepared statements",
            "Compile-time type erasure",
        ],
        "correct_option_index": 0,
        "category_tag": "SYSTEMS",
    },
    {
        "id": "pvp_q5",
        "prompt_en": "In Kubernetes, what happens when a container fails its livenessProbe?",
        "options": [
            "Kubelet restarts the container",
            "Cluster deletes the namespace",
            "Ingress switches to HTTP/1.0",
            "Secrets are rotated automatically",
        ],
        "correct_option_index": 0,
        "category_tag": "DEVOPS",
    },
    {
        "id": "pvp_q6",
        "prompt_en": "Which phrase best reports a production rollback during an incident?",
        "options": [
            "We have rolled back release v3.2 to mitigate elevated 5xx latency and are investigating root cause.",
            "Everything broke, maybe fixed tomorrow.",
            "Server is sad.",
            "Please restart your laptop.",
        ],
        "correct_option_index": 0,
        "category_tag": "INCIDENT",
    },
    {
        "id": "pvp_q7",
        "prompt_en": "What is a 'canary deployment'?",
        "options": [
            "Gradually routing a small percentage of traffic to a new version before full rollout",
            "Deleting all staging environments",
            "Running unit tests without assertions",
            "Disabling database indexes in production",
        ],
        "correct_option_index": 0,
        "category_tag": "DEVOPS",
    },
    {
        "id": "pvp_q8",
        "prompt_en": "Log error: 'java.lang.OutOfMemoryError: Metaspace'. Which layer is affected?",
        "options": [
            "JVM class metadata memory region",
            "TCP three-way handshake",
            "Git merge driver",
            "Android Room foreign key check",
        ],
        "correct_option_index": 0,
        "category_tag": "LOGS",
    },
]



@router.get("/v1/rooms")
def list_thematic_rooms():
    return {"rooms": list(THEMATIC_ROOMS.values())}


@router.get("/v1/rooms/{room_slug}/messages")
def list_room_messages(
    room_slug: str,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if room_slug not in THEMATIC_ROOMS:
        raise HTTPException(status_code=404, detail="Room not found")
    blocked_ids = {r.blocked_id for r in db.query(UserBlockRow).filter_by(blocker_id=user.id).all()}
    rows = (
        db.query(RoomMessageRow)
        .filter(RoomMessageRow.room_slug == room_slug, RoomMessageRow.is_hidden == False)  # noqa: E712
        .order_by(RoomMessageRow.created_at_ms.desc())
        .limit(60)
        .all()
    )
    rows.reverse()
    return {
        "room_slug": room_slug,
        "messages": [
            {
                "id": m.id,
                "room_slug": m.room_slug,
                "author_user_id": m.author_user_id,
                "author_username": m.author_username,
                "author_rank_title": m.author_rank_title,
                "body": m.body,
                "created_at_ms": m.created_at_ms,
            }
            for m in rows
            if m.author_user_id not in blocked_ids
        ],
    }


@router.post("/v1/rooms/{room_slug}/messages", status_code=status.HTTP_201_CREATED)
def post_room_message(
    room_slug: str,
    body: PostRoomMessageRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    broker: RedisRealtimeBroker = Depends(get_realtime_broker),
):
    if room_slug not in THEMATIC_ROOMS:
        raise HTTPException(status_code=404, detail="Room not found")
    try:
        clean_body = validate_ugc_text(body.body, min_len=2, max_len=800)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))

    msg = RoomMessageRow(
        id=str(uuid.uuid4()),
        room_slug=room_slug,
        author_user_id=user.id,
        author_username=user.username,
        author_rank_title=rank_title_for_xp(user.total_xp),
        body=clean_body,
        is_hidden=False,
        created_at_ms=now_ms(),
    )
    db.add(msg)
    db.commit()
    payload = {
        "id": msg.id,
        "room_slug": msg.room_slug,
        "author_user_id": msg.author_user_id,
        "author_username": msg.author_username,
        "author_rank_title": msg.author_rank_title,
        "body": msg.body,
        "created_at_ms": msg.created_at_ms,
    }
    broker.publish_room_event(room_slug, {"type": "message", **payload})
    return payload


@router.post("/v1/ws/ticket")
def issue_websocket_ticket(
    scope: str = Query(default="ws", max_length=32),
    user: UserRow = Depends(get_current_user),
    redis_client=Depends(get_redis_client),
):
    """Issues a short-lived (~30s), single-use WebSocket ticket stored in Redis (SP1-07a / BUG-10)."""
    ticket = issue_ws_ticket(redis_client, user_id=user.id, scope=scope.strip() or "ws", ttl_seconds=WS_TICKET_TTL_SECONDS)
    return {"ticket": ticket, "expires_in_seconds": WS_TICKET_TTL_SECONDS}


def _resolve_ws_user_id(redis_client: Any, ticket: str, expected_scope: str) -> Optional[str]:
    if not ticket:
        return None
    return consume_ws_ticket(redis_client, ticket, expected_scope=expected_scope)


@router.websocket("/ws/rooms/{room_slug}")
async def room_websocket(
    websocket: WebSocket,
    room_slug: str,
    ticket: str = Query(default=""),
    redis_client=Depends(get_redis_client),
):
    if room_slug not in THEMATIC_ROOMS:
        await websocket.close(code=1008)
        return
    uid = _resolve_ws_user_id(redis_client, ticket=ticket, expected_scope="rooms")
    if not uid:
        await websocket.close(code=1008)
        return

    with SessionLocal() as db:
        user = db.get(UserRow, uid)
        if not user:
            await websocket.close(code=1008)
            return
        username = user.username
        rank_title = rank_title_for_xp(user.total_xp)

    broker = RedisRealtimeBroker(redis_client)
    pubsub = broker.subscribe_room(room_slug)
    stop_event = asyncio.Event()
    await websocket.accept()
    forward_task = asyncio.create_task(
        broker.forward_pubsub_to_websocket(pubsub, websocket, stop_event)
    )
    try:
        while True:
            raw = await websocket.receive_json()
            text = str(raw.get("body", "")).strip()
            try:
                clean = validate_ugc_text(text, min_len=2, max_len=800)
            except ValueError as exc:
                await websocket.send_json({"type": "error", "detail": str(exc)})
                continue
            ts = now_ms()
            msg_id = str(uuid.uuid4())
            with SessionLocal() as db:
                db.add(
                    RoomMessageRow(
                        id=msg_id,
                        room_slug=room_slug,
                        author_user_id=uid,
                        author_username=username,
                        author_rank_title=rank_title,
                        body=clean,
                        is_hidden=False,
                        created_at_ms=ts,
                    )
                )
                db.commit()
            event = {
                "type": "message",
                "id": msg_id,
                "room_slug": room_slug,
                "author_user_id": uid,
                "author_username": username,
                "author_rank_title": rank_title,
                "body": clean,
                "created_at_ms": ts,
            }
            broker.publish_room_event(room_slug, event)
    except WebSocketDisconnect:
        pass
    finally:
        stop_event.set()
        await asyncio.gather(forward_task, return_exceptions=True)
        try:
            pubsub.close()
        except Exception:
            pass


# ---- ФТ-14.1 — 1v1 PvP Duels (Server Questions, 15s Training Fallback, Disconnect) ----


def _public_pvp_questions(questions: List[dict]) -> List[dict]:
    """Strip authoritative answer keys before sending PvP questions to clients (SP3-01 / BUG-09)."""
    return [
        {
            "id": q["id"],
            "prompt_en": q["prompt_en"],
            "options": list(q["options"]),
            "category_tag": q.get("category_tag", "VOCAB"),
        }
        for q in questions
    ]


@router.post("/v1/pvp/matchmake")
def pvp_matchmake(
    body: PvpMatchmakeRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    broker: RedisRealtimeBroker = Depends(get_realtime_broker),
):
    ts = now_ms()
    questions = _SERVER_PVP_QUESTIONS[:8]  # 8 server-issued questions (within 7–10 spec)

    if not body.force_training_fallback:
        requeue_ids: List[str] = []
        matched: Optional[PvpMatchRow] = None
        while True:
            waiting_id = broker.dequeue_pvp_match()
            if not waiting_id:
                break
            candidate = db.get(PvpMatchRow, waiting_id)
            if not candidate or candidate.state != "SEARCHING":
                continue
            if candidate.player1_id == user.id:
                requeue_ids.append(waiting_id)
                continue
            matched = candidate
            break
        for rid in requeue_ids:
            broker.enqueue_pvp_match(rid)
        if matched is not None:
            matched.player2_id = user.id
            matched.mode = "RANKED_1V1"
            matched.state = "IN_PROGRESS"
            db.commit()
            broker.publish_pvp_event(
                matched.id,
                {"type": "match_matched", "match_id": matched.id, "player2_id": user.id},
            )
            return {
                "match_id": matched.id,
                "mode": matched.mode,
                "state": matched.state,
                "awards_ranked_points": True,
                "round_duration_seconds": 60,
                "matchmaking_timeout_seconds": 15,
                "questions": _public_pvp_questions(json.loads(matched.questions_json)),
            }

    mode = "TRAINING_FALLBACK" if body.force_training_fallback else "RANKED_1V1"
    initial_state = "IN_PROGRESS" if body.force_training_fallback else "SEARCHING"
    match_id = str(uuid.uuid4())
    match = PvpMatchRow(
        id=match_id,
        player1_id=user.id,
        player2_id=None,
        mode=mode,
        state=initial_state,
        questions_json=json.dumps(questions, ensure_ascii=False),
        created_at_ms=ts,
    )
    db.add(match)
    db.commit()
    if initial_state == "SEARCHING":
        broker.enqueue_pvp_match(match_id)

    return {
        "match_id": match.id,
        "mode": match.mode,
        "state": match.state,
        "awards_ranked_points": match.mode == "RANKED_1V1",
        "round_duration_seconds": 60,
        "matchmaking_timeout_seconds": 15,
        "questions": _public_pvp_questions(questions),
    }


@router.post("/v1/pvp/{match_id}/submit")
def pvp_submit_answers(
    match_id: str,
    body: PvpSubmitRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    match = db.get(PvpMatchRow, match_id)
    if not match or user.id not in (match.player1_id, match.player2_id):
        raise HTTPException(status_code=404, detail="PvP match not found")

    questions = json.loads(match.questions_json)
    correct_count = 0
    for idx, q in enumerate(questions):
        if idx < len(body.answers) and body.answers[idx] == q["correct_option_index"]:
            correct_count += 1

    if user.id == match.player1_id:
        match.player1_score = correct_count
    else:
        match.player2_score = correct_count

    match.state = "COMPLETED"
    match.finished_at_ms = now_ms()
    if match.mode == "TRAINING_FALLBACK":
        match.ranked_xp_awarded = 0
        rating_delta = 0
    else:
        opp_score = match.player2_score if user.id == match.player1_id else match.player1_score
        if correct_count > opp_score:
            match.ranked_xp_awarded = 30
            rating_delta = 15
        elif correct_count == opp_score:
            match.ranked_xp_awarded = 15
            rating_delta = 0
        else:
            match.ranked_xp_awarded = 5
            rating_delta = -10
    db.commit()
    return {
        "match_id": match.id,
        "mode": match.mode,
        "state": match.state,
        "player_score": correct_count,
        "total_questions": len(questions),
        "ranked_xp_awarded": match.ranked_xp_awarded,
        "rating_delta": rating_delta,
    }


@router.post("/v1/pvp/{match_id}/disconnect")
def pvp_disconnect(
    match_id: str,
    body: PvpDisconnectRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    broker: RedisRealtimeBroker = Depends(get_realtime_broker),
):
    match = db.get(PvpMatchRow, match_id)
    if not match or user.id not in (match.player1_id, match.player2_id):
        raise HTTPException(status_code=404, detail="PvP match not found")
    broker.remove_pvp_match(match_id)
    match.state = "DISCONNECTED_FORFEIT"
    match.finished_at_ms = now_ms()
    match.ranked_xp_awarded = 0
    db.commit()
    broker.publish_pvp_event(
        match.id,
        {"type": "opponent_disconnected", "match_id": match.id, "user_id": user.id, "reason": body.reason},
    )
    return {
        "match_id": match.id,
        "mode": match.mode,
        "state": match.state,
        "ranked_xp_awarded": 0,
        "rating_delta": -10 if match.mode == "RANKED_1V1" else 0,
        "reason": body.reason,
    }


@router.websocket("/ws/pvp/{match_id}")
async def pvp_websocket(
    websocket: WebSocket,
    match_id: str,
    ticket: str = Query(default=""),
    redis_client=Depends(get_redis_client),
):
    uid = _resolve_ws_user_id(redis_client, ticket=ticket, expected_scope="pvp")
    if not uid:
        await websocket.close(code=1008)
        return

    with SessionLocal() as db:
        match = db.get(PvpMatchRow, match_id)
        if not match or uid not in (match.player1_id, match.player2_id):
            await websocket.close(code=1008)
            return
        questions = json.loads(match.questions_json)
        mode = match.mode

    broker = RedisRealtimeBroker(redis_client)
    pubsub = broker.subscribe_pvp(match_id)
    stop_event = asyncio.Event()
    await websocket.accept()
    await websocket.send_json(
        {
            "type": "match_ready",
            "match_id": match_id,
            "mode": mode,
            "round_duration_seconds": 60,
            "questions": _public_pvp_questions(questions),
        }
    )
    forward_task = asyncio.create_task(
        broker.forward_pubsub_to_websocket(
            pubsub,
            websocket,
            stop_event,
            predicate=lambda evt: evt.get("user_id") != uid,
        )
    )
    score = 0
    try:
        while True:
            msg = await websocket.receive_json()
            if msg.get("type") == "answer":
                q_idx = int(msg.get("question_index", -1))
                opt_idx = int(msg.get("option_index", -1))
                correct_idx = questions[q_idx]["correct_option_index"] if 0 <= q_idx < len(questions) else -1
                is_correct = correct_idx >= 0 and correct_idx == opt_idx
                if is_correct:
                    score += 1
                await websocket.send_json(
                    {
                        "type": "answer_ack",
                        "question_index": q_idx,
                        "is_correct": is_correct,
                        "correct_option_index": correct_idx,
                        "score": score,
                    }
                )
                broker.publish_pvp_event(
                    match_id,
                    {"type": "opponent_score", "match_id": match_id, "user_id": uid, "score": score},
                )
            elif msg.get("type") == "finish":
                with SessionLocal() as db:
                    m = db.get(PvpMatchRow, match_id)
                    if m:
                        m.state = "COMPLETED"
                        m.finished_at_ms = now_ms()
                        if uid == m.player1_id:
                            m.player1_score = score
                        else:
                            m.player2_score = score
                        db.commit()
                broker.publish_pvp_event(
                    match_id,
                    {"type": "opponent_finished", "match_id": match_id, "user_id": uid, "score": score},
                )
                await websocket.send_json({"type": "match_completed", "score": score, "mode": mode})
                break
    except WebSocketDisconnect:
        # Cleanly finalize match on socket drop without hanging state
        broker.remove_pvp_match(match_id)
        with SessionLocal() as db:
            m = db.get(PvpMatchRow, match_id)
            if m and m.state not in ("COMPLETED", "DISCONNECTED_FORFEIT"):
                m.state = "DISCONNECTED_FORFEIT"
                m.finished_at_ms = now_ms()
                db.commit()
        broker.publish_pvp_event(
            match_id,
            {"type": "opponent_disconnected", "match_id": match_id, "user_id": uid},
        )
    finally:
        stop_event.set()
        await asyncio.gather(forward_task, return_exceptions=True)
        try:
            pubsub.close()
        except Exception:
            pass
