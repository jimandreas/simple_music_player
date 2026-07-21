package com.bammellab.musicplayer.data.model

import android.net.Uri

data class AudioFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val duration: Long = 0L,
    val albumId: Long = 0L,
    val albumArtUri: Uri? = null,
    val artist: String = "",
    val album: String = "",
    val folderPath: String = ""
) {
    /** Display name without its file extension. */
    val baseName: String
        get() = stripExtension(displayName)

    /** Song title parsed from an "artist - album - song" filename, or the whole base name. */
    val songTitle: String by lazy { parseSongTitle(displayName) }

    /** Artist from the filename convention, falling back to MediaStore metadata. */
    val displayArtist: String? by lazy { parseArtist(displayName, artist) }

    /** Album from the filename convention, falling back to MediaStore metadata. */
    val displayAlbum: String? by lazy { parseAlbum(displayName, album) }

    val formattedDuration: String
        get() {
            if (duration <= 0) return "--:--"
            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /**
     * Check if this audio file can be played by Android's MediaPlayer.
     * Some formats like WMA and DRM-protected M4P files are not supported.
     */
    val isPlayable: Boolean
        get() {
            val lowerName = displayName.lowercase()
            val lowerMime = mimeType.lowercase()

            // Check for unplayable extensions
            if (lowerName.endsWith(".wma") ||
                lowerName.endsWith(".m4p") ||
                lowerName.endsWith(".asf")) {
                return false
            }

            // Check for unplayable MIME types
            if (lowerMime.contains("x-ms-wma") ||
                lowerMime.contains("x-ms-asf")) {
                return false
            }

            return true
        }

    // Parsing lives in pure companion functions (no Android deps) so it can be covered by
    // plain JVM unit tests, which can't construct an AudioFile (android.net.Uri).
    companion object {
        fun stripExtension(name: String): String {
            val dot = name.lastIndexOf('.')
            return if (dot > 0) name.substring(0, dot) else name
        }

        // limit = 3 keeps any further " - " occurrences inside the song part
        private fun nameParts(displayName: String): List<String> =
            stripExtension(displayName).split(" - ", limit = 3)

        fun parseSongTitle(displayName: String): String {
            val parts = nameParts(displayName)
            val song = when (parts.size) {
                3 -> parts[2]
                2 -> parts[1]
                else -> ""
            }.trim()
            return song.ifEmpty { stripExtension(displayName) }
        }

        fun parseArtist(displayName: String, metadataArtist: String): String? {
            val parts = nameParts(displayName)
            val fromName = if (parts.size >= 2) parts[0].trim() else ""
            return fromName.ifEmpty { null } ?: metadataOrNull(metadataArtist)
        }

        fun parseAlbum(displayName: String, metadataAlbum: String): String? {
            val parts = nameParts(displayName)
            val fromName = if (parts.size == 3) parts[1].trim() else ""
            return fromName.ifEmpty { null } ?: metadataOrNull(metadataAlbum)
        }

        // MediaStore reports missing tags as "<unknown>"
        private fun metadataOrNull(value: String): String? =
            value.trim().takeIf { it.isNotBlank() && !it.startsWith("<") }
    }
}
