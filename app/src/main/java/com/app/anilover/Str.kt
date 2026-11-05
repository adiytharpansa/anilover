package com.app.anilover

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Akses teks terjemahan dari mana saja (Composable, Service, Worker). Bahasa mengikuti
 * bahasa perangkat: values-in = Indonesia, values = Inggris (cadangan untuk bahasa lain).
 */
object Str {
    lateinit var app: Context

    fun init(context: Context) { app = context.applicationContext }

    fun get(@StringRes id: Int, vararg args: Any?): String =
        if (args.isEmpty()) app.getString(id) else app.getString(id, *args)

    /** Bentuk jamak; bila [args] kosong, jumlah [n] dipakai sebagai argumen pertama. */
    fun plural(@PluralsRes id: Int, n: Int, vararg args: Any?): String =
        app.resources.getQuantityString(id, n, *(if (args.isEmpty()) arrayOf<Any?>(n) else args))
}
