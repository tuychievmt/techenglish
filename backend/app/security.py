from __future__ import annotations

import base64
import hashlib
import hmac
import secrets
import time
from typing import Any, Dict, Optional

import jwt

from app.config import get_jwt_secret, settings


def hash_action_token(raw_token: str) -> str:
    """One-way SHA-256 hash for email verification and password reset tokens."""
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


def hash_password(password: str, salt_hex: Optional[str] = None) -> str:
    salt = bytes.fromhex(salt_hex) if salt_hex else secrets.token_bytes(16)
    derived = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 210_000)
    return f"pbkdf2_sha256$210000${salt.hex()}${derived.hex()}"


def verify_password(password: str, stored_hash: str) -> bool:
    try:
        scheme, iters_s, salt_hex, expected_hex = stored_hash.split("$", 3)
        if scheme != "pbkdf2_sha256":
            return False
        derived = hashlib.pbkdf2_hmac(
            "sha256",
            password.encode("utf-8"),
            bytes.fromhex(salt_hex),
            int(iters_s),
        )
        return hmac.compare_digest(derived.hex(), expected_hex)
    except Exception:
        return False


def verify_pkce_s256(code_verifier: str, code_challenge: str) -> bool:
    """RFC 7636 PKCE S256 verification for GitHub / Google OAuth Custom Tabs flow."""
    if not code_verifier or len(code_verifier) < 43 or len(code_verifier) > 128:
        return False
    digest = hashlib.sha256(code_verifier.encode("ascii")).digest()
    computed = base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")
    return hmac.compare_digest(computed, code_challenge)


def create_jwt(user_id: str, token_type: str, ttl_seconds: int) -> str:
    now = int(time.time())
    payload = {
        "sub": user_id,
        "type": token_type,
        "iat": now,
        "exp": now + ttl_seconds,
        "jti": secrets.token_hex(16),
    }
    return jwt.encode(payload, get_jwt_secret(), algorithm=settings.jwt_algorithm)


def decode_jwt(token: str, expected_type: str) -> Dict[str, Any]:
    payload = jwt.decode(
        token,
        get_jwt_secret(),
        algorithms=[settings.jwt_algorithm],
    )
    if payload.get("type") != expected_type:
        raise jwt.InvalidTokenError("Unexpected token type")
    return payload


def verify_play_integrity_token(integrity_token: Optional[str], expected_nonce: Optional[str] = None) -> bool:
    """Delegates to ClientIntegrityVerifier (never trusts raw token length)."""
    from app.integrity import get_integrity_verifier

    return get_integrity_verifier().verify(integrity_token, expected_nonce=expected_nonce).is_valid
