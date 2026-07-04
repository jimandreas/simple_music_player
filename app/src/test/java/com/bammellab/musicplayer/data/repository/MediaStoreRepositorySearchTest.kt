/*
 *  Copyright 2025 Bammellab / James Andreas
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

package com.bammellab.musicplayer.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for [MediaStoreRepository.matchesSearchQuery], the pure string predicate backing the
 * song search feature. Kept Android-free (no [android.net.Uri]) since this repo has no
 * Robolectric/Mockito, so a real AudioFile can't be constructed in a plain JVM unit test.
 */
class MediaStoreRepositorySearchTest {

    @Test
    @DisplayName("Match is case-insensitive")
    fun matchIsCaseInsensitive() {
        assertThat(
            MediaStoreRepository.matchesSearchQuery("Bohemian Rhapsody.mp3", "Queen", "A Night at the Opera", "rhapsody")
        ).isTrue()
        assertThat(
            MediaStoreRepository.matchesSearchQuery("Bohemian Rhapsody.mp3", "Queen", "A Night at the Opera", "RHAPSODY")
        ).isTrue()
    }

    @Test
    @DisplayName("Match is substring, not just prefix")
    fun matchIsSubstringNotPrefix() {
        assertThat(
            MediaStoreRepository.matchesSearchQuery("Bohemian Rhapsody.mp3", "Queen", "A Night at the Opera", "hapsod")
        ).isTrue()
    }

    @Test
    @DisplayName("Matches when only the title contains the query")
    fun matchInTitleOnly() {
        assertThat(
            MediaStoreRepository.matchesSearchQuery("Yesterday.mp3", "The Beatles", "Help!", "yesterday")
        ).isTrue()
    }

    @Test
    @DisplayName("Matches when only the artist contains the query")
    fun matchInArtistOnly() {
        assertThat(
            MediaStoreRepository.matchesSearchQuery("track01.mp3", "The Beatles", "Help!", "beatles")
        ).isTrue()
    }

    @Test
    @DisplayName("Matches when only the album contains the query")
    fun matchInAlbumOnly() {
        assertThat(
            MediaStoreRepository.matchesSearchQuery("track01.mp3", "The Beatles", "Abbey Road", "abbey")
        ).isTrue()
    }

    @Test
    @DisplayName("No match when query is absent from title, artist, and album")
    fun noMatchWhenAbsentEverywhere() {
        assertThat(
            MediaStoreRepository.matchesSearchQuery("Yesterday.mp3", "The Beatles", "Help!", "queen")
        ).isFalse()
    }
}
