from __future__ import annotations

import hashlib
import os
import sys
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import app  # noqa: E402
from app.models import Base, SessionLocal, UserRow, engine  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    from app.email_sender import default_email_sender
    from app.main import _AUTH_RATE_BUCKETS

    _AUTH_RATE_BUCKETS.clear()
    default_email_sender.outbox.clear()
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    _AUTH_RATE_BUCKETS.clear()
    default_email_sender.outbox.clear()
    Base.metadata.drop_all(bind=engine)


client = TestClient(app)


def test_register_and_reset_do_not_leak_tokens_in_http_and_store_only_hashes():
    reg = client.post(
        "/v1/auth/register",
        json={
            "email": "sec_alice@example.com",
            "password": "StrongPassword123!",
            "username": "sec_alice",
            "display_name": "Sec Alice",
            "age_confirmed_16_plus": True,
        },
    )
    assert reg.status_code == 201, reg.text
    reg_body = reg.json()
    assert "email_verify_token_dev" not in reg_body
    assert "email_verify_token" not in reg_body

    from app.email_sender import default_email_sender

    verify_msgs = [m for m in default_email_sender.outbox if m["to"] == "sec_alice@example.com" and m["kind"] == "verify"]
    assert len(verify_msgs) == 1
    raw_verify_token = verify_msgs[0]["token"]

    # Stored in DB as SHA-256 hash, never raw token
    with SessionLocal() as db:
        user = db.query(UserRow).filter_by(email="sec_alice@example.com").one()
        assert user.email_verify_token != raw_verify_token
        assert user.email_verify_token == hashlib.sha256(raw_verify_token.encode("utf-8")).hexdigest()

    # Single-use verification works once and fails on replay
    ok = client.post(f"/v1/auth/verify-email?token={raw_verify_token}")
    assert ok.status_code == 200 and ok.json()["verified"] is True
    replay = client.post(f"/v1/auth/verify-email?token={raw_verify_token}")
    assert replay.status_code == 400

    # Password reset request returns identical response for existing and non-existing accounts without token leak
    r_missing = client.post("/v1/auth/password-reset/request?email=nobody@example.com")
    r_exists = client.post("/v1/auth/password-reset/request?email=sec_alice@example.com")
    assert r_missing.status_code == 200
    assert r_exists.status_code == 200
    assert r_missing.json() == r_exists.json() == {"sent": True}
    assert "reset_token_dev" not in r_exists.json()

    reset_msgs = [m for m in default_email_sender.outbox if m["to"] == "sec_alice@example.com" and m["kind"] == "reset"]
    assert len(reset_msgs) == 1
    raw_reset_token = reset_msgs[0]["token"]

    with SessionLocal() as db:
        user = db.query(UserRow).filter_by(email="sec_alice@example.com").one()
        assert user.password_reset_token != raw_reset_token
        assert user.password_reset_token == hashlib.sha256(raw_reset_token.encode("utf-8")).hexdigest()

    # Reset works once and fails on replay
    confirm = client.post(
        "/v1/auth/password-reset/confirm",
        json={
            "email": "sec_alice@example.com",
            "reset_token": raw_reset_token,
            "new_password": "NewStrongPassword456!",
        },
    )
    assert confirm.status_code == 200 and confirm.json()["reset"] is True
    replay_reset = client.post(
        "/v1/auth/password-reset/confirm",
        json={
            "email": "sec_alice@example.com",
            "reset_token": raw_reset_token,
            "new_password": "AnotherPassword789!",
        },
    )
    assert replay_reset.status_code == 400


def test_tokens_expire_after_15_minutes_and_attempts_are_rate_limited():
    from app.email_sender import default_email_sender

    default_email_sender.outbox.clear()
    reg = client.post(
        "/v1/auth/register",
        json={
            "email": "ttl_bob@example.com",
            "password": "StrongPassword123!",
            "username": "ttl_bob",
            "age_confirmed_16_plus": True,
        },
    )
    assert reg.status_code == 201
    verify_token = default_email_sender.outbox[-1]["token"]

    # Force expiry > 15 min ago
    with SessionLocal() as db:
        user = db.query(UserRow).filter_by(email="ttl_bob@example.com").one()
        user.email_verify_expires_at_ms = int(time.time() * 1000) - 1000
        db.commit()

    expired_v = client.post(f"/v1/auth/verify-email?token={verify_token}")
    assert expired_v.status_code == 400

    # Password reset TTL expiry
    default_email_sender.outbox.clear()
    client.post("/v1/auth/password-reset/request?email=ttl_bob@example.com")
    reset_token = default_email_sender.outbox[-1]["token"]
    with SessionLocal() as db:
        user = db.query(UserRow).filter_by(email="ttl_bob@example.com").one()
        user.password_reset_expires_at_ms = int(time.time() * 1000) - 1000
        db.commit()
    expired_r = client.post(
        "/v1/auth/password-reset/confirm",
        json={"email": "ttl_bob@example.com", "reset_token": reset_token, "new_password": "NewPassword123!"},
    )
    assert expired_r.status_code == 400

    # Brute-force attempt limit on password reset confirm
    client.post("/v1/auth/password-reset/request?email=ttl_bob@example.com")
    for _ in range(5):
        resp = client.post(
            "/v1/auth/password-reset/confirm",
            json={"email": "ttl_bob@example.com", "reset_token": "wrong-token", "new_password": "NewPassword123!"},
        )
        assert resp.status_code == 400
    locked = client.post(
        "/v1/auth/password-reset/confirm",
        json={"email": "ttl_bob@example.com", "reset_token": "wrong-token", "new_password": "NewPassword123!"},
    )
    assert locked.status_code == 429


def test_prod_startup_fails_with_weak_or_default_jwt_secret_or_fake_email_sender(monkeypatch: pytest.MonkeyPatch):
    from app.main import validate_production_startup_config

    monkeypatch.setenv("ENV", "prod")

    # 1. Default JWT secret -> fail
    monkeypatch.setenv("JWT_SECRET", "dev-local-jwt-secret-key-min-32-bytes!!")
    monkeypatch.setenv("SMTP_HOST", "smtp.example.com")
    with pytest.raises(RuntimeError, match="JWT_SECRET"):
        validate_production_startup_config()

    # 2. Short JWT secret (< 32 chars) -> fail
    monkeypatch.setenv("JWT_SECRET", "too-short-secret")
    with pytest.raises(RuntimeError, match="JWT_SECRET"):
        validate_production_startup_config()

    # 3. Strong JWT secret, but fake EmailSender (no SMTP_HOST) -> fail
    monkeypatch.setenv("JWT_SECRET", "a9f8c7e6d5b4a39281706f5e4d3c2b1a0987654321fedcba")
    monkeypatch.delenv("SMTP_HOST", raising=False)
    with pytest.raises(RuntimeError, match="EmailSender"):
        validate_production_startup_config()

    # 4. Strong JWT secret + real SMTP_HOST + Play Integrity -> pass
    monkeypatch.setenv("SMTP_HOST", "smtp.example.com")
    monkeypatch.setenv("PLAY_INTEGRITY_STRICT", "true")
    monkeypatch.setenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", "TEST_CERT_SHA256")
    monkeypatch.setenv("PLAY_INTEGRITY_API_ACCESS_TOKEN", "test-access-token")
    validate_production_startup_config()
