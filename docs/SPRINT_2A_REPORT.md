# Отчёт по Спринту 2A (TechEnglish v3.0.0 → v3.1.0 Remediation)

- **Ветка:** `remediation/v3.1`
- **Тег подэтапа:** `v3.1-sprint2a`
- **Скоуп:** `SP2-01` (`BUG-17`), `SP2-02` (`BUG-07`), `SP2-10` (`R3`)

---

## 1. Выполненные задачи и коммиты

| Задача | Дефект / Правка | Коммит | Ключевые изменения |
|---|---|---|---|
| **SP2-01** | `BUG-17` | `24b7450` | Декомпозиция монолита `backend/app/main.py` (2 847 → **125 строк**, `< 180`) на `deps.py`, `schemas/` (`auth`, `sync`, `social`, `realtime`), `services/` (`startup`, `sync_service`) и **10 роутеров** (`ai`, `auth`, `community`, `crowdsource`, `peer_review`, `profile_moderation`, `realtime`, `social`, `squads`, `sync`). Заменён `@app.on_event("startup")` на `lifespan`, добавлены фабрика `create_app()` и единые обработчики исключений (`StarletteHTTPException`, `RequestValidationError`, `Exception`). |
| **SP2-02** | `BUG-07` | `ba4d183` | Удалены процессные глобалы `_ROOM_WS_CONNECTIONS` и `_PVP_WAITING_MATCHES`. Реализован `backend/app/services/realtime_broker.py` (`RedisRealtimeBroker`) с очередью PvP-матчмейкинга (`techenglish:pvp:waiting_queue`) и Redis Pub/Sub для комнат (`techenglish:room:{slug}`) и PvP (`techenglish:pvp:{match_id}`). Подписка выполняется до `websocket.accept()`, ретрансляция — асинхронной фоновой задачей с корректным завершением. |
| **SP2-10** | `R3` | `99ff5af` | Добавлена модель `DevicePushTokenRow` и миграция Alembic `0005_device_push_tokens` (с каскадной очисткой в `DELETE /v1/account`). Реализованы `PushNotificationGateway`, `FcmHttpV1PushNotificationGateway` (FCM HTTP v1) и `FakePushNotificationGateway`, эндпоинты `POST/DELETE /v1/notifications/device-token`, отправка push в `POST /v1/friends/{friend_id}/ping` (только при `accepts_pings=True` и не чаще 1 раза в 24 часа). На Android добавлены интерфейс `PushNotificationGateway`, `DefaultPushNotificationGateway` (собирается без `google-services.json`, активируется при наличии Firebase-ресурсов), методы регистрации токена в `TechEnglishApiClient` и `NotificationHelper.showPushNotification`. |

---

## 2. Фактически измеренные метрики и результаты проверок

| Проверка | Фактический результат |
|---|---|
| `wc -l backend/app/main.py` | **125 строк** (критерий DoD: `< 180`) |
| Роутеры в `backend/app/routers/` | **10 модулей** (критерий DoD: `≥ 8`) |
| `grep` по `_ROOM_WS_CONNECTIONS` / `_PVP_WAITING_MATCHES` в `backend/app/` | **0 совпадений** |
| `pytest tests/test_sp2_02_redis_realtime.py` (5 подряд прогонов) | **5/5 прогонов PASSED** (детерминированно, 0 флаков) |
| `cd backend && .venv/bin/pytest -v` | **35 passed, 1 skipped**, 0 failed (пропущен только live PostgreSQL тест из-за отсутствия Docker daemon в окружении) |
| Alembic SQLite `upgrade head` → `compare_metadata` → `downgrade base` → `upgrade head` | **PASSED** (`0001`…`0005_device_push_tokens`, 0 расхождений метаданных) |
| Alembic PostgreSQL offline DDL (`DATABASE_URL=postgresql+psycopg://... alembic upgrade head --sql`) | **PASSED** (`PostgresqlImpl`, `CREATE TABLE device_push_tokens`, ревизия `0005_device_push_tokens`) |
| `./gradlew :domain:test :domain:jacocoTestCoverageVerification` | **57/57 PASSED**, JaCoCo LINE **94.58%** (`2844/3007`, порог `≥ 90%`) |
| `./gradlew :app:testDebugUnitTest` | **50/50 PASSED** (включая новый `Sp210PushGatewayTest` на Robolectric) |
| **Итого локальных тестов** | **142 passed, 0 failed, 1 skipped** |

---

## 3. Неизмеренные / требующие ключей владельца пункты (§0.1, §0.2)

- **Реальный запуск 2 воркеров `uvicorn` на живом Redis + PostgreSQL в Docker:** не измерено локально (Docker daemon остановлен в окружении); проверяется в CI (`.github/workflows/ci.yml` с сервисными контейнерами `postgres:16-alpine` и `redis:7-alpine`), а кросс-воркерная изоляция проверена в `test_sp2_02_redis_realtime.py` на двух независимых `create_app()` экземплярах с общим `fakeredis.FakeServer`.
- **Реальная доставка push-уведомлений через Firebase Cloud Messaging (FCM):** **E2E не проверено, нужен ключ** (`FCM_PROJECT_ID`, `FCM_ACCESS_TOKEN` / сервисный аккаунт Firebase на бэкенде и `app/google-services.json` + Google Services Gradle plugin на Android). HTTP v1 протокол проверен через `httpx.MockTransport` (`test_sp2_10_push_fcm.py`), клиентская регистрация и показ локального уведомления — в `Sp210PushGatewayTest`.

---

## 4. Статус готовности

Все задачи Спринта 2A (`SP2-01`, `SP2-02`, `SP2-10`) и критерии DoD выполнены. Ожидание команды **`go`** от владельца для перехода к **Спринту 2B** (`SP2-03`…`SP2-09`).
