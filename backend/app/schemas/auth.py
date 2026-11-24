from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class RegisterRequest(BaseModel):
    email: str = Field(min_length=5, max_length=255)
    password: str = Field(min_length=8, max_length=128)
    username: str = Field(min_length=3, max_length=24)
    display_name: str = "Разработчик"
    age_confirmed_16_plus: bool = True


class LoginRequest(BaseModel):
    email: str
    password: str


class RefreshRequest(BaseModel):
    refresh_token: str


class OAuthPkceExchangeRequest(BaseModel):
    authorization_code: str = Field(min_length=4)
    code_verifier: str = Field(min_length=43, max_length=128)
    code_challenge: str = Field(min_length=20)
    provider_email: str
    suggested_username: str
    oauth_avatar_url: Optional[str] = None


class PasswordResetConfirmRequest(BaseModel):
    email: str
    reset_token: str
    new_password: str = Field(min_length=8, max_length=128)


class OAuthTicketExchangeRequest(BaseModel):
    ticket: str = Field(min_length=16, max_length=256)
