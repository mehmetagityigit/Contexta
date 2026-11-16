package com.example.core.common

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.model.TranscriptLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class AudioPlaybackState(
    val isTtsMode: Boolean = true,
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeLineIndex: Int = 0,
    val totalLines: Int = 0,
    val errorMessage: String? = null
)

/**
 * Dual-mode audio controller:
 * 1) Real Media3 ExoPlayer for tracks with valid MEDIA3 audioUri (full seek, +/-10s, progress bar, speed).
 * 2) Honest Android TextToSpeech for TTS lessons (play/pause/stop, speed 0.75x-1.5x, line step;
 *    unsupported timeline seek is explicitly marked via isTtsMode=true and disabled in UI).
 */
class ListeningPlayerController(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext

    private var exoPlayer: ExoPlayer? = null
    private var positionTickerJob: Job? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsLines: List<TranscriptLine> = emptyList()
    private var currentTtsIndex = 0
    private var currentSpeed = 1.0f

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun prepare(
        audioMode: String,
        audioUri: String?,
        transcript: List<TranscriptLine>
    ) {
        release()
        ttsLines = transcript
        currentTtsIndex = 0
        currentSpeed = 1.0f

        if (audioMode == "MEDIA3" && !audioUri.isNullOrBlank()) {
            prepareMedia3(audioUri)
        } else {
            prepareTts(transcript.size)
        }
    }

    private fun prepareMedia3(uriString: String) {
        val player = ExoPlayer.Builder(appContext).build()
        exoPlayer = player
        _state.value = AudioPlaybackState(
            isTtsMode = false,
            isReady = false,
            totalLines = ttsLines.size
        )
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val ready = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
                _state.value = _state.value.copy(
                    isReady = ready,
                    durationMs = player.duration.coerceAtLeast(0L),
                    isPlaying = player.isPlaying
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorMessage = error.localizedMessage ?: "Audio playback error"
                )
            }
        })
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
        player.prepare()
    }

    private fun prepareTts(totalLines: Int) {
        _state.value = AudioPlaybackState(
            isTtsMode = true,
            isReady = false,
            totalLines = totalLines,
            activeLineIndex = 0
        )
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale.US)
                ttsReady = langResult != TextToSpeech.LANG_MISSING_DATA &&
                    langResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (ttsReady) {
                    tts?.setSpeechRate(currentSpeed)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            val idx = utteranceId?.removePrefix("line_")?.toIntOrNull() ?: currentTtsIndex
                            _state.value = _state.value.copy(
                                isPlaying = true,
                                activeLineIndex = idx
                            )
                        }

                        override fun onDone(utteranceId: String?) {
                            val finishedIdx = utteranceId?.removePrefix("line_")?.toIntOrNull() ?: currentTtsIndex
                            scope.launch(Dispatchers.Main) {
                                if (_state.value.isPlaying && finishedIdx + 1 < ttsLines.size) {
                                    currentTtsIndex = finishedIdx + 1
                                    speakLine(currentTtsIndex)
                                } else {
                                    _state.value = _state.value.copy(isPlaying = false)
                                }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _state.value = _state.value.copy(
                                isPlaying = false,
                                errorMessage = "TTS error"
                            )
                        }
                    })
                    _state.value = _state.value.copy(isReady = true, errorMessage = null)
                } else {
                    _state.value = _state.value.copy(
                        isReady = false,
                        errorMessage = "English TTS voice data is unavailable on this device."
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    isReady = false,
                    errorMessage = "Failed to initialize Android Text-to-Speech."
                )
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                }
                player.play()
            }
            return
        }

        if (!ttsReady || ttsLines.isEmpty()) return
        if (_state.value.isPlaying) {
            tts?.stop()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            if (currentTtsIndex >= ttsLines.size) currentTtsIndex = 0
            _state.value = _state.value.copy(isPlaying = true)
            speakLine(currentTtsIndex)
        }
    }

    fun stepTtsLine(delta: Int) {
        if (!_state.value.isTtsMode || ttsLines.isEmpty()) return
        val next = (currentTtsIndex + delta).coerceIn(0, ttsLines.lastIndex)
        currentTtsIndex = next
        _state.value = _state.value.copy(activeLineIndex = next)
        if (_state.value.isPlaying) {
            tts?.stop()
            speakLine(next)
        }
    }

    fun seekByMillis(deltaMs: Long) {
        val player = exoPlayer ?: return // Not supported in TTS mode
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _state.value = _state.value.copy(currentPositionMs = target)
    }

    fun seekToMillis(positionMs: Long) {
        val player = exoPlayer ?: return // Not supported in TTS mode
        val target = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _state.value = _state.value.copy(currentPositionMs = target)
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        _state.value = _state.value.copy(playbackSpeed = speed)
        val player = exoPlayer
        if (player != null) {
            player.setPlaybackSpeed(speed)
        } else if (ttsReady) {
            tts?.setSpeechRate(speed)
            if (_state.value.isPlaying) {
                tts?.stop()
                speakLine(currentTtsIndex)
            }
        }
    }

    private fun speakLine(index: Int) {
        val line = ttsLines.getOrNull(index) ?: return
        val params = Bundle()
        tts?.setSpeechRate(currentSpeed)
        tts?.speak(line.text, TextToSpeech.QUEUE_FLUSH, params, "line_$index")
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionTickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = exoPlayer ?: break
                val pos = player.currentPosition.coerceAtLeast(0L)
                val lineIdx = ttsLines.indexOfLast { pos >= it.startMs }.coerceAtLeast(0)
                _state.value = _state.value.copy(
                    currentPositionMs = pos,
                    durationMs = player.duration.coerceAtLeast(0L),
                    activeLineIndex = lineIdx
                )
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopPositionTicker()
        exoPlayer?.release()
        exoPlayer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        _state.value = AudioPlaybackState()
    }
}
