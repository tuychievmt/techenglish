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
    public_base_url: str = os.getenv("PUBLIC_BASE_URL", "").strip().rstrip("/")
    cors_allow_origins: str = os.getenv("CORS_ALLOW_ORIGINS", "https://techenglish-trwk.onrender.com")
    github_client_id: str = os.getenv("GITHUB_CLIENT_ID", "").strip()
    github_client_secret: str = os.getenv("GITHUB_CLIENT_SECRET", "").strip()
    google_client_id: str = os.getenv("GOOGLE_CLIENT_ID", "").strip()
    google_client_secret: str = os.getenv("GOOGLE_CLIENT_SECRET", "").strip()


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


def get_public_base_url() -> str:
    return os.getenv("PUBLIC_BASE_URL", settings.public_base_url).strip().rstrip("/")


def get_cors_origins() -> list[str]:
    raw = os.getenv("CORS_ALLOW_ORIGINS", settings.cors_allow_origins).strip()
    origins = [item.strip().rstrip("/") for item in raw.split(",") if item.strip()]
    if not is_deployed_env():
        for local_origin in ("http://localhost:8000", "http://127.0.0.1:8000", "http://localhost:3000"):
            if local_origin not in origins:
                origins.append(local_origin)
    return origins or ["https://techenglish-trwk.onrender.com"]


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
