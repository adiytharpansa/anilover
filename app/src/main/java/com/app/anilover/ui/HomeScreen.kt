package com.app.anilover.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.app.anilover.R
import com.app.anilover.data.Title
import com.app.anilover.data.WatchEntry
import kotlinx.coroutines.delay

/**
 * Beranda: hero berputar (pager) di belakang header transparan, Lanjutkan Menonton,
 * Top 10, Baru Ditambahkan, genre, baris pilihan per genre, lalu sisa koleksi.
 * Setiap judul hanya muncul di satu "blok utama" supaya halaman tidak terasa berulang.
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
    onSeeNew: () -> Unit,
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
    val playFirst: (Title) -> Unit = { t -> onPlay(t, t.episodes.firstOrNull()?.number ?: 1, 0L) }

    // Hero: judul berskor tertinggi yang BUKAN sedang ditonton (itu sudah ada di "Lanjutkan").
    val heroes = titles.reversed()
        .filter { !resume.containsKey(it.id) }
        .sortedByDescending { it.score ?: 0.0 }
        .take(5)
        .ifEmpty { titles.take(1) }
    val genreList = titles.flatMap { it.genres }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(10).map { it.key }
    val fresh = titles.reversed().take(10)
    val freshIds = fresh.map { it.id }.toSet()
    val top10 = titles.filter { (it.score ?: 0.0) > 0.0 }.sortedByDescending { it.score }.take(10)
    val genreRows = genreList.take(4)
        .map { g -> g to titles.filter { it.genres.contains(g) }.sortedByDescending { it.score ?: 0.0 } }
        .filter { it.second.size >= 3 }
        .take(2)
    val rest = titles.reversed().filter { !freshIds.contains(it.id) }

    // Lebar poster seragam di semua baris & grid: 3 kolom, dengan ~8dp kartu ke-4 mengintip.
    val posterW: Dp = ((LocalConfiguration.current.screenWidthDp - 64) / 3f).dp

    val listState = rememberLazyListState()
    val fadePx = with(LocalDensity.current) { 240.dp.toPx() }
    val headerAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
        }
    }

    Box(Modifier.fillMaxSize().background(AniloverBackground)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            if (heroes.isNotEmpty()) {
                item(key = "hero") {
                    HeroPager(
                        heroes = heroes, history = history, myList = myList,
                        onOpen = onOpen, onPlay = onPlay, onToggleMyList = onToggleMyList,
                    )
                }
            } else {
                item(key = "hero-space") { Spacer(Modifier.statusBarsPadding().height(56.dp)) }
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
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
            if (top10.size >= 5) {
                item(key = "top10") {
                    SectionTitle("Top 10 Skor Tertinggi")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(top10, key = { _, t -> t.id }) { i, t ->
                            Top10Card(rank = i + 1, t = t, onClick = { onOpen(t) })
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
                        cardWidth = posterW, onSeeAll = onSeeNew,
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
                        items(genreList, key = { it }) { g -> GenreChip(g) { onGenre(g) } }
                    }
                }
            }
            genreRows.forEach { (g, list) ->
                item(key = "genre-row-$g") {
                    PosterRow(
                        "Pilihan $g", list.take(10), onOpen, newIds = newIds, myList = myList,
                        onPlayFirst = playFirst, onToggleMyList = onToggleMyList,
                        onDownloadFirst = onDownloadFirst,
                        cardWidth = posterW, onSeeAll = { onGenre(g) },
                    )
                }
            }
            if (rest.isNotEmpty()) {
                item(key = "all-title") { SectionTitle("Koleksi Lainnya") }
                items(rest.chunked(3), key = { row -> "rest-" + row.first().id }) { row ->
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
        HomeHeader(
            alpha = headerAlpha, onBell = onSeeNew,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/** Header melayang: transparan di atas hero, perlahan solid saat di-scroll. */
@Composable
private fun HomeHeader(alpha: Float, onBell: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.55f * (1f - alpha)), Color.Transparent),
                ),
            )
            .background(AniloverBackground.copy(alpha = alpha)),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp)
                .padding(start = 20.dp, end = 12.dp),
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
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.weight(1f))
            HeaderAction(Icons.Outlined.Notifications, "Judul baru & segera tayang", onBell)
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp)
                .background(AniloverOutline.copy(alpha = alpha * 0.6f)),
        )
    }
}

/** Tombol ikon bulat transparan untuk header. */
@Composable
private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.35f), AniloverShapes.Pill),
    ) {
        Icon(icon, contentDescription = desc, tint = AniloverTextPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HeroPager(
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
    // Putar otomatis tiap 6 detik; berhenti saat disentuh atau saat trailer sedang diputar.
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
        items(list, key = { it.id }) { t ->
            PosterCard(
                t = t,
                modifier = Modifier.width(cardWidth),
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
        fontFamily = AniloverFont,
        fontSize = if (rank < 10) 110.sp else 82.sp,
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
                    drawStyle = Stroke(width = 7f, join = androidx.compose.ui.graphics.StrokeJoin.Round),
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
private fun GenreChip(text: String, onClick: () -> Unit) {
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
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.2f to Color.Transparent,
                    0.50f to Color.Transparent,
                    0.78f to AniloverBackground.copy(alpha = 0.88f),
                    1f to AniloverBackground,
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                t.seriesName, color = AniloverTextPrimary, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                t.matchPercent?.let {
                    Text("$it% Cocok", color = AniloverGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                t.year?.let { Text(it.toString(), color = AniloverTextDim, fontSize = 12.sp) }
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
                    Text("${t.episodes.size} Eps", color = AniloverTextDim, fontSize = 12.sp)
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
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (resume != null) "Lanjutkan" else "Putar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Button(
                    onClick = onToggleMyList,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverSurfaceHigh.copy(alpha = 0.92f),
                        contentColor = AniloverTextPrimary,
                    ),
                    shape = AniloverShapes.Button,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(
                        if (inMyList) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = null, modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Daftarku", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

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

/** Judul kartu: nama seri (maks. 2 baris, tebal) + season/bagian atau tahun (redup). Tinggi menyesuaikan isi. */
@Composable
fun CardTitles(t: Title) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            t.seriesName, color = AniloverTextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(
            t.titleSub ?: listOfNotNull(
                t.year?.toString(),
                if (t.episodes.isNotEmpty()) "${t.episodes.size} Episode" else null,
            ).joinToString(" · "),
            color = AniloverTextDim,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
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
        Box(Modifier.pressScaleFx(pressSource)) {
            // Poster polos tanpa bingkai: sudut halus saja.
            Box(
                Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .clip(AniloverShapes.Poster).background(AniloverSurface)
                    .combinedClickable(
                        interactionSource = pressSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = if (hasMenu) ({ menuOpen = true }) else null,
                    ),
            ) {
                PosterImage(
                    model = t.poster ?: t.banner,
                    contentDescription = t.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (badge != null) {
                    Text(
                        badge, color = AniloverOnAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(AniloverGold, RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
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
        PosterImage(
            model = t.banner ?: t.poster,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Poster).background(AniloverSurface),
        )
        CardTitles(t)
    }
}

/** Kartu "Lanjutkan Menonton": info episode & sisa waktu di atas thumbnail, judul di bawah. */
@Composable
private fun ContinueCard(
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
                Modifier.align(Alignment.Center).size(38.dp)
                    .background(Color.Black.copy(alpha = 0.55f), AniloverShapes.Pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow, contentDescription = "Lanjutkan",
                    tint = Color.White, modifier = Modifier.size(26.dp),
                )
            }
            Text(
                "E${entry.episode} · Sisa " + formatRemainingPlain(entry.positionMs, entry.durationMs),
                color = Color.White, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp),
            )
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .height(4.dp).background(Color.White.copy(alpha = 0.35f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(entry.fraction.coerceIn(0f, 1f))
                        .height(4.dp).background(AniloverAccent),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.seriesName, color = AniloverTextPrimary,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
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
