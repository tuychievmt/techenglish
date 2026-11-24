# TechEnglish v3.0.0 → v3.1.0 Remediation Log (`REMEDIATION_LOG.md`)

Журнал фактических замеров, проверок воспроизводимости дефектов, расхождений с ожиданиями аудита и ограничений окружения (протокол §0.1).

---

## Спринт 0 — Базовая линия (`S0-01`, ревизия `331bad5` / `v3.0.0`)

**Ветка:** `remediation/v3.1` (создана от `main` / `331bad5`, тег `v3.0.0`).  
**Среда выполнения:** macOS, Java 21 (JBR 21.0.11) / Gradle JVM Target 17 (Temurin 17), Python `3.12.16` (`backend/.venv`), pytest `8.3.4`.

### 1. Результаты тестов (сверка с ожиданием аудита: 104 теста = 57 + 36 + 11)

| Набор тестов | Команда | Ожидание аудита | Факт | Статус |
|---|---|---:|---:|---|
| `:domain` (JVM unit) | `./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport` | 57 | **57** (0 failed, 0 skipped, 12 классов) | Совпадает |
| `:app` (Robolectric + Room + Compose UI) | `./gradlew :app:testDebugUnitTest` | 36 | **36** (0 failed, 0 skipped, 3 класса) | Совпадает |
| `backend/` (FastAPI + SQLAlchemy + WS) | `cd backend && .venv/bin/pytest -v` | 11 | **11** (0 failed, 0 skipped, `10.25s`) | Совпадает |
| **ИТОГО** | | **104** | **104 / 104 PASSED** | **Совпадает** |

**Дополнительное наблюдение (бенчмарк 30K в `TechEnglishDatabaseTest`):**
- `STAGE2_BENCHMARK_30K: exactLookupMs=4, paginatedFilterMs=18` (порог < 100 мс пройден; в `FINAL_REPORT.md` было `2 мс / 12 мс` на другом прогоне).

### 2. Покрытие `:domain` по JaCoCo (сверка с ожиданием аудита: 94,58%)

Сформировано единым запуском `./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport` (JaCoCo `0.8.12`):

| Формат отчёта | Line Coverage | Instruction Coverage | Branch Coverage |
|---|---|---|---|
| **XML** (`domain/build/reports/jacoco/test/jacocoTestReport.xml`, root / sourcefile) | **2844 / 3007 = 94.58%** (missed: 163) | **18377 / 20853 = 88.13%** (missed: 2476) | **739 / 1001 = 73.83%** (missed: 262) |
| **HTML** (`domain/build/reports/jacoco/test/html/index.html`, `<tfoot>`) | **2844 / 3007 = 94.58%** (missed: 163) | **18377 / 20853 = 88.13%** (missed: 2476) | **739 / 1001 = 73.83%** (missed: 262) |
| **CSV** (`domain/build/reports/jacoco/test/jacocoTestReport.csv`, сумма строк классов) | **2844 / 3008 = 94.55%** (missed: 164) | **18377 / 20853 = 88.13%** (missed: 2476) | **739 / 1001 = 73.83%** (missed: 262) |

**Расхождение CSV vs XML/HTML:** XML root и HTML совпадают бит-в-бит (`2844/3007 = 94.58%`, ровно как в аудите). В CSV сумма по строкам классов совпадает с суммой `<class>` в XML (`2844/3008 = 94.55%`) — разница ровно 1 строка возникает из-за стандартного поведения JaCoCo при агрегации по классам (одна общая строка исходника учитывается в двух вложенных/синтетических классах) против дедупликации по `<sourcefile>` в XML/HTML. Метрики `INSTRUCTION` и `BRANCH` совпадают 100% во всех трёх отчётах.

### 3. Перемер размеров базовой линии (правки R4, R5)

Проверены артефакты базовой линии v3.0.0 (`331bad5`):

| Артефакт / метрика | Фактический размер |
|---|---|
| `app/src/main/assets/databases/tech_english.db` (несжатая БД на диске) | **90 562 560 Б (86.37 MiB / 90.56 MB)** |
| `app-release.aab` (общий размер файла архива) | **28 177 402 Б (26.87 MiB / 28.18 MB)** |
| `app-release.aab` → сумма всех записей без сжатия | **154 512 309 Б (147.35 MiB / 154.51 MB)** |
| `app-release.aab` → модуль `base/` (сжатый / несжатый) | **26.61 MiB (27 898 085 Б)** / **147.33 MiB (154 482 934 Б)** |
| `app-release.aab` → `base/assets/databases/tech_english.db` (DEFLATE / несжатый) | **8.67 MiB (9 091 980 Б)** / **86.37 MiB (90 562 560 Б)** |
| `app-release.aab` → `base/dex` (сжатый / несжатый, R8 выключен) | **15.79 MiB** / **55.32 MiB** |
| `app-release.aab` → `base/res` (сжатый / несжатый) | **1.90 MiB** / **4.94 MiB** |
| `app-release-unsigned.apk` (общий / DB сжатая / сумма без сжатия) | **27.19 MiB (28 506 586 Б)** / **8.67 MiB** / **146.93 MiB** |
| `app-debug.apk` (общий / DB сжатая / сумма без сжатия) | **37.34 MiB (39 150 954 Б)** / **10.90 MiB** / **165.98 MiB** |

**Разъяснение R5:** Цифры аудита «AAB 26,87 МБ» и «147 МБ без сжатия» подтверждены как две разные метрики одного и того же файла `app-release.aab`: `26.87 MiB` — фактический размер архива AAB (сжатая БД занимает лишь `8.67 MiB`, основной вес в сжатом виде — `base/dex` `15.79 MiB` из-за выключенного R8), а `147.35 MiB` — сумма несжатых размеров всех записей внутри архива (в основном `tech_english.db` `86.37 MiB` + `base/dex` `55.32 MiB`).

### 4. Замеры, недоступные в текущем окружении (оговорка §0.1)

| Метрика | Статус | Причина |
|---|---|---|
| `bundletool get-size total` (размер загрузки базового модуля) | **не измерено (нет `bundletool` в окружении)** | Утилита `bundletool` не установлена в PATH / локальных каталогах. |
| Время первого запуска (распаковка БД) и повторного холодного старта | **не измерено (нет запущенного AVD в окружении)** | `adb devices` пуст, локальный эмулятор в текущей shell-сессии не запущен. |
| Локальный `docker compose build` / `alembic upgrade head` на живом Docker PostgreSQL | **не измерено локально (Docker daemon не запущен)** | `docker` CLI установлен (`/usr/local/bin/docker`), но Docker daemon остановлен; проверка на реальном PostgreSQL 16 и Redis 7 настроена в `.github/workflows/ci.yml` и локально проверяется через SQLite batch-режим + тест эквивалентности метаданных. |

---

## Спринт 1A — Backend: безопасность, OAuth, AI, MinIO, Redis-тикеты и Play Integrity

Все 7 подзадач (`SP1-01 → SP1-02 → SP1-03a → SP1-04 → SP1-05 → SP1-07a → SP1-11`) выполнены по протоколу «красный тест вперёд»:

| Задача / дефект | Воспроизведение (RED) | Исправление (GREEN) | Коммит |
|---|---|---|---|
| **SP1-01** (`BUG-02`, часть `BUG-07`) | `POST /v1/auth/register` возвращал `email_verify_token_dev` в JSON; `EmailSender` и `validate_production_startup_config` отсутствовали (`3 failed`). | Токены не возвращаются в HTTP, хранятся только как SHA-256 хеш, одноразовые, TTL 15 мин, лимит 5 попыток (`429`), единый ответ `{"sent": True}`, `EmailSender` (`SmtpEmailSender` / `ConsoleEmailSender`), миграция `0002_auth_token_security`, fail-fast в `ENV=prod` (`3 passed`). | `4d1ff21` |
| **SP1-02** (`BUG-03`) | Обычный пользователь получал `200 OK` на `GET /v1/moderation/queue` и `POST /v1/crowdsource/prs/{id}/approve` (`3 failed`). | Колонка `users.role` (`0003_user_role_rbac`), `require_moderator` (`MODERATOR`/`ADMIN`), запрет самоапрува PR (`403`), CLI/env-bootstrap первого админа `app/cli_admin.py` (`3 passed`). | `43b14cb` |
| **SP1-03a** (`BUG-01`, серверная часть `BUG-04`) | `POST /v1/auth/oauth/github/exchange` с подставным `provider_email` жертвы возвращал `200 OK` с `user_id` жертвы (полный account takeover; `2 failed`). | Серверный OAuth flow (`/authorize` → `/callback` → `/ticket/exchange`), таблица `oauth_identities` (`0004_oauth_identities`), привязка по `(provider, sub)`, запрет слияния по неподтверждённому email (`409`), allowlist `redirect_uri`, одноразовые `state` и `ticket`; старый `/exchange` отключён (`410 Gone`; `2 passed`). | `02cc05d` |
| **SP1-04** (`BUG-05`) | При выставленном `GEMINI_API_KEY` эндпоинт возвращал `provider="gemini-server-proxy"` без вызова Gemini (`2 failed`). | Асинхронный `HttpxGeminiClient` с таймаутами, JSON-схемой и Pydantic-валидацией, TTL-кэшем, PII-scrubbing до промпта, честным `provider` и явным `is_fallback`/`fallback_reason` (`2 passed`). | `48a2570` |
| **SP1-05** (`BUG-06`, сервер) | `S3VoiceObjectStorage` отсутствовал, клипы писались только на локальный диск (`1 failed`). | `S3VoiceObjectStorage` (`boto3`/MinIO) с 30-дневным bucket lifecycle, валидацией ≤ 20 с и ≤ 512 КБ, presigned GET (`/v1/peer-review/submissions/{id}/voice-url`), физическим удалением при `DELETE /v1/account`; проверено через `moto` (`1 passed`). | `8e45902` |
| **SP1-07a** (`R11`, `BUG-07`, сервер `BUG-10`) | Модуль `app.redis_client` и эндпоинт `/v1/ws/ticket` отсутствовали (`1 failed`). | Подключён клиент `REDIS_URL` (с `fakeredis` для локальных тестов без контейнеров), эндпоинт `POST /v1/ws/ticket` (TTL 30 с в Redis), атомарное погашение (`GETDEL`) в `/ws/rooms/{slug}` и `/ws/pvp/{match_id}` (временный fallback `?token=` сохраняется до `SP1-07b`; `2 passed`). | `2429993` |
| **SP1-11** (`R3`, `BUG-07`) | Проверка Play Integrity принимала любую строку ≥ 32 символов; `app.integrity` отсутствовал (`3 failed`). | Интерфейс `ClientIntegrityVerifier` + `GooglePlayIntegrityVerifier` (разбор вердикта `requestPackageName`, `timestampMillis`, `nonce`, `PLAY_RECOGNIZED`, `certificateSha256Digest`, `MEETS_DEVICE_INTEGRITY`, `UNLICENSED`), запрет fake-верификатора в `ENV=prod` (`3 passed`). | `3e0ce27` |

---

## Спринт 1B — Android: OAuth-клиент, Sync, защита, сборка и точечные фиксы

Все 6 подзадач (`SP1-03b → SP1-06 → SP1-07b → SP1-08 → SP1-09 → SP1-10`) выполнены по протоколу «красный тест вперёд»:

| Задача / дефект | Воспроизведение (RED) | Исправление (GREEN) | Коммит |
|---|---|---|---|
| **SP1-03b** (`BUG-04`, клиент) | В `AndroidManifest.xml` не было `techenglish://auth/callback`, `OAuthCallbackHandler` отсутствовал, `Stage3ViewModels` терял `codeVerifier` (`compileDebugUnitTestKotlin` failed). | Подключён `androidx.browser:browser:1.8.0` (Custom Tabs), добавлен `intent-filter` `techenglish://auth/callback` и `launchMode="singleTop"`, `OAuthCallbackHandler` хранит `client_state` строго в DataStore, отклоняет чужой/повторный `state` и обменивает `ticket` через `POST /v1/auth/oauth/ticket/exchange` (`2 passed`). | `6171b4a` |
| **SP1-06** (`BUG-08`, `R10`) | `SyncBatchBodyDto` не содержал `sm2_updates`, `CloudSyncEngine` не вызывал `GET /v1/sync/state`, Ktor не имел `Auth` refresh при 401 и `HttpTimeout`. | Двусторонний push (`events`, `sm2_updates`, `favorite_terms`, `custom_words`) и pull `GET /v1/sync/state` с `ResolveSm2SyncConflictUseCase` (R10/E1) в `CloudSyncEngine`; Ktor `Auth` (`bearer`) с авто-refresh через `/v1/auth/refresh` при 401 и `HttpTimeout`; обновлены `docs/REQUIREMENTS.md` и `docs/DECISIONS.md` (`2 passed` в `:app` + `6 passed` в `:domain`). | `5983416` |
| **SP1-07b** (`BUG-10`, `BUG-12`, `BUG-16`) | `AndroidKeystoreTokenStore` имел `PLAIN:`-фолбэк, `allowBackup="true"`, лишнее разрешение `CAMERA`, Ktor WS передавал `?token=<jwt>`, не было debug-исключения для `10.0.2.2`/`localhost`. | Удалён `PLAIN:`-фолбэк (пересоздание ключа + мягкий выход `clearSession()` при сбое Keystore), `allowBackup="false"` + `data_extraction_rules.xml`, удалены `CAMERA` и `hardware.camera`, создан `src/debug/res/xml/network_security_config.xml`, Ktor WS переведён на `POST /v1/ws/ticket` (`?ticket=`), временный `?token=` fallback удалён с сервера (`4 passed` Android + `5 passed` Backend; merged release manifest проверен). | `99c3192` |
| **SP1-08** (`BUG-11`) | В `app/build.gradle.kts` `isMinifyEnabled = false`, дубль `ksp(libs.hilt.compiler)`, отсутствовал `signingConfigs.release`. | Включены `isMinifyEnabled = true` и `isShrinkResources = true`, настроены правила `app/proguard-rules.pro` (Room/Hilt/WorkManager/Ktor/Serialization/Glance), удалён дубль `ksp(libs.hilt.compiler)` (исправлен на `ksp(libs.androidx.hilt.compiler)`), добавлен null-safe `signingConfigs.release` из env (`KEYSTORE_PATH` и др.). `:app:bundleRelease` собрался без ошибок: `app-release.aab` = **19.16 MiB** (было **26.87 MiB**), `base/dex` = **3.06 MiB сжатый / 6.77 MiB несжатый** (было **15.79 / 55.32 MiB**). Смоук-тест на AVD / `bundletool`: **не измерено (нет запущенного AVD / `bundletool` в окружении)**. | `0850ddb` |
| **SP1-09** (`BUG-13`, часть `BUG-16`) | Ключ кэша WAV в `TechAudioController` использовал `text.hashCode()` без локали/скорости, `release()` не был привязан к `ProcessLifecycleOwner`, `MainActivity` не обрабатывал `EXTRA_DESTINATION` в `onNewIntent()`. | Ключ `SHA-256(locale\|speed\|text)`, LRU-вытеснение (`10 MiB` / `40` файлов), `DefaultLifecycleObserver` привязан к `ProcessLifecycleOwner` в `TechEnglishApplication`, `MainActivity.onNewIntent()` обновляет `pendingDestination` для `TechEnglishNavShell` (`3 passed`). | `c8f5466` |
| **SP1-10** (`BUG-14`, часть `BUG-19`) | `ProgrammingScreens.kt:482,796` вызывал `isSystemInDarkTheme()`, `Stage4BViewModels.kt:374` подменял `accountAgeDays` через `streakDays`, `:477` отправлял `targetType="COMMENT"`. | `ProgrammingScreens.kt` использует `LocalTechInteractionConfig.current.isDarkTheme`; `CrowdsourcePrViewModel` загружает `/v1/crowdsource/eligibility` и отправляет `targetType="DICTIONARY_PR"` (добавлен в allowlist на бэкенде; `2 passed`). | `8f971d4` |

## Спринт 2A (`v3.1-sprint2a`) — Архитектура и масштабируемость бэкенда

| Задача / Дефект | Воспроизведение (RED) | Исправление и проверка (GREEN) | Коммит |
|---|---|---|---|
| **SP2-01** (`BUG-17`) | `backend/app/main.py` содержал 2 847 строк и deprecated `@app.on_event("startup")`; `test_sp2_01_decomposition.py` падал (`2847 > 180`, 0 роутеров). | Разбит на `app/deps.py`, `app/schemas/` (4 модуля), `app/services/` (`startup.py`, `sync_service.py`) и 10 роутеров в `app/routers/`; `main.py` сокращён до **125 строк** с `lifespan`, фабрикой `create_app()` и едиными обработчиками ошибок (`detail` + структурированный `error`). | `24b7450` |
| **SP2-02** (`BUG-07`) | В `app/routers/realtime.py` оставались процессные глобальные словари `_ROOM_WS_CONNECTIONS` и `_PVP_WAITING_MATCHES`; `test_sp2_02_redis_realtime.py` падал (`2 failed`). | Создан `app/services/realtime_broker.py` (`RedisRealtimeBroker`: очередь `techenglish:pvp:waiting_queue`, Pub/Sub `techenglish:room:*` и `techenglish:pvp:*`). Удалены все процессные глобалы. Подтверждено 5 последовательными прогонами `test_sp2_02_redis_realtime.py` (2 независимых `create_app()` воркера на общем Redis-сервере) без флаков. | `ba4d183` |
| **SP2-10** (`R3`, FCM / Push) | Отсутствовали таблица токенов, `PushNotificationGateway` и доставка push при пинге друга; `test_sp2_10_push_fcm.py` падал (`ModuleNotFoundError`). | Добавлены `DevicePushTokenRow` + миграция Alembic `0005_device_push_tokens` (+ очистка в `DELETE /v1/account`), `FcmHttpV1PushNotificationGateway` / `FakePushNotificationGateway`, эндпоинты `POST/DELETE /v1/notifications/device-token`, отправка push в `POST /v1/friends/{friend_id}/ping` (с соблюдением `accepts_pings` и лимита 1/24ч), а также Android `PushNotificationGateway` + `NotificationHelper.showPushNotification`. Реальная доставка FCM помечена «E2E не проверено, нужен ключ» (Firebase `google-services.json` + service account). | `99ff5af` |

## Спринт 2B (`v3.1-sprint2b`) — Android архитектура, поиск, производительность и UX

| Задача / Дефект | Воспроизведение (RED) | Исправление и проверка (GREEN) | Коммит |
|---|---|---|---|
| **SP2-03** (`BUG-18`) | 6 UI-файлов превышали 900 строк (`Stage3Screens.kt` 1400, `Stage2Screens.kt` 1287, `Components.kt` 1284, `AcademicScreens.kt` 1199, `ProgrammingScreens.kt` 1049, `ProfileScreen.kt` 901); `UiState` использовали мутабельные `List` без `@Immutable`; `Sp203ComposeStructureTest` падал (`2 failed`). | Все монолиты разбиты на 23 целевых файла (`≤ 446` строк каждый в разбитых модулях, 0 файлов `> 900` строк); ключевые `UiState` переведены на `@Immutable` + `kotlinx.collections.immutable.ImmutableList`; включены отчёты стабильности Compose Compiler (`app/build/compose_compiler/`, измерено `973/1482` skippable, `24628/24719 = 99.63%` stable args). | `5570770` |
| **SP2-04** (`BUG-15`, `R6`, `R12`) | Поиск использовал `LIKE '%...%'` по 8 колонкам с ASCII-only `LOWER()`; `PRAGMA user_version == 5`, таблицы `academic_terms_fts` не было (`Sp204FtsAndCyrillicSearchTest` падал). | Выполнен spike R6 и зафиксирован ADR `DEC-14` в `docs/DECISIONS.md` (`@Fts4` external content + `unicode61` + `normalized_search` для гарантированной совместимости с `minSdk 26`). Реализована миграция Room `MIGRATION_5_6` (полная цепочка `1→2→3→4→5→6` проверена), обновлён `scripts/build_seed_db.py` и пересобран `tech_english.db` до Room v6 (`103.23 MB`). Измеренная средняя задержка поиска на 30 000 терминов: **12.25 мс** (`< 15 мс`, `EXPLAIN QUERY PLAN` подтверждает `academic_terms_fts`). | `bd246e9` |
| **SP2-05** (`BUG-15`, `R4`, `R5`) | База `tech_english.db` лежала в `app/src/main/assets/` (раздувала `base` модуль AAB); копирование при первом старте не имело отдельного off-main-thread прогресса (`Sp205AssetPackAndBootstrapTest`). | Создан `install-time` Play Asset Delivery модуль `:content_pack`, перенесён `tech_english.db` туда (в `app-release.aab` модуль `base` = **5.15 MiB** сжатый / `12.24 MiB`, `content_pack` = **10.74 MiB** сжатый / `103.23 MiB`). Реализован `DatabaseBootstrapManager` (распаковка строго на `Dispatchers.IO` с `LinearProgressIndicator` в `MainActivity`): первый старт **514 мс**, повторный старт **0 мс**. `bundletool get-size total` и замер холодного старта на реальном AVD: не измерено (нет AVD / `bundletool` в окружении). | `b8cbde6` |
| **SP2-06** (`BUG-21`) | В `ProfileScreen` было 12 перегруженных секций/точек входа; в `PracticeHubScreen` дублировались 3 маршрута (`AI-ментор`, `PvP-дуэль`, `Peer Review`); `Sp206NavigationAndHubTest` падал (`2 failed`). | Выделен хаб `CommunityArenaHubScreen` (`Routes.COMMUNITY_ARENA_HUB`), `ProfileScreen` сокращён до **5 секций**, удалены 3 дубля из `PracticeHubScreen`. Визуальный стиль сохранён без изменений. | `7fa45a5` |
| **SP2-07** (`BUG-24`) | `TechEnglishGlanceWidget` захардкодил палитру Tokyo Night (`0xFF1A1B26`) и не имел кнопки обновления «Слова дня». | Добавлена привязка палитры виджета к активной `ThemeMode` из DataStore (`resolveWidgetPalette` + `SettingsRepository`) и кнопка обновления `"↻"` (`RefreshWidgetWordAction : ActionCallback`); проверено `Sp207GlanceWidgetThemeAndRefreshTest`. | `e0de29e` |
| **SP2-08** (`BUG-25`) | `GET /v1/friends/feed` не возвращал `user_reacted` (`test_sp2_08_feed_user_reacted.py` падал), `CloudSyncEngine` затирал `userReacted = false` и молча глотал ошибки повреждённых OTA-пакетов. | Добавлена таблица `activity_feed_reactions` + миграция Alembic `0006_activity_feed_reactions` (идемпотентный `react` + `user_reacted` в ленте), сохранение `userReacted` при синке, поля диагностики `last_status`/`last_error`/`last_attempt_at` в `ota_content_meta` (в Room v6 + UI `CloudAccountSyncScreen`) и повторная загрузка повреждённых OTA-пакетов (`Sp208FeedAndOtaDiagnosticsTest`). | `76c716a` |
| **SP2-09** (`BUG-26`) | Отсутствовал слой crash-мониторинга и выгрузка Compose stability-отчётов в CI. | Добавлен `CrashReporter` (`ConfigurableCrashReporter`, по умолчанию выключен, 0 секретов в репо, очистка PII/токенов) + выгрузка `app/build/compose_compiler/` в `.github/workflows/ci.yml` (`Sp209CrashReporterAndCiTest`). | `cba44fa` |

## Спринт 3 (`v3.1-sprint3`) — Realtime PvP, голос в Peer Review, таджикский язык (R7), офлайн-устойчивость и релиз v3.1.0

| Задача / Дефект | Воспроизведение (RED) | Исправление и проверка (GREEN) | Коммит |
|---|---|---|---|
| **SP3-01** (`BUG-09`) | `POST /v1/pvp/matchmake` и WS `match_ready` отдавали `correct_option_index` клиенту до ответа; `PvpDuelViewModel` не подключался к `/ws/pvp/{match_id}` (`test_sp3_01_pvp_no_answer_leak.py` + `Sp301PvpLiveWsTest` падали). | На сервере `correct_option_index` вырезается через `_public_pvp_questions()` и возвращается только в `answer_ack` после ответа; Android `PvpDuelViewModel` работает по живому `/ws/pvp/{match_id}` (`?ticket=`), получает живой `opponentScore`, корректно обрабатывает разрыв связи и сохраняет 15-секундный unranked training fallback. | `082ba5d` |
| **SP3-02** (`BUG-06`, клиент) | В `PeerReviewArenaViewModel` / `PeerReviewArenaScreen` не было реальной записи M4A, запроса `RECORD_AUDIO` и воспроизведения по presigned URL (`Sp302PeerReviewVoiceTest` падал). | Добавлены `PeerVoiceClipRecorder` (`AndroidPeerVoiceClipRecorder` M4A/AAC ≤ 20 с / ≤ 512 КБ), `TechAudioController.playRemoteAudioUrl()`, `TechEnglishApiClient.fetchPeerReviewVoiceUrl()`, runtime-запрос `RECORD_AUDIO` с обработкой отказа. | `229f510` |
| **SP3-03** (`BUG-20`, `R7`) | Отсутствовали `values-tg/strings.xml`, фича-флаг таджикской локали, пакеты черновиков `docs/tg_review/` и строгий скрипт импорта (`Sp303TajikLocalizationPolicyTest` падал). | Созданы `values-tg/strings.xml`, `TajikLocalizationPolicy` (флаг `TAJIK_UI_LOCALIZATION_ENABLED=false` по умолчанию + RU fallback), `docs/tg_review/` (`README.md`, `ui_strings_tg_draft.json`, `terms_tg_draft.csv` — 150 черновиков строго `pending_review`) и `scripts/import_verified_tg.py`. В релизной БД сохранены строго **19** проверенных человеком `translation_tg` (дефицит `1 981` до `2 000` честно зафиксирован по R7). | `c27b947` |
| **SP3-04** (`BUG-19`) | Сообщения в комнатах при офлайне терялись (не ставились в `sync_queue`), `WsConnectionState` был оптимистичен, Squads не имели Room-кэша (`Sp304OfflineRoomsAndSquadCacheTest` падал). | Офлайн-сообщения `local_msg_*` ставятся в `sync_queue` (`ROOM_MESSAGE_OUTBOX`) и переотправляются при восстановлении сети с заменой локальной записи на серверную; `WsConnectionState` выставляется только после живого handshake; добавлены `SquadCacheEntity` + `SquadCacheDao` в Room v6 (`MIGRATION_5_6`, `6.json`, пересобран `tech_english.db` = `103.24 MiB`, `identityHash = 2fed6cc837b9cd4374ad9e7e58e087bc`). | `d21aeeb` |
| **SP3-07** (`BUG-22`, `R8`) | Версия оставалась `3.0.0` (`300`); после SP3-01/02/04 `Stage4CViewModels.kt` вырос до 906 строк (`Sp203ComposeStructureTest` > 900); финальные доки и релизные артефакты требовали синхронизации. | Поднята версия до `3.1.0` (`versionCode = 310`), `Stage4CViewModels.kt` разбит на 3 файла (`ThematicRoomsViewModel` 294, `PvpDuelViewModel` 357, `PeerReviewArenaViewModel` 290), обновлены `OWNER_ACTIONS.md` (с Data Safety и Internal Track чек-листом R8), `ARCHITECTURE.md`, `REQUIREMENTS_TRACEABILITY.md`, `FINAL_REPORT.md`, `SPRINT_3_REPORT.md`. Собраны релизные артефакты (`app-release.aab = 21.30 MiB`, `base = 5.16 MiB`, `app-release-unsigned.apk = 5.59 MiB`, `app-debug.apk = 40.66 MiB`) и чистый архив `TechEnglish_Source_Clean.zip`. Прогон тестов: **160 passed, 0 failed, 1 skipped** (`:domain` `57`, JaCoCo `94.58%`, `:app` `66`, `backend` `37`, 30k search `8.13 мс`). | `SP3-07` |
