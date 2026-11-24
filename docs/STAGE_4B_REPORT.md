# Отчёт по Этапу 4B — TechEnglish v3.0 (AI-прокси, Squads и Краудсорсинг «PR в словарь»)

**Дата:** 2026-11-19  
**Статус:** Выполнено, проверено тестами, сборками (`Debug APK`, `Release AAB`, `pytest`) и smoke-тестом на эмуляторе  
**Тег:** `v3-stage4b`

---

## 1. Реализованный функционал по спецификации (`techenglish_v3_2_final.md` §3.2, §5.6, §9 и Master Prompt §6)

### 1.1. AI Mock Interviewer и AI Code/PR Reviewer через серверный FastAPI-прокси (`ФТ-8.1`–`8.3`)
- **Серверная часть (`backend/app/ai_proxy.py`, `backend/app/main.py`, `backend/app/models.py`):**
  - В APK и `:domain` **полностью отсутствуют** API-ключи и прямые вызовы к Gemini — все запросы идут исключительно через FastAPI-прокси (`/v1/ai/quota`, `/v1/ai/interview`, `/v1/ai/code-review`).
  - Обязательное явное согласие (`consent_accepted = true`, иначе `400 Bad Request`), хранимое локально в DataStore (`UserSettings.aiConsentAccepted`).
  - Автоматическое удаление PII (`scrub_pii` на сервере и `SanitizeAiPromptPiiUseCase` в `:domain`): email, телефоны, `@handles` и токены заменяются до передачи в LLM; поля профиля в промпты не попадают.
  - Серверный дневной лимит (`MAX_AI_REQUESTS_PER_DAY = 15`, таблица `ai_usage_logs`, при превышении — `429 Too Many Requests`).
  - Голосовой ввод на Android распознаётся локально (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`), на сервер уходит только текст; аудио не сохраняется (`audio_stored = false`).
- **Клиент (`feature/stage4b/Stage4BViewModels.kt`, `Stage4BScreens.kt`):**
  - `AiMentorScreen` с экраном явного согласия, индикатором дневной квоты, вкладками *Mock Interview* (5 ролей × 3 грейда + голосовой ввод) и *Code / PR Reviewer*. Доступен из `PracticeHubScreen` и `ProfileScreen`.

### 1.2. Учебные отряды Squads (`ФТ-14.4`, аудит #23)
- **Домен и сервер (`EvaluateSquadWeeklyGoalUseCase`, `SquadRow`, `SquadMemberRow`, `/v1/squads/*`):**
  - Команды строго от **5 до 10 разработчиков** (меньше 5 — челлендж не активен; 11-й участник отклоняется `409 Conflict`).
  - Общая недельная цель XP (цикл с понедельника 00:00 `Asia/Dushanbe` UTC+5); выполнение цели даёт бонус-множитель следующей недели (`1.25x`).
  - **Строгое соответствие `ФТ-9.3`:** множитель отряда **не влияет на рейтинговый XP лидербордов** (`affects_ranked_leaderboard_xp = false`; бонус начисляется отдельно в `squad_bonus_xp`).
- **Клиент:** `SquadsScreen` (создание отряда, вступление по инвайт-коду `SQ-XXXXXX`, прогресс-бар недельной цели, состав участников, выход).

### 1.3. Краудсорсинг «PR в словарь» (`ФТ-14.3`, аудит #22 и §9)
- **Домен и сервер (`CheckCrowdsourceEligibilityUseCase`, `ValidateDictionaryPrUseCase`, `DictionaryPrRow`, `DictionaryPrVoteRow`, `/v1/crowdsource/*`):**
  - 4 строгих условия допуска (проверяются и в UI, и на сервере с возвратом `403 Forbidden`):
    1. Ранг **Middle Developer и выше** (`≥ 600 XP`)
    2. Возраст аккаунта **≥ 14 дней**
    3. **≥ 100 изученных терминов** (`repetitions >= 1`)
    4. **0 подтверждённых жалоб** модерации
  - Обязательное подтверждение публикации вклада под открытой лицензией проекта **CC BY-SA 4.0** (`license_accepted = true`, иначе `422`).
  - Голосование сообщества, одобрение модерацией с выдачей бонусного XP и разблокировкой бейджа **Contributor** в `/v1/profile/me`.
  - `DELETE /v1/account` каскадно удаляет `ai_usage_logs`, членство в `squad_members`, `dictionary_pr_votes` и `dictionary_prs`.
- **Клиент:** `CrowdsourcePrScreen` с чек-листом 4 условий допуска, формой отправки PR (RU/TJ + источник + чекбокс лицензии CC BY-SA 4.0), списком заявок, голосованием и кнопкой жалобы.

---

## 2. Фактически измеренные показатели

| Метрика | Измеренное значение | Требование / Порог | Статус |
|---|---|---|---|
| Тесты FastAPI Backend (`pytest`) | **8 / 8 PASSED** (5,61 с) | Все пройдены | PASS |
| Тесты `:domain` (JVM JUnit) | **55 / 55 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Покрытие `:domain` (JaCoCo Line) | **94,29% CSV** (2677/2839), **94,33% XML** (2677/2838) | ≥ 90% | PASS |
| Тесты `:app` (Robolectric + Compose UI + Room) | **33 / 33 PASSED** (0 failed, 0 skipped) | Все пройдены | PASS |
| Суммарно автоматических тестов | **96 / 96 PASSED** | 0 падений | PASS |
| Поиск по 30 000 терминов (`exactLookupMs`) | **2 мс** | < 100 мс | PASS |
| Фильтрация с пагинацией на 30 000 (`paginatedFilterMs`) | **15 мс** | < 100 мс | PASS |
| Проверка клиентских секретов AI в `app/src/` и `domain/src/` | **0 реальных ключей/URL Gemini** (только учебный пример вопроса викторины Time Attack) | 0 API-ключей в APK | PASS |
| Размер `app-debug.apk` | **47,73 МБ** (50 053 165 байт) | — | PASS |
| Размер `app-release.aab` | **26,77 МБ** (28 071 266 байт) | ≤ 100 МБ | PASS |
| Smoke-тест установки и запуска (`emulator-5554`, API 37) | Установлен (`Success`), `Status: ok`, `WaitTime: 2230 мс`, **0 ошибок** в logcat | Без крашей | PASS |
| Время холодного старта на реальном устройстве (Release) | не измерено (замер и `bundletool` в Этапе 4C) | ≤ 5 с | — |
