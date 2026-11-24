from __future__ import annotations

import base64
import hashlib
import json
import os
import secrets
import time
from dataclasses import dataclass
from typing import Dict, Optional, Protocol, Set
from urllib.parse import urlencode

import httpx

from app.config import is_deployed_env, is_prod_env
from app.security import hash_action_token


@dataclass(frozen=True)
class OAuthUserInfo:
    provider: str
    sub: str
    email: Optional[str]
    email_verified: bool
    username: str
    avatar_url: str = "preset_terminal"
    display_name: Optional[str] = None


class OAuthProviderClient(Protocol):
    is_fake: bool

    def build_authorize_url(self, provider: str, state: str, code_challenge: str, callback_url: str) -> str: ...

    def exchange_code_for_user(
        self,
        provider: str,
        code: str,
        code_verifier: str,
        callback_url: str,
    ) -> OAuthUserInfo: ...


def compute_pkce_s256_challenge(code_verifier: str) -> str:
    digest = hashlib.sha256(code_verifier.encode("ascii")).digest()
    return base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")


def get_allowed_redirect_uris() -> Set[str]:
    base = {"techenglish://auth/callback"}
    extra = os.getenv("OAUTH_ALLOWED_REDIRECT_URIS", "").strip()
    if extra:
        for item in extra.split(","):
            uri = item.strip()
            if uri:
                base.add(uri)
    return base


class HttpOAuthProviderClient:
    """Real server-side GitHub / Google OAuth 2.0 client exchanging code with client_secret & PKCE."""

    is_fake: bool = False

    def __init__(self, timeout_seconds: float = 10.0) -> None:
        self.timeout_seconds = timeout_seconds

    def _credentials(self, provider: str) -> tuple[str, str]:
        prov = provider.upper()
        if prov == "GITHUB":
            cid = os.getenv("GITHUB_CLIENT_ID", "").strip()
            csec = os.getenv("GITHUB_CLIENT_SECRET", "").strip()
        elif prov == "GOOGLE":
            cid = os.getenv("GOOGLE_CLIENT_ID", "").strip()
            csec = os.getenv("GOOGLE_CLIENT_SECRET", "").strip()
        else:
            raise ValueError(f"Unsupported OAuth provider: {provider}")
        if not cid or not csec:
            raise RuntimeError(f"OAuth credentials not configured for {prov}")
        return cid, csec

    def build_authorize_url(self, provider: str, state: str, code_challenge: str, callback_url: str) -> str:
        prov = provider.upper()
        client_id, _ = self._credentials(prov)
        if prov == "GITHUB":
            params = urlencode(
                {
                    "client_id": client_id,
                    "redirect_uri": callback_url,
                    "scope": "read:user user:email",
                    "state": state,
                    "code_challenge": code_challenge,
                    "code_challenge_method": "S256",
                }
            )
            return f"https://github.com/login/oauth/authorize?{params}"
        if prov == "GOOGLE":
            params = urlencode(
                {
                    "client_id": client_id,
                    "redirect_uri": callback_url,
                    "response_type": "code",
                    "scope": "openid email profile",
                    "state": state,
                    "code_challenge": code_challenge,
                    "code_challenge_method": "S256",
                    "access_type": "online",
                }
            )
            return f"https://accounts.google.com/o/oauth2/v2/auth?{params}"
        raise ValueError(f"Unsupported OAuth provider: {provider}")

    def exchange_code_for_user(
        self,
        provider: str,
        code: str,
        code_verifier: str,
        callback_url: str,
    ) -> OAuthUserInfo:
        prov = provider.upper()
        client_id, client_secret = self._credentials(prov)
        with httpx.Client(timeout=self.timeout_seconds) as http:
            if prov == "GITHUB":
                tok = http.post(
                    "https://github.com/login/oauth/access_token",
                    headers={"Accept": "application/json", "User-Agent": "TechEnglish-API/3.1"},
                    data={
                        "client_id": client_id,
                        "client_secret": client_secret,
                        "code": code,
                        "redirect_uri": callback_url,
                        "code_verifier": code_verifier,
                    },
                )
                tok.raise_for_status()
                tok_json = tok.json() if tok.content else {}
                if not isinstance(tok_json, dict):
                    raise ValueError("GitHub token response is not a JSON object")
                access_token = tok_json.get("access_token")
                if not access_token or not isinstance(access_token, str):
                    err_hint = tok_json.get("error") or "missing_access_token"
                    raise ValueError(f"GitHub token exchange failed ({err_hint})")
                auth_headers = {
                    "Authorization": f"Bearer {access_token}",
                    "Accept": "application/vnd.github+json",
                    "User-Agent": "TechEnglish-API/3.1",
                    "X-GitHub-Api-Version": "2022-11-28",
                }
                profile = http.get("https://api.github.com/user", headers=auth_headers)
                profile.raise_for_status()
                pdata = profile.json()
                if not isinstance(pdata, dict):
                    raise ValueError("GitHub profile response is invalid")
                raw_id = pdata.get("id")
                sub_val = str(raw_id).strip() if raw_id is not None else ""
                if not sub_val:
                    raise ValueError("GitHub profile is missing user id")

                email: Optional[str] = None
                email_verified = False
                try:
                    emails_resp = http.get("https://api.github.com/user/emails", headers=auth_headers)
                except httpx.HTTPError:
                    emails_resp = None

                if emails_resp is not None and emails_resp.status_code == 200:
                    try:
                        emails_payload = emails_resp.json()
                    except ValueError:
                        emails_payload = []
                    if isinstance(emails_payload, list):
                        valid_entries = [
                            e for e in emails_payload
                            if isinstance(e, dict) and isinstance(e.get("email"), str) and e.get("email").strip()
                        ]
                        verified_primary = [e for e in valid_entries if e.get("primary") and e.get("verified")]
                        verified_any = [e for e in valid_entries if e.get("verified")]
                        chosen = (verified_primary or verified_any or [None])[0]
                        if chosen:
                            email = chosen["email"].strip().lower()
                            email_verified = bool(chosen.get("verified"))

                login_raw = pdata.get("login")
                username_val = (
                    login_raw.strip()
                    if isinstance(login_raw, str) and login_raw.strip()
                    else f"gh_{sub_val[:12]}"
                )
                avatar_raw = pdata.get("avatar_url")
                avatar_val = (
                    avatar_raw.strip()
                    if isinstance(avatar_raw, str) and avatar_raw.strip()
                    else "preset_terminal"
                )
                name_raw = pdata.get("name")
                display_name_val = (
                    name_raw.strip()[:120]
                    if isinstance(name_raw, str) and name_raw.strip()
                    else None
                )
                return OAuthUserInfo(
                    provider="GITHUB",
                    sub=sub_val,
                    email=email,
                    email_verified=email_verified,
                    username=username_val,
                    avatar_url=avatar_val,
                    display_name=display_name_val,
                )

            if prov == "GOOGLE":
                tok = http.post(
                    "https://oauth2.googleapis.com/token",
                    data={
                        "client_id": client_id,
                        "client_secret": client_secret,
                        "code": code,
                        "redirect_uri": callback_url,
                        "code_verifier": code_verifier,
                        "grant_type": "authorization_code",
                    },
                )
                tok.raise_for_status()
                tok_json = tok.json() if tok.content else {}
                if not isinstance(tok_json, dict):
                    raise ValueError("Google token response is not a JSON object")
                access_token = tok_json.get("access_token")
                if not access_token or not isinstance(access_token, str):
                    err_hint = tok_json.get("error") or "missing_access_token"
                    raise ValueError(f"Google token exchange failed ({err_hint})")
                info_resp = http.get(
                    "https://openidconnect.googleapis.com/v1/userinfo",
                    headers={"Authorization": f"Bearer {access_token}"},
                )
                info_resp.raise_for_status()
                idata = info_resp.json()
                if not isinstance(idata, dict):
                    raise ValueError("Google userinfo response is invalid")
                raw_sub = idata.get("sub") or idata.get("id")
                sub_val = str(raw_sub).strip() if raw_sub is not None else ""
                if not sub_val:
                    raise ValueError("Google userinfo is missing sub")

                raw_email = idata.get("email")
                email_val = (
                    raw_email.strip().lower()
                    if isinstance(raw_email, str) and raw_email.strip()
                    else None
                )
                raw_ev = idata.get("email_verified")
                email_verified = raw_ev is True or (
                    isinstance(raw_ev, str) and raw_ev.strip().lower() == "true"
                )
                email_prefix = email_val.split("@")[0] if email_val and "@" in email_val else None
                given_raw = idata.get("given_name")
                fallback_handle = (
                    given_raw.strip()
                    if isinstance(given_raw, str) and given_raw.strip()
                    else f"gg_{sub_val[:10]}"
                )
                username_val = email_prefix or fallback_handle
                pic_raw = idata.get("picture")
                avatar_val = (
                    pic_raw.strip()
                    if isinstance(pic_raw, str) and pic_raw.strip()
                    else "preset_terminal"
                )
                name_raw = idata.get("name") or given_raw
                display_name_val = (
                    name_raw.strip()[:120]
                    if isinstance(name_raw, str) and name_raw.strip()
                    else None
                )
                return OAuthUserInfo(
                    provider="GOOGLE",
                    sub=sub_val,
                    email=email_val,
                    email_verified=email_verified,
                    username=username_val,
                    avatar_url=avatar_val,
                    display_name=display_name_val,
                )
        raise ValueError(f"Unsupported OAuth provider: {provider}")


class DevLoopbackOAuthProvider:
    """
    Local development / test loopback OAuth provider (referenced in OWNER_ACTIONS.md).
    Forbidden in ENV=prod. Never trusts client-supplied provider_email; resolves synthetic dev codes only.
    """

    is_fake: bool = True

    def build_authorize_url(self, provider: str, state: str, code_challenge: str, callback_url: str) -> str:
        params = urlencode(
            {
                "state": state,
                "code": f"dev_loopback_{provider.lower()}_code",
                "code_challenge": code_challenge,
            }
        )
        return f"{callback_url}?{params}"

    def exchange_code_for_user(
        self,
        provider: str,
        code: str,
        code_verifier: str,
        callback_url: str,
    ) -> OAuthUserInfo:
        if is_deployed_env():
            raise RuntimeError("DevLoopbackOAuthProvider is forbidden in deployed environments")
        if not code or not code.startswith("dev_loopback_"):
            raise ValueError("Invalid dev loopback authorization code")
        prov = provider.upper()
        digest = hashlib.sha256(f"{prov}:{code}".encode("utf-8")).hexdigest()[:12]
        return OAuthUserInfo(
            provider=prov,
            sub=f"dev-{prov.lower()}-{digest}",
            email=f"dev_{prov.lower()}_{digest}@loopback.local",
            email_verified=True,
            username=f"dev_{digest[:8]}",
            avatar_url="https://avatars.githubusercontent.com/u/42" if prov == "GITHUB" else "preset_terminal",
        )


class OAuthFlowStore:
    """Redis-backed single-use store for server OAuth state/PKCE and short-lived login tickets."""

    STATE_TTL_MS = 10 * 60 * 1000
    TICKET_TTL_MS = 120 * 1000
    STATE_PREFIX = "oauth_state:"
    TICKET_PREFIX = "oauth_ticket:"

    def _redis(self):
        from app.redis_client import get_redis_client

        return get_redis_client()

    def _pop_json(self, key: str) -> Optional[Dict[str, object]]:
        r = self._redis()
        if hasattr(r, "getdel"):
            raw = r.getdel(key)
        else:
            pipe = r.pipeline()
            pipe.get(key)
            pipe.delete(key)
            raw, _ = pipe.execute()
        if not raw:
            return None
        try:
            data = json.loads(raw)
            return data if isinstance(data, dict) else None
        except (ValueError, TypeError):
            return None

    def clear(self) -> None:
        r = self._redis()
        for prefix in (self.STATE_PREFIX, self.TICKET_PREFIX):
            keys = list(r.keys(f"{prefix}*"))
            if keys:
                r.delete(*keys)

    def create_state(
        self,
        provider: str,
        redirect_uri: str,
        client_state: Optional[str] = None,
    ) -> tuple[str, str, str]:
        now = int(time.time() * 1000)
        state = secrets.token_urlsafe(32)
        code_verifier = secrets.token_urlsafe(48)
        code_challenge = compute_pkce_s256_challenge(code_verifier)
        record = {
            "provider": provider.upper(),
            "code_verifier": code_verifier,
            "redirect_uri": redirect_uri,
            "client_state": client_state,
            "expires_at_ms": now + self.STATE_TTL_MS,
        }
        self._redis().set(
            f"{self.STATE_PREFIX}{state}",
            json.dumps(record),
            px=self.STATE_TTL_MS,
        )
        return state, code_verifier, code_challenge

    def consume_state(self, state: str, expected_provider: str) -> Optional[Dict[str, object]]:
        record = self._pop_json(f"{self.STATE_PREFIX}{state}")
        if not record:
            return None
        now = int(time.time() * 1000)
        if record.get("provider") != expected_provider.upper() or now > int(record.get("expires_at_ms", 0)):
            return None
        return record

    def issue_ticket(self, user_id: str) -> str:
        now = int(time.time() * 1000)
        raw_ticket = secrets.token_urlsafe(32)
        ticket_hash = hash_action_token(raw_ticket)
        record = {
            "user_id": user_id,
            "expires_at_ms": now + self.TICKET_TTL_MS,
        }
        self._redis().set(
            f"{self.TICKET_PREFIX}{ticket_hash}",
            json.dumps(record),
            px=self.TICKET_TTL_MS,
        )
        return raw_ticket

    def consume_ticket(self, raw_ticket: str) -> Optional[str]:
        if not raw_ticket:
            return None
        ticket_hash = hash_action_token(raw_ticket.strip())
        record = self._pop_json(f"{self.TICKET_PREFIX}{ticket_hash}")
        if not record:
            return None
        now = int(time.time() * 1000)
        if now > int(record.get("expires_at_ms", 0)):
            return None
        return str(record["user_id"])


oauth_flow_store = OAuthFlowStore()
_http_oauth_provider = HttpOAuthProviderClient()
_dev_oauth_provider = DevLoopbackOAuthProvider()


def has_real_oauth_credentials(provider: Optional[str] = None) -> bool:
    gh_ok = bool(os.getenv("GITHUB_CLIENT_ID", "").strip() and os.getenv("GITHUB_CLIENT_SECRET", "").strip())
    gg_ok = bool(os.getenv("GOOGLE_CLIENT_ID", "").strip() and os.getenv("GOOGLE_CLIENT_SECRET", "").strip())
    if provider:
        p = provider.upper()
        return gh_ok if p == "GITHUB" else (gg_ok if p == "GOOGLE" else False)
    return gh_ok or gg_ok


def get_oauth_provider() -> OAuthProviderClient:
    if has_real_oauth_credentials() or is_deployed_env():
        return _http_oauth_provider
    return _dev_oauth_provider
