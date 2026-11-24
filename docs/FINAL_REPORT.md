# Final Engineering Report — TechEnglish v3.0.0 (Full Production Modernization)

**Project:** TechEnglish (`com.techenglish.app`)  
**Version:** `3.0.0` (`versionCode = 300`, `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`)  
**Database Version:** Room `v5` (`identityHash = cd74f367098fec999ef10e8f6c1c737f`, lossless migration chain `1→2→3→4→5`, 27 tables, 31 verified indexes)  
**Architecture:** Clean Architecture (Pure Kotlin JVM `:domain` + Android `:app`) + MVVM + UDF (`StateFlow`) + FastAPI/PostgreSQL/Redis/MinIO Backend (`backend/`)  
**Mode:** Offline-First by default + Optional Cloud Sync, Social Platform, AI Server Proxy & WebSocket Realtime

---

## 1. Сводка выполнения дорожной карты v3.0.0 (Этапы 1 → 4C)

| Этап | Тег Git | Ключевые результаты | Подробный отчёт |
|---|---|---|---|
| **Этап 1** | `v3-stage1` | Локальные шрифты `Inter` + `JetBrains Mono` (кириллица, IPA, `ғ ӣ қ ӯ ҳ ҷ`), 5 IDE-тем + Light/Dark с контрастом WCAG AA ≥ 4.5:1, 365-дневный GitHub Contribution Graph, Progressive Disclosure, интерфейс-хамелеон (GitHub PR / Slack / Terminal), Haptic + Reduce Motion + `fontScale = 2.0f` + TalkBack | `docs/STAGE_1_REPORT.md` |
| **Этап 2** | `v3-stage2` | `MIGRATION_2_3`, `ContentPackImporter` (Errata E3), расширение словаря до **20 658** верифицированных терминов (36 доменов, **8 048** лицензированных IPA), **166** статей/логов, **156** сценариев (12 контекстов), **312** фраз, **380** сниппетов (19×20), RU/TJ переключатель (Errata E6), Read Your Own Docs, Debug the Log, Pronunciation Trainer, Glance Widget, Time Attack, Deck Import/Export (JSON/CSV/Anki `.apkg`) | `docs/STAGE_2_REPORT.md` |
| **Этап 3** | `v3-stage3` | ADR `DEC-09..12`, Backend FastAPI + PostgreSQL 16 + Redis 7 (`backend/`), `MIGRATION_3_4` (8 таблиц), единый клиент Ktor Client (без Retrofit/Supabase), `AndroidKeystoreTokenStore` (AES-256-GCM + DataStore, без `EncryptedSharedPreferences`), HTTPS `network_security_config.xml`, Outbox `sync_queue` + Errata E1, серверный античит `ФТ-9.3`, 16+ Age Gate + модерация Errata E2, профиль/бейджи, 4-вкладочные лидерборды Top-10 с офлайн-кэшем, друзья/QR/лента/пинги 1/24ч, data-only OTA `ФТ-14.5`, оптимизация `@RawQuery` поиска | `docs/STAGE_3_REPORT.md` |
| **Этап 4A** | `v3-stage4a` | `MIGRATION_4_5` (`comments_cache`, `community_decks`, `community_deck_terms`, `room_messages_cache`), комментарии/мнемоники в формате StackOverflow под терминами и сниппетами (`ФТ-13.1`), Community Decks с публикацией из «Мои слова» и скачиванием в Room в 1 тап (`ФТ-13.2`), модерация UGC с авто-скрытием и блокировкой авторов | `docs/STAGE_4A_REPORT.md` |
| **Этап 4B** | `v3-stage4b` | AI Mock Interviewer и AI Code/PR Reviewer строго через серверный FastAPI-прокси (явное согласие, дневной лимит 15/день, очистка PII, 0 API-ключей в APK, 0 дней хранения аудио), Squads 5–10 участников с нерейтинговым бонус-множителем (`ФТ-14.4`), краудсорсинг «PR в словарь» с 4 условиями допуска, лицензией CC BY-SA 4.0 и бейджем Contributor (`ФТ-14.3`) | `docs/STAGE_4B_REPORT.md` |
| **Этап 4C** | `v3-stage4c`, `v3.0.0` | ADR `DEC-13` (Errata E5: MinIO S3 + 30 дней TTL), тематические комнаты на Ktor WebSockets (`ФТ-13.3`), 1v1 PvP-дуэли (60 с, 8 серверных вопросов, 15 с тренировочный фолбэк с 0 рейтинговых очков, корректный разрыв связи `ФТ-14.1`), Peer Review Arena с физическим удалением голосовых записей при `DELETE /v1/account` (`ФТ-14.2`), версия `3.0.0`, финальные сборки и чистый архив `TechEnglish_Source_Clean.zip` | `docs/STAGE_4C_REPORT.md` |

---

## 2. Финальные фактически измеренные метрики v3.0.0

### 2.1. Тестирование и покрытие кода (`НФТ-7.4`)
- **`:domain` (Pure Kotlin JVM JUnit):** **57 / 57 PASSED** (`0` failed, `0` skipped).
- **Покрытие `:domain` (JaCoCo Line Coverage):**
  - **XML (`jacocoTestReport.xml`):** **2844 / 3007 = 94,58%** (`163` missed).
  - **HTML (`html/index.html`):** **2844 / 3007 = 94,58%** (`163` missed) — точное совпадение с XML.
  - **CSV (`jacocoTestReport.csv`):** **2844 / 3008 = 94,55%** (`164` missed; разница в 1 синтетической строке агрегации классов).
- **`:app` (Robolectric + Room Migrations + Compose UI + Accessibility + Security):** **36 / 36 PASSED** (`0` failed, `0` skipped).
  - Покрыты все миграции `1→2`, `2→3`, `3→4`, `4→5`, `ContentPackImporter` (Errata E3), `AnkiApkgExtractor`, outbox `sync_queue`, `AndroidKeystoreTokenStore`, WCAG AA контраст всех 7 тем, шрифты `Inter`/`JetBrains Mono` (включая `ғ ӣ қ ӯ ҳ ҷ` и IPA), `fontScale = 2.0f`, TalkBack, Age Gate 16+, Offline Leaderboards, Comments/Community Decks, AI Consent, Squads, Crowdsourcing, Rooms, PvP Disconnect и Peer Review TTL.
- **`backend/` (FastAPI + SQLAlchemy + WebSockets `pytest`):** **11 / 11 PASSED** (`6,84 с`).
- **ИТОГО автоматических тестов:** **104 / 104 PASSED**.

### 2.2. Контент и производительность БД (`КД-1`–`КД-5`, `НФТ-7.2`)
- **Академических верифицированных терминов (`academic_terms`):** **20 658** (`36` доменов, `10` категорий, `155` подкатегорий, `29` полисемичных значений, `3 723` связей, `19` авторитетных источников, `0` дубликатов, `0` `pending_review` в релизной БД).
- **Базовых IT-терминов (`words`):** **255** (суммарно **20 913** уникальных терминов).
- **Лицензированных транскрипций IPA:** **8 048** (с `ipa_source_url` и `ipa_license`, порог ≥ 5 000 выполнен).
- **Статей документации и реальных логов (`doc_articles`):** **166** (100% с `source_url` и `license`, порог ≥ 150 выполнен).
- **Рабочих сценариев (`scenarios`):** **156** (`12` контекстов × `13` сценариев) + **312** профессиональных фраз (`work_phrases`).
- **Сниппетов кода (`code_snippets`):** **380** (`19` языков × `20` задач).
- **Бенчмарк поиска на 30 000 записей (Room v5 + оптимизированный `@RawQuery`):**
  - Точный индексный поиск (`exactLookupMs`): **2 мс** (< 100 мс).
  - Пагинированный фильтр по домену и сложности (`paginatedFilterMs`): **12 мс** (< 100 мс).

### 2.3. Сборки, размеры, безопасность и запуск (`НФТ-7.3`, `НФТ-7.6`, `НФТ-7.7`)
- `app/src/main/assets/databases/tech_english.db` (Room v5): **86,37 МБ** (`90 562 560` байт).
- `app/build/outputs/apk/debug/app-debug.apk`: **37,34 МБ** (`39 150 954` байт).
- `app/build/outputs/apk/release/app-release-unsigned.apk`: **27,19 МБ** (`28 506 586` байт), `lintVitalRelease` пройден без ошибок.
- `app/build/outputs/bundle/release/app-release.aab`: **26,87 МБ** (`28 177 402` байт); сжатый размер модуля `base/` внутри AAB — **26,61 МБ** (`27 898 085` байт, запас до бюджета 100 МБ — более 73 МБ).
- **Запуск v3.0.0 на `emulator-5554` (API 37):** `versionName=3.0.0`, `versionCode=300`, `Status: ok`, `TotalTime = 4414 мс` (`WaitTime = 4429 мс` ≤ 5 000 мс), **0 ошибок** в logcat.
- **Гигиена безопасности:** 0 упоминаний Retrofit, Supabase или `EncryptedSharedPreferences`; 0 клиентских AI API-ключей; `cleartextTrafficPermitted="false"`; 0 абсолютных путей хоста и секретов в репозитории и чистом архиве `TechEnglish_Source_Clean.zip`.

---

## 3. Невыполненные / неизмеренные пункты (честная фиксация по §0.1 и §7.6)

1. **Блокер 7 (`translation_tg` ≥ 2 000 терминов):** в релизную БД включены только **19** верифицированных таджикских переводов; **2 050** кандидатов вынесены в `tools/vocabulary_import/output/tajik_translations_pending_review.json` со статусом `pending_review` (в БД не включены до ручной проверки носителем языка согласно §1 и §4.2). Механизм переключения RU/TJ с автоматическим fallback (Errata E6) и краудсорсинг «PR в словарь» (`ФТ-14.3`) полностью реализованы.
2. **`bundletool get-size`:** `не измерено` — утилита `bundletool` не установлена в локальной среде (фактический сжатый размер `.aab` = `26,87 МБ`, модуля `base/` = `26,61 МБ`).
3. **JankStats / Macrobenchmark и холодный старт Release-сборки на физическом Android-устройстве:** `не измерено` — проверка выполнялась на эмуляторе `emulator-5554` (API 37, `TotalTime = 4414 мс`).
4. **Нагрузочное тестирование PostgreSQL 16 / Redis 7 / MinIO в боевом Docker-кластере:** `не измерено` — контейнерная конфигурация `backend/docker-compose.yml` подготовлена, функциональная и WebSocket-верификация выполнена через `pytest` (`11/11 PASSED`).

---

## 4. Итоги Remediation v3.0.0 → v3.1.0 (Спринты 0 → 3)

**Версия:** `3.1.0` (`versionCode = 310`, ветка `remediation/v3.1`, теги `v3.1-sprint0`, `v3.1-sprint1a`, `v3.1-sprint1b`, `v3.1-sprint2a`, `v3.1-sprint2b`, `v3.1-sprint3`).  
**База данных:** Room `v6` (`identityHash = 2fed6cc837b9cd4374ad9e7e58e087bc`, миграции `1→2→3→4→5→6`, `@Fts4` `academic_terms_fts` + `normalized_search`, `squad_cache`, `ota_content_meta` diagnostics, перенесена в `install-time` PAD-модуль `:content_pack`).  
**Бэкенд:** декомпозированный FastAPI (`main.py` `2 847 → 125` строк, 10 роутеров), Alembic (`0001_initial_v3_0_0` … `0006_activity_feed_reactions`), Redis Pub/Sub & matchmaking (`RedisRealtimeBroker`), FCM HTTP v1, S3/MinIO presigned voice storage, асинхронный `HttpxGeminiClient`, `GooglePlayIntegrityVerifier`, серверный OAuth + RBAC + безопасные SHA-256 auth-токены.

### 4.1. Фактически измеренные дельты v3.0.0 → v3.1.0

| Метрика | v3.0.0 (`331bad5`) | v3.1.0 (`v3.1-sprint3`) |
|---|---:|---:|
| Автоматические тесты (`:domain` + `:app` + `backend`) | `104` (`57 + 36 + 11`) | **`160 passed, 0 failed, 1 skipped`** (`57 + 66 + 37`) |
| Покрытие `:domain` JaCoCo (LINE, XML / HTML / CSV) | `94.58%` / `94.58%` / `94.55%` | **`94.58%`** (`2844/3007`) / **`94.58%`** / **`94.55%`** (`2844/3008`) — порог ≥ 90% |
| `app-release.aab` (общий размер архива) | `26.87 MiB` | **`21.30 MiB`** (`22 334 905` Б) |
| `app-release.aab` → модуль `base` (сжатый / несжатый) | `26.61 MiB` / `147.33 MiB` | **`5.16 MiB`** / **`12.28 MiB`** (R8 + PAD `:content_pack`) |
| `app-release.aab` → модуль `content_pack` (сжатый / несжатый) | отсутствовал | **`10.75 MiB`** / **`103.24 MiB`** (`databases/tech_english.db`) |
| `app-release-unsigned.apk` (сжатый, R8 + shrinkResources) | `27.19 MiB` | **`5.59 MiB`** (`5 860 640` Б), `lintVitalRelease` — **PASSED** |
| `app-debug.apk` | `37.34 MiB` | **`40.66 MiB`** (`42 634 180` Б) |
| Поиск по 30 000 терминов (`Sp204FtsAndCyrillicSearchTest`, Robolectric) | `LIKE '%...%'` (без FTS) | **`8.13 мс`** средняя задержка (`< 15 мс`, `@Fts4` `unicode61`) |
| Распаковка БД `DatabaseBootstrapManager` (Robolectric, `Dispatchers.IO`) | не выделена | **`514 мс`** (1-й запуск) / **`0 мс`** (повторный) |
| Compose/ViewModel файлы `> 900` строк | `6` | **`0`** |
| Строки `backend/app/main.py` | `2 847` | **`125`** |
| Миграции Alembic (`backend/alembic/versions/`) | `0` (`create_all`) | **`6`** (`0001`…`0006`) |
| Верифицированные `translation_tg` в релизной БД (правило R7) | `19` | **`19`** (+ `150` черновиков `pending_review` в `docs/tg_review/`; дефицит `1 981` до цели `2 000`) |

### 4.2. Ограничения окружения (§0.1), внешние ключи (§0.2) и опциональный скоуп (R9)

- **Не измерено в локальной среде (§0.1):** `bundletool get-size total` (нет `bundletool`), холодный старт release AAB на реальном устройстве / AVD (нет запущенного AVD), живой `docker compose` / live PostgreSQL тест Alembic (остановлен Docker daemon; проверка настроена в `.github/workflows/ci.yml`, offline PostgreSQL DDL проверен).
- **E2E не проверено, нужен ключ (§0.2):** реальные вызовы Gemini API, GitHub/Google OAuth, Firebase Cloud Messaging, Google Play Integrity API, SMTP, production S3/MinIO, Sentry/Crashlytics, release keystore и публикация в Play Console Internal Track (R8). Полный чек-лист действий владельца и Data Safety находится в `docs/OWNER_ACTIONS.md`.
- **Таджикская локализация (R7):** `values-tg/strings.xml` включён за флагом `TAJIK_UI_LOCALIZATION_ENABLED=false`; в релизную БД не добавлены непроверенные машинные переводы (остаются строго 19 проверенных записей).
- **Опциональный скоуп (R9):** `SP3-05` (KMP/iOS прототип) и `SP3-06` (tap-глоссарий и онбординг) вынесены из обязательного скоупа v3.1.0 и ожидают отдельного решения владельца. Подробные отчёты по спринтам: `docs/SPRINT_0_REPORT.md` … `docs/SPRINT_3_REPORT.md`, журнал `docs/REMEDIATION_LOG.md`.
