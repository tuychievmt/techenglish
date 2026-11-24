from __future__ import annotations

import sys
from pathlib import Path

from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))


def test_main_py_is_slim_uses_lifespan_and_has_at_least_8_routers():
    main_src = (BACKEND_DIR / "app" / "main.py").read_text(encoding="utf-8")
    line_count = len(main_src.splitlines())
    assert line_count < 180, f"main.py must be < 180 lines, got {line_count}"
    assert "@app.on_event" not in main_src, "main.py must use lifespan instead of deprecated @app.on_event"
    assert "lifespan" in main_src

    routers_dir = BACKEND_DIR / "app" / "routers"
    schemas_dir = BACKEND_DIR / "app" / "schemas"
    services_dir = BACKEND_DIR / "app" / "services"
    assert routers_dir.is_dir(), "Missing backend/app/routers/"
    assert schemas_dir.is_dir(), "Missing backend/app/schemas/"
    assert services_dir.is_dir(), "Missing backend/app/services/"

    router_modules = [p for p in routers_dir.glob("*.py") if p.name != "__init__.py"]
    assert len(router_modules) >= 8, f"Expected >= 8 routers, found {len(router_modules)}: {[p.name for p in router_modules]}"


def test_unified_error_handling_preserves_detail_and_error_code():
    from app.main import app

    client = TestClient(app, raise_server_exceptions=False)
    # 401/422 returns standard JSON with detail + error.code
    r_unauth = client.get("/v1/profile/me")
    assert r_unauth.status_code == 401
    body = r_unauth.json()
    assert "detail" in body
    assert body.get("error", {}).get("status") == 401

    # Validation error (422) includes detail + error.code
    r_422 = client.post("/v1/auth/login", json={})
    assert r_422.status_code == 422
    body_422 = r_422.json()
    assert "detail" in body_422
    assert body_422.get("error", {}).get("code") == "validation_error"
