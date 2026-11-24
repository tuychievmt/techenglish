# -*- coding: utf-8 -*-
"""
32 Technical Documentation Articles & Error Logs across all 9 required categories:
Programming, Linux, Git, SQL, REST API, HTTP, Networking, Software Engineering, Error Logs.
Each article includes an embedded contextual glossary for interactive word-tap lookup.
"""

def g(term, ipa, ru, expl, ex):
    return {
        "term": term,
        "transcription": ipa,
        "translation": ru,
        "explanationRu": expl,
        "example": ex,
    }

DOC_ARTICLES = [
    # ==================== PROGRAMMING (4) ====================
    {
        "id": 1,
        "title": "Kotlin Coroutines: Structured Concurrency",
        "tech_tag": "Programming",
        "level": "B2",
        "content": (
            "Structured concurrency ensures that every coroutine runs inside a well-defined scope with an explicit lifetime. "
            "When a parent scope cancels, all active child coroutines cancel automatically, preventing silent memory leaks and orphaned background work. "
            "Use Dispatchers.IO for blocking disk or database queries, Dispatchers.Default for CPU-intensive parsing, and Dispatchers.Main solely for updating Compose UI state. "
            "Never suppress CancellationException inside a broad catch block, because doing so breaks cooperative cancellation across the entire hierarchy."
        ),
        "explanation_ru": (
            "Структурная конкурентность в Kotlin гарантирует, что каждая корутина привязана к жизненному циклу своей области (CoroutineScope). "
            "При отмене родительской области (например, viewModelScope) дочерние задачи отменяются автоматически, защищая от утечек памяти. "
            "Важное правило: никогда не «проглатывайте» CancellationException в общем блоке catch — это ломает кооперативную отмену."
        ),
        "glossary": [
            g("concurrency", "/kənˈkʌr.ən.si/", "конкурентность", "Совместное выполнение нескольких задач в перекрывающиеся периоды времени.", "Structured concurrency keeps background jobs predictable."),
            g("scope", "/skoʊp/", "область видимости / жизненного цикла", "Контекст, ограничивающий время жизни дочерних корутин.", "Launch UI flows inside viewModelScope."),
            g("orphaned", "/ˈɔːr.fənd/", "осиротевший (потерявший родителя)", "Фоновый процесс, оставшийся без управляющего владельца.", "Orphaned coroutines keep draining battery after screen exit."),
            g("suppress", "/səˈpres/", "подавлять (скрывать ошибку)", "Перехватить исключение без повторного проброса.", "Do not suppress CancellationException in repositories."),
            g("cooperative", "/koʊˈɒp.ər.ə.tɪv/", "кооперативный", "Зависящий от явного согласия кода проверять статус отмены.", "Coroutine cancellation is cooperative in Kotlin."),
        ],
    },
    {
        "id": 2,
        "title": "Rust Ownership and the Borrow Checker",
        "tech_tag": "Programming",
        "level": "C1",
        "content": (
            "Rust achieves memory safety without a garbage collector through three compile-time ownership rules. "
            "Each value has a single owner; when the owner goes out of scope, the value is dropped deterministically. "
            "Code may hold either multiple immutable references or exactly one mutable reference at any point in time. "
            "By enforcing these aliasing invariants at compile time, the borrow checker eliminates data races and dangling pointers before the binary ever executes."
        ),
        "explanation_ru": (
            "Rust обеспечивает безопасность памяти без сборщика мусора за счёт правил владения: у каждого значения ровно один владелец, при выходе из области видимости память освобождается детерминированно. "
            "Правило заимствования разрешает либо много неизменяемых ссылок (&T), либо одну изменяемую (&mut T), полностью исключая гонки данных на этапе компиляции."
        ),
        "glossary": [
            g("ownership", "/ˈoʊ.nər.ʃɪp/", "владение", "Модель управления памятью в Rust с единственным владельцем ресурса.", "Moving a String transfers ownership to the callee."),
            g("deterministically", "/dɪˌtɜːr.mɪˈnɪs.tɪ.kəl.i/", "детерминированно (предсказуемо)", "В точно известный момент без пауз сборщика мусора.", "File handles close deterministically on drop."),
            g("aliasing", "/ˈeɪ.li.ə.sɪŋ/", "алиасинг (несколько ссылок на одну память)", "Наличие двух и более указателей на один участок памяти.", "Mutable aliasing is disallowed by Rust's borrow checker."),
            g("dangling", "/ˈdæŋ.ɡlɪŋ/", "висячий (указывающий на освобождённую память)", "Указатель на уже уничтоженный объект.", "Dangling pointers cause unpredictable crashes in C."),
        ],
    },
    {
        "id": 3,
        "title": "Python Generators and Lazy Evaluation",
        "tech_tag": "Programming",
        "level": "B1",
        "content": (
            "Loading a multi-gigabyte log file into a Python list immediately exhausts available RAM. "
            "Instead, declare a generator function using the yield keyword to produce one record at a time on demand. "
            "Each call to next() resumes execution right after the previous yield statement while keeping local variables intact. "
            "Chaining generator expressions creates clean streaming pipelines with constant O(1) memory overhead."
        ),
        "explanation_ru": (
            "Вместо загрузки гигантского файла целиком в список используйте генераторы с ключевым словом yield. "
            "Генератор вычисляет элементы лениво — по одному при каждом запросе next() — сохраняя постоянный расход памяти O(1)."
        ),
        "glossary": [
            g("exhausts", "/ɪɡˈzɔːsts/", "исчерпывает", "Полностью расходует доступный системный ресурс.", "Unbounded caching quickly exhausts heap memory."),
            g("yield", "/jiːld/", "возвращать из генератора", "Приостанавливает функцию и отдаёт очередное значение.", "Yield each parsed CSV row lazily."),
            g("intact", "/ɪnˈtækt/", "нетронутым / сохранённым", "Остающимся в прежнем состоянии между вызовами.", "Local frame state remains intact across yields."),
            g("overhead", "/ˈoʊ.vər.hed/", "накладные расходы", "Дополнительные затраты памяти или CPU на обслуживание механизма.", "Generators minimize allocation overhead."),
        ],
    },
    {
        "id": 4,
        "title": "JavaScript Event Loop and Microtask Queue",
        "tech_tag": "Programming",
        "level": "B2",
        "content": (
            "JavaScript runs user code on a single main thread coordinated by an asynchronous event loop. "
            "Callbacks scheduled by setTimeout enter the macrotask queue, whereas resolved Promise continuations run in the higher-priority microtask queue. "
            "After every synchronous call stack finishes, the engine drains the entire microtask queue before painting the next browser frame or dequeuing timers. "
            "Scheduling recursive microtasks carelessly can starve rendering and freeze user interactions."
        ),
        "explanation_ru": (
            "JavaScript исполняется в одном главном потоке с циклом событий (Event Loop). "
            "Промисы (Promise.then / await) попадают в очередь микрозадач (microtasks), которая полностью очищается раньше таймеров setTimeout и перерисовки кадра."
        ),
        "glossary": [
            g("continuations", "/kənˌtɪn.juˈeɪ.ʃənz/", "продолжения (коллбэки после await)", "Код, выполняемый после завершения асинхронной операции.", "Promise continuations execute as microtasks."),
            g("drains", "/dreɪnz/", "полностью опустошает", "Обрабатывает все накопившиеся элементы очереди до конца.", "The runtime drains microtasks before repainting."),
            g("starve", "/stɑːrv/", "вызывать голодание (лишать ресурсов)", "Не давать другим задачам получить процессорное время.", "Infinite microtasks starve UI rendering."),
        ],
    },

    # ==================== LINUX (4) ====================
    {
        "id": 5,
        "title": "Understanding Linux File Permissions (chmod & chown)",
        "tech_tag": "Linux",
        "level": "A2",
        "content": (
            "Every file and directory on Linux carries three permission groups: owner, group, and others. "
            "Each group combines read (4), write (2), and execute (1) bits into an octal mode such as 755 for executable scripts or 600 for private SSH keys. "
            "Applying chmod 777 blindly exposes sensitive credentials to every local account and breaks SSH key validation. "
            "Use chown to transfer ownership and verify effective rights with ls -la before deploying services."
        ),
        "explanation_ru": (
            "Права доступа в Linux делятся на три группы: владелец (owner), группа (group) и остальные (others), а права кодируются битами r=4, w=2, x=1. "
            "Для исполняемых скриптов стандарт — 755, для приватных SSH-ключей — строго 600; выставлять 777 небезопасно."
        ),
        "glossary": [
            g("octal", "/ˈɒk.təl/", "восьмеричный", "Система счисления с основанием 8, используемая в chmod.", "Mode 644 is the octal representation of rw-r--r--."),
            g("credentials", "/krɪˈden.ʃəlz/", "учётные данные / ключи доступа", "Пароли, токены, SSH-ключи или сертификаты.", "Protect private credentials with chmod 600."),
            g("blindly", "/ˈblaɪnd.li/", "вслепую / бездумно", "Без анализа последствий.", "Never apply chmod 777 blindly in production."),
        ],
    },
    {
        "id": 6,
        "title": "Inspecting Systemd Services with journalctl",
        "tech_tag": "Linux",
        "level": "B1",
        "content": (
            "Modern Linux servers manage background daemons through systemd unit files. "
            "When a service crashes repeatedly, inspect recent logs with journalctl -u api.service -n 100 --no-pager or stream live output using the -f flag. "
            "Check systemctl status to verify the exit code, memory usage, and active PID. "
            "Always run systemctl daemon-reload after editing a unit file so systemd picks up the revised configuration."
        ),
        "explanation_ru": (
            "Фоновые службы (демоны) в Linux управляются через systemd. "
            "Команда journalctl -u позволяет читать логи конкретного сервиса, systemctl status показывает код выхода и PID, а daemon-reload перечитывает обновлённые unit-файлы."
        ),
        "glossary": [
            g("daemons", "/ˈdiː.mənz/", "демоны (фоновые службы)", "Постоянно работающие системные процессы без привязки к терминалу.", "Systemd restarts crashed daemons automatically."),
            g("revised", "/rɪˈvaɪzd/", "изменённый / обновлённый", "Отредактированная версия конфигурации или документа.", "Reload systemd to apply the revised unit file."),
            g("stream", "/striːm/", "транслировать в реальном времени", "Непрерывно выводить поступающие записи лога.", "Stream live container logs with -f."),
        ],
    },
    {
        "id": 7,
        "title": "Linux Process Signals: SIGTERM vs SIGKILL",
        "tech_tag": "Linux",
        "level": "B2",
        "content": (
            "Orchestrators like Kubernetes stop containers gracefully by sending SIGTERM (signal 15) first. "
            "Well-behaved applications catch SIGTERM, stop accepting new traffic, flush open buffers, and close database connections cleanly. "
            "If the process ignores SIGTERM past the termination grace period, the kernel forcibly terminates it with SIGKILL (signal 9), which cannot be intercepted or handled. "
            "Relying on SIGKILL risks corrupting uncommitted writes and dropping active user requests."
        ),
        "explanation_ru": (
            "SIGTERM (15) — вежливый запрос на завершение, который приложение может перехватить для корректного закрытия соединений (graceful shutdown). "
            "SIGKILL (9) мгновенно убивает процесс на уровне ядра без возможности перехвата, что рискованно для незавершённых транзакций."
        ),
        "glossary": [
            g("flush", "/flʌʃ/", "сбросить буфер на диск", "Принудительно записать накопленные в памяти данные.", "Flush log buffers before shutting down."),
            g("forcibly", "/ˈfɔːr.sə.bli/", "принудительно", "Силой на уровне ОС без согласия процесса.", "SIGKILL terminates hung processes forcibly."),
            g("intercepted", "/ˌɪn.tərˈsep.tɪd/", "перехваченный", "Обработанный пользовательским обработчиком сигнала.", "Unlike SIGTERM, SIGKILL cannot be intercepted."),
        ],
    },
    {
        "id": 8,
        "title": "Pipelines and Stream Redirection in Bash",
        "tech_tag": "Linux",
        "level": "B1",
        "content": (
            "Unix processes inherit three standard streams: stdin (0), stdout (1), and stderr (2). "
            "The pipe operator (|) connects stdout of one filter directly into stdin of the next without creating temporary files on disk. "
            "Redirect diagnostic errors separately using 2> error.log, or merge stderr into stdout with 2>&1 when capturing full CI build output. "
            "Enable pipefail in Bash scripts so a failed upstream command does not get masked by a succeeding downstream filter."
        ),
        "explanation_ru": (
            "Каждый процесс Unix имеет три потока: stdin (0), stdout (1) и stderr (2). "
            "Оператор | связывает вывод одной утилиты со входом следующей, 2>&1 объединяет ошибки с обычным выводом, а set -o pipefail не даёт скрыть ошибку в начале конвейера."
        ),
        "glossary": [
            g("inherit", "/ɪnˈher.ɪt/", "наследовать", "Получать автоматически от родительского процесса.", "Child processes inherit environment variables."),
            g("masked", "/mɑːskt/", "замаскированный / скрытый", "Спрятанный за успешным кодом последующей команды.", "Without pipefail, compiler failures can be masked by grep."),
            g("downstream", "/ˌdaʊnˈstriːm/", "ниже по потоку (последующий)", "Следующий этап в конвейере обработки данных.", "Downstream services consume sanitized events."),
        ],
    },

    # ==================== GIT (4) ====================
    {
        "id": 9,
        "title": "Git Rebase vs Git Merge: Choosing Clean History",
        "tech_tag": "Git",
        "level": "B1",
        "content": (
            "Both git merge and git rebase integrate changes from one branch into another, yet they shape repository history very differently. "
            "Merging preserves exact chronological context by creating a two-parent merge commit, which is completely non-destructive. "
            "Rebasing replays your local commits onto the tip of main, yielding a linear history that simplifies git bisect and code archaeology. "
            "Golden rule: never rebase commits that have already been pushed to a shared branch used by teammates."
        ),
        "explanation_ru": (
            "git merge сохраняет реальную историю ветвления через merge-коммит и не меняет хеши. "
            "git rebase переписывает ваши локальные коммиты поверх свежей ветки main, делая историю линейной. Золотое правило: не делайте rebase для общих публичных веток."
        ),
        "glossary": [
            g("non-destructive", "/ˌnɒn.dɪˈstrʌk.tɪv/", "неразрушающий", "Не переписывающий существующую историю коммитов.", "Git merge is a non-destructive operation."),
            g("replays", "/ˌriːˈpleɪz/", "повторно применяет", "Пошагово накатывает изменения коммитов заново.", "Rebase replays feature commits onto origin/main."),
            g("tip", "/tɪp/", "вершина ветки (последний коммит)", "Самый свежий коммит указанной ветки.", "Rebase onto the latest tip of main."),
        ],
    },
    {
        "id": 10,
        "title": "Recovering Lost Commits with Git Reflog",
        "tech_tag": "Git",
        "level": "B2",
        "content": (
            "Accidentally running git reset --hard rarely destroys committed work permanently. "
            "Git maintains a local reference log (reflog) recording every movement of HEAD and branch pointers over the past ninety days by default. "
            "Run git reflog to locate the commit hash prior to the mistake, inspect it with git show, and restore a branch using git checkout -b rescue <hash>. "
            "Remember that reflog is strictly local and never synchronized to remote servers."
        ),
        "explanation_ru": (
            "Случайный git reset --hard обычно не уничтожает коммиты навсегда — локальный журнал git reflog хранит все перемещения указателя HEAD до 90 дней. "
            "Найдите нужный хеш в reflog и восстановите ветку командой git checkout -b rescue <hash>."
        ),
        "glossary": [
            g("permanently", "/ˈpɜːr.mə.nənt.li/", "безвозвратно / навсегда", "Без возможности последующего восстановления.", "Committed objects are not deleted permanently right away."),
            g("prior to", "/ˈpraɪ.ər tuː/", "до / перед", "Предшествующий указанному событию.", "Locate the SHA hash prior to the accidental reset."),
            g("rescue", "/ˈres.kjuː/", "спасение / восстановление", "Временная ветка для возврата потерянных правок.", "Create a rescue branch from the reflog entry."),
        ],
    },
    {
        "id": 11,
        "title": "Conventional Commits Specification",
        "tech_tag": "Git",
        "level": "A2",
        "content": (
            "Conventional Commits define a lightweight convention for structured commit messages: type(scope): description. "
            "Common types include feat for new features, fix for bug fixes, refactor for structural improvements, docs for documentation, and test for test suites. "
            "Appending an exclamation mark such as feat(api)!: indicates a breaking change that triggers a major Semantic Versioning bump. "
            "Consistent commit prefixes enable automated changelogs and instant release notes generation."
        ),
        "explanation_ru": (
            "Стандарт Conventional Commits задаёт единый формат коммитов: тип(область): описание (feat, fix, refactor, docs, test). "
            "Восклицательный знак (feat!:) обозначает обратно несовместимое изменение (breaking change) и позволяет автоматически генерировать CHANGELOG."
        ),
        "glossary": [
            g("lightweight", "/ˈlaɪt.weɪt/", "лёгковесный / простой", "Не требующий сложной инфраструктуры или лишних правил.", "Conventional Commits is a lightweight message standard."),
            g("breaking change", "/ˈbreɪ.kɪŋ tʃeɪndʒ/", "обратно несовместимое изменение", "Изменение API, ломающее существующих клиентов.", "Document every breaking change in the commit footer."),
            g("changelog", "/ˈtʃeɪndʒ.lɒɡ/", "журнал изменений релиза", "Список нововведений и исправлений по версиям.", "CI generates a Markdown changelog automatically."),
        ],
    },
    {
        "id": 12,
        "title": "Hunting Regressions with git bisect",
        "tech_tag": "Git",
        "level": "B2",
        "content": (
            "When a bug appears somewhere across hundreds of commits, checking revisions sequentially wastes hours. "
            "git bisect automates a binary search between a known good commit and a broken bad commit, narrowing 1,024 revisions down to ten steps. "
            "You can even run git bisect run ./gradlew test so Git executes your automated test suite unattended until it prints the exact culprit commit. "
            "Finish with git bisect reset to return your working tree to its original branch."
        ),
        "explanation_ru": (
            "git bisect выполняет бинарный поиск среди сотен коммитов между заведомо рабочей (good) и сломанной (bad) версией. "
            "С командой git bisect run можно передать автотест — Git сам найдёт точный коммит-виновник регрессии."
        ),
        "glossary": [
            g("regressions", "/rɪˈɡreʃ.ənz/", "регрессии (поломки ранее работавшего)", "Появление ошибки в ранее исправной функциональности.", "Automated tests catch regressions before release."),
            g("unattended", "/ˌʌn.əˈten.dɪd/", "автоматически / без участия человека", "Выполняемый скриптом без ручного подтверждения.", "Bisect run executes unattended overnight."),
            g("culprit", "/ˈkʌl.prɪt/", "виновник (проблемный коммит)", "Конкретное изменение, вызвавшее ошибку.", "Git bisect printed the culprit commit in two minutes."),
        ],
    },

    # ==================== SQL (4) ====================
    {
        "id": 13,
        "title": "SQLite B-Tree Indexes and Query Planning",
        "tech_tag": "SQL",
        "level": "B2",
        "content": (
            "Without an index, SQLite evaluates a WHERE clause using a full table scan whose cost grows linearly with row count. "
            "Creating a B-Tree index lets the query planner jump straight to matching rows in logarithmic O(log N) time, keeping local mobile queries well below 100 ms. "
            "Composite indexes follow the leftmost prefix rule: an index on (language_id, task_key) accelerates filters on language_id alone as well as both columns together. "
            "Inspect actual index usage with EXPLAIN QUERY PLAN before shipping schema changes."
        ),
        "explanation_ru": (
            "Без индекса SQLite вынужден сканировать всю таблицу (full table scan). B-Tree индекс ускоряет поиск до O(log N). "
            "Составные индексы работают по правилу левого префикса: индекс (language_id, task_key) ускоряет поиск и по одному language_id, и по обоим полям сразу."
        ),
        "glossary": [
            g("linearly", "/ˈlɪn.i.ər.li/", "линейно", "Пропорционально увеличению объёма данных O(N).", "Scan latency grows linearly without an index."),
            g("leftmost", "/ˈleft.moʊst/", "самый левый (ведущий)", "Первый столбец в определении составного индекса.", "Place the most selective filter in the leftmost column."),
            g("accelerates", "/əkˈsel.ə.reɪts/", "ускоряет", "Существенно сокращает время выполнения.", "An index on next_review_at accelerates SRS queries."),
        ],
    },
    {
        "id": 14,
        "title": "INNER JOIN vs LEFT JOIN Explained",
        "tech_tag": "SQL",
        "level": "A2",
        "content": (
            "Relational databases split related entities into separate tables to avoid duplicate data. "
            "An INNER JOIN returns only rows that have matching keys on both sides, making it ideal for finding shared coding tasks across two programming languages. "
            "A LEFT JOIN keeps every row from the left table and fills unmatched right-side columns with NULL, which is essential when showing categories that currently have zero words. "
            "Watch out: filtering a right-table column in the WHERE clause accidentally turns a LEFT JOIN into an INNER JOIN."
        ),
        "explanation_ru": (
            "INNER JOIN возвращает только строки с совпадениями в обеих таблицах (отлично подходит для пересечения общих задач двух языков). "
            "LEFT JOIN сохраняет все строки левой таблицы, подставляя NULL при отсутствии совпадений справа."
        ),
        "glossary": [
            g("unmatched", "/ʌnˈmætʃt/", "не имеющий совпадений", "Запись без соответствующей пары во второй таблице.", "LEFT JOIN preserves unmatched parent rows."),
            g("accidentally", "/ˌæk.sɪˈden.təl.i/", "случайно / непреднамеренно", "По ошибке без явного намерения.", "Adding right.flag = 1 in WHERE accidentally drops NULL rows."),
        ],
    },
    {
        "id": 15,
        "title": "Room Migrations and Schema Identity Hash",
        "tech_tag": "SQL",
        "level": "B2",
        "content": (
            "Jetpack Room computes an MD5 identity hash across every table, column, index, and foreign key at compile time and stores it in room_master_table. "
            "When opening a pre-populated SQLite asset via createFromAsset(), Room compares the embedded hash against the generated implementation and throws IllegalStateException on any mismatch. "
            "Enable exportSchema = true to track versioned JSON snapshots under version control and test every Migration(1, 2) with MigrationTestHelper."
        ),
        "explanation_ru": (
            "Room вычисляет хеш схемы (identity_hash) и хранит его в room_master_table. "
            "При использовании createFromAsset() схема предзаполненной БД должна в точности совпадать с сущностями Room, иначе возникнет ошибка при открытии."
        ),
        "glossary": [
            g("mismatch", "/ˈmɪs.mætʃ/", "несоответствие / расхождение", "Различие между ожидаемым и фактическим значением.", "Schema mismatch triggers an IllegalStateException."),
            g("snapshots", "/ˈsnæp.ʃɒts/", "снимки состояния", "Сохранённые версии схемы БД в JSON-файлах.", "Commit exported Room schema snapshots to Git."),
        ],
    },
    {
        "id": 16,
        "title": "SQL Transactions and Atomicity Guarantees",
        "tech_tag": "SQL",
        "level": "B1",
        "content": (
            "Updating a flashcard's SM-2 schedule and inserting a study_logs audit record must either succeed together or fail together. "
            "Wrapping both statements inside an explicit transaction guarantees atomicity: if the device shuts down halfway through, SQLite rolls back the partial write automatically on next launch. "
            "In Room, annotate DAO methods with @Transaction or wrap multi-DAO repository operations inside database.withTransaction { }."
        ),
        "explanation_ru": (
            "Обновление параметров SM-2 и запись события в study_logs должны выполняться атомарно в рамках одной транзакции. "
            "При внезапном выключении устройства SQLite автоматически откатит незавершённую запись, сохранив целостность данных."
        ),
        "glossary": [
            g("atomicity", "/ˌæt.əˈmɪs.ə.ti/", "атомарность", "Принцип «всё или ничего» для группы операций БД.", "Transactions guarantee atomicity across related tables."),
            g("partial", "/ˈpɑːr.ʃəl/", "частичный / незавершённый", "Выполненный только наполовину.", "SQLite rolls back partial writes after a sudden crash."),
        ],
    },

    # ==================== REST API (4) ====================
    {
        "id": 17,
        "title": "Designing Idempotent REST Endpoints",
        "tech_tag": "REST API",
        "level": "B2",
        "content": (
            "Mobile networks frequently drop connections right after a server commits a request but before the response reaches the client. "
            "HTTP GET, PUT, and DELETE are defined as idempotent, meaning repeating the exact same request produces the same server state. "
            "Because POST creates new resources and is not naturally idempotent, payment and checkout APIs accept a client-generated Idempotency-Key header. "
            "The server caches the first outcome under that key so automatic retries return the original receipt without duplicating charges."
        ),
        "explanation_ru": (
            "В мобильных сетях ответ может потеряться уже после обработки запроса на сервере. "
            "GET, PUT и DELETE идемпотентны по стандарту, а для POST-операций оплаты применяют заголовок Idempotency-Key, предотвращающий двойное списание при повторе."
        ),
        "glossary": [
            g("idempotent", "/ˌaɪ.demˈpoʊ.tənt/", "идемпотентный", "Безопасный для многократного повторения без дублирования эффекта.", "PUT /users/42 is idempotent by specification."),
            g("outcome", "/ˈaʊt.kʌm/", "результат / исход", "Итоговый ответ операции.", "Cache the initial outcome for 24 hours."),
            g("duplicating", "/ˈdjuː.plɪ.keɪ.tɪŋ/", "дублируя", "Создавая повторную нежелательную копию.", "Idempotency keys stop retries from duplicating orders."),
        ],
    },
    {
        "id": 18,
        "title": "Cursor vs Offset Pagination in Large APIs",
        "tech_tag": "REST API",
        "level": "B2",
        "content": (
            "Offset pagination (?page=500&limit=20) is simple to implement but suffers from two serious flaws at scale. "
            "First, the database still scans and discards 10,000 preceding rows on every deep page. "
            "Second, inserting or deleting records mid-scroll shifts page boundaries and causes duplicate or skipped items on the client. "
            "Cursor-based (keyset) pagination anchors each request to an opaque sorted token such as ?after=eyJpZCI6MTAwfQ, delivering constant index lookup speed and stable feeds."
        ),
        "explanation_ru": (
            "Пагинация через OFFSET замедляется на глубоких страницах и приводит к дубликатам при вставке новых строк. "
            "Курсорная пагинация (keyset pagination) опирается на токен последней записи, обеспечивая стабильную ленту и мгновенный поиск по индексу."
        ),
        "glossary": [
            g("flaws", "/flɔːz/", "недостатки / изъяны", "Архитектурные слабые места подхода.", "Offset pagination exposes performance flaws on huge tables."),
            g("discards", "/dɪsˈkɑːrdz/", "отбрасывает", "Пропускает прочитанные строки без возврата клиенту.", "Large OFFSET clauses scan and discard thousands of rows."),
            g("opaque", "/oʊˈpeɪk/", "непрозрачный (закодированный)", "Токен, внутренний формат которого клиент не должен парсить.", "Treat the next_cursor string as an opaque token."),
        ],
    },
    {
        "id": 19,
        "title": "API Versioning and Graceful Deprecation",
        "tech_tag": "REST API",
        "level": "B1",
        "content": (
            "Mobile clients remain installed on user devices for months after a backend deployment, making sudden breaking changes unacceptable. "
            "Adopt additive backward-compatible evolution whenever possible: add new optional JSON fields instead of renaming or removing existing ones. "
            "When a major contract change is unavoidable, introduce a new version path (/v2/words) and attach Sunset and Deprecation HTTP headers to /v1 responses. "
            "Monitor legacy traffic metrics before decommissioning the old endpoint."
        ),
        "explanation_ru": (
            "Мобильные клиенты обновляются постепенно, поэтому ломать существующий контракт API нельзя. "
            "Добавляйте новые опциональные поля обратно совместимо, а при необходимости новой версии (/v2) предупреждайте клиентов заголовками Deprecation и Sunset."
        ),
        "glossary": [
            g("additive", "/ˈæd.ɪ.tɪv/", "аддитивный (только добавляющий)", "Расширяющий контракт без удаления старых полей.", "Additive JSON fields stay backward-compatible."),
            g("unavoidable", "/ˌʌn.əˈvɔɪ.də.bəl/", "неизбежный", "Которого невозможно избежать.", "Bump the major version only when breaking changes are unavoidable."),
            g("decommissioning", "/ˌdiː.kəˈmɪʃ.ən.ɪŋ/", "вывод из эксплуатации", "Окончательное отключение устаревшего сервиса.", "Notify integrators 90 days before decommissioning v1."),
        ],
    },
    {
        "id": 20,
        "title": "Standardized Error Payloads (RFC 9457 Problem Details)",
        "tech_tag": "REST API",
        "level": "B2",
        "content": (
            "Returning inconsistent error strings forces frontend and mobile developers to write fragile parsing logic. "
            "RFC 9457 defines the application/problem+json media type with predictable fields: type, title, status, detail, and instance. "
            "Include machine-readable validation error codes so client UIs can localize messages cleanly into Russian or English without parsing server prose. "
            "Never leak raw SQL stack traces in production error payloads."
        ),
        "explanation_ru": (
            "Стандарт RFC 9457 (Problem Details) задаёт единый формат ошибок API (application/problem+json) с полями type, title, status и detail. "
            "Машиночитаемые коды позволяют локализовать ошибки на клиенте без парсинга английского текста и без утечки стектрейсов БД."
        ),
        "glossary": [
            g("fragile", "/ˈfrædʒ.aɪl/", "хрупкий (легко ломающийся)", "Зависимый от мелких изменений формата.", "Regex parsing of English error strings is fragile."),
            g("prose", "/proʊz/", "обычный неструктурированный текст", "Человекочитаемые фразы вместо кодов.", "Avoid branching client logic on server error prose."),
            g("leak", "/liːk/", "допускать утечку", "Случайно раскрывать внутренние детали системы.", "Never leak internal database schemas in API errors."),
        ],
    },

    # ==================== HTTP (3) ====================
    {
        "id": 21,
        "title": "Essential HTTP Status Codes Every Developer Must Know",
        "tech_tag": "HTTP",
        "level": "A2",
        "content": (
            "HTTP status codes group server outcomes into five clear classes: 1xx informational, 2xx success, 3xx redirection, 4xx client error, and 5xx server error. "
            "Return 201 Created with a Location header when creating a new entity, and 204 No Content after successful deletions. "
            "Distinguish 401 Unauthorized (missing or expired authentication credentials) from 403 Forbidden (authenticated user lacks permission). "
            "Use 429 Too Many Requests for rate limiting and 503 Service Unavailable during temporary overload or maintenance."
        ),
        "explanation_ru": (
            "Запомните ключевую разницу: 401 Unauthorized — пользователь не аутентифицирован (нет или истёк токен), 403 Forbidden — аутентифицирован, но прав недостаточно. "
            "201 означает создание ресурса, 204 — успех без тела ответа, 429 — превышен лимит запросов, 503 — временная недоступность."
        ),
        "glossary": [
            g("distinguish", "/dɪˈstɪŋ.ɡwɪʃ/", "различать", "Чётко отделять одно понятие от другого.", "Always distinguish 401 authentication from 403 authorization."),
            g("maintenance", "/ˈmeɪn.tən.əns/", "техническое обслуживание", "Плановые работы по обновлению сервера.", "Return 503 with Retry-After during maintenance."),
        ],
    },
    {
        "id": 22,
        "title": "HTTP Caching with ETag and Cache-Control",
        "tech_tag": "HTTP",
        "level": "B2",
        "content": (
            "Effective HTTP caching drastically lowers latency and cloud egress costs. "
            "The Cache-Control header directs browsers and CDNs how long a response stays fresh (e.g., max-age=3600, stale-while-revalidate=60). "
            "For dynamic resources, attach a strong ETag fingerprint; subsequent client requests send If-None-Match so the server can reply with a tiny 304 Not Modified header instead of resending megabytes of JSON. "
            "Mark user-private responses with Cache-Control: private, no-store so shared proxies never cache sensitive profiles."
        ),
        "explanation_ru": (
            "Заголовок Cache-Control задаёт время жизни кэша, а ETag + If-None-Match позволяют серверу отвечать лёгким 304 Not Modified без повторной отправки тела. "
            "Для приватных данных обязательно указывайте Cache-Control: private, no-store."
        ),
        "glossary": [
            g("egress", "/ˈiː.ɡres/", "исходящий сетевой трафик", "Трафик, выходящий из облачной инфраструктуры к клиентам.", "Caching reduces cloud egress billing dramatically."),
            g("fingerprint", "/ˈfɪŋ.ɡər.prɪnt/", "отпечаток / контрольный хеш", "Уникальный идентификатор конкретной версии ресурса.", "An ETag acts as a content fingerprint."),
        ],
    },
    {
        "id": 23,
        "title": "Understanding CORS Preflight Requests",
        "tech_tag": "HTTP",
        "level": "B1",
        "content": (
            "Cross-Origin Resource Sharing (CORS) is a browser-enforced security policy restricting web apps from calling APIs on a different scheme, domain, or port. "
            "Before sending requests with custom headers like Authorization or JSON Content-Type, the browser automatically fires an OPTIONS preflight request. "
            "The server must respond with matching Access-Control-Allow-Origin, Access-Control-Allow-Methods, and Access-Control-Allow-Headers values. "
            "Note that CORS is enforced by browsers only; tools like curl and native Android clients are unaffected."
        ),
        "explanation_ru": (
            "CORS — механизм безопасности браузера при кросс-доменных запросах. "
            "Для нестандартных заголовков (например, Authorization) браузер сначала шлёт предварительный запрос OPTIONS (preflight), проверяя разрешения сервера."
        ),
        "glossary": [
            g("preflight", "/ˈpriː.flaɪt/", "предварительный запрос (OPTIONS)", "Проверочный запрос браузера перед отправкой основного кросс-доменного запроса.", "Allow OPTIONS preflight routes without auth middleware."),
            g("unaffected", "/ˌʌn.əˈfek.tɪd/", "не затронутый", "На которого данное ограничение не распространяется.", "Native mobile clients and curl are unaffected by CORS."),
        ],
    },

    # ==================== NETWORKING (3) ====================
    {
        "id": 24,
        "title": "TCP Three-Way Handshake and Connection Teardown",
        "tech_tag": "Networking",
        "level": "B1",
        "content": (
            "Before reliable byte streaming begins over TCP, client and server synchronize sequence numbers via a three-way handshake: SYN, SYN-ACK, and ACK. "
            "TCP guarantees ordered delivery and retransmits lost packets automatically using congestion control algorithms. "
            "Each new connection costs one full round-trip time (RTT) before TLS negotiation even starts, which is why persistent connection pools and HTTP/2 multiplexing improve mobile responsiveness dramatically."
        ),
        "explanation_ru": (
            "Установка TCP-соединения проходит через тройное рукопожатие (SYN → SYN-ACK → ACK), занимая один полный RTT ещё до начала TLS. "
            "Поэтому переиспользование открытых сокетов (keep-alive / connection pooling) критично для быстрого отклика."
        ),
        "glossary": [
            g("teardown", "/ˈteər.daʊn/", "закрытие / разрыв соединения", "Процедура корректного завершения сетевой сессии (FIN/ACK).", "Graceful TCP teardown avoids half-open sockets."),
            g("congestion", "/kənˈdʒes.tʃən/", "перегрузка сети", "Затор на маршрутизаторах, вызывающий потерю пакетов.", "TCP congestion control throttles transmission speed."),
        ],
    },
    {
        "id": 25,
        "title": "TLS 1.3 Encryption and Certificate Validation",
        "tech_tag": "Networking",
        "level": "B2",
        "content": (
            "Transport Layer Security (TLS 1.3) secures data in transit with forward secrecy and eliminates obsolete cipher suites vulnerable to downgrade attacks. "
            "During the handshake, the client verifies the server's X.509 certificate chain against trusted Certificate Authorities (CAs) and checks hostname alignment. "
            "Disabling SSL verification in debug HTTP clients is a frequent source of accidental production vulnerabilities—always isolate custom trust managers strictly to debug builds."
        ),
        "explanation_ru": (
            "TLS 1.3 защищает трафик с помощью совершенной прямой секретности (forward secrecy) и проверяет цепочку сертификатов X.509. "
            "Никогда не отключайте проверку SSL-сертификатов в релизных сборках приложения."
        ),
        "glossary": [
            g("obsolete", "/ˌɒb.səˈliːt/", "устаревший / вышедший из употребления", "Старый алгоритм, признанный небезопасным.", "TLS 1.3 removed obsolete SHA-1 and RC4 ciphers."),
            g("downgrade", "/ˈdaʊn.ɡreɪd/", "понижение версии (атака)", "Принуждение клиента перейти на слабый протокол.", "Strict TLS config blocks protocol downgrade attacks."),
        ],
    },
    {
        "id": 26,
        "title": "DNS Records and TTL Caching Behavior",
        "tech_tag": "Networking",
        "level": "B1",
        "content": (
            "DNS translates human-friendly hostnames into IPv4 (A records) and IPv6 (AAAA records) addresses, while CNAME records alias one domain name to another. "
            "Every record specifies a Time To Live (TTL) in seconds instructing recursive resolvers how long to cache the answer. "
            "Before migrating servers to a new IP address, lower the DNS TTL to 60 seconds a day in advance so traffic shifts promptly without stale cache blackouts."
        ),
        "explanation_ru": (
            "A-записи DNS связывают домен с IPv4, AAAA — с IPv6, а CNAME задаёт псевдоним. "
            "Перед переездом на новый сервер заранее снизьте TTL до 60 секунд, чтобы клиенты не обращались к устаревшему IP."
        ),
        "glossary": [
            g("recursive", "/rɪˈkɜːr.sɪv/", "рекурсивный", "DNS-сервер, выполняющий полную цепочку поиска адреса.", "ISP recursive resolvers honor record TTL values."),
            g("promptly", "/ˈprɒmpt.li/", "оперативно / без задержек", "Быстро и своевременно.", "Lowering TTL ensures clients switch over promptly."),
        ],
    },

    # ==================== SOFTWARE ENGINEERING (3) ====================
    {
        "id": 27,
        "title": "Clean Architecture and the Dependency Rule",
        "tech_tag": "Software Engineering",
        "level": "B2",
        "content": (
            "Clean Architecture organizes software into concentric layers where business rules sit at the center, completely isolated from UI frameworks and databases. "
            "The Dependency Rule mandates that source-code dependencies point strictly inward: Presentation and Data may import Domain, but Domain must never import Android, Room, or Compose. "
            "By placing repository interfaces inside Domain and implementations inside Data (Dependency Inversion), core algorithms like SM-2 execute in pure JVM unit tests in milliseconds."
        ),
        "explanation_ru": (
            "В Clean Architecture зависимости направлены строго внутрь к доменному слою: Presentation и Data зависят от Domain, а Domain не зависит от Android SDK, Room или Compose. "
            "Благодаря инверсии зависимостей вся бизнес-логика и алгоритм SM-2 покрываются мгновенными JVM-тестами."
        ),
        "glossary": [
            g("concentric", "/kənˈsen.trɪk/", "концентрические (вложенные)", "Слои, организованные вокруг общего центра.", "Clean Architecture uses concentric dependency rings."),
            g("mandates", "/ˈmæn.deɪts/", "предписывает / строго требует", "Устанавливает обязательное архитектурное правило.", "The Dependency Rule mandates inward-only imports."),
            g("inversion", "/ɪnˈvɜːr.ʒən/", "инверсия (зависимостей)", "Принцип SOLID: модули зависят от абстракций, а не реализаций.", "Dependency inversion decouples use cases from SQLite."),
        ],
    },
    {
        "id": 28,
        "title": "SOLID Principles in Practical Kotlin Code",
        "tech_tag": "Software Engineering",
        "level": "B1",
        "content": (
            "SOLID summarizes five object-oriented design heuristics that curb codebase rigidity: Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion. "
            "Single Responsibility means a UseCase changes for only one domain reason rather than mixing SQL queries with Jetpack Compose formatting. "
            "Interface Segregation encourages small cohesive contracts like VocabularyRepository and SettingsRepository instead of a monolithic AppManager interface."
        ),
        "explanation_ru": (
            "Принципы SOLID снижают хрупкость кода. Принцип единственной ответственности (SRP) держит UseCases сфокусированными на одной задаче, "
            "а разделение интерфейсов (ISP) помогает разбивать репозитории на компактные контракты."
        ),
        "glossary": [
            g("curb", "/kɜːrb/", "сдерживать / ограничивать", "Уменьшать проявление нежелательного эффекта.", "Good abstractions curb tight coupling."),
            g("rigidity", "/rɪˈdʒɪd.ə.ti/", "жёсткость (сложность изменений)", "Состояние кода, когда мелкая правка ломает полпроекта.", "Monolithic classes introduce codebase rigidity."),
            g("cohesive", "/koʊˈhiː.sɪv/", "связный (логически цельный)", "Объединяющий только тесно связанные обязанности.", "Keep each repository cohesive and focused."),
        ],
    },
    {
        "id": 29,
        "title": "Writing Constructive Code Reviews",
        "tech_tag": "Software Engineering",
        "level": "B1",
        "content": (
            "Effective code reviews prioritize correctness, security, and maintainability over personal style debates. "
            "Critique the code, never the author: phrase feedback as collaborative questions such as 'Could we guard against an empty list here?' instead of commands. "
            "Prefix optional formatting suggestions with 'Nit:' so authors know immediately what blocks approval versus what is discretionary. "
            "Acknowledge clever simplifications too—positive reinforcement builds strong engineering culture."
        ),
        "explanation_ru": (
            "Качественное Code Review фокусируется на корректности, безопасности и поддерживаемости, а не на вкусовщине. "
            "Критикуйте код, а не автора, помечайте необязательные замечания префиксом «Nit:» и отмечайте удачные решения."
        ),
        "glossary": [
            g("maintainability", "/meɪnˌteɪ.nəˈbɪl.ə.ti/", "поддерживаемость", "Лёгкость понимания, исправления и развития кода.", "Clear naming improves long-term maintainability."),
            g("discretionary", "/dɪˈskreʃ.ən.ər.i/", "необязательный (на усмотрение автора)", "Оставленный на выбор разработчика.", "Mark discretionary style nits clearly."),
            g("reinforcement", "/ˌriː.ɪnˈfɔːrs.mənt/", "подкрепление / поддержка", "Позитивная обратная связь, закрепляющая хорошие практики.", "Positive reinforcement improves team morale."),
        ],
    },

    # ==================== ERROR LOGS (3) ====================
    {
        "id": 30,
        "title": "Android Error Log: SQLiteConstraintException (FOREIGN KEY failed)",
        "tech_tag": "Error Logs",
        "level": "B2",
        "content": (
            "android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)\n"
            "  at com.techenglish.app.core.database.dao.WordDao_Impl.insert(WordDao_Impl.kt:142)\n\n"
            "Diagnosis: An INSERT or UPDATE attempted to reference a category_id or language_id that does not exist in the parent table. "
            "Verify that parent seed records exist prior to inserting dependent rows, and ensure optional links like language_id pass SQL NULL rather than 0 when unassociated."
        ),
        "explanation_ru": (
            "Ошибка SQLITE_CONSTRAINT_FOREIGNKEY (код 787) возникает при попытке вставить запись с несуществующим значением внешнего ключа (category_id или language_id). "
            "Проверьте, что родительская запись существует, а для необязательного language_id передаётся NULL, а не 0."
        ),
        "glossary": [
            g("constraint", "/kənˈstreɪnt/", "ограничение БД", "Правило целостности столбца или связи.", "Foreign key constraints prevent orphaned rows."),
            g("unassociated", "/ˌʌn.əˈsoʊ.si.eɪ.tɪd/", "не привязанный", "Не имеющий связи с родительской сущностью.", "Pass NULL when a word is unassociated with a language."),
        ],
    },
    {
        "id": 31,
        "title": "JVM Error Log: NetworkOnMainThreadException & ANR",
        "tech_tag": "Error Logs",
        "level": "B1",
        "content": (
            "android.os.NetworkOnMainThreadException\n"
            "  at android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork(StrictMode.java:1668)\n"
            "  at com.example.net.SyncClient.fetch(SyncClient.kt:48)\n\n"
            "Diagnosis: Blocking I/O ran synchronously on Android's UI thread, risking an Application Not Responding (ANR) dialog after 5 seconds. "
            "Move all disk, Room, and socket operations onto Dispatchers.IO inside suspend functions so the main looper consistently renders at 60 FPS."
        ),
        "explanation_ru": (
            "NetworkOnMainThreadException возникает при попытке выполнить сетевой вызов на главном UI-потоке. "
            "Блокировка главного потока дольше 5 секунд вызывает диалог ANR (приложение не отвечает). Всегда выносите ввод-вывод в Dispatchers.IO."
        ),
        "glossary": [
            g("synchronously", "/ˈsɪŋ.krə.nəs.li/", "синхронно (с блокировкой)", "Ожидая завершения операции на текущем потоке.", "Never run disk I/O synchronously on Main."),
            g("consistently", "/kənˈsɪs.tənt.li/", "стабильно / постоянно", "Без просадок и случайных задержек.", "Keep frames rendering consistently at 60 FPS."),
        ],
    },
    {
        "id": 32,
        "title": "Build Error Log: Hilt MissingBinding & KSP Mismatch",
        "tech_tag": "Error Logs",
        "level": "C1",
        "content": (
            "error: [Dagger/MissingBinding] com.techenglish.domain.repository.VocabularyRepository cannot be provided without an @Provides-annotated method.\n"
            "  com.techenglish.domain.repository.VocabularyRepository is injected at\n"
            "      com.techenglish.app.feature.vocabulary.VocabularyViewModel(repository)\n\n"
            "Diagnosis: Dagger Hilt found a constructor dependency on an interface with no @Binds or @Provides declaration installed in SingletonComponent. "
            "Verify that your @Module class is annotated with @InstallIn(SingletonComponent::class) and that its @Binds method returns the exact domain interface type."
        ),
        "explanation_ru": (
            "Ошибка [Dagger/MissingBinding] означает, что Hilt не знает, какую реализацию подставить вместо интерфейса репозитория. "
            "Убедитесь, что в @Module с @InstallIn(SingletonComponent::class) есть метод @Binds, связывающий VocabularyRepositoryImpl с VocabularyRepository."
        ),
        "glossary": [
            g("injected", "/ɪnˈdʒek.tɪd/", "внедрённый (через DI)", "Переданный автоматически контейнером зависимостей.", "Repositories are injected into ViewModels by Hilt."),
            g("annotated", "/ˈæn.ə.teɪ.tɪd/", "аннотированный", "Помеченный метаданными компилятора (@Binds, @Provides).", "Ensure the module is annotated with @InstallIn."),
        ],
    },
]

from seed_data.docs_v3_expansion import DEFAULT_TAG_SOURCES, DOCS_V3_EXPANSION

for _art in DOC_ARTICLES:
    if not _art.get("source_url") or not _art.get("license"):
        _default_url, _default_lic = DEFAULT_TAG_SOURCES.get(
            _art["tech_tag"],
            ("https://developer.android.com/docs", "Apache-2.0 / CC-BY-4.0"),
        )
        _art.setdefault("source_url", _default_url)
        _art.setdefault("license", _default_lic)

DOC_ARTICLES.extend(DOCS_V3_EXPANSION)

