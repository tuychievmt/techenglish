from __future__ import annotations

import base64
import os
import sys
from pathlib import Path

import boto3
import pytest
from fastapi.testclient import TestClient
from moto import mock_aws

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
    return {"Authorization": f"Bearer {res.json()['access_token']}"}


def test_s3_voice_storage_lifecycle_presigned_get_limits_and_hard_delete_with_moto():
    from app.voice_storage import S3VoiceObjectStorage, get_voice_storage

    with mock_aws():
        s3_client = boto3.client("s3", region_name="us-east-1")
        storage = S3VoiceObjectStorage(
            bucket="techenglish-peer-voice",
            ttl_days=30,
            s3_client=s3_client,
        )
        storage.ensure_bucket_and_lifecycle()

        # Verify 30-day lifecycle expiration rule is configured on bucket
        lc = s3_client.get_bucket_lifecycle_configuration(Bucket="techenglish-peer-voice")
        assert any(
            r.get("Status") == "Enabled" and r.get("Expiration", {}).get("Days") == 30
            for r in lc["Rules"]
        )

        app.dependency_overrides[get_voice_storage] = lambda: storage
        try:
            hdr = _register("voice_s3@example.com", "voice_s3")

            # 1. Reject duration > 20s (422)
            small_b64 = base64.b64encode(b"OggS\x00fake-voice-clip-bytes").decode("ascii")
            too_long = client.post(
                "/v1/peer-review/submissions",
                headers=hdr,
                json={
                    "case_id": "case_s3_test",
                    "text_response": "We rolled back the migration to resolve lock contention.",
                    "voice_base64": small_b64,
                    "voice_duration_seconds": 21,
                    "voice_content_type": "audio/ogg",
                },
            )
            assert too_long.status_code == 422

            # 2. Reject payload > 512 KB (422)
            oversized_b64 = base64.b64encode(b"x" * (512 * 1024 + 1)).decode("ascii")
            too_big = client.post(
                "/v1/peer-review/submissions",
                headers=hdr,
                json={
                    "case_id": "case_s3_test",
                    "text_response": "We rolled back the migration to resolve lock contention.",
                    "voice_base64": oversized_b64,
                    "voice_duration_seconds": 10,
                    "voice_content_type": "audio/ogg",
                },
            )
            assert too_big.status_code == 422

            # 3. Valid clip uploads to S3 (moto), returns key and presigned GET URL
            ok = client.post(
                "/v1/peer-review/submissions",
                headers=hdr,
                json={
                    "case_id": "case_s3_test",
                    "text_response": "We rolled back the migration to resolve lock contention.",
                    "voice_base64": small_b64,
                    "voice_duration_seconds": 12,
                    "voice_content_type": "audio/ogg",
                },
            )
            assert ok.status_code == 201, ok.text
            sub_id = ok.json()["id"]
            key = ok.json()["voice_object_key"]
            assert storage.exists(key) is True

            # Presigned GET URL endpoint returns signed S3 URL
            signed = client.get(f"/v1/peer-review/submissions/{sub_id}/voice-url", headers=hdr)
            assert signed.status_code == 200, signed.text
            signed_url = signed.json()["voice_url"]
            assert "techenglish-peer-voice" in signed_url
            assert "X-Amz-Signature" in signed_url or "Signature=" in signed_url

            # 4. DELETE /v1/account physically deletes object from S3
            del_resp = client.delete("/v1/account", headers=hdr)
            assert del_resp.status_code == 200
            assert del_resp.json()["deleted_voice_clips"] == 1
            assert storage.exists(key) is False
        finally:
            app.dependency_overrides.pop(get_voice_storage, None)
