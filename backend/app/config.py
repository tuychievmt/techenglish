from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    env: str = os.getenv("ENV", "dev").strip().lower()
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./techenglish_dev.db")
    redis_url: str = os.getenv("REDIS_URL", "")
    jwt_secret: str = os.getenv("JWT_SECRET", "dev-local-jwt-secret-key-min-32-bytes!!")
    jwt_algorithm: str = "HS256"
    access_token_ttl_seconds: int = int(os.getenv("ACCESS_TOKEN_TTL_SECONDS", "1800"))
    refresh_token_ttl_seconds: int = int(os.getenv("REFRESH_TOKEN_TTL_SECONDS", "2592000"))
    play_integrity_strict: bool = os.getenv("PLAY_INTEGRITY_STRICT", "false").lower() == "true"


settings = Settings()


WEAK_DEFAULT_JWT_SECRETS = {
    "dev-local-jwt-secret-key-min-32-bytes!!",
    "dev-only-replace-in-production-32-chars",
    "change_me",
    "change_me_in_prod",
    "secret",
}


def get_env() -> str:
    return os.getenv("ENV", settings.env).strip().lower()


def is_prod_env() -> bool:
    return get_env() in {"prod", "production"}


def is_staging_env() -> bool:
    return get_env() == "staging"


def is_deployed_env() -> bool:
    return is_prod_env() or is_staging_env()


def get_jwt_secret() -> str:
    return os.getenv("JWT_SECRET", settings.jwt_secret)


def is_weak_jwt_secret(secret: str) -> bool:
    s = (secret or "").strip()
    if len(s) < 32:
        return True
    lower = s.lower()
    if s in WEAK_DEFAULT_JWT_SECRETS or lower in WEAK_DEFAULT_JWT_SECRETS:
        return True
    if lower.startswith("dev-") or lower.startswith("change_me"):
        return True
    return False
