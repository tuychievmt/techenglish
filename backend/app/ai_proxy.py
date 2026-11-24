from __future__ import annotations

import hashlib
import json
import os
import re
import time
from collections import OrderedDict
from typing import Any, Dict, List, Optional, Protocol

import httpx
from pydantic import BaseModel, Field, ValidationError

MAX_AI_REQUESTS_PER_DAY = int(os.getenv("TECHENGLISH_AI_DAILY_LIMIT", "15"))
DEFAULT_GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
DEFAULT_GEMINI_TIMEOUT_SECONDS = float(os.getenv("GEMINI_TIMEOUT_SECONDS", "12.0"))
AI_CACHE_TTL_SECONDS = int(os.getenv("AI_CACHE_TTL_SECONDS", "1800"))
AI_CACHE_MAX_SIZE = 512

_EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_PHONE_RE = re.compile(r"(?<!\d)(?:\+?\d[\d\s\-()]{7,}\d)(?!\d)")
_HANDLE_RE = re.compile(r"(?<!\w)@[a-zA-Z][a-zA-Z0-9_]{2,31}")
_TOKEN_RE = re.compile(r"(?i)(token=|api_key=|authorization:\s*bearer\s+)\S+")


def scrub_pii(text: str, max_chars: int = 2000) -> str:
    """ФТ-8.3: strips emails, phone numbers, handles, and tokens so zero profile PII reaches the LLM."""
    clean = (text or "").strip()
    clean = _EMAIL_RE.sub("[REDACTED_EMAIL]", clean)
    clean = _PHONE_RE.sub("[REDACTED_PHONE]", clean)
    clean = _HANDLE_RE.sub("@user", clean)
    clean = _TOKEN_RE.sub(r"\1[REDACTED_TOKEN]", clean)
    return clean[:max_chars]


_ROLE_QUESTIONS: Dict[str, List[str]] = {
    "backend": [
        "How do you ensure idempotency in a distributed payment API during network retries?",
        "When would you choose optimistic locking over pessimistic locking in PostgreSQL?",
        "How do you diagnose and prevent connection pool exhaustion under traffic spikes?",
    ],
    "android": [
        "How do structured concurrency and StateFlow prevent memory leaks across configuration changes?",
        "How do you design an offline-first Room + WorkManager sync pipeline without blocking the main thread?",
        "What causes excessive recomposition in Jetpack Compose and how do you measure it?",
    ],
    "system_design": [
        "How would you design a globally distributed rate limiter with low latency and graceful degradation?",
        "Explain the trade-offs between strong consistency and eventual consistency for a social feed.",
        "How do you implement backpressure between Kafka producers and slow downstream consumers?",
    ],
    "devops_sre": [
        "How do you structure a blameless postmortem after a production database outage?",
        "Explain the difference between Kubernetes liveness and readiness probes during rolling updates.",
        "How does an error budget govern release velocity in an SRE organization?",
    ],
    "ml_ai": [
        "How do you detect and mitigate data drift and concept drift in production ML pipelines?",
        "Explain the trade-offs between retrieval-augmented generation (RAG) and fine-tuning an LLM.",
        "How do you evaluate precision-recall trade-offs on severely imbalanced classification datasets?",
    ],
}

_INTERVIEW_RESPONSE_SCHEMA: Dict[str, Any] = {
    "type": "OBJECT",
    "properties": {
        "english_score": {"type": "INTEGER"},
        "technical_score": {"type": "INTEGER"},
        "feedback_ru": {"type": "STRING"},
        "improved_answer_en": {"type": "STRING"},
        "next_question_en": {"type": "STRING"},
    },
    "required": [
        "english_score",
        "technical_score",
        "feedback_ru",
        "improved_answer_en",
        "next_question_en",
    ],
}

_CODE_REVIEW_RESPONSE_SCHEMA: Dict[str, Any] = {
    "type": "OBJECT",
    "properties": {
        "tone_assessment_ru": {"type": "STRING"},
        "improved_english_text": {"type": "STRING"},
        "key_suggestions_ru": {"type": "ARRAY", "items": {"type": "STRING"}},
    },
    "required": ["tone_assessment_ru", "improved_english_text", "key_suggestions_ru"],
}


class InterviewGeminiPayload(BaseModel):
    english_score: int = Field(ge=0, le=100)
    technical_score: int = Field(ge=0, le=100)
    feedback_ru: str = Field(min_length=5)
    improved_answer_en: str = Field(min_length=5)
    next_question_en: str = Field(min_length=5)


class CodeReviewGeminiPayload(BaseModel):
    tone_assessment_ru: str = Field(min_length=5)
    improved_english_text: str = Field(min_length=5)
    key_suggestions_ru: List[str] = Field(min_length=1)


class GeminiAiClient(Protocol):
    model: str

    async def generate_json(self, prompt: str, response_schema: Dict[str, Any]) -> Dict[str, Any]: ...


class HttpxGeminiClient:
    """Async Gemini API client over HTTPS with explicit timeouts and structured JSON schema enforcement."""

    def __init__(
        self,
        api_key: str,
        model: str = DEFAULT_GEMINI_MODEL,
        timeout_seconds: float = DEFAULT_GEMINI_TIMEOUT_SECONDS,
        base_url: str = "https://generativelanguage.googleapis.com/v1beta",
    ) -> None:
        self.api_key = api_key
        self.model = model
        self.timeout_seconds = timeout_seconds
        self.base_url = base_url.rstrip("/")

    async def generate_json(self, prompt: str, response_schema: Dict[str, Any]) -> Dict[str, Any]:
        url = f"{self.base_url}/models/{self.model}:generateContent"
        payload = {
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.3,
                "responseMimeType": "application/json",
                "responseSchema": response_schema,
            },
        }
        timeout = httpx.Timeout(
            connect=min(5.0, self.timeout_seconds),
            read=self.timeout_seconds,
            write=5.0,
            pool=5.0,
        )
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(
                url,
                headers={"x-goog-api-key": self.api_key, "Content-Type": "application/json"},
                json=payload,
            )
            response.raise_for_status()
            data = response.json()
        candidates = data.get("candidates") or []
        if not candidates:
            raise ValueError("Gemini returned empty candidates list")
        parts = candidates[0].get("content", {}).get("parts") or []
        if not parts or "text" not in parts[0]:
            raise ValueError("Gemini response missing text part")
        return json.loads(parts[0]["text"])


def get_gemini_client() -> Optional[GeminiAiClient]:
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        return None
    model = os.getenv("GEMINI_MODEL", DEFAULT_GEMINI_MODEL).strip() or DEFAULT_GEMINI_MODEL
    timeout_s = float(os.getenv("GEMINI_TIMEOUT_SECONDS", str(DEFAULT_GEMINI_TIMEOUT_SECONDS)))
    return HttpxGeminiClient(api_key=api_key, model=model, timeout_seconds=timeout_s)


_AI_RESPONSE_CACHE: "OrderedDict[str, tuple[float, Dict[str, Any]]]" = OrderedDict()


def clear_ai_cache() -> None:
    _AI_RESPONSE_CACHE.clear()


def _cache_key(prefix: str, *parts: str) -> str:
    h = hashlib.sha256("\x1f".join([prefix, *parts]).encode("utf-8")).hexdigest()
    return h


def _cache_get(key: str) -> Optional[Dict[str, Any]]:
    entry = _AI_RESPONSE_CACHE.get(key)
    if not entry:
        return None
    expires_at, value = entry
    if time.monotonic() > expires_at:
        _AI_RESPONSE_CACHE.pop(key, None)
        return None
    _AI_RESPONSE_CACHE.move_to_end(key)
    return dict(value)


def _cache_put(key: str, value: Dict[str, Any]) -> None:
    _AI_RESPONSE_CACHE[key] = (time.monotonic() + AI_CACHE_TTL_SECONDS, dict(value))
    _AI_RESPONSE_CACHE.move_to_end(key)
    while len(_AI_RESPONSE_CACHE) > AI_CACHE_MAX_SIZE:
        _AI_RESPONSE_CACHE.popitem(last=False)


def _heuristic_interview_fallback(
    role: str,
    level: str,
    question_en: str,
    sanitized_answer: str,
    pii_scrubbed: bool,
    turn_index: int,
    fallback_reason: str,
) -> Dict[str, Any]:
    role_key = role if role in _ROLE_QUESTIONS else "backend"
    questions = _ROLE_QUESTIONS[role_key]
    next_q = questions[(turn_index + 1) % len(questions)]

    words = [w for w in re.split(r"\s+", sanitized_answer) if w]
    word_count = len(words)
    tech_keywords = {
        "idempotency", "transaction", "index", "lock", "cache", "queue", "retry", "backoff",
        "coroutine", "flow", "room", "recomposition", "consistency", "latency", "throughput",
        "backpressure", "replica", "partition", "probe", "budget", "postmortem", "drift", "rag",
        "rollback", "timeout", "mutex", "deadlock", "sharding", "canary", "metrics", "sla", "slo",
    }
    matched_terms = [w.strip(".,!?;:()").lower() for w in words if w.strip(".,!?;:()").lower() in tech_keywords]
    unique_tech = sorted(set(matched_terms))

    english_score = min(98, max(55, 60 + min(25, word_count // 2) + (8 if "." in sanitized_answer else 0)))
    technical_score = min(98, max(50, 55 + len(unique_tech) * 10 + min(15, word_count // 3)))

    if unique_tech:
        feedback_ru = (
            f"Хорошее использование профильной лексики ({', '.join(unique_tech[:4])}). "
            f"Для уровня {level.upper()} добавьте количественные метрики (SLA/p99 latency) и разбор trade-offs."
        )
    else:
        feedback_ru = (
            f"Ответ понятен, но для уровня {level.upper()} рекомендуется использовать точные инженерные термины "
            "(например: idempotency, latency trade-offs, graceful degradation, failure modes) и структурировать ответ по STAR."
        )

    improved_en = (
        f"In production ({role_key} / {level}), I would address this by clearly defining failure modes and trade-offs: "
        f"{sanitized_answer.rstrip('.')} Additionally, I would instrument p99 latency and error budgets to verify reliability."
    )

    return {
        "provider": "heuristic-fallback",
        "is_fallback": True,
        "fallback_reason": fallback_reason,
        "cached": False,
        "sanitized_input_used": True,
        "pii_scrubbed": pii_scrubbed,
        "question_en": question_en or questions[turn_index % len(questions)],
        "english_score": english_score,
        "technical_score": technical_score,
        "feedback_ru": feedback_ru,
        "improved_answer_en": improved_en,
        "next_question_en": next_q,
    }


def _heuristic_code_review_fallback(
    sanitized: str,
    context: str,
    pii_scrubbed: bool,
    fallback_reason: str,
) -> Dict[str, Any]:
    lower = sanitized.lower()
    suggestions: List[str] = []

    if any(blunt in lower for blunt in ("wrong", "bad code", "fix this", "stupid", "terrible")):
        tone_ru = "Тон слишком категоричный — в англоязычном Code Review предпочтительны мягкие формулировки (nit / suggestion)."
        suggestions.append("Замените императив («Fix this») на вежливый вопрос: «Could we consider…?» или «Nit: would it make sense to…?».")
    else:
        tone_ru = "Конструктивный и профессиональный тон Code Review."
        suggestions.append("Добавьте краткое обоснование («so that…» / «to prevent…»), чтобы автор PR сразу понял инженерную причину.")

    if "please" not in lower and "could we" not in lower and "nit:" not in lower:
        suggestions.append("Используйте маркеры приоритета комментария: `Nit:`, `Suggestion:`, `Blocking:`.")
    if len(sanitized.split()) < 8:
        suggestions.append("Сделайте комментарий чуть более конкретным: укажите ожидаемый результат или краевой случай (edge case).")

    improved = (
        f"Suggestion ({context}): Could we refine this section as follows — {sanitized.rstrip('.')}? "
        "This should improve maintainability and reduce the risk of regressions."
    )

    return {
        "provider": "heuristic-fallback",
        "is_fallback": True,
        "fallback_reason": fallback_reason,
        "cached": False,
        "pii_scrubbed": pii_scrubbed,
        "tone_assessment_ru": tone_ru,
        "improved_english_text": improved,
        "key_suggestions_ru": suggestions,
    }


async def evaluate_interview_answer_async(
    role: str,
    level: str,
    question_en: str,
    user_answer_raw: str,
    turn_index: int = 0,
    gemini_client: Optional[GeminiAiClient] = None,
) -> Dict[str, Any]:
    """
    Server-side async AI Mock Interviewer proxy (ФТ-8.1, ФТ-8.3).
    Scrubs PII strictly BEFORE building prompt or hitting cache/Gemini.
    Honest provider indicator + explicit is_fallback marker on any failure or unconfigured key.
    """
    sanitized_answer = scrub_pii(user_answer_raw)
    pii_scrubbed = sanitized_answer != (user_answer_raw or "").strip()
    role_key = role if role in _ROLE_QUESTIONS else "backend"
    questions = _ROLE_QUESTIONS[role_key]
    effective_question = question_en or questions[turn_index % len(questions)]

    if gemini_client is None:
        return _heuristic_interview_fallback(
            role_key,
            level,
            effective_question,
            sanitized_answer,
            pii_scrubbed,
            turn_index,
            fallback_reason="gemini_api_key_not_configured",
        )

    ck = _cache_key("interview", gemini_client.model, role_key, level, effective_question, sanitized_answer)
    cached = _cache_get(ck)
    if cached is not None:
        cached["cached"] = True
        cached["pii_scrubbed"] = pii_scrubbed
        return cached

    prompt = (
        f"You are a senior technical English interviewer evaluating a {level} {role_key} candidate.\n"
        f"Question: {effective_question}\n"
        f"Candidate answer (already PII-scrubbed): {sanitized_answer}\n"
        "Return strict JSON matching the schema with english_score (0..100), technical_score (0..100), "
        "concise feedback_ru in Russian, improved_answer_en in natural engineering English, and a relevant next_question_en."
    )
    try:
        raw_json = await gemini_client.generate_json(prompt, _INTERVIEW_RESPONSE_SCHEMA)
        parsed = InterviewGeminiPayload.model_validate(raw_json)
    except httpx.TimeoutException:
        return _heuristic_interview_fallback(
            role_key, level, effective_question, sanitized_answer, pii_scrubbed, turn_index, "gemini_timeout"
        )
    except ValidationError:
        return _heuristic_interview_fallback(
            role_key, level, effective_question, sanitized_answer, pii_scrubbed, turn_index, "gemini_invalid_schema"
        )
    except Exception:
        return _heuristic_interview_fallback(
            role_key, level, effective_question, sanitized_answer, pii_scrubbed, turn_index, "gemini_error"
        )

    result = {
        "provider": f"gemini:{gemini_client.model}",
        "is_fallback": False,
        "fallback_reason": None,
        "cached": False,
        "sanitized_input_used": True,
        "pii_scrubbed": pii_scrubbed,
        "question_en": effective_question,
        "english_score": parsed.english_score,
        "technical_score": parsed.technical_score,
        "feedback_ru": parsed.feedback_ru,
        "improved_answer_en": parsed.improved_answer_en,
        "next_question_en": parsed.next_question_en,
    }
    _cache_put(ck, result)
    return result


async def review_pr_comment_async(
    raw_text: str,
    context: str = "pull_request",
    gemini_client: Optional[GeminiAiClient] = None,
) -> Dict[str, Any]:
    """Server-side async AI Code/PR Reviewer proxy (ФТ-8.2, ФТ-8.3)."""
    sanitized = scrub_pii(raw_text)
    pii_scrubbed = sanitized != (raw_text or "").strip()

    if gemini_client is None:
        return _heuristic_code_review_fallback(
            sanitized, context, pii_scrubbed, fallback_reason="gemini_api_key_not_configured"
        )

    ck = _cache_key("code_review", gemini_client.model, context, sanitized)
    cached = _cache_get(ck)
    if cached is not None:
        cached["cached"] = True
        cached["pii_scrubbed"] = pii_scrubbed
        return cached

    prompt = (
        f"You are an engineering English coach reviewing a developer's {context} comment.\n"
        f"Draft (already PII-scrubbed): {sanitized}\n"
        "Return strict JSON matching the schema with tone_assessment_ru in Russian, "
        "a polite professional improved_english_text, and key_suggestions_ru (list of Russian tips)."
    )
    try:
        raw_json = await gemini_client.generate_json(prompt, _CODE_REVIEW_RESPONSE_SCHEMA)
        parsed = CodeReviewGeminiPayload.model_validate(raw_json)
    except httpx.TimeoutException:
        return _heuristic_code_review_fallback(sanitized, context, pii_scrubbed, "gemini_timeout")
    except ValidationError:
        return _heuristic_code_review_fallback(sanitized, context, pii_scrubbed, "gemini_invalid_schema")
    except Exception:
        return _heuristic_code_review_fallback(sanitized, context, pii_scrubbed, "gemini_error")

    result = {
        "provider": f"gemini:{gemini_client.model}",
        "is_fallback": False,
        "fallback_reason": None,
        "cached": False,
        "pii_scrubbed": pii_scrubbed,
        "tone_assessment_ru": parsed.tone_assessment_ru,
        "improved_english_text": parsed.improved_english_text,
        "key_suggestions_ru": parsed.key_suggestions_ru,
    }
    _cache_put(ck, result)
    return result


# Sync fallback wrappers kept for backwards compatibility with any direct sync callers
def evaluate_interview_answer(
    role: str,
    level: str,
    question_en: str,
    user_answer_raw: str,
    turn_index: int = 0,
) -> Dict[str, Any]:
    sanitized_answer = scrub_pii(user_answer_raw)
    pii_scrubbed = sanitized_answer != (user_answer_raw or "").strip()
    return _heuristic_interview_fallback(
        role, level, question_en, sanitized_answer, pii_scrubbed, turn_index, "sync_heuristic_caller"
    )


def review_pr_comment(raw_text: str, context: str = "pull_request") -> Dict[str, Any]:
    sanitized = scrub_pii(raw_text)
    pii_scrubbed = sanitized != (raw_text or "").strip()
    return _heuristic_code_review_fallback(sanitized, context, pii_scrubbed, "sync_heuristic_caller")
