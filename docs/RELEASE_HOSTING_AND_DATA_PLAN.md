# План доведения до релиза, масштабирования данных и бесплатного хостинга TechEnglish v3.1.0

**Статус документа:** Архитектурный, DevOps- и Data-план (подготовлен в строгом read-only режиме после remediation `v3.1.0`).  
**Базовая ревизия:** ветка `remediation/v3.1`, тег `v3.1-sprint3`, коммит `c155b63` (`versionName = "3.1.0"`, `versionCode = 310`).  
**Модули:** `:domain` (Pure Kotlin JVM), `:app` (Android / Jetpack Compose / Room v6 / Hilt / Ktor Client / Glance / WorkManager), `:content_pack` (`install-time` Play Asset Delivery), `backend/` (FastAPI + SQLAlchemy + Alembic + PostgreSQL 16 + Redis 7 + S3/MinIO).  
**Цель документа:** дать владельцу и команде чёткую, привязанную к реальному коду дорожную карту: (1) закрытия всех скрытых технических разрывов до рабочего релиза, (2) безопасного пополнения контента БД и OTA-пакетов без деградации поиска и размеров сборки, (3) запуска полностью рабочего бэкенда на $0/мес с последующей бесшовной миграцией на платный VPS.

---

## Раздел 1. Глубокий анализ текущего состояния проекта (Full Project Audit)

### 1.1. Что уже полностью реализовано и проверено тестами (Android + Backend + CI)

По данным исходного кода, отчётов `docs/FINAL_REPORT.md`, `docs/REMEDIATION_LOG.md`, `docs/SPRINT_0_REPORT.md`–`docs/SPRINT_3_REPORT.md` и `.github/workflows/ci.yml`:

#### Автоматическое тестирование и гейты качества
| Контур | Состав и покрытие | Зафиксированный результат (`v3.1-sprint3`) |
|---|---|---|
| **`:domain`** (Pure Kotlin JVM, `domain/build.gradle.kts`) | 12 тестовых файлов (SM-2, поиск, программирование, сценарии, Stage 2–4C use cases); JaCoCo `0.8.12` гейт `LINE >= 0.90` | **57 / 57 PASSED** (`0` failed, `0` skipped); Line Coverage **94.58%** (`2844/3007` в XML/HTML; `2844/3008 = 94.55%` в CSV из-за 1 синтетической строки агрегации); Instruction **88.13%**, Branch **73.83%** |
| **`:app`** (Robolectric + Room + Compose UI + Security + Accessibility) | 20 тестовых файлов (миграции Room `1→2→3→4→5→6`, `ContentPackImporter`, `DatabaseBootstrapManager`, `@Fts4`, `OAuthCallbackHandler`, `AndroidKeystoreTokenStore`, Ktor `Auth` refresh, Glance, Feed/OTA, PvP WS, Peer Review voice, offline rooms + `SquadCache`, Compose stability) | **66 / 66 PASSED** (`0` failed, `0` skipped); средняя задержка FTS4-поиска на 30 000 записей (`Sp204FtsAndCyrillicSearchTest`): **8.13 мс** (`< 15 мс`); `DatabaseBootstrapManager` на `Dispatchers.IO`: **514 мс** 1-й запуск / **0 мс** повторный; Compose Compiler (`app/build/compose_compiler/`): `973/1482` skippable, `24 628 / 24 719` (`99.63%`) stable args, **0** UI/ViewModel-файлов `> 900` строк |
| **`backend/`** (`pytest`, `backend/tests/`) | 17 тестовых файлов (`test_api_stage3/4a/4b/4c`, `test_alembic_migrations`, `test_sp1_*`, `test_sp2_*`, `test_sp3_01`) с использованием `fakeredis`, `moto[s3]`, `httpx.MockTransport` | **37 PASSED, 1 SKIPPED, 0 failed** (пропущен 1 тест — живой PostgreSQL Alembic из-за остановленного локального Docker daemon; offline PostgreSQL DDL и SQLite batch `0001..0006` пройдены) |
| **CI/CD** (`.github/workflows/ci.yml`) | Job `android`: JDK 17, `:domain` tests + JaCoCo >= 90% + сверка XML/CSV/HTML + `:app:testDebugUnitTest` + upload artifacts; Job `backend`: сервисы `postgres:16-alpine` и `redis:7-alpine`, `alembic upgrade head`, `pytest`, `docker compose config --quiet && docker compose build` | Настроено для `main` и `remediation/**` |

#### Артефакты сборки v3.1.0 (`versionCode = 310`)
- **`app/build/outputs/bundle/release/app-release.aab`:** **21.30 MiB** (`22 334 905` Б):
  - модуль `base`: **5.16 MiB** сжатый (`12.28 MiB` несжатый) — `isMinifyEnabled = true`, `isShrinkResources = true`, правила `app/proguard-rules.pro`;
  - модуль `:content_pack` (`install-time` PAD, `content_pack/build.gradle.kts`): **10.75 MiB** сжатый (`103.24 MiB` несжатый — `databases/tech_english.db`);
  - `BUNDLE-METADATA`: `5.15 MiB` (R8 mapping, не доставляется пользователям).
- **`app/build/outputs/apk/release/app-release-unsigned.apk`:** **5.59 MiB** (`5 860 640` Б), `lintVitalRelease` пройден без ошибок.
- **`app/build/outputs/apk/debug/app-debug.apk`:** **40.66 MiB** (`42 634 180` Б).

#### Реализованные подсистемы
- **Offline-First ядро Android:** Room v6 (`TechEnglishDatabase.kt`, 29 `@Entity`, 18 DAO, lossless-миграции `1→2→3→4→5→6`), `@Fts4` (`academic_terms_fts`, `unicode61`) + `normalized_search`, SM-2 SRS, 19 языков / 380 сниппетов кода, 166 статей и логов (Read Your Own Docs, Debug the Log), Pronunciation Trainer (`TechAudioController` с ключом кэша `SHA-256(locale|speed|text)` и LRU `10 MiB / 40` файлов), Glance Widget (`TechEnglishGlanceWidget` с палитрой из DataStore и кнопкой обновления), Time Attack, импорт/экспорт колод (JSON/CSV/Anki `.apkg`), `AndroidKeystoreTokenStore` (AES-256-GCM + DataStore, `allowBackup="false"`, без `PLAIN:` fallback), `OAuthCallbackHandler` (`androidx.browser` Custom Tabs + `techenglish://auth/callback`), Ktor Client (`TechEnglishApiClient.kt`: HTTP + WS + `Auth/bearer`), Outbox `sync_queue` + `CloudSyncEngine` + `CloudSyncWorker`, Community comments/decks, Squads + `SquadCacheEntity`, Crowdsourcing PRs, Thematic Rooms WS, live PvP WS (сервер скрывает `correct_option_index` до ответа), Peer Review M4A запись (`PeerVoiceClipRecorder`) + проигрывание presigned URL.
- **Модульный Backend (`backend/`):** `main.py` сокращён до 125 строк (`create_app()`, `lifespan`, единые обработчики ошибок с полями `detail` + `error`), 10 роутеров в `backend/app/routers/` (всего **68 маршрутов** включая `/health` и 2 WebSocket), 6 миграций Alembic (`0001_initial_v3_0_0` … `0006_activity_feed_reactions`), RBAC (`users.role`, `require_moderator`, `app/cli_admin.py`), серверный OAuth 2.0 + PKCE (`oauth_identities`), SHA-256 токены верификации/сброса и `SmtpEmailSender`, асинхронный `HttpxGeminiClient` + `scrub_pii` + LRU/TTL-кэш + честный fallback, `S3VoiceObjectStorage` (`boto3`/MinIO, TTL 30 дней, hard delete при `DELETE /v1/account`), `RedisRealtimeBroker` (Pub/Sub + PvP очередь + одноразовые WS-тикеты с `GETDEL`), `FcmHttpV1PushNotificationGateway`, `GooglePlayIntegrityVerifier` и guard `validate_production_startup_config()` для `ENV=prod`.

---

### 1.2. Что осталось недоделанным, за флагами, за заглушками и скрытые дефекты кода

Аудит исходного кода выявил **конкретные технические дефекты, контракты с расхождениями и незакрытые заглушки**, которые необходимо устранить перед живым запуском:

#### A. Скрытые дефекты кода и расхождения контракта Client ↔ Server (блокируют живой E2E)
| ID дефекта | Точное место в коде | Суть проблемы и реальное последствие в проде |
|---|---|---|
| **GAP-01** | `backend/app/routers/peer_review.py:116` vs `app/src/main/kotlin/com/techenglish/app/core/network/TechEnglishApiClient.kt:581` | **Несовпадение имени поля presigned voice URL:** сервер в `GET /v1/peer-review/submissions/{sub_id}/voice-url` возвращает `"voice_url"`, а Android `PeerReviewVoiceUrlResponseDto` ожидает `@SerialName("download_url")`. В результате `PeerReviewArenaViewModel.playSubmissionVoice()` (`PeerReviewArenaViewModel.kt:142`) всегда получает пустую строку и выводит *«Ссылка на аудиофайл недоступна»*. |
| **GAP-02** | `backend/app/routers/auth.py:421` | **Битая веб-форма удаления аккаунта (риск отклонения Google Play):** `GET /v1/account/delete-form` рендерит HTML `<form method="post" action="/v1/account/delete-request">`, однако эндпоинт `POST /v1/account/delete-request` в `backend/app/` **не реализован** (вернёт `404 Not Found`). Также в коде нет эндпоинтов `GET /privacy` и `GET /terms` (есть только JSON `GET /v1/legal/privacy-policy`). |
| **GAP-03** | `app/build.gradle.kts:13-22` | **Standalone release APK не содержит БД `tech_english.db`:** в `sourceSets` директория `content_pack/src/main/assets` подключена только к `debug` и `test`, а в `release` БД поставляется исключительно через PAD `assetPacks += listOf(":content_pack")`. Поэтому собранный `app-release-unsigned.apk` (`5.59 MiB`) при прямой установке (sideload, Telegram, GitHub Releases) упадёт в `DatabaseBootstrapManager` с `FileNotFoundException`! Для sideload-APK нужен `bundletool build-apks --mode=universal` из `.aab` либо отдельный buildType/flavor. |
| **GAP-04** | `app/src/main/kotlin/com/techenglish/app/core/sync/CloudSyncEngine.kt:93-135`, `domain/.../Stage3Models.kt:46-47`, `Stage3ViewModels.kt:221-238` | **Потеря офлайн-сообщений комнат при глобальной синхронизации:** `ThematicRoomsViewModel` кладёт офлайн-сообщения в `sync_queue` с `eventType = "ROOM_MESSAGE_OUTBOX"`, но `CloudSyncEngine.runFullSyncNow()` и `CloudAccountSyncViewModel.linkGuestAccount()` выбирают из `sync_queue` **все** `PENDING`-строки без фильтра. `SyncEventType.fromRaw("ROOM_MESSAGE_OUTBOX")` молча превращает их в `FLASHCARD_CORRECT`, отправляет на `/v1/sync/events` и помечает `SYNCED`, безвозвратно удаляя неотправленное сообщение чата! |
| **GAP-05** | `TechEnglishApiClient.kt:140-145`, `backend/app/integrity.py:150-163`, `backend/app/services/startup.py:24-28`, `backend/app/services/push_gateway.py:59-103` | **Разрыв Play Integrity & протухание OAuth2-токенов GCP через 1 час:** (1) На Android нет SDK `com.google.android.play:integrity`, а `SyncBatchBodyDto` не отправляет `play_integrity_token` и `play_integrity_nonce` → в `ENV=prod` любой `POST /v1/sync/events` упадёт с `403 Play Integrity verification failed (missing_integrity_token)`. (2) На сервере `GooglePlayIntegrityVerifier` и `FcmHttpV1PushNotificationGateway` принимают только сырой `PLAY_INTEGRITY_API_ACCESS_TOKEN` / `FCM_ACCESS_TOKEN`, которые в Google Cloud живут **60 минут**; при этом `startup.py:26` проверяет `PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON`, но `integrity.py` его не читает, а в `backend/requirements.txt` нет пакета `google-auth`. |
| **GAP-06** | `TechEnglishApiClient.kt:740-1197` | **Ручной заголовок `Authorization` поверх Ktor `Auth`:** ~28 методов клиента вручную проставляют `header(HttpHeaders.Authorization, "Bearer $token")`. Это конфликтует с установленным плагином `Auth { bearer { refreshTokens { ... } } }` (`TechEnglishApiClient.kt:629-672`): при протухании 30-минутного `access_token` Ktor-плагин может не перезаписать уже явно выставленный заголовок на повторном запросе после `401`. |
| **GAP-07** | `Stage3ViewModels.kt:423-462, 648-663`, `TechEnglishApiClient.kt` | **Невызываемые серверные эндпоинты (разрыв синхронизации UI):** бэкенд реализует `GET/PATCH /v1/profile/me`, `POST /v1/friends/feed/{event_id}/react`, `GET /v1/friends/search`, `POST /v1/auth/verify-email`, `POST /v1/auth/password-reset/request|confirm`, но в `TechEnglishApiClient.kt` нет методов для этих маршрутов (`saveDeveloperProfile` и `setAllowFriendPings` пишут только локально в Room/DataStore, `reactToFeedItem` инкрементирует счётчик только в локальном кэше, нет UI подтверждения email / сброса пароля). |
| **GAP-08** | `AndroidKeystoreTokenStore.kt:162-167, 281`, `MainActivity.kt:94-120`, `CloudAccountSyncScreen.kt:303` | **Мелкие клиентские дефекты:** (1) `DEFAULT_API_BASE_URL = "https://api.techenglish.app"` захардкожен без `BuildConfig.API_BASE_URL`; `setApiBaseUrl` разрешает `http://10.0.2.2` и `http://localhost`, но не `http://127.0.0.1` (хотя `debug/network_security_config.xml` его разрешает). (2) В `MainActivity.kt` состояния `DatabaseBootstrapState.Idle` и `Error` проваливаются в `else -> TechEnglishNavShell`, не показывая экран ошибки/ожидания при старте распаковки. (3) В `CloudAccountSyncScreen.kt:303` отображается устаревшая строка *«встроенная офлайн-база v4»* (актуально `v6`). |
| **GAP-09** | `scripts/import_verified_tg.py:48`, `tools/vocabulary_import/vocabulary_importer.py:128-209`, `AcademicTermDao.kt:402-466`, `ContentPackImporter.kt:231` | **Дефекты конвейера данных и OTA:** (1) `import_verified_tg.py` патчит `.db` напрямую без пересчёта `normalized_search` (FTS4 не найдёт новые таджикские переводы!) и без записи в исходники `tools/vocabulary_import/` (при следующем `build_seed_db.py` данные пропадут). (2) `vocabulary_importer.py` не обновляет `ipa_source_url`, `ipa_license`, `normalized_search`. (3) `AcademicTermDao.updateContentPreservingSrs()` не обновляет `normalized_search` при OTA-апдейте термина. (4) `ContentPackImporter.importFromJson()` декодирует только `terms`, игнорируя `docArticles`, `scenarios`, `workPhrases`, `codeSnippets`. |

#### B. Заглушки, фича-флаги и in-memory ограничения
1. **Таджикская локализация (`TAJIK_UI_LOCALIZATION_ENABLED = false` по умолчанию в `app/build.gradle.kts:33`):** `app/src/main/res/values-tg/strings.xml` содержит только 4 `<string>` (копии RU fallback), а остальные тексты интерфейса в Compose-экранах захардкожены на русском (не вынесены в `strings.xml`). В релизной БД только **19** проверенных `translation_tg`.
2. **Crash-мониторинг (`CRASH_REPORTING_ENABLED = false`, `SENTRY_DSN = ""`):** `ConfigurableCrashReporter` (`CrashReporter.kt:34-76`) не подключён к реальному SDK Sentry/Crashlytics (`sink = null`).
3. **Firebase Cloud Messaging (FCM):** `DefaultPushNotificationGateway` (`PushNotificationGateway.kt:34-73`) проверяет наличие ресурса `google_app_id`, но в проекте отсутствуют плагин `com.google.gms.google-services`, зависимость `firebase-messaging`, `FirebaseMessagingService` в `AndroidManifest.xml` и файл `app/google-services.json`.
4. **In-memory состояния на сервере (ломаются при нескольких воркерах или рестарте):** `OAuthFlowStore` (`backend/app/oauth_provider.py:239-304`) и `_AUTH_RATE_BUCKETS` (`backend/app/deps.py:17`) хранятся в словарях Python-процесса (не в Redis). `OTA_PACKS` (`backend/app/services/sync_service.py:31-60`) захардкожен в виде 1 тестового пака (`ota_docker_linux_week_v3`, 1 термин).
5. **Опциональный скоуп (R9, отложен решением владельца):** `SP3-05` (KMP/iOS прототип) и `SP3-06` (tap-глоссарий и онбординг).

#### C. Непроверенное E2E / неизмеренное локально (§0.1, §0.2)
- Реальные интеграции (без ключей и внешних аккаунтов): Gemini API, GitHub/Google OAuth, FCM HTTP v1, Google Play Integrity API, SMTP, production S3/MinIO/R2, Sentry/Crashlytics, release signing keystore, Play Console Internal Track.
- Локально не измерено: `bundletool get-size total`, холодный старт подписанного release AAB на физическом устройстве, live Docker PostgreSQL 16 + Redis 7 + MinIO (последние два проверяются на уровне CI в `.github/workflows/ci.yml`).

---

### 1.3. Точный аудит сетевого контракта и переменных окружения

#### Клиентская конфигурация сети (`:app`)
- **Стек:** единый Ktor Client (`ktor-client-okhttp`, `ktor-client-auth`, `ktor-client-content-negotiation`, `ktor-client-websockets`, `ktor-serialization-kotlinx-json` `2.3.12`) в `TechEnglishApiClient.kt` (без Retrofit/Supabase).
- **Таймауты (`TechEnglishApiClient.kt:1209-1213`):** `CONNECT_TIMEOUT_MS = 10_000L`, `REQUEST_TIMEOUT_MS = 20_000L`, `SOCKET_TIMEOUT_MS = 20_000L`.
- **Базовый URL (`AndroidKeystoreTokenStore.kt:162-167, 281`):** дефолт `DEFAULT_API_BASE_URL = "https://api.techenglish.app"`, динамически меняется в DataStore через диалог в `CloudAccountSyncScreen.kt:402-426` (разрешены `https://`, `http://10.0.2.2`, `http://localhost`).
- **Network Security Config:**
  - `app/src/main/res/xml/network_security_config.xml`: `<base-config cleartextTrafficPermitted="false" />` (строгий HTTPS TLS 1.2+ в release).
  - `app/src/debug/res/xml/network_security_config.xml`: debug-исключение cleartext для `10.0.2.2`, `localhost`, `127.0.0.1`.
- **OAuth & WS транспорт:**
  - OAuth Deep Link (`AndroidManifest.xml:43-51`, `launchMode="singleTop"`): `techenglish://auth/callback`.
  - WebSocket: `POST /v1/ws/ticket?scope=rooms|pvp` → одноразовый тикет (TTL `30` с в Redis, атомарный `GETDEL`) → `wss://<host>/ws/rooms/{roomSlug}?ticket=...` и `wss://<host>/ws/pvp/{matchId}?ticket=...`.

#### Полный реестр маршрутов FastAPI (68 эндпоинтов в 10 роутерах + `/health`)
| Роутер / Файл | Маршруты | Вызов из `TechEnglishApiClient.kt` / Статус контракта |
|---|---|---|
| `main.py` (1) | `GET /health` | Не вызывается клиентом (подходит для warmup и healthcheck) |
| `auth.py` (14) | `POST /v1/auth/register`, `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/oauth/ticket/exchange`, `POST /v1/auth/guest/link`, `DELETE /v1/account` | Вызываются (`register`, `login`, `Auth/refresh`, `exchangeOAuthTicket`, `linkGuestProgress`, `deleteAccount`) |
| `auth.py` (browser / email / legacy) | `GET /v1/auth/oauth/{provider}/authorize`, `GET /v1/auth/oauth/{provider}/callback`, `POST /v1/auth/oauth/{provider}/exchange` (`410 Gone`), `POST /v1/auth/verify-email`, `POST /v1/auth/password-reset/request`, `POST /v1/auth/password-reset/confirm`, `GET /v1/account/delete-form`, `GET /v1/legal/privacy-policy` | OAuth `authorize`/`callback` идут через Custom Tabs; `verify-email` и `password-reset/*` не подключены в Android UI; `delete-form` ссылается на несуществующий `POST /v1/account/delete-request` (**GAP-02**) |
| `sync.py` (4) | `POST /v1/sync/events`, `GET /v1/sync/state`, `GET /v1/ota/manifest`, `GET /v1/ota/packs/{pack_id}` | Вызываются все 4; в `SyncBatchBodyDto` отсутствуют поля `play_integrity_token`/`nonce` (**GAP-05**) |
| `profile_moderation.py` (6) | `GET /v1/profile/me`, `PATCH /v1/profile/me`, `POST /v1/moderation/report`, `GET /v1/moderation/queue` (RBAC), `POST /v1/moderation/block/{target_user_id}`, `GET /v1/moderation/blocks` | Вызываются `report` и `block`; `profile/me` (GET/PATCH) и `moderation/blocks` не подключены в клиенте (**GAP-07**) |
| `social.py` (9) | `GET /v1/leaderboards/{tab}`, `GET /v1/friends`, `POST /v1/friends/add`, `GET /v1/friends/feed`, `POST /v1/friends/{friend_id}/ping`, `POST/DELETE /v1/notifications/device-token`, `GET /v1/friends/search`, `POST /v1/friends/feed/{event_id}/react` | Вызываются 7; `friends/search` и `feed/{id}/react` не вызываются из Android (**GAP-07**) |
| `community.py` (8) | `GET /v1/comments/{target_type}/{target_id}`, `POST /v1/comments`, `POST /v1/comments/{comment_id}/vote`, `GET /v1/decks`, `GET /v1/decks/{deck_id}`, `POST /v1/decks`, `POST /v1/decks/{deck_id}/download`, `POST /v1/decks/{deck_id}/rate` | Вызываются 7 (`GET /v1/decks/{deck_id}` не требуется при наличии `/download`) |
| `ai.py` (3) | `GET /v1/ai/quota`, `POST /v1/ai/interview`, `POST /v1/ai/code-review` | Вызываются все 3 |
| `squads.py` (4) | `GET /v1/squads/my`, `POST /v1/squads`, `POST /v1/squads/join`, `POST /v1/squads/leave` | Вызываются все 4 |
| `crowdsource.py` (5) | `GET /v1/crowdsource/eligibility`, `GET /v1/crowdsource/prs`, `POST /v1/crowdsource/prs`, `POST /v1/crowdsource/prs/{pr_id}/vote`, `POST /v1/crowdsource/prs/{pr_id}/approve` (RBAC) | Вызываются 4 клиентских; `/approve` — серверный RBAC для `MODERATOR`/`ADMIN` |
| `realtime.py` (9) | `GET /v1/rooms`, `GET /v1/rooms/{room_slug}/messages`, `POST /v1/rooms/{room_slug}/messages`, `POST /v1/ws/ticket`, `WS /ws/rooms/{room_slug}`, `POST /v1/pvp/matchmake`, `POST /v1/pvp/{match_id}/submit`, `POST /v1/pvp/{match_id}/disconnect`, `WS /ws/pvp/{match_id}` | Вызываются 8 (`GET /v1/rooms` не нужен, список зашит в `ThematicRoomChannel` enum) |
| `peer_review.py` (5) | `GET /v1/peer-review/daily-case`, `GET /v1/peer-review/submissions`, `GET /v1/peer-review/submissions/{sub_id}/voice-url`, `POST /v1/peer-review/submissions`, `POST /v1/peer-review/submissions/{sub_id}/feedback` | Вызываются все 5; несовпадение поля `voice_url` vs `download_url` (**GAP-01**) |

#### Полный реестр переменных окружения (сверка с кодом и расхождения с `docs/OWNER_ACTIONS.md`)
> **Важное расхождение документации и кода:** в `docs/OWNER_ACTIONS.md` указаны имена `S3_ENDPOINT_URL`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET`, `S3_REGION`, `PLAY_INTEGRITY_CERT_SHA256`, `PLAY_INTEGRITY_ACCESS_TOKEN`, тогда как реальный код `voice_storage.py` и `integrity.py` читает **другие имена** (приведены ниже).

**Backend `.env` (все переменные из `backend/app/` и `backend/alembic/`):**
| Группа | Имя переменной в коде | Где читается | Дефолт | Статус в `ENV=prod` |
|---|---|---|---|---|
| **Core** | `ENV` | `config.py:9,32` | `dev` | `prod` (или новый `staging`) |
| **DB** | `DATABASE_URL` | `config.py:10`, `alembic/env.py:30` | `sqlite:///./techenglish_dev.db` | **Обязательна** (`postgresql+psycopg://...`) |
| **Redis** | `REDIS_URL` | `config.py:11`, `redis_client.py:38` | `""` (`fakeredis`) | **Обязательна** (`rediss://...`, guard в `redis_client.py:48`) |
| **JWT** | `JWT_SECRET` | `config.py:12,40` | дефолт отклонён | **Обязательна**, `>= 32` символов (`startup.py:15`) |
| **JWT TTL** | `ACCESS_TOKEN_TTL_SECONDS`, `REFRESH_TOKEN_TTL_SECONDS`, `WS_TICKET_TTL_SECONDS` | `config.py:14-15`, `redis_client.py:13` | `1800`, `2592000`, `30` | Опционально |
| **Admin** | `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, `BOOTSTRAP_ADMIN_USERNAME` | `startup.py:35`, `cli_admin.py:38-52` | `""`, `""`, `root_admin` | Для создания первого `ADMIN` |
| **SMTP** | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_USE_TLS` | `email_sender.py:90-98` | `""`, `587`, `""`, `""`, `noreply@techenglish.app`, `true` | `SMTP_HOST` **обязательна** (`startup.py:17`) |
| **OAuth** | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OAUTH_ALLOWED_REDIRECT_URIS` | `oauth_provider.py:49,69-73,310-311` | `""`, base=`techenglish://auth/callback` | Нужны для входа через GitHub/Google |
| **Gemini** | `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_TIMEOUT_SECONDS`, `TECHENGLISH_AI_DAILY_LIMIT`, `AI_CACHE_TTL_SECONDS` | `ai_proxy.py:14-17,162-166` | `""`, `gemini-2.0-flash`, `12.0`, `15`, `1800` | Без ключа работает `heuristic-fallback` |
| **Voice S3** | `VOICE_STORAGE_ENDPOINT`, `VOICE_STORAGE_BUCKET`, `VOICE_STORAGE_ACCESS_KEY`, `VOICE_STORAGE_SECRET_KEY`, `VOICE_STORAGE_REGION`, `PEER_REVIEW_VOICE_TTL_DAYS`, `VOICE_STORAGE_LOCAL_DIR` | `voice_storage.py:13,165,202-211` (+ fallback `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`) | `""`, `techenglish-peer-voice`, `us-east-1`, `30` | `VOICE_STORAGE_ENDPOINT` + ключи обязательны для S3 (иначе локальный диск, а в `prod` нужна проверка в `startup.py`) |
| **Play Integrity** | `PLAY_INTEGRITY_STRICT`, `PLAY_INTEGRITY_PACKAGE_NAME`, `PLAY_INTEGRITY_CERT_SHA256_DIGESTS`, `PLAY_INTEGRITY_API_ACCESS_TOKEN`, `PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON` | `config.py:16`, `integrity.py:205-218`, `startup.py:22-27` | `false`, `com.techenglish.app`, `""`, `""`, `""` | **Все обязательны** в `ENV=prod` (`startup.py:20-28`); `SERVICE_ACCOUNT_JSON` пока не читается в `integrity.py`! |
| **FCM Push** | `FCM_PROJECT_ID`, `FCM_ACCESS_TOKEN` | `push_gateway.py:126-131` | `""`, `""` | Без них работает `FakePushNotificationGateway` |

**Android Build & `BuildConfig` (`app/build.gradle.kts`):**
- Текущие `buildConfigField`: `CRASH_REPORTING_ENABLED` (`false`), `SENTRY_DSN` (`""`), `TAJIK_UI_LOCALIZATION_ENABLED` (`false`).
- Подпись релиза (`signingConfigs.release`): `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Рекомендуется добавить: `API_BASE_URL` (через `BuildConfig.API_BASE_URL`) и `app/google-services.json` (в `.gitignore`).

---

### 1.4. Read-only аудит базы данных `content_pack/src/main/assets/databases/tech_english.db` и черновиков

Фактические замеры выполнены в режиме `sqlite3.connect("file:<path>?mode=ro", uri=True)`:

- **Физические параметры файла БД:**
  - Путь: `content_pack/src/main/assets/databases/tech_english.db`
  - Размер на диске: **108 253 184 байт (103.24 MiB / 108.25 MB)**; в сжатом виде внутри `.aab` (`:content_pack`): **10.75 MiB** (сжатие DEFLATE **~9.6×**).
  - Схема Room: `PRAGMA user_version = 6`, `room_master_table.identity_hash = '2fed6cc837b9cd4374ad9e7e58e087bc'`.
  - Параметры страниц: `page_size = 4096`, `page_count = 26 429`, `freelist_count = 0`, `journal_mode = delete`.
  - Объекты схемы: `36` SQLite-таблиц (29 Room entities + FTS4 теневые + системные), `49` SQLite-индексов (`31` именованный индекс), `4` FTS4 sync-триггера (`BEFORE/AFTER UPDATE/DELETE/INSERT` на `academic_terms`).

- **Количество строк по всем ключевым таблицам:**
  | Таблица | Строк | Ключевые метрики качества и покрытия |
  |---|---:|---|
  | `academic_terms` | **20 658** | `100% verified`, `20 658` уникальных `LOWER(term)`, `10` major categories, `36` доменов (от max `1 395` *Computer Networks & Network Protocols* до min `110` *Reinforcement Learning*), `155` подкатегорий, `15` использованных `source_id`, `translation_ru` = **20 658 (100%)**, `normalized_search` = **20 658 (100%)** |
  | `academic_terms_fts` | **20 658** | Внешний индекс FTS4 (`unicode61`): `docsize = 20 658`, `segments = 1 057`, `segdir = 5` (`4.84 MiB`) |
  | `words` | **255** | Базовые IT-термины; `transcription` = **255 (100%)**, `translation` = **255 (100%)** |
  | `word_senses` | **29** | Полисемия: всего **8** уникальных терминов (`Memory`, `Kernel`, `Pipeline`, `Register`, `Vector`, `Token`, `Channel`, `Frame`) — **0.04%** от корпуса |
  | `word_relations` | **3 723** | `3 106` `RELATED` + `617` `SYNONYM` (строятся в `corpus_builder.py:856` только для `terms[:1200]`) |
  | `programming_languages` | **19** | `Python, SQL, C, C++, Java, Kotlin, C#, Go, JavaScript, Swift, Scala, Assembly, Pascal, Ruby, Dart, VBA, Bash, Lua, Rust` |
  | `code_snippets` | **380** | Ровно `20` задач для каждого из `19` языков |
  | `doc_articles` | **166** | **166/166 (100%)** имеют `source_url` и `license`; 9 тегов, в т.ч. `45` `Error Logs` (~`74` диагностических логов) |
  | `scenarios` | **156** | `12` контекстов ровно по `13` сценариев (`FILL_BLANKS` 35, `PHRASE_CHOICE` 34, `SENTENCE_BUILD` 28, `TRANSLATION_CHOICE` 22, `LISTENING_CHOICE` 21, `INTERVIEW_ANSWER` 16) |
  | `work_phrases` | **312** | `12` категорий ровно по `26` фраз |
  | `categories` / `subcategories` / `word_sources` | **11 / 155 / 19** | `10` тематических категорий + `11: Мои слова` (`is_system = 1`) |
  | Кэш/пользовательские таблицы + `ota_content_meta` | **0** | Чистое исходное состояние (`study_logs`, `sync_queue`, `ota_content_meta`, кэши Stage 3–4C пустые) |

- **Профиль дискового пространства (`dbstat` + вес колонок `academic_terms`):**
  - `academic_terms`: **92.12 MiB** (89.2% файла, ~4.68 KiB/строка); `academic_terms_fts_segments`: **4.84 MiB**; `idx_academic_normalized_term`: **1.46 MiB**; `code_snippets`: **0.73 MiB**; `idx_academic_domain`: **0.67 MiB**; `doc_articles`: **0.35 MiB**; `scenarios`: **0.26 MiB**.
  - Крупнейшие колонки `academic_terms`: `normalized_search` (**21.65 MiB**, в среднем 1 098.8 Б/стр. из-за включения полных определений), `definition_ru` (**11.48 MiB**), `example_ru` (**7.88 MiB**), `academic_explanation_ru` (**7.80 MiB**), `practical_explanation` (**5.26 MiB**), `definition_en` (**4.95 MiB**), `translation_ru` (**3.75 MiB**).

- **Реальное покрытие IPA и `translation_tg` + статус черновиков:**
  - **IPA в `academic_terms`:** **8 048 / 20 658 (38.96%)** — у 100% заполнены `ipa_source_url` и `ipa_license = 'MIT / CC-BY-SA-3.0 (open-dict-data/ipa-dict & CMUdict)'`; без IPA — **12 610** терминов (в `words` — 255/255).
  - **`translation_tg` в релизной БД:** **19 / 20 658 (0.09%)** верифицированных человеком записей (дефицит до целевого ориентира `≥ 2 000`: **1 981**).
  - **Статус черновиков в `docs/tg_review/` и `tools/vocabulary_import/output/`:**
    - `docs/tg_review/terms_tg_draft.csv`: **150** записей (`status = pending_review`: 150, `verified`: 0).
    - `docs/tg_review/ui_strings_tg_draft.json`: **18** UI-строк (`status = pending_review`: 18, `verified`: 0).
    - `tools/vocabulary_import/output/tajik_translations_pending_review.json`: **2 050** черновиков с префиксом `[PENDING_NATIVE_REVIEW]` (`verification_status = pending_review`, в БД не включены).
    - `app/src/main/res/values-tg/strings.xml`: 4 строки (копии RU fallback), закрыто флагом `BuildConfig.TAJIK_UI_LOCALIZATION_ENABLED = false`.

---

## Раздел 2. План добавления новых данных и масштабирования контента (Data Expansion Plan)

### 2.1. Аудит конвейера данных

Текущий конвейер состоит из двух независимых ветвей:
1. **Сборка базовой БД (`scripts/build_seed_db.py`):**
   - Источники данных: `scripts/seed_data/` (`languages_data.py`, `vocabulary_data.py`, `snippets_part1.py`, `snippets_part2.py`, `docs_data.py`, `scenarios_data.py` + модули расширения `docs_v3_expansion.py`, `scenarios_v3_expansion.py`, `snippets_v3_expansion.py`) и `tools/vocabulary_import/` (`corpus_builder.py`, `lexicon_domains_a_e.py`, `lexicon_domains_f_j.py`, `lexicon_new_domains_v3.py`, `licensed_ipa_lexicon.py`, `sources_and_taxonomy.py`).
   - Скрипт читает экспортированную Room-схему `app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/6.json`, создаёт таблицы/индексы, пересчитывает `normalized_search`, ставит 4 FTS4-триггера, выполняет `INSERT INTO academic_terms_fts(academic_terms_fts) VALUES('rebuild')`, проверяет `PRAGMA foreign_key_check`, `PRAGMA integrity_check`, наличие 31 индекса, прогоняет `ANALYZE` и `VACUUM` и записывает `content_pack/src/main/assets/databases/tech_english.db`.
2. **Инкрементальное обновление (`ContentPackImporter.kt` + `ota_content_meta` + скрипты импорта):**
   - `ContentPackImporter` (Errata E3) в транзакции Room делает UPSERT по стабильному ключу `normalized_term`, **не перезаписывая** поля SRS-прогресса (`repetitions`, `ease_factor`, `interval_days`, `next_review_at`, `last_studied_at`, `is_favorite`, `is_in_srs`), проверяет SHA-256 и игнорирует термины с `verification_status != 'verified'`.
   - `CloudSyncEngine.syncOtaContentPacks()` опрашивает `GET /v1/ota/manifest`, сравнивает `sha256Hex` и `lastStatus == "APPLIED"` в таблице `ota_content_meta` (при несовпадении SHA-256 или падении фиксирует `FAILED` и повторяет попытку на следующем синке).

**Пять дефектов конвейера, которые необходимо исправить до массового пополнения:**
1. **`scripts/import_verified_tg.py` не сохраняет данные в исходники и ломает FTS4:** обновляет только `translation_tg` напрямую в `.db`. При пересборке `build_seed_db.py` переводы исчезнут, а FTS4 (`normalized_search`) не узнает о новых таджикских словах. *Решение:* сохранять одобренные строки в версионируемый `tools/vocabulary_import/verified_tg_translations.csv` (подхватывается `corpus_builder.py`) и при прямом апдейте БД пересчитывать `normalized_search`.
2. **`AcademicTermDao.updateContentPreservingSrs()` (`AcademicTermDao.kt:402-466`) не обновляет `normalized_search`:** при прилёте OTA-пакета с `translation_tg` или новыми синонимами существующий термин не находится в поиске по новым словам. *Решение:* передавать и обновлять `normalized_search = :normalizedSearch` (FTS4-триггер `AFTER UPDATE` переиндексирует строку автоматически).
3. **`ContentPackImporter.importFromJson()` (`ContentPackImporter.kt:231`) ограничен только `terms`:** `ContentPackPayload` уже умеет принимать `docArticles`, `scenarios`, `workPhrases`, `codeSnippets`, `sources`, `subcategories`, но `ContentPackJsonDto` декодирует только `terms`. *Решение:* расширить JSON-контракт OTA на все типы контента.
4. **`tools/vocabulary_import/vocabulary_importer.py` и `vocabulary_validator.py` отстали от схемы v6:** импортер не пишет `ipa_source_url`, `ipa_license`, `normalized_search`; валидатор хранит константу `VALID_DOMAINS_30` (фактически доменов уже 36) и не проверяет provenance IPA.
5. **Хардкод `OTA_PACKS` в `backend/app/services/sync_service.py:31-60`:** пакеты зашиты в коде Python-словаря. Также присутствует рассинхронизация `category_id` (для `academic_terms` используется `101..110`, а в таблице `categories` лежат `1..11`). *Решение:* вынести каталог OTA в внешний `manifest.json` с `download_url` и унифицировать маппинг категорий.

---

### 2.2. Что и в каком объёме добавлять + требования к качеству

#### Целевая матрица расширения контента
| Слой данных | Факт v3.1.0 | Этап v3.2 (первая волна) | Этап v3.5 (масштаб) | Способ доставки |
|---|---:|---:|---:|---|
| `translation_tg` (верифицированный) | **19** | **169+** (ревью `docs/tg_review/terms_tg_draft.csv`) | **2 000–3 500+** (пакетная выверка `tajik_translations_pending_review.json` + краудсорсинг PRs) | OTA-паки по 250–500 шт. + включение в Seed DB при релизах |
| `values-tg` UI-локализация | **4** строки (RU) | **18** → **250+** (экстракция строк Compose в `values/strings.xml`) | Полная UI-локализация | Обновление ресурсов APK/AAB, флаг `TAJIK_UI_LOCALIZATION_ENABLED=true` |
| `word_senses` (полисемия) | **29** (8 терминов) | **150+** (50+ терминов) | **400+** (120+ многозначных терминов) | Seed DB (и расширение OTA) |
| `phonetic_transcription` (IPA) | **8 048** (39.0%) | **13 500+** (~65%) | **16 000+** (>75%) | Пополнение `licensed_ipa_lexicon.py` из open-source IPA словарей + OTA |
| `academic_terms` (устранение перекосов) | **20 658** (min домен 110) | **23 000** (добить 5 доменов `< 250` до `>= 400`: *RL*, *Blockchain*, *Compiler Design*, *Embedded*, *Graphics*) | **28 000–30 000** (новые кластеры: *LLM/Agentic AI*, *Observability/OTel*, *Platform/K8s*) | Месячные тематические OTA (500 шт.) + квартальная пересборка Seed DB |
| `word_relations` | **3 723** (`terms[:1200]`) | **12 000+** (топ-5 000 терминов) | **25 000+** | Seed DB (`corpus_builder.py:856`) |
| `doc_articles` + Debug the Log | **166** (~74 логов) | **220+** (+54 реальных лога: K8s, PG, Redis, Compose, FastAPI, TLS/OAuth) | **300+** | OTA-паки + Seed DB |
| `scenarios` / `work_phrases` | **156 / 312** | **216 / 450** (+5 сценариев × 12 контекстов) | **264 / 600** | OTA-паки + Seed DB |
| `code_snippets` | **380** (19×20) | **420+** (добавить TypeScript или +2 сложные задачи × 19 языков) | **480–570** (20–30 задач на язык) | Seed DB + OTA |

#### Жёсткие требования к качеству данных (Data Quality Contract)
1. **Единые форматы:** CSV (UTF-8 без BOM) для табличной выверки переводчиками (`terms_tg_*.csv`), JSON по расширенной схеме `tools/vocabulary_import/vocabulary_schema.json` для OTA-пакетов и импорта.
2. **Обязательный provenance (без вымышленных ссылок):**
   - Термины и статьи документации (`doc_articles`) обязаны содержать реальный `source_url` (IETF RFC, Linux Kernel, PostgreSQL, Kotlin/Android Docs, OWASP, MDN, PyTorch и др.) и открытую/допустимую `license` (`CC-BY-4.0`, `CC-BY-SA-4.0`, `MIT`, `Apache-2.0`, `Public Domain` и т.п.).
   - Запрещены сгенерированные «из головы» IPA-транскрипции: если заполнено `phonetic_transcription`, обязательны непустые `ipa_source_url` и `ipa_license` (из `licensed_ipa_lexicon.py`: `open-dict-data/ipa-dict` или `CMUdict`).
3. **Запрет непроверенных переводов (Правило R7):** любые переводы из LLM/MT имеют статус `pending_review` и хранятся только в `docs/tg_review/` или `tools/vocabulary_import/output/`. В `tech_english.db` и релизные OTA попадают строго записи со статусом `verification_status == "verified"`.
4. **Защита от дублей и конфликтов:** нормализация `normalize_term()` (`re.sub(r"\s+", " ", raw.strip().lower())`), проверка коллизий и конфликтующих `translation_ru` не только внутри пакета, но и по текущей `tech_english.db`.
5. **Обязательная CI-проверка:** запуск `python3 tools/vocabulary_import/vocabulary_validator.py` на каждый PR, добавляющий данные.

---

### 2.3. Стратегия доставки новых данных (Seed DB vs OTA Packs)

- **Базовая `tech_english.db` (`:content_pack`, `install-time` PAD):**
  - *Что зашиваем:* основной словарь (`20 658 → 28 000–30 000` терминов), базовые 19 языков и сниппеты, `word_senses`, `word_relations`, базовые статьи/логи и сценарии, а также **полностью предсобранный FTS4-индекс** (чтобы приложение сразу после установки работало на 100% офлайн).
  - *Частота и лимиты:* обновлять только в минорных/мажорных релизах раз в квартал. Внутри AAB `tech_english.db` весит **103.24 MiB** на диске и всего **10.75 MiB** сжатая в `content_pack` (суммарный размер загрузки `base + content_pack` = **15.91 MiB**, тогда как лимит Google Play для `install-time` asset pack — **1 GiB**, а порог предупреждения мобильной сети Play — **200 MB**).
  - *Практический потолок:* удерживать **несжатую `tech_english.db` ≤ 150–180 MiB** (сжатый `content_pack` ≤ 16–19 MiB), чтобы не замедлять первичную распаковку `DatabaseBootstrapManager` на бюджетных смартфонах и укладываться в разумный объём RAM/диска.
- **Инкрементальные OTA-пакеты (JSON / `.json.gz` + SHA-256 через `ContentPackImporter` + `ota_content_meta`):**
  - *Что выносим:* регулярные тематические наборы новых терминов (по 200–1 000 шт., **~80–400 КБ** в gzip), батчи проверенных таджикских переводов `translation_tg` (по 200–500 шт. по мере готовности), одобренные краудсорсинговые Dictionary PRs, свежие логи для Debug the Log, новые рабочие сценарии и фиксы опечаток.
  - *Почему не полная замена `.db` по сети:* скачивание целой `tech_english.db` (~10–18 МБ в сжатом виде, ~103–180 МБ на диске) для добавления 200 слов создаёт лишний трафик и рискует повредить пользовательские таблицы (`study_logs`, `sync_queue`, `sm2`). Дельта-OTA весит **< 0.5 МБ** и сохраняет 100% SRS-прогресса.
- **Где бесплатно и быстро раздавать OTA-пакеты (разгрузка бесплатного бэкенда):**
  - **Основной выбор — Cloudflare R2 (публичный бакет `techenglish-ota`):** 10 GB хранения, 10M Class B чтений/мес, **$0 за исходящий трафик (egress)**, быстрый глобальный CDN.
  - **Запасной выбор (без привязки карты) — GitHub Releases:** отдельный публичный репозиторий `techenglish-content-packs`, релиз-теги с файлами `pack_<id>_v<N>.json.gz` и `manifest.json` — безлимитный CDN-трафик и иммутабельные URL.
  - *Контракт:* FastAPI `GET /v1/ota/manifest` возвращает только лёгкий JSON-индекс (`pack_id`, `version`, `sha256_hex`, `download_url`, `size_bytes`, ~2–5 КБ) с кэшированием на 1 час (`Cache-Control`), а сам файл пакета Android качает **напрямую с R2 / GitHub Releases** по HTTPS и валидирует по `sha256_hex`.

---

### 2.4. Влияние роста данных на клиент и сервер

| Объём `academic_terms` | Несжатая `tech_english.db` | Сжатый PAD `:content_pack` (AAB) | Задержка FTS4 поиска (`unicode61`) | Fallback `LIKE '%...%'` | Распаковка `DatabaseBootstrapManager` (1-й запуск) |
|---:|---:|---:|---:|---:|---|
| **20 658** (факт v3.1.0) | **103.24 MiB** | **10.75 MiB** | **~6–9 мс** | ~25–45 мс | **514 мс** (Robolectric) / ~1.0–1.5 с (смартфон) |
| **30 000** (бенчмарк v3.1.0) | **~150 MiB** | **~15.6 MiB** | **8.13 мс** (измерено!) | ~40–70 мс | ~1.5–2.2 с (нужно ~300 MiB свободного места для `.tmp`) |
| **40 000** | **~200 MiB** | **~20.8 MiB** | ~10–12 мс | ~55–100 мс | ~2.0–3.0 с (нужно ~400 MiB свободного места) |
| **50 000** | **~250 MiB** | **~26.0 MiB** | ~12–15 мс (`O(log N)`) | ~70–150 мс (`O(N)`) | ~2.5–4.0 с (рекомендуется уже сплитить/оптимизировать) |

- **Резерв оптимизации размера SQLite:** сейчас колонка `academic_terms.normalized_search` весит **21.65 MiB** (21% всей БД, в среднем 1 099 Б на строку), так как в неё дублируются полные `definition_en` и `definition_ru`. Если при росте до 40–50K размер станет критичным, исключение длинных определений из `normalized_search` (оставив `term`, `abbreviations`, `translation_ru`, `translation_tg`, `synonyms`) уменьшит `.db` примерно на **18–20%** без потери поиска по терминам.
- **Оценка памяти клиента:** `Sp203`/`Sp204` используют пагинированный `LIMIT 40 OFFSET ?`, поэтому рост БД до 50K практически не увеличивает heap Android-приложения (в Robolectric тестам выделено `maxHeapSize = "1536m"` из-за параллельных прогонов и кэша тестовых БД).
- **Влияние на сервер (PostgreSQL 500 MB Free Tier):** справочник терминов, сниппеты и статьи **вообще не хранятся на сервере**. В PostgreSQL хранятся только пользовательские данные (`users`, `sync_events`, `sm2_states`, `comments`, `room_messages`, `pvp_matches`, `peer_review_submissions`, `dictionary_prs`, `ai_usage_logs`). При среднем следе **~80–150 КБ** на активного пользователя за год бесплатные **500 МБ** Postgres вмещают **~2 500–4 000 активных пользователей** (при регулярной очистке `ai_usage_logs` старше 30 дней и завершённых `pvp_matches` старше 60 дней).

---

## Раздел 3. Архитектура временного БЕСПЛАТНОГО хостинга ($0/мес)

### 3.1. Compute: FastAPI + WebSockets + Docker

| Вариант | Ресурсы Free Tier | Docker & WebSockets | Cold Start / Засыпание | Требование карты | Вердикт |
|---|---|---|---|---|---|
| **Render Free Web Service** | 512 MB RAM, 0.1 CPU, 750 ч/мес, 100 GB bandwidth | Да (`backend/Dockerfile`), полноценные `wss://` + авто-HTTPS | Засыпает через **15 мин** без трафика; пробуждение **30–60 с** | **Нет** (достаточно GitHub) | **ОСНОВНОЙ ВЫБОР** — простейший старт из Docker без карты, 750 ч/мес хватает на 1 круглосуточный сервис (744 ч) |
| **Koyeb Free** | 512 MB RAM, 0.1 vCPU, 1 сервис (Nano) | Да (`Dockerfile`), `wss://` + авто-HTTPS | Scale-to-zero через 60 мин; пробуждение **10–30 с** | Часто просит карту для антифрода | **ЗАПАСНОЙ №1** — быстрее пробуждение при наличии карты |
| **Oracle Cloud Always Free (ARM A1)** | До 4 OCPU, 24 GB RAM, 200 GB, 10 TB egress | Полный Linux VM — можно поднять весь `docker-compose.yml` | **0 с (не засыпает)** | Жёсткая верификация картой + нехватка квот ARM | **ЗАПАСНОЙ №2 (идеален при прохождении регистрации)** |
| **Hugging Face Spaces (Docker)** | 2 vCPU, 16 GB RAM | Да, порт `7860`, WS работают | Засыпание через **48 ч** простоя | Нет | Только для быстрого dev-прототипа (специфичный домен `*.hf.space`) |

### 3.2. PostgreSQL 16 + Alembic

- **Сравнение:**
  - **Neon Free (ОСНОВНОЙ):** PostgreSQL 16/17, **512 MB** на ветку, **191.9 compute-hours/мес**, автопауза через 5 мин простоя (пробуждение **1–2 с**), TLS `sslmode=require`, **без карты**.
  - **Supabase Free (ЗАПАСНОЙ):** **500 MB** БД, ставится на паузу через **7 дней** полного простоя, прямое IPv4-подключение закрыто (нужен Session/Transaction Pooler на порту 5432/6543).
  - **Aiven Free:** **1 GB** диск, 20 подключений (требует подтверждения аккаунта).
- **Проверка совместимости с `backend/`:**
  - В `backend/requirements.txt` указан драйвер `psycopg[binary]==3.2.3` (psycopg v3), а НЕ `psycopg2`. Поэтому стандартный URL от Neon (`postgresql://user:pass@host/db?sslmode=require`) **обязательно** должен иметь префикс **`postgresql+psycopg://`**.
  - `backend/alembic.ini` содержит пустой `sqlalchemy.url =`, поэтому `backend/alembic/env.py:30` корректно подхватывает `DATABASE_URL` из переменных окружения.
  - Миграции `alembic upgrade head` запускаются строго по **прямому (unpooled)** URL Neon (без `-pooler` в имени хоста). В `models.py:382-384` при переходе на Neon/Supabase обязательно добавить `pool_pre_ping=True` (см. п. 3.6).

### 3.3. Redis 7 (WS-тикеты, Pub/Sub, PvP matchmaking, rate limits)

- **Сравнение:**
  - **Upstash Redis Free:** **256 MB** памяти, **500 000 команд/мес**, обязательный TLS (`rediss://`), до 100 одновременных клиентов, поддерживает `GETDEL` и Pub/Sub, без карты.
  - **Redis Cloud Free:** **30 MB** памяти, до 30 подключений, **без жёсткого месячного лимита команд** (ограничен пропускной способностью), поддерживает TLS и `GETDEL`.
- **Выбор:** **Upstash Redis** (если важен `rediss://` и большой буфер 256 MB при умеренных тестах) или **Redis Cloud Free** (если планируется активный тест WebSocket-комнат/PvP без риска сжечь 500K команд).
- **Аудит совместимости `redis_client.py` и `realtime_broker.py`:**
  1. `redis-py==5.2.1` поддерживает `rediss://` и `GETDEL` (`redis_client.py:43,81`).
  2. **Найденная уязвимость:** `redis.Redis.from_url(url, decode_responses=True)` (`redis_client.py:43`) не передаёт `health_check_interval=30`, `socket_keepalive=True`, `retry_on_timeout=True`. На серверах Upstash idle TCP-соединения периодически закрываются балансировщиком — без `health_check_interval` `forward_pubsub_to_websocket` (`realtime_broker.py:121-123`) поймает `ConnectionError` и молча завершит трансляцию в WS.
  3. **Нагрузка по командам и потокам:** в `realtime_broker.py:94,114` для каждого открытого WS запускается `asyncio.to_thread(self.read_next_event, pubsub, 0.02)`. Приятный факт: `pubsub.get_message()` читает из уже открытого сокета и **не тратит команды Upstash** каждые 20 мс (команды списываются только при `SUBSCRIBE`, `PUBLISH`, `SET`, `GETDEL`, `RPUSH`, `LPOP`). Однако у стандартного `ThreadPoolExecutor` Python на 1 vCPU в бесплатном контейнере всего **5 потоков**: при `> 5` одновременных WS-подключениях начнутся задержки до переключения на неблокирующий `redis.asyncio` (на старте до 5 тестеров не критично, но важно ограничить concurrency).

### 3.4. S3-хранилище голосовых клипов Peer Review (TTL 30 дней)

- **Сравнение:**
  - **Cloudflare R2 Free (ОСНОВНОЙ):** **10 GB/мес** хранения, 1M Class A, 10M Class B операций/мес, **$0 за исходящий трафик**, S3v4 API, Lifecycle Rule 30d настраивается в 2 клика. (Обычно требует привязку карты с $0 списанием).
  - **Backblaze B2 Free (ЗАПАСНОЙ без карты):** **10 GB** хранения, **1 GB/день** бесплатного скачивания, S3-совместимый endpoint (`s3.<region>.backblazeb2.com`), Lifecycle Rules доступны.
  - **Supabase Storage:** **1 GB**, S3-совместимость, $0.
- **Аудит совместимости с `backend/app/voice_storage.py`:**
  - `S3VoiceObjectStorage` (`voice_storage.py:80-109`) использует `signature_version="s3v4"`, `addressing_style="path"` и при первом `put_voice_clip()` вызывает `ensure_bucket_and_lifecycle()` (`head_bucket` → `create_bucket` → `put_bucket_lifecycle_configuration`).
  - Для Cloudflare R2: `VOICE_STORAGE_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com`, `VOICE_STORAGE_REGION=auto`. Если API-токен выдан с минимальными правами (*Object Read & Write* на конкретный бакет без прав *Admin*), вызов `create_bucket()`/`put_bucket_lifecycle_configuration()` вернёт `AccessDenied` (`ClientError`). Поэтому нужно создать бакет и правило **TTL 30 дней** в панели R2/B2 заранее, а в `voice_storage.py` добавить переменную `VOICE_STORAGE_SKIP_BUCKET_INIT=true` (или игнорирование 403 на lifecycle).
  - *Важное преимущество облачного S3:* presigned GET URL (`s3://.../voice.m4a?X-Amz-...`) указывает прямо на публичный HTTPS R2/B2, поэтому `TechAudioController.playRemoteAudioUrl()` на смартфоне скачивает аудио напрямую, не нагружая 512 МБ контейнер Render! (Нужно только исправить имя поля `voice_url` ↔ `download_url` по **GAP-01**).

### 3.5. Внешние бесплатные сервисы ($0/мес)

1. **Gemini API (Google AI Studio Free Tier):** ключ `GEMINI_API_KEY`, модель `GEMINI_MODEL=gemini-2.0-flash` (или `gemini-2.5-flash`). Реализуется через `HttpxGeminiClient` (`ai_proxy.py:113-159`). Бесплатный тариф Google AI Studio ограничивает RPM/RPD (зависит от актуальной политики Google, ориентировочно 10–15 RPM, 250–1 500 RPD). При превышении сработает встроенный `heuristic-fallback`.
2. **Firebase Spark Plan (FCM Push):** `$0/мес`, безлимитные push-уведомления. Требует: Android — `app/google-services.json` + плагин Google Services + `FirebaseMessagingService`; Backend — `google-auth` для автоматического обновления OAuth2-токена из Service Account JSON вместо протухающего за 1 час `FCM_ACCESS_TOKEN`.
3. **GitHub & Google OAuth 2.0:** `$0/мес`. В Google Cloud Console (тип `Web application`, режим `Testing` — до 100 тестовых пользователей без длительной верификации приложения) прописывается callback `https://<api-domain>/v1/auth/oauth/google/callback`; в GitHub OAuth Apps — `https://<api-domain>/v1/auth/oauth/github/callback`. Приложение возвращает редирект на `techenglish://auth/callback`. *Критично:* Uvicorn должен запускаться с `--proxy-headers --forwarded-allow-ips="*"`, иначе `request.url_for("oauth_callback")` внутри Render/Koyeb сгенерирует `http://` вместо `https://` и OAuth сломается!
4. **Бесплатный SMTP (верификация email и сброс пароля):** **Brevo Free** (300 писем/день, `smtp-relay.brevo.com`, порт `587`, STARTTLS — не требует покупки домена для старта теста) или **Resend Free** (100 писем/день, 3 000/мес — требует DNS-верификации своего домена). Полностью совместимы с `SmtpEmailSender` (`email_sender.py:20-66`).
5. **Юридические страницы (`/privacy`, `/terms`) и форма удаления аккаунта:** **GitHub Pages** (`https://<user>.github.io/techenglish-legal/`) ИЛИ статические HTML-роуты прямо в `FastAPI` (`GET /privacy`, `GET /terms`, `GET /v1/account/delete-form` + рабочий `POST /v1/account/delete-request`). Второй вариант упрощает старт без отдельного сайта.
6. **Раздача OTA-пакетов и тестовых APK:** **GitHub Releases** (для universal APK и OTA-архивов) и публичный бакет **Cloudflare R2** (для OTA `manifest.json` + `*.json.gz`).
7. **Crash-мониторинг (опционально):** **Sentry Developer Free** (5 000 ошибок/мес, передача `CRASH_REPORTING_ENABLED=true` и `SENTRY_DSN` в release-сборку после подключения реального Sentry Android SDK в `ConfigurableCrashReporter`).

### 3.6. Ограничения бесплатного хостинга и обязательные адаптации в коде

1. **Холодный старт / засыпание контейнера (Render 30–60 с + Neon 1–2 с):**
   - `TechEnglishApiClient.kt:1209-1213`: таймауты `CONNECT=10s`, `REQUEST=20s`, `SOCKET=20s` гарантированно сорвут первый запрос после простоя. **Адаптация:** увеличить до `CONNECT=15_000L`, `REQUEST=65_000L`, `SOCKET=65_000L` (или сделать предварительный retry на `HttpRequestTimeoutException` + UI-статус *«Пробуждаем сервер (до 45 с)…»*).
   - `CloudSyncWorker.kt:386-391`: при сбое `runFullSyncNow()` часто глушит исключение внутри `runCatching` и возвращает `Result.success()`. **Адаптация:** возвращать `Result.retry()` при сетевых ошибках/таймаутах, чтобы `WorkManager` повторил синк с backoff после пробуждения Render.
   - `ThematicRoomsViewModel.kt:174-200`: однократное падение WS переводит экран в `WsConnectionState.ERROR`. **Адаптация:** добавить цикл auto-reconnect с задержками `2s → 5s → 15s → 30s` (пока экран активен) + предварительный `GET /health` до `POST /v1/ws/ticket`.
   - `PvpDuelViewModel.kt:155-193`: таймер матчмейкинга равен 15 с. Если сервер спит, `matchmakePvp()` может зависнуть дольше или упасть. **Адаптация:** перед стартом отсчёта 15 с пинговать `/health` с индикатором *«Подключение к арене…»*.
   - **Лайфхак с внешним пингером (Cron-job.org / UptimeRobot раз в 14 минут на `GET /health`):** на Render Free (750 ч/мес = 31.25 суток) 1 сервис может **вообще не засыпать** (0 с cold start)! Поскольку `/health` (`main.py:95-97`) не делает запросов в БД, **Neon PostgreSQL всё равно засыпает через 5 минут** и без проблем укладывается в лимит 191.9 compute-hrs/мес!
2. **Жёсткий лимит RAM 512 МБ (Render / Koyeb):**
   - `backend/Dockerfile:20`: запускать строго с `--workers 1 --limit-concurrency 64` (1 процесс FastAPI/Uvicorn занимает ~90–140 МБ RAM с `boto3` и `psycopg`; `--workers 2` упрётся в OOMKilled на 512 МБ).
   - `backend/app/models.py:382-384`: настроить SQLAlchemy пул для PostgreSQL: `pool_size=3`, `max_overflow=4`, `pool_pre_ping=True`, `pool_recycle=280`, `pool_timeout=15` — это экономит память и мгновенно восстанавливает соединения после засыпания Neon.
   - `backend/app/ai_proxy.py:18`: уменьшить `AI_CACHE_MAX_SIZE` до `256` (или вынести в `os.getenv`), чтобы кэш промптов не рос в памяти.
3. **Защита квот и бесплатных лимитов:**
   - Снизить `TECHENGLISH_AI_DAILY_LIMIT=10` в `.env` на время Free Tier (экономит RPD в Google AI Studio).
   - Перенести `OAuthFlowStore` и `_AUTH_RATE_BUCKETS` в Redis с TTL (чтобы защита от brute-force не сбрасывалась при пробуждении Render).
   - Ограничить частоту фонового `CloudSyncWorker` (уже стоит разумный минимум — 1 раз в 6 часов в `CloudSyncEngine.kt:350`).

### 3.7. Сводная таблица стека ($0/мес → платный VPS)

| Компонент | Бесплатный сервис (Основной / Запасной) | Лимиты Free Tier | Нужные переменные `.env` (по коду) | На сколько хватит пользователей | Что поменять при переезде на платный VPS |
|---|---|---|---|---|---|
| **Compute (FastAPI + WS)** | **Render Free** / *Koyeb Free* (или *Oracle Always Free ARM*) | 512 MB RAM, 0.1 CPU, 750 ч/мес, 100 GB egress, sleep 15m | `ENV=staging` (или `prod`), `JWT_SECRET`, `BOOTSTRAP_ADMIN_*` | ~50–150 DAU, до 20–40 одновременных запросов/WS | Запустить `backend/docker-compose.yml` + Caddy за 15 мин, увеличить `--workers 2..4`, переключить DNS `api.techenglish.app` |
| **PostgreSQL 16** | **Neon Free** / *Supabase Free* | 512 MB storage, 191.9 compute-hrs/mo, sleep 5m | `DATABASE_URL=postgresql+psycopg://...?sslmode=require` | **~2 500–4 000** зарегистрированных (при чистке старых логов) | `pg_dump` (Neon) → `pg_restore` в контейнер `postgres:16-alpine` на VPS, сменить `DATABASE_URL` |
| **Redis 7** | **Upstash Free** / *Redis Cloud Free* | Upstash: 256 MB, **500K cmd/mo** (`rediss://`); Redis Cloud: 30 MB, безлимит команд | `REDIS_URL=rediss://default:<pwd>@<host>:<port>` | Upstash: ~50–150 DAU (при 15–30K cmd/день); Redis Cloud: ~500 DAU | Указать `REDIS_URL=redis://redis:6379/0` в `docker-compose.yml` — нулевая задержка, 0 лимитов команд |
| **S3 Voice (TTL 30d)** | **Cloudflare R2** / *Backblaze B2* | R2: 10 GB, 10M read/mo, **$0 egress**; B2: 10 GB, 1 GB/day egress | `VOICE_STORAGE_ENDPOINT`, `VOICE_STORAGE_BUCKET`, `VOICE_STORAGE_ACCESS_KEY`, `VOICE_STORAGE_SECRET_KEY`, `VOICE_STORAGE_REGION=auto`, `PEER_REVIEW_VOICE_TTL_DAYS=30` | **70 000–120 000** активных 15-сек M4A-клипов за 30 дней | **Можно не переносить** (оставить R2 с $0 egress) или перенести на MinIO за `https://s3.techenglish.app` |
| **OTA CDN + APK** | **Cloudflare R2** / *GitHub Releases* | R2: 10 GB, $0 egress; GitHub Releases: 2 GB/файл, безлимит трафика | Манифест `download_url` на R2 / GitHub | Неограниченно (тысячи установок/обновлений в месяц) | **Оставить на R2 / GitHub Releases навсегда** (CDN быстрее и дешевле любого VPS) |
| **AI Mentor** | **Google AI Studio (Gemini Free)** | ~10–15 RPM, ~250–1 500 RPD (по модели), авто-fallback | `GEMINI_API_KEY`, `GEMINI_MODEL=gemini-2.0-flash`, `TECHENGLISH_AI_DAILY_LIMIT=10` | ~25–150 активных AI-сессий в день | При росте переключить тот же ключ на Pay-as-you-go в Google Cloud без правки кода |
| **OAuth 2.0** | **GitHub OAuth** + **Google OAuth** | GitHub: безлимит; Google Testing: 100 пользователей (Production — безлимит после верификации) | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OAUTH_ALLOWED_REDIRECT_URIS` | До 100 в Testing → безлимит после публикации Google Consent Screen | Добавить свой домен `https://api.techenglish.app/v1/auth/oauth/*/callback` с самого начала — тогда ничего не менять |
| **Push (FCM)** | **Firebase Spark (FCM)** | Бесплатно без ограничений объёма | Backend: `FCM_PROJECT_ID`, Service Account JSON; Android: `app/google-services.json` | Безлимит | Не требует изменений при переезде |
| **Email SMTP** | **Brevo Free** / *Resend Free* | Brevo: 300 писем/сутки; Resend: 100/сутки | `SMTP_HOST`, `SMTP_PORT=587`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_USE_TLS=true` | 100–300 регистраций/сбросов пароля в день | Подключить свой домен SPF/DKIM в Brevo/Resend или AWS SES |
| **Legal + Delete Form** | **FastAPI** / *GitHub Pages* | $0 | Не требуются | Безлимит | Зафиксировать стабильный URL `https://api.techenglish.app/...` или `https://<user>.github.io/...` |
| **Crash Monitoring** | **Sentry Free** | 5 000 errors/mo | Android: `CRASH_REPORTING_ENABLED=true`, `SENTRY_DSN=...` | До ~500–1 000 MAU | Не требует изменений при переезде |

---

## Раздел 4. Пошаговый план реализации (от текущего кода до живого запуска)

Ниже разделены задачи **Агента (правка кода и тестов)** и **Владельца (ключи, облачные сервисы, контент, Play Console)**.

### Фаза 1. Подготовка данных и конвейера контента

| ID | Кто | Цель | Точные файлы / модули | Критерий готовности (DoD) | Сложность |
|---|---|---|---|---|---|
| **DATA-01** | Агент | Сохранение верифицированных таджикских переводов в исходники и пересчёт `normalized_search` для FTS4 | `scripts/import_verified_tg.py`, `tools/vocabulary_import/corpus_builder.py`, `tools/vocabulary_import/verified_tg_translations.csv` (новый) | Запуск `import_verified_tg.py` обновляет CSV-источник истины; `build_seed_db.py` подхватывает переводы и перестраивает `academic_terms_fts`; поиск на таджикском находит обновлённый термин | **S** |
| **DATA-02** | Агент | Актуализация импортера и валидатора под схему Room v6 и 36 доменов | `tools/vocabulary_import/vocabulary_importer.py`, `tools/vocabulary_import/vocabulary_validator.py`, `tools/vocabulary_import/vocabulary_schema.json`, `tools/vocabulary_import/sources_and_taxonomy.py` | Валидатор проверяет все 36 доменов, запрещает IPA без `ipa_source_url`/`ipa_license`, отклоняет `pending_review`; импортер заполняет `normalized_search`, `ipa_source_url`, `ipa_license` | **S** |
| **DATA-03** | Агент | Поддержка многотабличных OTA-паков и внешнего CDN (`download_url` + `.json.gz`) + обновление FTS4 при OTA | `app/.../dao/AcademicTermDao.kt`, `app/.../database/ContentPackImporter.kt`, `app/.../core/network/TechEnglishApiClient.kt`, `app/.../core/sync/CloudSyncEngine.kt`, `backend/app/routers/sync.py`, `backend/app/services/sync_service.py` | `updateContentPreservingSrs()` обновляет `normalized_search`; `ContentPackImporter` принимает термины, статьи, сценарии, фразы, сниппеты и связи; `GET /v1/ota/manifest` отдаёт `download_url` из внешнего `manifest.json` | **M** |
| **DATA-04** | Владелец + Агент | Ревью черновиков таджикского языка (R7) и генерация первых OTA-паков | `docs/tg_review/terms_tg_draft.csv` (150), `docs/tg_review/ui_strings_tg_draft.json` (18), `tools/vocabulary_import/output/tajik_translations_pending_review.json` (2 050), `app/src/main/res/values*/strings.xml` | Владелец переводит `status` одобренных записей в `verified`; импорт в `verified_tg_translations.csv` и выпуск первого `ota_tg_batch_01.json.gz`; при одобрении UI-строк и выносе Compose-строк в `strings.xml` включение `TAJIK_UI_LOCALIZATION_ENABLED=true` | **M** |
| **DATA-05** | Агент + Владелец | Расширение контента: IPA (8K → 13.5K+), полисемия (8 → 50+), `word_relations` (`> 12K`), добор 5 слабых доменов, +50 логов и +60 сценариев | `tools/vocabulary_import/licensed_ipa_lexicon.py`, `sources_and_taxonomy.py`, `corpus_builder.py`, `scripts/seed_data/` | Все 5 доменов `< 250` доведены до `>= 400`; у всех новых данных есть `source_url`/`license`; `vocabulary_validator.py` проходит с 0 ошибок | **M** |
| **DATA-06** | Агент | Пересборка `tech_english.db` и проверка отсутствия регрессий | `scripts/build_seed_db.py`, `content_pack/src/main/assets/databases/tech_english.db`, `TechEnglishDatabaseTest.kt`, `Sp204FtsAndCyrillicSearchTest.kt` | `PRAGMA integrity_check == ok`, `foreign_key_check == []`, `31/31` индексов, размер БД `<= 150 MiB`, FTS4 поиск `< 15 мс`, все тесты `:app` и `:domain` зелёные | **S** |

### Фаза 2. Доработки кода под бесплатный хостинг и релиз

| ID | Кто | Цель | Точные файлы / модули | Критерий готовности (DoD) | Сложность |
|---|---|---|---|---|---|
| **REL-01** | Агент | Исправить несовпадение поля `voice_url` / `download_url` в Peer Review | `TechEnglishApiClient.kt:579-583`, `backend/app/routers/peer_review.py:114-119`, тест в `Sp302PeerReviewVoiceTest.kt` | `@SerialName("voice_url")` (и/или оба поля на сервере), тест проверяет десериализацию реального ответа `GET /v1/peer-review/submissions/{id}/voice-url` | **S** |
| **REL-02** | Агент | Защитить офлайн-сообщения комнат `ROOM_MESSAGE_OUTBOX` от «съедания» синкером прогресса | `CloudSyncEngine.kt:93-124`, `Stage3ViewModels.kt:221-233`, `Sp304OfflineRoomsAndSquadCacheTest.kt` | `runFullSyncNow()` и `linkGuestAccount()` исключают `ROOM_MESSAGE_OUTBOX` (либо сначала вызывают отправку комнатных сообщений); добавлен регрессионный тест | **S** |
| **REL-03** | Агент | Закрыть 404 на форме удаления аккаунта и отдать HTML `/privacy`, `/terms` | `backend/app/routers/auth.py:414-437`, `backend/tests/` | Реализован `POST /v1/account/delete-request` (принимает email/подтверждение и логирует/ставит заявку или удаляет по токену), а также HTML-роуты `GET /privacy` и `GET /terms` | **S** |
| **REL-04** | Агент | Адаптировать `Dockerfile`, SQLAlchemy pool и Alembic под Render/Neon Free | `backend/Dockerfile`, `backend/app/models.py:382-388`, `backend/alembic/env.py:26-30` | В `Dockerfile`: `--port ${PORT:-8000} --workers 1 --proxy-headers --forwarded-allow-ips='*' --limit-concurrency 64`; в `models.py`/`env.py` нормализация `postgres://`/`postgresql://` → `postgresql+psycopg://`, `pool_pre_ping=True`, `pool_size=3`, `max_overflow=4`, `pool_recycle=280` | **S** |
| **REL-05** | Агент | Адаптировать Redis (`rediss://` + перенос состояний) и R2/B2 S3 (`VOICE_STORAGE_SKIP_BUCKET_INIT`) | `backend/app/redis_client.py`, `backend/app/oauth_provider.py`, `backend/app/deps.py`, `backend/app/voice_storage.py` | В Redis заданы `health_check_interval=30`, `socket_keepalive=True`, `retry_on_timeout=True`; `OAuthFlowStore` и `_AUTH_RATE_BUCKETS` перенесены в Redis; в `S3VoiceObjectStorage` добавлен флаг `VOICE_STORAGE_SKIP_BUCKET_INIT` и мягкий catch на lifecycle | **S** |
| **REL-06** | Агент | Добавить `ENV=staging` (для беты без Play Integrity), синхронизировать `startup.py` и `OWNER_ACTIONS.md` | `backend/app/config.py`, `backend/app/services/startup.py`, `docs/OWNER_ACTIONS.md` | В `ENV=prod` проверяется также наличие `VOICE_STORAGE_ENDPOINT` (если S3 обязателен); в `ENV=staging` работают реальные PG/Redis/S3/SMTP/JWT guard без требования Play Integrity; в `OWNER_ACTIONS.md` исправлены имена `VOICE_STORAGE_*` и `PLAY_INTEGRITY_*` | **S** |
| **REL-07** | Агент | Адаптировать Android под cold start (65 с), `BuildConfig.API_BASE_URL`, Ktor Auth, `MainActivity` и `CloudSyncWorker` | `app/build.gradle.kts`, `TechEnglishApiClient.kt`, `AndroidKeystoreTokenStore.kt`, `CloudSyncEngine.kt`, `ThematicRoomsViewModel.kt`, `PvpDuelViewModel.kt`, `MainActivity.kt`, `CloudAccountSyncScreen.kt` | Таймауты `15s/65s/65s`; добавлен `BuildConfig.API_BASE_URL`; убраны дублирующиеся ручные заголовки `Authorization`; `CloudSyncWorker` возвращает `Result.retry()` при таймаутах; WS имеет auto-reconnect; `PvpDuelViewModel` прогревает `/health`; `MainActivity` показывает загрузку при `Idle` и ошибку при `Error` | **M** |
| **REL-08** | Агент | Доподключить `GET/PATCH /v1/profile/me` и `POST /v1/friends/feed/{id}/react` в Android | `TechEnglishApiClient.kt`, `Stage3ViewModels.kt`, `CloudSyncEngine.kt` | Сохранение профиля и переключателя `acceptsPings` синхронизируется с `/v1/profile/me`; лайк в ленте отправляется на сервер с оффлайн-защитой | **S** |
| **REL-09** | Агент | Обеспечить сборку рабочего sideload-APK с вшитой БД для тестеров | `app/build.gradle.kts` (flavor `standalone` или CI step с `bundletool build-apks --mode=universal`) | Итоговый `.apk` для раздачи тестерам содержит `assets/databases/tech_english.db` и успешно запускается на физическом телефоне без Play Store | **S** |
| **REL-10** | Агент | Автоматическое получение OAuth2-токенов GCP через `google-auth` для FCM и Play Integrity | `backend/requirements.txt`, `backend/app/integrity.py`, `backend/app/services/push_gateway.py` | Поддержка `PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON` и `FCM_SERVICE_ACCOUNT_JSON` (кэшируемый автообновляемый Bearer токен через `google-auth`), сохраняя обратную совместимость со статическими токенами | **M** |
| **REL-11** | Агент + Владелец | Интеграция FCM SDK на Android | `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `PushNotificationGateway.kt`, `app/google-services.json` (владелец) | Добавлен `FirebaseMessagingService`, передающий FCM-токен в `DefaultPushNotificationGateway.registerDeviceToken()` и входящий push в `handleIncomingPush()`; сборка без `google-services.json` не ломается | **M** |
| **REL-12** | Агент + Владелец | Интеграция Google Play Integrity API на Android (для `ENV=prod` в Play Store) и Sentry SDK (опц.) | `app/build.gradle.kts`, `TechEnglishApiClient.kt`, `CloudSyncEngine.kt`, `CrashReporter.kt` | В `SyncBatchBodyDto` передаются `play_integrity_token` и `play_integrity_nonce`; пройдена связка Play Console ↔ GCP | **M** |

---

### Фаза 3. Пошаговая инструкция поднятия временного бесплатного хоста ($0/мес — действия владельца)

#### Шаг 1. Регистрация облачных сервисов (порядок минимизирует повторные настройки)
1. **PostgreSQL 16 (Neon Free):** создать проект `techenglish-db` (регион `eu-central-1` Frankfurt или ближайший). Скопировать **Direct connection string** (без `-pooler`) и заменить начало `postgresql://` → `postgresql+psycopg://`.
2. **Redis 7 (Upstash Free или Redis Cloud Free):** создать БД в том же регионе (`eu-central-1`), скопировать URI вида `rediss://default:<PASSWORD>@<HOST>:<PORT>`.
3. **S3 для голоса и OTA (Cloudflare R2 или Backblaze B2):**
   - Создать приватный бакет `techenglish-peer-voice` и добавить **Lifecycle Rule**: удаление всех объектов через **30 дней**.
   - Создать второй публичный бакет `techenglish-ota` (или публичный репозиторий на GitHub Releases) для раздачи `*.json.gz` OTA-паков.
   - Выпустить S3 API-ключ и секрет, записать Endpoint (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com` для R2, регион `auto`).
4. **SMTP (Brevo Free):** зарегистрироваться, перейти в *SMTP & API → SMTP*, сгенерировать SMTP-ключ (`smtp-relay.brevo.com`, порт `587`, логин, пароль).
5. **Gemini API (Google AI Studio):** создать API-ключ на `https://aistudio.google.com/apikey`.
6. **OAuth 2.0 (GitHub + Google):**
   - В GitHub (*Settings → Developer settings → OAuth Apps*): создать App `TechEnglish`, Callback URL = `https://<твой-render-домен>/v1/auth/oauth/github/callback` (или `https://api.techenglish.app/...`).
   - В Google Cloud Console (*APIs & Services → Credentials → OAuth client ID → Web application*): Authorized redirect URI = `https://<твой-render-домен>/v1/auth/oauth/google/callback`; в OAuth Consent Screen добавить email тестеров.
7. **Compute (Render Free):**
   - Подключить GitHub-репозиторий → *New Web Service* → Root Directory: `backend`, Runtime: `Docker`, Plan: `Free`.
   - Health Check Path: `/health`.
   - Заполнить Environment Variables (см. шаблон ниже).

#### Шаг 2. Готовый шаблон `.env` для Render (режим `ENV=staging` для живого бета-запуска)
```dotenv
# Режим: staging для universal APK без Play Integrity; prod для релиза в Google Play
ENV=staging
PLAY_INTEGRITY_STRICT=false

# Безопасность (сгенерировать через: openssl rand -hex 32)
JWT_SECRET=<СЛУЧАЙНАЯ_СТРОКА_МИНИМУМ_32_СИМВОЛА>
ACCESS_TOKEN_TTL_SECONDS=1800
REFRESH_TOKEN_TTL_SECONDS=2592000
WS_TICKET_TTL_SECONDS=30

# PostgreSQL 16 (Neon Direct URL с префиксом postgresql+psycopg://)
DATABASE_URL=postgresql+psycopg://<USER>:<PASS>@<EP_HOST>.eu-central-1.aws.neon.tech/<DB>?sslmode=require

# Redis 7 (Upstash TLS или Redis Cloud)
REDIS_URL=rediss://default:<PASSWORD>@<UPSTASH_HOST>:<PORT>

# SMTP (Brevo Free)
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USER=<BREVO_LOGIN_EMAIL>
SMTP_PASSWORD=<BREVO_SMTP_KEY>
SMTP_FROM=noreply@techenglish.app
SMTP_USE_TLS=true

# OAuth 2.0
GITHUB_CLIENT_ID=<GH_ID>
GITHUB_CLIENT_SECRET=<GH_SECRET>
GOOGLE_CLIENT_ID=<GOOGLE_CLIENT_ID>
GOOGLE_CLIENT_SECRET=<GOOGLE_SECRET>
OAUTH_ALLOWED_REDIRECT_URIS=techenglish://auth/callback

# AI Proxy (Google AI Studio Free)
GEMINI_API_KEY=<AI_STUDIO_KEY>
GEMINI_MODEL=gemini-2.0-flash
GEMINI_TIMEOUT_SECONDS=12.0
TECHENGLISH_AI_DAILY_LIMIT=10
AI_CACHE_TTL_SECONDS=1800

# Voice S3 (Cloudflare R2 / Backblaze B2, TTL 30 дней)
VOICE_STORAGE_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
VOICE_STORAGE_BUCKET=techenglish-peer-voice
VOICE_STORAGE_ACCESS_KEY=<R2_ACCESS_KEY>
VOICE_STORAGE_SECRET_KEY=<R2_SECRET_KEY>
VOICE_STORAGE_REGION=auto
VOICE_STORAGE_SKIP_BUCKET_INIT=true
PEER_REVIEW_VOICE_TTL_DAYS=30

# Bootstrap первого администратора (можно удалить после 1-го старта)
BOOTSTRAP_ADMIN_EMAIL=<ТВОЙ_EMAIL>
BOOTSTRAP_ADMIN_PASSWORD=<НАДЁЖНЫЙ_ПАРОЛЬ>
BOOTSTRAP_ADMIN_USERNAME=owner_admin

# Подключается после настройки Firebase / Google Play (Фаза 2 REL-10..12)
FCM_PROJECT_ID=
FCM_SERVICE_ACCOUNT_JSON=
PLAY_INTEGRITY_PACKAGE_NAME=com.techenglish.app
PLAY_INTEGRITY_CERT_SHA256_DIGESTS=
PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON=
```

#### Шаг 3. Первичная инициализация БД, админа и smoke-проверка (с локального ПК или Render Shell)
```bash
# 1. Прогон миграций Alembic 0001..0006 на Neon (в директории backend/)
DATABASE_URL="postgresql+psycopg://<USER>:<PASS>@<NEON_DIRECT_HOST>/<DB>?sslmode=require" \
  .venv/bin/alembic upgrade head

# 2. Проверка таблиц и повышение пользователя до ADMIN (при необходимости вручную)
DATABASE_URL="postgresql+psycopg://<USER>:<PASS>@<NEON_DIRECT_HOST>/<DB>?sslmode=require" \
  .venv/bin/python -m app.cli_admin promote --email <ТВОЙ_EMAIL> --role ADMIN

# 3. Smoke-проверка API (замени HOST на адрес Render)
export API="https://techenglish-api.onrender.com"
curl -fsS "$API/health"
curl -fsS "$API/v1/ota/manifest"
curl -fsS "$API/v1/peer-review/daily-case"

# 4. Проверка регистрации (письмо уйдёт через Brevo SMTP) и AI Gemini (is_fallback=false)
TOKEN=$(curl -fsS -X POST "$API/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"<ТВОЙ_EMAIL>","password":"<НАДЁЖНЫЙ_ПАРОЛЬ>"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

curl -fsS -X POST "$API/v1/ai/code-review" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"draft_text":"Please extract this parsing logic into a helper.","context":"pull_request","consent_accepted":true}'

# 5. Проверка выдачи WS-тикета и WebSocket handshake (через python websockets или wscat)
WS_TICKET=$(curl -fsS -X POST "$API/v1/ws/ticket?scope=rooms" -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json; print(json.load(sys.stdin)["ticket"])')
echo "Connect to: wss://techenglish-api.onrender.com/ws/rooms/backend-english?ticket=$WS_TICKET"
```
*(Опционально для устранения cold start на Render: настроить бесплатный пинг раз в 14 минут на `https://<host>/health` в UptimeRobot или cron-job.org).*

---

### Фаза 4. Сквозное E2E-тестирование на реальном смартфоне (чек-лист 10 проверок)

1. **Первый холодный старт и распаковка БД:** установить universal release APK на реальный Android-смартфон → убедиться в показе прогресса `DatabaseBootstrapManager`, прохождении `AgeGate16PlusScreen`, скорости 2-го холодного запуска (`< 1 с`).
2. **Полный офлайн-режим (Авиарежим):** открыть словарь (`20 658` терминов), проверить поиск на английском, русском (в обоих регистрах) и таджикском, пройти сессию SRS, открыть Debug the Log, Pronunciation Trainer (`TTS`) и виджет Glance на рабочем столе (переключить тему IDE — убедиться, что виджет перекрасился).
3. **Аутентификация (Email + OAuth):** зарегистрироваться по Email → проверить доставку кода в почту через Brevo → подтвердить email; выйти → войти через **GitHub PKCE** и **Google PKCE** в Custom Tabs → убедиться в возврате по `techenglish://auth/callback` и сохранении сессии после перезапуска приложения.
4. **Двусторонний Sync и пробуждение сервера:** изучить 3 карточки в авиарежиме → включить сеть, подождать 15 минут простоя Render (без пингера) → нажать «Синхронизировать» → убедиться, что клиент выдерживает пробуждение без таймаута, начисляет XP, синхронизирует SM-2 и переотправляет прогресс на 2-е устройство.
5. **AI-ментор (Gemini Proxy):** принять согласие в AI Mock Interview и AI Code Review → проверить, что ответы приходят осмысленные (от Gemini, а не шаблон), email/токены в тексте заменяются (`scrub_pii`), счётчик квоты уменьшается.
6. **Тематические комнаты (WebSockets + Outbox):** на 2 телефонах зайти в `#backend-english` → убедиться в мгновенной доставке сообщений через Redis Pub/Sub; отключить Wi-Fi на одном телефоне, отправить сообщение (`local_msg_*` в `sync_queue`) → включить сеть и проверить автозамену локального сообщения на серверное без потери.
7. **PvP 1v1 Дуэль:**
   - На 1 телефоне: дождаться 15-секундного таймаута → убедиться в предложении тренировочного раунда (`0` рейтинговых очков).
   - На 2 телефонах одновременно: нажать поиск → убедиться в реальном матчмейкинге через Redis, обновлении `opponentScore` в реальном времени, корректной обработке разрыва сети одного из игроков (`DISCONNECTED_FORFEIT`).
8. **Peer Review Arena (голос + S3 R2/B2 + удаление аккаунта):** разрешить `RECORD_AUDIO` → записать голосовой ответ 5–10 с (M4A) → отправить → на 2-м телефоне нажать воспроизведение (скачивание по presigned HTTPS URL из R2/B2) → на 1-м телефоне нажать «Удалить облачный аккаунт и данные» (`DELETE /v1/account`) и убедиться в консоли R2/B2, что `.m4a`-объект физически удалён, а локальный словарь продолжает работать.
9. **Доставка OTA-пакета с новыми данными:** загрузить тестовый OTA-пак `*.json.gz` на R2/GitHub Releases, прописать его в `manifest.json` → нажать «Проверить обновления контента (OTA)» → убедиться, что `ota_content_meta` показывает `OK`, новые термины/переводы ищутся через FTS4, а SRS-прогресс старых слов не сбросился; дополнительно проверить сценарий с битым SHA-256 (показывается `FAILED` в UI и повторяется при следующем синке).
10. **Push-уведомление Ping (после `REL-10`/`REL-11`):** на 2-м телефоне разрешить `POST_NOTIFICATIONS` → на 1-м телефоне добавить друга и нажать «Пнуть» → получить push-уведомление на заблокированном экране, тапнуть по нему и проверить переход на экран карточек (`EXTRA_DESTINATION`); проверить блокировку повторного пинга в течение 24 ч и opt-out (`accepts_pings=false`).

---

### Фаза 5. Выпуск сборки и дальнейшее масштабирование

1. **Подписание релизной сборки:**
   - Сгенерировать `release.keystore` **вне репозитория** (`keytool -genkey -v ...`), сохранить пароль и бэкап в менеджере паролей.
   - Собрать подписанный AAB: задать env `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `API_BASE_URL` → `./gradlew :app:bundleRelease`.
   - Для раздачи тестерам вне Play Store сгенерировать универсальный APK из подписанного AAB через `bundletool build-apks --bundle=.../app-release.aab --output=techenglish.apks --mode=universal ...` (включает `content_pack` и БД!) и выложить в GitHub Releases.
2. **Публикация в Google Play Console (Internal Testing Track):**
   - Включить Play App Signing, загрузить подписанный `app-release.aab` (убедиться, что Play Console видит оба модуля: `base` и `install-time` asset pack `content_pack`).
   - Заполнить форму Data Safety (по чек-листу из `docs/OWNER_ACTIONS.md` разд. 2.1), указать URL `/privacy`, `/terms` и `/v1/account/delete-form`.
   - Получить SHA-256 отпечаток App Signing key из Play Console → вписать в `PLAY_INTEGRITY_CERT_SHA256_DIGESTS` и переключить сервер в `ENV=prod`, `PLAY_INTEGRITY_STRICT=true`.
3. **План безболезненного переезда с $0/мес на платный VPS ($5–12/мес) БЕЗ изменения кода приложения:**
   - **Ключевое правило на старте:** привязать собственный поддомен **`api.techenglish.app`** через CNAME на Render/Koyeb (выпуск бесплатного TLS-сертификата на Render автоматический) и зашить в `BuildConfig.API_BASE_URL = "https://api.techenglish.app"`. Тогда пользователям не придётся обновлять приложение при миграции!
   - **Последовательность переезда (downtime ≤ 5 минут):**
     1. Арендовать VPS (минимум 2 vCPU / 4 GB RAM, Ubuntu 24.04 + Docker), снизить TTL DNS-записи `api.techenglish.app` до `60` с.
     2. Поднять `backend/docker-compose.yml` + reverse-proxy Caddy (автоматический Let's Encrypt HTTPS + проксирование WebSockets) с `--workers 2..4`.
     3. Перенести данные Postgres: `pg_dump --format=custom` из Neon → `pg_restore` в локальный контейнер `postgres:16-alpine` (или догнать `alembic upgrade head`).
     4. Переключить `REDIS_URL=redis://redis:6379/0` (убирает лимит 500K команд и сетевые задержки Upstash).
     5. **Cloudflare R2 и OTA-бакет оставить на месте** (бесплатный egress $0/мес экономит трафик VPS; при желании позже переключить `VOICE_STORAGE_ENDPOINT` на локальный MinIO за `https://s3.techenglish.app`).
     6. Переключить DNS A-запись `api.techenglish.app` на IP VPS и остановить Render / Neon.

---

## Раздел 5. Карта рисков и чек-лист владельца

### 5.1. Топ-10 рисков при добавлении данных и запуске на бесплатном хосте

| № | Риск | Влияние / Вероятность | Причина в текущем коде и инфраструктуре | Способ обхода / митигации |
|---|---|---|---|---|
| **1** | **Срывы запросов и PvP при cold start 30–60 с** | Высокое / Высокая | `TechEnglishApiClient.kt:1210` (`REQUEST_TIMEOUT=20s`), 15-сек таймер `PvpDuelViewModel` | Поднять таймауты до `65s` (**REL-07**), вызывать `/health` warmup, поставить внешний пинг раз в 14 мин (750 ч/мес Render хватает на 24/7 аптайм) |
| **2** | **Краш `FileNotFoundException` на standalone APK** | Критическое / Высокая | `app/build.gradle.kts:13-22`: `app-release-unsigned.apk` (`5.59 MiB`) не включает `content_pack` | Выдавать тестерам только universal APK из `.aab` через `bundletool` или flavor **REL-09** |
| **3** | **Не работает аудио в Peer Review + падение R2 на `create_bucket`** | Высокое / Высокая | `voice_url` vs `download_url` (**GAP-01**); `voice_storage.py:90-108` вызывает `create_bucket`/lifecycle без catch | Починить DTO (**REL-01**), добавить `VOICE_STORAGE_SKIP_BUCKET_INIT=true` (**REL-05**) |
| **4** | **Поломка FTS4 и потеря `translation_tg` при пересборке БД** | Высокое / Средняя | `import_verified_tg.py` пишет в `.db` мимо `corpus_builder.py`; `updateContentPreservingSrs()` не обновляет `normalized_search` | Выполнить **DATA-01** и **DATA-03** до начала импорта переводов |
| **5** | **Потеря офлайн-сообщений чата при глобальной синхронизации** | Среднее / Высокая | `CloudSyncEngine.kt:93` и `SyncEventType.fromRaw` съедают `ROOM_MESSAGE_OUTBOX` | Фильтровать outbox в `CloudSyncEngine` (**REL-02**) |
| **6** | **Блокировка `sync` (403) и протухание FCM/Play Integrity токенов через 60 мин** | Критическое / Высокая | На клиенте нет Play Integrity SDK; на сервере статичные Bearer-токены в `integrity.py` и `push_gateway.py` | На бету ставить `ENV=staging` (**REL-06**); до Play Store сделать **REL-10** (`google-auth`) и **REL-12** |
| **7** | **Обрывы idle соединений Neon PG и Upstash `rediss://`, голодание потоков WS** | Высокое / Средняя | В `models.py` нет `pool_pre_ping=True`; в `redis_client.py` нет `health_check_interval`; `asyncio.to_thread` делит пул из 5 потоков | Настроить пул SQLAlchemy (**REL-04**) и `health_check_interval=30` (**REL-05**); ограничить WS concurrency |
| **8** | **Поломка OAuth redirect (`http://`) и потеря OAuth state / rate-limits при рестарте** | Среднее / Высокая | `Dockerfile` без `--proxy-headers`; `OAuthFlowStore` и `_AUTH_RATE_BUCKETS` лежат в RAM процесса | Добавить `--proxy-headers` (**REL-04**) и перенести store/buckets в Redis (**REL-05**) |
| **9** | **Отклонение в Google Play (Data Safety / Delete Account)** | Критическое / Высокая | `delete-form` шлёт на 404 `/v1/account/delete-request` (**GAP-02**); нет публичных `/privacy`, `/terms` | Реализовать `POST /v1/account/delete-request` и статические страницы (**REL-03**) |
| **10** | **Исчерпание квот Free Tier и раздувание `tech_english.db` > 200 MiB** | Среднее / Средняя | Порог Neon 500 MB / 191.9 ч, Upstash 500K cmd, Gemini RPD; дублирование определений в `normalized_search` (21.65 MiB) | Выносить дельты в OTA `.json.gz` на R2, снизить AI лимит до `10`/день, чистить старые логи в PG |

---

### 5.2. Сводный порядок действий владельца (3 волны запуска)

#### Волна 1 — Минимальный живой запуск за 1 вечер ($0/мес, свой тест на смартфоне)
- [ ] Дождаться от агента исправлений Фазы 2 (`REL-01` .. `REL-09`) и данных (`DATA-01` .. `DATA-03`).
- [ ] Зарегистрировать **Neon** (PG 16), **Upstash / Redis Cloud**, **Cloudflare R2** (или Backblaze B2 без карты, настроить Lifecycle 30d), **Brevo** (SMTP), **Google AI Studio** (`GEMINI_API_KEY`) и **Render Free**.
- [ ] Прописать `.env` в Render (`ENV=staging`, `PLAY_INTEGRITY_STRICT=false`), прогнать `alembic upgrade head`, создать админа и пройти `curl`-проверки (Фаза 3).
- [ ] Установить universal APK на свой смартфон и прогнать чек-лист (Фаза 4, пп. 1–9).

#### Волна 2 — Полноценная публичная бета (20–100 тестеров)
- [ ] Зарегистрировать свой домен/поддомен `api.techenglish.app` (CNAME → Render) и пересобрать клиент с `BuildConfig.API_BASE_URL`.
- [ ] Настроить **GitHub OAuth App** и **Google Cloud OAuth Web Client** (+ email тестеров).
- [ ] Подключить **Firebase Spark** (`app/google-services.json` + Service Account JSON после `REL-10`/`REL-11`) для реальных push-пингов.
- [ ] Выложить первые верифицированные OTA-паки (`docs/tg_review/terms_tg_draft.csv` после выверки носителем) на Cloudflare R2 / GitHub Releases.

#### Волна 3 — Релиз в Google Play и масштабирование на VPS
- [ ] Сгенерировать `release.keystore` вне репозитория, включить Play App Signing, собрать подписанный `.aab` и загрузить в **Internal Testing Track**.
- [ ] Подключить **Google Play Integrity API** (`REL-12`), прописать `PLAY_INTEGRITY_CERT_SHA256_DIGESTS` из Play Console, утвердить `/privacy`, `/terms` и рабочую форму удаления аккаунта, перевести сервер в **`ENV=prod`, `PLAY_INTEGRITY_STRICT=true`**.
- [ ] При росте аудитории > 300–500 DAU или нехватке лимитов Free Tier — поднять `backend/docker-compose.yml` на VPS ($5–12/мес), перенести Postgres из Neon через `pg_dump`/`pg_restore` и переключить DNS `api.techenglish.app` без перевыпуска приложения.
