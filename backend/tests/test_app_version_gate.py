from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app


def test_get_app_version_defaults_and_env_overrides(monkeypatch):
    client = TestClient(app)

    resp_default = client.get("/v1/app/version")
    assert resp_default.status_code == 200
    body = resp_default.json()
    assert body["latest_version_code"] == 310
    assert body["min_supported_version_code"] == 310
    assert body["force_update"] is False
    assert "apk_url" in body
    assert "apk_sha256" in body
    assert "changelog_ru" in body

    monkeypatch.setenv("LATEST_VERSION_CODE", "320")
    monkeypatch.setenv("LATEST_VERSION_NAME", "3.2.0")
    monkeypatch.setenv("MIN_SUPPORTED_VERSION_CODE", "315")
    monkeypatch.setenv("FORCE_UPDATE", "true")
    monkeypatch.setenv("LATEST_APK_URL", "https://cdn.example.com/techenglish-3.2.0.apk")
    monkeypatch.setenv("LATEST_APK_SHA256", "ABCDEF0123456789")
    monkeypatch.setenv("LATEST_CHANGELOG_RU", "• Новая арена\\n• Исправления ошибок")

    resp_custom = client.get("/v1/app/version")
    assert resp_custom.status_code == 200
    custom = resp_custom.json()
    assert custom["latest_version_code"] == 320
    assert custom["latest_version_name"] == "3.2.0"
    assert custom["min_supported_version_code"] == 315
    assert custom["force_update"] is True
    assert custom["apk_url"] == "https://cdn.example.com/techenglish-3.2.0.apk"
    assert custom["apk_sha256"] == "abcdef0123456789"
    assert "\n" in custom["changelog_ru"]
