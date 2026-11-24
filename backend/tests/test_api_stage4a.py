from __future__ import annotations

import os
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
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
            "display_name": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201, res.text
    data = res.json()
    return data["user_id"], {"Authorization": f"Bearer {data['access_token']}"}


def test_comments_voting_sorting_blocking_and_auto_hide_moderation():
    uid_a, hdr_a = _register("alice4a@dev.test", "alice_dev")
    uid_b, hdr_b = _register("bob4a@dev.test", "bob_dev")
    uid_c, hdr_c = _register("carol4a@dev.test", "carol_dev")
    uid_d, hdr_d = _register("dave4a@dev.test", "dave_dev")

    # Reject spam comment
    r_spam = client.post(
        "/v1/comments",
        headers=hdr_a,
        json={"target_type": "TERM", "target_id": 42, "body": "Claim free-crypto-airdrop right now!"},
    )
    assert r_spam.status_code == 422

    # Post 2 comments on TERM #42 and 1 comment on SNIPPET #7
    c1 = client.post(
        "/v1/comments",
        headers=hdr_a,
        json={"target_type": "TERM", "target_id": 42, "body": "Mnemonic: Idempotent = repeating PUT gives same state."},
    ).json()
    c2 = client.post(
        "/v1/comments",
        headers=hdr_b,
        json={"target_type": "TERM", "target_id": 42, "body": "Often asked in Senior Backend REST interviews."},
    ).json()
    c_snip = client.post(
        "/v1/comments",
        headers=hdr_a,
        json={"target_type": "SNIPPET", "target_id": 7, "body": "Watch out for defer execution order (LIFO) in Go!"},
    )
    assert c_snip.status_code == 201

    # Upvote c2 twice so c2 outranks c1 in top sort
    assert client.post(f"/v1/comments/{c2['id']}/vote", headers=hdr_a, json={"vote": 1}).json()["score"] == 1
    assert client.post(f"/v1/comments/{c2['id']}/vote", headers=hdr_c, json={"vote": 1}).json()["score"] == 2
    assert client.post(f"/v1/comments/{c1['id']}/vote", headers=hdr_b, json={"vote": -1}).json()["score"] == -1

    top_list = client.get("/v1/comments/TERM/42?sort=top", headers=hdr_a).json()["items"]
    assert [c["id"] for c in top_list] == [c2["id"], c1["id"]]
    assert top_list[0]["user_vote"] == 1

    # Blocking bob hides bob's comments for alice
    assert client.post(f"/v1/moderation/block/{uid_b}", headers=hdr_a).status_code == 200
    after_block = client.get("/v1/comments/TERM/42?sort=top", headers=hdr_a).json()["items"]
    assert [c["id"] for c in after_block] == [c1["id"]]

    # 3 distinct user reports auto-hide c1 & add to moderation queue
    r1 = client.post("/v1/moderation/report", headers=hdr_b, json={"target_type": "COMMENT", "target_id": c1["id"], "reason": "offtopic"}).json()
    r2 = client.post("/v1/moderation/report", headers=hdr_c, json={"target_type": "COMMENT", "target_id": c1["id"], "reason": "spam"}).json()
    r3 = client.post("/v1/moderation/report", headers=hdr_d, json={"target_type": "COMMENT", "target_id": c1["id"], "reason": "spam"}).json()
    assert r1["auto_hidden"] is False
    assert r2["auto_hidden"] is False
    assert r3["auto_hidden"] is True

    after_autohide = client.get("/v1/comments/TERM/42?sort=top", headers=hdr_d).json()["items"]
    assert all(c["id"] != c1["id"] for c in after_autohide)

    assert client.get("/v1/moderation/queue", headers=hdr_d).status_code == 403
    from app.cli_admin import promote_user_role
    from app.models import SessionLocal

    with SessionLocal() as db:
        promote_user_role(db, "dave4a@dev.test", "MODERATOR")
    queue = client.get("/v1/moderation/queue", headers=hdr_d).json()["items"]
    assert len(queue) >= 3


def test_community_decks_publish_download_rate_moderation_and_account_delete():
    uid_a, hdr_a = _register("author4a@dev.test", "deck_author")
    uid_b, hdr_b = _register("learner4a@dev.test", "deck_learner")
    uid_c, hdr_c = _register("mod4a@dev.test", "deck_mod1")
    uid_d, hdr_d = _register("mod4b@dev.test", "deck_mod2")

    pub = client.post(
        "/v1/decks",
        headers=hdr_a,
        json={
            "title": "Backend Middle Interview Pack",
            "description": "Key distributed systems & DB vocabulary",
            "tags": ["#Backend", "Interview", "SQL"],
            "terms": [
                {
                    "term": "Idempotency",
                    "transcription": "/ˌaɪ.dɛmˈpoʊ.tən.si/",
                    "translation": "идемпотентность",
                    "example": "Payment retries must preserve idempotency.",
                    "explanation_ru": "Повторный запрос не меняет результат.",
                },
                {
                    "term": "Backpressure",
                    "transcription": "/ˈbæk.preʃ.ər/",
                    "translation": "обратное давление",
                    "example": "Kafka consumers apply backpressure.",
                    "explanation_ru": "Механизм защиты от перегрузки очереди.",
                },
            ],
        },
    )
    assert pub.status_code == 201, pub.text
    deck_id = pub.json()["id"]
    assert pub.json()["term_count"] == 2
    assert pub.json()["tags"] == ["backend", "interview", "sql"]

    # List & filter by tag
    listed = client.get("/v1/decks?tag=backend&query=Middle", headers=hdr_b).json()["decks"]
    assert len(listed) == 1
    assert listed[0]["id"] == deck_id

    # 1-tap download increments downloads_count & returns all terms
    dl = client.post(f"/v1/decks/{deck_id}/download", headers=hdr_b)
    assert dl.status_code == 200
    assert dl.json()["downloads_count"] == 1
    assert len(dl.json()["terms"]) == 2

    # Rate deck
    rt = client.post(f"/v1/decks/{deck_id}/rate", headers=hdr_b, json={"stars": 4})
    assert rt.status_code == 200
    assert rt.json()["rating"] == 4.0

    # Account deletion cascades and deletes author's published deck completely
    del_res = client.delete("/v1/account", headers=hdr_a)
    assert del_res.status_code == 200
    assert client.get(f"/v1/decks/{deck_id}", headers=hdr_b).status_code == 404

    # Publish second deck by learner & verify 3 reports auto-hide deck
    pub2 = client.post(
        "/v1/decks",
        headers=hdr_b,
        json={
            "title": "Abusive Test Deck",
            "description": "Will be reported",
            "tags": ["test"],
            "terms": [{"term": "Stub", "translation": "заглушка"}],
        },
    ).json()
    for hdr in (hdr_b, hdr_c, hdr_d):
        res_rep = client.post(
            "/v1/moderation/report",
            headers=hdr,
            json={"target_type": "DECK", "target_id": pub2["id"], "reason": "spam"},
        )
        assert res_rep.status_code == 201
    assert client.get("/v1/decks", headers=hdr_d).json()["decks"] == []
