# Отчёт по Этапу 2 — Расширение контента, MIGRATION_2_3, ContentPackImporter и офлайн-функции (ФТ-7.1–ФТ-7.6)

**Проект:** TechEnglish (`com.techenglish.app`) — модернизация v3.0 (редакция 3.1, с учётом Errata E1–E6)  
**Версия БД:** Room v3 (`PRAGMA user_version = 3`, `identityHash = 97c1e06c906948757fc30288d8e4788f`)  
**Тег релиза этапа:** `v3-stage2`

---

## 1. Сводка фактически измеренных метрик Этапа 2

| Метрика / Критерий приёмки | Требование ТЗ 3.2 / Errata | Фактически измерено | Статус |
|---|---|---|---|
| Верифицированные академические термины (`КД-1`) | ≥ 20 000 (`verified`) | **20 658** (`0` `pending_review` в релизной БД) | ✅ Выполнено |
| Тематические домены (`КД-1`) | 30 + 6 новых (36) | **36 доменов** (+ `GameDev`, `Web3 / Smart Contracts`, `Mobile Internals (Android / iOS)`, `QA & Testing`, `UI/UX Engineering`, `Product Management`) | ✅ Выполнено |
| Лицензионные IPA-транскрипции (`КД-2`) | ≥ 5 000 с `ipa_source_url` и `ipa_license` | **8 048** (100% из `open-dict-data/ipa-dict` / CMUdict, с заполненными `ipa_source_url` и `ipa_license`) | ✅ Выполнено |
| Таджикские переводы `translation_tg` (`КД-2`, Блокер 7) | ≥ 2 000 верифицированных человеком | **19** в релизной БД (`verified`) + **2 050** кандидатов в `tools/vocabulary_import/output/tajik_translations_pending_review.json` (`pending_review`, в БД не включены) | ⚠️ Дефицит человеческой верификации (см. §4) |
| Статьи документации и логи ошибок (`КД-3`) | ≥ 150, у всех `source_url` и `license` | **166** (166/166 с `source_url` и `license`, 0 PII/токенов) | ✅ Выполнено |
| Рабочие сценарии (`КД-4`) | ≥ 150, 12 контекстов, ≥ 12 в каждом | **156** (12 контекстов, ровно **13** сценариев в каждом) | ✅ Выполнено |
| Рабочие IT-фразы (`КД-4`) | ≥ 300 | **312** (26 фраз на каждый из 12 контекстов) | ✅ Выполнено |
| Примеры кода (`КД-5`) | ≥ 350 (19 языков × 20 задач) | **380** (100% покрытие: 19 языков × 20 `task_key`, закрыты все 21 пробела v2 и добавлены 10 новых задач) | ✅ Выполнено |
| Переключатель языка перевода RU / TJ (Errata E6) | В настройках и словаре с авто-фолбэком на RU | Реализован (`TranslationLanguage.RU` / `TJ` в DataStore, Профиле, Академическом словаре, SRS, Read Your Own Docs и Glance-виджете) | ✅ Выполнено |
| Офлайн-функции `ФТ-7.1`–`ФТ-7.6` | 6 модулей полностью офлайн | Все 6 реализованы (Read Your Own Docs, Debug the Log, Shadowing API 33+, Glance Widget, Time Attack 60с, Deck Import/Export JSON/CSV/Anki TSV & `.apkg`) | ✅ Выполнено |
| `MIGRATION_1_2` + `MIGRATION_2_3` + `ContentPackImporter` (Errata E3) | Без потери SRS/прогресса, идемпотентность, отклонение `pending_review` | Проверено автотестами в `TechEnglishDatabaseTest` (v1→v2→v3 и v2→v3→дельта-импорт с SHA-256 и повторным запуском) | ✅ Выполнено |
| Бенчмарк поиска при 30 000 записях | < 100 мс | **`exactLookupMs = 3 мс`**, **`paginatedFilterMs = 73 мс`** (Robolectric SQLite на 30 000 записей) | ✅ Выполнено |
| Автотесты (`:domain` + `:app`) | 100% зелёные | **65 / 65 PASSED** (`43` Domain + `22` App, `0` failures, `0` errors) | ✅ Выполнено |
| Покрытие `:domain` (JaCoCo) | ≥ 90%, `CSV == XML == HTML` | **93.38%** (`2045 / 2190` строк; CSV = `93.38%`, XML = `93.38%`, совпадение 100%) | ✅ Выполнено |
| Размер Release AAB (бюджет ≤ 100 МБ) | ≤ 100 МБ | **`app-release.aab = 24.94 МБ`** (`tech_english.db`: `86.24 МБ` raw → `8.67 МБ` сжато в AAB); `bundletool get-size total` — не измерено (CLI `bundletool` не установлен в PATH, приведён точный размер собранного `app-release.aab`); `app-debug.apk = 35 МБ` | ✅ В бюджете (4× запас) |
| Холодный первый запуск (`adb shell am start -W`) | Замер на подключённом устройстве | `emulator-5554` (debug APK, software SwiftShader x86_64 API 37): **1-й холодный старт = `6 475 мс`** (`WaitTime = 6 483 мс`), **2-й холодный старт = `7 092 мс`**, процесс активен (`PID 14285`), 0 фатальных ошибок в `logcat`; физическое устройство / release-сборка — **не измерено** | ✅ Замерено без сбоев |

---

## 2. Реализованные изменения архитектуры и модулей

### База данных Room v3 и контент-доставка (Errata E3)
- Экспортирована схема `app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/3.json` (`version = 3`, 15 таблиц, 29 индексов).
- Добавлены сущности и DAO в `Stage2Entities.kt` / `Stage2Daos.kt`: `custom_imported_docs` и `time_attack_records`.
- Расширены существующие таблицы аддитивно через `MIGRATION_2_3`:
  - `doc_articles`: `source_url`, `license`
  - `word_sources`: `license`
  - `academic_terms`: `ipa_source_url`, `ipa_license`
- Реализован идемпотентный `ContentPackImporter.kt` с проверкой SHA-256, фильтрацией `verification_status == 'verified'` и обновлением контента по `normalized_term` (`updateContentPreservingSrs`, `updateContentPreservingProgress`) без перезаписи пользовательских полей SM-2 (`repetitions`, `ease_factor`, `interval_days`, `next_review_at`, `last_studied_at`, `is_favorite`, `is_in_srs`) и прогресса чтения статей.
- Обновлён детерминированный генератор `scripts/build_seed_db.py` и модули данных:
  - `tools/vocabulary_import/licensed_ipa_lexicon.py` (3 303 слова из открытого корпуса `open-dict-data/ipa-dict`), `lexicon_new_domains_v3.py`, `corpus_builder.py`, `vocabulary_statistics.py`.
  - `scripts/seed_data/docs_v3_expansion.py` (стектрейсы Kotlin/Android, Python, Docker, Git, PostgreSQL deadlocks, HTTP/CORS, адаптированные RFC и README).
  - `scripts/seed_data/scenarios_v3_expansion.py` (4 новых контекста `SYSTEM_DESIGN_INTERVIEW`, `INCIDENT_POST_MORTEM`, `SALARY_NEGOTIATION`, `CLIENT_COMMUNICATION`, 156 сценариев, 312 фраз).
  - `scripts/seed_data/snippets_v3_expansion.py` (380 примеров кода по 19 языкам × 20 задачам).

### 6 новых офлайн-функций (`ФТ-7.1`–`ФТ-7.6`) и Errata E6
- **ФТ-7.1 Read Your Own Docs:** `ScanCustomDocTermsUseCase` (1..4-словные n-граммы + стемминг окончаний на `Dispatchers.Default`, хранение в `custom_imported_docs`) + `ReadYourOwnDocsScreen` (SAF `OpenDocument` для `.md`/`.txt` + вставка текста + интерактивная подсветка и bottom sheet с озвучкой и добавлением в SRS).
- **ФТ-7.2 Debug the Log:** `DebugLogTrainerUseCase` + `DebugTheLogScreen` в терминальном контейнере `ChameleonScenarioContainer(ChameleonThemeType.TERMINAL)` (шаг 1: определение корневой причины, шаг 2: сборка каноничного английского баг-репорта для Jira/GitHub).
- **ФТ-7.3 Speaking / Shadowing:** `EvaluatePronunciationUseCase` (пословный diff с допуском Левенштейна) + `OnDeviceSpeechRecognizerHelper` (строгая проверка `SDK_INT >= TIRAMISU && isOnDeviceRecognitionAvailable`, `createOnDeviceSpeechRecognizer`, разрешение `RECORD_AUDIO` в рантайме и корректная плашка «Нужен голосовой пакет или сеть» на API 26–32 / эмуляторе).
- **ФТ-7.4 Home Screen Widget (Jetpack Glance):** `TechEnglishGlanceWidget` + `TechEnglishGlanceWidgetReceiver` + `res/xml/tech_english_widget_info.xml` («Слово дня» с IPA и RU/TJ переводом, счётчик карточек к повторению, текущий Streak, запуск карточек в 1 тап).
- **ФТ-7.5 Time Attack (60 секунд):** `TimeAttackQuizUseCase` + `TimeAttackScreen` (режимы перевода IT-терминов и поиска ошибок в PR, комбо-множитель, сохранение личных рекордов в `time_attack_records`).
- **ФТ-7.6 Deck Import & Export:** `DeckImportExportUseCase` + `AnkiApkgExtractor` (экспорт в JSON и CSV; импорт JSON, CSV, Anki TSV и ZIP-архивов Anki `.apkg` из таблицы `notes` с безопасной потоковой распаковкой и игнорированием расписания `cards` Anki в пользу собственного SM-2).
- **Errata E6:** Переключатель `RU / TJ` (`TranslationLanguage`) в `UserPreferencesDataStore`, `ProfileScreen`, `AcademicDictionaryScreen`, `AcademicTermDetailScreen`, `AcademicSrsScreen` и Glance-виджете с автоматическим откатом на `translation_ru`.

---

## 3. Проверенные команды сборки и тестирования

1. `python3 scripts/build_seed_db.py && python3 tools/vocabulary_import/vocabulary_statistics.py` — **PASSED** (`tech_english.db` v3, 20 658 терминов, 24/24 проверяемых индексов, `PRAGMA integrity_check == ok`, `PRAGMA foreign_key_check == []`).
2. `./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport` — **PASSED** (43 теста, 0 ошибок, JaCoCo Line Coverage **93.38%** `2045/2190`, `CSV == XML`).
3. `./gradlew :app:testDebugUnitTest` — **PASSED** (22 теста, 0 ошибок; `STAGE2_BENCHMARK_30K: exactLookupMs=3, paginatedFilterMs=73`).
4. `./gradlew :app:bundleRelease :app:assembleDebug` — **PASSED** (`app-release.aab = 24.94 МБ`, `app-debug.apk = 35 МБ`).
5. `adb install -r app-debug.apk && adb shell pm clear com.techenglish.app && adb shell am start -W -S com.techenglish.app/.MainActivity` — **PASSED** (запущено без падений на `emulator-5554`).

---

## 4. Открытый дефицит приёмки: Таджикские переводы `translation_tg` (Блокер 7)

Согласно §0.1, §0.2 (Блокер 7), §1 (*«непроверенные переводы запрещены»*) и §4.2 (*«в счёт идут только записи, верифицированные человеком или подтверждённые источником; остальные — `pending_review`, в поставку не входят»*):

- **Точный дефицит:** В релизной базе `tech_english.db` сейчас **19** подтверждённых таджикских переводов вместо целевых **≥ 2 000** (дефицит: **1 981** верифицированный перевод).
- **Что подготовлено:** Сформирован отдельный файл-очередь [tajik_translations_pending_review.json](../tools/vocabulary_import/output/tajik_translations_pending_review.json) на **2 050 терминов** со статусом `"verification_status": "pending_review"`, который строго исключён из релизной БД и готов к проверке носителем языка / IT-лингвистом. Переключатель `RU / TJ` (Errata E6) полностью работает и корректно подставляет русский перевод там, где `translation_tg` пуст.

### Варианты решения для владельца проекта:
1. **Вариант A (Рекомендуется для продолжения по графику):** Принять Этап 2 с 19 верифицированными таджикскими терминами в базовой поставке и автоматическим RU-фолбэком, перенести наполнение ≥ 2 000 `translation_tg` в дельта-пакет `ContentPackImporter` (доставляемый после человеческой ревизии [tajik_translations_pending_review.json](../tools/vocabulary_import/output/tajik_translations_pending_review.json)), и перейти к Этапу 3 (`go`).
2. **Вариант B:** Передать готовый верифицированный CSV/JSON словарь таджикских IT-терминов (≥ 2 000 записей от носителя языка / проверенного словарного источника) прямо сейчас — после этого я прогоню `vocabulary_importer.py` + `build_seed_db.py` и пересоберу `tech_english.db` перед переходом к Этапу 3.

**Ожидаю вашего решения по Блокеру 7 и подтверждения (`go`) для перехода к Этапу 3.**
