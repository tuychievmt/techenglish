package com.techenglish.app.core.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.techenglish.domain.model.SpeechEngineAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ФТ-7.3 — On-device SpeechRecognizer wrapper for Android 13+ (API 33+).
 * Uses `SpeechRecognizer.createOnDeviceSpeechRecognizer` strictly when
 * `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` and
 * `SpeechRecognizer.isOnDeviceRecognitionAvailable(context)` is true.
 * Never crashes on API 26–32 or devices without an offline voice pack.
 */
@Singleton
class OnDeviceSpeechRecognizerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var activeRecognizer: SpeechRecognizer? = null

    fun checkAvailability(): SpeechEngineAvailability {
        val available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)
        return if (available) {
            SpeechEngineAvailability.AVAILABLE_ON_DEVICE
        } else {
            SpeechEngineAvailability.REQUIRES_VOICE_PACK_OR_NETWORK
        }
    }

    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (checkAvailability() != SpeechEngineAvailability.AVAILABLE_ON_DEVICE) {
            onError(
                SpeechEngineAvailability.REQUIRES_VOICE_PACK_OR_NETWORK.statusBannerRu
                    ?: "Нужен голосовой пакет или сеть"
            )
            return false
        }
        stopListening()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val recognizer = runCatching {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        }.getOrElse {
            onError("Не удалось инициализировать локальный SpeechRecognizer: ${it.localizedMessage}")
            return false
        }

        activeRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Речь не распознана — попробуйте произнести чётче."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения RECORD_AUDIO на доступ к микрофону."
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Нужен голосовой пакет английского языка (en-US) или сеть."
                    else -> "Ошибка распознавания (код $error). Проверьте голосовой пакет."
                }
                onError(msg)
            }

            override fun onResults(results: Bundle?) {
                val best = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                onFinalResult(best)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) onPartialResult(partial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        return runCatching {
            recognizer.startListening(intent)
            true
        }.getOrElse { exc ->
            runCatching { recognizer.destroy() }
            activeRecognizer = null
            onError(exc.localizedMessage ?: "Не удалось запустить распознавание речи")
            false
        }
    }

    fun stopListening() {
        val current = activeRecognizer
        activeRecognizer = null
        runCatching { current?.stopListening() }
        runCatching { current?.destroy() }
    }
}
