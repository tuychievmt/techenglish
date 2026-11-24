# -*- coding: utf-8 -*-
"""
Common task definitions, interactive keyword explanations, and code snippets for Languages 1-10:
1: Python, 2: SQL, 3: C, 4: C++, 5: Java, 6: Kotlin, 7: C#, 8: Go, 9: JavaScript, 10: Swift
"""

TASK_META = {
    "hello_world": ("Hello World", "Вывод Hello World"),
    "variables": ("Variables & Types", "Переменные и типы"),
    "conditions": ("Conditions", "Условные конструкции"),
    "loops": ("Loops", "Циклы"),
    "functions": ("Functions", "Функции"),
    "arrays": ("Arrays & Collections", "Массивы и коллекции"),
    "classes": ("Classes & Structs", "Классы и структуры"),
    "file_reading": ("File Reading", "Чтение файла"),
    "async_ops": ("Async Operations", "Асинхронные операции"),
    "http_request": ("HTTP Request", "HTTP-запрос"),
    "error_handling": ("Error Handling", "Обработка ошибок"),
    "json_parsing": ("JSON Parsing", "Парсинг JSON"),
    "regex_matching": ("Regex Matching", "Регулярные выражения"),
    "unit_testing": ("Unit Testing", "Юнит-тестирование"),
    "singleton_pattern": ("Singleton Pattern", "Паттерн Одиночка (Singleton)"),
    "factory_pattern": ("Factory Pattern", "Паттерн Фабрика (Factory)"),
    "string_manipulation": ("String Manipulation", "Работа со строками"),
    "sorting": ("Sorting", "Сортировка"),
    "recursion": ("Recursion", "Рекурсия"),
    "generics": ("Generics & Templates", "Дженерики и шаблоны"),
}

# Rich reusable keyword definitions referenced in Code Breakdown interactive popups
KEYWORD_CATALOG = {
    "def": ("Defines a function or method block.", "Объявляет функцию или метод.", "def greet(name): return f'Hi {name}'"),
    "async": ("Marks a function as asynchronous so it can suspend execution without blocking the thread.", "Помечает функцию как асинхронную, позволяя приостанавливать выполнение без блокировки потока.", "async def fetch_user(): ..."),
    "await": ("Suspends execution until the awaited coroutine, Future, or Promise completes.", "Приостанавливает выполнение до завершения асинхронной операции (корутины или промиса).", "data = await fetch_data()"),
    "yield": ("Produces a value lazily from a generator function while preserving execution state.", "Возвращает очередное значение из генератора с сохранением его локального состояния.", "def count(): yield 1; yield 2"),
    "with": ("Context manager ensuring resources like files or locks are automatically released.", "Контекстный менеджер, гарантирующий автоматическое закрытие файлов и освобождение ресурсов.", "with open('log.txt') as f: data = f.read()"),
    "class": ("Declares a blueprint defining state (fields) and behavior (methods) for objects.", "Объявляет класс — шаблон с полями и методами для создания объектов.", "class User: pass"),
    "return": ("Exits the current function and sends a result value back to the caller.", "Завершает выполнение функции и возвращает результат вызывающей стороне.", "return total * tax"),
    "for": ("Iterates sequentially over a range, collection, or condition-controlled loop.", "Цикл для последовательного перебора диапазона или коллекции элементов.", "for item in items: process(item)"),
    "if": ("Evaluates a boolean condition and executes a branch only when true.", "Проверяет логическое условие и выполняет ветку кода при его истинности.", "if status == 200: handle_ok()"),
    "SELECT": ("Retrieves specified columns or expressions from one or more relational tables.", "Выбирает указанные столбцы или вычисляемые выражения из таблиц БД.", "SELECT id, term FROM words;"),
    "JOIN": ("Combines rows from two or more tables based on a related column condition.", "Объединяет строки из нескольких таблиц по условию совпадения ключевых столбцов.", "SELECT w.term FROM words w INNER JOIN categories c ON w.category_id = c.id;"),
    "WHERE": ("Filters query rows so only records matching the predicate are returned.", "Фильтрует строки запроса, оставляя только записи, удовлетворяющие условию.", "SELECT * FROM words WHERE repetitions = 0;"),
    "GROUP BY": ("Aggregates rows sharing identical column values for summary functions like COUNT or SUM.", "Группирует строки с одинаковыми значениями для вычисления агрегатов (COUNT, SUM).", "SELECT category_id, COUNT(*) FROM words GROUP BY category_id;"),
    "CASE": ("Evaluates conditional branches inline inside a SQL expression.", "Выполняет условное ветвление непосредственно внутри SQL-выражения.", "CASE WHEN score >= 80 THEN 'Pass' ELSE 'Retry' END"),
    "volatile": ("Prevents the compiler from caching a variable in a register when hardware or another thread may mutate it.", "Запрещает компилятору кэшировать переменную в регистре при изменении извне (железо/потоки).", "volatile int status_flag = 0;"),
    "struct": ("Groups related fields into a contiguous composite data type.", "Объединяет связанные поля в единый составной тип данных (структуру).", "struct Point { int x; int y; };"),
    "pointers": ("Variables storing direct memory addresses, enabling low-level access and zero-copy indirection.", "Переменные, хранящие прямые адреса памяти для эффективного низкоуровневого доступа.", "int *ptr = &value;"),
    "malloc": ("Allocates raw uninitialized heap memory of a specified byte size in C.", "Выделяет блок неинициализированной памяти заданного размера в куче.", "int *buf = malloc(10 * sizeof(int));"),
    "free": ("Releases previously heap-allocated memory back to the allocator to prevent leaks.", "Освобождает ранее выделенную память в куче для предотвращения утечек.", "free(buf);"),
    "const": ("Declares a value or reference immutable after initialization.", "Объявляет значение, ссылку или метод неизменяемым после инициализации.", "const int maxRetries = 3;"),
    "auto": ("Asks the C++ compiler to deduce a variable's static type automatically from its initializer.", "Поручает компилятору C++ автоматически вывести статический тип из инициализатора.", "auto count = items.size();"),
    "std::async": ("Launches a C++ task asynchronously and returns a std::future holding its eventual result.", "Запускает задачу асинхронно в C++ и возвращает std::future с будущим результатом.", "auto fut = std::async(std::launch::async, compute);"),
    "public": ("Access modifier making a class, method, or field accessible from any package or module.", "Модификатор доступа, открывающий класс, метод или поле для внешнего использования.", "public class Service { }"),
    "final": ("Prevents reassignment of a variable, overriding of a method, or subclassing of a class.", "Запрещает переприсваивание переменной, переопределение метода или наследование класса.", "final int retryLimit = 3;"),
    "CompletableFuture": ("Java asynchronous computation handle supporting non-blocking pipelines and composition.", "Класс Java для неблокирующих асинхронных вычислений и цепочек преобразований.", "CompletableFuture.supplyAsync(() -> fetch())"),
    "val": ("Declares a read-only (immutable reference) property or local variable in Kotlin/Scala.", "Объявляет неизменяемую ссылку (только для чтения) в Kotlin и Scala.", "val easeFactor = 2.5f"),
    "fun": ("Declares a function, method, or extension function in Kotlin.", "Объявляет функцию или функцию-расширение в Kotlin.", "fun add(a: Int, b: Int) = a + b"),
    "suspend": ("Marks a Kotlin function capable of pausing and resuming inside a coroutine without blocking a thread.", "Помечает функцию Kotlin, которую можно приостанавливать внутри корутины без блокировки потока.", "suspend fun loadWords(): List<Word>"),
    "data": ("Generates equals, hashCode, toString, and copy automatically for value-holding classes in Kotlin.", "Автоматически генерирует equals, hashCode, toString и copy для классов-моделей в Kotlin.", "data class Term(val name: String)"),
    "Task": ("Represents an asynchronous operation in .NET/C# or Swift structured concurrency.", "Представляет асинхронную операцию в C# (.NET) или структурной конкурентности Swift.", "await Task.Delay(100);"),
    "using": ("Imports namespaces or disposes IDisposable resources automatically at scope exit in C#.", "Импортирует пространство имён или гарантированно освобождает ресурс IDisposable в C#.", "using var reader = new StreamReader(path);"),
    "goroutines": ("Lightweight concurrent execution units multiplexed onto OS threads by the Go runtime scheduler.", "Сверхлёгкие потоки выполнения в Go, распределяемые планировщиком рантайма поверх потоков ОС.", "go processOrder(orderId)"),
    "defer": ("Schedules a function call to execute right before the surrounding function returns, ideal for cleanup.", "Откладывает вызов функции до момента выхода из текущей функции — идеально для закрытия ресурсов.", "defer resp.Body.Close()"),
    "chan": ("Typed Go channel enabling safe message passing and synchronization between goroutines.", "Типизированный канал Go для безопасного обмена данными между горутинами.", "ch := make(chan string, 1)"),
    "func": ("Declares a function or method in Go or Swift.", "Объявляет функцию или метод в Go и Swift.", "func sum(a, b int) int { return a + b }"),
    "Promise": ("JavaScript object representing the eventual completion or failure of an asynchronous operation.", "Объект JavaScript, представляющий будущий результат или ошибку асинхронной операции.", "new Promise(resolve => setTimeout(resolve, 100))"),
    "fetch": ("Standard Web & Node.js asynchronous API for issuing HTTP requests.", "Стандартный асинхронный API браузера и Node.js для выполнения HTTP-запросов.", "const res = await fetch(url);"),
    "guard": ("Swift early-exit statement enforcing preconditions and safely unwrapping optionals.", "Оператор раннего выхода в Swift, проверяющий условие и безопасно разворачивающий Optional.", "guard let name = user.name else { return }"),
    "let": ("Declares an immutable binding (Swift/Rust/Scala) or block-scoped variable (JavaScript).", "Объявляет неизменяемую привязку (Swift/Rust) или переменную с блочной областью (JS).", "let threshold = 1.3"),
    "trait": ("Defines shared behavior interfaces implemented by types in Rust and Scala.", "Определяет интерфейс общего поведения (типаж), реализуемый типами в Rust и Scala.", "trait Printable { fn show(&self); }"),
    "match": ("Exhaustive pattern-matching control structure checking all possible variants at compile time.", "Конструкция исчерпывающего сопоставления с образцом, проверяемая компилятором.", "match status { 200 => \"OK\", _ => \"Err\" }"),
    "fn": ("Declares a function in Rust.", "Объявляет функцию в Rust.", "fn square(x: i32) -> i32 { x * x }"),
    "impl": ("Attaches methods or trait implementations to a struct or enum in Rust.", "Реализует методы или трейты для структуры или перечисления в Rust.", "impl User { fn is_active(&self) -> bool { true } }"),
    "mut": ("Explicitly opts a Rust variable or reference into mutability (variables are immutable by default).", "Явно разрешает изменение переменной или ссылки в Rust (по умолчанию всё неизменяемо).", "let mut count = 0;"),
    "Future": ("Represents a deferred asynchronous result in Dart or Scala.", "Представляет отложенный асинхронный результат в Dart или Scala.", "Future<String> load() async => 'ready';"),
    "local": ("Scopes a variable strictly to the current block or function in Lua and Bash.", "Ограничивает область видимости переменной текущим блоком или функцией в Lua и Bash.", "local count = 10"),
    "Dim": ("Declares variables and allocates storage space in VBA.", "Объявляет переменные и их типы данных в VBA.", "Dim total As Double"),
    "mov": ("x86-64 Assembly instruction copying data between registers, immediates, and memory.", "Инструкция ассемблера x86-64 для копирования данных между регистрами и памятью.", "mov rax, 1"),
    "syscall": ("Invokes an operating system kernel service directly from user-space Assembly code.", "Вызывает системный сервис ядра ОС напрямую из кода на ассемблере.", "syscall ; Linux write/exit"),
    "begin": ("Starts a compound statement block in Pascal (paired with end).", "Открывает составной блок операторов в Pascal (в паре с end).", "begin WriteLn('Hi'); end;"),
    "yield_ruby": ("Invokes an attached block passed to a Ruby method for clean iteration and DSLs.", "Вызывает переданный в метод Ruby блок кода.", "def wrap; yield; end"),
}

def make_snippet(lang_id, task_key, code, expl_en, expl_ru, lines, kw_keys):
    title_en, title_ru = TASK_META[task_key]
    line_objs = [{"line": idx + 1, "en": en, "ru": ru} for idx, (en, ru) in enumerate(lines)]
    kw_objs = []
    for k in kw_keys:
        display_kw = "yield" if k == "yield_ruby" else k
        en, ru, ex = KEYWORD_CATALOG[k]
        kw_objs.append({"keyword": display_kw, "en": en, "ru": ru, "example": ex})
    return {
        "language_id": lang_id,
        "task_key": task_key,
        "task_title_en": title_en,
        "task_title_ru": title_ru,
        "code": code.strip("\n"),
        "explanation_en": expl_en,
        "explanation_ru": expl_ru,
        "line_explanations": line_objs,
        "keywords": kw_objs,
    }


SNIPPETS_PART1 = [
    # ==================== 1. PYTHON (10 tasks) ====================
    make_snippet(
        1, "hello_world",
        """def main() -> None:
    message = "Hello, Technical English!"
    print(message)

if __name__ == "__main__":
    main()""",
        "Defines an entry-point function with a type annotation and prints a formatted greeting when executed directly.",
        "Определяет точку входа с аннотацией типа и выводит приветствие при прямом запуске скрипта.",
        [
            ("Declare a typed function returning None.", "Объявляем функцию main с аннотацией возврата None."),
            ("Assign a UTF-8 string literal to a local variable.", "Присваиваем строковый литерал локальной переменной."),
            ("Print the string to standard output.", "Выводим строку в стандартный поток вывода."),
            ("Blank line separating function and guard.", "Пустая строка для читаемости (PEP 8)."),
            ("Ensure main() runs only when invoked directly, not imported.", "Запускаем main() только при прямом запуске модуля."),
            ("Invoke the main entry point.", "Вызываем функцию main()."),
        ],
        ["def", "if"]
    ),
    make_snippet(
        1, "variables",
        """service_name: str = "auth-api"
retry_count: int = 3
ease_factor: float = 2.5
is_offline: bool = True
print(f"{service_name}: retries={retry_count}, EF={ease_factor}")""",
        "Demonstrates dynamic variables annotated with Python type hints and f-string interpolation.",
        "Показывает объявление переменных с подсказками типов (type hints) и интерполяцией f-строк.",
        [
            ("String variable with explicit str hint.", "Строковая переменная с подсказкой типа str."),
            ("Integer variable holding retry limit.", "Целочисленная переменная для лимита повторов."),
            ("Floating-point variable for SM-2 ease factor.", "Вещественная переменная для коэффициента лёгкости SM-2."),
            ("Boolean flag indicating offline mode.", "Булев флаг автономного режима."),
            ("Format variables inline using an f-string.", "Форматируем строку с подстановкой значений через f-string."),
        ],
        []
    ),
    make_snippet(
        1, "conditions",
        """def classify_status(code: int) -> str:
    if 200 <= code < 300:
        return "Success"
    elif 400 <= code < 500:
        return "Client Error"
    else:
        return "Server Error\"""",
        "Uses chained numeric comparisons and if/elif/else branching to classify HTTP status codes.",
        "Использует цепочку сравнений и ветвление if/elif/else для классификации кодов состояния HTTP.",
        [
            ("Function accepting an HTTP status integer.", "Функция принимает целочисленный HTTP-код."),
            ("Chained comparison checks 2xx success range.", "Двойное сравнение проверяет диапазон успешных кодов 2xx."),
            ("Return success label.", "Возвращаем метку успешного ответа."),
            ("Handle 4xx client errors.", "Обрабатываем ошибки клиента 4xx."),
            ("Return client error label.", "Возвращаем метку ошибки клиента."),
            ("Fallback branch for 5xx or other codes.", "Ветка по умолчанию для серверных ошибок."),
            ("Return server error label.", "Возвращаем метку ошибки сервера."),
        ],
        ["def", "if", "return"]
    ),
    make_snippet(
        1, "loops",
        """def stream_due_terms(terms: list[str]):
    for index, term in enumerate(terms, start=1):
        yield f"{index}. {term}"

for line in stream_due_terms(["idempotency", "rebase", "latency"]):
    print(line)""",
        "Combines a for-loop with enumerate and a lazy yield generator to iterate memory-efficiently.",
        "Сочетает цикл for с enumerate и ленивым генератором yield для экономичной итерации.",
        [
            ("Define a generator function accepting a list of terms.", "Определяем функцию-генератор для списка терминов."),
            ("Iterate with 1-based index via enumerate().", "Перебираем элементы с нумерацией от 1 через enumerate()."),
            ("Lazily yield each formatted line on demand.", "Лениво возвращаем каждую отформатированную строку через yield."),
            ("Blank separator line.", "Пустая строка-разделитель."),
            ("Consume values lazily from the generator.", "Потребляем элементы генератора в цикле for."),
            ("Output each yielded term.", "Выводим каждый термин."),
        ],
        ["def", "for", "yield"]
    ),
    make_snippet(
        1, "functions",
        """def calculate_ef(ef: float, quality: int) -> float:
    delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
    return max(1.3, round(ef + delta, 4))

print(calculate_ef(2.5, 4))""",
        "Pure function implementing the SM-2 ease-factor update with clamping at 1.3.",
        "Чистая функция расчёта коэффициента лёгкости SM-2 с ограничением минимума 1.3.",
        [
            ("Typed signature for SM-2 ease factor calculation.", "Сигнатура функции расчёта коэффициента SM-2."),
            ("Compute adjustment delta from quality score q.", "Вычисляем поправку delta на основе оценки q."),
            ("Clamp result so ease factor never drops below 1.3.", "Ограничиваем результат снизу значением 1.3."),
            ("Separator line.", "Разделитель."),
            ("Invoke function with EF=2.5 and q=4.", "Вызываем функцию для EF=2.5 и q=4."),
        ],
        ["def", "return"]
    ),
    make_snippet(
        1, "arrays",
        """scores = [85, 92, 74, 98, 61]
passed = [s for s in scores if s >= 80]
average = sum(passed) / len(passed)
print(f"Passed ({len(passed)}): {passed}, avg={average:.1f}")""",
        "Uses Python list comprehensions to filter passing scores and compute the mean cleanly.",
        "Использует списковое включение (list comprehension) для фильтрации оценок и расчёта среднего.",
        [
            ("Create a dynamic list of integer scores.", "Создаём динамический список оценок."),
            ("Filter elements >= 80 via list comprehension.", "Отбираем оценки >= 80 с помощью list comprehension."),
            ("Compute average using built-in sum() and len().", "Вычисляем среднее через встроенные sum() и len()."),
            ("Print formatted count, list, and 1-decimal average.", "Выводим количество, список и среднее с точностью до десятых."),
        ],
        ["for", "if"]
    ),
    make_snippet(
        1, "classes",
        """from dataclasses import dataclass

@dataclass(frozen=True)
class Flashcard:
    term: str
    translation: str

    def summary(self) -> str:
        return f"{self.term} — {self.translation}\"""",
        "Uses an immutable Python dataclass to model a vocabulary flashcard with zero boilerplate.",
        "Использует неизменяемый dataclass для компактного описания карточки словаря без шаблонного кода.",
        [
            ("Import dataclass decorator from standard library.", "Импортируем декоратор dataclass из стандартной библиотеки."),
            ("Blank line.", "Пустая строка."),
            ("Mark dataclass frozen=True for immutability.", "Делаем класс неизменяемым (frozen=True)."),
            ("Declare Flashcard class.", "Объявляем класс Flashcard."),
            ("English term attribute.", "Поле для английского термина."),
            ("Russian translation attribute.", "Поле для русского перевода."),
            ("Blank line before method.", "Пустая строка перед методом."),
            ("Instance method formatting a card summary.", "Метод экземпляра для форматирования краткого описания."),
            ("Return formatted term and translation.", "Возвращаем термин и перевод."),
        ],
        ["class", "def", "return"]
    ),
    make_snippet(
        1, "file_reading",
        """from pathlib import Path

def read_log_lines(path: str) -> list[str]:
    with Path(path).open("r", encoding="utf-8") as handle:
        return [line.strip() for line in handle if line.strip()]""",
        "Opens a UTF-8 file safely with a context manager (with) so the descriptor closes automatically.",
        "Безопасно читает UTF-8 файл через контекстный менеджер with с автоматическим закрытием дескриптора.",
        [
            ("Import Path from pathlib for cross-platform file paths.", "Импортируем Path для кроссплатформенной работы с путями."),
            ("Blank line.", "Пустая строка."),
            ("Define helper returning non-empty stripped lines.", "Определяем функцию чтения непустых строк."),
            ("Open file inside a with-block guaranteeing close().", "Открываем файл в блоке with — закрытие гарантировано."),
            ("Strip whitespace and skip blank lines.", "Удаляем пробелы по краям и пропускаем пустые строки."),
        ],
        ["def", "with", "return", "for", "if"]
    ),
    make_snippet(
        1, "async_ops",
        """import asyncio

async def fetch_metric(name: str) -> str:
    await asyncio.sleep(0.1)
    return f"{name}: OK"

async def main() -> None:
    results = await asyncio.gather(fetch_metric("db"), fetch_metric("cache"))
    print(results)""",
        "Runs concurrent non-blocking coroutines using Python's async/await and asyncio.gather.",
        "Запускает конкурентные неблокирующие корутины с помощью async/await и asyncio.gather.",
        [
            ("Import built-in asyncio event-loop library.", "Импортируем встроенную библиотеку asyncio."),
            ("Blank line.", "Пустая строка."),
            ("Declare an asynchronous coroutine function.", "Объявляем асинхронную функцию-корутину."),
            ("Suspend coroutine non-blockingly for 100 ms.", "Приостанавливаем корутину на 100 мс без блокировки потока."),
            ("Return simulated health metric.", "Возвращаем результат проверки метрики."),
            ("Blank line.", "Пустая строка."),
            ("Declare async main coordinator.", "Объявляем асинхронную точку входа."),
            ("Await two coroutines concurrently via gather().", "Параллельно ожидаем две корутины через gather()."),
            ("Print combined results list.", "Выводим список собранных результатов."),
        ],
        ["async", "await", "def", "return"]
    ),
    make_snippet(
        1, "http_request",
        """import urllib.request
import json

def fetch_json(url: str) -> dict:
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=5) as response:
        return json.loads(response.read().decode("utf-8"))""",
        "Performs a standard-library HTTP GET request with explicit timeout and JSON decoding.",
        "Выполняет HTTP GET-запрос средствами стандартной библиотеки с таймаутом и парсингом JSON.",
        [
            ("Import standard HTTP request module.", "Импортируем стандартный модуль HTTP-запросов."),
            ("Import JSON parser.", "Импортируем парсер JSON."),
            ("Blank line.", "Пустая строка."),
            ("Function fetching and parsing a JSON endpoint.", "Функция для получения и разбора JSON-ответа."),
            ("Build HTTP request with Accept header.", "Формируем запрос с заголовком Accept."),
            ("Open connection with 5-second timeout in context manager.", "Открываем соединение с таймаутом 5 сек в блоке with."),
            ("Read bytes, decode UTF-8, and parse JSON dictionary.", "Читаем тело ответа, декодируем UTF-8 и парсим JSON."),
        ],
        ["def", "with", "return"]
    ),

    # ==================== 2. SQL (7 tasks) ====================
    make_snippet(
        2, "hello_world",
        """SELECT 'Hello, Technical English!' AS greeting;""",
        "Returns a single literal text column using a declarative SELECT statement.",
        "Возвращает строковый литерал с псевдонимом столбца с помощью декларативного SELECT.",
        [
            ("Select a string constant aliased as greeting.", "Выбираем строковую константу с псевдонимом greeting."),
        ],
        ["SELECT"]
    ),
    make_snippet(
        2, "variables",
        """WITH config AS (
    SELECT 20 AS daily_goal, 2.5 AS default_ef
)
SELECT daily_goal, default_ef FROM config;""",
        "Uses a Common Table Expression (CTE) to bind named constant values cleanly inside standard SQL.",
        "Использует обобщённое табличное выражение (CTE) для объявления именованных параметров в стандартном SQL.",
        [
            ("Define a named CTE block called config.", "Создаём именованный блок CTE с именем config."),
            ("Bind constant values to named columns.", "Задаём константные значения и их имена."),
            ("Close the CTE definition.", "Закрываем определение CTE."),
            ("Query the bound values from the CTE.", "Выбираем значения из объявленного CTE."),
        ],
        ["SELECT"]
    ),
    make_snippet(
        2, "conditions",
        """SELECT term,
       CASE
           WHEN repetitions >= 3 THEN 'Mastered'
           WHEN repetitions >= 1 THEN 'Learning'
           ELSE 'New'
       END AS mastery_state
FROM words;""",
        "Classifies rows declaratively with SQL CASE WHEN conditional expressions.",
        "Классифицирует строки декларативно с помощью условного выражения SQL CASE WHEN.",
        [
            ("Select the English term column.", "Выбираем столбец английского термина."),
            ("Start a CASE conditional expression.", "Начинаем условное выражение CASE."),
            ("Check if word reached 3+ successful reviews.", "Проверяем, повторено ли слово 3 и более раз."),
            ("Check if word is currently being learned.", "Проверяем, начато ли изучение слова."),
            ("Default branch for unseen cards.", "Ветка по умолчанию для новых карточек."),
            ("Close CASE expression and alias the computed column.", "Завершаем CASE и задаём псевдоним mastery_state."),
            ("Specify the source words table.", "Указываем исходную таблицу words."),
        ],
        ["SELECT", "CASE"]
    ),
    make_snippet(
        2, "loops",
        """WITH RECURSIVE counter(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM counter WHERE n < 5
)
SELECT n FROM counter;""",
        "Replaces imperative loops in SQL using a standards-compliant RECURSIVE CTE.",
        "Заменяет императивные циклы в SQL с помощью рекурсивного CTE (WITH RECURSIVE).",
        [
            ("Declare a recursive CTE named counter.", "Объявляем рекурсивный CTE с именем counter."),
            ("Anchor member starts iteration at 1.", "Базовый шаг задаёт начальное значение 1."),
            ("Append subsequent generated rows.", "Объединяем результаты шагов через UNION ALL."),
            ("Recursive step increments n until reaching 5.", "Рекурсивный шаг увеличивает n, пока n < 5."),
            ("Close recursive definition.", "Закрываем определение CTE."),
            ("Return generated sequence 1..5.", "Возвращаем сгенерированную последовательность 1..5."),
        ],
        ["SELECT", "WHERE"]
    ),
    make_snippet(
        2, "functions",
        """SELECT c.name AS category,
       COUNT(w.id) AS total_words,
       ROUND(AVG(w.ease_factor), 2) AS avg_ef
FROM categories c
LEFT JOIN words w ON w.category_id = c.id
GROUP BY c.id;""",
        "Joins categories with words and applies COUNT, AVG, and ROUND aggregate functions per group.",
        "Соединяет категории со словами через LEFT JOIN и вычисляет агрегаты COUNT, AVG и ROUND по группам.",
        [
            ("Select category name with alias.", "Выбираем название категории."),
            ("Count words belonging to each category.", "Считаем количество слов в каждой категории."),
            ("Compute average SM-2 ease factor rounded to 2 decimals.", "Вычисляем средний EF с округлением до 2 знаков."),
            ("Start from categories table aliased as c.", "Берём таблицу categories под псевдонимом c."),
            ("Join matching words while keeping empty categories.", "Присоединяем слова через LEFT JOIN, сохраняя пустые категории."),
            ("Group rows by category primary key.", "Группируем строки по идентификатору категории."),
        ],
        ["SELECT", "JOIN", "GROUP BY"]
    ),
    make_snippet(
        2, "arrays",
        """SELECT term, translation
FROM words
WHERE category_id IN (1, 3, 6)
ORDER BY term ASC
LIMIT 10;""",
        "Filters rows against a set of IDs with IN, sorts alphabetically, and paginates with LIMIT.",
        "Фильтрует строки по множеству значений через IN, сортирует по алфавиту и ограничивает выборку LIMIT.",
        [
            ("Choose term and Russian translation columns.", "Выбираем столбцы term и translation."),
            ("Target the words table.", "Указываем таблицу words."),
            ("Match rows belonging to Backend, Git, or DevOps.", "Оставляем термины из категорий Backend, Git или DevOps."),
            ("Sort results alphabetically ascending.", "Сортируем по алфавиту по возрастанию."),
            ("Cap output to the first 10 records.", "Ограничиваем вывод первыми 10 строками."),
        ],
        ["SELECT", "WHERE"]
    ),
    make_snippet(
        2, "classes",
        """CREATE TABLE flashcards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    term TEXT NOT NULL UNIQUE,
    ease_factor REAL NOT NULL DEFAULT 2.5
);""",
        "Models an entity schema declaratively with column types, constraints, and default values.",
        "Декларативно описывает схему сущности (аналог класса) с типами столбцов, ограничениями и значениями по умолчанию.",
        [
            ("Define a new relational table schema.", "Создаём новую таблицу базы данных."),
            ("Auto-incrementing integer primary key.", "Автоинкрементный целочисленный первичный ключ."),
            ("Mandatory unique text column for the English term.", "Обязательный уникальный текстовый столбец для термина."),
            ("Floating-point SM-2 ease factor defaulting to 2.5.", "Вещественный коэффициент SM-2 со значением по умолчанию 2.5."),
            ("End table definition.", "Завершаем определение таблицы."),
        ],
        []
    ),

    # ==================== 3. C (8 tasks) ====================
    make_snippet(
        3, "hello_world",
        """#include <stdio.h>

int main(void) {
    printf("Hello, Technical English!\\n");
    return 0;
}""",
        "Includes standard I/O header and prints a null-terminated C string from main(), returning exit code 0.",
        "Подключает заголовок stdio.h и выводит C-строку из функции main(), возвращая код успеха 0.",
        [
            ("Include standard input/output declarations.", "Подключаем стандартную библиотеку ввода-вывода."),
            ("Blank separator line.", "Пустая строка."),
            ("Program entry point returning an OS exit status.", "Точка входа программы, возвращающая код возврата ОС."),
            ("Write formatted string to stdout.", "Выводим строку в стандартный вывод."),
            ("Return 0 indicating successful termination.", "Возвращаем 0 как признак успешного завершения."),
            ("Close main block.", "Закрываем тело функции main."),
        ],
        ["return"]
    ),
    make_snippet(
        3, "variables",
        """#include <stdio.h>

int main(void) {
    const int max_retries = 3;
    volatile int hw_ready = 1;
    int *ptr = (int *)&max_retries;
    printf("ready=%d ptr=%p\\n", hw_ready, (void *)ptr);
    return 0;
}""",
        "Illustrates const immutability, volatile hardware-safe variables, and explicit memory pointers in C.",
        "Демонстрирует модификаторы const и volatile, а также работу с указателями на адреса памяти в C.",
        [
            ("Include stdio.h for printf.", "Подключаем stdio.h для функции printf."),
            ("Blank line.", "Пустая строка."),
            ("Entry point.", "Точка входа."),
            ("Read-only integer constant.", "Неизменяемая целочисленная константа."),
            ("Volatile flag preventing stale compiler register caching.", "Флаг volatile запрещает компилятору кэшировать значение в регистре."),
            ("Pointer holding memory address via & operator.", "Указатель, хранящий адрес переменной через оператор &."),
            ("Print integer value and hexadecimal pointer address.", "Выводим значение и шестнадцатеричный адрес указателя."),
            ("Exit cleanly.", "Успешное завершение."),
            ("End main.", "Конец функции."),
        ],
        ["const", "volatile", "pointers", "return"]
    ),
    make_snippet(
        3, "conditions",
        """const char *classify_code(int status) {
    if (status >= 200 && status < 300) {
        return "OK";
    } else if (status >= 400 && status < 500) {
        return "Client Error";
    }
    return "Server Error";
}""",
        "Evaluates HTTP status ranges in C and returns a read-only string literal pointer.",
        "Проверяет диапазоны HTTP-кодов в C и возвращает указатель на неизменяемый строковый литерал.",
        [
            ("Function returning pointer to constant string.", "Функция возвращает указатель на константную строку."),
            ("Test 2xx range with logical AND.", "Проверяем диапазон 2xx с логическим И."),
            ("Return OK literal.", "Возвращаем литерал OK."),
            ("Test 4xx client error range.", "Проверяем диапазон клиентских ошибок 4xx."),
            ("Return Client Error literal.", "Возвращаем метку ошибки клиента."),
            ("Close else-if block.", "Закрываем блок else if."),
            ("Default return for 5xx/other codes.", "Возврат по умолчанию для остальных кодов."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["const", "if", "return"]
    ),
    make_snippet(
        3, "loops",
        """#include <stdio.h>

void print_range(int limit) {
    for (int i = 1; i <= limit; i++) {
        printf("Step %d\\n", i);
    }
}""",
        "Uses a classic C counter-controlled for-loop with explicit initialization, guard, and increment.",
        "Использует классический цикл for языка C с инициализацией, условием и инкрементом счётчика.",
        [
            ("Include stdio header.", "Подключаем заголовок stdio."),
            ("Blank line.", "Пустая строка."),
            ("Void function printing numbered steps.", "Функция печати пронумерованных шагов."),
            ("Initialize i=1, loop while i<=limit, increment each pass.", "Инициализируем i=1, повторяем пока i<=limit, увеличиваем на 1."),
            ("Print formatted step counter.", "Выводим текущий номер шага."),
            ("End loop body.", "Конец тела цикла."),
            ("End function.", "Конец функции."),
        ],
        ["for"]
    ),
    make_snippet(
        3, "functions",
        """double clamp_ease(double ef, double delta) {
    double updated = ef + delta;
    return updated < 1.3 ? 1.3 : updated;
}""",
        "Defines a compact C helper function using the ternary conditional operator to enforce a lower bound.",
        "Определяет компактную функцию C с тернарным оператором для ограничения минимального значения.",
        [
            ("Declare function accepting and returning double.", "Объявляем функцию с параметрами и возвратом double."),
            ("Compute updated ease factor.", "Вычисляем обновлённый коэффициент лёгкости."),
            ("Clamp at 1.3 via ternary operator.", "Ограничиваем снизу значением 1.3 через тернарный оператор."),
            ("End function.", "Конец функции."),
        ],
        ["return"]
    ),
    make_snippet(
        3, "arrays",
        """#include <stdlib.h>

int sum_heap_array(int n) {
    int *arr = (int *)malloc((size_t)n * sizeof(int));
    if (!arr) return -1;
    int total = 0;
    for (int i = 0; i < n; i++) { arr[i] = i + 1; total += arr[i]; }
    free(arr);
    return total;
}""",
        "Allocates a dynamic array on the heap with malloc, checks for NULL, sums elements, and releases memory via free.",
        "Выделяет динамический массив в куче через malloc, проверяет на NULL, суммирует и освобождает память через free.",
        [
            ("Include stdlib.h for malloc and free.", "Подключаем stdlib.h для malloc и free."),
            ("Blank line.", "Пустая строка."),
            ("Function summing 1..n in heap memory.", "Функция суммирования массива в куче."),
            ("Allocate n integers dynamically via malloc.", "Динамически выделяем память под n целых чисел."),
            ("Guard against allocation failure (NULL pointer).", "Проверяем успешность выделения памяти."),
            ("Initialize running sum.", "Инициализируем переменную суммы."),
            ("Populate array and accumulate total.", "Заполняем элементы и накапливаем сумму."),
            ("Free heap block to avoid memory leaks.", "Освобождаем память в куче, предотвращая утечку."),
            ("Return computed total.", "Возвращаем вычисленную сумму."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["pointers", "malloc", "if", "for", "free", "return"]
    ),
    make_snippet(
        3, "classes",
        """typedef struct {
    int id;
    const char *term;
    float ease_factor;
} WordCard;

WordCard make_card(int id, const char *term) {
    WordCard card = { id, term, 2.5f };
    return card;
}""",
        "Models structured data in procedural C using typedef struct and a factory constructor function.",
        "Моделирует составной тип данных в процедурном C с помощью typedef struct и функции-конструктора.",
        [
            ("Define named struct type alias.", "Объявляем именованную структуру через typedef."),
            ("Integer identifier field.", "Целочисленное поле идентификатора."),
            ("Pointer to constant English term string.", "Указатель на константную строку термина."),
            ("SM-2 ease factor field.", "Поле коэффициента лёгкости SM-2."),
            ("Close struct typedef as WordCard.", "Завершаем объявление типа WordCard."),
            ("Blank line.", "Пустая строка."),
            ("Constructor helper returning a WordCard by value.", "Вспомогательная функция создания карточки."),
            ("Aggregate-initialize all struct fields.", "Инициализируем поля структуры списком."),
            ("Return constructed value.", "Возвращаем созданную структуру."),
            ("End function.", "Конец функции."),
        ],
        ["struct", "const", "return"]
    ),
    make_snippet(
        3, "file_reading",
        """#include <stdio.h>

void read_file(const char *path) {
    FILE *fp = fopen(path, "r");
    if (!fp) return;
    char buf[256];
    while (fgets(buf, sizeof(buf), fp)) { printf("%s", buf); }
    fclose(fp);
}""",
        "Reads a text file line by line into a bounded stack buffer with fopen, fgets, and fclose.",
        "Построчно читает текстовый файл в буфер фиксированного размера с помощью fopen, fgets и fclose.",
        [
            ("Include stdio.h for FILE API.", "Подключаем stdio.h для работы с файлами."),
            ("Blank line.", "Пустая строка."),
            ("Accept read-only file path string.", "Принимаем путь к файлу."),
            ("Open file stream in read mode.", "Открываем файловый поток на чтение."),
            ("Exit early if file cannot be opened.", "Выходим при ошибке открытия файла."),
            ("Allocate bounded 256-byte buffer to avoid overflow.", "Выделяем буфер 256 байт для защиты от переполнения."),
            ("Read lines safely until EOF.", "Безопасно читаем строки до конца файла."),
            ("Close file handle explicitly.", "Явно закрываем файловый дескриптор."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["const", "pointers", "if", "return"]
    ),

    # ==================== 4. C++ (9 tasks) ====================
    make_snippet(
        4, "hello_world",
        """#include <iostream>

int main() {
    std::cout << "Hello, Technical English!\\n";
    return 0;
}""",
        "Streams a greeting to standard output using C++ <iostream> and returns exit status 0.",
        "Выводит приветствие в стандартный поток через <iostream> и возвращает код возврата 0.",
        [
            ("Include C++ standard I/O stream library.", "Подключаем стандартную библиотеку потоков ввода-вывода."),
            ("Blank line.", "Пустая строка."),
            ("Standard C++ entry point.", "Точка входа программы C++."),
            ("Insert string into std::cout output stream.", "Отправляем строку в поток вывода std::cout."),
            ("Return success code.", "Возвращаем код успешного завершения."),
            ("End main.", "Конец функции."),
        ],
        ["return"]
    ),
    make_snippet(
        4, "variables",
        """#include <string>

const std::string service = "search-engine";
auto retries = 3;
volatile bool stop_requested = false;""",
        "Combines RAII std::string, compile-time type deduction via auto, const immutability, and volatile.",
        "Сочетает RAII-строку std::string, автоматический вывод типа auto, const и volatile.",
        [
            ("Include C++ managed string header.", "Подключаем заголовок std::string."),
            ("Blank line.", "Пустая строка."),
            ("Immutable C++ string managed automatically.", "Неизменяемая строка с автоматическим управлением памятью."),
            ("Compiler deduces int type via auto.", "Компилятор автоматически выводит тип int через auto."),
            ("Volatile flag for low-level visibility.", "Флаг volatile для видимости изменений извне."),
        ],
        ["const", "auto", "volatile"]
    ),
    make_snippet(
        4, "conditions",
        """#include <string_view>

std::string_view classify(int status) {
    if (status >= 200 && status < 300) return "Success";
    if (status >= 400 && status < 500) return "Client Error";
    return "Server Error";
}""",
        "Returns zero-allocation std::string_view slices using guard-style if statements.",
        "Возвращает неаллоцирующие срезы std::string_view с помощью проверок if.",
        [
            ("Include non-owning string_view header (C++17).", "Подключаем заголовок std::string_view (C++17)."),
            ("Blank line.", "Пустая строка."),
            ("Return lightweight view without heap copy.", "Возвращаем лёгкое представление строки без копирования."),
            ("Early return for 2xx status codes.", "Ранний возврат для кодов 2xx."),
            ("Early return for 4xx status codes.", "Ранний возврат для кодов 4xx."),
            ("Fallback return for server errors.", "Возврат по умолчанию для серверных ошибок."),
            ("End function.", "Конец функции."),
        ],
        ["if", "return"]
    ),
    make_snippet(
        4, "loops",
        """#include <iostream>
#include <vector>

void print_terms(const std::vector<std::string>& terms) {
    for (const auto& term : terms) {
        std::cout << term << '\\n';
    }
}""",
        "Uses a modern C++ range-based for-loop over a const reference to avoid copying strings.",
        "Использует современный range-based цикл for по константной ссылке без лишнего копирования строк.",
        [
            ("Include iostream.", "Подключаем iostream."),
            ("Include dynamic vector container.", "Подключаем динамический контейнер std::vector."),
            ("Blank line.", "Пустая строка."),
            ("Pass vector by const reference (zero copy).", "Передаём вектор по константной ссылке без копирования."),
            ("Iterate over elements by const auto reference.", "Итерируем по элементам по константной ссылке auto&."),
            ("Stream each term to stdout.", "Выводим каждый термин в консоль."),
            ("End loop.", "Конец цикла."),
            ("End function.", "Конец функции."),
        ],
        ["const", "for", "auto"]
    ),
    make_snippet(
        4, "functions",
        """#include <algorithm>

double next_ease(double ef, int q) {
    double diff = 5.0 - q;
    double updated = ef + (0.1 - diff * (0.08 + diff * 0.02));
    return std::max(1.3, updated);
}""",
        "Implements the SM-2 ease-factor formula using std::max from <algorithm>.",
        "Реализует формулу коэффициента лёгкости SM-2 с помощью std::max из <algorithm>.",
        [
            ("Include standard algorithm header for std::max.", "Подключаем <algorithm> для std::max."),
            ("Blank line.", "Пустая строка."),
            ("Pure C++ function returning new ease factor.", "Чистая функция расчёта нового EF."),
            ("Compute (5 - q) term.", "Вычисляем разность (5 - q)."),
            ("Apply SM-2 update formula.", "Применяем формулу обновления SM-2."),
            ("Enforce minimum EF floor of 1.3.", "Ограничиваем минимальный EF порогом 1.3."),
            ("End function.", "Конец функции."),
        ],
        ["return"]
    ),
    make_snippet(
        4, "arrays",
        """#include <vector>
#include <numeric>

double average_passing(const std::vector<int>& scores) {
    std::vector<int> passed;
    for (int s : scores) if (s >= 80) passed.push_back(s);
    int sum = std::accumulate(passed.begin(), passed.end(), 0);
    return passed.empty() ? 0.0 : static_cast<double>(sum) / passed.size();
}""",
        "Filters elements into a std::vector and sums them cleanly via std::accumulate.",
        "Фильтрует оценки в std::vector и суммирует их с помощью алгоритма std::accumulate.",
        [
            ("Include std::vector.", "Подключаем std::vector."),
            ("Include <numeric> for std::accumulate.", "Подключаем <numeric> для std::accumulate."),
            ("Blank line.", "Пустая строка."),
            ("Take vector by const reference.", "Принимаем вектор по константной ссылке."),
            ("Prepare output vector for passing scores.", "Создаём вектор для прошедших порог оценок."),
            ("Append scores meeting the threshold.", "Добавляем оценки >= 80."),
            ("Accumulate sum across iterator range.", "Суммируем элементы по итераторам."),
            ("Return floating-point average safely.", "Безопасно возвращаем среднее с приведением типа."),
            ("End function.", "Конец функции."),
        ],
        ["const", "for", "if", "return"]
    ),
    make_snippet(
        4, "classes",
        """#include <string>

class Flashcard {
    std::string term_;
    std::string translation_;
public:
    Flashcard(std::string t, std::string tr)
        : term_(std::move(t)), translation_(std::move(tr)) {}
    std::string summary() const { return term_ + " — " + translation_; }
};""",
        "Demonstrates C++ encapsulation, move semantics in member initializer lists, and const methods.",
        "Показывает инкапсуляцию C++, семантику перемещения std::move в списке инициализации и const-метод.",
        [
            ("Include std::string.", "Подключаем std::string."),
            ("Blank line.", "Пустая строка."),
            ("Declare Flashcard class (private by default).", "Объявляем класс Flashcard (поля по умолчанию private)."),
            ("Private English term field.", "Приватное поле термина."),
            ("Private Russian translation field.", "Приватное поле перевода."),
            ("Begin public interface section.", "Открываем секцию публичных методов."),
            ("Constructor taking strings by value for move efficiency.", "Конструктор, принимающий строки для перемещения."),
            ("Move strings directly into member fields.", "Перемещаем строки в поля без лишних аллокаций."),
            ("Const-qualified method promising not to mutate state.", "Метод с квалификатором const не изменяет состояние объекта."),
            ("End class definition.", "Завершаем объявление класса."),
        ],
        ["class", "public", "const", "return"]
    ),
    make_snippet(
        4, "file_reading",
        """#include <fstream>
#include <string>
#include <vector>

std::vector<std::string> read_lines(const std::string& path) {
    std::ifstream file(path);
    std::vector<std::string> lines;
    for (std::string line; std::getline(file, line); ) {
        lines.push_back(std::move(line));
    }
    return lines;
}""",
        "Uses RAII std::ifstream so the OS file handle closes automatically upon function exit.",
        "Использует RAII-поток std::ifstream — файл гарантированно закроется при выходе из функции.",
        [
            ("Include file stream header.", "Подключаем заголовок файловых потоков."),
            ("Include std::string.", "Подключаем std::string."),
            ("Include std::vector.", "Подключаем std::vector."),
            ("Blank line.", "Пустая строка."),
            ("Function returning all lines from a file.", "Функция чтения всех строк файла."),
            ("RAII file stream opens file and auto-closes in destructor.", "RAII-объект открывает файл и сам закроет его в деструкторе."),
            ("Vector collecting read lines.", "Вектор для накопления строк."),
            ("Loop reading lines until EOF.", "Читаем строки через std::getline до конца файла."),
            ("Move each line into vector without copying.", "Перемещаем строку в вектор без копирования."),
            ("End loop.", "Конец цикла."),
            ("Return lines (NRVO avoids copy).", "Возвращаем вектор строк."),
            ("End function.", "Конец функции."),
        ],
        ["const", "for", "return"]
    ),
    make_snippet(
        4, "async_ops",
        """#include <future>
#include <string>

std::string run_concurrent_checks() {
    auto db = std::async(std::launch::async, [] { return std::string("DB:OK"); });
    auto cache = std::async(std::launch::async, [] { return std::string("Cache:OK"); });
    return db.get() + " | " + cache.get();
}""",
        "Launches parallel asynchronous lambdas with std::async and synchronizes results via std::future::get().",
        "Запускает параллельные асинхронные лямбды через std::async и собирает результаты через std::future::get().",
        [
            ("Include <future> for std::async and std::future.", "Подключаем <future> для асинхронных задач."),
            ("Include std::string.", "Подключаем std::string."),
            ("Blank line.", "Пустая строка."),
            ("Function executing two checks concurrently.", "Функция параллельного запуска двух проверок."),
            ("Spawn first async task on a background thread.", "Запускаем первую задачу асинхронно в отдельном потоке."),
            ("Spawn second async task concurrently.", "Запускаем вторую задачу параллельно."),
            ("Block and retrieve both future results.", "Ожидаем завершения обеих задач и объединяем ответы."),
            ("End function.", "Конец функции."),
        ],
        ["auto", "std::async", "return"]
    ),

    # ==================== 5. JAVA (10 tasks) ====================
    make_snippet(
        5, "hello_world",
        """public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Technical English!");
    }
}""",
        "Standard JVM entry-point class printing a line to System.out.",
        "Классический класс с точкой входа JVM, выводящий строку в System.out.",
        [
            ("Public class matching filename Main.java.", "Публичный класс, имя которого совпадает с файлом."),
            ("Static JVM entry point accepting CLI arguments.", "Статическая точка входа JVM с аргументами командной строки."),
            ("Write line to standard output stream.", "Выводим строку в стандартный поток вывода."),
            ("Close main method.", "Закрываем метод main."),
            ("Close class.", "Закрываем класс."),
        ],
        ["public", "class"]
    ),
    make_snippet(
        5, "variables",
        """final String service = "billing-api";
int maxRetries = 3;
float easeFactor = 2.5f;
boolean offlineReady = true;""",
        "Declares strongly typed Java primitives and a final immutable reference.",
        "Объявляет строго типизированные примитивы Java и неизменяемую ссылку final.",
        [
            ("Immutable reference to a Java String.", "Неизменяемая ссылка на объект String."),
            ("32-bit signed integer primitive.", "32-битное знаковое целое число."),
            ("32-bit IEEE 754 float with 'f' suffix.", "Вещественное число float с суффиксом f."),
            ("Boolean primitive flag.", "Логический примитив boolean."),
        ],
        ["final"]
    ),
    make_snippet(
        5, "conditions",
        """public static String classifyStatus(int code) {
    if (code >= 200 && code < 300) {
        return "Success";
    } else if (code >= 400 && code < 500) {
        return "Client Error";
    } else {
        return "Server Error";
    }
}""",
        "Branches on HTTP status intervals using strongly typed Java conditionals.",
        "Классифицирует диапазоны HTTP-кодов с помощью условных ветвлений Java.",
        [
            ("Static helper method returning status category.", "Статический метод, возвращающий категорию ответа."),
            ("Check 2xx success interval.", "Проверяем успешный диапазон 2xx."),
            ("Return Success string.", "Возвращаем Success."),
            ("Check 4xx client error interval.", "Проверяем ошибки клиента 4xx."),
            ("Return Client Error string.", "Возвращаем Client Error."),
            ("Fallback else branch.", "Ветка по умолчанию."),
            ("Return Server Error string.", "Возвращаем Server Error."),
            ("Close else.", "Закрываем блок else."),
            ("Close method.", "Закрываем метод."),
        ],
        ["public", "if", "return"]
    ),
    make_snippet(
        5, "loops",
        """import java.util.List;

public static void printTerms(List<String> terms) {
    for (int i = 0; i < terms.size(); i++) {
        System.out.printf("%d. %s%n", i + 1, terms.get(i));
    }
}""",
        "Iterates over a typed List<String> with an indexed for-loop and formatted output.",
        "Перебирает типизированный список List<String> с индексом и форматированным выводом.",
        [
            ("Import java.util.List interface.", "Импортируем интерфейс java.util.List."),
            ("Blank line.", "Пустая строка."),
            ("Accept generic List of strings.", "Принимаем параметризованный список строк."),
            ("Indexed for-loop over list bounds.", "Индексированный цикл по элементам списка."),
            ("Print 1-based number and term.", "Выводим порядковый номер и термин."),
            ("End loop.", "Конец цикла."),
            ("End method.", "Конец метода."),
        ],
        ["public", "for"]
    ),
    make_snippet(
        5, "functions",
        """public static float updateEase(float ef, int q) {
    double diff = 5 - q;
    float next = (float) (ef + (0.1 - diff * (0.08 + diff * 0.02)));
    return Math.max(1.3f, next);
}""",
        "Computes the updated SM-2 ease factor in Java using Math.max for lower-bound clamping.",
        "Вычисляет обновлённый коэффициент SM-2 в Java с ограничением минимума через Math.max.",
        [
            ("Public static method signature.", "Сигнатура статического метода."),
            ("Compute quality distance from 5.", "Вычисляем отклонение оценки от максимума 5."),
            ("Apply SM-2 formula and cast to float.", "Применяем формулу SM-2 и приводим к float."),
            ("Guarantee result is at least 1.3f.", "Гарантируем значение не ниже 1.3f."),
            ("End method.", "Конец метода."),
        ],
        ["public", "return"]
    ),
    make_snippet(
        5, "arrays",
        """import java.util.List;

public static double averagePassing(List<Integer> scores) {
    return scores.stream()
        .filter(score -> score >= 80)
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0.0);
}""",
        "Uses the Java Streams functional pipeline to filter, unbox, and average passing scores.",
        "Использует функциональный конвейер Java Streams для фильтрации и расчёта среднего балла.",
        [
            ("Import List collection.", "Импортируем интерфейс List."),
            ("Blank line.", "Пустая строка."),
            ("Method returning average of passing scores.", "Метод вычисления среднего среди успешных оценок."),
            ("Open stream pipeline from collection.", "Создаём поток элементов Stream."),
            ("Filter elements matching predicate >= 80.", "Фильтруем оценки по условию >= 80."),
            ("Convert boxed Integer objects to primitive IntStream.", "Преобразуем в примитивный поток IntStream."),
            ("Calculate OptionalDouble mean.", "Вычисляем среднее (OptionalDouble)."),
            ("Default to 0.0 when no elements matched.", "Возвращаем 0.0, если список пуст."),
            ("End method.", "Конец метода."),
        ],
        ["public", "return"]
    ),
    make_snippet(
        5, "classes",
        """public record Flashcard(String term, String translation) {
    public String summary() {
        return term + " — " + translation;
    }
}""",
        "Uses a modern Java record (Java 16+) for an immutable value class with automatic accessors.",
        "Использует современный Java record (Java 16+) для неизменяемого класса-значения без шаблонного кода.",
        [
            ("Declare immutable record with two components.", "Объявляем неизменяемый record с двумя полями."),
            ("Add a domain formatting method.", "Добавляем метод форматирования."),
            ("Return combined term and translation.", "Возвращаем объединённую строку."),
            ("Close method.", "Закрываем метод."),
            ("Close record.", "Закрываем record."),
        ],
        ["public", "return"]
    ),
    make_snippet(
        5, "file_reading",
        """import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public static List<String> readLines(String path) throws IOException {
    return Files.readAllLines(Path.of(path));
}""",
        "Reads all UTF-8 lines concisely using the modern Java NIO Files API.",
        "Лаконично читает все строки UTF-8 с помощью современного API Java NIO Files.",
        [
            ("Import checked IOException.", "Импортируем проверяемое исключение IOException."),
            ("Import NIO Files utility class.", "Импортируем утилитный класс Files."),
            ("Import NIO Path factory.", "Импортируем Path."),
            ("Import List.", "Импортируем List."),
            ("Blank line.", "Пустая строка."),
            ("Declare method propagating checked IOException.", "Объявляем метод с пробросом IOException."),
            ("Read all lines cleanly with automatic stream closure.", "Читаем все строки с автоматическим закрытием потока."),
            ("End method.", "Конец метода."),
        ],
        ["public", "return"]
    ),
    make_snippet(
        5, "async_ops",
        """import java.util.concurrent.CompletableFuture;

public static CompletableFuture<String> fetchHealth() {
    var db = CompletableFuture.supplyAsync(() -> "DB:OK");
    var cache = CompletableFuture.supplyAsync(() -> "Cache:OK");
    return db.thenCombine(cache, (a, b) -> a + " | " + b);
}""",
        "Composes two parallel non-blocking tasks with Java's CompletableFuture.thenCombine.",
        "Объединяет две параллельные неблокирующие задачи через CompletableFuture.thenCombine.",
        [
            ("Import CompletableFuture.", "Импортируем CompletableFuture."),
            ("Blank line.", "Пустая строка."),
            ("Return a future representing combined health.", "Возвращаем будущий результат объединённой проверки."),
            ("Launch first async supplier on ForkJoinPool.", "Запускаем первую задачу в пуле потоков."),
            ("Launch second async supplier concurrently.", "Запускаем вторую задачу параллельно."),
            ("Combine both results non-blockingly once ready.", "Объединяем оба результата без блокировки потока."),
            ("End method.", "Конец метода."),
        ],
        ["public", "CompletableFuture", "return"]
    ),
    make_snippet(
        5, "http_request",
        """import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public static String getJson(String url) throws Exception {
    var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var req = HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json").GET().build();
    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
}""",
        "Uses Java 11+ standard HttpClient with connect timeout and typed response body handler.",
        "Использует встроенный HttpClient (Java 11+) с таймаутом подключения и обработчиком тела ответа.",
        [
            ("Import URI class.", "Импортируем класс URI."),
            ("Import standard java.net.http client API.", "Импортируем стандартный HTTP-клиент Java."),
            ("Import Duration for timeouts.", "Импортируем Duration для таймаута."),
            ("Blank line.", "Пустая строка."),
            ("Method issuing synchronous GET request.", "Метод выполнения HTTP GET-запроса."),
            ("Build HttpClient with 5-second timeout.", "Создаём клиент с таймаутом 5 секунд."),
            ("Construct GET request with Accept header.", "Собираем GET-запрос с заголовком Accept."),
            ("Send request and extract string body.", "Отправляем запрос и возвращаем тело ответа."),
            ("End method.", "Конец метода."),
        ],
        ["public", "return"]
    ),

    # ==================== 6. KOTLIN (10 tasks) ====================
    make_snippet(
        6, "hello_world",
        """fun main() {
    val greeting = "Hello, Technical English!"
    println(greeting)
}""",
        "Concise top-level Kotlin main function with type-inferred immutable val binding.",
        "Лаконичная функция main верхнего уровня в Kotlin с неизменяемой ссылкой val.",
        [
            ("Top-level entry point without enclosing class boilerplate.", "Точка входа без шаблонного класса-обёртки."),
            ("Immutable read-only String reference.", "Неизменяемая ссылка с автоматическим выводом типа."),
            ("Print greeting to standard output.", "Выводим приветствие в консоль."),
            ("Close main block.", "Закрываем функцию."),
        ],
        ["fun", "val"]
    ),
    make_snippet(
        6, "variables",
        """val serviceName: String = "srs-engine"
var retryCount = 3
val nullableLang: String? = null
val displayLang = nullableLang ?: "General\"""",
        "Highlights val vs var mutability, nullable types (String?), and the safe Elvis operator (?:).",
        "Показывает разницу между val и var, nullable-типы (String?) и безопасный оператор Элвиса (?:).",
        [
            ("Read-only non-null String.", "Неизменяемая не-null строка."),
            ("Mutable integer variable.", "Изменяемая целочисленная переменная."),
            ("Explicit nullable String? holding null safely.", "Явный nullable-тип String?, безопасно хранящий null."),
            ("Provide non-null fallback with Elvis operator ?:.", "Подставляем значение по умолчанию через оператор Элвиса ?:."),
        ],
        ["val"]
    ),
    make_snippet(
        6, "conditions",
        """fun classifyStatus(code: Int): String = when (code) {
    in 200..299 -> "Success"
    in 400..499 -> "Client Error"
    else -> "Server Error"
}""",
        "Uses Kotlin's expressive when expression with range checks as a single-expression function.",
        "Использует выразительный оператор when с проверкой диапазонов в виде однострочной функции.",
        [
            ("Single-expression function returning when result.", "Функция-выражение, возвращающая результат when."),
            ("Check 200..299 closed integer range.", "Проверяем диапазон 200..299."),
            ("Check 400..499 client error range.", "Проверяем диапазон клиентских ошибок."),
            ("Mandatory else branch for exhaustive String return.", "Обязательная ветка else для исчерпывающего возврата."),
            ("Close when block.", "Закрываем блок when."),
        ],
        ["fun"]
    ),
    make_snippet(
        6, "loops",
        """fun printTerms(terms: List<String>) {
    for ((index, term) in terms.withIndex()) {
        println("${index + 1}. $term")
    }
}""",
        "Destructures index and element cleanly with Kotlin's withIndex() and string templates.",
        "Деструктурирует индекс и элемент через withIndex() и шаблоны строк Kotlin.",
        [
            ("Function accepting read-only List<String>.", "Функция принимает неизменяемый список строк."),
            ("Destructure IndexedValue into index and term.", "Деструктурируем индекс и значение через withIndex()."),
            ("Interpolate expression directly inside string template.", "Подставляем значения прямо в шаблон строки."),
            ("Close loop.", "Закрываем цикл."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["fun", "for"]
    ),
    make_snippet(
        6, "functions",
        """fun nextEaseFactor(ef: Float, quality: Int): Float {
    val diff = 5 - quality
    val updated = ef + (0.1f - diff * (0.08f + diff * 0.02f))
    return updated.coerceAtLeast(1.3f)
}""",
        "Implements SM-2 ease-factor update using Kotlin's readable coerceAtLeast extension.",
        "Реализует формулу SM-2 с помощью встроенного расширения coerceAtLeast.",
        [
            ("Pure Kotlin function returning Float.", "Чистая функция расчёта коэффициента."),
            ("Calculate difference from perfect recall 5.", "Считаем отклонение от идеальной оценки 5."),
            ("Apply SM-2 adjustment formula.", "Применяем формулу корректировки SM-2."),
            ("Clamp floor cleanly with coerceAtLeast(1.3f).", "Гарантируем минимум 1.3f через coerceAtLeast."),
            ("End function.", "Конец функции."),
        ],
        ["fun", "val", "return"]
    ),
    make_snippet(
        6, "arrays",
        """val scores = listOf(85, 92, 74, 98, 61)
val avg = scores
    .filter { it >= 80 }
    .average()
    .takeUnless { it.isNaN() } ?: 0.0""",
        "Chains Kotlin collection extensions with null-safe NaN guarding.",
        "Объединяет функциональные расширения коллекций Kotlin с безопасной проверкой на NaN.",
        [
            ("Create immutable list with listOf().", "Создаём неизменяемый список через listOf()."),
            ("Start functional transformation chain.", "Начинаем цепочку преобразований."),
            ("Keep only passing scores >= 80.", "Фильтруем оценки >= 80."),
            ("Compute Double average.", "Вычисляем среднее значение."),
            ("Guard empty-list NaN and fallback to 0.0.", "Защищаемся от NaN при пустом списке через takeUnless и ?:."),
        ],
        ["val"]
    ),
    make_snippet(
        6, "classes",
        """data class Flashcard(
    val term: String,
    val translation: String,
    val easeFactor: Float = 2.5f
) {
    fun summary(): String = "$term — $translation"
}""",
        "Kotlin data class with default parameter values, immutable properties, and automatic copy().",
        "Data-класс Kotlin со значением по умолчанию, неизменяемыми свойствами и автогенерацией copy().",
        [
            ("Declare value-semantic data class.", "Объявляем data-класс для хранения данных."),
            ("English term property.", "Свойство английского термина."),
            ("Russian translation property.", "Свойство русского перевода."),
            ("Default SM-2 ease factor parameter.", "Параметр со значением по умолчанию 2.5f."),
            ("Open class body.", "Открываем тело класса."),
            ("Concise helper method returning formatted summary.", "Компактный метод форматирования."),
            ("Close class.", "Закрываем класс."),
        ],
        ["data", "class", "val", "fun"]
    ),
    make_snippet(
        6, "file_reading",
        """import java.io.File

fun readNonBlankLines(path: String): List<String> =
    File(path).useLines { sequence ->
        sequence.map(String::trim).filter(String::isNotEmpty).toList()
    }""",
        "Streams file lines lazily via Kotlin's useLines, closing the reader automatically.",
        "Лениво обрабатывает строки файла через useLines с автоматическим закрытием потока.",
        [
            ("Import java.io.File.", "Импортируем класс File."),
            ("Blank line.", "Пустая строка."),
            ("Single-expression file reader.", "Функция чтения непустых строк файла."),
            ("useLines opens a buffered reader and closes it safely.", "useLines открывает буферизированный поток и сам его закрывает."),
            ("Trim, filter blanks, and materialize List.", "Обрезаем пробелы, убираем пустые строки и собираем список."),
            ("Close lambda.", "Закрываем лямбду."),
        ],
        ["fun"]
    ),
    make_snippet(
        6, "async_ops",
        """import kotlinx.coroutines.*

suspend fun fetchSystemHealth(): String = coroutineScope {
    val db = async { delay(100); "DB:OK" }
    val cache = async { delay(100); "Cache:OK" }
    "${db.await()} | ${cache.await()}"
}""",
        "Structured concurrency in Kotlin: runs two async coroutines in parallel and cancels siblings on error.",
        "Структурная конкурентность в Kotlin: параллельно запускает две async-корутины внутри coroutineScope.",
        [
            ("Import Kotlin Coroutines primitives.", "Импортируем примитивы Kotlin Coroutines."),
            ("Blank line.", "Пустая строка."),
            ("Suspend function scoped for structured concurrency.", "Suspend-функция со структурной областью coroutineScope."),
            ("Launch first concurrent Deferred coroutine.", "Запускаем первую асинхронную корутину."),
            ("Launch second concurrent Deferred coroutine.", "Запускаем вторую корутину параллельно."),
            ("Suspend until both complete and format string.", "Ожидаем оба результата через await() без блокировки потока."),
            ("Close coroutineScope.", "Закрываем область корутин."),
        ],
        ["suspend", "fun", "val", "async", "await"]
    ),
    make_snippet(
        6, "http_request",
        """import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun fetchText(endpoint: String): String = withContext(Dispatchers.IO) {
    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        connectTimeout = 5_000
        readTimeout = 5_000
    }
    conn.inputStream.bufferedReader().use { it.readText() }
}""",
        "Offloads blocking HTTP I/O cleanly to Dispatchers.IO inside a suspend function with automatic stream close.",
        "Выносит сетевой ввод-вывод на Dispatchers.IO внутри suspend-функции с автозакрытием потока через use.",
        [
            ("Import HttpURLConnection.", "Импортируем HttpURLConnection."),
            ("Import URL.", "Импортируем URL."),
            ("Import coroutine dispatchers.", "Импортируем диспетчеры корутин."),
            ("Import withContext switcher.", "Импортируем withContext."),
            ("Blank line.", "Пустая строка."),
            ("Switch execution safely onto IO thread pool.", "Переключаем выполнение на пул потоков Dispatchers.IO."),
            ("Open connection and configure via apply scope function.", "Открываем соединение и настраиваем через apply."),
            ("Set 5-second connect timeout.", "Задаём таймаут подключения 5 секунд."),
            ("Set 5-second read timeout.", "Задаём таймаут чтения 5 секунд."),
            ("End apply block.", "Завершаем блок apply."),
            ("Read full UTF-8 body and auto-close stream via use().", "Читаем текст ответа с гарантированным закрытием потока."),
            ("End withContext.", "Закрываем withContext."),
        ],
        ["suspend", "fun", "val"]
    ),

    # ==================== 7. C# (10 tasks) ====================
    make_snippet(
        7, "hello_world",
        """using System;

Console.WriteLine("Hello, Technical English!");""",
        "Modern C# top-level statement printing directly to the console without verbose class wrapping.",
        "Современный стиль C# с оператором верхнего уровня для лаконичного вывода в консоль.",
        [
            ("Import the System namespace.", "Подключаем пространство имён System."),
            ("Blank line.", "Пустая строка."),
            ("Write line directly using top-level statement syntax.", "Выводим строку напрямую без шаблонного класса Program."),
        ],
        ["using"]
    ),
    make_snippet(
        7, "variables",
        """const int MaxRetries = 3;
string serviceName = "vocab-api";
float easeFactor = 2.5f;
string? optionalLang = null;
Console.WriteLine($"{serviceName}: {optionalLang ?? "General"}");""",
        "Shows C# compile-time constants, nullable reference types (string?), and null-coalescing (??).",
        "Показывает константы const, nullable-ссылочные типы (string?) и оператор объединения с null (??).",
        [
            ("Compile-time constant integer.", "Целочисленная константа этапа компиляции."),
            ("Non-nullable string variable.", "Строковая переменная."),
            ("Floating-point ease factor.", "Вещественный коэффициент лёгкости."),
            ("Nullable string reference explicitly allowing null.", "Nullable-ссылка, явно допускающая null."),
            ("Interpolate string with ?? null-coalescing fallback.", "Интерполируем строку с подстановкой по умолчанию через ??."),
        ],
        ["const"]
    ),
    make_snippet(
        7, "conditions",
        """static string ClassifyStatus(int code) => code switch
{
    >= 200 and < 300 => "Success",
    >= 400 and < 500 => "Client Error",
    _ => "Server Error"
};""",
        "Uses modern C# relational pattern matching inside a concise switch expression.",
        "Использует современное сопоставление с образцом диапазонов в switch-выражении C#.",
        [
            ("Expression-bodied static method using switch expression.", "Метод со switch-выражением."),
            ("Open pattern arms.", "Открываем ветки сопоставления."),
            ("Relational pattern matching 2xx codes.", "Сопоставление диапазона 2xx."),
            ("Relational pattern matching 4xx codes.", "Сопоставление диапазона 4xx."),
            ("Discard wildcard _ handles all other codes.", "Символ _ обрабатывает все остальные случаи."),
            ("Close switch expression.", "Закрываем switch-выражение."),
        ],
        []
    ),
    make_snippet(
        7, "loops",
        """var terms = new[] { "idempotency", "middleware", "sharding" };
for (int i = 0; i < terms.Length; i++)
{
    Console.WriteLine($"{i + 1}. {terms[i]}");
}""",
        "Iterates over a strongly typed C# array with zero-based indexing and string interpolation.",
        "Перебирает строго типизированный массив C# по индексу с интерполяцией строк.",
        [
            ("Implicitly typed string array initialization.", "Инициализация массива строк с выводом типа var."),
            ("Standard indexed for-loop.", "Индексированный цикл for."),
            ("Open loop body.", "Открываем тело цикла."),
            ("Print 1-based rank and term.", "Выводим порядковый номер и термин."),
            ("Close loop body.", "Закрываем тело цикла."),
        ],
        ["for"]
    ),
    make_snippet(
        7, "functions",
        """static float UpdateEase(float ef, int quality)
{
    float diff = 5 - quality;
    float updated = ef + (0.1f - diff * (0.08f + diff * 0.02f));
    return MathF.Max(1.3f, updated);
}""",
        "Implements SM-2 ease-factor calculation using .NET's single-precision MathF.Max.",
        "Реализует формулу SM-2 с использованием MathF.Max для чисел типа float.",
        [
            ("Static function returning updated float EF.", "Статический метод расчёта нового EF."),
            ("Open method block.", "Открываем тело метода."),
            ("Compute deviation from quality 5.", "Вычисляем отклонение от 5."),
            ("Apply SM-2 formula.", "Применяем формулу SM-2."),
            ("Enforce minimum ease factor of 1.3f.", "Удерживаем коэффициент не ниже 1.3f."),
            ("Close method.", "Закрываем метод."),
        ],
        ["return"]
    ),
    make_snippet(
        7, "arrays",
        """using System.Linq;

int[] scores = { 85, 92, 74, 98, 61 };
double avg = scores.Where(s => s >= 80).DefaultIfEmpty(0).Average();""",
        "Uses .NET LINQ extension methods for declarative filtering and safe averaging.",
        "Применяет методы расширения LINQ для декларативной фильтрации и безопасного расчёта среднего.",
        [
            ("Import LINQ query extensions.", "Подключаем расширения LINQ."),
            ("Blank line.", "Пустая строка."),
            ("Initialize integer array.", "Инициализируем массив целых чисел."),
            ("Filter >= 80, guard empty sequence, and compute average.", "Фильтруем >= 80, защищаемся от пустоты и считаем среднее."),
        ],
        ["using"]
    ),
    make_snippet(
        7, "classes",
        """public record Flashcard(string Term, string Translation)
{
    public string Summary() => $"{Term} — {Translation}";
}""",
        "Defines an immutable C# positional record with built-in value equality.",
        "Объявляет неизменяемый позиционный record в C# со встроенным сравнением по значению.",
        [
            ("Declare public positional record.", "Объявляем публичный позиционный record."),
            ("Open body.", "Открываем тело."),
            ("Expression-bodied formatting method.", "Метод-выражение для краткого описания."),
            ("Close record.", "Закрываем record."),
        ],
        ["public"]
    ),
    make_snippet(
        7, "file_reading",
        """using System.IO;

static string[] ReadNonEmptyLines(string path)
{
    using var reader = new StreamReader(path);
    var text = reader.ReadToEnd();
    return text.Split('\\n', StringSplitOptions.RemoveEmptyEntries);
}""",
        "Uses a C# using declaration to dispose the StreamReader deterministically at scope exit.",
        "Использует объявление using var для гарантированного освобождения StreamReader при выходе из функции.",
        [
            ("Import System.IO namespace.", "Импортируем System.IO."),
            ("Blank line.", "Пустая строка."),
            ("Helper returning non-empty lines.", "Метод чтения непустых строк."),
            ("Open block.", "Открываем тело."),
            ("Auto-disposed StreamReader via using var.", "Автоматически освобождаемый StreamReader через using var."),
            ("Read full stream content.", "Читаем всё содержимое потока."),
            ("Split lines dropping empty entries.", "Разбиваем на строки, пропуская пустые."),
            ("Close method.", "Закрываем метод."),
        ],
        ["using", "return"]
    ),
    make_snippet(
        7, "async_ops",
        """using System.Threading.Tasks;

static async Task<string> CheckHealthAsync()
{
    var dbTask = Task.Run(async () => { await Task.Delay(100); return "DB:OK"; });
    var cacheTask = Task.Run(async () => { await Task.Delay(100); return "Cache:OK"; });
    var results = await Task.WhenAll(dbTask, cacheTask);
    return string.Join(" | ", results);
}""",
        "Runs concurrent .NET Tasks with async/await and awaits all completions via Task.WhenAll.",
        "Запускает параллельные задачи .NET через async/await и ожидает их завершения через Task.WhenAll.",
        [
            ("Import Task asynchronous primitives.", "Подключаем пространство имён задач."),
            ("Blank line.", "Пустая строка."),
            ("Async method returning Task<string>.", "Асинхронный метод, возвращающий Task<string>."),
            ("Open method body.", "Открываем тело."),
            ("Start first concurrent task.", "Запускаем первую фоновую задачу."),
            ("Start second concurrent task.", "Запускаем вторую задачу параллельно."),
            ("Await both tasks without blocking threads.", "Неблокирующе ожидаем завершения обеих задач."),
            ("Join results into a single status string.", "Объединяем результаты в одну строку."),
            ("Close method.", "Закрываем метод."),
        ],
        ["using", "async", "await", "Task", "return"]
    ),
    make_snippet(
        7, "http_request",
        """using System.Net.Http;

static async Task<string> FetchJsonAsync(HttpClient client, string url)
{
    using var response = await client.GetAsync(url);
    response.EnsureSuccessStatusCode();
    return await response.Content.ReadAsStringAsync();
}""",
        "Issues an asynchronous HTTP GET using a shared HttpClient, validating status codes and disposing the response.",
        "Выполняет асинхронный HTTP GET через HttpClient с проверкой статуса и автоосвобождением ответа.",
        [
            ("Import System.Net.Http.", "Подключаем System.Net.Http."),
            ("Blank line.", "Пустая строка."),
            ("Async method accepting reusable HttpClient.", "Асинхронный метод, принимающий переиспользуемый HttpClient."),
            ("Open body.", "Открываем тело."),
            ("Send GET asynchronously and dispose response automatically.", "Отправляем GET-запрос и гарантируем освобождение ответа."),
            ("Throw if HTTP status is not 2xx.", "Выбрасываем исключение при ошибочном статусе."),
            ("Read response body string asynchronously.", "Асинхронно читаем строку тела ответа."),
            ("Close method.", "Закрываем метод."),
        ],
        ["using", "async", "await", "Task", "return"]
    ),

    # ==================== 8. GO (10 tasks) ====================
    make_snippet(
        8, "hello_world",
        """package main

import "fmt"

func main() {
    fmt.Println("Hello, Technical English!")
}""",
        "Minimal executable Go package printing a line via the standard fmt package.",
        "Минимальный исполняемый пакет Go, выводящий строку через стандартный пакет fmt.",
        [
            ("Declare executable main package.", "Объявляем исполняемый пакет main."),
            ("Blank line.", "Пустая строка."),
            ("Import formatted I/O standard package.", "Импортируем стандартный пакет fmt."),
            ("Blank line.", "Пустая строка."),
            ("Program entry function.", "Функция точки входа."),
            ("Print line with automatic newline.", "Выводим строку с переносом."),
            ("Close main.", "Закрываем функцию."),
        ],
        ["func"]
    ),
    make_snippet(
        8, "variables",
        """const maxRetries = 3
serviceName := "gateway"
var easeFactor float32 = 2.5
var offlineReady = true""",
        "Contrasts compile-time const, Go's short declaration operator (:=), and explicit typed var.",
        "Показывает константу const, короткое объявление (:=) и явную типизацию var в Go.",
        [
            ("Untyped compile-time constant.", "Нетипизированная константа этапа компиляции."),
            ("Short variable declaration inferring string.", "Короткое объявление переменной с выводом типа string."),
            ("Explicit 32-bit float declaration.", "Явное объявление 32-битного числа с плавающей точкой."),
            ("Inferred boolean variable.", "Булева переменная с выводом типа."),
        ],
        ["const"]
    ),
    make_snippet(
        8, "conditions",
        """func classifyStatus(code int) string {
    switch {
    case code >= 200 && code < 300:
        return "Success"
    case code >= 400 && code < 500:
        return "Client Error"
    default:
        return "Server Error"
    }
}""",
        "Uses Go's tagless switch statement as a clean, break-free replacement for if-else chains.",
        "Использует switch без условия в Go как чистую замену цепочкам if-else без оператора break.",
        [
            ("Declare typed Go function.", "Объявляем функцию с типом возврата string."),
            ("Tagless switch evaluates boolean cases top-to-bottom.", "Switch без выражения проверяет логические ветки сверху вниз."),
            ("Match 2xx success status.", "Проверка диапазона 2xx."),
            ("Return Success.", "Возвращаем Success."),
            ("Match 4xx client error.", "Проверка диапазона 4xx."),
            ("Return Client Error.", "Возвращаем Client Error."),
            ("Fallback branch.", "Ветка по умолчанию."),
            ("Return Server Error.", "Возвращаем Server Error."),
            ("Close switch.", "Закрываем switch."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "return"]
    ),
    make_snippet(
        8, "loops",
        """func printTerms(terms []string) {
    for i, term := range terms {
        fmt.Printf("%d. %s\\n", i+1, term)
    }
}""",
        "Iterates over a Go slice using the single unified for-range loop keyword.",
        "Перебирает срез (slice) Go с помощью единого ключевого слова цикла for-range.",
        [
            ("Accept a string slice parameter.", "Принимаем срез строк []string."),
            ("Range yields index i and copy of element term.", "Оператор range возвращает индекс и элемент."),
            ("Format numbered line to stdout.", "Выводим пронумерованный термин."),
            ("End loop.", "Конец цикла."),
            ("End function.", "Конец функции."),
        ],
        ["func", "for"]
    ),
    make_snippet(
        8, "functions",
        """func nextEase(ef float64, q int) float64 {
    diff := float64(5 - q)
    updated := ef + (0.1 - diff*(0.08+diff*0.02))
    if updated < 1.3 {
        return 1.3
    }
    return updated
}""",
        "Pure Go function demonstrating explicit numeric conversions (no implicit int-to-float casting).",
        "Чистая функция Go, показывающая явное приведение числовых типов (в Go нет неявного каста).",
        [
            ("Function signature with float64 and int parameters.", "Сигнатура функции с явными типами."),
            ("Convert int difference explicitly to float64.", "Явно преобразуем целую разность в float64."),
            ("Calculate SM-2 updated ease factor.", "Вычисляем новый коэффициент SM-2."),
            ("Guard minimum threshold 1.3.", "Проверяем нижний порог 1.3."),
            ("Clamp to 1.3.", "Возвращаем минимум 1.3."),
            ("Close if.", "Закрываем if."),
            ("Return updated EF.", "Возвращаем обновлённый коэффициент."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "if", "return"]
    ),
    make_snippet(
        8, "arrays",
        """func averagePassing(scores []int) float64 {
    sum, count := 0, 0
    for _, s := range scores {
        if s >= 80 {
            sum += s
            count++
        }
    }
    if count == 0 { return 0 }
    return float64(sum) / float64(count)
}""",
        "Computes the filtered average over a Go slice in a single zero-allocation pass.",
        "Вычисляет среднее отфильтрованных элементов среза Go за один проход без лишних аллокаций.",
        [
            ("Function taking an int slice.", "Функция принимает срез целых чисел."),
            ("Multi-variable initialization.", "Одновременная инициализация суммы и счётчика."),
            ("Iterate ignoring unused index with blank identifier _.", "Итерируем, игнорируя индекс через _."),
            ("Check passing threshold >= 80.", "Проверяем порог >= 80."),
            ("Add score to sum.", "Прибавляем оценку к сумме."),
            ("Increment matching counter.", "Увеличиваем счётчик."),
            ("Close if.", "Закрываем if."),
            ("Close loop.", "Закрываем цикл."),
            ("Avoid division by zero.", "Защита от деления на ноль."),
            ("Cast explicitly to float64 and return average.", "Явно приводим к float64 и возвращаем среднее."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "for", "if", "return"]
    ),
    make_snippet(
        8, "classes",
        """type Flashcard struct {
    Term        string
    Translation string
}

func (f Flashcard) Summary() string {
    return f.Term + " — " + f.Translation
}""",
        "Go replaces classical inheritance with lightweight structs and receiver methods.",
        "Go заменяет классическое наследование лёгкими структурами (struct) и методами с получателем.",
        [
            ("Define exported Flashcard struct type.", "Объявляем экспортируемую структуру Flashcard."),
            ("Exported field (capitalized).", "Экспортируемое поле Term (с заглавной буквы)."),
            ("Exported field Translation.", "Экспортируемое поле Translation."),
            ("Close struct.", "Закрываем структуру."),
            ("Blank line.", "Пустая строка."),
            ("Attach value-receiver method Summary() to Flashcard.", "Привязываем метод Summary() к структуре Flashcard."),
            ("Return formatted card summary.", "Возвращаем отформатированную строку."),
            ("Close method.", "Закрываем метод."),
        ],
        ["struct", "func", "return"]
    ),
    make_snippet(
        8, "file_reading",
        """func readFileFirstLine(path string) (string, error) {
    f, err := os.Open(path)
    if err != nil {
        return "", err
    }
    defer f.Close()
    scanner := bufio.NewScanner(f)
    if scanner.Scan() { return scanner.Text(), nil }
    return "", scanner.Err()
}""",
        "Idiomatic Go file I/O using explicit error returns and defer f.Close() for guaranteed cleanup.",
        "Идиоматичное чтение файла в Go с явным возвратом ошибки и defer f.Close() для закрытия ресурса.",
        [
            ("Multi-value return: result string and error.", "Возврат двух значений: результата и ошибки."),
            ("Open file handle via os.Open.", "Открываем файл через os.Open."),
            ("Handle error immediately without exceptions.", "Явно проверяем ошибку вместо исключений."),
            ("Propagate error upward.", "Возвращаем ошибку вызывающей стороне."),
            ("Close error guard.", "Закрываем проверку."),
            ("Schedule f.Close() to run automatically before return.", "Откладываем закрытие файла до выхода из функции через defer."),
            ("Create buffered line scanner.", "Создаём буферизированный сканер строк."),
            ("Read first line if present.", "Читаем первую строку, если она есть."),
            ("Return empty string and any scanner error.", "Возвращаем пустую строку и возможную ошибку."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "if", "defer", "return"]
    ),
    make_snippet(
        8, "async_ops",
        """func fetchHealth() string {
    ch := make(chan string, 2)
    go func() { ch <- "DB:OK" }()
    go func() { ch <- "Cache:OK" }()
    return <-ch + " | " + <-ch
}""",
        "Demonstrates CSP concurrency in Go: launching lightweight goroutines communicating via a buffered channel.",
        "Показывает конкурентность CSP в Go: запуск горутин, обменивающихся данными через буферизированный канал.",
        [
            ("Function orchestrating two concurrent checks.", "Функция, запускающая две конкурентные проверки."),
            ("Create buffered string channel of capacity 2.", "Создаём буферизированный канал ёмкостью 2."),
            ("Launch first goroutine sending result into channel.", "Запускаем первую горутину с отправкой в канал."),
            ("Launch second goroutine concurrently.", "Запускаем вторую горутину параллельно."),
            ("Receive both messages from channel and join.", "Читаем оба сообщения из канала и объединяем."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "chan", "goroutines", "return"]
    ),
    make_snippet(
        8, "http_request",
        """func fetchBody(url string) ([]byte, error) {
    client := &http.Client{Timeout: 5 * time.Second}
    resp, err := client.Get(url)
    if err != nil {
        return nil, err
    }
    defer resp.Body.Close()
    return io.ReadAll(resp.Body)
}""",
        "Performs a timeout-bounded HTTP GET using Go's net/http and defers closing the response body.",
        "Выполняет HTTP GET с таймаутом через стандартный пакет net/http и закрывает тело ответа через defer.",
        [
            ("Return response bytes and explicit error.", "Возвращаем байты ответа и ошибку."),
            ("Configure http.Client with 5s timeout.", "Настраиваем клиент с таймаутом 5 секунд."),
            ("Issue HTTP GET request.", "Выполняем GET-запрос."),
            ("Check network error.", "Проверяем сетевую ошибку."),
            ("Return nil bytes and error.", "Возвращаем ошибку."),
            ("Close if.", "Закрываем проверку."),
            ("Ensure TCP connection returns to pool via defer.", "Гарантируем закрытие тела ответа для переиспользования TCP-соединения."),
            ("Read full body bytes.", "Читаем всё тело ответа."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "if", "defer", "return"]
    ),

    # ==================== 9. JAVASCRIPT (10 tasks) ====================
    make_snippet(
        9, "hello_world",
        """const greeting = "Hello, Technical English!";
console.log(greeting);""",
        "Declares an immutable binding with const and logs a greeting to the JavaScript console.",
        "Объявляет неизменяемую привязку const и выводит приветствие в консоль JavaScript.",
        [
            ("Block-scoped constant string.", "Константная строка с блочной областью видимости."),
            ("Write message to stdout / DevTools console.", "Выводим сообщение в консоль."),
        ],
        ["const"]
    ),
    make_snippet(
        9, "variables",
        """const service = "frontend-app";
let retries = 3;
const easeFactor = 2.5;
const lang = null ?? "General";""",
        "Contrasts const vs let block scoping and uses the nullish coalescing operator (??).",
        "Показывает разницу между const и let, а также оператор нулевого слияния (??).",
        [
            ("Immutable reference binding.", "Неизменяемая привязка const."),
            ("Reassignable block-scoped counter.", "Изменяемая переменная let."),
            ("IEEE-754 double-precision number.", "Число с плавающей точкой (Number)."),
            ("Fallback only on null/undefined via ??.", "Значение по умолчанию только для null/undefined."),
        ],
        ["const", "let"]
    ),
    make_snippet(
        9, "conditions",
        """function classifyStatus(code) {
  if (code >= 200 && code < 300) return "Success";
  if (code >= 400 && code < 500) return "Client Error";
  return "Server Error";
}""",
        "Uses concise guard clauses to map HTTP status codes to readable labels.",
        "Использует ранние выходы (guard clauses) для сопоставления HTTP-кодов с текстовыми метками.",
        [
            ("Declare function accepting status code.", "Объявляем функцию классификации кода."),
            ("Return Success for 2xx range.", "Возвращаем Success для диапазона 2xx."),
            ("Return Client Error for 4xx range.", "Возвращаем Client Error для 4xx."),
            ("Fallback label for 5xx/other statuses.", "Метка по умолчанию для остальных кодов."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["if", "return"]
    ),
    make_snippet(
        9, "loops",
        """const terms = ["hydration", "debounce", "viewport"];
for (const [i, term] of terms.entries()) {
  console.log(`${i + 1}. ${term}`);
}""",
        "Iterates over an array using ES6 for...of with entries() destructuring and template literals.",
        "Перебирает массив через ES6 for...of с деструктуризацией entries() и шаблонными строками.",
        [
            ("Define constant array of frontend terms.", "Создаём массив фронтенд-терминов."),
            ("Destructure index and value with entries().", "Деструктурируем индекс и значение через entries()."),
            ("Interpolate using backtick template literal.", "Форматируем вывод шаблонной строкой."),
            ("End loop.", "Конец цикла."),
        ],
        ["const", "for"]
    ),
    make_snippet(
        9, "functions",
        """const nextEase = (ef, q) => {
  const diff = 5 - q;
  const updated = ef + (0.1 - diff * (0.08 + diff * 0.02));
  return Math.max(1.3, Number(updated.toFixed(4)));
};""",
        "Modern ES6 arrow function implementing the SM-2 ease-factor update with precision rounding.",
        "Стрелочная функция ES6, реализующая обновление коэффициента SM-2 с округлением.",
        [
            ("Arrow function assigned to const.", "Стрелочная функция, сохранённая в константу."),
            ("Compute deviation from max quality.", "Вычисляем разницу с максимальной оценкой."),
            ("Compute updated ease factor.", "Рассчитываем новый коэффициент лёгкости."),
            ("Round to 4 decimals and clamp at 1.3.", "Округляем до 4 знаков и ограничиваем снизу 1.3."),
            ("Close arrow function.", "Закрываем функцию."),
        ],
        ["const", "return"]
    ),
    make_snippet(
        9, "arrays",
        """const scores = [85, 92, 74, 98, 61];
const passed = scores.filter((s) => s >= 80);
const avg = passed.reduce((acc, s) => acc + s, 0) / (passed.length || 1);""",
        "Chains declarative filter() and reduce() higher-order array methods safely.",
        "Комбинирует декларативные методы высшего порядка filter() и reduce() для расчёта среднего.",
        [
            ("Source array of test scores.", "Исходный массив оценок."),
            ("Filter items meeting threshold >= 80.", "Фильтруем элементы >= 80."),
            ("Sum via reduce() and guard against empty division.", "Суммируем через reduce() с защитой от деления на 0."),
        ],
        ["const"]
    ),
    make_snippet(
        9, "classes",
        """class Flashcard {
  constructor(term, translation) {
    this.term = term;
    this.translation = translation;
  }
  summary() {
    return `${this.term} — ${this.translation}`;
  }
}""",
        "ES6 class syntax encapsulating constructor initialization and prototype methods.",
        "Синтаксис класса ES6 с конструктором и методом прототипа.",
        [
            ("Declare ES6 class.", "Объявляем класс ES6."),
            ("Constructor initializing instance properties.", "Конструктор инициализации свойств экземпляра."),
            ("Assign English term.", "Сохраняем английский термин."),
            ("Assign Russian translation.", "Сохраняем русский перевод."),
            ("Close constructor.", "Закрываем конструктор."),
            ("Define prototype method summary().", "Определяем метод summary()."),
            ("Return formatted card text.", "Возвращаем отформатированную строку."),
            ("Close method.", "Закрываем метод."),
            ("Close class.", "Закрываем класс."),
        ],
        ["class", "return"]
    ),
    make_snippet(
        9, "file_reading",
        """import { readFile } from "node:fs/promises";

async function readNonEmptyLines(path) {
  const content = await readFile(path, "utf8");
  return content.split(/\\r?\\n/).filter((line) => line.trim().length > 0);
}""",
        "Reads a UTF-8 file asynchronously in Node.js using promise-based fs/promises.",
        "Асинхронно читает UTF-8 файл в Node.js с помощью промисного API fs/promises.",
        [
            ("Import promise-based readFile from Node built-in module.", "Импортируем промисную функцию readFile из Node.js."),
            ("Blank line.", "Пустая строка."),
            ("Declare async reader function.", "Объявляем асинхронную функцию чтения."),
            ("Await full UTF-8 file content without blocking event loop.", "Ожидаем чтение файла без блокировки цикла событий."),
            ("Split lines cross-platform and filter blanks.", "Разбиваем на строки и убираем пустые."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["async", "await", "const", "return"]
    ),
    make_snippet(
        9, "async_ops",
        """const checkService = (name) =>
  new Promise((resolve) => setTimeout(() => resolve(`${name}:OK`), 100));

async function checkAll() {
  const [db, cache] = await Promise.all([checkService("DB"), checkService("Cache")]);
  return `${db} | ${cache}`;
}""",
        "Runs concurrent Promises in parallel using Promise.all and array destructuring.",
        "Запускает несколько Promise параллельно через Promise.all и деструктурирует результаты.",
        [
            ("Helper returning a delayed Promise.", "Функция, возвращающая отложенный Promise."),
            ("Resolve asynchronously after 100 ms.", "Разрешаем промис через 100 мс."),
            ("Blank line.", "Пустая строка."),
            ("Async coordinator function.", "Асинхронная функция-координатор."),
            ("Await both Promises concurrently via Promise.all.", "Параллельно ожидаем оба промиса через Promise.all."),
            ("Return combined health string.", "Возвращаем объединённый статус."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["const", "Promise", "async", "await", "return"]
    ),
    make_snippet(
        9, "http_request",
        """async function fetchJson(url) {
  const response = await fetch(url, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}""",
        "Uses the standard Fetch API with async/await and explicit HTTP status verification.",
        "Использует стандартный Fetch API с async/await и явной проверкой статуса response.ok.",
        [
            ("Async function returning parsed JSON Promise.", "Асинхронная функция загрузки JSON."),
            ("Issue HTTP GET with Accept header via fetch().", "Отправляем HTTP-запрос через fetch()."),
            ("Verify 2xx status because fetch only rejects on network failure.", "Проверяем response.ok, так как fetch не бросает ошибку на 4xx/5xx."),
            ("Parse and return JSON payload.", "Парсим и возвращаем JSON."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["async", "await", "const", "fetch", "if", "return"]
    ),

    # ==================== 10. SWIFT (10 tasks) ====================
    make_snippet(
        10, "hello_world",
        """let greeting = "Hello, Technical English!"
print(greeting)""",
        "Declares an immutable Swift constant with let and prints it to standard output.",
        "Объявляет неизменяемую константу через let в Swift и выводит её в стандартный вывод.",
        [
            ("Immutable String constant.", "Неизменяемая строковая константа."),
            ("Print line to console.", "Выводим строку в консоль."),
        ],
        ["let"]
    ),
    make_snippet(
        10, "variables",
        """let serviceName = "ios-client"
var retries = 3
let optionalLang: String? = nil
let display = optionalLang ?? "General\"""",
        "Shows Swift's let/var distinction, type-safe Optionals, and nil-coalescing (??).",
        "Демонстрирует let и var, типобезопасные опционалы (Optional) и оператор ??.",
        [
            ("Immutable inferred String.", "Неизменяемая строка с выводом типа."),
            ("Mutable Int variable.", "Изменяемая переменная Int."),
            ("Optional String holding nil.", "Опциональная строка со значением nil."),
            ("Unwrap safely with ?? fallback.", "Безопасно подставляем значение по умолчанию."),
        ],
        ["let"]
    ),
    make_snippet(
        10, "conditions",
        """func classifyStatus(_ code: Int) -> String {
    switch code {
    case 200..<300: return "Success"
    case 400..<500: return "Client Error"
    default: return "Server Error"
    }
}""",
        "Uses Swift's exhaustive switch with half-open ranges (200..<300).",
        "Использует исчерпывающий switch Swift с полуоткрытыми диапазонами (200..<300).",
        [
            ("Function with omitted external label (_).", "Функция без внешней метки аргумента."),
            ("Switch over status integer.", "Оператор switch по HTTP-коду."),
            ("Match 2xx half-open range.", "Сопоставление диапазона 200..<300."),
            ("Match 4xx range.", "Сопоставление диапазона 400..<500."),
            ("Default branch required for exhaustiveness.", "Обязательная ветка default."),
            ("Close switch.", "Закрываем switch."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "return"]
    ),
    make_snippet(
        10, "loops",
        """let terms = ["optional", "protocol", "actor"]
for (index, term) in terms.enumerated() {
    print("\\(index + 1). \\(term)")
}""",
        "Iterates over a Swift Array using enumerated() tuple destructuring and string interpolation.",
        "Перебирает массив Swift через enumerated() с деструктуризацией кортежа и интерполяцией.",
        [
            ("Immutable Swift array of strings.", "Неизменяемый массив строк."),
            ("Loop with 0-based index and element.", "Перебор с индексом и элементом."),
            ("Interpolate values with \\() syntax.", "Подставляем выражения через \\()."),
            ("Close loop.", "Закрываем цикл."),
        ],
        ["let", "for"]
    ),
    make_snippet(
        10, "functions",
        """func nextEase(ef: Double, quality: Int) -> Double {
    let diff = Double(5 - quality)
    let updated = ef + (0.1 - diff * (0.08 + diff * 0.02))
    return max(1.3, updated)
}""",
        "Pure Swift function with explicit numeric conversion and standard max() clamping.",
        "Чистая функция Swift с явным преобразованием к Double и ограничением через max().",
        [
            ("Labeled Swift function signature.", "Сигнатура функции с именованными параметрами."),
            ("Convert Int deviation to Double.", "Явно преобразуем отклонение в Double."),
            ("Apply SM-2 formula.", "Вычисляем новый EF по формуле SM-2."),
            ("Ensure minimum EF of 1.3.", "Ограничиваем минимум значением 1.3."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "let", "return"]
    ),
    make_snippet(
        10, "arrays",
        """let scores = [85, 92, 74, 98, 61]
let passed = scores.filter { $0 >= 80 }
let avg = passed.isEmpty ? 0.0 : Double(passed.reduce(0, +)) / Double(passed.count)""",
        "Uses shorthand closure arguments ($0) and operator references (+) for expressive collection processing.",
        "Использует краткие аргументы замыканий ($0) и оператор (+) в reduce для лаконичной обработки коллекций.",
        [
            ("Immutable Int array.", "Неизменяемый массив чисел."),
            ("Filter elements >= 80 with trailing closure.", "Фильтруем элементы >= 80 с помощью $0."),
            ("Sum with reduce(0, +) and compute average safely.", "Суммируем через reduce(0, +) и безопасно считаем среднее."),
        ],
        ["let"]
    ),
    make_snippet(
        10, "classes",
        """struct Flashcard {
    let term: String
    let translation: String

    func summary() -> String {
        "\\(term) — \\(translation)"
    }
}""",
        "Idiomatic Swift prefers value-type structs with synthesized memberwise initializers.",
        "Идиоматичный Swift отдаёт предпочтение структурам (value types) с автогенерируемым инициализатором.",
        [
            ("Declare value-type struct Flashcard.", "Объявляем структуру-значение Flashcard."),
            ("Immutable term property.", "Неизменяемое свойство term."),
            ("Immutable translation property.", "Неизменяемое свойство translation."),
            ("Blank line.", "Пустая строка."),
            ("Method returning formatted summary.", "Метод форматирования описания."),
            ("Implicit single-expression return.", "Неявный возврат единственного выражения."),
            ("Close method.", "Закрываем метод."),
            ("Close struct.", "Закрываем структуру."),
        ],
        ["struct", "let", "func"]
    ),
    make_snippet(
        10, "file_reading",
        """import Foundation

func readLines(at path: String) throws -> [String] {
    let content = try String(contentsOfFile: path, encoding: .utf8)
    return content.components(separatedBy: .newlines).filter { !$0.isEmpty }
}""",
        "Reads a UTF-8 file with Swift's typed throwing error handling and Foundation helpers.",
        "Читает UTF-8 файл с обработкой ошибок throws и стандартными средствами Foundation.",
        [
            ("Import Foundation framework.", "Импортируем фреймворк Foundation."),
            ("Blank line.", "Пустая строка."),
            ("Throwing function returning array of lines.", "Функция с throws, возвращающая массив строк."),
            ("Load UTF-8 file contents via try.", "Читаем содержимое файла в кодировке UTF-8."),
            ("Split on newlines and drop blank entries.", "Разделяем по переносам строк и фильтруем пустые."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "let", "return"]
    ),
    make_snippet(
        10, "async_ops",
        """func checkHealth() async -> String {
    async let db = "DB:OK"
    async let cache = "Cache:OK"
    let (d, c) = await (db, cache)
    return "\\(d) | \\(c)"
}""",
        "Uses Swift structured concurrency (async let) to run child tasks concurrently and await both.",
        "Использует структурную конкурентность Swift (async let) для параллельного выполнения подзадач.",
        [
            ("Async function declaration.", "Объявление асинхронной функции."),
            ("Start first concurrent async let binding.", "Запускаем первую параллельную задачу через async let."),
            ("Start second concurrent async let binding.", "Запускаем вторую задачу параллельно."),
            ("Suspend until both values resolve.", "Ожидаем оба значения через await."),
            ("Return combined status string.", "Возвращаем объединённый статус."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "async", "let", "await", "return"]
    ),
    make_snippet(
        10, "http_request",
        """import Foundation

func fetchData(from urlString: String) async throws -> String {
    guard let url = URL(string: urlString) else { throw URLError(.badURL) }
    let (data, _) = try await URLSession.shared.data(from: url)
    return String(decoding: data, as: UTF8.self)
}""",
        "Combines Swift guard let URL validation with async/await URLSession networking.",
        "Сочетает валидацию URL через guard let и асинхронный сетевой вызов URLSession.",
        [
            ("Import Foundation.", "Импортируем Foundation."),
            ("Blank line.", "Пустая строка."),
            ("Async throwing HTTP helper.", "Асинхронная функция с обработкой ошибок."),
            ("Validate URL safely with guard let.", "Безопасно проверяем URL через guard let."),
            ("Await response bytes from URLSession.", "Ожидаем данные ответа от URLSession."),
            ("Decode raw bytes into UTF-8 String.", "Декодируем байты в строку UTF-8."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["func", "async", "guard", "let", "await", "return"]
    ),
]
