from __future__ import annotations

import sys
from pathlib import Path

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

BACKEND_DIR = Path(__file__).resolve().parents[1]
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

from app.deps import get_db
from app.main import app
from app.models import Base


def test_delete_request_accepts_html_form_and_validates_email(tmp_path):
    engine = create_engine(f"sqlite:///{tmp_path / 'gap02.db'}", connect_args={"check_same_thread": False})
    Base.metadata.create_all(bind=engine)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

    def _override_db():
        db = SessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = _override_db
    try:
        client = TestClient(app)

        ok_resp = client.post(
            "/v1/account/delete-request",
            data={"email": "developer@example.com"},
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        assert ok_resp.status_code == 200
        assert "text/html" in ok_resp.headers.get("content-type", "")
        assert "Запрос" in ok_resp.text or "удален" in ok_resp.text.lower()

        bad_resp = client.post(
            "/v1/account/delete-request",
            data={"email": "not-an-email"},
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        assert bad_resp.status_code == 422
    finally:
        app.dependency_overrides.pop(get_db, None)


def test_public_html_privacy_and_terms_routes():
    client = TestClient(app)

    privacy = client.get("/privacy")
    assert privacy.status_code == 200
    assert "text/html" in privacy.headers.get("content-type", "")
    assert "TechEnglish" in privacy.text
    assert "/v1/account/delete-form" in privacy.text

    terms = client.get("/terms")
    assert terms.status_code == 200
    assert "text/html" in terms.headers.get("content-type", "")
    assert "TechEnglish" in terms.text
    assert "16+" in terms.text
