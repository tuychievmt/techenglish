# Отчёт по Этапу 4A — TechEnglish v3.0 (MIGRATION_4_5, Комментарии/мнемоники, Community Decks и модерация UGC)

**Дата:** 2026-11-19  
**Статус:** Выполнено, проверено тестами, сборками (`Debug APK`, `Release AAB`, `pytest`) и обновлением на эмуляторе  
**Тег:** `v3-stage4a`

---

## 1. Реализованный функционал по спецификации (`techenglish_v3_2_final.md` §5.5, §6.2, §9 и Master Prompt §6)

### 1.1. База данных Room v5 (`MIGRATION_4_5`)
- Добавлены 4 таблицы сообщества §6.2 (`Stage4AEntities.kt`, `Stage4ADaos.kt`, `TechEnglishDatabase.kt` version = 5):
  - `comments_cache` (`target_type` = `TERM` | `SNIPPET`, `target_id`, автор, ранг, `upvotes`, `downvotes`, `user_vote`, `is_hidden`, индекс `idx_comments_target`).
  - `community_decks` (название, описание, теги, автор, рейтинг, счётчик скачиваний, флаг `is_downloaded_locally`, `is_hidden`, индекс `idx_community_decks_rating`).
  - `community_deck_terms` (FK на `community_decks(id)` `ON DELETE CASCADE`, индекс `idx_deck_terms_deck_id`).
  - `room_messages_cache` (подготовлена в `MIGRATION_4_5` для волны 4C, индекс `idx_room_messages_room_created`).
- Экспортирована схема `app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/5.json`.
- Пересобран оффлайн-сид `app/src/main/assets/databases/tech_english.db` на версии Room 5 (`identityHash: cd74f367098fec999ef10e8f6c1c737f`, 31/31 обязательных индексов).

### 1.2. Чистый Kotlin JVM `:domain` (`Stage4AModels.kt`, `Stage4ACommunityUseCases.kt`)
- `ValidateUgcContentUseCase`: проверка длины комментариев (5–1000 символов), фильтр спама/запрещённых слов/избыточных ссылок, валидация заголовка/описания/тегов/состава колод (1–200 терминов, до 6 тегов).
- `SortAndFilterCommentsUseCase`: исключение скрытых комментариев и заблокированных авторов, сортировка *Лучшие* (`score = upvotes - downvotes DESC`) и *Новые*, идемпотентный переключатель голоса (`+1 / -1 / 0`).
- `PrepareDeckForLocalImportUseCase`: регистронезависимая дедупликация терминов колоды относительно существующей БД и подготовка пользовательских карточек для 1-тап импорта в «Мои слова».

### 1.3. Backend FastAPI (`backend/`)
- Новые ORM-таблицы в `backend/app/models.py`: `comments`, `comment_votes`, `community_decks`, `community_deck_terms`, `community_deck_ratings`.
- Расширена модерация UGC (`ФТ-9.4` + Errata E2):
  - `POST /v1/moderation/report` поддерживает `PROFILE`, `COMMENT`, `DECK`, `ROOM_MESSAGE` и автоматически скрывает комментарий или колоду при достижении 3 жалоб от разных пользователей (`AUTO_HIDE_REPORT_THRESHOLD = 3`).
  - `GET /v1/moderation/queue` — очередь открытых жалоб.
  - `DELETE /v1/account` каскадно удаляет все комментарии, голоса, опубликованные колоды, оценки и жалобы удалённого пользователя.
- Новые REST-эндпоинты:
  - **ФТ-13.1:** `GET /v1/comments/{target_type}/{target_id}?sort=top|newest`, `POST /v1/comments`, `POST /v1/comments/{comment_id}/vote`.
  - **ФТ-13.2:** `GET /v1/decks?tag=&query=`, `GET /v1/decks/{deck_id}`, `POST /v1/decks`, `POST /v1/decks/{deck_id}/download`, `POST /v1/decks/{deck_id}/rate`.

### 1.4. Android Ktor Client и Compose UI (`feature/stage4a/`)
- Расширен единый клиент `TechEnglishApiClient` (Ktor Client) методами комментариев, голосования, колод и типизированных жалоб.
- **`CommentsMnemonicsScreen` (`ФТ-13.1`):** формат StackOverflow с вертикальными кнопками голосов (`↑ / счёт / ↓`), переключением *Лучшие* / *Новые*, публикацией мнемоник, офлайн-кэшем и диалогом «Пожаловаться / Заблокировать автора». Интегрирован в `AcademicTermDetailScreen` (`TERM`) и `CodeBreakdownScreen` (`SNIPPET`).
- **`CommunityDecksScreen` (`ФТ-13.2`):** поиск, фильтрация по тегам, публикация колоды из категории «Мои слова», оценка 1–5★, скачивание в локальный Room в 1 тап (в «Мои слова» с дедупликацией) и диалог жалобы/блокировки автора. Интегрирован в `VocabularyScreen` и `ProfileScreen`.

---

## 2. Фактически измеренные показатели

| Метрика | Измеренное значение | Требование / Порог | Статус |
|---|---|---|---|
| Тесты FastAPI Backend (`pytest`) | **5 / 5 PASSED** (3,57 с) | Все пройдены | PASS |
| Тесты `:domain` (JVM JUnit) | **52 / 52 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Покрытие `:domain` (JaCoCo Line) | **94,10% CSV** (2534/2693), **94,13% XML** (2534/2692) | ≥ 90% | PASS |
| Тесты `:app` (Robolectric + Compose UI + Room) | **30 / 30 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Суммарно автоматических тестов | **87 / 87 PASSED** | 0 падений | PASS |
| Поиск по 30 000 терминов (`exactLookupMs`) | **2 мс** | < 100 мс | PASS |
| Фильтрация с пагинацией на 30 000 (`paginatedFilterMs`) | **12 мс** | < 100 мс | PASS |
| Миграции Room (`v1→v5`, `v2→v5`, `v3→v5`, `v4→v5`) | **100% без потерь** (проверено в `TechEnglishDatabaseTest`) | Без потери данных | PASS |
| Размер `app/src/main/assets/databases/tech_english.db` (v5) | **86,37 МБ** (90 562 560 байт) | Валидная Room v5 БД | PASS |
| Размер `app-debug.apk` | **47,73 МБ** (50 053 165 байт) | — | PASS |
| Размер `app-release.aab` | **26,67 МБ** (27 968 243 байт) | ≤ 100 МБ | PASS |
| Smoke-тест обновления v4 → v5 (`emulator-5554`, API 37) | Установлен (`Success`), `Status: ok`, `WaitTime: 2198 мс`, **0 ошибок** в logcat | Без крашей при обновлении | PASS |
| Время холодного старта на реальном устройстве (Release) | не измерено (замер в Этапе 4C) | ≤ 5 с | — |
