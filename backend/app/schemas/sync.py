from __future__ import annotations

from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class Sm2ItemPayload(BaseModel):
    normalized_term: str
    repetitions: int = 0
    ease_factor: float = 2.5
    interval_days: int = 0
    next_review_at_ms: int = 0
    client_timestamp_ms: int


class SyncEventInput(BaseModel):
    event_id: str = Field(min_length=4, max_length=64)
    event_type: str
    client_reported_xp: int = 0
    client_timestamp_ms: int
    session_duration_ms: int = 2000
    is_correct: bool = True
    payload_json: str = "{}"


class SyncBatchRequest(BaseModel):
    events: List[SyncEventInput] = Field(default_factory=list)
    sm2_updates: List[Sm2ItemPayload] = Field(default_factory=list)
    favorite_terms: List[str] = Field(default_factory=list)
    custom_words: Dict[str, str] = Field(default_factory=dict)
    play_integrity_token: Optional[str] = None
    play_integrity_nonce: Optional[str] = None
