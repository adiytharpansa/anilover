package com.app.anilover.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.app.anilover.data.Title
import com.app.anilover.data.WatchEntry
import kotlinx.coroutines.delay

/** Hero berputar otomatis tiap 6 detik; berhenti saat disentuh atau saat trailer diputar. */
@Composable
internal fun HeroPager(
    heroes: List<Title>,
    history: List<WatchEntry>,
    myList: Set<String>,
    onOpen: (Title) -> Unit,
    onPlay: (Title, Int, Long) -> Unit,
    onToggleMyList: (Title) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { heroes.size })
    val current = pagerState.currentPage
    val scrolling = pagerState.isScrollInProgress
    LaunchedEffect(current, scrolling, heroes.size) {
        if (heroes.size > 1 && !scrolling && heroes.getOrNull(current)?.trailer.isNullOrBlank()) {
            delay(6000)
            pagerState.animateScrollToPage((current + 1) % heroes.size)
        }
    }
    Box(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val t = heroes.getOrNull(page) ?: return@HorizontalPager
            Billboard(
                t = t, history = history,
                inMyList = myList.contains(t.id),
                active = page == pagerState.currentPage,
                onOpen = { onOpen(t) },
                onPlay = { ep, pos -> onPlay(t, ep, pos) },
                onToggleMyList = { onToggleMyList(t) },
            )
        }
        if (heroes.size > 1) {
            // Indikator dinaikkan sedikit supaya tidak menempel ke tombol Putar/Daftarku.
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(heroes.size) { i ->
                    val selected = i == current
                    val w by animateDpAsState(if (selected) 18.dp else 6.dp, label = "heroDot")
                    Box(
                        Modifier.height(6.dp).width(w).clip(AniloverShapes.Pill)
                            .background(if (selected) AniloverAccent else Color.White.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

/**
 * Pemutar trailer billboard: video bisu berputar otomatis (gaya Netflix).
 * Aktif hanya bila katalog menyediakan URL trailer untuk judul billboard;
 * tanpa data trailer, billboard memakai gambar banner seperti biasa.
 */
@Composable
internal fun TrailerPlayer(url: String, controls: Boolean = false) {
    val context = LocalContext.current
    val exo = remember(url, controls) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            volume = if (controls) 1f else 0f
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(url) { onDispose { exo.release() } }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exo
                useController = controls
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun Billboard(
    t: Title,
    history: List<WatchEntry>,
    inMyList: Boolean,
    active: Boolean,
    onOpen: () -> Unit,
    onPlay: (Int, Long) -> Unit,
    onToggleMyList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resume = history
        .filter { it.titleId == t.id && it.isStarted && !it.isFinished }
        .maxByOrNull { it.updatedAt }
    val type = MaterialTheme.typography
    // Billboard potret ala Netflix seluler: poster penuh sampai ke belakang status bar,
    // judul di tengah bawah, dua tombol sejajar.
    Box(
        modifier.fillMaxWidth().aspectRatio(0.78f).background(AniloverSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            ),
    ) {
        if (active && !t.trailer.isNullOrBlank()) {
            TrailerPlayer(url = t.trailer!!)
        } else {
            PosterImage(
                model = t.poster ?: t.banner,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Gradasi bawah lebih kuat: judul, metadata, dan tombol selalu terbaca di poster terang.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.2f to Color.Transparent,
                    0.42f to Color.Transparent,
                    0.72f to AniloverBackground.copy(alpha = 0.90f),
                    1f to AniloverBackground,
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                t.seriesName, color = AniloverTextPrimary,
                style = type.headlineLarge,
                maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                t.matchPercent?.let {
                    Text("$it% Cocok", color = AniloverGreen, style = type.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                t.year?.let { Text(it.toString(), color = AniloverTextDim, style = type.labelMedium) }
                t.statusLabel?.let {
                    Text(
                        it, color = AniloverTextPrimary,
                        style = type.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    "HD", color = AniloverTextPrimary,
                    style = type.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
                if (t.episodes.isNotEmpty()) {
                    Text("${t.episodes.size} Eps", color = AniloverTextDim, style = type.labelMedium)
                }
            }
            if (t.genres.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    t.genres.take(3).joinToString("  •  "),
                    color = AniloverTextPrimary, style = type.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        if (resume != null) onPlay(resume.episode, resume.positionMs)
                        else onPlay(t.episodes.firstOrNull()?.number ?: 1, 0L)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                    ),
                    shape = AniloverShapes.Button,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (resume != null) "Lanjutkan" else "Putar", style = type.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                // Tombol sekunder diberi garis tepi tegas agar sepadan dengan tombol Putar.
                Button(
                    onClick = onToggleMyList,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverSurfaceHigh.copy(alpha = 0.92f),
                        contentColor = AniloverTextPrimary,
                    ),
                    shape = AniloverShapes.Button,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AniloverTextPrimary.copy(alpha = 0.7f)),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(
                        if (inMyList) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = null, modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Daftarku", style = type.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
