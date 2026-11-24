# Technical Audit Report — TechEnglish v1.0 → v2.0 Upgrade (`UPGRADE_AUDIT.md`)

**Date:** 2026-11-19  
**Audited Codebase:** `TechEnglish` (Modules `:domain`, `:app`, `scripts/`, `docs/`)  
**Target Release:** TechEnglish v2.0 (Production Upgrade & 10,000+ Academic Technical Vocabulary Expansion)

---

## 1. Executive Summary of Existing Project

The v1.0 application is a functional, offline-first Android app built with Kotlin 2.0.21, Jetpack Compose (Material 3), Clean Architecture (`:domain` pure Kotlin JVM module + `:app` Android module), Room v1 (`createFromAsset`), Preferences DataStore, Dagger Hilt, Android `TextToSpeech` + `Media3 ExoPlayer`, and `WorkManager`.

All 6 baseline MVP modules (IT Vocabulary + SM-2 SRS, 19 Programming Languages Catalog + Code Breakdown + Side-by-Side Comparison, Technical Documentation Reader, Work Scenarios + Phrases, Listening Studio, Statistics & Notifications) compile and pass 42 automated tests (30 Domain + 12 App).

However, a rigorous technical audit against **Master Prompt 2.0** identified **7 technical issues / upgrade gaps** that must be fixed and verified.

---

## 2. Detailed Audit Findings & Remediation Matrix

### AUD-01: Hardcoded User SDK Path in `local.properties` (RESOLVED & VERIFIED)
- **Description:** In v1.0, `local.properties` in the workspace contained a user-specific local SDK path pointing to an external directory on the build machine.
- **File & Location:** `local.properties` (removed from workspace tree; listed in `.gitignore`).
- **Root Cause:** During v1.0 bootstrap, the Android SDK on the build machine was referenced via a local machine path rather than the standard Android Studio SDK resolution (`~/Library/Android/sdk` / `ANDROID_HOME`).
- **Implemented Fix:**
  1. Deleted the hardcoded `local.properties` file from the repository and ensured `local.properties` is listed in `.gitignore` and excluded from `TechEnglish_Source_Clean.zip`.
  2. Configured standard Android Studio SDK resolution (`~/Library/Android/sdk` / `ANDROID_HOME`) so Gradle sync and builds work transparently without machine-specific paths in the repository.
- **Verification Result:** Verified `.gitignore` contains `local.properties`, verified `TechEnglish_Source_Clean.zip` has zero `local.properties` entries, and verified `./gradlew :app:assembleDebug :app:assembleRelease` succeeds cleanly.

---

### AUD-02: JaCoCo Coverage Discrepancy Between CSV (92.22%) and XML (92.44%) (RESOLVED & VERIFIED)
- **Description:** In v1.0, `docs/FINAL_REPORT.md` and `jacocoTestReport.csv` reported `865/938 = 92.22%` line coverage, whereas `jacocoTestReport.xml` top-level `<counter type="LINE">` and HTML `index.html` reported `856/926 = 92.44%`.
- **File & Location:** `domain/build.gradle.kts` (`tasks.jacocoTestReport`), `domain/build/reports/jacoco/test/jacocoTestReport.{xml,csv,html}`, `docs/FINAL_REPORT.md`.
- **Root Cause:** Kotlin's inline coroutine flow operators (`Flow.map { ... }`) generate synthetic inner classes (`*$$inlined$map$1.class` and `*$$inlined$map$1$2.class`) attributed to external source files `SafeCollector.common.kt` (2/3 lines each × 4 classes = 8/12) and `Emitters.kt` (1/1 line each × 4 classes = 4/4). JaCoCo's CSV report sums `<class>` rows (`853 + 12 = 865 / 922 + 16 = 938`), whereas JaCoCo's XML/HTML `<report>` counter aggregates per `<sourcefile>` (`853 + 3 = 856 / 922 + 4 = 926`).
- **Implemented Fix:**
  1. Configured `tasks.jacocoTestReport` in `domain/build.gradle.kts` to exclude synthetic Kotlin inline classes (`**/*$inlined$*.class`) from `classDirectories`.
  2. Executed a single clean test run (`./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport`).
- **Verification Result:** **100% identical coverage across all formats:**
  - `jacocoTestReport.csv`: **`1113 / 1196` lines covered (`93.06%`), `83` missed**
  - `jacocoTestReport.xml`: **`1113 / 1196` lines covered (`93.06%`), `83` missed**
  - `html/index.html`: **`1113 / 1196` lines covered (`93.06%`), `83` missed**
  - `docs/FINAL_REPORT.md`: **`1113 / 1196` lines covered (`93.06%`), `83` missed**

---

### AUD-03: Vocabulary Size & Schema Scaling (255 Terms vs 10,000+ Academic Terms Target & 30,000+ Scalability) (RESOLVED & VERIFIED)
- **Description:** v1.0 contained 255 general IT vocabulary terms in a flat `words` table without dedicated fields for academic explanations, normalized search keys, domain/subdomain taxonomy (10 major categories / 30 thematic areas), multi-sense disambiguation (`word_senses`), structured citations (`word_sources`), term relationships (`word_relations`), or verification status.
- **File & Location:** `app/src/main/kotlin/com/techenglish/app/core/database/`, `tools/vocabulary_import/`, `app/src/main/kotlin/com/techenglish/app/feature/academic/`.
- **Root Cause:** v1.0 was scoped to the initial 200–300 term MVP requirement.
- **Implemented Fix:**
  1. Upgraded Room Database to **Version 2** (`TechEnglishDatabase` v2), keeping all v1 tables intact and adding normalized scalable tables: `subcategories`, `word_sources`, `academic_terms` (with 12 new SQLite indexes), `word_senses`, and `word_relations`.
  2. Built the reproducible import, normalization, deduplication, and quality validation pipeline in `tools/vocabulary_import/` (`vocabulary_schema.json`, `vocabulary_importer.py`, `vocabulary_validator.py`, `vocabulary_statistics.py`) and populated **10,308 unique, verified academic & deep technical terms** across all 10 major categories and 30 thematic domains.
  3. Implemented DAO-level pagination (`LIMIT :limit OFFSET :offset`) and benchmarked search/pagination at both 10,308 and 30,000 records.
- **Verification Result:** `vocabulary_validator.py` verified **10,308 unique verified terms (`0` duplicates, `0` validation errors, `100%` required field completeness)**; `TechEnglishDatabaseTest` verified `< 100 ms` search and pagination at both 10,308 and **30,000 records**.

---

### AUD-04: Lossless Room Migration (`MIGRATION_1_2`) & Migration Tests (RESOLVED & VERIFIED)
- **Description:** Upgrading from Room v1 to v2 required a non-destructive migration preserving all v1 user data (custom words, favorites, SM-2 intervals, reading progress, and `study_logs`).
- **File & Location:** `app/src/main/kotlin/com/techenglish/app/core/database/TechEnglishDatabase.kt`, `app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/2.json`.
- **Root Cause:** Initial v1 schema had no prior version to migrate from.
- **Implemented Fix:**
  1. Implemented `MIGRATION_1_2` in `TechEnglishDatabase.kt` creating all v2 tables and indexes without altering existing user rows in `words`, `categories`, `doc_articles`, `scenarios`, or `study_logs`.
  2. Exported Room schema `2.json` alongside `1.json`.
  3. Added automated test `migration_1_to_2_isLosslessForExistingUserProgressAndCustomWords` in `TechEnglishDatabaseTest`.
- **Verification Result:** `TechEnglishDatabaseTest` passed, confirming 100% preservation of custom words, favorites, SM-2 repetitions/intervals, and `study_logs` across `1 -> 2` migration.

---

### AUD-05: Audio & TTS Accent Switching (`en-US` / `en-GB`) and Lifecycle Hardening (RESOLVED & VERIFIED)
- **Description:** `TechAudioController.kt` needed runtime English locale/accent switching (`en-US` vs `en-GB`) requested in Phase 2.5 ("Смена языка озвучивания").
- **File & Location:** `app/src/main/kotlin/com/techenglish/app/core/audio/TechAudioController.kt`.
- **Root Cause:** v1.0 used `Locale.US` as the sole TTS target locale.
- **Implemented Fix:**
  1. Added `setSpeechLocale(localeTag: String)` (`"en-US"` / `"en-GB"`) to `TechAudioController`, updating `AudioPlaybackState.speechLocaleTag` and re-selecting the best offline voice for that locale, plus exposed `US`/`UK` accent toggles in `AcademicTermDetailScreen` and `AcademicSrsScreen`.
- **Verification Result:** Verified in unit and UI tests with full offline fallback resilience.

---

### AUD-06: WorkManager Notification Scheduling & Boot/Timezone Alignment (RESOLVED & VERIFIED)
- **Description:** `ReviewReminderWorker` needed to combine due counts across both core SRS words and academic SRS terms, and `NotificationScheduler` needed a deterministic timezone delay helper.
- **File & Location:** `app/src/main/kotlin/com/techenglish/app/core/notifications/NotificationsEngine.kt`.
- **Root Cause:** v1.0 only counted due rows in `words`.
- **Implemented Fix:**
  1. Updated `ReviewReminderWorker` to query combined due count (`vocabularyRepository.getDueCount(now) + academicVocabularyRepository.getDueCount(now)`).
  2. Added `NotificationScheduler.computeInitialDelayMinutes(now, targetHour, targetMinute, zoneId)` and verified timezone and next-day wrap-around in `ReviewReminderWorkerTest`.
- **Verification Result:** All 3 tests in `ReviewReminderWorkerTest` passed.

---

### AUD-07: Unsigned Release APK & Clean Source Archive Packaging (RESOLVED & VERIFIED)
- **Description:** Master Prompt 2.0 Phase 2.7 & Phase 12 require both `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk`, plus a clean source archive `TechEnglish_Source_Clean.zip`.
- **File & Location:** `app/build.gradle.kts`, root directory.
- **Implemented Fix & Verification Result:** Both `app-debug.apk` and `app-release-unsigned.apk` built cleanly via `./gradlew :app:assembleDebug :app:assembleRelease`, and `TechEnglish_Source_Clean.zip` packaged without any build/cache/IDE artifacts or `local.properties`.
