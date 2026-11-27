package com.techenglish.app.core.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TtsVoiceStatus {
    data object Initializing : TtsVoiceStatus
    data class ReadyOffline(val voiceName: String) : TtsVoiceStatus
    data object ReadyOnlineOnly : TtsVoiceStatus
    data class MissingVoiceData(val message: String) : TtsVoiceStatus
    data class Error(val message: String) : TtsVoiceStatus
}

data class AudioPlaybackState(
    val currentText: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val speechLocaleTag: String = "en-US",
    val progress: Float = 0f,
    val usingMedia3: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Offline-first Audio & Pronunciation controller combining Android TextToSpeech
 * with AndroidX Media3 ExoPlayer.
 *
 * - Detects installed offline English TTS voices without crashing when absent.
 * - Synthesizes speech to local cache WAV files and plays via Media3 ExoPlayer for full
 *   Play / Pause / Replay / Stop / Speed controls.
 * - Gracefully falls back to direct TextToSpeech playback when synthesis is unsupported.
 */
@Singleton
class TechAudioController @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _voiceStatus = MutableStateFlow<TtsVoiceStatus>(TtsVoiceStatus.Initializing)
    val voiceStatus: StateFlow<TtsVoiceStatus> = _voiceStatus.asStateFlow()

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsReady: Boolean = false
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private var synthesisJob: Job? = null
    @Volatile
    private var currentPlaybackIsRemote: Boolean = false
    private var lastSynthesizedFile: File? = null
    private val synthesisWaiters = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    @Synchronized
    fun ensureInitialized() {
        if (tts != null) return
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    configureEnglishVoice()
                } else {
                    ttsReady = false
                    _voiceStatus.value = TtsVoiceStatus.MissingVoiceData(
                        "Синтезатор речи Android не настроен. Установите голосовой пакет английского языка."
                    )
                }
            }.apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId?.startsWith("direct_") == true) {
                            _playbackState.update {
                                it.copy(
                                    isPlaying = true,
                                    isPaused = false,
                                    usingMedia3 = false,
                                    errorMessage = null
                                )
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == null) return
                        synthesisWaiters.remove(utteranceId)?.complete(true)
                        if (utteranceId.startsWith("direct_")) {
                            _playbackState.update {
                                it.copy(
                                    isPlaying = false,
                                    isPaused = false,
                                    progress = 1f
                                )
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        handleUtteranceError(utteranceId)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        handleUtteranceError(utteranceId)
                    }
                })
            }
        } catch (t: Throwable) {
            ttsReady = false
            _voiceStatus.value = TtsVoiceStatus.Error(
                "Озвучивание недоступно на этом устройстве (${t.localizedMessage ?: "TTS error"})."
            )
        }
    }

    private fun handleUtteranceError(utteranceId: String?) {
        if (utteranceId != null) {
            synthesisWaiters.remove(utteranceId)?.complete(false)
            if (utteranceId.startsWith("synth_")) {
                return
            }
        }
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                errorMessage = "Не удалось воспроизвести аудио. Проверьте установленный английский голосовой пакет."
            )
        }
    }

    private fun configureEnglishVoice(targetLocaleTag: String = _playbackState.value.speechLocaleTag) {
        val engine = tts ?: return
        try {
            val targetLocale = Locale.forLanguageTag(targetLocaleTag).takeIf { it.language.isNotEmpty() } ?: Locale.US
            val langResult = engine.setLanguage(targetLocale)
            when (langResult) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                    ttsReady = false
                    _voiceStatus.value = TtsVoiceStatus.MissingVoiceData(
                        "Офлайн-голос английского языка ($targetLocaleTag) не установлен. Нажмите «Установить голос» для работы без интернета."
                    )
                    return
                }
            }

            val voices = runCatching { engine.voices }.getOrNull().orEmpty()
            val offlineVoice = voices
                .filter { it.locale.language.equals("en", ignoreCase = true) }
                .sortedByDescending { it.locale.country.equals(targetLocale.country, ignoreCase = true) }
                .firstOrNull { !it.isNetworkConnectionRequired }

            if (offlineVoice != null) {
                runCatching { engine.voice = offlineVoice }
                ttsReady = true
                _voiceStatus.value = TtsVoiceStatus.ReadyOffline(offlineVoice.name)
            } else {
                // Still usable when engine provides default local voice or online fallback
                ttsReady = true
                _voiceStatus.value = TtsVoiceStatus.ReadyOnlineOnly
            }
        } catch (t: Throwable) {
            ttsReady = false
            _voiceStatus.value = TtsVoiceStatus.Error("Ошибка инициализации голоса TTS.")
        }
    }

    fun setSpeechLocale(localeTag: String) {
        val sanitized = if (localeTag.equals("en-GB", ignoreCase = true)) "en-GB" else "en-US"
        _playbackState.value = _playbackState.value.copy(speechLocaleTag = sanitized)
        if (tts != null) {
            configureEnglishVoice(sanitized)
        }
    }

    fun recheckVoiceStatus() {
        if (tts == null) {
            ensureInitialized()
        } else {
            configureEnglishVoice(_playbackState.value.speechLocaleTag)
        }
    }

    /**
     * Quick single-tap speech for Flashcards, Vocabulary items, and Doc popups.
     */
    fun speakQuick(text: String, speed: Float = _playbackState.value.playbackSpeed) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        ensureInitialized()
        stopInternal()

        val engine = tts
        if (engine == null || !ttsReady) {
            _playbackState.value = _playbackState.value.copy(
                currentText = clean,
                isPlaying = false,
                isPaused = false,
                errorMessage = "Установите офлайн-голос английского языка в настройках TTS."
            )
            return
        }

        _playbackState.value = _playbackState.value.copy(
            currentText = clean,
            isPlaying = true,
            isPaused = false,
            playbackSpeed = speed,
            progress = 0f,
            usingMedia3 = false,
            errorMessage = null
        )

        runCatching {
            engine.setSpeechRate(speed.coerceIn(0.5f, 1.5f))
            val utteranceId = "direct_${UUID.randomUUID()}"
            engine.speak(clean, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
        }.onFailure {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                errorMessage = "Не удалось запустить TTS."
            )
        }
    }

    /**
     * Full controlled playback (Play / Pause / Replay / Stop) using TTS synthesis -> Media3 ExoPlayer,
     * falling back cleanly to direct TTS if file synthesis is unavailable.
     */
    fun playWithMedia3(text: String, speed: Float = _playbackState.value.playbackSpeed) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        ensureInitialized()

        // Resume if currently paused on the exact same text with ExoPlayer
        val player = exoPlayer
        if (_playbackState.value.isPaused &&
            _playbackState.value.currentText == clean &&
            player != null &&
            _playbackState.value.usingMedia3
        ) {
            player.playbackParameters = PlaybackParameters(speed)
            player.play()
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                isPaused = false,
                playbackSpeed = speed,
                errorMessage = null
            )
            startProgressTicker()
            return
        }

        stopInternal()

        val engine = tts
        if (engine == null || !ttsReady) {
            _playbackState.value = _playbackState.value.copy(
                currentText = clean,
                isPlaying = false,
                isPaused = false,
                errorMessage = "Офлайн-голос английского языка не найден. Установите голосовой пакет TTS."
            )
            return
        }

        _playbackState.update {
            it.copy(
                currentText = clean,
                isPlaying = true,
                isPaused = false,
                playbackSpeed = speed,
                progress = 0f,
                errorMessage = null
            )
        }

        synthesisJob?.cancel()
        synthesisJob = scope.launch {
            val wavFile = withContext(Dispatchers.IO) {
                synthesizeToCacheWav(engine, clean, speed)
            }
            if (wavFile != null && wavFile.exists() && wavFile.length() > 44L) {
                lastSynthesizedFile = wavFile
                playFileViaExoPlayer(wavFile, speed)
            } else {
                // Graceful fallback to direct TTS utterance
                speakQuick(clean, speed)
            }
        }
    }

    fun pause() {
        val player = exoPlayer
        if (_playbackState.value.usingMedia3 && player != null && player.isPlaying) {
            player.pause()
            progressJob?.cancel()
            _playbackState.value = _playbackState.value.copy(isPlaying = false, isPaused = true)
        } else {
            // Direct TTS cannot mid-sentence pause; stop cleanly and mark paused for replay
            runCatching { tts?.stop() }
            _playbackState.value = _playbackState.value.copy(isPlaying = false, isPaused = true)
        }
    }

    fun replay() {
        val text = _playbackState.value.currentText
        if (text.isEmpty()) return
        val player = exoPlayer
        val cached = lastSynthesizedFile
        if (_playbackState.value.usingMedia3 && player != null && cached != null && cached.exists()) {
            player.seekTo(0L)
            player.playbackParameters = PlaybackParameters(_playbackState.value.playbackSpeed)
            player.play()
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                isPaused = false,
                progress = 0f,
                errorMessage = null
            )
            startProgressTicker()
        } else {
            playWithMedia3(text, _playbackState.value.playbackSpeed)
        }
    }

    fun stop() {
        stopInternal()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            progress = 0f
        )
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.6f, 1.5f)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
        exoPlayer?.playbackParameters = PlaybackParameters(clamped)
        runCatching { tts?.setSpeechRate(clamped) }
    }

    fun buildInstallTtsVoiceIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private suspend fun synthesizeToCacheWav(engine: TextToSpeech, text: String, speed: Float): File? {
        var utteranceId: String? = null
        return try {
            val dir = File(context.cacheDir, "tts_audio").apply { mkdirs() }
            val localeTag = _playbackState.value.speechLocaleTag
            val cacheKey = buildCacheKey(localeTag = localeTag, speed = speed, text = text)
            val outFile = File(dir, "clip_$cacheKey.wav")
            if (outFile.exists() && outFile.length() > 44L) {
                outFile.setLastModified(System.currentTimeMillis())
                return outFile
            }
            val id = "synth_${UUID.randomUUID()}"
            utteranceId = id
            val deferred = CompletableDeferred<Boolean>()
            synthesisWaiters[id] = deferred
            engine.setSpeechRate(1.0f)
            val result = engine.synthesizeToFile(text, Bundle(), outFile, id)
            if (result != TextToSpeech.SUCCESS) {
                return null
            }
            val done = withTimeoutOrNull(4_000L) { deferred.await() } ?: false
            if (done && outFile.exists() && outFile.length() > 44L) {
                outFile.setLastModified(System.currentTimeMillis())
                evictCacheLru(dir)
                outFile
            } else {
                null
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            null
        } finally {
            utteranceId?.let { synthesisWaiters.remove(it) }
        }
    }

    private fun getOrCreateExoPlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        return ExoPlayer.Builder(context.applicationContext).build().also { created ->
            created.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        progressJob?.cancel()
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                isPaused = false,
                                progress = 1f
                            )
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    progressJob?.cancel()
                    if (currentPlaybackIsRemote) {
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                isPaused = false,
                                errorMessage = error.localizedMessage ?: "Не удалось воспроизвести удалённую запись"
                            )
                        }
                    } else {
                        // Fall back transparently to direct TTS for local term/article audio
                        speakQuick(_playbackState.value.currentText, _playbackState.value.playbackSpeed)
                    }
                }
            })
            exoPlayer = created
        }
    }

    fun playRemoteAudioUrl(url: String, label: String = "Peer Review Voice") {
        if (url.isBlank()) return
        stop()
        currentPlaybackIsRemote = true
        try {
            val player = getOrCreateExoPlayer()
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            player.playbackParameters = PlaybackParameters(1.0f)
            player.prepare()
            player.play()
            _playbackState.update {
                it.copy(
                    currentText = label,
                    isPlaying = true,
                    isPaused = false,
                    usingMedia3 = true,
                    errorMessage = null
                )
            }
            startProgressTicker()
        } catch (exc: Throwable) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = exc.localizedMessage ?: "Не удалось воспроизвести аудио"
                )
            }
        }
    }

    private fun playFileViaExoPlayer(file: File, speed: Float) {
        currentPlaybackIsRemote = false
        try {
            val player = getOrCreateExoPlayer()
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.playbackParameters = PlaybackParameters(speed)
            player.prepare()
            player.play()
            _playbackState.update {
                it.copy(
                    isPlaying = true,
                    isPaused = false,
                    usingMedia3 = true,
                    errorMessage = null
                )
            }
            startProgressTicker()
        } catch (_: Throwable) {
            speakQuick(_playbackState.value.currentText, speed)
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_playbackState.value.isPlaying && _playbackState.value.usingMedia3) {
                val player = exoPlayer ?: break
                val duration = player.duration.takeIf { it > 0 } ?: 1L
                val position = player.currentPosition.coerceAtLeast(0L)
                val fraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                _playbackState.update { it.copy(progress = fraction) }
                delay(100L)
            }
        }
    }

    private fun stopInternal() {
        synthesisJob?.cancel()
        synthesisJob = null
        progressJob?.cancel()
        runCatching { exoPlayer?.stop() }
        runCatching { tts?.stop() }
    }

    override fun onStop(owner: LifecycleOwner) {
        release()
    }

    fun release() {
        stopInternal()
        synthesisWaiters.values.forEach { it.complete(false) }
        synthesisWaiters.clear()
        runCatching { exoPlayer?.release() }
        exoPlayer = null
        runCatching { tts?.shutdown() }
        tts = null
        ttsReady = false
        _playbackState.value = AudioPlaybackState(
            playbackSpeed = _playbackState.value.playbackSpeed,
            speechLocaleTag = _playbackState.value.speechLocaleTag
        )
    }

    companion object {
        const val MAX_TTS_CACHE_BYTES: Long = 10L * 1024L * 1024L
        const val MAX_TTS_CACHE_FILES: Int = 40

        internal fun buildCacheKey(localeTag: String, speed: Float, text: String): String {
            val normLocale = localeTag.trim().ifEmpty { "en-US" }
            val normSpeed = String.format(Locale.US, "%.2f", speed.coerceIn(0.5f, 2.0f))
            val normText = text.trim()
            val payload = "$normLocale|$normSpeed|$normText"
            val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            return digest.joinToString(separator = "") { b -> "%02x".format(b) }
        }

        internal fun evictCacheLru(
            cacheDir: File,
            maxBytes: Long = MAX_TTS_CACHE_BYTES,
            maxFiles: Int = MAX_TTS_CACHE_FILES
        ) {
            val files = cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".wav") }
                ?.sortedBy { it.lastModified() }
                ?.toMutableList()
                ?: return
            var totalBytes = files.sumOf { it.length() }
            while (files.isNotEmpty() && (totalBytes > maxBytes || files.size > maxFiles)) {
                val oldest = files.removeAt(0)
                val size = oldest.length()
                if (oldest.delete()) {
                    totalBytes -= size
                }
            }
        }
    }
}
