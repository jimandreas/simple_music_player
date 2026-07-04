package com.bammellab.musicplayer.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bammellab.musicplayer.data.model.FolderNode
import com.bammellab.musicplayer.data.model.SingleTrackItem
import com.bammellab.musicplayer.player.PlaybackState
import com.bammellab.musicplayer.ui.components.AboutDescriptionDialog
import com.bammellab.musicplayer.ui.components.AboutDialog
import com.bammellab.musicplayer.ui.components.CastButton
import com.bammellab.musicplayer.ui.components.FileListView
import com.bammellab.musicplayer.ui.components.FolderListView
import com.bammellab.musicplayer.ui.components.MusicLoadingIndicator
import com.bammellab.musicplayer.ui.components.NowPlayingView
import com.bammellab.musicplayer.ui.components.PlayerControls
import com.bammellab.musicplayer.ui.components.SinglesListView
import com.bammellab.musicplayer.viewmodel.MusicPlayerViewModel
import com.bammellab.musicplayer.viewmodel.MusicPlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val castState by viewModel.castState.collectAsStateWithLifecycle()

    val canGoBack = hasPermission && (
        (!uiState.showFolderBrowser && uiState.hasFiles) ||
        (uiState.showFolderBrowser && uiState.canNavigateUp)
    )
    BackHandler(enabled = canGoBack) {
        viewModel.handleBackNavigation()
    }
    // Registered after the handler above, so it takes priority while search is active.
    BackHandler(enabled = uiState.isSearchActive) {
        viewModel.deactivateSearch()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val fileListState = rememberLazyListState()
    val folderListState = rememberLazyListState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAboutDescriptionDialog by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val smallestWidth = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val isTablet = smallestWidth >= 600

    // Load media when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission && !uiState.hasPermission) {
            viewModel.onPermissionResult(true)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.currentTrackIndex) {
        val index = uiState.currentTrackIndex
        if (index < 0) return@LaunchedEffect

        val layoutInfo = fileListState.layoutInfo
        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val offset = if (viewportHeight > 0) -(viewportHeight / 2) else 0

        // Animate only for small sequential hops (prev/next); jump instantly for shuffle
        // or any other large distance to avoid the multi-pass layout jank.
        val firstVisible = fileListState.firstVisibleItemIndex
        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisible
        val isNearby = index in (firstVisible - 2)..(lastVisible + 2)

        if (isNearby) {
            fileListState.animateScrollToItem(index = index, scrollOffset = offset)
        } else {
            fileListState.scrollToItem(index = index, scrollOffset = offset)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            placeholder = { Text("Search in ${uiState.currentBrowseDisplayPath}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    } else {
                        Column {
                            Text("Music Player")
                            if (castState.isCasting) {
                                Text(
                                    text = "Casting to ${castState.castDeviceName ?: "device"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.isSearchActive) {
                        IconButton(onClick = { viewModel.deactivateSearch() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search"
                            )
                        }
                    } else {
                        // Show back button when:
                        // 1. Viewing tracks (not in folder browser) - go back to folder browser
                        // 2. In folder browser and can navigate up - go to parent folder
                        val showBackButton = hasPermission && (
                            (!uiState.showFolderBrowser && uiState.hasFiles) ||
                            (uiState.showFolderBrowser && uiState.canNavigateUp)
                        )
                        if (showBackButton) {
                            IconButton(onClick = { viewModel.handleBackNavigation() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.isSearchActive) {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { viewModel.activateSearch() }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "About"
                            )
                        }
                        // Cast button - shows when Cast devices are available
                        CastButton(modifier = Modifier.size(48.dp))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        when {
            // No permission - show permission request UI
            !hasPermission -> {
                PermissionRequestContent(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            // Loading
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    MusicLoadingIndicator(
                        message = "Loading music files..."
                    )
                }
            }

            // Search results take priority over both the folder browser and the track list,
            // since neither is reachable while search is active (see the BackHandler comment above).
            uiState.isSearchActive -> {
                SearchResultsContent(
                    query = uiState.searchQuery,
                    scopeName = uiState.currentBrowseDisplayPath,
                    results = uiState.searchResults,
                    currentPlayingUri = uiState.currentTrack?.uri,
                    onResultSelected = { viewModel.playSearchResult(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            // Show folder browser
            uiState.showFolderBrowser -> {
                FolderBrowserContent(
                    folders = uiState.currentFolderChildren,
                    displayPath = uiState.currentBrowseDisplayPath,
                    singlesCollection = uiState.singlesCollection,
                    currentPlayingUri = uiState.currentTrack?.uri,
                    folderListState = folderListState,
                    onFolderSelected = { viewModel.navigateToFolder(it.path) },
                    onPlayFolder = { viewModel.selectFolderForPlayback(it) },
                    onPlaySingles = { singles, idx -> viewModel.playAllSinglesFrom(singles, idx) },
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshCurrentView() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            // Show track list and player
            else -> {
                if (isLandscape) {
                    LandscapeLayout(
                        uiState = uiState,
                        playbackState = playbackState,
                        currentPosition = currentPosition,
                        duration = duration,
                        fileListState = fileListState,
                        isTablet = isTablet,
                        onTrackSelected = { viewModel.playTrack(it) },
                        onSeek = { viewModel.seekTo(it) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.playPrevious() },
                        onNext = { viewModel.playNext() },
                        onShuffleToggle = { viewModel.toggleShuffle() },
                        onVolumeUp = { viewModel.volumeUp() },
                        onVolumeDown = { viewModel.volumeDown() },
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCurrentView() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                } else {
                    PortraitLayout(
                        uiState = uiState,
                        playbackState = playbackState,
                        currentPosition = currentPosition,
                        duration = duration,
                        fileListState = fileListState,
                        onTrackSelected = { viewModel.playTrack(it) },
                        onSeek = { viewModel.seekTo(it) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.playPrevious() },
                        onNext = { viewModel.playNext() },
                        onShuffleToggle = { viewModel.toggleShuffle() },
                        onVolumeUp = { viewModel.volumeUp() },
                        onVolumeDown = { viewModel.volumeDown() },
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCurrentView() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                onShowDescription = {
                    showAboutDialog = false
                    showAboutDescriptionDialog = true
                }
            )
        }
        if (showAboutDescriptionDialog) {
            AboutDescriptionDialog(
                onDismiss = { showAboutDescriptionDialog = false },
                onBack = {
                    showAboutDescriptionDialog = false
                    showAboutDialog = true
                }
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant access to play your music files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderBrowserContent(
    modifier: Modifier = Modifier,
    folders: List<FolderNode>,
    displayPath: String,
    singlesCollection: List<SingleTrackItem> = emptyList(),
    currentPlayingUri: Uri? = null,
    folderListState: LazyListState,
    onFolderSelected: (FolderNode) -> Unit,
    onPlayFolder: (FolderNode) -> Unit,
    onPlaySingles: (List<SingleTrackItem>, Int) -> Unit = { _, _ -> },
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // Reset whenever the browse path changes; default to Folders view
    var showSingles by remember(displayPath) {
        mutableStateOf(false)
    }

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = displayPath,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // View-mode toggle: shown when both singles and folder children exist
        if (singlesCollection.isNotEmpty() && folders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showSingles,
                    onClick = { showSingles = true },
                    label = { Text("All Tracks (${singlesCollection.size})") }
                )
                FilterChip(
                    selected = !showSingles,
                    onClick = { showSingles = false },
                    label = { Text("Folders") }
                )
            }
        }

        if (showSingles && singlesCollection.isNotEmpty()) {
            SinglesListView(
                singles = singlesCollection,
                currentPlayingUri = currentPlayingUri,
                onSingleSelected = { idx -> onPlaySingles(singlesCollection, idx) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        } else if (folders.isEmpty()) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EmptyState(
                    icon = Icons.Filled.Folder,
                    message = "No music folders found",
                    hint = "Pull down to refresh",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            FolderListView(
                folders = folders,
                onFolderSelected = { node ->
                    // If folder has only direct music (no children), play it directly
                    // Otherwise, navigate into it
                    if (node.hasDirectMusic && !node.hasChildren) {
                        onPlayFolder(node)
                    } else {
                        onFolderSelected(node)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                listState = folderListState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    message: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultsContent(
    modifier: Modifier = Modifier,
    query: String,
    scopeName: String,
    results: List<SingleTrackItem>,
    currentPlayingUri: Uri?,
    onResultSelected: (Int) -> Unit
) {
    when {
        query.isBlank() -> {
            EmptyState(
                icon = Icons.Filled.Search,
                message = "Search in $scopeName",
                hint = "Type a song, artist, or album name",
                modifier = modifier
            )
        }
        results.isEmpty() -> {
            EmptyState(
                icon = Icons.Filled.Search,
                message = "No matches in $scopeName",
                hint = "Try a different search term, or go back and browse into another folder",
                modifier = modifier
            )
        }
        else -> {
            SinglesListView(
                singles = results,
                currentPlayingUri = currentPlayingUri,
                onSingleSelected = onResultSelected,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    modifier: Modifier = Modifier,
    uiState: MusicPlayerUiState,
    playbackState: PlaybackState,
    currentPosition: Int,
    duration: Int,
    fileListState: LazyListState,
    onTrackSelected: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = uiState.selectedFolderName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        FileListView(
            audioFiles = uiState.audioFiles,
            currentTrackIndex = uiState.currentTrackIndex,
            onTrackSelected = onTrackSelected,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            listState = fileListState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        )

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            NowPlayingView(
                currentTrack = uiState.currentTrack,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlayerControls(
                playbackState = playbackState,
                shuffleEnabled = uiState.shuffleEnabled,
                volume = uiState.volume,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onShuffleToggle = onShuffleToggle,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                isCurrentTrackPlayable = uiState.currentTrack?.isPlayable ?: true
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    modifier: Modifier = Modifier,
    uiState: MusicPlayerUiState,
    playbackState: PlaybackState,
    currentPosition: Int,
    duration: Int,
    fileListState: LazyListState,
    isTablet: Boolean,
    onTrackSelected: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    Row(modifier = modifier) {
        // Left side - File list
        Column(modifier = Modifier.weight(0.5f)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = uiState.selectedFolderName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            FileListView(
                audioFiles = uiState.audioFiles,
                currentTrackIndex = uiState.currentTrackIndex,
                onTrackSelected = onTrackSelected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                listState = fileListState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }

        // Vertical divider between sections
        VerticalDivider(modifier = Modifier.fillMaxHeight())

        // Right side - Player controls
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NowPlayingView(
                currentTrack = uiState.currentTrack,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = onSeek,
                isCompact = !isTablet
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlayerControls(
                playbackState = playbackState,
                shuffleEnabled = uiState.shuffleEnabled,
                volume = uiState.volume,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onShuffleToggle = onShuffleToggle,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                isCompact = !isTablet,
                isCurrentTrackPlayable = uiState.currentTrack?.isPlayable ?: true
            )
        }
    }
}
