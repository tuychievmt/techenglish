# Комплексный технический аудит проекта и баз данных `TechEnglish`

> **Режим составления отчёта:** строгий read-only аудит исходного кода, конфигураций, экспортированных схем Room и файла SQLite (`mode=ro&immutable=1`).  
> **Версия приложения:** `3.0.0` (`versionCode = 300`, `applicationId = "com.techenglish.app"`).  
> **Дата аудита:** `2026-11-20`.

---

## Содержание

1. [Обзор и расположение файлов в папке проекта](#1-обзор-и-расположение-файлов-в-папке-проекта)
2. [Подробный отчёт по локальной базе данных (Room / SQLite)](#2-подробный-отчёт-по-локальной-базе-данных-room--sqlite)
3. [Отчёт по базе данных и хранилищам бэкенда](#3-отчёт-по-базе-данных-и-хранилищам-бэкенда)
4. [Конвейер данных, импорта и синхронизации](#4-конвейер-данных-импорта-и-синхронизации)
5. [Техническая инвентаризация проекта](#5-техническая-инвентаризация-проекта)

---

## 1. Обзор и расположение файлов в папке проекта

### 1.1. Сводная таблица модулей и корневых директорий

> Исключены служебные каталоги: `.git/`, `.gradle/`, `build/`, `.idea/`, `__pycache__/`, `.venv/`.  
> Всего на диске (без 6 исключённых папок): **215 файлов**, из них **202** отслеживаются в Git (`git ls-files`) и **13** не отслеживаются / игнорируются (`.gitignore`), плюс пустой служебный каталог `.kotlin/sessions/`.

| Путь | Назначение | Файлов в Git | Файлов на диске (excl. 6 dirs) | Примечание по составу |
|---|---|---:|---:|---|
| `/` (корень) | Конфигурация корневого Gradle, `.gitignore`, спецификация | 6 | 9 | Git: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `.gitignore`, `Техническая_спецификация_исправленная.md`. Untracked: `.DS_Store`, `local.properties`, `TechEnglish_Source_Clean.zip` |
| `gradle/` | Gradle Version Catalog и Gradle Wrapper | 3 | 3 | `libs.versions.toml`, `wrapper/gradle-wrapper.jar`, `wrapper/gradle-wrapper.properties` |
| `app/` | Android-приложение (Compose UI, Room v5, DataStore, Keystore, Ktor, WorkManager, Glance Widget, ресурсы, ассеты, тесты) | 100 | 101 | 1 `build.gradle.kts`, 1 `proguard-rules.pro`, 5 JSON-схем Room, 90 файлов в `src/main/` (72 `.kt`, 1 `AndroidManifest.xml`, 1 `tech_english.db`, 16 в `res/`), 3 в `src/test/`. Untracked: `.DS_Store` |
| `domain/` | Чистый Kotlin JVM доменный модуль (алгоритм SM-2, модели, интерфейсы репозиториев, UseCases, unit-тесты) | 38 | 39 | 1 `build.gradle.kts`, 24 `.kt` в `src/main/`, 13 `.kt` в `src/test/`. Untracked: `.DS_Store` |
| `backend/` | Серверная часть (FastAPI, SQLAlchemy 2.0, anti-cheat, AI-прокси, WebSockets, хранилище голосовых записей, Docker Compose, тесты) | 15 | 17 | `Dockerfile`, `docker-compose.yml`, `requirements.txt`, 8 файлов в `app/`, 4 в `tests/`. Untracked: `.DS_Store`, `test_stage3.db` (438 272 Б, 0 таблиц) |
| `scripts/` | Скрипт сборки предзаполненной SQLite БД и модули seed-данных | 11 | 11 | `build_seed_db.py` + 10 файлов в `seed_data/` |
| `tools/vocabulary_import/` | Конвейер сборки, валидации, импорта и статистики академического словаря + выходные файлы | 12 | 12 | 10 файлов скриптов/лексиконов/схемы + 2 файла в `output/` |
| `docs/` | Архитектурная и отчётная документация, ТЗ, RTM, ADR, аудит качества словаря | 17 | 17 | 17 Markdown-документов (`ARCHITECTURE.md`, `techenglish_v3_2_final.md`, отчёты этапов и др.) |
| `.pytest_cache/` | Локальный кэш `pytest` (gitignored) | 0 | 6 | Служебные файлы кэша тестов Python |
| `.kotlin/` | Локальная служебная директория Kotlin (gitignored) | 0 | 0 | Содержит только пустой каталог `.kotlin/sessions/` |
| **Итого** | | **202** | **215** | |

---

### 1.2. Полное дерево каталогов и файлов проекта

Ниже приведено полное дерево файлов проекта (исключая `.git/`, `.gradle/`, `build/`, `.idea/`, `__pycache__/`, `.venv/`). Пометкой `[untracked]` отмечены 13 локальных/gitignored-файлов на диске.

```text
TechEnglish/
├── .DS_Store                                              [untracked, 10 244 B]
├── .gitignore                                             (349 B)
├── .kotlin/
│   └── sessions/                                          [untracked, пустой каталог]
├── .pytest_cache/                                         [untracked, 6 файлов]
│   ├── .gitignore                                         [untracked, 37 B]
│   ├── CACHEDIR.TAG                                       [untracked, 191 B]
│   ├── README.md                                          [untracked, 302 B]
│   └── v/cache/
│       ├── lastfailed                                     [untracked, 2 B]
│       ├── nodeids                                        [untracked, 1 142 B]
│       └── stepwise                                       [untracked, 2 B]
├── TechEnglish_Source_Clean.zip                           [untracked, 11 964 483 B]
├── build.gradle.kts                                       (355 B)
├── gradle.properties                                      (259 B)
├── gradlew                                                (3 380 B)
├── local.properties                                       [untracked, 374 B]
├── settings.gradle.kts                                    (555 B)
├── Техническая_спецификация_исправленная.md               (42 857 B)
├── app/
│   ├── .DS_Store                                          [untracked, 6 148 B]
│   ├── build.gradle.kts                                   (4 612 B)
│   ├── proguard-rules.pro                                 (329 B)
│   ├── schemas/com.techenglish.app.core.database.TechEnglishDatabase/
│   │   ├── 1.json                                         (25 441 B)
│   │   ├── 2.json                                         (45 016 B)
│   │   ├── 3.json                                         (50 777 B)
│   │   ├── 4.json                                         (68 508 B)
│   │   └── 5.json                                         (80 010 B)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                        (3 046 B)
│       │   ├── assets/databases/
│       │   │   └── tech_english.db                        (90 562 560 B)
│       │   ├── kotlin/com/techenglish/app/
│       │   │   ├── MainActivity.kt                        (3 153 B)
│       │   │   ├── TechEnglishApplication.kt              (525 B)
│       │   │   ├── core/
│       │   │   │   ├── audio/
│       │   │   │   │   ├── OnDeviceSpeechRecognizerHelper.kt (4 908 B)
│       │   │   │   │   └── TechAudioController.kt         (17 959 B)
│       │   │   │   ├── database/
│       │   │   │   │   ├── AnkiApkgExtractor.kt           (3 528 B)
│       │   │   │   │   ├── ContentPackImporter.kt         (12 471 B)
│       │   │   │   │   ├── TechEnglishDatabase.kt         (25 538 B)
│       │   │   │   │   ├── dao/
│       │   │   │   │   │   ├── AcademicTermDao.kt         (16 525 B)
│       │   │   │   │   │   ├── CategoryDao.kt             (2 124 B)
│       │   │   │   │   │   ├── DocArticleDao.kt           (1 727 B)
│       │   │   │   │   │   ├── ProgrammingLanguageDao.kt  (2 307 B)
│       │   │   │   │   │   ├── ScenarioDao.kt             (1 444 B)
│       │   │   │   │   │   ├── Stage2Daos.kt              (1 814 B)
│       │   │   │   │   │   ├── Stage3Daos.kt              (4 910 B)
│       │   │   │   │   │   ├── Stage4ADaos.kt             (3 934 B)
│       │   │   │   │   │   ├── StudyLogDao.kt             (1 732 B)
│       │   │   │   │   │   └── WordDao.kt                 (6 063 B)
│       │   │   │   │   └── entity/
│       │   │   │   │       ├── AcademicEntities.kt        (7 155 B)
│       │   │   │   │       ├── CategoryEntity.kt          (567 B)
│       │   │   │   │       ├── CodeSnippetEntity.kt       (1 334 B)
│       │   │   │   │       ├── DocArticleEntity.kt        (1 034 B)
│       │   │   │   │       ├── ProgrammingLanguageEntity.kt (1 417 B)
│       │   │   │   │       ├── ScenarioEntity.kt          (918 B)
│       │   │   │   │       ├── Stage2Entities.kt          (1 680 B)
│       │   │   │   │       ├── Stage3Entities.kt          (6 556 B)
│       │   │   │   │       ├── Stage4AEntities.kt         (4 554 B)
│       │   │   │   │       ├── StudyLogEntity.kt          (2 068 B)
│       │   │   │   │       ├── WordEntity.kt              (2 102 B)
│       │   │   │   │       └── WorkPhraseEntity.kt        (595 B)
│       │   │   │   ├── datastore/
│       │   │   │   │   └── UserPreferencesDataStore.kt    (5 730 B)
│       │   │   │   ├── designsystem/
│       │   │   │   │   ├── CodeSyntaxHighlighter.kt       (7 345 B)
│       │   │   │   │   ├── Components.kt                  (48 062 B)
│       │   │   │   │   └── Theme.kt                       (17 034 B)
│       │   │   │   ├── navigation/
│       │   │   │   │   └── AppNavigation.kt               (19 461 B)
│       │   │   │   ├── network/
│       │   │   │   │   └── TechEnglishApiClient.kt        (34 097 B)
│       │   │   │   ├── notifications/
│       │   │   │   │   └── NotificationsEngine.kt         (6 363 B)
│       │   │   │   ├── security/
│       │   │   │   │   └── AndroidKeystoreTokenStore.kt   (6 866 B)
│       │   │   │   └── sync/
│       │   │   │       └── CloudSyncEngine.kt             (10 517 B)
│       │   │   ├── data/
│       │   │   │   ├── mapper/
│       │   │   │   │   └── EntityMappers.kt               (15 218 B)
│       │   │   │   └── repository/
│       │   │   │       └── RepositoryImpls.kt             (19 317 B)
│       │   │   ├── di/
│       │   │   │   └── AppModules.kt                      (6 314 B)
│       │   │   └── feature/
│       │   │       ├── academic/
│       │   │       │   ├── AcademicScreens.kt             (62 724 B)
│       │   │       │   └── AcademicViewModels.kt          (19 366 B)
│       │   │       ├── documentation/
│       │   │       │   ├── DocumentationScreens.kt        (23 003 B)
│       │   │       │   └── DocumentationViewModels.kt     (6 772 B)
│       │   │       ├── home/
│       │   │       │   ├── HomeScreen.kt                  (29 168 B)
│       │   │       │   └── HomeViewModel.kt               (5 740 B)
│       │   │       ├── listening/
│       │   │       │   ├── ListeningScreen.kt             (16 567 B)
│       │   │       │   └── ListeningViewModel.kt          (5 273 B)
│       │   │       ├── profile/
│       │   │       │   ├── ProfileScreen.kt               (51 404 B)
│       │   │       │   └── ProfileViewModel.kt            (5 785 B)
│       │   │       ├── programming/
│       │   │       │   ├── ProgrammingScreens.kt          (45 672 B)
│       │   │       │   └── ProgrammingViewModels.kt       (10 415 B)
│       │   │       ├── scenarios/
│       │   │       │   ├── ScenariosScreens.kt            (44 069 B)
│       │   │       │   └── ScenariosViewModels.kt         (9 991 B)
│       │   │       ├── stage2/
│       │   │       │   ├── Stage2Screens.kt               (59 044 B)
│       │   │       │   └── Stage2ViewModels.kt            (28 229 B)
│       │   │       ├── stage3/
│       │   │       │   ├── Stage3Screens.kt               (68 724 B)
│       │   │       │   └── Stage3ViewModels.kt            (30 161 B)
│       │   │       ├── stage4a/
│       │   │       │   ├── Stage4AScreens.kt              (31 072 B)
│       │   │       │   └── Stage4AViewModels.kt           (32 251 B)
│       │   │       ├── stage4b/
│       │   │       │   ├── Stage4BScreens.kt              (41 767 B)
│       │   │       │   └── Stage4BViewModels.kt           (22 200 B)
│       │   │       ├── stage4c/
│       │   │       │   ├── Stage4CScreens.kt              (36 347 B)
│       │   │       │   └── Stage4CViewModels.kt           (27 609 B)
│       │   │       ├── statistics/
│       │   │       │   ├── StatisticsScreen.kt            (15 335 B)
│       │   │       │   └── StatisticsViewModel.kt         (1 409 B)
│       │   │       ├── vocabulary/
│       │   │       │   ├── FlashcardsScreen.kt            (22 921 B)
│       │   │       │   ├── FlashcardsViewModel.kt         (5 355 B)
│       │   │       │   ├── VocabularyScreen.kt            (29 736 B)
│       │   │       │   └── VocabularyViewModel.kt         (7 033 B)
│       │   │       └── widget/
│       │   │           └── TechEnglishGlanceWidget.kt     (7 180 B)
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_background.xml         (393 B)
│       │       │   └── ic_launcher_foreground.xml         (646 B)
│       │       ├── font/
│       │       │   ├── inter_bold.ttf                     (715 204 B)
│       │       │   ├── inter_medium.ttf                   (694 512 B)
│       │       │   ├── inter_regular.ttf                  (680 240 B)
│       │       │   ├── inter_semibold.ttf                 (710 040 B)
│       │       │   ├── jetbrains_mono_bold.ttf            (277 828 B)
│       │       │   ├── jetbrains_mono_medium.ttf          (273 860 B)
│       │       │   ├── jetbrains_mono_regular.ttf         (273 900 B)
│       │       │   └── jetbrains_mono_semibold.ttf        (277 092 B)
│       │       ├── mipmap-anydpi-v26/
│       │       │   ├── ic_launcher.xml                    (273 B)
│       │       │   └── ic_launcher_round.xml              (273 B)
│       │       ├── values/
│       │       │   ├── strings.xml                        (457 B)
│       │       │   └── themes.xml                         (473 B)
│       │       └── xml/
│       │           ├── network_security_config.xml        (426 B)
│       │           └── tech_english_widget_info.xml       (416 B)
│       └── test/kotlin/com/techenglish/app/
│           ├── database/
│           │   └── TechEnglishDatabaseTest.kt             (64 583 B)
│           ├── notifications/
│           │   └── ReviewReminderWorkerTest.kt            (6 807 B)
│           └── ui/
│               └── TechEnglishUiTest.kt                   (39 554 B)
├── backend/
│   ├── .DS_Store                                          [untracked, 6 148 B]
│   ├── Dockerfile                                         (356 B)
│   ├── docker-compose.yml                                 (1 839 B)
│   ├── requirements.txt                                   (157 B)
│   ├── test_stage3.db                                     [untracked, 438 272 B]
│   ├── app/
│   │   ├── __init__.py                                    (43 B)
│   │   ├── ai_proxy.py                                    (7 725 B)
│   │   ├── anti_cheat.py                                  (6 622 B)
│   │   ├── config.py                                      (675 B)
│   │   ├── main.py                                        (93 396 B)
│   │   ├── models.py                                      (16 440 B)
│   │   ├── security.py                                    (2 611 B)
│   │   └── voice_storage.py                               (2 013 B)
│   └── tests/
│       ├── test_api_stage3.py                             (11 665 B)
│       ├── test_api_stage4a.py                            (7 344 B)
│       ├── test_api_stage4b.py                            (9 164 B)
│       └── test_api_stage4c.py                            (7 105 B)
├── docs/
│   ├── ARCHITECTURE.md                                    (20 542 B)
│   ├── DECISIONS.md                                       (12 704 B)
│   ├── FINAL_REPORT.md                                    (10 579 B)
│   ├── IMPLEMENTATION_PLAN.md                             (13 139 B)
│   ├── OWNER_ACTIONS.md                                   (9 724 B)
│   ├── REQUIREMENTS.md                                    (28 486 B)
│   ├── REQUIREMENTS_TRACEABILITY.md                       (17 727 B)
│   ├── STAGE_1_REPORT.md                                  (16 927 B)
│   ├── STAGE_2_REPORT.md                                  (14 092 B)
│   ├── STAGE_3_REPORT.md                                  (12 931 B)
│   ├── STAGE_4A_REPORT.md                                 (7 170 B)
│   ├── STAGE_4B_REPORT.md                                 (6 872 B)
│   ├── STAGE_4C_REPORT.md                                 (6 911 B)
│   ├── UPGRADE_AUDIT.md                                   (9 517 B)
│   ├── UPGRADE_PLAN.md                                    (6 682 B)
│   ├── VOCABULARY_QUALITY_REPORT.md                       (8 649 B)
│   └── techenglish_v3_2_final.md                          (57 758 B)
├── domain/
│   ├── .DS_Store                                          [untracked, 6 148 B]
│   ├── build.gradle.kts                                   (954 B)
│   └── src/
│       ├── main/kotlin/com/techenglish/domain/
│       │   ├── algorithm/
│       │   │   └── Sm2Engine.kt                           (3 352 B)
│       │   ├── model/
│       │   │   ├── AcademicModels.kt                      (5 518 B)
│       │   │   ├── DocModels.kt                           (815 B)
│       │   │   ├── ProgrammingModels.kt                   (1 423 B)
│       │   │   ├── ScenarioAndListeningModels.kt          (3 080 B)
│       │   │   ├── Stage2Models.kt                        (4 727 B)
│       │   │   ├── Stage3Models.kt                        (7 150 B)
│       │   │   ├── Stage4AModels.kt                       (2 807 B)
│       │   │   ├── Stage4BModels.kt                       (3 985 B)
│       │   │   ├── Stage4CModels.kt                       (3 964 B)
│       │   │   ├── StatsAndSettingsModels.kt              (7 423 B)
│       │   │   └── WordModels.kt                          (1 327 B)
│       │   ├── repository/
│       │   │   └── Repositories.kt                        (7 067 B)
│       │   └── usecase/
│       │       ├── AcademicVocabularyUseCases.kt          (7 472 B)
│       │       ├── DocumentationUseCases.kt               (12 642 B)
│       │       ├── ProgrammingUseCases.kt                 (4 708 B)
│       │       ├── ScenarioAndListeningUseCases.kt        (8 964 B)
│       │       ├── Stage2OfflineFeaturesUseCases.kt       (54 056 B)
│       │       ├── Stage3SyncAndAntiCheatUseCases.kt      (14 659 B)
│       │       ├── Stage4ACommunityUseCases.kt            (8 379 B)
│       │       ├── Stage4BUseCases.kt                     (6 848 B)
│       │       ├── Stage4CRealtimeUseCases.kt             (7 130 B)
│       │       ├── StatisticsAndSettingsUseCases.kt       (11 217 B)
│       │       └── VocabularyUseCases.kt                  (9 064 B)
│       └── test/kotlin/com/techenglish/domain/
│           ├── FakeRepositories.kt                        (12 124 B)
│           ├── algorithm/
│           │   └── Sm2EngineTest.kt                       (5 636 B)
│           └── usecase/
│               ├── AcademicVocabularyUseCasesTest.kt      (15 787 B)
│               ├── DocumentationUseCasesTest.kt           (4 418 B)
│               ├── ProgrammingAndComparisonUseCasesTest.kt (5 564 B)
│               ├── ScenarioAndListeningUseCasesTest.kt    (5 375 B)
│               ├── Stage2OfflineFeaturesUseCasesTest.kt   (20 309 B)
│               ├── Stage3SyncAndAntiCheatUseCasesTest.kt  (14 525 B)
│               ├── Stage4ACommunityUseCasesTest.kt        (6 736 B)
│               ├── Stage4BUseCasesTest.kt                 (6 108 B)
│               ├── Stage4CRealtimeUseCasesTest.kt         (4 616 B)
│               ├── StatisticsAndSettingsUseCasesTest.kt   (10 476 B)
│               └── VocabularyUseCasesTest.kt              (6 610 B)
├── gradle/
│   ├── libs.versions.toml                                 (6 655 B)
│   └── wrapper/
│       ├── gradle-wrapper.jar                             (68 243 B)
│       └── gradle-wrapper.properties                      (253 B)
├── scripts/
│   ├── build_seed_db.py                                   (14 194 B)
│   └── seed_data/
│       ├── __init__.py                                    (60 B)
│       ├── docs_data.py                                   (61 935 B)
│       ├── docs_v3_expansion.py                           (183 708 B)
│       ├── languages_data.py                              (28 945 B)
│       ├── scenarios_data.py                              (71 978 B)
│       ├── scenarios_v3_expansion.py                      (244 326 B)
│       ├── snippets_part1.py                              (136 184 B)
│       ├── snippets_part2.py                              (90 043 B)
│       ├── snippets_v3_expansion.py                       (97 318 B)
│       └── vocabulary_data.py                             (95 384 B)
└── tools/
    └── vocabulary_import/
        ├── corpus_builder.py                              (51 739 B)
        ├── lexicon_domains_a_e.py                         (153 203 B)
        ├── lexicon_domains_f_j.py                         (72 082 B)
        ├── lexicon_new_domains_v3.py                      (58 752 B)
        ├── licensed_ipa_lexicon.py                        (97 130 B)
        ├── sources_and_taxonomy.py                        (23 011 B)
        ├── vocabulary_importer.py                         (11 876 B)
        ├── vocabulary_schema.json                         (3 028 B)
        ├── vocabulary_statistics.py                       (6 705 B)
        ├── vocabulary_validator.py                        (7 272 B)
        └── output/
            ├── sample_academic_vocabulary_500.csv         (1 002 970 B)
            └── tajik_translations_pending_review.json     (1 356 341 B)
```

---

### 1.3. Ключевые точки входа

| Компонент | Точный путь | Детали реализации |
|---|---|---|
| `Application` | `app/src/main/kotlin/com/techenglish/app/TechEnglishApplication.kt` | `@HiltAndroidApp`, реализует `Configuration.Provider` для WorkManager с инъекцией `HiltWorkerFactory` (стандартный `WorkManagerInitializer` отключён в манифесте) |
| `MainActivity` | `app/src/main/kotlin/com/techenglish/app/MainActivity.kt` | `@AndroidEntryPoint`, вызывает `installSplashScreen()`, `enableEdgeToEdge()`, в `Dispatchers.IO` настраивает ежедневный `NotificationScheduler` и `CloudSyncEngine.scheduleBackgroundSync()`, проверяет 16+ Age Gate (`AgeGate16PlusScreen`) и монтирует `TechEnglishNavShell` |
| Навигация | `app/src/main/kotlin/com/techenglish/app/core/navigation/AppNavigation.kt` | `Routes` (**34** маршрута: 5 корневых вкладок `BottomNavTab` — `HOME`, `VOCABULARY`, `LANGUAGES`, `PRACTICE`, `PROFILE` и 29 дочерних/функциональных экранов), `NavHost` стартует с `Routes.HOME` |
| DI-модули (Hilt) | `app/src/main/kotlin/com/techenglish/app/di/AppModules.kt` | 3 Hilt-модуля в `SingletonComponent`: `DatabaseModule` (`TechEnglishDatabase` через `.createFromAsset(...)` + 17 `@Provides` DAO), `DomainModule` (`Sm2Engine`), `RepositoryModule` (9 `@Binds` реализаций доменных репозиториев) |
| DI EntryPoint виджета | `app/src/main/kotlin/com/techenglish/app/feature/widget/TechEnglishGlanceWidget.kt` | `@EntryPoint @InstallIn(SingletonComponent::class) interface WidgetEntryPoint`, класс `TechEnglishGlanceWidget` и `TechEnglishGlanceWidgetReceiver` |
| Точка входа бэкенда | `backend/app/main.py` | FastAPI-приложение `app = FastAPI(title="TechEnglish v3.0 API", version="3.0.0")`, `@app.on_event("startup")` (`init_db()`), **59** REST-маршрутов + **2** WebSocket-маршрута (`/ws/rooms/{room_slug}`, `/ws/pvp/{match_id}`) |
| Конфиги Docker | `backend/Dockerfile`, `backend/docker-compose.yml` | Образ `python:3.12-slim` (пользователь `appuser`, uid `10001`, `CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]`); стек из 4 сервисов: `postgres`, `redis`, `minio`, `api` |

---

### 1.4. Расположение ресурсов, манифеста и предзаполненных ассетов

| Категория | Путь | Размер на диске | Назначение / содержимое |
|---|---|---:|---|
| Манифест | `app/src/main/AndroidManifest.xml` | 3 046 Б | Разрешения, `<queries>` для TTS/SpeechRecognizer, `MainActivity`, `TechEnglishGlanceWidgetReceiver`, удаление `WorkManagerInitializer` |
| Шрифты Inter (4 шт.) | `app/src/main/res/font/inter_regular.ttf` | 680 240 Б | Основной UI-шрифт (Regular) |
| | `app/src/main/res/font/inter_medium.ttf` | 694 512 Б | Основной UI-шрифт (Medium) |
| | `app/src/main/res/font/inter_semibold.ttf` | 710 040 Б | Основной UI-шрифт (SemiBold) |
| | `app/src/main/res/font/inter_bold.ttf` | 715 204 Б | Основной UI-шрифт (Bold) |
| Шрифты JetBrains Mono (4 шт.) | `app/src/main/res/font/jetbrains_mono_regular.ttf` | 273 900 Б | Моноширинный шрифт для кода, логов и IPA (Regular) |
| | `app/src/main/res/font/jetbrains_mono_medium.ttf` | 273 860 Б | Моноширинный шрифт (Medium) |
| | `app/src/main/res/font/jetbrains_mono_semibold.ttf` | 277 092 Б | Моноширинный шрифт (SemiBold) |
| | `app/src/main/res/font/jetbrains_mono_bold.ttf` | 277 828 Б | Моноширинный шрифт (Bold) |
| Итого шрифты (`res/font/`) | 8 `.ttf`-файлов | **3 902 676 Б** (**3.72 MiB**) | Покрытие латиницы, кириллицы, IPA и таджикских символов |
| XML сетевой безопасности | `app/src/main/res/xml/network_security_config.xml` | 426 Б | Запрет cleartext (`cleartextTrafficPermitted="false"`) и `<debug-overrides>` |
| XML виджета рабочего стола | `app/src/main/res/xml/tech_english_widget_info.xml` | 416 Б | `minWidth="220dp"`, `minHeight="110dp"`, `4×2` клетки, `updatePeriodMillis="14400000"` (4 ч) |
| Строки и темы | `app/src/main/res/values/strings.xml` | 457 Б | `app_name`, `app_subtitle`, `notification_channel_name`, `notification_channel_desc` |
| | `app/src/main/res/values/themes.xml` | 473 Б | `Theme.TechEnglish` и `Theme.TechEnglish.Starting` (фон `#0F172A`) |
| Графика и иконка запуска | `app/src/main/res/drawable/ic_launcher_background.xml` | 393 Б | Фон адаптивной иконки |
| | `app/src/main/res/drawable/ic_launcher_foreground.xml` | 646 Б | Передний слой иконки и splash-экран |
| | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 273 Б | Адаптивная иконка |
| | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 273 Б | Круглая адаптивная иконка |
| Итого каталог `app/src/main/res/` | 16 файлов | **3 906 033 Б** (**3.73 MiB**) | |
| Предзаполненная БД (ассет) | `app/src/main/assets/databases/tech_english.db` | **90 562 560 Б** (**86.37 MiB** / **90.56 MB**) | Офлайн-база Room v5 (`PRAGMA page_size = 4096`, `page_count = 22 110`) |

---

## 2. Подробный отчёт по локальной базе данных (Room / SQLite)

### 2.1. Общие характеристики предзаполненной БД

- **Путь в репозитории:** `app/src/main/assets/databases/tech_english.db` (`TechEnglishDatabase.SEED_ASSET_PATH = "databases/tech_english.db"`, рабочее имя на устройстве `DATABASE_NAME = "tech_english.db"`).
- **Точный размер:** `90 562 560` байт (`86.37 MiB` по основанию 1024² / `90.56 MB` по основанию 1000²).
- **Текущая версия схемы Room:** **`5`** (`exportSchema = true`).
- **Метаданные SQLite (read-only PRAGMA):** `user_version = 5`, `journal_mode = delete`, `page_size = 4096`, `page_count = 22 110`, `encoding = UTF-8`, `foreign_keys = 0` (по умолчанию при открытии через `sqlite3`, в Room контролируется фреймворком).
- **Идентификатор схемы (`room_master_table`):** `(42, 'cd74f367098fec999ef10e8f6c1c737f')` — совпадает с `identityHash` в `5.json`.

---

### 2.2. История версий, экспортированных JSON-схем и миграций

Все экспортированные KSP-схемы расположены в `app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/`:

| Версия | Файл схемы | Размер | `identityHash` | Кол-во таблиц | Добавленные / изменённые сущности |
|---:|---|---:|---|---:|---|
| 1 | `1.json` | 25 441 Б | `dffd197c4cbd03957d233033990ac7ce` | 8 | Базовое ядро: `categories`, `programming_languages`, `code_snippets`, `words`, `doc_articles`, `scenarios`, `work_phrases`, `study_logs` |
| 2 | `2.json` | 45 016 Б | `8170a848655f927ea61bfbb1ab7cf233` | 13 | Академический словарь (+5 таблиц): `subcategories`, `word_sources`, `academic_terms`, `word_senses`, `word_relations` |
| 3 | `3.json` | 50 777 Б | `97c1e06c906948757fc30288d8e4788f` | 15 | Офлайн-тренажёры Stage 2 (+2 таблицы, +5 столбцов): `custom_imported_docs`, `time_attack_records`, поля источников и лицензий |
| 4 | `4.json` | 68 508 Б | `255ecb23e292f0db500c173447b671ae` | 23 | Stage 3 Sync/Social/OTA (+8 таблиц): `sync_queue`, `user_profile_cache`, `local_account_link`, `leaderboard_cache`, `friends_cache`, `activity_feed_cache`, `badges_cache`, `ota_content_meta` |
| 5 | `5.json` | 80 010 Б | `cd74f367098fec999ef10e8f6c1c737f` | 27 | Stage 4A Community (+4 таблицы): `comments_cache`, `community_decks`, `community_deck_terms`, `room_messages_cache` |

Реализованные миграции в `app/src/main/kotlin/com/techenglish/app/core/database/TechEnglishDatabase.kt` (`ALL_MIGRATIONS`):

1. **`MIGRATION_1_2` (`1 -> 2`):**
   - Создаёт 5 таблиц: `subcategories`, `word_sources`, `academic_terms`, `word_senses`, `word_relations`.
   - Создаёт 14 индексов: `idx_subcategories_slug` (UNIQUE), `idx_subcategories_category`, `idx_academic_normalized_term` (UNIQUE), `idx_academic_category_id`, `idx_academic_subcategory_id`, `idx_academic_difficulty`, `idx_academic_cefr`, `idx_academic_next_review`, `idx_academic_source_id`, `idx_academic_domain`, `idx_academic_status`, `idx_academic_srs_due`, `idx_senses_term_id`, `idx_relations_term_id`.
   - Настраивает FK: `academic_terms.source_id -> word_sources(id) ON DELETE SET NULL`, `word_senses.term_id -> academic_terms(id) ON DELETE CASCADE`, `word_relations.term_id -> academic_terms(id) ON DELETE CASCADE`.
2. **`MIGRATION_2_3` (`2 -> 3`):**
   - Создаёт 2 таблицы: `custom_imported_docs` (индекс `idx_custom_docs_created`) и `time_attack_records` (индекс `idx_time_attack_score`).
   - Добавляет 5 столбцов с сохранением пользовательских данных через `ALTER TABLE`: `doc_articles.source_url` (`TEXT NOT NULL DEFAULT ''`), `doc_articles.license` (`TEXT NOT NULL DEFAULT 'CC-BY-4.0'`), `word_sources.license` (`TEXT NOT NULL DEFAULT 'Public Specification / Standard'`), `academic_terms.ipa_source_url` (`TEXT NOT NULL DEFAULT ''`), `academic_terms.ipa_license` (`TEXT NOT NULL DEFAULT ''`).
3. **`MIGRATION_3_4` (`3 -> 4`):**
   - Создаёт 8 таблиц Stage 3: `sync_queue`, `user_profile_cache`, `local_account_link`, `leaderboard_cache` (составной PK `(tab, user_id)`), `friends_cache`, `activity_feed_cache`, `badges_cache`, `ota_content_meta`.
   - Создаёт 3 индекса: `idx_sync_queue_status_created`, `idx_leaderboard_tab_rank`, `idx_activity_feed_created`.
4. **`MIGRATION_4_5` (`4 -> 5`):**
   - Создаёт 4 таблицы Stage 4A: `comments_cache`, `community_decks`, `community_deck_terms` (с FK на `community_decks(id) ON DELETE CASCADE`), `room_messages_cache`.
   - Создаёт 4 индекса: `idx_comments_target`, `idx_community_decks_rating`, `idx_deck_terms_deck_id`, `idx_room_messages_room_created`.

---

### 2.3. Полный каталог всех таблиц (Entity) Room v5

Все Entity расположены в каталоге `app/src/main/kotlin/com/techenglish/app/core/database/entity/`. Ниже приведены точные определения согласно `5.json` и Kotlin-классам.

#### 1. `categories` (`CategoryEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `index_categories_name` (`name`, **UNIQUE**)
- **Столбцы (4):**
  - `id`: `INTEGER NOT NULL`
  - `name`: `TEXT NOT NULL`
  - `icon`: `TEXT NOT NULL`
  - `is_system`: `INTEGER NOT NULL DEFAULT 0`

#### 2. `programming_languages` (`ProgrammingLanguageEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `index_programming_languages_slug` (`slug`, **UNIQUE**), `index_programming_languages_name` (`name`, **UNIQUE**)
- **Столбцы (16):**
  - `id`: `INTEGER NOT NULL`
  - `slug`: `TEXT NOT NULL`
  - `name`: `TEXT NOT NULL`
  - `description_en`: `TEXT NOT NULL`
  - `description_ru`: `TEXT NOT NULL`
  - `history_en`: `TEXT NOT NULL`
  - `history_ru`: `TEXT NOT NULL`
  - `paradigms`: `TEXT NOT NULL`
  - `domains`: `TEXT NOT NULL`
  - `frameworks`: `TEXT NOT NULL`
  - `typing_system`: `TEXT NOT NULL`
  - `memory_management`: `TEXT NOT NULL`
  - `difficulty`: `TEXT NOT NULL`
  - `year_created`: `INTEGER NOT NULL`
  - `creator`: `TEXT NOT NULL`
  - `accent_color_hex`: `TEXT NOT NULL`

#### 3. `code_snippets` (`CodeSnippetEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** `language_id -> programming_languages(id)` (`ON DELETE CASCADE`, `ON UPDATE NO ACTION`)
- **Индексы:** `idx_snippets_lang_task` (`language_id`, `task_key`, **UNIQUE**)
- **Столбцы (10):**
  - `id`: `INTEGER NOT NULL`
  - `language_id`: `INTEGER NOT NULL`
  - `task_key`: `TEXT NOT NULL`
  - `task_title_en`: `TEXT NOT NULL`
  - `task_title_ru`: `TEXT NOT NULL`
  - `code`: `TEXT NOT NULL`
  - `explanation_en`: `TEXT NOT NULL`
  - `explanation_ru`: `TEXT NOT NULL`
  - `line_explanations_json`: `TEXT NOT NULL`
  - `keywords_json`: `TEXT NOT NULL`

#### 4. `words` (`WordEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:**
  - `category_id -> categories(id)` (`ON DELETE CASCADE`, `ON UPDATE NO ACTION`)
  - `language_id -> programming_languages(id)` (`ON DELETE SET NULL`, `ON UPDATE NO ACTION`)
- **Индексы:** `idx_words_next_review` (`next_review_at`), `idx_words_category` (`category_id`), `idx_words_language` (`language_id`), `idx_words_term_lang` (`term`, `language_id`)
- **Столбцы (14):**
  - `id`: `INTEGER NOT NULL`
  - `category_id`: `INTEGER NOT NULL`
  - `language_id`: `INTEGER NULL`
  - `term`: `TEXT NOT NULL`
  - `transcription`: `TEXT NOT NULL`
  - `translation`: `TEXT NOT NULL`
  - `example`: `TEXT NOT NULL`
  - `explanation_ru`: `TEXT NOT NULL`
  - `is_favorite`: `INTEGER NOT NULL DEFAULT 0`
  - `is_custom`: `INTEGER NOT NULL DEFAULT 0`
  - `repetitions`: `INTEGER NOT NULL DEFAULT 0`
  - `ease_factor`: `REAL NOT NULL DEFAULT 2.5`
  - `interval_days`: `INTEGER NOT NULL DEFAULT 0`
  - `next_review_at`: `INTEGER NOT NULL DEFAULT 0`

#### 5. `doc_articles` (`DocArticleEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** нет
- **Столбцы (11):**
  - `id`: `INTEGER NOT NULL`
  - `title`: `TEXT NOT NULL`
  - `tech_tag`: `TEXT NOT NULL`
  - `content`: `TEXT NOT NULL`
  - `explanation_ru`: `TEXT NOT NULL`
  - `level`: `TEXT NOT NULL`
  - `reading_progress`: `REAL NOT NULL DEFAULT 0.0`
  - `is_completed`: `INTEGER NOT NULL DEFAULT 0`
  - `glossary_json`: `TEXT NOT NULL`
  - `source_url`: `TEXT NOT NULL DEFAULT ''`
  - `license`: `TEXT NOT NULL DEFAULT 'CC-BY-4.0'`

#### 6. `scenarios` (`ScenarioEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** нет
- **Столбцы (11):**
  - `id`: `INTEGER NOT NULL`
  - `type`: `TEXT NOT NULL`
  - `exercise_type`: `TEXT NOT NULL`
  - `title`: `TEXT NOT NULL`
  - `prompt`: `TEXT NOT NULL`
  - `prompt_ru`: `TEXT NOT NULL`
  - `solution`: `TEXT NOT NULL`
  - `options_json`: `TEXT NOT NULL`
  - `explanation_ru`: `TEXT NOT NULL`
  - `useful_expressions_json`: `TEXT NOT NULL`
  - `difficulty`: `TEXT NOT NULL`

#### 7. `work_phrases` (`WorkPhraseEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** нет
- **Столбцы (6):**
  - `id`: `INTEGER NOT NULL`
  - `category`: `TEXT NOT NULL`
  - `phrase_en`: `TEXT NOT NULL`
  - `translation_ru`: `TEXT NOT NULL`
  - `context_ru`: `TEXT NOT NULL`
  - `difficulty`: `TEXT NOT NULL`

#### 8. `study_logs` (`StudyLogEntity.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:**
  - `word_id -> words(id)` (`ON DELETE SET NULL`, `ON UPDATE NO ACTION`)
  - `scenario_id -> scenarios(id)` (`ON DELETE SET NULL`, `ON UPDATE NO ACTION`)
  - `article_id -> doc_articles(id)` (`ON DELETE SET NULL`, `ON UPDATE NO ACTION`)
  - `snippet_id -> code_snippets(id)` (`ON DELETE SET NULL`, `ON UPDATE NO ACTION`)
- **Индексы:** `idx_logs_timestamp` (`timestamp`), `idx_logs_word` (`word_id`), `idx_logs_scenario` (`scenario_id`), `idx_logs_article` (`article_id`), `idx_logs_snippet` (`snippet_id`)
- **Столбцы (9):**
  - `id`: `INTEGER NOT NULL`
  - `word_id`: `INTEGER NULL`
  - `scenario_id`: `INTEGER NULL`
  - `article_id`: `INTEGER NULL`
  - `snippet_id`: `INTEGER NULL`
  - `action_type`: `TEXT NOT NULL`
  - `is_correct`: `INTEGER NOT NULL`
  - `xp_earned`: `INTEGER NOT NULL DEFAULT 10`
  - `timestamp`: `INTEGER NOT NULL`

#### 9. `subcategories` (`AcademicEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_subcategories_slug` (`slug`, **UNIQUE**), `idx_subcategories_category` (`category_id`)
- **Столбцы (5):**
  - `id`: `INTEGER NOT NULL`
  - `category_id`: `INTEGER NOT NULL`
  - `name`: `TEXT NOT NULL`
  - `slug`: `TEXT NOT NULL`
  - `domain`: `TEXT NOT NULL`

#### 10. `word_sources` (`AcademicEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** нет
- **Столбцы (6):**
  - `id`: `INTEGER NOT NULL`
  - `title`: `TEXT NOT NULL`
  - `url`: `TEXT NOT NULL`
  - `source_type`: `TEXT NOT NULL`
  - `publisher`: `TEXT NOT NULL`
  - `license`: `TEXT NOT NULL DEFAULT 'Public Specification / Standard'`

#### 11. `academic_terms` (`AcademicEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** `source_id -> word_sources(id)` (`ON DELETE SET NULL`, `ON UPDATE NO ACTION`)
- **Индексы (10):**
  - `idx_academic_normalized_term` (`normalized_term`, **UNIQUE**)
  - `idx_academic_category_id` (`category_id`)
  - `idx_academic_subcategory_id` (`subcategory_id`)
  - `idx_academic_difficulty` (`difficulty_level`)
  - `idx_academic_cefr` (`cefr_level`)
  - `idx_academic_next_review` (`next_review_at`)
  - `idx_academic_source_id` (`source_id`)
  - `idx_academic_domain` (`domain`)
  - `idx_academic_status` (`verification_status`)
  - `idx_academic_srs_due` (`is_in_srs`, `next_review_at`)
- **Столбцы (38):**
  - `id`: `INTEGER NOT NULL`
  - `term`: `TEXT NOT NULL`
  - `normalized_term`: `TEXT NOT NULL`
  - `phonetic_transcription`: `TEXT NOT NULL DEFAULT ''`
  - `part_of_speech`: `TEXT NOT NULL`
  - `translation_ru`: `TEXT NOT NULL`
  - `translation_tg`: `TEXT NOT NULL DEFAULT ''`
  - `definition_en`: `TEXT NOT NULL`
  - `definition_ru`: `TEXT NOT NULL`
  - `academic_explanation_en`: `TEXT NOT NULL`
  - `academic_explanation_ru`: `TEXT NOT NULL`
  - `practical_explanation`: `TEXT NOT NULL`
  - `example_en`: `TEXT NOT NULL`
  - `example_ru`: `TEXT NOT NULL`
  - `major_category`: `TEXT NOT NULL`
  - `category_id`: `INTEGER NOT NULL`
  - `subcategory_id`: `INTEGER NOT NULL`
  - `domain`: `TEXT NOT NULL`
  - `subdomain`: `TEXT NOT NULL`
  - `difficulty_level`: `TEXT NOT NULL`
  - `cefr_level`: `TEXT NOT NULL`
  - `synonyms`: `TEXT NOT NULL DEFAULT ''`
  - `related_terms`: `TEXT NOT NULL DEFAULT ''`
  - `abbreviations`: `TEXT NOT NULL DEFAULT ''`
  - `source_reference`: `TEXT NOT NULL DEFAULT ''`
  - `source_id`: `INTEGER NULL`
  - `verification_status`: `TEXT NOT NULL DEFAULT 'verified'`
  - `is_favorite`: `INTEGER NOT NULL DEFAULT 0`
  - `is_in_srs`: `INTEGER NOT NULL DEFAULT 0`
  - `repetitions`: `INTEGER NOT NULL DEFAULT 0`
  - `ease_factor`: `REAL NOT NULL DEFAULT 2.5`
  - `interval_days`: `INTEGER NOT NULL DEFAULT 0`
  - `next_review_at`: `INTEGER NOT NULL DEFAULT 0`
  - `last_studied_at`: `INTEGER NOT NULL DEFAULT 0`
  - `created_at`: `INTEGER NOT NULL DEFAULT 0`
  - `updated_at`: `INTEGER NOT NULL DEFAULT 0`
  - `ipa_source_url`: `TEXT NOT NULL DEFAULT ''`
  - `ipa_license`: `TEXT NOT NULL DEFAULT ''`

#### 12. `word_senses` (`AcademicEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** `term_id -> academic_terms(id)` (`ON DELETE CASCADE`, `ON UPDATE NO ACTION`)
- **Индексы:** `idx_senses_term_id` (`term_id`)
- **Столбцы (9):**
  - `id`: `INTEGER NOT NULL`, `term_id`: `INTEGER NOT NULL`, `sense_number`: `INTEGER NOT NULL`, `sense_label`: `TEXT NOT NULL`, `domain`: `TEXT NOT NULL`, `definition_en`: `TEXT NOT NULL`, `definition_ru`: `TEXT NOT NULL`, `example_en`: `TEXT NOT NULL`, `example_ru`: `TEXT NOT NULL`

#### 13. `word_relations` (`AcademicEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** `term_id -> academic_terms(id)` (`ON DELETE CASCADE`, `ON UPDATE NO ACTION`)
- **Индексы:** `idx_relations_term_id` (`term_id`)
- **Столбцы (4):**
  - `id`: `INTEGER NOT NULL`, `term_id`: `INTEGER NOT NULL`, `related_term`: `TEXT NOT NULL`, `relation_type`: `TEXT NOT NULL`

#### 14. `custom_imported_docs` (`Stage2Entities.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** нет
- **Индексы:** `idx_custom_docs_created` (`created_at`)
- **Столбцы (7):**
  - `id`: `INTEGER NOT NULL`, `title`: `TEXT NOT NULL`, `source_file_name`: `TEXT NOT NULL DEFAULT ''`, `content`: `TEXT NOT NULL`, `detected_terms_json`: `TEXT NOT NULL DEFAULT '[]'`, `detected_terms_count`: `INTEGER NOT NULL DEFAULT 0`, `created_at`: `INTEGER NOT NULL DEFAULT 0`

#### 15. `time_attack_records` (`Stage2Entities.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** нет
- **Индексы:** `idx_time_attack_score` (`score`)
- **Столбцы (8):**
  - `id`: `INTEGER NOT NULL`, `mode`: `TEXT NOT NULL`, `score`: `INTEGER NOT NULL`, `correct_count`: `INTEGER NOT NULL`, `total_answered`: `INTEGER NOT NULL`, `xp_earned`: `INTEGER NOT NULL`, `duration_seconds`: `INTEGER NOT NULL DEFAULT 60`, `achieved_at`: `INTEGER NOT NULL`

#### 16. `sync_queue` (`Stage3Entities.kt`)
- **Primary Key:** `event_id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_sync_queue_status_created` (`sync_status`, `created_at`)
- **Столбцы (7):**
  - `event_id`: `TEXT NOT NULL`, `event_type`: `TEXT NOT NULL`, `payload_json`: `TEXT NOT NULL DEFAULT '{}'`, `created_at`: `INTEGER NOT NULL`, `retry_count`: `INTEGER NOT NULL DEFAULT 0`, `sync_status`: `TEXT NOT NULL DEFAULT 'PENDING'`, `last_error`: `TEXT NOT NULL DEFAULT ''`

#### 17. `user_profile_cache` (`Stage3Entities.kt`)
- **Primary Key:** `user_id` (`autoGenerate = false`)
- **Foreign Keys / Индексы:** нет
- **Столбцы (13):**
  - `user_id`: `TEXT NOT NULL`, `username`: `TEXT NOT NULL`, `display_name`: `TEXT NOT NULL`, `avatar_key`: `TEXT NOT NULL DEFAULT 'preset_terminal'`, `status_text`: `TEXT NOT NULL DEFAULT ''`, `tech_stack_csv`: `TEXT NOT NULL DEFAULT 'Kotlin,Python,SQL'`, `github_url`: `TEXT NOT NULL DEFAULT ''`, `telegram_handle`: `TEXT NOT NULL DEFAULT ''`, `rank_title`: `TEXT NOT NULL DEFAULT 'Intern Developer'`, `total_xp`: `INTEGER NOT NULL DEFAULT 0`, `streak_days`: `INTEGER NOT NULL DEFAULT 0`, `league`: `TEXT NOT NULL DEFAULT 'BRONZE'`, `updated_at`: `INTEGER NOT NULL DEFAULT 0`

#### 18. `local_account_link` (`Stage3Entities.kt`)
- **Primary Key:** `local_user_id` (`autoGenerate = false`)
- **Foreign Keys / Индексы:** нет
- **Столбцы (4):**
  - `local_user_id`: `TEXT NOT NULL`, `remote_user_id`: `TEXT NOT NULL`, `provider`: `TEXT NOT NULL DEFAULT 'EMAIL'`, `linked_at`: `INTEGER NOT NULL`

#### 19. `leaderboard_cache` (`Stage3Entities.kt`)
- **Primary Key:** составной `["tab", "user_id"]` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_leaderboard_tab_rank` (`tab`, `rank`)
- **Столбцы (12):**
  - `tab`: `TEXT NOT NULL`, `user_id`: `TEXT NOT NULL`, `rank`: `INTEGER NOT NULL`, `username`: `TEXT NOT NULL`, `avatar_key`: `TEXT NOT NULL DEFAULT 'preset_terminal'`, `rank_title`: `TEXT NOT NULL`, `league`: `TEXT NOT NULL`, `tech_stack`: `TEXT NOT NULL DEFAULT ''`, `weekly_xp`: `INTEGER NOT NULL DEFAULT 0`, `total_xp`: `INTEGER NOT NULL DEFAULT 0`, `is_current_user`: `INTEGER NOT NULL DEFAULT 0`, `cached_at`: `INTEGER NOT NULL DEFAULT 0`

#### 20. `friends_cache` (`Stage3Entities.kt`)
- **Primary Key:** `user_id` (`autoGenerate = false`)
- **Foreign Keys / Индексы:** нет
- **Столбцы (8):**
  - `user_id`: `TEXT NOT NULL`, `username`: `TEXT NOT NULL`, `avatar_key`: `TEXT NOT NULL DEFAULT 'preset_terminal'`, `rank_title`: `TEXT NOT NULL`, `streak_days`: `INTEGER NOT NULL DEFAULT 0`, `accepts_pings`: `INTEGER NOT NULL DEFAULT 1`, `last_pinged_at`: `INTEGER NOT NULL DEFAULT 0`, `cached_at`: `INTEGER NOT NULL DEFAULT 0`

#### 21. `activity_feed_cache` (`Stage3Entities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_activity_feed_created` (`created_at`)
- **Столбцы (7):**
  - `id`: `TEXT NOT NULL`, `actor_user_id`: `TEXT NOT NULL`, `actor_username`: `TEXT NOT NULL`, `summary_ru`: `TEXT NOT NULL`, `reaction_count`: `INTEGER NOT NULL DEFAULT 0`, `user_reacted`: `INTEGER NOT NULL DEFAULT 0`, `created_at`: `INTEGER NOT NULL`

#### 22. `badges_cache` (`Stage3Entities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys / Индексы:** нет
- **Столбцы (6):**
  - `id`: `TEXT NOT NULL`, `title_ru`: `TEXT NOT NULL`, `description_ru`: `TEXT NOT NULL DEFAULT ''`, `icon_key`: `TEXT NOT NULL DEFAULT 'star'`, `is_unlocked`: `INTEGER NOT NULL DEFAULT 0`, `unlocked_at`: `INTEGER NOT NULL DEFAULT 0`

#### 23. `ota_content_meta` (`Stage3Entities.kt`)
- **Primary Key:** `pack_id` (`autoGenerate = false`)
- **Foreign Keys / Индексы:** нет
- **Столбцы (6):**
  - `pack_id`: `TEXT NOT NULL`, `version`: `INTEGER NOT NULL`, `title_ru`: `TEXT NOT NULL`, `sha256_hex`: `TEXT NOT NULL`, `item_count`: `INTEGER NOT NULL DEFAULT 0`, `applied_at`: `INTEGER NOT NULL DEFAULT 0`

#### 24. `comments_cache` (`Stage4AEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_comments_target` (`target_type`, `target_id`, `created_at`)
- **Столбцы (12):**
  - `id`: `TEXT NOT NULL`, `target_type`: `TEXT NOT NULL`, `target_id`: `INTEGER NOT NULL`, `author_user_id`: `TEXT NOT NULL`, `author_username`: `TEXT NOT NULL`, `author_rank_title`: `TEXT NOT NULL DEFAULT 'Intern Developer'`, `body`: `TEXT NOT NULL`, `upvotes`: `INTEGER NOT NULL DEFAULT 0`, `downvotes`: `INTEGER NOT NULL DEFAULT 0`, `user_vote`: `INTEGER NOT NULL DEFAULT 0`, `is_hidden`: `INTEGER NOT NULL DEFAULT 0`, `created_at`: `INTEGER NOT NULL`

#### 25. `community_decks` (`Stage4AEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_community_decks_rating` (`rating`)
- **Столбцы (12):**
  - `id`: `TEXT NOT NULL`, `title`: `TEXT NOT NULL`, `description`: `TEXT NOT NULL DEFAULT ''`, `tags_csv`: `TEXT NOT NULL DEFAULT ''`, `author_user_id`: `TEXT NOT NULL`, `author_username`: `TEXT NOT NULL`, `rating`: `REAL NOT NULL DEFAULT 0.0`, `downloads_count`: `INTEGER NOT NULL DEFAULT 0`, `term_count`: `INTEGER NOT NULL DEFAULT 0`, `is_downloaded_locally`: `INTEGER NOT NULL DEFAULT 0`, `is_hidden`: `INTEGER NOT NULL DEFAULT 0`, `created_at`: `INTEGER NOT NULL`

#### 26. `community_deck_terms` (`Stage4AEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = true`)
- **Foreign Keys:** `deck_id -> community_decks(id)` (`ON DELETE CASCADE`, `ON UPDATE NO ACTION`)
- **Индексы:** `idx_deck_terms_deck_id` (`deck_id`)
- **Столбцы (7):**
  - `id`: `INTEGER NOT NULL`, `deck_id`: `TEXT NOT NULL`, `term`: `TEXT NOT NULL`, `transcription`: `TEXT NOT NULL DEFAULT ''`, `translation`: `TEXT NOT NULL`, `example`: `TEXT NOT NULL DEFAULT ''`, `explanation_ru`: `TEXT NOT NULL DEFAULT ''`

#### 27. `room_messages_cache` (`Stage4AEntities.kt`)
- **Primary Key:** `id` (`autoGenerate = false`)
- **Foreign Keys:** нет
- **Индексы:** `idx_room_messages_room_created` (`room_slug`, `created_at`)
- **Столбцы (8):**
  - `id`: `TEXT NOT NULL`, `room_slug`: `TEXT NOT NULL`, `author_user_id`: `TEXT NOT NULL`, `author_username`: `TEXT NOT NULL`, `author_rank_title`: `TEXT NOT NULL DEFAULT 'Intern Developer'`, `body`: `TEXT NOT NULL`, `is_hidden`: `INTEGER NOT NULL DEFAULT 0`, `created_at`: `INTEGER NOT NULL`

---

### 2.4. Фактическая статистика содержимого SQLite (read-only запросы)

#### 2.4.1. Количество строк в каждой таблице (`tech_english.db`)

В файле SQLite присутствуют **30** таблиц: 27 сущностей Room + 3 системные таблицы SQLite/Room (`room_master_table`, `sqlite_sequence`, `sqlite_stat1`).

| № | Таблица | Назначение | Кол-во строк |
|---:|---|---|---:|
| 1 | `academic_terms` | Верифицированный академический технический словарь | **20 658** |
| 2 | `word_relations` | Семантические связи (`RELATED`, `SYNONYM`) для академических терминов | **3 723** |
| 3 | `code_snippets` | Примеры кода на 19 языках программирования | **380** |
| 4 | `work_phrases` | Профессиональные рабочие фразы по 12 контекстам | **312** |
| 5 | `words` | Базовый словарь IT-терминов (карточки SM-2) | **255** |
| 6 | `doc_articles` | Технические статьи и диагностические логи | **166** |
| 7 | `scenarios` | Интерактивные рабочие сценарии | **156** |
| 8 | `subcategories` | Подкатегории академического словаря | **155** |
| 9 | `word_senses` | Значения многозначных терминов по доменам | **29** |
| 10 | `programming_languages` | Каталог языков программирования | **19** |
| 11 | `word_sources` | Реестр авторитетных источников и лицензий | **19** |
| 12 | `categories` | Категории базового словаря (вкл. системную «Мои слова») | **11** |
| 13 | `study_logs` | Лог учебной активности и начисления XP | **0** |
| 14 | `custom_imported_docs` | Импортированные пользователем документы (`Read Your Own Docs`) | **0** |
| 15 | `time_attack_records` | Локальные рекорды режима Time Attack | **0** |
| 16 | `sync_queue` | Очередь Outbox-синхронизации | **0** |
| 17 | `user_profile_cache` | Кэш профиля облачного аккаунта | **0** |
| 18 | `local_account_link` | Связь локального гостевого профиля с облачным аккаунтом | **0** |
| 19 | `leaderboard_cache` | Офлайн-кэш 4 вкладок таблицы лидеров | **0** |
| 20 | `friends_cache` | Офлайн-кэш друзей | **0** |
| 21 | `activity_feed_cache` | Офлайн-кэш ленты активности друзей | **0** |
| 22 | `badges_cache` | Офлайн-кэш достижений | **0** |
| 23 | `ota_content_meta` | Метаданные применённых data-only OTA-пакетов | **0** |
| 24 | `comments_cache` | Кэш комментариев и мнемоник | **0** |
| 25 | `community_decks` | Кэш колод сообщества | **0** |
| 26 | `community_deck_terms` | Карточки загруженных колод сообщества | **0** |
| 27 | `room_messages_cache` | Кэш сообщений тематических комнат | **0** |
| — | `room_master_table` *(системная)* | Хеш идентичности схемы Room | **1** |
| — | `sqlite_sequence` *(системная)* | Счётчики `AUTOINCREMENT` | **5** |
| — | `sqlite_stat1` *(системная)* | Статистика планировщика SQLite после `ANALYZE` | **27** |

#### 2.4.2. Основной словарь (`words`: 255 терминов, `categories`: 11)

- **Заполненность:** `transcription` (IPA) — **255 / 255 (100%)**, `translation` — **255 / 255 (100%)**, `is_custom = 1` — **0**, `is_favorite = 1` — **0**.
- **Привязка к `language_id`:** **47** терминов привязаны к языку (`SQL`: 26, `JavaScript`: 7, `Python`: 2, `C++`: 2, `Go`: 2, `Bash`: 2, `Rust`: 2, `C`: 1, `Kotlin`: 1, `Swift`: 1, `Dart`: 1), **208** без привязки к языку (`language_id IS NULL`).
- **Распределение по 11 категориям:**
  `Backend` (`dns`): 26, `Frontend` (`web`): 25, `Git` (`merge_type`): 25, `Algorithms` (`account_tree`): 25, `Soft Skills` (`groups`): 25, `DevOps` (`cloud_sync`): 25, `Databases` (`storage`): 26, `Programming` (`code`): 28, `Networking` (`lan`): 25, `Security` (`shield`): 25, `Мои слова` (`bookmark_added`, `is_system = 1`): 0.

#### 2.4.3. Академический словарь (`academic_terms`: 20 658 терминов)

- **Верификация и заполненность полей:**
  - `verification_status = 'verified'`: **20 658 (100.00%)**
  - `phonetic_transcription` (IPA): **8 048 (38.96%)** (12 610 пустых); у всех 8 048 заполнены `ipa_source_url` и `ipa_license` (`MIT / CC-BY-SA-3.0 (open-dict-data/ipa-dict & CMUdict)`)
  - `translation_ru`: **20 658 (100.00%)**
  - `translation_tg`: **19 (0.09%)** (остальные оставлены пустыми до нативной проверки; 2 050 кандидатов вынесены в `tajik_translations_pending_review.json`)
  - `definition_en`: **20 658 (100.00%)**
  - `definition_ru`: **20 658 (100.00%)**
  - `related_terms`: **20 658 (100.00%)**
  - `source_reference` и `source_id IS NOT NULL`: **20 658 (100.00%)**
  - `synonyms`: **2 603 (12.60%)**
  - `abbreviations`: **800 (3.87%)**
- **По `major_category` (10 категорий, `category_id` 101–110):**
  - `101` — Machine Learning & Artificial Intelligence: **2 650**
  - `102` — Cryptography & Cybersecurity: **2 300**
  - `103` — Low-Level Programming & Computer Architecture: **2 358**
  - `104` — Hardware Engineering & Digital Electronics: **1 950**
  - `105` — Algorithms & Theoretical Computer Science: **1 950**
  - `106` — Databases & Distributed Systems: **2 050**
  - `107` — Networking & Protocol Engineering: **1 850**
  - `108` — Software Engineering & Compiler Technology: **2 150**
  - `109` — Mathematics & Scientific Computing: **1 750**
  - `110` — Advanced Documentation & Research Vocabulary: **1 650**
- **По уникальным доменам (`domain` — 36 уникальных значений; `subdomain` — 154 уникальных значения; `subcategories` — 155 записей):**
  `Computer Networks & Network Protocols` (1 395), `Mathematics for Computer Science` (1 115), `Distributed Systems` (1 042), `Hardware Engineering` (1 036), `Web3 / Smart Contracts` (965), `Mobile Internals (Android / iOS)` (951), `Product Management` (879), `Cybersecurity & Information Security` (844), `GameDev` (829), `Algorithms & Data Structures` (828), `Databases & Database Internals` (779), `Deep Learning & Neural Networks` (776), `Cryptography & Applied Cryptography` (768), `Artificial Intelligence & Machine Learning` (664), `Software Engineering` (617), `Low-Level Programming` (564), `QA & Testing` (557), `Statistics & Probability Theory` (538), `UI/UX Engineering` (519), `Research Methods & Academic Computing` (478), `Computer Architecture` (414), `Digital Electronics` (395), `Scientific Computing` (384), `Operating Systems` (374), `Programming Language Theory` (340), `Natural Language Processing` (334), `Computer Vision` (330), `Computational Complexity` (324), `Cloud Computing` (275), `Microprocessors & Microcontrollers` (258), `DevOps & Infrastructure` (233), `Computer Graphics` (227), `Embedded Systems` (221), `Compiler Design` (171), `Blockchain & Distributed Ledger Technology` (124), `Reinforcement Learning` (110).
- **По сложности (`difficulty_level`), CEFR и частям речи:**
  - Сложность: `Advanced` — **11 191**, `Intermediate` — **4 402**, `Expert` — **4 342**, `Research Level` — **634**, `Beginner` — **89**.
  - CEFR: `C1` — **11 191**, `C2` — **4 976**, `B2` — **4 402**, `B1` — **89**.
  - Часть речи (`part_of_speech`): `noun phrase` — **20 498**, `noun` — **146**, `abbreviation` — **14**.
- **Связанные таблицы академического словаря:**
  - `word_sources` (**19** записей): `ACM CCS` (4 579 терминов), `PyTorch Docs` (2 650), `PostgreSQL 16` (2 020), `ARM Architecture Reference Manual` (1 950), `IETF RFC Editor` (1 876), `IEEE 754-2019` (1 747), `NIST FIPS/SP 800` (1 518), `Intel SDM` (871), `Linux Kernel Docs` (852), `OWASP ASVS` (782), `Kotlin Docs` (702), `LLVM Reference` (601), `MDN Web Docs` (507), `Rust Reference` (2), `SQLite Docs` (1), а также `Python 3`, `Microsoft Learn`, `ISO/IEC 9075`, `ipa-dict & CMUdict` (по 0 прямых привязок `source_id`, при этом `ipa-dict & CMUdict` используется для атрибуции IPA в `ipa_source_url`/`ipa_license`).
  - `word_senses`: **29** значений для **8** многозначных терминов.
  - `word_relations`: **3 723** связей для **1 200** уникальных терминов (`RELATED` — **3 106**, `SYNONYM` — **617**).

#### 2.4.4. Языки программирования (`programming_languages`: 19) и сниппеты кода (`code_snippets`: 380)

- **19 языков программирования** (по **20** сниппетов кода на каждый язык, итого `19 × 20 = 380`):
  1. `Python` (`python`, 1991, Beginner)
  2. `SQL` (`sql`, 1974, Beginner)
  3. `C` (`c`, 1972, Advanced)
  4. `C++` (`cpp`, 1985, Advanced)
  5. `Java` (`java`, 1995, Intermediate)
  6. `Kotlin` (`kotlin`, 2011, Intermediate)
  7. `C#` (`csharp`, 2000, Intermediate)
  8. `Go` (`go`, 2009, Beginner)
  9. `JavaScript` (`javascript`, 1995, Beginner)
  10. `Swift` (`swift`, 2014, Intermediate)
  11. `Scala` (`scala`, 2004, Advanced)
  12. `Assembly` (`assembly`, 1949, Advanced)
  13. `Pascal` (`pascal`, 1970, Beginner)
  14. `Ruby` (`ruby`, 1995, Beginner)
  15. `Dart` (`dart`, 2011, Beginner)
  16. `VBA` (`vba`, 1993, Beginner)
  17. `Bash` (`bash`, 1989, Beginner)
  18. `Lua` (`lua`, 1993, Beginner)
  19. `Rust` (`rust`, 2010, Advanced)
- **20 уникальных `task_key` (по 19 реализаций каждого):**
  `hello_world`, `variables`, `conditions`, `loops`, `functions`, `arrays`, `classes`, `file_reading`, `async_ops`, `http_request`, `error_handling`, `json_parsing`, `regex_matching`, `unit_testing`, `singleton_pattern`, `factory_pattern`, `string_manipulation`, `sorting`, `recursion`, `generics`.

#### 2.4.5. Документация (`doc_articles`: 166), сценарии (`scenarios`: 156) и фразы (`work_phrases`: 312)

- **`doc_articles` (166 статей):** `source_url` заполнен у **166 / 166 (100%)**.
  - По уровню (`level`): `B2` — 79, `B1` — 46, `C1` — 32, `A2` — 9.
  - По `tech_tag`: `Error Logs` — 45, `SQL` — 23, `Git` — 20, `Software Engineering` — 19, `Programming` — 14, `Linux` — 13, `REST API` — 11, `Networking` — 11, `HTTP` — 10.
  - Лицензии: `IETF Trust Legal Provisions` (и `/ CC-BY-4.0` / адаптированные) — 28, `Apache-2.0` (Android/Docker/Kotlin/Synthetic) — 35, `CC-BY-NC-SA-3.0 (Pro Git)` — 20, `PostgreSQL License` — 17, `PSF-2.0` / `Public Domain (PEP 479 / SQLite)` — 18, `CC-BY-4.0` (Kubernetes, SRE Book, gRPC и др.) — 14, `Linux man-pages` / `GPL-2.0` / `LGPL-2.1` — 11, `CC-BY-SA-2.5 (MDN)` / `CC-BY-SA-4.0 (Redis)` — 7, и др. (`W3C`, `BSD`, `CC-BY-3.0`).
- **`scenarios` (156 сценариев):** 12 контекстов ровно по **13** сценариев (`TEAM_COMMUNICATION`, `SYSTEM_DESIGN_INTERVIEW`, `SALARY_NEGOTIATION`, `PR_COMMENTS`, `JIRA_TICKETS`, `INTERVIEW`, `INCIDENT_POST_MORTEM`, `GITHUB_ISSUES`, `DAILY_STANDUP`, `CODE_REVIEW`, `CLIENT_COMMUNICATION`, `BUG_REPORTING`).
  - По `exercise_type`: `FILL_BLANKS` — 35, `PHRASE_CHOICE` — 34, `SENTENCE_BUILD` — 28, `TRANSLATION_CHOICE` — 22, `LISTENING_CHOICE` — 21, `INTERVIEW_ANSWER` — 16.
  - По `difficulty`: `B2` — 66, `B1` — 45, `C1` — 28, `A2` — 17.
- **`work_phrases` (312 фраз):** те же 12 категорий ровно по **26** фраз; по `difficulty`: `B2` — 153, `B1` — 86, `C1` — 49, `A2` — 24.

---

### 2.5. Каталог всех 17 DAO-интерфейсов

Все DAO находятся в `app/src/main/kotlin/com/techenglish/app/core/database/dao/`:

| Файл | DAO-интерфейс | Целевые таблицы | Ключевые операции |
|---|---|---|---|
| `CategoryDao.kt` | `CategoryDao` | `categories`, `words` | `observeAll`, `getAll`, `getById`, `getSystemCategory`, `insertAll`, `deleteNonSystemCategory` (`is_system = 0`), `observeCategoryProgress` (`LEFT JOIN words`) |
| `WordDao.kt` | `WordDao` | `words`, `categories`, `programming_languages` | `observeAllWithDetails`, `observeDueWords`, `getDueWords`, `getDueWordsByCategory`, `searchWords`, `findByExactTerm`, `findByTermPrefix`, `getRandomWords`, `insert`/`update`, `updateSrsState`, `updateFavorite`, `deleteCustomWord` (`is_custom = 1`) |
| `ProgrammingLanguageDao.kt` | `ProgrammingLanguageDao` | `programming_languages` | `observeAll`, `getAll`, `observeById`, `getById`, `getBySlug`, `insertAll` |
| `ProgrammingLanguageDao.kt` | `CodeSnippetDao` | `code_snippets` | `observeByLanguage`, `getByLanguage`, `getById`, `getByLanguageAndTask`, `getCommonTaskKeys` (self-join по `task_key`), `insertAll` |
| `DocArticleDao.kt` | `DocArticleDao` | `doc_articles` | `observeAll`, `observeById`, `getById`, `updateReadingProgress`, `observeCompletedCount`, `insertAll`, `updateContentPreservingProgress` |
| `ScenarioDao.kt` | `ScenarioDao` | `scenarios` | `observeAll`, `observeByType`, `getById`, `getAll`, `insertAll` |
| `ScenarioDao.kt` | `WorkPhraseDao` | `work_phrases` | `observeAll`, `getAll`, `observeByCategory`, `insertAll` |
| `StudyLogDao.kt` | `StudyLogDao` | `study_logs` | `insert`, `observeAll`, `observeRecent`, `observeSince`, `getAll`, `observeCompletedScenarioIds`, `observeTotalXp`, `observeTodayReviewedCards`, `observeTodayActivityCount`, `observeTotalLogsCount`, `observeCorrectLogsCount` |
| `AcademicTermDao.kt` | `AcademicTermDao` | `academic_terms`, `word_senses`, `word_relations`, `word_sources`, `subcategories` | Агрегаты (`observeMajorCategoryCounts`, `observeDomainCounts`), индексный пагинированный поиск через `@RawQuery` (`searchPaginated`, `countMatching`), `updateContentPreservingSrs`, `getSrsDeckForMode`, `getSensesForTerm`, `getRelationsForTerm`, `getSourceById`, `updateFavorite`, `updateInSrs`, `updateSrsProgress` |
| `Stage2Daos.kt` | `CustomImportedDocDao` | `custom_imported_docs` | `observeAll`, `getAll`, `getById`, `insert`, `deleteById` |
| `Stage2Daos.kt` | `TimeAttackRecordDao` | `time_attack_records` | `observeTopRecords`, `getTopRecords`, `observeBestScoreForMode`, `getBestScoreForMode`, `insert` |
| `Stage3Daos.kt` | `SyncQueueDao` | `sync_queue` | `enqueue` (`IGNORE`), `getPendingBatch`, `observePendingCount`, `getPendingCount`, `markSynced`, `recordRetryFailure`, `pruneSynced` |
| `Stage3Daos.kt` | `SocialCacheDao` | `user_profile_cache`, `local_account_link`, `leaderboard_cache`, `friends_cache`, `activity_feed_cache`, `badges_cache` | CRUD и кэширование профиля, связки аккаунта, 4 вкладок лидерборда, друзей, ленты реакций и достижений |
| `Stage3Daos.kt` | `OtaContentMetaDao` | `ota_content_meta` | `observeAppliedPacks`, `getPackMeta`, `upsertPackMeta` |
| `Stage4ADaos.kt` | `CommentsCacheDao` | `comments_cache` | `observeCommentsForTarget` (сортировка `upvotes - downvotes`), `upsertComments`, `hideCommentLocally`, `hideCommentsByAuthor`, `clearForTarget` |
| `Stage4ADaos.kt` | `CommunityDecksDao` | `community_decks`, `community_deck_terms` | `observeVisibleDecks`, `countDecks`, `getDeckById`, `getTermsForDeck`, `upsertDecks`, `insertDeckTerms`, `markDownloadedLocally`, `updateDeckRating`, `hideDeckLocally`, `hideDecksByAuthor` |
| `Stage4ADaos.kt` | `RoomMessagesCacheDao` | `room_messages_cache` | `observeRoomMessages` (`LIMIT 100`), `upsertMessages`, `hideMessageLocally`, `hideMessagesByAuthor` |

---

### 2.6. Хранилища вне Room

1. **`UserPreferencesDataStore`** (`app/src/main/kotlin/com/techenglish/app/core/datastore/UserPreferencesDataStore.kt`):
   - Jetpack Preferences DataStore с именем файла **`tech_english_prefs`**.
   - Хранит **14** пользовательских настроек:

| Ключ DataStore | Тип | Значение по умолчанию | Назначение |
|---|---|---|---|
| `user_name` | `String` | `"Разработчик"` | Отображаемое локальное имя пользователя |
| `daily_goal` | `Int` | `20` | Дневная цель карточек (`10`/`20`/`30` или `5..100`) |
| `theme_mode` | `String` | `"SYSTEM"` | Тема интерфейса (вкл. IDE-темы) |
| `dynamic_colors` | `Boolean` | `true` | Material You динамические цвета |
| `reading_font_size` | `Float` | `16f` | Размер шрифта чтения в `sp` (`13f..24f`) |
| `notifications_enabled` | `Boolean` | `true` | Напоминания WorkManager о повторении SM-2 |
| `reminder_hour` | `Int` | `19` | Час напоминания (`0..23`) |
| `reminder_minute` | `Int` | `0` | Минута напоминания (`0..59`) |
| `haptic_feedback_enabled` | `Boolean` | `true` | Тактильный отклик |
| `reduce_motion_enabled` | `Boolean` | `false` | Уменьшение анимаций (a11y) |
| `translation_language` | `String` | `"RU"` | Язык перевода (`RU` / `TG`) |
| `age_verified_16_plus` | `Boolean` | `false` | Подтверждение возраста 16+ |
| `allow_friend_pings` | `Boolean` | `true` | Разрешение дружеских пингов |
| `ai_consent_accepted` | `Boolean` | `false` | Явное согласие на отправку обезличенного текста в AI-прокси |

2. **`AndroidKeystoreTokenStore`** (`app/src/main/kotlin/com/techenglish/app/core/security/AndroidKeystoreTokenStore.kt`):
   - Jetpack Preferences DataStore с именем файла **`techenglish_secure_tokens`**.
   - Механизм защиты: ключ в аппаратном/системном `AndroidKeyStore` под алиасом `techenglish_jwt_aes256_gcm_key` (`AES/GCM/NoPadding`, 256 бит, 128-битный тег аутентификации); зашифрованные значения кодируются как `GCM:<ivB64>:<ctB64>`. В JVM/Robolectric unit-тестах при отсутствии `AndroidKeyStore` автоматически используется тестовый fallback с префиксом `PLAIN:<base64>`.
   - Хранимые ключи (7 шт.): `access_token_enc`, `refresh_token_enc`, `user_id`, `username`, `email`, `provider`, `api_base_url` (по умолчанию `https://api.techenglish.app`; принимаются только `https://`, `http://10.0.2.2`, `http://localhost`).

---

## 3. Отчёт по базе данных и хранилищам бэкенда

### 3.1. Серверные модели БД (`backend/app/models.py`)

ORM: **SQLAlchemy 2.0** (`DeclarativeBase`), **24** таблицы. Инициализируются в `init_db()` (`Base.metadata.create_all`). В локальном dev-режиме по умолчанию подключается SQLite (`sqlite:///./techenglish_dev.db`), в Docker Compose — **PostgreSQL 16**.

| Этап | Класс модели | Таблица (`__tablename__`) | Ключевые поля, типы, FK и ограничения |
|---|---|---|---|
| Stage 3 | `UserRow` | `users` | PK `id` (`String(64)`), `username` (`String(32)`, UNIQUE, idx), `email` (`String(255)`, UNIQUE, idx, NULL), `password_hash`, `provider`, `is_email_verified`, `email_verify_token`, `password_reset_token`, `display_name`, `avatar_key`, `status_text`, `tech_stack_csv`, `github_url`, `telegram_handle`, `league`, `total_xp`, `squad_bonus_xp`, `streak_days`, `accepts_pings`, `confirmed_reports`, `is_contributor`, `created_at_ms` (`BigInteger`) |
| Stage 3 | `SyncEventRow` | `sync_events` | PK `event_id` (`String(64)`), `user_id` (`FK -> users.id CASCADE`, idx), `event_type`, `payload_json` (`Text`), `awarded_xp`, `client_timestamp_ms`, `server_received_ms` (idx), `week_start_utc5_ms` (idx) |
| Stage 3 | `Sm2StateRow` | `sm2_states` | PK `id` (`Integer`, auto), `user_id` (`FK -> users.id CASCADE`, idx), `normalized_term` (`String(200)`), `repetitions`, `ease_factor`, `interval_days`, `next_review_at_ms`, `client_timestamp_ms`, `server_received_ms`; **`UniqueConstraint("user_id", "normalized_term", name="uq_sm2_user_term")`** |
| Stage 3 | `UserFavoriteRow` | `user_favorites` | PK `id` (auto), `user_id` (`FK -> users.id CASCADE`, idx), `normalized_term`; **`uq_fav_user_term`** (`user_id`, `normalized_term`) |
| Stage 3 | `UserCustomWordRow` | `user_custom_words` | PK `id` (auto), `user_id` (`FK -> users.id CASCADE`, idx), `normalized_term`, `translation`; **`uq_custom_user_term`** (`user_id`, `normalized_term`) |
| Stage 3 | `FriendshipRow` | `friendships` | PK `id` (auto), `user_id` (`FK -> users.id CASCADE`, idx), `friend_id` (`FK -> users.id CASCADE`, idx), `created_at_ms`; **`uq_friendship_pair`** (`user_id`, `friend_id`) |
| Stage 3 | `FriendPingRow` | `friend_pings` | PK `id` (auto), `sender_id` (`FK -> users.id CASCADE`, idx), `receiver_id` (`FK -> users.id CASCADE`, idx), `sent_at_ms` (idx) |
| Stage 3 | `ActivityFeedRow` | `activity_feed` | PK `id` (`String(64)`), `actor_user_id` (`FK -> users.id CASCADE`, idx), `summary_ru`, `reaction_count`, `created_at_ms` (idx) |
| Stage 3 | `UserBlockRow` | `user_blocks` | PK `id` (auto), `blocker_id` (`FK -> users.id CASCADE`, idx), `blocked_id` (`FK -> users.id CASCADE`, idx), `created_at_ms`; **`uq_user_block`** (`blocker_id`, `blocked_id`) |
| Stage 3 | `ModerationReportRow` | `moderation_reports` | PK `id` (`String(64)`), `reporter_id` (`FK -> users.id CASCADE`), `target_type`, `target_id`, `reason`, `status`, `created_at_ms` |
| Stage 4A | `CommentRow` | `comments` | PK `id` (`String(64)`), `target_type` (idx), `target_id` (`Integer`, idx), `author_user_id` (`FK -> users.id CASCADE`, idx), `author_username`, `author_rank_title`, `body` (`Text`), `upvotes`, `downvotes`, `is_hidden`, `created_at_ms` (idx) |
| Stage 4A | `CommentVoteRow` | `comment_votes` | PK `id` (auto), `comment_id` (`FK -> comments.id CASCADE`, idx), `user_id` (`FK -> users.id CASCADE`, idx), `vote`; **`uq_comment_vote_user`** (`comment_id`, `user_id`) |
| Stage 4A | `CommunityDeckRow` | `community_decks` | PK `id` (`String(64)`), `title`, `description`, `tags_csv`, `author_user_id` (`FK -> users.id CASCADE`, idx), `author_username`, `rating` (`Float`), `downloads_count`, `term_count`, `is_hidden`, `created_at_ms` (idx) |
| Stage 4A | `CommunityDeckTermRow` | `community_deck_terms` | PK `id` (auto), `deck_id` (`FK -> community_decks.id CASCADE`, idx), `term`, `transcription`, `translation`, `example`, `explanation_ru` |
| Stage 4A | `CommunityDeckRatingRow` | `community_deck_ratings` | PK `id` (auto), `deck_id` (`FK -> community_decks.id CASCADE`, idx), `user_id` (`FK -> users.id CASCADE`, idx), `stars`; **`uq_deck_rating_user`** (`deck_id`, `user_id`) |
| Stage 4B | `AiUsageLogRow` | `ai_usage_logs` | PK `id` (`String(64)`), `user_id` (`FK -> users.id CASCADE`, idx), `feature` (`INTERVIEW` / `CODE_REVIEW`), `utc_day` (idx), `created_at_ms` |
| Stage 4B | `SquadRow` | `squads` | PK `id` (`String(64)`), `name`, `invite_code` (UNIQUE, idx), `creator_user_id` (`FK -> users.id CASCADE`), `weekly_goal_xp`, `created_at_ms` |
| Stage 4B | `SquadMemberRow` | `squad_members` | PK `id` (auto), `squad_id` (`FK -> squads.id CASCADE`, idx), `user_id` (`FK -> users.id CASCADE`, idx), `joined_at_ms`; **`uq_squad_member`** (`squad_id`, `user_id`) |
| Stage 4B | `DictionaryPrRow` | `dictionary_prs` | PK `id` (`String(64)`), `author_user_id` (`FK -> users.id CASCADE`, idx), `author_username`, `term`, `proposed_translation_ru`, `proposed_translation_tg`, `proposed_definition_ru`, `source_reference`, `license_accepted`, `status`, `upvotes`, `downvotes`, `created_at_ms` (idx) |
| Stage 4B | `DictionaryPrVoteRow` | `dictionary_pr_votes` | PK `id` (auto), `pr_id` (`FK -> dictionary_prs.id CASCADE`, idx), `user_id` (`FK -> users.id CASCADE`, idx), `vote`; **`uq_dict_pr_vote`** (`pr_id`, `user_id`) |
| Stage 4C | `RoomMessageRow` | `room_messages` | PK `id` (`String(64)`), `room_slug` (idx), `author_user_id` (`FK -> users.id CASCADE`, idx), `author_username`, `author_rank_title`, `body` (`Text`), `is_hidden`, `created_at_ms` (idx) |
| Stage 4C | `PvpMatchRow` | `pvp_matches` | PK `id` (`String(64)`), `player1_id` (`FK -> users.id CASCADE`, idx), `player2_id` (idx, NULL), `mode`, `state`, `questions_json` (`Text`), `player1_score`, `player2_score`, `ranked_xp_awarded`, `created_at_ms` (idx), `finished_at_ms` |
| Stage 4C | `PeerReviewSubmissionRow` | `peer_review_submissions` | PK `id` (`String(64)`), `case_id` (idx), `author_user_id` (`FK -> users.id CASCADE`, idx), `author_username`, `author_rank_title`, `text_response`, `voice_object_key` (NULL), `voice_duration_seconds`, `voice_expires_at_ms`, `is_hidden`, `created_at_ms` (idx) |
| Stage 4C | `PeerReviewFeedbackRow` | `peer_review_feedback` | PK `id` (`String(64)`), `submission_id` (`FK -> peer_review_submissions.id CASCADE`, idx), `reviewer_user_id` (`FK -> users.id CASCADE`, idx), `reviewer_username`, `reviewer_rank_title`, `rating_stars`, `comment_text`, `created_at_ms` |

---

### 3.2. Использование Redis, WebSockets и хранилища голосовых записей (фактическое состояние кода)

1. **Redis:**
   - В `backend/docker-compose.yml` поднят контейнер `redis:7-alpine` (`--appendonly yes`, порт `6379`, том `redis_data:/data`), в `backend/requirements.txt` подключён пакет `redis==5.2.1`, а в `backend/app/config.py` читается переменная `REDIS_URL`.
   - **Фактическое наблюдение по коду `backend/app/main.py`:** в текущей реализации `main.py` Python-клиент `redis` напрямую не импортируется и не вызывается:
     - **Рейтинги (`/v1/leaderboards/{tab}`):** вычисляются SQL-агрегациями по `users` и `sync_events` (с фильтром по `week_start_utc5_ms`).
     - **Лимиты (rate limits / квоты):** проверяются SQL-запросами к БД — anti-cheat (`sync_events` за 1 мин / 24 ч), cooldown пинга друзей (`friend_pings`), суточная квота AI-прокси (`ai_usage_logs` по `utc_day`, лимит `MAX_AI_REQUESTS_PER_DAY = 15`).
     - **WebSockets (`/ws/rooms/{room_slug}` и `/ws/pvp/{match_id}`):** активные сокет-соединения комнат и очередь поиска соперника в 1v1 PvP хранятся в оперативной памяти процесса Python (`_ROOM_WS_CONNECTIONS` и `_PVP_WAITING_MATCHES` в `backend/app/main.py`), при этом сами сообщения комнат и состояние матчей сохраняются в SQL-таблицы `room_messages` и `pvp_matches`.
2. **Хранилище голосовых записей и локальные файлы бэкенда:**
   - Класс `VoiceObjectStorage` (`backend/app/voice_storage.py`) обслуживает голосовые ответы Peer Review Arena (≤ 20 с, ≤ 512 КБ, форматы `m4a`/`ogg`/`webm`, TTL 30 дней).
   - Просроченные записи удаляются лениво при обращении к `GET /v1/peer-review/submissions`; при удалении аккаунта (`DELETE /v1/account`) клипы пользователя удаляются принудительно (`voice_storage.delete_object`).
   - В текущей реализации `VoiceObjectStorage` записывает бинарные объекты в локальную директорию `backend/.voice_objects` (переопределяется переменной `VOICE_STORAGE_LOCAL_DIR`), тогда как в `docker-compose.yml` для продакшн/S3-контура описан сервис `minio/minio:latest` с переменными `VOICE_STORAGE_ENDPOINT` и `VOICE_STORAGE_BUCKET`.
   - На диске также присутствует gitignored-артефакт `backend/test_stage3.db` (`438 272` Б) — при read-only проверке в нём обнаружено `0` таблиц.

---

### 3.3. Сервисы Docker Compose, порты и переменные окружения

| Сервис | Образ / сборка | Порты | Volumes | Healthcheck / параметры |
|---|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `5432:5432` | `pg_data:/var/lib/postgresql/data` | `pg_isready` (интервал 5s, таймаут 5s, 10 попыток) |
| `redis` | `redis:7-alpine` | `6379:6379` | `redis_data:/data` | `redis-server --appendonly yes`, `redis-cli ping` |
| `minio` | `minio/minio:latest` | `9000:9000` (S3), `9001:9001` (Console) | `minio_data:/data` | `server /data --console-address ":9001"` |
| `api` | `build: .` (`backend/Dockerfile`, `python:3.12-slim`) | `8000:8000` | — | Зависит от `postgres` (`healthy`), `redis` (`healthy`), `minio` (`started`), user `appuser` (`10001`) |

**Перечень имён переменных окружения** (из `backend/docker-compose.yml`, `backend/app/config.py`, `backend/app/ai_proxy.py`, `backend/app/voice_storage.py`, без раскрытия значений):

| Имя переменной | Где используется | Назначение |
|---|---|---|
| `POSTGRES_DB` | `docker-compose.yml` | Имя базы данных PostgreSQL |
| `POSTGRES_USER` | `docker-compose.yml` | Имя пользователя PostgreSQL |
| `POSTGRES_PASSWORD` | `docker-compose.yml` | Пароль пользователя PostgreSQL |
| `MINIO_ROOT_USER` | `docker-compose.yml` | Логин root-аккаунта MinIO |
| `MINIO_ROOT_PASSWORD` | `docker-compose.yml` | Пароль root-аккаунта MinIO |
| `DATABASE_URL` | `docker-compose.yml`, `config.py` | Строка подключения SQLAlchemy (PostgreSQL или SQLite) |
| `REDIS_URL` | `docker-compose.yml`, `config.py` | URL подключения к Redis |
| `JWT_SECRET` | `docker-compose.yml`, `config.py` | Секретный ключ подписи JWT (HS256) |
| `ACCESS_TOKEN_TTL_SECONDS` | `config.py` | Время жизни access-токена в секундах (по умолчанию `1800`) |
| `REFRESH_TOKEN_TTL_SECONDS` | `config.py` | Время жизни refresh-токена в секундах (по умолчанию `2592000`) |
| `PLAY_INTEGRITY_STRICT` | `docker-compose.yml`, `config.py` | Флаг строгой проверки токена Play Integrity |
| `VOICE_STORAGE_ENDPOINT` | `docker-compose.yml` | HTTP-эндпоинт S3/MinIO для голосовых клипов |
| `VOICE_STORAGE_BUCKET` | `docker-compose.yml` | Имя S3-бакета (`techenglish-peer-voice`) |
| `VOICE_STORAGE_LOCAL_DIR` | `voice_storage.py` | Переопределение локальной директории хранения голосовых клипов |
| `PEER_REVIEW_VOICE_TTL_DAYS` | `docker-compose.yml`, `voice_storage.py` | TTL голосовых клипов Peer Review в днях (по умолчанию `30`) |
| `TECHENGLISH_AI_DAILY_LIMIT` | `ai_proxy.py` | Суточный лимит запросов к AI-прокси на пользователя (по умолчанию `15`) |
| `GEMINI_API_KEY` | `ai_proxy.py` | Серверный ключ API Gemini (при отсутствии используется детерминированный rubric-режим) |

---

## 4. Конвейер данных, импорта и синхронизации

### 4.1. Конвейер сборки предзаполненной БД и валидации словаря

```text
app/schemas/.../5.json (DDL + identityHash Room v5)
        │
        ▼
scripts/build_seed_db.py ──► app/src/main/assets/databases/tech_english.db (90.56 MB)
   ▲                  ▲
   │                  └─ tools/vocabulary_import/corpus_builder.py
   │                        ├── sources_and_taxonomy.py (19 источников, категории, смыслы)
   │                        ├── lexicon_domains_a_e.py
   │                        ├── lexicon_domains_f_j.py
   │                        ├── lexicon_new_domains_v3.py
   │                        ├── licensed_ipa_lexicon.py (IPA из ipa-dict / CMUdict)
   │                        ├── scripts/seed_data/vocabulary_data.py
   │                        └── проверка через vocabulary_validator.py + vocabulary_schema.json
   │
   └─ scripts/seed_data/:
        ├── languages_data.py (CATEGORIES, LANGUAGES)
        ├── vocabulary_data.py (WORDS)
        ├── snippets_part1.py + snippets_part2.py (импортирует snippets_v3_expansion.py)
        ├── docs_data.py (импортирует docs_v3_expansion.py)
        └── scenarios_data.py (импортирует scenarios_v3_expansion.py)
```

- **`scripts/build_seed_db.py`**: считывает точную структуру таблиц, индексов, внешних ключей и `identityHash` из `app/schemas/.../5.json`, выставляет `PRAGMA user_version = 5`, заполняет все seed-таблицы, затем проверяет `PRAGMA foreign_key_check`, `PRAGMA integrity_check == "ok"`, наличие **31** обязательного индекса и выполняет `ANALYZE;` + `VACUUM;`. (Примечание аудита: комментарии в файле частично остались от v2/v3, но `SCHEMA_PATH` указывает на `5.json`, а `snippets_part2.py`, `docs_data.py` и `scenarios_data.py` транзитивно подгружают модули `*_v3_expansion.py`).
- **`tools/vocabulary_import/vocabulary_validator.py`**: валидирует корпус против JSON Schema (`vocabulary_schema.json`, **20** обязательных полей), проверяет отсутствие нормализованных дубликатов, конфликтующих переводов, минимальную длину определений, значения enum-полей и наличие авторитетных источников.
- **`tools/vocabulary_import/vocabulary_importer.py`**: CLI-утилита (`--input`, `--db`, `--export-csv`) для импорта/экспорта JSON и CSV и идемпотентного upsert в `academic_terms` SQLite с сохранением пользовательского прогресса SRS и избранного.
- **`tools/vocabulary_import/vocabulary_statistics.py`**: формирует сводный отчёт `docs/VOCABULARY_QUALITY_REPORT.md` и артефакты в `tools/vocabulary_import/output/`.

---

### 4.2. Файлы скриптов, импорта и промежуточные/выходные артефакты

| Папка / файл | Размер (байт) | Размер (МБ / МиБ) | Назначение и метаданные |
|---|---:|---:|---|
| `scripts/build_seed_db.py` | 14 194 | — | Сборщик `tech_english.db` |
| `scripts/seed_data/__init__.py` | 60 | — | Инициализатор пакета |
| `scripts/seed_data/docs_data.py` | 61 935 | — | Базовые статьи документации + импорт `docs_v3_expansion.py` |
| `scripts/seed_data/docs_v3_expansion.py` | 183 708 | — | Расширение статей и логов ошибок (v3) |
| `scripts/seed_data/languages_data.py` | 28 945 | — | 11 категорий + 19 языков программирования |
| `scripts/seed_data/scenarios_data.py` | 71 978 | — | Базовые сценарии/фразы + импорт `scenarios_v3_expansion.py` |
| `scripts/seed_data/scenarios_v3_expansion.py` | 244 326 | — | Расширение сценариев и рабочих фраз (v3) |
| `scripts/seed_data/snippets_part1.py` | 136 184 | — | Первая часть сниппетов кода и хелпер `make_snippet` |
| `scripts/seed_data/snippets_part2.py` | 90 043 | — | Вторая часть сниппетов + импорт `snippets_v3_expansion.py` |
| `scripts/seed_data/snippets_v3_expansion.py` | 97 318 | — | Расширение сниппетов (v3) |
| `scripts/seed_data/vocabulary_data.py` | 95 384 | — | 255 базовых IT-терминов |
| **Итого `scripts/` (11 файлов)** | **1 024 075** | **0.98 MiB** | |
| `tools/vocabulary_import/corpus_builder.py` | 51 739 | — | Компилятор корпуса 20 658 терминов, смыслов и связей |
| `tools/vocabulary_import/lexicon_domains_a_e.py` | 153 203 | — | Лексикон доменов A–E |
| `tools/vocabulary_import/lexicon_domains_f_j.py` | 72 082 | — | Лексикон доменов F–J |
| `tools/vocabulary_import/lexicon_new_domains_v3.py` | 58 752 | — | Новые домены v3 |
| `tools/vocabulary_import/licensed_ipa_lexicon.py` | 97 130 | — | Открытый фонетический словарь IPA (`ipa-dict` / `CMUdict`) |
| `tools/vocabulary_import/sources_and_taxonomy.py` | 23 011 | — | 19 источников, 10 major-категорий, домены, `POLYSEMOUS_SENSES` |
| `tools/vocabulary_import/vocabulary_importer.py` | 11 876 | — | Импортер/экспортер JSON/CSV в SQLite |
| `tools/vocabulary_import/vocabulary_schema.json` | 3 028 | — | JSON Schema Draft 2020-12 для академических терминов |
| `tools/vocabulary_import/vocabulary_statistics.py` | 6 705 | — | Генератор отчёта качества и выходных артефактов |
| `tools/vocabulary_import/vocabulary_validator.py` | 7 272 | — | Валидатор корпуса и детектор дубликатов/конфликтов |
| `tools/vocabulary_import/output/sample_academic_vocabulary_500.csv` | **1 002 970** | **0.96 MiB / 1.00 MB** | Выборка первых **500** записей словаря (+ строка заголовка) |
| `tools/vocabulary_import/output/tajik_translations_pending_review.json` | **1 356 341** | **1.29 MiB / 1.36 MB** | **2 050** кандидатов на таджикском (`schema_version: "3.0.0"`, `verification_status: "pending_review"`, `included_in_release_db: false`) |
| **Итого `tools/` (12 файлов)** | **2 844 109** | **2.71 MiB** | |

---

### 4.3. Механизмы импорта пользовательских данных, OTA и облачной синхронизации

1. **Импорт/экспорт пользовательских колод (Anki `.apkg`, Anki TSV, JSON, CSV):**
   - `app/src/main/kotlin/com/techenglish/app/core/database/AnkiApkgExtractor.kt`: потоково распаковывает ZIP-архив `.apkg` с буфером 8 КБ, защищается от Zip Slip (`!entry.name.contains("..")`), извлекает `collection.anki21` или `collection.anki2` во временный файл в `cacheDir`, открывает SQLite строго в `OPEN_READONLY`, читает только таблицу `notes(flds, tags)`, игнорирует расписание `cards` (чтобы интервалы велись SM-2 в TechEnglish) и удаляет временный файл в `finally`.
   - `domain/src/main/kotlin/com/techenglish/domain/usecase/Stage2OfflineFeaturesUseCases.kt` (`DeckImportExportUseCase`): сериализация/парсинг `techenglish-deck-v3` JSON, CSV, Anki TSV и Anki `notes` (разделитель `\u001F`, очистка HTML-тегов), импорт в системную категорию «Мои слова» с дедупликацией. UI/VM: `Stage2ViewModels.kt` (`DeckImportExportViewModel`) и `Stage2Screens.kt` (`DeckImportExportScreen`).
2. **Импорт собственных документов (Markdown / TXT — `Read Your Own Docs`):**
   - `ScanCustomDocTermsUseCase` (`Stage2OfflineFeaturesUseCases.kt`), `CustomDocRepositoryImpl` (`RepositoryImpls.kt`), `CustomImportedDocDao` (`Stage2Daos.kt`), `ReadYourOwnDocsViewModel` (`Stage2ViewModels.kt`).
3. **Data-Only OTA-пакеты контента:**
   - Клиент: `ContentPackImporter.kt` (`importFromJson` / `importPayload`) проверяет SHA-256 (`sha256Hex`), пропускает записи со статусом, отличным от `verified`, и выполняет транзакционный upsert по ключу `normalized_term` через `AcademicTermDao.updateContentPreservingSrs(...)` и `DocArticleDao.updateContentPreservingProgress(...)`, **никогда не перезаписывая** пользовательские поля SM-2 (`repetitions`, `ease_factor`, `interval_days`, `next_review_at`, `last_studied_at`, `is_favorite`, `is_in_srs`) и прогресс чтения. Применённые пакеты фиксируются в `ota_content_meta` (`OtaContentMetaDao`).
   - Сервер: `GET /v1/ota/manifest` и `GET /v1/ota/packs/{pack_id}` (`OTA_PACKS` в `backend/app/main.py`).
4. **Облачная синхронизация (Outbox / Sync Queue), конфликты SM-2 и Anti-Cheat:**
   - **Outbox-очередь:** при каждой записи в `StudyLogRepositoryImpl.insertLog()` (`RepositoryImpls.kt`) автоматически ставится событие в таблицу `sync_queue` с UUID `event_id` и статусом `PENDING`.
   - **Фоновый движок:** `CloudSyncEngine` и `CloudSyncWorker` (`app/src/main/kotlin/com/techenglish/app/core/sync/CloudSyncEngine.kt`) отправляют пакет до 50 событий через `TechEnglishApiClient` (Ktor OkHttp + JSON + WebSockets) на `POST /v1/sync/events`, обновляют офлайн-кэши лидерборда/друзей/ленты и проверяют OTA-пакеты. WorkManager планирует периодическую задачу каждые **6 часов** с условием `NetworkType.CONNECTED` (плюс разовый запуск при необходимости).
   - **Разрешение конфликтов SM-2 (Errata E1):** синхронизировано на клиенте (`ResolveSm2SyncConflictUseCase` в `Stage3SyncAndAntiCheatUseCases.kt`) и на сервере (`should_incoming_sm2_win` в `backend/app/anti_cheat.py`): 1) future client timestamp > +5 минут от `server_received` урезается; 2) побеждает более свежий `client_timestamp`; 3) при равенстве — большее `repetitions`; 4) при равенстве — более ранний `server_received`.
   - **Серверно-клиентский Anti-Cheat:** `ValidateRankedEventAntiCheatUseCase` и `evaluate_ranked_event`: идемпотентность по `event_id`, канонический XP по типу события (игнорирование подмены XP клиентом), пороги **120 XP/мин**, **2 500 XP/день**, минимальная длительность сессии **300 мс**, окно офлайн-событий до 30 дней.

---

## 5. Техническая инвентаризация проекта

### 5.1. Стек технологий и точные версии

#### Android / Gradle (`app/build.gradle.kts`, `domain/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle-wrapper.properties`)
- **SDK & App Config:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, `versionCode = 300`, `versionName = "3.0.0"`, `applicationId = "com.techenglish.app"`, `Java/Kotlin JVM Target = 17`, `isMinifyEnabled = false` в `buildTypes.release`.
- **Инструментарий сборки:** Gradle Wrapper **`8.10.2`**, AGP **`8.7.2`**, Kotlin **`2.0.21`**, KSP **`2.0.21-1.0.28`**, JaCoCo **`0.8.12`** (в `:domain`).

| Библиотека / плагин | Версия | Назначение |
|---|---|---|
| Hilt (`hilt-android` / `hilt-compiler`) | `2.52` | DI в Android-модуле |
| AndroidX Hilt (`navigation-compose`, `work`, `compiler`) | `1.2.0` | Интеграция Hilt с Compose Navigation и WorkManager |
| Room (`runtime`, `ktx`, `compiler`, `testing`) | `2.6.1` | Локальная SQLite БД (KSP, экспорт схем в `app/schemas`) |
| Jetpack Compose BOM | `2024.10.01` | UI, Material 3, Foundation, Animation, UI Test |
| Navigation Compose | `2.8.4` | Навигация по 34 маршрутам |
| DataStore Preferences | `1.1.1` | Хранилища настроек и шифрованных токенов |
| WorkManager (`work-runtime-ktx`, `work-testing`) | `2.9.1` | Фоновая синхронизация и ежедневные напоминания |
| Media3 (`exoplayer`, `common`) | `1.4.1` | Аудиоконтроллер |
| Jetpack Glance (`appwidget`, `material3`) | `1.1.1` | Виджет рабочего стола «Слово дня» |
| Ktor Client (`core`, `okhttp`, `content-negotiation`, `websockets`, `serialization-kotlinx-json`) | `2.3.12` | Единый HTTP и WebSocket клиент (без Retrofit) |
| Coil (`coil-compose`) | `2.7.0` | Загрузка аватаров OAuth |
| Kotlinx Coroutines (`core`, `android`, `test`) | `1.9.0` | Асинхронность и Flow |
| Kotlinx Serialization JSON | `1.7.3` | JSON-сериализация для Ktor, Room-полей и OTA |
| Lifecycle (`runtime-ktx`, `runtime-compose`, `viewmodel-compose`) | `2.8.7` | Жизненный цикл и ViewModel |
| Activity Compose / Core KTX / Core Splashscreen | `1.9.3` / `1.15.0` / `1.0.1` | Интеграция Activity и SplashScreen |
| `javax.inject` | `1` | Аннотации `@Inject` в чистом JVM модуле `:domain` |
| Тестирование (`junit`, `robolectric`, `test-core`, `ext-junit`, `arch-core`) | `4.13.2` / `4.13` / `1.6.1` / `1.2.1` / `2.2.0` | Модульные, Room, WorkManager и Compose UI тесты |

#### Серверный стек (`backend/requirements.txt`, `Dockerfile`, `docker-compose.yml`)
- **Язык / рантайм:** Python `3.12-slim`.
- **Зависимости Python:** `fastapi==0.115.6`, `uvicorn[standard]==0.32.1`, `sqlalchemy==2.0.36`, `pydantic==2.10.3`, `psycopg[binary]==3.2.3`, `redis==5.2.1`, `PyJWT==2.10.1`, `httpx==0.28.1`, `pytest==8.3.4`.
- **Контейнеры инфраструктуры:** `postgres:16-alpine`, `redis:7-alpine`, `minio/minio:latest`.

---

### 5.2. Разрешения (`AndroidManifest.xml`) и настройки сетевой безопасности

- **Разрешения `<uses-permission>` (5 шт.):**
  1. `android.permission.INTERNET` — синхронизация, облачные функции и WebSockets.
  2. `android.permission.ACCESS_NETWORK_STATE` — проверка офлайн/онлайн статуса перед синхронизацией.
  3. `android.permission.POST_NOTIFICATIONS` — ежедневные уведомления интервального повторения SM-2.
  4. `android.permission.RECORD_AUDIO` — по запросу для тренажёра произношения / голосовых ответов.
  5. `android.permission.CAMERA` — объявлено по запросу (вместе с `<uses-feature android:name="android.hardware.camera" android:required="false" />`).
- **Видимость пакетов (`<queries>`):** интенты `android.intent.action.TTS_SERVICE` и `android.speech.RecognitionService` для офлайн-движков речи на Android 11+.
- **Прочие атрибуты манифеста:** `android:allowBackup="true"`, `android:supportsRtl="true"`, `android:networkSecurityConfig="@xml/network_security_config"`, удаление стандартного `androidx.work.WorkManagerInitializer` в пользу `HiltWorkerFactory`.
- **Сетевая безопасность (`app/src/main/res/xml/network_security_config.xml`):**
  - `<base-config cleartextTrafficPermitted="false" />` — в релизных сборках открытый HTTP-трафик полностью запрещён (требуется HTTPS / TLS).
  - `<debug-overrides>` — разрешает доверие пользовательским (`user`) и системным (`system`) сертификатам в отладочных сборках.

---

### 5.3. Полный перечень тестовых наборов

Всего в репозитории **20** тестовых файлов (**102** автоматических теста и 1 файл тестовых заглушек `FakeRepositories.kt`). Каталог `app/src/androidTest/` отсутствует — UI и Room тесты запускаются локально на JVM через Robolectric (`isIncludeAndroidResources = true`).

#### Domain Unit-тесты (`domain/src/test/`, 13 файлов, 55 `@Test`)

| Файл | `@Test` | Проверяемые области |
|---|---:|---|
| `domain/src/test/kotlin/com/techenglish/domain/FakeRepositories.kt` | 0 | In-memory fake-реализации 6 базовых репозиториев |
| `domain/src/test/kotlin/com/techenglish/domain/algorithm/Sm2EngineTest.kt` | 8 | Алгоритм интервального повторения SM-2 (интервалы 1/6/×EF, сброс при `FORGOT`, границы `EF >= 1.3`, превью интервалов) |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/AcademicVocabularyUseCasesTest.kt` | 4 | Нормализация запросов, дашборд и пагинированный поиск академического словаря, карточка термина со смыслами/связями/источником, 6-режимный SM-2 |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/DocumentationUseCasesTest.kt` | 3 | Фильтрация статей документации, однократное начисление XP за прочтение, поиск слов (глоссарий/БД/fallback) |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/ProgrammingAndComparisonUseCasesTest.kt` | 4 | Фильтрация языков, пересечение общих `task_key` (`Python vs Go`, `Cpp vs Rust`, `Java vs Kotlin`), детали сниппетов и логирование |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/ScenarioAndListeningUseCasesTest.kt` | 3 | Проверка ответов сценариев, фильтрация сценариев и фраз, генерация и оценка аудирования |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/Stage2OfflineFeaturesUseCasesTest.kt` | 7 | `ФТ-7.1`–`ФТ-7.6` и `Errata E6`: сканер n-грамм `Read Your Own Docs`, `Debug the Log`, оценка произношения, виджет (RU/TJ), Time Attack, импорт/экспорт JSON/CSV/Anki TSV/`.apkg` |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/Stage3SyncAndAntiCheatUseCasesTest.kt` | 6 | `Errata E1` SM-2 конфликты (+5 мин skew), `ФТ-9.3` anti-cheat и идемпотентность, слияние гостя, `ФТ-11.1` сброс лиги по Пн 00:00 `Asia/Dushanbe` UTC+5, `Errata E2`, `ФТ-12.3` 24ч пинг-кулдаун |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/Stage4ACommunityUseCasesTest.kt` | 3 | `ФТ-9.4` валидация UGC/спам-фильтр, `ФТ-13.1` сортировка и голосование за комментарии, `ФТ-13.2` подготовка колод сообщества к локальному импорту |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/Stage4BUseCasesTest.kt` | 3 | `ФТ-8.3` очистка PII перед AI-прокси, `ФТ-14.4` границы отрядов 5–10 человек и неранговый бонус, `ФТ-14.3` 4 порога допуска и лицензия краудсорсинга |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/Stage4CRealtimeUseCasesTest.kt` | 2 | `ФТ-14.1` PvP 15s training fallback и чистый разрыв соединения, `ФТ-14.2` / `Errata E5` лимиты 20с/30д TTL для Peer Review и комнат |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/StatisticsAndSettingsUseCasesTest.kt` | 6 | Серия (streak) из `study_logs`, расчёт уровней и рангов, агрегация статистики, настройки (IDE-темы/reduceMotion), combo streak, реестр хамелеона |
| `domain/src/test/kotlin/com/techenglish/domain/usecase/VocabularyUseCasesTest.kt` | 6 | SM-2 повторения карточек, цикл пользовательских слов, поиск, избранное, добавление слова из документации |

#### Android / Room / WorkManager / Compose UI тесты (`app/src/test/`, 3 файла, 36 `@Test`)

| Файл | `@Test` | Проверяемые области |
|---|---:|---|
| `app/src/test/kotlin/com/techenglish/app/database/TechEnglishDatabaseTest.kt` | 14 | Верификация реального ассета `tech_english.db` и `identityHash`, CRUD и защита системной категории, производительность `due words` (<100 мс), FK `SET NULL`, сравнение языков по БД, миграции `1->2`, `2->3`, `3->4`, `4->5` без потери данных, бенчмарк поиска на 30 000 терминов (<100 мс), `ContentPackImporter`, `AnkiApkgExtractor`, Outbox `sync_queue`, шифрование в `AndroidKeystoreTokenStore` |
| `app/src/test/kotlin/com/techenglish/app/notifications/ReviewReminderWorkerTest.kt` | 2 | `ReviewReminderWorker` (подсчёт просроченных слов основного и академического словарей) и расчёт задержки до напоминания через полночь |
| `app/src/test/kotlin/com/techenglish/app/ui/TechEnglishUiTest.kt` | 20 | Базовые UI-компоненты, 5 вкладок навигации и маршруты, подсветка синтаксиса, `TechAudioController`, контрастность WCAG AA для всех 5 IDE-тем (Light/Dark), покрытие кириллицы/IPA/таджикских букв в `.ttf`-шрифтах, `200% fontScale` и `reduceMotion`, `AgeGate16PlusScreen`, офлайн-баннер лидербордов, экраны Stages 4A/4B/4C |

#### Тесты бэкенда (`backend/tests/`, 4 файла, 11 `pytest`-функций)

| Файл | Тестов | Проверяемые области |
|---|---:|---|
| `backend/tests/test_api_stage3.py` | 3 | Регистрация/логин/PKCE OAuth/гостевая связка/`DELETE /v1/account`, идемпотентность синхронизации + Errata E1 + серверный anti-cheat, лидерборды/друзья/пинг/модерация/OTA |
| `backend/tests/test_api_stage4a.py` | 2 | Комментарии (голосование, сортировка, блокировка авторов, автоскрытие после репортов), колоды сообщества (публикация, скачивание, рейтинг, удаление аккаунта) |
| `backend/tests/test_api_stage4b.py` | 3 | AI-прокси (согласие, удаление PII, суточная квота), отряды Squads (5–10 участников, недельная цель, неранговый бонус), краудсорсинг PR и лицензия |
| `backend/tests/test_api_stage4c.py` | 3 | WebSocket тематических комнат и модерация, PvP 1v1 матчмейкинг/WS/disconnect, Peer Review Arena (TTL 30д голосовых клипов и физическое удаление клипа при `DELETE /v1/account`) |

---

### 5.4. Список всех файлов документации

В каталоге `docs/` размещены **17** файлов, плюс **1** корневой файл исходной спецификации:

| Файл | Размер (байт) | Строк | Краткое описание содержимого |
|---|---:|---:|---|
| `docs/ARCHITECTURE.md` | 20 542 | 282 | Спецификация гибридной Offline-First + Cloud/Realtime архитектуры, схема модулей, ER-модель Room |
| `docs/DECISIONS.md` | 12 704 | 94 | Журнал инженерных и архитектурных решений (ADR `DEC-01`–`DEC-13`), разрешение расхождений спецификаций |
| `docs/FINAL_REPORT.md` | 10 579 | 63 | Итоговый инженерный отчёт по v3.0.0: сводка этапов 1→4C, метрики тестов/БД/сборки и открытые пункты |
| `docs/IMPLEMENTATION_PLAN.md` | 13 139 | 105 | Пофазный план реализации (Phases 1–8) базовой архитектуры и модулей |
| `docs/OWNER_ACTIONS.md` | 9 724 | 28 | Реестр внешних блокеров и действий владельца (секреты, ключи, OAuth, Play Integrity, доменные сертификаты, нативные TG-переводы) |
| `docs/REQUIREMENTS.md` | 28 486 | 160 | Полная спецификация требований и матрица прослеживаемости для v1.0 / v2.0 / v3.0 (`ФТ-*`, `НФТ-*`) |
| `docs/REQUIREMENTS_TRACEABILITY.md` | 17 727 | 99 | Сквозная матрица трассируемости всех требований по этапам (от v1.0/v2.0 и Stage 1 до Wave 4C) с учётом Errata E1–E6 |
| `docs/STAGE_1_REPORT.md` | 16 927 | 127 | Отчёт по Этапу 1: редизайн UI/UX, типографика, IDE-темы, 365-дневный граф, доступность (a11y) и измеренные метрики |
| `docs/STAGE_2_REPORT.md` | 14 092 | 80 | Отчёт по Этапу 2: расширение контента, Room v3 (`MIGRATION_2_3`), `ContentPackImporter`, тренажёры `ФТ-7.1`–`7.6` и блокер по `translation_tg` |
| `docs/STAGE_3_REPORT.md` | 12 931 | 87 | Отчёт по Этапу 3: FastAPI-бэкенд, Room v4 (`MIGRATION_3_4`), Keystore, Ktor, Outbox-синхронизация, социальный блок и аудит разрешений |
| `docs/STAGE_4A_REPORT.md` | 7 170 | 58 | Отчёт по Этапу 4A: Room v5 (`MIGRATION_4_5`), комментарии/мнемоники, Community Decks и модерация UGC |
| `docs/STAGE_4B_REPORT.md` | 6 872 | 57 | Отчёт по Этапу 4B: AI Mock Interviewer/Reviewer прокси, Squads и краудсорсинг «PR в словарь» |
| `docs/STAGE_4C_REPORT.md` | 6 911 | 52 | Отчёт по Этапу 4C и финальной верификации: WebSocket-комнаты, PvP-дуэли, Peer Review Arena и хранение голосовых записей |
| `docs/UPGRADE_AUDIT.md` | 9 517 | 95 | Технический аудит миграции v1.0 → v2.0 (`AUD-01`–`AUD-05`: SDK-пути, JaCoCo, масштабирование до 30k+ терминов, аудио) |
| `docs/UPGRADE_PLAN.md` | 6 682 | 62 | Поэтапный план модернизации v1.0 → v2.0 (Phases 1–13) |
| `docs/VOCABULARY_QUALITY_REPORT.md` | 8 649 | 127 | Автогенерируемый отчёт качества академического корпуса (`20 658` терминов, 10 категорий, домены, CEFR, источники) |
| `docs/techenglish_v3_2_final.md` | 57 758 | 263 | Финальное ТЗ модернизации платформы TechEnglish до версии 3.0 (редакция 3.2) |
| `Техническая_спецификация_исправленная.md` *(в корне)* | 42 857 | 294 | Исходная академическая/проектная техническая спецификация Android-приложения |
