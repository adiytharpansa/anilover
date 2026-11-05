package com.app.anilover

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AniloverApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Str.init(this)
    }

    /**
     * Pemuat gambar tunggal untuk seluruh aplikasi (poster, banner, thumbnail):
     *  - cache memori 25% RAM + cache disk 256 MB, sehingga poster yang sudah
     *    pernah dilihat langsung tampil tanpa unduh ulang;
     *  - mengabaikan header cache server (CDN sering mengirim no-cache);
     *  - OkHttp sendiri dengan dispatcher lebih longgar dan User-Agent tetap;
     *  - RGB565 + crossfade bawaan dimatikan (PosterImage mengatur sendiri).
     */
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 48
                maxRequestsPerHost = 12
            })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Anilover Android)")
                        .build()
                )
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.25).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
}
