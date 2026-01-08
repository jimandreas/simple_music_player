# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run specific unit test class
./gradlew testDebugUnitTest --tests "com.example.musicplayer.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean and rebuild
./gradlew clean assembleDebug
```

## Architecture Overview

This is an Android music player app using Jetpack Compose and MVVM architecture.

### Key Layers

- **ViewModel** (`MusicPlayerViewModel`): Central business logic hub. Manages UI state via `MusicPlayerUiState` data class and `StateFlow`. Handles folder selection, track playback coordination, shuffle logic, and volume control.

- **Player** (`AudioPlayerManager`): MediaPlayer wrapper exposing reactive `StateFlow` for playback state, position, and duration. The ViewModel subscribes to these flows and drives the UI.

- **UI** (`ui/`): Jetpack Compose components that observe ViewModel state. `MusicPlayerScreen` is the main composable, composed of `FileListView`, `NowPlayingView`, and `PlayerControls`.

### Data Flow

1. User selects folder via SAF (Storage Access Framework) → `onFolderSelected()` parses audio files
2. User taps track → `playTrack()` → `AudioPlayerManager.play()` → MediaPlayer prepares async → state updates flow to UI
3. Track completion triggers `onCompletionCallback` → `playNext()` respects shuffle state

### State Management

- `MusicPlayerUiState` contains: audio files list, current track index, shuffle state, volume, folder info, loading/error states
- `PlaybackState` enum: IDLE, PLAYING, PAUSED, STOPPED, ERROR
- Position updates run on coroutine job every 500ms during playback

## Tech Stack

- Kotlin 2.0, Compose BOM 2024.09, Material 3
- Target SDK 36, Min SDK 24
- AndroidX DocumentFile for SAF file access
- Android MediaPlayer for audio playback
