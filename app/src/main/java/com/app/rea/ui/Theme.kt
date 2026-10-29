package com.app.rea.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

val ReaBackground = Color(0xFF141414)
val ReaSurface = Color(0xFF1F1F1F)
val ReaAccent = Color(0xFFE50914)
val ReaTextDim = Color(0xFFB3B3B3)
val ReaSurfaceHigh = Color(0xFF2B2B2B)
val ReaGreen = Color(0xFF46D369)

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

/** Kotak rangka berkilau (shimmer) untuk keadaan memuat, gaya Netflix. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(6.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    Box(modifier.background(Color.White.copy(alpha = alpha), shape))
}

/** Efek memampat halus saat kartu ditekan; pasangkan dengan interactionSource klik yang sama. */
@Composable
fun Modifier.pressScaleFx(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
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
