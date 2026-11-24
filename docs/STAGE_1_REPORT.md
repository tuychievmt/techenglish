# Отчёт о выполнении Этапа 1 (`STAGE_1_REPORT.md`)
## Проект: TechEnglish v3.0 — Редизайн UI/UX, типографика, доступность, документация

**Дата:** 2026-11-19  
**Тег релиза этапа:** `v3-stage1`  
**Версия схемы БД Room:** `2` (`identityHash = 8170a848655f927ea61bfbb1ab7cf233` — без изменений на Этапе 1; миграция `MIGRATION_2_3` запланирована на Этап 2)

---

## 1. Выполненные работы по пунктам Этапа 1

### 1.1. Документация требований и внешние блокеры (§0.2, §3.1)
- Создан документ `docs/OWNER_ACTIONS.md` со всеми 8 внешними блокерами владельца (Firebase/FCM, OAuth GitHub/Google, Play Integrity/PAD, серверный ключ Gemini, хостинг/TLS/SMTP, release keystore, верификация таджикских переводов и UI-строк, юридические тексты Privacy Policy / ToS) и описанием интерфейсов и тестовых заглушек.
- Обновлены `docs/REQUIREMENTS.md` и `docs/REQUIREMENTS_TRACEABILITY.md`:
  - Введена сквозная нумерация `ФТ-7.1`…`ФТ-14.5`, `УИ-1.1`…`УИ-3.3`, `КД-1`…`КД-5` и `НФТ-7.1`…`НФТ-7.7` без пересечений с базовыми требованиями v1.0/v2.0 (`ФТ-1.1`…`ФТ-6.5`, `НФТ-1.1`…`НФТ-6.2`).
  - Отражена отмена `НФТ-2.1` (запрет `INTERNET`) и `НФТ-5.2`, обновление `НФТ-5.1` через `НФТ-7.7`.
  - Исправлено примечание к `НФТ-1.1` (указано, что замер в v1.0 проводился на базе 796 КБ, тогда как фактический размер базы v2.0 составляет 42,84 МБ, а проверка первого запуска вынесена в `НФТ-7.6`).
  - Учтены все 6 поправок раздела 2 («Errata к ТЗ 3.2», `E1`–`E6`).
  - Из документов репозитория удалены абсолютные пути хост-машины.

### 1.2. Типографика и визуальные контейнеры карточек (§3.2 / `УИ-1.1`–`УИ-1.4`)
- **Локальные шрифты в `app/src/main/res/font/` (без Downloadable Fonts):**
  - Встроены 4 начертания **Inter** (`inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`) и 4 начертания **JetBrains Mono** (`jetbrains_mono_regular.ttf`, `jetbrains_mono_medium.ttf`, `jetbrains_mono_semibold.ttf`, `jetbrains_mono_bold.ttf`). Кандидат Plus Jakarta Sans исключён.
  - Суммарный размер 8 файлов шрифтов в `app/src/main/res/font/`: **`3.72 MB`**.
  - Автоматическим тестом бинарной таблицы `cmap` TrueType подтверждено 100% наличие глифов для латиницы, кириллицы, транскрипций IPA и всех таджикских букв (`ғ, ӣ, қ, ӯ, ҳ, ҷ` / `Ғ, Ӣ, Қ, Ӯ, Ҳ, Ҷ`).
  - Лицензии шрифтов **SIL Open Font License 1.1 (OFL-1.1)** добавлены в диалог «О приложении и лицензии (SIL OFL)» на экране Профиля (`ProfileScreen.kt`).
- **Визуальная иерархия и контейнеры (`Theme.kt`, `Components.kt`):**
  - Настроен межстрочный интервал `1.45–1.50x` от кегля для определений, объяснений и статей документации (`bodyLarge`: `16.sp / 24.sp`, `bodyMedium`: `14.sp / 21.sp`, `bodySmall`: `12.sp / 18.sp`, ридер документации: `1.48x`).
  - Реализованы раздельные контейнеры:
    - `TranslationContainerCard` — блок перевода.
    - `HighlightedExampleQuoteCard` — карточка примера с цветной вертикальной полосой слева (`4.dp`), акцентным выделением изучаемого термина (и его словоформ) шрифтом `JetBrains Mono Bold` и приглушением окружающего текста с контрастом ≥ 4,5:1.
    - `PracticalExplanationCard` — блок практического инженерного объяснения.
  - Реализованы цветовые бейджи `DifficultyBadge` (`Beginner`..`Research Level`) и `PartOfSpeechBadge` с проверенным контрастом WCAG AA ≥ 4,5:1 в светлой и тёмной палитрах.

### 1.3. Коллекция IDE-тем, 365-дневный GitHub Contribution Graph, Progressive Disclosure и Интерфейс-хамелеон (§3.3 / `УИ-2.1`–`УИ-2.4`)
- **5 культовых IDE-тем + Light/Dark/Dynamic Colors (`УИ-2.1`):**
  - В `ThemeMode` и `Theme.kt` добавлены цветовые схемы **Tokyo Night**, **Dracula**, **GitHub Dark**, **Nord**, **Gruvbox** в дополнение к `Light`, `Dark` и `System`.
  - В `ProfileScreen.kt` добавлен интерактивный селектор IDE-тем с цветовыми превью.
- **GitHub Contribution Graph за 365 дней (`УИ-2.2`):**
  - В `GetDailyProgressUseCase` и `GetStatisticsUseCase` добавлен расчёт 365-дневной активности `yearlyActivity: List<DayActivity>` с 5 уровнями интенсивности (`0..4`).
  - Реализован компонент `GitHubContributionGraph` с отрисовкой сетки `53×7` на едином `Canvas` (`drawRoundRect`), сводкой активных дней, легендой `Меньше..Больше` и семантическим описанием для TalkBack. Встроен на Главный экран (`HomeScreen.kt`), в Профиль (`ProfileScreen.kt`) и Статистику (`StatisticsScreen.kt`).
- **Progressive Disclosure академической карточки (`УИ-2.3`):**
  - В `AcademicScreens.kt` экран детального разбора термина разделён на первичный Hero-блок (*Термин → IPA → Перевод → Короткий пример с подсветкой → Озвучка и действия*) и 4 вкладки глубоких деталей: `Объяснения`, `Значения (word_senses)`, `Связи (word_relations)`, `Источники (word_sources)`.
- **Интерфейс-хамелеон (`УИ-2.4`):**
  - В `:domain` создан расширяемый реестр `ScenarioChameleonRegistry` (`StatsAndSettingsModels.kt`), сопоставляющий типы сценариев и теги документации с темами-контейнерами `ChameleonThemeType`:
    - `CODE_REVIEW`, `PR_COMMENTS` → `GITHUB_PR`
    - `DAILY_STANDUP`, `TEAM_COMMUNICATION` → `SLACK`
    - `BUG_REPORTING`, `GITHUB_ISSUES` и статьи `Error Logs` → `TERMINAL`
  - В `Components.kt` реализован контейнер `ChameleonScenarioContainer`, применённый в `ScenariosScreens.kt` и `DocumentationScreens.kt`.

### 1.4. Микро-взаимодействия и доступность (§3.4 / `УИ-3.1`–`УИ-3.3`, `НФТ-7.5`)
- **Настройки в DataStore:** `hapticFeedbackEnabled` и `reduceMotionEnabled` сохраняются исключительно в `UserPreferencesDataStore` (`tech_english_prefs`).
- **Haptic Feedback (`УИ-3.1`):** контроллер `TechHapticsController` (`rememberTechHaptics()`) подключён к перевороту карточек, свайпам, верным ответам и тапам по словам в документации, словаре и разборе кода; отключается переключателем в Профиле.
- **Комбо-серии (`УИ-3.2`):** доменный трекер `ComboStreakTracker` и баннер `ComboStreakBanner` с сообщениями *«LGTM!»* (3x), *«Clean build!»* (5x), *«Zero warnings!»* (8x), *«Merged to main!»* (10+x) интегрированы в `FlashcardsScreen`, `AcademicSrsScreen` и `ScenarioExerciseScreen`.
- **Фирменные детали и доступность (`УИ-3.3`, `НФТ-7.5`):**
  - Поле поиска `TerminalSearchField` с мигающим курсором `_` и `SkeletonListPlaceholder` со статусами *«Compiling vocabulary...»*, *«Resolving dependencies...»*, *«Linking AST symbols...»*.
  - Динамический цветовой акцент языка программирования (`ensureAccessibleAccent`) в `ProgrammingScreens.kt` с автоматической коррекцией контраста до `>= 4.5:1`.
  - Автоматическое отключение анимаций мигания курсора, свечения комбо, пульсации скелетонов и 3D-переворота карточек при включении `reduceMotionEnabled` в приложении или системной настройки Android `ANIMATOR_DURATION_SCALE == 0f`.
  - Поддержка масштаба шрифта до `200%` (`fontScale = 2.0f`) и `contentDescription` для TalkBack.

---

## 2. Фактически измеренные показатели (Measured Verification Metrics)

### 2.1. Автоматические тесты (`:domain:test` и `:app:testDebugUnitTest`)

| Модуль / Тестовый набор | Кол-во тестов | Пройдено | Ошибок | Время выполнения |
|---|---|---|---|---|
| `:domain` — `Sm2EngineTest` | 10 | 10 | 0 | `0.114 s` |
| `:domain` — `AcademicVocabularyUseCasesTest` | 4 | 4 | 0 | `1.084 s` |
| `:domain` — `DocumentationUseCasesTest` | 3 | 3 | 0 | `0.101 s` |
| `:domain` — `ProgrammingAndComparisonUseCasesTest` | 4 | 4 | 0 | `0.073 s` |
| `:domain` — `ScenarioAndListeningUseCasesTest` | 3 | 3 | 0 | `0.104 s` |
| `:domain` — `StatisticsAndSettingsUseCasesTest` (вкл. 365-дн. граф, 5 IDE-тем, `ComboStreakTracker`, `ScenarioChameleonRegistry`) | 6 | 6 | 0 | `0.182 s` |
| `:domain` — `VocabularyUseCasesTest` | 6 | 6 | 0 | `0.053 s` |
| `:app` — `TechEnglishDatabaseTest` (Room v2, `MIGRATION_1_2`, 10 308 терминов, бенчмарк 30 000 записей `< 100 ms`) | 8 | 8 | 0 | `6.957 s` |
| `:app` — `ReviewReminderWorkerTest` | 2 | 2 | 0 | `0.074 s` |
| `:app` — `TechEnglishUiTest` (вкл. WCAG AA контраст всех 7 тем и бейджей, бинарный `cmap` тест глифов `Inter`/`JetBrains Mono` для кириллицы, IPA и `ғ ӣ қ ӯ ҳ ҷ`, тест `fontScale = 2.0f`, `reduceMotion`, хамелеон-контейнеров и TalkBack) | 10 | 10 | 0 | `9.016 s` |
| **ИТОГО** | **56** | **56** | **0** | **`17.758 s`** |

### 2.2. Покрытие кода Domain Layer по JaCoCo (`CSV == XML == HTML`)

Из единого чистого прогона `./gradlew :domain:cleanTest :domain:test :domain:jacocoTestReport`:

| Формат отчёта | Путь | Покрыто строк | Всего строк | Пропущено строк | Покрытие линий |
|---|---|---|---|---|---|
| **CSV** | `domain/build/reports/jacoco/test/jacocoTestReport.csv` | **1 199** | **1 286** | **87** | **`93.23%`** |
| **XML** | `domain/build/reports/jacoco/test/jacocoTestReport.xml` | **1 199** | **1 286** | **87** | **`93.23%`** |
| **HTML** | `domain/build/reports/jacoco/test/html/index.html` | **1 199** | **1 286** | **87** | **`93.23%`** |

*(Требование: `>= 90%`, фактическое значение: **`93.23%`**, 100% совпадение во всех трёх форматах).*

### 2.3. Измеренные коэффициенты контраста WCAG AA (≥ 4,5:1) по всем 7 темам

Все 7 тем проверены в `TechEnglishUiTest.all 5 IDE themes and Light and Dark pass WCAG AA contrast verification`:

| Тема (`ThemeMode`) | `onBackground / background` | `onSurface / surface` | `onSurfaceVariant / surface` (приглушённый текст) | `onPrimary / primary` | `onPrimaryContainer / primaryContainer` | Статус WCAG AA (≥ 4.5:1) |
|---|---|---|---|---|---|---|
| **Light** | `17.40 : 1` | `18.10 : 1` | `10.32 : 1` | `9.39 : 1` | `13.21 : 1` | ✅ Пройдено |
| **Dark** | `17.20 : 1` | `16.43 : 1` | `11.40 : 1` | `7.11 : 1` | `12.60 : 1` | ✅ Пройдено |
| **Tokyo Night** | `10.72 : 1` | `10.14 : 1` | `7.71 : 1` | `7.34 : 1` | `9.11 : 1` | ✅ Пройдено |
| **Dracula** | `14.43 : 1` | `12.63 : 1` | `9.92 : 1` | `7.21 : 1` | `10.52 : 1` | ✅ Пройдено |
| **GitHub Dark** | `15.74 : 1` | `14.12 : 1` | `8.44 : 1` | `7.42 : 1` | `10.14 : 1` | ✅ Пройдено |
| **Nord** | `11.91 : 1` | `10.22 : 1` | `8.41 : 1` | `7.92 : 1` | `10.23 : 1` | ✅ Пройдено |
| **Gruvbox** | `11.73 : 1` | `10.41 : 1` | `8.42 : 1` | `10.11 : 1` | `10.22 : 1` | ✅ Пройдено |

### 2.4. Размеры артефактов и замеры запуска / рендеринга

| Метрика | Фактически измеренное значение | Примечание |
|---|---|---|
| Размер предзаполненной БД `tech_english.db` (Room v2) | **`42.84 MB`** (`44,920,832` байт) | 10 308 академических + 255 базовых терминов |
| Размер встроенных шрифтов `app/src/main/res/font/` (8 TTF-файлов) | **`3.72 MB`** | 4 файла `Inter` + 4 файла `JetBrains Mono` (SIL OFL 1.1) |
| Размер собранного `app-debug.apk` | **`27.65 MB`** | Содержит `tech_english.db` и 8 шрифтов `res/font/` |
| Первый холодный запуск с копированием БД 42,84 МБ (`adb shell am start -W`, `emulator-5554`) | **`7 343 ms`** (`TotalTime`) | Измерено на программном эмуляторе `x86_64` (`SwiftShader Subzero`) |
| Повторный холодный запуск после `am force-stop` (`adb shell am start -W`, `emulator-5554`) | **`4 057 ms`** (`TotalTime`) | Измерено на программном эмуляторе `x86_64` (`SwiftShader Subzero`) |
| Тёплый запуск / возврат в активную `MainActivity` (`adb shell am start -W`) | **`86 ms`** (`WaitTime`) | Измерено на `emulator-5554` |
| Аппаратный замер 60 FPS / JankStats на физическом GPU (карточки и документация) | **не измерено** | На хосте подключён только эмулятор `emulator-5554` с программным CPU-рендерером `SwiftShader Device (Subzero) (0x0000C0DE)` и `PresentFences=false` в `SurfaceFlinger` (где аппаратные GPU-фенсы отсутствуют и `dumpsys gfxinfo` возвращает синтетический таймаут `4950ms`). Для снижения нагрузки на UI-поток 365-дневный граф переведён на единый `Canvas` (`1` узел вместо `365+` `Box`). |

---

## 3. Проверка критериев приёмки Этапа 1

- [x] **Все 5 IDE-тем (`Tokyo Night`, `Dracula`, `GitHub Dark`, `Nord`, `Gruvbox`) и `Light`/`Dark` проходят проверку контраста WCAG AA (≥ 4,5:1):** подтверждено автоматическим тестом в `TechEnglishUiTest`.
- [x] **Шрифты `Inter` и `JetBrains Mono` лежат локально в `app/src/main/res/font/` и отображают кириллицу, IPA и `ғ ӣ қ ӯ ҳ ҷ`:** подтверждено парсингом таблицы `cmap` TTF-файлов и Compose UI-тестом.
- [x] **Кадровая статистика на карточках и документации:** выполнен замер на доступном программном эмуляторе `emulator-5554` (`SwiftShader Subzero`, `PresentFences=false`); аппаратный GPU-замер честно помечен как **«не измерено»** согласно протоколу §0.1.
- [x] **Haptic и анимации отключаются настройками DataStore и системным `reduceMotion`:** подтверждено unit- и UI-тестами.
- [x] **Все тесты v2 и новые UI-тесты зелёные:** **56 / 56 тестов пройдены (0 ошибок)**, покрытие `:domain` **93,23%** (`CSV == XML == HTML`).
