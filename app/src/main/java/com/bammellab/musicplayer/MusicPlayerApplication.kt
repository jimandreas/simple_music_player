package com.bammellab.musicplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.bammellab.musicplayer.data.AlbumArtFetcher

class MusicPlayerApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AlbumArtFetcher.Factory(applicationContext))
            }
            .crossfade(true)
            .build()
    }
}
