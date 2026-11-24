from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


class ProfileUpdateRequest(BaseModel):
    username: Optional[str] = None
    display_name: Optional[str] = None
    avatar_key: Optional[str] = None
    status_text: Optional[str] = None
    tech_stack: Optional[List[str]] = None
    github_url: Optional[str] = None
    telegram_handle: Optional[str] = None
    accepts_pings: Optional[bool] = None


class ReportRequest(BaseModel):
    target_type: str = "PROFILE"
    target_id: str
    reason: str = Field(min_length=3, max_length=300)


class AddFriendRequest(BaseModel):
    username: str


class CreateCommentRequest(BaseModel):
    target_type: str = Field(default="TERM")  # TERM | SNIPPET
    target_id: int
    body: str


class VoteCommentRequest(BaseModel):
    vote: int  # -1, 0, +1


class CommunityDeckTermIn(BaseModel):
    term: str
    transcription: str = ""
    translation: str
    example: str = ""
    explanation_ru: str = ""


class PublishDeckRequest(BaseModel):
    title: str
    description: str = ""
    tags: List[str] = Field(default_factory=list)
    terms: List[CommunityDeckTermIn] = Field(default_factory=list)


class RateDeckRequest(BaseModel):
    stars: int = Field(ge=1, le=5)


class CreateSquadRequest(BaseModel):
    name: str
    weekly_goal_xp: int = Field(default=1000, ge=200, le=20000)


class JoinSquadRequest(BaseModel):
    invite_code: str


class SubmitDictionaryPrRequest(BaseModel):
    term: str
    proposed_translation_ru: str = ""
    proposed_translation_tg: str = ""
    proposed_definition_ru: str = ""
    source_reference: str
    license_accepted: bool = False


class RegisterDeviceTokenRequest(BaseModel):
    token: str = Field(min_length=8, max_length=512)
    platform: str = Field(default="ANDROID", max_length=24)


class UnregisterDeviceTokenRequest(BaseModel):
    token: Optional[str] = Field(default=None, max_length=512)
