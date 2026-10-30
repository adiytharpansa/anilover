package com.app.anilover.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.anilover.MainActivity
import com.app.anilover.R

/**
 * Pemeriksa katalog berkala (WorkManager): membandingkan katalog api.anime
 * dengan id yang terakhir diketahui perangkat.
 * - Judul yang ditandai "Ingatkan Saya" dan kini terbit -> notifikasi khusus.
 * - Judul baru lain -> notifikasi ringkas.
 * Jalan pertama hanya mencatat keadaan awal (tanpa notifikasi).
 */
class CatalogCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val library = LibraryStore(applicationContext)
        val fresh = try {
            CatalogApi.fetchCatalog()
        } catch (e: Exception) {
            return Result.retry()
        }
        val freshIds = fresh.map { it.id }.toSet()
        val known = library.knownIdsNow()
        if (known == null) {
            library.setKnownIds(freshIds)
            return Result.success()
        }
        val newTitles = fresh.filter { it.id !in known }
        if (newTitles.isNotEmpty()) {
            val reminders = library.remindersNow()
            val reminded = newTitles.filter { reminders.contains(it.id) }
            val others = newTitles.filter { !reminders.contains(it.id) }
            if (reminded.isNotEmpty()) {
                notify(
                    "Sudah tayang di Anilover",
                    reminded.joinToString(", ") { it.seriesName } +
                        " — yang kamu tunggu sudah bisa ditonton.",
                    1001,
                )
                library.removeReminders(reminded.map { it.id }.toSet())
            }
            if (others.isNotEmpty()) {
                val names = others.take(3).joinToString(", ") { it.seriesName }
                val suffix = if (others.size > 3) " dan ${others.size - 3} judul lain" else ""
                notify("Judul baru di Anilover", names + suffix, 1002)
            }
        }
        library.setKnownIds(freshIds)
        return Result.success()
    }

    private fun notify(title: String, text: String, notificationId: Int) {
        val ctx = applicationContext
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Judul & Episode Baru", NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_anilover)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notif)
    }

    companion object {
        const val CHANNEL_ID = "anilover_updates"
    }
}
