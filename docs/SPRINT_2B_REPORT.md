# Отчёт по Спринту 2B (TechEnglish v3.0.0 → v3.1.0 Remediation)

- **Ветка:** `remediation/v3.1`
- **Тег подэтапа:** `v3.1-sprint2b`
- **Скоуп:** `SP2-03` (`BUG-18`), `SP2-04` (`BUG-15`, `R6`, `R12`), `SP2-05` (`BUG-15`, `R4`, `R5`), `SP2-06` (`BUG-21`), `SP2-07` (`BUG-24`), `SP2-08` (`BUG-25`), `SP2-09` (`BUG-26`)

---

## 1. Выполненные задачи и коммиты

| Задача | Дефект / Правка | Коммит | Ключевые изменения |
|---|---|---|---|
| **SP2-03** | `BUG-18` | `5570770` | Разбиты все 6 Compose-монолитов `> 900` строк (`Stage3Screens`, `Stage2Screens`, `Components`, `AcademicScreens`, `ProgrammingScreens`, `ProfileScreen`) на целевые подэкраны (`≤ 446` строк). Ключевые `UiState` переведены на `@Immutable` + `kotlinx.collections.immutable.ImmutableList`. Включены метрики и отчёты стабильности Compose Compiler (`app/build/compose_compiler/`). Визуальный стиль не изменён. |
| **SP2-04** | `BUG-15`, `R6`, `R12` | `bd246e9` | Зафиксирован ADR `DEC-14` (`docs/DECISIONS.md`) по выбору `@Fts4(contentEntity = AcademicTermEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)` + `normalized_search` для гарантированной работы на `minSdk 26`. Выполнена миграция Room v5 → v6 (`MIGRATION_5_6`), поддержан регистронезависимый поиск по кириллице и fallback-поиск подстрок. Обновлён `scripts/build_seed_db.py`, пересобран предзаполненный `tech_english.db` до Room v6 с готовым FTS4-индексом. |
| **SP2-05** | `BUG-15`, `R4`, `R5` | `b8cbde6` | Выделен `install-time` Play Asset Delivery модуль `:content_pack` с `databases/tech_english.db` (удалён из `app/src/main/assets/`). Реализован off-main-thread `DatabaseBootstrapManager` с поэтапным прогрессом (`LinearProgressIndicator`) в `MainActivity` при первом запуске и мгновенной проверкой при повторных запусках. |
| **SP2-06** | `BUG-21` | `7fa45a5` | Выделен единый экран-хаб `CommunityArenaHubScreen` (`Routes.COMMUNITY_ARENA_HUB`). `ProfileScreen` сокращён с 12 до **5 секций**. Из `PracticeHubScreen` удалены 3 дублирующихся маршрута (`AI-ментор`, `PvP-дуэль`, `Peer Review Arena`). |
| **SP2-07** | `BUG-24` | `e0de29e` | В `TechEnglishGlanceWidget` удалён хардкод Tokyo Night (`0xFF1A1B26`), палитра читается из активной `ThemeMode` в DataStore (`resolveWidgetPalette`), добавлена кнопка обновления «Слова дня» (`RefreshWidgetWordAction`). |
| **SP2-08** | `BUG-25` | `76c716a` | Бэкенд: таблица `activity_feed_reactions` + миграция Alembic `0006_activity_feed_reactions`, идемпотентный `POST /v1/friends/feed/{event_id}/react`, поле `user_reacted` в `GET /v1/friends/feed`. Android: сохранение `userReacted` при синке, поля `last_status`/`last_error`/`last_attempt_at` в `ota_content_meta`, диагностика OTA в `CloudAccountSyncScreen` и повторная загрузка повреждённых OTA-пакетов. |
| **SP2-09** | `BUG-26` | `cba44fa` | Добавлена абстракция `CrashReporter` (`ConfigurableCrashReporter` с флагами `CRASH_REPORTING_ENABLED`/`SENTRY_DSN`, 0 секретов в репозитории, санитайзер PII/токенов) и выгрузка `app/build/compose_compiler/` в `.github/workflows/ci.yml`. |
| — | Тесты/JVM | `2c3757a` | Обновлён путь проверки в `Sp110ThemeAndCrowdsourceFixesTest` после разбиения `feature/programming/` и увеличен `maxHeapSize = "1536m"` для юнит-тестов `:app` (открытие 103 МБ asset DB в Robolectric). |

---

## 2. Фактически измеренные метрики и результаты проверок

| Метрика / Проверка | Фактический результат |
|---|---|
| Compose-файлы `> 900` строк (`feature/`, `core/designsystem/`) | **0 файлов** (было 6; все разбитые экраны `≤ 446` строк) |
| Отчёт стабильности Compose Compiler (`app_debug-module.json`, полная компиляция в `SP2-03`) | `skippableComposables`: **973 / 1482**, `knownStableArguments`: **24 628 / 24 719 (99.63%)**, `StrongSkipping: true`, `markedStableClasses: 7` |
| Поиск по 30 000 терминов (`Sp204FtsAndCyrillicSearchTest`, Robolectric SQLite) | Средняя задержка: **12.25 мс** (критерий DoD: `< 15 мс`); `EXPLAIN QUERY PLAN` использует `academic_terms_fts`; кириллица (верхний/нижний регистр) и подстроки — **PASSED** |
| Цепочка миграций Room `1→2→3→4→5→6` (`TechEnglishDatabaseTest`) | **PASSED** (`PRAGMA user_version = 6`, `20 658` терминов, `31/31` индексов) |
| Распаковка БД `DatabaseBootstrapManager` (Robolectric, `103.23 MiB`) | Первый запуск (off main thread, `Dispatchers.IO`): **514 мс**; повторный запуск: **0 мс** |
| Сборка `:app:bundleRelease` (с R8 + PAD `:content_pack`) | `app-release.aab` всего: **21.28 MiB**; модуль `base`: **5.15 MiB** сжатый (`12.24 MiB` без сжатия); модуль `content_pack`: **10.74 MiB** сжатый (`103.23 MiB` без сжатия, `databases/tech_english.db`) |
| `cd backend && .venv/bin/pytest -v` | **36 passed, 1 skipped**, 0 failed (Alembic SQLite `0001`…`0006` + PostgreSQL offline DDL `PostgresqlImpl` — **PASSED**) |
| `./gradlew :domain:test :domain:jacocoTestCoverageVerification` | **57/57 PASSED**, JaCoCo LINE **94.58%** (`2844/3007`, порог `≥ 90%`) |
| `./gradlew :app:testDebugUnitTest` | **62/62 PASSED**, 0 failed |
| **Итого локальных тестов** | **155 passed, 0 failed, 1 skipped** |

---

## 3. Неизмеренные / требующие ключей владельца пункты (§0.1, §0.2)

- **Точный размер загрузки через `bundletool get-size total` и замер холодного старта на Android-устройстве / AVD:** не измерено (нет AVD и `bundletool` в окружении). Вместо этого замерены точные размеры модулей внутри собранного `app-release.aab` (`base = 5.15 MiB`, `content_pack = 10.74 MiB`) и время распаковки БД в Robolectric (`514 мс` первый старт / `0 мс` повторный).
- **Боевая отправка crash-репортов (Sentry / Crashlytics):** **E2E не проверено, нужен ключ** (`CRASH_REPORTING_ENABLED=true` и `SENTRY_DSN` или конфигурация Crashlytics в релизной сборке владельца). В репозитории секретов нет, очистка PII проверена в `Sp209CrashReporterAndCiTest`.
- **Live PostgreSQL 16 тест Alembic:** пропущен локально (остановлен Docker daemon), проверяется в CI (`.github/workflows/ci.yml`), offline PostgreSQL DDL до `0006_activity_feed_reactions` проверен.

---

## 4. Статус готовности

Все задачи Спринта 2B (`SP2-03`…`SP2-09`) и критерии DoD выполнены. Ожидание команды **`go`** от владельца для перехода к **Спринту 3** (`SP3-01`, `SP3-02`, `SP3-03`, `SP3-04`, `SP3-07`; опциональные `SP3-05` / `SP3-06` согласно R9 не выполняются без отдельного `go`).
