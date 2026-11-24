# Requirements & Traceability Matrix (RTM) — TechEnglish v1.0 / v2.0 / v3.0
## Project: Technical English for Developers (`com.techenglish.app`)

**Source Specifications (in priority order):**
1. **Master Prompt v3.0 (редакция 3.1), Раздел 2 — «Errata к ТЗ 3.2» (`E1`–`E6`)** (highest priority on conflicts)
2. **`docs/techenglish_v3_2_final.md`** — Техническое задание: Модернизация платформы TechEnglish до версии 3.0 (редакция 3.2 финальная)
3. **Master Prompt 2.0 & 1.0 + `Техническая_спецификация_исправленная.md`** — Baseline v1.0 / v2.0 requirements (`ФТ-1.1`…`ФТ-6.5`, `НФТ-1.1`…`НФТ-6.2`)

---

## 1. Baseline Functional Requirements v1.0 / v2.0 (`ФТ-1.1`…`ФТ-6.5`)

### Module 1 — IT Vocabulary, Academic Dictionary & Spaced Repetition System (SRS / SM-2)
| ID | Requirement | Priority | Implemented Component | Status |
|---|---|---|---|---|
| ФТ-1.1 | Structured IT vocabulary database divided into 10 thematic categories + protected system category «Мои слова» + 10,308+ Academic Technical Dictionary across 30 domains | Critical | `CategoryEntity`, `WordEntity`, `AcademicTermEntity`, `VocabularyScreen`, `AcademicDictionaryScreen` | [x] Verified (Baseline v2.0) |
| ФТ-1.2 | Word card displays English term, IPA transcription, Russian translation, English example, Russian explanation, category, optional programming language, favorite toggle, and audio button | Critical | `VocabularyScreen`, `FlashcardsScreen`, `AcademicTermDetailScreen` | [x] Verified (Baseline v2.0; redesigned in Stage 1 via `УИ-1.x`, `УИ-2.3`) |
| ФТ-1.3 | Full SM-2 algorithm in Domain Layer: `EF=2.5`, `repetitions=0`, `interval=0`; `I1=1`, `I2=6`, `In=round(I(n-1)*EF')`; ratings `q=1` (resets repetitions & interval), `q=3`, `q=4`, `q=5`; `EF >= 1.3` | Critical | `Sm2Engine`, `CalculateNextReviewUseCase`, `ReviewWordUseCase`, `ReviewAcademicTermUseCase` | [x] Verified |
| ФТ-1.4 | Offline audio pronunciation via Android `TextToSpeech` (`en-US`/`en-GB`) with missing-voice detection and voice-pack installation prompt | High | `TechAudioController`, `TtsVoiceStatusBanner` | [x] Verified |
| ФТ-1.5 | Custom dictionary: create, edit, delete user terms, search & filter, favorites | Critical | `AddCustomWordUseCase`, `UpdateCustomWordUseCase`, `DeleteCustomWordUseCase`, `SearchWordsUseCase` | [x] Verified |
| ФТ-1.6 | Interactive Flashcards mode with 3D flip animation, swipe gestures, interval preview, and automatic SM-2 scheduling (plus 6 Academic SRS modes) | Critical | `FlashcardsScreen`, `AcademicSrsScreen` | [x] Verified |

### Module 2 — Programming Languages Catalog, Code Breakdown & Comparison (`ФТ-6.1`…`ФТ-6.5`)
| ID | Requirement | Priority | Implemented Component | Status |
|---|---|---|---|---|
| ФТ-6.1 | All 19 mandatory programming languages: Python, SQL, C, C++, Java, Kotlin, C#, Go, JavaScript, Swift, Scala, Assembly, Pascal, Ruby, Dart, VBA, Bash, Lua, Rust | Critical | `ProgrammingLanguageEntity`, `LanguagesCatalogScreen` | [x] Verified |
| ФТ-6.2 | Full bilingual profile per language: description EN/RU, history EN/RU, paradigms, domains, frameworks, typing, memory management, difficulty, practical examples | Critical | `LanguageDetailScreen`, `LanguageDetailViewModel` | [x] Verified |
| ФТ-6.3 | Domain filtering across Backend, Frontend, Mobile, Systems, Scripting, Data/DB | High | `GetProgrammingLanguagesUseCase`, `LanguagesCatalogScreen` | [x] Verified |
| ФТ-6.4 | Interactive Code Breakdown: syntax highlighting (`AnnotatedString`), line numbers, line-by-line EN + RU explanations, horizontal scrolling, clickable keywords with popup & «Add to Vocabulary» | Critical | `CodeBreakdownScreen`, `CodeSyntaxHighlighter` | [x] Verified |
| ФТ-6.5 | Dynamic Side-by-Side Language Comparison: choose any 2 languages, characteristics diff table, strictly common `task_key` intersection, two highlighted code blocks side-by-side, EN/RU toggle | Critical | `LanguageComparisonScreen`, `CompareLanguagesUseCase` | [x] Verified |

### Module 3 — Technical Documentation Interactive Reader (`ФТ-2.1`…`ФТ-2.3`)
| ID | Requirement | Priority | Implemented Component | Status |
|---|---|---|---|---|
| ФТ-2.1 | Built-in library of technical documentation excerpts & error logs across 9 categories (`Programming`, `Linux`, `Git`, `SQL`, `REST API`, `HTTP`, `Networking`, `Software Engineering`, `Error Logs`) with CEFR levels, RU explanation & persisted reading progress | Critical | `DocArticleEntity`, `DocumentationListScreen` | [x] Verified (32 in v2.0; expands to ≥150 in Stage 2 via `КД-3`) |
| ФТ-2.2 | Interactive word-tap reading: tapping any English word opens translation, contextual explanation, example, TTS playback, and «Add to Vocabulary» saving directly into Room SRS | Critical | `DocReaderScreen`, `LookupWordUseCase`, `AddWordFromDocUseCase` | [x] Verified |
| ФТ-2.3 | Reading controls: font size `-`/`+` (persisted in DataStore), EN/RU guide toggle, reading progress persistence, `DOC_READ` study logs | High | `DocReaderScreen`, `UpdateReadingProgressUseCase` | [x] Verified |

### Module 4 — Work Scenarios & Professional Communication (`ФТ-3.1`…`ФТ-3.3`)
| ID | Requirement | Priority | Implemented Component | Status |
|---|---|---|---|---|
| ФТ-3.1 | Interactive trainers covering workplace contexts + professional IT English phrases | Critical | `ScenarioEntity`, `WorkPhraseEntity`, `PracticeHubScreen` | [x] Verified (36 scenarios / 8 contexts / 64 phrases in v2.0; expands to ≥150 scenarios / 12 contexts / ≥300 phrases in Stage 2 via `КД-4`) |
| ФТ-3.2 | All 6 interactive exercise types (`SENTENCE_BUILD`, `TRANSLATION_CHOICE`, `PHRASE_CHOICE`, `FILL_BLANKS`, `INTERVIEW_ANSWER`, `LISTENING_CHOICE`) | Critical | `ScenarioExerciseScreen`, `ScenarioExerciseViewModel` | [x] Verified |
| ФТ-3.3 | Immediate post-exercise feedback: correctness, canonical answer, RU error explanation, useful expressions (with TTS & Add-to-SRS), retry, next, and `study_logs` persistence | Critical | `SubmitScenarioAnswerUseCase`, `ScenarioExerciseScreen` | [x] Verified |

### Module 5 — Listening & Pronunciation (`ФТ-5.1`…`ФТ-5.3`)
| ID | Requirement | Priority | Implemented Component | Status |
|---|---|---|---|---|
| ФТ-5.1 | TTS pronunciation integrated into Vocabulary, Academic Dictionary, Documentation, and Work Scenarios | High | `TechAudioController` | [x] Verified |
| ФТ-5.2 | Dedicated Listening Studio using Android `TextToSpeech` + `Media3 ExoPlayer` with Listen & Choose and Listen & Type (dictation), Play/Pause/Replay/Stop and speed (`0.75x`/`1x`/`1.25x`) | Critical | `ListeningScreen`, `ListeningViewModel` | [x] Verified |
| ФТ-5.3 | Offline voice pack resilience: graceful notice + `ACTION_INSTALL_TTS_DATA` button without crashing when voice pack is missing | Critical | `TechAudioController`, `TtsVoiceStatusBanner` | [x] Verified |

### Module 6 — Progress Tracking, Gamification & Local Notifications (`ФТ-4.1`…`ФТ-4.3`)
| ID | Requirement | Priority | Implemented Component | Status |
|---|---|---|---|---|
| ФТ-4.1 | Real DB-driven statistics: learned words, reviewed cards, accuracy %, daily activity, 7-day weekly bar chart, 30-day monthly + 365-day yearly GitHub activity heatmap, streak from `study_logs`, XP & developer rank (`Intern` → `Principal`), category progress | Critical | `GetDailyProgressUseCase`, `GetStatisticsUseCase`, `CalculateStreakUseCase`, `CalculateXpLevelUseCase`, `StatisticsScreen`, `HomeScreen`, `ProfileScreen` | [x] Verified (expanded to 365-day graph in Stage 1 via `УИ-2.2`) |
| ФТ-4.2 | Configurable Daily Learning Goal (10 / 20 / 30 cards/day) persisted in DataStore | High | `UserPreferencesDataStore`, `ProfileScreen` | [x] Verified |
| ФТ-4.3 | WorkManager periodic local reminders for due SM-2 cards (core + academic) handling `POST_NOTIFICATIONS` on Android 13+ | High | `ReviewReminderWorker`, `NotificationScheduler` | [x] Verified |

---

## 2. Content & Database Expansion Requirements v3.0 (`КД-1`…`КД-5`)

| ID | Requirement | Target Stage | Specification & Errata Rules |
|---|---|---|---|
| **КД-1** | **New Academic Vocabulary Domains & 20,000–30,000 Verified Terms:** Expand `academic_terms` from 10,308 to 20,000–30,000 verified terms, adding 6 new domains: `GameDev`, `Web3 / Smart Contracts`, `Mobile Internals (Android / iOS)`, `QA & Testing`, `UI/UX Engineering`, `Product Management`. Every term must reference an authoritative source in `word_sources`; unverified entries (`verification_status != 'verified'`) are excluded from release DB. Delivered via `ContentPackImporter` (Errata **E3**). | Stage 2 | `tools/vocabulary_import/`, `ContentPackImporter`, `tech_english.db` v3 |
| **КД-2** | **Verified IPA (≥ 5,000) & Tajik Localization (`translation_tg` ≥ 2,000 + RU/TJ Switch):** Expand verified IPA transcriptions in `academic_terms` from 351 to ≥ 5,000 strictly from documented open-license sources (`source_url`, `license`; zero fabricated IPA). Expand `translation_tg` from 17 to ≥ 2,000 verified terms (human/source verified per Blocker 7; unverified stay `pending_review`). Implement **RU / TJ** translation toggle (Errata **E6**: use `translation_tg` when present and verified, fallback to `translation_ru`). | Stage 2 | `tools/vocabulary_import/`, `UserPreferencesDataStore` |
| **КД-3** | **Real Error Logs, RFCs & README Library (≥ 150 `doc_articles`):** Expand `doc_articles` from 32 to ≥ 150 with stack traces and crash logs (Kotlin/Android, Python, Docker, Git merge conflicts, PostgreSQL deadlocks, HTTP/CORS) and RFC/README excerpts. Every external record stores `source_url` and `license`, with all PII, tokens, and user paths scrubbed. | Stage 2 | `DocArticleEntity`, `scripts/build_seed_db.py` |
| **КД-4** | **New Work Scenario Contexts (≥ 150 scenarios, ≥ 12 per context, ≥ 300 phrases):** Add 4 new contexts: `SYSTEM_DESIGN_INTERVIEW`, `INCIDENT_POST_MORTEM`, `SALARY_NEGOTIATION`, `CLIENT_COMMUNICATION` (bound to Slack theme). Expand scenarios from 36 to ≥ 150 and `work_phrases` from 64 to ≥ 300 across all 12 contexts. | Stage 2 | `ScenarioType`, `ScenarioEntity`, `WorkPhraseEntity` |
| **КД-5** | **20 `task_key` Matrix Across 19 Languages (≥ 350 snippets):** Preserve the 10 existing keys (`arrays`, `async_ops`, `classes`, `conditions`, `file_reading`, `functions`, `hello_world`, `http_request`, `loops`, `variables`), fill matrix gaps in `async_ops`, `http_request`, `classes`, `file_reading`, and add 10 new keys: `error_handling`, `json_parsing`, `regex_matching`, `unit_testing`, `singleton_pattern`, `factory_pattern`, `string_manipulation`, `sorting`, `recursion`, `generics`. | Stage 2 | `CodeSnippetEntity`, `scripts/build_seed_db.py` |

---

## 3. UI/UX Redesign, Typography & Micro-Interactions v3.0 (`УИ-1.1`…`УИ-3.3`)

| ID | Requirement | Target Stage | Implemented Component |
|---|---|---|---|
| **УИ-1.1** | **Font Pair «UI + Code» (Local `res/font`):** Bundle **Inter** (UI & prose) and **JetBrains Mono** (English terms, IPA, tech tags, numbers, code) locally in `app/src/main/res/font/` (no Downloadable Fonts; Plus Jakarta Sans excluded). Must cover Latin, Cyrillic, IPA, and Tajik letters (`ғ, ӣ, қ, ӯ, ҳ, ҷ`). Display OFL font licenses in «О приложении». | Stage 1 | `res/font/`, `Theme.kt`, `ProfileScreen.kt` |
| **УИ-1.2** | **Syntax Highlighting for English Sentences & Semantic Badges:** Highlight the target term in usage examples with an accent color while dimming surrounding context (contrast ≥ 4.5:1 WCAG AA). Color-coded badges for Part of Speech and Difficulty (`Beginner` soft green .. `Research Level` neon purple, AA contrast). | Stage 1 | `Components.kt` (`HighlightedExampleQuoteCard`, `DifficultyBadge`, `PartOfSpeechBadge`) |
| **УИ-1.3** | **Visual Hierarchy & Breathing Room:** Large bold term heading with generous bottom spacing, compact muted IPA transcription, comfortable line-height (`1.4–1.5x` font size) in definitions and documentation articles. | Stage 1 | `Theme.kt`, `AcademicScreens.kt`, `DocumentationScreens.kt`, `VocabularyScreen.kt` |
| **УИ-1.4** | **Distinct Visual Content Containers:** Separate translation, usage example, and practical explanation into stylistically distinct containers (usage example styled as a quote card with a colored vertical left accent bar). | Stage 1 | `Components.kt`, `AcademicScreens.kt`, `VocabularyScreen.kt`, `FlashcardsScreen.kt` |
| **УИ-2.1** | **Iconic IDE Themes Collection:** Add 5 developer themes alongside Light, Dark, and Dynamic Colors: **Tokyo Night**, **Dracula**, **GitHub Dark**, **Nord**, **Gruvbox**. Every theme passes automated WCAG AA contrast checks (≥ 4.5:1). | Stage 1 | `ThemeMode`, `Theme.kt`, `ProfileScreen.kt` |
| **УИ-2.2** | **365-Day GitHub Contribution Graph:** Render daily learning activity over 365 days (`study_logs`) as a GitHub-style contribution grid on Home, Profile, and Statistics screens with 5 intensity levels (`0..4`). | Stage 1 | `GitHubContributionGraph` in `Components.kt`, `GetStatisticsUseCase`, `HomeScreen.kt`, `ProfileScreen.kt` |
| **УИ-2.3** | **Progressive Disclosure for Academic Term Cards:** First screen displays core essentials (*Term → Translation → Short Example → Audio*), while deep academic/practical explanations, polysemous senses (`word_senses`), authoritative sources (`word_sources`), and related terms (`word_relations`) are organized in tabs / expandable sections. | Stage 1 | `AcademicScreens.kt` |
| **УИ-2.4** | **Chameleon Interface in Work Scenarios & Logs:** Extensible scenario-type-to-theme registry (`ScenarioChameleonRegistry`): `CODE_REVIEW` / `PR_COMMENTS` → **GitHub PR** container; `DAILY_STANDUP` / `TEAM_COMMUNICATION` (and `CLIENT_COMMUNICATION` in Stage 2) → **Slack** container; `BUG_REPORTING` / `GITHUB_ISSUES` / `Error Logs` (and `Debug the Log` in Stage 2) → **Terminal** container. | Stage 1 (core), Stage 2 (new contexts) | `ScenarioChameleonRegistry`, `ChameleonContainers` in `Components.kt`, `ScenariosScreens.kt`, `DocumentationScreens.kt` |
| **УИ-3.1** | **Haptic Feedback:** Subtle vibration on flashcard flip, swipe, correct answer, and tapping a word in documentation/code, controlled by a DataStore toggle (`hapticFeedbackEnabled`). | Stage 1 | `TechHaptics` in `Components.kt`, `UserPreferencesDataStore.kt`, `ProfileScreen.kt` |
| **УИ-3.2** | **Combo Streaks & Developer Humor:** At 3, 5, 8, and 10+ consecutive correct answers, display a glowing counter and developer feedback (*«LGTM!»*, *«Clean build!»*, *«Zero warnings!»*, *«Merged to main!»*). | Stage 1 | `ComboStreakTracker`, `ComboBanner`, `FlashcardsScreen.kt`, `AcademicScreens.kt`, `ScenariosScreens.kt` |
| **УИ-3.3** | **Signature Developer UI Details:** Blinking terminal cursor (`_`) in search bars, thematic loading statuses (*«Compiling vocabulary...»*, *«Resolving dependencies...»*), and dynamic accent tinting from the active programming language's `accentColorHex`. Animations disable automatically when system/app `reduceMotion` is active. | Stage 1 | `TerminalSearchField`, `SkeletonListPlaceholder`, `ProgrammingScreens.kt` |

---

## 4. New Functional Requirements v3.0 (`ФТ-7.1`…`ФТ-14.5`)

### 4.1. New Offline Learning Tools (`ФТ-7.1`…`ФТ-7.6` — Stage 2)
| ID | Requirement | Target Stage | Notes |
|---|---|---|---|
| **ФТ-7.1** | **Read Your Own Docs:** Paste English text or import `.md`/`.txt`/README via SAF; off-main-thread scanning against 10,308–30,000 terms supporting inflected forms and multi-word terms; tap to view translation and add to SRS. Stored in `custom_imported_docs` (`MIGRATION_2_3`). | Stage 2 | Offline-First |
| **ФТ-7.2** | **Interactive «Debug the Log» Trainer:** Analyze real/synthetic English stack traces & logs (Gradle, Linux, SQL, Docker, Python), identify root cause, and assemble/select a bug report for Jira / GitHub Issues; styled in Terminal Chameleon theme. | Stage 2 | Offline-First |
| **ФТ-7.3** | **Speaking / Shadowing Pronunciation Trainer:** Android `SpeechRecognizer` strictly on-device (`createOnDeviceSpeechRecognizer`, API 33+, `isOnDeviceRecognitionAvailable`). On API < 33 or missing offline pack, gracefully shows status «Нужен голосовой пакет или сеть». Requests `RECORD_AUDIO` on first entry; compares spoken phrase/term with reference and highlights mispronounced words. | Stage 2 | Offline-First with graceful degradation |
| **ФТ-7.4** | **Home Screen Widget (Jetpack Glance):** Displays «Word of the Day» (term, IPA, translation), due cards count, and current Streak with 1-tap launch into review. | Stage 2 | Offline-First |
| **ФТ-7.5** | **Time Attack / Speedrun Quiz:** 60-second rapid session (term translation or PR error spotting) awarding XP and saving personal bests in `time_attack_records` (`MIGRATION_2_3`). | Stage 2 | Offline-First |
| **ФТ-7.6** | **Deck Import & Export (JSON / CSV / Anki):** Local export/import of user progress and «Мои слова» in JSON/CSV; import third-party decks from JSON, CSV, and Anki (`.apkg` / TSV — importing card fields and tags only, without Anki scheduling). | Stage 2 | Offline-First |

### 4.2. Cloud Backend, Auth, Sync, Anti-Cheat & Social Core (`ФТ-9.1`…`ФТ-12.3`, `ФТ-14.5` — Stage 3)
| ID | Requirement | Target Stage | Notes & Errata |
|---|---|---|---|
| **ФТ-9.1** | **Backend & Authentication:** FastAPI + PostgreSQL + Redis over HTTPS (TLS 1.2+). GitHub OAuth & Google OAuth (Custom Tabs + PKCE), Email/password (verification + reset), and Guest Mode with seamless account linking (`local_account_link` in `MIGRATION_3_4`). | Stage 3 | ADR in `docs/DECISIONS.md`; no Supabase |
| **ФТ-9.2** | **Bidirectional Sync Engine (Outbox + Conflict Resolution, правка R10 / Errata E1):** Offline actions recorded in `sync_queue` with UUID `event_id` (idempotent; `PENDING` state only in `sync_queue`). WorkManager pushes `events`, `sm2_updates`, `favorite_terms`, `custom_words` and pulls `GET /v1/sync/state` with Ktor `Auth` automatic JWT refresh on `401` and `HttpTimeout`. **R10 / Errata E1 SM-2 Conflict Rule (отменяет и заменяет формулировку `ФТ-9.2` в `docs/techenglish_v3_2_final.md` о приоритете серверного `last_studied_at`):** сравнивается `client_timestamp` события (с защитой от дат в будущем, допуск по умолчанию 5 минут), затем `repetitions`, затем порядок приёма сервером (`server_received_at`). `study_logs` append-only; favorites and «Мои слова» merge by set union; XP по-прежнему вычисляет сервер. Implemented and unit-tested in `:domain`. | Stage 3 / v3.1 (R10) | **R10 / Errata E1** overrides `techenglish_v3_2_final.md` §5.1 / `ФТ-9.2` |
| **ФТ-9.3** | **Server-Side Anti-Cheat:** Server calculates ranked XP from validated events (per-minute/per-day rate limits, session interval checks); ranked quiz questions/answers issued by server; client integrity via Play Integrity API interface (Blocker 3). | Stage 3 | Logic in `:domain` + `backend/` |
| **ФТ-9.4** | **Privacy, 16+ Age Gate & Profile Moderation:** First-launch 16+ age confirmation screen (including Guest Mode); Privacy Policy; account & data deletion in-app and via web form. **Errata E2:** Report user, block user, moderation queue, and `@username` filter implemented in **Stage 3** for profiles, and extended to comments/decks/rooms/Peer Review in Stage 4. | Stage 3 & Stage 4 | Errata **E2** |
| **ФТ-10.1–10.3** | **Developer Profile Card, Ranks & Badges:** Preset/OAuth avatar (no custom file upload per **E2**), unique `@username`, status, tech stack, GitHub/Telegram links, rank badge (`Intern` → `Principal`), streak, 365-day contribution graph, domain mastery %, and Badges showcase. | Stage 3 | Cached in `user_profile_cache`, `badges_cache` |
| **ФТ-11.1–11.2** | **Segmented Top-10 Leaderboards & Weekly Leagues:** 4 tabs: Weekly Top-10 (resets Monday 00:00 `Asia/Dushanbe`, UTC+5), League Top-10 (`Bronze → Silver → Gold → Platinum → Diamond`, cohorts of 30, Top-3 promoted, bottom-3 demoted, separate from permanent XP ranks), Friends Top-10, Tech Stack Top-10. Pinned user position row; zero bots. | Stage 3 | Cached in `leaderboard_cache` |
| **ФТ-12.1–12.3** | **Friends System, Activity Feed & Ping/Nudge:** Search by `@username`, QR code & deep-link invite; Friends Activity Feed with GitHub/Slack reactions; «Пнуть» (Ping) via FCM (max 1/day per friend, opt-out toggle). | Stage 3 | Cached in `friends_cache`, `activity_feed_cache` |
| **ФТ-14.5** | **Dynamic OTA Content Delivery:** Background delivery of new doc articles, scenarios, and weekly challenges into Room via `ContentPackImporter` (Errata **E3**, **E4**); data-only JSON/content packs verified by signature/SHA-256 checksum (zero executable code). Serves as fallback for non-Play devices. | Stage 3 | `ota_content_meta` |

### 4.3. Community, AI Tutor & Realtime Mechanics (`ФТ-8.x`, `ФТ-13.x`, `ФТ-14.1`–`14.4` — Stages 4A, 4B, 4C)
| ID | Requirement | Target Stage | Notes & Errata |
|---|---|---|---|
| **ФТ-13.1** | **StackOverflow-Style Comments & Mnemonics:** Under any dictionary term or code snippet (`target_type = TERM | SNIPPET` in `comments_cache`, `MIGRATION_4_5`), with upvote/downvote, top-answer sorting, reporting, and author blocking. | Stage 4A | Wave 4A |
| **ФТ-13.2** | **Community Decks Exchange:** Publish custom decks from «Мои слова» with tags and ratings; 1-tap download into local Room (`community_decks`, `community_deck_terms`) for offline study; reporting & moderation. | Stage 4A | Wave 4A |
| **ФТ-8.1–8.3** | **AI Mock Interviewer, AI Code/PR Reviewer & Privacy Limits:** Gemini API accessed strictly via FastAPI backend proxy (Blocker 4); Junior/Middle/Senior interview simulator (text & on-device STT voice) and PR comment reviewer; server-side daily quotas, explicit first-use consent dialog, no audio retention, zero profile PII sent to LLM. | Stage 4B | Wave 4B |
| **ФТ-14.3** | **Vocabulary Crowdsourcing («Pull Requests to DB»):** Eligible users (Rank ≥ Middle, account age ≥ 14 days, ≥ 100 learned terms, 0 confirmed reports) propose verified IPA, RU/TJ translations, examples, or fixes; community voting + moderation; awards bonus XP, **Contributor** badge, and displays project content license notice on submission. | Stage 4B | Wave 4B |
| **ФТ-14.4** | **Study Squads (Clans):** Teams of 5–10 users with a shared weekly XP goal; completing the goal grants a next-week XP multiplier (multiplier applies to personal level XP only and never affects ranked leaderboard XP per `ФТ-9.3`). | Stage 4B | Wave 4B |
| **ФТ-13.3** | **Thematic Rooms:** Channels `#code-review-practice`, `#daily-standup`, `#interviews`, `#backend-english` powered by Ktor WebSockets + Redis (`room_messages_cache`) with spam reporting and user blocking. | Stage 4C | Wave 4C |
| **ФТ-14.1** | **1v1 Realtime PvP Duels:** 60-second WebSocket match of 7–10 server-issued questions against a friend or same-tier developer; graceful disconnect handling; falls back to an unranked practice round after 15s matchmaking timeout. | Stage 4C | Wave 4C |
| **ФТ-14.2** | **Practice Arena (Peer Review):** Daily server prompt; users submit text or up to 20s voice note stored in S3-compatible object storage (MinIO in Docker Compose per Errata **E5** ADR) with strict Privacy Policy TTL; community critiques and upvotes. | Stage 4C | Wave 4C (Errata **E5**) |

---

## 5. Non-Functional Requirements (`НФТ-1.1`…`НФТ-6.2` & `НФТ-7.1`…`НФТ-7.7`)

### 5.1. Baseline Non-Functional Requirements (Updated for v3.0)
| ID | Requirement | v3.0 Status & Superseding Rule |
|---|---|---|
| **НФТ-1.1** | Cold start ≤ 2s repeat, ≤ 5s first launch | **Updated by `НФТ-7.6`:** In v1.0, `НФТ-1.1` was verified on a 796 KB seed DB, whereas the actual v2.0 DB is **42.84 MB** (10,308 academic + 255 core terms). Re-verification on the full asset DB is tracked in `НФТ-7.6`. |
| **НФТ-1.2** | Smooth 60 FPS animations | Active; extended by `НФТ-7.2` and `НФТ-7.5` (`reduceMotion` support). |
| **НФТ-1.3** | Local DB query response ≤ 100 ms | Active; verified `< 100 ms` up to 30,000 records (`НФТ-7.2`). |
| **НФТ-2.1** | ~~Strict Offline-only without `INTERNET` permission~~ | **CANCELLED in v3.0 (Отменено):** Replaced by **`НФТ-7.1`** (Hybrid Offline-First + Cloud Sync) and **`НФТ-7.3`**. |
| **НФТ-2.2** | Data integrity across kills/reboots & lossless migrations | Active (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5`). |
| **НФТ-2.3** | Graceful error handling without crashes | Active. |
| **НФТ-3.1** | Minimum Android 8.0 (API Level 26), target API 35 | Active. |
| **НФТ-3.2** | Adaptive portrait layouts across smartphones & tablets | Active. |
| **НФТ-4.1** | Material Design 3 | Active. |
| **НФТ-4.2** | Light, Dark, Dynamic Colors + 5 IDE Themes | Extended by `УИ-2.1` and `НФТ-7.5`. |
| **НФТ-4.3** | Access to flashcard review in ≤ 2 taps from Home | Active (1-tap CTA on Home & Glance Widget in Stage 2). |
| **НФТ-5.1** | Minimal permissions | **UPDATED by `НФТ-7.7`:** Stage 1–2 keep offline permissions (`POST_NOTIFICATIONS`, plus `RECORD_AUDIO` in Stage 2 for on-device STT); Stage 3 adds `INTERNET`, `ACCESS_NETWORK_STATE`, and on-demand Camera for QR. |
| **НФТ-5.2** | ~~HTTPS/TLS 1.2+ marked N/A in Offline MVP~~ | **CANCELLED in v3.0 (Отменено):** Replaced by **`НФТ-7.3`** (mandatory HTTPS TLS 1.2+, Keystore + Tink/DataStore). |
| **НФТ-6.1** | Kotlin + Clean Architecture (`:domain` pure JVM) + MVVM + UDF | Active (`НФТ-7.4`). |
| **НФТ-6.2** | Domain Layer unit test coverage | Upgraded from ≥ 80% to **≥ 90%** by **`НФТ-7.4`** (CSV == XML == HTML). |

### 5.2. v3.0 Non-Functional Requirements (`НФТ-7.1`…`НФТ-7.7`)
| ID | Requirement | Target Stage | Verification Method |
|---|---|---|---|
| **НФТ-7.1** | **Offline-First Resilience:** All v2.0 modules and offline features `ФТ-7.1`, `ФТ-7.2`, `ФТ-7.4`–`ФТ-7.6` work 100% offline. `ФТ-7.3` uses on-device STT (API 33+) and degrades gracefully with a clear status otherwise. Online screens display the last local Room cache with an «Офлайн-режим» badge when offline. | Stages 1–4 | Unit & Robolectric UI tests in offline state |
| **НФТ-7.2** | **Performance at 30,000+ Records:** Indexed search and pagination in Room `< 100 ms`; animations across themes, chameleon scenarios, and duels maintain smooth frame pacing. | Stages 1–4 | `TechEnglishDatabaseTest` 30k benchmark + frame/jank check |
| **НФТ-7.3** | **Security:** All network requests over HTTPS (TLS 1.2+); JWT (Access + Refresh) encrypted via Android Keystore + Tink/DataStore (`EncryptedSharedPreferences` forbidden); Gemini API key stored exclusively on the backend server. Replaces `НФТ-2.1` and `НФТ-5.2`. | Stages 3–4 | Security unit tests & static repo audit |
| **НФТ-7.4** | **Testing & Coverage:** `:domain` remains pure Kotlin JVM with JaCoCo line coverage **≥ 90%** (`CSV == XML == HTML`). Sync conflict resolution (Errata **E1**) and Anti-Cheat rules reside in `:domain`. Every Room migration (`1→2`, `2→3`, `3→4`, `4→5`) and `ContentPackImporter` (Errata **E3**) covered by automated lossless tests. | Stages 1–4 | `./gradlew :domain:jacocoTestReport :app:testDebugUnitTest` & `pytest` |
| **НФТ-7.5** | **Accessibility:** Text contrast **≥ 4.5:1 (WCAG AA)** across all 7 themes (`Light`, `Dark`, `Tokyo Night`, `Dracula`, `GitHub Dark`, `Nord`, `Gruvbox`), font scaling up to `200%` (`fontScale = 2.0f`), meaningful `contentDescription` for TalkBack, and automatic animation disabling when system/app `reduceMotion` is active. | Stage 1 | Automated WCAG AA contrast test + Compose accessibility & `200%` fontScale test |
| **НФТ-7.6** | **Install Size & Cold First Launch:** Base v2.0 content in AAB ≤ 100 MB download size (`bundletool get-size`); expansion to 30,000 terms via Play Asset Delivery domain packs + `ContentPackImporter` (and OTA fallback in Stage 3 per Errata **E4**). Cold first launch ≤ 5s on real 42.84 MB+ database. | Stage 2 & Stage 3 | `bundletool get-size total` on `.aab` + cold start measurement |
| **НФТ-7.7** | **Permissions Hygiene:** Manifest contains only required permissions (`POST_NOTIFICATIONS` in Stage 1; `RECORD_AUDIO` in Stage 2; `INTERNET`, `ACCESS_NETWORK_STATE`, and on-demand `CAMERA` in Stage 3). Updates `НФТ-5.1`. | Stages 1–3 | Manifest inspection & `aapt2 dump permissions` |
