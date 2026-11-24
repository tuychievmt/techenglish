# Отчёт по Спринту 1A — Backend: безопасность, OAuth, AI, MinIO, Redis-тикеты и Play Integrity (`SPRINT_1A_REPORT.md`)

**Ветка:** `remediation/v3.1`  
**Тег:** `v3.1-sprint1a`  
**Дата:** 2026-11-23

---

## 1. Выполненные задачи и коммиты (в порядке плана)

| Задача | Дефекты | Коммит | Ключевые изменения |
|---|---|---|---|
| **SP1-01** | `BUG-02`, часть `BUG-07` | `4d1ff21` | Удалена утечка `email_verify_token_dev` / `reset_token_dev`; токены хранятся только как SHA-256 хеш, одноразовые, TTL 15 мин, лимит 5 попыток (`429`), ответ `/password-reset/request` не раскрывает существование аккаунта; введён `EmailSender` (`SmtpEmailSender` + `ConsoleEmailSender` через DI); добавлена миграция `0002_auth_token_security`; в `ENV=prod` сервер падает при старте с дефолтным/коротким `JWT_SECRET` (< 32) или fake-провайдерами. |
| **SP1-02** | `BUG-03` | `43b14cb` | Добавлена колонка `users.role` (`USER`/`MODERATOR`/`ADMIN`) через миграцию `0003_user_role_rbac`; добавлена зависимость `require_moderator` на `/v1/moderation/queue` и `/v1/crowdsource/prs/{pr_id}/approve`; запрещён самоапрув PR (`author_user_id == user.id` → `403`); добавлен CLI/env-bootstrap первого администратора `backend/app/cli_admin.py`. |
| **SP1-03a** | `BUG-01`, сервер `BUG-04` | `02cc05d` | Реализован серверный OAuth 2.0 + PKCE flow: `GET /v1/auth/oauth/{provider}/authorize` → серверный `GET /v1/auth/oauth/{provider}/callback` (сервер сам обменивает `code` с `client_secret` и `code_verifier`) → редирект на allowlisted `techenglish://auth/callback?ticket=…` → `POST /v1/auth/oauth/ticket/exchange`. Добавлена таблица `oauth_identities` (`0004_oauth_identities`) с привязкой `(provider, sub)` и запретом слияния по неподтверждённому email (`409`). Старый небезопасный `/v1/auth/oauth/{provider}/exchange` отключён (`410 Gone`). |
| **SP1-04** | `BUG-05` | `48a2570` | Реализован асинхронный `HttpxGeminiClient` (`backend/app/ai_proxy.py`) с таймаутами, JSON-схемой ответа и Pydantic-валидацией, TTL/LRU-кэшированием, обязательным PII-scrubbing до промпта, честным `provider` (`gemini:<model>` vs `heuristic-fallback`) и явным признаком `is_fallback` / `fallback_reason` при ошибках или таймауте. |
| **SP1-05** | `BUG-06` (сервер) | `8e45902` | Реализован `S3VoiceObjectStorage` (`boto3` / MinIO в `docker-compose.yml`) с 30-дневным bucket lifecycle, валидацией клипа ≤ 20 с и ≤ 512 КБ, подписанным GET (`GET /v1/peer-review/submissions/{id}/voice-url`), синхронным удалением объектов при `DELETE /v1/account`. Добавлен `boto3==1.44.9` в `backend/requirements.txt`. |
| **SP1-07a** | `R11`, `BUG-07`, сервер `BUG-10` | `2429993` | Подключён клиент Redis по `REDIS_URL` (`backend/app/redis_client.py`, с `fakeredis` для локальных тестов); реализован `POST /v1/ws/ticket` (одноразовый тикет в Redis, TTL 30 с) и атомарное погашение (`GETDEL`) в `/ws/rooms/{slug}` и `/ws/pvp/{match_id}`. Временная совместимость `?token=` сохранена строго до `SP1-07b`. |
| **SP1-11** | `R3`, `BUG-07` | `3e0ce27` | Реализован `ClientIntegrityVerifier` (`backend/app/integrity.py`) и `GooglePlayIntegrityVerifier` (декодирование вердикта через Play Integrity API, проверка `nonce`, `requestPackageName`/`packageName`, `PLAY_RECOGNIZED`, `certificateSha256Digest`, `MEETS_DEVICE_INTEGRITY`/`MEETS_STRONG_INTEGRITY`, свежести и лицензии); в `ENV=prod` fake-верификатор запрещён. |

---

## 2. Фактически измеренные результаты проверок

### 2.1. Тесты

| Модуль / набор | Команда | Результат |
|---|---|---|
| `backend/` (`pytest`) | `cd backend && .venv/bin/pytest -v` | **29 PASSED, 1 SKIPPED** (`17.66s`; 11 базовых API/WS + 2 Alembic/prod + 16 новых security-тестов Спринта 1A; 1 PostgreSQL-тест ожидает CI-контейнер) |
| Alembic цепочка миграций | `0001 → 0002 → 0003 → 0004` | **PASSED** (`compare_metadata == []`, `upgrade head → downgrade base → upgrade head` на SQLite; статический PostgreSQL DDL `--sql` exit code `0`) |
| `:domain` + JaCoCo ≥ 90% | `./gradlew :domain:test :domain:jacocoTestCoverageVerification` | **57 / 57 PASSED**, гейт `LINE >= 90%` (`94.58%`) **PASSED** |
| `:app` (Robolectric + UI) | `./gradlew :app:testDebugUnitTest` | **36 / 36 PASSED** |
| **ИТОГО локально выполнено** | | **122 / 122 PASSED** (`0` failed) |

### 2.2. Валидация конфигураций
- `backend/docker-compose.yml` (`docker compose config --quiet`): **PASSED**.

---

## 3. Непроверенное E2E без реальных ключей (протокол §0.2, см. `docs/OWNER_ACTIONS.md`)

- **SMTP (`SP1-01`):** E2E не проверено, нужен ключ/реквизиты SMTP-сервера (`SMTP_HOST`, `SMTP_USER`, `SMTP_PASSWORD`); проверено на `ConsoleEmailSender` и unit-проверках DI.
- **GitHub / Google OAuth (`SP1-03a`):** E2E не проверено, нужны ключи (`GITHUB_CLIENT_ID`/`SECRET`, `GOOGLE_CLIENT_ID`/`SECRET`); проверено с мок-провайдером и `DevLoopbackOAuthProvider`.
- **Gemini API (`SP1-04`):** E2E не проверено, нужен ключ (`GEMINI_API_KEY`); проверено на fake async клиенте, кэше, PII-scrubbing, таймауте и битой схеме.
- **MinIO контейнер (`SP1-05`):** E2E с живым контейнером не проверено локально (Docker daemon остановлен); S3-контракт, 30-дневный lifecycle, presigned GET и hard delete проверены на `moto`.
- **Google Play Integrity API (`SP1-11`):** E2E не проверено, нужен облачный проект Google Cloud / Play Console и `PLAY_INTEGRITY_CERT_SHA256_DIGESTS` / сервисные учётные данные; разбор вердикта, проверка `nonce`/пакета/сертификата/свежести и prod-запрет fake-верификатора проверены unit-тестами.
- **Живой PostgreSQL 16 / Redis 7 (`SP1-02`, `SP1-07a`):** не проверено локально (нет локального Docker daemon); проверяется в `.github/workflows/ci.yml` (`job: backend`).

---

## 4. Статус DoD Спринта 1A

- [x] Все backend-дефекты P0 (`BUG-01`, `BUG-02`, `BUG-03`, `BUG-05`, сервер `BUG-06`, Redis-тикеты и Play Integrity из `BUG-07`, сервер `BUG-04`/`BUG-10`) воспроизведены красными тестами и закрыты зелёными `pytest`.
- [x] Все 4 миграции Alembic (`0001..0004`) применяются и откатываются чисто, схема совпадает с `models.py`.
- [x] Все тесты (`backend` + `:domain` + `:app`) зелёные.
- [x] Тег `v3.1-sprint1a` готов; работа остановлена до подтверждения **`go`** для перехода к **Спринту 1B**.
