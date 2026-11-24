package com.techenglish.domain.model

// =========================================================================
// ФТ-9.1 — Auth, Guest Mode & Account Linking
// =========================================================================

enum class AuthProvider(val labelRu: String) {
    GUEST("Гостевой режим"),
    EMAIL("Email / пароль"),
    GITHUB("GitHub OAuth"),
    GOOGLE("Google OAuth");

    companion object {
        fun fromRaw(raw: String): AuthProvider =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GUEST
    }
}

data class AuthSession(
    val userId: String = "guest_local",
    val username: String = "guest_dev",
    val email: String = "",
    val provider: AuthProvider = AuthProvider.GUEST,
    val isGuest: Boolean = true,
    val isEmailVerified: Boolean = false,
    val linkedAtMillis: Long = 0L
)

// =========================================================================
// ФТ-9.2 & Errata E1 — Bidirectional Outbox Sync & SM-2 Conflict Models
// =========================================================================

enum class SyncEventType(val canonicalXp: Int, val maxDurationMs: Long = 3_600_000L) {
    FLASHCARD_CORRECT(canonicalXp = 10),
    FLASHCARD_RETRY(canonicalXp = 5),
    SCENARIO_CORRECT(canonicalXp = 20),
    SCENARIO_ATTEMPT(canonicalXp = 5),
    DOC_COMPLETED(canonicalXp = 15),
    LISTENING_CORRECT(canonicalXp = 15),
    CODE_BREAKDOWN(canonicalXp = 10),
    TIME_ATTACK_COMPLETE(canonicalXp = 25),
    CUSTOM_WORD_UPSERT(canonicalXp = 0),
    FAVORITE_TOGGLE(canonicalXp = 0);

    companion object {
        fun fromRaw(raw: String): SyncEventType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FLASHCARD_CORRECT
    }
}

enum class SyncQueueStatus {
    PENDING,
    SYNCED,
    FAILED
}

data class SyncQueueEvent(
    val eventId: String,
    val eventType: SyncEventType,
    val payloadJson: String,
    val createdAtMillis: Long,
    val retryCount: Int = 0,
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    val lastError: String = ""
)

data class Sm2SyncSnapshot(
    val normalizedTerm: String,
    val repetitions: Int,
    val easeFactor: Float,
    val intervalDays: Int,
    val nextReviewAtMillis: Long,
    val clientTimestampMillis: Long,
    val serverReceivedAtMillis: Long
)

enum class ConflictWinner {
    LOCAL,
    REMOTE
}

data class Sm2ConflictDecision(
    val winner: ConflictWinner,
    val resolvedSnapshot: Sm2SyncSnapshot,
    val reason: String,
    val futureTimestampClamped: Boolean
)

data class GuestMergeResult(
    val mergedSm2States: List<Sm2SyncSnapshot>,
    val mergedEventIds: Set<String>,
    val mergedFavoriteTerms: Set<String>,
    val mergedCustomTerms: Set<String>,
    val recalculatedServerXp: Int
)

// =========================================================================
// ФТ-9.3 — Server-Side Anti-Cheat Models
// =========================================================================

data class RankedEventSubmission(
    val eventId: String,
    val eventType: SyncEventType,
    val clientReportedXp: Int,
    val clientTimestampMillis: Long,
    val serverReceivedAtMillis: Long,
    val sessionDurationMs: Long = 2_000L,
    val isCorrect: Boolean = true
)

enum class AntiCheatRejectReason {
    DUPLICATE_EVENT_ID,
    FUTURE_TIMESTAMP_SKEW,
    STALE_EVENT_TIMESTAMP,
    IMPLAUSIBLE_SESSION_SPEED,
    MINUTE_XP_LIMIT_EXCEEDED,
    DAILY_XP_LIMIT_EXCEEDED
}

data class AntiCheatEvaluation(
    val accepted: Boolean,
    val awardedXp: Int,
    val clientXpTamperingDetected: Boolean,
    val rejectReason: AntiCheatRejectReason? = null
)

// =========================================================================
// ФТ-10.x — Developer Profile & Badges
// =========================================================================

data class DeveloperProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarKey: String = "preset_terminal",
    val statusText: String = "Building offline-first apps",
    val techStack: List<String> = listOf("Kotlin", "Python", "SQL"),
    val githubUrl: String = "",
    val telegramHandle: String = "",
    val rankTitle: String = "Intern Developer",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val league: LeagueTier = LeagueTier.BRONZE,
    val domainMasteryPercent: Int = 0
)

data class BadgeItem(
    val id: String,
    val titleRu: String,
    val descriptionRu: String,
    val iconKey: String,
    val isUnlocked: Boolean,
    val unlockedAtMillis: Long = 0L
)

// =========================================================================
// ФТ-11.x — Segmented Top-10 Leaderboards & Weekly Leagues
// =========================================================================

enum class LeagueTier(val labelRu: String, val tierOrder: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 1),
    GOLD("Gold", 2),
    PLATINUM("Platinum", 3),
    DIAMOND("Diamond", 4);

    fun promote(): LeagueTier = entries.firstOrNull { it.tierOrder == tierOrder + 1 } ?: DIAMOND
    fun demote(): LeagueTier = entries.firstOrNull { it.tierOrder == tierOrder - 1 } ?: BRONZE

    companion object {
        fun fromRaw(raw: String): LeagueTier =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BRONZE
    }
}

enum class LeaderboardTab(val labelRu: String, val apiKey: String) {
    WEEKLY("Неделя", "weekly"),
    LEAGUE("Лига", "league"),
    FRIENDS("Друзья", "friends"),
    TECH_STACK("Стек", "tech_stack");

    companion object {
        fun fromRaw(raw: String): LeaderboardTab =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.apiKey.equals(raw, ignoreCase = true) }
                ?: WEEKLY
    }
}

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val avatarKey: String,
    val rankTitle: String,
    val league: LeagueTier,
    val techStack: String,
    val weeklyXp: Int,
    val totalXp: Int,
    val isCurrentUser: Boolean = false
)

enum class LeagueMovement {
    PROMOTED,
    STAYED,
    DEMOTED
}

// =========================================================================
// ФТ-12.x & ФТ-9.4 — Friends, Feed, Ping & Profile Moderation (Errata E2)
// =========================================================================

data class FriendEntry(
    val userId: String,
    val username: String,
    val avatarKey: String,
    val rankTitle: String,
    val streakDays: Int,
    val streakAtRisk: Boolean = false,
    val lastPingedAtMillis: Long = 0L,
    val acceptsPings: Boolean = true
)

data class ActivityFeedItem(
    val id: String,
    val actorUserId: String,
    val actorUsername: String,
    val summaryRu: String,
    val reactionCount: Int = 0,
    val userReacted: Boolean = false,
    val createdAtMillis: Long
)

data class OtaContentPackMeta(
    val packId: String,
    val version: Int,
    val titleRu: String,
    val sha256Hex: String,
    val itemCount: Int,
    val appliedAtMillis: Long = 0L
)
