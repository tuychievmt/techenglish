from __future__ import annotations

import logging
import os
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Protocol

import httpx

logger = logging.getLogger("techenglish.push")


@dataclass(frozen=True)
class PushMessage:
    user_id: str
    token: str
    title: str
    body: str
    data: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class PushDeliveryResult:
    delivered: bool
    provider: str
    is_fake: bool
    reason: str = ""


class PushNotificationGateway(Protocol):
    is_fake: bool

    def send(self, message: PushMessage) -> PushDeliveryResult: ...


class FakePushNotificationGateway:
    """In-memory push notification sender for local development and automated tests."""

    is_fake: bool = True

    def __init__(self) -> None:
        self.sent_messages: List[PushMessage] = []

    def send(self, message: PushMessage) -> PushDeliveryResult:
        self.sent_messages.append(message)
        logger.debug("FakePushNotificationGateway recorded push for user=%s", message.user_id)
        return PushDeliveryResult(delivered=True, provider="fake", is_fake=True)


class FcmHttpV1PushNotificationGateway:
    """
    Firebase Cloud Messaging HTTP v1 sender (SP2-10 / R3).
    Configured via FCM_PROJECT_ID and FCM_ACCESS_TOKEN (OAuth2 access token minted from
    Firebase service account credentials by the deployment environment).
    """

    is_fake: bool = False

    def __init__(
        self,
        project_id: str,
        access_token: str,
        timeout_seconds: float = 5.0,
        transport: Optional[httpx.BaseTransport] = None,
    ) -> None:
        self.project_id = project_id.strip()
        self.access_token = access_token.strip()
        self.timeout_seconds = timeout_seconds
        self._transport = transport

    @property
    def endpoint(self) -> str:
        return f"https://fcm.googleapis.com/v1/projects/{self.project_id}/messages:send"

    def send(self, message: PushMessage) -> PushDeliveryResult:
        if not self.project_id or not self.access_token:
            return PushDeliveryResult(
                delivered=False,
                provider="fcm_v1",
                is_fake=False,
                reason="missing_fcm_credentials",
            )
        payload = {
            "message": {
                "token": message.token,
                "notification": {
                    "title": message.title,
                    "body": message.body,
                },
                "data": {str(k): str(v) for k, v in message.data.items()},
                "android": {
                    "priority": "high",
                },
            }
        }
        headers = {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json",
        }
        try:
            with httpx.Client(timeout=self.timeout_seconds, transport=self._transport) as client:
                resp = client.post(self.endpoint, headers=headers, json=payload)
            if 200 <= resp.status_code < 300:
                return PushDeliveryResult(delivered=True, provider="fcm_v1", is_fake=False)
            logger.warning("FCM HTTP v1 delivery failed: status=%s", resp.status_code)
            return PushDeliveryResult(
                delivered=False,
                provider="fcm_v1",
                is_fake=False,
                reason=f"fcm_http_{resp.status_code}",
            )
        except Exception as exc:
            logger.warning("FCM HTTP v1 transport error: %s", exc)
            return PushDeliveryResult(
                delivered=False,
                provider="fcm_v1",
                is_fake=False,
                reason="fcm_network_error",
            )


_default_fake_gateway = FakePushNotificationGateway()


def is_fcm_configured() -> bool:
    return bool(os.getenv("FCM_PROJECT_ID", "").strip() and os.getenv("FCM_ACCESS_TOKEN", "").strip())


def get_push_gateway() -> PushNotificationGateway:
    project_id = os.getenv("FCM_PROJECT_ID", "").strip()
    access_token = os.getenv("FCM_ACCESS_TOKEN", "").strip()
    if project_id and access_token:
        return FcmHttpV1PushNotificationGateway(project_id=project_id, access_token=access_token)
    return _default_fake_gateway
