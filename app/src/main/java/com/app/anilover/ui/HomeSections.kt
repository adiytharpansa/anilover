package com.app.anilover.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.data.Title
import com.app.anilover.data.WatchEntry

/** Judul bagian; bila [onSeeAll] diberikan, muncul tautan "Lihat semua ›" di kanan. */
@Composable
fun SectionTitle(text: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, color = AniloverTextPrimary, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (onSeeAll != null) {
            Row(
                Modifier.clip(AniloverShapes.Pill).clickable(onClick = onSeeAll)
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Lihat semua", color = AniloverAccent,
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    Icons.Filled.ChevronRight, contentDescription = null,
                    tint = AniloverAccent, modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun PosterRow(
    title: String,
    list: List<Title>,
    onOpen: (Title) -> Unit,
    newIds: Set<String> = emptySet(),
    myList: Set<String>? = null,
    onPlayFirst: ((Title) -> Unit)? = null,
    onToggleMyList: ((Title) -> Unit)? = null,
    onDownloadFirst: ((Title) -> Unit)? = null,
    cardWidth: Dp = 116.dp,
    onSeeAll: (() -> Unit)? = null,
) {
    SectionTitle(title, onSeeAll)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
            PosterCard(
                t = t,
                modifier = Modifier.width(cardWidth),
                appearDelayMs = i.coerceAtMost(6) * 45,
                parallax = true,
                badge = if (newIds.contains(t.id)) "BARU" else null,
                inMyList = myList?.contains(t.id) ?: false,
                onPlayFirst = onPlayFirst?.let { f -> { f(t) } },
                onToggleMyList = onToggleMyList?.let { f -> { f(t) } },
                onDownloadFirst = onDownloadFirst?.let { f -> { f(t) } },
                onClick = { onOpen(t) },
            )
        }
    }
}

/** Baris kartu lanskap (banner 16:9) untuk variasi bentuk, mis. "Baru Ditambahkan". */
@Composable
fun LandscapeRow(
    title: String,
    list: List<Title>,
    onOpen: (Title) -> Unit,
    newIds: Set<String> = emptySet(),
    myList: Set<String> = emptySet(),
    onToggleMyList: ((Title) -> Unit)? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    val cardW: Dp = minOf((LocalConfiguration.current.screenWidthDp * 0.68f).dp, 300.dp)
    SectionTitle(title, onSeeAll)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
            LandscapeCard(
                t = t,
                modifier = Modifier.width(cardW),
                appearDelayMs = i.coerceAtMost(5) * 50,
                isNew = newIds.contains(t.id),
                inMyList = myList.contains(t.id),
                onToggleMyList = onToggleMyList?.let { f -> { f(t) } },
                onClick = { onOpen(t) },
            )
        }
    }
}

/** Ukuran angka raksasa Top 10: satu digit (1–9) dan dua digit (10). */
private val RankSizeSingle = 110.sp
private val RankSizeDouble = 82.sp

/** Kartu Top 10 gaya Netflix: angka raksasa bergaris tepi abu-abu + poster yang menimpanya. */
@Composable
fun Top10Card(rank: Int, t: Title, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    val numStyle = TextStyle(
        fontFamily = AniloverFont,
        fontSize = if (rank < 10) RankSizeSingle else RankSizeDouble,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-6).sp,
    )
    Row(
        Modifier.pressScaleFx(pressSource)
            .clickable(interactionSource = pressSource, indication = LocalIndication.current, onClick = onClick),
        verticalAlignment = Alignment.Bottom,
    ) {
        // Dua lapis: garis tepi terang + isi warna latar.
        Box(contentAlignment = Alignment.BottomEnd) {
            Text(
                text = rank.toString(),
                style = numStyle.copy(
                    color = AniloverTextDim,
                    drawStyle = Stroke(width = 7f, join = StrokeJoin.Round),
                ),
            )
            Text(text = rank.toString(), style = numStyle.copy(color = AniloverBackground))
        }
        PosterImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(100.dp).aspectRatio(2f / 3f)
                .offset(x = (-14).dp)
                .clip(AniloverShapes.Poster).background(AniloverSurface),
        )
    }
}

/** Chip genre: tipis, dengan semburat aksen supaya terasa bisa diketuk. */
@Composable
internal fun GenreChip(text: String, onClick: () -> Unit) {
    Text(
        text, color = AniloverTextPrimary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(AniloverShapes.Pill)
            .background(
                Brush.horizontalGradient(
                    listOf(AniloverAccent.copy(alpha = 0.16f), AniloverSurface),
                ),
            )
            .border(1.dp, AniloverOutline, AniloverShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
fun EpisodeCard(t: Title, episode: Int, entry: WatchEntry?, onClick: () -> Unit) {
    val type = MaterialTheme.typography
    Column(Modifier.width(170.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Poster).background(AniloverSurface),
        ) {
            PosterImage(
                model = t.banner ?: t.poster,
                contentDescription = "Episode $episode",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "E$episode", color = AniloverTextPrimary,
                    style = type.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            if (entry != null && entry.isStarted && !entry.isFinished) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .height(4.dp).background(Color.White.copy(alpha = 0.35f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(entry.fraction).height(4.dp)
                            .background(AniloverAccent),
                    )
                }
            }
        }
        Text(
            "Episode $episode", color = AniloverTextPrimary, style = type.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            when {
                entry != null && entry.isFinished -> "Selesai ditonton"
                entry != null && entry.isStarted ->
                    "Sisa " + formatRemainingPlain(entry.positionMs, entry.durationMs)
                else -> t.seriesName
            },
            color = AniloverTextDim, style = type.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Kartu "Lanjutkan Menonton": info episode & sisa waktu di atas thumbnail, judul di bawah.
 * Tombol putar dikecilkan dan bilah progres dipertebal supaya gambar tetap terlihat.
 */
@Composable
internal fun ContinueCard(
    t: Title,
    entry: WatchEntry,
    onClick: () -> Unit,
    onInfo: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val pressSource = remember { MutableInteractionSource() }
    Column(modifier.width(200.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .pressScaleFx(pressSource)
                .clip(AniloverShapes.Poster).background(AniloverSurface)
                .clickable(interactionSource = pressSource, indication = LocalIndication.current, onClick = onClick),
        ) {
            PosterImage(
                model = t.banner ?: t.poster,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.40f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
            )
            Box(
                Modifier.align(Alignment.Center).size(32.dp)
                    .background(Color.Black.copy(alpha = 0.45f), AniloverShapes.Pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow, contentDescription = "Lanjutkan",
                    tint = Color.White, modifier = Modifier.size(22.dp),
                )
            }
            Text(
                "E${entry.episode} · Sisa " + formatRemainingPlain(entry.positionMs, entry.durationMs),
                color = Color.White, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 12.dp),
            )
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .height(5.dp).background(Color.White.copy(alpha = 0.35f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(entry.fraction.coerceIn(0f, 1f))
                        .height(5.dp).background(AniloverAccent),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.seriesName, color = AniloverTextPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = 2.dp),
            )
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Pilihan", tint = AniloverTextDim, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Detail") },
                        onClick = { menuOpen = false; onInfo() },
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus dari baris") },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
    }
}
