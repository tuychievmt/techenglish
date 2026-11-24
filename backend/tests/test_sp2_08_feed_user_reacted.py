from __future__ import annotations

import os
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.main import app  # noqa: E402
from app.models import ActivityFeedRow, Base, SessionLocal, engine  # noqa: E402


@pytest.fixture(autouse=True)
def reset_database():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


def _register(client: TestClient, email: str, username: str):
    res = client.post(
        "/v1/auth/register",
        json={
            "email": email,
            "password": "StrongPassword123!",
            "username": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201, res.text
    d = res.json()
    return d["user_id"], {"Authorization": f"Bearer {d['access_token']}"}


def test_activity_feed_returns_per_user_reacted_state_and_prevents_duplicate_reactions():
    client = TestClient(app)
    uid_a, hdr_a = _register(client, "feed_a@example.com", "feed_alice")
    uid_b, hdr_b = _register(client, "feed_b@example.com", "feed_bob")

    assert client.post("/v1/friends/add", headers=hdr_a, json={"username": "feed_bob"}).status_code == 200

    with SessionLocal() as db:
        db.add(
            ActivityFeedRow(
                id="evt-feed-1",
                actor_user_id=uid_b,
                summary_ru="@feed_bob завершил 20 карточек SM-2!",
                reaction_count=0,
                created_at_ms=1_800_000_000_000,
            )
        )
        db.commit()

    # Before reacting: user_reacted must be present and False
    before = client.get("/v1/friends/feed", headers=hdr_a)
    assert before.status_code == 200
    items_before = before.json()["items"]
    assert len(items_before) == 1
    assert "user_reacted" in items_before[0], "Feed item missing user_reacted field (BUG-25)"
    assert items_before[0]["user_reacted"] is False

    # React once
    r1 = client.post("/v1/friends/feed/evt-feed-1/react", headers=hdr_a)
    assert r1.status_code == 200
    assert r1.json()["reaction_count"] == 1
    assert r1.json()["user_reacted"] is True

    # React again (idempotent -> count remains 1)
    r2 = client.post("/v1/friends/feed/evt-feed-1/react", headers=hdr_a)
    assert r2.status_code == 200
    assert r2.json()["reaction_count"] == 1

    # Feed for Alice shows user_reacted=True; feed for Bob shows user_reacted=False
    after_a = client.get("/v1/friends/feed", headers=hdr_a).json()["items"][0]
    after_b = client.get("/v1/friends/feed", headers=hdr_b).json()["items"][0]
    assert after_a["user_reacted"] is True
    assert after_a["reaction_count"] == 1
    assert after_b["user_reacted"] is False
