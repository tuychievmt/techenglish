package com.techenglish.domain.usecase

import com.techenglish.domain.model.ActivityFeedItem
import com.techenglish.domain.model.AntiCheatRejectReason
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.AuthSession
import com.techenglish.domain.model.BadgeItem
import com.techenglish.domain.model.ConflictWinner
import com.techenglish.domain.model.DeveloperProfile
import com.techenglish.domain.model.FriendEntry
import com.techenglish.domain.model.LeaderboardEntry
import com.techenglish.domain.model.LeaderboardTab
import com.techenglish.domain.model.LeagueMovement
import com.techenglish.domain.model.LeagueTier
import com.techenglish.domain.model.OtaContentPackMeta
import com.techenglish.domain.model.RankedEventSubmission
import com.techenglish.domain.model.Sm2SyncSnapshot
import com.techenglish.domain.model.SyncEventType
import com.techenglish.domain.model.SyncQueueEvent
import com.techenglish.domain.model.SyncQueueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class Stage3SyncAndAntiCheatUseCasesTest {

    private val resolveSm2 = ResolveSm2SyncConflictUseCase()
    private val antiCheat = ValidateRankedEventAntiCheatUseCase()
    private val mergeGuest = MergeGuestIntoAccountUseCase(resolveSm2, antiCheat)
    private val weeklyLeague = CalculateWeeklyLeagueUseCase()
    private val validateSocial = ValidateSocialActionsUseCase()

    @Test
    fun `Errata E1 resolves SM-2 conflicts by client_timestamp, clamping over 5m future skew, then repetitions, then server receipt`() {
        val baseServer = 1_800_000_000_000L

        // 1. Newer client_timestamp wins even if received later (offline device syncs later)
        val local = Sm2SyncSnapshot("deadlock", 4, 2.6f, 15, baseServer + 1000, clientTimestampMillis = baseServer - 10_000L, serverReceivedAtMillis = baseServer + 50_000L)
        val remote = Sm2SyncSnapshot("deadlock", 3, 2.5f, 6, baseServer + 500, clientTimestampMillis = baseServer - 30_000L, serverReceivedAtMillis = baseServer)
        val res1 = resolveSm2(local, remote)
        assertEquals(ConflictWinner.LOCAL, res1.winner)
        assertEquals(4, res1.resolvedSnapshot.repetitions)
        assertFalse(res1.futureTimestampClamped)

        // 2. Spoofed future timestamp (> +5 minutes ahead of server receipt) gets clamped to serverReceivedAt
        val cheatedLocal = local.copy(
            clientTimestampMillis = baseServer + 60L * 60L * 1000L, // +1 hour in future!
            serverReceivedAtMillis = baseServer
        )
        val honestRemote = remote.copy(
            clientTimestampMillis = baseServer + 2L * 60L * 1000L, // +2 min within 5 min tolerance
            serverReceivedAtMillis = baseServer
        )
        val res2 = resolveSm2(cheatedLocal, honestRemote)
        assertTrue(res2.futureTimestampClamped)
        assertEquals(ConflictWinner.REMOTE, res2.winner)

        // 3. Equal client_timestamp -> higher repetitions wins
        val tieTimeA = local.copy(clientTimestampMillis = baseServer, repetitions = 2, serverReceivedAtMillis = baseServer)
        val tieTimeB = remote.copy(clientTimestampMillis = baseServer, repetitions = 5, serverReceivedAtMillis = baseServer + 100)
        val res3 = resolveSm2(tieTimeA, tieTimeB)
        assertEquals(ConflictWinner.REMOTE, res3.winner)
        assertEquals(5, res3.resolvedSnapshot.repetitions)

        // 4. Equal client_timestamp & equal repetitions -> earlier serverReceivedAt wins
        val tieAllLocal = tieTimeA.copy(repetitions = 5, serverReceivedAtMillis = baseServer)
        val tieAllRemote = tieTimeB.copy(repetitions = 5, serverReceivedAtMillis = baseServer + 1_000L)
        assertEquals(ConflictWinner.LOCAL, resolveSm2(tieAllLocal, tieAllRemote).winner)
        assertEquals(ConflictWinner.REMOTE, resolveSm2(tieAllRemote, tieAllLocal).winner)
    }

    @Test
    fun `FT-9_3 anti-cheat guarantees event_id idempotency, ignores client XP tampering, and enforces rate limits`() {
        val now = 1_800_000_000_000L

        // 1. Tampered client XP (99999) is ignored; server awards canonical 10 XP and flags tampering
        val tampered = RankedEventSubmission(
            eventId = "uuid-1",
            eventType = SyncEventType.FLASHCARD_CORRECT,
            clientReportedXp = 99_999,
            clientTimestampMillis = now - 5_000L,
            serverReceivedAtMillis = now,
            sessionDurationMs = 4_000L,
            isCorrect = true
        )
        val v1 = antiCheat(tampered, emptySet(), xpEarnedLastMinute = 0, xpEarnedToday = 0)
        assertTrue(v1.accepted)
        assertEquals(10, v1.awardedXp)
        assertTrue(v1.clientXpTamperingDetected)
        assertNull(v1.rejectReason)

        // 2. Duplicate event_id is rejected idempotently with 0 XP
        val vDup = antiCheat(tampered, setOf("uuid-1"), xpEarnedLastMinute = 10, xpEarnedToday = 10)
        assertFalse(vDup.accepted)
        assertEquals(0, vDup.awardedXp)
        assertEquals(AntiCheatRejectReason.DUPLICATE_EVENT_ID, vDup.rejectReason)

        // 3. Future timestamp > +5 min rejected
        val futureEv = tampered.copy(eventId = "uuid-2", clientTimestampMillis = now + 10L * 60L * 1000L)
        assertEquals(AntiCheatRejectReason.FUTURE_TIMESTAMP_SKEW, antiCheat(futureEv, emptySet(), 0, 0).rejectReason)

        // 4. Stale > 30 days rejected
        val staleEv = tampered.copy(eventId = "uuid-3", clientTimestampMillis = now - 40L * 24L * 60L * 60L * 1000L)
        assertEquals(AntiCheatRejectReason.STALE_EVENT_TIMESTAMP, antiCheat(staleEv, emptySet(), 0, 0).rejectReason)

        // 5. Implausible speed (< 300ms) rejected
        val botEv = tampered.copy(eventId = "uuid-4", sessionDurationMs = 50L)
        assertEquals(AntiCheatRejectReason.IMPLAUSIBLE_SESSION_SPEED, antiCheat(botEv, emptySet(), 0, 0).rejectReason)

        // 6. Per-minute & per-day caps enforced
        val okEv = tampered.copy(eventId = "uuid-5", clientReportedXp = 10)
        assertEquals(AntiCheatRejectReason.MINUTE_XP_LIMIT_EXCEEDED, antiCheat(okEv, emptySet(), 115, 115).rejectReason)
        assertEquals(AntiCheatRejectReason.DAILY_XP_LIMIT_EXCEEDED, antiCheat(okEv, emptySet(), 0, 2_495).rejectReason)
    }

    @Test
    fun `FT-9_1 and FT-9_2 guest to account merge preserves all progress without duplicating events`() {
        val now = 1_800_000_000_000L
        val guestSm2 = listOf(
            Sm2SyncSnapshot("deadlock", 4, 2.6f, 15, now + 1000, now - 1000, now),
            Sm2SyncSnapshot("sharding", 2, 2.5f, 6, now + 1000, now - 2000, now)
        )
        val accountSm2 = listOf(
            Sm2SyncSnapshot("deadlock", 1, 2.5f, 1, now + 500, now - 50_000, now - 40_000)
        )
        val guestEvents = listOf(
            RankedEventSubmission("ev-1", SyncEventType.FLASHCARD_CORRECT, 10, now - 2000, now, 3000L, true),
            RankedEventSubmission("ev-1", SyncEventType.FLASHCARD_CORRECT, 10, now - 2000, now, 3000L, true), // duplicate!
            RankedEventSubmission("ev-2", SyncEventType.SCENARIO_CORRECT, 9999, now - 1000, now, 5000L, true), // tampered XP!
            RankedEventSubmission("ev-existing", SyncEventType.DOC_COMPLETED, 15, now - 1000, now, 5000L, true) // already on server!
        )

        val merged = mergeGuest(
            guestSm2 = guestSm2,
            accountSm2 = accountSm2,
            guestEvents = guestEvents,
            existingAccountEventIds = setOf("ev-existing"),
            guestFavorites = setOf("Deadlock", "Mutex"),
            accountFavorites = setOf("mutex", "CAP Theorem"),
            guestCustomTerms = setOf("Singleflight"),
            accountCustomTerms = setOf("Backpressure")
        )

        assertEquals(2, merged.mergedSm2States.size)
        val deadlock = merged.mergedSm2States.first { it.normalizedTerm == "deadlock" }
        assertEquals(4, deadlock.repetitions) // guest newer state won
        assertEquals(setOf("ev-existing", "ev-1", "ev-2"), merged.mergedEventIds)
        assertEquals(30, merged.recalculatedServerXp) // 10 (ev-1) + 20 (ev-2 canonical), ignoring duplicate & tampered 9999!
        assertEquals(setOf("deadlock", "mutex", "cap theorem"), merged.mergedFavoriteTerms)
        assertEquals(setOf("singleflight", "backpressure"), merged.mergedCustomTerms)
    }

    @Test
    fun `FT-11_1 weekly league reset uses Monday 00_00 Asia_Dushanbe UTC+5 and promotes Top-3 and demotes Bottom-3`() {
        // Thursday 2026-11-19 18:00 UTC+5 -> Monday 2026-11-16 00:00 UTC+5
        val utc5 = ZoneOffset.ofHours(5)
        val thursdayMillis = LocalDateTime.of(2026, 11, 19, 18, 0).toInstant(utc5).toEpochMilli()
        val expectedMondayMillis = LocalDateTime.of(2026, 11, 16, 0, 0).toInstant(utc5).toEpochMilli()
        assertEquals(expectedMondayMillis, weeklyLeague.calculateWeekStartUtc5Millis(thursdayMillis))

        assertEquals(LeagueMovement.PROMOTED to LeagueTier.SILVER, weeklyLeague.evaluateMovement(LeagueTier.BRONZE, 1, 30))
        assertEquals(LeagueMovement.STAYED to LeagueTier.DIAMOND, weeklyLeague.evaluateMovement(LeagueTier.DIAMOND, 1, 30))
        assertEquals(LeagueMovement.DEMOTED to LeagueTier.GOLD, weeklyLeague.evaluateMovement(LeagueTier.PLATINUM, 28, 30))
        assertEquals(LeagueMovement.STAYED to LeagueTier.BRONZE, weeklyLeague.evaluateMovement(LeagueTier.BRONZE, 30, 30))
        assertEquals(LeagueMovement.STAYED to LeagueTier.SILVER, weeklyLeague.evaluateMovement(LeagueTier.SILVER, 10, 30))

        assertEquals(AuthProvider.GITHUB, AuthProvider.fromRaw("github"))
        assertEquals(LeaderboardTab.TECH_STACK, LeaderboardTab.fromRaw("tech_stack"))
        assertEquals(LeagueTier.GOLD, LeagueTier.fromRaw("GOLD"))
        assertEquals(SyncEventType.SCENARIO_CORRECT, SyncEventType.fromRaw("SCENARIO_CORRECT"))
    }

    @Test
    fun `Errata E2 and FT-12_3 validate usernames, avatar allowlist, and 24h ping cooldown`() {
        assertTrue(validateSocial.validateUsername("@kotlin_dev99").isSuccess)
        assertEquals("kotlin_dev99", validateSocial.validateUsername("@Kotlin_Dev99").getOrThrow())
        assertTrue(validateSocial.validateUsername("ab").isFailure)
        assertTrue(validateSocial.validateUsername("admin").isFailure)
        assertTrue(validateSocial.validateUsername("user_spam_bot").isFailure)

        assertTrue(validateSocial.isAvatarAllowed("preset_terminal"))
        assertTrue(validateSocial.isAvatarAllowed("https://avatars.githubusercontent.com/u/12345"))
        assertTrue(validateSocial.isAvatarAllowed("https://lh3.googleusercontent.com/a/avatar"))
        assertFalse(validateSocial.isAvatarAllowed("https://evil.example.com/upload.png"))
        assertFalse(validateSocial.isAvatarAllowed("file:///sdcard/photo.jpg"))

        val now = 1_800_000_000_000L
        assertTrue(validateSocial.canPingFriend(acceptsPings = true, lastPingedAtMillis = 0L, nowMillis = now))
        assertFalse(validateSocial.canPingFriend(acceptsPings = false, lastPingedAtMillis = 0L, nowMillis = now))
        assertFalse(validateSocial.canPingFriend(acceptsPings = true, lastPingedAtMillis = now - 60_000L, nowMillis = now))
        assertTrue(validateSocial.canPingFriend(acceptsPings = true, lastPingedAtMillis = now - ValidateSocialActionsUseCase.PING_COOLDOWN_MS, nowMillis = now))
    }

    @Test
    fun `Stage 3 models, fallback parsers, and edge branches work deterministically`() {
        val session = AuthSession()
        assertTrue(session.isGuest)
        assertEquals(AuthProvider.GUEST, AuthProvider.fromRaw("unknown_provider"))
        assertEquals("Гостевой режим", AuthProvider.GUEST.labelRu)

        assertEquals(SyncEventType.FLASHCARD_CORRECT, SyncEventType.fromRaw("unknown_event"))
        assertEquals(LeaderboardTab.WEEKLY, LeaderboardTab.fromRaw("unknown_tab"))
        assertEquals(LeagueTier.BRONZE, LeagueTier.fromRaw("unknown_tier"))
        assertEquals(LeagueTier.DIAMOND, LeagueTier.DIAMOND.promote())
        assertEquals(LeagueTier.BRONZE, LeagueTier.BRONZE.demote())
        assertEquals(LeagueMovement.STAYED to LeagueTier.SILVER, weeklyLeague.evaluateMovement(LeagueTier.SILVER, 0, 0))

        val queueEvent = SyncQueueEvent("ev-q", SyncEventType.DOC_COMPLETED, "{}", 100L)
        assertEquals(SyncQueueStatus.PENDING, queueEvent.status)
        assertEquals(0, queueEvent.retryCount)
        assertEquals("", queueEvent.lastError)

        val profile = DeveloperProfile(userId = "u1", username = "dev_one", displayName = "Dev One")
        assertEquals("preset_terminal", profile.avatarKey)
        assertEquals(LeagueTier.BRONZE, profile.league)
        assertEquals(0, profile.domainMasteryPercent)

        val badge = BadgeItem("b1", "Pioneer", "Desc", "star", true)
        assertEquals(0L, badge.unlockedAtMillis)

        val lbEntry = LeaderboardEntry(1, "u1", "dev_one", "preset_terminal", "Senior", LeagueTier.GOLD, "Kotlin", 150, 900)
        assertFalse(lbEntry.isCurrentUser)

        val friend = FriendEntry("u2", "dev_two", "preset_kotlin", "Middle", 5)
        assertTrue(friend.acceptsPings)
        assertFalse(friend.streakAtRisk)
        assertEquals(0L, friend.lastPingedAtMillis)

        val feedItem = ActivityFeedItem("f1", "u2", "dev_two", "Завершил серию 5 дней", createdAtMillis = 100L)
        assertEquals(0, feedItem.reactionCount)
        assertFalse(feedItem.userReacted)

        val otaMeta = OtaContentPackMeta("pack-1", 1, "AI Pack", "abcd", 50)
        assertEquals(0L, otaMeta.appliedAtMillis)

        // Cover local-higher-repetitions tie break & incorrect event canonical XP cap
        val base = 1_800_000_000_000L
        val localHighReps = Sm2SyncSnapshot("mutex", 6, 2.5f, 10, base, base, base + 500)
        val remoteLowReps = Sm2SyncSnapshot("mutex", 2, 2.5f, 4, base, base, base)
        val res = resolveSm2(localHighReps, remoteLowReps)
        assertEquals(ConflictWinner.LOCAL, res.winner)
        assertEquals("tie_higher_local_repetitions", res.reason)

        val incorrectSub = RankedEventSubmission("ev-inc", SyncEventType.SCENARIO_CORRECT, 5, base - 1000, base, 2000L, isCorrect = false)
        val verdict = antiCheat(incorrectSub, emptySet(), 0, 0)
        assertTrue(verdict.accepted)
        assertEquals(5, verdict.awardedXp)
        assertFalse(verdict.clientXpTamperingDetected)
    }
}
