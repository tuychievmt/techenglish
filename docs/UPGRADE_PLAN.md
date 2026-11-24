# Upgrade Execution Plan — TechEnglish v2.0 (`UPGRADE_PLAN.md`)

This document outlines the phased engineering plan for upgrading TechEnglish from v1.0 to v2.0 without breaking any existing functionality.

---

## Phase 1 — Technical Audit & Traceability Documentation
- [x] Audit existing codebase, Room v1 schema, JaCoCo reports, `local.properties`, Audio/TTS, Notifications, and APK packaging.
- [x] Create `docs/UPGRADE_AUDIT.md`, `docs/UPGRADE_PLAN.md`, and `docs/REQUIREMENTS_TRACEABILITY.md`.

## Phase 2 — Technical Fixes (SDK, JaCoCo, Audio, Notifications)
- **2.1 SDK & `local.properties`:** Configure standard `~/Library/Android/sdk` path, keep `local.properties` ignored in `.gitignore` and excluded from source archives, remove hardcoded user path references from docs.
- **2.2 JaCoCo Consistency:** Exclude Kotlin `*$$inlined$*.class` synthetic classes in `domain/build.gradle.kts` so XML, CSV, HTML, and `FINAL_REPORT.md` match 100% down to the exact line count (`>= 90%` Domain Layer line coverage).
- **2.3 Audio & TTS Hardening:** Add `en-US` / `en-GB` accent switching to `TechAudioController`, verify Play/Pause/Replay/Stop/Release and missing-voice resilience.
- **2.4 Notifications Hardening:** Include due Academic SRS terms in `ReviewReminderWorker`, add timezone-aware scheduling calculation helper and unit tests.

## Phase 3, 4, 5 & 9 — Academic Technical Vocabulary Corpus (10,000+ Unique Terms) & Import Pipeline
- Build `tools/vocabulary_import/`:
  - `vocabulary_schema.json` — Formal JSON Schema for academic terms, polysemous senses, and source references.
  - `corpus_builder.py` — Deterministic, domain-curated academic terminology compiler generating **10,250+ unique, verified technical terms** across the 10 mandatory domains and 30 thematic areas, plus explicit multi-sense disambiguation records (`word_senses`) and authoritative references (`word_sources`: IETF RFCs, NIST FIPS/SP, IEEE standards, ISO/IEC standards, Linux Kernel Docs, PostgreSQL Docs, Rust/Kotlin/Python/MDN/OWASP documentation).
  - `vocabulary_importer.py` — Supports importing and upserting terms from JSON and CSV files with normalization (`normalized_term`), duplicate detection, category mapping, and conflict checking.
  - `vocabulary_validator.py` — Validates every record against `vocabulary_schema.json`, checks required fields, verifies zero duplicates, validates translations and sources, and assigns verification statuses (`verified`, `pending_review`, `draft`, `needs_revision`, `rejected`).
  - `vocabulary_statistics.py` — Computes corpus statistics and generates `docs/VOCABULARY_QUALITY_REPORT.md`.

## Phase 6 — Room Database v2 & Lossless Migration (`MIGRATION_1_2`)
- Upgrade `TechEnglishDatabase` to `version = 2` (`exportSchema = true`, generating `2.json` alongside `1.json`).
- Add normalized tables capable of scaling to **30,000–50,000+ terms**:
  - `subcategories` (`id`, `category_id`, `name`, `slug`)
  - `word_sources` (`id`, `title`, `url`, `source_type`, `publisher`)
  - `academic_terms` (25 columns covering all linguistic, academic, practical, taxonomic, citation, verification, and SM-2 SRS fields + 10 SQLite indexes)
  - `word_senses` (polysemous technical meanings per term)
  - `word_relations` (synonyms & related terms graph)
- Implement `MIGRATION_1_2` preserving 100% of v1 user data (`words`, `categories`, `doc_articles`, `scenarios`, `work_phrases`, `study_logs`).
- Update `scripts/build_seed_db.py` to build the v2 pre-populated `app/src/main/assets/databases/tech_english.db` with all v1 data + 10,250+ verified academic terms, senses, sources, subcategories, and relations.

## Phase 7 & 8 — Academic Technical Dictionary UI/UX & Advanced SRS Modes
- Create the **Academic Technical Dictionary** feature (`feature/academic/`) accessible directly from Home, Vocabulary, and Navigation:
  - **Overview Dashboard:** Total Terms (`10,000+`), Category & 30-Domain Explorer, Recently Studied, Difficult Terms (`EF < 2.1` or `difficulty IN ('Advanced','Expert','Research Level')`), Bookmarked Terms, Due for Review count.
  - **Indexed Offline Search & Advanced Filters:** Case-insensitive, whitespace-normalized search across English term, Russian translation, definition, synonyms, and abbreviations; filters for Domain (all 10 major + 30 thematic areas), Subdomain, Difficulty (`Beginner`, `Intermediate`, `Advanced`, `Expert`, `Research Level`), CEFR (`B1`–`C2`), Part of Speech, Favorites, Studied/Not Studied, Due for Review.
  - **Paginated List:** DAO-backed paged loading (`LIMIT 40 OFFSET ...` with automatic scroll pagination) so Compose never loads 10,000+ items at once.
  - **Term Detail Screen:** Displays English term, IPA transcription, Russian translation, English & Russian technical definitions, Academic & Practical explanations, multiple Senses (for polysemous terms), bilingual usage examples, Synonyms, clickable Related Terms (navigating directly to the related term!), Domain/Subdomain, Difficulty & CEFR badges, and authoritative Sources; action buttons for **Listen (TTS/Media3)**, **Add to Favorites**, **Add to SRS**, and **Mark as Learned**.
  - **Advanced SRS Study Modes:** Support specialized study decks without forcing the user to review all 10,000 terms:
    1. `Academic Vocabulary`
    2. `Machine Learning Vocabulary`
    3. `Cryptography Vocabulary`
    4. `Computer Architecture Vocabulary`
    5. `Research Paper Vocabulary`
    6. `Custom Vocabulary`
    Plus Definition Comprehension & Translation Quiz modes!

## Phase 10 & 11 — Performance Benchmarking (10,000+ & 30,000 Terms) & Automated Tests
- Expand Domain unit tests (`:domain:test`) for Academic Dictionary & Advanced SRS use cases (`>= 90%` line coverage).
- Expand Android Robolectric Room & UI tests (`:app:testDebugUnitTest`):
  - Verify 10,000+ unique verified terms in `tech_english.db`.
  - Verify `MIGRATION_1_2` preserves existing v1 user data.
  - Verify indexed search, Russian translation search, abbreviation search, domain/difficulty filtering, polysemous senses, related-term navigation, and pagination.
  - **30,000-Term Scalability Benchmark Test:** Insert up to 30,000 records in SQLite Room and measure indexed search, filtered count, and paginated query latencies (`< 100 ms`).

## Phase 12 & 13 — Final Build, Packaging & Documentation
- Build `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Package clean source archive `TechEnglish_Source_Clean.zip`.
- Update `docs/FINAL_REPORT.md`, `docs/REQUIREMENTS_TRACEABILITY.md`, `docs/UPGRADE_AUDIT.md`, `docs/VOCABULARY_QUALITY_REPORT.md`, and `docs/ARCHITECTURE.md`.
