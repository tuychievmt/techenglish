from __future__ import annotations

import json
import os
import sys
from pathlib import Path

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


def _register(client: TestClient, email: str, username: str):
    res = client.post(
        "/v1/auth/register",
        json={
            "email": email,
            "password": "StrongPassword123!",
            "username": username,
            "age_confirmed_16_plus": True,
        },
    )
    assert res.status_code == 201, res.text
    d = res.json()
    return d["user_id"], {"Authorization": f"Bearer {d['access_token']}"}


def test_friend_ping_dispatches_push_via_gateway_and_respects_opt_out_and_24h_cooldown():
    from app.services.push_gateway import FakePushNotificationGateway, get_push_gateway

    fake_gateway = FakePushNotificationGateway()
    app.dependency_overrides[get_push_gateway] = lambda: fake_gateway
    try:
        client = TestClient(app)
        uid_a, hdr_a = _register(client, "ping_sender@example.com", "ping_sender")
        uid_b, hdr_b = _register(client, "ping_recv@example.com", "ping_recv")

        # Register receiver FCM token
        reg = client.post(
            "/v1/notifications/device-token",
            headers=hdr_b,
            json={"token": "fcm_device_token_receiver_1234567890", "platform": "ANDROID"},
        )
        assert reg.status_code == 200, reg.text
        assert reg.json()["registered"] is True

        # Ping succeeds and dispatches push notification through gateway
        ping1 = client.post(f"/v1/friends/{uid_b}/ping", headers=hdr_a)
        assert ping1.status_code == 200, ping1.text
        body = ping1.json()
        assert body["pinged"] is True
        assert body["push_dispatched"] is True
        assert body["push_devices_notified"] == 1
        assert len(fake_gateway.sent_messages) == 1
        sent = fake_gateway.sent_messages[0]
        assert sent.token == "fcm_device_token_receiver_1234567890"
        assert sent.data.get("type") == "friend_ping"
        assert sent.data.get("sender_username") == "ping_sender"

        # Second ping within 24h is rejected (429) and MUST NOT dispatch another push
        ping2 = client.post(f"/v1/friends/{uid_b}/ping", headers=hdr_a)
        assert ping2.status_code == 429
        assert len(fake_gateway.sent_messages) == 1

        # Opt out of pings -> rejected (403) and no push
        uid_c, hdr_c = _register(client, "ping_sender2@example.com", "ping_sender2")
        patch = client.patch("/v1/profile/me", headers=hdr_b, json={"accepts_pings": False})
        assert patch.status_code == 200
        ping3 = client.post(f"/v1/friends/{uid_b}/ping", headers=hdr_c)
        assert ping3.status_code == 403
        assert len(fake_gateway.sent_messages) == 1

        # Unregister token & account delete cleanup
        del_tok = client.request(
            "DELETE",
            "/v1/notifications/device-token",
            headers=hdr_b,
            json={"token": "fcm_device_token_receiver_1234567890"},
        )
        assert del_tok.status_code == 200
        assert del_tok.json()["removed"] >= 1
    finally:
        app.dependency_overrides.pop(get_push_gateway, None)


def test_fcm_http_v1_gateway_sends_valid_payload_with_mock_transport():
    from app.services.push_gateway import FcmHttpV1PushNotificationGateway, PushMessage

    captured: dict = {}

    def _handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["auth"] = request.headers.get("Authorization")
        captured["body"] = json.loads(request.content.decode("utf-8"))
        return httpx.Response(200, json={"name": "projects/techenglish-prod/messages/12345"})

    gateway = FcmHttpV1PushNotificationGateway(
        project_id="techenglish-prod",
        access_token="oauth2_fcm_test_token",
        transport=httpx.MockTransport(_handler),
    )
    assert gateway.is_fake is False

    res = gateway.send(
        PushMessage(
            user_id="u2",
            token="fcm_tok_abc",
            title="Ping from @alice",
            body="Time to review your IT terms!",
            data={"type": "friend_ping", "destination": "social"},
        )
    )
    assert res.delivered is True
    assert res.provider == "fcm_v1"
    assert res.is_fake is False
    assert captured["url"] == "https://fcm.googleapis.com/v1/projects/techenglish-prod/messages:send"
    assert captured["auth"] == "Bearer oauth2_fcm_test_token"
    assert captured["body"]["message"]["token"] == "fcm_tok_abc"
    assert captured["body"]["message"]["notification"]["title"] == "Ping from @alice"
    assert captured["body"]["message"]["data"]["type"] == "friend_ping"
