package com.app.anilover.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.app.anilover.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

// ── Token warna: "Tinta" — dasar biru-tinta gelap, aksen sakura, emas untuk sorotan ──
val AniloverBackground = Color(0xFF0D0E17)
val AniloverSurface = Color(0xFF161828)
val AniloverSurfaceHigh = Color(0xFF212438)
val AniloverOutline = Color(0xFF2E3250)
val AniloverAccent = Color(0xFFFF5C85)
/** Teks/ikon di atas aksen sakura. */
val AniloverOnAccent = Color(0xFF1C0610)
val AniloverGold = Color(0xFFFFC857)
val AniloverTextPrimary = Color(0xFFF3F4FA)
val AniloverTextDim = Color(0xFF9EA3BF)
val AniloverGreen = Color(0xFF3DD68C)

/** Skala radius: kecil (lencana), sedang (poster/kartu), besar (tombol/panel). */
object AniloverShapes {
    val Chip = RoundedCornerShape(8.dp)
    val Poster = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(12.dp)
    val Button = RoundedCornerShape(14.dp)
    val Pill = RoundedCornerShape(50)
}

/**
 * Font utama: Poppins (lisensi OFL) dibundel di res/font, jadi tampil sama di semua
 * perangkat. Ganti font cukup dengan mengubah daftar Font di bawah ini.
 */
val AniloverFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val AniloverColors = darkColorScheme(
    primary = AniloverAccent,
    onPrimary = AniloverOnAccent,
    secondary = AniloverGold,
    background = AniloverBackground,
    surface = AniloverSurface,
    surfaceVariant = AniloverSurfaceHigh,
    outline = AniloverOutline,
    onBackground = AniloverTextPrimary,
    onSurface = AniloverTextPrimary,
    onSurfaceVariant = AniloverTextDim,
)

/** Skala tipografi: judul rapat & tebal, isi longgar agar nyaman dibaca. */
private val AniloverTypography = Typography(
    headlineLarge = TextStyle(fontFamily = AniloverFont, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = AniloverFont, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = AniloverFont, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = AniloverFont, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = AniloverFont, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = AniloverFont, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = AniloverFont, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = AniloverFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = AniloverFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = AniloverFont, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

/** Judul halaman tab: besar, rapat, dengan subjudul opsional. */
@Composable
fun ScreenTitle(text: String, subtitle: String? = null) {
    androidx.compose.foundation.layout.Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
    ) {
        androidx.compose.material3.Text(
            text, color = AniloverTextPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
        if (!subtitle.isNullOrBlank()) {
            androidx.compose.material3.Text(
                subtitle, color = AniloverTextDim,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Panel permukaan bergaris tepi halus untuk mengelompokkan isi. */
fun Modifier.panel(): Modifier = this
    .clip(AniloverShapes.Card)
    .background(AniloverSurface)
    .border(1.dp, AniloverOutline, AniloverShapes.Card)

@Composable
fun aniloverSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = AniloverOnAccent,
    checkedTrackColor = AniloverAccent,
    uncheckedThumbColor = AniloverTextDim,
    uncheckedTrackColor = AniloverSurfaceHigh,
    uncheckedBorderColor = AniloverOutline,
)

@Composable
fun aniloverChipColors() = androidx.compose.material3.FilterChipDefaults.filterChipColors(
    containerColor = AniloverSurface,
    labelColor = AniloverTextPrimary,
    selectedContainerColor = AniloverAccent,
    selectedLabelColor = AniloverOnAccent,
)

@Composable
fun AniloverTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AniloverColors, typography = AniloverTypography, content = content)
}

/** Kotak rangka berkilau (shimmer) untuk keadaan memuat, gaya Netflix. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    Box(modifier.background(AniloverSurfaceHigh.copy(alpha = alpha), shape))
}

/** Efek memampat halus saat kartu ditekan; pasangkan dengan interactionSource klik yang sama. */
@Composable
fun Modifier.pressScaleFx(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
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

/** Muncul halus (fade + naik sedikit) sekali saja saat kartu pertama kali tampil; [delayMs] untuk efek berurutan. */
@Composable
fun Modifier.appearFx(delayMs: Int = 0): Modifier {
    var shown by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(360, delayMillis = delayMs),
        label = "appear",
    )
    return this.graphicsLayer {
        alpha = a
        translationY = (1f - a) * 22.dp.toPx()
    }
}
