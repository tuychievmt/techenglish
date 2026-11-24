package com.techenglish.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.techenglish.domain.model.KeywordTooltip

/**
 * Offline multi-language syntax highlighter producing Compose AnnotatedString
 * for all 19 mandatory programming languages with JetBrains-inspired Light & Dark palettes.
 * Also attaches "KEYWORD" annotations on interactive keyword spans for Code Breakdown popups.
 */
object CodeSyntaxHighlighter {

    const val KEYWORD_ANNOTATION_TAG = "INTERACTIVE_KEYWORD"

    private data class Palette(
        val defaultText: Color,
        val keyword: Color,
        val interactiveKeywordBg: Color,
        val string: Color,
        val comment: Color,
        val number: Color,
        val type: Color
    )

    private val darkPalette = Palette(
        defaultText = Color(0xFFE6EDF3),
        keyword = Color(0xFFFF7B72),
        interactiveKeywordBg = Color(0x3858A6FF),
        string = Color(0xFFA5D6FF),
        comment = Color(0xFF8B949E),
        number = Color(0xFF79C0FF),
        type = Color(0xFFD2A8FF)
    )

    private val lightPalette = Palette(
        defaultText = Color(0xFF24292F),
        keyword = Color(0xFFCF222E),
        interactiveKeywordBg = Color(0x280969DA),
        string = Color(0xFF0A3069),
        comment = Color(0xFF6E7781),
        number = Color(0xFF0550AE),
        type = Color(0xFF8250DF)
    )

    private val commonKeywords = setOf(
        "def", "class", "return", "if", "elif", "else", "for", "while", "in", "with", "as",
        "import", "from", "async", "await", "yield", "try", "except", "finally", "raise", "pass",
        "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "match", "use", "mod",
        "func", "package", "go", "chan", "defer", "range", "switch", "case", "default", "type",
        "fun", "val", "var", "data", "sealed", "when", "suspend", "object", "override", "private",
        "public", "protected", "static", "final", "void", "const", "volatile", "auto", "new",
        "using", "namespace", "record", "throws", "throw", "catch", "guard", "extension", "protocol",
        "local", "function", "then", "elseif", "end", "do", "repeat", "until",
        "program", "begin", "procedure", "array", "of", "to", "not", "and", "or",
        "sub", "dim", "option", "explicit", "byval", "next", "loop", "close", "open",
        "readonly", "fi", "done", "esac", "set", "echo",
        "select", "from", "where", "join", "inner", "left", "right", "on", "group", "by",
        "order", "limit", "create", "table", "primary", "key", "autoincrement", "integer",
        "text", "real", "recursive", "union", "all", "null", "true", "false", "nil", "none",
        "section", "global", "mov", "add", "sub", "xor", "cmp", "jl", "jge", "jnz", "jmp",
        "dec", "inc", "ret", "syscall", "db", "dq", "equ", "resb",
        "malloc", "free", "typedef", "include", "sizeof", "attr_reader", "require", "puts"
    )

    private val tokenRegex = Regex(
        pattern = """(//[^\n]*|#[^\n]*|--[^\n]*|;[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`|\b\d+(?:\.\d+)?[fFuUlL]?\b|\b[A-Za-z_][A-Za-z0-9_:]*\b)"""
    )

    fun highlightLine(
        line: String,
        languageSlug: String,
        isDark: Boolean,
        interactiveKeywords: List<KeywordTooltip> = emptyList()
    ): AnnotatedString {
        val palette = if (isDark) darkPalette else lightPalette
        val interactiveMap = interactiveKeywords.associateBy { it.keyword.lowercase() }
        val isVba = languageSlug.equals("vba", ignoreCase = true)
        val isAsm = languageSlug.equals("assembly", ignoreCase = true)
        val isSqlOrLua = languageSlug.equals("sql", ignoreCase = true) || languageSlug.equals("lua", ignoreCase = true)
        val isHashComment = languageSlug.lowercase() in setOf("python", "ruby", "bash")

        return buildAnnotatedString {
            append(line)
            addStyle(SpanStyle(color = palette.defaultText), 0, line.length)

            tokenRegex.findAll(line).forEach { match ->
                val token = match.value
                val start = match.range.first
                val end = match.range.last + 1

                val isComment = token.startsWith("//") ||
                    token.startsWith("/*") ||
                    (isHashComment && token.startsWith("#")) ||
                    (isSqlOrLua && token.startsWith("--")) ||
                    (isAsm && token.startsWith(";"))

                val isString = !isComment && (
                    token.startsWith("\"") ||
                        token.startsWith("`") ||
                        (!isVba && token.startsWith("'"))
                    )

                when {
                    isComment -> {
                        addStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic), start, end)
                    }
                    isString -> {
                        addStyle(SpanStyle(color = palette.string), start, end)
                    }
                    token.first().isDigit() -> {
                        addStyle(SpanStyle(color = palette.number), start, end)
                    }
                    else -> {
                        val lower = token.lowercase()
                        val interactiveHit = interactiveMap[lower]
                            ?: if (lower == "go" && interactiveMap.containsKey("goroutines")) interactiveMap["goroutines"]
                            else if (token.startsWith("*") && interactiveMap.containsKey("pointers")) interactiveMap["pointers"]
                            else null

                        if (interactiveHit != null) {
                            addStyle(
                                SpanStyle(
                                    color = palette.keyword,
                                    background = palette.interactiveKeywordBg,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                start,
                                end
                            )
                            addStringAnnotation(
                                tag = KEYWORD_ANNOTATION_TAG,
                                annotation = interactiveHit.keyword,
                                start = start,
                                end = end
                            )
                        } else if (lower in commonKeywords) {
                            addStyle(
                                SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold),
                                start,
                                end
                            )
                        } else if (token.first().isUpperCase() && token.any { it.isLowerCase() }) {
                            addStyle(SpanStyle(color = palette.type), start, end)
                        }
                    }
                }
            }
        }
    }
}
