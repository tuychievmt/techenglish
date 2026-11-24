# Implementation Plan
## Project: Technical English for Developers (Android)

This plan structures the full production implementation into 8 gated engineering phases. After every phase, compilation, tests, and requirements tracking are verified before advancing.

---

## PHASE 1 — Project Setup & Build Architecture
- Initialize Gradle Kotlin DSL multi-module workspace:
  - Root `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore` (`local.properties` excluded from VCS), Gradle Version Catalog (`gradle/libs.versions.toml`), Gradle Wrapper (`8.10.2`).
  - `:domain` — Pure Kotlin JVM library module (zero Android SDK dependencies) + JaCoCo coverage config.
  - `:app` — Android Application module (`minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`) with Jetpack Compose, Material 3, Room (KSP + `exportSchema = true`), Dagger Hilt (KSP), DataStore, WorkManager, Media3, Navigation Compose, Kotlinx Serialization, Robolectric.
- Configure `AndroidManifest.xml` with minimal permissions (`POST_NOTIFICATIONS` for API 33+, `RECEIVE_BOOT_COMPLETED` optional or WorkManager auto-reschedule).
- Verify clean compilation (`./gradlew :domain:compileKotlin :app:compileDebugKotlin`).

## PHASE 2 — Database Layer & Pre-populated Offline Seed Content
- Design and implement Room entities matching the exact ER schema and all 10 mandatory SQLite indexes:
  - `CategoryEntity` (`categories`)
  - `ProgrammingLanguageEntity` (`programming_languages`)
  - `CodeSnippetEntity` (`code_snippets`, `idx_snippets_lang_task`)
  - `WordEntity` (`words`, `idx_words_next_review`, `idx_words_category`, `idx_words_language`, `idx_words_term_lang`)
  - `DocArticleEntity` (`doc_articles`)
  - `ScenarioEntity` (`scenarios`)
  - `WorkPhraseEntity` (`work_phrases` — supporting table for 60+ phrases & listening)
  - `StudyLogEntity` (`study_logs`, `idx_logs_timestamp`, `idx_logs_word`, `idx_logs_scenario`, `idx_logs_article`, `idx_logs_snippet`)
- Implement DAOs (`CategoryDao`, `WordDao`, `ProgrammingLanguageDao`, `CodeSnippetDao`, `DocArticleDao`, `ScenarioDao`, `WorkPhraseDao`, `StudyLogDao`), `TechEnglishDatabase`, `DatabaseMigrations` (with schema validation), and DataStore `UserPreferencesDataStore`.
- Compile `:app` to generate Room exported schema JSON (`app/schemas/.../1.json`) and extract the EXACT Room `identity_hash` and `CREATE TABLE` / `CREATE INDEX` SQL!
- Write a deterministic Python seed builder (`scripts/build_seed_db.py`) that:
  1. Reads Room's exact generated `1.json` schema and creates tables, indexes, and `room_master_table` with the exact Room `identity_hash`!
  2. Populates rich, original, technically accurate bilingual content:
     - 11 categories (10 thematic + protected `Мои слова`)
     - All 19 programming languages with full metadata
     - ~150+ syntax-accurate code snippets covering 6–10 `task_key`s per language with line-by-line EN/RU breakdowns and interactive keyword definitions
     - ~260 unique IT vocabulary terms with IPA transcriptions, RU translations, real technical examples, and RU explanations
     - 32 technical documentation articles & error logs across all 9 categories with CEFR levels, RU guides, and embedded glossaries
     - 36 interactive work scenarios across all 8 contexts & 6 exercise types
     - 60+ professional IT workplace phrases
  3. Writes the verified binary SQLite file to `app/src/main/assets/databases/tech_english.db`.
- Wire `Room.databaseBuilder(...).createFromAsset("databases/tech_english.db")` in Hilt `DatabaseModule`.
- Write and run Room database verification tests (including `createFromAsset` integrity check, FK constraints, cascade/set-null, custom word CRUD, due words query, study logs).

## PHASE 3 — Pure Kotlin Domain Layer & Unit Tests
- Implement Domain Models (`Word`, `Category`, `ProgrammingLanguage`, `CodeSnippet`, `LineExplanation`, `KeywordTooltip`, `DocArticle`, `GlossaryEntry`, `WorkScenario`, `UsefulExpression`, `WorkPhrase`, `StudyLog`, `ReviewQuality`, `Sm2Result`, `DailyProgress`, `UserStats`, `CategoryProgress`, `LanguageComparison`, `UserSettings`, `ListeningQuestion`).
- Implement `Sm2Engine` (exact SM-2 formula, `EF >= 1.3`, `I1 = 1`, `I2 = 6`, `In = round(I(n-1) * EF)`, `q = 1` reset).
- Implement Repository interfaces (`VocabularyRepository`, `ProgrammingLanguageRepository`, `DocumentationRepository`, `ScenarioRepository`, `StudyLogRepository`, `SettingsRepository`).
- Implement all UseCases:
  - Vocabulary & SRS: `CalculateNextReviewUseCase`, `GetDueWordsUseCase`, `ReviewWordUseCase`, `AddCustomWordUseCase`, `UpdateCustomWordUseCase`, `DeleteCustomWordUseCase`, `SearchWordsUseCase`, `ToggleFavoriteWordUseCase`, `AddWordFromDocUseCase`
  - Languages & Comparison: `GetProgrammingLanguagesUseCase`, `GetLanguageDetailUseCase`, `GetCodeSnippetUseCase`, `CompareLanguagesUseCase`, `LogCodeBreakdownUseCase`
  - Documentation: `GetDocArticlesUseCase`, `GetDocArticleDetailUseCase`, `UpdateReadingProgressUseCase`, `LookupWordUseCase`
  - Scenarios & Listening: `GetScenariosUseCase`, `SubmitScenarioAnswerUseCase`, `GetWorkPhrasesUseCase`, `GenerateListeningSessionUseCase`, `LogListeningAttemptUseCase`
  - Statistics & Progress: `GetDailyProgressUseCase`, `GetStatisticsUseCase`, `CalculateStreakUseCase`, `CalculateXpLevelUseCase`, `UpdateSettingsUseCase`
- Implement Data Mappers & Repository Implementations in `:app/data`.
- Write comprehensive JUnit 4 + Coroutines Test suite in `:domain` covering SM-2 edge cases, streak logic, XP/level, comparison pairing, doc lookup, scenario evaluation, listening grading.
- Run `./gradlew :domain:test :domain:jacocoTestReport` and verify **≥ 80% Domain Layer line coverage**.

## PHASE 4 — Material 3 Design System, Audio Engine & Navigation
- Build Material 3 Design System (`core/designsystem`):
  - Custom brand identity combining developer IDE precision (JetBrains/GitHub) + clean Linear minimalism + Duolingo motivation accents (Indigo/Cyan/Emerald/Amber palette, dedicated Light & Dark color schemes, Dynamic Colors on Android 12+).
  - Typography scale including Monospace code typography.
  - Shared UI components: `SkeletonPlaceholder`, `EmptyStateView`, `ErrorStateView`, `ConfirmDialog`, `LevelProgressBadge`, `StreakChip`, `DailyGoalRing`, `CategoryBadge`, `AudioControlBar`, `CodeSyntaxHighlighter` (custom multi-language `AnnotatedString` tokenizer for all 19 languages!).
- Build Audio & TTS subsystem (`core/audio`):
  - `TtsManager`: Android `TextToSpeech` lifecycle management, offline `en-US` voice detection (`!voice.isNetworkConnectionRequired`), `TtsVoiceState` (`Initializing`, `ReadyOffline`, `ReadyOnlineOnly`, `MissingVoiceData`, `Error`), `synthesizeToFile` helper, safe install voice Intent launcher (`ACTION_INSTALL_TTS_DATA`).
  - `AudioPlayerManager`: AndroidX `Media3 ExoPlayer` playback controller supporting `Play`, `Pause`, `Replay`, `Stop`, playback speed (`0.75x`, `1.0x`, `1.25x`), and playback progress Flow for synthesized or local audio clips.
- Build Navigation (`core/navigation`):
  - 5-item Bottom Navigation Bar: Главная (`home`), Словарь (`vocabulary`), Языки (`languages`), Практика (`practice`), Профиль (`profile`).
  - Full nested/secondary routes: Flashcards SRS session, Language Detail, Interactive Code Breakdown, Side-by-Side Comparison, Technical Documentation Library, Interactive Doc Reader, Work Scenarios Hub, Scenario Exercise Runner, Listening & Pronunciation Studio, Detailed Statistics, Settings.

## PHASE 5 — All 6 Feature Modules (Full Interactive Implementation)
- **Home Screen**: greeting, Daily Goal ring & today's progress, due words count, 1-tap «Начать повторение» CTA (НФТ-4.3), real streak, XP & developer rank, module quick-access cards (Documentation, Listening, Compare Languages, Work Scenarios, Statistics), recent activity feed from `study_logs`, adaptive learning recommendation.
- **Module 1 (Vocabulary & SRS)**:
  - Dictionary screen: search bar, category chips (10 categories + «Мои слова» + «Избранное» + «Мои термины»), language filter, due badge, word list with pronunciation button, favorite toggle, word detail sheet, add/edit/delete custom word dialog.
  - Flashcards SRS screen: 3D flip animation, horizontal swipe gestures + 4 explicit SM-2 rating buttons (showing next calculated interval preview!), audio for term & example, session progress bar, session completion summary with XP gained.
- **Module 2 (Programming Languages)**:
  - Catalog screen: search, 6 domain filter chips (`Backend`, `Frontend`, `Mobile`, `Systems`, `Scripting`, `Data/DB`), 19 language cards with difficulty & paradigms, direct CTA to «Сравнить языки».
  - Language Detail screen: EN/RU toggle, description, history, paradigms, domains, frameworks, typing, memory management, list of practical code snippets.
  - Interactive Code Breakdown screen: syntax-highlighted code with line numbers & horizontal scrolling, interactive clickable keywords highlighting + bottom sheet (EN/RU explanation + example + «Добавить в словарь»), line-by-line EN/RU breakdown panel, completion log.
  - Dynamic Side-by-Side Comparison screen: pick any 2 of 19 languages, characteristics diff table, dynamic common `task_key` selector (strictly intersection of both languages), side-by-side highlighted code panels, EN/RU explanation switch.
- **Module 3 (Technical Documentation)**:
  - Library screen: search, 9 tech-tag filters, CEFR level badge, reading progress bars.
  - Interactive Reader screen: font size controls (`-`/`+`), EN/RU explanation toggle, TTS article playback bar (`Play`/`Pause`/`Replay`/`Stop`), word-level tappable text rendering -> bottom sheet with translation, contextual explanation, example, TTS, «Добавить в словарь» (`AddWordFromDocUseCase`), scroll/manual reading progress persistence & «Отметить прочитанным».
- **Module 4 (Work Scenarios)**:
  - Practice Hub: access to Work Scenarios, Listening Studio, Technical Documentation, and Professional Phrases Library (60+ phrases with search, filter, TTS).
  - Scenarios screen: filter across all 8 mandatory workplace types, status indicator (completed/unattempted).
  - Interactive Scenario Exercise screen: real interactive engines for all 6 exercise types (`SENTENCE_BUILD` chip builder, `TRANSLATION_CHOICE`, `PHRASE_CHOICE`, `FILL_BLANKS`, `INTERVIEW_ANSWER`, `LISTENING_CHOICE`), instant detailed feedback card (correctness, canonical answer, RU error explanation, useful expressions with TTS & save-to-vocab, retry & next).
- **Module 5 (Listening & Pronunciation)**:
  - Listening Studio: offline TTS voice status banner with one-tap voice pack installer, mode switcher («Выбор на слух» / «Диктант»), category/source filter, Media3 + TTS audio controls (`Play`, `Pause`, `Replay`, `Stop`, speed `0.75x`/`1.0x`), interactive answer check with character/word diff for dictation, XP & `study_logs` recording.

## PHASE 6 — Progress System, Gamification & WorkManager Notifications
- **Statistics Screen**: real Room-driven KPI cards (learned words, total reviews, accuracy %, streak, XP & level progress bar), interactive 7-day weekly activity bar chart, 30-day monthly activity heatmap/chart, per-category vocabulary mastery breakdown.
- **Profile & Settings Screen**: user rank & summary, Daily Goal selector (`10` / `20` / `30` cards), Theme mode (`System` / `Light` / `Dark`), Dynamic Colors toggle, Reading font size slider, Reminder Notifications toggle + time picker + runtime `POST_NOTIFICATIONS` permission flow on Android 13+ + «Тестовое уведомление» button, TTS offline voice diagnostic panel.
- **Notifications Engine**: `ReviewReminderWorker` (`HiltWorker`), `NotificationHelper` (channel creation + deep-link PendingIntent to Flashcards), `NotificationScheduler` (periodic WorkManager scheduling).

## PHASE 7 — End-to-End Integration & Offline-First Verification
- Connect all routes, deep-links, Snackbars, confirmation dialogs, and reactive Flows across modules.
- Verify adding a word from Documentation or Code Breakdown immediately updates Vocabulary & due count on Home and Flashcards.
- Verify completing a Flashcard / Scenario / Doc / Listening / Code Breakdown updates `study_logs`, Home daily goal, Streak, XP, and Statistics in real time.
- Verify zero network permissions or calls are required for all 6 modules.

## PHASE 8 — QA, Automated Testing, APK Build & Final Report
- Run full test suites:
  - `:domain:test` (SM-2, Streak, XP, Comparison, Scenarios, Listening, Docs, Vocabulary UseCases)
  - `:domain:jacocoTestReport` (verify ≥ 80% Domain Layer line coverage)
  - `:app:testDebugUnitTest` (Room database tests with Robolectric + asset DB integrity test + DAO/FK tests + ViewModel/UI tests + Navigation/Loading/Empty/Error state tests)
- Build production-ready APK (`./gradlew :app:assembleDebug` and verify APK structure/manifest/assets).
- Update `docs/REQUIREMENTS.md` traceability checkmarks and write `docs/FINAL_REPORT.md`.
