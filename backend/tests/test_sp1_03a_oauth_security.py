from __future__ import annotations

import base64
import hashlib
import os
import sys
from pathlib import Path
from urllib.parse import parse_qs, urlparse

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


def test_legacy_pkce_exchange_cannot_hijack_victim_account_by_spoofing_provider_email():
    # Victim registers normally
    reg = client.post(
        "/v1/auth/register",
        json={
            "email": "victim@example.com",
            "password": "VictimStrongPassword123!",
            "username": "victim_dev",
            "age_confirmed_16_plus": True,
        },
    )
    assert reg.status_code == 201
    victim_id = reg.json()["user_id"]

    # Attacker tries BUG-01: generates own PKCE pair and sends victim's email to /exchange
    verifier = "a" * 64
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii")).digest()).rstrip(b"=").decode("ascii")
    hijack = client.post(
        "/v1/auth/oauth/github/exchange",
        json={
            "authorization_code": "attacker_fake_code",
            "code_verifier": verifier,
            "code_challenge": challenge,
            "provider_email": "victim@example.com",
            "suggested_username": "attacker",
        },
    )
    assert hijack.status_code in (403, 404, 410), (
        f"BUG-01 STILL EXPLOITABLE! Attacker got status={hijack.status_code}, "
        f"user_id={hijack.json().get('user_id') if hijack.status_code == 200 else None} vs victim={victim_id}"
    )


def test_server_side_oauth_authorize_callback_ticket_flow_and_protections():
    from app.oauth_provider import OAuthUserInfo, get_oauth_provider

    class MockOAuthProvider:
        is_fake = True

        def __init__(self) -> None:
            self.users_by_code = {
                "valid_gh_code": OAuthUserInfo(
                    provider="GITHUB",
                    sub="gh-sub-9001",
                    email="oauth_alice@example.com",
                    email_verified=True,
                    username="oauth_alice",
                    avatar_url="https://avatars.githubusercontent.com/u/9001",
                ),
                "unverified_email_code": OAuthUserInfo(
                    provider="GITHUB",
                    sub="gh-sub-unverified",
                    email="unverified_target@example.com",
                    email_verified=False,
                    username="unverified_u",
                    avatar_url="preset_terminal",
                ),
            }
            self.seen_verifiers = []

        def build_authorize_url(self, provider: str, state: str, code_challenge: str, callback_url: str) -> str:
            return (
                f"https://provider.mock/{provider.lower()}/authorize"
                f"?state={state}&code_challenge={code_challenge}&redirect_uri={callback_url}"
            )

        def exchange_code_for_user(self, provider: str, code: str, code_verifier: str, callback_url: str) -> OAuthUserInfo:
            assert len(code_verifier) >= 43
            self.seen_verifiers.append(code_verifier)
            if code not in self.users_by_code:
                raise ValueError("Invalid OAuth authorization code")
            return self.users_by_code[code]

    mock_provider = MockOAuthProvider()
    app.dependency_overrides[get_oauth_provider] = lambda: mock_provider
    try:
        # 1. Non-allowlisted redirect_uri is rejected
        bad_redir = client.get(
            "/v1/auth/oauth/github/authorize?redirect_uri=https://evil.example/steal",
            follow_redirects=False,
        )
        assert bad_redir.status_code == 400

        # 2. Valid authorize creates server state + PKCE and redirects to provider
        auth_resp = client.get(
            "/v1/auth/oauth/github/authorize?redirect_uri=techenglish://auth/callback&client_state=cli_st_123",
            follow_redirects=False,
        )
        assert auth_resp.status_code in (302, 307), auth_resp.text
        loc = urlparse(auth_resp.headers["location"])
        qs = parse_qs(loc.query)
        server_state = qs["state"][0]
        assert qs["code_challenge"][0]

        # 3. Wrong state on callback is rejected
        bad_state = client.get(
            "/v1/auth/oauth/github/callback?code=valid_gh_code&state=forged_state",
            follow_redirects=False,
        )
        assert bad_state.status_code == 400

        # 4. Valid callback exchanges code on server and redirects to deep link with one-time ticket
        cb_resp = client.get(
            f"/v1/auth/oauth/github/callback?code=valid_gh_code&state={server_state}",
            follow_redirects=False,
        )
        assert cb_resp.status_code in (302, 307), cb_resp.text
        deep_loc = urlparse(cb_resp.headers["location"])
        assert deep_loc.scheme == "techenglish"
        assert deep_loc.netloc == "auth"
        assert deep_loc.path == "/callback"
        deep_qs = parse_qs(deep_loc.query)
        ticket = deep_qs["ticket"][0]
        assert deep_qs["state"][0] == "cli_st_123"
        assert len(mock_provider.seen_verifiers) == 1

        # Replaying the same state fails
        replay_state = client.get(
            f"/v1/auth/oauth/github/callback?code=valid_gh_code&state={server_state}",
            follow_redirects=False,
        )
        assert replay_state.status_code == 400

        # 5. Exchange one-time ticket for JWT succeeds once, replay fails
        ex_ok = client.post("/v1/auth/oauth/ticket/exchange", json={"ticket": ticket})
        assert ex_ok.status_code == 200, ex_ok.text
        assert ex_ok.json()["provider"] == "GITHUB"
        assert ex_ok.json()["access_token"]
        user_id_first = ex_ok.json()["user_id"]

        ex_replay = client.post("/v1/auth/oauth/ticket/exchange", json={"ticket": ticket})
        assert ex_replay.status_code == 400

        # 6. Second login with same (provider, sub) resolves same user
        auth2 = client.get(
            "/v1/auth/oauth/github/authorize?redirect_uri=techenglish://auth/callback",
            follow_redirects=False,
        )
        state2 = parse_qs(urlparse(auth2.headers["location"]).query)["state"][0]
        cb2 = client.get(
            f"/v1/auth/oauth/github/callback?code=valid_gh_code&state={state2}",
            follow_redirects=False,
        )
        ticket2 = parse_qs(urlparse(cb2.headers["location"]).query)["ticket"][0]
        ex2 = client.post("/v1/auth/oauth/ticket/exchange", json={"ticket": ticket2})
        assert ex2.status_code == 200 and ex2.json()["user_id"] == user_id_first

        # 7. Cannot merge into existing unverified email account (or with unverified provider email)
        client.post(
            "/v1/auth/register",
            json={
                "email": "unverified_target@example.com",
                "password": "StrongPassword123!",
                "username": "unver_target",
                "age_confirmed_16_plus": True,
            },
        )
        auth3 = client.get(
            "/v1/auth/oauth/github/authorize?redirect_uri=techenglish://auth/callback",
            follow_redirects=False,
        )
        state3 = parse_qs(urlparse(auth3.headers["location"]).query)["state"][0]
        cb3 = client.get(
            f"/v1/auth/oauth/github/callback?code=unverified_email_code&state={state3}",
            follow_redirects=False,
        )
        assert cb3.status_code == 409
    finally:
        app.dependency_overrides.pop(get_oauth_provider, None)
