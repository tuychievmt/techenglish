from __future__ import annotations

import os
import sys
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import app  # noqa: E402
from app.models import Base, SessionLocal, Sm2StateRow, SyncEventRow, UserRow, engine  # noqa: E402


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
            "display_name": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201, res.text
    data = res.json()
    return data, {"Authorization": f"Bearer {data['access_token']}"}


def _make_crowdsource_eligible(user_id: str) -> None:
    now = int(time.time() * 1000)
    with SessionLocal() as db:
        u = db.get(UserRow, user_id)
        assert u is not None
        u.created_at_ms = now - 20 * 86_400_000
        u.total_xp = 2000
        u.streak_days = 10
        for i in range(105):
            db.add(
                Sm2StateRow(
                    user_id=user_id,
                    normalized_term=f"term_{i}",
                    repetitions=3,
                    ease_factor=2.5,
                    interval_days=10,
                    next_review_at_ms=now + 86_400_000,
                    client_timestamp_ms=now,
                    server_received_ms=now,
                )
            )
        db.commit()


def test_moderation_queue_requires_moderator_or_admin_role():
    user_data, user_hdr = _register("regular@example.com", "regular_dev")
    mod_data, mod_hdr = _register("mod@example.com", "mod_dev")

    # Regular USER receives 403
    r_user = client.get("/v1/moderation/queue", headers=user_hdr)
    assert r_user.status_code == 403, f"Expected 403 for USER, got {r_user.status_code}"

    # Promote via CLI helper (not API)
    from app.cli_admin import promote_user_role

    with SessionLocal() as db:
        promote_user_role(db, "mod@example.com", "MODERATOR")

    # MODERATOR receives 200
    r_mod = client.get("/v1/moderation/queue", headers=mod_hdr)
    assert r_mod.status_code == 200, r_mod.text


def test_pr_approve_requires_moderator_and_blocks_self_approval():
    author_data, author_hdr = _register("author@example.com", "pr_author")
    other_user, other_hdr = _register("other@example.com", "other_dev")
    mod_data, mod_hdr = _register("reviewer@example.com", "pr_mod")

    _make_crowdsource_eligible(author_data["user_id"])

    pr_resp = client.post(
        "/v1/crowdsource/prs",
        headers=author_hdr,
        json={
            "term": "Backpressure",
            "proposed_translation_ru": "Обратное давление",
            "proposed_translation_tg": "",
            "proposed_definition_ru": "Механизм регулирования скорости между производителем и потребителем.",
            "source_reference": "https://reactive-streams.org",
            "license_accepted": True,
        },
    )
    assert pr_resp.status_code == 201, pr_resp.text
    pr_id = pr_resp.json()["id"]

    # Regular user cannot approve -> 403
    r_regular = client.post(f"/v1/crowdsource/prs/{pr_id}/approve", headers=other_hdr)
    assert r_regular.status_code == 403

    # Even if author is promoted to ADMIN, self-approval is strictly forbidden -> 403
    from app.cli_admin import promote_user_role

    with SessionLocal() as db:
        promote_user_role(db, "author@example.com", "ADMIN")
        promote_user_role(db, "reviewer@example.com", "MODERATOR")

    r_self = client.post(f"/v1/crowdsource/prs/{pr_id}/approve", headers=author_hdr)
    assert r_self.status_code == 403

    # Independent moderator approves -> 200
    r_ok = client.post(f"/v1/crowdsource/prs/{pr_id}/approve", headers=mod_hdr)
    assert r_ok.status_code == 200 and r_ok.json()["status"] == "APPROVED"


def test_bootstrap_first_admin_via_env_var(monkeypatch: pytest.MonkeyPatch):
    user_data, user_hdr = _register("firstadmin@example.com", "first_admin")
    assert client.get("/v1/moderation/queue", headers=user_hdr).status_code == 403

    from app.cli_admin import bootstrap_admin_from_env

    monkeypatch.setenv("BOOTSTRAP_ADMIN_EMAIL", "firstadmin@example.com")
    with SessionLocal() as db:
        admin = bootstrap_admin_from_env(db)
        assert admin is not None and admin.role == "ADMIN"

    assert client.get("/v1/moderation/queue", headers=user_hdr).status_code == 200
