from __future__ import annotations

import os
import sys
import time
from pathlib import Path

import fakeredis
import pytest
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import app  # noqa: E402
from app.models import Base, engine  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


client = TestClient(app)


def _register(email: str, username: str):
    res = client.post(
        "/v1/auth/register",
        json={
            "email": email,
            "password": "StrongPassword123!",
            "username": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201
    return res.json()["user_id"], {"Authorization": f"Bearer {res.json()['access_token']}"}


def test_redis_ws_ticket_single_use_ttl_and_replay_rejection():
    from app.redis_client import get_redis_client

    fake_redis = fakeredis.FakeRedis(decode_responses=True)
    app.dependency_overrides[get_redis_client] = lambda: fake_redis
    try:
        _, hdr = _register("ws_user@example.com", "ws_user")

        # 1. Request one-time WS ticket (stored in Redis with ~30s TTL)
        t_resp = client.post("/v1/ws/ticket", headers=hdr)
        assert t_resp.status_code == 200, t_resp.text
        t_data = t_resp.json()
        ticket = t_data["ticket"]
        assert 1 <= t_data["expires_in_seconds"] <= 60

        # Verify key exists in Redis with TTL > 0 and <= 30
        keys = fake_redis.keys("ws_ticket:*")
        assert len(keys) == 1
        ttl = fake_redis.ttl(keys[0])
        assert 0 < ttl <= 30

        # 2. Connect to room WS using ?ticket= succeeds once
        with client.websocket_connect(f"/ws/rooms/backend-english?ticket={ticket}") as ws:
            ws.send_json({"body": "Hello from Redis ticket WS!"})
            evt = ws.receive_json()
            assert evt["type"] == "message"
            assert evt["body"] == "Hello from Redis ticket WS!"

        # Key was atomically consumed on connect
        assert fake_redis.keys("ws_ticket:*") == []

        # 3. Replaying the same ticket is rejected (close 1008)
        with pytest.raises(WebSocketDisconnect) as exc_info:
            with client.websocket_connect(f"/ws/rooms/backend-english?ticket={ticket}") as ws:
                ws.receive_json()
        assert exc_info.value.code == 1008

        # 4. Expired ticket in Redis is rejected
        t_resp2 = client.post("/v1/ws/ticket", headers=hdr)
        ticket2 = t_resp2.json()["ticket"]
        for k in fake_redis.keys("ws_ticket:*"):
            fake_redis.delete(k)  # Simulate TTL expiry

        with pytest.raises(WebSocketDisconnect) as exc_exp:
            with client.websocket_connect(f"/ws/rooms/backend-english?ticket={ticket2}") as ws:
                ws.receive_json()
        assert exc_exp.value.code == 1008

        # 5. PvP WS also authenticates with one-time ticket
        m = client.post("/v1/pvp/matchmake", headers=hdr, json={"force_training_fallback": True}).json()
        t_pvp = client.post("/v1/ws/ticket", headers=hdr).json()["ticket"]
        with client.websocket_connect(f"/ws/pvp/{m['match_id']}?ticket={t_pvp}") as ws:
            ready = ws.receive_json()
            assert ready["type"] == "match_ready"

        # Replay on PvP WS also rejected
        with pytest.raises(WebSocketDisconnect) as exc_pvp:
            with client.websocket_connect(f"/ws/pvp/{m['match_id']}?ticket={t_pvp}") as ws:
                ws.receive_json()
        assert exc_pvp.value.code == 1008

        # 6. Legacy ?token=<jwt> query param is permanently rejected (BUG-10 / SP1-07b)
        raw_jwt = hdr["Authorization"].split(" ", 1)[1]
        with pytest.raises(WebSocketDisconnect) as exc_jwt:
            with client.websocket_connect(f"/ws/rooms/backend-english?token={raw_jwt}") as ws:
                ws.receive_json()
        assert exc_jwt.value.code == 1008
    finally:
        app.dependency_overrides.pop(get_redis_client, None)


def test_ws_ticket_ttl_expiry_and_prod_requires_redis_url(monkeypatch: pytest.MonkeyPatch):
    from app.redis_client import consume_ws_ticket, get_redis_client, issue_ws_ticket, reset_redis_client

    fake_redis = fakeredis.FakeRedis(decode_responses=True)
    short_ticket = issue_ws_ticket(fake_redis, user_id="u-ttl", ttl_seconds=1)
    time.sleep(1.15)
    assert consume_ws_ticket(fake_redis, short_ticket) is None

    reset_redis_client()
    monkeypatch.setenv("ENV", "prod")
    monkeypatch.delenv("REDIS_URL", raising=False)
    try:
        with pytest.raises(RuntimeError, match="REDIS_URL"):
            get_redis_client()
    finally:
        reset_redis_client()
