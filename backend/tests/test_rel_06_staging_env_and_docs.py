from __future__ import annotations

import sys
from pathlib import Path

import pytest

BACKEND_DIR = Path(__file__).resolve().parents[1]
ROOT_DIR = BACKEND_DIR.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))


def test_staging_env_enforces_jwt_smtp_redis_without_play_integrity(monkeypatch):
    from app import config, redis_client
    from app.services.startup import validate_production_startup_config

    monkeypatch.setenv("ENV", "staging")
    assert config.is_staging_env() is True
    assert config.is_prod_env() is False

    # Weak JWT fails in staging
    monkeypatch.setenv("JWT_SECRET", "dev-local-jwt-secret-key-min-32-bytes!!")
    monkeypatch.setenv("SMTP_HOST", "smtp-relay.brevo.com")
    monkeypatch.setenv("REDIS_URL", "rediss://default:pass@upstash.example.com:6379")
    monkeypatch.delenv("PLAY_INTEGRITY_STRICT", raising=False)
    with pytest.raises(RuntimeError, match="JWT_SECRET"):
        validate_production_startup_config()

    # Fake email fails in staging
    monkeypatch.setenv("JWT_SECRET", "s" * 40)
    monkeypatch.delenv("SMTP_HOST", raising=False)
    with pytest.raises(RuntimeError, match="EmailSender"):
        validate_production_startup_config()

    # Missing REDIS_URL fails in staging
    monkeypatch.setenv("SMTP_HOST", "smtp-relay.brevo.com")
    monkeypatch.delenv("REDIS_URL", raising=False)
    redis_client.reset_redis_client()
    with pytest.raises(RuntimeError, match="REDIS_URL"):
        redis_client.get_redis_client()

    # Valid staging config passes WITHOUT Play Integrity
    monkeypatch.setenv("REDIS_URL", "rediss://default:pass@upstash.example.com:6379")
    validate_production_startup_config()
    redis_client.reset_redis_client()


def test_prod_env_checks_voice_storage_endpoint_when_s3_enabled(monkeypatch):
    from app.services.startup import validate_production_startup_config

    monkeypatch.setenv("ENV", "prod")
    monkeypatch.setenv("JWT_SECRET", "p" * 40)
    monkeypatch.setenv("SMTP_HOST", "smtp.example.com")
    monkeypatch.setenv("PLAY_INTEGRITY_STRICT", "true")
    monkeypatch.setenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", "AA:BB:CC")
    monkeypatch.setenv("PLAY_INTEGRITY_API_ACCESS_TOKEN", "ya29.token")
    monkeypatch.setenv("VOICE_STORAGE_BACKEND", "s3")
    monkeypatch.delenv("VOICE_STORAGE_ENDPOINT", raising=False)

    with pytest.raises(RuntimeError, match="VOICE_STORAGE_ENDPOINT"):
        validate_production_startup_config()

    monkeypatch.setenv("VOICE_STORAGE_ENDPOINT", "https://account.r2.cloudflarestorage.com")
    validate_production_startup_config()


def test_owner_actions_synced_with_real_env_var_names():
    doc = (ROOT_DIR / "docs" / "OWNER_ACTIONS.md").read_text(encoding="utf-8")
    assert "VOICE_STORAGE_ENDPOINT" in doc
    assert "VOICE_STORAGE_SKIP_BUCKET_INIT" in doc
    assert "PLAY_INTEGRITY_CERT_SHA256_DIGESTS" in doc
    assert "PLAY_INTEGRITY_API_ACCESS_TOKEN" in doc
    assert "ENV=staging" in doc
