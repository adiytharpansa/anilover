package com.app.anilover.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.data.Title

/** Judul kartu: nama seri (maks. 2 baris, medium) + genre utama & jumlah episode (redup). */
@Composable
fun CardTitles(t: Title) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            t.seriesName, color = AniloverTextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(
            t.titleSub ?: listOfNotNull(
                t.genres.firstOrNull(),
                if (t.episodes.isNotEmpty()) "${t.episodes.size} Eps" else null,
            ).joinToString(" · "),
            color = AniloverTextDim,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

private fun ongoingLabel(t: Title): String? {
    val s = t.status?.lowercase() ?: return null
    return if ("ongoing" in s || "airing" in s || "berjalan" in s) "ONGOING" else null
}

private fun scoreText(t: Title): String? =
    t.score?.takeIf { it > 0.0 }?.let { "★ " + String.format(java.util.Locale("id", "ID"), "%.1f", it) }

/** Pill skor di pojok poster (latar gelap transparan agar terbaca di gambar apa pun). */
@Composable
private fun ScorePill(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = AniloverGold,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/** Tombol "+" / centang kecil di pojok poster untuk Daftarku. */
@Composable
private fun MyListFab(inMyList: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier.size(26.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (inMyList) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = if (inMyList) "Hapus dari Daftarku" else "Tambah ke Daftarku",
            tint = if (inMyList) AniloverAccent else Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

private val BottomShade: Brush
    get() = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))

/** Teks lencana kecil di poster (BARU / ONGOING). */
private val BadgeStyle
    @Composable get() = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    t: Title,
    modifier: Modifier = Modifier.width(116.dp),
    badge: String? = null,
    progress: Float? = null,
    inMyList: Boolean = false,
    onPlayFirst: (() -> Unit)? = null,
    onToggleMyList: (() -> Unit)? = null,
    onDownloadFirst: (() -> Unit)? = null,
    appearDelayMs: Int = 0,
    parallax: Boolean = false,
    onClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasMenu = onPlayFirst != null || onToggleMyList != null || onDownloadFirst != null
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val screenPx = LocalConfiguration.current.screenWidthDp * density
    var centerX by androidx.compose.runtime.mutableFloatStateOf(screenPx / 2f)
    val topBadge = badge ?: ongoingLabel(t)
    val score = scoreText(t)
    Column(modifier.appearFx(appearDelayMs)) {
        Box(Modifier.pressScaleFx(pressSource)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .clip(AniloverShapes.Poster).background(AniloverSurface)
                    .then(
                        if (parallax) Modifier.onGloballyPositioned {
                            centerX = it.positionInWindow().x + it.size.width / 2f
                        } else Modifier
                    )
                    .combinedClickable(
                        interactionSource = pressSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = if (hasMenu) ({
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        }) else null,
                    ),
            ) {
                PosterImage(
                    model = t.poster ?: t.banner,
                    contentDescription = t.title,
                    contentScale = ContentScale.Crop,
                    modifier = if (parallax) Modifier.fillMaxSize().graphicsLayer {
                        // Parallax halus: gambar sedikit tertinggal dari kartu saat baris digulir.
                        val shift = ((screenPx / 2f - centerX) / screenPx).coerceIn(-0.5f, 0.5f) * 0.08f * size.width
                        scaleX = 1.08f; scaleY = 1.08f; translationX = shift
                    } else Modifier.fillMaxSize(),
                )
                // Gradasi gelap di dasar: skor & bilah progres selalu terbaca.
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .fillMaxHeight(0.38f).background(BottomShade),
                )
                if (topBadge != null) {
                    val isNew = badge != null
                    Text(
                        topBadge,
                        color = if (isNew) AniloverOnAccent else Color.White,
                        style = BadgeStyle,
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(
                                if (isNew) AniloverGold else Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(5.dp),
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                if (onToggleMyList != null) {
                    MyListFab(
                        inMyList = inMyList,
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                        onClick = onToggleMyList,
                    )
                }
                if (score != null) {
                    ScorePill(
                        score,
                        Modifier.align(Alignment.BottomEnd)
                            .padding(end = 6.dp, bottom = if (progress != null) 10.dp else 6.dp),
                    )
                }
                if (progress != null) {
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp)
                                .background(AniloverAccent),
                        )
                    }
                }
                if (pressed) {
                    Box(Modifier.fillMaxSize().border(1.5.dp, AniloverAccent, AniloverShapes.Poster))
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (onPlayFirst != null) {
                    DropdownMenuItem(
                        text = { Text("Putar") },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = { menuOpen = false; onPlayFirst() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Detail") },
                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    onClick = { menuOpen = false; onClick() },
                )
                if (onToggleMyList != null) {
                    DropdownMenuItem(
                        text = { Text(if (inMyList) "Hapus dari Daftarku" else "Tambah ke Daftarku") },
                        leadingIcon = {
                            Icon(if (inMyList) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null)
                        },
                        onClick = { menuOpen = false; onToggleMyList() },
                    )
                }
                if (onDownloadFirst != null && t.episodes.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Unduh Episode 1") },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                        onClick = { menuOpen = false; onDownloadFirst() },
                    )
                }
            }
        }
        Column(Modifier.clickable(onClick = onClick)) {
            CardTitles(t)
        }
    }
}

/**
 * Kartu lanskap (banner 16:9) dengan judul di atas gambar. Satu-satunya LandscapeCard:
 * varian lama "judul di bawah gambar" (lebar 190dp) dihapus karena tidak dipakai di mana pun.
 */
@Composable
fun LandscapeCard(
    t: Title,
    modifier: Modifier = Modifier,
    appearDelayMs: Int = 0,
    isNew: Boolean = false,
    inMyList: Boolean = false,
    onToggleMyList: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val score = scoreText(t)
    val type = MaterialTheme.typography
    Box(modifier.appearFx(appearDelayMs).pressScaleFx(src)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Card).background(AniloverSurface)
                .clickable(interactionSource = src, indication = LocalIndication.current, onClick = onClick),
        ) {
            PosterImage(
                model = t.banner ?: t.poster,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f)
                    .background(BottomShade),
            )
            if (isNew) {
                Text(
                    "BARU", color = AniloverOnAccent, style = BadgeStyle,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .background(AniloverGold, RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (onToggleMyList != null) {
                MyListFab(
                    inMyList = inMyList,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                    onClick = onToggleMyList,
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    t.seriesName, color = Color.White,
                    style = type.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(
                    score,
                    t.genres.take(2).joinToString(", ").ifBlank { null },
                ).joinToString("  ·  ")
                if (sub.isNotBlank()) {
                    Text(
                        sub, color = Color.White.copy(alpha = 0.78f), style = type.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (pressed) {
                Box(Modifier.fillMaxSize().border(1.5.dp, AniloverAccent, AniloverShapes.Card))
            }
        }
    }
}
