from __future__ import annotations

import sys
from pathlib import Path

BACKEND_DIR = Path(__file__).resolve().parents[1]
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))


def test_dockerfile_configured_for_render_free_proxy_and_port():
    dockerfile = (BACKEND_DIR / "Dockerfile").read_text(encoding="utf-8")
    assert "${PORT:-8000}" in dockerfile
    assert "--workers 1" in dockerfile
    assert "--proxy-headers" in dockerfile
    assert "--forwarded-allow-ips" in dockerfile
    assert "--limit-concurrency 64" in dockerfile


def test_models_normalizes_postgres_url_and_sets_pool_options():
    from app.models import build_engine_kwargs, normalize_database_url

    assert normalize_database_url("postgres://user:pass@ep.neon.tech/db") == "postgresql+psycopg://user:pass@ep.neon.tech/db"
    assert normalize_database_url("postgresql://user:pass@ep.neon.tech/db") == "postgresql+psycopg://user:pass@ep.neon.tech/db"
    assert normalize_database_url("postgresql+psycopg://user:pass@ep.neon.tech/db") == "postgresql+psycopg://user:pass@ep.neon.tech/db"
    assert normalize_database_url("sqlite:///./dev.db") == "sqlite:///./dev.db"

    pg_kwargs = build_engine_kwargs("postgresql://user:pass@ep.neon.tech/db")
    assert pg_kwargs["pool_pre_ping"] is True
    assert pg_kwargs["pool_size"] == 3
    assert pg_kwargs["max_overflow"] == 4
    assert pg_kwargs["pool_recycle"] == 280

    sqlite_kwargs = build_engine_kwargs("sqlite:///./dev.db")
    assert sqlite_kwargs["connect_args"] == {"check_same_thread": False}
    assert "pool_size" not in sqlite_kwargs


def test_alembic_env_normalizes_database_url():
    env_text = (BACKEND_DIR / "alembic" / "env.py").read_text(encoding="utf-8")
    assert "normalize_database_url" in env_text
