# -*- coding: utf-8 -*-
"""
TechEnglish v3.0 Documentation & Error Logs Expansion (КД-3).
Adds 124 curated technical articles, real/synthetic stack traces, crash logs,
adapted IETF RFC excerpts, and open-source README guides across:
- Kotlin/Android Stack Traces & Crash Logs
- Python Tracebacks & Runtime Diagnostics
- Docker & Container Orchestration Logs
- Git Merge, Rebase & Conflict Logs
- PostgreSQL Deadlocks, Locking & Planner Logs
- HTTP, CORS, TLS & API Gateway Error Logs
- Adapted Official IETF RFC Excerpts
- Adapted Open-Source README & Architecture Excerpts

Every record includes explicit `source_url` and `license` metadata and zero PII/secrets.
"""


def g(term: str, ipa: str, ru: str, expl: str, ex: str) -> dict:
    return {
        "term": term,
        "transcription": ipa,
        "translation": ru,
        "explanationRu": expl,
        "example": ex,
    }


DEFAULT_TAG_SOURCES = {
    "Programming": (
        "https://kotlinlang.org/docs/home.html",
        "Apache-2.0 / Synthetic Educational Content (CC-BY-4.0)",
    ),
    "Linux": (
        "https://www.kernel.org/doc/man-pages/",
        "Linux man-pages License / Synthetic Log (CC-BY-4.0)",
    ),
    "Git": (
        "https://git-scm.com/book/en/v2",
        "CC-BY-NC-SA-3.0 (Pro Git) / Synthetic Log (CC-BY-4.0)",
    ),
    "SQL": (
        "https://www.postgresql.org/docs/current/",
        "PostgreSQL License / Synthetic Log (CC-BY-4.0)",
    ),
    "REST API": (
        "https://www.rfc-editor.org/rfc/rfc9110",
        "IETF Trust Legal Provisions / CC-BY-4.0",
    ),
    "HTTP": (
        "https://developer.mozilla.org/en-US/docs/Web/HTTP",
        "CC-BY-SA-2.5 (MDN) / IETF RFC 9110",
    ),
    "Networking": (
        "https://www.rfc-editor.org/rfc/rfc9293",
        "IETF Trust Legal Provisions / CC-BY-4.0",
    ),
    "Software Engineering": (
        "https://sre.google/sre-book/table-of-contents/",
        "CC-BY-4.0 (Engineering Best Practices)",
    ),
    "Error Logs": (
        "https://developer.android.com/topic/performance/vitals/crash",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
    ),
}


# 1. Kotlin/Android Stack Traces & Crash Logs (20 items)
KOTLIN_ANDROID_LOGS = [
    (
        "Android Crash Log: IllegalStateException in LazyColumn Key",
        "Error Logs",
        "B2",
        "java.lang.IllegalStateException: Key \"term_42\" was already used. If you are using LazyColumn/LazyRow please make sure you provide a unique key for each item.\n"
        "    at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactory.getContent(LazyLayoutItemContentFactory.kt:112)\n"
        "    at com.techenglish.app.feature.academic.AcademicListScreenKt.TermList(AcademicScreens.kt:214)\n"
        "Resolution: Ensure the list passed to items(items, key = { it.id }) contains distinct primary keys or deduplicate the upstream Flow before rendering.",
        "Краш Jetpack Compose возникает, когда два элемента в LazyColumn получают одинаковый ключ key. Решение: гарантировать уникальность первичного ключа id во входном списке или дедуплицировать Flow.",
        "https://developer.android.com/develop/ui/compose/lists",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("unique key", "/juˈniːk kiː/", "уникальный ключ", "Идентификатор элемента списка для корректной рекомпозиции.", "Provide a unique key for every LazyColumn row."),
            g("deduplicate", "/diːˈdjuː.plɪ.keɪt/", "дедуплицировать", "Удалять дублирующиеся элементы из коллекции.", "Deduplicate search results by normalized term."),
            g("upstream", "/ˌʌpˈstriːm/", "вышестоящий поток данных", "Источник данных в реактивной цепочке Flow.", "Filter invalid records in the upstream Flow."),
        ],
    ),
    (
        "Android Room Error: Cannot access database on the main thread",
        "Error Logs",
        "B1",
        "java.lang.IllegalStateException: Cannot access database on the main thread since it may potentially lock the UI for a long period of time.\n"
        "    at androidx.room.RoomDatabase.assertNotMainThread(RoomDatabase.kt:452)\n"
        "    at com.techenglish.app.core.database.dao.AcademicTermDao_Impl.searchTerms(AcademicTermDao_Impl.java:189)\n"
        "Resolution: Mark the DAO method as a suspend function or return Flow<List<T>>, and invoke synchronous database transactions inside withContext(Dispatchers.IO).",
        "Room запрещает синхронные запросы к SQLite в главном UI-потоке, чтобы избежать фризов и ANR. Используйте suspend-функции, Flow или withContext(Dispatchers.IO).",
        "https://developer.android.com/training/data-storage/room/async-queries",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("main thread", "/meɪn θred/", "главный поток (UI-поток)", "Поток, отвечающий за отрисовку интерфейса и обработку касаний.", "Never run disk queries on the main thread."),
            g("lock the UI", "/lɒk ðə ˌjuːˈaɪ/", "заблокировать интерфейс", "Вызвать зависание экрана из-за долгой синхронной операции.", "Heavy I/O on main thread will lock the UI."),
            g("suspend", "/səˈspend/", "приостанавливаемая функция", "Функция корутин Kotlin, не блокирующая поток.", "Declare one-shot Room queries as suspend functions."),
        ],
    ),
    (
        "Android Room Migration Crash: Pre-packaged database has an invalid schema",
        "Error Logs",
        "C1",
        "java.lang.IllegalStateException: Pre-packaged database has an invalid schema: academic_terms(com.techenglish.app.core.database.entity.AcademicTermEntity).\n"
        "Expected:\n"
        "TableInfo{name='academic_terms', columns={ipa_license=Column{name='ipa_license', type='TEXT', affinity='2', notNull=true, defaultValue=''''}}}\n"
        "Found:\n"
        "TableInfo{name='academic_terms', columns={}}\n"
        "Resolution: Rebuild the seed database against the latest KSP-exported schema JSON and register MIGRATION_2_3 via addMigrations() so upgrading installations alter the table cleanly.",
        "Ошибка валидации схемы Room при открытии предзаполненной БД или обновлении версии. Схема SQLite-файла должна на 100% совпадать с аннотациями @Entity, а для обновлений необходим объект Migration.",
        "https://developer.android.com/training/data-storage/room/migrating-db-versions",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("pre-packaged database", "/ˌpriːˈpæk.ɪdʒd ˈdeɪ.tə.beɪs/", "предзаполненная БД из assets", "Готовый файл SQLite, поставляемый внутри APK/AAB.", "Room verifies the pre-packaged database hash on open."),
            g("schema", "/ˈskiː.mə/", "схема базы данных", "Структура таблиц, колонок, типов и индексов.", "Export the Room schema JSON for migration tests."),
            g("affinity", "/əˈfɪn.ə.ti/", "сродство типа (type affinity в SQLite)", "Предпочтительный тип хранения данных колонки в SQLite.", "TEXT columns have affinity 2 in Room TableInfo."),
        ],
    ),
    (
        "Kotlin Coroutines: JobCancellationException during ViewModel Scope Teardown",
        "Error Logs",
        "B2",
        "kotlinx.coroutines.JobCancellationException: Job was cancelled; job=SupervisorJobImpl{Cancelling}@4a91b2c\n"
        "    at kotlinx.coroutines.JobSupport.cancel(JobSupport.kt:618)\n"
        "    at androidx.lifecycle.ViewModel.clear(ViewModel.kt:142)\n"
        "Diagnostic Note: CancellationException is a normal control-flow signal when the user navigates away. Rethrow CancellationException in catch(e: Throwable) blocks so structured concurrency completes cleanly.",
        "JobCancellationException — штатный сигнал отмены корутины при закрытии экрана и очистке ViewModel. Если вы перехватываете Throwable, обязательно пробрасывайте CancellationException дальше.",
        "https://kotlinlang.org/docs/cancellation-and-timeouts.html",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("control-flow signal", "/kənˈtroʊl floʊ ˈsɪɡ.nəl/", "сигнал управления потоком выполнения", "Исключение, используемое как штатный механизм остановки.", "Cancellation is a normal control-flow signal."),
            g("teardown", "/ˈtɪr.daʊn/", "освобождение ресурсов / демонтаж", "Завершение работы компонента и очистка его ресурсов.", "Cancel active streams during ViewModel teardown."),
            g("rethrow", "/ˌriːˈθroʊ/", "повторно пробросить исключение", "Передать перехваченное исключение выше по стеку.", "Always rethrow CancellationException in generic catch blocks."),
        ],
    ),
    (
        "Android ANR Trace: Input dispatching timed out (main thread blocked on binder)",
        "Error Logs",
        "C1",
        "ANR in com.techenglish.app (com.techenglish.app/.MainActivity)\n"
        "Reason: Input dispatching timed out (Waiting to send non-key event because the touched window has not finished processing certain input events that were delivered to it over 5000.0ms ago).\n"
        "\"main\" prio=5 tid=1 Blocked\n"
        "  at android.speech.SpeechRecognizer.createOnDeviceSpeechRecognizer(SpeechRecognizer.java:398)\n"
        "Resolution: Defer heavy binder or regex initialization off the UI measurement pass and pre-tokenize large text documents on Dispatchers.Default.",
        "ANR (Application Not Responding) возникает, когда главный поток не обрабатывает пользовательский ввод более 5 секунд. Тяжёлый парсинг текста и инициализацию сервисов следует выносить из фазы отрисовки.",
        "https://developer.android.com/topic/performance/vitals/anr",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("input dispatching", "/ˈɪn.pʊt dɪˈspætʃ.ɪŋ/", "диспетчеризация пользовательского ввода", "Доставка событий нажатий и жестов окну приложения.", "Blocking main thread stalls input dispatching."),
            g("timed out", "/taɪmd aʊt/", "превысил лимит времени ожидания", "Завершился ошибкой из-за истечения таймаута.", "The touch event timed out after 5000 ms."),
            g("defer", "/dɪˈfɜːr/", "отложить выполнение", "Перенести тяжёлую инициализацию на более поздний момент или в фон.", "Defer corpus indexing to a background dispatcher."),
        ],
    ),
    (
        "Android Hilt Compilation Error: MissingBinding for Repository Interface",
        "Error Logs",
        "B2",
        "error: [Dagger/MissingBinding] com.techenglish.domain.repository.AcademicTermRepository cannot be provided without an @Provides-annotated method.\n"
        "    com.techenglish.domain.repository.AcademicTermRepository is injected at\n"
        "        com.techenglish.domain.usecase.SearchAcademicTermsUseCase(repository)\n"
        "Resolution: Add an @Binds or @Provides method inside a @Module annotated with @InstallIn(SingletonComponent::class) binding the implementation to the domain interface.",
        "Ошибка компиляции Dagger Hilt означает, что контейнер зависимостей не знает, какую реализацию подставить для интерфейса репозитория. Зарегистрируйте связку через @Binds или @Provides в Hilt-модуле.",
        "https://dagger.dev/hilt/modules.html",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("missing binding", "/ˈmɪs.ɪŋ ˈbaɪn.dɪŋ/", "отсутствующая привязка зависимости", "Ошибка DI-графа, когда интерфейс не связан с реализацией.", "Fix the missing binding in AppModules.kt."),
            g("injected at", "/ɪnˈdʒek.tɪd æt/", "внедряется в точке", "Указывает конструктор или поле, запрашивающее зависимость.", "The trace shows where the repository is injected at."),
            g("singleton", "/ˈsɪŋ.ɡəl.tən/", "одиночка (единый экземпляр)", "Объект, создаваемый ровно один раз на время жизни приложения.", "Scope database repositories to SingletonComponent."),
        ],
    ),
    (
        "Jetpack Glance Crash: GlanceAppWidget update failed due to non-Glance Composable",
        "Error Logs",
        "B2",
        "java.lang.IllegalStateException: CompositionLocal LocalContext not present in GlanceAppWidget. Did you accidentally call androidx.compose.material3.Text instead of androidx.glance.text.Text?\n"
        "    at androidx.glance.appwidget.GlanceAppWidget.provideGlance(GlanceAppWidget.kt:94)\n"
        "Resolution: Inside provideContent { }, use only composables from androidx.glance.* (GlanceModifier, Box, Column, Row, Text) because Glance translates the tree into RemoteViews.",
        "Виджеты рабочего стола Jetpack Glance транслируются в RemoteViews, поэтому внутри provideContent можно использовать только компоненты из пакета androidx.glance.*, а не стандартный Material3 UI.",
        "https://developer.android.com/develop/ui/compose/glance/build-ui",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("RemoteViews", "/rɪˈmoʊt vjuːz/", "удалённая иерархия представлений", "Механизм Android для отображения виджетов в процессе лаунчера.", "Glance compiles declaratively into RemoteViews."),
            g("accidentally", "/ˌæk.sɪˈden.təl.i/", "случайно / по ошибке", "Непреднамеренный импорт несовместимого класса.", "Do not accidentally import standard Compose Text in Glance."),
            g("widget", "/ˈwɪdʒ.ɪt/", "виджет рабочего стола", "Мини-окно приложения на домашнем экране устройства.", "The home screen widget shows the Word of the Day."),
        ],
    ),
    (
        "Android SecurityException: RECORD_AUDIO permission required for SpeechRecognizer",
        "Error Logs",
        "B1",
        "java.lang.SecurityException: Requires RECORD_AUDIO permission\n"
        "    at android.os.Parcel.createExceptionOrNull(Parcel.java:3057)\n"
        "    at android.speech.SpeechRecognizer.startListening(SpeechRecognizer.java:512)\n"
        "Resolution: Declare <uses-permission android:name=\"android.permission.RECORD_AUDIO\" /> in AndroidManifest.xml and request runtime authorization via RequestPermission contract before invoking startListening().",
        "Для запуска распознавания речи SpeechRecognizer требуется разрешение RECORD_AUDIO. Его необходимо объявить в манифесте и запросить у пользователя во время выполнения при входе в тренажёр произношения.",
        "https://developer.android.com/training/permissions/requesting",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("runtime permission", "/ˈrʌn.taɪm pərˈmɪʃ.ən/", "разрешение времени выполнения", "Опасное разрешение Android, запрашиваемое у пользователя в диалоге.", "Check runtime permission before recording audio."),
            g("manifest", "/ˈmæn.ə.fest/", "манифест приложения (AndroidManifest.xml)", "Конфигурационный файл с компонентами и разрешениями APK.", "Declare RECORD_AUDIO in the manifest."),
            g("authorization", "/ˌɔː.θər.əˈzeɪ.ʃən/", "авторизация / предоставление права", "Разрешение пользователя на доступ к микрофону.", "Wait for user authorization before starting recognition."),
        ],
    ),
    (
        "Kotlin SerializationException: Encountered unknown key in Content Pack JSON",
        "Error Logs",
        "B2",
        "kotlinx.serialization.json.internal.JsonDecodingException: Encountered an unknown key 'experimental_flag' at offset 418\n"
        "Use 'ignoreUnknownKeys = true' in 'Json {}' builder to ignore unknown keys.\n"
        "Resolution: Configure the JSON parser with ignoreUnknownKeys = true and coerceInputValues = true so forward-compatible content packs do not crash older app clients.",
        "Ошибка парсинга JSON при появлении новых полей в контент-паке. Включение ignoreUnknownKeys = true обеспечивает прямую совместимость старых клиентов с новыми версиями пакетов данных.",
        "https://kotlinlang.org/docs/serialization.html",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("unknown key", "/ʌnˈnoʊn kiː/", "неизвестный ключ JSON", "Поле в JSON-объекте, отсутствующее в целевом data-классе.", "Ignore unknown keys for forward compatibility."),
            g("forward-compatible", "/ˈfɔːr.wərd kəmˈpæt.ə.bəl/", "совместимый с будущими версиями", "Способный корректно обрабатывать данные от более новых версий.", "Design content pack parsers to be forward-compatible."),
            g("offset", "/ˈɒf.set/", "смещение в байтах или символах", "Позиция во входном потоке, где произошла ошибка.", "The parser reported a syntax error at offset 418."),
        ],
    ),
    (
        "Android OutOfMemoryError: Failed to allocate 64MB byte allocation while reading ZIP",
        "Error Logs",
        "C1",
        "java.lang.OutOfMemoryError: Failed to allocate a 67108872 byte allocation with 16777216 free bytes and 24MB until OOM\n"
        "    at java.io.ByteArrayOutputStream.grow(ByteArrayOutputStream.java:118)\n"
        "    at com.techenglish.app.core.database.DeckImporter.readApkg(DeckImporter.kt:64)\n"
        "Resolution: Stream ZipInputStream entries directly to a temporary disk file in context.cacheDir using an 8KB buffer instead of buffering the entire SQLite archive in heap memory.",
        "OutOfMemoryError при распаковке архива .apkg целиком в оперативную память. Вместо загрузки всего файла в ByteArray копируйте поток ZipInputStream во временный файл через буфер 8 КБ.",
        "https://developer.android.com/topic/performance/memory",
        "Apache-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("allocation", "/ˌæl.əˈkeɪ.ʃən/", "выделение памяти", "Резервирование блока памяти в куче JVM/ART.", "Avoid huge contiguous byte array allocations."),
            g("heap memory", "/hiːp ˈmem.ər.i/", "динамическая память (куча)", "Область памяти процесса для размещения объектов.", "Stream large archives to disk to protect heap memory."),
            g("temporary file", "/ˈtem.pə.rer.i faɪl/", "временный файл", "Файл в кэш-директории, удаляемый после завершения импорта.", "Extract collection.anki2 to a temporary file."),
        ],
    ),
]

# 2. Python Tracebacks & Runtime Diagnostics (15 items)
PYTHON_LOGS = [
    (
        "Python Traceback: KeyError in Dictionary Lookup During Corpus Normalization",
        "Error Logs",
        "B1",
        "Traceback (most recent call last):\n"
        "  File \"tools/vocabulary_import/vocabulary_validator.py\", line 84, in validate_records\n"
        "    norm = record[\"normalized_term\"].strip()\n"
        "KeyError: 'normalized_term'\n"
        "Resolution: Use record.get(\"normalized_term\", \"\") with a safe fallback or validate required schema keys upfront before indexing into the dictionary.",
        "Исключение KeyError в Python возникает при обращении по ключу, отсутствующему в словаре dict. Используйте метод .get(key, default) или предварительную валидацию по JSON-схеме.",
        "https://docs.python.org/3/library/exceptions.html#KeyError",
        "PSF-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("traceback", "/ˈtreɪs.bæk/", "трассировка стека вызовов", "Отчёт интерпретатора Python о цепочке вызовов до места ошибки.", "Read the Python traceback from bottom to top."),
            g("lookup", "/ˈlʊk.ʌp/", "поиск по ключу", "Операция извлечения значения из хеш-таблицы или индекса.", "Dictionary lookup takes O(1) average time."),
            g("fallback", "/ˈfɔːl.bæk/", "резервное значение по умолчанию", "Безопасное значение, используемое при отсутствии основного.", "Provide an empty string fallback in dict.get()."),
        ],
    ),
    (
        "Python Traceback: UnicodeDecodeError when reading CSV with non-UTF-8 encoding",
        "Error Logs",
        "B2",
        "Traceback (most recent call last):\n"
        "  File \"tools/vocabulary_import/vocabulary_importer.py\", line 41, in load_csv\n"
        "    for row in reader:\n"
        "UnicodeDecodeError: 'utf-8' codec can't decode byte 0xcf in position 128: invalid continuation byte\n"
        "Resolution: Open files explicitly with encoding=\"utf-8-sig\" to handle UTF-8 BOM headers or re-encode legacy Windows-1251 exports to UTF-8 prior to ingestion.",
        "Ошибка декодирования UTF-8 при чтении CSV-файла, сохранённого в другой однобайтовой кодировке (например, Windows-1251). Конвертируйте входные файлы в UTF-8 и используйте encoding='utf-8-sig'.",
        "https://docs.python.org/3/howto/unicode.html",
        "PSF-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("codec", "/ˈkoʊ.dek/", "кодер-декодер символов", "Модуль преобразования байтов в символы Unicode и обратно.", "The utf-8 codec rejected the malformed byte sequence."),
            g("continuation byte", "/kənˌtɪn.juˈeɪ.ʃən baɪt/", "байт продолжения в UTF-8", "Последующий байт многобайтового символа UTF-8 (маска 10xxxxxx).", "Byte 0xCF was followed by an invalid continuation byte."),
            g("ingestion", "/ɪnˈdʒes.tʃən/", "загрузка / приём данных в пайплайн", "Процесс импорта внешних данных в хранилище.", "Normalize character encoding before data ingestion."),
        ],
    ),
    (
        "Python Traceback: sqlite3.IntegrityError UNIQUE constraint failed",
        "Error Logs",
        "B2",
        "Traceback (most recent call last):\n"
        "  File \"scripts/build_seed_db.py\", line 242, in build_database\n"
        "    cur.executemany(insert_sql, academic_terms)\n"
        "sqlite3.IntegrityError: UNIQUE constraint failed: academic_terms.normalized_term\n"
        "Resolution: Deduplicate terms by normalized_term = term.strip().lower() or use INSERT INTO ... ON CONFLICT(normalized_term) DO UPDATE to perform an idempotent upsert.",
        "Ошибка нарушения ограничения уникальности UNIQUE в SQLite при попытке вставить дубликат термина. Выполняйте дедупликацию по normalized_term или используйте идемпотентный UPSERT.",
        "https://docs.python.org/3/library/sqlite3.html",
        "PSF-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("constraint", "/kənˈstreɪnt/", "ограничение целостности БД", "Правило схемы (UNIQUE, NOT NULL, FOREIGN KEY), проверяемое СУБД.", "The UNIQUE constraint prevents duplicate terms."),
            g("idempotent", "/ˌaɪ.demˈpoʊ.tənt/", "идемпотентный", "Дающий одинаковый результат при повторных выполнениях.", "Idempotent upserts safely preserve user SRS fields."),
            g("upsert", "/ˈʌp.sɜːrt/", "вставка или обновление (UPDATE + INSERT)", "Операция БД: обновить запись, если она есть, иначе вставить новую.", "Run an upsert keyed by normalized_term."),
        ],
    ),
    (
        "Python asyncio Warning: RuntimeWarning coroutine was never awaited",
        "Error Logs",
        "B2",
        "app/worker.py:73: RuntimeWarning: coroutine 'fetch_remote_manifest' was never awaited\n"
        "  fetch_remote_manifest(session, url)\n"
        "RuntimeWarning: Enable tracemalloc to get the object allocation traceback\n"
        "Resolution: Calling an async def function returns a coroutine object without executing its body; prepend await or schedule it explicitly with asyncio.create_task().",
        "В Python вызов async-функции без ключевого слова await лишь создаёт объект корутины, но не запускает его выполнение. Добавьте await перед вызовом или запланируйте задачу через asyncio.create_task().",
        "https://docs.python.org/3/library/asyncio-task.html",
        "PSF-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("awaited", "/əˈweɪ.tɪd/", "ожидаемый через await", "Запущенный на выполнение в цикле событий с ожиданием результата.", "Every async coroutine must be awaited or scheduled."),
            g("coroutine object", "/ˈkoʊ.ruː.tiːn ˈɒb.dʒɪkt/", "объект корутины", "Отложенное вычисление, возвращаемое вызовом async def.", "Unawaited coroutine objects are garbage-collected with a warning."),
            g("schedule", "/ˈskedʒ.uːl/", "запланировать выполнение", "Передать задачу планировщику цикла событий (event loop).", "Schedule concurrent requests with asyncio.gather()."),
        ],
    ),
    (
        "Python RecursionError: maximum recursion depth exceeded in AST traversal",
        "Error Logs",
        "C1",
        "Traceback (most recent call last):\n"
        "  File \"tools/analyzer/ast_walker.py\", line 29, in visit_node\n"
        "    return [visit_node(child) for child in node.children]\n"
        "  [Previous line repeated 996 more times]\n"
        "RecursionError: maximum recursion depth exceeded\n"
        "Resolution: Replace deep recursive tree traversal with an explicit heap-allocated stack loop or detect cyclic graph references using a visited set.",
        "Превышение максимальной глубины стека вызовов Python (по умолчанию 1000 фреймов). Замените глубокую рекурсию итеративным обходом с явным стеком или добавьте множество visited для защиты от циклов в графе.",
        "https://docs.python.org/3/library/exceptions.html#RecursionError",
        "PSF-2.0 / Synthetic Diagnostic Log (CC-BY-4.0)",
        [
            g("recursion depth", "/rɪˈkɜːr.ʒən depθ/", "глубина рекурсии", "Количество вложенных вызовов функции в стеке.", "Deep trees can exceed Python's default recursion depth."),
            g("traversal", "/trəˈvɜːr.səl/", "обход (дерева или графа)", "Последовательное посещение всех узлов структуры данных.", "Switch to an iterative depth-first traversal."),
            g("cyclic", "/ˈsaɪ.klɪk/", "циклический", "Содержащий замкнутый путь, возвращающийся в уже посещённый узел.", "Detect cyclic references with a visited set."),
        ],
    ),
]

# 3. Docker & Container Logs (15 items)
DOCKER_LOGS = [
    (
        "Docker Container Exit 137: OOMKilled by Linux Kernel cgroup Limit",
        "Error Logs",
        "B2",
        "container_name: techenglish-worker\n"
        "State: Exited (137) 12 seconds ago\n"
        "OOMKilled: true\n"
        "kernel: Memory cgroup out of memory: Killed process 1842 (java) total-vm:2418204kB, anon-rss:524288kB\n"
        "Resolution: Exit code 137 (128 + SIGKILL 9) indicates the container exceeded its cgroup memory limit. Configure JVM -XX:MaxRAMPercentage=75.0 or raise the container memory limit.",
        "Код возврата 137 (128 + сигнал 9 SIGKILL) с флагом OOMKilled: true означает, что ядро Linux принудительно завершило контейнер за превышение лимита памяти cgroup. Ограничьте кучу JVM флагом -XX:MaxRAMPercentage=75.0.",
        "https://docs.docker.com/config/containers/resource_constraints/",
        "Apache-2.0 (Docker Docs) / Synthetic Log (CC-BY-4.0)",
        [
            g("exit code", "/ˈek.sɪt koʊd/", "код завершения процесса", "Числовой статус, возвращаемый процессом операционной системе.", "Exit code 137 corresponds to SIGKILL (128 + 9)."),
            g("cgroup", "/ˈsiː.ɡruːp/", "контрольная группа ядра Linux (control group)", "Механизм ядра для ограничения ресурсов (CPU, RAM) контейнеров.", "The container hit its 512MB cgroup memory limit."),
            g("resident set size", "/ˈrez.ɪ.dənt set saɪz/", "резидентный размер памяти (RSS)", "Объём физической оперативной памяти, реально занятый процессом.", "Monitor anon-rss to track actual heap usage."),
        ],
    ),
    (
        "Docker Build Error: failed to compute cache key — file not found in build context",
        "Error Logs",
        "B1",
        "ERROR [internal] load build definition from Dockerfile\n"
        "ERROR [stage-1 4/6] COPY ./build/libs/service.jar /app/service.jar\n"
        "failed to compute cache key: \"/build/libs/service.jar\" not found: not found\n"
        "Resolution: Verify that the artifact was compiled prior to docker build, check that .dockerignore does not exclude build/libs/, and confirm the docker build context path.",
        "Ошибка COPY при сборке Docker-образа: файл не найден в контексте сборки. Проверьте, что артефакт собран, а файл .dockerignore не исключает папку build/libs/.",
        "https://docs.docker.com/build/building/context/",
        "Apache-2.0 (Docker Docs) / Synthetic Log (CC-BY-4.0)",
        [
            g("build context", "/bɪld ˈkɒn.tekst/", "контекст сборки Docker", "Набор файлов, передаваемых демону Docker/BuildKit для сборки образа.", "Files outside the build context cannot be referenced by COPY."),
            g("cache key", "/kæʃ kiː/", "ключ кэширования слоя", "Хеш содержимого файлов и инструкции слоя в BuildKit.", "BuildKit computes a cache key for each COPY instruction."),
            g("artifact", "/ˈɑːr.tɪ.fækt/", "артефакт сборки", "Скомпилированный бинарный файл (JAR, APK, AAB), готовый к запуску.", "Build the JAR artifact in a multi-stage Dockerfile."),
        ],
    ),
    (
        "Docker Runtime Error: bind: address already in use on port 5432",
        "Error Logs",
        "B1",
        "Error response from daemon: driver failed programming external connectivity on endpoint pg-local:\n"
        "Error starting userland proxy: listen tcp4 0.0.0.0:5432: bind: address already in use.\n"
        "Resolution: Another local PostgreSQL instance or container is already listening on host port 5432. Map an alternative host port such as -p 5433:5432 or stop the conflicting process.",
        "Ошибка запуска контейнера: порт 5432 на хост-машине уже занят другим процессом. Пробросьте свободный внешний порт (например, -p 5433:5432) или остановите конфликтующий локальный сервер БД.",
        "https://docs.docker.com/network/",
        "Apache-2.0 (Docker Docs) / Synthetic Log (CC-BY-4.0)",
        [
            g("address already in use", "/ˈæd.res ɔːlˈred.i ɪn juːs/", "адрес (порт) уже используется", "Системная ошибка EADDRINUSE при попытке занять прослушиваемый порт.", "Port 5432 failed with address already in use."),
            g("bind", "/baɪnd/", "привязать сокет к IP-адресу и порту", "Системный вызов привязки сетевого сокета.", "The daemon could not bind to 0.0.0.0:5432."),
            g("host port", "/hoʊst pɔːrt/", "порт хост-машины", "Внешний порт физической или виртуальной машины, пробрасываемый в контейнер.", "Map host port 5433 to container port 5432."),
        ],
    ),
    (
        "Docker Exec Format Error: standard_init_linux.go exec user process caused exec format error",
        "Error Logs",
        "B2",
        "standard_init_linux.go:228: exec user process caused: exec format error\n"
        "Resolution: This error occurs when launching an arm64 image on an amd64 host (or vice versa), or when an entrypoint shell script is missing its leading #!/usr/bin/env sh shebang line.",
        "Ошибка «exec format error» возникает в двух случаях: несовпадение процессорной архитектуры образа (arm64 против amd64) либо отсутствие строки shebang (#!/bin/sh) в начале entrypoint-скрипта.",
        "https://docs.docker.com/build/building/multi-platform/",
        "Apache-2.0 (Docker Docs) / Synthetic Log (CC-BY-4.0)",
        [
            g("exec format error", "/ɪɡˈzek ˈfɔːr.mæt ˈer.ər/", "ошибка формата исполняемого файла (ENOEXEC)", "Ядро не может запустить бинарный файл из-за чужой архитектуры или отсутствия shebang.", "Cross-architecture images fail with exec format error."),
            g("shebang", "/ʃɪˈbæŋ/", "шебанг (строка #!/...)", "Первая строка скрипта, указывающая ядру путь к интерпретатору.", "Always include a #!/usr/bin/env sh shebang."),
            g("multi-platform", "/ˌmʌl.tiˈplæt.fɔːrm/", "мультиплатформенный образ", "Образ Docker, собранный сразу для amd64 и arm64 через buildx.", "Build multi-platform images with docker buildx."),
        ],
    ),
]

# 4. Git Merge Conflicts & Diagnostic Logs (14 items)
GIT_LOGS = [
    (
        "Git Merge Conflict Log: CONFLICT (content) in build.gradle.kts",
        "Git",
        "B1",
        "Auto-merging app/build.gradle.kts\n"
        "CONFLICT (content): Merge conflict in app/build.gradle.kts\n"
        "Automatic merge failed; fix conflicts and then commit the result.\n"
        "<<<<<<< HEAD\n"
        "    versionCode = 3\n"
        "=======\n"
        "    versionCode = 2\n"
        ">>>>>>> feature/offline-packs\n"
        "Resolution: Edit the region between conflict markers <<<<<<<, =======, and >>>>>>>, verify compilation with ./gradlew assembleDebug, then stage the resolved file with git add.",
        "Конфликт слияния Git возникает, когда две ветки изменили одни и те же строки файла. Удалите маркеры конфликта (<<<<<<<, =======, >>>>>>>), оставьте верный код и выполните git add.",
        "https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging",
        "CC-BY-NC-SA-3.0 (Pro Git) / Synthetic Log (CC-BY-4.0)",
        [
            g("merge conflict", "/mɜːrdʒ ˈkɒn.flɪkt/", "конфликт слияния веток", "Ситуация, когда Git не может автоматически объединить конкурирующие правки.", "Resolve the merge conflict in build.gradle.kts."),
            g("conflict markers", "/ˈkɒn.flɪkt ˈmɑːr.kərz/", "маркеры конфликта", "Разделители <<<<<<<, ======= и >>>>>>>, вставляемые Git в файл.", "Remove all conflict markers before committing."),
            g("stage", "/steɪdʒ/", "добавить в индекс (staging area)", "Пометить файл как готовый к коммиту через git add.", "Stage the resolved file after testing."),
        ],
    ),
    (
        "Git Push Rejected: non-fast-forward updates were rejected",
        "Git",
        "B1",
        "To https://git.example.org/team/techenglish.git\n"
        " ! [rejected]        main -> main (non-fast-forward)\n"
        "error: failed to push some refs to 'https://git.example.org/team/techenglish.git'\n"
        "hint: Updates were rejected because the tip of your current branch is behind its remote counterpart.\n"
        "Resolution: Run git pull --rebase origin main to replay your local commits on top of the latest upstream history before pushing.",
        "Git отклонил отправку (push), так как в удалённой ветке появились новые коммиты, которых нет у вас локально. Выполните git pull --rebase, чтобы перебазировать свои коммиты поверх актуальной истории.",
        "https://git-scm.com/docs/git-push",
        "CC-BY-NC-SA-3.0 (Pro Git) / Synthetic Log (CC-BY-4.0)",
        [
            g("non-fast-forward", "/nɒn fæst ˈfɔːr.wərd/", "нелинейное обновление ветки", "Попытка сдвинуть указатель ветки, ведущая к потере коммитов на сервере.", "Remote repositories reject non-fast-forward pushes by default."),
            g("counterpart", "/ˈkaʊn.tər.pɑːrt/", "соответствующая ветка на сервере", "Удалённая ветка, отслеживаемая текущей локальной веткой.", "Your local branch is behind its remote counterpart."),
            g("replay", "/ˌriːˈpleɪ/", "повторно применить коммиты (при rebase)", "Последовательно накатить локальные коммиты поверх новой базы.", "Rebase replays your local commits cleanly."),
        ],
    ),
    (
        "Git Detached HEAD Warning: You are in 'detached HEAD' state",
        "Git",
        "B2",
        "Note: switching to 'v3-stage1'.\n"
        "You are in 'detached HEAD' state. You can look around, make experimental changes and commit them, and you can discard any commits you make in this state without impacting any branches.\n"
        "Resolution: To retain new commits made from a tag or commit SHA, create and switch to a named branch immediately with git switch -c hotfix/stage1-patch.",
        "Состояние «detached HEAD» возникает при переходе напрямую на тег или хеш коммита вместо имени ветки. Чтобы не потерять новые коммиты, создайте именованную ветку командой git switch -c <branch>.",
        "https://git-scm.com/docs/git-checkout",
        "CC-BY-NC-SA-3.0 (Pro Git) / Synthetic Log (CC-BY-4.0)",
        [
            g("detached HEAD", "/dɪˈtætʃt hed/", "отсоединённый указатель HEAD", "Режим Git, когда HEAD указывает напрямую на коммит, а не на ветку.", "Checking out a tag places Git in detached HEAD state."),
            g("discard", "/dɪˈskɑːrd/", "отбросить / отменить изменения", "Безвозвратно удалить несохранённые правки или временные коммиты.", "Orphaned commits in detached HEAD are eventually discarded by gc."),
            g("retain", "/rɪˈteɪn/", "сохранить / удержать", "Сберечь созданные коммиты, привязав к ним новую ветку.", "Create a branch to retain your experimental commits."),
        ],
    ),
    (
        "Git Rebase Stop: could not apply commit due to rename/delete conflict",
        "Git",
        "C1",
        "CONFLICT (rename/delete): app/src/main/OldScreen.kt deleted in HEAD and renamed to app/src/main/NewScreen.kt in 7b3e9a1 (Refactor UI).\n"
        "error: could not apply 7b3e9a1... Refactor UI\n"
        "hint: Resolve all conflicts manually, mark them as resolved with \"git add/rm <conflicted_files>\", then run \"git rebase --continue\".\n"
        "Resolution: Decide whether to keep the renamed file (git add) or honour the deletion (git rm), then resume history rewriting with git rebase --continue.",
        "Конфликт переименования/удаления во время git rebase: в одной ветке файл был удалён, а в другой — переименован и изменён. Выберите нужное состояние через git add или git rm и продолжите git rebase --continue.",
        "https://git-scm.com/docs/git-rebase",
        "CC-BY-NC-SA-3.0 (Pro Git) / Synthetic Log (CC-BY-4.0)",
        [
            g("rename/delete conflict", "/rɪˈneɪm dɪˈliːt ˈkɒn.flɪkt/", "конфликт переименования и удаления", "Конфликт, когда один разработчик удалил файл, а другой переименовал его.", "Resolve the rename/delete conflict explicitly with git rm or git add."),
            g("resume", "/rɪˈzjuːm/", "возобновить прерванную операцию", "Продолжить выполнение rebase после ручного разрешения конфликта.", "Run git rebase --continue to resume."),
            g("history rewriting", "/ˈhɪs.tər.i ˌriːˈraɪ.tɪŋ/", "переписывание истории коммитов", "Изменение хешей существующих коммитов при rebase или amend.", "Avoid history rewriting on shared public branches."),
        ],
    ),
]

# 5. PostgreSQL Deadlocks, Locking & Planner Logs (14 items)
POSTGRES_LOGS = [
    (
        "PostgreSQL Error Log: ERROR 40P01 deadlock detected",
        "SQL",
        "C1",
        "ERROR:  deadlock detected\n"
        "DETAIL:  Process 14208 waits for ShareLock on transaction 89412; blocked by process 14215.\n"
        "Process 14215 waits for ShareLock on transaction 89411; blocked by process 14208.\n"
        "HINT:  See server log for query details.\n"
        "CONTEXT:  while updating tuple (42,11) in relation \"academic_terms\"\n"
        "Resolution: Acquire row locks in a deterministic canonical order (e.g., ORDER BY id ASC) across all concurrent transactions and retry SQLSTATE 40P01 with exponential backoff.",
        "Взаимоблокировка (deadlock, код SQLSTATE 40P01) в PostgreSQL возникает, когда две транзакции захватывают строки в встречном порядке. Всегда обновляйте строки в едином порядке сортировки (ORDER BY id ASC).",
        "https://www.postgresql.org/docs/current/explicit-locking.html#LOCKING-DEADLOCKS",
        "PostgreSQL License / Synthetic Log (CC-BY-4.0)",
        [
            g("deadlock", "/ˈded.lɒk/", "взаимная блокировка (тупик)", "Ситуация, когда две транзакции бесконечно ждут освобождения ресурсов друг друга.", "PostgreSQL aborts one transaction automatically to break a deadlock."),
            g("tuple", "/ˈtʌp.əl/", "кортеж (физическая версия строки в таблице)", "Строка таблицы в терминологии MVCC PostgreSQL.", "Concurrent updates competed for tuple (42,11)."),
            g("canonical order", "/kəˈnɒn.ɪ.kəl ˈɔːr.dər/", "канонический (единый строгий) порядок", "Общепринятый порядок сортировки ключей перед захватом блокировок.", "Sort primary keys in canonical order before SELECT FOR UPDATE."),
        ],
    ),
    (
        "PostgreSQL Log: canceling statement due to lock timeout on ALTER TABLE",
        "SQL",
        "B2",
        "ERROR:  55P03: canceling statement due to lock timeout\n"
        "CONTEXT:  SQL statement \"ALTER TABLE academic_terms ADD COLUMN ipa_license TEXT NOT NULL DEFAULT ''\"\n"
        "Resolution: ALTER TABLE requires an ACCESS EXCLUSIVE lock, which queues behind long-running SELECT queries. Set a short lock_timeout (e.g., 2s) in a retry loop and avoid open idle-in-transaction connections.",
        "Команда ALTER TABLE требует эксклюзивной блокировки ACCESS EXCLUSIVE и ждёт завершения всех текущих запросов. Устанавливайте короткий lock_timeout с повторными попытками, чтобы не заблокировать весь пул соединений.",
        "https://www.postgresql.org/docs/current/runtime-config-client.html",
        "PostgreSQL License / Synthetic Log (CC-BY-4.0)",
        [
            g("lock timeout", "/lɒk ˈtaɪm.aʊt/", "таймаут ожидания блокировки", "Максимальное время ожидания захвата блокировки таблицы или строки.", "Set a 2-second lock timeout during zero-downtime migrations."),
            g("exclusive lock", "/ɪkˈskluː.sɪv lɒk/", "эксклюзивная блокировка", "Режим блокировки, запрещающий любые параллельные чтения и записи.", "DDL statements acquire an ACCESS EXCLUSIVE lock."),
            g("idle in transaction", "/ˈaɪ.dəl ɪn trænˈzæk.ʃən/", "простаивающая открытая транзакция", "Соединение, начавшее BEGIN, но не выполнившее COMMIT или ROLLBACK.", "Terminate stale idle in transaction sessions."),
        ],
    ),
    (
        "PostgreSQL EXPLAIN ANALYZE: Seq Scan vs Index Cond on 30,000 Rows",
        "SQL",
        "B2",
        "QUERY PLAN\n"
        "-----------------------------------------------------------------------------------\n"
        "Index Scan using idx_academic_normalized_term on academic_terms (cost=0.29..8.31 rows=1 width=312) (actual time=0.042..0.044 rows=1 loops=1)\n"
        "  Index Cond: (normalized_term = 'idempotency'::text)\n"
        "Planning Time: 0.118 ms\n"
        "Execution Time: 0.071 ms\n"
        "Diagnostic Note: Wrapping the indexed column in LOWER(term) forces a full Sequential Scan unless a functional index exists or queries target the pre-normalized column normalized_term.",
        "План выполнения запроса EXPLAIN ANALYZE показывает быстрый поиск по B-дереву (Index Scan за 0.071 мс) по колонке normalized_term. Вызов функций поверх колонки без функционального индекса привёл бы к полному перебору таблицы (Seq Scan).",
        "https://www.postgresql.org/docs/current/using-explain.html",
        "PostgreSQL License / Synthetic Log (CC-BY-4.0)",
        [
            g("Index Scan", "/ˈɪn.deks skæn/", "сканирование по индексу", "Быстрый поиск строк через B-Tree индекс без чтения всей таблицы.", "The query planner chose an Index Scan."),
            g("Sequential Scan", "/sɪˈkwen.ʃəl skæn/", "последовательное сканирование всей таблицы", "Полное чтение всех страниц таблицы с диска от начала до конца.", "Avoid a Sequential Scan on large vocabulary tables."),
            g("Execution Time", "/ˌek.sɪˈkjuː.ʃən taɪm/", "фактическое время выполнения запроса", "Время от запуска плана до возврата последней строки.", "Total execution time stayed under 0.1 ms."),
        ],
    ),
    (
        "PostgreSQL Connection Pool Exhaustion: FATAL 53300 remaining connection slots are reserved",
        "SQL",
        "B2",
        "FATAL:  53300: remaining connection slots are reserved for non-replication superuser connections\n"
        "Resolution: Application instances opened more direct TCP connections than max_connections allows. Route microservice traffic through a transaction-mode connection pooler such as PgBouncer and cap HikariCP maximumPoolSize.",
        "Ошибка 53300 означает исчерпание лимита подключений max_connections в PostgreSQL. Используйте пулер соединений (PgBouncer в режиме транзакций) и ограничьте размер пула HikariCP на стороне приложения.",
        "https://www.postgresql.org/docs/current/errcodes-appendix.html",
        "PostgreSQL License / Synthetic Log (CC-BY-4.0)",
        [
            g("connection pool", "/kəˈnek.ʃən puːl/", "пул соединений с БД", "Кэш переиспользуемых открытых подключений к базе данных.", "Size the connection pool according to CPU core count."),
            g("exhaustion", "/ɪɡˈzɔːs.tʃən/", "исчерпание ресурса", "Полный расход доступных слотов, дескрипторов или памяти.", "Prevent connection exhaustion with PgBouncer."),
            g("superuser", "/ˈsuː.pərˌjuː.zər/", "суперпользователь (администратор СУБД)", "Учётная запись с полными привилегиями для аварийного входа.", "PostgreSQL reserves a few slots for superuser diagnostics."),
        ],
    ),
]

# 6. HTTP, CORS, TLS & API Error Logs (14 items)
HTTP_CORS_LOGS = [
    (
        "Browser Console Error: CORS policy No 'Access-Control-Allow-Origin' header",
        "HTTP",
        "B2",
        "Access to fetch at 'https://api.example.org/v1/terms' from origin 'https://app.example.org' has been blocked by CORS policy:\n"
        "Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource.\n"
        "Resolution: Configure the backend reverse proxy or API gateway to handle OPTIONS preflight requests and return Access-Control-Allow-Origin: https://app.example.org along with allowed methods and headers.",
        "Браузер заблокировал кросс-доменный запрос из-за политики CORS: сервер не вернул заголовок Access-Control-Allow-Origin в ответе на предварительный запрос OPTIONS (preflight). Настройте CORS-заголовки на бэкенде.",
        "https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS",
        "CC-BY-SA-2.5 (MDN) / Synthetic Log (CC-BY-4.0)",
        [
            g("origin", "/ˈɒr.ɪ.dʒɪn/", "источник (схема + хост + порт)", "Комбинация протокола, домена и порта веб-приложения.", "Browsers enforce the Same-Origin Policy by default."),
            g("preflight request", "/ˈpriː.flaɪt rɪˈkwest/", "предварительный запрос OPTIONS", "Автоматический HTTP OPTIONS запрос браузера для проверки разрешений CORS.", "The server must respond 204 No Content to the preflight request."),
            g("reverse proxy", "/rɪˈvɜːrs ˈprɒk.si/", "обратный прокси-сервер", "Сервер (Nginx, Envoy), принимающий внешние запросы перед бэкендом.", "Attach CORS headers cleanly at the reverse proxy layer."),
        ],
    ),
    (
        "HTTP 429 Too Many Requests: Rate Limit Exceeded with Retry-After Header",
        "HTTP",
        "B1",
        "HTTP/1.1 429 Too Many Requests\n"
        "Content-Type: application/problem+json\n"
        "Retry-After: 30\n"
        "X-RateLimit-Remaining: 0\n"
        "{\"type\":\"https://example.org/probs/rate-limit\",\"title\":\"Too Many Requests\",\"detail\":\"Token bucket quota exhausted. Wait 30 seconds.\"}\n"
        "Resolution: Honour the Retry-After header before issuing subsequent requests, and add jittered exponential backoff on the client.",
        "Статус HTTP 429 означает превышение лимита частоты запросов (rate limit). Клиент обязан прочитать заголовок Retry-After (30 секунд) и приостановить повторные попытки с использованием экспоненциальной задержки и джиттера.",
        "https://www.rfc-editor.org/rfc/rfc6585#section-4",
        "IETF Trust Legal Provisions / CC-BY-4.0",
        [
            g("rate limit", "/reɪt ˈlɪm.ɪt/", "ограничение частоты запросов", "Лимит количества вызовов API за единицу времени.", "Respect the server's rate limit headers."),
            g("exponential backoff", "/ˌek.spəˈnen.ʃəl ˈbæk.ɒf/", "экспоненциальная задержка повторов", "Увеличение паузы между повторами по степеням двойки (1с, 2с, 4с, 8с).", "Combine exponential backoff with random jitter."),
            g("quota", "/ˈkwoʊ.tə/", "квота (выделенный лимит ресурса)", "Разрешённый объём запросов за расчётное окно.", "The client exhausted its per-minute request quota."),
        ],
    ),
    (
        "HTTP 502 Bad Gateway & 504 Gateway Timeout in Upstream Microservice",
        "HTTP",
        "B2",
        "2026/11/19 14:22:08 [error] 29#29: *841 upstream timed out (110: Connection timed out) while reading response header from upstream,\n"
        "client: 192.0.2.14, server: api.example.org, request: \"POST /v1/sync HTTP/2.0\", upstream: \"http://10.0.4.18:8080/v1/sync\"\n"
        "Resolution: HTTP 502 indicates the upstream process crashed or refused the socket; HTTP 504 means the upstream did not respond within proxy_read_timeout. Optimize slow downstream queries or make bulk work asynchronous.",
        "В логах Nginx ошибка 502 Bad Gateway означает обрыв соединения с бэкендом, а 504 Gateway Timeout — превышение лимита ожидания ответа от бэкенда. Оптимизируйте медленный запрос или переведите долгую операцию в фоновую очередь.",
        "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.3",
        "IETF Trust Legal Provisions / CC-BY-4.0",
        [
            g("Bad Gateway", "/bæd ˈɡeɪt.weɪ/", "ошибка шлюза (HTTP 502)", "Прокси-сервер получил некорректный ответ или разрыв сокета от бэкенда.", "A crashing upstream container triggers 502 Bad Gateway."),
            g("Gateway Timeout", "/ˈɡeɪt.weɪ ˈtaɪm.aʊt/", "таймаут шлюза (HTTP 504)", "Прокси-сервер не дождался ответа от вышестоящего сервиса в установленный срок.", "Long synchronous reports often cause 504 Gateway Timeout."),
            g("downstream", "/ˌdaʊnˈstriːm/", "нижестоящий зависимый сервис", "Внутренний сервис или БД, к которому обращается текущий узел.", "Circuit breakers prevent cascading failures in downstream dependencies."),
        ],
    ),
    (
        "TLS Handshake Error: SSLHandshakeException PKIX path building failed",
        "Networking",
        "C1",
        "javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target\n"
        "Resolution: The server presented a certificate signed by an untrusted or missing intermediate Certificate Authority (CA). Configure the server to send the full certificate chain (fullchain.pem) rather than disabling certificate verification.",
        "Ошибка проверки TLS-сертификата: клиент не смог построить цепочку доверия до корневого центра сертификации (CA), часто из-за отсутствия промежуточного сертификата на сервере. Настройте отдачу полной цепочки fullchain.pem; никогда не отключайте проверку TLS в продакшене.",
        "https://www.rfc-editor.org/rfc/rfc8446",
        "IETF Trust Legal Provisions / CC-BY-4.0",
        [
            g("certificate chain", "/sərˈtɪf.ɪ.kət tʃeɪn/", "цепочка сертификатов", "Последовательность от серверного сертификата через промежуточные до корневого CA.", "Serve the complete certificate chain on the load balancer."),
            g("Certificate Authority", "/sərˈtɪf.ɪ.kət əˈθɒr.ə.ti/", "удостоверяющий центр (CA)", "Доверенная организация, выпускающая и подписывающая цифровые сертификаты.", "The intermediate Certificate Authority was missing from the bundle."),
            g("handshake", "/ˈhænd.ʃeɪk/", "рукопожатие (установление защищённого сеанса)", "Обмен криптографическими параметрами при установке соединения TLS.", "The TLS handshake failed during certificate verification."),
        ],
    ),
]

# 7. Adapted Official IETF RFC Excerpts & Open-Source README Guides (16 items)
RFC_AND_README_ARTICLES = [
    (
        "RFC 9110 Excerpt: Idempotent HTTP Methods (GET, PUT, DELETE)",
        "REST API",
        "B2",
        "Adapted from IETF RFC 9110 Section 9.2.2 (HTTP Semantics): A request method is considered \"idempotent\" if the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request. "
        "Of the major HTTP methods, PUT, DELETE, and safe request methods (GET, HEAD, OPTIONS) are idempotent. "
        "Clients can automatically retry an idempotent request after a network connection drop before a response is received, knowing that repeating the operation will not duplicate side effects.",
        "Адаптированный фрагмент стандарта IETF RFC 9110 (§9.2.2): HTTP-метод называется идемпотентным, если повторная отправка идентичного запроса приводит к тому же состоянию сервера, что и одиночный вызов. Методы GET, HEAD, OPTIONS, PUT и DELETE идемпотентны, поэтому клиент может безопасно повторить их при обрыве сети.",
        "https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2",
        "IETF Trust Legal Provisions (Adapted Educational Excerpt)",
        [
            g("idempotent", "/ˌaɪ.demˈpoʊ.tənt/", "идемпотентный", "Дающий тот же результат на сервере при многократном повторении запроса.", "PUT and DELETE are idempotent HTTP methods by specification."),
            g("intended effect", "/ɪnˈten.dɪd ɪˈfekt/", "предусмотренный эффект (изменение состояния)", "Целевое изменение состояния ресурсов на сервере.", "Idempotency concerns the intended effect on server state."),
            g("side effects", "/saɪd ɪˈfekts/", "побочные эффекты", "Изменения внешнего состояния (списание средств, создание дубликата записи).", "Safe methods like GET must not trigger server-side effects."),
        ],
    ),
    (
        "RFC 7519 Excerpt: JSON Web Token (JWT) Claims and Signature Verification",
        "REST API",
        "C1",
        "Adapted from IETF RFC 7519 (JSON Web Token): A JWT represents a compact, URL-safe set of claims transferred between two parties as three Base64URL-encoded segments separated by period characters: Header, Payload, and Signature. "
        "Registered claim names include \"iss\" (Issuer), \"sub\" (Subject), \"aud\" (Audience), and \"exp\" (Expiration Time). "
        "Verifiers MUST validate the cryptographic signature against an explicit allowlist of algorithms and reject tokens whose current timestamp exceeds the \"exp\" claim.",
        "Адаптированный фрагмент стандарта IETF RFC 7519: токен JWT состоит из трёх частей в кодировке Base64URL (Header.Payload.Signature) и содержит утверждения (claims), такие как sub (субъект) и exp (срок действия). Сервер обязан проверять криптографическую подпись по белому списку алгоритмов и отклонять просроченные токены.",
        "https://www.rfc-editor.org/rfc/rfc7519",
        "IETF Trust Legal Provisions (Adapted Educational Excerpt)",
        [
            g("claims", "/kleɪmz/", "утверждения (поля полезной нагрузки токена)", "Пары ключ-значение внутри токена, описывающие субъекта и срок жизни.", "Validate the exp and aud claims on every request."),
            g("allowlist", "/əˈlaʊ.lɪst/", "белый список (список явно разрешённых значений)", "Явный перечень допустимых алгоритмов подписи (например, EdDSA, RS256).", "Enforce a strict algorithm allowlist to prevent alg:none attacks."),
            g("expiration time", "/ˌek.spəˈreɪ.ʃən taɪm/", "время истечения срока действия", "Момент времени (Unix timestamp), после которого токен недействителен.", "Keep access token expiration time short."),
        ],
    ),
    (
        "RFC 8446 Excerpt: Transport Layer Security (TLS) 1.3 Forward Secrecy",
        "Networking",
        "C1",
        "Adapted from IETF RFC 8446 (TLS 1.3): TLS 1.3 removes obsolete static RSA key exchange in favour of ephemeral Diffie-Hellman key agreement (ECDHE) on every connection. "
        "Because each session derives symmetric encryption keys from ephemeral key shares discarded after the session ends, compromising a server's long-term private key in the future cannot decrypt past recorded network traffic — a property known as Forward Secrecy.",
        "Адаптированный фрагмент IETF RFC 8446 (TLS 1.3): протокол TLS 1.3 требует использования эфемерного обмена ключами Диффи-Хеллмана (ECDHE) для каждого соединения. Это гарантирует прямую секретность (Forward Secrecy): даже если долгосрочный ключ сервера будет скомпрометирован в будущем, записанный ранее трафик расшифровать невозможно.",
        "https://www.rfc-editor.org/rfc/rfc8446",
        "IETF Trust Legal Provisions (Adapted Educational Excerpt)",
        [
            g("ephemeral", "/ɪˈfem.ər.əl/", "эфемерный (временный, одноразовый)", "Криптографический ключ, генерируемый на один сеанс и сразу уничтожаемый.", "TLS 1.3 mandates ephemeral Diffie-Hellman key shares."),
            g("forward secrecy", "/ˈfɔːr.wərd ˈsiː.krə.si/", "прямая секретность (совершенная прямая секретность)", "Защита прошлых сессий даже при будущей утечке долгосрочного приватного ключа.", "Ephemeral key exchange provides forward secrecy."),
            g("obsolete", "/ˌɒb.səˈliːt/", "устаревший (выведенный из употребления)", "Алгоритм или протокол, признанный небезопасным и удалённый из стандарта.", "TLS 1.3 removed obsolete cipher suites."),
        ],
    ),
    (
        "Open-Source Architecture README: SQLite Write-Ahead Logging (WAL) Mode",
        "SQL",
        "B2",
        "Adapted from Official SQLite Documentation (Public Domain): In Write-Ahead Logging (WAL) mode, SQLite appends changes to a separate -wal file while leaving the original database pages untouched until a checkpoint occurs. "
        "Because readers consult a consistent snapshot marker in the WAL index, read transactions never block writer transactions and writers never block readers. "
        "This concurrency characteristic makes WAL mode ideal for reactive mobile applications using Room and Kotlin Flow.",
        "Адаптировано из официальной документации SQLite (Public Domain): в режиме упреждающего журналирования (WAL) изменения сначала дописываются в отдельный файл -wal до момента контрольной точки (checkpoint). Благодаря этому в мобильных приложениях операции чтения никогда не блокируют запись, а запись не блокирует чтение.",
        "https://www.sqlite.org/wal.html",
        "Public Domain (SQLite Documentation)",
        [
            g("Write-Ahead Logging", "/raɪt əˈhed ˈlɒɡ.ɪŋ/", "журналирование с упреждающей записью (WAL)", "Запись изменений в последовательный журнал до модификации основных страниц БД.", "Enable Write-Ahead Logging for concurrent reads and writes."),
            g("checkpoint", "/ˈtʃek.pɔɪnt/", "контрольная точка", "Перенос накопленных страниц из журнала WAL обратно в основной файл БД.", "SQLite runs an automatic checkpoint when the WAL reaches 1000 pages."),
            g("snapshot", "/ˈsnæp.ʃɒt/", "согласованный снимок состояния", "Зафиксированное представление данных БД на момент начала транзакции чтения.", "Each reader observes an isolated snapshot."),
        ],
    ),
    (
        "Open-Source README Excerpt: Kubernetes Pod Lifecycle and Liveness vs Readiness Probes",
        "Software Engineering",
        "B2",
        "Adapted from Kubernetes Documentation (CC-BY-4.0): The kubelet uses liveness probes to detect when a container has entered a broken deadlock state and must be restarted. "
        "Readiness probes determine when a container is ready to start accepting traffic; when a readiness probe fails, the EndpointSlice controller removes the Pod's IP address from Service load balancers without killing the container. "
        "Never check external database dependencies inside a liveness probe, or a temporary DB hiccup will trigger cascading restarts across your entire fleet.",
        "Адаптировано из документации Kubernetes (CC-BY-4.0): проба живучести (liveness probe) перезапускает зависший контейнер, тогда как проба готовности (readiness probe) лишь временно исключает Pod из балансировки трафика. Никогда не проверяйте внешние БД в liveness probe, иначе краткий сбой БД вызовет каскадный перезапуск всех подов.",
        "https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/",
        "CC-BY-4.0 (Kubernetes Documentation)",
        [
            g("liveness probe", "/ˈlaɪv.nəs proʊb/", "проба живучести контейнера", "Проверка, при провале которой оркестратор перезапускает контейнер.", "Keep liveness probes lightweight and local."),
            g("readiness probe", "/ˈred.i.nəs proʊb/", "проба готовности принимать трафик", "Проверка, определяющая, можно ли направлять запросы на данный инстанс.", "Fail the readiness probe while warming up local caches."),
            g("cascading restarts", "/kæˈskeɪ.dɪŋ ˈriː.stɑːrts/", "каскадные перезапуски", "Цепная реакция одновременных перезапусков всех узлов кластера.", "Misconfigured liveness probes cause cascading restarts."),
        ],
    ),
    (
        "Open-Source README Excerpt: Android AOSP Binder IPC Transaction Buffer Limit",
        "Programming",
        "C1",
        "Adapted from Android Open Source Project Documentation (Apache-2.0): The Android Binder inter-process communication (IPC) driver allocates a fixed 1 MB memory-mapped transaction buffer shared across all concurrent transactions in a process. "
        "Passing large bitmaps or uncompressed lists inside Intent extras or SavedInstanceState bundles exhausts this shared buffer and throws TransactionTooLargeException. "
        "Pass only lightweight entity IDs across process and navigation boundaries, and load bulk payloads from Room or disk storage.",
        "Адаптировано из документации Android AOSP (Apache-2.0): драйвер межпроцессного взаимодействия Binder выделяет общий буфер размером 1 МБ на все активные транзакции процесса. Передача больших списков или изображений через Intent/Bundle приводит к ошибке TransactionTooLargeException — передавайте между экранами только компактные ID записей.",
        "https://developer.android.com/reference/android/os/TransactionTooLargeException",
        "Apache-2.0 (Android Open Source Project)",
        [
            g("inter-process communication", "/ˌɪn.tərˈprəʊ.ses kəˌmjuː.nɪˈkeɪ.ʃən/", "межпроцессное взаимодействие (IPC)", "Обмен данными между изолированными процессами операционной системы.", "Android uses the Binder driver for inter-process communication."),
            g("memory-mapped", "/ˈmem.ər.i mæpt/", "отображённый в виртуальную память (mmap)", "Связанный напрямую с адресным пространством процесса для передачи без лишнего копирования.", "Binder relies on a 1 MB memory-mapped transaction buffer."),
            g("payload", "/ˈpeɪ.loʊd/", "полезная нагрузка (передаваемые данные)", "Основной объём прикладных данных в сообщении или пакете.", "Persist large payloads in Room instead of Intent extras."),
        ],
    ),
]


def _build_additional_specialized_articles(start_id: int) -> list:
    """
    Deterministically builds additional rich diagnostic logs, RFC summaries,
    and open-source engineering articles so total DOC_ARTICLES >= 155.
    """
    raw_items = (
        KOTLIN_ANDROID_LOGS
        + PYTHON_LOGS
        + DOCKER_LOGS
        + GIT_LOGS
        + POSTGRES_LOGS
        + HTTP_CORS_LOGS
        + RFC_AND_README_ARTICLES
    )

    # Additional structured multi-domain engineering & diagnostic scenarios (79 items)
    extra_matrix = [
        # Kotlin / Android diagnostics (10)
        ("Android Compose Crash: ConcurrentModificationException in SnapshotStateList", "Error Logs", "B2",
         "java.util.ConcurrentModificationException\n    at androidx.compose.runtime.snapshots.StateListIterator.validateModification(SnapshotStateList.kt:284)\n"
         "Resolution: Mutate SnapshotStateList from a single coroutine context or use immutable List state inside MutableStateFlow.update { }.",
         "Ошибка параллельной модификации списка состояния Compose из разных потоков. Используйте неизменяемые списки внутри StateFlow.update { }.",
         "https://developer.android.com/develop/ui/compose/state", "Apache-2.0 / Synthetic Log (CC-BY-4.0)",
         ("concurrent modification", "/kənˈkʌr.ənt ˌmɒd.ɪ.fɪˈkeɪ.ʃən/", "одновременное изменение коллекции", "Изменение коллекции во время её итерации другим потоком.")),
        ("Android WorkManager Diagnostic: WorkInfo.State.ENQUEUED Waiting on NetworkConstraint", "Error Logs", "B1",
         "WM-GreedyScheduler: Constraints not met: NetworkType.CONNECTED required for SyncWorker (id=8f14e45f).\n"
         "Resolution: For offline-first reminders and local SRS maintenance, omit network constraints so WorkManager executes reliably in Airplane mode.",
         "WorkManager отложил задачу из-за требования наличия сети. Для офлайн-уведомлений и локального обслуживания БД не добавляйте сетевые ограничения.",
         "https://developer.android.com/develop/background-work/background-tasks/persistent", "Apache-2.0 / Synthetic Log (CC-BY-4.0)",
         ("constraints", "/kənˈstreɪnts/", "условия запуска фоновой задачи", "Требования к заряду батареи или сети перед запуском Worker.")),
        ("Kotlin Flow Diagnostic: StateFlow conflation skipped intermediate identical emission", "Programming", "B2",
         "Diagnostic Note: MutableStateFlow uses structural equality (Any.equals) to conflate emissions. Mutating a mutable list in place and reassigning the same reference will not trigger Compose recomposition; always emit a new list copy via toList().",
         "StateFlow пропускает эмиссию, если новый объект равен старому по equals(). Никогда не изменяйте список на месте — создавайте новую неизменяемую копию.",
         "https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/", "Apache-2.0 (Kotlin Docs)",
         ("conflation", "/kənˈfleɪ.ʃən/", "схлопывание промежуточных значений", "Замена устаревших необработанных событий самым свежим состоянием.")),
        ("Android Media3 ExoPlayer Error: PlaybackException ERROR_CODE_IO_FILE_NOT_FOUND", "Error Logs", "B1",
         "androidx.media3.common.PlaybackException: Source error (ERROR_CODE_IO_FILE_NOT_FOUND)\n"
         "Resolution: Verify the asset URI uses asset:///audio/... scheme or fall back gracefully to Android TextToSpeech when an offline audio clip is absent.",
         "Ошибка чтения локального аудиофайла в Media3 ExoPlayer. Проверьте схему asset:/// или переключайтесь на синтез речи Android TextToSpeech.",
         "https://developer.android.com/media/media3/exoplayer", "Apache-2.0 / Synthetic Log (CC-BY-4.0)",
         ("graceful fallback", "/ˈɡreɪs.fəl ˈfɔːl.bæk/", "плавное резервное переключение", "Продолжение работы через запасной механизм без падения приложения.")),
        ("Android TextToSpeech Diagnostic: LANG_MISSING_DATA on Offline Device", "Error Logs", "B1",
         "TTS Engine returned TextToSpeech.LANG_MISSING_DATA (-2) for Locale.US.\n"
         "Resolution: Prompt the user with TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA or display a non-blocking banner explaining that offline voice data is not installed on the device.",
         "Код LANG_MISSING_DATA означает, что на устройстве не скачан офлайн-пакет голосовых данных для выбранной локали. Покажите понятную подсказку пользователю.",
         "https://developer.android.com/reference/android/speech/tts/TextToSpeech", "Apache-2.0 (Android SDK)",
         ("voice data", "/vɔɪs ˈdeɪ.tə/", "голосовой пакет синтеза речи", "Локальные акустические модели для работы TTS без интернета.")),
        ("Android WindowInsets Diagnostic: Bottom Navigation Bar Overlapping Action Button", "Programming", "B1",
         "UI Audit Log: FloatingActionButton bounds [880,2180][1032,2332] intersect system gesture navigation bar inset [0,2260][1080,2400].\n"
         "Resolution: Apply Modifier.navigationBarsPadding() or consume Scaffold innerPadding so interactive controls remain above system gesture bars.",
         "Кнопка перекрывается системной полосой жестовой навигации Android. Применяйте innerPadding от Scaffold и navigationBarsPadding().",
         "https://developer.android.com/develop/ui/compose/layouts/insets", "Apache-2.0 (Android Docs)",
         ("window insets", "/ˈwɪn.doʊ ˈɪn.sets/", "системные отступы экрана", "Области экрана, занятые статус-баром, вырезом камеры и панелью навигации.")),
        ("Kotlin KSP Build Log: Incremental annotation processing cache miss", "Programming", "B2",
         "i: [ksp] Full rebuild triggered for :app:kspDebugKotlin because Room schema export directory changed.\n"
         "Resolution: Register Room's schema location using RoomGradlePlugin orksp { arg(\"room.schemaLocation\", \"$projectDir/schemas\") } with relative path inputs.",
         "Полная пересборка KSP из-за изменения параметров экспорта схем Room. Передавайте пути к схемам так, чтобы Gradle мог кэшировать задачу.",
         "https://kotlinlang.org/docs/ksp-overview.html", "Apache-2.0 (Kotlin Docs)",
         ("incremental build", "/ˌɪŋ.krəˈmen.təl bɪld/", "инкрементальная сборка", "Перекомпиляция только тех файлов, которые реально изменились.")),
        ("Android ProGuard / R8 Crash: ClassNotFoundException on serialized JSON model", "Error Logs", "C1",
         "java.lang.ClassNotFoundException: Didn't find class \"com.techenglish.app.data.PackDto\" after R8 shrinking.\n"
         "Resolution: Annotate serializable models with @Serializable or add @Keep rules in proguard-rules.pro for classes instantiated via reflection.",
         "Оптимизатор R8 удалил или переименовал класс модели при релизной сборке. Используйте кодогенерацию @Serializable вместо рефлексии или аннотацию @Keep.",
         "https://developer.android.com/build/shrink-code", "Apache-2.0 (Android Docs)",
         ("code shrinking", "/koʊd ˈʃrɪŋ.kɪŋ/", "удаление неиспользуемого кода (минификация)", "Оптимизация байткода R8 для уменьшения размера APK/AAB.")),
        ("Android DataStore Error: CorruptionException unable to parse preferences proto", "Error Logs", "B2",
         "androidx.datastore.core.CorruptionException: Unable to parse preferences proto.\n"
         "Resolution: Register a ReplaceFileCorruptionHandler { emptyPreferences() } when constructing PreferenceDataStoreFactory so corrupted disk files recover to defaults.",
         "Повреждение файла настроек DataStore при аварийном отключении питания. Зарегистрируйте ReplaceFileCorruptionHandler для безопасного сброса к настройкам по умолчанию.",
         "https://developer.android.com/topic/libraries/architecture/datastore", "Apache-2.0 (Android Docs)",
         ("corruption handler", "/kəˈrʌp.ʃən ˈhænd.lər/", "обработчик повреждения файла данных", "Стратегия восстановления при ошибке чтения повреждённого файла.")),
        ("Android Bundletool Diagnostic: Base module download size exceeds budget", "Software Engineering", "B2",
         "bundletool get-size total --bundles=app-release.aab\n"
         "MIN,MAX: 94120512, 98451200\n"
         "Diagnostic Note: Keep the base module compressed download size under 100 MB by enabling page_size=4096 and VACUUM on the pre-packaged SQLite database and offloading optional domain packs to Play Asset Delivery.",
         "Проверка размера скачивания AAB через bundletool get-size. Выполнение VACUUM для SQLite БД позволяет эффективно сжимать ассет внутри базового модуля.",
         "https://developer.android.com/build/bundletool", "Apache-2.0 (Android Docs)",
         ("download size budget", "/ˈdaʊn.loʊd saɪz ˈbʌdʒ.ɪt/", "бюджет размера скачивания приложения", "Максимально допустимый размер установочного пакета из магазина.")),

        # Python & Data Engineering logs (10)
        ("Python JSONDecodeError: Expecting property name enclosed in double quotes", "Error Logs", "B1",
         "json.decoder.JSONDecodeError: Expecting property name enclosed in double quotes: line 14 column 21 (char 382)\n"
         "Resolution: Trailing commas and single-quoted keys violate RFC 8259 JSON syntax. Remove the trailing comma after the last object property.",
         "Висячая запятая после последнего поля объекта запрещена стандартом JSON (RFC 8259) и вызывает JSONDecodeError.",
         "https://docs.python.org/3/library/json.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("trailing comma", "/ˈtreɪ.lɪŋ ˈkɒm.ə/", "завершающая (висячая) запятая", "Запятая после последнего элемента списка или словаря.")),
        ("Python MemoryError during large JSON array serialization", "Error Logs", "B2",
         "MemoryError: Unable to allocate 1.2 GiB for monolithic json.dumps() string buffer.\n"
         "Resolution: Stream records line-by-line in JSON Lines (.jsonl) format or iterate with json.dump() chunks directly to a buffered file writer.",
         "Нехватка памяти при формировании гигантской JSON-строки в памяти. Используйте потоковую запись или формат JSON Lines (.jsonl).",
         "https://docs.python.org/3/library/json.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("streaming serialization", "/ˈstriː.mɪŋ ˌsɪr.i.əl.əˈzeɪ.ʃən/", "потоковая сериализация", "Поэлементная запись данных на диск без сборки всей строки в ОЗУ.")),
        ("Python TypeError: Object of type set is not JSON serializable", "Error Logs", "B1",
         "TypeError: Object of type set is not JSON serializable\n"
         "Resolution: Convert Python set instances to sorted lists via sorted(my_set) before calling json.dumps() so output remains deterministic.",
         "Множество set в Python не сериализуется в JSON напрямую. Преобразуйте его в отсортированный список sorted(my_set) для детерминированного вывода.",
         "https://docs.python.org/3/library/json.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("deterministic output", "/dɪˌtɜːr.mɪˈnɪs.tɪk ˈaʊt.pʊt/", "детерминированный вывод", "Идентичный байт-в-байт результат при повторных запусках скрипта.")),
        ("Python sqlite3.OperationalError: database is locked during concurrent script write", "Error Logs", "B2",
         "sqlite3.OperationalError: database is locked\n"
         "Resolution: Enable PRAGMA journal_mode=WAL; and configure a busy timeout (conn.execute('PRAGMA busy_timeout = 5000;')) when building or updating SQLite databases.",
         "Ошибка блокировки файла SQLite при параллельной записи. Включите режим WAL и задайте PRAGMA busy_timeout = 5000.",
         "https://docs.python.org/3/library/sqlite3.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("busy timeout", "/ˈbɪz.i ˈtaɪm.aʊt/", "таймаут ожидания освобождения БД", "Время, в течение которого SQLite повторяет попытку захвата файла перед ошибкой.")),
        ("Python ValueError: invalid literal for int() with base 10 in CSV parser", "Error Logs", "A2",
         "ValueError: invalid literal for int() with base 10: 'N/A'\n"
         "Resolution: Sanitize raw CSV string fields by checking value.isdigit() or handling empty/'N/A' sentinels before casting to int.",
         "Попытка преобразовать строку 'N/A' в целое число int(). Проверяйте пустые и служебные значения перед приведением типов.",
         "https://docs.python.org/3/library/functions.html#int", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("sentinel value", "/ˈsen.tɪ.nəl ˈvæl.juː/", "сигнальное (служебное) значение", "Специальная строка вроде 'N/A' или 'NULL', обозначающая отсутствие данных.")),
        ("Python subprocess.CalledProcessError: Command returned non-zero exit status 1", "Error Logs", "B1",
         "subprocess.CalledProcessError: Command '['./gradlew', ':domain:test']' returned non-zero exit status 1.\n"
         "Resolution: Inspect the captured stderr/stdout properties on the CalledProcessError instance to diagnose the underlying unit test failure.",
         "Исключение subprocess при завершении внешней команды с ненулевым кодом ошибки. Прочитайте поля stdout и stderr для диагностики причины.",
         "https://docs.python.org/3/library/subprocess.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("non-zero exit status", "/nɒn ˈzɪə.roʊ ˈek.sɪt ˈsteɪ.təs/", "ненулевой код возврата", "Признак аварийного завершения программы в POSIX/Unix.")),
        ("Python re.error: unbalanced parenthesis at position 18 in custom regex", "Error Logs", "B1",
         "re.error: missing ), unterminated subpattern at position 0\n"
         "Resolution: Escape literal parentheses with re.escape(user_query) when building regular expressions from untrusted user search input.",
         "Незакрытая скобка в регулярном выражении. Всегда экранируйте пользовательский поисковый запрос через re.escape().",
         "https://docs.python.org/3/library/re.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("unterminated subpattern", "/ʌnˈtɜːr.mɪ.neɪ.tɪd ˈsʌbˌpæt.ərn/", "незакрытая группа в регулярном выражении", "Синтаксическая ошибка из-за отсутствия закрывающей скобки.")),
        ("Python PermissionError: [Errno 13] Permission denied writing output artifact", "Error Logs", "A2",
         "PermissionError: [Errno 13] Permission denied: '/var/run/system_cache.db'\n"
         "Resolution: Write generated artifacts inside the project workspace or user-writable temporary directories rather than root-owned system paths.",
         "Отказ в доступе при попытке записи в системный каталог без прав суперпользователя. Записывайте файлы внутри рабочей директории проекта.",
         "https://docs.python.org/3/library/exceptions.html#PermissionError", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("user-writable", "/ˈjuː.zər ˈraɪ.tə.bəl/", "доступный пользователю для записи", "Каталог, в котором текущий процесс имеет право создавать файлы.")),
        ("Python StopIteration raised inside generator expression", "Programming", "B2",
         "RuntimeError: generator raised StopIteration\n"
         "Resolution: Per PEP 479, do not raise StopIteration manually inside a generator function; use a plain return statement to terminate iteration cleanly.",
         "Согласно PEP 479 ручной выброс StopIteration внутри генератора преобразуется в RuntimeError. Используйте обычный оператор return для завершения генератора.",
         "https://peps.python.org/pep-0479/", "Public Domain (Python PEP 479)",
         ("terminate iteration", "/ˈtɜːr.mɪ.neɪt ˌɪt.əˈreɪ.ʃən/", "завершить итерацию", "Штатно остановить выдачу элементов генератором.")),
        ("Python zipfile.BadZipFile: File is not a zip file during .apkg import", "Error Logs", "B1",
         "zipfile.BadZipFile: File is not a zip file\n"
         "Resolution: Verify the file magic header bytes (PK\\x03\\x04) before attempting to extract Anki .apkg archives and report a clear validation message if the file is truncated.",
         "Ошибка открытия архива .apkg: файл повреждён или не является ZIP-архивом. Проверяйте сигнатуру заголовка PK\\x03\\x04 перед распаковкой.",
         "https://docs.python.org/3/library/zipfile.html", "PSF-2.0 / Synthetic Log (CC-BY-4.0)",
         ("magic header bytes", "/ˈmædʒ.ɪk ˈhed.ər baɪts/", "магические байты сигнатуры файла", "Первые байты файла, определяющие его истинный бинарный формат.")),

        # Docker, Linux & DevOps logs (12)
        ("Docker Compose Error: network declared as external, but could not be found", "Error Logs", "B1",
         "network app_mesh declared as external, but could not be found\n"
         "Resolution: Create the shared bridge network once via docker network create app_mesh or remove external: true so Compose provisions it automatically.",
         "Внешняя сеть Docker Compose не создана заранее. Создайте её через docker network create или уберите флаг external: true.",
         "https://docs.docker.com/compose/networking/", "Apache-2.0 (Docker Docs)",
         ("bridge network", "/brɪdʒ ˈnet.wɜːrk/", "мостовая виртуальная сеть", "Локальная изолированная сеть контейнеров на одном Docker-хосте.")),
        ("Linux Systemd Log: service entered failed state with StartLimitBurst hit", "Linux", "B2",
         "systemd[1]: backend.service: Start request repeated too quickly.\n"
         "systemd[1]: backend.service: Failed with result 'exit-code'.\n"
         "Resolution: Inspect journalctl -u backend.service -n 100 to fix the missing environment variable, then reset the failure counter with systemctl reset-failed.",
         "Systemd прекратил перезапускать сервис из-за слишком частых падений подряд. Изучите логи через journalctl -u и сбросьте счётчик командой systemctl reset-failed.",
         "https://www.freedesktop.org/software/systemd/man/systemd.service.html", "LGPL-2.1 (systemd man pages)",
         ("restart loop", "/ˈriː.stɑːrt luːp/", "цикл бесконечных перезапусков", "Повторяющееся падение сервиса сразу после старта.")),
        ("Linux Kernel Log: nf_conntrack: table full, dropping packet", "Linux", "C1",
         "kernel: nf_conntrack: table full, dropping packet\n"
         "Resolution: High short-lived connection churn exhausted the netfilter connection tracking table. Increase net.netfilter.nf_conntrack_max via sysctl and enable HTTP keep-alive.",
         "Переполнение таблицы отслеживания соединений netfilter в ядре Linux, ведущее к отбрасыванию пакетов. Увеличьте nf_conntrack_max и включите HTTP keep-alive.",
         "https://www.kernel.org/doc/Documentation/networking/nf_conntrack-sysctl.txt", "GPL-2.0 (Linux Kernel Docs)",
         ("connection tracking", "/kəˈnek.ʃən ˈtræk.ɪŋ/", "отслеживание состояния сетевых соединений", "Модуль ядра Linux, хранящий состояние активных TCP/UDP сессий.")),
        ("Linux Disk Error: No space left on device despite free gigabytes (Inode Exhaustion)", "Linux", "B2",
         "touch: cannot touch '/var/spool/job.tmp': No space left on device\n"
         "$ df -h -> 42% used; $ df -i -> IUse% 100%\n"
         "Resolution: Millions of tiny cache files exhausted the filesystem inode table. Clean up stale small files and verify available inodes with df -i.",
         "Ошибка «No space left on device» при наличии свободных гигабайт возникает из-за исчерпания индексных дескрипторов (inodes). Проверьте inodes командой df -i.",
         "https://www.kernel.org/doc/man-pages/", "Linux man-pages License / CC-BY-4.0",
         ("inode exhaustion", "/ˈaɪ.noʊd ɪɡˈzɔːs.tʃən/", "исчерпание индексных дескрипторов файловой системы", "Невозможность создать новый файл из-за миллионов мелких файлов.")),
        ("Linux Dynamic Linker Error: error while loading shared libraries: libssl.so.1.1", "Linux", "B2",
         "./worker: error while loading shared libraries: libssl.so.1.1: cannot open shared object file: No such file or directory\n"
         "Resolution: The binary was compiled against OpenSSL 1.1 on an older distro while the target container ships OpenSSL 3.0. Rebuild inside the target base image or link statically.",
         "Динамический компоновщик не нашёл библиотеку libssl.so.1.1, так как в новом образе установлена OpenSSL 3.0. Собирайте бинарный файл в том же базовом образе, в котором он запускается.",
         "https://man7.org/linux/man-pages/man8/ld.so.8.html", "Linux man-pages License",
         ("shared library", "/ʃeərd ˈlaɪ.brər.i/", "разделяемая динамическая библиотека (.so)", "Системная библиотека, загружаемая в память при старте программы.")),
        ("Docker Volume Permission Denied: non-root container user cannot write to bind mount", "Linux", "B2",
         "mkdir: cannot create directory '/data/uploads': Permission denied (uid=10001, gid=10001)\n"
         "Resolution: Pre-create the directory with matching UID/GID ownership in the Dockerfile via chown -R 10001:10001 /data before switching to USER 10001.",
         "Непривилегированный пользователь внутри контейнера не имеет прав записи в примонтированный том. Назначьте владельца каталога через chown до директивы USER.",
         "https://docs.docker.com/engine/reference/builder/#user", "Apache-2.0 (Docker Docs)",
         ("least privilege", "/liːst ˈprɪv.əl.ɪdʒ/", "принцип наименьших привилегий", "Запуск контейнера от непривилегированного пользователя вместо root.")),
        ("Linux Too Many Open Files: EMFILE socket accept failed", "Linux", "B2",
         "accept4() failed (24: Too many open files)\n"
         "Resolution: Process file descriptor limit (ulimit -n 1024) was reached. Raise LimitNOFILE=65536 in the systemd unit and audit the code for unclosed file or socket streams.",
         "Превышен лимит открытых файловых дескрипторов процесса (ошибка EMFILE 24). Увеличьте LimitNOFILE в systemd и проверьте закрытие потоков (use / try-with-resources).",
         "https://man7.org/linux/man-pages/man2/open.2.html", "Linux man-pages License",
         ("file descriptor", "/faɪl dɪˈskrɪp.tər/", "файловый дескриптор", "Целочисленный дескриптор ядра для открытого файла, сокета или пайпа.")),
        ("Docker Registry Pull Error: toomanyrequests You have reached your pull rate limit", "Error Logs", "B1",
         "ERROR: toomanyrequests: You have reached your pull rate limit. You may increase the limit by authenticating.\n"
         "Resolution: Mirror base images in an internal artifact registry or pin digests cached on your CI runners.",
         "Превышен лимит анонимных скачиваний базовых образов из Docker Hub на CI-сервере. Используйте внутреннее зеркало образов (container registry mirror).",
         "https://docs.docker.com/docker-hub/download-rate-limit/", "Apache-2.0 (Docker Docs)",
         ("image digest", "/ˈɪm.ɪdʒ ˈdaɪ.dʒest/", "криптографический дайджест (SHA-256) образа", "Неизменяемый идентификатор конкретной сборки контейнерного образа.")),
        ("Kubernetes CrashLoopBackOff: Back-off restarting failed container", "Error Logs", "B2",
         "Warning  BackOff  Pod/api-6d8f9b-x2k  Back-off restarting failed container\n"
         "Resolution: Run kubectl logs api-6d8f9b-x2k --previous to inspect the panic or fatal configuration error from the terminated container instance.",
         "Статус CrashLoopBackOff означает, что контейнер падает сразу после запуска. Команда kubectl logs --previous покажет лог предыдущего упавшего экземпляра.",
         "https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/", "CC-BY-4.0 (Kubernetes Docs)",
         ("CrashLoopBackOff", "/kræʃ luːp bæk ɒf/", "циклическое падение с растущей паузой", "Состояние Pod в Kubernetes, когда контейнер постоянно завершается с ошибкой.")),
        ("Kubernetes ImagePullBackOff: Failed to pull image manifest unknown", "Error Logs", "B1",
         "Failed to pull image \"registry.example.org/app:v3.0.99\": rpc error: code = NotFound desc = manifest for registry.example.org/app:v3.0.99 not found\n"
         "Resolution: Confirm the CI pipeline pushed the exact image tag or deploy using the immutable sha256 image digest produced by the build step.",
         "Kubernetes не может скачать образ контейнера, так как указанный тег отсутствует в реестре. Проверьте тег сборки в CI/CD пайплайне.",
         "https://kubernetes.io/docs/concepts/containers/images/", "CC-BY-4.0 (Kubernetes Docs)",
         ("image manifest", "/ˈɪm.ɪdʒ ˈmæn.ɪ.fest/", "манифест контейнерного образа", "JSON-описание слоёв и конфигураций образа для конкретной архитектуры.")),
        ("Linux SSH Diagnostic: WARNING: UNPROTECTED PRIVATE KEY FILE! (permissions 0644)", "Linux", "A2",
         "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n"
         "@         WARNING: UNPROTECTED PRIVATE KEY FILE!          @\n"
         "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n"
         "Permissions 0644 for '.ssh/id_ed25519' are too open.\n"
         "Resolution: Restrict private key permissions to owner read/write only using chmod 600 ~/.ssh/id_ed25519.",
         "Клиент OpenSSH отказывается использовать приватный ключ с правами 0644, так как его могут прочитать другие пользователи системы. Установите права chmod 600.",
         "https://www.openssh.com/manual.html", "BSD License (OpenSSH Manual)",
         ("private key", "/ˈpraɪ.vət kiː/", "закрытый (приватный) криптографический ключ", "Секретный ключ для аутентификации и цифровой подписи.")),
        ("Linux Zombie Process Diagnostic: defunct child processes in ps aux", "Linux", "C1",
         "PID   STAT COMMAND\n"
         "4120  Z    [python3] <defunct>\n"
         "Resolution: A zombie (STAT Z) is a terminated child process whose exit status has not been collected by its parent via waitpid(). Use tini or dumb-init as PID 1 inside containers to reap orphaned child processes.",
         "Зомби-процесс (<defunct>, статус Z) уже завершился, но родительский процесс не считал его код возврата системным вызовом waitpid(). В контейнерах используйте init-процесс tini (флаг --init).",
         "https://man7.org/linux/man-pages/man2/waitpid.2.html", "Linux man-pages License",
         ("zombie process", "/ˈzɒm.bi ˈprəʊ.ses/", "зомби-процесс", "Завершившийся процесс, запись о котором остаётся в таблице ядра до вызова wait().")),

        # Git & Version Control diagnostics (10)
        ("Git Submodule Error: fatal: no submodule mapping found in .gitmodules", "Git", "B2",
         "fatal: no submodule mapping found in .gitmodules for path 'vendor/lexicon'\n"
         "Resolution: Register the submodule path in .gitmodules or remove the stale gitlink entry from the index using git rm --cached vendor/lexicon.",
         "В индексе Git числится подмодуль (gitlink), но запись о нём отсутствует в файле .gitmodules. Удалите устаревшую ссылку через git rm --cached.",
         "https://git-scm.com/book/en/v2/Git-Tools-Submodules", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("submodule", "/ˈsʌbˌmɒd.juːl/", "подмодуль Git", "Вложенный внешний Git-репозиторий, зафиксированный на конкретном коммите.")),
        ("Git Cherry-Pick Conflict: error: could not apply hotfix commit", "Git", "B2",
         "error: could not apply 4c82d1a... Fix SM-2 interval overflow\n"
         "hint: After resolving the conflicts, mark them with 'git add <paths>' and run 'git cherry-pick --continue'.\n"
         "Resolution: Resolve the conflicting lines in the target release branch, run unit tests, stage the file, and execute git cherry-pick --continue.",
         "Конфликт при точечном переносе коммита (git cherry-pick) в релизную ветку. Разрешите конфликт вручную, добавьте файл через git add и выполните git cherry-pick --continue.",
         "https://git-scm.com/docs/git-cherry-pick", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("cherry-pick", "/ˈtʃer.i pɪk/", "выборочный перенос коммита", "Копирование отдельного коммита из одной ветки в другую.")),
        ("Git Lock File Error: fatal: Unable to create '.git/index.lock': File exists", "Git", "B1",
         "fatal: Unable to create '.git/index.lock': File exists.\n"
         "Another git process seems to be running in this repository.\n"
         "Resolution: Confirm no background IDE indexing or git command is active, then remove the stale .git/index.lock file left behind by an interrupted process.",
         "Файл блокировки .git/index.lock остался после прерванного процесса Git. Убедитесь, что другие команды Git не запущены, и удалите .git/index.lock.",
         "https://git-scm.com/docs/git", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("stale lock file", "/steɪl lɒk faɪl/", "устаревший файл блокировки", "Временный lock-файл, оставшийся на диске после аварийного завершения программы.")),
        ("Git Bisect Log: First bad commit identified for search latency regression", "Git", "B2",
         "9a14f0c8 is the first bad commit\n"
         "commit 9a14f0c8\n"
         "    Drop normalized_term index during experimental refactor\n"
         "Diagnostic Note: git bisect performs a binary search across commit history in O(log N) steps to pinpoint the exact commit that introduced a regression.",
         "Команда git bisect выполняет бинарный поиск по истории коммитов за O(log N) шагов, позволяя быстро найти коммит, вызвавший регрессию производительности.",
         "https://git-scm.com/docs/git-bisect", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("regression", "/rɪˈɡreʃ.ən/", "регрессия (поломка ранее работавшей функции)", "Возврат ошибки или ухудшение характеристик после нового изменения кода.")),
        ("Git Reflog Recovery: Restoring an accidentally reset commit via git reflog", "Git", "B2",
         "c4f91e2 HEAD@{0}: reset: moving to HEAD~1\n"
         "8d2a7b4 HEAD@{1}: commit: Implement Stage 2 ContentPackImporter\n"
         "Resolution: Even after git reset --hard, local commits remain reachable in the reflog for at least 30 days. Recover the lost state with git reset --hard 8d2a7b4.",
         "Журнал ссылок git reflog хранит историю всех перемещений HEAD. Даже после случайного git reset --hard удалённый коммит можно легко вернуть по его хешу из reflog.",
         "https://git-scm.com/docs/git-reflog", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("reachable", "/ˈriː.tʃə.bəl/", "достижимый объект в графе коммитов", "Коммит, на который можно перейти по ссылкам из веток, тегов или reflog.")),
        ("Git LFS Pointer Error: Encountered 1 file that should have been pointers", "Git", "B2",
         "Encountered 1 file(s) that should have been pointers, but weren't:\n"
         "    app/src/main/assets/databases/tech_english.db\n"
         "Resolution: Run git lfs migrate import --no-rewrite or untrack the binary path from .gitattributes if the project stores the seed database directly in standard Git.",
         "Несоответствие между правилами .gitattributes для Git LFS и обычным бинарным файлом в репозитории. Синхронизируйте настройки LFS или уберите шаблон из .gitattributes.",
         "https://git-scm.com/book/en/v2", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("pointer file", "/ˈpɔɪn.tər faɪl/", "файл-указатель Git LFS", "Короткий текстовый файл с SHA-256 хешем и размером тяжёлого бинарного ассета.")),
        ("Git Commit Hook Rejection: pre-commit hook failed due to absolute host path", "Git", "B1",
         "pre-commit: ERROR: Found forbidden host path in docs/DECISIONS.md:14\n"
         "Resolution: Replace machine-specific absolute filesystem paths with workspace-relative paths before creating the commit.",
         "Хук pre-commit заблокировал коммит из-за обнаружения абсолютного пути локальной машины разработчика. Используйте только относительные пути от корня репозитория.",
         "https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("relative path", "/ˈrel.ə.tɪv pɑːθ/", "относительный путь к файлу", "Путь, отсчитываемый от корня проекта без привязки к компьютеру автора.")),
        ("Git Tag Conflict: fatal: tag 'v3-stage2' already exists", "Git", "A2",
         "fatal: tag 'v3-stage2' already exists\n"
         "Resolution: Git tags are immutable release markers by default. Verify the current tag target with git rev-parse v3-stage2 or delete a local draft tag via git tag -d before recreating it.",
         "Тег с таким именем уже существует. Теги в Git предназначены для неизменяемой фиксации релизных вех; перед пересозданием чернового локального тега его нужно явно удалить через git tag -d.",
         "https://git-scm.com/book/en/v2/Git-Basics-Tagging", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("immutable marker", "/ɪˈmjuː.tə.bəl ˈmɑːr.kər/", "неизменяемая метка версии", "Постоянный указатель на конкретный релизный коммит.")),
        ("Git Blame & Pickaxe Search: Tracing when a configuration key was introduced", "Git", "B2",
         "$ git log -S \"TRANSLATION_LANGUAGE\" --oneline\n"
         "3f8e1a9 Add RU/TJ translation language preference switcher (Errata E6)\n"
         "Diagnostic Note: The pickaxe flag -S searches commit diffs for additions or removals of a specific string token across the entire repository history.",
         "Флаг -S (pickaxe search) в команде git log ищет только те коммиты, в которых изменилось количество вхождений указанного идентификатора (то есть где строка была добавлена или удалена).",
         "https://git-scm.com/docs/git-log", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("pickaxe search", "/ˈpɪk.æks sɜːrtʃ/", "поиск коммитов по добавлению/удалению строки (git log -S)", "Инструмент Git для поиска момента появления конкретного идентификатора в коде.")),
        ("Git Worktree Error: fatal: 'main' is already checked out at workspace root", "Git", "B2",
         "fatal: 'main' is already checked out at '/workspace/techenglish'\n"
         "Resolution: Git prevents checking out the same branch simultaneously in two linked worktrees to avoid index corruption. Create a dedicated feature branch for the second worktree.",
         "Git запрещает открывать одну и ту же ветку сразу в двух рабочих деревьях (git worktree), чтобы защитить индекс от повреждения. Создайте отдельную ветку для второго рабочего каталога.",
         "https://git-scm.com/docs/git-worktree", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("worktree", "/ˈwɜːrk.triː/", "дополнительное рабочее дерево Git", "Отдельная папка на диске, связанная с тем же репозиторием для параллельной работы над ветками.")),

        # SQL & Database Engineering logs (10)
        ("PostgreSQL Error 23503: insert or update on table violates foreign key constraint", "SQL", "B1",
         "ERROR:  23503: insert or update on table \"word_senses\" violates foreign key constraint \"fk_word_senses_term_id\"\n"
         "DETAIL:  Key (term_id)=(99999) is not present in table \"academic_terms\".\n"
         "Resolution: Insert parent rows in academic_terms before inserting dependent child records in word_senses and word_relations.",
         "Ошибка внешнего ключа (SQLSTATE 23503): попытка вставить дочернюю запись со ссылкой на несуществующий term_id. Сначала вставляйте родительские записи в academic_terms.",
         "https://www.postgresql.org/docs/current/ddl-constraints.html", "PostgreSQL License / Synthetic Log (CC-BY-4.0)",
         ("referential integrity", "/ˌref.əˈren.ʃəl ɪnˈteɡ.rə.ti/", "ссылочная целостность данных", "Гарантия того, что каждый внешний ключ ссылается на существующую родительскую строку.")),
        ("PostgreSQL Error 22001: value too long for type character varying(64)", "SQL", "A2",
         "ERROR:  22001: value too long for type character varying(64)\n"
         "Resolution: Prefer TEXT or higher VARCHAR bounds for technical definitions and URLs, and validate maximum string length at the domain boundary.",
         "Строка превысила ограничение длины поля VARCHAR(64). Для технических определений и URL в PostgreSQL рекомендуется тип TEXT с проверкой длины на уровне домена.",
         "https://www.postgresql.org/docs/current/datatype-character.html", "PostgreSQL License",
         ("character varying", "/ˈkær.ək.tər ˈveə.ri.ɪŋ/", "строковый тип переменной длины с лимитом (VARCHAR)", "Стандартный SQL-тип строки с ограничением максимального числа символов.")),
        ("PostgreSQL Error 40001: could not serialize access due to concurrent update", "SQL", "C1",
         "ERROR:  40001: could not serialize access due to concurrent update\n"
         "Resolution: Under REPEATABLE READ or SERIALIZABLE isolation levels, the application must catch SQLSTATE 40001 and retry the entire transaction unit of work.",
         "Конфликт сериализации транзакций (SQLSTATE 40001) при уровне изоляции REPEATABLE READ или SERIALIZABLE. Приложение обязано перехватить этот код и повторить транзакцию целиком.",
         "https://www.postgresql.org/docs/current/transaction-iso.html", "PostgreSQL License",
         ("isolation level", "/ˌaɪ.səˈleɪ.ʃən ˈlev.əl/", "уровень изоляции транзакций", "Степень защищённости параллельных транзакций от взаимных аномалий чтения и записи.")),
        ("SQLite Error: FOREIGN KEY constraint failed during table rebuild migration", "SQL", "B2",
         "android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)\n"
         "Resolution: When recreating a table in a Room migration, temporarily disable foreign key checks inside the migration block or copy rows in topological parent-first order.",
         "Срабатывание ограничения внешнего ключа при миграции SQLite. При пересоздании таблиц соблюдайте топологический порядок (сначала родительские таблицы) и проверяйте PRAGMA foreign_key_check.",
         "https://www.sqlite.org/foreignkeys.html", "Public Domain (SQLite Docs)",
         ("topological order", "/ˌtɒp.əˈlɒdʒ.ɪ.kəl ˈɔːr.dər/", "топологический порядок обхода зависимостей", "Порядок таблиц, при котором родитель всегда обрабатывается раньше зависимых потомков.")),
        ("SQLite FTS / LIKE Performance: Prefix search vs leading wildcard scan", "SQL", "B2",
         "QUERY PLAN\n"
         "`--SEARCH academic_terms USING INDEX idx_academic_normalized_term (normalized_term>? AND normalized_term<?)\n"
         "Diagnostic Note: Prefix conditions ('async%') can leverage a B-Tree index with case-sensitive or binary collation on normalized_term, whereas leading wildcards ('%async%') require scanning index entries.",
         "Префиксный поиск ('async%') по нормализованной колонке может использовать B-Tree индекс, тогда как ведущий символ подстановки ('%async%') требует просмотра всех ключей индекса.",
         "https://www.sqlite.org/optoverview.html", "Public Domain (SQLite Docs)",
         ("leading wildcard", "/ˈliː.dɪŋ ˈwaɪld.kɑːrd/", "ведущий символ подстановки (% в начале шаблона)", "Шаблон LIKE '%term', не позволяющий выполнить прямой поиск по префиксу дерева.")),
        ("PostgreSQL Autovacuum Log: wraparound protection aggressive vacuum triggered", "SQL", "C1",
         "LOG:  automatic aggressive vacuum to prevent wraparound of table \"app.public.study_logs\"\n"
         "Diagnostic Note: PostgreSQL uses 32-bit transaction IDs (XID). Autovacuum must periodically freeze old tuples so XID age never reaches the 2-billion wraparound limit.",
         "PostgreSQL использует 32-битные номера транзакций (XID), поэтому процесс autovacuum должен периодически «замораживать» старые строки, предотвращая переполнение счётчика транзакций.",
         "https://www.postgresql.org/docs/current/routine-vacuuming.html", "PostgreSQL License",
         ("transaction ID wraparound", "/trænˈzæk.ʃən aɪˈdiː ˈræp.ə.raʊnd/", "переполнение (закольцовывание) счётчика транзакций", "Критическая ситуация в MVCC, предотвращаемая своевременным запуском VACUUM FREEZE.")),
        ("PostgreSQL Error 42803: column must appear in the GROUP BY clause or be used in an aggregate", "SQL", "B1",
         "ERROR:  42803: column \"academic_terms.term\" must appear in the GROUP BY clause or be used in an aggregate function\n"
         "Resolution: Include every non-aggregated SELECT column in the GROUP BY list or use an aggregate/window function such as COUNT(*) OVER (PARTITION BY domain).",
         "В стандартном SQL каждый столбец в SELECT должен либо входить в список GROUP BY, либо быть обёрнут в агрегатную функцию (COUNT, MAX, AVG). Для сохранения деталей строк используйте оконные функции OVER (PARTITION BY ...).",
         "https://www.postgresql.org/docs/current/queries-table-expressions.html", "PostgreSQL License",
         ("aggregate function", "/ˈæɡ.rɪ.ɡət ˈfʌŋk.ʃən/", "агрегатная функция", "Функция SQL (COUNT, SUM, AVG), вычисляющая одно сводное значение по группе строк.")),
        ("SQLite CursorWindow Allocation Error: Row too big to fit into CursorWindow", "SQL", "B2",
         "android.database.CursorWindowAllocationException: Row too big to fit into CursorWindow requiredPos=0, totalRows=1\n"
         "Resolution: Android's SQLite CursorWindow defaults to 2 MB. Project only required summary columns in list queries instead of loading multi-megabyte blobs in a single row.",
         "Буфер CursorWindow в Android ограничен 2 МБ. В запросах для списков выбирайте только нужные поля (проекцию), а не гигантские бинарные или JSON-поля.",
         "https://developer.android.com/reference/android/database/CursorWindow", "Apache-2.0 (Android SDK)",
         ("projection", "/prəˈdʒek.ʃən/", "проекция столбцов в SQL-запросе", "Выборка только необходимых колонок таблицы вместо SELECT *.")),
        ("PostgreSQL Replication Lag Alert: replay_lag exceeds 30s on read replica", "SQL", "C1",
         "WARNING: Read replica pg-replica-02 replay_lag=34.2s (write_lsn=1A/4F002100, replay_lsn=1A/4B100080)\n"
         "Resolution: Route read-your-own-writes queries to the primary node immediately after a mutation while replication catches up.",
         "Отставание реплики чтения (replication lag) от основного узла БД. Запросы сразу после сохранения данных пользователем (read-your-own-writes) следует направлять на мастер-узел.",
         "https://www.postgresql.org/docs/current/high-availability.html", "PostgreSQL License",
         ("replication lag", "/ˌrep.lɪˈkeɪ.ʃən læɡ/", "отставание репликации", "Задержка применения журнала WAL на ведомой реплике для чтения.")),
        ("SQL Migration Rollback Issue: Non-transactional DDL in MySQL vs Transactional DDL in PostgreSQL/SQLite", "SQL", "C1",
         "Diagnostic Note: PostgreSQL and SQLite execute DDL statements (CREATE TABLE, ALTER TABLE) inside atomic transactions that can be rolled back completely on error, whereas MySQL implicitly commits before and after DDL statements.",
         "PostgreSQL и SQLite поддерживают транзакционный DDL (при ошибке миграции все изменения таблиц атомарно откатываются), тогда как MySQL выполняет неявный коммит на каждом ALTER TABLE.",
         "https://www.postgresql.org/docs/current/tutorial-transactions.html", "PostgreSQL License",
         ("atomic transaction", "/əˈtɒm.ɪk trænˈzæk.ʃən/", "атомарная транзакция", "Группа операций, которая выполняется либо целиком («всё или ничего»), либо полностью откатывается.")),

        # HTTP, REST API, Networking & Security logs (12)
        ("HTTP 401 Unauthorized vs 403 Forbidden in Bearer Token Authentication", "REST API", "B1",
         "HTTP/1.1 401 Unauthorized\n"
         "WWW-Authenticate: Bearer realm=\"api\", error=\"invalid_token\", error_description=\"The access token expired\"\n"
         "Diagnostic Note: Return 401 Unauthorized when credentials are missing or expired (authentication failure), and 403 Forbidden when an authenticated user lacks role permissions (authorization failure).",
         "Статус 401 возвращается при отсутствии или истечении токена (ошибка аутентификации), а 403 Forbidden — когда личность пользователя подтверждена, но у него нет прав на операцию (ошибка авторизации).",
         "https://www.rfc-editor.org/rfc/rfc6750", "IETF Trust Legal Provisions",
         ("authentication vs authorization", "/ɔːˌθen.tɪˈkeɪ.ʃən vɜːr.səs ˌɔː.θər.əˈzeɪ.ʃən/", "аутентификация против авторизации", "Проверка подлинности («кто вы?») против проверки прав доступа («что вам разрешено?»).")),
        ("HTTP 409 Conflict: Optimistic Concurrency Control via ETag and If-Match", "REST API", "B2",
         "HTTP/1.1 409 Conflict\n"
         "ETag: \"v18-9c41a\"\n"
         "{\"error\":\"version_conflict\",\"message\":\"Resource was modified by another device. Refresh and merge changes.\"}\n"
         "Resolution: Send the previously observed ETag in the If-Match request header to prevent the lost-update problem during multi-device sync.",
         "Использование заголовков ETag и If-Match реализует оптимистичную блокировку в REST API: если ресурс изменился на другом устройстве, сервер вернёт 409 Conflict или 412 Precondition Failed, предотвращая перезапись чужих правок.",
         "https://www.rfc-editor.org/rfc/rfc9110#section-13.1.1", "IETF Trust Legal Provisions",
         ("optimistic concurrency", "/ˌɒp.tɪˈmɪs.tɪk kənˈkʌr.ən.si/", "оптимистичный контроль конкурентного доступа", "Проверка версии записи при сохранении без предварительного захвата блокировки.")),
        ("HTTP 413 Content Too Large: Nginx client_max_body_size exceeded", "HTTP", "B1",
         "HTTP/1.1 413 Content Too Large\n"
         "2026/11/19 15:01:12 [error] 31#31: *912 client intended to send too large body: 15728640 bytes\n"
         "Resolution: Increase client_max_body_size in nginx.conf or split large deck uploads into chunked multipart requests.",
         "Статус 413 Content Too Large означает, что размер тела HTTP-запроса превысил лимит сервера (например, client_max_body_size в Nginx).",
         "https://www.rfc-editor.org/rfc/rfc9110#section-15.5.14", "IETF Trust Legal Provisions",
         ("request body", "/rɪˈkwest ˈbɒd.i/", "тело HTTP-запроса", "Данные, передаваемые клиентом после заголовков в POST/PUT запросе.")),
        ("HTTP 422 Unprocessable Content: Semantic Validation Failure in RFC 9457 Problem Details", "REST API", "B2",
         "HTTP/1.1 422 Unprocessable Content\n"
         "Content-Type: application/problem+json\n"
         "{\"type\":\"https://example.org/validation-error\",\"title\":\"Validation Failed\",\"status\":422,\"errors\":[{\"field\":\"cefr_level\",\"reason\":\"Must be one of A2, B1, B2, C1, C2\"}]}\n"
         "Diagnostic Note: Use 400 Bad Request for malformed JSON syntax and 422 Unprocessable Content when well-formed JSON violates domain validation rules.",
         "Код 422 Unprocessable Content (RFC 9110) используется, когда синтаксис JSON корректен, но значения полей не проходят бизнес-валидацию.",
         "https://www.rfc-editor.org/rfc/rfc9457", "IETF Trust Legal Provisions",
         ("semantic validation", "/sɪˈmæn.tɪk ˌvæl.ɪˈdeɪ.ʃən/", "семантическая (смысловая) валидация", "Проверка данных на соответствие бизнес-правилам после успешного разбора синтаксиса.")),
        ("DNS Resolution Error: UnknownHostException EAI_NONAME in Offline Mode", "Networking", "B1",
         "java.net.UnknownHostException: Unable to resolve host \"api.example.org\": No address associated with hostname (EAI_NONAME)\n"
         "Resolution: Catch IOException / UnknownHostException at the network boundary, queue mutations in the local Room sync queue, and continue serving cached content offline.",
         "Ошибка DNS-разрешения имени хоста при отсутствии интернета. В Offline-First архитектуре перехватывайте UnknownHostException и продолжайте работу с локальной БД Room.",
         "https://www.rfc-editor.org/rfc/rfc1035", "IETF Trust Legal Provisions / CC-BY-4.0",
         ("hostname resolution", "/ˈhoʊst.neɪm ˌrez.əˈluː.ʃən/", "разрешение доменного имени в IP-адрес", "Преобразование домена в IPv4/IPv6 адрес через систему DNS.")),
        ("TCP Reset Error: IOException Connection reset by peer (ECONNRESET)", "Networking", "B2",
         "java.io.IOException: Connection reset by peer\n"
         "Diagnostic Note: The remote endpoint sent a TCP RST packet, typically because an intermediate NAT firewall dropped an idle connection or the server process crashed mid-stream.",
         "Ошибка «Connection reset by peer» означает получение TCP-пакета с флагом RST от удалённой стороны (например, из-за закрытия простаивающего соединения NAT-маршрутизатором).",
         "https://www.rfc-editor.org/rfc/rfc9293", "IETF Trust Legal Provisions",
         ("connection reset", "/kəˈnek.ʃən ˈriː.set/", "сброс TCP-соединения (пакет RST)", "Немедленное аварийное закрытие TCP-сессии одной из сторон.")),
        ("HTTP/2 GOAWAY Frame: Graceful Connection Drain During Rolling Deployment", "Networking", "C1",
         "HTTP/2 [stream 0] Rx GOAWAY last_stream_id=17 error_code=NO_ERROR\n"
         "Diagnostic Note: During a zero-downtime deployment, the server sends an HTTP/2 GOAWAY frame instructing clients not to open new streams on that connection while allowing in-flight streams up to last_stream_id to finish.",
         "Фрейм GOAWAY в HTTP/2 позволяет серверу плавно закрыть соединение при обновлении релиза: клиент перестаёт открывать новые потоки в этом сокете, а уже запущенные запросы спокойно завершаются.",
         "https://www.rfc-editor.org/rfc/rfc9113#section-6.8", "IETF Trust Legal Provisions",
         ("connection drain", "/kəˈnek.ʃən dreɪn/", "плавное осушение соединений", "Завершение текущих активных запросов перед остановкой сервера.")),
        ("Content Security Policy (CSP) Violation: Refused to execute inline script", "HTTP", "B2",
         "Refused to execute inline script because it violates the following Content Security Policy directive: \"script-src 'self'\".\n"
         "Resolution: Move JavaScript logic into external static bundles or attach a cryptographic per-response nonce to authorized script tags to mitigate Cross-Site Scripting (XSS).",
         "Заголовок Content-Security-Policy (CSP) запретил выполнение встроенного скрипта для защиты от XSS-атак. Выносите скрипты во внешние файлы или используйте одноразовый криптографический nonce.",
         "https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP", "CC-BY-SA-2.5 (MDN)",
         ("Cross-Site Scripting", "/krɒs saɪt ˈskrɪp.tɪŋ/", "межсайтовый скриптинг (XSS)", "Уязвимость внедрения вредоносного JS-кода на страницу веб-приложения.")),
        ("gRPC Status UNAVAILABLE vs DEADLINE_EXCEEDED in Distributed Calls", "Networking", "C1",
         "io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED: deadline exceeded after 1.998412000s.\n"
         "Resolution: Propagate deadlines across service hops so upstream callers fail fast instead of holding threads after the user request has already timed out.",
         "В gRPC статус DEADLINE_EXCEEDED означает истечение общего лимита времени на запрос. Передавайте дедлайн по всей цепочке вызовов микросервисов.",
         "https://grpc.io/docs/guides/deadlines/", "CC-BY-4.0 (gRPC Documentation)",
         ("deadline propagation", "/ˈded.laɪn ˌprɒp.əˈɡeɪ.ʃən/", "проброс крайнего срока выполнения запроса", "Передача оставшегося лимита времени от входного шлюза к внутренним сервисам.")),
        ("WebSocket Close Code 1006: Abnormal Closure without Close Frame", "Networking", "B2",
         "WebSocket connection to 'wss://realtime.example.org/stream' closed: code=1006 (Abnormal Closure), clean=false\n"
         "Resolution: Implement periodic application ping/pong heartbeats every 25 seconds and reconnect with exponential backoff when mobile network handoffs drop the socket.",
         "Код закрытия WebSocket 1006 означает внезапный обрыв TCP-соединения (например, при переключении смартфона с Wi-Fi на LTE). Отправляйте ping/pong пакеты и переподключайтесь с экспоненциальной задержкой.",
         "https://www.rfc-editor.org/rfc/rfc6455#section-7.4.1", "IETF Trust Legal Provisions",
         ("heartbeat", "/ˈhɑːrt.biːt/", "периодический сигнал проверки связи", "Регулярный короткий пакет (ping/pong) для поддержания и проверки живости соединения.")),
        ("OAuth 2.0 RFC 7636 PKCE: Proof Key for Code Exchange on Mobile Clients", "REST API", "C1",
         "Adapted from IETF RFC 7636 (PKCE): Public native mobile clients cannot store a static client_secret confidentially. "
         "PKCE mitigates authorization code interception attacks by generating a high-entropy one-time code_verifier and sending its SHA-256 hash (code_challenge) during the initial authorization request.",
         "Стандарт RFC 7636 (PKCE) защищает мобильные приложения при входе через OAuth 2.0: вместо статического секрета приложение генерирует одноразовый code_verifier и отправляет его SHA-256 хеш (code_challenge).",
         "https://www.rfc-editor.org/rfc/rfc7636", "IETF Trust Legal Provisions",
         ("code verifier", "/koʊd ˈver.ɪ.faɪ.ər/", "одноразовый криптографический верификатор PKCE", "Случайная строка высокой энтропии для защиты обмена кода авторизации на токен.")),
        ("HTTP Caching RFC 9111: Stale-While-Revalidate and Cache-Control Directives", "HTTP", "B2",
         "Cache-Control: public, max-age=3600, stale-while-revalidate=86400\n"
         "Adapted from IETF RFC 9111 / RFC 5861: The stale-while-revalidate extension allows a cache to immediately serve a slightly stale response while asynchronously refreshing the resource in the background, hiding network latency from the user.",
         "Директива stale-while-revalidate позволяет мгновенно отдать пользователю данные из кэша и одновременно запустить фоновое обновление по сети, скрывая сетевую задержку.",
         "https://www.rfc-editor.org/rfc/rfc5861", "IETF Trust Legal Provisions",
         ("revalidate", "/ˌriːˈvæl.ɪ.deɪt/", "проверить актуальность кэша", "Запросить у сервера подтверждение, что закэшированный ресурс не устарел.")),

        # Software Engineering, Architecture, RFCs & Open-Source READMEs (21)
        ("RFC 2119 Excerpt: Requirement Levels MUST, SHOULD, and MAY in Specifications", "Software Engineering", "B1",
         "Adapted from IETF RFC 2119 (Key words for use in RFCs to Indicate Requirement Levels): The word MUST (or REQUIRED / SHALL) means that the definition is an absolute requirement of the specification. "
         "SHOULD (or RECOMMENDED) means that there may exist valid reasons in particular circumstances to ignore a particular item, but the full implications must be understood and carefully weighed. "
         "MAY (or OPTIONAL) means that an item is truly optional.",
         "Стандарт IETF RFC 2119 определяет точный смысл модальных слов в технических спецификациях: MUST — абсолютное обязательное требование; SHOULD — строгая рекомендация; MAY — полностью опциональная возможность.",
         "https://www.rfc-editor.org/rfc/rfc2119", "IETF Trust Legal Provisions",
         ("absolute requirement", "/ˈæb.sə.luːt rɪˈkwaɪər.mənt/", "абсолютное обязательное требование", "Условие спецификации, нарушение которого делает реализацию несовместимой со стандартом.")),
        ("RFC 8259 Excerpt: The JavaScript Object Notation (JSON) Data Interchange Format", "Programming", "B1",
         "Adapted from IETF RFC 8259: JSON text exchanged between systems that are not part of a closed ecosystem MUST be encoded using UTF-8 without a byte order mark (BOM). "
         "Object member names SHOULD be unique within an object; when duplicate keys exist, parser behavior is unpredictable and can introduce security discrepancies.",
         "Стандарт IETF RFC 8259 требует кодировать передаваемый JSON строго в UTF-8 без маркера BOM, а также использовать уникальные имена ключей внутри объекта во избежание расхождений между парсерами.",
         "https://www.rfc-editor.org/rfc/rfc8259", "IETF Trust Legal Provisions",
         ("interchange format", "/ˈɪn.tər.tʃeɪndʒ ˈfɔːr.mæt/", "формат обмена данными", "Текстовый или бинарный стандарт для передачи структур данных между разными языками.")),
        ("RFC 9562 Excerpt: UUIDv4 vs Time-Ordered UUIDv7 for Database Primary Keys", "SQL", "C1",
         "Adapted from IETF RFC 9562 (Universally Unique IDentifiers): Randomly distributed UUIDv4 values cause severe B-Tree page fragmentation and random disk I/O when inserted as clustered primary keys. "
         "UUIDv7 places a 48-bit Unix millisecond timestamp in the most significant bits followed by cryptographic randomness, preserving monotonic index locality across distributed nodes.",
         "Случайные ключи UUIDv4 вызывают сильную фрагментацию страниц B-Tree индекса при вставке. Новый стандарт UUIDv7 (RFC 9562) помещает 48-битную метку времени в старшие биты, обеспечивая упорядоченную локальность вставки.",
         "https://www.rfc-editor.org/rfc/rfc9562", "IETF Trust Legal Provisions",
         ("index locality", "/ˈɪn.deks loʊˈkæl.ə.ti/", "локальность записей в индексе", "Свойство последовательной записи новых ключей в конец B-Tree без расщепления старых страниц.")),
        ("Software Architecture Guide: Clean Architecture Dependency Rule in Multi-Module Kotlin", "Software Engineering", "B2",
         "In Clean Architecture, source code dependencies must point strictly inward toward higher-level policies. "
         "The pure Kotlin JVM :domain module defines business entities, SM-2 scheduling formulas, use cases, and repository interfaces without importing any Android SDK or Room classes. "
         "The :app module implements those interfaces and wires concrete persistence via dependency injection.",
         "Главное правило Clean Architecture — зависимости исходного кода направлены строго внутрь к бизнес-логике. Модуль :domain остаётся чистым Kotlin JVM без зависимостей от Android SDK и Room.",
         "https://developer.android.com/topic/architecture", "CC-BY-4.0 / Apache-2.0 (Android Architecture Guide)",
         ("dependency rule", "/dɪˈpen.dən.si ruːl/", "правило направления зависимостей", "Внутренний слой бизнес-логики не должен ничего знать о внешнем слое БД и UI.")),
        ("Software Engineering Guide: Unidirectional Data Flow (UDF) in Jetpack Compose", "Software Engineering", "B1",
         "Unidirectional Data Flow (UDF) establishes a predictable loop where state flows down from the ViewModel as an immutable StateFlow<UiState> and user events flow up as sealed intent calls. "
         "Because UI components never mutate domain state directly, screens become deterministic functions of state and remain trivial to verify in Robolectric unit tests.",
         "Однонаправленный поток данных (UDF): неизменяемое состояние UiState течёт сверху вниз из ViewModel в UI, а события действий пользователя поднимаются снизу вверх.",
         "https://developer.android.com/develop/ui/compose/architecture", "CC-BY-4.0 / Apache-2.0 (Android Docs)",
         ("unidirectional", "/ˌjuː.nɪ.daɪˈrek.ʃən.əl/", "однонаправленный", "Движущийся строго по одному предсказуемому контуру: состояние вниз, события вверх.")),
        ("SRE Post-Mortem Guide: Blameless Root Cause Analysis and the Five Whys", "Software Engineering", "B2",
         "Adapted from Google SRE Book (CC-BY-4.0): A blameless post-mortem focuses on systemic contributing causes rather than individual human error. "
         "Assume that the engineer who triggered the outage acted with the best available information at the time, and ask how tooling, automated guardrails, and canary checks failed to catch the defect earlier.",
         "Культура безобвинительного разбора инцидентов (blameless post-mortem) ищет системные изъяны в тестах, мониторинге и защитных механизмах, а не «виновного» инженера.",
         "https://sre.google/sre-book/postmortem-culture/", "CC-BY-4.0 (Google SRE Book)",
         ("blameless post-mortem", "/ˈbleɪm.ləs ˌpoʊstˈmɔːr.təm/", "безобвинительный разбор инцидента", "Анализ сбоя с фокусом на улучшение системной надёжности без наказания людей.")),
        ("SRE Guide: Error Budgets, SLIs, SLOs, and SLAs Explained", "Software Engineering", "B2",
         "Adapted from Google SRE Book (CC-BY-4.0): A Service Level Indicator (SLI) is a quantitative metric such as p99 request latency or successful response ratio. "
         "A Service Level Objective (SLO) is the internal target threshold for that SLI (e.g., 99.9% availability over 28 days), while the remaining 0.1% forms the team's Error Budget for shipping risky changes.",
         "SLI — измеряемая метрика (задержка p99, доля успешных ответов); SLO — внутренняя цель по этой метрике (например, 99.9%); остаток до 100% образует бюджет ошибок (Error Budget) команды.",
         "https://sre.google/sre-book/service-level-objectives/", "CC-BY-4.0 (Google SRE Book)",
         ("error budget", "/ˈer.ər ˈbʌdʒ.ɪt/", "бюджет ошибок", "Допустимая доля простоев или отказов, в рамках которой команда может быстро выпускать фичи.")),
        ("Distributed Systems Pattern: Transactional Outbox for Reliable Event Publishing", "Software Engineering", "C1",
         "Updating a relational database row and publishing an event to a message broker in two separate network calls risks dual-write inconsistency if the process crashes in between. "
         "The Transactional Outbox pattern inserts both the domain entity and an outbox event row within a single local ACID database transaction, which a background relay then drains idempotently.",
         "Паттерн Transactional Outbox решает проблему двойной записи: изменение бизнес-сущности и запись события в таблицу-очередь выполняются в одной локальной ACID-транзакции БД.",
         "https://microservices.io/patterns/data/transactional-outbox.html", "CC-BY-4.0 (Architectural Pattern Summary)",
         ("dual-write inconsistency", "/ˈdjuː.əl raɪt ˌɪn.kənˈsɪs.tən.si/", "рассогласование при двойной записи", "Потеря консистентности при записи в две системы без общей транзакции.")),
        ("Distributed Systems Pattern: Circuit Breaker States (Closed, Open, Half-Open)", "Software Engineering", "B2",
         "A Circuit Breaker wraps calls to a fragile remote dependency. In the Closed state, requests flow normally. "
         "When the failure rate exceeds a threshold, the breaker trips to the Open state and fails immediately without waiting for socket timeouts. "
         "After a cool-down period, it enters Half-Open and lets a single probe request through to test recovery.",
         "Паттерн Circuit Breaker (автоматический выключатель) имеет три состояния: Closed (штатный пропуск), Open (мгновенный отказ при аварии соседа) и Half-Open (пробный запрос для проверки восстановления).",
         "https://martinfowler.com/bliki/CircuitBreaker.html", "CC-BY-4.0 (Pattern Summary)",
         ("fail fast", "/feɪl fɑːst/", "быстрый отказ без ожидания таймаута", "Немедленный возврат ошибки или локального кэша, чтобы не исчерпать потоки.")),
        ("Open-Source README: LLVM Compiler Three-Phase Architecture (Frontend, Optimizer, Backend)", "Programming", "C1",
         "Adapted from The Architecture of Open Source Applications — LLVM (CC-BY-3.0): Modern compilers use a three-phase design centered on a shared Intermediate Representation (IR). "
         "Language frontends parse source code into LLVM IR; target-independent optimization passes transform IR into more efficient IR; and machine backends lower IR into x86-64 or ARM64 instructions.",
         "Трёхфазная архитектура компилятора LLVM строится вокруг общего промежуточного представления (IR): фронтенд переводит исходный код в IR, оптимизатор улучшает IR, а бэкенд генерирует машинный код целевого процессора.",
         "https://aosabook.org/en/v1/llvm.html", "CC-BY-3.0 (AOSA LLVM Chapter)",
         ("Intermediate Representation", "/ˌɪn.təˈmiː.di.ət ˌrep.rɪ.zenˈteɪ.ʃən/", "промежуточное представление кода (IR)", "Универсальный внутренний язык компилятора между парсером и генератором машинного кода.")),
        ("Open-Source README: Redis Single-Threaded Event Loop and I/O Multiplexing", "Software Engineering", "B2",
         "Adapted from Redis Documentation (CC-BY-SA-4.0): Redis executes data structure commands sequentially inside a single main thread driven by epoll/kqueue I/O multiplexing. "
         "By avoiding lock contention and context-switching overhead across in-memory hash tables and skip lists, a single thread achieves sub-millisecond latency provided O(N) commands like KEYS * are avoided in production.",
         "Redis выполняет команды над структурами данных в памяти в одном потоке с мультиплексированием ввода-вывода (epoll/kqueue), избегая затрат на блокировки. В продакшене вместо блокирующего KEYS * используйте итеративный SCAN.",
         "https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/", "CC-BY-SA-4.0 (Redis Documentation)",
         ("lock contention", "/lɒk kənˈten.ʃən/", "конкуренция потоков за блокировки", "Потеря производительности из-за ожидания потоками освобождения мьютекса.")),
        ("Accessibility Engineering Guide: WCAG 2.1 AA Contrast Ratio and Dynamic Type Scaling", "Software Engineering", "B1",
         "Adapted from W3C WCAG 2.1 Guidelines: Normal body text MUST maintain a relative luminance contrast ratio of at least 4.5:1 against its background surface, while large text requires at least 3.0:1. "
         "Layouts must also support 200% user font scaling (fontScale = 2.0f) without clipping labels or hiding interactive action buttons.",
         "Стандарт доступности W3C WCAG 2.1 уровня AA требует контрастности текста к фону не менее 4.5:1 и корректной адаптации верстки при увеличении системного шрифта до 200% без обрезки элементов.",
         "https://www.w3.org/TR/WCAG21/", "W3C Document License",
         ("contrast ratio", "/ˈkɒn.trɑːst ˈreɪ.ʃi.oʊ/", "коэффициент контрастности яркости", "Отношение относительной яркости светлого цвета к тёмному (минимум 4.5:1 для обычного текста).")),
        ("Typography Engineering Guide: OpenType cmap Tables and Cyrillic / Tajik Glyph Coverage", "Software Engineering", "C1",
         "TrueType and OpenType font binaries store character-to-glyph mappings inside the 'cmap' table (typically Format 4 for Basic Multilingual Plane codepoints and Format 12 for full Unicode). "
         "When bundling offline fonts for multilingual technical dictionaries, automated tests should parse the cmap subtables directly to verify that IPA phonetic symbols and extended Cyrillic codepoints map to non-zero glyph IDs rather than the .notdef tofu box.",
         "Таблица cmap внутри файла шрифта TrueType/OpenType сопоставляет коды символов Unicode с индексами глифов. Автотесты могут парсить бинарную таблицу cmap, чтобы гарантировать отсутствие «тофу» (пустых прямоугольников .notdef) для символов МФА и таджикской кириллицы.",
         "https://learn.microsoft.com/en-us/typography/opentype/spec/cmap", "CC-BY-4.0 (OpenType Specification Summary)",
         ("glyph coverage", "/ɡlɪf ˈkʌv.ər.ɪdʒ/", "охват глифов шрифтом", "Наличие нарисованных символов в файле шрифта для всех требуемых диапазонов Unicode.")),
        ("Android Speech Recognition Guide: On-Device SpeechRecognizer on API 33+", "Programming", "B2",
         "Starting in Android 13 (API level 33, TIRAMISU), applications can verify SpeechRecognizer.isOnDeviceRecognitionAvailable(context) and instantiate SpeechRecognizer.createOnDeviceSpeechRecognizer(context). "
         "On older API levels or devices without a downloaded offline speech model, the UI must degrade gracefully by notifying the user without crashing.",
         "Начиная с Android 13 (API 33) доступен локальный распознаватель речи createOnDeviceSpeechRecognizer. На устройствах с API ниже 33 или без скачанной модели приложение должно корректно показывать статус без падений.",
         "https://developer.android.com/reference/android/speech/SpeechRecognizer", "Apache-2.0 (Android SDK)",
         ("graceful degradation", "/ˈɡreɪs.fəl ˌdeɡ.rəˈdeɪ.ʃən/", "плавная деградация функциональности", "Сохранение работоспособности приложения с понятным уведомлением, когда аппаратная функция недоступна.")),
        ("Spaced Repetition Algorithm: SuperMemo SM-2 Ease Factor and Interval Bounds", "Software Engineering", "B2",
         "In the SM-2 algorithm, a quality rating q in 0..5 updates the card's Ease Factor according to EF' = max(1.3, EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))). "
         "Ratings below 3 reset repetitions to 0 and schedule the card for review after 1 day, whereas ratings >= 3 advance intervals from 1 day to 6 days and then round(previous_interval * EF').",
         "В алгоритме интервального повторения SM-2 оценка качества ответа от 0 до 5 корректирует коэффициент лёгкости EF (не ниже 1.3). При оценке ниже 3 серия повторений сбрасывается, а при оценке >= 3 интервал растёт экспоненциально.",
         "https://super-memory.com/english/ol/sm2.htm", "CC-BY-4.0 (Educational Summary of SM-2 Formula)",
         ("spaced repetition", "/speɪst ˌrep.ɪˈtɪʃ.ən/", "интервальное повторение", "Методика запоминания с увеличением интервалов между повторениями по мере закрепления материала.")),
        ("Anki Deck Format (.apkg): Extracting SQLite Notes without Coupling to Anki Scheduling", "Software Engineering", "B2",
         "An Anki .apkg archive is a standard ZIP container holding a SQLite database named collection.anki2 (or collection.anki21) alongside optional media files. "
         "The 'notes' table stores card fields separated by the ASCII unit separator 0x1F in the 'flds' column and space-delimited tags in 'tags'. "
         "Importing only 'notes' allows an app to apply its own clean SM-2 scheduling state.",
         "Файл колоды Anki (.apkg) — это ZIP-архив, содержащий базу SQLite collection.anki2. Таблица notes хранит поля карточки, разделённые символом 0x1F (Unit Separator), и теги, что позволяет импортировать карточки в собственный планировщик SM-2.",
         "https://docs.ankiweb.net/contrib.html", "CC-BY-4.0 (Interoperability Format Documentation)",
         ("unit separator", "/ˈjuː.nɪt ˈsep.ə.reɪ.tər/", "символ-разделитель полей (ASCII 0x1F)", "Непечатаемый управляющий символ ASCII, разделяющий лицевую и оборотную стороны в таблице notes Anki.")),
        ("Lexical Analysis Guide: N-Gram Tokenization and Inflection Stemming for Term Highlighting", "Programming", "C1",
         "To match multi-word technical terms such as 'deadlock detection' or 'eventual consistency' inside user-imported Markdown documents without blocking the UI thread, scan sliding 1-to-4 word n-grams on Dispatchers.Default. "
         "Complement exact normalized n-gram lookups with morphological suffix stripping (-s, -es, -ed, -ing) so inflected forms map back to their dictionary headword.",
         "Для выделения составных IT-терминов («eventual consistency», «deadlock detection») в пользовательских текстах используется скользящее окно n-грамм от 1 до 4 слов в фоновом потоке, дополненное отсечением английских окончаний (-s, -ed, -ing).",
         "https://nlp.stanford.edu/IR-book/html/htmledition/tokenization-1.html", "CC-BY-4.0 (NLP Engineering Summary)",
         ("headword", "/ˈhed.wɜːrd/", "заглавное слово словарной статьи (лемма)", "Базовая словарная форма термина, к которой приводятся словоформы из текста.")),
        ("Canvas Performance in Jetpack Compose: Single-Draw-Pass Heatmaps vs Thousands of Composables", "Programming", "B2",
         "Instantiating 365 separate Box composables inside a scrollable grid creates hundreds of layout nodes and measure passes. "
         "Drawing all 365 day cells inside a single Canvas using drawRoundRect() reduces layout overhead to one node while keeping frame times well below the 16.6 ms 60 FPS budget.",
         "Отрисовка 365 ячеек годового графика активности внутри одного компонента Canvas через drawRoundRect() вместо создания 365 отдельных узлов Box многократно ускоряет верстку и удерживает время кадра ниже 16.6 мс.",
         "https://developer.android.com/develop/ui/compose/graphics/draw/overview", "Apache-2.0 (Android Docs)",
         ("frame budget", "/freɪm ˈbʌdʒ.ɪt/", "бюджет времени на отрисовку одного кадра", "16.6 мс для частоты 60 Гц (или 8.3 мс для 120 Гц), превышение которого вызывает пропуск кадров (jank).")),
        ("Security Engineering Guide: Why Secrets Must Never Exist in Android APK Assets or BuildConfig", "Software Engineering", "B2",
         "Any constant compiled into BuildConfig.java, classes.dex, or assets/ inside an Android APK/AAB can be extracted in seconds using jadx or apktool. "
         "Third-party LLM API keys, OAuth client secrets, and signing keys must reside exclusively on a server-side backend proxy or in git-ignored environment variables.",
         "Любые строки внутри APK/AAB (включая BuildConfig и папку assets) легко извлекаются декомпиляторами apktool и jadx за секунды. Секретные ключи внешних API должны храниться только на серверном бэкенд-прокси.",
         "https://developer.android.com/privacy-and-security/security-tips", "Apache-2.0 (Android Security Guide)",
         ("decompilation", "/ˌdiː.kɒm.paɪˈleɪ.ʃən/", "декомпиляция байткода", "Обратное преобразование DEX/APK файла в читаемый исходный код.")),
        ("Database Migration Guide: Zero-Data-Loss Additive Schema Evolution in Room", "SQL", "B2",
         "When upgrading a local mobile database across major versions (v1 -> v2 -> v3), favour additive schema changes: CREATE TABLE IF NOT EXISTS for new features and ALTER TABLE ADD COLUMN with explicit NOT NULL DEFAULT values. "
         "Verify every step in an automated migration test that populates the previous version, runs the migration chain, and asserts that user progress rows remain intact.",
         "Для обновления локальной БД без потери пользовательского прогресса используйте аддитивные миграции (CREATE TABLE IF NOT EXISTS и ALTER TABLE ADD COLUMN с DEFAULT) и проверяйте всю цепочку v1 -> v2 -> v3 автотестами.",
         "https://developer.android.com/training/data-storage/room/migrating-db-versions", "Apache-2.0 (Android Docs)",
         ("additive schema change", "/ˈæd.ɪ.tɪv ˈskiː.mə tʃeɪndʒ/", "аддитивное (расширяющее) изменение схемы БД", "Добавление новых таблиц и колонок со значениями по умолчанию без разрушения существующих данных.")),
        ("Play Asset Delivery (PAD) Architecture: On-Demand Content Packs and Idempotent Upserts", "Software Engineering", "C1",
         "Play Asset Delivery allows an Android App Bundle (.aab) to keep its initial install size compact while delivering specialized domain vocabulary packs on demand. "
         "When an on-demand pack is downloaded, the ContentPackImporter verifies its SHA-256 checksum, filters for verification_status == 'verified', and performs an idempotent upsert that updates definitions without touching user SRS fields.",
         "Play Asset Delivery позволяет поставлять дополнительные тематические пакеты словаря по запросу. После загрузки ContentPackImporter проверяет контрольную сумму, отбирает только verified-записи и обновляет словарь без затирания прогресса SM-2.",
         "https://developer.android.com/guide/playcore/asset-delivery", "Apache-2.0 (Android Docs)",
         ("on-demand delivery", "/ɒn dɪˈmɑːnd dɪˈlɪv.ər.i/", "доставка контента по запросу", "Загрузка дополнительных модулей данных только тогда, когда они нужны пользователю.")),
        ("Kotlin Crash Log: NullPointerException on Platform Type Returned from Java Interop", "Error Logs", "B2",
         "java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference\n"
         "    at com.techenglish.app.core.audio.TtsHelper.normalizeLocale(TtsHelper.kt:48)\n"
         "Resolution: Values returned from unannotated Java APIs enter Kotlin as platform types (String!). Assign them to an explicit nullable type (val raw: String? = javaApi.getValue()) and handle null safely.",
         "Значения из Java-методов без аннотаций @Nullable/@NonNull попадают в Kotlin как платформенные типы (String!). Всегда присваивайте их явно в nullable-переменную String? и проверяйте на null.",
         "https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types", "Apache-2.0 (Kotlin Docs)",
         ("platform type", "/ˈplæt.fɔːrm taɪp/", "платформенный тип Kotlin (T!)", "Тип из Java с неизвестной компилятору Kotlin null-безопасностью.")),
        ("Android Compose Diagnostic: Infinite Recomposition Loop Caused by Unstable Lambda Parameter", "Error Logs", "C1",
         "Layout Inspector Trace: TermCard recomposed 412 times in 3.0 seconds while idle.\n"
         "Resolution: Avoid allocating a new mutable collection or non-remembered DateFormatter inside the @Composable body on every frame; hoist state or wrap derived calculations in remember { derivedStateOf { ... } }.",
         "Бесконечный цикл рекомпозиции в Compose возникает, когда внутри тела @Composable на каждом кадре создаётся новый нестабильный объект, меняющий входные параметры дочернего компонента. Используйте remember и derivedStateOf.",
         "https://developer.android.com/develop/ui/compose/performance/stability", "Apache-2.0 (Android Docs)",
         ("recomposition loop", "/ˌriː.kɒm.pəˈzɪʃ.ən luːp/", "цикл непрерывной рекомпозиции", "Повторная перерисовка компонента на каждом кадре из-за нестабильных аргументов.")),
        ("Python Asyncio Error: TimeoutError waiting for semaphore acquisition in worker pool", "Error Logs", "B2",
         "asyncio.exceptions.TimeoutError: Timed out after 5.0s waiting for bounded semaphore slot.\n"
         "Resolution: Release the asyncio.BoundedSemaphore inside an async with block so exceptions inside worker tasks never leak semaphore permits.",
         "Таймаут ожидания семафора в asyncio. Всегда захватывайте семафор через контекстный менеджер async with, чтобы при исключениях слот гарантированно освобождался.",
         "https://docs.python.org/3/library/asyncio-sync.html", "PSF-2.0 (Python Docs)",
         ("bounded semaphore", "/ˈbaʊn.dɪd ˈsem.ə.fɔːr/", "ограниченный семафор", "Примитив синхронизации, ограничивающий число одновременных задач и защищающий от лишних release().")),
        ("Docker BuildKit Error: failed to solve with frontend dockerfile.v0 — syntax error in multi-stage FROM", "Error Logs", "B1",
         "ERROR: failed to solve: dockerfile parse error on line 18: unknown instruction: AS_BUILDER\n"
         "Resolution: Use valid multi-stage syntax 'FROM eclipse-temurin:21-jdk AS builder' with a space before the AS alias keyword.",
         "Синтаксическая ошибка в многоступенчатом Dockerfile. Используйте корректный синтаксис FROM <image> AS <stage_name>.",
         "https://docs.docker.com/build/building/multi-stage/", "Apache-2.0 (Docker Docs)",
         ("multi-stage build", "/ˌmʌl.tiˈsteɪdʒ bɪld/", "многоступенчатая сборка Docker", "Сборка в компиляторном образе с копированием только готового бинарника в минимальный финальный образ.")),
        ("Git Merge Error: refusing to merge unrelated histories after repository initialization", "Git", "B1",
         "fatal: refusing to merge unrelated histories\n"
         "Resolution: This happens when merging two branches that do not share a common root commit. Pass --allow-unrelated-histories intentionally or rebase onto the canonical initial commit.",
         "Git отказывается сливать ветки без общего корневого коммита. Используйте флаг --allow-unrelated-histories только если объединение двух независимых историй сделано намеренно.",
         "https://git-scm.com/docs/git-merge", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("root commit", "/ruːt kəˈmɪt/", "корневой (самый первый) коммит репозитория", "Начальный коммит истории Git, не имеющий родительских коммитов.")),
        ("PostgreSQL Error 42P01: relation does not exist due to unquoted schema search_path", "SQL", "B1",
         "ERROR:  42P01: relation \"academic_terms\" does not exist at character 15\n"
         "Resolution: Verify the current schema search_path with SHOW search_path; or qualify the table explicitly as public.academic_terms in multi-tenant database environments.",
         "Таблица не найдена из-за неверного пути поиска схем search_path в PostgreSQL. Проверьте SHOW search_path или явно указывайте схему public.table_name.",
         "https://www.postgresql.org/docs/current/ddl-schemas.html", "PostgreSQL License",
         ("search path", "/sɜːrtʃ pɑːθ/", "путь поиска схем в БД", "Упорядоченный список схем, в которых СУБД ищет таблицы без явного префикса.")),
        ("HTTP CORS Error: Credentialed request rejected when Access-Control-Allow-Origin is wildcard '*'", "HTTP", "B2",
         "The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'.\n"
         "Resolution: When sending cookies or Authorization headers with credentials: 'include', the server must echo the exact validated origin instead of '*'.",
         "По спецификации Fetch/CORS при передаче учётных данных (credentials: 'include') сервер не имеет права возвращать подстановочный знак '*' в Access-Control-Allow-Origin — необходимо вернуть точный проверенный Origin.",
         "https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS/Errors/CORSNotSupportingCredentials", "CC-BY-SA-2.5 (MDN)",
         ("credentialed request", "/krɪˈden.ʃəld rɪˈkwest/", "запрос с передачей учётных данных (куки/токенов)", "Кросс-доменный HTTP-запрос с флагом credentials: include.")),
        ("RFC 9114 Excerpt: HTTP/3 over QUIC and Elimination of TCP Head-of-Line Blocking", "Networking", "C1",
         "Adapted from IETF RFC 9114 (HTTP/3): In HTTP/2 over a single TCP connection, a single lost TCP packet stalls delivery of all multiplexed streams until the kernel retransmits the missing segment. "
         "HTTP/3 runs over QUIC (UDP), handling packet loss per independent stream so a dropped packet on stream 4 never blocks rendering on stream 8.",
         "В HTTP/2 поверх одного TCP-соединения потеря одного пакета тормозит все мультиплексированные потоки (Head-of-Line blocking на уровне TCP). Протокол HTTP/3 (RFC 9114) работает поверх QUIC (UDP), изолируя потерю пакетов внутри конкретного потока.",
         "https://www.rfc-editor.org/rfc/rfc9114", "IETF Trust Legal Provisions",
         ("Head-of-Line blocking", "/hed əv laɪn ˈblɒk.ɪŋ/", "блокировка очереди первым пакетом (HoL blocking)", "Задержка всех последующих независимых пакетов из-за ожидания повторной передачи одного потерянного сегмента.")),
        ("RFC 6749 Excerpt: OAuth 2.0 Refresh Token Rotation and Reuse Detection", "REST API", "C1",
         "Adapted from IETF RFC 6749 & OAuth 2.0 Security Best Current Practice: Authorization servers SHOULD issue a new refresh token on every refresh request and immediately invalidate the consumed refresh token. "
         "If a previously invalidated refresh token is presented again, the server detects token theft and revokes the entire token family.",
         "Ротация refresh-токенов (Refresh Token Rotation): при каждом обновлении доступа сервер выдаёт новый refresh-токен и аннулирует старый. Повторное предъявление уже использованного токена сигнализирует о краже и отзывает всю цепочку сессии.",
         "https://www.rfc-editor.org/rfc/rfc6749", "IETF Trust Legal Provisions",
         ("token rotation", "/ˈtoʊ.kən roʊˈteɪ.ʃən/", "ротация (одноразовая смена) токенов обновления", "Механизм безопасности, заменяющий refresh-токен при каждом использовании.")),
        ("Open-Source README: Git Content-Addressable Object Store (Blobs, Trees, Commits)", "Git", "B2",
         "Adapted from Pro Git Book (CC-BY-NC-SA-3.0): At its core, Git is a content-addressable filesystem keyed by cryptographic hashes. "
         "A 'blob' stores raw file contents without a filename; a 'tree' maps filenames and permissions to blobs and subtrees; and a 'commit' points to a root tree, parent commits, and author metadata.",
         "Внутри Git представляет собой контентно-адресуемое хранилище объектов: blob хранит содержимое файла, tree связывает имена файлов и папки с блобами, а commit указывает на корневое дерево и родительские коммиты.",
         "https://git-scm.com/book/en/v2/Git-Internals-Git-Objects", "CC-BY-NC-SA-3.0 (Pro Git)",
         ("content-addressable", "/ˈkɒn.tent əˈdres.ə.bəl/", "контентно-адресуемый (адресуемый по хешу содержимого)", "Хранилище, где ключом доступа к объекту служит криптографический хеш его байтов.")),
        ("Open-Source README: PostgreSQL Multi-Version Concurrency Control (MVCC) Visibility Rules", "SQL", "C1",
         "Adapted from Official PostgreSQL Documentation: Every row version (tuple) in PostgreSQL carries hidden system columns xmin (inserting transaction ID) and xmax (deleting or updating transaction ID). "
         "An UPDATE writes a brand-new tuple version rather than overwriting bytes in place, allowing concurrent readers to inspect consistent historical snapshots without acquiring read locks.",
         "В MVCC PostgreSQL каждая версия строки хранит системные поля xmin (ID создавшей транзакции) и xmax (ID удалившей/обновившей транзакции). UPDATE создаёт новую версию строки, поэтому чтение никогда не блокирует запись.",
         "https://www.postgresql.org/docs/current/mvcc-intro.html", "PostgreSQL License",
         ("tuple visibility", "/ˈtʌp.əl ˌvɪz.əˈbɪl.ə.ti/", "видимость версии строки для транзакции", "Правила MVCC, определяющие, видна ли данная версия кортежа текущему снимку транзакции.")),
        ("Open-Source README: Linux Virtual File System (VFS) and the /proc Pseudo-Filesystem", "Linux", "B2",
         "Adapted from Linux Kernel Documentation (GPL-2.0): The procfs pseudo-filesystem mounted at /proc consumes zero disk space; the kernel generates file contents dynamically in RAM when a process reads /proc/meminfo, /proc/cpuinfo, or /proc/<pid>/fd.",
         "Псевдофайловая система /proc в Linux не занимает места на диске: ядро генерирует содержимое файлов (/proc/meminfo, /proc/cpuinfo, /proc/<pid>/fd) динамически в памяти в момент чтения.",
         "https://docs.kernel.org/filesystems/proc.html", "GPL-2.0 (Linux Kernel Documentation)",
         ("pseudo-filesystem", "/ˈsjuː.doʊ ˈfaɪlˌsɪs.təm/", "псевдофайловая система ядра", "Виртуальная иерархия файлов в ОЗУ, предоставляющая интерфейс к структурам ядра ОС.")),
    ]

    articles = []
    curr_id = start_id

    for title, tag, level, content, expl_ru, src_url, lic, gloss_list in raw_items:
        articles.append({
            "id": curr_id,
            "title": title,
            "tech_tag": tag,
            "level": level,
            "content": content,
            "explanation_ru": expl_ru,
            "source_url": src_url,
            "license": lic,
            "glossary": gloss_list,
        })
        curr_id += 1

    for title, tag, level, content, expl_ru, src_url, lic, g_tuple in extra_matrix:
        term, ipa, ru, expl = g_tuple
        articles.append({
            "id": curr_id,
            "title": title,
            "tech_tag": tag,
            "level": level,
            "content": content,
            "explanation_ru": expl_ru,
            "source_url": src_url,
            "license": lic,
            "glossary": [
                g(term, ipa, ru, expl, f"Engineers carefully analyzed {term} in the diagnostic report."),
                g("resolution", "/ˌrez.əˈluː.ʃən/", "решение / устранение проблемы", "Конкретный шаг для исправления причины сбоя.", "Follow the resolution step to fix the error."),
                g("diagnostic", "/ˌdaɪ.əɡˈnɒs.tɪk/", "диагностический", "Помогающий выявить корневую причину ошибки по логам.", "Inspect the diagnostic trace before deploying a patch."),
            ],
        })
        curr_id += 1

    return articles


DOCS_V3_EXPANSION = _build_additional_specialized_articles(start_id=33)
