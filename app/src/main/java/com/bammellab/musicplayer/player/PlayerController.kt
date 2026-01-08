package com.bammellab.musicplayer.player

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for audio playback controllers.
 * Implemented by both AudioPlayerManager (local) and CastRemotePlayer (cast).
 * Allows the ViewModel to switch between players seamlessly.
 */
interface PlayerController {

    /**
     * Current playback state.
     */
    val playbackState: StateFlow<PlaybackState>

    /**
     * Current playback position in milliseconds.
     */
    val currentPosition: StateFlow<Int>

    /**
     * Total duration of current track in milliseconds.
     */
    val duration: StateFlow<Int>

    /**
     * Start playing audio from the given URI.
     */
    fun play(uri: Uri)

    /**
     * Pause playback.
     */
    fun pause()

    /**
     * Resume paused playback.
     */
    fun resume()

    /**
     * Stop playback.
     */
    fun stop()

    /**
     * Seek to position in milliseconds.
     */
    fun seekTo(position: Int)

    /**
     * Set volume (0.0 to 1.0).
     */
    fun setVolume(volume: Float)

    /**
     * Update current position. Called periodically.
     */
    fun updateCurrentPosition()

    /**
     * Set callback for track completion.
     */
    fun setOnCompletionListener(listener: () -> Unit)

    /**
     * Release resources.
     */
    fun release()
}
