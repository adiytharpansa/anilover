package com.app.anilover.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.anilover.R
import com.app.anilover.data.Title
import com.app.anilover.data.WatchEntry

/**
 * Beranda: hero unggulan, Lanjutkan Menonton, Baru Ditambahkan, genre, lalu semua judul.
 * Judul yang sedang ditonton tampil paling depan dengan bilah progres tipis di poster.
 */
@Composable
fun HomeScreen(
    titles: List<Title>,
    history: List<WatchEntry>,
    myList: Set<String>,
    ratings: Map<String, Int>,
    hiddenContinue: Set<String>,
    onOpen: (Title) -> Unit,
    onPlay: (Title, Int, Long) -> Unit,
    onToggleMyList: (Title) -> Unit,
    onDownloadFirst: (Title) -> Unit,
    onRemoveContinue: (WatchEntry) -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onGenre: (String) -> Unit,
) {
    val byId = titles.associateBy { it.id }
    val resume: Map<String, WatchEntry> = history
        .filter {
            it.isStarted && !it.isFinished && byId.containsKey(it.titleId) &&
                !hiddenContinue.contains("${it.titleId}|${it.episode}")
        }
        .sortedByDescending { it.updatedAt }
        .distinctBy { it.titleId }
        .associateBy { it.titleId }
    // Urutan katalog = urutan terbit di gudang; yang terakhir masuk = terbaru.
    val newIds = if (titles.size >= 4) titles.takeLast(3).map { it.id }.toSet() else emptySet()
    val ordered = resume.keys.mapNotNull { byId[it] } +
        titles.reversed().filter { !resume.containsKey(it.id) }
    val playFirst: (Title) -> Unit = { t -> onPlay(t, t.episodes.firstOrNull()?.number ?: 1, 0L) }

    Column(Modifier.fillMaxSize().background(AniloverBackground)) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.anilover_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 22.dp, height = 27.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Anilover", color = AniloverTextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
            Spacer(Modifier.weight(1f))
            HeaderAction(Icons.Filled.Search, "Cari", onSearch)
            Spacer(Modifier.width(8.dp))
            HeaderAction(Icons.Filled.AccountCircle, "Profil", onProfile)
        }
        val featured = ordered.firstOrNull()
        val genreList = titles.flatMap { it.genres }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(10).map { it.key }
        val fresh = titles.reversed().take(10)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            if (featured != null) {
                item(key = "hero") {
                    Billboard(
                        t = featured, history = history,
                        inMyList = myList.contains(featured.id),
                        genres = featured.genres.take(3), onGenre = onGenre,
                        onOpen = { onOpen(featured) },
                        onPlay = { ep, pos -> onPlay(featured, ep, pos) },
                        onToggleMyList = { onToggleMyList(featured) },
                    )
                }
            }
            if (resume.isNotEmpty()) {
                item(key = "continue") {
                    SectionTitle("Lanjutkan Menonton")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(resume.values.sortedByDescending { it.updatedAt }.toList(), key = { it.titleId }) { e ->
                            val t = byId[e.titleId] ?: return@items
                            ContinueCard(
                                t = t, entry = e,
                                onClick = { onPlay(t, e.episode, e.positionMs) },
                                onInfo = { onOpen(t) },
                                onRemove = { onRemoveContinue(e) },
                            )
                        }
                    }
                }
            }
            if (fresh.isNotEmpty()) {
                item(key = "fresh") {
                    PosterRow(
                        "Baru Ditambahkan", fresh, onOpen, newIds = newIds, myList = myList,
                        onPlayFirst = playFirst, onToggleMyList = onToggleMyList,
                        onDownloadFirst = onDownloadFirst,
                    )
                }
            }
            if (genreList.isNotEmpty()) {
                item(key = "genres") {
                    SectionTitle("Jelajahi Genre")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(genreList) { g ->
                            Text(
                                g, color = AniloverTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clip(AniloverShapes.Pill)
                                    .background(AniloverSurface)
                                    .border(1.dp, AniloverOutline, AniloverShapes.Pill)
                                    .clickable { onGenre(g) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
            item(key = "all-title") { SectionTitle("Semua Judul") }
            items(ordered.chunked(3), key = { row -> row.first().id }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { t ->
                        PosterCard(
                            t = t, modifier = Modifier.weight(1f),
                            badge = if (newIds.contains(t.id)) "BARU" else null,
                            progress = resume[t.id]?.fraction,
                            inMyList = myList.contains(t.id),
                            onPlayFirst = { playFirst(t) },
                            onToggleMyList = { onToggleMyList(t) },
                            onDownloadFirst = { onDownloadFirst(t) },
                            onClick = { onOpen(t) },
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Tombol ikon bulat bertonjol halus untuk bilah atas. */
@Composable
private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp).background(AniloverSurface, AniloverShapes.Pill)
            .border(1.dp, AniloverOutline, AniloverShapes.Pill),
    ) {
        Icon(icon, contentDescription = desc, tint = AniloverTextPrimary, modifier = Modifier.size(22.dp))
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
) {
    SectionTitle(title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(list) { t ->
            PosterCard(
                t = t,
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

/** Kartu Top 10 gaya Netflix: angka raksasa bergaris tepi abu-abu + poster yang menimpanya. */
@Composable
fun Top10Card(rank: Int, t: Title, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    val numStyle = TextStyle(
        fontSize = if (rank < 10) 118.sp else 88.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-6).sp,
    )
    Row(
        Modifier.pressScaleFx(pressSource)
            .clickable(interactionSource = pressSource, indication = LocalIndication.current, onClick = onClick),
        verticalAlignment = Alignment.Bottom,
    ) {
        // Dua lapis: garis tepi terang + isi warna latar. (Sebelumnya garis tepi
        // berwarna sama dengan latar sehingga angka tidak terlihat sama sekali.)
        Box(contentAlignment = Alignment.BottomEnd) {
            Text(
                text = rank.toString(),
                style = numStyle.copy(
                    color = AniloverTextDim,
                    drawStyle = Stroke(width = 7f, join = androidx.compose.ui.graphics.StrokeJoin.Round),
                ),
            )
            Text(text = rank.toString(), style = numStyle.copy(color = AniloverBackground))
        }
        AsyncImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(104.dp).aspectRatio(2f / 3f)
                .offset(x = (-14).dp)
                .clip(RoundedCornerShape(4.dp)).background(AniloverSurface),
        )
    }
}

@Composable
private fun GenrePill(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text, color = AniloverTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(shape)
            .background(AniloverBackground.copy(alpha = 0.55f))
            .border(1.dp, AniloverTextPrimary.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun Billboard(
    t: Title,
    history: List<WatchEntry>,
    inMyList: Boolean,
    genres: List<String>,
    onGenre: (String) -> Unit,
    onOpen: () -> Unit,
    onPlay: (Int, Long) -> Unit,
    onToggleMyList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resume = history
        .filter { it.titleId == t.id && it.isStarted && !it.isFinished }
        .maxByOrNull { it.updatedAt }
    // Billboard potret ala Netflix seluler: poster penuh (bukan banner yang
    // dipotong sampai buram), judul di tengah bawah, dua tombol sejajar.
    Box(
        modifier.fillMaxWidth().aspectRatio(0.82f).background(AniloverSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            ),
    ) {
        if (!t.trailer.isNullOrBlank()) {
            TrailerPlayer(url = t.trailer!!)
        } else {
            AsyncImage(
                model = t.poster ?: t.banner,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.25f to Color.Transparent,
                    0.50f to Color.Transparent,
                    0.78f to AniloverBackground.copy(alpha = 0.88f),
                    1f to AniloverBackground,
                )
            )
        )
        if (genres.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(genres) { g -> GenrePill(g) { onGenre(g) } }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                t.seriesName, color = AniloverTextPrimary, fontSize = 28.sp,
                fontWeight = FontWeight.Black, maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 31.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                t.matchPercent?.let {
                    Text("$it% Cocok", color = AniloverGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                t.year?.let { Text(it.toString(), color = AniloverTextDim, fontSize = 13.sp) }
                t.statusLabel?.let {
                    Text(
                        it, color = AniloverTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    "HD", color = AniloverTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
                if (t.episodes.isNotEmpty()) {
                    Text("${t.episodes.size} Eps", color = AniloverTextDim, fontSize = 13.sp)
                }
            }
            if (t.genres.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    t.genres.take(3).joinToString("  •  "),
                    color = AniloverTextPrimary, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
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
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (resume != null) "Lanjutkan" else "Putar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Button(
                    onClick = onToggleMyList,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverSurfaceHigh.copy(alpha = 0.92f),
                        contentColor = AniloverTextPrimary,
                    ),
                    shape = AniloverShapes.Button,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Icon(
                        if (inMyList) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = null, modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Daftarku", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, color = AniloverTextPrimary, style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp),
    )
}

/** Judul kartu dua baris tetap: nama seri (tebal) + season/bagian (redup). */
@Composable
fun CardTitles(t: Title) {
    Column(Modifier.height(42.dp).padding(top = 8.dp)) {
        Text(
            t.seriesName, color = AniloverTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            t.titleSub ?: listOfNotNull(
                t.year?.toString(),
                if (t.episodes.isNotEmpty()) "${t.episodes.size} Episode" else null,
            ).joinToString(" · "),
            color = AniloverTextDim, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

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
    onClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasMenu = onPlayFirst != null || onToggleMyList != null || onDownloadFirst != null
    val pressSource = remember { MutableInteractionSource() }
    Column(modifier) {
        Box {
            AsyncImage(
                model = t.poster ?: t.banner,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .clip(AniloverShapes.Card).background(AniloverSurface)
                    .border(1.dp, AniloverOutline.copy(alpha = 0.6f), AniloverShapes.Card)
                    .pressScaleFx(pressSource)
                    .combinedClickable(
                        interactionSource = pressSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = if (hasMenu) ({ menuOpen = true }) else null,
                    ),
            )
            if (badge != null) {
                Text(
                    badge, color = AniloverOnAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(AniloverGold, AniloverShapes.Chip)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            if (progress != null) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(5.dp)
                            .background(AniloverAccent),
                    )
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

@Composable
fun EpisodeCard(t: Title, episode: Int, entry: WatchEntry?, onClick: () -> Unit) {
    Column(Modifier.width(170.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Card).background(AniloverSurface),
        ) {
            AsyncImage(
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
                Text("E$episode", color = AniloverTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            "Episode $episode", color = AniloverTextPrimary, fontSize = 12.sp,
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
            color = AniloverTextDim, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LandscapeCard(t: Title, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    Column(
        Modifier.width(190.dp).pressScaleFx(pressSource)
            .clickable(interactionSource = pressSource, indication = LocalIndication.current, onClick = onClick),
    ) {
        AsyncImage(
            model = t.banner ?: t.poster,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Card).background(AniloverSurface),
        )
        CardTitles(t)
    }
}

@Composable
private fun ContinueCard(
    t: Title,
    entry: WatchEntry,
    onClick: () -> Unit,
    onInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.width(190.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Card).background(AniloverSurface)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = t.banner ?: t.poster,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.align(Alignment.Center).size(44.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow, contentDescription = "Lanjutkan",
                    tint = Color.White, modifier = Modifier.size(30.dp),
                )
            }
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .height(4.dp).background(Color.White.copy(alpha = 0.35f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(entry.fraction)
                        .height(4.dp).background(AniloverAccent),
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, color = AniloverTextPrimary, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "E${entry.episode} · Sisa " + formatRemainingPlain(entry.positionMs, entry.durationMs),
                    color = AniloverTextDim, fontSize = 11.sp,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Pilihan", tint = Color.White, modifier = Modifier.size(20.dp))
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
