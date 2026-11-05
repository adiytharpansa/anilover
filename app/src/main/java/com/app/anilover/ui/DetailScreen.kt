package com.app.anilover.ui

import com.app.anilover.Str
import com.app.anilover.R
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.anilover.data.DownloadItem
import com.app.anilover.data.Episode
import com.app.anilover.data.Title
import com.app.anilover.data.WatchEntry

@Composable
fun DetailScreen(
    title: Title,
    history: List<WatchEntry>,
    inMyList: Boolean,
    rating: Int,
    downloads: List<DownloadItem>,
    similar: List<Title>,
    onBack: () -> Unit,
    onPlay: (Int, Long) -> Unit,
    onToggleMyList: () -> Unit,
    onRate: (Int) -> Unit,
    onDownload: (Episode) -> Unit,
    onDeleteDownload: (DownloadItem) -> Unit,
    onOpenTitle: (Title) -> Unit,
) {
    val context = LocalContext.current
    val progressByEp = history.filter { it.titleId == title.id }
        .associateBy { it.episode }
    val resume = progressByEp.values
        .filter { it.isStarted && !it.isFinished }
        .maxByOrNull { it.updatedAt }
    val downloadByEp = downloads.filter { it.titleId == title.id }.associateBy { it.episode }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var synopsisOverflow by remember(title.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val hasTrailer = !title.trailer.isNullOrBlank()
    val tabKeys = buildList {
        add("ep")
        if (hasTrailer) add("trailer")
        if (similar.isNotEmpty()) add("similar")
    }
    var tabKey by remember(title.id) { mutableStateOf("ep") }
    val activeTab = if (tabKeys.contains(tabKey)) tabKey else "ep"
    val firstEp = title.episodes.firstOrNull()?.number ?: 1
    val playLabel = if (resume != null) Str.get(R.string.resume_episode_n, resume.episode)
        else Str.get(R.string.play_episode_n, firstEp)
    val playAction = {
        if (resume != null) onPlay(resume.episode, resume.positionMs) else onPlay(firstEp, 0L)
    }
    // Tombol Putar lengket: muncul setelah hero + info tergeser lewat.
    val showSticky by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= 2 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 650)
        }
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(bottom = 84.dp),
    ) {
        item(key = "hero") {
            // Hero menembus status bar. Tanpa banner, poster tegak tidak lagi dipotong 16:9
            // (kabur & kasar): dipakai sebagai latar buram + poster tajam di tengah.
            val posterMode = !hasTrailer && title.banner.isNullOrBlank()
            Box(
                Modifier.fillMaxWidth()
                    .aspectRatio(if (posterMode) 1f else 16f / 10f)
                    .background(AniloverSurface),
            ) {
                when {
                    hasTrailer -> TrailerPlayer(title.trailer.orEmpty())
                    posterMode -> {
                        PosterImage(
                            model = title.poster,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().blur(28.dp).alpha(0.55f),
                        )
                        PosterImage(
                            model = title.poster,
                            contentDescription = title.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.align(Alignment.TopCenter)
                                .statusBarsPadding().padding(top = 18.dp)
                                .fillMaxHeight(0.6f).aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    else -> PosterImage(
                        model = title.banner,
                        contentDescription = title.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.28f to Color.Transparent,
                                0.55f to Color.Transparent,
                                1f to AniloverBackground,
                            )
                        )
                    )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.statusBarsPadding().padding(10.dp).size(40.dp)
                        .background(AniloverBackground.copy(alpha = 0.6f), AniloverShapes.Pill),
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = Str.get(R.string.back), tint = Color.White)
                }
                Text(
                    title.title, color = AniloverTextPrimary, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                // Lencana seragam: status, 18+, skor, HD — satu gaya, satu tinggi.
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    title.statusLabel?.let { DetailBadge(it) }
                    if (title.adult) DetailBadge("18+", container = AniloverAccent, content = AniloverOnAccent)
                    title.score?.let { DetailBadge("★ " + String.format("%.1f", it)) }
                    DetailBadge("HD")
                }
                // Meta: tahun • jumlah episode • kecocokan (pemisah titik, konsisten dengan genre).
                val meta = listOfNotNull(
                    title.year?.toString(),
                    if (title.episodes.isNotEmpty()) Str.plural(R.plurals.episodes_count_cap, title.episodes.size) else null,
                    title.matchPercent?.let { Str.get(R.string.match_pct, it) },
                )
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString("  •  "), color = AniloverTextDim, fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                }
                if (title.genres.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    // Genre jadi chip satu baris yang bisa digeser: tidak ada bullet menggantung.
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        items(title.genres) { g ->
                            Text(
                                g, color = AniloverTextPrimary, fontSize = 12.sp, maxLines = 1,
                                modifier = Modifier
                                    .clip(AniloverShapes.Pill)
                                    .background(AniloverSurfaceHigh)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Tombol Putar selebar layar & kontekstual: "Putar Episode 1" / "Lanjutkan Episode 3".
                Button(
                    onClick = { playAction() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black,
                    ),
                    shape = AniloverShapes.Pill,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(playLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                }
                if (resume != null) {
                    Text(
                        Str.get(R.string.resume_from, formatClock(resume.positionMs)),
                        color = AniloverTextDim, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Satu baris, empat aksi sama lebar.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DetailAction(
                        icon = if (inMyList) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        label = Str.get(R.string.my_list),
                        active = inMyList,
                        onClick = onToggleMyList,
                        modifier = Modifier.weight(1f),
                    )
                    DetailAction(
                        icon = Icons.Filled.ThumbUp,
                        label = when (rating) {
                            2 -> Str.get(R.string.love)
                            1 -> Str.get(R.string.liked)
                            else -> Str.get(R.string.like)
                        },
                        active = rating > 0,
                        // Ketuk berurutan ala Netflix: Suka -> Sangat Suka -> batal.
                        onClick = { onRate(when (rating) { 1 -> 2; 2 -> 0; else -> 1 }) },
                        modifier = Modifier.weight(1f),
                    )
                    DetailAction(
                        icon = Icons.Filled.ThumbDown,
                        label = Str.get(R.string.dislike),
                        active = rating == -1,
                        onClick = { onRate(if (rating == -1) 0 else -1) },
                        modifier = Modifier.weight(1f),
                    )
                    DetailAction(
                        icon = Icons.Filled.Share,
                        label = Str.get(R.string.share),
                        active = false,
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, Str.get(R.string.share_watch, title.title))
                            }
                            context.startActivity(Intent.createChooser(send, Str.get(R.string.share)))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!title.synopsis.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier.fillMaxWidth().animateContentSize(tween(200))
                            .clickable(enabled = synopsisOverflow || synopsisExpanded) { synopsisExpanded = !synopsisExpanded },
                    ) {
                        Text(
                            title.synopsis, color = AniloverTextDim, fontSize = 14.sp, lineHeight = 20.sp,
                            maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!synopsisExpanded) synopsisOverflow = it.hasVisualOverflow },
                        )
                        // "Lainnya" hanya muncul bila teks memang terpotong.
                        if (synopsisOverflow || synopsisExpanded) {
                            Text(
                                if (synopsisExpanded) Str.get(R.string.close) else Str.get(R.string.more),
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (key in tabKeys) {
                        val label = when (key) {
                            "ep" -> "Episode"
                            "trailer" -> Str.get(R.string.tab_trailers)
                            else -> Str.get(R.string.tab_similar)
                        }
                        Column(
                            Modifier.weight(1f).clickable { tabKey = key },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (activeTab == key) {
                                Box(Modifier.fillMaxWidth().height(3.dp).background(AniloverAccent))
                            } else {
                                Spacer(Modifier.height(3.dp))
                            }
                            Text(
                                label,
                                color = if (activeTab == key) Color.White else AniloverTextDim,
                                fontSize = 13.sp,
                                fontWeight = if (activeTab == key) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        if (activeTab == "ep") {
            items(title.episodes.size, key = { idx -> "ep" + title.episodes[idx].number }) { idx ->
                val ep = title.episodes[idx]
                val prog = progressByEp[ep.number]
                EpisodeRow(
                    ep = ep,
                    prog = prog,
                    dl = downloadByEp[ep.number],
                    current = resume?.episode == ep.number,
                    onClick = {
                        onPlay(ep.number, if (prog != null && prog.isStarted && !prog.isFinished) prog.positionMs else 0L)
                    },
                    onDownload = { onDownload(ep) },
                    onDeleteDownload = onDeleteDownload,
                )
            }
        }
        if (activeTab == "trailer" && hasTrailer) {
            item(key = "trailer") {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp)).background(AniloverSurface),
                    ) {
                        TrailerPlayer(title.trailer.orEmpty(), controls = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Trailer — ${title.title}", color = Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        Str.get(R.string.trailer_desc),
                        color = AniloverTextDim, fontSize = 12.sp,
                    )
                }
            }
        }
        if (activeTab == "similar" && similar.isNotEmpty()) {
            item(key = "similar") { PosterRow(Str.get(R.string.similar_titles), similar, onOpenTitle) }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
    AnimatedVisibility(
        visible = showSticky,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it },
        exit = fadeOut(tween(120)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(Modifier.fillMaxWidth().background(AniloverSurface)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(AniloverOutline))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title.title, color = Color.White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        Str.plural(R.plurals.episodes_count, title.episodes.size),
                        color = AniloverTextDim, fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { playAction() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black,
                    ),
                    shape = AniloverShapes.Pill,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (resume != null) Str.get(R.string.resume_ep, resume.episode)
                        else Str.get(R.string.play_ep, firstEp),
                        fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun DetailBadge(
    text: String,
    container: Color = AniloverSurfaceHigh,
    content: Color = Color.White,
) {
    Text(
        text, color = content, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier
            .background(container, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Satu baris episode. Gambar cuplikan hanya dipakai bila episode punya gambar sendiri;
 * kalau tidak, ditampilkan kotak nomor (bukan poster yang sama diulang 12x).
 */
@Composable
private fun EpisodeRow(
    ep: Episode,
    prog: WatchEntry?,
    dl: DownloadItem?,
    current: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: (DownloadItem) -> Unit,
) {
    val finished = prog != null && prog.isFinished
    val started = prog != null && prog.isStarted && !prog.isFinished
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete && dl != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(Str.get(R.string.delete_download_title)) },
            text = { Text(Str.get(R.string.delete_download_msg, ep.number)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false; onDeleteDownload(dl) }) {
                    Text(Str.get(R.string.delete), color = AniloverDanger)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text(Str.get(R.string.cancel)) }
            },
        )
    }
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(Modifier.heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!ep.thumb.isNullOrBlank()) {
                Box(
                    Modifier.width(118.dp).aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)).background(AniloverSurface),
                ) {
                    PosterImage(
                        model = ep.thumb,
                        contentDescription = Str.get(R.string.episode_n, ep.number),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (finished) 0.55f else 1f),
                    )
                    Box(
                        Modifier.align(Alignment.Center).size(32.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    if (started) {
                        Box(
                            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .height(3.dp).background(Color.White.copy(alpha = 0.35f)),
                        ) {
                            Box(Modifier.fillMaxWidth((prog?.fraction ?: 0f)).height(3.dp).background(AniloverAccent))
                        }
                    }
                }
            } else {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (current) AniloverAccent else AniloverSurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${ep.number}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (current) AniloverOnAccent else Color.White,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ep.title ?: Str.get(R.string.episode_n, ep.number),
                    color = if (finished) AniloverTextDim else Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (!ep.synopsis.isNullOrBlank()) {
                    Text(
                        ep.synopsis, color = AniloverTextDim, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                val status = when {
                    finished -> Str.get(R.string.watched)
                    started -> Str.get(R.string.dur_time_left, formatClock(prog?.durationMs ?: 0L), formatRemainingPlain(prog?.positionMs ?: 0L, prog?.durationMs ?: 0L))
                    dl?.state == "selesai" -> Str.get(R.string.downloaded_size, formatSize(dl.sizeBytes))
                    else -> null
                }
                if (status != null) {
                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (finished) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = AniloverTextDim, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(status, color = AniloverTextDim, fontSize = 12.sp, maxLines = 1)
                    }
                }
                if (started && ep.thumb.isNullOrBlank()) {
                    Box(
                        Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp)
                            .clip(AniloverShapes.Pill).background(Color.White.copy(alpha = 0.2f)),
                    ) {
                        Box(Modifier.fillMaxWidth((prog?.fraction ?: 0f)).height(3.dp).background(AniloverAccent))
                    }
                }
            }
            when (dl?.state) {
                "mengunduh" -> Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { dl.fraction },
                        color = AniloverAccent, modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )
                }
                // Selesai diunduh: tanda centang aksen (ketuk untuk menghapus unduhan).
                "selesai" -> IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = Str.get(R.string.delete_download), tint = AniloverAccent)
                }
                else -> IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = Str.get(R.string.download_episode), tint = AniloverTextDim)
                }
            }
        }
        if (dl?.state == "gagal") {
            Text(
                Str.get(R.string.download_failed_hint),
                color = AniloverAccent, fontSize = 11.sp,
                modifier = Modifier.padding(start = 62.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun DetailAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.height(60.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (active) AniloverAccent else Color.White,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (active) AniloverAccent else AniloverTextDim, fontSize = 11.sp, maxLines = 1)
    }
}
