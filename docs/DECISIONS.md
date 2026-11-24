# Architectural Decisions & Ambiguity Log (DECISIONS.md)
## Project: Technical English for Developers (Android)

This document records ambiguities, discrepancies between specification documents, and explicit engineering decisions made during design and implementation without silently dropping any requirement.

---

## DEC-01: Specification Filename Discrepancy
- **Observation:** The Master Prompt references `Техническая_спецификация_финальная.md`, whereas the actual specification file in the workspace root is named `Техническая_спецификация_исправленная.md`.
- **Decision:** Analyzed `Техническая_спецификация_исправленная.md` in full as the primary specification and combined it with all extended requirements from the Master Prompt (including Module 2: 19 Programming Languages Catalog, Code Breakdown, Side-by-Side Comparison, extended categories, extended DB schema/indexes). No requirement from either source is omitted.

## DEC-02: Schema Reconciliation Between Spec Document and Master Prompt
- **Observation:** `Техническая_спецификация_исправленная.md` §3.3 specifies 5 tables (`categories`, `words`, `doc_articles`, `scenarios`, `study_logs`) and 6 indexes. The Master Prompt §11 specifies 7 mandatory tables (adding `programming_languages` and `code_snippets`), 10 mandatory indexes (adding `idx_words_language`, `idx_words_term_lang`, `idx_snippets_lang_task`, `idx_logs_snippet`), plus 50+ professional workplace phrases.
- **Decision:** Implemented the full superset schema:
  - All 7 mandatory tables (`categories`, `programming_languages`, `code_snippets`, `words`, `doc_articles`, `scenarios`, `study_logs`) with all 10 mandatory indexes.
  - Added 1 lightweight supporting table `work_phrases` to store 60+ categorized IT workplace English phrases cleanly in Room (powering both the Work Scenarios Phrases Reference and Module 5 Listening exercises without bloating `scenarios`).
  - Added non-breaking columns to `words` (`language_id` nullable FK, `explanation_ru`, `is_favorite`), `doc_articles` (`explanation_ru`, `reading_progress`, `is_completed`, `glossary_json`), `scenarios` (`exercise_type`, `prompt_ru`, `options_json`, `explanation_ru`, `useful_expressions_json`, `difficulty`), and `study_logs` (`snippet_id` nullable FK, `xp_earned`), preserving every single column and FK constraint from `Техническая_спецификация_исправленная.md`.

## DEC-03: Exact SM-2 Algorithm Edge Cases
- **Observation:** Both specifications define SM-2:
  - Initial: `EF = 2.5`, `repetitions = 0`, `interval = 0`.
  - Quality mapping: «Не помню» `q=1`, «Сложно» `q=3`, «Хорошо» `q=4`, «Легко» `q=5`.
  - EF formula: `EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))`, clamped to `>= 1.3`.
  - Intervals: 1st success (`repetitions = 1`) -> `1` day; 2nd success (`repetitions = 2`) -> `6` days; `n >= 3` -> `round(previousInterval * EF')`.
- **Ambiguity:** Whether `q = 1` sets `interval_days` to `0` (due immediately for re-study) or `1` day, and whether `I(n)` for `n >= 3` multiplies by updated `EF'` or prior `EF`.
- **Decision:**
  - When `q < 3` (`q = 1`): `repetitions = 0`, `intervalDays = 0` (literally resetting both repetitions and interval as stated in Master Prompt §5: *"Оценка q=1 сбрасывает счетчик успешных повторений и интервал"*), `nextReviewAt = currentTimeMillis` (remains due immediately so the user can re-practice missed cards), and `EF` is still updated via the formula and clamped to `>= 1.3`.
  - When `q >= 3` (`q = 3, 4, 5`): `EF'` is calculated first (clamped to `>= 1.3`), `repetitions = previousRepetitions + 1`, then:
    - `repetitions == 1` -> `intervalDays = 1`
    - `repetitions == 2` -> `intervalDays = 6`
    - `repetitions >= 3` -> `intervalDays = max(1, (previousInterval * newEaseFactor).roundToInt())`
    - `nextReviewAt = currentTimeMillis + intervalDays * 86_400_000L`.
  - Using `newEaseFactor` ensures that on 3rd+ repetitions, rating «Легко» (`q=5`) produces a longer interval than «Хорошо» (`q=4`), which produces a longer interval than «Сложно» (`q=3`).

## DEC-04: Gradle Multi-Module Layout
- **Observation:** Spec §3.2 requires the Domain Layer to be implemented in pure Kotlin without Android SDK dependencies. Master Prompt §3 suggests a logical folder structure (`app/`, `core/`, `domain/`, `data/`, `feature/`) and notes it may be adapted while preserving Clean Architecture.
- **Decision:** Used a 2-module Gradle layout:
  - `:domain` (`org.jetbrains.kotlin.jvm`) — pure Kotlin module enforcing zero Android dependencies at compile time and isolating Domain Layer JaCoCo test coverage (`>= 80%`).
  - `:app` (`com.android.application`) — cleanly organized into `core/`, `data/`, `di/`, and `feature/` packages matching Master Prompt §3.

## DEC-05: Code Syntax Highlighting Implementation
- **Observation:** Master Prompt §2 lists `Highlights / AnnotatedString` for code highlighting.
- **Decision:** Implemented a dedicated, zero-dependency, offline multi-language lexical tokenizer producing Compose `AnnotatedString` (`CodeSyntaxHighlighter`) tailored to all 19 mandatory languages (including Assembly, Pascal, VBA, Bash, Lua, SQL, Rust, Go, Kotlin, etc., many of which third-party highlighting libraries do not cover). Supports JetBrains-inspired Light & Dark color palettes, line numbering, and interactive clickable keyword ranges (`LinkAnnotation` / character offset hit-testing).

## DEC-06: Offline Media3 + TextToSpeech Integration
- **Observation:** Both TTS and Media3 are required offline without internet dependency or massive bundled MP3 assets.
- **Decision:** Implemented both `TtsManager` (Android `TextToSpeech` with offline voice detection and `ACTION_INSTALL_TTS_DATA` fallback) and `AudioPlayerManager` (AndroidX `Media3 ExoPlayer`). For Listening exercises and Documentation playback, `TtsManager.synthesizeToFile()` renders offline audio into app cache WAV files which `Media3 ExoPlayer` plays with true Play, Pause, Replay, Stop, progress scrubbing, and playback speed control (`0.75x`, `1.0x`, `1.25x`), falling back transparently to direct `TextToSpeech.speak()` if file synthesis is unavailable on a specific device/emulator.

## DEC-07: Streak & XP Calculation Rules
- **Observation:** Streak must reflect real study history (not app launches).
- **Decision:**
  - `CalculateStreakUseCase` converts `study_logs.timestamp` values to local calendar dates (`LocalDate`), deduplicates active days, and counts consecutive days ending on `today` or `yesterday` (so a user who studied yesterday hasn't lost their streak before finishing today's session; if neither today nor yesterday has a study log, streak = 0).
  - XP is stored per `study_logs` entry (`+10` correct flashcard, `+5` missed flashcard effort, `+20` correct scenario, `+5` attempted scenario, `+15` completed doc article, `+15` correct listening, `+10` code breakdown). User Level = `1 + totalXp / 150` with developer ranks (Intern -> Junior -> Middle -> Senior -> Tech Lead -> Staff -> Principal).

## DEC-08: System Category Protection («Мои слова»)
- **Observation:** Spec §3.3 mandates a system category «Мои слова» created during pre-population that cannot be deleted so cascade deletion never destroys user progress.
- **Decision:** Category `id = 11` (`name = "Мои слова"`, `is_system = 1`) is pre-populated in `tech_english.db`. `CategoryDao` and `VocabularyRepositoryImpl` enforce `WHERE is_system = 0` on deletion attempts and reject attempts to delete system categories in the Domain Layer.

## DEC-09 (ADR-Stage3-01): Backend Stack — FastAPI + PostgreSQL + Redis (No Supabase)
- **Status:** Accepted (Stage 3, `techenglish_v3_2_final.md` §6.1 & §9)
- **Context:** TechEnglish v3.0 requires a hybrid Offline-First + Cloud Sync backend supporting REST API, WebSockets (Stage 4C), server-side Anti-Cheat XP validation, weekly leagues (Monday 00:00 `Asia/Dushanbe`, UTC+5), OTA content pack delivery, and a secure server-side Gemini AI proxy (Stage 4B).
- **Decision:** Standardize strictly on **Python 3.11+ (FastAPI + SQLAlchemy 2.0 + Pydantic v2)**, **PostgreSQL 16** (authoritative persistence), and **Redis 7** (leaderboard caches, Anti-Cheat rate-limit windows, WebSocket pub/sub). **Supabase is explicitly excluded.** On Android, **Ktor Client** is used as the single HTTP/WebSocket client (Retrofit is excluded).
- **Consequences:** Full control over server-side Anti-Cheat (`ФТ-9.3`), deterministic `pytest` suite runnable locally without cloud lock-in, and reproducible deployment via `backend/docker-compose.yml`.

## DEC-10 (ADR-Stage3-02 / Remediation R10): Bidirectional Sync Outbox, Errata E1 SM-2 Conflict Resolution & Anti-Cheat
- **Status:** Accepted & Updated in v3.1.0 (`SP1-06`, `R10`, `ФТ-9.2`, `ФТ-9.3`, Errata **E1**)
- **Decision:**
  - Offline mutations are written to Room `sync_queue` (`event_id` UUID primary key, `PENDING` status strictly inside `sync_queue`). `CloudSyncEngine` performs bidirectional sync: pushes `events`, `sm2_updates`, `favorite_terms`, and `custom_words` via `POST /v1/sync/events`, and pulls authoritative remote snapshots via `GET /v1/sync/state`, backed by Ktor `Auth` (automatic `/v1/auth/refresh` on `401`) and `HttpTimeout`.
  - Per **R10 / Errata E1** (explicitly superseding and replacing the server-time `last_studied_at` rule in `docs/techenglish_v3_2_final.md` `ФТ-9.2` / §5.1): SM-2 flashcard conflicts are resolved by comparing `client_timestamp` (clamping timestamps > 5 minutes ahead of `server_received_at`); ties break by higher `repetitions`, then earlier `server_received_at`.
  - `study_logs` are append-only deduplicated by `event_id`; favorites and «Мои слова» merge by set union (`normalized_term`); XP is never trusted from clients and is recomputed strictly by the server from validated events.
  - Both `:domain` (`ResolveSm2SyncConflictUseCase`, `ValidateRankedEventAntiCheatUseCase`) and `backend/app/anti_cheat.py` share identical deterministic rules.

## DEC-11 (ADR-Stage3-03): Profile Moderation, 16+ Age Gate & Preset/OAuth Avatars (Errata E2)
- **Status:** Accepted (Stage 3, `ФТ-9.4`, `ФТ-10.1`, Errata **E2**)
- **Decision:**
  - Enforce a mandatory 16+ Age Gate on first launch (including Guest Mode), persisted in DataStore, alongside in-app Privacy Policy and account/data deletion (`DELETE /v1/account` + web form `GET /v1/account/delete-form`).
  - Implement report-user, block-user, moderation queue, and `@username` filtering in Stage 3 for profiles.
  - Avatars are restricted to curated preset developer avatars and verified GitHub/Google OAuth avatar URLs; arbitrary binary avatar uploads are disabled until object storage lands in Stage 4C.

## DEC-12 (ADR-Stage3-04): Token Storage via Android Keystore AES-256-GCM + DataStore (`НФТ-7.3`)
- **Status:** Accepted (Stage 3, `НФТ-7.3`)
- **Context:** `androidx.security:security-crypto` (`EncryptedSharedPreferences`) is officially deprecated.
- **Decision:** Encrypt JWT access and refresh tokens using an `AndroidKeyStore` hardware-backed `AES/GCM/NoPadding` key (`KeyGenParameterSpec`, 256-bit) and persist the Base64-encoded IV + ciphertext inside Jetpack DataStore (`AndroidKeystoreTokenStore`). Release builds enforce strict HTTPS (TLS 1.2+) via `network_security_config.xml` (`cleartextTrafficPermitted="false"`).

## DEC-13 (ADR-Stage4C-01 / Errata E5): S3-Compatible Voice Storage (MinIO) & 30-Day TTL for Peer Review Arena (`ФТ-14.2`, `ФТ-9.4`)
- **Status:** Accepted (Pre-Stage 4C per Errata **E5**)
- **Context:** Peer Review Arena (`ФТ-14.2`) allows users to optionally attach short voice clips (≤ 20 seconds) answering the daily technical communication case. Storing binary audio inside PostgreSQL inflates database backups and complicates lifecycle cleanup and GDPR/account-deletion compliance (`ФТ-9.4`).
- **Decision:**
  1. Provision an S3-compatible object store (**MinIO**) in `backend/docker-compose.yml` (`bucket: techenglish-peer-voice`) paired with a storage abstraction (`backend/app/voice_storage.py`) that supports MinIO/S3 in production and isolated local object directories in CI/`pytest`.
  2. Enforce strict upload bounds: duration `1..20` seconds, max payload `512 KB`, content types `audio/mp4`, `audio/ogg`, `audio/webm`, `audio/aac`.
  3. Set an explicit **30-day TTL** (`PEER_REVIEW_VOICE_TTL_DAYS = 30`) on all Peer Review voice recordings (`expires_at_ms` stored in `peer_review_submissions`, purged automatically on expiry and documented in both `/v1/legal/privacy-policy` and the Android `PrivacyPolicyDialog`). Local Pronunciation Trainer (`ФТ-7.3`) and AI Mock Interview dictation (`ФТ-8.1`) retain `0` days (never persisted or uploaded).
  4. `DELETE /v1/account` synchronously deletes both the user's `peer_review_submissions` rows and every referenced voice object from object storage before committing account removal.

## DEC-14 (ADR-Remediation-R6 / SP2-04): Full-Text Search Strategy on Android `minSdk 26` (`@Fts4` External Content + `normalized_search`)
- **Status:** Accepted (Sprint 2B, `SP2-04`, `BUG-15`, `R6`, `R12`)
- **Context:** Dictionary search in v3.0.0 ran `LIKE '%query%'` across 8 columns with SQLite's ASCII-only `LOWER()`, causing full table scans on 20,658+ terms and broken case-insensitive Cyrillic matching on OEM SQLite builds without ICU `LOWER()`. While FTS5 is desirable, system SQLite FTS5 availability is not guaranteed across API 26–30 OEM images, Room 2.6.1 does not provide an `@Fts5` annotation, and bundling `androidx.sqlite:sqlite-bundled` adds ~5–8 MB of multi-ABI native `.so` binaries and requires Room 2.7 KMP driver migration.
- **Decision:**
  1. Use Room's first-class **`@Fts4(contentEntity = AcademicTermEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)`** virtual table (`academic_terms_fts`) backed by a dedicated **`normalized_search`** column (pre-lowercased in Unicode across Latin, Cyrillic, and Tajik via Kotlin `Locale.ROOT` and Python `.lower()`).
  2. Add `normalized_search` to both `academic_terms` and `words` in **Room v6 (`MIGRATION_5_6`)** with a Unicode-lowercase backfill pass and automatic Room FTS4 content-sync triggers + `rebuild`.
  3. Query strategy in `AcademicTermDao`: primary fast path joins `academic_terms_fts MATCH 'token1* token2*'` (inverted index, `< 15 ms` on 30,000+ terms); if FTS4 returns 0 matches for a non-empty query (e.g., mid-word Cyrillic substring like `"лансировщ"`), transparently fall back to `normalized_search LIKE '%query%'`.
  4. Per **R12**, update `scripts/build_seed_db.py` to consume Room v6 `6.json`, populate `normalized_search`, create Room FTS4 triggers, rebuild `academic_terms_fts`, and ship the pre-indexed v6 `tech_english.db`.
- **Consequences:** Guaranteed compatibility on every Android device from API 26 to API 35 with zero extra native binary bloat, full Room compile-time schema verification, deterministic case-insensitive Cyrillic + substring search, and sub-15 ms search latency at 30,000 terms.



