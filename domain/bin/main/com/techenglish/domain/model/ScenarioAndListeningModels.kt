package com.techenglish.domain.model

enum class ScenarioType(val labelRu: String, val labelEn: String) {
    DAILY_STANDUP("Daily Standup", "Daily Standup"),
    CODE_REVIEW("Code Review", "Code Review"),
    PR_COMMENTS("Комментарии к PR", "Pull Request Comments"),
    JIRA_TICKETS("Jira-тикеты", "Jira Tickets"),
    GITHUB_ISSUES("GitHub Issues", "GitHub Issues"),
    INTERVIEW("Техническое интервью", "Technical Interview"),
    TEAM_COMMUNICATION("Общение в команде", "Team Communication"),
    BUG_REPORTING("Баг-репорты", "Bug Reporting"),
    SYSTEM_DESIGN_INTERVIEW("System Design Interview", "System Design Interview"),
    INCIDENT_POST_MORTEM("Разбор инцидента (Post-Mortem)", "Incident Post-Mortem"),
    SALARY_NEGOTIATION("Переговоры о зарплате", "Salary Negotiation"),
    CLIENT_COMMUNICATION("Общение с заказчиком", "Client Communication");

    companion object {
        fun fromRaw(raw: String): ScenarioType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TEAM_COMMUNICATION
    }
}

enum class ExerciseType(val labelRu: String) {
    SENTENCE_BUILD("Собери предложение"),
    TRANSLATION_CHOICE("Выбор перевода"),
    PHRASE_CHOICE("Выбор фразы"),
    FILL_BLANKS("Заполни пропуск"),
    INTERVIEW_ANSWER("Ответ интервьюеру"),
    LISTENING_CHOICE("Аудирование");

    companion object {
        fun fromRaw(raw: String): ExerciseType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PHRASE_CHOICE
    }
}

data class UsefulExpression(
    val en: String,
    val ru: String,
    val context: String
)

data class WorkScenario(
    val id: Int,
    val type: ScenarioType,
    val exerciseType: ExerciseType,
    val title: String,
    val prompt: String,
    val promptRu: String,
    val solution: String,
    val options: List<String>,
    val explanationRu: String,
    val usefulExpressions: List<UsefulExpression>,
    val difficulty: String
)

data class ScenarioSubmissionResult(
    val isCorrect: Boolean,
    val userAnswer: String,
    val expectedAnswer: String,
    val explanationRu: String,
    val usefulExpressions: List<UsefulExpression>,
    val xpEarned: Int
)

data class WorkPhrase(
    val id: Int,
    val category: ScenarioType,
    val phraseEn: String,
    val translationRu: String,
    val contextRu: String,
    val difficulty: String
)

enum class ListeningMode(val labelRu: String) {
    CHOOSE("Выбор ответа"),
    DICTATION("Диктант")
}

data class ListeningQuestion(
    val id: String,
    val audioText: String,
    val translationRu: String,
    val contextHintRu: String,
    val options: List<String>,
    val correctOption: String,
    val wordId: Int? = null
)

data class ListeningEvaluation(
    val isCorrect: Boolean,
    val similarityPercent: Int,
    val expectedText: String,
    val userInput: String,
    val xpEarned: Int
)
