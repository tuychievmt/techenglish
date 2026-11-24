from __future__ import annotations

import os
import sys
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BACKEND_DIR))

os.environ.setdefault("DATABASE_URL", f"sqlite:///{BACKEND_DIR / 'test_stage3.db'}")
os.environ["PLAY_INTEGRITY_STRICT"] = "false"

from app.ai_proxy import MAX_AI_REQUESTS_PER_DAY  # noqa: E402
from app.main import app  # noqa: E402
from app.models import Base, SessionLocal, Sm2StateRow, SyncEventRow, UserRow, engine  # noqa: E402
from app.anti_cheat import week_start_utc5_ms  # noqa: E402


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
    d = res.json()
    return d["user_id"], {"Authorization": f"Bearer {d['access_token']}"}


def test_ai_proxy_consent_pii_scrubbing_and_daily_rate_limit():
    _, hdr = _register("ai_user@dev.test", "ai_dev_user")

    # 1. Reject without explicit consent (ФТ-8.3)
    no_consent = client.post(
        "/v1/ai/interview",
        headers=hdr,
        json={
            "role": "backend",
            "level": "middle",
            "user_answer_text": "We use idempotency keys for payment retries.",
            "consent_accepted": False,
        },
    )
    assert no_consent.status_code == 400

    # 2. Succeed with consent, scrub PII, never store audio
    ok = client.post(
        "/v1/ai/interview",
        headers=hdr,
        json={
            "role": "backend",
            "level": "senior",
            "user_answer_text": "I am @ai_dev_user (ai_user@dev.test) and I use idempotency keys and backoff retries.",
            "consent_accepted": True,
        },
    )
    assert ok.status_code == 200, ok.text
    body = ok.json()
    assert body["pii_scrubbed"] is True
    assert body["audio_stored"] is False
    assert "ai_user@dev.test" not in body["improved_answer_en"]
    assert body["remaining_daily_quota"] == MAX_AI_REQUESTS_PER_DAY - 1

    # 3. Code / PR Reviewer proxy (ФТ-8.2)
    rev = client.post(
        "/v1/ai/code-review",
        headers=hdr,
        json={
            "draft_text": "This is wrong, fix this loop immediately.",
            "context": "github_pr",
            "consent_accepted": True,
        },
    )
    assert rev.status_code == 200
    assert rev.json()["remaining_daily_quota"] == MAX_AI_REQUESTS_PER_DAY - 2

    # 4. Exhaust daily quota -> 429 Too Many Requests
    for _ in range(MAX_AI_REQUESTS_PER_DAY - 2):
        r = client.post(
            "/v1/ai/code-review",
            headers=hdr,
            json={"draft_text": "Could we extract this into a helper?", "consent_accepted": True},
        )
        assert r.status_code == 200

    over_limit = client.post(
        "/v1/ai/interview",
        headers=hdr,
        json={
            "role": "android",
            "level": "middle",
            "user_answer_text": "Structured concurrency binds child coroutines to a scope.",
            "consent_accepted": True,
        },
    )
    assert over_limit.status_code == 429


def test_squads_5_to_10_bounds_weekly_goal_and_non_ranked_multiplier():
    users = [_register(f"sq{i}@dev.test", f"squad_dev_{i}") for i in range(11)]
    leader_id, leader_hdr = users[0]

    sq = client.post("/v1/squads", headers=leader_hdr, json={"name": "Kotlin Guild", "weekly_goal_xp": 500})
    assert sq.status_code == 201, sq.text
    code = sq.json()["invite_code"]
    assert sq.json()["is_active_for_challenge"] is False  # only 1 member (< 5)
    assert sq.json()["affects_ranked_leaderboard_xp"] is False

    # Join 4 more developers -> total 5 members (active!)
    for i in range(1, 5):
        j = client.post("/v1/squads/join", headers=users[i][1], json={"invite_code": code})
        assert j.status_code == 200
    assert j.json()["member_count"] == 5
    assert j.json()["is_active_for_challenge"] is True

    # Seed weekly canonical XP across members >= 500 -> next-week bonus multiplier 1.25x (non-ranked)
    now_ms = int(time.time() * 1000)
    wk = week_start_utc5_ms(now_ms)
    with SessionLocal() as db:
        for i in range(5):
            db.add(
                SyncEventRow(
                    event_id=f"sq-ev-{i}",
                    user_id=users[i][0],
                    event_type="SCENARIO_CORRECT",
                    payload_json="{}",
                    awarded_xp=120,
                    client_timestamp_ms=now_ms - 5000,
                    server_received_ms=now_ms,
                    week_start_utc5_ms=wk,
                )
            )
        db.commit()

    my_sq = client.get("/v1/squads/my", headers=leader_hdr).json()["squad"]
    assert my_sq["current_week_xp"] == 600
    assert my_sq["goal_reached"] is True
    assert my_sq["next_week_bonus_multiplier"] == 1.25
    assert my_sq["affects_ranked_leaderboard_xp"] is False

    # Join members 6..10 -> succeed; 11th member rejected with 409
    for i in range(5, 10):
        assert client.post("/v1/squads/join", headers=users[i][1], json={"invite_code": code}).status_code == 200
    full_res = client.post("/v1/squads/join", headers=users[10][1], json={"invite_code": code})
    assert full_res.status_code == 409


def test_crowdsource_eligibility_gate_license_pr_approval_and_account_delete():
    uid, hdr = _register("contrib@dev.test", "contrib_dev")
    _, voter_hdr = _register("voter@dev.test", "voter_dev")

    # 1. Fresh account fails eligibility gate (403)
    elig0 = client.get("/v1/crowdsource/eligibility", headers=hdr).json()
    assert elig0["is_eligible"] is False
    assert len(elig0["unmet_reasons_ru"]) == 3  # rank, age, learned terms

    blocked_pr = client.post(
        "/v1/crowdsource/prs",
        headers=hdr,
        json={
            "term": "Idempotency",
            "proposed_translation_ru": "идемпотентность",
            "source_reference": "https://www.rfc-editor.org/rfc/rfc9110",
            "license_accepted": True,
        },
    )
    assert blocked_pr.status_code == 403

    # 2. Satisfy all 4 eligibility gates: >= 600 XP, >= 14 days old, >= 100 learned SM-2 terms, 0 confirmed reports
    now_ms = int(time.time() * 1000)
    with SessionLocal() as db:
        u = db.get(UserRow, uid)
        u.total_xp = 750
        u.created_at_ms = now_ms - (20 * 24 * 60 * 60 * 1000)
        u.confirmed_reports = 0
        for i in range(100):
            db.add(
                Sm2StateRow(
                    user_id=uid,
                    normalized_term=f"term_{i}",
                    repetitions=1,
                    ease_factor=2.5,
                    interval_days=1,
                    next_review_at_ms=now_ms + 86400000,
                    client_timestamp_ms=now_ms,
                    server_received_ms=now_ms,
                )
            )
        db.commit()

    elig1 = client.get("/v1/crowdsource/eligibility", headers=hdr).json()
    assert elig1["is_eligible"] is True

    # 3. License rejection (422)
    no_lic = client.post(
        "/v1/crowdsource/prs",
        headers=hdr,
        json={
            "term": "Idempotency",
            "proposed_translation_ru": "идемпотентность",
            "source_reference": "https://www.rfc-editor.org/rfc/rfc9110",
            "license_accepted": False,
        },
    )
    assert no_lic.status_code == 422

    # 4. Submit valid PR, vote, approve -> unlocks Contributor badge
    ok_pr = client.post(
        "/v1/crowdsource/prs",
        headers=hdr,
        json={
            "term": "Idempotency",
            "proposed_translation_ru": "идемпотентность",
            "proposed_translation_tg": "идемпотентност",
            "source_reference": "https://www.rfc-editor.org/rfc/rfc9110",
            "license_accepted": True,
        },
    )
    assert ok_pr.status_code == 201, ok_pr.text
    pr_id = ok_pr.json()["id"]

    vote_res = client.post(f"/v1/crowdsource/prs/{pr_id}/vote", headers=voter_hdr, json={"vote": 1})
    assert vote_res.status_code == 200
    assert vote_res.json()["upvotes"] == 1

    assert client.post(f"/v1/crowdsource/prs/{pr_id}/approve", headers=voter_hdr).status_code == 403
    from app.cli_admin import promote_user_role

    with SessionLocal() as db:
        promote_user_role(db, "voter@dev.test", "MODERATOR")
    appr = client.post(f"/v1/crowdsource/prs/{pr_id}/approve", headers=voter_hdr)
    assert appr.status_code == 200
    assert appr.json()["contributor_badge_unlocked"] is True

    prof = client.get("/v1/profile/me", headers=hdr).json()
    contrib_badge = [b for b in prof["badges"] if b["id"] == "contributor"][0]
    assert contrib_badge["unlocked"] is True

    # 5. Account deletion cleanly removes user's PRs
    assert client.delete("/v1/account", headers=hdr).status_code == 200
    assert client.get("/v1/crowdsource/prs", headers=voter_hdr).json()["items"] == []
