# Отчёт по Спринту 0 — Базовая линия, Alembic и CI (`SPRINT_0_REPORT.md`)

**Ветка:** `remediation/v3.1` (от `331bad5` / `v3.0.0`)  
**Тег:** `v3.1-sprint0`  
**Дата:** 2026-11-23

---

## 1. Выполненные задачи и коммиты

| Подзадача | Коммит | Что сделано |
|---|---|---|
| **S0-01** (Базовая линия + R5) | `41bbb71` | Создана ветка `remediation/v3.1`, выполнен прогон всех базовых наборов тестов и JaCoCo, перемер размеров AAB/APK/DB по R5, создан `docs/REMEDIATION_LOG.md`. |
| **S0-02** (Alembic baseline, R1) | `20b7e05` | Настроены `backend/alembic.ini` и `backend/alembic/env.py` (`render_as_batch=True` для SQLite, нативный DDL для PostgreSQL), создана начальная ревизия `0001_initial_v3_0_0` (24 таблицы, все FK `ON DELETE CASCADE`, уникальные ограничения и индексы). В `backend/app/config.py`, `models.py` и `main.py` отключён вызов `Base.metadata.create_all()` при `ENV=prod`/`production`. Обновлён `backend/Dockerfile` (`alembic.ini` + `alembic/`). Добавлен `backend/tests/test_alembic_migrations.py` (красный → зелёный). |
| **S0-03** (CI, R2) | `e0245dc` | Создан `.github/workflows/ci.yml` (Android job с кэшем Gradle и гейтом `:domain` JaCoCo ≥ 90% + проверка согласованности XML/CSV/HTML; Backend job с сервисными контейнерами `postgres:16-alpine` и `redis:7-alpine`, `alembic upgrade head`, `pytest`, `docker compose build`, без утечки секретов). В `domain/build.gradle.kts` добавлен `jacocoTestCoverageVerification` (`LINE >= 0.90`). Добавлен `backend/requirements-dev.txt` (`fakeredis`, `moto[s3]`) для локальных прогонов без контейнеров. |

---

## 2. Фактически измеренные результаты проверок

### 2.1. Тесты

| Модуль / набор | Команда | Результат |
|---|---|---|
| `:domain` (JVM unit) | `./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport :domain:jacocoTestCoverageVerification` | **57 / 57 PASSED** (`0` failed, `0` skipped), гейт `LINE >= 90%` **PASSED** |
| `:app` (Robolectric + Room + Compose UI) | `./gradlew :app:testDebugUnitTest` | **36 / 36 PASSED** (`0` failed, `0` skipped; `STAGE2_BENCHMARK_30K: exactLookupMs=4, paginatedFilterMs=18`) |
| `backend/` (FastAPI + Alembic + WS) | `cd backend && .venv/bin/pytest -v` | **13 PASSED, 1 SKIPPED** (`7.85s`; 11 базовых API/WS + 2 новых Alembic/prod-guard; 1 PostgreSQL-тест пропущен локально из-за отсутствия локального PostgreSQL и активируется в CI) |
| **ИТОГО локально выполнено** | | **106 / 106 PASSED** (`1` PostgreSQL-тест ожидает CI-контейнер) |

*Сверка с базовой линией аудита (S0-01):* до добавления новых тестов ровно **104 / 104** (`57 + 36 + 11`), полное совпадение.

### 2.2. Покрытие `:domain` (JaCoCo `0.8.12`)

- **XML (`jacocoTestReport.xml`):** Line **2844 / 3007 = 94.58%** (missed: 163), Instruction **18377 / 20853 = 88.13%**, Branch **739 / 1001 = 73.83%**.
- **HTML (`html/index.html`):** Line **2844 / 3007 = 94.58%** (missed: 163), Instruction **88%**, Branch **73%** — точное совпадение с XML.
- **CSV (`jacocoTestReport.csv`):** Line (сумма по классам) **2844 / 3008 = 94.55%** (разница ровно 1 строка из-за стандартной агрегации JaCoCo по классам вместо `<sourcefile>`), Instruction **18377 / 20853 = 88.13%**, Branch **739 / 1001 = 73.83%** — 100% совпадение с XML по инструкциям и ветвям.
- **Проверка согласованности XML/CSV/HTML из `.github/workflows/ci.yml`:** **PASSED** локально.

### 2.3. Alembic и валидация конфигураций

- `alembic upgrade head` → `compare_metadata` (`diffs == []`) → `downgrade base` → `upgrade head` на SQLite (batch mode): **PASSED**.
- Генерация статического PostgreSQL DDL (`alembic upgrade head --sql` и `alembic downgrade 0001_initial_v3_0_0:base --sql`): **PASSED** (exit code `0`).
- Защита `ENV=prod` (`create_all()` не вызывается ни в `init_db()`, ни в `on_startup()`): **PASSED** (`called["count"] == 0`).
- Валидация YAML `.github/workflows/ci.yml`: **PASSED** (jobs `android`, `backend`; services `postgres`, `redis`).
- Валидация `backend/docker-compose.yml` (`docker compose config --quiet`): **PASSED**.

### 2.4. Размеры базовой линии v3.0.0 (R5)

| Метрика | Измеренное значение |
|---|---|
| `app/src/main/assets/databases/tech_english.db` (на диске) | **86.37 MiB** (`90 562 560` Б) |
| `app-release.aab` (файл архива) | **26.87 MiB** (`28 177 402` Б) |
| `app-release.aab` → сумма всех записей без сжатия | **147.35 MiB** (`154 512 309` Б) |
| `app-release.aab` → `base/` (сжатый / несжатый) | **26.61 MiB** / **147.33 MiB** |
| `app-release.aab` → `base/assets/databases/tech_english.db` (DEFLATE / несжатый) | **8.67 MiB** (`9 091 980` Б) / **86.37 MiB** |
| `app-release.aab` → `base/dex` (R8 выключен, сжатый / несжатый) | **15.79 MiB** / **55.32 MiB** |
| `app-release-unsigned.apk` (общий / DB DEFLATE / без сжатия) | **27.19 MiB** / **8.67 MiB** / **146.93 MiB** |
| `app-debug.apk` (общий / DB DEFLATE / без сжатия) | **37.34 MiB** / **10.90 MiB** / **165.98 MiB** |

---

## 3. Неизмеренные метрики (оговорка §0.1)

- **`bundletool get-size total` (размер загрузки базового модуля):** не измерено (нет `bundletool` в окружении).
- **Время первого запуска (распаковка БД) и повторного холодного старта:** не измерено (нет запущенного AVD в окружении).
- **`alembic upgrade head` на живом PostgreSQL 16 и `docker compose build`:** не измерено локально (Docker daemon не запущен в текущей среде); проверки включены в `.github/workflows/ci.yml` (`job: backend`) и сработают при push в GitHub.

---

## 4. Статус DoD Спринта 0

- [x] Ветка `remediation/v3.1` создана, базовая линия сверена (`104/104`, `:domain` `94.58%`), `docs/REMEDIATION_LOG.md` заполнен фактическими замерами.
- [x] Alembic baseline (`0001_initial_v3_0_0`) создан и проверен на эквивалентность `models.py`; в `ENV=prod` `create_all()` отключён.
- [x] `.github/workflows/ci.yml` создан и валидирован (Android + JaCoCo ≥ 90% gate + Backend с PostgreSQL 16, Redis 7, Alembic, pytest и Docker Compose build).
- [x] Тег `v3.1-sprint0` готов; работа остановлена до подтверждения **`go`** для перехода к **Спринту 1A**.
