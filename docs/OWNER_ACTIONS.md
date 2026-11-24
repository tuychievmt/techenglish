# Действия владельца и чек-лист публикации (`OWNER_ACTIONS.md`)
## Проект: TechEnglish v3.1.0 (`com.techenglish.app`, `versionCode = 310`)

Настоящий документ синхронизирован с фактическими интерфейсами, классами и переменными окружения в кодовой базе v3.1.0 (`SP3-07` / `BUG-22` / `R8`). Ни один реальный секрет, токен, ключ подписи или `local.properties` не хранится в репозитории.

---

## 1. Внешние ключи, сервисы и действия владельца (E2E не проверено без ключей)

| № | Блокер / Ключ | Фактическая реализация в коде v3.1.0 | Переменные окружения / файлы | Действие владельца перед production-запуском |
|---|---|---|---|---|
| **1** | **Gemini API Key (AI-ментор)** | `backend/app/ai_proxy.py` (`HttpxGeminiClient` + PII-санитайзер + TTL/LRU кэш; при отсутствии ключа честный fallback `provider="deterministic_fallback"`, `is_fallback=True`) | `GEMINI_API_KEY`, `GEMINI_MODEL` (опционально) | Получить ключ в Google AI Studio / Vertex AI и прописать строго в серверном `.env` (клиент ключа не содержит). |
| **2** | **GitHub и Google OAuth (Server-side Authorization Code)** | Backend: `backend/app/oauth_provider.py`, `backend/app/routers/auth.py` (`/v1/auth/oauth/{provider}/authorize`, `/callback`, `/ticket/exchange`, таблица `oauth_identities`); Android: `OAuthCallbackHandler.kt`, Custom Tabs (`techenglish://auth/callback`) | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OAUTH_ALLOWED_REDIRECT_URIS` | Зарегистрировать OAuth-приложения в GitHub и Google Cloud Console с callback-URL бэкенда и разрешить `techenglish://auth/callback`. |
| **3** | **SMTP-сервер (подтверждение email и сброс пароля)** | `backend/app/email_sender.py` (`SmtpEmailSender` / `FakeEmailSender`; `ENV=prod` запрещает запуск с `FakeEmailSender`) | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` | Настроить транзакционный SMTP-провайдер (SES / Postmark / Resend / Mailgun) и задать переменные в `.env`. |
| **4** | **Google Play Integrity API** | `backend/app/integrity.py` (`GooglePlayIntegrityVerifier` / `ClientIntegrityVerifier`; `ENV=prod` запрещает фейковый верификатор; в `ENV=staging` для бета-сборок проверка не обязательна) | `PLAY_INTEGRITY_STRICT=true`, `PLAY_INTEGRITY_PACKAGE_NAME=com.techenglish.app`, `PLAY_INTEGRITY_CERT_SHA256_DIGESTS`, `PLAY_INTEGRITY_API_ACCESS_TOKEN` (или `PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON`) | Связать приложение в Google Play Console с Google Cloud, включить Play Integrity API и указать SHA-256 сертификата подписи релиза. |
| **5** | **Firebase Cloud Messaging (FCM HTTP v1)** | Backend: `backend/app/services/push_gateway.py` (`FcmHttpV1PushNotificationGateway` / `FakePushNotificationGateway`), `POST/DELETE /v1/notifications/device-token`, таблица `device_push_tokens`; Android: `PushNotificationGateway.kt` (`DefaultPushNotificationGateway`) | Backend: `FCM_PROJECT_ID`, `FCM_ACCESS_TOKEN`; Android: `app/google-services.json` (+ подключение плагина `com.google.gms.google-services`) | Создать проект Firebase для `com.techenglish.app`, добавить `app/google-services.json` (в `.gitignore`) и настроить выдачу OAuth2-токена для FCM HTTP v1. |
| **6** | **S3 / MinIO / R2 объектное хранилище (голос Peer Review, TTL 30 дн.)** | `backend/app/voice_storage.py` (`S3VoiceObjectStorage` через `boto3`, lifecycle 30 дней, presigned GET `/v1/peer-review/submissions/{id}/voice-url`); Android: `PeerVoiceClipRecorder.kt` + `TechAudioController.playRemoteAudioUrl` | `VOICE_STORAGE_BACKEND=s3`, `VOICE_STORAGE_ENDPOINT`, `VOICE_STORAGE_ACCESS_KEY`, `VOICE_STORAGE_SECRET_KEY`, `VOICE_STORAGE_BUCKET`, `VOICE_STORAGE_REGION`, `VOICE_STORAGE_SKIP_BUCKET_INIT=true` (для R2/B2) | Поднять MinIO из `backend/docker-compose.yml` или подключить управляемый S3/R2-бакет с HTTPS endpoint и lifecycle TTL 30 дней. |
| **7** | **Crash-мониторинг (Sentry / Crashlytics)** | `app/src/main/kotlin/com/techenglish/app/core/monitoring/CrashReporter.kt` (`ConfigurableCrashReporter`, санитайзер PII/токенов, по умолчанию выключен) | `CRASH_REPORTING_ENABLED=true`, `SENTRY_DSN` | При желании включить сбор крэшей в релизной сборке — передать `CRASH_REPORTING_ENABLED=true` и `SENTRY_DSN` в CI/CD. |
| **8** | **Staging / Production PostgreSQL 16, Redis 7, JWT Secret, домен и TLS 1.2+** | `backend/app/services/startup.py` (`validate_production_startup_config`), `backend/app/redis_client.py`, `backend/app/services/realtime_broker.py`, `backend/alembic/` (`0001`…`0006`) | `ENV=staging` (для беты) или `ENV=prod`, `DATABASE_URL`, `REDIS_URL`, `JWT_SECRET` (≥ 32 символов), `BOOTSTRAP_ADMIN_EMAIL` (для первого администратора или `python -m app.cli_admin`) | Развернуть `backend/Dockerfile` или `backend/docker-compose.yml` за reverse-proxy с TLS 1.2+, выполнить `alembic upgrade head` и назначить первого `ADMIN`/`MODERATOR`. |
| **9** | **Релизный ключ подписи Android (`release.keystore`)** | `app/build.gradle.kts` (`signingConfigs.release`, null-safe) | `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Сгенерировать keystore вне репозитория, включить Play App Signing и подписать `.aab` в CI или локально. |
| **10** | **Человеческая верификация таджикской локализации (R7)** | `app/src/main/res/values-tg/strings.xml`, `TajikLocalizationPolicy.kt`, `docs/tg_review/ui_strings_tg_draft.json`, `docs/tg_review/terms_tg_draft.csv`, `scripts/import_verified_tg.py` | `TAJIK_UI_LOCALIZATION_ENABLED=true` (после одобрения) | Проверить черновики в `docs/tg_review/` носителем языка, сменить `status` на `verified`, импортировать через `scripts/import_verified_tg.py`, пересобрать БД (`scripts/build_seed_db.py`) и включить флаг. В релизной БД v3.1.0 сейчас **19** проверенных `translation_tg` (остаток до цели ≥ 2 000: **1 981**). |
| **11** | **Юридическое утверждение Privacy Policy / Terms of Service** | `backend/app/routers/profile_moderation.py` (`/v1/legal/privacy-policy`), `backend/app/routers/auth.py` (`/v1/account/delete-form`), Android `AgeGateAndPrivacyDialog.kt` | Публичные URL `/privacy` и `/terms` | Утвердить финальные тексты с юристом и опубликовать по HTTPS URL до подачи в Google Play. |

---

## 2. Чек-лист Google Play Data Safety и публикации в Internal Track (R8)

### 2.1. Data Safety (Google Play Console)
- [ ] **Возрастная аудитория:** 16+ (принудительный `AgeGate16PlusScreen` при первом запуске, включая гостевой режим).
- [ ] **Собираемые данные (только при использовании облачных/социальных функций):**
  - Email / `@username` / OAuth `(provider, sub)` — для аутентификации и синхронизации прогресса.
  - Прогресс обучения (`study_logs`, SM-2, избранное, пользовательские слова) — для облачной синхронизации и лидербордов.
  - Голосовые записи Peer Review Arena (≤ 20 с, M4A/OGG) — **опционально**, хранятся в S3/MinIO с автоматическим **TTL 30 дней**, удаляются при удалении аккаунта (`DELETE /v1/account`). Локальное произношение и AI-диктовка не сохраняются (TTL = 0).
  - Токен устройства FCM (`device_push_tokens`) — опционально для пингов друзей (можно отключить в профиле `accepts_pings=false`).
- [ ] **Шифрование при передаче:** включено (HTTPS TLS 1.2+, `cleartextTrafficPermitted="false"` в release `network_security_config.xml`, одноразовые WS-тикеты).
- [ ] **Хранение токенов на устройстве:** Android Keystore AES-256-GCM + DataStore, `allowBackup="false"`, исключение токенов в `backup_rules.xml` / `data_extraction_rules.xml`.
- [ ] **Удаление аккаунта и данных:** доступно внутри приложения (`DELETE /v1/account`) и по веб-ссылке (`GET /v1/account/delete-form`), удаляет записи БД, голосовые клипы S3/MinIO и push-токены.
- [ ] **Разрешения Android:** `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` (Android 13+, по запросу), `RECORD_AUDIO` (on-demand в Peer Review Arena / Pronunciation Trainer). Разрешение `CAMERA` удалено.

### 2.2. Загрузка в Internal Testing Track (выполняет владелец)
1. Подписать `app/build/outputs/bundle/release/app-release.aab` релизным keystore (`KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` → `./gradlew :app:bundleRelease`).
2. Проверить, что в `.aab` присутствуют оба модуля: `base` и `install-time` asset pack `content_pack` (`databases/tech_english.db`).
3. Загрузить подписанный `.aab` в Google Play Console → **Testing → Internal testing**, заполнить форму Data Safety и приложить URL политики конфиденциальности и веб-формы удаления аккаунта.
