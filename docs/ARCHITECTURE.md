# Software Architecture Specification
## Project: Technical English for Developers — v3.0.0 (Hybrid Offline-First + Cloud/Realtime)

---

## 0. v3.0.0 Hybrid Architecture Evolution Summary

TechEnglish **v3.0.0** preserves the 100% **Offline-First** Clean Architecture baseline of v2.0 (`:domain` pure Kotlin JVM + `:app` Android Room/Compose/Hilt/WorkManager/Media3/Glance) and extends it across Stages 1–4C with an optional Cloud, Community, AI Proxy, and Realtime layer:

- **Database Schema Evolution (Room v1 → v5, `exportSchema = true`):**
  - `1.json` → `2.json` (`MIGRATION_1_2`): Academic Dictionary (`academic_terms`, `word_senses`, `word_relations`, `word_sources`, `subcategories`).
  - `2.json` → `3.json` (`MIGRATION_2_3`): `custom_imported_docs`, `time_attack_records`, `ipa_source_url`/`ipa_license`, `source_url`/`license` on docs, + idempotent `ContentPackImporter` (Errata E3).
  - `3.json` → `4.json` (`MIGRATION_3_4`): 8 Cloud/Sync/Social/OTA tables (`sync_queue`, `user_profile_cache`, `local_account_link`, `leaderboard_cache`, `friends_cache`, `activity_feed_cache`, `badges_cache`, `ota_content_meta`).
  - `4.json` → `5.json` (`MIGRATION_4_5`): 4 Community/Realtime tables (`comments_cache`, `community_decks`, `community_deck_terms`, `room_messages_cache`).
  - Final Room v5 seed DB: `app/src/main/assets/databases/tech_english.db` (`identityHash: cd74f367098fec999ef10e8f6c1c737f`, 27 tables, 31 verified indexes, `20,658` verified academic terms, `8,048` licensed IPA, `166` docs, `156` scenarios, `312` phrases, `380` code snippets).
- **Single Android Network Stack (§6.1, `DEC-09`):** **Ktor Client `2.3.12`** (`ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-client-websockets`, `ktor-serialization-kotlinx-json`) — zero Retrofit, zero Supabase.
- **Security (`DEC-12`, `НФТ-7.3`):** `AndroidKeystoreTokenStore` (hardware-backed `AndroidKeyStore` AES-256-GCM + DataStore, no deprecated `EncryptedSharedPreferences`), `network_security_config.xml` (`cleartextTrafficPermitted="false"`), zero client-side AI API keys.
- **Backend (`backend/`, `DEC-09`, `DEC-13`):** FastAPI + SQLAlchemy 2.0 + PostgreSQL 16 + Redis 7 + MinIO S3 voice object storage (`docker-compose.yml`), REST + WebSockets (`/ws/rooms/{slug}`, `/ws/pvp/{match_id}`), server-side Anti-Cheat (`anti_cheat.py`), server-side AI proxy (`ai_proxy.py`), and 30-day TTL voice clip storage (`voice_storage.py`) with hard deletion on `DELETE /v1/account`.
- **Android UI Features (v3.0.0):**
  - `feature/stage2/`: Read Your Own Docs, Debug the Log, Pronunciation Trainer, Time Attack, Deck Import/Export, Glance Home Widget (`core/widget/`).
  - `feature/stage3/`: 16+ Age Gate, Cloud Account/Sync/OTA, Developer Profile Card & Badges, Top-10 Leaderboards (4 tabs + offline cache), Friends/QR/Feed/Ping.
  - `feature/stage4a/`: StackOverflow Comments & Mnemonics (`TERM`/`SNIPPET`), Community Decks (publish & 1-tap Room download) + UGC moderation.
  - `feature/stage4b/`: AI Mentor (Mock Interviewer + Code/PR Reviewer via FastAPI proxy), Squads (5–10 devs, non-ranked multiplier), Dictionary Crowdsourcing PRs (4-gate eligibility + Contributor badge).
  - `feature/stage4c/`: Thematic Rooms (Ktor WebSockets), 1v1 PvP Duels (8 server questions, 15s training fallback, disconnect handling), Peer Review Arena (daily case + ≤20s voice with 30d TTL).

---

## 1. Architectural Style

The application follows **Clean Architecture** combined with **MVVM (Model-View-ViewModel)** and **Unidirectional Data Flow (UDF)**.

### Dependency Rule (Compiler-Enforced)
```text
+----------------------------------------------------------------------+
|                         :app — Presentation                          |
|   Jetpack Compose (Material 3), Navigation Compose, ViewModels,      |
|   Immutable UiState (StateFlow), UiEvent (SharedFlow/Channel)        |
+----------------------------------+-----------------------------------+
                                   | depends on
                                   v
+----------------------------------------------------------------------+
|                   :domain — Pure Kotlin JVM Module                   |
|   Domain Models, Repository Interfaces, SM-2 Algorithm Engine,       |
|   Business Rules, UseCases (ZERO Android SDK dependencies)           |
+----------------------------------+-----------------------------------+
                                   ^ implements interfaces
                                   |
+----------------------------------------------------------------------+
|                          :app — Data & Core                          |
|   Room Database (SQLite + createFromAsset), DAOs, Entities, Mappers, |
|   Preferences DataStore, Android TextToSpeech, Media3 ExoPlayer,     |
|   WorkManager (ReviewReminderWorker), Hilt DI Modules                |
+----------------------------------------------------------------------+
```

---

## 2. Project Module & Package Structure

```text
TechEnglish/
├── domain/                                  # Pure Kotlin JVM Gradle module
│   └── src/main/kotlin/com/techenglish/domain/
│       ├── algorithm/                       # Sm2Engine
│       ├── model/                           # Immutable domain models
│       ├── repository/                      # Repository interfaces
│       └── usecase/                         # Single-responsibility UseCases
├── app/                                     # Android Application module
│   ├── schemas/                             # Exported Room JSON schemas (1.json, 2.json)
│   └── src/main/
│       ├── assets/databases/tech_english.db # Pre-populated offline SQLite DB (v2, 10,308+ academic terms)
│       └── kotlin/com/techenglish/app/
│           ├── core/
│           │   ├── common/                  # TimeProvider, CoroutineDispatchers, TextUtils
│           │   ├── database/                # TechEnglishDatabase (v2), Entities, DAOs, MIGRATION_1_2
│           │   ├── datastore/               # UserPreferencesDataStore
│           │   ├── designsystem/            # M3 Theme, Colors, Typography, Components, SyntaxHighlighter
│           │   ├── navigation/              # AppDestination, BottomNavItem, TechEnglishNavHost
│           │   ├── audio/                   # TechAudioController (TTS en-US/en-GB + Media3 ExoPlayer)
│           │   └── notifications/           # ReviewReminderWorker, NotificationHelper, NotificationScheduler
│           ├── data/
│           │   ├── mapper/                  # Entity <-> Domain mappers
│           │   └── repository/              # Repository implementations (Core + AcademicVocabularyRepository)
│           ├── di/                          # Hilt DI modules
│           └── feature/
│               ├── home/                    # Dashboard, Academic Dictionary CTA & daily summary
│               ├── vocabulary/              # Core Dictionary, Custom Words, Flashcards SRS
│               ├── academic/                # Academic Technical Dictionary (10,308+ terms, Detail, 6-mode SRS & Quiz)
│               ├── programming/             # 19 Languages Catalog, Detail, Code Breakdown, Side-by-Side Comparison
│               ├── documentation/           # Docs Library & Interactive Word-Tap Reader
│               ├── scenarios/               # Work Scenarios, 6 Exercise Types & 60+ Phrases
│               ├── listening/               # Listening & Pronunciation Studio (TTS + Media3)
│               ├── statistics/              # Analytics, Charts, Streak, Category Mastery
│               └── profile/                 # Profile & Settings (Goals, Theme, Notifications, TTS)
├── tools/
│   └── vocabulary_import/                   # Academic Vocabulary Import, Validation & Statistics Pipeline
│       ├── vocabulary_schema.json
│       ├── sources_and_taxonomy.py
│       ├── lexicon_domains_a_e.py
│       ├── lexicon_domains_f_j.py
│       ├── corpus_builder.py
│       ├── vocabulary_importer.py
│       ├── vocabulary_validator.py
│       └── vocabulary_statistics.py
├── scripts/
│   └── build_seed_db.py                     # Deterministic Room v2 SQLite seed generator
└── docs/
    ├── REQUIREMENTS.md
    ├── REQUIREMENTS_TRACEABILITY.md
    ├── UPGRADE_AUDIT.md
    ├── UPGRADE_PLAN.md
    ├── VOCABULARY_QUALITY_REPORT.md
    ├── IMPLEMENTATION_PLAN.md
    ├── ARCHITECTURE.md
    ├── DECISIONS.md
    └── FINAL_REPORT.md
```

---

## 3. Room / SQLite Database Schema (ER Model — Version 2)

Database name: `tech_english.db`, Version: `2` (`identityHash = 8170a848655f927ea61bfbb1ab7cf233`), `exportSchema = true`, pre-populated via `createFromAsset("databases/tech_english.db")` with lossless `MIGRATION_1_2`.

### 3.1 Core Modules Schema (v1 -> v2 Preserved)

```text
+-----------------------+        1   N       +-------------------------------+
|      categories       |------------------->|             words             |
+-----------------------+                    +-------------------------------+
| PK id : Int           |                    | PK id : Int (AutoInc)         |
|    name : Text (UNIQ) |                    | FK category_id : Int CASCADE  |
|    icon : Text        |                    | FK language_id : Int? SETNULL |
|    is_system : Int    |                    |    term : Text                |
+-----------------------+                    |    transcription : Text       |
                                             |    translation : Text         |
+-----------------------+        1   N       |    example : Text             |
| programming_languages |--------+---------->|    explanation_ru : Text      |
+-----------------------+        |           |    is_favorite : Int          |
| PK id : Int           |        |           |    is_custom : Int            |
|    slug : Text (UNIQ) |        |           |    repetitions : Int (0)      |
|    name : Text (UNIQ) |        |           |    ease_factor : Real (2.5)   |
|    description_en/ru  |        |           |    interval_days : Int (0)    |
|    history_en/ru      |        |           |    next_review_at : Int (0)   |
|    paradigms, domains |        |           +---------------+---------------+
|    frameworks, typing |        | 1                         | 1
|    memory_management  |        | N                         | N
+-----------------------+        v                           v
                           +-------------------+   +-------------------------+
+-----------------------+  |   code_snippets   |   |       study_logs        |
|     doc_articles      |  +-------------------+   +-------------------------+
+-----------------------+  | PK id : Int       |   | PK id : Int (AutoInc)   |
| PK id : Int           |  | FK language_id    |<--| FK snippet_id? SETNULL  |
|    title, tech_tag    |  |    task_key       |   | FK word_id? SETNULL     |
|    content, level     |  |    code, expl...  |   | FK scenario_id? SETNULL |
|    explanation_ru     |  +-------------------+   | FK article_id? SETNULL  |
|    reading_progress   |                          |    action_type : Text   |
|    is_completed : Int |  +-------------------+   |    is_correct : Int     |
|    glossary_json      |  |     scenarios     |-->|    xp_earned : Int      |
+-----------------------+  +-------------------+   |    timestamp : Int      |
                           | PK id : Int       |   +-------------------------+
+-----------------------+  |    type, ex_type  |
|     work_phrases      |  |    title, prompt  |
+-----------------------+  |    solution, ...  |
| PK id : Int           |  +-------------------+
|    category, phrase   |
+-----------------------+
```

### 3.2 Academic Technical Dictionary Schema (v2 Expansion — 10,308+ Verified Terms, 30,000+ Ready)

```text
+---------------------------+                +-----------------------------------+
|       subcategories       |                |           word_sources            |
+---------------------------+                +-----------------------------------+
| PK id : Int               |                | PK id : Int                       |
| FK category_id : Int      |                |    title : Text                   |
|    name : Text            |                |    url : Text                     |
|    slug : Text (UNIQ)     |                |    source_type : Text             |
|    domain : Text          |                |    publisher : Text               |
+-------------+-------------+                +-----------------+-----------------+
              | 1                                              | 1
              | N                                              | N
              v                                                v
+--------------------------------------------------------------------------------+
|                                 academic_terms                                 |
+--------------------------------------------------------------------------------+
| PK id : Int (AutoInc)               |    example_en : Text                     |
|    term : Text                      |    example_ru : Text                     |
|    normalized_term : Text (UNIQ)    |    major_category : Text                 |
|    phonetic_transcription : Text    | FK category_id : Int                     |
|    part_of_speech : Text            | FK subcategory_id : Int? SET_NULL        |
|    translation_ru : Text            |    domain : Text                         |
|    translation_tg : Text            |    subdomain : Text                      |
|    definition_en : Text             |    difficulty_level : Text               |
|    definition_ru : Text             |    cefr_level : Text                     |
|    academic_explanation_en : Text   |    synonyms : Text                       |
|    academic_explanation_ru : Text   |    related_terms : Text                  |
|    practical_explanation : Text     |    abbreviations : Text                  |
|    source_reference : Text          | FK source_id : Int? SET_NULL             |
|    verification_status : Text       |    is_favorite, is_in_srs : Int          |
|    repetitions : Int (0)            |    ease_factor : Real (2.5)              |
|    interval_days : Int (0)          |    next_review_at, last_studied_at : Long|
|    created_at, updated_at : Long    |                                          |
+-----------------+-------------------+--------------------+---------------------+
                  | 1                                      | 1
                  | N                                      | N
                  v                                        v
+-------------------------------------+  +---------------------------------------+
|             word_senses             |  |            word_relations             |
+-------------------------------------+  +---------------------------------------+
| PK id : Int (AutoInc)               |  | PK id : Int (AutoInc)                 |
| FK term_id : Int CASCADE            |  | FK term_id : Int CASCADE              |
|    sense_number : Int               |  |    related_term : Text                |
|    sense_label : Text               |  |    relation_type : Text               |
|    domain : Text                    |  +---------------------------------------+
|    definition_en : Text             |
|    definition_ru : Text             |
|    example_en / example_ru : Text   |
+-------------------------------------+
```

### Mandatory SQLite Indexes (24 Named + 3 Unique = 27 Total)
**Core v1 Indexes (10 Named + 3 Unique):**
1. `index_categories_name` (UNIQUE) on `categories(name)`
2. `index_programming_languages_slug` (UNIQUE) on `programming_languages(slug)`
3. `index_programming_languages_name` (UNIQUE) on `programming_languages(name)`
4. `idx_words_next_review` on `words(next_review_at)`
5. `idx_words_category` on `words(category_id)`
6. `idx_words_language` on `words(language_id)`
7. `idx_words_term_lang` on `words(term, language_id)`
8. `idx_snippets_lang_task` on `code_snippets(language_id, task_key)`
9. `idx_logs_timestamp` on `study_logs(timestamp)`
10. `idx_logs_word` on `study_logs(word_id)`
11. `idx_logs_scenario` on `study_logs(scenario_id)`
12. `idx_logs_article` on `study_logs(article_id)`
13. `idx_logs_snippet` on `study_logs(snippet_id)`

**Academic v2 Indexes (14 Named):**
14. `idx_subcategories_slug` (UNIQUE) on `subcategories(slug)`
15. `idx_subcategories_category` on `subcategories(category_id)`
16. `idx_academic_normalized_term` (UNIQUE) on `academic_terms(normalized_term)`
17. `idx_academic_category_id` on `academic_terms(category_id)`
18. `idx_academic_subcategory_id` on `academic_terms(subcategory_id)`
19. `idx_academic_difficulty` on `academic_terms(difficulty_level)`
20. `idx_academic_cefr` on `academic_terms(cefr_level)`
21. `idx_academic_next_review` on `academic_terms(next_review_at)`
22. `idx_academic_source_id` on `academic_terms(source_id)`
23. `idx_academic_domain` on `academic_terms(domain, subdomain)`
24. `idx_academic_status` on `academic_terms(verification_status)`
25. `idx_academic_srs_due` on `academic_terms(is_in_srs, next_review_at)`
26. `idx_senses_term_id` on `word_senses(term_id)`
27. `idx_relations_term_id` on `word_relations(term_id)`

---

## 4. Navigation Architecture

### Primary Bottom Navigation Bar (5 Tabs)
1. **Главная** (`home`) — Dashboard, Daily Goal, Streak, XP/Level, Quick Start Review (1 tap), Module Shortcuts, Recent Activity, Smart Recommendation.
2. **Словарь** (`vocabulary`) — IT Vocabulary Explorer, Search, Category/Language/Favorites filters, Custom Words CRUD, launch SRS Flashcards.
3. **Языки** (`languages`) — 19 Programming Languages Catalog, Domain filters, Language Detail, Interactive Code Breakdown, Side-by-Side Comparison.
4. **Практика** (`practice`) — Hub for Work Scenarios (8 contexts, 6 exercise types), Professional Phrases Library, Technical Documentation Reader, and Listening & Pronunciation Studio.
5. **Профиль** (`profile`) — Developer Level Profile, Statistics preview & full Statistics screen, Daily Goal, Theme/Dynamic Colors, Reading Font Size, Notification Settings, TTS Offline Voice Check.

---

## 5. Audio & Offline TTS + Media3 Pipeline

1. **TtsManager** initializes Android `TextToSpeech` lazily on startup/first use, inspects installed `Locale.US`/`Locale.UK` voices for `!voice.isNetworkConnectionRequired`, and exposes `StateFlow<TtsVoiceStatus>`.
2. For short pronunciations (Flashcards, tapped Doc words, scenario phrases), `TtsManager` speaks immediately offline (or synthesizes to local cache WAV when Media3 playback controls are active).
3. For Listening exercises & long Documentation reading, `AudioPlayerManager` wraps **AndroidX Media3 (`ExoPlayer`)** over synthesized local audio files (generated offline via `TextToSpeech.synthesizeToFile` into app cache), providing genuine `Play`, `Pause`, `Replay`, `Stop`, playback speed (`0.75x`, `1.0x`, `1.25x`), and live progress tracking — with seamless fallback to direct TTS if synthesis is unavailable on an OEM device.
4. If no English TTS voice pack is installed, the app never crashes; it displays an actionable banner launching `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`.

---

## 6. Offline-First & Performance Strategy

- **Zero network dependency**: `AndroidManifest.xml` does not require `INTERNET` for any MVP feature.
- **Seed Database Integrity**: `scripts/build_seed_db.py` derives table DDL and `room_master_table` `identity_hash` directly from Room's KSP-exported `app/schemas/.../1.json`, preventing schema mismatch crashes on first open.
- **Main-thread safety**: All Room DAO calls are `suspend` or return `Flow` (dispatched off Main thread); DataStore reads/writes use Coroutines; syntax highlighting is memoized via Compose `remember(code, language, isDark)`.
- **Cold start optimization**: Heavy services (TTS, Media3 ExoPlayer) initialize lazily; WorkManager schedules asynchronously; Home observes reactive Room Flows with skeleton shimmer placeholders while DB copies on first launch.

---

## 7. v3.1.0 Remediation Architecture Updates (Sprints 0–3)

The **v3.1.0** remediation preserves the Offline-First Clean Architecture and visual style while resolving all security, scalability, database, and UI-architecture findings from the v3.0.0 audit:

- **Play Asset Delivery (`:content_pack`) & Async DB Bootstrap (`SP2-05`, `R4`, `R5`):**
  - Moved the pre-populated SQLite asset (`tech_english.db`, Room v6, `103.24 MiB`) out of `app/src/main/assets/` into the `install-time` PAD asset pack module `:content_pack` (`content_pack/src/main/assets/databases/tech_english.db`).
  - `DatabaseBootstrapManager` unpacks the DB strictly off the main thread (`Dispatchers.IO`) with progress updates displayed via `LinearProgressIndicator` in `MainActivity` (`514 ms` first launch, `0 ms` subsequent launches).
  - R8 minification & resource shrinking (`SP1-08`) plus `:content_pack` reduced `base` in `app-release.aab` from `26.61 MiB` → **`5.16 MiB`** compressed (`app-release-unsigned.apk`: `27.19 MiB` → **`5.59 MiB`**).
- **Room v6 Schema, FTS4 Search & Offline Caches (`SP2-04`, `SP2-08`, `SP3-04`, `R6`, `R12`):**
  - `MIGRATION_5_6` upgrades Room to **v6** (`identityHash = 2fed6cc837b9cd4374ad9e7e58e087bc`), adding external-content `@Fts4` (`academic_terms_fts`, `unicode61`) + `normalized_search` (`DEC-14`), `ota_content_meta` diagnostics (`last_status`, `last_error`, `last_attempt_at`), and `squad_cache` (`SquadCacheEntity` / `SquadCacheDao`).
  - Average 30k-term search latency verified at **`8.13 ms`** (`< 15 ms`), with case-insensitive Cyrillic & substring fallback.
  - Full migration chain `1→2→3→4→5→6` verified; `scripts/build_seed_db.py` builds the synchronized Room v6 seed DB.
- **Modular Compose UI & Navigation (`SP2-03`, `SP2-06`, `SP2-07`, `SP3-07`):**
  - Decomposed all UI/ViewModel monoliths `> 900` lines (`Stage3Screens`, `Stage2Screens`, `Components`, `AcademicScreens`, `ProgrammingScreens`, `ProfileScreen`, `Stage4CViewModels`) into focused files (`0` files `> 900` lines), migrated key `UiState` models to `@Immutable` + `ImmutableList`, and enabled Compose Compiler stability reports (`app/build/compose_compiler/`).
  - Extracted `CommunityArenaHubScreen` (`Routes.COMMUNITY_ARENA_HUB`), slimmed `ProfileScreen` from 12 → 5 sections, removed 3 duplicate entries from `PracticeHubScreen`, and synced `TechEnglishGlanceWidget` to the active `ThemeMode` with a `"↻"` refresh action.
- **Security, Auth, Sync & Realtime Hardening (`SP1-01`..`SP1-11`, `SP3-01`..`SP3-04`):**
  - Server-side OAuth (`oauth_identities`, `(provider, sub)` binding, Custom Tabs + `OAuthCallbackHandler`), SHA-256 hashed single-use 15-min auth tokens with lockout, RBAC (`users.role`), Play Integrity verifier interface (`GooglePlayIntegrityVerifier`), strict `AndroidKeystoreTokenStore` (zero `PLAIN:` fallback), `allowBackup="false"`, removed `CAMERA` permission, and ephemeral Redis WebSocket tickets (`POST /v1/ws/ticket`, `?ticket=`).
  - Bidirectional `CloudSyncEngine` with R10/E1 SM-2 conflict resolution in `:domain`, Ktor `Auth` 401 refresh + timeouts, offline `ROOM_MESSAGE_OUTBOX` resend via `sync_queue` with truthful `WsConnectionState`, live Android PvP over `/ws/pvp/{match_id}` without pre-answer key leakage, M4A voice recording (≤ 20s / ≤ 512 KB) + S3/MinIO presigned playback, and feature-flagged Tajik localization (`TajikLocalizationPolicy`, default `false` per R7).
- **Decomposed Scalable Backend (`SP2-01`, `SP2-02`, `SP2-10`, `R1`, `R2`):**
  - `backend/app/main.py` reduced from `2,847` → `125` lines (`deps.py`, 4 `schemas/`, `services/`, 10 `routers/`, `lifespan`, `create_app()`).
  - Process-local realtime state replaced by `RedisRealtimeBroker` (`techenglish:pvp:waiting_queue`, Pub/Sub `techenglish:room:*` and `techenglish:pvp:*`).
  - Added Alembic migrations `0001_initial_v3_0_0` through `0006_activity_feed_reactions`, `FcmHttpV1PushNotificationGateway` + `device_push_tokens`, async `HttpxGeminiClient` with PII scrubbing & honest fallback, `S3VoiceObjectStorage`, `CrashReporter` abstraction (PII-safe, disabled by default), and `.github/workflows/ci.yml` with PostgreSQL 16 & Redis 7 service containers.
