from __future__ import annotations

import os
import sys
from pathlib import Path

import fakeredis
import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import create_app  # noqa: E402
from app.models import Base, engine  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


def test_no_process_global_ws_or_pvp_dictionaries_remain_in_backend():
    app_dir = BACKEND_DIR / "app"
    for py_file in app_dir.rglob("*.py"):
        src = py_file.read_text(encoding="utf-8")
        assert "_ROOM_WS_CONNECTIONS" not in src, f"Process-global _ROOM_WS_CONNECTIONS still in {py_file}"
        assert "_PVP_WAITING_MATCHES" not in src, f"Process-global _PVP_WAITING_MATCHES still in {py_file}"


def test_two_independent_worker_instances_share_matchmaking_and_pubsub_over_single_redis():
    from app.redis_client import get_redis_client
    from app.services.realtime_broker import RedisRealtimeBroker

    # One shared Redis backend (FakeServer mirrors a single Redis 7 instance shared by 2 independent workers)
    shared_server = fakeredis.FakeServer()
    redis_worker_1 = fakeredis.FakeRedis(server=shared_server, decode_responses=True)
    redis_worker_2 = fakeredis.FakeRedis(server=shared_server, decode_responses=True)

    app_worker_1 = create_app()
    app_worker_2 = create_app()
    app_worker_1.dependency_overrides[get_redis_client] = lambda: redis_worker_1
    app_worker_2.dependency_overrides[get_redis_client] = lambda: redis_worker_2

    client1 = TestClient(app_worker_1)
    client2 = TestClient(app_worker_2)

    def _register(c: TestClient, email: str, username: str):
        r = c.post(
            "/v1/auth/register",
            json={
                "email": email,
                "password": "StrongPassword123!",
                "username": username,
                "age_confirmed_16_plus": True,
            },
        )
        assert r.status_code == 201, r.text
        return {"Authorization": f"Bearer {r.json()['access_token']}"}

    hdr1 = _register(client1, "worker1_u1@example.com", "worker1_u1")
    hdr2 = _register(client2, "worker2_u2@example.com", "worker2_u2")

    # 1. Cross-worker PvP matchmaking through Redis queue
    m1 = client1.post("/v1/pvp/matchmake", headers=hdr1, json={"force_training_fallback": False})
    assert m1.status_code == 200 and m1.json()["state"] == "SEARCHING"
    match_id = m1.json()["match_id"]

    # Worker 2 dequeues Player 1's waiting match from Redis and pairs Player 2
    m2 = client2.post("/v1/pvp/matchmake", headers=hdr2, json={"force_training_fallback": False})
    assert m2.status_code == 200
    assert m2.json()["match_id"] == match_id
    assert m2.json()["state"] == "IN_PROGRESS"
    assert m2.json()["mode"] == "RANKED_1V1"

    # 2. Cross-worker Room WebSocket delivery via Redis Pub/Sub:
    #    Player 1 listens on Worker 1 WS; Player 2 publishes via Worker 2 broker/WS -> Player 1 receives it!
    ticket1 = client1.post("/v1/ws/ticket?scope=rooms", headers=hdr1).json()["ticket"]
    broker2 = RedisRealtimeBroker(redis_worker_2)

    with client1.websocket_connect(f"/ws/rooms/backend-english?ticket={ticket1}") as ws1:
        # Publish from Worker 2 (independent app & redis client)
        broker2.publish_room_event(
            "backend-english",
            {
                "type": "message",
                "id": "cross-worker-msg-1",
                "room_slug": "backend-english",
                "author_user_id": "u2",
                "author_username": "worker2_u2",
                "author_rank_title": "Intern Developer",
                "body": "Hello across Redis Pub/Sub workers!",
                "created_at_ms": 1_800_000_000_000,
            },
        )
        evt = ws1.receive_json()
        assert evt["type"] == "message"
        assert evt["id"] == "cross-worker-msg-1"
        assert evt["body"] == "Hello across Redis Pub/Sub workers!"

    # 3. Cross-worker PvP Pub/Sub broker test: Worker 1 subscribes, Worker 2 publishes score update
    broker1 = RedisRealtimeBroker(redis_worker_1)
    ps = broker1.subscribe_pvp(match_id)
    try:
        broker2.publish_pvp_event(match_id, {"type": "opponent_score", "match_id": match_id, "score": 5})
        received = broker1.read_next_event(ps, timeout_seconds=1.0)
        assert received is not None
        assert received["type"] == "opponent_score"
        assert received["score"] == 5
    finally:
        ps.close()
