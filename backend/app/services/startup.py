from __future__ import annotations

import os

from app.config import get_jwt_secret, is_deployed_env, is_prod_env, is_weak_jwt_secret
from app.email_sender import get_email_sender
from app.integrity import get_integrity_verifier, is_play_integrity_strict
from app.models import SessionLocal, init_db


def validate_production_startup_config() -> None:
    """Fail-fast guard for ENV=staging and ENV=prod."""
    if not is_deployed_env():
        return
    if is_weak_jwt_secret(get_jwt_secret()):
        raise RuntimeError("Insecure JWT_SECRET in production/staging: must be non-default and >= 32 chars.")
    if get_email_sender().is_fake:
        raise RuntimeError("Fake EmailSender is forbidden in production/staging: configure SMTP_HOST.")
    if not is_prod_env():
        return
    verifier = get_integrity_verifier()
    if verifier.is_fake or not is_play_integrity_strict():
        raise RuntimeError("Fake Play Integrity verifier is forbidden in production: set PLAY_INTEGRITY_STRICT=true.")
    if not os.getenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", "").strip():
        raise RuntimeError("Play Integrity certificate SHA-256 digests must be configured in production.")
    if not (
        os.getenv("PLAY_INTEGRITY_API_ACCESS_TOKEN", "").strip()
        or os.getenv("PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON", "").strip()
    ):
        raise RuntimeError("Play Integrity API credentials must be configured in production.")
    voice_backend = os.getenv("VOICE_STORAGE_BACKEND", "").strip().lower()
    if voice_backend == "s3" and not os.getenv("VOICE_STORAGE_ENDPOINT", "").strip():
        raise RuntimeError("VOICE_STORAGE_ENDPOINT must be configured in production when VOICE_STORAGE_BACKEND=s3.")


def run_startup_tasks() -> None:
    validate_production_startup_config()
    if not is_deployed_env():
        init_db()
    if os.getenv("BOOTSTRAP_ADMIN_EMAIL", "").strip():
        from app.cli_admin import bootstrap_admin_from_env

        with SessionLocal() as db:
            bootstrap_admin_from_env(db)
