package com.example.musicplayer.player

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class ShuffleTracker(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private var playedIndices: MutableSet<Int> = mutableSetOf()
    private var totalTracks: Int = 0
    private var currentFolderUri: String? = null

    init {
        loadState()
    }

    fun initialize(folderUri: Uri?, trackCount: Int) {
        val folderString = folderUri?.toString()

        // Reset if folder changed
        if (folderString != currentFolderUri) {
            reset()
            currentFolderUri = folderString
            totalTracks = trackCount
            saveState()
        } else {
            totalTracks = trackCount
            // Remove any played indices that are now out of bounds
            playedIndices.removeAll { it >= trackCount }
            saveState()
        }
    }

    fun markPlayed(index: Int) {
        if (index in 0 until totalTracks) {
            playedIndices.add(index)
            saveState()
        }
    }

    fun getNextUnplayedIndex(): Int? {
        if (totalTracks == 0) return null

        val unplayed = (0 until totalTracks).filter { it !in playedIndices }

        return if (unplayed.isEmpty()) {
            null // All tracks played
        } else {
            unplayed.random()
        }
    }

    fun isAllPlayed(): Boolean {
        return totalTracks > 0 && playedIndices.size >= totalTracks
    }

    fun getUnplayedCount(): Int {
        return (totalTracks - playedIndices.size).coerceAtLeast(0)
    }

    fun reset() {
        playedIndices.clear()
        saveState()
    }

    fun resetAndClearFolder() {
        playedIndices.clear()
        currentFolderUri = null
        totalTracks = 0
        clearSavedState()
    }

    private fun loadState() {
        currentFolderUri = prefs.getString(KEY_FOLDER_URI, null)
        totalTracks = prefs.getInt(KEY_TOTAL_TRACKS, 0)

        val playedString = prefs.getString(KEY_PLAYED_INDICES, "") ?: ""
        playedIndices = if (playedString.isNotEmpty()) {
            playedString.split(",")
                .mapNotNull { it.toIntOrNull() }
                .toMutableSet()
        } else {
            mutableSetOf()
        }
    }

    private fun saveState() {
        prefs.edit()
            .putString(KEY_FOLDER_URI, currentFolderUri)
            .putInt(KEY_TOTAL_TRACKS, totalTracks)
            .putString(KEY_PLAYED_INDICES, playedIndices.joinToString(","))
            .apply()
    }

    private fun clearSavedState() {
        prefs.edit()
            .remove(KEY_FOLDER_URI)
            .remove(KEY_TOTAL_TRACKS)
            .remove(KEY_PLAYED_INDICES)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "shuffle_tracker"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_TOTAL_TRACKS = "total_tracks"
        private const val KEY_PLAYED_INDICES = "played_indices"
    }
}
