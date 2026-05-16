package org.evoionosp.noveliq.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackConnection.playbackState

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            playbackConnection.pause()
        } else {
            playbackConnection.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playbackConnection.seekTo(positionMs)
    }
}
