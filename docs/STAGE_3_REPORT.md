# Отчёт по Этапу 3 — TechEnglish v3.0 (Backend, Sync, Anti-Cheat, Social & OTA)

**Дата:** 2026-11-19  
**Статус:** Выполнено, проверено тестами и сборками (`Debug APK`, `Release AAB`, `pytest`, smoke-тест на эмуляторе)  
**Тег:** `v3-stage3`

---

## 1. Реализованный функционал по спецификации (`techenglish_v3_2_final.md` + Errata E1–E6)

### 1.1. Backend FastAPI + PostgreSQL 16 + Redis 7 (`backend/`, `DEC-09`)
- Создан самодостаточный контейнерный стек: `backend/Dockerfile`, `backend/docker-compose.yml` (`postgres:16-alpine`, `redis:7-alpine`, `api` с healthcheck), `backend/requirements.txt`.
- Реализована модель данных SQLAlchemy 2.0 (`backend/app/models.py`): `users`, `sync_events`, `sm2_states`, `user_favorites`, `user_custom_words`, `friendships`, `friend_pings`, `activity_feed`, `user_blocks`, `moderation_reports`.
- Реализована серверная безопасность (`backend/app/security.py`): PBKDF2-SHA256 хеширование паролей, JWT Access/Refresh токены, RFC 7636 PKCE S256 верификатор и интерфейс верификации Google Play Integrity.
- Реализовано полное REST API (`backend/app/main.py`):
  - `GET /health`
  - Аутентификация (`ФТ-9.1`): `POST /v1/auth/register`, `POST /v1/auth/verify-email`, `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/password-reset/request`, `POST /v1/auth/password-reset/confirm`, `POST /v1/auth/oauth/{provider}/exchange`, `POST /v1/auth/guest/link`.
  - Приватность и удаление аккаунта (`ФТ-9.4`): `DELETE /v1/account`, `GET /v1/account/delete-form`, `GET /v1/legal/privacy-policy`.
  - Двусторонняя синхронизация и античит (`ФТ-9.2`, `ФТ-9.3`, Errata E1): `POST /v1/sync/events`, `GET /v1/sync/state`.
  - Публичный профиль и модерация (`ФТ-10.x`, Errata E2): `GET/PATCH /v1/profile/me`, `POST /v1/moderation/report`, `POST /v1/moderation/block/{target_id}`, `GET /v1/moderation/blocks`.
  - Сегментированные лидерборды Top-10 и лиги (`ФТ-11.1`–`11.2`): `GET /v1/leaderboards/{tab}` (`weekly`, `league`, `friends`, `tech_stack`).
  - Друзья, лента и пинги (`ФТ-12.1`–`12.3`): `GET /v1/friends`, `GET /v1/friends/search`, `POST /v1/friends/add`, `POST /v1/friends/{id}/ping`, `GET /v1/friends/feed`, `POST /v1/friends/feed/{id}/react`.
  - OTA-доставка данных (`ФТ-14.5`, Errata E3/E4): `GET /v1/ota/manifest`, `GET /v1/ota/packs/{pack_id}` (строго `data_only = true`, проверка SHA-256).

### 1.2. Чистый Kotlin JVM `:domain` (`НФТ-7.4`, `DEC-10`, `DEC-11`)
- `domain/src/main/kotlin/com/techenglish/domain/model/Stage3Models.kt` — модели авторизации, outbox-очереди, SM-2 снимков, античита, профиля разработчика, бейджей, лиг, лидербордов, друзей, ленты и OTA-метаданных.
- `domain/src/main/kotlin/com/techenglish/domain/usecase/Stage3SyncAndAntiCheatUseCases.kt`:
  - `ResolveSm2SyncConflictUseCase` — разрешение конфликтов строго по **Errata E1** (клампинг `client_timestamp > server_received_at + 5 мин`, победа более нового валидного `client_timestamp`, тай-брейк по `repetitions`, затем по более раннему `server_received_at`).
  - `ValidateRankedEventAntiCheatUseCase` — идемпотентность по UUID `event_id`, пересчёт канонического XP на сервере с игнорированием клиентской подмены, проверка временных границ и минимальной длительности сессии, лимиты XP в минуту (120) и в сутки (2 500).
  - `MergeGuestIntoAccountUseCase` — бесшовное объединение гостевого прогресса (SM-2 по Errata E1, дедупликация событий по `event_id`, объединение избранного и пользовательских слов).
  - `CalculateWeeklyLeagueUseCase` — расчёт начала недели (понедельник 00:00 `Asia/Dushanbe`, UTC+5) и переходов между лигами (Топ-3 ↑, последние 3 ↓).
  - `ValidateSocialActionsUseCase` — валидация `@username` (3–20 символов, фильтр зарезервированных и запрещённых подстрок), allowlist аватаров **Errata E2** (только пресеты или верифицированный OAuth URL без загрузки произвольных изображений), ограничение «Пнуть» 1 раз в 24 часа + opt-out (`ФТ-12.3`).

### 1.3. Android Room v4 (`MIGRATION_3_4`), Security, Ktor Client & Outbox Sync
- **Room v4 (`TechEnglishDatabase.kt`, `Stage3Entities.kt`, `Stage3Daos.kt`):**
  - Добавлены 8 таблиц §6.2: `sync_queue`, `user_profile_cache`, `local_account_link`, `leaderboard_cache`, `friends_cache`, `activity_feed_cache`, `badges_cache`, `ota_content_meta` и 3 индекса (`idx_sync_queue_status_created`, `idx_leaderboard_tab_rank`, `idx_activity_feed_created`).
  - Экспортирована схема `app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/4.json`.
  - Пересобран оффлайн-сид `app/src/main/assets/databases/tech_english.db` на версии Room 4 (`identityHash: 255ecb23e292f0db500c173447b671ae`, 27/27 обязательных индексов).
  - Выполнена оптимизация `AcademicTermDao.searchPaginated` и `countMatching` через параметризованный `@RawQuery` (`SimpleSQLiteQuery`), устранив полное сканирование `SCAN academic_terms` и вернув индексный поиск 30k записей с 273 мс до **54 мс**.
- **Хранение токенов (`DEC-12`, `НФТ-7.3`):** `AndroidKeystoreTokenStore` на базе `AndroidKeyStore` AES-256-GCM + Jetpack DataStore (без устаревшего `EncryptedSharedPreferences`).
- **Сетевой стек (§6.1, `DEC-09`):** единый клиент **Ktor Client 2.3.12 (`ktor-client-okhttp`)** + Kotlinx Serialization (`TechEnglishApiClient.kt`), без Retrofit и Supabase. Добавлен `res/xml/network_security_config.xml` (`cleartextTrafficPermitted="false"` для HTTPS TLS 1.2+).
- **Outbox Sync + OTA (`CloudSyncEngine.kt` + `StudyLogRepositoryImpl`):**
  - Каждая запись в `StudyLogRepositoryImpl.insertLog` автоматически добавляет событие с UUID `event_id` в `sync_queue` (`PENDING`).
  - `@HiltWorker CloudSyncWorker` выполняет фоновую периодическую (каждые 6 часов) и ручную синхронизацию `sync_queue`, обновляет кэш лидербордов/друзей/ленты и применяет data-only OTA-пакеты через `ContentPackImporter` с проверкой SHA-256.

### 1.4. Compose UI экраны Этапа 3 (`feature/stage3/`)
- `AgeGate16PlusScreen` (`ФТ-9.4`) — обязательное подтверждение 16+ и Политика конфиденциальности при первом запуске.
- `CloudAccountSyncScreen` (`ФТ-9.1`, `ФТ-9.2`, `ФТ-9.4`, `ФТ-14.5`) — статус гостя/аккаунта, регистрация/вход по Email, OAuth 2.0 PKCE S256 (GitHub/Google), привязка гостевого прогресса, счётчик `sync_queue`, ручная синхронизация, список и проверка OTA-пакетов, настройка API URL, удаление облачного аккаунта и данных.
- `DeveloperProfileCardScreen` (`ФТ-10.1`–`10.3`, Errata E2) — редактирование `@username`, галерея безопасных пресетов аватаров, стек, GitHub/Telegram, витрина бейджей (динамически открывающихся по локальной статистике даже офлайн).
- `LeaderboardsScreen` (`ФТ-11.1`–`11.2`, `НФТ-7.1`) — 4 вкладки (*Неделя*, *Лига*, *Друзья*, *Стек*), напоминание о сбросе в понедельник 00:00 UTC+5, закреплённая позиция пользователя и баннер **«Офлайн-режим»** с отображением локального кэша `leaderboard_cache`.
- `FriendsAndFeedScreen` (`ФТ-12.1`–`12.3`, Errata E2) — добавление друзей по `@username`, детерминированный QR-код + share deep-link (`techenglish://invite/@username`), кнопка «Пнуть» (лимит 1/24ч) + переключатель opt-out, лента активности друзей с реакциями, диалог жалобы и блокировки пользователя.

---

## 2. Фактически измеренные показатели

| Метрика | Измеренное значение | Требование / Порог | Статус |
|---|---|---|---|
| Тесты FastAPI Backend (`pytest`) | **3 / 3 PASSED** (7,44 с) | Все пройдены | PASS |
| Тесты `:domain` (JVM JUnit) | **49 / 49 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Покрытие `:domain` (JaCoCo Line) | **93,85% CSV** (2380/2536), **93,89% XML** (2380/2535) | ≥ 90% | PASS |
| Тесты `:app` (Robolectric + Compose UI + Room) | **27 / 27 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Суммарно автоматических тестов | **79 / 79 PASSED** | 0 падений | PASS |
| Поиск по 30 000 терминов (`exactLookupMs`) | **15 мс** | < 100 мс | PASS |
| Фильтрация с пагинацией на 30 000 (`paginatedFilterMs`) | **54 мс** | < 100 мс | PASS |
| Миграции Room (`v1→v4`, `v2→v4`, `v3→v4`) | **100% без потерь** (проверено в `TechEnglishDatabaseTest`) | Без потери данных | PASS |
| Размер `app/src/main/assets/databases/tech_english.db` (v4) | **86,32 МБ** (90 513 408 байт) | Валидная Room v4 БД | PASS |
| Размер `app-debug.apk` | **36,84 МБ** (38 624 742 байт) | — | PASS |
| Размер `app-release.aab` | **26,56 МБ** (27 854 035 байт) | ≤ 100 МБ | PASS |
| Smoke-тест установки и запуска (`emulator-5554`, API 37) | Установлен поверх v3 (`Success`), `Status: ok`, **0 fatal ошибок** в logcat | Без крашей при обновлении | PASS |
| Время холодного старта на реальном устройстве (Release) | не измерено (только эмулятор, `WaitTime: 16941 мс` debug) | Проверить в Этапе 4C | — |
| Нагрузочный тест PostgreSQL/Redis под нагрузкой | не измерено (интеграционные тесты `pytest` на SQLite/FastAPI TestClient) | Развёртывание Docker Compose | — |

---

## 3. Аудит разрешений и гигиены (`НФТ-7.3`, `НФТ-7.7`)

- **Разрешения в merged Release `AndroidManifest.xml`:**
  - `INTERNET`, `ACCESS_NETWORK_STATE` (облачная синхронизация и OTA)
  - `POST_NOTIFICATIONS` (локальные SRS-напоминания Android 13+)
  - `RECORD_AUDIO` (тренажёр произношения `ФТ-7.3`)
  - `CAMERA` с `<uses-feature android:name="android.hardware.camera" android:required="false" />` (QR-приглашения `ФТ-12.1`)
  - Служебные разрешения AndroidX WorkManager (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`).
  - Нет запрещённых разрешений (`READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `READ_EXTERNAL_STORAGE` и др.).
- **Безопасность:** `android:networkSecurityConfig="@xml/network_security_config"` включён, `cleartextTrafficPermitted="false"`, токены в `AndroidKeystoreTokenStore` (AES-256-GCM).
- **Стек:** 0 упоминаний Retrofit или Supabase; `EncryptedSharedPreferences` не используется.
- **Гигиена Git:** 0 абсолютных путей хоста в исходниках, `local.properties`, `.env`, `backend/.venv/` и `backend/*.db` исключены через `.gitignore`.
