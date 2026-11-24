from __future__ import annotations

import os
import sys
from pathlib import Path
from typing import Any, Dict, List

import httpx
import pytest
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
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
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201
    return {"Authorization": f"Bearer {res.json()['access_token']}"}


def test_ai_proxy_never_lies_about_gemini_provider_when_key_set_without_real_call(monkeypatch: pytest.MonkeyPatch):
    # BUG-05 reproduction: setting GEMINI_API_KEY previously made ai_proxy claim
    # provider="gemini-server-proxy" while returning hardcoded heuristics with zero HTTP call.
    monkeypatch.setenv("GEMINI_API_KEY", "fake-invalid-key-not-reachable")
    hdr = _register("ai_honest@example.com", "ai_honest")

    from app.ai_proxy import clear_ai_cache, get_gemini_client

    clear_ai_cache()

    class FailingGeminiClient:
        model = "gemini-2.0-flash"

        async def generate_json(self, prompt: str, response_schema: Dict[str, Any]) -> Dict[str, Any]:
            raise httpx.ConnectError("upstream unreachable")

    app.dependency_overrides[get_gemini_client] = lambda: FailingGeminiClient()
    try:
        resp = client.post(
            "/v1/ai/interview",
            headers=hdr,
            json={
                "role": "backend",
                "level": "senior",
                "user_answer_text": "We use idempotency keys and database transactions.",
                "consent_accepted": True,
            },
        )
        assert resp.status_code == 200, resp.text
        body = resp.json()
        assert body["is_fallback"] is True
        assert body["provider"] == "heuristic-fallback"
        assert body["fallback_reason"]
    finally:
        app.dependency_overrides.pop(get_gemini_client, None)


def test_ai_proxy_calls_async_gemini_scrubs_pii_caches_and_handles_timeout():
    from app.ai_proxy import clear_ai_cache, get_gemini_client

    clear_ai_cache()
    hdr = _register("ai_fake@example.com", "ai_fake")

    captured_prompts: List[str] = []

    class FakeGeminiClient:
        model = "gemini-2.0-flash"

        def __init__(self, mode: str = "ok") -> None:
            self.mode = mode
            self.calls = 0

        async def generate_json(self, prompt: str, response_schema: Dict[str, Any]) -> Dict[str, Any]:
            self.calls += 1
            captured_prompts.append(prompt)
            if self.mode == "timeout":
                raise httpx.ReadTimeout("Gemini request timed out")
            if self.mode == "bad_schema":
                return {"unexpected": "garbage"}
            if "tone_assessment_ru" in str(response_schema):
                return {
                    "tone_assessment_ru": "Вежливый конструктивный комментарий.",
                    "improved_english_text": "Could we add an index to avoid full table scans?",
                    "key_suggestions_ru": ["Уточните влияние на p99 latency."],
                }
            return {
                "english_score": 91,
                "technical_score": 94,
                "feedback_ru": "Чёткое объяснение идемпотентности и отказоустойчивости.",
                "improved_answer_en": "I enforce idempotency using unique keys persisted within the same DB transaction.",
                "next_question_en": "How would you handle expired idempotency keys across regions?",
            }

    fake = FakeGeminiClient(mode="ok")
    app.dependency_overrides[get_gemini_client] = lambda: fake
    try:
        # 1. Interview turn calls Gemini, strips PII BEFORE prompt reaches client
        r1 = client.post(
            "/v1/ai/interview",
            headers=hdr,
            json={
                "role": "backend",
                "level": "senior",
                "user_answer_text": "Contact secret@corp.io or +992901234567 @myhandle token=xyz123 about idempotency keys.",
                "consent_accepted": True,
            },
        )
        assert r1.status_code == 200, r1.text
        b1 = r1.json()
        assert b1["provider"] == "gemini:gemini-2.0-flash"
        assert b1["is_fallback"] is False
        assert b1["pii_scrubbed"] is True
        assert b1["english_score"] == 91
        assert b1["technical_score"] == 94
        assert b1["cached"] is False
        assert fake.calls == 1
        assert "secret@corp.io" not in captured_prompts[0]
        assert "+992901234567" not in captured_prompts[0]
        assert "@myhandle" not in captured_prompts[0]
        assert "xyz123" not in captured_prompts[0]

        # 2. Identical request hits cache (no second Gemini call)
        r2 = client.post(
            "/v1/ai/interview",
            headers=hdr,
            json={
                "role": "backend",
                "level": "senior",
                "user_answer_text": "Contact secret@corp.io or +992901234567 @myhandle token=xyz123 about idempotency keys.",
                "consent_accepted": True,
            },
        )
        assert r2.status_code == 200
        assert r2.json()["cached"] is True
        assert fake.calls == 1

        # 3. Code review calls Gemini
        r_cr = client.post(
            "/v1/ai/code-review",
            headers=hdr,
            json={
                "draft_text": "Fix this slow query asap",
                "context": "pull_request",
                "consent_accepted": True,
            },
        )
        assert r_cr.status_code == 200
        assert r_cr.json()["provider"] == "gemini:gemini-2.0-flash"
        assert r_cr.json()["is_fallback"] is False

        # 4. Timeout falls back cleanly with explicit fallback marker
        clear_ai_cache()
        fake.mode = "timeout"
        r_to = client.post(
            "/v1/ai/interview",
            headers=hdr,
            json={
                "role": "android",
                "level": "middle",
                "user_answer_text": "StateFlow survives configuration changes inside ViewModel.",
                "consent_accepted": True,
            },
        )
        assert r_to.status_code == 200
        assert r_to.json()["is_fallback"] is True
        assert r_to.json()["provider"] == "heuristic-fallback"
        assert "timeout" in r_to.json()["fallback_reason"]

        # 5. Malformed Gemini JSON schema also falls back cleanly
        fake.mode = "bad_schema"
        r_bad = client.post(
            "/v1/ai/code-review",
            headers=hdr,
            json={
                "draft_text": "Bad code, rewrite immediately!",
                "context": "pull_request",
                "consent_accepted": True,
            },
        )
        assert r_bad.status_code == 200
        assert r_bad.json()["is_fallback"] is True
        assert r_bad.json()["provider"] == "heuristic-fallback"
    finally:
        app.dependency_overrides.pop(get_gemini_client, None)
