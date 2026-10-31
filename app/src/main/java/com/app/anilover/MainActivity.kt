package com.app.anilover

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.anilover.data.AuthRepository
import com.app.anilover.data.CatalogApi
import com.app.anilover.data.CatalogCheckWorker
import com.app.anilover.data.DownloadManager
import com.app.anilover.data.HistoryStore
import com.app.anilover.data.LibraryStore
import com.app.anilover.data.AniloverSettings
import com.app.anilover.data.SoonTitle
import com.app.anilover.data.Title
import com.app.anilover.player.PlayerActivity
import com.app.anilover.ui.CategoryScreen
import com.app.anilover.ui.DetailScreen
import com.app.anilover.ui.DownloadsScreen
import com.app.anilover.ui.HomeScreen
import com.app.anilover.ui.NewScreen
import com.app.anilover.ui.ProfileScreen
import com.app.anilover.ui.AniloverAccent
import com.app.anilover.ui.AuthGate
import com.app.anilover.ui.AniloverOnAccent
import com.app.anilover.ui.AniloverBackground
import com.app.anilover.ui.AniloverSurface
import com.app.anilover.ui.AniloverTheme
import com.app.anilover.ui.AniloverShapes
import com.app.anilover.ui.AniloverTextDim
import com.app.anilover.ui.AniloverOutline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import com.app.anilover.ui.ShimmerBox
import com.app.anilover.ui.SearchScreen
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        com.app.anilover.data.CrashReporter.install(applicationContext)
        val splashScreen = installSplashScreen()
        // Animasi keluar splash: logo membesar memudar (rasa pembuka Netflix).
        // iconView BISA null di sebagian perangkat/kondisi luncur (NPE terbukti
        // dari laporan galat 2.6.3) — pakai panggilan aman + jaring pengaman,
        // dan pastikan provider.remove() selalu terpanggil.
        splashScreen.setOnExitAnimationListener { provider ->
            try {
                provider.iconView?.animate()
                    ?.scaleX(1.3f)?.scaleY(1.3f)?.alpha(0f)
                    ?.setDuration(380)?.start()
                provider.view.animate()
                    .alpha(0f).setDuration(420)
                    .withEndAction {
                        try { provider.remove() } catch (e: Exception) { }
                    }.start()
            } catch (e: Exception) {
                try { provider.remove() } catch (e2: Exception) { }
            }
        }
        // Tepi-ke-tepi: hero & latar menembus status bar / bilah navigasi (gaya Netflix).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Izin notifikasi (Android 13+) untuk kabar judul baru & "Sudah tayang".
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        // Pemeriksa katalog berkala: notifikasi judul baru / yang diingatkan sudah tayang.
        try {
            val checkRequest = PeriodicWorkRequestBuilder<CatalogCheckWorker>(
                6, java.util.concurrent.TimeUnit.HOURS,
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "anilover-catalog-check", ExistingPeriodicWorkPolicy.KEEP, checkRequest,
            )
        } catch (e: Exception) { /* notifikasi latar bersifat tambahan */ }
        AuthRepository.init(applicationContext)
        val store = HistoryStore(applicationContext)
        val library = LibraryStore(applicationContext)
        val downloader = DownloadManager(applicationContext, library)
        setContent {
            AniloverTheme {
              AuthGate {
                AppRoot(store, library, downloader) { title, episode, startPos, autoplay, subtitle ->
                    val eps = JSONArray()
                    for (e in title.episodes) {
                        val uri = downloader.localPlaylistUri(title.id, e.number) ?: e.playlist
                        eps.put(JSONObject().put("n", e.number).put("p", uri).put("i", e.introEnd ?: 0L))
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
                            putExtra(
                                PlayerActivity.EXTRA_META,
                                listOfNotNull(
                                    title.score?.let { "★ " + String.format(java.util.Locale.US, "%.1f", it) },
                                    title.year?.toString(),
                                    title.genres.take(3).joinToString(", ").ifBlank { null },
                                ).joinToString(" · "),
                            )
                        }
                    )
                }
              }
            }
        }
    }
}

private enum class Tab { Home, Search, New, Downloads, Profile }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppRoot(
    store: HistoryStore,
    library: LibraryStore,
    downloader: DownloadManager,
    onPlay: (Title, Int, Long, Boolean, String) -> Unit,
) {
    var titles by remember { mutableStateOf<List<Title>?>(null) }
    var soon by remember { mutableStateOf<List<SoonTitle>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Title?>(null) }
    var genreSel by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(Tab.Home) }
    val scope = rememberCoroutineScope()

    val history by store.entries.collectAsState(initial = emptyList())
    val myList by library.myList.collectAsState(initial = emptySet())
    val ratings by library.ratings.collectAsState(initial = emptyMap())
    val hidden by library.hiddenContinue.collectAsState(initial = emptySet())
    val settings by library.settings.collectAsState(initial = AniloverSettings())
    val downloads by library.downloads.collectAsState(initial = emptyList())
    val reminders by library.reminders.collectAsState(initial = emptySet())
    val recents by library.recentSearches.collectAsState(initial = emptyList())
    val account by AuthRepository.user.collectAsState()
    val context = LocalContext.current

    // Laporan galat terakhir (dari CrashReporter): tampilkan agar bisa dibagikan
    // untuk diagnosis, lalu hapus.
    var crashText by remember { mutableStateOf(com.app.anilover.data.CrashReporter.read(context)) }
    if (crashText != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Aplikasi sempat berhenti") },
            text = {
                Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                    Text(crashText!!, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, crashText)
                    }
                    context.startActivity(Intent.createChooser(send, "Bagikan laporan galat"))
                }) { Text("Bagikan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    com.app.anilover.data.CrashReporter.clear(context)
                    crashText = null
                }) { Text("Tutup") }
            },
        )
    }

    var refreshing by remember { mutableStateOf(false) }
    var lastLoad by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // Ambil katalog terbaru. Mengembalikan true bila berhasil. Bila gagal, data lama
    // tetap tampil (galat layar penuh hanya muncul saat belum ada data sama sekali).
    suspend fun loadCatalog(): Boolean {
        var ok = false
        try {
            val raw = CatalogApi.fetchRawCatalog()
            val fresh = CatalogApi.parseCatalog(raw)
            titles = fresh
            error = null
            ok = true
            lastLoad = System.currentTimeMillis()
            try { library.saveCatalogCache(raw) } catch (e: Exception) { }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (titles.isNullOrEmpty()) {
                error = "Tidak bisa memuat katalog. Periksa koneksi internetmu."
            }
        }
        try { soon = CatalogApi.fetchSoon() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { /* Segera Tayang tambahan */ }
        return ok
    }

    LaunchedEffect(reloadKey) {
        if (titles.isNullOrEmpty()) {
            val cached = try { library.readCatalogCache() } catch (e: Exception) { emptyList() }
            if (cached.isNotEmpty()) titles = cached
        }
        loadCatalog()
    }

    // Tarik-untuk-segarkan di Beranda.
    val refreshHome: () -> Unit = {
        if (!refreshing) {
            scope.launch {
                refreshing = true
                val ok = try { loadCatalog() } finally { refreshing = false }
                if (!ok) {
                    android.widget.Toast.makeText(
                        context, "Gagal menyegarkan. Periksa koneksi internetmu.",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    // Muat otomatis (diam-diam, tanpa layar loading): saat aplikasi dibuka kembali
    // bila data sudah lebih dari 3 menit, dan berkala tiap 5 menit selama aplikasi terlihat.
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                lastLoad != 0L && System.currentTimeMillis() - lastLoad > 3 * 60_000L && !refreshing
            ) {
                scope.launch { loadCatalog() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5 * 60_000L)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && !refreshing) {
                loadCatalog()
            }
        }
    }

    // Unduhan Cerdas: sesudah episode selesai ditonton, unduh lanjutannya dan
    // hapus unduhan episode yang sudah selesai. Berhenti di data seluler bila
    // pengaturan "Hanya Wi-Fi" aktif.
    LaunchedEffect(history, downloads, settings.smartDownloads, settings.wifiOnlyDownloads, titles) {
        if (!settings.smartDownloads) return@LaunchedEffect
        val list = titles ?: return@LaunchedEffect
        if (settings.wifiOnlyDownloads) {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            if (!onWifi) return@LaunchedEffect
        }
        val byId = list.associateBy { it.id }
        for (d in downloads.filter { it.state == "selesai" }) {
            val h = history.firstOrNull { it.titleId == d.titleId && it.episode == d.episode }
            if (h != null && h.isFinished) downloader.delete(d)
        }
        for (h in history.filter { it.isFinished }) {
            val t = byId[h.titleId] ?: continue
            val next = t.episodes.firstOrNull { it.number == h.episode + 1 } ?: continue
            val exists = downloads.any { it.titleId == t.id && it.episode == next.number }
            if (!exists && downloader.localPlaylistUri(t.id, next.number) == null) {
                downloader.start(t, next)
            }
        }
    }

    fun play(t: Title, ep: Int, pos: Long) =
        onPlay(t, ep, pos, settings.autoplayNext, settings.subtitleDefault)

    BackHandler(enabled = selected != null) { selected = null }
    BackHandler(enabled = selected == null && genreSel != null) { genreSel = null }

    // Status bar ditangani tiap layar (Beranda justru menembusnya); sisi bawah/samping ditangani Scaffold.
    Scaffold(
        containerColor = AniloverBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            if (selected == null && genreSel == null) {
                // Garis tipis hanya di sisi atas (bukan kotak penuh).
                NavigationBar(
                    containerColor = AniloverSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = AniloverOutline.copy(alpha = 0.7f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
                ) {
                    NavItem(Tab.Home, tab, Icons.Filled.Home, Icons.Outlined.Home, "Beranda") { tab = it }
                    NavItem(Tab.Search, tab, Icons.Filled.Search, Icons.Outlined.Search, "Cari") { tab = it }
                    NavItem(Tab.New, tab, Icons.Filled.NewReleases, Icons.Outlined.NewReleases, "Baru") { tab = it }
                    NavItem(Tab.Downloads, tab, Icons.Filled.Download, Icons.Outlined.Download, "Unduhan") { tab = it }
                    NavItem(Tab.Profile, tab, Icons.Filled.Person, Icons.Outlined.Person, "Profil") { tab = it }
                }
            }
        },
    ) { padding ->
        // Layar penuh (detail, genre, error, skeleton) memberi ruang status bar sendiri;
        // konten tab menanganinya di dalam AnimatedContent agar tidak melompat saat berpindah tab.
        val tabContent = selected == null && genreSel == null && titles != null
        Box(
            Modifier.fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .then(if (tabContent) Modifier else Modifier.statusBarsPadding())
                .background(AniloverBackground),
        ) {
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
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(t.id) { shown = true }
                    AnimatedVisibility(
                        visible = shown,
                        enter = fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 8 } +
                            androidx.compose.animation.scaleIn(tween(300), initialScale = 0.96f),
                        exit = fadeOut(tween(150)),
                    ) {
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
                }
                genreSel != null -> CategoryScreen(
                    genre = genreSel!!,
                    titles = (titles ?: emptyList()).filter { it.genres.contains(genreSel) },
                    onOpen = { selected = it },
                    onBack = { genreSel = null },
                )
                error != null && titles == null -> Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error!!, color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { error = null; reloadKey++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                        ),
                    ) { Text("Coba Lagi") }
                }
                titles == null -> SkeletonHome()
                else -> AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (fadeIn(tween(240, delayMillis = 60)) +
                            slideInHorizontally(tween(260)) { dir * it / 14 }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { -dir * it / 20 })
                    },
                    label = "tab",
                ) { currentTab ->
                    Box(if (currentTab == Tab.Home) Modifier else Modifier.statusBarsPadding()) {
                    when (currentTab) {
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
                        onSeeNew = { tab = Tab.New },
                        onGenre = { genreSel = it },
                        isRefreshing = refreshing,
                        onRefresh = refreshHome,
                    )
                    Tab.Search -> SearchScreen(
                        titles = titles!!,
                        onOpen = { selected = it },
                        recents = recents,
                        onSaveRecent = { q -> scope.launch { library.addRecentSearch(q) } },
                        onClearRecents = { scope.launch { library.clearRecentSearches() } },
                    )
                    Tab.New -> NewScreen(
                        titles = titles!!,
                        soon = soon,
                        reminders = reminders,
                        onToggleReminder = { s -> scope.launch { library.toggleReminder(s.id) } },
                        onOpen = { selected = it },
                    )
                    Tab.Downloads -> DownloadsScreen(
                        downloads = downloads,
                        titles = titles!!,
                        settings = settings,
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
                        onSmartChange = { v -> scope.launch { library.setSmartDownloads(v) } },
                        onWifiOnlyChange = { v -> scope.launch { library.setWifiOnlyDownloads(v) } },
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
                        accountName = account?.name,
                        accountEmail = account?.email,
                        onLogout = if (AuthRepository.isConfigured) ({ AuthRepository.signOut() }) else null,
                    )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    item: Tab,
    current: Tab,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onSelect: (Tab) -> Unit,
) {
    val selected = current == item
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(item) },
        // Ikon terisi untuk tab aktif, ikon garis untuk yang lain.
        icon = { Icon(if (selected) selectedIcon else unselectedIcon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AniloverAccent,
            selectedTextColor = AniloverAccent,
            unselectedIconColor = AniloverTextDim,
            unselectedTextColor = AniloverTextDim,
            indicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SkeletonHome() {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(Modifier.height(24.dp).width(110.dp))
            Spacer(Modifier.weight(1f))
            ShimmerBox(Modifier.size(42.dp), androidx.compose.foundation.shape.CircleShape)
        }
        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(0.9f), androidx.compose.ui.graphics.RectangleShape)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    ShimmerBox(Modifier.weight(1f).aspectRatio(2f / 3f), AniloverShapes.Card)
                }
            }
        }
    }
}
