package com.bammellab.musicplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bammellab.musicplayer.data.model.AudioFile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingView(
    currentTrack: AudioFile?,
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val albumArtSize: Dp = if (isCompact) 56.dp else 120.dp
    var showDetails by remember(currentTrack?.uri) { mutableStateOf(false) }

    if (showDetails && currentTrack != null) {
        TrackDetailsDialog(track = currentTrack, onDismiss = { showDetails = false })
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        if (isCompact) {
            // Horizontal layout for compact mode (landscape phone)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    uri = currentTrack?.uri,
                    albumArtUri = currentTrack?.albumArtUri,
                    size = albumArtSize,
                    fallbackIcon = Icons.Filled.Album,
                    showBackground = true,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack?.songTitle ?: "No track selected",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = currentTrack != null) { showDetails = true }
                            .basicMarquee()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val hasValidDuration = duration > 0
                    Slider(
                        value = if (hasValidDuration) currentPosition.toFloat() else 0f,
                        onValueChange = { if (hasValidDuration) onSeek(it.toInt()) },
                        valueRange = 0f..(if (hasValidDuration) duration.toFloat() else 1f),
                        enabled = hasValidDuration,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Vertical layout for normal mode (portrait)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlbumArtImage(
                    uri = currentTrack?.uri,
                    albumArtUri = currentTrack?.albumArtUri,
                    size = albumArtSize,
                    fallbackIcon = Icons.Filled.Album,
                    showBackground = true,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTrack?.songTitle ?: "No track selected",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = currentTrack != null) { showDetails = true }
                        .basicMarquee()
                )

                Spacer(modifier = Modifier.height(16.dp))

                val hasValidDuration = duration > 0
                Slider(
                    value = if (hasValidDuration) currentPosition.toFloat() else 0f,
                    onValueChange = { if (hasValidDuration) onSeek(it.toInt()) },
                    valueRange = 0f..(if (hasValidDuration) duration.toFloat() else 1f),
                    enabled = hasValidDuration,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackDetailsDialog(
    track: AudioFile,
    onDismiss: () -> Unit
) {
    // Toast-like behavior: linger long enough to read, then dismiss on its own
    LaunchedEffect(track.uri) {
        delay(10_000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlbumArtImage(
                    uri = track.uri,
                    albumArtUri = track.albumArtUri,
                    size = 120.dp,
                    fallbackIcon = Icons.Filled.Album,
                    showBackground = true,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = track.songTitle,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                track.displayArtist?.let { artist ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                track.displayAlbum?.let { album ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
