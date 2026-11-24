from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.deps import _AUTH_RATE_BUCKETS
from app.routers import (
    ai,
    auth,
    community,
    crowdsource,
    peer_review,
    profile_moderation,
    realtime,
    social,
    squads,
    sync,
)
from app.services.startup import run_startup_tasks, validate_production_startup_config

logger = logging.getLogger("techenglish.api")


def on_startup() -> None:
    """Backward-compatible startup hook delegating to services.startup.run_startup_tasks."""
    run_startup_tasks()


@asynccontextmanager
async def lifespan(_app: FastAPI):
    run_startup_tasks()
    yield


def _register_error_handlers(application: FastAPI) -> None:
    @application.exception_handler(StarletteHTTPException)
    async def http_exception_handler(_request: Request, exc: StarletteHTTPException):
        return JSONResponse(
            status_code=exc.status_code,
            content={
                "detail": exc.detail,
                "error": {
                    "code": f"http_{exc.status_code}",
                    "status": exc.status_code,
                    "message": str(exc.detail),
                },
            },
            headers=exc.headers,
        )

    @application.exception_handler(RequestValidationError)
    async def validation_exception_handler(_request: Request, exc: RequestValidationError):
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={
                "detail": exc.errors(),
                "error": {
                    "code": "validation_error",
                    "status": status.HTTP_422_UNPROCESSABLE_ENTITY,
                    "message": "Request validation failed",
                },
            },
        )

    @application.exception_handler(Exception)
    async def unhandled_exception_handler(_request: Request, exc: Exception):
        logger.exception("Unhandled server exception: %s", exc)
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "detail": "Internal server error",
                "error": {
                    "code": "internal_server_error",
                    "status": status.HTTP_500_INTERNAL_SERVER_ERROR,
                    "message": "Internal server error",
                },
            },
        )


def create_app() -> FastAPI:
    application = FastAPI(
        title="TechEnglish v3.1 API",
        version="3.1.0",
        description="FastAPI + PostgreSQL + Redis backend for TechEnglish",
        lifespan=lifespan,
    )
    _register_error_handlers(application)

    @application.get("/health", tags=["system"])
    def health():
        return {"status": "ok", "service": "techenglish-api", "version": "3.1.0"}

    for router_module in (
        auth,
        sync,
        profile_moderation,
        social,
        community,
        ai,
        squads,
        crowdsource,
        realtime,
        peer_review,
    ):
        application.include_router(router_module.router)

    return application


app = create_app()

__all__ = [
    "app",
    "create_app",
    "lifespan",
    "on_startup",
    "validate_production_startup_config",
    "_AUTH_RATE_BUCKETS",
]
