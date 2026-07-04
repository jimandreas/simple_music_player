# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build release AAB (for Play Store)
./gradlew bundleRelease

# Run unit tests (JUnit 5 via Jupiter)
./gradlew test

# Run specific unit test class
./gradlew testDebugUnitTest --tests "com.bammellab.musicplayer.release.ReleaseBuildTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean and rebuild
./gradlew clean assembleDebug
```

## Release Signing

Release builds read signing config from two sources (in priority order):
1. Environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (used in CI)
2. `gradle/signing.properties` with keys `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (used locally)

## Architecture Overview

This is an Android music player app using Jetpack Compose and MVVM architecture with Chromecast support.

### Key Layers

- **ViewModel** (`MusicPlayerViewModel`): Central business logic hub. Manages UI state via `MusicPlayerUiState` data class and `StateFlow`. Handles hierarchical folder browsing, track playback, shuffle logic, volume control, and player switching between local and cast playback.

- **Data Layer** (`data/`):
  - `MediaStoreRepository`: Queries MediaStore for all audio files, groups by folder path, and builds a `FolderNode` tree. Also provides `findNodeAtPath`, `getChildrenAtPath`, `getParentPath`, `collectLeafTracksIfSinglesThresholdMet`, and `searchTracks` helpers.
  - `AudioFile`: Data class with uri, displayName, mimeType, duration, albumId, albumArtUri, artist, album, folderPath. Has computed `isPlayable` (returns false for WMA, ASF, M4P) and `formattedDuration`.
  - `MusicFolder`: Flat folder model used alongside the tree for quick lookup.
  - `FolderNode`: Tree node for hierarchical browsing — has `path`, `name` (human-friendly), `directTrackCount`, `totalTrackCount`, `albumArtUri`, `children`. `hasDirectMusic` and `hasChildren` drive navigation decisions.
  - `SingleTrackItem`: Pairs an `AudioFile` with its `FolderNode`; used for the All Tracks flat view.
  - `AlbumArtFetcher`: Custom Coil fetcher for album art (MediaStore URIs and embedded art).

- **Player Abstraction** (`PlayerController` interface): Defines common playback operations (play, pause, resume, stop, seek, setVolume, updateCurrentPosition). Two implementations:
  - `AudioPlayerManager`: Local MediaPlayer wrapper.
  - `CastRemotePlayer`: Chromecast playback via Google Cast SDK.

- **Shuffle** (`ShuffleTracker`): Persists shuffle state in SharedPreferences. Tracks which indices have been played (as a comma-separated set) so all tracks play once before repeating. `initialize()` resets if the folder changes.

- **Cast System** (`cast/`): Components for Chromecast support:
  - `CastSessionManager`: Manages Cast sessions and device discovery; exposes `castState: StateFlow<CastUiState>`.
  - `CastRemotePlayer`: Implements `PlayerController` for remote playback.
  - `AudioStreamServer`: NanoHTTPD server that bridges content:// URIs to HTTP endpoints for Cast devices. Uses a session token for basic access control. Port is auto-assigned (0).

- **UI** (`ui/`): Jetpack Compose components that observe ViewModel state. `MusicPlayerScreen` is the main composable with four modes:
  - Permission request UI (first launch)
  - Loading indicator
  - Folder browser (`FolderBrowserContent` / `FolderListView`) — tree-based navigation; shows "All Tracks" / "Folders" toggle chips when a singles-heavy subtree is detected
  - Track list with `NowPlayingView`, `PlayerControls`, and `CastButton`
  - Supports landscape layouts (`LandscapeLayout`) with side-by-side file list and player panel; tablet detection via smallest-width ≥ 600dp.
  - `SinglesListView`: flat list composable showing artist + track name + duration for the All Tracks view; also reused to render song search results.
  - System back button is intercepted via `BackHandler` — navigates up the folder tree or back to the folder browser; exits the app only when at the tree root. A second `BackHandler` closes song search first when active.

### Hierarchical Folder Navigation

The folder browser operates on a `FolderNode` tree rather than a flat list:
- `folderTree`: root `FolderNode` built from all audio file paths.
- `currentBrowsePath`: the path currently being browsed.
- `currentFolderChildren`: the children of the current node, shown in the list.
- `currentBrowseDisplayPath`: computed human-friendly path string (e.g., "Internal Storage > Jim > Music") using friendly names from the tree.
- Navigation: `navigateToFolder(path)` goes deeper; `navigateUp()` goes to parent; `handleBackNavigation()` either goes up in the tree or exits to folder browser from track list. The Android system back button is wired to the same logic via `BackHandler`.
- Folder names are humanized: `"emulated"` → `"Internal Storage"`, numeric user IDs → username via `UserManager` (API 31+) or `"Primary User"`, SD card volume IDs (hex-hex pattern) → `"SD Card"`.
- If a `FolderNode` has direct music but no children, tapping it immediately starts playback. If it has children, it navigates into it.

### All Tracks Flat View

For libraries with many single-track leaf directories (e.g. iTunes singles collections):
- On each navigation, `collectLeafTracksIfSinglesThresholdMet` scans the subtree. If ≥ `SINGLES_THRESHOLD` (5) leaf folders contain exactly one track, the method returns **all** tracks from **all** leaf folders (albums and singles alike), sorted by artist then title.
- This list is stored in `singlesCollection` on `MusicPlayerUiState`.
- When non-empty, `FolderBrowserContent` shows "All Tracks (N)" and "Folders" filter chips. Default is Folders; the user opts into All Tracks.
- Tapping a track in the All Tracks view calls `playAllSinglesFrom()`, which loads the entire flat list as the active playlist and starts playback at the selected index. Normal shuffle/prev/next applies across the full list.

### Song Search

Search is scoped to the folder subtree currently being browsed (`currentBrowsePath`), not the whole library:
- A search icon in the `TopAppBar` (`MusicPlayerScreen`) swaps the title into a text field and opens the keyboard. A second `BackHandler`, registered after the folder-navigation one, closes search first on system back while it's active.
- `MediaStoreRepository.searchTracks(node, allFiles, query)` walks every node in the given subtree (not just leaves, unlike `collectLeafTracksIfSinglesThresholdMet`) and matches each track's `displayName`, `artist`, and `album` case-insensitively via the pure `matchesSearchQuery` helper. Results are returned as `List<SingleTrackItem>` so they reuse `SinglesListView` for rendering and `playAllSinglesFrom` for playback.
- `MusicPlayerViewModel.updateSearchQuery()` debounces input (~300ms) and resolves the scope node from `currentBrowsePath` — not `selectedFolderPath`. Because tapping into a leaf folder with direct music jumps straight to playback without updating `currentBrowsePath`, search from a leaf's track list still covers the parent subtree the user was browsing, not just that one leaf.
- Search state (`isSearchActive`, `searchQuery`, `searchResults`) is ephemeral — not persisted to SharedPreferences — and playback already in progress continues uninterrupted while searching.

### Data Flow

1. App requests `READ_MEDIA_AUDIO` (API 33+) or `READ_EXTERNAL_STORAGE` (older).
2. On permission grant → `loadMediaStoreAudio()` queries MediaStore, builds flat folder list + `FolderNode` tree.
3. User browses tree → taps leaf folder → `selectFolderForPlayback()` loads tracks.
4. User taps track → `playTrack()` → routes to `activePlayer` (local or cast) → state updates flow to UI.
5. Track completion triggers `onCompletionCallback` → `playNext()` respects shuffle state via `ShuffleTracker`.
6. For Chromecast: `AudioStreamServer` registers the file and returns an HTTP URL; Cast device fetches audio over the local network.

### State Management

- `MusicPlayerUiState` contains:
  - `allFolders`, `folderTree`, `currentBrowsePath`, `currentFolderChildren`, `currentBrowseDisplayPath`
  - `audioFiles`: tracks in selected folder; `currentTrackIndex`, `currentTrack`
  - `shuffleEnabled`, `volume`, `selectedFolderPath`, `selectedFolderName`
  - `showFolderBrowser`: folder browser vs track list
  - `hasPermission`, `isLoading`, `isRefreshing` (pull-to-refresh), `errorMessage`
  - `canNavigateUp`: true when not at tree root
  - `singlesCollection`: non-empty `List<SingleTrackItem>` when the current subtree has ≥ 5 single-track leaf folders; populated on every navigation
  - `isSearchActive`, `searchQuery`, `searchResults`: ephemeral song-search state, scoped to the current browse subtree (see "Song Search" above); not persisted
- `CastUiState`: casting status, device name, connection state.
- `PlaybackState` enum: IDLE, PLAYING, PAUSED, STOPPED, ERROR.
- `activePlayer` property dynamically returns local or cast player based on `castState.isCasting`.
- Position updates run on a coroutine job every 500ms during playback.
- Persistence via two SharedPreferences files: `music_player_prefs` (folder path, browse path, track index, shuffle state) and `shuffle_tracker` (played indices set).
- Shuffle icon tint: `Color(0xFFFFD600)` (bright yellow) when enabled, `Color(0xFF616161)` (dark grey) when disabled.
- Track list auto-scroll: uses `animateScrollToItem` only when the target is within 2 positions of the current viewport; uses instant `scrollToItem` for large jumps (shuffle) to avoid multi-pass layout jank.

### Album Art

Album art loading uses a priority chain:
1. MediaStore album art URI (`content://media/external/audio/albumart/<albumId>`)
2. Embedded art extracted via MediaMetadataRetriever
3. Fallback icon

`AlbumArtImage` composable handles this with `albumArtUri` and `uri` parameters.

## Tech Stack

- Kotlin 2.3, Compose BOM 2026.01, Material 3
- Target SDK 36, Min SDK 24
- Unit tests use **JUnit 5 (Jupiter)** via `useJUnitPlatform()` — not JUnit 4
- MediaStore API for audio file discovery and album art
- Android MediaPlayer for local audio playback
- Google Cast SDK + NanoHTTPD for Chromecast streaming
- Coil for album art loading with custom `AlbumArtFetcher`
