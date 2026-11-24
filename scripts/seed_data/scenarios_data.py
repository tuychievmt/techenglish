# -*- coding: utf-8 -*-
"""
36 Interactive Work Scenarios across ALL 8 mandatory contexts:
- DAILY_STANDUP
- CODE_REVIEW
- PR_COMMENTS
- JIRA_TICKETS
- GITHUB_ISSUES
- INTERVIEW
- TEAM_COMMUNICATION
- BUG_REPORTING

Supporting ALL 6 interactive exercise types:
- SENTENCE_BUILD
- TRANSLATION_CHOICE
- PHRASE_CHOICE
- FILL_BLANKS
- INTERVIEW_ANSWER
- LISTENING_CHOICE

Plus 64 curated professional IT workplace English phrases (WORK_PHRASES).
"""

def expr(en, ru, context):
    return {"en": en, "ru": ru, "context": context}

SCENARIOS = [
    # ==================== 1. DAILY_STANDUP (5) ====================
    {
        "id": 1,
        "type": "DAILY_STANDUP",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Reporting a Blocker on Standup",
        "prompt": "Build a clear Daily Standup update explaining that you finished the Room schema and are now blocked by missing staging keys.",
        "prompt_ru": "Соберите реплику для Daily Standup: вы закончили схему Room и сейчас заблокированы ожиданием ключей для staging.",
        "solution": "Yesterday I finished the Room schema and today I am blocked on staging credentials.",
        "options": [
            "Yesterday", "I finished", "the Room schema", "and today",
            "I am blocked", "on staging credentials.", "already", "skipping"
        ],
        "explanation_ru": "В английском на стендапе для описания препятствия используется устойчивая конструкция «I am blocked on <dependency>» (или «blocked by»).",
        "useful_expressions": [
            expr("I'm currently blocked on...", "Я сейчас заблокирован из-за...", "Сообщение о блокере"),
            expr("Yesterday I wrapped up...", "Вчера я завершил...", "Отчёт о сделанном"),
            expr("No blockers on my end.", "С моей стороны блокеров нет.", "Завершение апдейта"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 2,
        "type": "DAILY_STANDUP",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Flagging a Scope Risk Politely",
        "prompt": "The sprint ends tomorrow and the caching task turned out twice as complex as estimated. What is the most professional standup update?",
        "prompt_ru": "Спринт заканчивается завтра, а задача по кэшированию оказалась вдвое сложнее оценки. Выберите самый профессиональный вариант для стендапа.",
        "solution": "Heads up: the caching refactor is taking longer than estimated, so it might slip into the next sprint.",
        "options": [
            "Heads up: the caching refactor is taking longer than estimated, so it might slip into the next sprint.",
            "This task is impossible and whoever estimated it made a mistake.",
            "I am working on caching and everything is totally fine.",
            "Let's cancel the sprint because caching broke everything."
        ],
        "explanation_ru": "Фраза «Heads up» мягко предупреждает команду о риске заранее, а глагол «slip into the next sprint» — стандартный IT-термин для переноса задачи на следующий спринт.",
        "useful_expressions": [
            expr("Heads up, ...", "Обратите внимание / заранее предупреждаю...", "Предупреждение о риске"),
            expr("might slip into the next sprint", "может перенестись на следующий спринт", "Оценка сроков"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 3,
        "type": "DAILY_STANDUP",
        "exercise_type": "FILL_BLANKS",
        "title": "Taking a Discussion Offline",
        "prompt": "Complete the standup phrase to avoid turning a 15-minute standup into a deep architecture debate:\n\"Let's take this architecture debate ___ after standup so we don't hold up the rest of the team.\"",
        "prompt_ru": "Заполните пропуск в стандартной фразе, чтобы вынести долгое обсуждение за рамки стендапа.",
        "solution": "offline",
        "options": ["offline", "upstream", "deprecated", "hardcoded"],
        "explanation_ru": "«Take it offline» в корпоративном английском означает «обсудить отдельно после общей встречи» (не путать с отсутствием интернета!).",
        "useful_expressions": [
            expr("Let's take this offline.", "Давайте обсудим это отдельно после встречи.", "Управление временем митинга"),
            expr("hold up the team", "задерживать команду", "Этикет созвонов"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 4,
        "type": "DAILY_STANDUP",
        "exercise_type": "LISTENING_CHOICE",
        "title": "Standup Update: PR Ready for Review",
        "prompt": "I just pushed the SM-2 unit tests and opened a pull request; could someone review it before lunch?",
        "prompt_ru": "Прослушайте реплику разработчика на стендапе и выберите её точный смысл.",
        "solution": "Разработчик запушил юнит-тесты SM-2, открыл PR и просит провести ревью до обеда.",
        "options": [
            "Разработчик запушил юнит-тесты SM-2, открыл PR и просит провести ревью до обеда.",
            "Разработчик отложил написание тестов SM-2 до следующего спринта.",
            "Разработчик нашёл баг в чужом PR и просит его закрыть до обеда.",
            "Разработчик просит отменить встречу перед обедом."
        ],
        "explanation_ru": "«Pushed» = отправил коммиты в удалённый репозиторий, «opened a pull request» = создал PR, «before lunch» = до обеда.",
        "useful_expressions": [
            expr("Could someone take a look at my PR?", "Кто-нибудь сможет посмотреть мой PR?", "Запрос ревью"),
            expr("I just pushed the latest fixes.", "Я только что запушил последние правки.", "Апдейт статуса"),
        ],
        "difficulty": "A2",
    },
    {
        "id": 5,
        "type": "DAILY_STANDUP",
        "exercise_type": "TRANSLATION_CHOICE",
        "title": "Standup Term: Carry Over",
        "prompt": "Choose the accurate Russian meaning of the highlighted IT phrase:\n\"Two low-priority UI tickets will carry over to Sprint 14.\"",
        "prompt_ru": "Выберите точный перевод выделенного выражения «carry over» в контексте Agile-спринта.",
        "solution": "Перенесутся незавершёнными в следующий спринт",
        "options": [
            "Перенесутся незавершёнными в следующий спринт",
            "Будут полностью удалены из бэклога",
            "Получат наивысший приоритет сегодня",
            "Успешно закрыты досрочно"
        ],
        "explanation_ru": "«Carry over» в Agile/Scrum означает перенос незакрытых задач из текущего спринта в следующий.",
        "useful_expressions": [
            expr("carry over to the next sprint", "перенестись на следующий спринт", "Планирование спринта"),
        ],
        "difficulty": "B1",
    },

    # ==================== 2. CODE_REVIEW (5) ====================
    {
        "id": 6,
        "type": "CODE_REVIEW",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Leaving Constructive Feedback on Null Safety",
        "prompt": "You notice a potential NullPointerException in a teammate's code. Choose the most constructive Code Review comment.",
        "prompt_ru": "Вы заметили потенциальный NullPointerException в коде коллеги. Выберите наиболее конструктивный комментарий для Code Review.",
        "solution": "Nice cleanup! One concern: could user.email be null here? Maybe we can guard it with an early return.",
        "options": [
            "Nice cleanup! One concern: could user.email be null here? Maybe we can guard it with an early return.",
            "This code will obviously crash in production. Fix it.",
            "Why did you write this without checking null?",
            "LGTM, I didn't really check edge cases."
        ],
        "explanation_ru": "Конструктивный комментарий на Code Review формулируется через «we» и вежливый вопрос («could ... be null?»), фокусируясь на коде, а не на авторе.",
        "useful_expressions": [
            expr("One small concern:", "Один важный момент:", "Указание на риск"),
            expr("Could we guard against...?", "Можем ли мы защититься от...?", "Предложение проверки"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 7,
        "type": "CODE_REVIEW",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Suggesting a Helper Extraction",
        "prompt": "Build a polite Code Review suggestion to extract duplicated validation into a shared helper function.",
        "prompt_ru": "Соберите вежливое предложение на ревью: вынести дублирующуюся валидацию в общую вспомогательную функцию.",
        "solution": "Consider extracting this validation logic into a reusable helper function.",
        "options": [
            "Consider", "extracting", "this validation logic", "into a reusable",
            "helper function.", "delete", "hardcoding"
        ],
        "explanation_ru": "«Consider + V-ing» (Consider extracting...) — мягкая, уважительная форма предложения рефакторинга на Code Review.",
        "useful_expressions": [
            expr("Consider extracting this into...", "Рассмотри вариант вынести это в...", "Рефакторинг"),
            expr("reusable helper function", "переиспользуемая вспомогательная функция", "Чистый код"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 8,
        "type": "CODE_REVIEW",
        "exercise_type": "FILL_BLANKS",
        "title": "Marking a Non-Blocking Style Suggestion",
        "prompt": "Fill in the standard prefix used in Code Reviews for minor optional naming suggestions:\n\"___: renaming `d` to `dueWordsCount` would make this loop easier to scan.\"",
        "prompt_ru": "Заполните стандартный префикс для необязательного мелкого замечания по стилю на Code Review.",
        "solution": "Nit",
        "options": ["Nit", "Blocker", "Hotfix", "Fatal"],
        "explanation_ru": "«Nit:» (от nitpick — придирка) показывает автору PR, что замечание косметическое и не блокирует аппрув.",
        "useful_expressions": [
            expr("Nit: minor naming tweak", "Мелочь: небольшое улучшение названия", "Неблокирующее замечание"),
            expr("Non-blocking suggestion", "Неблокирующее предложение", "Приоритет замечания"),
        ],
        "difficulty": "A2",
    },
    {
        "id": 9,
        "type": "CODE_REVIEW",
        "exercise_type": "TRANSLATION_CHOICE",
        "title": "Understanding Review Acronym: LGTM",
        "prompt": "A senior reviewer leaves the comment: \"LGTM with one minor nit — feel free to merge once CI is green.\" What does LGTM mean?",
        "prompt_ru": "Ревьюер написал: «LGTM with one minor nit — feel free to merge once CI is green». Что означает LGTM?",
        "solution": "Looks Good To Me (выглядит отлично, одобряю)",
        "options": [
            "Looks Good To Me (выглядит отлично, одобряю)",
            "Let's Go Test Manually (давайте протестируем вручную)",
            "Low Grade Technical Merge (слабое слияние)",
            "Log Generated Too Much (слишком много логов)"
        ],
        "explanation_ru": "LGTM — общепринятая аббревиатура «Looks Good To Me», означающая одобрение Pull Request.",
        "useful_expressions": [
            expr("LGTM, feel free to merge!", "Выглядит отлично, можно вливать!", "Одобрение PR"),
            expr("once CI is green", "как только пройдут проверки CI", "Условие мержа"),
        ],
        "difficulty": "A2",
    },
    {
        "id": 10,
        "type": "CODE_REVIEW",
        "exercise_type": "LISTENING_CHOICE",
        "title": "Review Discussion: Database Query on Main Thread",
        "prompt": "This DAO query appears to run synchronously; let's make it a suspend function so we never block the UI thread.",
        "prompt_ru": "Прослушайте комментарий ревьюера и выберите, какую проблему он обнаружил.",
        "solution": "Запрос DAO выполняется синхронно; его нужно сделать suspend-функцией, чтобы не блокировать UI-поток.",
        "options": [
            "Запрос DAO выполняется синхронно; его нужно сделать suspend-функцией, чтобы не блокировать UI-поток.",
            "Нужно удалить таблицу из базы данных Room.",
            "Запрос возвращает неверный тип данных String вместо Int.",
            "Нужно переписать UI с Compose на XML."
        ],
        "explanation_ru": "«Runs synchronously» = выполняется синхронно; «block the UI thread» = заблокировать главный поток интерфейса.",
        "useful_expressions": [
            expr("so we never block the UI thread", "чтобы никогда не блокировать UI-поток", "Производительность Android"),
        ],
        "difficulty": "B2",
    },

    # ==================== 3. PR_COMMENTS (4) ====================
    {
        "id": 11,
        "type": "PR_COMMENTS",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Responding to Review Feedback",
        "prompt": "Build a polite PR reply confirming you addressed all comments and added a regression test.",
        "prompt_ru": "Соберите ответ в Pull Request: вы учли все замечания и добавили регрессионный тест.",
        "solution": "Good catch! I addressed your feedback and added a unit test.",
        "options": [
            "Good catch!", "I addressed", "your feedback", "and added",
            "a unit test.", "ignored", "reverted"
        ],
        "explanation_ru": "«Good catch!» — дружелюбная благодарность за найденный баг или недочёт, «addressed your feedback» — учёл замечания.",
        "useful_expressions": [
            expr("Good catch, thanks!", "Отлично подмечено, спасибо!", "Благодарность ревьюеру"),
            expr("Addressed in the latest commit.", "Исправлено в последнем коммите.", "Ответ на замечание"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 12,
        "type": "PR_COMMENTS",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Writing a Clear PR Description Summary",
        "prompt": "Choose the strongest opening sentence for a Pull Request description implementing offline SM-2 review reminders.",
        "prompt_ru": "Выберите наиболее чёткое описание Pull Request для реализации офлайн-напоминаний SM-2.",
        "solution": "Implements periodic WorkManager reminders for due SM-2 cards while gracefully handling Android 13+ notification permissions.",
        "options": [
            "Implements periodic WorkManager reminders for due SM-2 cards while gracefully handling Android 13+ notification permissions.",
            "Fixed stuff and added some files.",
            "WIP maybe works on my phone.",
            "Changes to app folder."
        ],
        "explanation_ru": "Хорошее описание PR начинается с глагола действия (Implements / Fixes / Refactors) и чётко формулирует ценность и крайние случаи.",
        "useful_expressions": [
            expr("Implements / Fixes / Refactors...", "Реализует / Исправляет / Рефакторит...", "Заголовок PR"),
            expr("gracefully handling edge cases", "корректно обрабатывая крайние случаи", "Надёжность"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 13,
        "type": "PR_COMMENTS",
        "exercise_type": "FILL_BLANKS",
        "title": "Pushing Back Respectfully on Scope",
        "prompt": "Complete the respectful PR reply proposing a separate follow-up ticket:\n\"Great idea! To keep this PR focused, would you mind if we handle dark-mode animations in a ___ ticket?\"",
        "prompt_ru": "Заполните пропуск в вежливом ответе, предлагающем вынести доработку в отдельную задачу.",
        "solution": "follow-up",
        "options": ["follow-up", "deadlock", "rollback", "downtime"],
        "explanation_ru": "«In a follow-up ticket» — стандартный способ не раздувать текущий PR и зафиксировать хорошую идею на потом.",
        "useful_expressions": [
            expr("To keep this PR focused...", "Чтобы не раздувать этот PR...", "Контроль объёма PR"),
            expr("handle it in a follow-up ticket", "сделать это в отдельной последующей задаче", "Декомпозиция"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 14,
        "type": "PR_COMMENTS",
        "exercise_type": "TRANSLATION_CHOICE",
        "title": "PR Comment: Out of Scope",
        "prompt": "What does a reviewer mean by writing: \"Refactoring the entire navigation graph feels out of scope for this bugfix PR\"?",
        "prompt_ru": "Что означает комментарий ревьюера: «Refactoring the entire navigation graph feels out of scope for this bugfix PR»?",
        "solution": "Рефакторинг всей навигации выходит за рамки данного PR с исправлением бага.",
        "options": [
            "Рефакторинг всей навигации выходит за рамки данного PR с исправлением бага.",
            "Граф навигации сломан и требует срочного удаления.",
            "Переменная графа навигации недоступна в текущей области видимости Kotlin.",
            "PR готов к немедленному слиянию."
        ],
        "explanation_ru": "«Out of scope» означает «выходит за рамки текущей задачи».",
        "useful_expressions": [
            expr("out of scope for this PR", "выходит за рамки данного PR", "Контроль границ задачи"),
        ],
        "difficulty": "B1",
    },

    # ==================== 4. JIRA_TICKETS (4) ====================
    {
        "id": 15,
        "type": "JIRA_TICKETS",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Writing Clear Acceptance Criteria",
        "prompt": "Assemble a testable Jira Acceptance Criterion for the SM-2 algorithm.",
        "prompt_ru": "Соберите проверяемый критерий приёмки (Acceptance Criterion) в Jira для алгоритма SM-2.",
        "solution": "Given a word with EF 1.4, when rated Hard, then EF stays at least 1.3.",
        "options": [
            "Given a word", "with EF 1.4,", "when rated Hard,", "then EF stays",
            "at least 1.3.", "always crashes", "ignores"
        ],
        "explanation_ru": "Формат Gherkin (Given / When / Then) — золотой стандарт написания чётких, проверяемых Acceptance Criteria в Jira.",
        "useful_expressions": [
            expr("Given ... When ... Then ...", "Дано ... Когда ... Тогда ...", "Критерии приёмки Gherkin"),
            expr("Acceptance Criteria (AC)", "Критерии приёмки", "Оформление тикетов"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 16,
        "type": "JIRA_TICKETS",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Clarifying Ambiguous Requirements in Jira",
        "prompt": "A Jira ticket says 'Add language comparison' without specifying what happens when two languages share no snippets. How should you comment?",
        "prompt_ru": "В тикете Jira написано «Добавить сравнение языков», но не указано поведение при отсутствии общих задач. Как правильно задать вопрос?",
        "solution": "Quick clarification before starting: if two selected languages have no common task_key, should we display an empty state explaining why?",
        "options": [
            "Quick clarification before starting: if two selected languages have no common task_key, should we display an empty state explaining why?",
            "This ticket makes no sense, rewrite it.",
            "I'll just guess and hide the screen.",
            "Closing as Won't Fix."
        ],
        "explanation_ru": "«Quick clarification before starting» задаёт конструктивный тон, а конкретный вопрос с предложенным вариантом («should we display an empty state...») ускоряет ответ продакт-менеджера.",
        "useful_expressions": [
            expr("Quick clarification before starting:", "Небольшое уточнение перед началом работы:", "Уточнение требований"),
            expr("display an empty state", "показать пустое состояние экрана", "UX-терминология"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 17,
        "type": "JIRA_TICKETS",
        "exercise_type": "FILL_BLANKS",
        "title": "Linking Dependent Jira Issues",
        "prompt": "Fill in the standard Jira status/transition phrase:\n\"Moving this ticket to In QA — the APK build is attached and ready for ___.\"",
        "prompt_ru": "Заполните пропуск в комментарии при передаче тикета тестировщикам.",
        "solution": "verification",
        "options": ["verification", "deprecation", "serialization", "throttling"],
        "explanation_ru": "«Ready for verification» (готово к проверке) — стандартная фраза при переводе задачи в колонку QA.",
        "useful_expressions": [
            expr("Ready for QA verification", "Готово к проверке QA", "Передача в тестирование"),
            expr("Moving this ticket to In Progress", "Перевожу задачу в работу", "Обновление статуса"),
        ],
        "difficulty": "A2",
    },
    {
        "id": 18,
        "type": "JIRA_TICKETS",
        "exercise_type": "TRANSLATION_CHOICE",
        "title": "Jira Resolution: Cannot Reproduce",
        "prompt": "QA left a comment on a bug ticket: \"Tested on Android 14 and 15 with offline mode enabled — cannot reproduce on latest build.\" What does it mean?",
        "prompt_ru": "Что означает комментарий QA: «cannot reproduce on latest build»?",
        "solution": "Не удаётся воспроизвести ошибку на последней сборке.",
        "options": [
            "Не удаётся воспроизвести ошибку на последней сборке.",
            "Ошибка воспроизводится на каждом устройстве.",
            "Сборка не компилируется на Android 14.",
            "Тестирование отложено на следующую неделю."
        ],
        "explanation_ru": "«Cannot reproduce» (CNR) — стандартная резолюция в Jira, когда баг не повторяется по описанным шагам.",
        "useful_expressions": [
            expr("Cannot reproduce on latest build", "Не воспроизводится на последней сборке", "Резолюция QA"),
        ],
        "difficulty": "A2",
    },

    # ==================== 5. GITHUB_ISSUES (4) ====================
    {
        "id": 19,
        "type": "GITHUB_ISSUES",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Triaging an Open-Source GitHub Issue",
        "prompt": "A user opens a GitHub Issue saying 'Audio does not play' without device info or steps. Pick the most helpful maintainer response.",
        "prompt_ru": "Пользователь открыл GitHub Issue «Звук не играет» без модели устройства и шагов. Выберите лучший ответ мейнтейнера.",
        "solution": "Thanks for reporting this! Could you share your Android version and whether an offline English TTS voice pack is installed?",
        "options": [
            "Thanks for reporting this! Could you share your Android version and whether an offline English TTS voice pack is installed?",
            "Works on my device. Closed.",
            "Read the docs before opening issues.",
            "Not a bug."
        ],
        "explanation_ru": "Вежливый триаж в GitHub Issues начинается с благодарности за репорт и запроса минимально необходимой диагностической информации.",
        "useful_expressions": [
            expr("Thanks for reporting this!", "Спасибо за сообщение о проблеме!", "Ответ на Issue"),
            expr("Could you share your environment details?", "Не могли бы вы поделиться деталями окружения?", "Сбор данных"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 20,
        "type": "GITHUB_ISSUES",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Linking a Commit to Auto-Close an Issue",
        "prompt": "Construct a PR footer note explaining that this PR resolves GitHub issue #42 and adds offline fallback handling.",
        "prompt_ru": "Соберите фразу для PR, которая автоматически закроет GitHub Issue #42 и опишет добавление офлайн-обработки.",
        "solution": "Closes #42 by adding graceful fallback when TTS voice data is missing.",
        "options": [
            "Closes #42", "by adding", "graceful fallback", "when TTS voice data",
            "is missing.", "breaks", "forgetting"
        ],
        "explanation_ru": "Ключевые слова «Closes #42» или «Fixes #42» в описании PR заставляют GitHub автоматически закрыть связанный Issue при слиянии.",
        "useful_expressions": [
            expr("Closes #42 / Fixes #42", "Закрывает задачу #42", "Автоматизация GitHub"),
            expr("graceful fallback", "корректный запасной вариант", "Надёжность"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 21,
        "type": "GITHUB_ISSUES",
        "exercise_type": "FILL_BLANKS",
        "title": "Requesting a Minimal Reproducible Example",
        "prompt": "Complete the maintainer request on a complex library bug:\n\"Could you provide a minimal ___ example so we can isolate the Room schema issue?\"",
        "prompt_ru": "Заполните пропущенное слово в устойчивом термине MRE (минимальный воспроизводимый пример).",
        "solution": "reproducible",
        "options": ["reproducible", "deprecated", "immutable", "concurrent"],
        "explanation_ru": "«Minimal reproducible example» (MRE / repro) — минимальный фрагмент кода, гарантированно воспроизводящий баг.",
        "useful_expressions": [
            expr("minimal reproducible example (MRE)", "минимальный воспроизводимый пример", "Диагностика багов"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 22,
        "type": "GITHUB_ISSUES",
        "exercise_type": "LISTENING_CHOICE",
        "title": "GitHub Issue Triage: Duplicate Report",
        "prompt": "I am closing this issue as a duplicate of number eighteen so we can track all discussion in one thread.",
        "prompt_ru": "Прослушайте сообщение мейнтейнера и выберите правильную интерпретацию.",
        "solution": "Мейнтейнер закрывает issue как дубликат #18, чтобы вести всё обсуждение в одной ветке.",
        "options": [
            "Мейнтейнер закрывает issue как дубликат #18, чтобы вести всё обсуждение в одной ветке.",
            "Мейнтейнер просит открыть 18 новых issue для каждого экрана.",
            "Баг полностью исправлен в версии 1.8.",
            "Issue переносится в приватный репозиторий."
        ],
        "explanation_ru": "«Closing as a duplicate of #18» — стандартная практика объединения повторяющихся обращений в одном треде.",
        "useful_expressions": [
            expr("closing as a duplicate of #...", "закрываю как дубликат #...", "Триаж Issues"),
            expr("track all discussion in one thread", "вести всё обсуждение в одном треде", "Организация работы"),
        ],
        "difficulty": "B1",
    },

    # ==================== 6. INTERVIEW (5) ====================
    {
        "id": 23,
        "type": "INTERVIEW",
        "exercise_type": "INTERVIEW_ANSWER",
        "title": "Technical Interview: Explaining Clean Architecture Trade-Offs",
        "prompt": "Interviewer: \"Why did you separate the Domain layer into a pure Kotlin module instead of calling Room DAOs directly from your ViewModels?\"",
        "prompt_ru": "Интервьюер спрашивает: «Зачем вы вынесли Domain-слой в чистый Kotlin-модуль вместо прямых вызовов Room DAO из ViewModel?» Выберите сильнейший ответ уровня Middle/Senior.",
        "solution": "Isolating business rules like SM-2 in pure Kotlin enforces the Dependency Rule at compile time, keeps ViewModels slim, and lets us run fast JVM unit tests with high coverage.",
        "options": [
            "Isolating business rules like SM-2 in pure Kotlin enforces the Dependency Rule at compile time, keeps ViewModels slim, and lets us run fast JVM unit tests with high coverage.",
            "Because everyone on YouTube uses Clean Architecture and it looks impressive.",
            "Room doesn't allow calling DAOs from ViewModels in Android.",
            "I don't really know, it was just in a template."
        ],
        "explanation_ru": "На техническом интервью важно объяснять инженерные выгоды и компромиссы: изоляция бизнес-правил, контроль зависимостей на этапе компиляции, быстрые JVM-тесты.",
        "useful_expressions": [
            expr("enforces the Dependency Rule at compile time", "контролирует правило зависимостей при компиляции", "Архитектура"),
            expr("keeps ViewModels slim and testable", "сохраняет ViewModel компактными и тестируемыми", "MVVM"),
        ],
        "difficulty": "C1",
    },
    {
        "id": 24,
        "type": "INTERVIEW",
        "exercise_type": "INTERVIEW_ANSWER",
        "title": "Technical Interview: Offline-First Strategy",
        "prompt": "Interviewer: \"How do you ensure your Android application remains reliable with zero internet connectivity?\"",
        "prompt_ru": "Интервьюер спрашивает: «Как вы обеспечиваете стабильную работу Android-приложения без подключения к интернету?» Выберите наиболее грамотный ответ.",
        "solution": "We treat local Room SQLite as the single source of truth, bundle pre-populated assets via createFromAsset, use DataStore for preferences, and gracefully detect offline TTS voice availability.",
        "options": [
            "We treat local Room SQLite as the single source of truth, bundle pre-populated assets via createFromAsset, use DataStore for preferences, and gracefully detect offline TTS voice availability.",
            "We show a 'No Internet' dialog and block all screens until Wi-Fi returns.",
            "We store everything in static companion objects in RAM.",
            "Android automatically caches every API response offline."
        ],
        "explanation_ru": "Ключевые термины на интервью по мобильной архитектуре: «single source of truth» (единый источник истины), локальное хранилище Room, предзаполнение ассетов и graceful degradation.",
        "useful_expressions": [
            expr("single source of truth (SSOT)", "единый источник истины", "Архитектура данных"),
            expr("gracefully degrade when a capability is missing", "плавно деградировать при отсутствии компонента", "Надёжность"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 25,
        "type": "INTERVIEW",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Talking About Algorithmic Complexity",
        "prompt": "Build a crisp technical interview answer explaining how adding an index optimized database lookups.",
        "prompt_ru": "Соберите чёткий ответ на собеседовании о том, как добавление индекса ускорило выборку в базе данных.",
        "solution": "Adding a composite index reduced query time from linear scan to logarithmic lookup.",
        "options": [
            "Adding a composite index", "reduced query time", "from linear scan",
            "to logarithmic lookup.", "randomly", "increased latency"
        ],
        "explanation_ru": "Чёткое противопоставление «from linear scan to logarithmic lookup» демонстрирует уверенное владение терминологией алгоритмов и БД.",
        "useful_expressions": [
            expr("reduced query time from O(n) to O(log n)", "снизил время запроса с O(n) до O(log n)", "Оптимизация"),
            expr("composite index", "составной индекс", "Базы данных"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 26,
        "type": "INTERVIEW",
        "exercise_type": "FILL_BLANKS",
        "title": "Behavioral Question: Handling a Production Incident",
        "prompt": "Complete the STAR-method behavioral interview sentence:\n\"After resolving the crash, we wrote a blameless ___ to add automated migration tests to CI.\"",
        "prompt_ru": "Заполните пропуск в ответе на поведенческий вопрос (метод STAR) про разбор продакшен-инцидента.",
        "solution": "postmortem",
        "options": ["postmortem", "polyfill", "boilerplate", "shebang"],
        "explanation_ru": "«Blameless postmortem» — зрелая инженерная практика разбора инцидента с фокусом на системные улучшения, а не поиск виновных.",
        "useful_expressions": [
            expr("blameless postmortem", "разбор инцидента без поиска виноватых", "Инженерная культура"),
            expr("prevent similar regressions in CI", "предотвратить подобные регрессии в CI", "Качество"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 27,
        "type": "INTERVIEW",
        "exercise_type": "LISTENING_CHOICE",
        "title": "Interview Prompt: StateFlow vs SharedFlow",
        "prompt": "In Jetpack Compose, we expose immutable screen state via StateFlow and model one-shot events like Snackbars with a Channel or SharedFlow.",
        "prompt_ru": "Прослушайте объяснение кандидата и выберите верное утверждение.",
        "solution": "Неизменяемое состояние экрана передаётся через StateFlow, а одноразовые события (Snackbar) — через Channel или SharedFlow.",
        "options": [
            "Неизменяемое состояние экрана передаётся через StateFlow, а одноразовые события (Snackbar) — через Channel или SharedFlow.",
            "Все экраны должны хранить Snackbar прямо в базе Room.",
            "StateFlow не поддерживает Jetpack Compose.",
            "SharedFlow используется для хранения постоянного состояния экрана."
        ],
        "explanation_ru": "«One-shot events» — одноразовые события интерфейса (показ Snackbar, навигация), которые не должны повторяться при повороте экрана.",
        "useful_expressions": [
            expr("one-shot UI events", "одноразовые события интерфейса", "UDF / MVVM"),
            expr("expose immutable screen state", "публиковать неизменяемое состояние экрана", "Compose"),
        ],
        "difficulty": "B2",
    },

    # ==================== 7. TEAM_COMMUNICATION (5) ====================
    {
        "id": 28,
        "type": "TEAM_COMMUNICATION",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Asking for Help Efficiently on Slack",
        "prompt": "You have been stuck on a Hilt KSP build error for 30 minutes. What is the best Slack message to a senior teammate?",
        "prompt_ru": "Вы 30 минут разбираетесь с ошибкой сборки Hilt KSP. Выберите наиболее эффективное сообщение коллеге в Slack.",
        "solution": "Hi Alex! I'm hitting a Hilt MissingBinding error on VocabularyRepository. I checked @InstallIn(SingletonComponent) and cleaned caches, still stuck. Do you have 5 mins to pair?",
        "options": [
            "Hi Alex! I'm hitting a Hilt MissingBinding error on VocabularyRepository. I checked @InstallIn(SingletonComponent) and cleaned caches, still stuck. Do you have 5 mins to pair?",
            "Hey",
            "My build is broken, please fix it ASAP.",
            "Hilt is terrible, nothing compiles."
        ],
        "explanation_ru": "Правило «No Hello» + контекст: назовите точную ошибку, перечислите, что уже попробовали, и попросите конкретное небольшое время («5 mins to pair»).",
        "useful_expressions": [
            expr("I'm hitting an error on...", "Я столкнулся с ошибкой в...", "Описание проблемы"),
            expr("Do you have 5 minutes to pair?", "Есть 5 минут посмотреть вместе?", "Запрос помощи"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 29,
        "type": "TEAM_COMMUNICATION",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Aligning on an API Contract",
        "prompt": "Build a message proposing a quick sync to align mobile and backend teams on pagination fields.",
        "prompt_ru": "Соберите сообщение команде с предложением быстро синхронизироваться по формату пагинации.",
        "solution": "Let's sync briefly tomorrow to align on the pagination response schema.",
        "options": [
            "Let's sync briefly", "tomorrow", "to align on", "the pagination",
            "response schema.", "arguing", "breaking"
        ],
        "explanation_ru": "«Sync briefly» (быстро созвониться/свериться) и «align on» (прийти к единому пониманию) — базовые глаголы ежедневной коммуникации в распределённых командах.",
        "useful_expressions": [
            expr("Let's sync briefly on...", "Давайте коротко сверимся по...", "Планирование созвона"),
            expr("align on the contract", "согласовать контракт", "Взаимодействие команд"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 30,
        "type": "TEAM_COMMUNICATION",
        "exercise_type": "FILL_BLANKS",
        "title": "Declining Extra Work Professionally",
        "prompt": "Complete the polite message communicating capacity limits:\n\"I don't have the ___ to pick up the analytics redesign this week without delaying the release.\"",
        "prompt_ru": "Заполните пропуск, чтобы вежливо сообщить об отсутствии свободного ресурса на этой неделе.",
        "solution": "bandwidth",
        "options": ["bandwidth", "deadlock", "latency", "checksum"],
        "explanation_ru": "«Have the bandwidth» в рабочей переписке означает иметь свободное время и пропускную способность для новой задачи.",
        "useful_expressions": [
            expr("I don't have the bandwidth this week.", "У меня нет свободного ресурса на этой неделе.", "Управление нагрузкой"),
            expr("without delaying the release", "без задержки релиза", "Приоритизация"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 31,
        "type": "TEAM_COMMUNICATION",
        "exercise_type": "TRANSLATION_CHOICE",
        "title": "Slack Phrase: Loop Someone In",
        "prompt": "Your tech lead writes: \"Let's loop in the QA lead before changing the offline database schema.\" What does 'loop in' mean?",
        "prompt_ru": "Что означает фразовый глагол «loop in» в сообщении тимлида?",
        "solution": "Подключить к обсуждению / ввести в курс дела",
        "options": [
            "Подключить к обсуждению / ввести в курс дела",
            "Исключить из чата проекта",
            "Написать бесконечный цикл в коде",
            "Перенести встречу на месяц"
        ],
        "explanation_ru": "«Loop someone in» — добавить нужного человека в переписку, встречу или обсуждение, чтобы он был в контексте.",
        "useful_expressions": [
            expr("Let's loop in the QA lead.", "Давайте подключим к обсуждению лида QA.", "Координация"),
            expr("keep me in the loop", "держи меня в курсе", "Коммуникация"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 32,
        "type": "TEAM_COMMUNICATION",
        "exercise_type": "LISTENING_CHOICE",
        "title": "Team Sync: Feature Flag Rollout",
        "prompt": "Let's ship the new comparison screen behind a feature flag so we can gather internal feedback safely before enabling it for everyone.",
        "prompt_ru": "Прослушайте предложение коллеги и выберите его точный смысл.",
        "solution": "Выпустить экран сравнения под фича-флагом, чтобы безопасно собрать внутренний фидбек перед общим запуском.",
        "options": [
            "Выпустить экран сравнения под фича-флагом, чтобы безопасно собрать внутренний фидбек перед общим запуском.",
            "Отменить разработку экрана сравнения навсегда.",
            "Включить экран сравнения сразу для всех пользователей без тестов.",
            "Удалить все фича-флаги из проекта."
        ],
        "explanation_ru": "«Ship behind a feature flag» = выпустить код в релиз скрытым за переключателем для постепенного включения.",
        "useful_expressions": [
            expr("ship behind a feature flag", "выкатить под фича-флагом", "Безопасные релизы"),
            expr("gather internal feedback", "собрать внутреннюю обратную связь", "Проверка качества"),
        ],
        "difficulty": "B2",
    },

    # ==================== 8. BUG_REPORTING (4) ====================
    {
        "id": 33,
        "type": "BUG_REPORTING",
        "exercise_type": "SENTENCE_BUILD",
        "title": "Writing Expected vs Actual Behavior",
        "prompt": "Build a precise bug report description contrasting expected and actual behavior.",
        "prompt_ru": "Соберите чёткое описание бага, противопоставляющее ожидаемое и фактическое поведение.",
        "solution": "Expected the card to flip smoothly, but the app froze on Android 13.",
        "options": [
            "Expected the card", "to flip smoothly,", "but the app froze",
            "on Android 13.", "perfectly", "compiled"
        ],
        "explanation_ru": "Качественный баг-репорт всегда чётко разделяет Steps to Reproduce (шаги), Expected Behavior (ожидание) и Actual Behavior (факт) с указанием версии ОС.",
        "useful_expressions": [
            expr("Steps to reproduce:", "Шаги воспроизведения:", "Структура баг-репорта"),
            expr("Expected vs Actual behavior", "Ожидаемое и фактическое поведение", "Диагностика"),
        ],
        "difficulty": "A2",
    },
    {
        "id": 34,
        "type": "BUG_REPORTING",
        "exercise_type": "PHRASE_CHOICE",
        "title": "Reporting an Intermittent Bug Accurately",
        "prompt": "A notification reminder fails about 1 out of 5 times after reboot. How do you describe this accurately in a bug report?",
        "prompt_ru": "Уведомление о повторении не срабатывает примерно в 1 из 5 случаев после перезагрузки. Как точнее всего описать этот баг?",
        "solution": "Intermittent issue (~20% repro rate): WorkManager reminder occasionally skips execution after device reboot on Android 14; logcat attached.",
        "options": [
            "Intermittent issue (~20% repro rate): WorkManager reminder occasionally skips execution after device reboot on Android 14; logcat attached.",
            "Notifications never work at all.",
            "Weird bug happens sometimes idk why.",
            "Android 14 is broken."
        ],
        "explanation_ru": "«Intermittent issue» (плавающий баг) с указанием частоты воспроизведения («~20% repro rate») и приложенным logcat экономит часы отладки.",
        "useful_expressions": [
            expr("intermittent issue", "плавающая / непостоянная ошибка", "Классификация багов"),
            expr("repro rate (~20%)", "частота воспроизведения (~20%)", "QA-метрики"),
        ],
        "difficulty": "B2",
    },
    {
        "id": 35,
        "type": "BUG_REPORTING",
        "exercise_type": "FILL_BLANKS",
        "title": "Describing a Temporary Fix",
        "prompt": "Complete the bug ticket note explaining how users can bypass the issue while a permanent patch is developed:\n\"Temporary ___: toggling Dark Theme off and back on refreshes the syntax highlight colors immediately.\"",
        "prompt_ru": "Заполните пропущенный термин, обозначающий временное обходное решение проблемы.",
        "solution": "workaround",
        "options": ["workaround", "bottleneck", "throughput", "pipeline"],
        "explanation_ru": "«Workaround» — временный обходной путь, позволяющий пользователю продолжить работу до выхода исправления.",
        "useful_expressions": [
            expr("Temporary workaround:", "Временное обходное решение:", "Поддержка пользователей"),
            expr("permanent fix scheduled for v1.1", "постоянное исправление запланировано на v1.1", "Планирование"),
        ],
        "difficulty": "B1",
    },
    {
        "id": 36,
        "type": "BUG_REPORTING",
        "exercise_type": "TRANSLATION_CHOICE",
        "title": "Bug Report Severity: Edge Case",
        "prompt": "Choose the accurate translation of: \"This crash only triggers in an edge case when the user adds a word with 500 emoji characters.\"",
        "prompt_ru": "Выберите точный перевод термина «edge case» в данном баг-репорте.",
        "solution": "Крайний / пограничный случай",
        "options": [
            "Крайний / пограничный случай",
            "Основной пользовательский сценарий",
            "Ошибка сетевого соединения",
            "Проблема совместимости браузера Edge"
        ],
        "explanation_ru": "«Edge case» — редкий пограничный сценарий с экстремальными входными данными (пустая строка, 500 эмодзи, нулевое деление).",
        "useful_expressions": [
            expr("edge case", "крайний / пограничный случай", "Тестирование"),
            expr("only triggers when...", "срабатывает только когда...", "Условие воспроизведения"),
        ],
        "difficulty": "A2",
    },
]

# ==================== 64 PROFESSIONAL IT WORKPLACE PHRASES ====================
WORK_PHRASES = [
    # DAILY_STANDUP (8)
    (1, "DAILY_STANDUP", "Yesterday I wrapped up the database migration and opened a PR.", "Вчера я завершил миграцию базы данных и открыл PR.", "Отчёт о выполненной работе на стендапе", "B1"),
    (2, "DAILY_STANDUP", "Today I'll be focusing on unit tests for the SM-2 engine.", "Сегодня я сфокусируюсь на юнит-тестах для движка SM-2.", "План на текущий рабочий день", "B1"),
    (3, "DAILY_STANDUP", "I'm currently blocked on API credentials from DevOps.", "Я сейчас заблокирован ожиданием API-ключей от DevOps.", "Сообщение о блокере", "B1"),
    (4, "DAILY_STANDUP", "No blockers on my end, everything is on track.", "С моей стороны блокеров нет, всё идёт по плану.", "Спокойное завершение апдейта", "A2"),
    (5, "DAILY_STANDUP", "Heads up: this task might slip into the next sprint.", "Предупреждаю заранее: эта задача может перенестись в следующий спринт.", "Раннее предупреждение о сдвиге сроков", "B2"),
    (6, "DAILY_STANDUP", "Let's take the deep-dive discussion offline right after standup.", "Давайте вынесем подробное обсуждение за рамки стендапа сразу после встречи.", "Экономия времени команды", "B1"),
    (7, "DAILY_STANDUP", "I'm pairing with Maria this afternoon to debug the crash.", "Сегодня после обеда я в паре с Марией отлаживаю падение.", "Упоминание парного программирования", "B1"),
    (8, "DAILY_STANDUP", "Two tickets are ready for QA verification in staging.", "Два тикета готовы к проверке QA на staging-среде.", "Передача задач тестировщикам", "A2"),

    # CODE_REVIEW (8)
    (9, "CODE_REVIEW", "Nit: could we rename this variable to make its unit explicit?", "Мелочь: можем переименовать переменную, чтобы была ясна единица измерения?", "Неблокирующее замечание по неймингу", "B1"),
    (10, "CODE_REVIEW", "LGTM! Just one non-blocking suggestion below.", "Выглядит отлично! Ниже одно необязательное предложение.", "Одобрение кода с мелким комментарием", "A2"),
    (11, "CODE_REVIEW", "Could we guard against a null or empty list here?", "Можем ли мы добавить проверку на null или пустой список здесь?", "Мягкое указание на потенциальный баг", "B1"),
    (12, "CODE_REVIEW", "Consider extracting this logic into a pure helper function.", "Рассмотри вариант вынести эту логику в чистую вспомогательную функцию.", "Предложение декомпозиции", "B2"),
    (13, "CODE_REVIEW", "Great catch on avoiding recomposition inside LazyColumn!", "Отличная оптимизация рекомпозиций внутри LazyColumn!", "Позитивная обратная связь", "B2"),
    (14, "CODE_REVIEW", "Is this database query guaranteed to run off the main thread?", "Гарантировано ли, что этот запрос к БД выполняется не на главном потоке?", "Проверка производительности", "B2"),
    (15, "CODE_REVIEW", "What happens if the user rotates the screen mid-request?", "Что произойдёт, если пользователь повернёт экран во время запроса?", "Проверка крайних случаев Android", "B1"),
    (16, "CODE_REVIEW", "Let's add a unit test covering the minimum ease factor floor.", "Давай добавим юнит-тест, покрывающий нижнюю границу коэффициента лёгкости.", "Запрос тестового покрытия", "B1"),

    # PR_COMMENTS (8)
    (17, "PR_COMMENTS", "Good catch! Addressed in the latest commit.", "Отлично подмечено! Исправлено в последнем коммите.", "Ответ на замечание ревьюера", "A2"),
    (18, "PR_COMMENTS", "I rebased onto main and resolved the merge conflicts.", "Я перебазировал ветку на main и разрешил конфликты слияния.", "Обновление ветки перед мержем", "B1"),
    (19, "PR_COMMENTS", "To keep this PR small, I opened a follow-up ticket for animations.", "Чтобы PR оставался компактным, я создал отдельную задачу на анимации.", "Контроль размера PR", "B2"),
    (20, "PR_COMMENTS", "Could you please re-review when you get a chance?", "Не мог бы ты перепроверить PR, когда будет минутка?", "Запрос повторного ревью", "A2"),
    (21, "PR_COMMENTS", "Squashing commits and merging once CI turns green.", "Сожму коммиты и волью, как только CI станет зелёным.", "Финальный шаг перед слиянием", "B1"),
    (22, "PR_COMMENTS", "This change is strictly additive and fully backward-compatible.", "Это изменение чисто аддитивное и полностью обратно совместимо.", "Подтверждение безопасности контракта", "B2"),
    (23, "PR_COMMENTS", "I attached before-and-after screenshots in the PR description.", "Я прикрепил скриншоты «до и после» в описании PR.", "Демонстрация UI-правок", "A2"),
    (24, "PR_COMMENTS", "Draft PR: looking for early feedback on the architecture approach.", "Черновой PR: жду раннюю обратную связь по архитектурному подходу.", "Ранний архитектурный фидбек", "B1"),

    # JIRA_TICKETS (8)
    (25, "JIRA_TICKETS", "Acceptance criteria have been verified on Android 8 and Android 15.", "Критерии приёмки проверены на Android 8 и Android 15.", "Закрытие задачи", "B1"),
    (26, "JIRA_TICKETS", "Moving this story to In Progress; ETA is Thursday afternoon.", "Перевожу историю в работу; ориентировочный срок — четверг после обеда.", "Обновление оценки сроков", "B1"),
    (27, "JIRA_TICKETS", "Quick clarification before starting: should custom words be editable?", "Небольшое уточнение перед стартом: пользовательские слова должны редактироваться?", "Снятие неоднозначности", "B1"),
    (28, "JIRA_TICKETS", "This task depends on the Room schema ticket being merged first.", "Эта задача зависит от предварительного вливания тикета со схемой Room.", "Фиксация зависимости", "B1"),
    (29, "JIRA_TICKETS", "Breaking this epic down into three independently testable subtasks.", "Декомпозирую этот эпик на три независимо тестируемые подзадачи.", "Декомпозиция бэклога", "B2"),
    (30, "JIRA_TICKETS", "Let's timebox the investigation spike to four hours.", "Давайте ограничим исследовательский спайк четырьмя часами.", "Контроль времени исследования", "B2"),
    (31, "JIRA_TICKETS", "Updated the ticket scope to reflect our alignment with product.", "Обновил объём тикета согласно договорённости с продуктом.", "Актуализация требований", "B2"),
    (32, "JIRA_TICKETS", "Closing as Won't Do per our retrospective decision.", "Закрываю со статусом «Не будет сделано» по итогам ретроспективы.", "Закрытие неактуального тикета", "B1"),

    # GITHUB_ISSUES (8)
    (33, "GITHUB_ISSUES", "Thanks for reporting this! Could you share a minimal reproducible example?", "Спасибо за репорт! Не могли бы вы приложить минимальный воспроизводимый пример?", "Триаж нового Issue", "B1"),
    (34, "GITHUB_ISSUES", "Closing as a duplicate of #18 so we keep discussion centralized.", "Закрываю как дубликат #18, чтобы вести обсуждение в одном месте.", "Закрытие дубликата", "B1"),
    (35, "GITHUB_ISSUES", "Fixes #42 by validating the SQLite schema identity hash in CI.", "Исправляет #42, добавляя проверку identity hash схемы SQLite в CI.", "Связывание фикса с Issue", "B2"),
    (36, "GITHUB_ISSUES", "We welcome pull requests for this enhancement—marking as help wanted.", "Мы приветствуем PR для этого улучшения — ставлю метку help wanted.", "Работа с контрибьюторами", "B1"),
    (37, "GITHUB_ISSUES", "Could you confirm whether this still reproduces on version 1.0?", "Можете подтвердить, воспроизводится ли это на версии 1.0?", "Проверка актуальности", "A2"),
    (38, "GITHUB_ISSUES", "Root cause identified: TTS crashed when no offline English voice was installed.", "Первопричина найдена: TTS падал при отсутствии офлайн-голоса.", "Публикация диагноза", "B2"),
    (39, "GITHUB_ISSUES", "Tagged for the v2.0 milestone since it requires cloud synchronization.", "Пометил для вехи v2.0, так как это требует облачной синхронизации.", "Распределение по релизам", "B1"),
    (40, "GITHUB_ISSUES", "Workaround documented in the README until the upstream patch lands.", "Обходное решение описано в README до выхода исправления в библиотеке.", "Временная помощь пользователям", "B2"),

    # INTERVIEW (8)
    (41, "INTERVIEW", "Let me clarify the functional requirements and scale constraints first.", "Позвольте сперва уточнить функциональные требования и ограничения масштаба.", "Начало секции System Design", "B2"),
    (42, "INTERVIEW", "That's a classic trade-off between memory footprint and query latency.", "Это классический компромисс между потреблением памяти и задержкой запроса.", "Обсуждение компромиссов", "C1"),
    (43, "INTERVIEW", "We treat the local Room database as the single source of truth.", "Мы используем локальную базу Room как единый источник истины.", "Архитектура Offline-first", "B2"),
    (44, "INTERVIEW", "In the worst case, time complexity is O(n log n) with O(1) extra space.", "В худшем случае временная сложность O(n log n) при O(1) доп. памяти.", "Алгоритмическая секция", "B2"),
    (45, "INTERVIEW", "To prevent regressions, we cover the pure domain layer with over 80% unit tests.", "Чтобы избежать регрессий, мы покрываем доменный слой тестами более чем на 80%.", "Ответ про качество и QA", "B2"),
    (46, "INTERVIEW", "I haven't used that specific library in production, but I understand its core trade-offs.", "Я не использовал эту библиотеку в проде, но понимаю её ключевые компромиссы.", "Честный зрелый ответ на пробел", "C1"),
    (47, "INTERVIEW", "Walk me through the expected read-to-write ratio before choosing indexes.", "Расскажите об ожидаемом соотношении чтений и записей перед выбором индексов.", "Проектирование БД", "C1"),
    (48, "INTERVIEW", "After the outage, we conducted a blameless postmortem and added alerts.", "После сбоя мы провели разбор без поиска виноватых и добавили алерты.", "Поведенческий вопрос (STAR)", "B2"),

    # TEAM_COMMUNICATION (8)
    (49, "TEAM_COMMUNICATION", "Let's align on the JSON payload format before either side starts coding.", "Давайте согласуем формат JSON до того, как обе стороны начнут писать код.", "Синхронизация контрактов", "B1"),
    (50, "TEAM_COMMUNICATION", "Could we loop in the designer to confirm dark-theme contrast ratios?", "Можем подключить дизайнера, чтобы подтвердить контрастность тёмной темы?", "Вовлечение коллег", "B1"),
    (51, "TEAM_COMMUNICATION", "I don't have the bandwidth this sprint without pushing back the release.", "У меня нет ресурса в этом спринте без сдвига даты релиза.", "Корректное обозначение границ", "B2"),
    (52, "TEAM_COMMUNICATION", "Thanks for unblocking me so quickly!", "Спасибо, что так быстро помог снять блокер!", "Благодарность коллеге", "A2"),
    (53, "TEAM_COMMUNICATION", "Let's ship this behind a feature flag and roll it out gradually.", "Давайте выкатим это под фича-флагом и будем включать постепенно.", "Безопасная доставка", "B2"),
    (54, "TEAM_COMMUNICATION", "Just following up on the RFC—any remaining concerns before we finalize?", "Напоминаю про RFC — остались ли ещё замечания перед финализацией?", "Вежливый пинг в чате", "B2"),
    (55, "TEAM_COMMUNICATION", "I'll document the setup steps in the wiki to improve our bus factor.", "Я задокументирую шаги настройки в вики, чтобы снизить зависимость от одного человека.", "Обмен знаниями", "B2"),
    (56, "TEAM_COMMUNICATION", "Do you have ten minutes to rubber-duck a tricky Room migration?", "Найдётся десять минут обсудить непростую миграцию Room?", "Запрос быстрого брейншторма", "B1"),

    # BUG_REPORTING (8)
    (57, "BUG_REPORTING", "Steps to reproduce: open a doc article, tap a word, and press Add to Vocabulary.", "Шаги воспроизведения: откройте статью, нажмите на слово и выберите «В словарь».", "Точное описание шагов", "A2"),
    (58, "BUG_REPORTING", "Expected behavior: the card schedules for tomorrow. Actual behavior: interval stays zero.", "Ожидаемое поведение: карточка планируется на завтра. Фактическое: интервал остаётся нулём.", "Контраст ожидания и факта", "B1"),
    (59, "BUG_REPORTING", "This is an intermittent crash reproducing in roughly 20% of offline launches.", "Это плавающий краш, воспроизводимый примерно в 20% офлайн-запусков.", "Оценка частоты бага", "B2"),
    (60, "BUG_REPORTING", "Stack trace and device metadata are attached below.", "Стектрейс и данные об устройстве приложены ниже.", "Сопровождение логами", "A2"),
    (61, "BUG_REPORTING", "Temporary workaround: restart the listening session to rebind the audio player.", "Временный обход: перезапустите сессию аудирования для переподключения плеера.", "Описание обходного пути", "B1"),
    (62, "BUG_REPORTING", "The regression was introduced in commit 4a8f12c according to git bisect.", "По данным git bisect регрессия появилась в коммите 4a8f12c.", "Точная локализация регрессии", "B2"),
    (63, "BUG_REPORTING", "Only occurs in an edge case when the search query contains special SQL characters.", "Возникает только в пограничном случае при спецсимволах SQL в поиске.", "Описание edge case", "B2"),
    (64, "BUG_REPORTING", "Marking as P0 critical because it prevents cold startup on Android 8.0.", "Помечаю как критический P0, так как он блокирует холодный запуск на Android 8.0.", "Приоритизация дефекта", "B2"),
]

from seed_data.scenarios_v3_expansion import SCENARIOS_V3_EXPANSION, WORK_PHRASES_V3_EXPANSION

SCENARIOS.extend(SCENARIOS_V3_EXPANSION)
WORK_PHRASES.extend(WORK_PHRASES_V3_EXPANSION)

