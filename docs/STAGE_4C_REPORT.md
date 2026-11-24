# Отчёт по Этапу 4C и финальной верификации — TechEnglish v3.0.0

**Дата:** 2026-11-19  
**Версия приложения:** `3.0.0` (`versionCode = 300`, `minSdk = 26`, `targetSdk = 35`)  
**Статус:** Выполнено, пройдены все тесты (`104/104`), финальные сборки (`Debug APK`, `Release Unsigned APK`, `Release AAB`), проверка на эмуляторе и упаковка чистого архива исходного кода  
**Теги:** `v3-stage4c`, `v3.0.0`

---

## 1. Реализованный функционал Этапа 4C (`techenglish_v3_2_final.md` §5.5–5.6, Errata E5, Master Prompt §6–7)

### 1.1. ADR по хранению голосовых записей (Errata E5 / `DEC-13`)
- До старта реализации оформлен `DEC-13` в `docs/DECISIONS.md`.
- В `backend/docker-compose.yml` добавлен S3-совместимый сервис **MinIO** (`techenglish-peer-voice`, порт `9000` / консоль `9001`).
- Реализована абстракция `backend/app/voice_storage.py` (`MAX_VOICE_SECONDS = 20`, `MAX_VOICE_BYTES = 512 KB`, `PEER_REVIEW_VOICE_TTL_DAYS = 30`).
- Обновлены `/v1/legal/privacy-policy` и `PrivacyPolicyDialog` в Android: чётко указано `0` дней для локальной диктовки произношения/AI и **30 дней TTL** для голосовых ответов Peer Review Arena.
- `DELETE /v1/account` синхронно удаляет все серверные данные пользователя **включая физическое удаление файлов голосовых записей** из хранилища (`deleted_voice_clips` проверен тестом `pytest`).

### 1.2. Тематические комнаты на WebSockets (`ФТ-13.3`)
- Backend: `GET /v1/rooms`, `GET/POST /v1/rooms/{room_slug}/messages`, WebSocket **`/ws/rooms/{room_slug}`** с JWT-авторизацией, валидацией UGC и авто-скрытием при 3 жалобах (`ROOM_MESSAGE`).
- Android: добавлен `ktor-client-websockets` (единый стек Ktor §6.1), `TechEnglishApiClient.observeRoomWebSocket(...)`, `ThematicRoomsViewModel` + `ThematicRoomsScreen` для 4 каналов (`#code-review-practice`, `#daily-standup`, `#interviews`, `#backend-english`), локальный кэш `room_messages_cache` для офлайна, диалог жалобы и блокировки автора на каждое сообщение.

### 1.3. 1v1 PvP-дуэли в реальном времени (`ФТ-14.1`, аудит #21)
- Domain: `EvaluatePvpDuelUseCase` (`60` с раунд, `15` с таймаут поиска, `0` рейтинговых очков в `TRAINING_FALLBACK`, корректная финализация `DISCONNECTED_FORFEIT` / `DISCONNECTED_OPPONENT_LEFT`).
- Backend: `POST /v1/pvp/matchmake` (выдаёт 8 серверных вопросов, переводит в `TRAINING_FALLBACK` без рейтинговых очков по запросу/таймауту), `POST /v1/pvp/{match_id}/submit`, `POST /v1/pvp/{match_id}/disconnect`, WebSocket **`/ws/pvp/{match_id}`** (корректно завершает матч при обрыве соединения).
- Android: `PvpDuelViewModel` + `PvpDuelScreen` с 15-секундным таймером поиска и переходом в тренировочный раунд, 60-секундным таймером раунда и корректной обработкой разрыва связи.

### 1.4. Peer Review Arena (`ФТ-14.2`, Errata E5)
- Domain: `ValidatePeerReviewSubmissionUseCase` (текст `15–1200` символов, голос `1–20` с, ≤ `512 КБ`, расчёт TTL `30` дней).
- Backend: `GET /v1/peer-review/daily-case`, `GET/POST /v1/peer-review/submissions` (текст + опциональный голосовой клип с TTL 30 дней), `POST /v1/peer-review/submissions/{id}/feedback`, модерация `PEER_REVIEW` с авто-скрытием и блокировкой.
- Android: `PeerReviewArenaViewModel` + `PeerReviewArenaScreen` с ежедневным кейсом, уведомлением о TTL 30 дней, лентой ответов сообщества, формой Peer Review (1–5★ + комментарий) и диалогом жалобы/блокировки.

---

## 2. Финальные фактически измеренные показатели (v3.0.0)

| Метрика | Измеренное значение | Порог / Требование | Статус |
|---|---|---|---|
| Тесты `:domain` (JVM JUnit) | **57 / 57 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Покрытие `:domain` (JaCoCo Line) | **XML: 94,58%** (2844/3007), **HTML: 94,58%** (2844/3007), **CSV: 94,55%** (2844/3008) | ≥ 90% | PASS |
| Тесты `:app` (Robolectric + Room + Compose UI) | **36 / 36 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Тесты FastAPI Backend (`pytest`, включая WebSockets и удаление голоса) | **11 / 11 PASSED** (6,84 с) | Все пройдены | PASS |
| Суммарно автоматических тестов | **104 / 104 PASSED** | 0 падений | PASS |
| Миграции Room (`v1→v5`, `v2→v5`, `v3→v5`, `v4→v5`) + `ContentPackImporter` | **100% без потерь** | Без потери данных | PASS |
| Поиск по 30 000 терминов (`exactLookupMs` / `paginatedFilterMs`) | **2 мс / 12 мс** | < 100 мс | PASS |
| `app-debug.apk` (`versionName=3.0.0`, `versionCode=300`) | **37,34 МБ** (39 150 954 байт) | Сборка без ошибок | PASS |
| `app-release-unsigned.apk` + `lintVitalRelease` | **27,19 МБ** (28 506 586 байт) | Сборка + Lint без ошибок | PASS |
| `app-release.aab` (`bundleRelease`) | **26,87 МБ** (28 177 402 байт); `base/` сжато **26,61 МБ** | ≤ 100 МБ | PASS |
| `bundletool get-size` | не измерено (`bundletool` не установлен в среде; факт. размер AAB 26,87 МБ / `base/` 26,61 МБ) | ≤ 100 МБ | — |
| Запуск v3.0.0 на `emulator-5554` (API 37) | `Status: ok`, `TotalTime: 4414 мс` (`WaitTime: 4429 мс`), **0 ошибок** в logcat | ≤ 5 000 мс | PASS |
| JankStats / Macrobenchmark на физическом устройстве | не измерено (нет подключённого физического устройства) | — | — |
| Ручная верификация 2 050 кандидатов `translation_tg` (Блокер 7) | 19 верифицированы в БД, 2 050 в `pending_review` JSON | Ожидает носителя языка | Отложено по §1 |
