# Отчёт по Спринту 1B — Android: OAuth-клиент, Sync, защита, сборка и точечные фиксы (`SPRINT_1B_REPORT.md`)

**Ветка:** `remediation/v3.1`  
**Тег:** `v3.1-sprint1b`  
**Дата:** 2026-11-23

---

## 1. Выполненные задачи и коммиты (в порядке плана)

| Задача | Дефекты | Коммит | Ключевые изменения |
|---|---|---|---|
| **SP1-03b** | `BUG-04` (клиент) | `6171b4a` | Подключён Android Custom Tabs (`androidx.browser:browser:1.8.0`); в `AndroidManifest.xml` добавлен `intent-filter` для `techenglish://auth/callback` и `launchMode="singleTop"`; реализован `OAuthCallbackHandler` с хранением `client_state` строго в DataStore, отклонением чужого/повторного `state` и обменом одноразового `ticket` на JWT через `POST /v1/auth/oauth/ticket/exchange`. |
| **SP1-06** | `BUG-08`, `R10` | `5983416` | Реализован двусторонний sync в `CloudSyncEngine`: push `events`, `sm2_updates`, `favorite_terms`, `custom_words` и pull `GET /v1/sync/state` с разрешением конфликтов по R10/E1 (`ResolveSm2SyncConflictUseCase` в `:domain`); в `TechEnglishApiClient` добавлены Ktor `Auth` (`bearer`) с авто-refresh через `/v1/auth/refresh` при `401` и `HttpTimeout`; зафиксирована правка R10 в `docs/REQUIREMENTS.md` и `docs/DECISIONS.md`. |
| **SP1-07b** | `BUG-10`, `BUG-12`, `BUG-16` | `99c3192` | Удалён `PLAIN:`-фолбэк из `AndroidKeystoreTokenStore` (при сбое Keystore выполняется пересоздание ключа и мягкий выход `clearSession()`); выставлены `allowBackup="false"`, `data_extraction_rules.xml`, `backup_rules.xml`; удалены `CAMERA` и `hardware.camera`; добавлен debug-only `src/debug/res/xml/network_security_config.xml` для `10.0.2.2`/`localhost`; Ktor WebSocket переведён на одноразовый WS-тикет `POST /v1/ws/ticket`, а временный `?token=` fallback удалён на сервере. |
| **SP1-08** | `BUG-11` | `0850ddb` | Включены `isMinifyEnabled = true` и `isShrinkResources = true` в `release`; настроены правила `app/proguard-rules.pro` (Room, Hilt/WorkManager, Ktor/OkHttp, Kotlinx Serialization, Glance); исправлен дубль `ksp(libs.hilt.compiler)` → `ksp(libs.androidx.hilt.compiler)`; добавлен null-safe `signingConfigs.release` из env (`KEYSTORE_PATH` и др.), собирающий unsigned `.aab`/`.apk` при отсутствии ключей. |
| **SP1-09** | `BUG-13`, `BUG-16` | `c8f5466` | Ключ кэша WAV в `TechAudioController` переведён на `SHA-256(locale\|speed\|text)` с LRU-вытеснением (`10 MiB` / `40` файлов); `release()` привязан к `ProcessLifecycleOwner` в `TechEnglishApplication`; `MainActivity.onNewIntent()` обрабатывает `EXTRA_DESTINATION` из виджета и уведомлений. |
| **SP1-10** | `BUG-14`, `BUG-19` | `8f971d4` | Подсветка кода в `ProgrammingScreens.kt` использует `LocalTechInteractionConfig.current.isDarkTheme` вместо `isSystemInDarkTheme()`; жалоба на PR отправляет `targetType="DICTIONARY_PR"` (добавлен на сервере); допуск к краудсорсингу в `CrowdsourcePrViewModel` загружается с `/v1/crowdsource/eligibility` вместо подмены `streakDays`. |

---

## 2. Фактически измеренные результаты проверок

### 2.1. Тесты и покрытие `:domain`

| Модуль / набор | Команда | Результат |
|---|---|---|
| `:domain` (JVM unit + JaCoCo ≥ 90%) | `./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport :domain:jacocoTestCoverageVerification` | **57 / 57 PASSED** (`0` failed, `0` skipped); Line **2844 / 3007 = 94.58%**, Instruction **88.13%**, Branch **73.83%**, гейт ≥ 90% **PASSED** |
| `:app` (Robolectric + Room + Compose UI + Ktor MockEngine) | `./gradlew :app:testDebugUnitTest` | **49 / 49 PASSED** (`0` failed, `0` skipped; 36 базовых + 13 новых тестов Спринта 1B в 5 тестовых классах) |
| `backend/` (`pytest`) | `cd backend && .venv/bin/pytest -v` | **29 PASSED, 1 SKIPPED** (`15.40s`; включая проверку отказа `?token=<jwt>` на WS кодом `1008`) |
| **ИТОГО локально выполнено** | | **135 / 135 PASSED** (`0` failed) |

### 2.2. Сборка Release AAB с R8 (`SP1-08`) и Merged Release Manifest (`SP1-07b`)

- **`./gradlew :app:bundleRelease` (без `KEYSTORE_PATH` → unsigned `.aab`):** **BUILD SUCCESSFUL** (`4m 22s`, `minifyReleaseWithR8` + `shrinkBundleReleaseResources` без ошибок).
- **Изменение размеров `app-release.aab` до и после R8:**
  - Общий размер `app-release.aab`: `26.87 MiB` → **19.16 MiB** (`20 092 530` Б, из которых `5.10 MiB` — `mapping.txt` в `BUNDLE-METADATA` для Play Console, удаляемый перед доставкой пользователю).
  - `base/dex`: `15.79 MiB` сжатый / `55.32 MiB` несжатый → **3.06 MiB сжатый / 6.77 MiB несжатый** (**−87.8%** несжатого DEX!).
  - `base/assets` (`tech_english.db`): `8.67 MiB` сжатый / `86.37 MiB` несжатый (вынос в PAD запланирован на `SP2-05`).
- **Merged Release Manifest (`processReleaseMainManifest`):** подтверждено отсутствие `android.permission.CAMERA` и `android.hardware.camera`, наличие `android:allowBackup="false"`, `dataExtractionRules` и `techenglish://auth/callback`.

---

## 3. Неизмеренное / E2E без ключей (оговорки §0.1 и §0.2)

- **Реальный OAuth E2E с GitHub / Google (`SP1-03b`):** E2E не проверено, нужны ключи (`GITHUB_CLIENT_ID`/`SECRET`, `GOOGLE_CLIENT_ID`/`SECRET`); deep link, сохранение `state` в DataStore, отклонение чужого `state` и обмен `ticket` проверены Robolectric-тестом.
- **Смоук-тест R8 release `.aab` через `bundletool` на AVD (`SP1-08`):** не измерено (нет `bundletool` и запущенного AVD в окружении).
- **Релизная подпись AAB (`SP1-08`):** собрана неподписанная сборка, так как production `release.keystore` и env-переменные подписи предоставляет владелец (`docs/OWNER_ACTIONS.md` п. 6).

---

## 4. Статус DoD Спринта 1B

- [x] Все тесты `:domain` (57), `:app` (49) и `backend` (29) зелёные (`135 / 135`), покрытие `:domain` **94.58%** (≥ 90%).
- [x] Все P0-дефекты (`BUG-01..BUG-07`) и Sprint 1B P1-дефекты (`BUG-08`, `BUG-10`, `BUG-11`, `BUG-12`, `BUG-13`, `BUG-14`, `BUG-16`, часть `BUG-19`) закрыты в коде и подтверждены автотестами (остающиеся UI/Realtime части `BUG-06`/`BUG-07`/`BUG-09` запланированы на Спринты 2A и 3 согласно плану).
- [x] Тег `v3.1-sprint1b` готов; работа остановлена до подтверждения **`go`** для перехода к **Спринту 2A**.
