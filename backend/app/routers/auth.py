from __future__ import annotations

import hmac
import html
import json
import logging
import secrets
import uuid
from typing import Optional
from urllib.parse import parse_qs, urlencode

import jwt
from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from fastapi.responses import HTMLResponse, RedirectResponse
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.anti_cheat import is_avatar_allowed, sanitize_oauth_username, validate_username
from app.config import get_public_base_url
from app.deps import (
    ACTION_TOKEN_TTL_MS,
    MAX_TOKEN_ATTEMPTS,
    check_auth_rate_limit,
    get_current_user,
    get_db,
    now_ms,
)
from app.email_sender import EmailSender, get_email_sender
from app.integrity import ClientIntegrityVerifier, get_integrity_verifier
from app.models import (
    ActivityFeedReactionRow,
    ActivityFeedRow,
    AiUsageLogRow,
    CommentRow,
    CommentVoteRow,
    CommunityDeckRatingRow,
    CommunityDeckRow,
    CommunityDeckTermRow,
    DevicePushTokenRow,
    DictionaryPrRow,
    DictionaryPrVoteRow,
    FriendPingRow,
    FriendshipRow,
    ModerationReportRow,
    OAuthIdentityRow,
    PeerReviewFeedbackRow,
    PeerReviewSubmissionRow,
    PvpMatchRow,
    RoomMessageRow,
    Sm2StateRow,
    SquadMemberRow,
    SyncEventRow,
    UserBlockRow,
    UserCustomWordRow,
    UserFavoriteRow,
    UserRow,
)
from app.oauth_provider import (
    OAuthProviderClient,
    get_allowed_redirect_uris,
    get_oauth_provider,
    oauth_flow_store,
)
from app.schemas.auth import (
    LoginRequest,
    OAuthTicketExchangeRequest,
    PasswordResetConfirmRequest,
    RefreshRequest,
    RegisterRequest,
)
from app.schemas.sync import SyncBatchRequest
from app.security import decode_jwt, hash_action_token, hash_password, verify_password
from app.services.sync_service import _apply_sync_batch, _issue_tokens
from app.voice_storage import PEER_REVIEW_VOICE_TTL_DAYS, VoiceStorageBackend, get_voice_storage

router = APIRouter(tags=['auth'])


@router.post("/v1/auth/register", status_code=status.HTTP_201_CREATED)
def register(
    body: RegisterRequest,
    db: Session = Depends(get_db),
    email_sender: EmailSender = Depends(get_email_sender),
):
    if not body.age_confirmed_16_plus:
        raise HTTPException(status_code=400, detail="Age 16+ confirmation is required (ФТ-9.4)")
    try:
        clean_username = validate_username(body.username)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))

    clean_email = body.email.strip().lower()
    if db.query(UserRow).filter((UserRow.email == clean_email) | (UserRow.username == clean_username)).first():
        raise HTTPException(status_code=409, detail="Email or username already exists")

    ts = now_ms()
    raw_verify_token = secrets.token_urlsafe(32)
    user = UserRow(
        id=str(uuid.uuid4()),
        username=clean_username,
        email=clean_email,
        password_hash=hash_password(body.password),
        provider="EMAIL",
        is_email_verified=False,
        email_verify_token=hash_action_token(raw_verify_token),
        email_verify_expires_at_ms=ts + ACTION_TOKEN_TTL_MS,
        display_name=body.display_name.strip() or clean_username,
        created_at_ms=ts,
    )
    db.add(user)
    db.commit()
    email_sender.send_verification_email(clean_email, raw_verify_token)
    return _issue_tokens(user)


@router.post("/v1/auth/verify-email")
def verify_email(token: str = Query(...), db: Session = Depends(get_db)):
    token_hash = hash_action_token(token.strip())
    user = db.query(UserRow).filter(UserRow.email_verify_token == token_hash).first()
    if not user:
        check_auth_rate_limit("verify_email_invalid")
        raise HTTPException(status_code=400, detail="Invalid or expired verification token")
    if not user.email_verify_expires_at_ms or now_ms() > user.email_verify_expires_at_ms:
        user.email_verify_token = None
        user.email_verify_expires_at_ms = None
        db.commit()
        raise HTTPException(status_code=400, detail="Invalid or expired verification token")
    user.is_email_verified = True
    user.email_verify_token = None
    user.email_verify_expires_at_ms = None
    db.commit()
    return {"verified": True, "user_id": user.id}


@router.post("/v1/auth/login")
def login(body: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(UserRow).filter(UserRow.email == body.email.strip().lower()).first()
    if not user or not user.password_hash or not verify_password(body.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return _issue_tokens(user)


@router.post("/v1/auth/refresh")
def refresh_token(body: RefreshRequest, db: Session = Depends(get_db)):
    try:
        payload = decode_jwt(body.refresh_token, expected_type="refresh")
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Invalid refresh token")
    user = db.get(UserRow, payload["sub"])
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return _issue_tokens(user)


@router.post("/v1/auth/password-reset/request")
def request_password_reset(
    email: str = Query(...),
    db: Session = Depends(get_db),
    email_sender: EmailSender = Depends(get_email_sender),
):
    clean_email = email.strip().lower()
    check_auth_rate_limit(f"reset_req:{clean_email}", max_calls=MAX_TOKEN_ATTEMPTS)
    user = db.query(UserRow).filter(UserRow.email == clean_email).first()
    if user:
        raw_token = secrets.token_urlsafe(32)
        user.password_reset_token = hash_action_token(raw_token)
        user.password_reset_expires_at_ms = now_ms() + ACTION_TOKEN_TTL_MS
        user.password_reset_attempts = 0
        db.commit()
        email_sender.send_password_reset_email(clean_email, raw_token)
    return {"sent": True}


@router.post("/v1/auth/password-reset/confirm")
def confirm_password_reset(body: PasswordResetConfirmRequest, db: Session = Depends(get_db)):
    clean_email = body.email.strip().lower()
    user = db.query(UserRow).filter(UserRow.email == clean_email).first()
    if not user:
        check_auth_rate_limit(f"reset_confirm_missing:{clean_email}", max_calls=MAX_TOKEN_ATTEMPTS)
        raise HTTPException(status_code=400, detail="Invalid or expired password reset token")

    if int(user.password_reset_attempts or 0) >= MAX_TOKEN_ATTEMPTS:
        raise HTTPException(status_code=429, detail="Too many failed reset attempts. Request a new token.")

    incoming_hash = hash_action_token(body.reset_token.strip())
    stored_hash = user.password_reset_token or ""
    token_matches = bool(stored_hash) and hmac.compare_digest(stored_hash, incoming_hash)
    not_expired = bool(user.password_reset_expires_at_ms) and now_ms() <= user.password_reset_expires_at_ms

    if not token_matches or not not_expired:
        user.password_reset_attempts = int(user.password_reset_attempts or 0) + 1
        if not not_expired and stored_hash:
            user.password_reset_token = None
            user.password_reset_expires_at_ms = None
        db.commit()
        raise HTTPException(status_code=400, detail="Invalid or expired password reset token")

    user.password_hash = hash_password(body.new_password)
    user.password_reset_token = None
    user.password_reset_expires_at_ms = None
    user.password_reset_attempts = 0
    db.commit()
    return {"reset": True}


def _resolve_oauth_callback_url(request: Request, provider_slug: str) -> str:
    public_base = get_public_base_url()
    if public_base:
        return f"{public_base}/v1/auth/oauth/{provider_slug}/callback"
    raw_url = str(request.url_for("oauth_callback", provider=provider_slug))
    forwarded_proto = (request.headers.get("x-forwarded-proto") or "").split(",")[0].strip().lower()
    host = (request.headers.get("host") or "").strip().lower()
    if (forwarded_proto == "https" or host.endswith(".onrender.com")) and raw_url.startswith("http://"):
        raw_url = "https://" + raw_url[len("http://"):]
    return raw_url


@router.get("/v1/auth/oauth/{provider}/start")
@router.get("/v1/auth/oauth/{provider}/authorize")
def oauth_authorize(
    provider: str,
    request: Request,
    redirect_uri: str = Query(default="techenglish://auth/callback"),
    client_state: Optional[str] = Query(default=None),
    oauth_provider: OAuthProviderClient = Depends(get_oauth_provider),
):
    prov = provider.strip().upper()
    if prov not in {"GITHUB", "GOOGLE"}:
        raise HTTPException(status_code=400, detail="Unsupported OAuth provider")

    allowed_uris = get_allowed_redirect_uris()
    if redirect_uri not in allowed_uris:
        raise HTTPException(status_code=400, detail="redirect_uri is not in allowlist")

    server_state, _, code_challenge = oauth_flow_store.create_state(
        provider=prov,
        redirect_uri=redirect_uri,
        client_state=client_state,
    )
    callback_url = _resolve_oauth_callback_url(request, prov.lower())
    try:
        provider_url = oauth_provider.build_authorize_url(
            provider=prov,
            state=server_state,
            code_challenge=code_challenge,
            callback_url=callback_url,
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))
    return RedirectResponse(url=provider_url, status_code=status.HTTP_302_FOUND)


@router.get("/v1/auth/oauth/{provider}/callback", name="oauth_callback")
def oauth_callback(
    provider: str,
    request: Request,
    code: Optional[str] = Query(default=None),
    state: str = Query(..., min_length=1),
    error: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
    oauth_provider: OAuthProviderClient = Depends(get_oauth_provider),
):
    prov = provider.strip().upper()
    if prov not in {"GITHUB", "GOOGLE"}:
        raise HTTPException(status_code=400, detail="Unsupported OAuth provider")

    state_record = oauth_flow_store.consume_state(state=state, expected_provider=prov)
    if not state_record:
        raise HTTPException(status_code=400, detail="Invalid or expired OAuth state")

    if error:
        err_params = {"error": error[:64]}
        if state_record.get("client_state"):
            err_params["state"] = str(state_record["client_state"])
        return RedirectResponse(
            url=f"{state_record['redirect_uri']}?{urlencode(err_params)}",
            status_code=status.HTTP_302_FOUND,
        )

    clean_code = (code or "").strip()
    if not clean_code:
        raise HTTPException(status_code=400, detail="Missing OAuth authorization code")

    callback_url = _resolve_oauth_callback_url(request, prov.lower())
    try:
        info = oauth_provider.exchange_code_for_user(
            provider=prov,
            code=clean_code,
            code_verifier=str(state_record["code_verifier"]),
            callback_url=callback_url,
        )
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"OAuth provider code exchange failed: {exc}")

    identity = (
        db.query(OAuthIdentityRow)
        .filter_by(provider=prov, provider_sub=info.sub)
        .first()
    )
    user: Optional[UserRow] = None
    if identity:
        user = db.get(UserRow, identity.user_id)
        if user is None:
            db.delete(identity)
            db.flush()
            identity = None

    if user is None:
        clean_email = info.email.strip().lower() if isinstance(info.email, str) and info.email.strip() else None
        if clean_email:
            existing = db.query(UserRow).filter(UserRow.email == clean_email).first()
            if existing is not None:
                if not info.email_verified or not existing.is_email_verified:
                    raise HTTPException(
                        status_code=status.HTTP_409_CONFLICT,
                        detail="Cannot link OAuth identity to an account without verified email on both sides.",
                    )
                user = existing
        if user is None:
            try:
                clean_username = sanitize_oauth_username(info.username)
            except ValueError:
                clean_username = f"dev_{secrets.token_hex(4)}"
            if db.query(UserRow).filter(UserRow.username == clean_username).first():
                clean_username = f"{clean_username[:12]}_{secrets.token_hex(3)}"
            raw_avatar = (info.avatar_url or "").strip()
            avatar = (
                raw_avatar
                if (raw_avatar and len(raw_avatar) <= 512 and is_avatar_allowed(raw_avatar))
                else "preset_terminal"
            )
            display_name = (info.display_name or "").strip()[:120] or clean_username
            user = UserRow(
                id=str(uuid.uuid4()),
                username=clean_username,
                email=clean_email if info.email_verified else None,
                provider=prov,
                is_email_verified=bool(info.email_verified and clean_email),
                display_name=display_name,
                avatar_key=avatar,
                created_at_ms=now_ms(),
            )
            db.add(user)
            db.flush()

        db.add(
            OAuthIdentityRow(
                user_id=user.id,
                provider=prov,
                provider_sub=info.sub,
                provider_email=clean_email,
                created_at_ms=now_ms(),
            )
        )
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="OAuth identity conflict; please retry.")

    ticket = oauth_flow_store.issue_ticket(user.id)
    redirect_params = {"ticket": ticket}
    if state_record.get("client_state"):
        redirect_params["state"] = str(state_record["client_state"])
    target_uri = f"{state_record['redirect_uri']}?{urlencode(redirect_params)}"
    return RedirectResponse(url=target_uri, status_code=status.HTTP_302_FOUND)


@router.post("/v1/auth/oauth/ticket/exchange")
def oauth_ticket_exchange(body: OAuthTicketExchangeRequest, db: Session = Depends(get_db)):
    user_id = oauth_flow_store.consume_ticket(body.ticket)
    if not user_id:
        raise HTTPException(status_code=400, detail="Invalid or expired OAuth login ticket")
    user = db.get(UserRow, user_id)
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return _issue_tokens(user)


@router.post("/v1/auth/oauth/{provider}/exchange")
def oauth_pkce_exchange_deprecated(provider: str):
    """Disabled insecure legacy endpoint (BUG-01). Use /v1/auth/oauth/{provider}/authorize -> /callback -> /ticket/exchange."""
    raise HTTPException(
        status_code=status.HTTP_410_GONE,
        detail="Client-asserted OAuth exchange is disabled for security. Use /v1/auth/oauth/{provider}/authorize.",
    )


@router.post("/v1/auth/guest/link")
def link_guest_progress(
    body: SyncBatchRequest,
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    integrity_verifier: ClientIntegrityVerifier = Depends(get_integrity_verifier),
):
    """Links guest local progress into the authenticated user account using ФТ-9.2 / Errata E1 merge rules."""
    summary = _apply_sync_batch(db, user, body, integrity_verifier=integrity_verifier)
    summary["linked_user_id"] = user.id
    return summary


@router.delete("/v1/account")
def delete_account(
    user: UserRow = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: VoiceStorageBackend = Depends(get_voice_storage),
):
    uid = user.id
    # Explicit cascade cleanup so SQLite (without PRAGMA foreign_keys=ON) & PostgreSQL both purge all user data
    user_deck_ids = [d.id for d in db.query(CommunityDeckRow.id).filter_by(author_user_id=uid).all()]
    if user_deck_ids:
        db.query(CommunityDeckTermRow).filter(CommunityDeckTermRow.deck_id.in_(user_deck_ids)).delete(synchronize_session=False)
        db.query(CommunityDeckRatingRow).filter(CommunityDeckRatingRow.deck_id.in_(user_deck_ids)).delete(synchronize_session=False)
    user_comment_ids = [c.id for c in db.query(CommentRow.id).filter_by(author_user_id=uid).all()]
    if user_comment_ids:
        db.query(CommentVoteRow).filter(CommentVoteRow.comment_id.in_(user_comment_ids)).delete(synchronize_session=False)
    db.query(CommentVoteRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(CommentRow).filter_by(author_user_id=uid).delete(synchronize_session=False)
    db.query(CommunityDeckRatingRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(CommunityDeckRow).filter_by(author_user_id=uid).delete(synchronize_session=False)
    db.query(ModerationReportRow).filter_by(reporter_id=uid).delete(synchronize_session=False)
    db.query(UserBlockRow).filter((UserBlockRow.blocker_id == uid) | (UserBlockRow.blocked_id == uid)).delete(synchronize_session=False)
    db.query(DevicePushTokenRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(FriendPingRow).filter((FriendPingRow.sender_id == uid) | (FriendPingRow.receiver_id == uid)).delete(synchronize_session=False)
    db.query(FriendshipRow).filter((FriendshipRow.user_id == uid) | (FriendshipRow.friend_id == uid)).delete(synchronize_session=False)
    user_feed_ids = [f.id for f in db.query(ActivityFeedRow.id).filter_by(actor_user_id=uid).all()]
    if user_feed_ids:
        db.query(ActivityFeedReactionRow).filter(ActivityFeedReactionRow.event_id.in_(user_feed_ids)).delete(synchronize_session=False)
    db.query(ActivityFeedReactionRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(ActivityFeedRow).filter_by(actor_user_id=uid).delete(synchronize_session=False)
    db.query(SyncEventRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(Sm2StateRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(UserFavoriteRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(UserCustomWordRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(AiUsageLogRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(SquadMemberRow).filter_by(user_id=uid).delete(synchronize_session=False)
    user_pr_ids = [p.id for p in db.query(DictionaryPrRow.id).filter_by(author_user_id=uid).all()]
    if user_pr_ids:
        db.query(DictionaryPrVoteRow).filter(DictionaryPrVoteRow.pr_id.in_(user_pr_ids)).delete(synchronize_session=False)
    db.query(DictionaryPrVoteRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.query(DictionaryPrRow).filter_by(author_user_id=uid).delete(synchronize_session=False)
    db.query(RoomMessageRow).filter_by(author_user_id=uid).delete(synchronize_session=False)
    db.query(PvpMatchRow).filter((PvpMatchRow.player1_id == uid) | (PvpMatchRow.player2_id == uid)).delete(synchronize_session=False)
    # Errata E5 / ФТ-9.4: hard-delete all Peer Review voice clips from S3/MinIO object storage!
    user_peer_subs = db.query(PeerReviewSubmissionRow).filter_by(author_user_id=uid).all()
    deleted_voice_clips = 0
    for sub in user_peer_subs:
        if sub.voice_object_key and storage.delete_object(sub.voice_object_key):
            deleted_voice_clips += 1
    sub_ids = [s.id for s in user_peer_subs]
    if sub_ids:
        db.query(PeerReviewFeedbackRow).filter(PeerReviewFeedbackRow.submission_id.in_(sub_ids)).delete(synchronize_session=False)
    db.query(PeerReviewFeedbackRow).filter_by(reviewer_user_id=uid).delete(synchronize_session=False)
    db.query(PeerReviewSubmissionRow).filter_by(author_user_id=uid).delete(synchronize_session=False)
    db.query(OAuthIdentityRow).filter_by(user_id=uid).delete(synchronize_session=False)
    db.delete(user)
    db.commit()
    return {"deleted": True, "user_id": uid, "deleted_voice_clips": deleted_voice_clips}


@router.get("/v1/account/delete-form", response_class=HTMLResponse)
def delete_account_web_form():
    return """<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Удаление аккаунта TechEnglish</title></head>
<body>
<h1>Удаление аккаунта и данных TechEnglish (ФТ-9.4)</h1>
<p>Вы можете удалить аккаунт в приложении (Профиль → Аккаунт → Удалить аккаунт) или отправить запрос через веб-форму.</p>
<form method="post" action="/v1/account/delete-request"><input name="email" type="email" required/><button type="submit">Запросить удаление</button></form>
</body></html>"""


@router.post("/v1/account/delete-request", response_class=HTMLResponse)
async def submit_delete_account_request(
    request: Request,
    db: Session = Depends(get_db),
    email_sender: EmailSender = Depends(get_email_sender),
):
    check_auth_rate_limit(f"delete-request:{request.client.host if request.client else 'unknown'}")
    raw_body = (await request.body()).decode("utf-8", errors="ignore").strip()
    content_type = (request.headers.get("content-type") or "").lower()
    email = ""
    if "application/json" in content_type:
        try:
            parsed = json.loads(raw_body or "{}")
            if isinstance(parsed, dict):
                email = str(parsed.get("email", "")).strip().lower()
        except json.JSONDecodeError:
            email = ""
    else:
        form_data = parse_qs(raw_body)
        email = (form_data.get("email") or [""])[0].strip().lower()

    if not email or "@" not in email or "." not in email.split("@")[-1] or len(email) > 254:
        raise HTTPException(status_code=422, detail="Please provide a valid email address")

    user = db.query(UserRow).filter(UserRow.email == email).first()
    if user:
        logging.getLogger("techenglish.auth").info("Account deletion web request received for user_id=%s", user.id)
        try:
            email_sender.send(
                to_email=email,
                subject="TechEnglish — запрос на удаление аккаунта",
                body_text=(
                    "Мы получили запрос на удаление вашего аккаунта TechEnglish. "
                    "Для немедленного удаления откройте приложение: Профиль → Аккаунт → Удалить аккаунт, "
                    "либо дождитесь обработки заявки в течение 7 рабочих дней."
                ),
            )
        except Exception:
            logging.getLogger("techenglish.auth").warning("Failed to dispatch deletion confirmation email")

    safe_email = html.escape(email)
    return HTMLResponse(
        content=f"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Запрос на удаление принят — TechEnglish</title></head>
<body>
<h1>Запрос на удаление аккаунта принят</h1>
<p>Если аккаунт с адресом <strong>{safe_email}</strong> зарегистрирован в TechEnglish, мы обработаем запрос и удалим все связанные серверные данные (профиль, прогресс синхронизации, сообщения и голосовые записи Peer Review) в течение 7 рабочих дней.</p>
<p>Вы также можете удалить аккаунт мгновенно внутри приложения: <em>Профиль → Аккаунт → Удалить аккаунт</em>.</p>
</body></html>""",
        status_code=200,
    )


@router.get("/privacy", response_class=HTMLResponse)
def privacy_policy_html():
    return f"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Политика конфиденциальности — TechEnglish</title></head>
<body>
<h1>Политика конфиденциальности TechEnglish (v3.1)</h1>
<p><strong>Возрастное ограничение:</strong> 16+.</p>
<p><strong>Принцип Offline-First:</strong> словарь, прогресс обучения (SM-2) и упражнения хранятся локально на вашем устройстве. Облачная синхронизация является добровольной.</p>
<p><strong>Голосовые данные:</strong> записи тренажёра произношения и диктовки обрабатываются локально и не сохраняются на сервере (0 дней). Голосовые ответы в Peer Review Arena (до 20 секунд) хранятся в защищённом объектом хранилище не более {PEER_REVIEW_VOICE_TTL_DAYS} дней (TTL) и немедленно удаляются при удалении аккаунта.</p>
<p><strong>Удаление аккаунта и данных:</strong> вы можете удалить аккаунт в приложении (Профиль → Аккаунт → Удалить аккаунт) или подать веб-запрос на странице <a href="/v1/account/delete-form">/v1/account/delete-form</a>.</p>
</body></html>"""


@router.get("/terms", response_class=HTMLResponse)
def terms_of_service_html():
    return """<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Условия использования — TechEnglish</title></head>
<body>
<h1>Условия использования TechEnglish (v3.1)</h1>
<p>Используя приложение и сервисы TechEnglish, вы подтверждаете, что достигли возраста 16+ лет и соглашаетесь соблюдать правила сообщества.</p>
<ul>
<li>Запрещены оскорбления, спам, публикация персональных данных третьих лиц и обход систем честного рейтинга.</li>
<li>Контент сообщества (комментарии, колоды, PR в словарь, сообщения в комнатах) подлежит модерации.</li>
<li>Вопросы по данным и удалению аккаунта регулируются <a href="/privacy">Политикой конфиденциальности</a>.</li>
</ul>
</body></html>"""


@router.get("/v1/legal/privacy-policy")
def privacy_policy():
    return {
        "version": "3.0",
        "min_age": 16,
        "pronunciation_voice_retention_days": 0,
        "peer_review_voice_ttl_days": PEER_REVIEW_VOICE_TTL_DAYS,
        "voice_storage_backend": "S3-compatible (MinIO)",
        "summary_ru": "TechEnglish работает по принципу Offline-First. Возрастное ограничение платформы — 16+. "
        "Голосовые записи тренажёра произношения и AI-диктовки обрабатываются локально и не сохраняются (0 дней). "
        f"Голосовые ответы в Peer Review Arena (≤ 20 с) хранятся в S3/MinIO не более {PEER_REVIEW_VOICE_TTL_DAYS} дней (TTL) "
        "и немедленно удаляются вместе со всеми серверными данными при удалении аккаунта (DELETE /v1/account).",
    }
