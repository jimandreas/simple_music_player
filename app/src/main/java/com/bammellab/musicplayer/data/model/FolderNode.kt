package com.bammellab.musicplayer.data.model

import android.net.Uri

data class FolderNode(
    val path: String,              // Full path: /storage/emulated/0/Music/dance
    val name: String,              // Display name: "dance"
    val directTrackCount: Int,     // Tracks directly in this folder
    val totalTrackCount: Int,      // Tracks in this + all descendants
    val albumArtUri: Uri?,         // Album art (from direct tracks, or inherited from child)
    val children: List<FolderNode> // Immediate subfolders with music
) {
    val hasDirectMusic: Boolean
        get() = directTrackCount > 0

    val hasChildren: Boolean
        get() = children.isNotEmpty()
}
