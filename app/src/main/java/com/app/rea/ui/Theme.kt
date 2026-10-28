package com.app.rea.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ReaBackground = Color(0xFF141414)
val ReaSurface = Color(0xFF1F1F1F)
val ReaAccent = Color(0xFFE50914)
val ReaTextDim = Color(0xFFB3B3B3)

private val ReaColors = darkColorScheme(
    primary = ReaAccent,
    background = ReaBackground,
    surface = ReaSurface,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun ReaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReaColors, content = content)
}

fun formatClock(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Sisa waktu gaya Netflix: "-12:34". Dipakai di dalam pemutar. */
fun formatRemaining(positionMs: Long, durationMs: Long): String =
    if (durationMs <= 0) "--:--" else "-" + formatClock(durationMs - positionMs)

/** Sisa waktu polos tanpa tanda minus: "12:34". Dipakai di kartu dan detail. */
fun formatRemainingPlain(positionMs: Long, durationMs: Long): String =
    if (durationMs <= 0) "--:--" else formatClock(durationMs - positionMs)

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format("%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format("%.0f MB", bytes / 1_000_000.0)
    bytes > 0 -> String.format("%.0f KB", bytes / 1_000.0)
    else -> "0 MB"
}
