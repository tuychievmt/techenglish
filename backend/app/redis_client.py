from __future__ import annotations

import json
import os
import secrets
from typing import Any, Optional

import redis

from app.config import is_deployed_env, settings
from app.security import hash_action_token

WS_TICKET_TTL_SECONDS = int(os.getenv("WS_TICKET_TTL_SECONDS", "30"))

_redis_instance: Optional[Any] = None
_redis_url_cached: str = ""
_redis_is_fake: bool = False


def reset_redis_client() -> None:
    global _redis_instance, _redis_url_cached, _redis_is_fake
    _redis_instance = None
    _redis_url_cached = ""
    _redis_is_fake = False


def is_fake_redis() -> bool:
    return _redis_is_fake


def get_redis_client() -> Any:
    """
    Returns a Redis client configured via REDIS_URL.
    In local dev/tests without REDIS_URL, transparently uses fakeredis.FakeRedis.
    In ENV=prod, REDIS_URL is mandatory.
    """
    global _redis_instance, _redis_url_cached, _redis_is_fake
    url = os.getenv("REDIS_URL", settings.redis_url).strip()
    if _redis_instance is not None and _redis_url_cached == url:
        return _redis_instance

    if url:
        _redis_instance = redis.Redis.from_url(
            url,
            decode_responses=True,
            health_check_interval=30,
            socket_keepalive=True,
            retry_on_timeout=True,
        )
        _redis_url_cached = url
        _redis_is_fake = False
        return _redis_instance

    if is_deployed_env():
        raise RuntimeError("REDIS_URL must be configured in production/staging (fake Redis is forbidden).")

    import fakeredis

    _redis_instance = fakeredis.FakeRedis(decode_responses=True)
    _redis_url_cached = ""
    _redis_is_fake = True
    return _redis_instance


def issue_ws_ticket(
    redis_client: Any,
    user_id: str,
    scope: str = "ws",
    ttl_seconds: int = WS_TICKET_TTL_SECONDS,
) -> str:
    raw_ticket = secrets.token_urlsafe(32)
    key = f"ws_ticket:{hash_action_token(raw_ticket)}"
    payload = json.dumps({"user_id": user_id, "scope": scope})
    redis_client.set(key, payload, ex=max(1, int(ttl_seconds)))
    return raw_ticket


def consume_ws_ticket(
    redis_client: Any,
    raw_ticket: str,
    expected_scope: Optional[str] = None,
) -> Optional[str]:
    if not raw_ticket or not raw_ticket.strip():
        return None
    key = f"ws_ticket:{hash_action_token(raw_ticket.strip())}"
    if hasattr(redis_client, "getdel"):
        raw_val = redis_client.getdel(key)
    else:
        pipe = redis_client.pipeline()
        pipe.get(key)
        pipe.delete(key)
        raw_val, _ = pipe.execute()
    if not raw_val:
        return None
    try:
        data = json.loads(raw_val)
    except Exception:
        return None
    scope = data.get("scope", "ws")
    if expected_scope and scope != "ws" and scope != expected_scope:
        return None
    user_id = data.get("user_id")
    return str(user_id) if user_id else None
