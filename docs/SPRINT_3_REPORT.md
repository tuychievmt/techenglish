# Отчёт по Спринту 3 и завершению Remediation (TechEnglish v3.0.0 → v3.1.0)

- **Ветка:** `remediation/v3.1`
- **Тег подэтапа / релиза remediation:** `v3.1-sprint3`
- **Версия приложения:** `3.1.0` (`versionCode = 310`)
- **Скоуп Спринта 3:** `SP3-01` (`BUG-09`), `SP3-02` (`BUG-06` клиент), `SP3-03` (`BUG-20`, `R7`), `SP3-04` (`BUG-19`), `SP3-07` (`BUG-22`, `R8`)
- **Опциональный скоуп (не выполнялся по правилу R9 без отдельного `go`):** `SP3-05` (KMP/iOS прототип), `SP3-06` (tap-глоссарий и онбординг). Дефект `BUG-23` вне скоупа remediation.

---

## 1. Выполненные задачи и коммиты Спринта 3

| Задача | Дефект / Правка | Коммит | Ключевые изменения |
|---|---|---|---|
| **SP3-01** | `BUG-09` | `082ba5d` | Бэкенд: `_public_pvp_questions()` удаляет `correct_option_index` из `POST /v1/pvp/matchmake` и WS-события `match_ready`; правильный ответ раскрывается только в `answer_ack` после ответа игрока. Android: `PvpDuelViewModel` подключён к живому `/ws/pvp/{match_id}` (`?ticket=`), отправляет ответы и отображает живой `opponentScore`, корректно обрабатывает разрыв связи и сохраняет 15-секундный unranked training fallback. Тесты: `test_sp3_01_pvp_no_answer_leak.py`, `Sp301PvpLiveWsTest`. |
| **SP3-02** | `BUG-06` (клиент) | `229f510` | Добавлена реальная запись голосовых клипов `PeerVoiceClipRecorder` (`AndroidPeerVoiceClipRecorder`, M4A/AAC, строгий лимит ≤ 20 с и ≤ 512 КБ), runtime-запрос `RECORD_AUDIO` с обработкой отказа в `PeerReviewArenaScreen`, загрузка presigned URL через `TechEnglishApiClient.fetchPeerReviewVoiceUrl()` и воспроизведение через `TechAudioController.playRemoteAudioUrl()`. Тест: `Sp302PeerReviewVoiceTest`. |
| **SP3-03** | `BUG-20`, `R7` | `c27b947` | Добавлены `app/src/main/res/values-tg/strings.xml` и `TajikLocalizationPolicy` за флагом `BuildConfig.TAJIK_UI_LOCALIZATION_ENABLED` (по умолчанию `false`) с RU fallback. Подготовлены пакеты черновиков для человеческой верификации в `docs/tg_review/` (`README.md`, `ui_strings_tg_draft.json`, `terms_tg_draft.csv` — 150 записей строго со статусом `pending_review`) и скрипт строгого импорта `scripts/import_verified_tg.py`. В релизной БД сохранены строго **19** проверенных человеком `translation_tg` (0 машинных записей с `verified`). Тест: `Sp303TajikLocalizationPolicyTest`. |
| **SP3-04** | `BUG-19` | `d21aeeb` | Офлайн-сообщения в тематических комнатах (`local_msg_*`) ставятся в `sync_queue` (`ROOM_MESSAGE_OUTBOX`) и автоматически переотправляются при восстановлении сети с заменой локальной записи на серверную. `WsConnectionState` обновляется правдиво после живого handshake. Добавлены `SquadCacheEntity` + `SquadCacheDao` в Room v6 (`MIGRATION_5_6`, `6.json`, пересобран предзаполненный `tech_english.db` = `103.24 MiB`, `identityHash = 2fed6cc837b9cd4374ad9e7e58e087bc`), подключённые к `SquadsViewModel`. Тест: `Sp304OfflineRoomsAndSquadCacheTest`. |
| **SP3-07** | `BUG-22`, `R8` | *текущий коммит* | Поднята версия до `3.1.0` (`versionCode = 310`), `Stage4CViewModels.kt` (906 строк) разбит на 3 файла (`ThematicRoomsViewModel.kt`, `PvpDuelViewModel.kt`, `PeerReviewArenaViewModel.kt` — `0` файлов `> 900` строк), актуализированы `docs/OWNER_ACTIONS.md` (с Data Safety и Internal Track чек-листом R8), `docs/ARCHITECTURE.md`, `docs/REQUIREMENTS_TRACEABILITY.md`, `docs/FINAL_REPORT.md`, `docs/REMEDIATION_LOG.md`. Собраны v3.1.0 debug/release APK, release AAB и чистый архив исходников `TechEnglish_Source_Clean.zip`. |

---

## 2. Финальные фактически измеренные метрики v3.1.0

### 2.1. Тесты и покрытие кода

| Проверка | Команда | Фактический результат |
|---|---|---|
| Backend (`FastAPI + SQLAlchemy + Redis + Alembic`) | `cd backend && .venv/bin/pytest -v` | **37 passed, 1 skipped, 0 failed** (Alembic `0001`…`0006` на SQLite + PostgreSQL offline DDL `PostgresqlImpl` — PASSED; live PG пропущен локально из-за остановленного Docker daemon) |
| `:domain` (Pure Kotlin JVM, 0 Android SDK deps) | `./gradlew :domain:test :domain:jacocoTestReport :domain:jacocoTestCoverageVerification` | **57 passed, 0 failed**; JaCoCo LINE: **94.58%** (`2844/3007`) в XML и HTML, **94.55%** (`2844/3008`, 1 синтетическая строка класса) в CSV; INSTRUCTION (`88.13%`) и BRANCH (`73.83%`) совпадают 100% |
| `:app` (Robolectric + Room `1→6` + Compose + Security) | `./gradlew :app:testDebugUnitTest` | **66 passed, 0 failed**; средняя задержка FTS4-поиска на 30 000 терминов (`Sp204FtsAndCyrillicSearchTest`): **8.13 мс** (`< 15 мс`); `0` UI/ViewModel файлов `> 900` строк |
| **ИТОГО локальных тестов** | | **160 passed, 0 failed, 1 skipped** (базовая линия v3.0.0: `104`) |

### 2.2. Финальные артефакты сборки v3.1.0 (`versionCode = 310`)

| Артефакт | Путь | Фактический размер | Примечание |
|---|---|---|---|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | **40.66 MiB** (`42 634 180` Б) | Debug-сборка (включает debug `network_security_config.xml`) |
| Release APK (unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | **5.59 MiB** (`5 860 640` Б) | R8 + `isShrinkResources = true`, `lintVitalRelease` — PASSED (было `27.19 MiB` в v3.0.0) |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | **21.30 MiB** (`22 334 905` Б) | `base`: **5.16 MiB** сжатый (`12.28 MiB` несжатый); `content_pack` (`install-time` PAD): **10.75 MiB** сжатый (`103.24 MiB` несжатый); `BUNDLE-METADATA`: `5.15 MiB` (R8 symbols, не доставляется пользователям) |
| Предзаполненная БД (Room v6) | `content_pack/src/main/assets/databases/tech_english.db` | **103.24 MiB** (`108 253 184` Б) | `Room Version: 6`, `identityHash: 2fed6cc837b9cd4374ad9e7e58e087bc`, `20 658` верифицированных терминов, `19` проверенных `translation_tg`, `31/31` индексов + `@Fts4` |
| Чистый архив исходников | `TechEnglish_Source_Clean.zip` | **13.66 MiB** (`316` файлов) | 0 секретов, 0 `.git/`, 0 `build/`, 0 `.venv/`, 0 `local.properties`, 0 `*.keystore`/`*.jks`, 0 `google-services.json` |

---

## 3. Честная фиксация ограничений, дефицитов и действий владельца (§0.1, §0.2, R7, R8)

1. **Таджикская локализация (R7 / `SP3-03`):** в релизной БД `tech_english.db` находятся строго **19** проверенных человеком `translation_tg` (дефицит до целевых `2 000`: **1 981** запись). **150** черновиков терминов и черновик UI-строк размещены в `docs/tg_review/` со статусом `pending_review`. Включение `values-tg` заблокировано флагом `TAJIK_UI_LOCALIZATION_ENABLED=false` до ручного одобрения владельца и импорта через `scripts/import_verified_tg.py`.
2. **Не измерено в локальной среде (§0.1):**
   - `bundletool get-size total` — не измерено (нет `bundletool` в окружении); замерены точные размеры модулей внутри AAB (`base = 5.16 MiB`, `content_pack = 10.75 MiB`).
   - Время первого и повторного холодного старта на реальном Android-устройстве / AVD — не измерено (нет запущенного AVD); в Robolectric замерена off-main-thread распаковка `DatabaseBootstrapManager`: **514 мс** (1-й старт) / **0 мс** (повторный).
   - Live PostgreSQL 16 прогон Alembic и `docker compose build` — пропущено локально (Docker daemon остановлен); полная проверка настроена в `.github/workflows/ci.yml`.
3. **E2E не проверено, нужен ключ (§0.2) и шаги владельца (R8):** реальные ключи Gemini API, GitHub/Google OAuth, Firebase (`google-services.json` + FCM service account), Google Play Integrity API, SMTP, production S3/MinIO, Sentry/Crashlytics, release signing keystore, а также заполнение Play Console Data Safety и загрузка подписанного AAB в Internal Track. Полный чек-лист с точными именами переменных окружения и интерфейсами см. в `docs/OWNER_ACTIONS.md`.
