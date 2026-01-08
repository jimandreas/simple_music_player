package com.bammellab.musicplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.bammellab.musicplayer.data.AlbumArtFetcher
import com.google.android.gms.cast.framework.CastContext
import java.util.concurrent.Executors

class MusicPlayerApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Initialize Cast context in background to avoid blocking app startup
        Executors.newSingleThreadExecutor().execute {
            try {
                CastContext.getSharedInstance(this)
            } catch (e: Exception) {
                // Cast not available on this device (e.g., no Play Services)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AlbumArtFetcher.Factory(applicationContext))
            }
            .crossfade(true)
            .build()
    }
}
