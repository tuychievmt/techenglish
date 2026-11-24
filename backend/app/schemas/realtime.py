from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field

from app.voice_storage import MAX_VOICE_SECONDS


class AiInterviewRequest(BaseModel):
    role: str = Field(default="backend")
    level: str = Field(default="middle")
    question_en: str = Field(default="")
    user_answer_text: str
    turn_index: int = Field(default=0, ge=0)
    consent_accepted: bool = Field(default=False)


class AiCodeReviewRequest(BaseModel):
    draft_text: str
    context: str = Field(default="pull_request")
    consent_accepted: bool = Field(default=False)


class PostRoomMessageRequest(BaseModel):
    body: str


class PvpMatchmakeRequest(BaseModel):
    force_training_fallback: bool = False


class PvpSubmitRequest(BaseModel):
    answers: List[int] = Field(default_factory=list)


class PvpDisconnectRequest(BaseModel):
    reason: str = "network_drop"


class PeerReviewSubmitRequest(BaseModel):
    case_id: str = "daily_deploy_rollback"
    text_response: str = ""
    voice_base64: Optional[str] = None
    voice_duration_seconds: int = Field(default=0, ge=0, le=MAX_VOICE_SECONDS)
    voice_content_type: str = "audio/mp4"


class PeerReviewFeedbackRequest(BaseModel):
    rating_stars: int = Field(ge=1, le=5)
    comment_text: str
