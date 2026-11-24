from __future__ import annotations

import base64
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest
from botocore.exceptions import ClientError
from fastapi import HTTPException

BACKEND_DIR = Path(__file__).resolve().parents[1]
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))


def test_redis_from_url_passes_keepalive_and_healthcheck(monkeypatch):
    from app import redis_client

    redis_client.reset_redis_client()
    monkeypatch.setenv("REDIS_URL", "rediss://default:secret@upstash.example.com:6379")
    captured = {}

    def _fake_from_url(url, **kwargs):
        captured["url"] = url
        captured["kwargs"] = kwargs
        return MagicMock()

    with patch.object(redis_client.redis.Redis, "from_url", side_effect=_fake_from_url):
        redis_client.get_redis_client()

    redis_client.reset_redis_client()
    assert captured["url"] == "rediss://default:secret@upstash.example.com:6379"
    assert captured["kwargs"].get("health_check_interval") == 30
    assert captured["kwargs"].get("socket_keepalive") is True
    assert captured["kwargs"].get("retry_on_timeout") is True


def test_oauth_flow_store_shares_state_and_tickets_across_instances_via_redis(monkeypatch):
    from app import redis_client
    from app.oauth_provider import OAuthFlowStore

    monkeypatch.delenv("REDIS_URL", raising=False)
    redis_client.reset_redis_client()

    store_a = OAuthFlowStore()
    store_b = OAuthFlowStore()

    state, verifier, _ = store_a.create_state("GITHUB", "techenglish://auth/callback", "cs-1")
    consumed = store_b.consume_state(state, "GITHUB")
    assert consumed is not None
    assert consumed["code_verifier"] == verifier
    assert store_a.consume_state(state, "GITHUB") is None

    ticket = store_a.issue_ticket("user-rel05")
    assert store_b.consume_ticket(ticket) == "user-rel05"
    assert store_a.consume_ticket(ticket) is None


def test_auth_rate_limit_persists_in_redis_and_blocks_excess(monkeypatch):
    from app import deps, redis_client

    monkeypatch.delenv("REDIS_URL", raising=False)
    redis_client.reset_redis_client()
    deps._AUTH_RATE_BUCKETS.clear()

    r = redis_client.get_redis_client()
    deps.check_auth_rate_limit("rel05-bucket", max_calls=2, window_ms=60_000)
    deps.check_auth_rate_limit("rel05-bucket", max_calls=2, window_ms=60_000)

    redis_keys = list(r.keys("auth_rl:*"))
    assert len(redis_keys) >= 1, "Auth rate limit must be stored in Redis"

    with pytest.raises(HTTPException) as exc:
        deps.check_auth_rate_limit("rel05-bucket", max_calls=2, window_ms=60_000)
    assert exc.value.status_code == 429
    deps._AUTH_RATE_BUCKETS.clear()


def test_s3_voice_storage_skip_bucket_init_and_soft_lifecycle_catch(monkeypatch):
    from app.voice_storage import S3VoiceObjectStorage

    fake_s3 = MagicMock()
    monkeypatch.setenv("VOICE_STORAGE_SKIP_BUCKET_INIT", "true")
    storage_skip = S3VoiceObjectStorage(bucket="r2-bucket", s3_client=fake_s3)
    payload_b64 = base64.b64encode(b"fake-m4a-audio-bytes").decode("ascii")
    key = storage_skip.put_voice_clip("u1", payload_b64, "audio/mp4")
    assert key.startswith("u1/")
    fake_s3.head_bucket.assert_not_called()
    fake_s3.create_bucket.assert_not_called()
    fake_s3.put_bucket_lifecycle_configuration.assert_not_called()

    # Now verify soft catch when lifecycle API is unsupported by provider (e.g. R2/B2)
    fake_s3_2 = MagicMock()
    fake_s3_2.put_bucket_lifecycle_configuration.side_effect = ClientError(
        {"Error": {"Code": "NotImplemented", "Message": "Lifecycle API unsupported"}},
        "PutBucketLifecycleConfiguration",
    )
    monkeypatch.setenv("VOICE_STORAGE_SKIP_BUCKET_INIT", "false")
    storage_soft = S3VoiceObjectStorage(bucket="b2-bucket", s3_client=fake_s3_2)
    key2 = storage_soft.put_voice_clip("u2", payload_b64, "audio/mp4")
    assert key2.startswith("u2/")
    fake_s3_2.put_object.assert_called_once()
