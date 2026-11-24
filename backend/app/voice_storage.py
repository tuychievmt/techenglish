from __future__ import annotations

import base64
import os
import uuid
from pathlib import Path
from typing import Any, Optional, Protocol

import boto3
from botocore.config import Config as BotoConfig
from botocore.exceptions import ClientError

PEER_REVIEW_VOICE_TTL_DAYS = int(os.getenv("PEER_REVIEW_VOICE_TTL_DAYS", "30"))
MAX_VOICE_SECONDS = 20
MAX_VOICE_BYTES = 512 * 1024
ALLOWED_VOICE_CONTENT_TYPES = {
    "audio/mp4",
    "audio/ogg",
    "audio/webm",
    "audio/aac",
    "audio/mpeg",
}

_DEFAULT_LOCAL_DIR = Path(__file__).resolve().parent.parent / ".voice_objects"


def _decode_and_validate_voice_payload(payload_base64: str, content_type: str) -> tuple[bytes, str]:
    clean_ct = (content_type or "audio/mp4").strip().lower()
    if clean_ct not in ALLOWED_VOICE_CONTENT_TYPES:
        raise ValueError(f"Unsupported audio content type '{clean_ct}'.")
    raw = base64.b64decode(payload_base64, validate=True)
    if not raw or len(raw) > MAX_VOICE_BYTES:
        raise ValueError("Voice clip payload must be between 1 byte and 512 KB.")
    ext_map = {
        "audio/mp4": "m4a",
        "audio/ogg": "ogg",
        "audio/webm": "webm",
        "audio/aac": "aac",
        "audio/mpeg": "mp3",
    }
    return raw, ext_map[clean_ct]


class VoiceStorageBackend(Protocol):
    is_fake: bool

    def put_voice_clip(self, user_id: str, payload_base64: str, content_type: str = "audio/mp4") -> str: ...

    def exists(self, object_key: str) -> bool: ...

    def delete_object(self, object_key: str) -> bool: ...

    def generate_presigned_get_url(self, object_key: str, expires_in_seconds: int = 300) -> str: ...


class S3VoiceObjectStorage:
    """
    Production S3-compatible (MinIO / AWS S3) voice storage backend (Errata E5 / DEC-13).
    Configures 30-day bucket lifecycle expiration, validates <= 512 KB payloads,
    issues short-lived presigned GET URLs, and supports synchronous hard deletion.
    """

    is_fake: bool = False

    def __init__(
        self,
        bucket: str,
        ttl_days: int = PEER_REVIEW_VOICE_TTL_DAYS,
        endpoint_url: Optional[str] = None,
        access_key: Optional[str] = None,
        secret_key: Optional[str] = None,
        region_name: str = "us-east-1",
        s3_client: Optional[Any] = None,
        skip_bucket_init: Optional[bool] = None,
    ) -> None:
        self.bucket = bucket
        self.ttl_days = ttl_days
        if skip_bucket_init is None:
            self.skip_bucket_init = os.getenv("VOICE_STORAGE_SKIP_BUCKET_INIT", "false").strip().lower() in {
                "1",
                "true",
                "yes",
            }
        else:
            self.skip_bucket_init = bool(skip_bucket_init)
        if s3_client is not None:
            self.client = s3_client
        else:
            self.client = boto3.client(
                "s3",
                endpoint_url=endpoint_url or None,
                aws_access_key_id=access_key or "techenglish_minio",
                aws_secret_access_key=secret_key or "change_me_minio_prod",
                region_name=region_name,
                config=BotoConfig(signature_version="s3v4", s3={"addressing_style": "path"}),
            )
        self._initialized = False

    def ensure_bucket_and_lifecycle(self) -> None:
        if self.skip_bucket_init:
            self._initialized = True
            return
        try:
            self.client.head_bucket(Bucket=self.bucket)
        except ClientError:
            self.client.create_bucket(Bucket=self.bucket)

        try:
            self.client.put_bucket_lifecycle_configuration(
                Bucket=self.bucket,
                LifecycleConfiguration={
                    "Rules": [
                        {
                            "ID": "peer-review-voice-ttl",
                            "Status": "Enabled",
                            "Filter": {"Prefix": ""},
                            "Expiration": {"Days": int(self.ttl_days)},
                        }
                    ]
                },
            )
        except ClientError:
            pass
        self._initialized = True

    def _ensure_ready(self) -> None:
        if not self._initialized:
            self.ensure_bucket_and_lifecycle()

    def put_voice_clip(self, user_id: str, payload_base64: str, content_type: str = "audio/mp4") -> str:
        self._ensure_ready()
        raw, ext = _decode_and_validate_voice_payload(payload_base64, content_type)
        object_key = f"{user_id}/{uuid.uuid4().hex}.{ext}"
        self.client.put_object(
            Bucket=self.bucket,
            Key=object_key,
            Body=raw,
            ContentType=content_type,
            Metadata={"ttl_days": str(self.ttl_days)},
        )
        return object_key

    def exists(self, object_key: str) -> bool:
        if not object_key:
            return False
        try:
            self.client.head_object(Bucket=self.bucket, Key=object_key)
            return True
        except ClientError:
            return False

    def delete_object(self, object_key: str) -> bool:
        if not object_key:
            return False
        existed = self.exists(object_key)
        if not existed:
            return False
        self.client.delete_object(Bucket=self.bucket, Key=object_key)
        return True

    def generate_presigned_get_url(self, object_key: str, expires_in_seconds: int = 300) -> str:
        return str(
            self.client.generate_presigned_url(
                ClientMethod="get_object",
                Params={"Bucket": self.bucket, "Key": object_key},
                ExpiresIn=expires_in_seconds,
            )
        )


class VoiceObjectStorage:
    """
    Local filesystem dev/test fallback storage when VOICE_STORAGE_ENDPOINT is not configured.
    Forbidden in ENV=prod (must configure S3/MinIO via VOICE_STORAGE_ENDPOINT).
    """

    is_fake: bool = True

    def __init__(self, base_dir: Optional[Path] = None):
        env_dir = os.getenv("VOICE_STORAGE_LOCAL_DIR")
        self.base_dir = Path(env_dir) if env_dir else (base_dir or _DEFAULT_LOCAL_DIR)
        self.base_dir.mkdir(parents=True, exist_ok=True)

    def put_voice_clip(self, user_id: str, payload_base64: str, content_type: str = "audio/mp4") -> str:
        raw, ext = _decode_and_validate_voice_payload(payload_base64, content_type)
        object_key = f"{user_id}/{uuid.uuid4().hex}.{ext}"
        target = self.base_dir / object_key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(raw)
        return object_key

    def exists(self, object_key: str) -> bool:
        if not object_key:
            return False
        return (self.base_dir / object_key).exists()

    def delete_object(self, object_key: str) -> bool:
        if not object_key:
            return False
        target = self.base_dir / object_key
        if target.exists():
            target.unlink()
            return True
        return False

    def generate_presigned_get_url(self, object_key: str, expires_in_seconds: int = 300) -> str:
        return f"local://voice/{object_key}?expires_in={expires_in_seconds}"


voice_storage = VoiceObjectStorage()
_cached_s3_storage: Optional[S3VoiceObjectStorage] = None
_cached_s3_endpoint: str = ""


def get_voice_storage() -> VoiceStorageBackend:
    global _cached_s3_storage, _cached_s3_endpoint
    endpoint = os.getenv("VOICE_STORAGE_ENDPOINT", "").strip()
    if endpoint:
        if _cached_s3_storage is None or _cached_s3_endpoint != endpoint:
            _cached_s3_storage = S3VoiceObjectStorage(
                bucket=os.getenv("VOICE_STORAGE_BUCKET", "techenglish-peer-voice").strip(),
                ttl_days=int(os.getenv("PEER_REVIEW_VOICE_TTL_DAYS", str(PEER_REVIEW_VOICE_TTL_DAYS))),
                endpoint_url=endpoint,
                access_key=os.getenv("VOICE_STORAGE_ACCESS_KEY", os.getenv("MINIO_ROOT_USER", "techenglish_minio")),
                secret_key=os.getenv("VOICE_STORAGE_SECRET_KEY", os.getenv("MINIO_ROOT_PASSWORD", "change_me_minio_prod")),
                region_name=os.getenv("VOICE_STORAGE_REGION", "us-east-1"),
            )
            _cached_s3_endpoint = endpoint
        return _cached_s3_storage
    return voice_storage
