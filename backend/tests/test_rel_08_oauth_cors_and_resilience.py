from __future__ import annotations

import os
import sys
from pathlib import Path
from unittest.mock import patch
from urllib.parse import parse_qs, urlparse

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


def test_cors_headers_present_for_render_origin():
    resp = client.options(
        "/v1/auth/login",
        headers={
            "Origin": "https://techenglish-trwk.onrender.com",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "Authorization, Content-Type",
        },
    )
    assert resp.status_code == 200
    assert resp.headers.get("access-control-allow-origin") == "https://techenglish-trwk.onrender.com"


def test_oauth_start_and_callback_with_missing_optional_profile_fields_and_render_https(monkeypatch):
    monkeypatch.setenv("GITHUB_CLIENT_ID", "gh_cid_prod")
    monkeypatch.setenv("GITHUB_CLIENT_SECRET", "gh_csec_prod")
    monkeypatch.setenv("GOOGLE_CLIENT_ID", "gg_cid_prod")
    monkeypatch.setenv("GOOGLE_CLIENT_SECRET", "gg_csec_prod")

    def fake_httpx_handler(request: httpx.Request) -> httpx.Response:
        url = str(request.url)
        if url == "https://github.com/login/oauth/access_token":
            return httpx.Response(200, json={"access_token": "gho_token_123"})
        if url == "https://api.github.com/user":
            # Optional fields missing/null + hyphenated login
            return httpx.Response(
                200,
                json={"id": 778899, "login": "octo-cat-dev", "name": None, "avatar_url": None, "email": None},
            )
        if url == "https://api.github.com/user/emails":
            # Simulate forbidden / empty private emails
            return httpx.Response(403, json={"message": "Resource not accessible"})
        if url == "https://oauth2.googleapis.com/token":
            return httpx.Response(200, json={"access_token": "ya29.google_token"})
        if url == "https://openidconnect.googleapis.com/v1/userinfo":
            # Optional name/picture/email missing or dotted email
            return httpx.Response(
                200,
                json={
                    "sub": "google-sub-554433",
                    "email": None,
                    "email_verified": False,
                    "given_name": None,
                    "name": None,
                    "picture": "https://lh4.googleusercontent.com/a/default-user=s96-c",
                },
            )
        return httpx.Response(404, json={"error": "unexpected_url"})

    transport = httpx.MockTransport(fake_httpx_handler)
    real_client_cls = httpx.Client

    def patched_client(*args, **kwargs):
        kwargs["transport"] = transport
        return real_client_cls(*args, **kwargs)

    with patch("app.oauth_provider.httpx.Client", side_effect=patched_client):
        # 1. GitHub /start behind Render HTTPS proxy -> callback -> deep link -> ticket exchange
        gh_start = client.get(
            "/v1/auth/oauth/github/start?redirect_uri=techenglish://auth/callback&client_state=mob_gh_1",
            headers={"Host": "techenglish-trwk.onrender.com", "X-Forwarded-Proto": "https"},
            follow_redirects=False,
        )
        assert gh_start.status_code == 302
        gh_loc = urlparse(gh_start.headers["location"])
        assert gh_loc.netloc == "github.com"
        gh_qs = parse_qs(gh_loc.query)
        assert gh_qs["redirect_uri"][0] == "https://techenglish-trwk.onrender.com/v1/auth/oauth/github/callback"
        gh_state = gh_qs["state"][0]

        gh_cb = client.get(
            f"/v1/auth/oauth/github/callback?code=gh_code_ok&state={gh_state}",
            headers={"Host": "techenglish-trwk.onrender.com", "X-Forwarded-Proto": "https"},
            follow_redirects=False,
        )
        assert gh_cb.status_code == 302, gh_cb.text
        gh_deep = urlparse(gh_cb.headers["location"])
        assert f"{gh_deep.scheme}://{gh_deep.netloc}{gh_deep.path}" == "techenglish://auth/callback"
        gh_ticket = parse_qs(gh_deep.query)["ticket"][0]
        assert parse_qs(gh_deep.query)["state"][0] == "mob_gh_1"

        gh_jwt = client.post("/v1/auth/oauth/ticket/exchange", json={"ticket": gh_ticket})
        assert gh_jwt.status_code == 200
        assert gh_jwt.json()["username"] == "octo_cat_dev"
        assert gh_jwt.json()["provider"] == "GITHUB"

        # 2. Google /start -> callback with missing email/name -> deep link -> ticket exchange (no 500!)
        gg_start = client.get(
            "/v1/auth/oauth/google/start?redirect_uri=techenglish://auth/callback&client_state=mob_gg_2",
            headers={"Host": "techenglish-trwk.onrender.com", "X-Forwarded-Proto": "https"},
            follow_redirects=False,
        )
        assert gg_start.status_code == 302
        gg_loc = urlparse(gg_start.headers["location"])
        assert gg_loc.netloc == "accounts.google.com"
        gg_qs = parse_qs(gg_loc.query)
        assert gg_qs["redirect_uri"][0] == "https://techenglish-trwk.onrender.com/v1/auth/oauth/google/callback"
        gg_state = gg_qs["state"][0]

        gg_cb = client.get(
            f"/v1/auth/oauth/google/callback?code=gg_code_ok&state={gg_state}",
            headers={"Host": "techenglish-trwk.onrender.com", "X-Forwarded-Proto": "https"},
            follow_redirects=False,
        )
        assert gg_cb.status_code == 302, gg_cb.text
        gg_deep = urlparse(gg_cb.headers["location"])
        assert f"{gg_deep.scheme}://{gg_deep.netloc}{gg_deep.path}" == "techenglish://auth/callback"
        gg_ticket = parse_qs(gg_deep.query)["ticket"][0]

        gg_jwt = client.post("/v1/auth/oauth/ticket/exchange", json={"ticket": gg_ticket})
        assert gg_jwt.status_code == 200
        assert gg_jwt.json()["provider"] == "GOOGLE"

        # 3. User cancels consent screen (?error=access_denied) -> clean redirect to deep link
        cancel_start = client.get(
            "/v1/auth/oauth/google/start?redirect_uri=techenglish://auth/callback&client_state=mob_cancel",
            follow_redirects=False,
        )
        cancel_state = parse_qs(urlparse(cancel_start.headers["location"]).query)["state"][0]
        cancel_cb = client.get(
            f"/v1/auth/oauth/google/callback?error=access_denied&state={cancel_state}",
            follow_redirects=False,
        )
        assert cancel_cb.status_code == 302
        cancel_deep = urlparse(cancel_cb.headers["location"])
        assert f"{cancel_deep.scheme}://{cancel_deep.netloc}{cancel_deep.path}" == "techenglish://auth/callback"
        assert parse_qs(cancel_deep.query)["error"][0] == "access_denied"
        assert parse_qs(cancel_deep.query)["state"][0] == "mob_cancel"
