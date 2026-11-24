from __future__ import annotations

import base64
import hashlib
import os
import sys
import time
import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BACKEND_DIR))

os.environ["DATABASE_URL"] = f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}"
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


def _register_and_headers(email: str, username: str):
    r = client.post(
        "/v1/auth/register",
        json={
            "email": email,
            "password": "StrongPassword123!",
            "username": username,
            "display_name": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert r.status_code == 201, r.text
    data = r.json()
    return data, {"Authorization": f"Bearer {data['access_token']}"}


def test_auth_email_pkce_oauth_guest_link_and_account_deletion():
    assert client.get("/health").json()["status"] == "ok"
    assert client.get("/v1/legal/privacy-policy").json()["min_age"] == 16
    assert "Удаление аккаунта" in client.get("/v1/account/delete-form").text

    # 1. Age 16+ gate rejected if false
    r_underage = client.post(
        "/v1/auth/register",
        json={
            "email": "kid@example.com",
            "password": "StrongPassword123!",
            "username": "young_dev",
            "age_confirmed_16_plus": False,
        },
    )
    assert r_underage.status_code == 400

    # 2. Email registration, verification, login, refresh, password reset
    from app.email_sender import default_email_sender

    default_email_sender.outbox.clear()
    reg, headers = _register_and_headers("alice@example.com", "alice_dev")
    assert "email_verify_token_dev" not in reg
    verify_token = [m["token"] for m in default_email_sender.outbox if m["to"] == "alice@example.com" and m["kind"] == "verify"][-1]
    v = client.post(f"/v1/auth/verify-email?token={verify_token}")
    assert v.status_code == 200 and v.json()["verified"] is True

    login = client.post("/v1/auth/login", json={"email": "alice@example.com", "password": "StrongPassword123!"})
    assert login.status_code == 200
    ref = client.post("/v1/auth/refresh", json={"refresh_token": login.json()["refresh_token"]})
    assert ref.status_code == 200

    # 3. Server-side GitHub OAuth authorize -> callback -> one-time ticket exchange (BUG-01 fixed)
    from urllib.parse import parse_qs, urlparse

    auth_resp = client.get(
        "/v1/auth/oauth/github/authorize?redirect_uri=techenglish://auth/callback",
        follow_redirects=False,
    )
    assert auth_resp.status_code == 302
    cb_url = urlparse(auth_resp.headers["location"])
    cb_resp = client.get(f"{cb_url.path}?{cb_url.query}", follow_redirects=False)
    assert cb_resp.status_code == 302
    ticket = parse_qs(urlparse(cb_resp.headers["location"]).query)["ticket"][0]
    oauth_ok = client.post("/v1/auth/oauth/ticket/exchange", json={"ticket": ticket})
    assert oauth_ok.status_code == 200 and oauth_ok.json()["provider"] == "GITHUB"

    # Legacy client-asserted exchange endpoint is permanently disabled (410 Gone)
    verifier = "a" * 64
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii")).digest()).rstrip(b"=").decode("ascii")
    oauth_bad = client.post(
        "/v1/auth/oauth/github/exchange",
        json={
            "authorization_code": "gh_code_12345",
            "code_verifier": verifier,
            "code_challenge": challenge,
            "provider_email": "alice@example.com",
            "suggested_username": "gh_dev",
        },
    )
    assert oauth_bad.status_code == 410

    # 4. Guest -> account linking preserves SM-2, favorites, custom words, and events
    now = int(time.time() * 1000)
    link_res = client.post(
        "/v1/auth/guest/link",
        headers=headers,
        json={
            "events": [
                {
                    "event_id": str(uuid.uuid4()),
                    "event_type": "FLASHCARD_CORRECT",
                    "client_reported_xp": 10,
                    "client_timestamp_ms": now - 2000,
                    "session_duration_ms": 3000,
                    "is_correct": True,
                }
            ],
            "sm2_updates": [
                {
                    "normalized_term": "idempotency",
                    "repetitions": 3,
                    "ease_factor": 2.6,
                    "interval_days": 15,
                    "next_review_at_ms": now + 86_400_000,
                    "client_timestamp_ms": now - 1000,
                }
            ],
            "favorite_terms": ["idempotency"],
            "custom_words": {"singleflight": "объединение параллельных запросов"},
        },
    )
    assert link_res.status_code == 200
    assert link_res.json()["server_total_xp"] == 10

    state = client.get("/v1/sync/state", headers=headers).json()
    assert len(state["sm2_states"]) == 1
    assert "idempotency" in state["favorite_terms"]
    assert "singleflight" in state["custom_words"]

    # 5. Account deletion removes user and invalidates token
    del_res = client.delete("/v1/account", headers=headers)
    assert del_res.status_code == 200 and del_res.json()["deleted"] is True
    assert client.get("/v1/profile/me", headers=headers).status_code == 401


def test_sync_idempotency_errata_e1_sm2_conflicts_and_server_anticheat():
    _, headers = _register_and_headers("sync@example.com", "sync_dev")
    now = int(time.time() * 1000)
    ev_id = str(uuid.uuid4())

    # Send tampered XP (99999) + valid event_id
    r1 = client.post(
        "/v1/sync/events",
        headers=headers,
        json={
            "events": [
                {
                    "event_id": ev_id,
                    "event_type": "SCENARIO_CORRECT",
                    "client_reported_xp": 99999,
                    "client_timestamp_ms": now - 3000,
                    "session_duration_ms": 4000,
                    "is_correct": True,
                }
            ],
            "sm2_updates": [
                {
                    "normalized_term": "deadlock",
                    "repetitions": 2,
                    "ease_factor": 2.5,
                    "interval_days": 6,
                    "next_review_at_ms": now + 10_000,
                    "client_timestamp_ms": now - 5_000,
                }
            ],
        },
    )
    assert r1.status_code == 200
    d1 = r1.json()
    assert d1["awarded_xp"] == 20  # Canonical XP, NOT tampered 99999
    assert d1["server_total_xp"] == 20
    assert d1["tampering_flags"] == 1

    # Re-sending the exact same event_id MUST NOT change total XP (idempotency!)
    r2 = client.post(
        "/v1/sync/events",
        headers=headers,
        json={
            "events": [
                {
                    "event_id": ev_id,
                    "event_type": "SCENARIO_CORRECT",
                    "client_reported_xp": 20,
                    "client_timestamp_ms": now - 3000,
                    "session_duration_ms": 4000,
                    "is_correct": True,
                }
            ]
        },
    )
    assert r2.status_code == 200
    assert r2.json()["awarded_xp"] == 0
    assert r2.json()["server_total_xp"] == 20
    assert ev_id in r2.json()["duplicate_event_ids"]

    # Errata E1 conflict: older client_timestamp loses, newer client_timestamp wins
    r_older = client.post(
        "/v1/sync/events",
        headers=headers,
        json={
            "sm2_updates": [
                {
                    "normalized_term": "deadlock",
                    "repetitions": 9,
                    "ease_factor": 2.8,
                    "interval_days": 90,
                    "next_review_at_ms": now + 90_000,
                    "client_timestamp_ms": now - 60_000,  # older than existing now - 5000
                }
            ]
        },
    )
    assert r_older.json()["sm2_conflicts_kept_existing"] == 1

    r_newer = client.post(
        "/v1/sync/events",
        headers=headers,
        json={
            "sm2_updates": [
                {
                    "normalized_term": "deadlock",
                    "repetitions": 4,
                    "ease_factor": 2.6,
                    "interval_days": 15,
                    "next_review_at_ms": now + 50_000,
                    "client_timestamp_ms": now - 1_000,  # newer than existing now - 5000
                }
            ]
        },
    )
    assert r_newer.json()["sm2_updated"] == 1
    state = client.get("/v1/sync/state", headers=headers).json()
    assert state["sm2_states"][0]["repetitions"] == 4


def test_leaderboards_friends_ping_moderation_and_ota_packs():
    u1, h1 = _register_and_headers("dev1@example.com", "kotlin_one")
    u2, h2 = _register_and_headers("dev2@example.com", "python_two")
    now = int(time.time() * 1000)

    # Errata E2: reject arbitrary non-preset avatar URL and reserved username
    assert client.patch("/v1/profile/me", headers=h1, json={"avatar_key": "https://evil.site/pic.jpg"}).status_code == 422
    assert client.patch("/v1/profile/me", headers=h1, json={"username": "admin"}).status_code == 422
    assert client.patch("/v1/profile/me", headers=h1, json={"avatar_key": "preset_kotlin"}).status_code == 200

    # Award XP to u1 so u1 leads weekly leaderboard
    client.post(
        "/v1/sync/events",
        headers=h1,
        json={
            "events": [
                {
                    "event_id": str(uuid.uuid4()),
                    "event_type": "TIME_ATTACK_COMPLETE",
                    "client_reported_xp": 25,
                    "client_timestamp_ms": now - 1000,
                    "session_duration_ms": 60_000,
                }
            ]
        },
    )

    # Add friend, check feed & reactions, test Ping 1/day limit
    assert client.post("/v1/friends/add", headers=h1, json={"username": "python_two"}).status_code == 200
    assert len(client.get("/v1/friends", headers=h1).json()["friends"]) == 1
    feed = client.get("/v1/friends/feed", headers=h2).json()["items"]
    assert len(feed) >= 1
    react = client.post(f"/v1/friends/feed/{feed[0]['id']}/react", headers=h2)
    assert react.status_code == 200 and react.json()["reaction_count"] == 1

    # Ping succeeds once, second within 24h returns 429
    assert client.post(f"/v1/friends/{u2['user_id']}/ping", headers=h1).status_code == 200
    assert client.post(f"/v1/friends/{u2['user_id']}/ping", headers=h1).status_code == 429

    # Leaderboards across all 4 tabs (zero bots)
    for tab in ("weekly", "league", "friends", "tech_stack"):
        lb = client.get(f"/v1/leaderboards/{tab}", headers=h1).json()
        assert lb["bots_used"] is False
        assert lb["reset_timezone"] == "Asia/Dushanbe"
        assert lb["current_user_entry"]["username"] == "kotlin_one"

    # Moderation report + block hides user from leaderboard/search
    rep = client.post(
        "/v1/moderation/report",
        headers=h1,
        json={"target_type": "PROFILE", "target_id": u2["user_id"], "reason": "Spam profile link"},
    )
    assert rep.status_code == 201
    assert client.post(f"/v1/moderation/block/{u2['user_id']}", headers=h1).status_code == 200
    assert u2["user_id"] in client.get("/v1/moderation/blocks", headers=h1).json()["blocked_user_ids"]

    # OTA manifest & SHA-256 pack verification (ФТ-14.5)
    manifest = client.get("/v1/ota/manifest").json()
    assert len(manifest["packs"]) >= 1
    first_pack = manifest["packs"][0]
    assert first_pack["data_only"] is True
    pack_res = client.get(f"/v1/ota/packs/{first_pack['pack_id']}").json()
    computed = hashlib.sha256(pack_res["payload_json"].encode("utf-8")).hexdigest()
    assert computed == first_pack["sha256_hex"] == pack_res["sha256_hex"]
