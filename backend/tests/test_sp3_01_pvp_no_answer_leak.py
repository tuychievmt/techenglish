from __future__ import annotations

import os
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import app  # noqa: E402
from app.models import Base, engine  # noqa: E402
from app.redis_client import get_redis_client  # noqa: E402
from app.services.realtime_broker import RedisRealtimeBroker  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    RedisRealtimeBroker(get_redis_client()).clear_pvp_queue()
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    RedisRealtimeBroker(get_redis_client()).clear_pvp_queue()
    Base.metadata.drop_all(bind=engine)


def _register(client: TestClient, email: str, username: str):
    res = client.post(
        "/v1/auth/register",
        json={
            "email": email,
            "password": "StrongPassword123!",
            "username": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201, res.text
    d = res.json()
    return d["user_id"], {"Authorization": f"Bearer {d['access_token']}"}


def test_pvp_never_leaks_correct_option_index_before_answer_and_streams_opponent_score():
    client = TestClient(app)
    _, hdr1 = _register(client, "pvp_sec1@example.com", "pvp_sec1")
    _, hdr2 = _register(client, "pvp_sec2@example.com", "pvp_sec2")

    m1 = client.post("/v1/pvp/matchmake", headers=hdr1, json={"force_training_fallback": False})
    assert m1.status_code == 200
    for q in m1.json()["questions"]:
        assert "correct_option_index" not in q, f"Answer key leaked in matchmake response: {q}"

    m2 = client.post("/v1/pvp/matchmake", headers=hdr2, json={"force_training_fallback": False})
    assert m2.status_code == 200
    match_id = m2.json()["match_id"]
    for q in m2.json()["questions"]:
        assert "correct_option_index" not in q, f"Answer key leaked in player 2 matchmake response: {q}"

    t1 = client.post("/v1/ws/ticket?scope=pvp", headers=hdr1).json()["ticket"]
    t2 = client.post("/v1/ws/ticket?scope=pvp", headers=hdr2).json()["ticket"]

    with client.websocket_connect(f"/ws/pvp/{match_id}?ticket={t1}") as ws1, \
         client.websocket_connect(f"/ws/pvp/{match_id}?ticket={t2}") as ws2:
        ready1 = ws1.receive_json()
        ready2 = ws2.receive_json()
        assert ready1["type"] == "match_ready"
        assert ready2["type"] == "match_ready"
        for q in ready1["questions"]:
            assert "correct_option_index" not in q, f"Answer key leaked in WS match_ready: {q}"

        # Player 1 answers Q0 -> receives answer_ack with revealed correct_option_index; Player 2 receives opponent_score
        ws1.send_json({"type": "answer", "question_index": 0, "option_index": 0})
        ack1 = ws1.receive_json()
        assert ack1["type"] == "answer_ack"
        assert ack1["is_correct"] is True
        assert ack1["correct_option_index"] == 0
        assert ack1["score"] == 1

        opp_evt = ws2.receive_json()
        assert opp_evt["type"] == "opponent_score"
        assert opp_evt["score"] == 1
