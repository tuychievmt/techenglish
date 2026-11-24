package com.techenglish.app.stage4c

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.audio.MAX_PEER_VOICE_BYTES
import com.techenglish.app.core.audio.MAX_PEER_VOICE_SECONDS
import com.techenglish.app.core.audio.PeerVoiceClipRecorder
import com.techenglish.app.core.audio.RecordedVoiceClip
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.validatePeerVoiceClipBounds
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.feature.stage4c.PeerReviewArenaViewModel
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.usecase.ValidatePeerReviewSubmissionUseCase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Sp302PeerReviewVoiceTest {

    private class FakeVoiceRecorder(
        private val clipToReturn: RecordedVoiceClip
    ) : PeerVoiceClipRecorder {
        override var isRecording: Boolean = false
        override fun startRecording(): Result<Unit> {
            isRecording = true
            return Result.success(Unit)
        }
        override fun stopRecording(): Result<RecordedVoiceClip> {
            isRecording = false
            return Result.success(clipToReturn)
        }
        override fun cancelRecording() {
            isRecording = false
        }
    }

    @Test
    fun `voice clip bounds enforce max 20s and 512KB, permission denial surfaces clear error, and presigned voice-url is fetched`() = runTest {
        // 1. Validate bounds
        assertTrue(validatePeerVoiceClipBounds(durationSeconds = 12, byteSize = 64 * 1024).isSuccess)
        assertTrue(validatePeerVoiceClipBounds(durationSeconds = MAX_PEER_VOICE_SECONDS, byteSize = MAX_PEER_VOICE_BYTES).isSuccess)
        assertTrue(validatePeerVoiceClipBounds(durationSeconds = 21, byteSize = 10_000).isFailure)
        assertTrue(validatePeerVoiceClipBounds(durationSeconds = 0, byteSize = 10_000).isFailure)
        assertTrue(validatePeerVoiceClipBounds(durationSeconds = 10, byteSize = MAX_PEER_VOICE_BYTES + 1).isFailure)

        // 2. ViewModel voice recording + permission denied + presigned URL fetch
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.saveTokensAndSession(
            accessToken = "valid_jwt",
            refreshToken = "valid_refresh",
            userId = "u1",
            username = "tester",
            email = "tester@example.com",
            provider = AuthProvider.EMAIL
        )

        var requestedVoiceUrlPath: String? = null
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/v1/peer-review/submissions/sub-77/voice-url" -> {
                    requestedVoiceUrlPath = path
                    respond(
                        content = """{"submission_id":"sub-77","download_url":"https://minio.local/presigned/sub-77.m4a?sig=ok","expires_in_seconds":300}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond(
                    content = """{"items":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val fakeClip = RecordedVoiceClip(
            base64Payload = "AAAAFGZ0eXBNNEFA",
            durationSeconds = 9,
            byteSize = 16_384,
            contentType = "audio/mp4"
        )
        val vm = PeerReviewArenaViewModel(
            apiClient = apiClient,
            tokenStore = tokenStore,
            validatePeer = ValidatePeerReviewSubmissionUseCase(),
            voiceClipRecorder = FakeVoiceRecorder(fakeClip),
            audioController = TechAudioController(context)
        )

        // Record & stop
        vm.startVoiceRecording()
        assertTrue(vm.uiState.value.isRecordingVoice)
        vm.stopVoiceRecording()
        assertFalse(vm.uiState.value.isRecordingVoice)
        assertEquals(9, vm.uiState.value.recordedVoiceClip?.durationSeconds)

        // Permission denied surfaces clear error message
        vm.onMicPermissionDenied()
        assertNotNull(vm.uiState.value.micPermissionError)
        assertTrue(vm.uiState.value.micPermissionError!!.contains("RECORD_AUDIO"))

        // Presigned voice URL endpoint works
        val voiceDto = apiClient.fetchPeerReviewVoiceUrl("sub-77")
        assertEquals("/v1/peer-review/submissions/sub-77/voice-url", requestedVoiceUrlPath)
        assertTrue(voiceDto.downloadUrl.startsWith("https://minio.local/presigned/"))
    }

    @Test
    fun `fetchPeerReviewVoiceUrl deserializes canonical backend voice_url field`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "valid_jwt",
            refreshToken = "valid_refresh",
            userId = "u-gap01",
            username = "gap01_dev",
            email = "gap01@example.com",
            provider = AuthProvider.EMAIL
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                      "submission_id": "sub-88",
                      "voice_url": "https://r2.example.com/presigned/sub-88.m4a?sig=ok",
                      "expires_in_seconds": 300,
                      "voice_expires_at_ms": 1900000000000
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val voiceDto = apiClient.fetchPeerReviewVoiceUrl("sub-88")
        assertEquals("sub-88", voiceDto.submissionId)
        assertEquals("https://r2.example.com/presigned/sub-88.m4a?sig=ok", voiceDto.downloadUrl)
    }
}
