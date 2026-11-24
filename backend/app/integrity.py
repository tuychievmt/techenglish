from __future__ import annotations

import hmac
import os
import time
from dataclasses import dataclass
from typing import Any, Callable, Dict, List, Optional, Protocol, Set

import httpx

from app.config import is_prod_env

DEFAULT_PACKAGE_NAME = "com.techenglish.app"
DEFAULT_MAX_TOKEN_AGE_MS = 5 * 60 * 1000
VALID_DEVICE_VERDICTS = {"MEETS_DEVICE_INTEGRITY", "MEETS_STRONG_INTEGRITY"}


@dataclass(frozen=True)
class IntegrityVerificationResult:
    is_valid: bool
    reason: str
    app_recognition: Optional[str] = None
    device_recognition: Optional[List[str]] = None


class ClientIntegrityVerifier(Protocol):
    is_fake: bool

    def verify(
        self,
        token: Optional[str],
        expected_nonce: Optional[str] = None,
    ) -> IntegrityVerificationResult: ...


def evaluate_play_integrity_verdict(
    payload: Dict[str, Any],
    expected_package_name: str,
    allowed_cert_digests: Set[str],
    expected_nonce: Optional[str] = None,
    now_ms: Optional[int] = None,
    max_age_ms: int = DEFAULT_MAX_TOKEN_AGE_MS,
) -> IntegrityVerificationResult:
    """
    Validates a decoded Google Play Integrity API tokenPayloadExternal verdict:
    - requestDetails: package name, freshness timestamp, and optional nonce
    - appIntegrity: PLAY_RECOGNIZED, matching package, matching signing certificate SHA-256 digest
    - deviceIntegrity: MEETS_DEVICE_INTEGRITY or MEETS_STRONG_INTEGRITY
    - accountDetails: rejects UNLICENSED
    """
    ext = payload.get("tokenPayloadExternal") if isinstance(payload, dict) else None
    if not isinstance(ext, dict):
        return IntegrityVerificationResult(is_valid=False, reason="missing_token_payload_external")

    req = ext.get("requestDetails") or {}
    app_int = ext.get("appIntegrity") or {}
    dev_int = ext.get("deviceIntegrity") or {}
    acct = ext.get("accountDetails") or {}

    req_pkg = str(req.get("requestPackageName") or "")
    if req_pkg != expected_package_name:
        return IntegrityVerificationResult(is_valid=False, reason=f"package_mismatch:{req_pkg}")

    try:
        ts_ms = int(req.get("timestampMillis", 0))
    except (TypeError, ValueError):
        return IntegrityVerificationResult(is_valid=False, reason="invalid_timestamp")
    current_ms = now_ms if now_ms is not None else int(time.time() * 1000)
    if ts_ms <= 0 or abs(current_ms - ts_ms) > max_age_ms:
        return IntegrityVerificationResult(is_valid=False, reason="token_expired_or_clock_skew")

    if expected_nonce is not None:
        actual_nonce = str(req.get("nonce") or "")
        if not actual_nonce or not hmac.compare_digest(actual_nonce, expected_nonce):
            return IntegrityVerificationResult(is_valid=False, reason="nonce_mismatch")

    app_verdict = str(app_int.get("appRecognitionVerdict") or "")
    app_pkg = str(app_int.get("packageName") or "")
    if app_verdict != "PLAY_RECOGNIZED":
        return IntegrityVerificationResult(
            is_valid=False,
            reason=f"app_not_play_recognized:{app_verdict}",
            app_recognition=app_verdict,
        )
    if app_pkg != expected_package_name:
        return IntegrityVerificationResult(
            is_valid=False,
            reason=f"app_package_mismatch:{app_pkg}",
            app_recognition=app_verdict,
        )

    certs = {str(c).strip() for c in (app_int.get("certificateSha256Digest") or []) if str(c).strip()}
    if not allowed_cert_digests or not (certs & allowed_cert_digests):
        return IntegrityVerificationResult(
            is_valid=False,
            reason="certificate_digest_mismatch",
            app_recognition=app_verdict,
        )

    dev_verdicts = [str(v) for v in (dev_int.get("deviceRecognitionVerdict") or [])]
    if not (set(dev_verdicts) & VALID_DEVICE_VERDICTS):
        return IntegrityVerificationResult(
            is_valid=False,
            reason="device_integrity_failed",
            app_recognition=app_verdict,
            device_recognition=dev_verdicts,
        )

    licensing = str(acct.get("appLicensingVerdict") or "")
    if licensing == "UNLICENSED":
        return IntegrityVerificationResult(
            is_valid=False,
            reason="app_unlicensed",
            app_recognition=app_verdict,
            device_recognition=dev_verdicts,
        )

    return IntegrityVerificationResult(
        is_valid=True,
        reason="ok",
        app_recognition=app_verdict,
        device_recognition=dev_verdicts,
    )


class GooglePlayIntegrityVerifier:
    """
    Production Play Integrity API server verifier (SP1-11 / R3 / BUG-07).
    Decodes client integrityToken via Google Play Integrity API:
      POST https://playintegrity.googleapis.com/v1/{package_name}:decodeIntegrityToken
    """

    is_fake: bool = False

    def __init__(
        self,
        package_name: str = DEFAULT_PACKAGE_NAME,
        allowed_cert_digests: Optional[Set[str]] = None,
        api_access_token: str = "",
        timeout_seconds: float = 8.0,
        decode_fn: Optional[Callable[[str], Dict[str, Any]]] = None,
    ) -> None:
        self.package_name = package_name
        self.allowed_cert_digests = set(allowed_cert_digests or set())
        self.api_access_token = api_access_token
        self.timeout_seconds = timeout_seconds
        self._decode_fn = decode_fn

    def _decode_via_google_api(self, integrity_token: str) -> Dict[str, Any]:
        if not self.api_access_token:
            raise RuntimeError("PLAY_INTEGRITY_API_ACCESS_TOKEN is not configured")
        url = f"https://playintegrity.googleapis.com/v1/{self.package_name}:decodeIntegrityToken"
        with httpx.Client(timeout=self.timeout_seconds) as client:
            resp = client.post(
                url,
                headers={
                    "Authorization": f"Bearer {self.api_access_token}",
                    "Content-Type": "application/json",
                },
                json={"integrityToken": integrity_token},
            )
            resp.raise_for_status()
            return resp.json()

    def verify(
        self,
        token: Optional[str],
        expected_nonce: Optional[str] = None,
    ) -> IntegrityVerificationResult:
        if not token or not token.strip():
            return IntegrityVerificationResult(is_valid=False, reason="missing_integrity_token")
        try:
            decoded = (
                self._decode_fn(token.strip())
                if self._decode_fn is not None
                else self._decode_via_google_api(token.strip())
            )
        except Exception as exc:
            return IntegrityVerificationResult(is_valid=False, reason=f"decode_failed:{exc}")

        return evaluate_play_integrity_verdict(
            payload=decoded,
            expected_package_name=self.package_name,
            allowed_cert_digests=self.allowed_cert_digests,
            expected_nonce=expected_nonce,
        )


class DevAttestationStub:
    """Dev/test stub verifier accepted ONLY when ENV!=prod and PLAY_INTEGRITY_STRICT=false."""

    is_fake: bool = True

    def verify(
        self,
        token: Optional[str],
        expected_nonce: Optional[str] = None,
    ) -> IntegrityVerificationResult:
        if is_prod_env():
            return IntegrityVerificationResult(is_valid=False, reason="fake_verifier_forbidden_in_prod")
        return IntegrityVerificationResult(is_valid=True, reason="dev_stub_ok")


def _parse_cert_digests_env() -> Set[str]:
    raw = os.getenv("PLAY_INTEGRITY_CERT_SHA256_DIGESTS", "").strip()
    return {item.strip() for item in raw.split(",") if item.strip()}


def is_play_integrity_strict() -> bool:
    return os.getenv("PLAY_INTEGRITY_STRICT", "false").strip().lower() == "true"


def get_integrity_verifier() -> ClientIntegrityVerifier:
    if is_prod_env() or is_play_integrity_strict():
        return GooglePlayIntegrityVerifier(
            package_name=os.getenv("PLAY_INTEGRITY_PACKAGE_NAME", DEFAULT_PACKAGE_NAME).strip() or DEFAULT_PACKAGE_NAME,
            allowed_cert_digests=_parse_cert_digests_env(),
            api_access_token=os.getenv("PLAY_INTEGRITY_API_ACCESS_TOKEN", "").strip(),
        )
    return DevAttestationStub()
