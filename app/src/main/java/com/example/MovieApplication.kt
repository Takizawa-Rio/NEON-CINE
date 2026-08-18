package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MovieApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return try {
            val builder = ImageLoader.Builder(this)
                .crossfade(true)
                .allowHardware(true)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }

            try {
                builder.diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
            } catch (e: Throwable) {
                Log.w("MovieApplication", "Disk cache init fallback: ${e.message}")
            }

            builder.build()
        } catch (e: Throwable) {
            Log.e("MovieApplication", "ImageLoader creation fallback: ${e.message}")
            ImageLoader(this)
        }
    }
}


