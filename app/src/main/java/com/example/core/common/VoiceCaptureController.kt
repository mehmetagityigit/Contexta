package com.example.core.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceCaptureState {
    data object Idle : VoiceCaptureState
    data object Listening : VoiceCaptureState
    data class Partial(val text: String) : VoiceCaptureState
    data class Recognized(val text: String) : VoiceCaptureState
    data class Error(val errorType: VoiceErrorType) : VoiceCaptureState
}

enum class VoiceErrorType {
    PERMISSION_DENIED,
    SERVICE_UNAVAILABLE,
    NETWORK_ERROR,
    NO_MATCH,
    AUDIO_ERROR,
    BUSY_OR_CANCELLED
}

/**
 * Push-to-talk English dictation helper backed by Android SpeechRecognizer.
 * Never listens continuously; releases recognizer resources immediately when finished or stopped.
 */
class VoiceCaptureController(context: Context) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceCaptureState>(VoiceCaptureState.Idle)
    val state: StateFlow<VoiceCaptureState> = _state.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening() {
        if (!isAvailable()) {
            _state.value = VoiceCaptureState.Error(VoiceErrorType.SERVICE_UNAVAILABLE)
            return
        }
        release()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceCaptureState.Listening
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val mapped = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceErrorType.PERMISSION_DENIED
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceErrorType.NETWORK_ERROR
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceErrorType.NO_MATCH
                    SpeechRecognizer.ERROR_AUDIO -> VoiceErrorType.AUDIO_ERROR
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceErrorType.BUSY_OR_CANCELLED
                    else -> VoiceErrorType.SERVICE_UNAVAILABLE
                }
                _state.value = VoiceCaptureState.Error(mapped)
                releaseInternal()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull()?.trim().orEmpty()
                if (best.isNotEmpty()) {
                    _state.value = VoiceCaptureState.Recognized(best)
                } else {
                    _state.value = VoiceCaptureState.Error(VoiceErrorType.NO_MATCH)
                }
                releaseInternal()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (partial.isNotEmpty()) {
                    _state.value = VoiceCaptureState.Partial(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        _state.value = VoiceCaptureState.Listening
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        releaseInternal()
        if (_state.value is VoiceCaptureState.Listening) {
            _state.value = VoiceCaptureState.Idle
        }
    }

    fun resetState() {
        _state.value = VoiceCaptureState.Idle
    }

    fun release() {
        releaseInternal()
        _state.value = VoiceCaptureState.Idle
    }

    private fun releaseInternal() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }
}
