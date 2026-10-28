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

/** Sisa waktu gaya Netflix: "-12:34". */
fun formatRemaining(positionMs: Long, durationMs: Long): String =
    if (durationMs <= 0) "--:--" else "-" + formatClock(durationMs - positionMs)
