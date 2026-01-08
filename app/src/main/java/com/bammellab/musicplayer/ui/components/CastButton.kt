package com.bammellab.musicplayer.ui.components

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Compose wrapper for the Google Cast MediaRouteButton.
 * Shows the standard Cast icon and handles device discovery/connection.
 */
@Composable
fun CastButton(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // Wrap context with AppCompat theme to fix MediaRouteButton color issues
            val themedContext = ContextThemeWrapper(
                context,
                androidx.appcompat.R.style.Theme_AppCompat_DayNight
            )
            MediaRouteButton(themedContext).apply {
                try {
                    // Set up the Cast button with Cast SDK
                    CastButtonFactory.setUpMediaRouteButton(context, this)
                } catch (e: Exception) {
                    // Cast not available on this device
                }
            }
        }
    )
}
