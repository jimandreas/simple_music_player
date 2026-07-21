/*
 *  Copyright 2026 Bammellab / James Andreas
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package com.bammellab.musicplayer.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the pure "artist - album - song" filename parsing helpers on [AudioFile]'s
 * companion. Kept Android-free (no [android.net.Uri]) since this repo has no
 * Robolectric/Mockito, so a real AudioFile can't be constructed in a plain JVM unit test.
 */
class AudioFileTitleParsingTest {

    @Test
    @DisplayName("Three-part name parses into artist, album, and song")
    fun threePartNameParses() {
        val name = "Radiohead - OK Computer - Karma Police.mp3"
        assertThat(AudioFile.parseSongTitle(name)).isEqualTo("Karma Police")
        assertThat(AudioFile.parseArtist(name, "")).isEqualTo("Radiohead")
        assertThat(AudioFile.parseAlbum(name, "")).isEqualTo("OK Computer")
    }

    @Test
    @DisplayName("Two-part name parses into artist and song with no album")
    fun twoPartNameParses() {
        val name = "Radiohead - Karma Police.mp3"
        assertThat(AudioFile.parseSongTitle(name)).isEqualTo("Karma Police")
        assertThat(AudioFile.parseArtist(name, "")).isEqualTo("Radiohead")
        assertThat(AudioFile.parseAlbum(name, "")).isNull()
    }

    @Test
    @DisplayName("Name without separators is the whole song title")
    fun plainNameIsSongTitle() {
        assertThat(AudioFile.parseSongTitle("Karma Police.mp3")).isEqualTo("Karma Police")
        assertThat(AudioFile.parseArtist("Karma Police.mp3", "")).isNull()
        assertThat(AudioFile.parseAlbum("Karma Police.mp3", "")).isNull()
    }

    @Test
    @DisplayName("Extra separators stay inside the song title (limit 3)")
    fun extraSeparatorsStayInSongTitle() {
        val name = "Artist - Album - Some - Song.mp3"
        assertThat(AudioFile.parseSongTitle(name)).isEqualTo("Some - Song")
        assertThat(AudioFile.parseArtist(name, "")).isEqualTo("Artist")
        assertThat(AudioFile.parseAlbum(name, "")).isEqualTo("Album")
    }

    @Test
    @DisplayName("Hyphens without surrounding spaces are not separators")
    fun bareHyphensAreNotSeparators() {
        assertThat(AudioFile.parseSongTitle("Jay-Z.mp3")).isEqualTo("Jay-Z")
    }

    @Test
    @DisplayName("Extension stripping handles missing extensions and leading dots")
    fun extensionStripping() {
        assertThat(AudioFile.stripExtension("Karma Police.mp3")).isEqualTo("Karma Police")
        assertThat(AudioFile.stripExtension("Karma Police")).isEqualTo("Karma Police")
        assertThat(AudioFile.stripExtension(".hidden")).isEqualTo(".hidden")
        assertThat(AudioFile.stripExtension("Track.1.flac")).isEqualTo("Track.1")
    }

    @Test
    @DisplayName("Artist and album fall back to MediaStore metadata when name has no parts")
    fun fallsBackToMetadata() {
        assertThat(AudioFile.parseArtist("Karma Police.mp3", "Radiohead")).isEqualTo("Radiohead")
        assertThat(AudioFile.parseAlbum("Karma Police.mp3", "OK Computer")).isEqualTo("OK Computer")
    }

    @Test
    @DisplayName("MediaStore <unknown> and blank metadata are treated as absent")
    fun unknownMetadataIsAbsent() {
        assertThat(AudioFile.parseArtist("Karma Police.mp3", "<unknown>")).isNull()
        assertThat(AudioFile.parseArtist("Karma Police.mp3", "   ")).isNull()
        assertThat(AudioFile.parseAlbum("Karma Police.mp3", "<unknown>")).isNull()
    }

    @Test
    @DisplayName("Filename parse takes priority over MediaStore metadata")
    fun filenameParseWinsOverMetadata() {
        val name = "Radiohead - OK Computer - Karma Police.mp3"
        assertThat(AudioFile.parseArtist(name, "Tagged Artist")).isEqualTo("Radiohead")
        assertThat(AudioFile.parseAlbum(name, "Tagged Album")).isEqualTo("OK Computer")
    }

    @Test
    @DisplayName("Blank song part falls back to the full base name")
    fun blankSongPartFallsBack() {
        assertThat(AudioFile.parseSongTitle("Artist - Album - .mp3"))
            .isEqualTo("Artist - Album - ")
    }
}
