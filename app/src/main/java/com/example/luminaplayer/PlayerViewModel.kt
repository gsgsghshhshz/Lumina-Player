package com.example.luminaplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val showControls: Boolean = true,
    val volume: Float = 0.5f,
    val brightness: Float = 0.5f,
    val seekPosition: Long? = null,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var player: ExoPlayer? = null
        private set

    private var updateJob: Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        player = LuminaPlayerFactory.create(getApplication()).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {}
                override fun onPlayerError(error: PlaybackException) {
                    _uiState.update { it.copy(errorMessage = error.errorCodeName) }
                }
            })
        }
        startProgressUpdates()
    }

    private fun startProgressUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                player?.let { p ->
                    _uiState.update {
                        it.copy(
                            isPlaying = p.isPlaying,
                            positionMs = p.currentPosition,
                            durationMs = p.duration.coerceAtLeast(0),
                            bufferedPercentage = p.bufferedPercentage
                        )
                    }
                }
                delay(100)
            }
        }
    }

    fun onSeekStart() = _uiState.update { it.copy(seekPosition = player?.currentPosition ?: 0) }
    
    fun onSeekDelta(deltaMs: Long) {
        val current = _uiState.value.seekPosition ?: return
val newPos = (current + deltaMs).coerceIn(0, _uiState.value.durationMs)
        _uiState.update { it.copy(seekPosition = newPos) }
    }
    
    fun onSeekEnd() {
        _uiState.value.seekPosition?.let { player?.seekTo(it) }
        _uiState.update { it.copy(seekPosition = null) }
    }

    fun togglePlayPause() {
        player?.playWhenReady = !(player?.playWhenReady ?: true)
    }

    fun resetErrorMessage() = _uiState.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        player?.release()
        player = null
    }
}
