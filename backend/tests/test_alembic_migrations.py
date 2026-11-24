from __future__ import annotations

import os
import sys
from pathlib import Path

import pytest
from sqlalchemy import create_engine, inspect, text

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

INITIAL_DATABASE_URL = os.environ.get("ALEMBIC_DATABASE_URL") or os.environ.get("DATABASE_URL", "")


def _assert_alembic_upgrade_matches_metadata(database_url: str) -> None:
    from alembic import command
    from alembic.autogenerate import compare_metadata
    from alembic.config import Config
    from alembic.runtime.migration import MigrationContext
    from app.models import Base

    alembic_ini = BACKEND_DIR / "alembic.ini"
    assert alembic_ini.exists(), f"Missing Alembic configuration: {alembic_ini}"

    cfg = Config(str(alembic_ini))
    cfg.set_main_option("sqlalchemy.url", database_url)

    engine = create_engine(
        database_url,
        connect_args={"check_same_thread": False} if database_url.startswith("sqlite") else {},
        future=True,
    )
    try:
        # Ensure clean database before running alembic upgrade head
        with engine.begin() as conn:
            if database_url.startswith("postgresql"):
                conn.execute(text("DROP SCHEMA public CASCADE"))
                conn.execute(text("CREATE SCHEMA public"))
            else:
                Base.metadata.drop_all(bind=conn)
                conn.execute(text("DROP TABLE IF EXISTS alembic_version"))

        command.upgrade(cfg, "head")

        with engine.connect() as conn:
            insp = inspect(conn)
            actual_tables = set(insp.get_table_names()) - {"alembic_version"}
            expected_tables = set(Base.metadata.tables.keys())
            assert actual_tables == expected_tables, (
                f"Table mismatch: missing={expected_tables - actual_tables}, "
                f"extra={actual_tables - expected_tables}"
            )

            mc = MigrationContext.configure(
                conn,
                opts={
                    "compare_type": True,
                    "compare_server_default": False,
                },
            )
            diffs = compare_metadata(mc, Base.metadata)
            assert diffs == [], f"Alembic schema differs from SQLAlchemy metadata: {diffs}"

        # Verify downgrade -> upgrade round-trip cleanly works
        command.downgrade(cfg, "base")
        with engine.connect() as conn:
            remaining = set(inspect(conn).get_table_names()) - {"alembic_version"}
            assert remaining == set(), f"Tables left behind after downgrade base: {remaining}"

        command.upgrade(cfg, "head")
    finally:
        engine.dispose()


def test_alembic_upgrade_head_matches_sqlalchemy_metadata_sqlite(tmp_path: Path) -> None:
    sqlite_url = f"sqlite:///{tmp_path / 'alembic_baseline.db'}"
    _assert_alembic_upgrade_matches_metadata(sqlite_url)


@pytest.mark.skipif(
    not INITIAL_DATABASE_URL.startswith("postgresql"),
    reason="PostgreSQL DATABASE_URL not configured in local environment",
)
def test_alembic_upgrade_head_matches_sqlalchemy_metadata_postgres() -> None:
    _assert_alembic_upgrade_matches_metadata(INITIAL_DATABASE_URL)


def test_env_prod_disables_create_all(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    from app import main as app_main
    from app import models as app_models

    monkeypatch.setenv("ENV", "prod")
    monkeypatch.setenv("JWT_SECRET", "prod-test-strong-secret-key-at-least-32-characters-long-9988")
    monkeypatch.setenv("SMTP_HOST", "smtp.example.com")
    monkeypatch.setenv("PLAY_INTEGRITY_STRICT", "true")
    monkeypatch.setenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", "TEST_CERT_SHA256")
    monkeypatch.setenv("PLAY_INTEGRITY_API_ACCESS_TOKEN", "test-access-token")

    test_engine = create_engine(
        f"sqlite:///{tmp_path / 'prod_no_create_all.db'}",
        connect_args={"check_same_thread": False},
        future=True,
    )
    called = {"count": 0}
    original_create_all = app_models.Base.metadata.create_all

    def spy_create_all(*args, **kwargs):
        called["count"] += 1
        return original_create_all(*args, **kwargs)

    monkeypatch.setattr(app_models.Base.metadata, "create_all", spy_create_all)
    try:
        app_models.init_db(target_engine=test_engine)
        app_main.on_startup()
        assert called["count"] == 0, "create_all() must NOT be called when ENV=prod"
        with test_engine.connect() as conn:
            assert inspect(conn).get_table_names() == []
    finally:
        test_engine.dispose()
