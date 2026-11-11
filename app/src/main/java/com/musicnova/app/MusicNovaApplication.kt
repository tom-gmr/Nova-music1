
package com.musicnova.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicNovaApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(filesDir.resolve("nova_artwork_images"))
                    .maxSizeBytes(300L * 1024L * 1024L)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.20)
                    .build()
            }
            .crossfade(false)
            .build()
    }
}
