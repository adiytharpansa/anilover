package com.app.anilover.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.app.anilover.R
import com.app.anilover.data.HistoryStore
import com.app.anilover.data.LibraryStore
import com.app.anilover.data.AniloverSettings
import com.app.anilover.data.WatchEntry
import com.app.anilover.data.CatalogApi
import com.app.anilover.ui.AniloverAccent
import com.app.anilover.ui.AniloverOnAccent
import com.app.anilover.ui.AniloverTheme
import com.app.anilover.ui.AniloverOutline
import com.app.anilover.ui.AniloverShapes
import com.app.anilover.ui.AniloverTextDim
import com.app.anilover.ui.AniloverSurfaceHigh
import com.app.anilover.ui.AniloverSurface
import com.app.anilover.ui.formatClock
import com.app.anilover.ui.formatRemaining
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

data class EpRef(val number: Int, val playlist: String, val introEndSec: Long = 0L)

class PlayerActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TITLE_ID = "title_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_EPISODES = "episodes"
        const val EXTRA_START_POS = "start_pos"
        const val EXTRA_AUTOPLAY = "autoplay_next"
        const val EXTRA_SUBTITLE = "subtitle_default"
    }

    private var playerRef: ExoPlayer? = null
    private var titleId: String = ""
    private var currentEp: Int = 1
    private lateinit var store: HistoryStore
    private var inPip by mutableStateOf(false)
    private var bgService = false

    override fun onCreate(savedInstanceState: Bundle?) {
        com.app.anilover.data.CrashReporter.install(applicationContext)
        super.onCreate(savedInstanceState)
        store = HistoryStore(applicationContext)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        titleId = intent.getStringExtra(EXTRA_TITLE_ID) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        currentEp = intent.getIntExtra(EXTRA_EPISODE, 1)
        val startPos = intent.getLongExtra(EXTRA_START_POS, 0L)
        val autoplayNext = intent.getBooleanExtra(EXTRA_AUTOPLAY, true)
        val subtitleDefault = intent.getStringExtra(EXTRA_SUBTITLE) ?: "id"
        val episodes = parseEpisodes(intent.getStringExtra(EXTRA_EPISODES) ?: "[]")
        PlaybackHolder.title = title
        PlaybackHolder.episode = currentEp

        setContent {
            AniloverTheme {
                PlayerScreen(
                    titleId = titleId,
                    title = title,
                    episodes = episodes,
                    initialEpisode = currentEp,
                    startPos = startPos,
                    autoplayNext = autoplayNext,
                    subtitleDefault = subtitleDefault,
                    store = store,
                    inPip = inPip,
                    onBack = { finish() },
                    onEnterPip = { enterPip() },
                    onPlayerCreated = { playerRef = it; PlaybackHolder.player = it; startBackgroundService() },
                    onEpisodeChanged = { currentEp = it; PlaybackHolder.episode = it },
                )
            }
        }
    }

    private fun parseEpisodes(s: String): List<EpRef> = try {
        val arr = JSONArray(s)
        val out = ArrayList<EpRef>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(EpRef(o.optInt("n"), o.optString("p"), o.optLong("i")))
        }
        out.sortedBy { it.number }
    } catch (e: Exception) {
        emptyList()
    }

    /** Suara tetap jalan saat layar mati / pindah aplikasi, dengan notifikasi media. */
    private fun startBackgroundService() {
        bgService = try {
            ContextCompat.startForegroundService(this, Intent(this, PlaybackService::class.java))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun enterPip() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val p = playerRef
        if (p != null && p.isPlaying) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        inPip = isInPictureInPictureMode
    }

    override fun onPause() {
        super.onPause()
        val p = playerRef ?: return
        if (!isChangingConfigurations) saveProgress(p)
    }

    override fun onStop() {
        super.onStop()
        // Layar mati / pindah aplikasi: suara lanjut lewat PlaybackService.
        // Jeda hanya bila halaman ditutup atau layanan latar gagal dimulai.
        if (isFinishing || !bgService) playerRef?.pause()
    }

    override fun onDestroy() {
        stopService(Intent(this, PlaybackService::class.java))
        PlaybackHolder.player = null
        playerRef?.release()
        playerRef = null
        super.onDestroy()
    }

    private fun saveProgress(p: ExoPlayer) {
        val dur = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        if (dur <= 0) return
        lifecycleScope.launch {
            store.save(
                WatchEntry(titleId, currentEp, p.currentPosition, dur, System.currentTimeMillis())
            )
        }
    }
}

private enum class Panel { None, Speed, Tracks, Episodes, Quality }

/**
 * Pemutar dengan jaringan yang lebih tahan banting untuk HLS dari jsDelivr:
 * User-Agent jelas, timeout longgar, redirect lintas-protokol, percobaan ulang
 * segmen 6x, buffer lebih besar, fokus audio & jeda saat headset dicabut.
 */
private fun buildPlayer(context: Context): ExoPlayer {
    val http = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android) Anilover/2.7")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)
        .setAllowCrossProtocolRedirects(true)
    val sources = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http))
        .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))
    val load = DefaultLoadControl.Builder()
        .setBufferDurationsMs(25_000, 60_000, 2_000, 4_000)
        .build()
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(sources)
        .setLoadControl(load)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
}

private fun friendlyError(e: PlaybackException): String = when (e.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Koneksi terputus atau terlalu lambat. Periksa internet lalu coba lagi."
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "Berkas video tidak ditemukan di server."
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "Server video menolak permintaan. Coba lagi beberapa saat."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_DECODING_FAILED ->
        "Format video tidak bisa diputar di perangkat ini."
    else -> "Gagal memutar. Periksa koneksi lalu coba lagi."
}

@Composable
private fun PlayerScreen(
    titleId: String,
    title: String,
    episodes: List<EpRef>,
    initialEpisode: Int,
    startPos: Long,
    autoplayNext: Boolean,
    subtitleDefault: String,
    store: HistoryStore,
    inPip: Boolean,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onPlayerCreated: (ExoPlayer) -> Unit,
    onEpisodeChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentEp by remember { mutableIntStateOf(initialEpisode) }
    val currentPlaylist = episodes.firstOrNull { it.number == currentEp }?.playlist ?: ""

    val player = remember {
        buildPlayer(context).also { onPlayerCreated(it) }
    }
    val scope = rememberCoroutineScope()
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val library = remember { LibraryStore(context) }
    val aniloverSettings by library.settings.collectAsState(initial = AniloverSettings())

    // ---- Cast ke TV (Google Cast, Default Media Receiver) ----
    val castContext = remember {
        try { CastContext.getSharedInstance(context) } catch (e: Throwable) { null }
    }
    var casting by remember { mutableStateOf(false) }
    val latestPlaylist by rememberUpdatedState(currentPlaylist)
    val latestEp by rememberUpdatedState(currentEp)
    val latestTitle by rememberUpdatedState(title)

    fun loadToCast(session: CastSession) {
        val url = latestPlaylist
        if (!url.startsWith("http")) {
            Toast.makeText(
                context,
                "Cast hanya untuk streaming — episode unduhan tidak bisa dikirim ke TV",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        val remote = session.remoteMediaClient ?: return
        val meta = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        meta.putString(MediaMetadata.KEY_TITLE, "$latestTitle — Episode $latestEp")
        val info = MediaInfo.Builder(url)
            .setContentType("application/x-mpegURL")
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(meta)
            .build()
        remote.load(info, true, player.currentPosition)
        player.playWhenReady = false
        casting = true
        Toast.makeText(context, "Memutar di TV", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(castContext) {
        val sm = castContext?.sessionManager
        if (sm == null) return@DisposableEffect onDispose { }
        val listener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) = loadToCast(session)
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                casting = true
                player.playWhenReady = false
            }
            override fun onSessionEnded(session: CastSession, error: Int) {
                val pos = try {
                    session.remoteMediaClient?.approximateStreamPosition ?: -1L
                } catch (e: Exception) { -1L }
                casting = false
                if (pos > 0) player.seekTo(pos)
                player.playWhenReady = true
            }
            override fun onSessionStarting(session: CastSession) { }
            override fun onSessionStartFailed(session: CastSession, error: Int) { }
            override fun onSessionEnding(session: CastSession) { }
            override fun onSessionResuming(session: CastSession, sessionId: String) { }
            override fun onSessionResumeFailed(session: CastSession, error: Int) { }
            override fun onSessionSuspended(session: CastSession, reason: Int) { }
        }
        sm.addSessionManagerListener(listener, CastSession::class.java)
        if (sm.currentCastSession != null) casting = true
        onDispose { sm.removeSessionManagerListener(listener, CastSession::class.java) }
    }

    // Terapkan gaya subtitle pilihan pengguna (warna + ukuran) ke tampilan subtitle.
    LaunchedEffect(playerViewRef, aniloverSettings.subtitleSize, aniloverSettings.subtitleColor) {
        val sv = playerViewRef?.subtitleView ?: return@LaunchedEffect
        val fg = when (aniloverSettings.subtitleColor) {
            "kuning" -> android.graphics.Color.YELLOW
            "hijau" -> android.graphics.Color.GREEN
            else -> android.graphics.Color.WHITE
        }
        sv.setStyle(
            CaptionStyleCompat(
                fg,
                android.graphics.Color.argb(150, 0, 0, 0),
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                android.graphics.Color.BLACK,
                null,
            )
        )
        sv.setFractionalTextSize(
            when (aniloverSettings.subtitleSize) {
                "kecil" -> 0.040f
                "besar" -> 0.075f
                else -> 0.0533f
            }
        )
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1.0f) }

    var showControls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf(Panel.None) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var hudKind by remember { mutableIntStateOf(0) }
    var hudFraction by remember { mutableFloatStateOf(0f) }
    var scrubPreview by remember { mutableStateOf<Long?>(null) }
    var postPlayIn by remember { mutableStateOf<Int?>(null) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var autoRetry by remember { mutableIntStateOf(0) }
    var retryNonce by remember { mutableIntStateOf(0) }
    var activeDoors by remember { mutableStateOf<List<String>>(emptyList()) }
    var doorIdx by remember { mutableIntStateOf(0) }
    // State gestur seret — di tingkat atas, BUKAN di blok kondisional (slot Compose).
    var dragMode by remember { mutableIntStateOf(0) } // 0=belum, 1=geser posisi, 2=kecerahan, 3=volume
    var dragStartPos by remember { mutableLongStateOf(0L) }
    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }
    var startBrightness by remember { mutableFloatStateOf(0.5f) }
    var startVolume by remember { mutableIntStateOf(0) }

    val nextEp = episodes.filter { it.number > currentEp }.minByOrNull { it.number }
    // Pendengar pemutar dibuat sekali; nilai terbaru dibaca lewat state ini
    // (dulu pendengar memegang nextEp/ended basi -> autoplay salah episode).
    val latestNext by rememberUpdatedState(nextEp)
    val latestAutoplay by rememberUpdatedState(autoplayNext)
    val latestEnded by rememberUpdatedState(ended)

    fun saveNow(finished: Boolean = false) {
        val d = player.duration
        if (d == C.TIME_UNSET || d <= 0) return
        val pos = if (finished) d else player.currentPosition
        val ep = currentEp
        scope.launch { store.save(WatchEntry(titleId, ep, pos, d, System.currentTimeMillis())) }
    }

    // Muat episode lewat pintu CDN terbaik: probe pintu-pintu gudang
    // (CatalogApi.doorVariants) dan pakai yang pertama menjawab valid.
    fun loadEpisodeViaDoors(ep: Int, playlist: String, pos: Long) {
        activeDoors = CatalogApi.doorVariants(playlist)
        doorIdx = 0
        isBuffering = true
        scope.launch {
            val picked = CatalogApi.pickDoor(playlist)
            doorIdx = activeDoors.indexOf(picked).let { if (it < 0) 0 else it }
            loadEpisode(ep, picked, pos)
        }
    }

    fun loadEpisode(ep: Int, playlist: String, pos: Long) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (subtitleDefault == "off") {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                setPreferredTextLanguage(subtitleDefault)
            }
        }.build()
        player.setMediaItem(
            MediaItem.Builder().setUri(playlist).setMimeType(MimeTypes.APPLICATION_M3U8).build()
        )
        player.prepare()
        if (pos > 0) player.seekTo(pos)
        player.playWhenReady = true
        // Reset keadaan episode sebelumnya (dulu durasi lama membuat tombol
        // "Episode Berikutnya" muncul sesaat di episode baru).
        ended = false
        errorMsg = null
        errorDetail = null
        autoRetry = 0
        postPlayIn = null
        positionMs = pos
        bufferedMs = 0L
        durationMs = 0L
        isBuffering = true
    }

    fun switchTo(ep: EpRef) {
        saveNow()
        currentEp = ep.number
        onEpisodeChanged(ep.number)
        loadEpisodeViaDoors(ep.number, ep.playlist, 0L)
    }

    // Muat episode awal
    LaunchedEffect(Unit) {
        if (currentPlaylist.isNotBlank()) loadEpisodeViaDoors(currentEp, currentPlaylist, startPos)
    }

    // Dengarkan keadaan pemutar
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                // Putar ulang setelah selesai: bersihkan tanda "selesai".
                if (playing) ended = false
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    ended = true
                    saveNow(finished = true)
                    if (latestNext != null && latestAutoplay) postPlayIn = 10
                }
                if (state == Player.STATE_READY) {
                    durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                    autoRetry = 0
                    ended = false
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }
                // Galat jaringan/IO (2xxx): coba otomatis 3x dengan jeda 1s, 2s, 4s
                // dan lanjut dari posisi terakhir sebelum menyerah ke tombol manual.
                val isIo = error.errorCode in 2000..2999
                if (isIo && activeDoors.size > 1 && doorIdx + 1 < activeDoors.size) {
                    // Pintu aktif gagal di tengah putar -> pindah pintu berikutnya
                    // dari posisi yang sama, sebelum coba-ulang pintu yang sama.
                    doorIdx += 1
                    isBuffering = true
                    loadEpisode(currentEp, activeDoors[doorIdx], player.currentPosition)
                } else if (isIo && autoRetry < 3) {
                    autoRetry += 1
                    isBuffering = true
                    retryNonce += 1
                } else {
                    errorMsg = friendlyError(error)
                    errorDetail = error.errorCodeName
                    isBuffering = false
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Coba ulang otomatis setelah galat jaringan (backoff 1s/2s/4s).
    LaunchedEffect(retryNonce) {
        if (retryNonce == 0) return@LaunchedEffect
        delay(1000L shl (autoRetry - 1).coerceIn(0, 3))
        player.prepare()
        player.playWhenReady = true
    }

    // Polling posisi + simpan progres berkala
    LaunchedEffect(player, currentEp) {
        while (true) {
            positionMs = player.currentPosition
            bufferedMs = player.bufferedPosition
            if (player.duration != C.TIME_UNSET) durationMs = player.duration
            delay(500)
        }
    }
    LaunchedEffect(player, currentEp) {
        while (true) {
            delay(5000)
            val d = player.duration
            if (d != C.TIME_UNSET && d > 0) {
                store.save(WatchEntry(titleId, currentEp, player.currentPosition, d, System.currentTimeMillis()))
            }
        }
    }

    // Hitung mundur post-play
    LaunchedEffect(postPlayIn) {
        var v = postPlayIn ?: return@LaunchedEffect
        while (v > 0) {
            delay(1000)
            v -= 1
            if (postPlayIn == null) return@LaunchedEffect
            postPlayIn = v
        }
        val nx = latestNext
        if (nx != null && latestEnded) {
            switchTo(nx)
        }
    }

    // Sembunyikan kontrol otomatis
    LaunchedEffect(showControls, isPlaying, panel, locked) {
        if (showControls && isPlaying && panel == Panel.None && !locked) {
            delay(3500)
            showControls = false
        }
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) { delay(700); seekFeedback = null }
    }
    LaunchedEffect(gestureHint) {
        if (gestureHint != null) { delay(700); gestureHint = null }
    }

    fun toggleControls() { showControls = !showControls }
    fun seekBy(delta: Long) {
        val target = (player.currentPosition + delta).coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
        player.seekTo(target)
        seekFeedback = if (delta > 0) "+10 dtk" else "-10 dtk"
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    playerViewRef = this
                }
            },
            update = {
                it.resizeMode = resizeMode
                if (playerViewRef !== it) playerViewRef = it
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPip && !locked) {
            // Satu lapisan gestur: ketuk, ketuk-dua-kali, dan seret. (Dulu dua Box
            // bertumpuk: Box atas menelan semua sentuhan, sehingga ketuk untuk
            // menampilkan kontrol & ketuk-dua-kali ±10 dtk tidak pernah jalan.)
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { toggleControls() },
                        onDoubleTap = { off ->
                            if (off.x < size.width / 2) seekBy(-10_000) else seekBy(10_000)
                        },
                    )
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { off ->
                            dragMode = 0
                            dragAccumX = 0f
                            dragAccumY = 0f
                            dragStartPos = player.currentPosition
                            val act = context as android.app.Activity
                            val b = act.window.attributes.screenBrightness
                            startBrightness = if (b < 0) 0.5f else b
                            startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        },
                        onDragEnd = {
                            if (dragMode == 1 && scrubPreview != null) {
                                player.seekTo(scrubPreview!!)
                            }
                            scrubPreview = null
                            dragMode = 0
                            showControls = true
                        },
                        onDragCancel = { scrubPreview = null; dragMode = 0 },
                        onDrag = { change, dragAmount ->
                            dragAccumX += dragAmount.x
                            dragAccumY += dragAmount.y
                            if (dragMode == 0 && (kotlin.math.abs(dragAccumX) > 24 || kotlin.math.abs(dragAccumY) > 24)) {
                                dragMode = if (kotlin.math.abs(dragAccumX) > kotlin.math.abs(dragAccumY)) 1
                                else if (change.position.x < size.width / 2) 2 else 3
                            }
                            when (dragMode) {
                                1 -> {
                                    val delta = (dragAccumX / size.width * 120_000).toLong()
                                    val target = (dragStartPos + delta).coerceIn(0L, if (durationMs > 0) durationMs else 0L)
                                    scrubPreview = target
                                }
                                2 -> {
                                    val act = context as android.app.Activity
                                    val frac = (startBrightness - dragAccumY / size.height).coerceIn(0.05f, 1f)
                                    act.window.attributes = act.window.attributes.apply { screenBrightness = frac }
                                    gestureHint = "Kecerahan ${(frac * 100).toInt()}%"
                                    hudKind = 2
                                    hudFraction = frac
                                }
                                3 -> {
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val frac = (startVolume.toFloat() / max - dragAccumY / size.height).coerceIn(0f, 1f)
                                    val vol = (frac * max).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                                    gestureHint = "Volume ${(frac * 100).toInt()}%"
                                    hudKind = 3
                                    hudFraction = frac
                                }
                            }
                        },
                    )
                }
            )
        }

        // Indikator buffering / galat
        if (isBuffering && errorMsg == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        if (errorMsg != null) {
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(errorMsg!!, color = Color.White, fontWeight = FontWeight.Medium)
                if (errorDetail != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Kode: $errorDetail", color = AniloverTextDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = AniloverAccent, shape = AniloverShapes.Button,
                        modifier = Modifier.clickable {
                            loadEpisodeViaDoors(currentEp, currentPlaylist, positionMs)
                        },
                    ) {
                        Text("Coba Lagi", color = AniloverOnAccent, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp))
                    }
                    Surface(
                        color = AniloverSurfaceHigh, shape = AniloverShapes.Button,
                        modifier = Modifier.clickable(onClick = onBack),
                    ) {
                        Text("Kembali", color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp))
                    }
                }
            }
        }

        // Umpan balik loncat & gestur
        if (seekFeedback != null) {
            Text(
                seekFeedback!!, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (gestureHint != null) {
            // HUD gestur gaya Netflix: ikon besar + bilah tingkat + persen.
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    if (hudKind == 2) Icons.Filled.BrightnessHigh else Icons.Filled.VolumeUp,
                    contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.width(150.dp).height(5.dp)
                        .background(Color.White.copy(alpha = 0.3f), AniloverShapes.Pill),
                ) {
                    Box(
                        Modifier.fillMaxWidth(hudFraction.coerceIn(0f, 1f)).height(5.dp)
                            .background(AniloverAccent, AniloverShapes.Pill),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(gestureHint!!, color = Color.White, fontSize = 12.sp)
            }
        }
        if (scrubPreview != null) {
            Text(
                formatClock(scrubPreview!!) + " / " + formatClock(durationMs),
                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Kunci layar
        if (locked && !inPip) {
            IconButton(
                onClick = { locked = false; showControls = true },
                modifier = Modifier.align(Alignment.CenterStart).padding(20.dp),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Buka kunci", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }

        // Kontrol utama
        AnimatedVisibility(
            visible = showControls && !locked && !inPip && errorMsg == null,
            enter = fadeIn(), exit = fadeOut(),
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.85f),
                        )
                    )
                )
            ) {
                // Bar atas
                Row(
                    Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "Episode $currentEp dari ${episodes.size}" + if (casting) " · Diputar di TV" else "",
                            color = AniloverTextDim, fontSize = 12.sp,
                        )
                    }
                    if (castContext != null) {
                        AndroidView(
                            factory = { ctx ->
                                // Tema khusus + try/catch: kegagalan tombol Cast
                                // tidak boleh sampai menutup aplikasi.
                                try {
                                    val themed = android.view.ContextThemeWrapper(ctx, R.style.Theme_Anilover_Cast)
                                    MediaRouteButton(themed).also { btn ->
                                        CastButtonFactory.setUpMediaRouteButton(ctx, btn)
                                    }
                                } catch (e: Throwable) {
                                    android.view.View(ctx)
                                }
                            },
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    IconButton(onClick = { panel = Panel.Tracks }) {
                        Icon(Icons.Filled.Subtitles, contentDescription = "Audio & subtitle", tint = Color.White)
                    }
                    IconButton(onClick = onEnterPip) {
                        Icon(Icons.Filled.PictureInPicture, contentDescription = "Picture in Picture", tint = Color.White)
                    }
                    IconButton(onClick = { locked = true; showControls = false }) {
                        Icon(Icons.Filled.LockOpen, contentDescription = "Kunci layar", tint = Color.White)
                    }
                }

                // Kontrol tengah
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { seekBy(-10_000) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Mundur 10 detik", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    IconButton(
                        onClick = { if (isPlaying) player.pause() else player.play() },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color.White, modifier = Modifier.size(56.dp),
                        )
                    }
                    IconButton(onClick = { seekBy(10_000) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Maju 10 detik", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                }

                // Bar bawah
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    NetflixSeekBar(
                        positionMs = scrubPreview ?: positionMs,
                        bufferedMs = bufferedMs,
                        durationMs = durationMs,
                        onScrub = { player.seekTo(it) },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatClock(positionMs), color = Color.White, fontSize = 12.sp)
                        Text(" / ", color = AniloverTextDim, fontSize = 12.sp)
                        Text(formatRemaining(positionMs, durationMs), color = AniloverTextDim, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { panel = Panel.Speed }) {
                            Icon(Icons.Filled.Speed, contentDescription = "Kecepatan", tint = Color.White)
                        }
                        Text(speedLabel(speed), color = Color.White, fontSize = 12.sp)
                        IconButton(onClick = { panel = Panel.Quality }) {
                            Icon(Icons.Filled.HighQuality, contentDescription = "Kualitas", tint = Color.White)
                        }
                        IconButton(onClick = { panel = Panel.Tracks }) {
                            Icon(Icons.Filled.Audiotrack, contentDescription = "Audio", tint = Color.White)
                        }
                        if (nextEp != null) {
                            IconButton(onClick = { switchTo(nextEp) }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = "Episode berikutnya", tint = Color.White)
                            }
                        }
                        IconButton(onClick = { panel = Panel.Episodes }) {
                            Icon(Icons.Filled.List, contentDescription = "Episode", tint = Color.White)
                        }
                        IconButton(onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }) {
                            Icon(Icons.Filled.AspectRatio, contentDescription = "Ukuran layar", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Lewati Intro: batas dari data katalog bila ada (introEnd), selain itu
        // heuristik awal episode (intro umumnya < 90 detik).
        val introDataMs = (episodes.firstOrNull { it.number == currentEp }?.introEndSec ?: 0L) * 1000
        val skipTargetMs = if (introDataMs > 0) introDataMs else 90_000L
        val skipVisible = !ended && !inPip && !locked && durationMs > 180_000 && positionMs > 0 &&
            positionMs <= (if (introDataMs > 0) introDataMs else 85_000L)
        if (skipVisible) {
            Surface(
                color = AniloverAccent,
                shape = AniloverShapes.Button,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 108.dp)
                    .clickable {
                        val target = minOf(skipTargetMs, (durationMs - 5_000).coerceAtLeast(0L))
                        player.seekTo(target)
                        showControls = false
                    },
            ) {
                Text(
                    "Lewati Intro", color = AniloverOnAccent, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        // Episode Berikutnya: muncul menjelang akhir
        if (!ended && !inPip && !locked && nextEp != null && durationMs > 0 &&
            (durationMs - positionMs) > 0 && (durationMs - positionMs) <= 45_000
        ) {
            Surface(
                color = AniloverSurface,
                shape = AniloverShapes.Button,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 108.dp)
                    .clickable { switchTo(nextEp) },
            ) {
                Text(
                    "Episode Berikutnya", color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        // Post-play: episode berikutnya
        if (ended && nextEp != null && !inPip) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(22.dp).width(300.dp)
                    .background(AniloverSurface, AniloverShapes.Card)
                    .border(1.dp, AniloverOutline, AniloverShapes.Card)
                    .padding(16.dp),
            ) {
                Column {
                    Text("Episode Berikutnya", color = AniloverTextDim, fontSize = 12.sp)
                    Text("Episode ${nextEp.number}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            postPlayIn != null -> "Diputar dalam $postPlayIn detik…"
                            autoplayNext -> ""
                            else -> "Autoplay nonaktif — putar manual bila siap."
                        },
                        color = AniloverTextDim, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = AniloverAccent, shape = AniloverShapes.Button,
                            modifier = Modifier.clickable { switchTo(nextEp) },
                        ) {
                            Text("Putar Sekarang", color = AniloverOnAccent, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                        Surface(
                            color = AniloverSurfaceHigh, shape = AniloverShapes.Button,
                            modifier = Modifier.clickable { postPlayIn = null },
                        ) {
                            Text("Batal", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
        if (ended && nextEp == null && !inPip) {
            IconButton(
                onClick = { player.seekTo(0); player.play() },
                modifier = Modifier.align(Alignment.Center).size(72.dp),
            ) {
                Icon(Icons.Filled.Replay, contentDescription = "Putar ulang", tint = Color.White, modifier = Modifier.size(56.dp))
            }
        }

        // Panel bawah
        when (panel) {
            Panel.Speed -> PanelSheet(onClose = { panel = Panel.None }) {
                Text("Kecepatan Putar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                for (s in SPEEDS) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            player.setPlaybackSpeed(s); speed = s; panel = Panel.None
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(speedLabel(s) + if (s == 1.0f) " (Normal)" else "", color = Color.White, modifier = Modifier.weight(1f))
                        if (s == speed) Icon(Icons.Filled.Check, contentDescription = null, tint = AniloverAccent)
                    }
                }
            }
            Panel.Quality -> QualityPanel(player = player, onClose = { panel = Panel.None })
            Panel.Tracks -> TracksPanel(player = player, onClose = { panel = Panel.None })
            Panel.Episodes -> PanelSheet(onClose = { panel = Panel.None }) {
                Text("Episode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxHeight(0.7f)) {
                    items(episodes) { ep ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                panel = Panel.None
                                if (ep.number != currentEp) switchTo(ep)
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Episode ${ep.number}", color = if (ep.number == currentEp) AniloverAccent else Color.White, modifier = Modifier.weight(1f))
                            if (ep.number == currentEp) Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = AniloverAccent)
                        }
                    }
                }
            }
            Panel.None -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}

@Composable
private fun PanelSheet(onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onClose))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(AniloverSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                .padding(18.dp),
        ) { content() }
    }
}

@Composable
private fun TracksPanel(player: ExoPlayer, onClose: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    PanelSheet(onClose = onClose) {
        Text("Audio & Subtitle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        val groups = player.currentTracks.groups
        val audio = groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val text = groups.filter { it.type == C.TRACK_TYPE_TEXT }
        if (audio.isEmpty() && text.isEmpty()) {
            Text("Tidak ada trek audio/subtitle tambahan pada episode ini.", color = AniloverTextDim, fontSize = 13.sp)
        }
        if (audio.isNotEmpty()) {
            Text("Audio", color = AniloverAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            for (g in audio) {
                for (i in 0 until g.length) {
                    val f = g.getTrackFormat(i)
                    val label = trackLabel(f.language, f.label, "Audio ${i + 1}")
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
                            refresh++
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = Color.White, modifier = Modifier.weight(1f))
                        if (g.isTrackSelected(i)) Icon(Icons.Filled.Check, contentDescription = null, tint = AniloverAccent)
                    }
                }
            }
        }
        if (text.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Subtitle", color = AniloverAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().clickable {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                    refresh++
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Nonaktif", color = Color.White, modifier = Modifier.weight(1f))
            }
            for (g in text) {
                for (i in 0 until g.length) {
                    val f = g.getTrackFormat(i)
                    val label = trackLabel(f.language, f.label, "Subtitle ${i + 1}")
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
                            refresh++
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = Color.White, modifier = Modifier.weight(1f))
                        if (g.isTrackSelected(i)) Icon(Icons.Filled.Check, contentDescription = null, tint = AniloverAccent)
                    }
                }
            }
        }
        if (refresh < 0) Text("") // penanda state dibaca ulang
    }
}

@Composable
private fun QualityPanel(player: ExoPlayer, onClose: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    PanelSheet(onClose = onClose) {
        Text("Kualitas Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        val videoGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        Row(
            Modifier.fillMaxWidth().clickable {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO).build()
                refresh++
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Otomatis (disarankan)", color = Color.White, modifier = Modifier.weight(1f))
        }
        if (videoGroups.isEmpty()) {
            Text("Hanya satu kualitas tersedia dari gudang untuk episode ini.", color = AniloverTextDim, fontSize = 13.sp)
        }
        for (g in videoGroups) {
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                val label = when {
                    f.height > 0 -> "${f.height}p"
                    f.bitrate > 0 -> "${f.bitrate / 1000} kbps"
                    else -> "Kualitas ${i + 1}"
                }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
                        refresh++
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = Color.White, modifier = Modifier.weight(1f))
                    if (g.isTrackSelected(i)) Icon(Icons.Filled.Check, contentDescription = null, tint = AniloverAccent)
                }
            }
        }
        if (refresh < 0) Text("")
    }
}

private fun trackLabel(language: String?, label: String?, fallback: String): String {
    if (!label.isNullOrBlank()) return label
    if (!language.isNullOrBlank() && language != "und") {
        return Locale(language).getDisplayLanguage(Locale("id")).ifBlank { language }
    }
    return fallback
}

private fun speedLabel(s: Float): String =
    if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"

@Composable
private fun NetflixSeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
) {
    var scrubFrac by remember { mutableStateOf<Float?>(null) }
    val played = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val buffered = if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier.fillMaxWidth().height(32.dp).pointerInput(durationMs) {
            detectTapGestures { off ->
                if (durationMs > 0) {
                    val f = (off.x / size.width).coerceIn(0f, 1f)
                    onScrub((f * durationMs).toLong())
                }
            }
        }.pointerInput(durationMs) {
            detectDragGestures(
                onDragStart = { off -> scrubFrac = (off.x / size.width).coerceIn(0f, 1f) },
                onDrag = { change, _ -> scrubFrac = (change.position.x / size.width).coerceIn(0f, 1f) },
                onDragEnd = {
                    val f = scrubFrac
                    if (f != null && durationMs > 0) onScrub((f * durationMs).toLong())
                    scrubFrac = null
                },
                onDragCancel = { scrubFrac = null },
            )
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.25f), AniloverShapes.Pill))
        Box(Modifier.fillMaxWidth(buffered).height(5.dp).background(Color.White.copy(alpha = 0.45f), AniloverShapes.Pill))
        Box(Modifier.fillMaxWidth(scrubFrac ?: played).height(5.dp).background(AniloverAccent, AniloverShapes.Pill))
        Box(
            Modifier.fillMaxWidth(scrubFrac ?: played),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.size(16.dp).background(AniloverAccent, AniloverShapes.Pill).border(2.dp, Color.White, AniloverShapes.Pill))
        }
    }
}
