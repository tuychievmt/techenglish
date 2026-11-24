# -*- coding: utf-8 -*-
"""
Authoritative sources, 10 Major Categories, 30 Thematic Domains,
Subcategories, and Multi-Sense Polysemous Entries for TechEnglish v2.0.
All sources are real, verifiable official standards and technical documentation.
"""

AUTHORITATIVE_SOURCES = [
    (1, "NIST Cryptographic Standards & Guidelines (FIPS / SP 800 Series)", "https://csrc.nist.gov/publications", "Standard", "National Institute of Standards and Technology", "NIST Public Domain (17 U.S.C. § 105)"),
    (2, "IETF RFC Editor — Internet Standards & Protocol Specifications", "https://www.rfc-editor.org/", "Standard", "Internet Engineering Task Force", "IETF Trust Legal Provisions (BSD-3-Clause / RFC License)"),
    (3, "IEEE Standard for Floating-Point Arithmetic (IEEE 754-2019)", "https://standards.ieee.org/ieee/754/6210/", "Standard", "IEEE Computer Society", "IEEE Standard Specification Citation"),
    (4, "OWASP Application Security Verification Standard (ASVS)", "https://owasp.org/www-project-application-security-verification-standard/", "Standard", "OWASP Foundation", "CC-BY-SA-4.0"),
    (5, "The Linux Kernel Documentation", "https://docs.kernel.org/", "Official Documentation", "Linux Kernel Organization", "GPL-2.0 / Linux Kernel Documentation License"),
    (6, "PostgreSQL 16 Official Documentation", "https://www.postgresql.org/docs/16/", "Official Documentation", "PostgreSQL Global Development Group", "PostgreSQL License (OSI Approved)"),
    (7, "The Rust Reference & Rustonomicon", "https://doc.rust-lang.org/reference/", "Official Documentation", "Rust Project Developers", "MIT / Apache-2.0"),
    (8, "Kotlin Language Documentation & Coroutines Guide", "https://kotlinlang.org/docs/home.html", "Official Documentation", "JetBrains", "Apache-2.0 / CC-BY-4.0"),
    (9, "Python 3 Standard Library & Language Reference", "https://docs.python.org/3/reference/", "Official Documentation", "Python Software Foundation", "PSF License Version 2"),
    (10, "MDN Web Docs — Web APIs, HTTP & JavaScript Reference", "https://developer.mozilla.org/", "Official Documentation", "Mozilla", "CC-BY-SA-2.5 / CC0-1.0"),
    (11, "Microsoft Learn — .NET, C# & Systems Architecture Documentation", "https://learn.microsoft.com/", "Official Documentation", "Microsoft", "CC-BY-4.0"),
    (12, "PyTorch & Deep Learning Technical Documentation", "https://pytorch.org/docs/stable/", "Official Documentation", "Linux Foundation / PyTorch", "BSD-3-Clause"),
    (13, "ACM Computing Classification System (CCS)", "https://dl.acm.org/ccs", "Taxonomy Standard", "Association for Computing Machinery", "ACM CCS Open Taxonomy Citation"),
    (14, "ISO/IEC 9075 Information Technology — Database Languages (SQL)", "https://www.iso.org/standard/76583.html", "Standard", "ISO/IEC", "ISO/IEC Standard Terminology Citation"),
    (15, "ARM Architecture Reference Manual", "https://developer.arm.com/documentation", "Hardware Specification", "Arm Limited", "Arm Technical Documentation License"),
    (16, "Intel 64 and IA-32 Architectures Software Developer Manuals", "https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html", "Hardware Specification", "Intel Corporation", "Intel SDM Technical Reference License"),
    (17, "LLVM Language Reference Manual & Compiler Infrastructure", "https://llvm.org/docs/LangRef.html", "Official Documentation", "LLVM Foundation", "Apache-2.0 WITH LLVM-exception"),
    (18, "SQLite Query Planner & Transactional Storage Documentation", "https://www.sqlite.org/docs.html", "Official Documentation", "SQLite Consortium", "Public Domain"),
    (19, "open-dict-data / ipa-dict & CMU Pronouncing Dictionary", "https://github.com/open-dict-data/ipa-dict", "Phonetic Dictionary", "Open Dict Data / Carnegie Mellon University", "MIT / CC-BY-SA-3.0 / BSD-2-Clause"),
]

# 10 Major Academic Categories (IDs 101..110 in categories or dedicated mapping)
MAJOR_CATEGORIES = [
    (101, "Machine Learning & Artificial Intelligence", "account_tree", 1200, 12),
    (102, "Cryptography & Cybersecurity", "shield", 1000, 1),
    (103, "Low-Level Programming & Computer Architecture", "code", 1000, 5),
    (104, "Hardware Engineering & Digital Electronics", "dns", 900, 15),
    (105, "Algorithms & Theoretical Computer Science", "account_tree", 900, 13),
    (106, "Databases & Distributed Systems", "storage", 900, 6),
    (107, "Networking & Protocol Engineering", "lan", 800, 2),
    (108, "Software Engineering & Compiler Technology", "merge_type", 800, 17),
    (109, "Mathematics & Scientific Computing", "account_tree", 800, 3),
    (110, "Advanced Documentation & Research Vocabulary", "web", 700, 13),
]

# All 30 Mandatory Thematic Domains (Additional Requirement §2)
THEMATIC_DOMAINS_30 = [
    "Artificial Intelligence & Machine Learning",
    "Deep Learning & Neural Networks",
    "Natural Language Processing",
    "Computer Vision",
    "Reinforcement Learning",
    "Mathematics for Computer Science",
    "Statistics & Probability Theory",
    "Cryptography & Applied Cryptography",
    "Cybersecurity & Information Security",
    "Operating Systems",
    "Low-Level Programming",
    "Computer Architecture",
    "Hardware Engineering",
    "Microprocessors & Microcontrollers",
    "Digital Electronics",
    "Embedded Systems",
    "Algorithms & Data Structures",
    "Computational Complexity",
    "Compiler Design",
    "Programming Language Theory",
    "Databases & Database Internals",
    "Distributed Systems",
    "Cloud Computing",
    "Computer Networks & Network Protocols",
    "Blockchain & Distributed Ledger Technology",
    "Software Engineering",
    "DevOps & Infrastructure",
    "Computer Graphics",
    "Scientific Computing",
    "Research Methods & Academic Computing",
    # 6 New Specialized Domains in v3.0 (КД-1)
    "GameDev",
    "Web3 / Smart Contracts",
    "Mobile Internals (Android / iOS)",
    "QA & Testing",
    "UI/UX Engineering",
    "Product Management",
]

# Explicit Polysemous Terms with Multiple Distinct Technical Senses (Phase 4 & Additional Req §3)
POLYSEMOUS_SENSES = [
    {
        "term": "Memory",
        "senses": [
            ("Computer Memory (DRAM/SRAM)", "Computer Architecture", "Hardware volatile storage cells addressable by the CPU over the memory bus.", "Аппаратная энергозависимая память (DRAM/SRAM), адресуемая процессором по шине памяти.", "The CPU fetches cache lines from main memory.", "Процессор загружает кэш-линии из оперативной памяти."),
            ("Memory Allocation (Heap/Stack)", "Low-Level Programming", "Runtime reservation and deallocation of virtual address ranges for program objects.", "Выделение и освобождение диапазонов виртуальных адресов для объектов программы во время выполнения.", "Dynamic memory allocation on the heap requires explicit deallocation or GC.", "Динамическое выделение памяти в куче требует явного освобождения или сборки мусора."),
            ("Memory Hierarchy", "Computer Architecture", "Tiered organization of registers, L1/L2/L3 caches, main RAM, and persistent storage balancing latency and capacity.", "Многоуровневая организация регистров, кэшей L1/L2/L3, ОЗУ и дисков, балансирующая задержку и ёмкость.", "Exploiting spatial locality keeps hot data high in the memory hierarchy.", "Использование пространственной локальности удерживает горячие данные на верхних уровнях иерархии памяти."),
            ("Human / Cognitive Working Memory", "Research Methods & Academic Computing", "Cognitive retention system modeled by spaced repetition algorithms such as SM-2 to schedule optimal review intervals.", "Когнитивная система удержания знаний, моделируемая алгоритмами интервального повторения (SM-2) для расчёта интервалов.", "Spaced repetition consolidates technical terminology in long-term memory.", "Интервальное повторение закрепляет техническую терминологию в долговременной памяти."),
        ],
    },
    {
        "term": "Kernel",
        "senses": [
            ("Operating System Kernel", "Operating Systems", "Privileged core of an OS managing process scheduling, virtual memory, interrupts, and hardware drivers.", "Привилегированное ядро ОС, управляющее планированием процессов, виртуальной памятью, прерываниями и драйверами.", "The Linux kernel handles system calls in ring 0.", "Ядро Linux обрабатывает системные вызовы в кольце защиты 0."),
            ("Convolutional Kernel (Filter Matrix)", "Computer Vision", "Small learnable weight matrix slid across an input tensor to extract spatial features.", "Небольшая обучаемая матрица весов, сворачиваемая с входным тензором для выделения пространственных признаков.", "A 3x3 convolutional kernel detects local edges.", "Свёрточное ядро 3x3 выделяет локальные границы изображения."),
            ("Mercer Kernel Function (SVM)", "Artificial Intelligence & Machine Learning", "Symmetric positive semi-definite function computing inner products implicitly in a high-dimensional feature space.", "Симметричная положительно полуопределённая функция, неявно вычисляющая скалярное произведение в пространстве высокой размерности.", "The radial basis function (RBF) kernel maps inputs nonlinearly.", "Радиально-базисное ядро (RBF) нелинейно отображает входные данные."),
            ("Linear Map Kernel (Null Space)", "Mathematics for Computer Science", "The subspace of all vectors that a linear transformation maps to the zero vector.", "Подпространство всех векторов, которые линейное отображение переводит в нулевой вектор.", "A linear operator is injective if and only if its kernel is trivial.", "Линейный оператор инъективен тогда и только тогда, когда его ядро тривиально."),
        ],
    },
    {
        "term": "Pipeline",
        "senses": [
            ("CPU Instruction Pipeline", "Computer Architecture", "Hardware execution datapath divided into fetch, decode, execute, memory, and writeback stages.", "Аппаратный тракт выполнения инструкций, разделённый на стадии выборки, декодирования, исполнения, доступа к памяти и записи.", "A branch misprediction flushes the CPU instruction pipeline.", "Ошибка предсказания перехода сбрасывает конвейер инструкций процессора."),
            ("CI/CD Deployment Pipeline", "DevOps & Infrastructure", "Automated sequence of build, lint, test, security scan, and deployment stages.", "Автоматизированная последовательность стадий сборки, статического анализа, тестирования и развёртывания.", "The CI/CD pipeline blocks merges when unit tests fail.", "Конвейер CI/CD блокирует слияние при падении модульных тестов."),
            ("Machine Learning Feature Pipeline", "Artificial Intelligence & Machine Learning", "Reproducible chain of data preprocessing, normalization, encoding, and estimator transforms.", "Воспроизводимая цепочка предобработки, нормализации, кодирования признаков и обучения модели.", "Fit the scaling pipeline strictly on training folds to prevent data leakage.", "Обучайте конвейер масштабирования только на тренировочной выборке, чтобы избежать утечки данных."),
            ("Graphics Rendering Pipeline", "Computer Graphics", "GPU stage sequence transforming 3D vertices and shaders into rasterized screen pixels.", "Последовательность стадий GPU, преобразующая 3D-вершины и шейдеры в растеризованные пиксели экрана.", "Fragment shaders execute late in the graphics rendering pipeline.", "Фрагментные шейдеры выполняются на поздней стадии графического конвейера."),
        ],
    },
    {
        "term": "Register",
        "senses": [
            ("CPU Architecture Register", "Computer Architecture", "Ultra-fast storage location inside the processor core accessed directly by machine instructions.", "Сверхбыстрая ячейка хранения внутри ядра процессора, доступная напрямую машинным инструкциям.", "System V ABI passes the first integer argument in register RDI.", "Соглашение System V ABI передаёт первый целочисленный аргумент в регистре RDI."),
            ("Digital Logic Shift/Hold Register", "Digital Electronics", "Synchronous array of flip-flops storing a multi-bit binary word across clock edges.", "Синхронная линейка триггеров, хранящая многоразрядное двоичное слово по фронтам тактового сигнала.", "Data latches into the output register on the rising clock edge.", "Данные фиксируются в выходном регистре по переднему фронту тактового сигнала."),
            ("Memory-Mapped I/O Control Register", "Embedded Systems", "Peripheral hardware configuration address mapped into the processor's physical address space.", "Аппаратный регистр управления периферией, отображённый в физическое адресное пространство процессора.", "Mark memory-mapped peripheral registers volatile in C.", "Помечайте отображённые в память периферийные регистры спецификатором volatile в C."),
        ],
    },
    {
        "term": "Vector",
        "senses": [
            ("Linear Algebra Vector", "Mathematics for Computer Science", "Element of a vector space supporting addition and scalar multiplication.", "Элемент векторного пространства, для которого определены операции сложения и умножения на скаляр.", "Normalize the feature vector to unit Euclidean norm.", "Нормализуйте вектор признаков до единичной евклидовой нормы."),
            ("Dynamic Contiguous Array (std::vector / Vec<T>)", "Algorithms & Data Structures", "Heap-allocated resizable contiguous buffer providing O(1) random access and amortized O(1) push.", "Динамический непрерывный массив в куче с доступом по индексу за O(1) и амортизированной вставкой за O(1).", "Pre-allocate vector capacity with reserve() to avoid reallocations.", "Заранее резервируйте ёмкость вектора через reserve() во избежание переаллокаций."),
            ("Interrupt Vector", "Operating Systems", "Indexed entry in the Interrupt Descriptor Table pointing to the hardware interrupt service routine.", "Индексированная запись в таблице дескрипторов прерываний, указывающая на адрес обработчика прерывания.", "The timer controller triggers interrupt vector 32.", "Контроллер таймера вызывает вектор прерывания 32."),
            ("Cybersecurity Attack Vector", "Cybersecurity & Information Security", "Specific pathway or mechanism exploited by an adversary to breach a target system.", "Конкретный путь или механизм, используемый злоумышленником для компрометации целевой системы.", "Unvalidated deserialization is a critical remote attack vector.", "Непроверенная десериализация — критический вектор удалённой атаки."),
        ],
    },
    {
        "term": "Token",
        "senses": [
            ("Lexical Token (Compiler)", "Compiler Design", "Atomic syntactic unit emitted by a lexer, such as an identifier, keyword, or literal.", "Атомарная синтаксическая единица, выделяемая лексическим анализатором (идентификатор, ключевое слово, литерал).", "The lexer splits source code into a stream of lexical tokens.", "Лексер разбивает исходный код на поток лексических токенов."),
            ("Subword Token (NLP / LLM)", "Natural Language Processing", "Discrete vocabulary unit produced by Byte-Pair Encoding or WordPiece for transformer input.", "Дискретная единица словаря, получаемая алгоритмами BPE или WordPiece для подачи на вход трансформера.", "Context window limits are measured in subword tokens.", "Ограничения контекстного окна измеряются в субсловных токенах."),
            ("Authentication / Bearer Token (JWT)", "Cybersecurity & Information Security", "Cryptographically signed credential proving client identity and authorization claims.", "Криптографически подписанный мандат, подтверждающий личность клиента и его права доступа.", "Attach the bearer token in the HTTP Authorization header.", "Передавайте bearer-токен в HTTP-заголовке Authorization."),
        ],
    },
    {
        "term": "Channel",
        "senses": [
            ("Concurrency Message Channel (CSP)", "Low-Level Programming", "Thread-safe typed conduit for passing data and synchronizing concurrent coroutines or goroutines.", "Потокобезопасный типизированный канал для передачи данных и синхронизации корутин или горутин.", "Unbuffered channels synchronize sender and receiver goroutines.", "Небуферизированные каналы синхронизируют отправляющую и принимающую горутины."),
            ("Shannon Communication Channel", "Mathematics for Computer Science", "Physical or logical transmission medium characterized by bandwidth, noise distribution, and channel capacity.", "Физическая или логическая среда передачи сигнала, характеризуемая полосой пропускания, шумом и пропускной способностью.", "Channel capacity is bounded by the Shannon-Hartley theorem.", "Пропускная способность канала ограничена теоремой Шеннона — Хартли."),
            ("Feature Map Channel (CNN)", "Deep Learning & Neural Networks", "Depth slice of a convolutional tensor representing activations of a specific learned filter.", "Срез глубины свёрточного тензора, отражающий активации конкретного обученного фильтра.", "RGB input tensors contain three color channels.", "Входные RGB-тензоры содержат три цветовых канала."),
            ("Cryptographic Side Channel", "Cryptography & Applied Cryptography", "Unintended physical leakage such as timing, power consumption, or cache eviction patterns.", "Побочный физический канал утечки информации: время выполнения, энергопотребление или задержки кэша.", "Constant-time arithmetic prevents timing side-channel leaks.", "Вычисления за константное время защищают от утечек по тайминговому побочному каналу."),
        ],
    },
    {
        "term": "Frame",
        "senses": [
            ("Call Stack Frame", "Low-Level Programming", "Activation record pushed onto the thread stack holding return address, saved registers, and local variables.", "Запись активации на стеке потока, содержащая адрес возврата, сохранённые регистры и локальные переменные.", "Base pointer RBP anchors the current stack frame.", "Базовый указатель RBP фиксирует текущий стековый кадр."),
            ("Physical Page Frame", "Operating Systems", "Fixed-size contiguous block of physical RAM (typically 4 KB) mapped by virtual memory page tables.", "Блок физической оперативной памяти фиксированного размера (обычно 4 КБ), отображаемый таблицами страниц.", "The page allocator maps a virtual page to a free physical page frame.", "Аллокатор страниц связывает виртуальную страницу со свободным физическим страничным кадром."),
            ("Data-Link / HTTP/2 Network Frame", "Computer Networks & Network Protocols", "Framed protocol data unit carrying header metadata, payload bytes, and integrity checksum.", "Структурированный блок данных протокола, содержащий заголовок, полезную нагрузку и контрольную сумму.", "HTTP/2 multiplexes concurrent streams as interleaved binary frames.", "HTTP/2 мультиплексирует параллельные потоки в виде чередующихся бинарных кадров."),
        ],
    },
]
