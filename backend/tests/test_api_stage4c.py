from __future__ import annotations

import base64
import os
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import app  # noqa: E402
from app.models import Base, SessionLocal, PvpMatchRow, engine  # noqa: E402
from app.redis_client import get_redis_client  # noqa: E402
from app.services.realtime_broker import RedisRealtimeBroker  # noqa: E402
from app.voice_storage import voice_storage  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    RedisRealtimeBroker(get_redis_client()).clear_pvp_queue()
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    RedisRealtimeBroker(get_redis_client()).clear_pvp_queue()
    Base.metadata.drop_all(bind=engine)


client = TestClient(app)


def _register(email: str, username: str):
    res = client.post(
        "/v1/auth/register",
        json={
            "email": email,
            "password": "StrongPassword123!",
            "username": username,
            "display_name": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201, res.text
    d = res.json()
    return d["user_id"], d["access_token"], {"Authorization": f"Bearer {d['access_token']}"}


def test_thematic_rooms_websocket_and_moderation():
    uid_a, tok_a, hdr_a = _register("room_a@dev.test", "room_alice")
    uid_b, tok_b, hdr_b = _register("room_b@dev.test", "room_bob")
    uid_c, _, hdr_c = _register("room_c@dev.test", "room_carol")
    uid_d, _, hdr_d = _register("room_d@dev.test", "room_dave")

    rooms = client.get("/v1/rooms").json()["rooms"]
    assert len(rooms) == 4
    assert {r["slug"] for r in rooms} == {"code-review-practice", "daily-standup", "interviews", "backend-english"}

    # Real-time WebSocket message send & broadcast via single-use WS ticket (SP1-07b)
    ws_ticket = client.post("/v1/ws/ticket?scope=rooms", headers=hdr_a).json()["ticket"]
    with client.websocket_connect(f"/ws/rooms/code-review-practice?ticket={ws_ticket}") as ws:
        ws.send_json({"body": "Nit: could we extract this SQL query into a DAO method?"})
        evt = ws.receive_json()
        assert evt["type"] == "message"
        assert evt["author_username"] == "room_alice"
        msg_id = evt["id"]

    hist = client.get("/v1/rooms/code-review-practice/messages", headers=hdr_b).json()["messages"]
    assert any(m["id"] == msg_id for m in hist)

    # 3 distinct moderation reports auto-hide room message
    for hdr in (hdr_b, hdr_c, hdr_d):
        r = client.post(
            "/v1/moderation/report",
            headers=hdr,
            json={"target_type": "ROOM_MESSAGE", "target_id": msg_id, "reason": "spam"},
        )
        assert r.status_code == 201

    hist_after = client.get("/v1/rooms/code-review-practice/messages", headers=hdr_d).json()["messages"]
    assert all(m["id"] != msg_id for m in hist_after)


def test_pvp_1v1_server_questions_training_fallback_and_clean_disconnect():
    uid1, tok1, hdr1 = _register("pvp1@dev.test", "pvp_one")
    uid2, tok2, hdr2 = _register("pvp2@dev.test", "pvp_two")

    # 1. Training fallback after 15s timeout -> 8 server questions, 0 ranked XP / 0 rating delta
    tr = client.post("/v1/pvp/matchmake", headers=hdr1, json={"force_training_fallback": True})
    assert tr.status_code == 200
    tr_data = tr.json()
    assert tr_data["mode"] == "TRAINING_FALLBACK"
    assert tr_data["awards_ranked_points"] is False
    assert 7 <= len(tr_data["questions"]) <= 10

    sub_tr = client.post(
        f"/v1/pvp/{tr_data['match_id']}/submit",
        headers=hdr1,
        json={"answers": [0] * len(tr_data["questions"])},
    )
    assert sub_tr.status_code == 200
    assert sub_tr.json()["ranked_xp_awarded"] == 0
    assert sub_tr.json()["rating_delta"] == 0

    # 2. Ranked 1v1 matchmaking between two players + WebSocket play + clean disconnect finalization
    m1 = client.post("/v1/pvp/matchmake", headers=hdr1, json={"force_training_fallback": False}).json()
    assert m1["state"] == "SEARCHING"
    m2 = client.post("/v1/pvp/matchmake", headers=hdr2, json={"force_training_fallback": False}).json()
    assert m2["match_id"] == m1["match_id"]
    assert m2["state"] == "IN_PROGRESS"

    match_id = m2["match_id"]
    pvp_ticket = client.post("/v1/ws/ticket?scope=pvp", headers=hdr1).json()["ticket"]
    with client.websocket_connect(f"/ws/pvp/{match_id}?ticket={pvp_ticket}") as ws:
        ready = ws.receive_json()
        assert ready["type"] == "match_ready"
        assert len(ready["questions"]) == 8
        ws.send_json({"type": "answer", "question_index": 0, "option_index": 0})
        ack = ws.receive_json()
        assert ack["type"] == "answer_ack"
        assert ack["is_correct"] is True
        # Abruptly close socket -> server must finalize cleanly as DISCONNECTED_FORFEIT

    with SessionLocal() as db:
        row = db.get(PvpMatchRow, match_id)
        assert row is not None
        assert row.state == "DISCONNECTED_FORFEIT"


def test_peer_review_voice_clip_ttl_feedback_and_hard_delete_on_account_removal():
    uid_a, _, hdr_a = _register("peer_a@dev.test", "peer_alice")
    uid_b, _, hdr_b = _register("peer_b@dev.test", "peer_bob")

    # Privacy policy declares 30-day TTL for Peer Review voice clips (Errata E5)
    pp = client.get("/v1/legal/privacy-policy").json()
    assert pp["peer_review_voice_ttl_days"] == 30
    assert pp["pronunciation_voice_retention_days"] == 0

    daily = client.get("/v1/peer-review/daily-case").json()
    assert daily["voice_max_seconds"] == 20
    assert daily["voice_ttl_days"] == 30

    fake_audio = base64.b64encode(b"OggS-test-peer-review-voice-clip-payload").decode("ascii")
    sub = client.post(
        "/v1/peer-review/submissions",
        headers=hdr_a,
        json={
            "case_id": daily["case_id"],
            "text_response": "The production deployment failed due to a missing migration index causing lock contention. We rolled back safely and are adding a concurrent index.",
            "voice_base64": fake_audio,
            "voice_duration_seconds": 14,
            "voice_content_type": "audio/ogg",
        },
    )
    assert sub.status_code == 201, sub.text
    sub_data = sub.json()
    voice_key = sub_data["voice_object_key"]
    assert voice_key is not None
    assert voice_storage.exists(voice_key) is True

    # Peer feedback from senior/peer reviewer
    fb = client.post(
        f"/v1/peer-review/submissions/{sub_data['id']}/feedback",
        headers=hdr_b,
        json={"rating_stars": 5, "comment_text": "Clear incident summary and crisp root-cause phrasing!"},
    )
    assert fb.status_code == 201

    feed = client.get("/v1/peer-review/submissions", headers=hdr_b).json()["items"]
    assert len(feed) == 1
    assert feed[0]["has_voice_clip"] is True
    assert len(feed[0]["feedback"]) == 1

    # DELETE /v1/account MUST delete both DB rows AND physical voice clip from storage!
    del_res = client.delete("/v1/account", headers=hdr_a)
    assert del_res.status_code == 200
    assert del_res.json()["deleted_voice_clips"] == 1
    assert voice_storage.exists(voice_key) is False
    assert client.get("/v1/peer-review/submissions", headers=hdr_b).json()["items"] == []
