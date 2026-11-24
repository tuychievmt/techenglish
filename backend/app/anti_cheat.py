from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple

MAX_FUTURE_SKEW_MS = 5 * 60 * 1000  # 5 minutes per Errata E1
MAX_OFFLINE_AGE_MS = 30 * 24 * 60 * 60 * 1000
MIN_SESSION_DURATION_MS = 300
MAX_XP_PER_MINUTE = 120
MAX_XP_PER_DAY = 2_500
PING_COOLDOWN_MS = 24 * 60 * 60 * 1000

CANONICAL_XP_BY_EVENT = {
    "FLASHCARD_CORRECT": 10,
    "FLASHCARD_RETRY": 5,
    "SCENARIO_CORRECT": 20,
    "SCENARIO_ATTEMPT": 5,
    "DOC_COMPLETED": 15,
    "LISTENING_CORRECT": 15,
    "CODE_BREAKDOWN": 10,
    "TIME_ATTACK_COMPLETE": 25,
    "CUSTOM_WORD_UPSERT": 0,
    "FAVORITE_TOGGLE": 0,
}

USERNAME_REGEX = re.compile(r"^[a-z][a-z0-9_]{2,19}$")
RESERVED_USERNAMES = {
    "admin", "administrator", "root", "support", "moderator", "system", "techenglish", "official"
}
BLOCKED_SUBSTRINGS = ("spam", "scam", "nazi", "porn")
PRESET_AVATARS = {
    "preset_terminal",
    "preset_kotlin",
    "preset_python",
    "preset_rust",
    "preset_linux",
    "preset_architect",
}
ALLOWED_OAUTH_AVATAR_PREFIXES = (
    "https://avatars.githubusercontent.com/",
    "https://lh3.googleusercontent.com/",
)

DUSHANBE_TZ = timezone(timedelta(hours=5))


def week_start_utc5_ms(now_ms: int) -> int:
    """Computes Monday 00:00 Asia/Dushanbe (UTC+5) week boundary in epoch milliseconds (ФТ-11.1)."""
    dt = datetime.fromtimestamp(now_ms / 1000.0, tz=DUSHANBE_TZ)
    monday = (dt - timedelta(days=dt.weekday())).replace(hour=0, minute=0, second=0, microsecond=0)
    return int(monday.timestamp() * 1000)


def rank_title_for_xp(total_xp: int) -> str:
    level = 1 + (max(0, total_xp) // 150)
    if level <= 2:
        return "Intern Developer"
    if level <= 4:
        return "Junior Developer"
    if level <= 7:
        return "Middle Developer"
    if level <= 11:
        return "Senior Developer"
    if level <= 16:
        return "Tech Lead"
    if level <= 22:
        return "Staff Engineer"
    return "Principal Architect"


def clamp_client_timestamp(client_ts_ms: int, server_received_ms: int) -> Tuple[int, bool]:
    """Errata E1: clamp future timestamps > +5 minutes relative to server receipt."""
    if client_ts_ms > server_received_ms + MAX_FUTURE_SKEW_MS:
        return server_received_ms, True
    return client_ts_ms, False


def should_incoming_sm2_win(
    existing_client_ts: int,
    existing_reps: int,
    existing_server_ts: int,
    incoming_client_ts: int,
    incoming_reps: int,
    incoming_server_ts: int,
) -> Tuple[bool, int, bool]:
    """
    Errata E1 SM-2 conflict resolver:
    1. Clamp future client_timestamp > +5m ahead of server receipt.
    2. Higher sanitized client_timestamp wins.
    3. Tie -> higher repetitions wins.
    4. Tie -> earlier server receipt wins (existing keeps win when existing_server_ts <= incoming_server_ts).
    Returns (incoming_wins, sanitized_incoming_client_ts, was_clamped).
    """
    inc_ts, clamped = clamp_client_timestamp(incoming_client_ts, incoming_server_ts)
    ex_ts, _ = clamp_client_timestamp(existing_client_ts, existing_server_ts)
    if inc_ts > ex_ts:
        return True, inc_ts, clamped
    if inc_ts < ex_ts:
        return False, inc_ts, clamped
    if incoming_reps > existing_reps:
        return True, inc_ts, clamped
    if incoming_reps < existing_reps:
        return False, inc_ts, clamped
    return incoming_server_ts < existing_server_ts, inc_ts, clamped


@dataclass
class AntiCheatResult:
    accepted: bool
    awarded_xp: int
    tampering_detected: bool
    reject_reason: Optional[str] = None


def evaluate_ranked_event(
    event_type: str,
    client_reported_xp: int,
    client_timestamp_ms: int,
    server_received_ms: int,
    session_duration_ms: int,
    is_correct: bool,
    xp_last_minute: int,
    xp_today: int,
) -> AntiCheatResult:
    if client_timestamp_ms > server_received_ms + MAX_FUTURE_SKEW_MS:
        return AntiCheatResult(False, 0, True, "FUTURE_TIMESTAMP_SKEW")
    if server_received_ms - client_timestamp_ms > MAX_OFFLINE_AGE_MS:
        return AntiCheatResult(False, 0, False, "STALE_EVENT_TIMESTAMP")

    base_xp = CANONICAL_XP_BY_EVENT.get(event_type, 0)
    canonical_xp = base_xp if is_correct else min(5, base_xp)
    tampered = client_reported_xp != canonical_xp

    if canonical_xp > 0 and session_duration_ms < MIN_SESSION_DURATION_MS:
        return AntiCheatResult(False, 0, True, "IMPLAUSIBLE_SESSION_SPEED")
    if xp_last_minute + canonical_xp > MAX_XP_PER_MINUTE:
        return AntiCheatResult(False, 0, tampered, "MINUTE_XP_LIMIT_EXCEEDED")
    if xp_today + canonical_xp > MAX_XP_PER_DAY:
        return AntiCheatResult(False, 0, tampered, "DAILY_XP_LIMIT_EXCEEDED")

    return AntiCheatResult(True, canonical_xp, tampered, None)


def validate_username(raw: str) -> str:
    norm = raw.strip().lstrip("@").lower()
    if not USERNAME_REGEX.match(norm):
        raise ValueError("Username must be 3-20 lowercase alphanumeric/underscore characters starting with a letter.")
    if norm in RESERVED_USERNAMES or any(bad in norm for bad in BLOCKED_SUBSTRINGS):
        raise ValueError("Username is reserved or violates community guidelines.")
    return norm


def is_avatar_allowed(avatar_key: str) -> bool:
    clean = (avatar_key or "").strip()
    if clean in PRESET_AVATARS:
        return True
    return any(clean.startswith(prefix) for prefix in ALLOWED_OAUTH_AVATAR_PREFIXES)


UGC_BLOCKED_TERMS = (
    "casino-bonus",
    "free-crypto-airdrop",
    "porn",
    "nazi",
    "buy-followers",
    "scam-link",
)
UGC_URL_REGEX = re.compile(r"https?://\S+")
DECK_TAG_REGEX = re.compile(r"^[a-z0-9_\-+]{2,24}$")
AUTO_HIDE_REPORT_THRESHOLD = 3


def validate_ugc_text(raw: str, min_len: int = 5, max_len: int = 1000) -> str:
    clean = re.sub(r"\s+", " ", (raw or "").strip())
    if len(clean) < min_len or len(clean) > max_len:
        raise ValueError(f"Text length must be between {min_len} and {max_len} characters.")
    lower = clean.lower()
    if any(bad in lower for bad in UGC_BLOCKED_TERMS) or len(UGC_URL_REGEX.findall(lower)) >= 3:
        raise ValueError("Content violates community moderation rules.")
    return clean


def sanitize_deck_tags(tags: list[str]) -> list[str]:
    out: list[str] = []
    seen: set[str] = set()
    for t in tags:
        clean = t.strip().lstrip("#").lower()
        if clean and clean not in seen and DECK_TAG_REGEX.match(clean) and not any(b in clean for b in UGC_BLOCKED_TERMS):
            seen.add(clean)
            out.append(clean)
        if len(out) >= 6:
            break
    return out
