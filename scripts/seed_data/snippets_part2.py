# -*- coding: utf-8 -*-
"""
Code snippets for Languages 11-19:
11: Scala, 12: Assembly, 13: Pascal, 14: Ruby, 15: Dart, 16: VBA, 17: Bash, 18: Lua, 19: Rust
"""

from seed_data.snippets_part1 import make_snippet

SNIPPETS_PART2 = [
    # ==================== 11. SCALA (9 tasks) ====================
    make_snippet(
        11, "hello_world",
        """@main def hello(): Unit =
  val greeting = "Hello, Technical English!"
  println(greeting)""",
        "Scala 3 indentation-based @main entry point with immutable val binding.",
        "Точка входа @main в Scala 3 с синтаксисом на отступах и неизменяемой переменной val.",
        [
            ("Scala 3 @main annotation replaces App trait boilerplate.", "Аннотация @main в Scala 3 задаёт точку входа."),
            ("Bind immutable greeting value.", "Создаём неизменяемое значение val."),
            ("Print line to stdout.", "Выводим строку в консоль."),
        ],
        ["def", "val"]
    ),
    make_snippet(
        11, "variables",
        """val service: String = "spark-job"
var retries: Int = 3
val easeFactor: Double = 2.5
val lang: Option[String] = None
val label = lang.getOrElse("General")""",
        "Demonstrates Scala val/var, algebraic Option[T], and safe fallback via getOrElse.",
        "Показывает val/var, функциональный тип Option[T] и безопасную подстановку getOrElse.",
        [
            ("Immutable String value.", "Неизменяемое строковое значение."),
            ("Mutable Int variable.", "Изменяемая переменная Int."),
            ("Double ease factor.", "Вещественный коэффициент лёгкости."),
            ("Functional Option replacing unsafe null.", "Функциональный Option вместо небезопасного null."),
            ("Extract value safely or use fallback.", "Безопасно извлекаем значение или подставляем запасное."),
        ],
        ["val"]
    ),
    make_snippet(
        11, "conditions",
        """def classifyStatus(code: Int): String = code match
  case c if c >= 200 && c < 300 => "Success"
  case c if c >= 400 && c < 500 => "Client Error"
  case _ => "Server Error\"""",
        "Uses Scala pattern matching with boolean pattern guards for clear status classification.",
        "Использует сопоставление с образцом match и охранные условия (guards) в Scala.",
        [
            ("Match expression returned directly from def.", "Выражение match возвращается напрямую из функции."),
            ("Guarded pattern matching 2xx.", "Ветка с условием для 2xx."),
            ("Guarded pattern matching 4xx.", "Ветка с условием для 4xx."),
            ("Wildcard fallback for remaining codes.", "Ветка по умолчанию для остальных кодов."),
        ],
        ["def", "match", "if"]
    ),
    make_snippet(
        11, "loops",
        """val terms = List("monad", "immutability", "actor")
for (term, idx) <- terms.zipWithIndex do
  println(s"${idx + 1}. $term")""",
        "Iterates over an immutable Scala List using zipWithIndex and s-interpolated strings.",
        "Перебирает неизменяемый список Scala с zipWithIndex и интерполяцией s-строк.",
        [
            ("Immutable singly-linked List.", "Неизменяемый односвязный список List."),
            ("For-do loop destructuring element and index.", "Цикл for-do с деструктуризацией пары."),
            ("Format output using s-interpolator.", "Форматируем вывод через s-строку."),
        ],
        ["val", "for"]
    ),
    make_snippet(
        11, "functions",
        """def nextEase(ef: Double, q: Int): Double =
  val diff = 5 - q
  math.max(1.3, ef + (0.1 - diff * (0.08 + diff * 0.02)))""",
        "Pure expression-oriented Scala function enforcing the 1.3 SM-2 ease-factor floor.",
        "Чистая функция-выражение Scala с ограничением минимума 1.3 через math.max.",
        [
            ("Typed Scala function header.", "Заголовок типизированной функции."),
            ("Compute deviation from max quality.", "Вычисляем отклонение от 5."),
            ("Last expression is automatically returned.", "Последнее выражение возвращается автоматически."),
        ],
        ["def", "val"]
    ),
    make_snippet(
        11, "arrays",
        """val scores = Vector(85, 92, 74, 98, 61)
val passed = scores.filter(_ >= 80)
val avg = if passed.isEmpty then 0.0 else passed.sum.toDouble / passed.size""",
        "Uses immutable Vector, placeholder lambda syntax (_), and expression-based if/then/else.",
        "Использует неизменяемый Vector, краткую лямбду (_) и выражение if/then/else.",
        [
            ("Immutable indexed Vector collection.", "Неизменяемая индексируемая коллекция Vector."),
            ("Filter elements >= 80 with placeholder _.", "Фильтруем элементы >= 80 с помощью _."),
            ("Safe expression-level average calculation.", "Безопасный расчёт среднего через if/then/else."),
        ],
        ["val", "if"]
    ),
    make_snippet(
        11, "classes",
        """trait Summarizable:
  def summary: String

case class Flashcard(term: String, translation: String) extends Summarizable:
  def summary: String = s"$term — $translation\"""",
        "Combines a Scala trait with an immutable case class for clean domain modeling.",
        "Объединяет трейт (trait) и неизменяемый case class для чистого моделирования домена.",
        [
            ("Declare trait defining shared behavior.", "Объявляем трейт общего поведения."),
            ("Abstract summary property.", "Абстрактный метод summary."),
            ("Blank line.", "Пустая строка."),
            ("Immutable case class implementing Summarizable.", "Неизменяемый case class, реализующий трейт."),
            ("Provide formatted summary implementation.", "Реализуем форматирование summary."),
        ],
        ["trait", "def", "class"]
    ),
    make_snippet(
        11, "file_reading",
        """import scala.io.Source
import scala.util.Using

def readLines(path: String): List[String] =
  Using.resource(Source.fromFile(path))(_.getLines().toList)""",
        "Uses Scala's Using.resource to guarantee safe file handle disposal.",
        "Использует Using.resource для гарантированного закрытия файлового ресурса в Scala.",
        [
            ("Import standard Source reader.", "Импортируем стандартный Source."),
            ("Import safe resource manager Using.", "Импортируем безопасный менеджер ресурсов Using."),
            ("Blank line.", "Пустая строка."),
            ("Declare line-reading helper.", "Объявляем функцию чтения строк."),
            ("Open file, materialize List, and auto-close.", "Открываем файл, собираем список и автоматически закрываем поток."),
        ],
        ["def"]
    ),
    make_snippet(
        11, "async_ops",
        """import scala.concurrent.{Future, ExecutionContext}

def checkHealth(using ExecutionContext): Future[String] =
  val db = Future("DB:OK")
  val cache = Future("Cache:OK")
  for d <- db; c <- cache yield s"$d | $c\"""",
        "Composes two concurrent Scala Futures cleanly with a monadic for-yield comprehension.",
        "Комбинирует два параллельных Future в Scala с помощью выражения for-yield.",
        [
            ("Import Future and ExecutionContext.", "Импортируем Future и ExecutionContext."),
            ("Blank line.", "Пустая строка."),
            ("Accept ExecutionContext implicitly via using.", "Принимаем контекст выполнения через using."),
            ("Start first Future eagerly.", "Запускаем первый Future."),
            ("Start second Future concurrently.", "Запускаем второй Future параллельно."),
            ("Combine both Futures into a single Future[String].", "Объединяем оба Future в единый результат через for-yield."),
        ],
        ["def", "Future", "val", "for", "yield"]
    ),

    # ==================== 12. ASSEMBLY (6 tasks) ====================
    make_snippet(
        12, "hello_world",
        """section .data
    msg db "Hello, Technical English!", 10
    len equ $ - msg
section .text
    global _start
_start:
    mov rax, 1          ; sys_write
    mov rdi, 1          ; stdout
    mov rsi, msg
    mov rdx, len
    syscall
    mov rax, 60         ; sys_exit
    xor rdi, rdi        ; code 0
    syscall""",
        "x86-64 Linux NASM program invoking sys_write and sys_exit kernel syscalls directly via CPU registers.",
        "Программа NASM x86-64 для Linux, напрямую вызывающая системные вызовы ядра sys_write и sys_exit через регистры.",
        [
            ("Initialized read-write data section.", "Секция инициализированных данных."),
            ("Define byte string ending with newline (10).", "Объявляем строку байтов с переносом строки (10)."),
            ("Compute string length at assembly time.", "Вычисляем длину строки на этапе ассемблирования."),
            ("Executable machine code section.", "Секция исполняемого кода."),
            ("Export linker entry symbol _start.", "Экспортируем точку входа _start для линковщика."),
            ("Entry label.", "Метка начала программы."),
            ("Load syscall number 1 (write) into RAX.", "Загружаем номер системного вызова 1 (write) в RAX."),
            ("File descriptor 1 = stdout in RDI.", "Дескриптор 1 (stdout) в RDI."),
            ("Pointer to message buffer in RSI.", "Адрес буфера строки в RSI."),
            ("Byte length in RDX.", "Длина сообщения в байтах в RDX."),
            ("Trap into Linux kernel to write bytes.", "Вызываем ядро Linux для вывода байтов."),
            ("Load syscall number 60 (exit) into RAX.", "Загружаем номер системного вызова 60 (exit)."),
            ("Zero out RDI for exit status 0.", "Обнуляем RDI — код возврата 0."),
            ("Terminate process cleanly.", "Корректно завершаем процесс."),
        ],
        ["mov", "syscall"]
    ),
    make_snippet(
        12, "variables",
        """section .data
    retries dq 3           ; 64-bit integer
    flag    db 1           ; 8-bit boolean
section .bss
    buffer  resb 64        ; reserve 64 zeroed bytes""",
        "Allocates initialized values in .data and uninitialized zero-filled memory buffers in .bss.",
        "Объявляет инициализированные переменные в секции .data и неинициализированный буфер в секции .bss.",
        [
            ("Start initialized static data section.", "Секция инициализированных статических данных."),
            ("Define 64-bit quadword initialized to 3.", "Выделяем 64-битное целое со значением 3."),
            ("Define 1-byte boolean flag.", "Выделяем 1-байтовый флаг."),
            ("Start uninitialized memory section.", "Секция неинициализированных данных .bss."),
            ("Reserve 64 bytes without bloating binary size.", "Резервируем 64 байта без увеличения размера файла."),
        ],
        []
    ),
    make_snippet(
        12, "conditions",
        """; Input: rdi = status code -> Output: rax = 1 if 2xx else 0
is_success:
    cmp rdi, 200
    jl .fail
    cmp rdi, 300
    jge .fail
    mov rax, 1
    ret
.fail:
    xor rax, rax
    ret""",
        "Compares CPU register values using CMP and conditional jump instructions (JL, JGE) based on RFLAGS.",
        "Сравнивает значения в регистрах инструкцией CMP и условными переходами JL / JGE по флагам процессора.",
        [
            ("ABI comment explaining register input/output.", "Комментарий соглашения вызовов System V ABI."),
            ("Subroutine label.", "Метка подпрограммы."),
            ("Compare RDI against 200.", "Сравниваем значение в RDI с 200."),
            ("Jump to .fail if signed less than 200.", "Переходим на .fail, если меньше 200."),
            ("Compare RDI against 300.", "Сравниваем RDI с 300."),
            ("Jump to .fail if greater than or equal to 300.", "Переходим на .fail, если >= 300."),
            ("Set return register RAX = 1 (true).", "Устанавливаем возвращаемое значение RAX = 1."),
            ("Return to caller.", "Возврат в вызывающий код."),
            ("Local failure branch label.", "Локальная метка ветки неуспеха."),
            ("Zero RAX efficiently using XOR.", "Быстро обнуляем RAX операцией XOR."),
            ("Return 0 to caller.", "Возвращаем 0."),
        ],
        ["mov"]
    ),
    make_snippet(
        12, "loops",
        """; Sum numbers from 1 to RCX into RAX
sum_to_n:
    xor rax, rax
.loop:
    add rax, rcx
    dec rcx
    jnz .loop
    ret""",
        "Implements a tight register-only countdown loop terminating when the Zero Flag (ZF) is set.",
        "Реализует быстрый регистровый цикл с обратным отсчётом до установки флага нуля (ZF).",
        [
            ("Document register contract.", "Описание входного и выходного регистров."),
            ("Function entry label.", "Метка функции."),
            ("Initialize accumulator RAX to 0.", "Обнуляем аккумулятор RAX."),
            ("Loop head label.", "Метка начала цикла."),
            ("Add current counter RCX to accumulator.", "Прибавляем счётчик RCX к сумме."),
            ("Decrement RCX and update CPU flags.", "Уменьшаем RCX на 1 с обновлением флагов."),
            ("Jump back while RCX is non-zero.", "Повторяем цикл, пока RCX не равен нулю."),
            ("Return sum in RAX.", "Возвращаем сумму в RAX."),
        ],
        []
    ),
    make_snippet(
        12, "functions",
        """; System V AMD64 ABI: int add_two(int a [rdi], int b [rsi])
add_two:
    mov rax, rdi
    add rax, rsi
    ret""",
        "Follows the System V AMD64 calling convention passing arguments in RDI/RSI and returning in RAX.",
        "Следует соглашению System V AMD64: аргументы передаются в RDI и RSI, а результат возвращается в RAX.",
        [
            ("Calling convention documentation.", "Документация соглашения о передаче аргументов."),
            ("Subroutine label.", "Метка процедуры."),
            ("Copy first argument into RAX.", "Копируем первый аргумент в RAX."),
            ("Add second argument in RSI to RAX.", "Прибавляем второй аргумент из RSI."),
            ("Pop return address from stack and resume caller.", "Возвращаем управление вызывающему коду."),
        ],
        ["mov"]
    ),
    make_snippet(
        12, "arrays",
        """; Sum 64-bit array: rdi = base ptr, rsi = length
sum_array:
    xor rax, rax
    xor rcx, rcx
.next:
    cmp rcx, rsi
    jge .done
    add rax, [rdi + rcx*8]
    inc rcx
    jmp .next
.done:
    ret""",
        "Traverses a contiguous 64-bit integer array using x86-64 scaled-index memory addressing [rdi + rcx*8].",
        "Обходит массив 64-битных целых с использованием масштабируемой адресации памяти [rdi + rcx*8].",
        [
            ("Pointer in RDI, count in RSI.", "Указатель на массив в RDI, длина в RSI."),
            ("Procedure label.", "Метка функции."),
            ("Zero sum accumulator.", "Обнуляем сумму."),
            ("Zero element index RCX.", "Обнуляем индекс RCX."),
            ("Loop start.", "Начало итерации."),
            ("Compare index against length.", "Сравниваем индекс с длиной массива."),
            ("Exit loop if index >= length.", "Выходим при достижении конца массива."),
            ("Load 8-byte element at base + index*8.", "Читаем 8-байтовый элемент по адресу base + index*8."),
            ("Advance index.", "Увеличиваем индекс."),
            ("Unconditional jump to next iteration.", "Переход к следующей итерации."),
            ("Exit label.", "Метка выхода."),
            ("Return accumulated total in RAX.", "Возвращаем итоговую сумму в RAX."),
        ],
        ["pointers"]
    ),

    # ==================== 13. PASCAL (8 tasks) ====================
    make_snippet(
        13, "hello_world",
        """program HelloTech;
begin
  WriteLn('Hello, Technical English!');
end.""",
        "Classic structured Pascal program block writing a line via built-in WriteLn.",
        "Классическая структурная программа на Pascal с выводом строки через встроенную процедуру WriteLn.",
        [
            ("Program header naming the executable unit.", "Заголовок программы."),
            ("Open main executable statement block.", "Начало основного блока программы."),
            ("Output greeting with newline.", "Выводим приветствие с переносом строки."),
            ("End program (period marks final block).", "Завершение программы (точка обязательна)."),
        ],
        ["begin"]
    ),
    make_snippet(
        13, "variables",
        """const
  MaxRetries = 3;
var
  ServiceName: string;
  EaseFactor: Double;
  IsOffline: Boolean;
begin
  ServiceName := 'desktop-tool';
  EaseFactor := 2.5;
  IsOffline := True;
end.""",
        "Separates declarations (const/var) cleanly from executable statements with := assignment.",
        "Чётко отделяет секцию объявлений (const/var) от исполняемого блока с оператором присваивания :=.",
        [
            ("Start constant declarations.", "Секция констант."),
            ("Typed-inferred integer constant.", "Целочисленная константа."),
            ("Start variable declaration block.", "Секция объявления переменных."),
            ("Managed string variable.", "Строковая переменная."),
            ("Double-precision floating-point number.", "Вещественное число двойной точности."),
            ("Boolean flag.", "Логическая переменная."),
            ("Begin executable block.", "Начало исполняемого кода."),
            ("Assign string with := operator.", "Присваиваем строку оператором :=."),
            ("Assign ease factor.", "Присваиваем коэффициент лёгкости."),
            ("Assign boolean value.", "Присваиваем логическое значение."),
            ("End block.", "Конец блока."),
        ],
        ["const", "begin"]
    ),
    make_snippet(
        13, "conditions",
        """function ClassifyStatus(Code: Integer): string;
begin
  if (Code >= 200) and (Code < 300) then
    Result := 'Success'
  else if (Code >= 400) and (Code < 500) then
    Result := 'Client Error'
  else
    Result := 'Server Error';
end;""",
        "Assigns the implicit Result variable inside structured Pascal if/then/else branches.",
        "Присваивает значение встроенной переменной Result в ветках if/then/else языка Pascal.",
        [
            ("Declare function returning string.", "Объявляем функцию с возвратом string."),
            ("Start function body.", "Начало тела функции."),
            ("Check 2xx range (parentheses required around comparisons).", "Проверяем диапазон 2xx (скобки обязательны из-за приоритета and)."),
            ("Set return value to Success.", "Задаём возвращаемое значение Success."),
            ("Check 4xx range.", "Проверяем клиентские ошибки 4xx."),
            ("Set return value to Client Error.", "Задаём Client Error."),
            ("Fallback branch.", "Ветка по умолчанию."),
            ("Set Server Error.", "Задаём Server Error."),
            ("End function.", "Конец функции."),
        ],
        ["begin", "if"]
    ),
    make_snippet(
        13, "loops",
        """var
  I: Integer;
begin
  for I := 1 to 5 do
    WriteLn('Step ', I);
end.""",
        "Uses Pascal's readable inclusive range loop (for I := 1 to 5 do).",
        "Использует наглядный цикл со включительным диапазоном (for I := 1 to 5 do).",
        [
            ("Declare loop counter section.", "Объявляем секцию переменных."),
            ("Integer loop variable.", "Переменная счётчика цикла."),
            ("Open block.", "Начало блока."),
            ("Iterate inclusively from 1 through 5.", "Итерируем включительно от 1 до 5."),
            ("Print step label and counter.", "Выводим номер шага."),
            ("End block.", "Конец блока."),
        ],
        ["begin", "for"]
    ),
    make_snippet(
        13, "functions",
        """function NextEase(EF: Double; Quality: Integer): Double;
var
  Diff, Updated: Double;
begin
  Diff := 5 - Quality;
  Updated := EF + (0.1 - Diff * (0.08 + Diff * 0.02));
  if Updated < 1.3 then Result := 1.3 else Result := Updated;
end;""",
        "Computes the SM-2 ease factor in Pascal with explicit local variable declarations.",
        "Рассчитывает коэффициент SM-2 в Pascal с явным объявлением локальных переменных.",
        [
            ("Function header with semicolon-separated parameters.", "Заголовок функции с параметрами через точку с запятой."),
            ("Local variable block.", "Блок локальных переменных."),
            ("Declare two Double locals.", "Объявляем две локальные переменные Double."),
            ("Start body.", "Начало тела."),
            ("Calculate deviation from 5.", "Вычисляем разницу с 5."),
            ("Apply SM-2 adjustment.", "Применяем формулу SM-2."),
            ("Enforce 1.3 floor via if-then-else.", "Ограничиваем снизу порогом 1.3."),
            ("End function.", "Конец функции."),
        ],
        ["begin", "if"]
    ),
    make_snippet(
        13, "arrays",
        """var
  Scores: array[0..4] of Integer = (85, 92, 74, 98, 61);
  I, Sum, Count: Integer;
begin
  Sum := 0; Count := 0;
  for I := Low(Scores) to High(Scores) do
    if Scores[I] >= 80 then begin Inc(Sum, Scores[I]); Inc(Count); end;
end.""",
        "Uses Pascal's bounds-safe Low() and High() intrinsics to traverse a fixed-size typed array.",
        "Использует безопасные встроенные функции Low() и High() для обхода статического массива.",
        [
            ("Variable section.", "Секция переменных."),
            ("Fixed 5-element array with explicit index bounds.", "Фиксированный массив с явным диапазоном индексов."),
            ("Declare loop index, sum, and match counter.", "Объявляем индекс, сумму и счётчик."),
            ("Open main block.", "Начало блока."),
            ("Initialize accumulators.", "Инициализируем счётчики."),
            ("Loop safely across array bounds via Low/High.", "Безопасно перебираем границы массива через Low/High."),
            ("Accumulate passing scores with Inc().", "Суммируем оценки >= 80 через процедуру Inc()."),
            ("End program.", "Конец программы."),
        ],
        ["begin", "for", "if"]
    ),
    make_snippet(
        13, "classes",
        """type
  TFlashcard = record
    Term: string;
    Translation: string;
  end;""",
        "Defines a structured Pascal record grouping strongly typed fields cleanly.",
        "Определяет структуру record в Pascal для группировки типизированных полей.",
        [
            ("Open custom type declaration section.", "Секция объявления пользовательских типов."),
            ("Declare record named TFlashcard (Delphi/FPC T-prefix convention).", "Объявляем запись TFlashcard по соглашению с префиксом T."),
            ("Term field.", "Поле английского термина."),
            ("Translation field.", "Поле перевода."),
            ("Close record.", "Завершаем запись."),
        ],
        []
    ),
    make_snippet(
        13, "file_reading",
        """var
  F: TextFile;
  Line: string;
begin
  AssignFile(F, 'notes.txt');
  Reset(F);
  while not Eof(F) do begin ReadLn(F, Line); WriteLn(Line); end;
  CloseFile(F);
end.""",
        "Reads a text file sequentially in Pascal with AssignFile, Reset, Eof, and CloseFile.",
        "Последовательно читает текстовый файл в Pascal с помощью AssignFile, Reset, Eof и CloseFile.",
        [
            ("Variable section.", "Секция переменных."),
            ("Typed text file handle.", "Файловая переменная TextFile."),
            ("Line buffer.", "Строка для чтения."),
            ("Start block.", "Начало блока."),
            ("Associate handle with file path.", "Связываем файловую переменную с именем файла."),
            ("Open file at beginning for reading.", "Открываем файл на чтение с начала."),
            ("Read and print until end-of-file.", "Читаем и выводим строки до конца файла Eof."),
            ("Close file handle.", "Закрываем файл."),
            ("End program.", "Конец программы."),
        ],
        ["begin"]
    ),

    # ==================== 14. RUBY (9 tasks) ====================
    make_snippet(
        14, "hello_world",
        """greeting = "Hello, Technical English!"
puts greeting""",
        "Idiomatic, minimal Ruby script printing a string with automatic newline via puts.",
        "Минималистичный идиоматичный скрипт Ruby с выводом строки через puts.",
        [
            ("Assign dynamic string variable.", "Присваиваем строковое значение переменной."),
            ("Print string with trailing newline.", "Выводим строку с переносом."),
        ],
        []
    ),
    make_snippet(
        14, "variables",
        """MAX_RETRIES = 3
service_name = "rails-api"
ease_factor = 2.5
lang = nil || "General"
puts "#{service_name} (#{lang}) EF=#{ease_factor}\"""",
        "Shows Ruby uppercase constants, dynamic variables, nil fallback (||), and #{} interpolation.",
        "Показывает константы в верхнем регистре, nil-подстановку (||) и интерполяцию #{} в Ruby.",
        [
            ("Constant defined by uppercase identifier.", "Константа (в Ruby начинается с заглавной буквы)."),
            ("Dynamic String variable.", "Строковая переменная."),
            ("Float ease factor.", "Вещественный коэффициент."),
            ("Fallback value when nil.", "Подстановка значения при nil."),
            ("Interpolate expressions inside double-quoted string.", "Интерполируем значения в двойных кавычках."),
        ],
        []
    ),
    make_snippet(
        14, "conditions",
        """def classify_status(code)
  case code
  when 200...300 then "Success"
  when 400...500 then "Client Error"
  else "Server Error"
  end
end""",
        "Uses Ruby's case/when with exclusive Range objects (200...300) and implicit return.",
        "Использует case/when с диапазонами (200...300) и неявным возвратом последнего выражения.",
        [
            ("Define Ruby method.", "Объявляем метод Ruby."),
            ("Switch on HTTP status code.", "Выражение case по коду ответа."),
            ("Match exclusive range 200..299.", "Сопоставление диапазона 200...300."),
            ("Match exclusive range 400..499.", "Сопоставление диапазона 400...500."),
            ("Default branch.", "Ветка по умолчанию."),
            ("Close case.", "Закрываем case."),
            ("Close method (implicitly returns case result).", "Закрываем метод с неявным возвратом."),
        ],
        ["def"]
    ),
    make_snippet(
        14, "loops",
        """def with_numbered(items)
  items.each_with_index { |item, i| yield "#{i + 1}. #{item}" }
end

with_numbered(["gem", "migration", "rake"]) { |line| puts line }""",
        "Showcases Ruby blocks and yield: iterating with each_with_index and passing a custom block.",
        "Показывает фирменные блоки Ruby и ключевое слово yield для выразительной итерации.",
        [
            ("Method taking an enumerable and implicit block.", "Метод принимает коллекцию и блок кода."),
            ("Iterate and invoke caller's block via yield.", "Перебираем элементы и передаём строку в блок через yield."),
            ("Close method.", "Закрываем метод."),
            ("Blank line.", "Пустая строка."),
            ("Call method with inline printing block.", "Вызываем метод с переданным блоком вывода."),
        ],
        ["def", "yield_ruby"]
    ),
    make_snippet(
        14, "functions",
        """def next_ease(ef, quality)
  diff = 5 - quality
  updated = ef + (0.1 - diff * (0.08 + diff * 0.02))
  [1.3, updated.round(4)].max
end""",
        "Expressive Ruby method computing SM-2 ease factor and clamping with Array#max.",
        "Выразительный метод Ruby для формулы SM-2 с ограничением минимума через [1.3, ...].max.",
        [
            ("Define Ruby method.", "Объявляем метод."),
            ("Compute deviation from quality 5.", "Считаем отклонение от 5."),
            ("Apply SM-2 adjustment.", "Применяем формулу SM-2."),
            ("Round to 4 decimals and clamp at 1.3.", "Округляем и выбираем максимум с 1.3."),
            ("End method.", "Конец метода."),
        ],
        ["def"]
    ),
    make_snippet(
        14, "arrays",
        """scores = [85, 92, 74, 98, 61]
passed = scores.select { |s| s >= 80 }
avg = passed.empty? ? 0.0 : passed.sum.to_f / passed.length""",
        "Filters an array with select and computes a float average via sum.to_f.",
        "Фильтрует массив методом select и вычисляет вещественное среднее через sum.to_f.",
        [
            ("Define score array.", "Создаём массив оценок."),
            ("Select elements passing >= 80.", "Отбираем оценки >= 80."),
            ("Guard empty array and divide as Float.", "Защищаемся от пустого массива и делим с .to_f."),
        ],
        []
    ),
    make_snippet(
        14, "classes",
        """class Flashcard
  attr_reader :term, :translation

  def initialize(term, translation)
    @term = term
    @translation = translation
  end

  def summary = "#{@term} — #{@translation}"
end""",
        "Pure Ruby OOP class using attr_reader metaprogramming and Ruby 3 endless method syntax.",
        "ООП-класс Ruby с метапрограммированием attr_reader и лаконичным методом Ruby 3.",
        [
            ("Declare Flashcard class.", "Объявляем класс Flashcard."),
            ("Generate getter methods automatically via attr_reader.", "Автоматически создаём геттеры через attr_reader."),
            ("Blank line.", "Пустая строка."),
            ("Constructor method initialize.", "Конструктор класса initialize."),
            ("Store instance variable @term.", "Сохраняем переменную экземпляра @term."),
            ("Store instance variable @translation.", "Сохраняем переменную экземпляра @translation."),
            ("Close initialize.", "Закрываем конструктор."),
            ("Blank line.", "Пустая строка."),
            ("Endless single-line method returning summary.", "Однострочный метод Ruby 3 для форматирования."),
            ("Close class.", "Закрываем класс."),
        ],
        ["class", "def"]
    ),
    make_snippet(
        14, "file_reading",
        """def read_non_empty(path)
  File.readlines(path, chomp: true).reject(&:empty?)
end""",
        "Reads lines without trailing newlines (chomp: true) and filters blanks in one idiomatic Ruby line.",
        "Читает строки без символов переноса (chomp: true) и убирает пустые в одну идиоматичную строку.",
        [
            ("Declare file helper.", "Объявляем метод чтения файла."),
            ("Read stripped lines and reject empty ones with symbol-to-proc.", "Читаем строки и отбрасываем пустые через &:empty?."),
            ("End method.", "Конец метода."),
        ],
        ["def"]
    ),
    make_snippet(
        14, "http_request",
        """require "net/http"
require "uri"

def fetch_body(url)
  uri = URI.parse(url)
  Net::HTTP.get_response(uri).body
end""",
        "Uses Ruby's built-in net/http library to send an HTTP GET request and extract the response body.",
        "Использует встроенную библиотеку net/http для выполнения GET-запроса и получения тела ответа.",
        [
            ("Load standard Net::HTTP client.", "Подключаем стандартный HTTP-клиент."),
            ("Load URI parser.", "Подключаем парсер URI."),
            ("Blank line.", "Пустая строка."),
            ("Define helper accepting URL string.", "Объявляем метод загрузки URL."),
            ("Parse string into structured URI object.", "Парсим строку в объект URI."),
            ("Perform GET request and return body string.", "Отправляем GET-запрос и возвращаем тело ответа."),
            ("End method.", "Конец метода."),
        ],
        ["def"]
    ),

    # ==================== 15. DART (10 tasks) ====================
    make_snippet(
        15, "hello_world",
        """void main() {
  const greeting = 'Hello, Technical English!';
  print(greeting);
}""",
        "Sound null-safe Dart entry point with compile-time const string.",
        "Точка входа Dart со строгой null-безопасностью и константой этапа компиляции const.",
        [
            ("Top-level main() entry function.", "Функция main() верхнего уровня."),
            ("Compile-time constant string literal.", "Константная строка этапа компиляции."),
            ("Print to stdout.", "Выводим в консоль."),
            ("Close main.", "Закрываем функцию."),
        ],
        ["const"]
    ),
    make_snippet(
        15, "variables",
        """const maxRetries = 3;
final serviceName = 'flutter-app';
double easeFactor = 2.5;
String? nullableLang;
final label = nullableLang ?? 'General';""",
        "Highlights const vs final, sound nullable types (String?), and the ?? fallback operator in Dart.",
        "Показывает разницу const и final, строгий nullable-тип String? и оператор ?? в Dart.",
        [
            ("Compile-time constant.", "Константа компиляции."),
            ("Runtime single-assignment variable.", "Однократно присваиваемая переменная final."),
            ("64-bit floating-point double.", "Вещественное число double."),
            ("Nullable String defaulting to null.", "Nullable-строка со значением null."),
            ("Coalesce null safely to 'General'.", "Безопасная подстановка значения по умолчанию."),
        ],
        ["const", "final"]
    ),
    make_snippet(
        15, "conditions",
        """String classifyStatus(int code) => switch (code) {
  >= 200 && < 300 => 'Success',
  >= 400 && < 500 => 'Client Error',
  _ => 'Server Error',
};""",
        "Uses Dart 3 relational switch expression patterns for concise, exhaustive branching.",
        "Использует switch-выражение Dart 3 с диапазонами для компактного исчерпывающего ветвления.",
        [
            ("Arrow function returning Dart 3 switch expression.", "Стрелочная функция со switch-выражением Dart 3."),
            ("Match 2xx codes.", "Проверка диапазона 2xx."),
            ("Match 4xx codes.", "Проверка диапазона 4xx."),
            ("Wildcard catch-all arm.", "Ветка по умолчанию для остальных кодов."),
            ("End switch expression.", "Завершаем выражение."),
        ],
        []
    ),
    make_snippet(
        15, "loops",
        """void printTerms(List<String> terms) {
  for (final (i, term) in terms.indexed) {
    print('${i + 1}. $term');
  }
}""",
        "Uses Dart 3 record destructuring over List.indexed for clean numbered loops.",
        "Использует деструктуризацию записей Dart 3 с List.indexed для чистой нумерации в цикле.",
        [
            ("Accept typed List<String>.", "Принимаем типизированный список строк."),
            ("Destructure (index, element) record directly.", "Деструктурируем кортеж (индекс, элемент)."),
            ("Print formatted line with interpolation.", "Выводим строку с интерполяцией."),
            ("Close loop.", "Закрываем цикл."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["for", "final"]
    ),
    make_snippet(
        15, "functions",
        """import 'dart:math';

double nextEase(double ef, int quality) {
  final diff = 5 - quality;
  final updated = ef + (0.1 - diff * (0.08 + diff * 0.02));
  return max(1.3, updated);
}""",
        "Pure Dart SM-2 ease-factor calculation using dart:math's generic max().",
        "Чистая функция расчёта коэффициента SM-2 на Dart с использованием max() из dart:math.",
        [
            ("Import standard dart:math library.", "Подключаем стандартную библиотеку dart:math."),
            ("Blank line.", "Пустая строка."),
            ("Typed Dart function.", "Типизированная функция."),
            ("Calculate difference from 5.", "Вычисляем отклонение от 5."),
            ("Apply SM-2 formula.", "Применяем формулу SM-2."),
            ("Enforce lower bound 1.3.", "Ограничиваем снизу порогом 1.3."),
            ("End function.", "Конец функции."),
        ],
        ["final", "return"]
    ),
    make_snippet(
        15, "arrays",
        """final scores = [85, 92, 74, 98, 61];
final passed = scores.where((s) => s >= 80).toList();
final avg = passed.isEmpty ? 0.0 : passed.reduce((a, b) => a + b) / passed.length;""",
        "Uses Iterable.where and reduce to filter passing grades and compute their average.",
        "Применяет методы Iterable.where и reduce для фильтрации оценок и расчёта среднего.",
        [
            ("Typed inferred List<int>.", "Список целых чисел List<int>."),
            ("Filter lazily with where() and materialize List.", "Фильтруем через where() и собираем список."),
            ("Sum via reduce() with empty-list protection.", "Суммируем через reduce() с защитой от пустого списка."),
        ],
        ["final"]
    ),
    make_snippet(
        15, "classes",
        """class Flashcard {
  final String term;
  final String translation;
  const Flashcard({required this.term, required this.translation});

  String summary() => '$term — $translation';
}""",
        "Immutable Dart class with a const named-parameter constructor ideal for Flutter widget optimization.",
        "Неизменяемый класс Dart с const-конструктором и именованными параметрами (идеально для Flutter).",
        [
            ("Declare Flashcard class.", "Объявляем класс Flashcard."),
            ("Immutable term field.", "Неизменяемое поле term."),
            ("Immutable translation field.", "Неизменяемое поле translation."),
            ("Const constructor with required named parameters.", "Const-конструктор с обязательными именованными параметрами."),
            ("Blank line.", "Пустая строка."),
            ("Arrow method returning summary.", "Стрелочный метод форматирования."),
            ("Close class.", "Закрываем класс."),
        ],
        ["class", "final", "const"]
    ),
    make_snippet(
        15, "file_reading",
        """import 'dart:io';

Future<List<String>> readNonEmpty(String path) async {
  final lines = await File(path).readAsLines();
  return lines.where((l) => l.trim().isNotEmpty).toList();
}""",
        "Reads file lines asynchronously on Dart's event loop via dart:io File.readAsLines().",
        "Асинхронно читает строки файла через dart:io File.readAsLines() без блокировки UI-изолята.",
        [
            ("Import dart:io file API.", "Импортируем dart:io."),
            ("Blank line.", "Пустая строка."),
            ("Async function returning Future<List<String>>.", "Асинхронная функция с возвратом Future."),
            ("Await non-blocking line read.", "Ожидаем неблокирующее чтение строк."),
            ("Filter blank lines and return List.", "Фильтруем пустые строки и возвращаем список."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["Future", "async", "final", "await", "return"]
    ),
    make_snippet(
        15, "async_ops",
        """Future<String> checkHealth() async {
  final results = await Future.wait([
    Future.delayed(const Duration(milliseconds: 100), () => 'DB:OK'),
    Future.delayed(const Duration(milliseconds: 100), () => 'Cache:OK'),
  ]);
  return results.join(' | ');
}""",
        "Runs concurrent Dart Futures in parallel using Future.wait and async/await.",
        "Выполняет несколько Dart Future параллельно через Future.wait и async/await.",
        [
            ("Async function returning Future<String>.", "Асинхронная функция, возвращающая Future<String>."),
            ("Await list of concurrent Futures via Future.wait.", "Параллельно ожидаем список Future через Future.wait."),
            ("First simulated async check.", "Первая асинхронная проверка."),
            ("Second simulated async check.", "Вторая асинхронная проверка."),
            ("Close wait list.", "Закрываем список задач."),
            ("Combine results cleanly.", "Объединяем результаты."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["Future", "async", "final", "await", "const", "return"]
    ),
    make_snippet(
        15, "http_request",
        """import 'dart:convert';
import 'dart:io';

Future<String> fetchBody(String url) async {
  final client = HttpClient();
  try {
    final req = await client.getUrl(Uri.parse(url));
    final res = await req.close();
    return await res.transform(utf8.decoder).join();
  } finally {
    client.close();
  }
}""",
        "Performs a standard-library async HTTP GET in Dart with try/finally socket cleanup.",
        "Выполняет асинхронный HTTP GET средствами dart:io с гарантированным закрытием клиента в finally.",
        [
            ("Import UTF-8 decoder.", "Подключаем декодер UTF-8."),
            ("Import dart:io HttpClient.", "Подключаем HttpClient из dart:io."),
            ("Blank line.", "Пустая строка."),
            ("Async GET helper.", "Асинхронная функция загрузки."),
            ("Instantiate HttpClient.", "Создаём экземпляр HttpClient."),
            ("Begin try block ensuring cleanup.", "Открываем try для гарантированного закрытия."),
            ("Open HTTP GET request.", "Открываем GET-запрос."),
            ("Send request and await response headers.", "Отправляем запрос и получаем ответ."),
            ("Decode UTF-8 stream chunks into full string.", "Декодируем поток UTF-8 в строку."),
            ("Finally block runs even on error.", "Блок finally выполняется даже при исключении."),
            ("Close client sockets cleanly.", "Закрываем сокеты клиента."),
            ("End finally.", "Конец finally."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["Future", "async", "final", "await", "return"]
    ),

    # ==================== 16. VBA (7 tasks) ====================
    make_snippet(
        16, "hello_world",
        """Sub HelloTech()
    Debug.Print "Hello, Technical English!"
End Sub""",
        "Defines a VBA Sub procedure writing output to the Office Immediate Window.",
        "Объявляет процедуру VBA Sub, выводящую сообщение в окно отладки Immediate Window.",
        [
            ("Declare executable macro procedure.", "Объявляем макрос-процедуру Sub."),
            ("Print message to VBA Immediate debug window.", "Выводим строку в окно Immediate."),
            ("Close Sub procedure.", "Завершаем процедуру."),
        ],
        []
    ),
    make_snippet(
        16, "variables",
        """Option Explicit

Sub ConfigureVars()
    Const MaxRetries As Integer = 3
    Dim ServiceName As String
    Dim EaseFactor As Double
    ServiceName = "excel-report"
    EaseFactor = 2.5
End Sub""",
        "Enforces strict declaration with Option Explicit and declares typed VBA variables via Dim.",
        "Включает строгую проверку имён Option Explicit и объявляет типизированные переменные через Dim.",
        [
            ("Require explicit variable declaration to catch typos.", "Запрещаем неявное объявление переменных для защиты от опечаток."),
            ("Blank line.", "Пустая строка."),
            ("Start procedure.", "Начало процедуры."),
            ("Declare typed integer constant.", "Объявляем целочисленную константу."),
            ("Declare String variable with Dim.", "Объявляем строковую переменную через Dim."),
            ("Declare Double floating-point variable.", "Объявляем вещественную переменную Double."),
            ("Assign string literal.", "Присваиваем строку."),
            ("Assign ease factor.", "Присваиваем коэффициент."),
            ("End procedure.", "Конец процедуры."),
        ],
        ["Dim"]
    ),
    make_snippet(
        16, "conditions",
        """Function ClassifyStatus(ByVal Code As Integer) As String
    If Code >= 200 And Code < 300 Then
        ClassifyStatus = "Success"
    ElseIf Code >= 400 And Code < 500 Then
        ClassifyStatus = "Client Error"
    Else
        ClassifyStatus = "Server Error"
    End If
End Function""",
        "Returns a value in VBA by assigning directly to the function's own name inside If/ElseIf/Else.",
        "Возвращает результат в VBA путём присваивания имени самой функции внутри If/ElseIf/Else.",
        [
            ("Declare value-returning VBA Function.", "Объявляем функцию VBA, возвращающую String."),
            ("Check 2xx success range.", "Проверяем диапазон 2xx."),
            ("Assign return value via function name.", "Задаём возврат через имя функции."),
            ("Check 4xx range.", "Проверяем диапазон 4xx."),
            ("Assign Client Error.", "Задаём Client Error."),
            ("Fallback branch.", "Ветка по умолчанию."),
            ("Assign Server Error.", "Задаём Server Error."),
            ("Close If block.", "Закрываем If."),
            ("Close Function.", "Закрываем функцию."),
        ],
        []
    ),
    make_snippet(
        16, "loops",
        """Sub PrintRange()
    Dim i As Integer
    For i = 1 To 5
        Debug.Print "Step " & i
    Next i
End Sub""",
        "Uses VBA's For...Next loop and the & string concatenation operator.",
        "Использует цикл For...Next и оператор конкатенации строк & в VBA.",
        [
            ("Start procedure.", "Начало процедуры."),
            ("Declare counter variable.", "Объявляем счётчик цикла."),
            ("Loop from 1 to 5 inclusively.", "Перебираем значения от 1 до 5."),
            ("Concatenate label and number with &.", "Объединяем текст и число оператором &."),
            ("Advance counter to next iteration.", "Переходим к следующей итерации."),
            ("End procedure.", "Конец процедуры."),
        ],
        ["Dim"]
    ),
    make_snippet(
        16, "functions",
        """Function NextEase(ByVal EF As Double, ByVal Quality As Integer) As Double
    Dim Diff As Double, Updated As Double
    Diff = 5 - Quality
    Updated = EF + (0.1 - Diff * (0.08 + Diff * 0.02))
    If Updated < 1.3 Then NextEase = 1.3 Else NextEase = Updated
End Function""",
        "Computes the SM-2 ease factor in VBA with ByVal parameters so caller cells/variables stay untouched.",
        "Вычисляет коэффициент SM-2 в VBA с передачей аргументов по значению (ByVal).",
        [
            ("Declare function accepting parameters by value.", "Объявляем функцию с передачей параметров ByVal."),
            ("Declare local Double variables.", "Объявляем локальные переменные Double."),
            ("Compute deviation from 5.", "Считаем отклонение от 5."),
            ("Apply SM-2 adjustment formula.", "Применяем формулу SM-2."),
            ("Enforce 1.3 minimum floor.", "Удерживаем минимум 1.3."),
            ("End function.", "Конец функции."),
        ],
        ["Dim"]
    ),
    make_snippet(
        16, "arrays",
        """Function AveragePassing(Scores As Variant) As Double
    Dim i As Long, Total As Double, Count As Long
    For i = LBound(Scores) To UBound(Scores)
        If Scores(i) >= 80 Then Total = Total + Scores(i): Count = Count + 1
    Next i
    If Count > 0 Then AveragePassing = Total / Count
End Function""",
        "Safely traverses any 0-based or 1-based VBA array using LBound() and UBound().",
        "Безопасно обходит массивы VBA с любой нижней границей (0 или 1) через LBound() и UBound().",
        [
            ("Accept array via Variant parameter.", "Принимаем массив через Variant."),
            ("Declare loop index, sum, and count.", "Объявляем индекс, сумму и счётчик."),
            ("Loop dynamically from lower to upper bound.", "Итерируем от нижней до верхней границы массива."),
            ("Accumulate elements >= 80.", "Суммируем оценки >= 80."),
            ("Next iteration.", "Следующий элемент."),
            ("Return average safely if Count > 0.", "Безопасно возвращаем среднее при Count > 0."),
            ("End function.", "Конец функции."),
        ],
        ["Dim"]
    ),
    make_snippet(
        16, "file_reading",
        """Sub ReadTextFile(ByVal FilePath As String)
    Dim FileNum As Integer, LineText As String
    FileNum = FreeFile
    Open FilePath For Input As #FileNum
    Do While Not EOF(FileNum)
        Line Input #FileNum, LineText
        Debug.Print LineText
    Loop
    Close #FileNum
End Sub""",
        "Reads a file line by line in VBA using FreeFile() to avoid file-handle collisions.",
        "Построчно читает текстовый файл в VBA, получая свободный дескриптор через FreeFile().",
        [
            ("Procedure taking a file path.", "Процедура чтения файла по пути."),
            ("Declare file handle number and line string.", "Объявляем номер дескриптора и буфер строки."),
            ("Allocate next unused OS file handle.", "Запрашиваем свободный номер файлового потока."),
            ("Open file for sequential reading.", "Открываем файл на чтение."),
            ("Loop until end-of-file reached.", "Повторяем до конца файла EOF."),
            ("Read one full line into LineText.", "Читаем целую строку в LineText."),
            ("Print line to Immediate window.", "Выводим строку."),
            ("Repeat loop.", "Конец итерации."),
            ("Close file handle.", "Закрываем файл."),
            ("End procedure.", "Конец процедуры."),
        ],
        ["Dim"]
    ),

    # ==================== 17. BASH (8 tasks) ====================
    make_snippet(
        17, "hello_world",
        """#!/usr/bin/env bash
set -euo pipefail

greeting="Hello, Technical English!"
echo "$greeting\"""",
        "Portable Bash script using strict safety flags (set -euo pipefail) and quoted variable expansion.",
        "Переносимый Bash-скрипт со строгим режимом безопасности (set -euo pipefail) и кавычками вокруг переменных.",
        [
            ("Portable shebang locating bash via PATH.", "Переносимый шебанг для поиска bash в PATH."),
            ("Exit on error, unset variable, or pipe failure.", "Останавливаем скрипт при ошибке, пустой переменной или сбое пайпа."),
            ("Blank line.", "Пустая строка."),
            ("Assign string variable (no spaces around =).", "Присваиваем строку (без пробелов вокруг =)."),
            ("Always double-quote expansions to prevent word splitting.", "Двойные кавычки защищают от разбиения строки по пробелам."),
        ],
        []
    ),
    make_snippet(
        17, "variables",
        """readonly MAX_RETRIES=3
service_name="ci-runner"
env_name="${APP_ENV:-production}"
echo "$service_name env=$env_name retries=$MAX_RETRIES\"""",
        "Demonstrates readonly constants and POSIX parameter expansion defaults (${VAR:-default}).",
        "Демонстрирует константы readonly и подстановку значения по умолчанию (${VAR:-default}).",
        [
            ("Declare immutable readonly constant.", "Объявляем неизменяемую константу readonly."),
            ("Standard shell string variable.", "Обычная строковая переменная."),
            ("Fallback to 'production' if APP_ENV is unset or empty.", "Подставляем production, если переменная APP_ENV не задана."),
            ("Expand variables safely inside double quotes.", "Безопасно выводим значения в двойных кавычках."),
        ],
        []
    ),
    make_snippet(
        17, "conditions",
        """classify_status() {
  local code="$1"
  if (( code >= 200 && code < 300 )); then
    echo "Success"
  elif (( code >= 400 && code < 500 )); then
    echo "Client Error"
  else
    echo "Server Error"
  fi
}""",
        "Uses Bash arithmetic evaluation (( ... )) and local scoping inside a shell function.",
        "Использует арифметический контекст (( ... )) и локальную переменную local внутри функции Bash.",
        [
            ("Define shell function.", "Объявляем функцию Bash."),
            ("Bind first positional argument to local variable.", "Сохраняем первый аргумент в локальной переменной."),
            ("Arithmetic test for 2xx range.", "Арифметическая проверка диапазона 2xx."),
            ("Output Success.", "Выводим Success."),
            ("Arithmetic test for 4xx range.", "Проверка диапазона 4xx."),
            ("Output Client Error.", "Выводим Client Error."),
            ("Fallback branch.", "Ветка по умолчанию."),
            ("Output Server Error.", "Выводим Server Error."),
            ("Close if statement (fi).", "Закрываем условие (fi)."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["local", "if"]
    ),
    make_snippet(
        17, "loops",
        """terms=("container" "pipeline" "rollback")
for i in "${!terms[@]}"; do
  echo "$((i + 1)). ${terms[$i]}"
done""",
        "Iterates over Bash array indices (${!terms[@]}) cleanly without breaking on spaces.",
        "Перебирает индексы массива Bash (${!terms[@]}) безопасно даже для элементов с пробелами.",
        [
            ("Declare indexed Bash array.", "Создаём индексированный массив Bash."),
            ("Iterate over all array indices safely.", "Перебираем все индексы массива."),
            ("Compute 1-based number with $((...)) and print item.", "Вычисляем номер через $((...)) и выводим элемент."),
            ("Close loop.", "Завершаем цикл."),
        ],
        ["for"]
    ),
    make_snippet(
        17, "functions",
        """greet_dev() {
  local name="${1:-Developer}"
  local role="${2:-Backend}"
  echo "Welcome, $name ($role)!"
  return 0
}""",
        "Shows Bash positional parameters ($1, $2), defaults, local variables, and numeric exit status.",
        "Показывает позиционные аргументы ($1, $2), значения по умолчанию, local и код возврата return.",
        [
            ("Declare Bash function.", "Объявляем функцию."),
            ("First argument with fallback default.", "Первый аргумент со значением по умолчанию."),
            ("Second argument with fallback default.", "Второй аргумент со значением по умолчанию."),
            ("Emit greeting to stdout.", "Выводим приветствие в stdout."),
            ("Return exit code 0 (success).", "Возвращаем код успеха 0."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["local", "return"]
    ),
    make_snippet(
        17, "arrays",
        """scores=(85 92 74 98 61)
passed=()
for s in "${scores[@]}"; do
  (( s >= 80 )) && passed+=("$s")
done
echo "Passed count: ${#passed[@]}" """,
        "Filters elements into a new Bash array using += and queries length via ${#passed[@]}.",
        "Фильтрует элементы в новый массив Bash через += и получает длину через ${#passed[@]}.",
        [
            ("Initialize numeric array.", "Инициализируем массив чисел."),
            ("Create empty result array.", "Создаём пустой массив для результатов."),
            ("Loop safely over every array element.", "Перебираем все элементы массива."),
            ("Append scores >= 80 using arithmetic short-circuit.", "Добавляем элементы >= 80 в массив passed."),
            ("End loop.", "Конец цикла."),
            ("Print total matching count via ${#arr[@]}.", "Выводим количество элементов через ${#passed[@]}."),
        ],
        ["for"]
    ),
    make_snippet(
        17, "file_reading",
        """read_lines() {
  local file="$1"
  while IFS= read -r line; do
    [[ -n "$line" ]] && echo "$line"
  done < "$file"
}""",
        "Canonical safe Bash file reading loop preserving backslashes (-r) and leading spaces (IFS=).",
        "Эталонное безопасное чтение файла в Bash с сохранением обратных слэшей (-r) и пробелов (IFS=).",
        [
            ("Declare file-reading function.", "Объявляем функцию чтения файла."),
            ("Store file path locally.", "Сохраняем путь в локальной переменной."),
            ("Read raw lines without backslash escaping or trimming.", "Читаем строки «как есть» без потери пробелов и слэшей."),
            ("Skip blank lines with [[ -n ]].", "Пропускаем пустые строки через [[ -n ]]."),
            ("Redirect target file into while-loop stdin.", "Перенаправляем файл на вход цикла while."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["local"]
    ),
    make_snippet(
        17, "http_request",
        """fetch_json() {
  local url="$1"
  curl --fail --silent --show-error --max-time 5 \\
    -H "Accept: application/json" "$url"
}""",
        "Invokes curl with production DevOps flags (--fail, --max-time) so CI fails immediately on HTTP errors.",
        "Вызывает curl с надёжными флагами DevOps (--fail, --max-time), чтобы скрипты падали при HTTP-ошибках.",
        [
            ("Declare HTTP fetch wrapper.", "Объявляем обёртку для HTTP-запроса."),
            ("Capture URL argument.", "Сохраняем URL."),
            ("Fail on 4xx/5xx, stay quiet unless error, cap timeout at 5s.", "Падаем на 4xx/5xx, убираем прогресс-бар и ставим таймаут 5с."),
            ("Pass JSON Accept header and quoted URL.", "Передаём заголовок Accept и URL в кавычках."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["local"]
    ),

    # ==================== 18. LUA (8 tasks) ====================
    make_snippet(
        18, "hello_world",
        """local greeting = "Hello, Technical English!"
print(greeting)""",
        "Declares a fast lexical local variable in Lua (avoiding global table pollution) and prints it.",
        "Объявляет быструю локальную переменную local в Lua (без загрязнения глобальной таблицы) и выводит её.",
        [
            ("Prefer local variables for speed and scope safety.", "Используем local ради производительности и чистоты области видимости."),
            ("Print string to standard output.", "Выводим строку в консоль."),
        ],
        ["local"]
    ),
    make_snippet(
        18, "variables",
        """local service = "redis-script"
local retries = 3
local ease_factor = 2.5
local lang = nil or "General"
print(string.format("%s (%s) EF=%.1f", service, lang, ease_factor))""",
        "Shows Lua's clean local bindings, nil fallback via 'or', and C-style string.format.",
        "Показывает локальные переменные, подстановку значения при nil через or и string.format.",
        [
            ("Local string.", "Локальная строка."),
            ("Lua number (64-bit integer in Lua 5.3+).", "Числовая переменная."),
            ("Floating-point number.", "Вещественное число."),
            ("Short-circuit fallback when nil.", "Подстановка по умолчанию при nil."),
            ("Format output string.", "Форматируем строку через string.format."),
        ],
        ["local"]
    ),
    make_snippet(
        18, "conditions",
        """local function classify_status(code)
  if code >= 200 and code < 300 then
    return "Success"
  elseif code >= 400 and code < 500 then
    return "Client Error"
  else
    return "Server Error"
  end
end""",
        "Structured Lua conditional branching using if / elseif / else / end.",
        "Структурное ветвление Lua с использованием if / elseif / else / end.",
        [
            ("Define local function.", "Объявляем локальную функцию."),
            ("Test 2xx status range.", "Проверяем диапазон 2xx."),
            ("Return Success.", "Возвращаем Success."),
            ("Test 4xx client error range.", "Проверяем диапазон 4xx."),
            ("Return Client Error.", "Возвращаем Client Error."),
            ("Fallback branch.", "Ветка по умолчанию."),
            ("Return Server Error.", "Возвращаем Server Error."),
            ("Close if block.", "Закрываем if."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["local", "if", "return"]
    ),
    make_snippet(
        18, "loops",
        """local terms = { "table", "metatable", "coroutine" }
for index, term in ipairs(terms) do
  print(index .. ". " .. term)
end""",
        "Iterates sequentially over a 1-indexed Lua table array using ipairs and the .. concat operator.",
        "Перебирает 1-индексированную таблицу-массив Lua через ipairs и оператор конкатенации ... ",
        [
            ("Lua arrays are tables indexed starting at 1.", "Массивы в Lua — таблицы с индексацией от 1."),
            ("ipairs iterates sequentially from index 1 until nil.", "ipairs последовательно перебирает элементы с индекса 1."),
            ("Concatenate strings with .. operator.", "Склеиваем строки оператором .. ."),
            ("End loop.", "Конец цикла."),
        ],
        ["local", "for"]
    ),
    make_snippet(
        18, "functions",
        """local function next_ease(ef, quality)
  local diff = 5 - quality
  local updated = ef + (0.1 - diff * (0.08 + diff * 0.02))
  return math.max(1.3, updated)
end""",
        "Pure Lua SM-2 ease-factor function clamped via math.max.",
        "Чистая функция расчёта коэффициента SM-2 на Lua с ограничением через math.max.",
        [
            ("Declare local helper function.", "Объявляем локальную функцию."),
            ("Compute deviation from 5.", "Считаем отклонение от 5."),
            ("Apply SM-2 formula.", "Применяем формулу SM-2."),
            ("Clamp floor at 1.3 via math.max.", "Ограничиваем снизу значением 1.3."),
            ("End function.", "Конец функции."),
        ],
        ["local", "return"]
    ),
    make_snippet(
        18, "arrays",
        """local scores = { 85, 92, 74, 98, 61 }
local sum, count = 0, 0
for _, s in ipairs(scores) do
  if s >= 80 then sum = sum + s; count = count + 1 end
end
local avg = count > 0 and (sum / count) or 0""",
        "Single-pass Lua table scan using the idiomatic 'cond and a or b' ternary pattern.",
        "Однопроходный обход таблицы Lua с идиоматичным тернарным выражением cond and a or b.",
        [
            ("Initialize sequence table.", "Создаём таблицу оценок."),
            ("Multi-assign initial accumulators.", "Инициализируем сумму и счётчик."),
            ("Iterate ignoring unused index _.", "Перебираем элементы, игнорируя индекс."),
            ("Accumulate passing scores >= 80.", "Суммируем оценки >= 80."),
            ("Close loop.", "Закрываем цикл."),
            ("Compute safe average avoiding divide-by-zero.", "Безопасно считаем среднее без деления на ноль."),
        ],
        ["local", "for", "if"]
    ),
    make_snippet(
        18, "classes",
        """local Flashcard = {}
Flashcard.__index = Flashcard

function Flashcard.new(term, translation)
  return setmetatable({ term = term, translation = translation }, Flashcard)
end

function Flashcard:summary()
  return self.term .. " — " .. self.translation
end""",
        "Idiomatic prototype-based OOP in Lua implemented via metatables and __index lookup.",
        "Идиоматичное прототипное ООП в Lua на основе метатаблиц и поля __index.",
        [
            ("Create prototype table.", "Создаём таблицу-прототип класса."),
            ("Direct missing method lookups to Flashcard table.", "Направляем поиск методов в таблицу Flashcard."),
            ("Blank line.", "Пустая строка."),
            ("Constructor binding metatable to instance table.", "Конструктор, привязывающий метатаблицу к объекту."),
            ("Return newly constructed instance.", "Возвращаем созданный экземпляр."),
            ("Close constructor.", "Закрываем конструктор."),
            ("Blank line.", "Пустая строка."),
            ("Colon syntax passes implicit self parameter.", "Двоеточие автоматически передаёт параметр self."),
            ("Format term and translation.", "Форматируем термин и перевод."),
            ("Close method.", "Закрываем метод."),
        ],
        ["local", "return"]
    ),
    make_snippet(
        18, "file_reading",
        """local function read_lines(path)
  local file, err = io.open(path, "r")
  if not file then return nil, err end
  local lines = {}
  for line in file:lines() do lines[#lines + 1] = line end
  file:close()
  return lines
end""",
        "Reads file lines safely in Lua returning (result, err) tuple style and closing the handle.",
        "Безопасно читает файл в Lua, возвращая пару (результат, ошибка) и закрывая дескриптор.",
        [
            ("Declare file helper.", "Объявляем функцию чтения."),
            ("Open file capturing optional error message.", "Открываем файл, перехватывая возможную ошибку."),
            ("Return nil and error if open failed.", "Возвращаем nil и текст ошибки при неудаче."),
            ("Prepare output sequence table.", "Создаём таблицу для строк."),
            ("Append each line via fast #lines + 1 indexing.", "Добавляем каждую строку в конец таблицы."),
            ("Close file handle explicitly.", "Закрываем файл."),
            ("Return collected lines.", "Возвращаем собранные строки."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["local", "if", "for", "return"]
    ),

    # ==================== 19. RUST (10 tasks) ====================
    make_snippet(
        19, "hello_world",
        """fn main() {
    let greeting = "Hello, Technical English!";
    println!("{greeting}");
}""",
        "Minimal zero-cost Rust program using an immutable &str slice and compile-time checked println! macro.",
        "Минимальная программа на Rust с неизменяемым срезом &str и проверяемым на компиляции макросом println!.",
        [
            ("Program entry function.", "Функция точки входа."),
            ("Immutable static string slice (&'static str).", "Неизменяемая привязка строкового среза."),
            ("Compile-time validated format macro.", "Макрос вывода с проверкой форматирования при компиляции."),
            ("Close main.", "Закрываем функцию."),
        ],
        ["fn", "let"]
    ),
    make_snippet(
        19, "variables",
        """const MAX_RETRIES: u32 = 3;
let service = "telemetry-agent";
let mut attempts = 0;
let lang: Option<&str> = None;
let label = lang.unwrap_or("General");""",
        "Illustrates immutability-by-default (let), explicit mutability (mut), and safe Option<T>.",
        "Показывает неизменяемость по умолчанию (let), явную изменяемость (mut) и безопасный Option<T>.",
        [
            ("Typed compile-time constant.", "Типизированная константа компиляции."),
            ("Immutable string slice binding.", "Неизменяемая привязка строки."),
            ("Explicitly opt into mutation with mut.", "Явно разрешаем изменение переменной через mut."),
            ("Algebraic Option replacing null pointers.", "Тип Option вместо небезопасного null."),
            ("Unwrap safely with fallback.", "Безопасно извлекаем значение с запасным вариантом."),
        ],
        ["const", "let", "mut"]
    ),
    make_snippet(
        19, "conditions",
        """fn classify_status(code: u16) -> &'static str {
    match code {
        200..=299 => "Success",
        400..=499 => "Client Error",
        _ => "Server Error",
    }
}""",
        "Uses Rust's exhaustive match expression with inclusive ranges (..=) verified at compile time.",
        "Использует исчерпывающее выражение match с включительными диапазонами (..=), проверяемое компилятором.",
        [
            ("Function returning a static string slice.", "Функция возвращает статический срез строки."),
            ("Exhaustive pattern match on status code.", "Исчерпывающее сопоставление по HTTP-коду."),
            ("Match inclusive 2xx range.", "Сопоставление диапазона 200..=299."),
            ("Match inclusive 4xx range.", "Сопоставление диапазона 400..=499."),
            ("Catch-all wildcard required for exhaustiveness.", "Ветка _ обязательна для полноты проверки."),
            ("Close match.", "Закрываем match."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["fn", "match"]
    ),
    make_snippet(
        19, "loops",
        """fn print_terms(terms: &[&str]) {
    for (idx, term) in terms.iter().enumerate() {
        println!("{}. {}", idx + 1, term);
    }
}""",
        "Borrows a slice (&[&str]) without taking ownership and iterates zero-cost via iter().enumerate().",
        "Заимствует срез (&[&str]) без передачи владения и итерирует с нулевыми накладными расходами.",
        [
            ("Borrow immutable slice of string slices.", "Заимствуем неизменяемый срез строк без копирования."),
            ("Zero-cost iterator adapter with index.", "Итератор с нулевой стоимостью и нумерацией."),
            ("Print 1-based number and term.", "Выводим порядковый номер и термин."),
            ("Close loop.", "Закрываем цикл."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["fn", "for"]
    ),
    make_snippet(
        19, "functions",
        """fn next_ease(ef: f32, quality: u8) -> f32 {
    let diff = (5 - quality as i32) as f32;
    let updated = ef + (0.1 - diff * (0.08 + diff * 0.02));
    updated.max(1.3)
}""",
        "Pure Rust function enforcing explicit numeric casts and returning via expression without semicolon.",
        "Чистая функция Rust с явным приведением типов и возвратом последнего выражения без точки с запятой.",
        [
            ("Typed signature using f32 and unsigned u8.", "Сигнатура с f32 и беззнаковым u8."),
            ("Explicit integer/float conversion.", "Явное приведение типов."),
            ("Apply SM-2 ease adjustment.", "Вычисляем поправку SM-2."),
            ("Clamp floor at 1.3 and return implicitly.", "Ограничиваем снизу 1.3 и возвращаем результат."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["fn", "let"]
    ),
    make_snippet(
        19, "arrays",
        """fn average_passing(scores: &[i32]) -> f64 {
    let passed: Vec<i32> = scores.iter().copied().filter(|&s| s >= 80).collect();
    if passed.is_empty() { return 0.0; }
    passed.iter().sum::<i32>() as f64 / passed.len() as f64
}""",
        "Chains zero-cost Rust iterator combinators with explicit turbofish sum::<i32>() type inference.",
        "Объединяет комбинаторы итераторов Rust с явным указанием типа суммы sum::<i32>().",
        [
            ("Borrow read-only slice of i32 scores.", "Заимствуем срез оценок i32."),
            ("Filter elements >= 80 and collect into Vec.", "Фильтруем элементы >= 80 и собираем в вектор."),
            ("Guard empty vector to avoid NaN.", "Защищаемся от деления на 0."),
            ("Sum and divide as f64.", "Суммируем и делим в типе f64."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["fn", "let", "if", "return"]
    ),
    make_snippet(
        19, "classes",
        """trait Summarizable {
    fn summary(&self) -> String;
}

struct Flashcard { term: String, translation: String }

impl Summarizable for Flashcard {
    fn summary(&self) -> String {
        format!("{} — {}", self.term, self.translation)
    }
}""",
        "Idiomatic Rust composition: decoupling data (struct) from polymorphic behavior (trait + impl).",
        "Идиоматичная композиция Rust: разделение данных (struct) и полиморфного поведения (trait + impl).",
        [
            ("Define shared trait interface.", "Объявляем трейт общего поведения."),
            ("Method taking immutable borrow &self.", "Метод с неизменяемым заимствованием &self."),
            ("Close trait.", "Закрываем трейт."),
            ("Blank line.", "Пустая строка."),
            ("Declare data-holding struct.", "Объявляем структуру данных."),
            ("Blank line.", "Пустая строка."),
            ("Implement trait for Flashcard.", "Реализуем трейт для Flashcard."),
            ("Provide borrowed &self method.", "Реализуем метод с &self."),
            ("Allocate formatted summary String.", "Формируем итоговую строку через format!."),
            ("Close method.", "Закрываем метод."),
            ("Close impl.", "Закрываем блок impl."),
        ],
        ["trait", "fn", "struct", "impl"]
    ),
    make_snippet(
        19, "file_reading",
        """use std::fs;
use std::io;

fn read_non_empty(path: &str) -> io::Result<Vec<String>> {
    let content = fs::read_to_string(path)?;
    Ok(content.lines().filter(|l| !l.trim().is_empty()).map(String::from).collect())
}""",
        "Uses Rust's Result<T, E> and the ergonomic ? error-propagation operator with RAII file closing.",
        "Использует Result<T, E> и оператор проброса ошибки ? с автоматическим закрытием файла по RAII.",
        [
            ("Import filesystem module.", "Импортируем модуль файловой системы."),
            ("Import I/O Result alias.", "Импортируем io::Result."),
            ("Blank line.", "Пустая строка."),
            ("Return Result instead of throwing exceptions.", "Возвращаем Result вместо исключений."),
            ("Read UTF-8 file or early-return Err via ?.", "Читаем файл или сразу возвращаем ошибку через ?."),
            ("Split lines, drop blanks, collect into Ok(Vec).", "Разделяем на строки, убираем пустые и оборачиваем в Ok()."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["fn", "let"]
    ),
    make_snippet(
        19, "async_ops",
        """async fn check(name: &str) -> String {
    format!("{name}:OK")
}

async fn check_health() -> String {
    let (db, cache) = tokio::join!(check("DB"), check("Cache"));
    format!("{db} | {cache}")
}""",
        "Zero-cost Rust async/await futures polled concurrently using Tokio's join! macro.",
        "Асинхронные Future нулевой стоимости в Rust с конкурентным запуском через макрос tokio::join!.",
        [
            ("Async function returning an impl Future<Output = String>.", "Асинхронная функция, возвращающая Future."),
            ("Format health check result.", "Форматируем результат проверки."),
            ("Close helper.", "Закрываем функцию."),
            ("Blank line.", "Пустая строка."),
            ("Coordinator async function.", "Асинхронная функция-координатор."),
            ("Poll both futures concurrently on the same task.", "Параллельно выполняем обе задачи через tokio::join!."),
            ("Combine results.", "Объединяем результаты."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["async", "fn", "let"]
    ),
    make_snippet(
        19, "http_request",
        """async fn fetch_json(url: &str) -> Result<String, reqwest::Error> {
    let body = reqwest::Client::new()
        .get(url)
        .header("Accept", "application/json")
        .send().await?
        .error_for_status()?
        .text().await?;
    Ok(body)
}""",
        "Idiomatic async Rust HTTP client chaining await and ? to handle network and non-2xx errors safely.",
        "Идиоматичный асинхронный HTTP-клиент Rust с цепочкой .await? для безопасной обработки всех ошибок.",
        [
            ("Return explicit Result<String, Error>.", "Возвращаем явный Result без исключений."),
            ("Create HTTP client builder chain.", "Создаём цепочку запроса через reqwest."),
            ("Configure GET target URL.", "Указываем URL для GET-запроса."),
            ("Attach JSON Accept header.", "Добавляем заголовок Accept."),
            ("Send asynchronously and propagate network errors.", "Отправляем запрос с пробросом сетевых ошибок."),
            ("Convert 4xx/5xx responses into Err automatically.", "Преобразуем HTTP 4xx/5xx в Err через error_for_status()."),
            ("Await UTF-8 response body text.", "Асинхронно читаем текст ответа."),
            ("Wrap success payload in Ok().", "Возвращаем успешный результат в Ok()."),
            ("Close function.", "Закрываем функцию."),
        ],
        ["async", "fn", "let", "await"]
    ),
]

from seed_data.snippets_v3_expansion import SNIPPETS_V3_EXPANSION

SNIPPETS_PART2.extend(SNIPPETS_V3_EXPANSION)

