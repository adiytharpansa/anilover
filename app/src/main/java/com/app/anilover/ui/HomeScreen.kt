package com.app.anilover.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.anilover.R
import com.app.anilover.data.Title
import com.app.anilover.data.WatchEntry

/**
 * Beranda: hero berputar (pager) di belakang header transparan, Lanjutkan Menonton,
 * Top 10, Baru Ditambahkan, genre, baris pilihan per genre, lalu sisa koleksi.
 * Setiap judul hanya muncul di satu "blok utama" supaya halaman tidak terasa berulang.
 *
 * Komponen dipecah ke berkas terpisah:
 *  - HeroBillboard.kt : HeroPager, Billboard, TrailerPlayer
 *  - HomeSections.kt  : SectionTitle, PosterRow, LandscapeRow, Top10Card, GenreChip, ContinueCard, EpisodeCard
 *  - HomeCards.kt     : PosterCard, LandscapeCard, CardTitles
 */

/** Jumlah judul terakhir di katalog yang diberi lencana BARU (katalog belum punya tanggal rilis). */
private const val NEW_BADGE_COUNT = 3

/** Genre baku yang layak jadi chip; tag lain dari data (mis. "Earth", "Japan") disaring. */
internal val KnownGenres = setOf(
    "action", "adventure", "comedy", "drama", "fantasy", "horror", "mystery", "romance",
    "sci-fi", "slice of life", "sports", "supernatural", "thriller", "mecha", "music",
    "psychological", "ecchi", "shounen", "seinen", "shoujo", "josei", "isekai", "school",
    "historical", "military", "magic", "samurai", "martial arts", "game", "harem", "demons",
)

/** Ambil genre baku saja; bila hasil kosong (data tak lazim), pakai semua supaya baris tidak hilang. */
internal fun cleanGenres(all: List<String>): List<String> =
    all.filter { it.trim().lowercase() in KnownGenres }.ifEmpty { all }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
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
    val newIds = if (titles.size > NEW_BADGE_COUNT) {
        titles.takeLast(NEW_BADGE_COUNT).map { it.id }.toSet()
    } else emptySet()
    val playFirst: (Title) -> Unit = { t -> onPlay(t, t.episodes.firstOrNull()?.number ?: 1, 0L) }

    // Hero: judul berskor tertinggi yang BUKAN sedang ditonton (itu sudah ada di "Lanjutkan").
    val heroes = titles.reversed()
        .filter { !resume.containsKey(it.id) }
        .sortedByDescending { it.score ?: 0.0 }
        .take(5)
        .ifEmpty { titles.take(1) }
    val genreList = cleanGenres(
        titles.flatMap { it.genres }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key },
    ).take(10)
    val fresh = titles.reversed().take(10)
    val freshIds = fresh.map { it.id }.toSet()
    val top10 = titles.filter { (it.score ?: 0.0) > 0.0 }.sortedByDescending { it.score }.take(10)
    // Judul yang sudah tampil di Top 10 tidak diulang di baris genre (bila masih cukup judul lain).
    val top10Ids = if (top10.size >= 5) top10.map { it.id }.toSet() else emptySet()
    val genreRows = genreList.take(4)
        .map { g ->
            val all = titles.filter { it.genres.contains(g) }.sortedByDescending { it.score ?: 0.0 }
            val unique = all.filter { it.id !in top10Ids }
            g to (if (unique.size >= 3) unique else all)
        }
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
        val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                // Turun di bawah header melayang supaya indikator tidak tertutup.
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp),
                    containerColor = AniloverSurface,
                    color = AniloverAccent,
                )
            },
        ) {
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
                    LandscapeRow(
                        "Baru Ditambahkan", fresh, onOpen, newIds = newIds, myList = myList,
                        onToggleMyList = onToggleMyList, onSeeAll = onSeeNew,
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
