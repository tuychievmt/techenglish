from __future__ import annotations

import time
from datetime import datetime, timezone
from typing import Dict, List, Optional

import jwt
from fastapi import Depends, Header, HTTPException, status
from sqlalchemy.orm import Session

from app.models import SessionLocal, UserRow
from app.security import decode_jwt

MODERATOR_ROLES = {"MODERATOR", "ADMIN"}
ACTION_TOKEN_TTL_MS = 15 * 60 * 1000
MAX_TOKEN_ATTEMPTS = 5
AUTH_RATE_LIMIT_PREFIX = "auth_rl:"


class _RedisBackedRateBuckets(dict):
    """Compatibility wrapper so tests calling `_AUTH_RATE_BUCKETS.clear()` also clear Redis keys."""

    def clear(self) -> None:
        super().clear()
        try:
            from app.redis_client import get_redis_client

            r = get_redis_client()
            keys = list(r.keys(f"{AUTH_RATE_LIMIT_PREFIX}*"))
            if keys:
                r.delete(*keys)
        except Exception:
            pass


_AUTH_RATE_BUCKETS: Dict[str, List[int]] = _RedisBackedRateBuckets()


def now_ms() -> int:
    return int(time.time() * 1000)


def utc_day_str() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d")


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_current_user(
    authorization: Optional[str] = Header(default=None),
    db: Session = Depends(get_db),
) -> UserRow:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing Bearer token")
    token = authorization.split(" ", 1)[1].strip()
    try:
        payload = decode_jwt(token, expected_type="access")
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=f"Invalid token: {exc}")
    user = db.get(UserRow, payload["sub"])
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user


def require_moderator(user: UserRow = Depends(get_current_user)) -> UserRow:
    if (user.role or "USER").upper() not in MODERATOR_ROLES:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Moderator or Admin role required",
        )
    return user


def check_auth_rate_limit(
    bucket_key: str,
    max_calls: int = MAX_TOKEN_ATTEMPTS,
    window_ms: int = ACTION_TOKEN_TTL_MS,
) -> None:
    try:
        from app.redis_client import get_redis_client

        r = get_redis_client()
        redis_key = f"{AUTH_RATE_LIMIT_PREFIX}{bucket_key}"
        count = int(r.incr(redis_key))
        if count == 1:
            r.pexpire(redis_key, max(1000, int(window_ms)))
        if count > max_calls:
            raise HTTPException(status_code=429, detail="Too many attempts. Please try again later.")
        return
    except HTTPException:
        raise
    except Exception:
        pass

    now = now_ms()
    hits = [ts for ts in _AUTH_RATE_BUCKETS.get(bucket_key, []) if now - ts < window_ms]
    if len(hits) >= max_calls:
        _AUTH_RATE_BUCKETS[bucket_key] = hits
        raise HTTPException(status_code=429, detail="Too many attempts. Please try again later.")
    hits.append(now)
    _AUTH_RATE_BUCKETS[bucket_key] = hits
