# Requirements Traceability Matrix — All Specifications (`REQUIREMENTS_TRACEABILITY.md`)

**Sources Reconciled (in priority order):**
1. **`MP-3.1-Errata`** — Master Prompt v3.0 (редакция 3.1), Раздел 2 «Errata к ТЗ 3.2» (`E1`–`E6`, highest priority)
2. **`ТЗ-3.2`** — `docs/techenglish_v3_2_final.md` (Техническое задание: Модернизация платформы TechEnglish до версии 3.0, редакция 3.2 финальная)
3. **`MP-2.0` / `MP-1.0` / `ТЗ-Испр`** — Baseline v1.0 & v2.0 specifications (`Техническая_спецификация_исправленная.md`)

---

## 1. Baseline v1.0 / v2.0 Traceability (`ФТ-1.1`…`ФТ-6.5`, `НФТ-1.1`…`НФТ-6.2`)

| Requirement | Source | Current Status | Stage / Required Action in v3.0 |
|---|---|---|---|
| **ФТ-1.1** Structured IT vocabulary DB (11 categories, 255 core terms) + 10,308 verified Academic Technical Terms across 30 domains | `ТЗ-Испр`, `MP-1.0`, `MP-2.0` | Implemented & Verified | Preserve 100% in v3.0; expand to 20,000–30,000 verified terms & 36 domains in Stage 2 (`КД-1`) |
| **ФТ-1.2** Word card with term, IPA, RU translation, EN example, RU explanation, audio, favorite | `ТЗ-Испр`, `MP-1.0` | Implemented & Verified | Redesigned in Stage 1 (`УИ-1.1`–`УИ-1.4`, `УИ-2.3` Progressive Disclosure) |
| **ФТ-1.3** Full SM-2 algorithm (`q=1,3,4,5`, `EF>=1.3`, `I1=1`, `I2=6`, `In=I(n-1)*EF`) | `ТЗ-Испр`, `MP-1.0` | Implemented & Verified (`Sm2Engine`) | Preserve in `:domain`; add sync conflict resolver in Stage 3 per Errata **E1** |
| **ФТ-1.4 / ФТ-5.1–5.3** Offline TTS (`en-US`/`en-GB`) + Media3 audio playback with missing-voice resilience | `ТЗ-Испр`, `MP-2.0 §2.5` | Implemented & Verified | Preserve 100% offline capability |
| **ФТ-1.5** Custom dictionary CRUD (add, edit, delete custom terms in «Мои слова») | `ТЗ-Испр`, `MP-1.0` | Implemented & Verified | Preserve across `MIGRATION_2_3`, `3_4`, `4_5`; add JSON/CSV/Anki export/import in Stage 2 (`ФТ-7.6`) & Community Decks in Stage 4A (`ФТ-13.2`) |
| **ФТ-2.1–2.3** Technical Documentation library (32 articles, 9 tags, interactive word-tap lookup & Add-to-SRS) | `ТЗ-Испр`, `MP-1.0` | Implemented & Verified | Apply Terminal Chameleon theme to `Error Logs` in Stage 1 (`УИ-2.4`); expand to ≥ 150 articles/logs in Stage 2 (`КД-3`) |
| **ФТ-3.1–3.3** Work Scenarios (36 trainers, 8 contexts, 6 exercise mechanics, 64 phrases, instant feedback) | `ТЗ-Испр`, `MP-1.0` | Implemented & Verified | Apply Chameleon containers (GitHub PR / Slack / Terminal) in Stage 1 (`УИ-2.4`); expand to ≥ 150 scenarios, 12 contexts & ≥ 300 phrases in Stage 2 (`КД-4`) |
| **ФТ-4.1–4.3** Real DB statistics, streak from `study_logs`, daily goal (10/20/30), WorkManager reminders | `ТЗ-Испр`, `MP-2.0 §2.6` | Implemented & Verified | Expand activity heatmap from 30 days to 365-day GitHub Contribution Graph in Stage 1 (`УИ-2.2`) |
| **ФТ-6.1–6.5** 19 Programming Languages catalog, Interactive Code Breakdown, Dynamic Side-by-Side Comparison | `MP-1.0` | Implemented & Verified (19 languages, 169 snippets) | Add dynamic language accent tinting in Stage 1 (`УИ-3.3`); expand to 20 `task_key` & ≥ 350 snippets in Stage 2 (`КД-5`) |
| **НФТ-1.1** Cold start ≤ 2s repeat, ≤ 5s first launch | `ТЗ-Испр`, `ТЗ-3.2 §1.1` | Corrected note in Stage 1 | Note corrected: v1.0 verified on 796 KB DB, actual v2.0 DB is 42.84 MB; re-verified under `НФТ-7.6` in Stage 2 |
| **НФТ-2.1** ~~Strict Offline-only without `INTERNET`~~ | `ТЗ-Испр`, `ТЗ-3.2 §1.1` | **Cancelled in v3.0** | Replaced by `НФТ-7.1` (Hybrid Offline-First + Cloud Sync) & `НФТ-7.3` |
| **НФТ-5.1** Minimal permissions | `ТЗ-Испр`, `ТЗ-3.2 §6.3` | **Updated in v3.0** | Updated by `НФТ-7.7` (`POST_NOTIFICATIONS`, `RECORD_AUDIO` in Stage 2, `INTERNET` + `ACCESS_NETWORK_STATE` + on-demand `CAMERA` in Stage 3) |
| **НФТ-5.2** ~~HTTPS/TLS 1.2+ N/A in Offline MVP~~ | `ТЗ-Испр`, `ТЗ-3.2 §6.3` | **Cancelled in v3.0** | Replaced by `НФТ-7.3` (HTTPS TLS 1.2+, Keystore + Tink/DataStore) |

---

## 2. Stage 1 Traceability — UI/UX Redesign, Typography, Accessibility & Documentation

| Requirement | Source | Current Status | Stage 1 Implementation & Verification |
|---|---|---|---|
| **3.1 / Docs** Update `REQUIREMENTS.md`, `REQUIREMENTS_TRACEABILITY.md`, create `OWNER_ACTIONS.md` | `MP-3.1 §0.2, §3.1`, `ТЗ-3.2 §1.1` | Implemented & Verified in Stage 1 | Updated numbering `ФТ-7.x`–`14.x`, `НФТ-7.x`, cancelled `НФТ-2.1`/`5.2`, updated `НФТ-5.1`/`1.1`, documented 8 owner blockers |
| **УИ-1.1** Local font pair **Inter** + **JetBrains Mono** in `res/font/` covering Cyrillic, IPA & Tajik (`ғ, ӣ, қ, ӯ, ҳ, ҷ`) + OFL license in «О приложении» | `ТЗ-3.2 §4.1`, `MP-3.1 §1, §3.2` | Implemented & Verified in Stage 1 | Bundled TTF fonts in `app/src/main/res/font/`; verified glyph coverage via `Paint.hasGlyph` test; added OFL license dialog in `ProfileScreen` |
| **УИ-1.2** Term highlighting in examples + muted context (contrast ≥ 4.5:1) + POS & Difficulty badges | `ТЗ-3.2 §4.1`, `MP-3.1 §3.2` | Implemented & Verified in Stage 1 | `HighlightedExampleQuoteCard`, `DifficultyBadge`, `PartOfSpeechBadge` in `Components.kt` |
| **УИ-1.3** Visual hierarchy: large bold term, compact muted IPA, line-height `1.4–1.5x` in definitions & docs | `ТЗ-3.2 §4.1`, `MP-3.1 §3.2` | Implemented & Verified in Stage 1 | Configured in `Theme.kt` (`TechTypography`), `AcademicScreens.kt`, `DocumentationScreens.kt` |
| **УИ-1.4** Separate visual containers for translation, example (left vertical accent bar), and practical explanation | `ТЗ-3.2 §4.1`, `MP-3.1 §3.2` | Implemented & Verified in Stage 1 | Implemented across `AcademicScreens.kt`, `VocabularyScreen.kt`, `FlashcardsScreen.kt` |
| **УИ-2.1** 5 Iconic IDE Themes (**Tokyo Night**, **Dracula**, **GitHub Dark**, **Nord**, **Gruvbox**) + Light/Dark/Dynamic Colors with WCAG AA contrast | `ТЗ-3.2 §4.2`, `MP-3.1 §3.3` | Implemented & Verified in Stage 1 | Added to `ThemeMode` and `Theme.kt`; verified all 7 themes ≥ 4.5:1 contrast in `TechEnglishUiTest` |
| **УИ-2.2** 365-Day GitHub Contribution Graph on Home, Profile & Statistics | `ТЗ-3.2 §4.2`, `MP-3.1 §3.3` | Implemented & Verified in Stage 1 | Added `yearlyActivity` (365 days) in `GetStatisticsUseCase`/`HomeViewModel`/`ProfileViewModel` & `GitHubContributionGraph` composable |
| **УИ-2.3** Progressive Disclosure on Academic Term Card (Hero card first; explanations, `word_senses`, `word_sources`, `word_relations` in tabs/expandable sections) | `ТЗ-3.2 §4.2`, `MP-3.1 §3.3` | Implemented & Verified in Stage 1 | Refactored `AcademicTermDetailScreen` & dictionary list cards in `AcademicScreens.kt` |
| **УИ-2.4** Chameleon Interface containers (**GitHub PR**, **Slack**, **Terminal**) & extensible `ScenarioChameleonRegistry` | `ТЗ-3.2 §4.2`, `MP-3.1 §3.3` | Implemented & Verified in Stage 1 | Mapped `CODE_REVIEW`/`PR_COMMENTS` → GitHub PR, `DAILY_STANDUP`/`TEAM_COMMUNICATION` → Slack, `BUG_REPORTING`/`GITHUB_ISSUES`/`Error Logs` → Terminal |
| **УИ-3.1** Haptic Feedback (flip, swipe, correct answer, word tap) with DataStore toggle | `ТЗ-3.2 §4.3`, `MP-3.1 §3.4` | Implemented & Verified in Stage 1 | `TechHaptics` + `hapticFeedbackEnabled` in `UserPreferencesDataStore` & `ProfileScreen` |
| **УИ-3.2** Combo streaks (3, 5, 8, 10+) with developer feedback (*«LGTM!»*, *«Clean build!»*, *«Zero warnings!»*, *«Merged to main!»*) | `ТЗ-3.2 §4.3`, `MP-3.1 §3.4` | Implemented & Verified in Stage 1 | `ComboStreakTracker` in `:domain` + `ComboBanner` in Flashcards, Academic SRS & Scenarios |
| **УИ-3.3** Blinking terminal cursor `_` in search, thematic loading statuses, dynamic language accent color | `ТЗ-3.2 §4.3`, `MP-3.1 §3.4` | Implemented & Verified in Stage 1 | `TerminalSearchField`, `SkeletonListPlaceholder`, dynamic language accent in `ProgrammingScreens.kt` |
| **НФТ-7.5** Accessibility: contrast ≥ 4.5:1 across all themes, font scale up to 200%, TalkBack `contentDescription`, `reduceMotion` disables animations | `ТЗ-3.2 §6.3`, `MP-3.1 §3.4` | Implemented & Verified in Stage 1 | `LocalTechInteractionConfig` + `reduceMotionEnabled` in DataStore + automated WCAG AA & `fontScale = 2.0f` tests |

---

## 3. Stage 2 Traceability — Content Expansion, `MIGRATION_2_3`, `ContentPackImporter` & Offline Tools

| Requirement | Source | Current Status | Planned Action in Stage 2 |
|---|---|---|---|
| **4.1 / DB v3** `MIGRATION_2_3` (`custom_imported_docs`, `time_attack_records`, export `3.json`) + `ContentPackImporter` (Errata **E3**) | `ТЗ-3.2 §6.2`, `MP-3.1 §2 E3, §4.1` | Planned for Stage 2 | Upgrade Room to v3, implement lossless `MIGRATION_2_3` & idempotent `ContentPackImporter` preserving SRS fields |
| **КД-1** 20,000–30,000 verified academic terms + 6 new domains | `ТЗ-3.2 §2.1–2.2`, `MP-3.1 §4.2` | Planned for Stage 2 | Expand verified corpus & domain packs |
| **КД-2 / E6** Verified IPA ≥ 5,000 (documented licensed sources) + `translation_tg` ≥ 2,000 + RU/TJ switch | `ТЗ-3.2 §2.2`, `MP-3.1 §2 E6, §4.2` | Planned for Stage 2 | Add licensed IPA sources, verified Tajik translations (or stop & report deficit per §0.1), add RU/TJ toggle in DataStore |
| **КД-3** `doc_articles` ≥ 150 (stack traces, crash logs, RFCs, READMEs with `source_url`, `license`, PII-free) | `ТЗ-3.2 §2.2`, `MP-3.1 §4.2` | Planned for Stage 2 | Populate ≥ 150 articles & error logs |
| **КД-4** Scenarios ≥ 150 (≥ 12 per context, 4 new contexts incl. `CLIENT_COMMUNICATION` → Slack) & Phrases ≥ 300 | `ТЗ-3.2 §2.2`, `MP-3.1 §4.2` | Planned for Stage 2 | Populate ≥ 150 scenarios & ≥ 300 phrases |
| **КД-5** Snippets ≥ 350 across 20 `task_key` × 19 languages | `ТЗ-3.2 §2.2`, `MP-3.1 §4.2` | Planned for Stage 2 | Close gaps in existing 10 keys & add 10 new keys |
| **НФТ-7.6** Base AAB ≤ 100 MB, Play Asset Delivery / OTA packs, launch ≤ 5s | `ТЗ-3.2 §6.3`, `MP-3.1 §4.3` | **Implemented (v3.0.0)** | `app-release.aab = 26.87 MB` (`base/` 26.61 MB); emulator launch `4414 ms` (`bundletool get-size`: не измерено) |
| **ФТ-7.1** Read Your Own Docs (`.md`/`.txt`/paste, off-main-thread multi-word/inflected term scanner) | `ТЗ-3.2 §3.1`, `MP-3.1 §4.4` | **Implemented (Stage 2)** | `ScanDocumentForTermsUseCase`, `ReadYourOwnDocsScreen`, `custom_imported_docs` |
| **ФТ-7.2** Interactive «Debug the Log» trainer (Terminal theme) | `ТЗ-3.2 §3.1`, `MP-3.1 §4.4` | **Implemented (Stage 2)** | `DebugTheLogScreen`, `AnalyzeLogForBugReportUseCase` |
| **ФТ-7.3** On-device Speaking / Shadowing trainer (`createOnDeviceSpeechRecognizer`, API 33+, `RECORD_AUDIO`) | `ТЗ-3.2 §3.1`, `MP-3.1 §4.4` | **Implemented (Stage 2)** | `PronunciationTrainerScreen`, `ScorePronunciationUseCase` |
| **ФТ-7.4** Jetpack Glance Home Screen Widget (Word of the Day, due count, Streak, 1-tap review) | `ТЗ-3.2 §3.1`, `MP-3.1 §4.4` | **Implemented (Stage 2)** | `TechEnglishGlanceWidget`, `TechEnglishWidgetReceiver` |
| **ФТ-7.5** Time Attack / Speedrun Quiz (60s session, `time_attack_records`) | `ТЗ-3.2 §3.1`, `MP-3.1 §4.4` | **Implemented (Stage 2)** | `TimeAttackScreen`, `TimeAttackQuizUseCase`, `time_attack_records` |
| **ФТ-7.6** Deck Import & Export (JSON / CSV / Anki `.apkg` & TSV without Anki schedule) | `ТЗ-3.2 §3.1`, `MP-3.1 §4.4` | **Implemented (Stage 2)** | `DeckImportExportScreen`, `DeckImportExportUseCase`, `AnkiApkgExtractor` |

---

## 4. Stage 3 Traceability — Backend, `MIGRATION_3_4`, Security, Sync & Social Core

| Requirement | Source | Current Status | Implementation & Verification |
|---|---|---|---|
| **5.1 / Stack** ADR `DEC-09..12`, `backend/` (FastAPI + PostgreSQL + Redis + Docker Compose + pytest), Ktor Client, Keystore AES-256-GCM + DataStore | `ТЗ-3.2 §6.1`, `MP-3.1 §5.1` | **Implemented (Stage 3)** | `backend/`, `TechEnglishApiClient`, `AndroidKeystoreTokenStore` (no Retrofit, no Supabase, no `EncryptedSharedPreferences`) |
| **5.2 / DB v4** `MIGRATION_3_4` (`sync_queue`, `user_profile_cache`, `local_account_link`, `leaderboard_cache`, `friends_cache`, `activity_feed_cache`, `badges_cache`, `ota_content_meta`) | `ТЗ-3.2 §6.2`, `MP-3.1 §5.2` | **Implemented (Stage 3)** | `Stage3Entities.kt`, `Stage3Daos.kt`, `4.json`, tested in `TechEnglishDatabaseTest` |
| **ФТ-9.1** GitHub/Google OAuth (PKCE), Email/password, Guest Mode + seamless account linking | `ТЗ-3.2 §5.1`, `MP-3.1 §5.3` | **Implemented (Stage 3)** | `CloudAccountSyncScreen`, `/v1/auth/*`, `MergeGuestIntoAccountUseCase`, `test_api_stage3.py` |
| **ФТ-9.2 / E1** Outbox sync engine (`event_id` UUID) & Errata **E1** SM-2 conflict resolution by `client_timestamp` in `:domain` | `ТЗ-3.2 §5.1`, `MP-3.1 §2 E1, §5.3` | **Implemented (Stage 3)** | `CloudSyncEngine`, `ResolveSm2SyncConflictUseCase`, `backend/app/anti_cheat.py` |
| **ФТ-9.3** Server-side Anti-Cheat (ranked XP computed by server, rate limits, server-issued questions, Play Integrity interface) | `ТЗ-3.2 §5.1`, `MP-3.1 §5.3` | **Implemented (Stage 3)** | `ValidateRankedEventAntiCheatUseCase`, `backend/app/anti_cheat.py`, `backend/app/security.py` |
| **ФТ-9.4 / E2** 16+ Age Gate, Privacy Policy, Account Deletion, Profile Moderation (report, block, queue, `@username` filter) | `ТЗ-3.2 §5.1`, `MP-3.1 §2 E2, §5.3` | **Implemented (Stage 3)** | `AgeGate16PlusScreen`, `PrivacyPolicyDialog`, `DELETE /v1/account`, `/v1/moderation/*` |
| **ФТ-10.1–10.3** Developer Profile Card, Ranks (`Intern` → `Principal`), 365-day graph, domain mastery %, Badges | `ТЗ-3.2 §5.2`, `MP-3.1 §5.4` | **Implemented (Stage 3)** | `DeveloperProfileCardScreen`, `badges_cache`, `GitHubContributionGraph` |
| **ФТ-11.1–11.2** Segmented Top-10 Leaderboards (Weekly reset Monday 00:00 `Asia/Dushanbe` UTC+5, 5 Leagues `Bronze`..`Diamond`, Friends, Stack) | `ТЗ-3.2 §5.3`, `MP-3.1 §5.4` | **Implemented (Stage 3)** | `LeaderboardsScreen` (4 tabs + offline cache badge), `CalculateWeeklyLeagueUseCase`, `/v1/leaderboards/{tab}` |
| **ФТ-12.1–12.3** Friends (`@username`, QR, deep-link), Activity Feed with reactions, Ping/Nudge (1/24h/friend + opt-out) | `ТЗ-3.2 §5.4`, `MP-3.1 §5.4` | **Implemented (Stage 3)** | `FriendsAndFeedScreen`, `ValidateSocialActionsUseCase`, `/v1/friends/*` |
| **ФТ-14.5 / E4** Dynamic OTA Content Delivery (checksummed data-only JSON packs into Room via `ContentPackImporter`) | `ТЗ-3.2 §5.6`, `MP-3.1 §2 E4, §5.4` | **Implemented (Stage 3)** | `CloudSyncEngine.syncOtaContentPacks()`, `/v1/ota/*`, `ota_content_meta` |

---

## 5. Stage 4 Traceability — Community, AI & Realtime (Waves 4A → 4B → 4C)

| Requirement | Source | Current Status | Implementation & Verification |
|---|---|---|---|
| **DB v5** `MIGRATION_4_5` (`comments_cache`, `community_decks`, `community_deck_terms`, `room_messages_cache`) | `ТЗ-3.2 §6.2`, `MP-3.1 §6` | **Implemented (Wave 4A)** | `Stage4AEntities.kt`, `Stage4ADaos.kt`, `5.json`, lossless test in `TechEnglishDatabaseTest` |
| **ФТ-13.1** StackOverflow-style Comments & Mnemonics on terms & code snippets (`TERM`/`SNIPPET`) + UGC moderation | `ТЗ-3.2 §5.5`, `MP-3.1 §6 (4A)` | **Implemented (Wave 4A)** | `CommentsMnemonicsScreen` (wired to `AcademicTermDetailScreen` & `CodeBreakdownScreen`), `/v1/comments/*`, `test_api_stage4a.py` |
| **ФТ-13.2** Community Decks Exchange (publish from «Мои слова», 1-tap download to Room) + UGC moderation | `ТЗ-3.2 §5.5`, `MP-3.1 §6 (4A)` | **Implemented (Wave 4A)** | `CommunityDecksScreen`, `PrepareDeckForLocalImportUseCase`, `/v1/decks/*`, `test_api_stage4a.py` |
| **ФТ-8.1–8.3** AI Mock Interviewer & AI Code/PR Reviewer via FastAPI proxy + quotas, consent, zero PII, zero audio storage | `ТЗ-3.2 §3.2`, `MP-3.1 §6 (4B)` | **Implemented (Wave 4B)** | `AiMentorScreen`, `SanitizeAiPromptPiiUseCase`, `backend/app/ai_proxy.py`, `/v1/ai/*`, `test_api_stage4b.py` |
| **ФТ-14.3** Vocabulary Crowdsourcing («PR в словарь», Middle+, ≥14d, ≥100 terms, 0 reports, Contributor badge, CC BY-SA 4.0) | `ТЗ-3.2 §5.6`, `MP-3.1 §6 (4B)` | **Implemented (Wave 4B)** | `CrowdsourcePrScreen`, `CheckCrowdsourceEligibilityUseCase`, `/v1/crowdsource/*`, `test_api_stage4b.py` |
| **ФТ-14.4** Study Squads (5–10 members, weekly goal, non-ranked XP multiplier per `ФТ-9.3`) | `ТЗ-3.2 §5.6`, `MP-3.1 §6 (4B)` | **Implemented (Wave 4B)** | `SquadsScreen`, `EvaluateSquadWeeklyGoalUseCase`, `/v1/squads/*`, `test_api_stage4b.py` |
| **ФТ-13.3** Thematic Rooms (`#code-review-practice`, `#daily-standup`, `#interviews`, `#backend-english`) on Ktor WebSockets | `ТЗ-3.2 §5.5`, `MP-3.1 §6 (4C)` | **Implemented (Wave 4C)** | `ThematicRoomsScreen`, `TechEnglishApiClient.observeRoomWebSocket`, `/ws/rooms/{slug}`, `test_api_stage4c.py` |
| **ФТ-14.1** 1v1 Realtime PvP Duels (60s, 8 server questions, disconnect handling, 15s timeout training fallback) | `ТЗ-3.2 §5.6`, `MP-3.1 §6 (4C)` | **Implemented (Wave 4C)** | `PvpDuelScreen`, `EvaluatePvpDuelUseCase`, `/v1/pvp/*`, `/ws/pvp/{match_id}`, `test_api_stage4c.py` |
| **ФТ-14.2 / E5** Practice Arena (Peer Review: daily prompt, text & ≤20s voice clip in S3/MinIO with 30d TTL per `DEC-13`) | `ТЗ-3.2 §5.6`, `MP-3.1 §2 E5, §6 (4C)` | **Implemented (Wave 4C)** | `DEC-13`, MinIO in `docker-compose.yml`, `voice_storage.py`, `PeerReviewArenaScreen`, `test_api_stage4c.py` |

---

## 6. v3.1.0 Remediation Traceability (Audit Defects `BUG-01`–`BUG-26` & Rules `R1`–`R12`)

| Task | Defect / Rule | Sprint | Status | Implementation & Automated Verification |
|---|---|---|---|---|
| **S0-01..03** | `R1`, `R2`, `R5` | Sprint 0 (`v3.1-sprint0`) | **Verified** | Baseline measurements (`REMEDIATION_LOG.md`), Alembic `0001_initial_v3_0_0`, `.github/workflows/ci.yml` (PG 16, Redis 7, JaCoCo ≥ 90% gate) |
| **SP1-01** | `BUG-02`, `BUG-07` | Sprint 1A (`v3.1-sprint1a`) | **Verified** | SHA-256 single-use 15m auth tokens, 5-attempt lockout, `EmailSender`, Alembic `0002_auth_token_security`, prod fail-fast (`test_sp1_01_auth_tokens.py`) |
| **SP1-02** | `BUG-03`, `R1` | Sprint 1A (`v3.1-sprint1a`) | **Verified** | `users.role` (`0003_user_role_rbac`), `require_moderator`, no self-approve PR, `app/cli_admin.py` (`test_sp1_02_rbac.py`) |
| **SP1-03a** | `BUG-01`, `BUG-04` | Sprint 1A (`v3.1-sprint1a`) | **Verified** | Server-side OAuth (`/authorize`→`/callback`→`/ticket/exchange`), `oauth_identities` (`0004_oauth_identities`), `(provider, sub)` binding, `410 Gone` on legacy `/exchange` (`test_sp1_03a_oauth.py`) |
| **SP1-04** | `BUG-05` | Sprint 1A (`v3.1-sprint1a`) | **Verified** | Async `HttpxGeminiClient` with timeouts, Pydantic validation, TTL cache, PII scrubbing, honest `provider`/`is_fallback` (`test_sp1_04_gemini_proxy.py`) |
| **SP1-05** | `BUG-06` (server) | Sprint 1A (`v3.1-sprint1a`) | **Verified** | `S3VoiceObjectStorage` (MinIO/S3, 30d lifecycle, ≤20s/≤512KB), presigned GET `/voice-url`, hard delete on `DELETE /v1/account` (`test_sp1_05_s3_voice_storage.py`) |
| **SP1-07a** | `BUG-07`, `BUG-10`, `R11` | Sprint 1A (`v3.1-sprint1a`) | **Verified** | Redis client (`REDIS_URL` + `fakeredis`), `POST /v1/ws/ticket` (30s TTL, atomic `GETDEL`) (`test_sp1_07a_redis_ws_ticket.py`) |
| **SP1-11** | `BUG-07`, `R3` | Sprint 1A (`v3.1-sprint1a`) | **Verified** | `ClientIntegrityVerifier` + `GooglePlayIntegrityVerifier`, prod rejection of fake verifier (`test_sp1_11_play_integrity.py`) |
| **SP1-03b** | `BUG-04` (client) | Sprint 1B (`v3.1-sprint1b`) | **Verified** | Custom Tabs + `techenglish://auth/callback`, `OAuthCallbackHandler` (`client_state` in DataStore) (`Sp103bOAuthClientFlowTest`) |
| **SP1-06** | `BUG-08`, `R10` | Sprint 1B (`v3.1-sprint1b`) | **Verified** | Bidirectional sync (`events`/`sm2_updates`/`favorite_terms`/`custom_words`), R10/E1 conflict resolution in `:domain`, Ktor `Auth` 401 refresh & timeouts (`Sp106SyncAndKtorAuthTest`) |
| **SP1-07b** | `BUG-10`, `BUG-12`, `BUG-16` | Sprint 1B (`v3.1-sprint1b`) | **Verified** | Removed `PLAIN:` Keystore fallback, `allowBackup="false"`, removed `CAMERA`, debug `network_security_config.xml`, WS `?ticket=` only (`Sp107bSecurityHardeningTest`) |
| **SP1-08** | `BUG-11` | Sprint 1B (`v3.1-sprint1b`) | **Verified** | R8 minification & resource shrinking, ProGuard rules, null-safe env `signingConfigs.release`, fixed duplicate KSP (`Sp108ReleaseBuildConfigTest`) |
| **SP1-09** | `BUG-13`, `BUG-16` | Sprint 1B (`v3.1-sprint1b`) | **Verified** | SHA-256 WAV cache key + LRU, `ProcessLifecycleOwner` TTS cleanup, `MainActivity.onNewIntent` deep-link handling (`Sp109AudioCacheAndDeepLinkTest`) |
| **SP1-10** | `BUG-14`, `BUG-19` | Sprint 1B (`v3.1-sprint1b`) | **Verified** | Active theme syntax highlighting, `/v1/crowdsource/eligibility` + `targetType="DICTIONARY_PR"` (`Sp110ThemeAndCrowdsourceFixesTest`) |
| **SP2-01** | `BUG-17` | Sprint 2A (`v3.1-sprint2a`) | **Verified** | Decomposed `main.py` (2,847 → 125 lines) into `deps.py`, 4 `schemas/`, 2 `services/`, 10 `routers/`, `lifespan`, `create_app()` (`test_sp2_01_decomposition.py`) |
| **SP2-02** | `BUG-07` | Sprint 2A (`v3.1-sprint2a`) | **Verified** | `RedisRealtimeBroker` for PvP matchmaking queue & Room/PvP Pub/Sub across multi-worker instances (`test_sp2_02_redis_realtime.py`) |
| **SP2-10** | `R3` (FCM Push) | Sprint 2A (`v3.1-sprint2a`) | **Verified** | `DevicePushTokenRow` (`0005_device_push_tokens`), `FcmHttpV1PushNotificationGateway`, `/v1/notifications/device-token`, friend ping push, Android `PushNotificationGateway` (`test_sp2_10_push_fcm.py`) |
| **SP2-03** | `BUG-18` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | Split all Compose files > 900 lines (0 remaining), `@Immutable` + `ImmutableList`, Compose Compiler stability reports (`Sp203ComposeStructureTest`) |
| **SP2-04** | `BUG-15`, `R6`, `R12` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | ADR `DEC-14` (`@Fts4` + `normalized_search`), Room v6 `MIGRATION_5_6`, rebuilt v6 seed DB, 30k search `8.13 ms` `< 15 ms` (`Sp204FtsAndCyrillicSearchTest`) |
| **SP2-05** | `BUG-15`, `R4`, `R5` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | `install-time` PAD module `:content_pack` (`base = 5.16 MiB`, `content_pack = 10.75 MiB`), off-main-thread `DatabaseBootstrapManager` (`514 ms` / `0 ms`) (`Sp205AssetPackAndBootstrapTest`) |
| **SP2-06** | `BUG-21` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | `CommunityArenaHubScreen`, `ProfileScreen` 12 → 5 sections, removed 3 duplicates from `PracticeHubScreen` (`Sp206NavigationAndHubTest`) |
| **SP2-07** | `BUG-24` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | `TechEnglishGlanceWidget` synced to active `ThemeMode` + `"↻"` refresh action (`Sp207GlanceWidgetThemeAndRefreshTest`) |
| **SP2-08** | `BUG-25` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | `activity_feed_reactions` (`0006_activity_feed_reactions`), `user_reacted` preservation, `ota_content_meta` diagnostics & corrupt pack retry (`test_sp2_08_feed_user_reacted.py`, `Sp208FeedAndOtaDiagnosticsTest`) |
| **SP2-09** | `BUG-26` | Sprint 2B (`v3.1-sprint2b`) | **Verified** | PII-safe `CrashReporter` behind build flags (default `false`, 0 secrets) + CI upload of Compose stability reports (`Sp209CrashReporterAndCiTest`) |
| **SP3-01** | `BUG-09` | Sprint 3 (`v3.1-sprint3`) | **Verified** | Stripped `correct_option_index` until `answer_ack`; live Android PvP over `/ws/pvp/{match_id}` (`?ticket=`) + 15s unranked fallback (`test_sp3_01_pvp_no_answer_leak.py`, `Sp301PvpLiveWsTest`) |
| **SP3-02** | `BUG-06` (client) | Sprint 3 (`v3.1-sprint3`) | **Verified** | `AndroidPeerVoiceClipRecorder` (M4A ≤ 20s / ≤ 512 KB), on-demand `RECORD_AUDIO` prompt, presigned playback via `TechAudioController.playRemoteAudioUrl` (`Sp302PeerReviewVoiceTest`) |
| **SP3-03** | `BUG-20`, `R7` | Sprint 3 (`v3.1-sprint3`) | **Verified (R7)** | `values-tg/strings.xml` behind `TAJIK_UI_LOCALIZATION_ENABLED` (default `false`) + RU fallback, 150 `pending_review` drafts in `docs/tg_review/`, strict `scripts/import_verified_tg.py`; release DB retains strictly **19** human-verified entries (deficit `1,981` honestly reported per R7) (`Sp303TajikLocalizationPolicyTest`) |
| **SP3-04** | `BUG-19` | Sprint 3 (`v3.1-sprint3`) | **Verified** | Offline room message outbox (`ROOM_MESSAGE_OUTBOX` in `sync_queue`), truthful `WsConnectionState`, `squad_cache` Room v6 entity/DAO wired to `SquadsViewModel` (`Sp304OfflineRoomsAndSquadCacheTest`) |
| **SP3-07** | `BUG-22`, `R8` | Sprint 3 (`v3.1-sprint3`) | **Verified** | Version `3.1.0` (`310`), split `Stage4CViewModels.kt`, updated `OWNER_ACTIONS.md` + Play Console Data Safety checklist, release APK/AAB & `TechEnglish_Source_Clean.zip` |
| **Out of scope / Optional** | `BUG-23`, `SP3-05`, `SP3-06` (`R9`) | — | **Excluded per prompt & R9** | `BUG-23` excluded from remediation scope; `SP3-05` (KMP/iOS prototype) and `SP3-06` (tap-glossary & onboarding) deferred pending explicit owner `go` per R9 |
