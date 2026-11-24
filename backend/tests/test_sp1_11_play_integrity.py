from __future__ import annotations

import os
import sys
import time
from pathlib import Path
from typing import Any, Dict

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")

from app.main import app  # noqa: E402
from app.models import Base, engine  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


client = TestClient(app)


def test_random_32_char_string_is_rejected_in_strict_mode(monkeypatch: pytest.MonkeyPatch):
    # BUG-07 reproduction: previously any 32+ char string passed verify_play_integrity_token!
    monkeypatch.setenv("PLAY_INTEGRITY_STRICT", "true")
    from app.integrity import GooglePlayIntegrityVerifier

    def fail_decode(token: str) -> Dict[str, Any]:
        raise ValueError("Invalid signed Play Integrity JWS token")

    verifier = GooglePlayIntegrityVerifier(
        package_name="com.techenglish.app",
        allowed_cert_digests={"CERT_DIGEST_BASE64URL_VALID"},
        decode_fn=fail_decode,
    )
    res = verifier.verify("a" * 40, expected_nonce="nonce-123")
    assert res.is_valid is False


def test_verdict_parsing_validates_nonce_package_cert_device_and_freshness():
    from app.integrity import GooglePlayIntegrityVerifier

    now_ms = int(time.time() * 1000)

    def make_verdict(**overrides) -> Dict[str, Any]:
        payload: Dict[str, Any] = {
            "tokenPayloadExternal": {
                "requestDetails": {
                    "requestPackageName": overrides.get("requestPackageName", "com.techenglish.app"),
                    "timestampMillis": str(overrides.get("timestampMillis", now_ms)),
                    "nonce": overrides.get("nonce", "expected-nonce-xyz"),
                },
                "appIntegrity": {
                    "appRecognitionVerdict": overrides.get("appRecognitionVerdict", "PLAY_RECOGNIZED"),
                    "packageName": overrides.get("appPackageName", "com.techenglish.app"),
                    "certificateSha256Digest": overrides.get("certs", ["CERT_SHA256_PROD_OK"]),
                },
                "deviceIntegrity": {
                    "deviceRecognitionVerdict": overrides.get("deviceVerdict", ["MEETS_DEVICE_INTEGRITY"]),
                },
                "accountDetails": {
                    "appLicensingVerdict": overrides.get("licensing", "LICENSED"),
                },
            }
        }
        return payload

    def verify_with(verdict: Dict[str, Any], nonce: str = "expected-nonce-xyz"):
        v = GooglePlayIntegrityVerifier(
            package_name="com.techenglish.app",
            allowed_cert_digests={"CERT_SHA256_PROD_OK"},
            decode_fn=lambda tok: verdict,
        )
        return v.verify("valid.jws.token", expected_nonce=nonce)

    # Valid verdict passes
    ok = verify_with(make_verdict())
    assert ok.is_valid is True

    # Wrong nonce fails
    bad_nonce = verify_with(make_verdict(nonce="attacker-nonce"))
    assert bad_nonce.is_valid is False and "nonce" in bad_nonce.reason

    # Wrong package fails
    bad_pkg = verify_with(make_verdict(requestPackageName="com.evil.clone"))
    assert bad_pkg.is_valid is False and "package" in bad_pkg.reason

    # Wrong signing certificate fails
    bad_cert = verify_with(make_verdict(certs=["RESIGNED_FAKE_CERT"]))
    assert bad_cert.is_valid is False and "certificate" in bad_cert.reason

    # Unrecognized app fails
    bad_app = verify_with(make_verdict(appRecognitionVerdict="UNRECOGNIZED_VERSION"))
    assert bad_app.is_valid is False

    # Rooted/untrusted device fails
    bad_dev = verify_with(make_verdict(deviceVerdict=[]))
    assert bad_dev.is_valid is False

    # Expired token (> 5 min old) fails
    stale = verify_with(make_verdict(timestampMillis=now_ms - 600_000))
    assert stale.is_valid is False and "expired" in stale.reason


def test_prod_env_forbids_fake_integrity_verifier_and_sync_endpoint_enforces_verifier(monkeypatch: pytest.MonkeyPatch):
    from app.integrity import DevAttestationStub, IntegrityVerificationResult, get_integrity_verifier
    from app.main import validate_production_startup_config

    # 1. ENV=prod rejects fake verifier / unconfigured Play Integrity
    monkeypatch.setenv("ENV", "prod")
    monkeypatch.setenv("JWT_SECRET", "prod-strong-jwt-secret-key-at-least-32-chars-ok")
    monkeypatch.setenv("SMTP_HOST", "smtp.example.com")
    monkeypatch.setenv("PLAY_INTEGRITY_STRICT", "false")
    monkeypatch.delenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", raising=False)
    with pytest.raises(RuntimeError, match="Play Integrity"):
        validate_production_startup_config()

    # 2. ENV=prod with strict real Play Integrity config passes
    monkeypatch.setenv("PLAY_INTEGRITY_STRICT", "true")
    monkeypatch.setenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", "CERT_SHA256_PROD_OK")
    monkeypatch.setenv("PLAY_INTEGRITY_API_ACCESS_TOKEN", "oauth2-access-token")
    validate_production_startup_config()
    assert get_integrity_verifier().is_fake is False

    # 3. Sync batch endpoint enforces injected ClientIntegrityVerifier
    monkeypatch.setenv("ENV", "test")
    monkeypatch.delenv("SMTP_HOST", raising=False)
    reg = client.post(
        "/v1/auth/register",
        json={
            "email": "integ@example.com",
            "password": "StrongPassword123!",
            "username": "integ_user",
            "age_confirmed_16_plus": True,
        },
    )
    assert reg.status_code == 201
    hdr = {"Authorization": f"Bearer {reg.json()['access_token']}"}

    class RejectingVerifier:
        is_fake = False

        def verify(self, token, expected_nonce=None):
            return IntegrityVerificationResult(is_valid=False, reason="invalid_signature")

    app.dependency_overrides[get_integrity_verifier] = lambda: RejectingVerifier()
    try:
        r = client.post(
            "/v1/sync/events",
            headers=hdr,
            json={"events": [], "play_integrity_token": "x" * 64},
        )
        assert r.status_code == 403
    finally:
        app.dependency_overrides.pop(get_integrity_verifier, None)
