package com.app.anilover.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import com.app.anilover.R

/** Pemegang pemutar yang sedang aktif agar layanan latar bisa menampilkan kontrolnya. */
object PlaybackHolder {
    @Volatile var player: ExoPlayer? = null
    @Volatile var title: String = ""
    @Volatile var episode: Int = 1
}

/**
 * Layanan latar belakang: menjaga suara tetap berjalan saat layar dimatikan atau
 * aplikasi ditinggalkan, dengan notifikasi media (Putar/Jeda, mundur/maju 10 dtk).
 * Layanan dimulai oleh PlayerActivity saat pemutar dibuat dan dihentikan saat ditutup.
 */
class PlaybackService : Service() {

    private var manager: PlayerNotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        // Wajib segera masuk mode foreground; notifikasi media menggantikannya setelah siap.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, placeholder(), serviceType())
        val player = PlaybackHolder.player
        if (player == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (manager == null) {
            manager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(descriptionAdapter)
                .setNotificationListener(notificationListener)
                .build()
                .apply {
                    setSmallIcon(R.drawable.ic_stat_anilover)
                    setUseNextAction(false)
                    setUsePreviousAction(false)
                }
        }
        manager?.setPlayer(player)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        manager?.setPlayer(null)
        manager = null
        super.onDestroy()
    }

    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence =
            PlaybackHolder.title.ifBlank { "Anilover" }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val open = Intent(this@PlaybackService, PlayerActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            return PendingIntent.getActivity(
                this@PlaybackService, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        override fun getCurrentContentText(player: Player): CharSequence =
            "Episode ${PlaybackHolder.episode}"

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? = null
    }

    private val notificationListener = object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            if (ongoing) {
                ServiceCompat.startForeground(this@PlaybackService, notificationId, notification, serviceType())
            } else {
                // Dijeda: lepas status foreground agar notifikasi bisa digeser hilang.
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun placeholder(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_anilover)
            .setContentTitle(PlaybackHolder.title.ifBlank { "Anilover" })
            .setContentText("Memutar…")
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun serviceType(): Int =
        if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pemutaran", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "anilover_playback"
        private const val NOTIFICATION_ID = 2001
    }
}
