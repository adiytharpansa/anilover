package com.app.anilover

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import android.util.Log

/**
 * Akses teks terjemahan dari mana saja (Composable, Service, Worker). Bahasa mengikuti
 * bahasa perangkat: values-in = Indonesia, values = Inggris (cadangan untuk bahasa lain).
 * Thread-safe dan menangani kasus jika context belum diinisialisasi.
 */
object Str {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) { 
        appContext = context.applicationContext
    }

    fun get(@StringRes id: Int, vararg args: Any?): String {
        val ctx = appContext ?: return ""  // Return empty string instead of crash
        return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
    }

    /** Bentuk jamak; bila [args] kosong, jumlah [n] dipakai sebagai argumen pertama. */
    fun plural(@PluralsRes id: Int, n: Int, vararg args: Any?): String {
        val ctx = appContext ?: return ""
        return ctx.resources.getQuantityString(id, n, *(if (args.isEmpty()) arrayOf<Any?>(n) else args))
    }

    /** Check if Str is initialized */
    fun isInitialized(): Boolean = appContext != null
}
