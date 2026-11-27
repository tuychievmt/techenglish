package com.techenglish.domain.usecase

import com.techenglish.domain.model.AntiCheatEvaluation
import com.techenglish.domain.model.AntiCheatRejectReason
import com.techenglish.domain.model.ConflictWinner
import com.techenglish.domain.model.GuestMergeResult
import com.techenglish.domain.model.LeagueMovement
import com.techenglish.domain.model.LeagueTier
import com.techenglish.domain.model.RankedEventSubmission
import com.techenglish.domain.model.Sm2ConflictDecision
import com.techenglish.domain.model.Sm2SyncSnapshot
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

// =========================================================================
// Errata E1 & ФТ-9.2 — SM-2 Conflict Resolution Engine
// =========================================================================

/**
 * Resolves SM-2 card conflicts between two devices strictly according to Errata E1:
 * 1. Rejects/clamps future client timestamps > +5 minutes relative to `serverReceivedAtMillis`.
 * 2. Higher sanitized `clientTimestampMillis` wins.
 * 3. Ties break by higher `repetitions`.
 * 4. Remaining ties break by earlier `serverReceivedAtMillis`.
 */
class ResolveSm2SyncConflictUseCase @Inject constructor() {

    operator fun invoke(
        local: Sm2SyncSnapshot,
        remote: Sm2SyncSnapshot
    ): Sm2ConflictDecision {
        val localClamped = clampClientTimestamp(local.clientTimestampMillis, local.serverReceivedAtMillis)
        val remoteClamped = clampClientTimestamp(remote.clientTimestampMillis, remote.serverReceivedAtMillis)
        val wasClamped = localClamped != local.clientTimestampMillis || remoteClamped != remote.clientTimestampMillis

        val localSanitized = local.copy(clientTimestampMillis = localClamped)
        val remoteSanitized = remote.copy(clientTimestampMillis = remoteClamped)

        return when {
            localSanitized.clientTimestampMillis > remoteSanitized.clientTimestampMillis ->
                Sm2ConflictDecision(
                    winner = ConflictWinner.LOCAL,
                    resolvedSnapshot = localSanitized,
                    reason = "local_newer_client_timestamp",
                    futureTimestampClamped = wasClamped
                )
            remoteSanitized.clientTimestampMillis > localSanitized.clientTimestampMillis ->
                Sm2ConflictDecision(
                    winner = ConflictWinner.REMOTE,
                    resolvedSnapshot = remoteSanitized,
                    reason = "remote_newer_client_timestamp",
                    futureTimestampClamped = wasClamped
                )
            localSanitized.repetitions > remoteSanitized.repetitions ->
                Sm2ConflictDecision(
                    winner = ConflictWinner.LOCAL,
                    resolvedSnapshot = localSanitized,
                    reason = "tie_higher_local_repetitions",
                    futureTimestampClamped = wasClamped
                )
            remoteSanitized.repetitions > localSanitized.repetitions ->
                Sm2ConflictDecision(
                    winner = ConflictWinner.REMOTE,
                    resolvedSnapshot = remoteSanitized,
                    reason = "tie_higher_remote_repetitions",
                    futureTimestampClamped = wasClamped
                )
            localSanitized.serverReceivedAtMillis <= remoteSanitized.serverReceivedAtMillis ->
                Sm2ConflictDecision(
                    winner = ConflictWinner.LOCAL,
                    resolvedSnapshot = localSanitized,
                    reason = "tie_earlier_server_receipt",
                    futureTimestampClamped = wasClamped
                )
            else ->
                Sm2ConflictDecision(
                    winner = ConflictWinner.REMOTE,
                    resolvedSnapshot = remoteSanitized,
                    reason = "tie_earlier_server_receipt",
                    futureTimestampClamped = wasClamped
                )
        }
    }

    internal fun clampClientTimestamp(clientTimestampMillis: Long, serverReceivedAtMillis: Long): Long {
        val maxAllowed = serverReceivedAtMillis + MAX_FUTURE_SKEW_MS
        return if (clientTimestampMillis > maxAllowed) serverReceivedAtMillis else clientTimestampMillis
    }

    companion object {
        const val MAX_FUTURE_SKEW_MS = 5L * 60L * 1000L // 5 minutes per Errata E1
    }
}

// =========================================================================
// ФТ-9.3 — Server-Mirrored Anti-Cheat & Ranked XP Validator
// =========================================================================

class ValidateRankedEventAntiCheatUseCase @Inject constructor() {

    operator fun invoke(
        submission: RankedEventSubmission,
        processedEventIds: Set<String>,
        xpEarnedLastMinute: Int,
        xpEarnedToday: Int
    ): AntiCheatEvaluation {
        // 1. Strict idempotency by UUID event_id
        if (submission.eventId in processedEventIds) {
            return AntiCheatEvaluation(
                accepted = false,
                awardedXp = 0,
                clientXpTamperingDetected = false,
                rejectReason = AntiCheatRejectReason.DUPLICATE_EVENT_ID
            )
        }

        // 2. Timestamp bounds check (reject > +5 min future or > 30 days stale)
        if (submission.clientTimestampMillis > submission.serverReceivedAtMillis + ResolveSm2SyncConflictUseCase.MAX_FUTURE_SKEW_MS) {
            return AntiCheatEvaluation(
                accepted = false,
                awardedXp = 0,
                clientXpTamperingDetected = true,
                rejectReason = AntiCheatRejectReason.FUTURE_TIMESTAMP_SKEW
            )
        }
        if (submission.serverReceivedAtMillis - submission.clientTimestampMillis > MAX_OFFLINE_AGE_MS) {
            return AntiCheatEvaluation(
                accepted = false,
                awardedXp = 0,
                clientXpTamperingDetected = false,
                rejectReason = AntiCheatRejectReason.STALE_EVENT_TIMESTAMP
            )
        }

        // 3. Minimum plausible session duration for XP-bearing events
        val canonicalXp = if (submission.isCorrect) {
            submission.eventType.canonicalXp
        } else {
            minOf(5, submission.eventType.canonicalXp)
        }
        val tampered = submission.clientReportedXp != canonicalXp

        if (canonicalXp > 0 && submission.sessionDurationMs < MIN_SESSION_DURATION_MS) {
            return AntiCheatEvaluation(
                accepted = false,
                awardedXp = 0,
                clientXpTamperingDetected = true,
                rejectReason = AntiCheatRejectReason.IMPLAUSIBLE_SESSION_SPEED
            )
        }

        // 4. Rate limits (per-minute & per-day caps)
        if (xpEarnedLastMinute + canonicalXp > MAX_XP_PER_MINUTE) {
            return AntiCheatEvaluation(
                accepted = false,
                awardedXp = 0,
                clientXpTamperingDetected = tampered,
                rejectReason = AntiCheatRejectReason.MINUTE_XP_LIMIT_EXCEEDED
            )
        }
        if (xpEarnedToday + canonicalXp > MAX_XP_PER_DAY) {
            return AntiCheatEvaluation(
                accepted = false,
                awardedXp = 0,
                clientXpTamperingDetected = tampered,
                rejectReason = AntiCheatRejectReason.DAILY_XP_LIMIT_EXCEEDED
            )
        }

        // 5. Server ALWAYS awards canonical XP, ignoring tampered clientReportedXp
        return AntiCheatEvaluation(
            accepted = true,
            awardedXp = canonicalXp,
            clientXpTamperingDetected = tampered,
            rejectReason = null
        )
    }

    companion object {
        const val MIN_SESSION_DURATION_MS = 300L
        const val MAX_XP_PER_MINUTE = 120
        const val MAX_XP_PER_DAY = 2_500
        const val MAX_OFFLINE_AGE_MS = 30L * 24L * 60L * 60L * 1000L
    }
}

// =========================================================================
// ФТ-9.1 & ФТ-9.2 — Guest -> Account Merge UseCase
// =========================================================================

class MergeGuestIntoAccountUseCase @Inject constructor(
    private val resolveSm2Conflict: ResolveSm2SyncConflictUseCase,
    private val antiCheat: ValidateRankedEventAntiCheatUseCase
) {
    operator fun invoke(
        guestSm2: List<Sm2SyncSnapshot>,
        accountSm2: List<Sm2SyncSnapshot>,
        guestEvents: List<RankedEventSubmission>,
        existingAccountEventIds: Set<String>,
        guestFavorites: Set<String>,
        accountFavorites: Set<String>,
        guestCustomTerms: Set<String>,
        accountCustomTerms: Set<String>
    ): GuestMergeResult {
        // 1. Merge SM-2 states using Errata E1
        val byTerm = LinkedHashMap<String, Sm2SyncSnapshot>()
        accountSm2.forEach { byTerm[it.normalizedTerm] = it }
        guestSm2.forEach { local ->
            val existing = byTerm[local.normalizedTerm]
            byTerm[local.normalizedTerm] = if (existing == null) {
                local
            } else {
                resolveSm2Conflict(local, existing).resolvedSnapshot
            }
        }

        // 2. Append-only events deduplicated by event_id + server-recalculated XP
        val seenIds = existingAccountEventIds.toMutableSet()
        var xpSum = 0
        var xpToday = 0
        for (ev in guestEvents) {
            val verdict = antiCheat(
                submission = ev,
                processedEventIds = seenIds,
                xpEarnedLastMinute = 0,
                xpEarnedToday = xpToday
            )
            if (verdict.accepted) {
                seenIds.add(ev.eventId)
                xpSum += verdict.awardedXp
                xpToday += verdict.awardedXp
            }
        }

        // 3. Set union for favorites & custom words
        val mergedFavorites = (accountFavorites + guestFavorites)
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .toSet()
        val mergedCustom = (accountCustomTerms + guestCustomTerms)
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .toSet()

        return GuestMergeResult(
            mergedSm2States = byTerm.values.toList(),
            mergedEventIds = seenIds,
            mergedFavoriteTerms = mergedFavorites,
            mergedCustomTerms = mergedCustom,
            recalculatedServerXp = xpSum
        )
    }
}

// =========================================================================
// ФТ-11.1 — Weekly League Promotion & Asia/Dushanbe (UTC+5) Reset Calculator
// =========================================================================

class CalculateWeeklyLeagueUseCase @Inject constructor() {

    fun calculateWeekStartUtc5Millis(nowMillis: Long): Long {
        val dushanbeOffset = ZoneOffset.ofHours(5)
        val localDate = Instant.ofEpochMilli(nowMillis).atOffset(dushanbeOffset).toLocalDate()
        val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay().toInstant(dushanbeOffset).toEpochMilli()
    }

    fun evaluateMovement(
        currentLeague: LeagueTier,
        rankInCohort: Int,
        cohortSize: Int
    ): Pair<LeagueMovement, LeagueTier> {
        if (cohortSize <= 0 || rankInCohort <= 0) return LeagueMovement.STAYED to currentLeague
        return when {
            rankInCohort <= 3 && currentLeague != LeagueTier.DIAMOND ->
                LeagueMovement.PROMOTED to currentLeague.promote()
            cohortSize >= 6 && rankInCohort > cohortSize - 3 && currentLeague != LeagueTier.BRONZE ->
                LeagueMovement.DEMOTED to currentLeague.demote()
            else ->
                LeagueMovement.STAYED to currentLeague
        }
    }
}

// =========================================================================
// ФТ-9.4, Errata E2, ФТ-12.3 — Profile Validation, Avatar Allowlist & Ping Cooldown
// =========================================================================

class ValidateSocialActionsUseCase @Inject constructor() {

    fun validateUsername(raw: String): Result<String> {
        val normalized = raw.trim().removePrefix("@").lowercase(Locale.ROOT)
        if (!USERNAME_REGEX.matches(normalized)) {
            return Result.failure(
                IllegalArgumentException("Username должен содержать 3–20 символов: латиница, цифры или _.")
            )
        }
        if (normalized in RESERVED_OR_BLOCKED_USERNAMES || BLOCKED_SUBSTRINGS.any { normalized.contains(it) }) {
            return Result.failure(
                IllegalArgumentException("Этот @username зарезервирован или нарушает правила сообщества.")
            )
        }
        return Result.success(normalized)
    }

    /**
     * Errata E2: only preset developer avatars or verified GitHub/Google OAuth avatar HTTPS URLs are allowed.
     */
    fun isAvatarAllowed(avatarKeyOrUrl: String): Boolean {
        val clean = avatarKeyOrUrl.trim()
        if (clean in PRESET_AVATARS) return true
        return ALLOWED_OAUTH_AVATAR_PREFIXES.any { clean.startsWith(it, ignoreCase = false) }
    }

    /**
     * ФТ-12.3: Friend Ping / Nudge is allowed at most once per 24 hours per friend and can be opted out.
     */
    fun canPingFriend(
        acceptsPings: Boolean,
        lastPingedAtMillis: Long,
        nowMillis: Long
    ): Boolean {
        if (!acceptsPings) return false
        if (lastPingedAtMillis <= 0L) return true
        return (nowMillis - lastPingedAtMillis) >= PING_COOLDOWN_MS
    }

    companion object {
        private val USERNAME_REGEX = Regex("^[a-z][a-z0-9_]{2,19}$")
        private val RESERVED_OR_BLOCKED_USERNAMES = setOf(
            "admin", "administrator", "root", "support", "moderator", "system", "techenglish", "official"
        )
        private val BLOCKED_SUBSTRINGS = setOf("spam", "scam", "nazi", "porn")
        val PRESET_AVATARS = setOf(
            "preset_terminal",
            "preset_kotlin",
            "preset_python",
            "preset_rust",
            "preset_linux",
            "preset_architect"
        )
        private val ALLOWED_OAUTH_AVATAR_PREFIXES = listOf(
            "https://avatars.githubusercontent.com/",
            "https://lh3.googleusercontent.com/"
        )
        const val PING_COOLDOWN_MS = 24L * 60L * 60L * 1000L
    }
}
