package com.app.rea

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.rea.data.CatalogApi
import com.app.rea.data.DownloadManager
import com.app.rea.data.HistoryStore
import com.app.rea.data.LibraryStore
import com.app.rea.data.ReaSettings
import com.app.rea.data.Title
import com.app.rea.player.PlayerActivity
import com.app.rea.ui.DetailScreen
import com.app.rea.ui.DownloadsScreen
import com.app.rea.ui.HomeScreen
import com.app.rea.ui.NewScreen
import com.app.rea.ui.ProfileScreen
import com.app.rea.ui.ReaAccent
import com.app.rea.ui.ReaBackground
import com.app.rea.ui.ReaSurface
import com.app.rea.ui.ReaTheme
import com.app.rea.ui.SearchScreen
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val store = HistoryStore(applicationContext)
        val library = LibraryStore(applicationContext)
        val downloader = DownloadManager(applicationContext, library)
        setContent {
            ReaTheme {
                AppRoot(store, library, downloader) { title, episode, startPos, autoplay, subtitle ->
                    val eps = JSONArray()
                    for (e in title.episodes) {
                        val uri = downloader.localPlaylistUri(title.id, e.number) ?: e.playlist
                        eps.put(JSONObject().put("n", e.number).put("p", uri))
                    }
                    startActivity(
                        Intent(this, PlayerActivity::class.java).apply {
                            putExtra(PlayerActivity.EXTRA_TITLE_ID, title.id)
                            putExtra(PlayerActivity.EXTRA_TITLE, title.title)
                            putExtra(PlayerActivity.EXTRA_EPISODE, episode)
                            putExtra(PlayerActivity.EXTRA_EPISODES, eps.toString())
                            putExtra(PlayerActivity.EXTRA_START_POS, startPos)
                            putExtra(PlayerActivity.EXTRA_AUTOPLAY, autoplay)
                            putExtra(PlayerActivity.EXTRA_SUBTITLE, subtitle)
                        }
                    )
                }
            }
        }
    }
}

private enum class Tab { Home, Search, New, Downloads, Profile }

@Composable
private fun AppRoot(
    store: HistoryStore,
    library: LibraryStore,
    downloader: DownloadManager,
    onPlay: (Title, Int, Long, Boolean, String) -> Unit,
) {
    var titles by remember { mutableStateOf<List<Title>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Title?>(null) }
    var tab by remember { mutableStateOf(Tab.Home) }
    val scope = rememberCoroutineScope()

    val history by store.entries.collectAsState(initial = emptyList())
    val myList by library.myList.collectAsState(initial = emptySet())
    val ratings by library.ratings.collectAsState(initial = emptyMap())
    val hidden by library.hiddenContinue.collectAsState(initial = emptySet())
    val settings by library.settings.collectAsState(initial = ReaSettings())
    val downloads by library.downloads.collectAsState(initial = emptyList())

    LaunchedEffect(reloadKey) {
        if (titles.isNullOrEmpty()) {
            val cached = try { library.readCatalogCache() } catch (e: Exception) { emptyList() }
            if (cached.isNotEmpty()) titles = cached
        }
        try {
            val raw = CatalogApi.fetchRawCatalog()
            val fresh = CatalogApi.parseCatalog(raw)
            titles = fresh
            error = null
            try { library.saveCatalogCache(raw) } catch (e: Exception) { }
        } catch (e: Exception) {
            if (titles.isNullOrEmpty()) {
                error = "Tidak bisa memuat katalog. Periksa koneksi internetmu."
            }
        }
    }

    fun play(t: Title, ep: Int, pos: Long) =
        onPlay(t, ep, pos, settings.autoplayNext, settings.subtitleDefault)

    BackHandler(enabled = selected != null) { selected = null }

    Scaffold(
        containerColor = ReaBackground,
        bottomBar = {
            if (selected == null) {
                NavigationBar(containerColor = Color(0xFF0B0B0B)) {
                    NavItem(Tab.Home, tab, Icons.Filled.Home, "Beranda") { tab = it }
                    NavItem(Tab.Search, tab, Icons.Filled.Search, "Cari") { tab = it }
                    NavItem(Tab.New, tab, Icons.Filled.Whatshot, "Baru") { tab = it }
                    NavItem(Tab.Downloads, tab, Icons.Filled.Download, "Unduhan") { tab = it }
                    NavItem(Tab.Profile, tab, Icons.Filled.Person, "Profil") { tab = it }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(ReaBackground)) {
            when {
                selected != null -> {
                    val t = titles?.firstOrNull { it.id == selected!!.id } ?: selected!!
                    val similar = (titles ?: emptyList())
                        .filter { it.id != t.id }
                        .sortedByDescending { other ->
                            other.genres.count { g -> t.genres.contains(g) } * 100 +
                                (other.score ?: 0.0)
                        }
                        .take(10)
                    DetailScreen(
                        title = t,
                        history = history,
                        inMyList = myList.contains(t.id),
                        rating = ratings[t.id] ?: 0,
                        downloads = downloads,
                        similar = similar,
                        onBack = { selected = null },
                        onPlay = { ep, pos -> play(t, ep, pos) },
                        onToggleMyList = { scope.launch { library.toggleMyList(t.id) } },
                        onRate = { v -> scope.launch { library.setRating(t.id, v) } },
                        onDownload = { ep -> downloader.start(t, ep) },
                        onDeleteDownload = { item -> downloader.delete(item) },
                        onOpenTitle = { other -> selected = other },
                    )
                }
                error != null && titles == null -> Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error!!, color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { error = null; reloadKey++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReaAccent, contentColor = Color.White,
                        ),
                    ) { Text("Coba Lagi") }
                }
                titles == null -> SkeletonHome()
                else -> when (tab) {
                    Tab.Home -> HomeScreen(
                        titles = titles!!,
                        history = history,
                        myList = myList,
                        ratings = ratings,
                        hiddenContinue = hidden,
                        onOpen = { selected = it },
                        onPlay = { t, ep, pos -> play(t, ep, pos) },
                        onToggleMyList = { t -> scope.launch { library.toggleMyList(t.id) } },
                        onDownloadFirst = { t ->
                            t.episodes.firstOrNull()?.let { ep -> downloader.start(t, ep) }
                        },
                        onRemoveContinue = { entry ->
                            scope.launch {
                                library.hideContinue(entry.titleId, entry.episode)
                                store.remove(entry.titleId, entry.episode)
                            }
                        },
                        onSearch = { tab = Tab.Search },
                        onProfile = { tab = Tab.Profile },
                    )
                    Tab.Search -> SearchScreen(
                        titles = titles!!,
                        onOpen = { selected = it },
                    )
                    Tab.New -> NewScreen(
                        titles = titles!!,
                        onOpen = { selected = it },
                    )
                    Tab.Downloads -> DownloadsScreen(
                        downloads = downloads,
                        titles = titles!!,
                        onPlayDownload = { item ->
                            titles!!.firstOrNull { it.id == item.titleId }?.let { t ->
                                val entry = history.firstOrNull {
                                    it.titleId == item.titleId && it.episode == item.episode &&
                                        it.isStarted && !it.isFinished
                                }
                                play(t, item.episode, entry?.positionMs ?: 0L)
                            }
                        },
                        onDeleteDownload = { item -> downloader.delete(item) },
                        onBrowse = { tab = Tab.Home },
                    )
                    Tab.Profile -> ProfileScreen(
                        myListTitles = titles!!.filter { myList.contains(it.id) },
                        historyCount = history.size,
                        finishedEpisodes = history.count { it.isFinished },
                        watchedTitles = history.map { it.titleId }.distinct().size,
                        settings = settings,
                        onOpenTitle = { selected = it },
                        onAutoplayChange = { v -> scope.launch { library.setAutoplay(v) } },
                        onSubtitleChange = { v -> scope.launch { library.setSubtitleDefault(v) } },
                        onSubtitleStyleChange = { size, color ->
                            scope.launch { library.setSubtitleStyle(size, color) }
                        },
                        onClearHistory = { scope.launch { store.clearAll() } },
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    item: Tab,
    current: Tab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onSelect: (Tab) -> Unit,
) {
    NavigationBarItem(
        selected = current == item,
        onClick = { onSelect(item) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            unselectedIconColor = Color(0xFF8C8C8C),
            unselectedTextColor = Color(0xFF8C8C8C),
            indicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SkeletonHome() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(28.dp).width(90.dp)
                .background(ReaSurface, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
        )
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f)
                .background(ReaSurface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        )
        Box(
            Modifier.fillMaxWidth(0.45f).height(18.dp)
                .background(ReaSurface, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
        )
        Box(
            Modifier.fillMaxWidth().height(110.dp)
                .background(ReaSurface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        )
        Box(
            Modifier.fillMaxWidth(0.35f).height(18.dp)
                .background(ReaSurface, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
        )
        Box(
            Modifier.fillMaxWidth().height(170.dp)
                .background(ReaSurface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        )
    }
}
