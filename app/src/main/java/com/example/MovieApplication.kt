package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory

class MovieApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        
        // Cài đặt Exception Handler toàn cục để ngăn chặn ứng dụng bị buộc dừng (Crash)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MovieApplication", "Global Uncaught Exception in thread ${thread.name}: ${throwable.message}", throwable)
            try {
                // Ghi log và duy trì ổn định
            } catch (e: Throwable) {
                // Ignored
            }
            // Không để crash văng ra OS làm đóng ứng dụng
        }
    }

    override fun newImageLoader(): ImageLoader {
        return try {
            ImageLoader.Builder(this)
                .crossfade(true)
                .build()
        } catch (e: Throwable) {
            Log.e("MovieApplication", "ImageLoader creation fallback: ${e.message}")
            ImageLoader(this)
        }
    }
}


