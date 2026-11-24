package com.techenglish.app.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class RecordedVoiceClip(
    val base64Payload: String,
    val durationSeconds: Int,
    val byteSize: Int,
    val contentType: String = "audio/mp4"
)

const val MAX_PEER_VOICE_SECONDS = 20
const val MAX_PEER_VOICE_BYTES = 512 * 1024

fun validatePeerVoiceClipBounds(durationSeconds: Int, byteSize: Int): Result<Unit> {
    if (durationSeconds !in 1..MAX_PEER_VOICE_SECONDS) {
        return Result.failure(
            IllegalArgumentException("Длительность голосовой записи должна быть от 1 до $MAX_PEER_VOICE_SECONDS сек.")
        )
    }
    if (byteSize <= 0 || byteSize > MAX_PEER_VOICE_BYTES) {
        return Result.failure(
            IllegalArgumentException("Размер аудиофайла не должен превышать 512 КБ.")
        )
    }
    return Result.success(Unit)
}

/**
 * SP3-02 / BUG-06: Client voice clip recorder for Peer Review Arena (M4A/AAC, ≤ 20 s, ≤ 512 KB).
 */
interface PeerVoiceClipRecorder {
    val isRecording: Boolean
    fun startRecording(): Result<Unit>
    fun stopRecording(): Result<RecordedVoiceClip>
    fun cancelRecording()
}

@Singleton
class AndroidPeerVoiceClipRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) : PeerVoiceClipRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    @Volatile
    override var isRecording: Boolean = false
        private set

    override fun startRecording(): Result<Unit> = runCatching {
        cancelRecording()
        val file = File(context.cacheDir, "peer_review_voice_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioEncodingBitRate(32_000)
        mr.setAudioSamplingRate(16_000)
        mr.setMaxDuration(MAX_PEER_VOICE_SECONDS * 1_000)
        mr.setMaxFileSize(MAX_PEER_VOICE_BYTES.toLong())
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()

        recorder = mr
        outputFile = file
        startedAtMs = System.currentTimeMillis()
        isRecording = true
    }.onFailure {
        cancelRecording()
    }

    override fun stopRecording(): Result<RecordedVoiceClip> {
        val mr = recorder ?: return Result.failure(IllegalStateException("Запись не запущена"))
        val file = outputFile ?: return Result.failure(IllegalStateException("Файл записи не найден"))
        val elapsedSec = (((System.currentTimeMillis() - startedAtMs).coerceAtLeast(1_000L)) / 1_000.0)
            .roundToInt()
            .coerceIn(1, MAX_PEER_VOICE_SECONDS)

        return runCatching {
            runCatching { mr.stop() }
            mr.release()
            recorder = null
            isRecording = false

            val bytes = file.readBytes()
            validatePeerVoiceClipBounds(elapsedSec, bytes.size).getOrThrow()
            RecordedVoiceClip(
                base64Payload = Base64.encodeToString(bytes, Base64.NO_WRAP),
                durationSeconds = elapsedSec,
                byteSize = bytes.size,
                contentType = "audio/mp4"
            )
        }.also {
            file.delete()
            outputFile = null
            isRecording = false
            recorder = null
        }
    }

    override fun cancelRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile?.delete()
        outputFile = null
        isRecording = false
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceRecorderModule {
    @Binds
    @Singleton
    abstract fun bindPeerVoiceClipRecorder(impl: AndroidPeerVoiceClipRecorder): PeerVoiceClipRecorder
}
