package com.app.anilover

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Penyedia opsi Google Cast untuk Anilover. Memakai Default Media Receiver
 * (receiver bawaan Google) yang dapat memutar HLS langsung dari URL gudang,
 * jadi tidak perlu receiver khusus. Nama kelas ini dirujuk dari AndroidManifest.
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(
                CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
            )
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
