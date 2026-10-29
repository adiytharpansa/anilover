package com.app.rea.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.app.rea.data.Title
import com.app.rea.data.WatchEntry

@OptIn(ExperimentalFoundationApi::class)
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
    val continueWatching = history
        .filter {
            it.isStarted && !it.isFinished && byId.containsKey(it.titleId) &&
                !hiddenContinue.contains("${it.titleId}|${it.episode}")
        }
        .sortedByDescending { it.updatedAt }
    val featured = titles.maxByOrNull { it.score ?: 0.0 } ?: titles.firstOrNull()
    val myListTitles = titles.filter { myList.contains(it.id) }
    // Urutan katalog = urutan terbit di gudang; yang terakhir masuk = terbaru.
    val newIds = if (titles.size >= 4) titles.takeLast(3).map { it.id }.toSet() else emptySet()
    val top10 = titles.sortedByDescending { it.score ?: 0.0 }.take(10)
    // Chip kategori ala Netflix: genre terlaris di katalog, urut jumlah judul.
    val homeGenres = remember(titles) {
        titles.flatMap { it.genres }.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.map { it.key }.take(14)
    }
    // "Karena Anda menonton X": X = judul terakhir ditonton; rekomendasi = genre sama,
    // judul yang dinilai tidak suka dikeluarkan.
    val becauseBase = history.filter { it.isStarted }
        .maxByOrNull { it.updatedAt }?.let { byId[it.titleId] }
    val becauseList = becauseBase?.let { base ->
        titles.filter { other ->
            other.id != base.id && ratings[other.id] != -1 &&
                other.genres.any { g -> base.genres.contains(g) }
        }.sortedByDescending { other ->
            other.genres.count { g -> base.genres.contains(g) } * 100 + (other.score ?: 0.0)
        }.take(10)
    } ?: emptyList()

    val listState = rememberLazyListState()
    val solidHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 260
        }
    }
    val headerAlpha by animateFloatAsState(
        targetValue = if (solidHeader) 0.97f else 0f,
        animationSpec = tween(220), label = "headerAlpha",
    )
    val playFirst: (Title) -> Unit = { t -> onPlay(t, t.episodes.firstOrNull()?.number ?: 1, 0L) }

    @Composable
    fun RowOf(name: String, list: List<Title>) = PosterRow(
        name, list, onOpen,
        newIds = newIds, myList = myList,
        onPlayFirst = playFirst,
        onToggleMyList = onToggleMyList,
        onDownloadFirst = onDownloadFirst,
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            // Hindari baris berisi judul yang sama persis (katalog kecil membuat
            // "Di Gudang", "Action", "Drama" tampak berulang).
            val shownSets = HashSet<Set<String>>()
            fun fresh(list: List<Title>): Boolean =
                list.size >= 3 && shownSets.add(list.map { it.id }.toSet())

            if (featured != null) {
                item {
                    Billboard(
                        t = featured,
                        history = history,
                        inMyList = myList.contains(featured.id),
                        genres = homeGenres,
                        onGenre = onGenre,
                        onOpen = { onOpen(featured) },
                        onPlay = { ep, pos -> onPlay(featured, ep, pos) },
                        onToggleMyList = { onToggleMyList(featured) },
                        modifier = Modifier.graphicsLayer {
                            // Parallax halus: billboard tertinggal + memudar saat digeser.
                            val off = if (listState.firstVisibleItemIndex == 0) {
                                listState.firstVisibleItemScrollOffset.toFloat()
                            } else 1200f
                            translationY = off * 0.3f
                            alpha = (1f - off / 1300f).coerceIn(0.25f, 1f)
                        },
                    )
                }
            }

            if (continueWatching.isNotEmpty()) {
                item {
                    SectionTitle("Lanjutkan Menonton")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(continueWatching) { entry ->
                            val t = byId.getValue(entry.titleId)
                            ContinueCard(
                                t = t, entry = entry,
                                onClick = { onPlay(t, entry.episode, entry.positionMs) },
                                onInfo = { onOpen(t) },
                                onRemove = { onRemoveContinue(entry) },
                            )
                        }
                    }
                }
            }

            if (top10.size >= 3) {
                item {
                    SectionTitle("10 Teratas di REA Hari Ini")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(top10.size) { idx ->
                            Top10Card(rank = idx + 1, t = top10[idx]) { onOpen(top10[idx]) }
                        }
                    }
                }
            }

            if (becauseBase != null && becauseList.size >= 3) {
                shownSets.add(becauseList.map { it.id }.toSet())
                item { RowOf("Karena Anda Menonton ${becauseBase.seriesName}", becauseList) }
            }

            if (titles.size >= 2) {
                val latest = titles.reversed().take(10)
                shownSets.add(latest.map { it.id }.toSet())
                item { RowOf("Baru Ditambahkan", latest) }
            }

            if (myListTitles.isNotEmpty()) {
                item { RowOf("Daftarku", myListTitles) }
            }

            val releasing = titles.filter { it.status == "RELEASING" }
            if (fresh(releasing)) item { RowOf("Sedang Tayang", releasing) }

            for (g in homeGenres.take(6)) {
                val inGenre = titles.filter { it.genres.contains(g) }
                    .sortedByDescending { it.score ?: 0.0 }
                if (fresh(inGenre)) item { RowOf(g, inGenre) }
            }

            val finished = titles.filter { it.status == "FINISHED" }
            if (finished.size != titles.size && fresh(finished)) {
                item { RowOf("Serial Tamat", finished) }
            }

            if (featured != null && featured.episodes.size > 1) {
                item {
                    SectionTitle("Episode " + featured.seriesName)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(featured.episodes) { ep ->
                            val entry = history.firstOrNull {
                                it.titleId == featured.id && it.episode == ep.number
                            }
                            EpisodeCard(featured, ep.number, entry) {
                                onPlay(
                                    featured, ep.number,
                                    if (entry != null && entry.isStarted && !entry.isFinished) entry.positionMs else 0L,
                                )
                            }
                        }
                    }
                }
            }

            if (titles.size > 10) {
                item { RowOf("Jelajahi Semua Judul", titles.sortedBy { it.title }) }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }

        // Bilah atas melayang di atas billboard: gradasi gelap di posisi awal,
        // menghitam pekat saat daftar digeser (gaya Netflix).
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().height(56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0B0B0B).copy(alpha = maxOf(headerAlpha, 0.72f)),
                            Color(0xFF0B0B0B).copy(alpha = headerAlpha),
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "REA", color = ReaAccent, fontSize = 28.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Cari", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onProfile) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Profil", tint = Color.White, modifier = Modifier.size(28.dp))
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
) {
    SectionTitle(title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    color = Color(0xFF9A9A9A),
                    drawStyle = Stroke(width = 7f, join = androidx.compose.ui.graphics.StrokeJoin.Round),
                ),
            )
            Text(text = rank.toString(), style = numStyle.copy(color = ReaBackground))
        }
        AsyncImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(104.dp).aspectRatio(2f / 3f)
                .offset(x = (-14).dp)
                .clip(RoundedCornerShape(4.dp)).background(ReaSurface),
        )
    }
}

@Composable
private fun GenrePill(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
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
        modifier.fillMaxWidth().aspectRatio(0.82f).background(ReaSurface)
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
                    0.78f to ReaBackground.copy(alpha = 0.88f),
                    1f to ReaBackground,
                )
            )
        )
        if (genres.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp),
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
                t.seriesName, color = Color.White, fontSize = 28.sp,
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
                    Text("$it% Cocok", color = ReaGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                t.year?.let { Text(it.toString(), color = Color(0xFFD9D9D9), fontSize = 13.sp) }
                t.statusLabel?.let {
                    Text(
                        it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    "HD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
                if (t.episodes.isNotEmpty()) {
                    Text("${t.episodes.size} Eps", color = Color(0xFFD9D9D9), fontSize = 13.sp)
                }
            }
            if (t.genres.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    t.genres.take(3).joinToString("  •  "),
                    color = Color(0xFFE5E5E5), fontSize = 12.sp,
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
                        containerColor = Color.White, contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (resume != null) "Lanjutkan" else "Putar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Button(
                    onClick = onToggleMyList,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A3A3A).copy(alpha = 0.85f),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
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
        text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
    )
}

/** Judul kartu dua baris tetap: nama seri (tebal) + season/bagian (redup). */
@Composable
fun CardTitles(t: Title) {
    Column(Modifier.height(36.dp).padding(top = 4.dp)) {
        Text(
            t.seriesName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            t.titleSub ?: listOfNotNull(
                t.year?.toString(),
                if (t.episodes.isNotEmpty()) "${t.episodes.size} Episode" else null,
            ).joinToString(" · "),
            color = ReaTextDim, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    t: Title,
    badge: String? = null,
    inMyList: Boolean = false,
    onPlayFirst: (() -> Unit)? = null,
    onToggleMyList: (() -> Unit)? = null,
    onDownloadFirst: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasMenu = onPlayFirst != null || onToggleMyList != null || onDownloadFirst != null
    val pressSource = remember { MutableInteractionSource() }
    Column(Modifier.width(116.dp)) {
        Box {
            AsyncImage(
                model = t.poster ?: t.banner,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(4.dp)).background(ReaSurface)
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
                    badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopStart)
                        .background(ReaAccent, RoundedCornerShape(bottomEnd = 6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
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
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
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
                Text("E$episode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (entry != null && entry.isStarted && !entry.isFinished) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .height(4.dp).background(Color.White.copy(alpha = 0.35f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(entry.fraction).height(4.dp)
                            .background(ReaAccent),
                    )
                }
            }
        }
        Text(
            "Episode $episode", color = Color.White, fontSize = 12.sp,
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
            color = ReaTextDim, fontSize = 11.sp,
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
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
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
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface)
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
                        .height(4.dp).background(ReaAccent),
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, color = Color.White, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "E${entry.episode} · Sisa " + formatRemainingPlain(entry.positionMs, entry.durationMs),
                    color = ReaTextDim, fontSize = 11.sp,
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
