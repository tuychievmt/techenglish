package com.techenglish.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.techenglish.app.BuildConfig
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.AuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RegisterBodyDto(
    val email: String,
    val password: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("age_confirmed_16_plus") val ageConfirmed16Plus: Boolean = true
)

@Serializable
data class LoginBodyDto(
    val email: String,
    val password: String
)

@Serializable
data class WsTicketResponseDto(
    val ticket: String,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int = 30
)

@Serializable
data class RegisterDeviceTokenBodyDto(
    val token: String,
    val platform: String = "ANDROID"
)

@Serializable
data class UnregisterDeviceTokenBodyDto(
    val token: String? = null
)

@Serializable
data class OAuthTicketExchangeBodyDto(
    val ticket: String
)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: String,
    val username: String,
    val provider: String = "EMAIL",
    @SerialName("is_email_verified") val isEmailVerified: Boolean = false
)

@Serializable
data class SyncEventBodyDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("client_reported_xp") val clientReportedXp: Int,
    @SerialName("client_timestamp_ms") val clientTimestampMs: Long,
    @SerialName("session_duration_ms") val sessionDurationMs: Long = 3000L,
    @SerialName("is_correct") val isCorrect: Boolean = true,
    @SerialName("payload_json") val payloadJson: String = "{}"
)

@Serializable
data class RefreshTokenBodyDto(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class Sm2ItemPayloadDto(
    @SerialName("normalized_term") val normalizedTerm: String,
    val repetitions: Int,
    @SerialName("ease_factor") val easeFactor: Float,
    @SerialName("interval_days") val intervalDays: Int,
    @SerialName("next_review_at_ms") val nextReviewAtMs: Long,
    @SerialName("client_timestamp_ms") val clientTimestampMs: Long
)

@Serializable
data class Sm2StateRemoteDto(
    @SerialName("normalized_term") val normalizedTerm: String,
    val repetitions: Int,
    @SerialName("ease_factor") val easeFactor: Float,
    @SerialName("interval_days") val intervalDays: Int,
    @SerialName("next_review_at_ms") val nextReviewAtMs: Long,
    @SerialName("client_timestamp_ms") val clientTimestampMs: Long,
    @SerialName("server_received_ms") val serverReceivedMs: Long = 0L
)

@Serializable
data class SyncStateResponseDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("server_total_xp") val serverTotalXp: Int = 0,
    @SerialName("rank_title") val rankTitle: String = "",
    val league: String = "BRONZE",
    @SerialName("sm2_states") val sm2States: List<Sm2StateRemoteDto> = emptyList(),
    @SerialName("favorite_terms") val favoriteTerms: List<String> = emptyList(),
    @SerialName("custom_words") val customWords: Map<String, String> = emptyMap()
)

@Serializable
data class SyncBatchBodyDto(
    val events: List<SyncEventBodyDto> = emptyList(),
    @SerialName("sm2_updates") val sm2Updates: List<Sm2ItemPayloadDto> = emptyList(),
    @SerialName("favorite_terms") val favoriteTerms: List<String> = emptyList(),
    @SerialName("custom_words") val customWords: Map<String, String> = emptyMap()
)

@Serializable
data class RejectedSyncEventDto(
    @SerialName("event_id") val eventId: String = "",
    val reason: String = ""
)

@Serializable
data class SyncBatchResponseDto(
    @SerialName("accepted_event_ids") val acceptedEventIds: List<String> = emptyList(),
    @SerialName("duplicate_event_ids") val duplicateEventIds: List<String> = emptyList(),
    @SerialName("rejected_events") val rejectedEvents: List<RejectedSyncEventDto> = emptyList(),
    @SerialName("awarded_xp") val awardedXp: Int = 0,
    @SerialName("server_total_xp") val serverTotalXp: Int = 0
)

class ApiHttpException(val statusCode: Int, val detail: String) : Exception(detail)

@Serializable
private data class ApiErrorDetailDto(val detail: String = "")

@Serializable
data class LeaderboardRowDto(
    val rank: Int,
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("avatar_key") val avatarKey: String = "preset_terminal",
    @SerialName("rank_title") val rankTitle: String,
    val league: String,
    @SerialName("tech_stack") val techStack: String = "",
    @SerialName("weekly_xp") val weeklyXp: Int = 0,
    @SerialName("total_xp") val totalXp: Int = 0,
    @SerialName("is_current_user") val isCurrentUser: Boolean = false
)

@Serializable
data class LeaderboardResponseDto(
    val tab: String,
    @SerialName("top_10") val top10: List<LeaderboardRowDto> = emptyList(),
    @SerialName("current_user_entry") val currentUserEntry: LeaderboardRowDto? = null
)

@Serializable
data class FriendRowDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("avatar_key") val avatarKey: String = "preset_terminal",
    @SerialName("rank_title") val rankTitle: String,
    @SerialName("streak_days") val streakDays: Int = 0,
    @SerialName("accepts_pings") val acceptsPings: Boolean = true,
    @SerialName("last_pinged_at_ms") val lastPingedAtMs: Long = 0L
)

@Serializable
data class FriendsListResponseDto(
    val friends: List<FriendRowDto> = emptyList()
)

@Serializable
data class FeedItemDto(
    val id: String,
    @SerialName("actor_user_id") val actorUserId: String,
    @SerialName("actor_username") val actorUsername: String,
    @SerialName("summary_ru") val summaryRu: String,
    @SerialName("reaction_count") val reactionCount: Int = 0,
    @SerialName("user_reacted") val userReacted: Boolean = false,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L
)

@Serializable
data class FeedResponseDto(
    val items: List<FeedItemDto> = emptyList()
)

@Serializable
data class OtaPackManifestItemDto(
    @SerialName("pack_id") val packId: String,
    val version: Int,
    @SerialName("title_ru") val titleRu: String,
    val domain: String = "",
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("sha256_hex") val sha256Hex: String,
    @SerialName("data_only") val dataOnly: Boolean = true,
    @SerialName("download_url") val downloadUrl: String? = null
)

@Serializable
data class OtaManifestResponseDto(
    val packs: List<OtaPackManifestItemDto> = emptyList()
)

@Serializable
data class OtaPackDownloadDto(
    @SerialName("pack_id") val packId: String,
    @SerialName("sha256_hex") val sha256Hex: String,
    @SerialName("payload_json") val payloadJson: String
)

@Serializable
data class AppVersionMetadataDto(
    @SerialName("latest_version_code") val latestVersionCode: Int = 0,
    @SerialName("latest_version_name") val latestVersionName: String = "",
    @SerialName("min_supported_version_code") val minSupportedVersionCode: Int = 0,
    @SerialName("force_update") val forceUpdate: Boolean = false,
    @SerialName("apk_url") val apkUrl: String = "",
    @SerialName("apk_sha256") val apkSha256: String = "",
    @SerialName("changelog_ru") val changelogRu: String = ""
)

@Serializable
data class ModerationReportBodyDto(
    @SerialName("target_type") val targetType: String = "PROFILE",
    @SerialName("target_id") val targetId: String,
    val reason: String
)

@Serializable
data class AddFriendBodyDto(val username: String)

// Stage 4A DTOs (ФТ-13.1 Comments/Mnemonics & ФТ-13.2 Community Decks)

@Serializable
data class CommentItemDto(
    val id: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: Int,
    @SerialName("author_user_id") val authorUserId: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("author_rank_title") val authorRankTitle: String = "Intern Developer",
    val body: String,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    @SerialName("user_vote") val userVote: Int = 0,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L
)

@Serializable
data class CommentsListResponseDto(
    val items: List<CommentItemDto> = emptyList()
)

@Serializable
data class CreateCommentBodyDto(
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: Int,
    val body: String
)

@Serializable
data class VoteCommentBodyDto(val vote: Int)

@Serializable
data class CommunityDeckTermDto(
    val term: String,
    val transcription: String = "",
    val translation: String,
    val example: String = "",
    @SerialName("explanation_ru") val explanationRu: String = ""
)

@Serializable
data class CommunityDeckSummaryDto(
    val id: String,
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    @SerialName("author_user_id") val authorUserId: String,
    @SerialName("author_username") val authorUsername: String,
    val rating: Float = 0.0f,
    @SerialName("downloads_count") val downloadsCount: Int = 0,
    @SerialName("term_count") val termCount: Int = 0,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L
)

@Serializable
data class CommunityDecksListResponseDto(
    val decks: List<CommunityDeckSummaryDto> = emptyList()
)

@Serializable
data class PublishDeckBodyDto(
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val terms: List<CommunityDeckTermDto> = emptyList()
)

@Serializable
data class DownloadDeckResponseDto(
    @SerialName("deck_id") val deckId: String,
    val title: String,
    @SerialName("downloads_count") val downloadsCount: Int = 0,
    val terms: List<CommunityDeckTermDto> = emptyList()
)

@Serializable
data class RateDeckBodyDto(val stars: Int)

@Serializable
data class RateDeckResponseDto(
    @SerialName("deck_id") val deckId: String,
    val rating: Float
)

// Stage 4B DTOs (AI Mentor Proxy ФТ-8.1–8.3, Squads ФТ-14.4, Crowdsourcing ФТ-14.3)

@Serializable
data class AiQuotaResponseDto(
    @SerialName("daily_limit") val dailyLimit: Int = 15,
    @SerialName("used_today") val usedToday: Int = 0,
    @SerialName("remaining_today") val remainingToday: Int = 15
)

@Serializable
data class AiInterviewRequestDto(
    val role: String,
    val level: String,
    @SerialName("question_en") val questionEn: String,
    @SerialName("user_answer_text") val userAnswerText: String,
    @SerialName("turn_index") val turnIndex: Int = 0,
    @SerialName("consent_accepted") val consentAccepted: Boolean = true
)

@Serializable
data class AiInterviewResponseDto(
    val provider: String? = null,
    @SerialName("is_fallback") val isFallback: Boolean? = null,
    @SerialName("fallback_reason") val fallbackReason: String? = null,
    @SerialName("question_en") val questionEn: String = "",
    @SerialName("english_score") val englishScore: Int = 80,
    @SerialName("technical_score") val technicalScore: Int = 80,
    @SerialName("feedback_ru") val feedbackRu: String = "",
    @SerialName("improved_answer_en") val improvedAnswerEn: String = "",
    @SerialName("next_question_en") val nextQuestionEn: String = "",
    @SerialName("remaining_daily_quota") val remainingDailyQuota: Int = 14
)

@Serializable
data class AiCodeReviewRequestDto(
    @SerialName("draft_text") val draftText: String,
    val context: String = "pull_request",
    @SerialName("consent_accepted") val consentAccepted: Boolean = true
)

@Serializable
data class AiCodeReviewResponseDto(
    val provider: String? = null,
    @SerialName("is_fallback") val isFallback: Boolean? = null,
    @SerialName("fallback_reason") val fallbackReason: String? = null,
    @SerialName("tone_assessment_ru") val toneAssessmentRu: String = "",
    @SerialName("improved_english_text") val improvedEnglishText: String = "",
    @SerialName("key_suggestions_ru") val keySuggestionsRu: List<String> = emptyList(),
    @SerialName("remaining_daily_quota") val remainingDailyQuota: Int = 14
)

@Serializable
data class SquadMemberDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("rank_title") val rankTitle: String,
    @SerialName("weekly_xp") val weeklyXp: Int = 0
)

@Serializable
data class SquadDto(
    val id: String,
    val name: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("weekly_goal_xp") val weeklyGoalXp: Int = 1000,
    @SerialName("current_week_xp") val currentWeekXp: Int = 0,
    @SerialName("next_week_bonus_multiplier") val nextWeekBonusMultiplier: Float = 1.0f,
    @SerialName("is_active_for_challenge") val isActiveForChallenge: Boolean = false,
    val members: List<SquadMemberDto> = emptyList()
)

@Serializable
data class MySquadResponseDto(val squad: SquadDto? = null)

@Serializable
data class CreateSquadBodyDto(
    val name: String,
    @SerialName("weekly_goal_xp") val weeklyGoalXp: Int = 1000
)

@Serializable
data class JoinSquadBodyDto(@SerialName("invite_code") val inviteCode: String)

@Serializable
data class CrowdsourceEligibilityResponseDto(
    @SerialName("is_eligible") val isEligible: Boolean = false,
    @SerialName("meets_rank_middle_plus") val meetsRankMiddlePlus: Boolean = false,
    @SerialName("meets_account_age_14_days") val meetsAccountAge14Days: Boolean = false,
    @SerialName("meets_learned_terms_100") val meetsLearnedTerms100: Boolean = false,
    @SerialName("meets_zero_confirmed_reports") val meetsZeroConfirmedReports: Boolean = true,
    @SerialName("total_xp") val totalXp: Int = 0,
    @SerialName("account_age_days") val accountAgeDays: Int = 0,
    @SerialName("learned_terms_count") val learnedTermsCount: Int = 0,
    @SerialName("confirmed_reports") val confirmedReports: Int = 0,
    @SerialName("unmet_reasons_ru") val unmetReasonsRu: List<String> = emptyList()
)

@Serializable
data class DictionaryPrDto(
    val id: String,
    val term: String,
    @SerialName("proposed_translation_ru") val proposedTranslationRu: String = "",
    @SerialName("proposed_translation_tg") val proposedTranslationTg: String = "",
    @SerialName("proposed_definition_ru") val proposedDefinitionRu: String = "",
    @SerialName("source_reference") val sourceReference: String = "",
    @SerialName("author_username") val authorUsername: String = "",
    val status: String = "OPEN",
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    @SerialName("license_accepted") val licenseAccepted: Boolean = true,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L
)

@Serializable
data class DictionaryPrListResponseDto(val items: List<DictionaryPrDto> = emptyList())

@Serializable
data class SubmitDictionaryPrBodyDto(
    val term: String,
    @SerialName("proposed_translation_ru") val proposedTranslationRu: String,
    @SerialName("proposed_translation_tg") val proposedTranslationTg: String,
    @SerialName("proposed_definition_ru") val proposedDefinitionRu: String,
    @SerialName("source_reference") val sourceReference: String,
    @SerialName("license_accepted") val licenseAccepted: Boolean
)

// Stage 4C DTOs (Thematic Rooms WS ФТ-13.3, PvP 1v1 ФТ-14.1, Peer Review Arena ФТ-14.2)

@Serializable
data class RoomMessageDto(
    val id: String = "",
    @SerialName("room_slug") val roomSlug: String = "",
    @SerialName("author_user_id") val authorUserId: String = "",
    @SerialName("author_username") val authorUsername: String = "",
    @SerialName("author_rank_title") val authorRankTitle: String = "Developer",
    val body: String = "",
    @SerialName("created_at_ms") val createdAtMs: Long = 0L
)

@Serializable
data class RoomMessagesListResponseDto(val messages: List<RoomMessageDto> = emptyList())

@Serializable
data class PostRoomMessageBodyDto(val body: String)

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Serializable
data class PvpQuestionDto(
    val id: String,
    @SerialName("prompt_en") val promptEn: String,
    val options: List<String> = emptyList(),
    @SerialName("correct_option_index") val correctOptionIndex: Int = -1,
    @SerialName("category_tag") val categoryTag: String = "VOCAB"
)

@Serializable
data class PvpWsEventDto(
    val type: String = "",
    @SerialName("match_id") val matchId: String = "",
    val mode: String = "",
    @SerialName("question_index") val questionIndex: Int = -1,
    @SerialName("is_correct") val isCorrect: Boolean = false,
    @SerialName("correct_option_index") val correctOptionIndex: Int = -1,
    val score: Int = 0,
    val questions: List<PvpQuestionDto> = emptyList()
)

@Serializable
data class PvpMatchmakeBodyDto(@SerialName("force_training_fallback") val forceTrainingFallback: Boolean = false)

@Serializable
data class PvpMatchmakeResponseDto(
    @SerialName("match_id") val matchId: String,
    val mode: String = "TRAINING_FALLBACK",
    val state: String = "IN_PROGRESS",
    @SerialName("awards_ranked_points") val awardsRankedPoints: Boolean = false,
    @SerialName("round_duration_seconds") val roundDurationSeconds: Int = 60,
    val questions: List<PvpQuestionDto> = emptyList()
)

@Serializable
data class PvpSubmitBodyDto(val answers: List<Int> = emptyList())

@Serializable
data class PvpSubmitResponseDto(
    @SerialName("match_id") val matchId: String,
    val mode: String,
    val state: String,
    @SerialName("player_score") val playerScore: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 8,
    @SerialName("ranked_xp_awarded") val rankedXpAwarded: Int = 0,
    @SerialName("rating_delta") val ratingDelta: Int = 0
)

@Serializable
data class PvpDisconnectBodyDto(val reason: String = "network_drop")

@Serializable
data class PeerReviewCaseDto(
    @SerialName("case_id") val caseId: String,
    @SerialName("date_iso") val dateIso: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("prompt_en") val promptEn: String,
    @SerialName("prompt_ru") val promptRu: String,
    @SerialName("target_sentences_count") val targetSentencesCount: Int = 3,
    @SerialName("voice_max_seconds") val voiceMaxSeconds: Int = 20,
    @SerialName("voice_ttl_days") val voiceTtlDays: Int = 30
)

@Serializable
data class PeerReviewFeedbackDto(
    val id: String,
    @SerialName("reviewer_username") val reviewerUsername: String,
    @SerialName("reviewer_rank_title") val reviewerRankTitle: String = "Developer",
    @SerialName("rating_stars") val ratingStars: Int = 5,
    @SerialName("comment_text") val commentText: String,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L
)

@Serializable
data class PeerReviewSubmissionDto(
    val id: String,
    @SerialName("case_id") val caseId: String,
    @SerialName("author_user_id") val authorUserId: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("author_rank_title") val authorRankTitle: String = "Developer",
    @SerialName("text_response") val textResponse: String = "",
    @SerialName("has_voice_clip") val hasVoiceClip: Boolean = false,
    @SerialName("voice_duration_seconds") val voiceDurationSeconds: Int = 0,
    @SerialName("voice_expires_at_ms") val voiceExpiresAtMs: Long = 0L,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L,
    val feedback: List<PeerReviewFeedbackDto> = emptyList()
)

@Serializable
data class PeerReviewSubmissionsResponseDto(val items: List<PeerReviewSubmissionDto> = emptyList())

@Serializable
data class PeerReviewSubmitBodyDto(
    @SerialName("case_id") val caseId: String,
    @SerialName("text_response") val textResponse: String,
    @SerialName("voice_base64") val voiceBase64: String? = null,
    @SerialName("voice_duration_seconds") val voiceDurationSeconds: Int = 0,
    @SerialName("voice_content_type") val voiceContentType: String = "audio/mp4"
)

@Serializable
data class PeerReviewFeedbackBodyDto(
    @SerialName("rating_stars") val ratingStars: Int,
    @SerialName("comment_text") val commentText: String
)

@Serializable
data class PeerReviewVoiceUrlResponseDto(
    @SerialName("submission_id") val submissionId: String = "",
    @SerialName("voice_url") val voiceUrl: String = "",
    @SerialName("download_url") val legacyDownloadUrl: String = "",
    @SerialName("expires_in_seconds") val expiresInSeconds: Int = 300
) {
    val downloadUrl: String
        get() = voiceUrl.ifBlank { legacyDownloadUrl }
}

@Serializable
data class ProfileBadgeDto(
    val id: String = "",
    @SerialName("title_ru") val titleRu: String = "",
    val unlocked: Boolean = false
)

@Serializable
data class ProfileResponseDto(
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_key") val avatarKey: String = "preset_terminal",
    @SerialName("status_text") val statusText: String = "",
    @SerialName("tech_stack") val techStack: List<String> = emptyList(),
    @SerialName("github_url") val githubUrl: String = "",
    @SerialName("telegram_handle") val telegramHandle: String = "",
    @SerialName("rank_title") val rankTitle: String = "Intern Developer",
    @SerialName("total_xp") val totalXp: Int = 0,
    @SerialName("streak_days") val streakDays: Int = 0,
    val league: String = "BRONZE",
    @SerialName("accepts_pings") val acceptsPings: Boolean = true,
    val badges: List<ProfileBadgeDto> = emptyList()
)

@Serializable
data class ProfileUpdateBodyDto(
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_key") val avatarKey: String? = null,
    @SerialName("status_text") val statusText: String? = null,
    @SerialName("tech_stack") val techStack: List<String>? = null,
    @SerialName("github_url") val githubUrl: String? = null,
    @SerialName("telegram_handle") val telegramHandle: String? = null,
    @SerialName("accepts_pings") val acceptsPings: Boolean? = null
)

@Serializable
data class FeedReactResponseDto(
    val id: String = "",
    @SerialName("reaction_count") val reactionCount: Int = 0,
    @SerialName("user_reacted") val userReacted: Boolean = true
)

/**
 * §6.1 — Single Android network stack based on Ktor Client (HTTP + WebSockets) + Kotlinx Serialization (no Retrofit).
 */
@Singleton
class TechEnglishApiClient internal constructor(
    private val context: Context,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val engineOverride: HttpClientEngine? = null
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        tokenStore: AndroidKeystoreTokenStore
    ) : this(context = context, tokenStore = tokenStore, engineOverride = null)

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun isPublicAuthEndpoint(path: String): Boolean {
        return path.startsWith("/v1/auth/login") ||
            path.startsWith("/v1/auth/register") ||
            path.startsWith("/v1/auth/refresh") ||
            path.startsWith("/v1/auth/oauth") ||
            path.startsWith("/v1/auth/verify-email") ||
            path.startsWith("/v1/auth/password-reset") ||
            path.startsWith("/v1/legal/") ||
            path.startsWith("/v1/ota/") ||
            path == "/v1/app/version" ||
            path == "/v1/peer-review/daily-case" ||
            path == "/health"
    }

    private val authCacheLock = Any()

    @Volatile
    private var cachedAccessTokenSnapshot: String? = null

    private suspend fun HttpResponse.requireSuccess(): HttpResponse {
        if (status.isSuccess()) return this
        val rawBody = runCatching { bodyAsText() }.getOrDefault("")
        val parsedDetail = runCatching {
            if (rawBody.trimStart().startsWith("{")) {
                jsonConfig.decodeFromString<ApiErrorDetailDto>(rawBody).detail.trim()
            } else {
                ""
            }
        }.getOrDefault("")
        val message = parsedDetail.ifBlank {
            rawBody.trim().take(200).ifBlank { "HTTP ${status.value}: ${status.description}" }
        }
        throw ApiHttpException(status.value, message)
    }

    private suspend fun resolveBaseUrl(): String =
        tokenStore.getApiBaseUrl().ifBlank { DEFAULT_BASE_URL }.trimEnd('/')

    private val httpClient: HttpClient by lazy {
        val engine = engineOverride ?: OkHttp.create {
            config {
                connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                pingInterval(20, TimeUnit.SECONDS)
            }
        }
        val client = HttpClient(engine) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(WebSockets) {
                pingInterval = 20_000L
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val access = tokenStore.getAccessToken()
                        val refresh = tokenStore.getRefreshToken()
                        synchronized(authCacheLock) {
                            cachedAccessTokenSnapshot = access
                        }
                        if (!access.isNullOrBlank()) {
                            BearerTokens(accessToken = access, refreshToken = refresh.orEmpty())
                        } else {
                            null
                        }
                    }
                    refreshTokens {
                        val refresh = tokenStore.getRefreshToken()?.takeIf { it.isNotBlank() }
                            ?: oldTokens?.refreshToken?.takeIf { it.isNotBlank() }
                            ?: return@refreshTokens null
                        val base = resolveBaseUrl()
                        val resp = try {
                            client.post("$base/v1/auth/refresh") {
                                markAsRefreshTokenRequest()
                                contentType(ContentType.Application.Json)
                                setBody(RefreshTokenBodyDto(refreshToken = refresh))
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Exception) {
                            return@refreshTokens null
                        }

                        if (!resp.status.isSuccess()) {
                            if (resp.status == HttpStatusCode.Unauthorized || resp.status == HttpStatusCode.Forbidden) {
                                tokenStore.clearSession()
                                synchronized(authCacheLock) {
                                    cachedAccessTokenSnapshot = null
                                    clearAuthCache(client)
                                }
                            }
                            return@refreshTokens null
                        }
                        val refreshed = try {
                            resp.body<AuthResponseDto>()
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Exception) {
                            return@refreshTokens null
                        }
                        val updated = tokenStore.updateTokens(
                            accessToken = refreshed.accessToken,
                            refreshToken = refreshed.refreshToken
                        )
                        if (!updated) {
                            synchronized(authCacheLock) {
                                cachedAccessTokenSnapshot = null
                                clearAuthCache(client)
                            }
                            return@refreshTokens null
                        }
                        synchronized(authCacheLock) {
                            cachedAccessTokenSnapshot = refreshed.accessToken
                        }
                        BearerTokens(
                            accessToken = refreshed.accessToken,
                            refreshToken = refreshed.refreshToken
                        )
                    }
                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        path.startsWith("/v1/") && !isPublicAuthEndpoint(path)
                    }
                }
            }
        }
        // Keep Ktor BearerAuthProvider synchronized with AndroidKeystoreTokenStore whenever tokens change
        // (e.g., immediately after /v1/auth/login, account switch, or logout).
        client.requestPipeline.intercept(HttpRequestPipeline.Before) {
            val path = context.url.encodedPath
            if (path.startsWith("/v1/") && !isPublicAuthEndpoint(path)) {
                val latestAccess = tokenStore.getAccessToken()
                synchronized(authCacheLock) {
                    if (latestAccess != cachedAccessTokenSnapshot) {
                        cachedAccessTokenSnapshot = latestAccess
                        clearAuthCache(client)
                    }
                }
            }
        }
        client
    }

    private fun clearAuthCache(client: HttpClient) {
        runCatching {
            client.plugin(Auth).providers
                .filterIsInstance<BearerAuthProvider>()
                .forEach { it.clearToken() }
        }
    }

    fun clearAuthCache() {
        synchronized(authCacheLock) {
            cachedAccessTokenSnapshot = null
            clearAuthCache(httpClient)
        }
    }

    fun isNetworkAvailable(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

    suspend fun register(body: RegisterBodyDto): AuthResponseDto {
        val base = resolveBaseUrl()
        val response: AuthResponseDto = httpClient.post("$base/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess().body()
        tokenStore.saveTokensAndSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.userId,
            username = response.username,
            email = body.email.trim(),
            provider = AuthProvider.fromRaw(response.provider)
        )
        synchronized(authCacheLock) {
            cachedAccessTokenSnapshot = response.accessToken
            clearAuthCache(httpClient)
        }
        return response
    }

    suspend fun login(body: LoginBodyDto): AuthResponseDto {
        val base = resolveBaseUrl()
        val response: AuthResponseDto = httpClient.post("$base/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess().body()
        tokenStore.saveTokensAndSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.userId,
            username = response.username,
            email = body.email.trim(),
            provider = AuthProvider.fromRaw(response.provider)
        )
        synchronized(authCacheLock) {
            cachedAccessTokenSnapshot = response.accessToken
            clearAuthCache(httpClient)
        }
        return response
    }

    suspend fun exchangeOAuthTicket(ticket: String): AuthResponseDto {
        val base = resolveBaseUrl()
        val response: AuthResponseDto = httpClient.post("$base/v1/auth/oauth/ticket/exchange") {
            contentType(ContentType.Application.Json)
            setBody(OAuthTicketExchangeBodyDto(ticket = ticket))
        }.requireSuccess().body()
        synchronized(authCacheLock) {
            clearAuthCache(httpClient)
        }
        return response
    }

    suspend fun syncEvents(batch: SyncBatchBodyDto): SyncBatchResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/sync/events") {
            contentType(ContentType.Application.Json)
            setBody(batch)
        }.requireSuccess().body()
    }

    suspend fun fetchSyncState(): SyncStateResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/sync/state").requireSuccess().body()
    }

    suspend fun linkGuestProgress(batch: SyncBatchBodyDto): SyncBatchResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/auth/guest/link") {
            contentType(ContentType.Application.Json)
            setBody(batch)
        }.requireSuccess().body()
    }

    suspend fun deleteAccount() {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.delete("$base/v1/account").requireSuccess()
    }

    suspend fun fetchLeaderboard(tabApiKey: String): LeaderboardResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/leaderboards/$tabApiKey").requireSuccess().body()
    }

    suspend fun fetchFriends(): FriendsListResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/friends").requireSuccess().body()
    }

    suspend fun addFriend(username: String) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/friends/add") {
            contentType(ContentType.Application.Json)
            setBody(AddFriendBodyDto(username))
        }.requireSuccess()
    }

    suspend fun pingFriend(friendId: String) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/friends/$friendId/ping").requireSuccess()
    }

    suspend fun registerPushDeviceToken(deviceToken: String, platform: String = "ANDROID"): Boolean {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: return false
        val response = httpClient.post("$base/v1/notifications/device-token") {
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceTokenBodyDto(token = deviceToken, platform = platform))
        }
        return response.status.isSuccess()
    }

    suspend fun unregisterPushDeviceToken(deviceToken: String? = null): Boolean {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: return false
        val response = httpClient.delete("$base/v1/notifications/device-token") {
            contentType(ContentType.Application.Json)
            setBody(UnregisterDeviceTokenBodyDto(token = deviceToken))
        }
        return response.status.isSuccess()
    }

    suspend fun fetchActivityFeed(): FeedResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/friends/feed").requireSuccess().body()
    }

    suspend fun reactToFeedEvent(eventId: String): FeedReactResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/friends/feed/$eventId/react").requireSuccess().body()
    }

    suspend fun fetchMyProfile(): ProfileResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/profile/me").requireSuccess().body()
    }

    suspend fun updateMyProfile(body: ProfileUpdateBodyDto): ProfileResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.patch("$base/v1/profile/me") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess().body()
    }

    suspend fun reportUser(targetId: String, reason: String) {
        reportContent(targetType = "PROFILE", targetId = targetId, reason = reason)
    }

    suspend fun reportContent(targetType: String, targetId: String, reason: String) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/moderation/report") {
            contentType(ContentType.Application.Json)
            setBody(ModerationReportBodyDto(targetType = targetType, targetId = targetId, reason = reason))
        }.requireSuccess()
    }

    suspend fun blockUser(targetUserId: String) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/moderation/block/$targetUserId").requireSuccess()
    }

    // Stage 4A — Comments & Mnemonics (ФТ-13.1)

    suspend fun fetchComments(targetType: String, targetId: Int, sort: String = "top"): CommentsListResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/comments/$targetType/$targetId?sort=$sort").requireSuccess().body()
    }

    suspend fun postComment(targetType: String, targetId: Int, bodyText: String): CommentItemDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/comments") {
            contentType(ContentType.Application.Json)
            setBody(CreateCommentBodyDto(targetType = targetType, targetId = targetId, body = bodyText))
        }.requireSuccess().body()
    }

    suspend fun voteComment(commentId: String, vote: Int) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/comments/$commentId/vote") {
            contentType(ContentType.Application.Json)
            setBody(VoteCommentBodyDto(vote = vote))
        }.requireSuccess()
    }

    // Stage 4A — Community Decks (ФТ-13.2)

    suspend fun fetchCommunityDecks(): CommunityDecksListResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/decks").requireSuccess().body()
    }

    suspend fun publishCommunityDeck(body: PublishDeckBodyDto): CommunityDeckSummaryDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/decks") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess().body()
    }

    suspend fun downloadCommunityDeck(deckId: String): DownloadDeckResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/decks/$deckId/download").requireSuccess().body()
    }

    suspend fun rateCommunityDeck(deckId: String, stars: Int): RateDeckResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/decks/$deckId/rate") {
            contentType(ContentType.Application.Json)
            setBody(RateDeckBodyDto(stars = stars))
        }.requireSuccess().body()
    }

    // Stage 4B — AI Mentor Server Proxy (ФТ-8.1–8.3 — zero client API keys)

    suspend fun fetchAiQuota(): AiQuotaResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/ai/quota").requireSuccess().body()
    }

    suspend fun submitAiInterview(body: AiInterviewRequestDto): AiInterviewResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/ai/interview") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess().body()
    }

    suspend fun submitAiCodeReview(body: AiCodeReviewRequestDto): AiCodeReviewResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/ai/code-review") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess().body()
    }

    // Stage 4B — Squads (ФТ-14.4)

    suspend fun fetchMySquad(): MySquadResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/squads/my").requireSuccess().body()
    }

    suspend fun createSquad(name: String, weeklyGoalXp: Int): SquadDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/squads") {
            contentType(ContentType.Application.Json)
            setBody(CreateSquadBodyDto(name = name, weeklyGoalXp = weeklyGoalXp))
        }.requireSuccess().body()
    }

    suspend fun joinSquad(inviteCode: String): SquadDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/squads/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinSquadBodyDto(inviteCode = inviteCode))
        }.requireSuccess().body()
    }

    suspend fun leaveSquad() {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/squads/leave").requireSuccess()
    }

    // Stage 4B — Dictionary Crowdsourcing PRs (ФТ-14.3)

    suspend fun fetchCrowdsourceEligibility(): CrowdsourceEligibilityResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/crowdsource/eligibility").requireSuccess().body()
    }

    suspend fun fetchDictionaryPrs(): DictionaryPrListResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/crowdsource/prs").requireSuccess().body()
    }

    suspend fun submitDictionaryPr(body: SubmitDictionaryPrBodyDto) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/crowdsource/prs") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess()
    }

    suspend fun voteDictionaryPr(prId: String, vote: Int) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/crowdsource/prs/$prId/vote") {
            contentType(ContentType.Application.Json)
            setBody(VoteCommentBodyDto(vote = vote))
        }.requireSuccess()
    }

    // Stage 4C — Thematic Rooms REST + Ktor WebSockets (ФТ-13.3)

    suspend fun fetchRoomMessages(roomSlug: String): RoomMessagesListResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/rooms/$roomSlug/messages").requireSuccess().body()
    }

    suspend fun postRoomMessage(roomSlug: String, bodyText: String): RoomMessageDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/rooms/$roomSlug/messages") {
            contentType(ContentType.Application.Json)
            setBody(PostRoomMessageBodyDto(body = bodyText))
        }.requireSuccess().body()
    }

    suspend fun requestWsTicket(scope: String = "ws"): WsTicketResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/ws/ticket?scope=$scope").requireSuccess().body()
    }

    internal fun buildRoomWsUrl(baseUrl: String, roomSlug: String, ticket: String): String {
        val wsBase = baseUrl.trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        return "$wsBase/ws/rooms/$roomSlug?ticket=$ticket"
    }

    suspend fun observeRoomWebSocket(
        roomSlug: String,
        onMessage: suspend (RoomMessageDto) -> Unit,
        onStatusChanged: (WsConnectionState) -> Unit = {}
    ) {
        val base = resolveBaseUrl()
        if (tokenStore.getAccessToken().isNullOrBlank()) {
            onStatusChanged(WsConnectionState.DISCONNECTED)
            return
        }
        onStatusChanged(WsConnectionState.CONNECTING)
        try {
            val ticketDto = requestWsTicket(scope = "rooms")
            val wsUrl = buildRoomWsUrl(base, roomSlug, ticketDto.ticket)
            httpClient.webSocket(wsUrl) {
                onStatusChanged(WsConnectionState.CONNECTED)
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val dto = runCatching {
                            jsonConfig.decodeFromString<RoomMessageDto>(frame.readText())
                        }.getOrNull()
                        if (dto != null && dto.id.isNotBlank()) onMessage(dto)
                    }
                }
            }
            onStatusChanged(WsConnectionState.DISCONNECTED)
        } catch (ce: CancellationException) {
            onStatusChanged(WsConnectionState.DISCONNECTED)
            throw ce
        } catch (exc: Exception) {
            onStatusChanged(WsConnectionState.ERROR)
            throw exc
        }
    }

    internal fun buildPvpWsUrl(baseUrl: String, matchId: String, ticket: String): String {
        val wsBase = baseUrl.trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        return "$wsBase/ws/pvp/$matchId?ticket=$ticket"
    }

    suspend fun connectPvpWebSocket(
        matchId: String,
        outgoingFrames: ReceiveChannel<String>,
        onEvent: suspend (PvpWsEventDto) -> Unit,
        onStatusChanged: (WsConnectionState) -> Unit = {}
    ) {
        val base = resolveBaseUrl()
        if (tokenStore.getAccessToken().isNullOrBlank()) {
            onStatusChanged(WsConnectionState.DISCONNECTED)
            return
        }
        onStatusChanged(WsConnectionState.CONNECTING)
        try {
            val ticketDto = requestWsTicket(scope = "pvp")
            val wsUrl = buildPvpWsUrl(base, matchId, ticketDto.ticket)
            httpClient.webSocket(wsUrl) {
                onStatusChanged(WsConnectionState.CONNECTED)
                coroutineScope {
                    val senderJob = launch {
                        for (payload in outgoingFrames) {
                            send(Frame.Text(payload))
                        }
                    }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val evt = runCatching {
                                    jsonConfig.decodeFromString<PvpWsEventDto>(frame.readText())
                                }.getOrNull()
                                if (evt != null && evt.type.isNotBlank()) onEvent(evt)
                            }
                        }
                    } finally {
                        senderJob.cancel()
                    }
                }
            }
            onStatusChanged(WsConnectionState.DISCONNECTED)
        } catch (ce: CancellationException) {
            onStatusChanged(WsConnectionState.DISCONNECTED)
            throw ce
        } catch (exc: Exception) {
            onStatusChanged(WsConnectionState.ERROR)
            throw exc
        }
    }

    // Stage 4C — PvP 1v1 Duels (ФТ-14.1)

    suspend fun matchmakePvp(forceTrainingFallback: Boolean = false): PvpMatchmakeResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/pvp/matchmake") {
            contentType(ContentType.Application.Json)
            setBody(PvpMatchmakeBodyDto(forceTrainingFallback = forceTrainingFallback))
        }.requireSuccess().body()
    }

    suspend fun submitPvpAnswers(matchId: String, answers: List<Int>): PvpSubmitResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.post("$base/v1/pvp/$matchId/submit") {
            contentType(ContentType.Application.Json)
            setBody(PvpSubmitBodyDto(answers = answers))
        }.requireSuccess().body()
    }

    suspend fun disconnectPvp(matchId: String, reason: String = "network_drop") {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: return
        try {
            httpClient.post("$base/v1/pvp/$matchId/disconnect") {
                contentType(ContentType.Application.Json)
                setBody(PvpDisconnectBodyDto(reason = reason))
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
        }
    }

    // Stage 4C — Peer Review Arena (ФТ-14.2 & Errata E5)

    suspend fun fetchDailyPeerCase(): PeerReviewCaseDto {
        val base = resolveBaseUrl()
        return httpClient.get("$base/v1/peer-review/daily-case").requireSuccess().body()
    }

    suspend fun fetchPeerSubmissions(): PeerReviewSubmissionsResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/peer-review/submissions").requireSuccess().body()
    }

    suspend fun submitPeerReview(body: PeerReviewSubmitBodyDto) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/peer-review/submissions") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.requireSuccess()
    }

    suspend fun postPeerFeedback(submissionId: String, ratingStars: Int, commentText: String) {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        httpClient.post("$base/v1/peer-review/submissions/$submissionId/feedback") {
            contentType(ContentType.Application.Json)
            setBody(PeerReviewFeedbackBodyDto(ratingStars = ratingStars, commentText = commentText))
        }.requireSuccess()
    }

    suspend fun fetchPeerReviewVoiceUrl(submissionId: String): PeerReviewVoiceUrlResponseDto {
        val base = resolveBaseUrl()
        tokenStore.getAccessToken() ?: error("Not authenticated")
        return httpClient.get("$base/v1/peer-review/submissions/$submissionId/voice-url").requireSuccess().body()
    }

    suspend fun fetchOtaManifest(): OtaManifestResponseDto {
        val base = resolveBaseUrl()
        return httpClient.get("$base/v1/ota/manifest").requireSuccess().body()
    }

    suspend fun fetchOtaPack(packId: String): OtaPackDownloadDto {
        val base = resolveBaseUrl()
        return httpClient.get("$base/v1/ota/packs/$packId").requireSuccess().body()
    }

    suspend fun fetchExternalOtaPayload(downloadUrl: String): String {
        return httpClient.get(downloadUrl).requireSuccess().body()
    }

    suspend fun fetchAppVersionFromBackend(): AppVersionMetadataDto {
        val base = resolveBaseUrl()
        val raw = httpClient.get("$base/v1/app/version") {
            timeout {
                connectTimeoutMillis = 6_000L
                requestTimeoutMillis = 8_000L
                socketTimeoutMillis = 8_000L
            }
        }.requireSuccess().bodyAsText()
        return jsonConfig.decodeFromString<AppVersionMetadataDto>(raw)
    }

    suspend fun fetchAppVersionFromFallback(
        fallbackUrl: String = BuildConfig.OTA_MANIFEST_FALLBACK_URL
    ): AppVersionMetadataDto {
        val raw = httpClient.get(fallbackUrl) {
            timeout {
                connectTimeoutMillis = 8_000L
                requestTimeoutMillis = 10_000L
                socketTimeoutMillis = 10_000L
            }
        }.requireSuccess().bodyAsText()
        return jsonConfig.decodeFromString<AppVersionMetadataDto>(raw)
    }

    suspend fun downloadApkToFile(
        apkUrl: String,
        targetFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ) {
        withContext(Dispatchers.IO) {
            targetFile.parentFile?.mkdirs()
            httpClient.prepareGet(apkUrl) {
                timeout {
                    connectTimeoutMillis = 20_000L
                    requestTimeoutMillis = 600_000L
                    socketTimeoutMillis = 120_000L
                }
            }.execute { response ->
                response.requireSuccess()
                val totalBytes = response.contentLength() ?: -1L
                val channel = response.bodyAsChannel()
                FileOutputStream(targetFile).use { fos ->
                    val buffer = ByteArray(32 * 1024)
                    var downloaded = 0L
                    onProgress(0L, totalBytes)
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read < 0) break
                        if (read > 0) {
                            fos.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, totalBytes)
                        }
                    }
                    fos.fd.sync()
                }
            }
        }
    }

    suspend fun warmupHealth(): Boolean = runCatching {
        val base = resolveBaseUrl()
        httpClient.get("$base/health").status.isSuccess()
    }.getOrDefault(false)

    companion object {
        val DEFAULT_BASE_URL: String = BuildConfig.API_BASE_URL
        const val CONNECT_TIMEOUT_MS = 30_000L
        const val READ_TIMEOUT_MS = 60_000L
        const val WRITE_TIMEOUT_MS = 60_000L
        const val CALL_TIMEOUT_MS = 60_000L
        const val REQUEST_TIMEOUT_MS = CALL_TIMEOUT_MS
        const val SOCKET_TIMEOUT_MS = READ_TIMEOUT_MS
    }
}
