package com.app.rea.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.app.rea.data.HistoryStore
import com.app.rea.data.LibraryStore
import com.app.rea.data.ReaSettings
import com.app.rea.data.WatchEntry
import com.app.rea.ui.ReaAccent
import com.app.rea.ui.ReaTheme
import com.app.rea.ui.formatClock
import com.app.rea.ui.formatRemaining
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

data class EpRef(val number: Int, val playlist: String)

class PlayerActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
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

        setContent {
            ReaTheme {
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
                    onPlayerCreated = { playerRef = it },
                    onEpisodeChanged = { currentEp = it },
                )
            }
        }
    }

    private fun parseEpisodes(s: String): List<EpRef> = try {
        val arr = JSONArray(s)
        val out = ArrayList<EpRef>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(EpRef(o.optInt("n"), o.optString("p")))
        }
        out.sortedBy { it.number }
    } catch (e: Exception) {
        emptyList()
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

    override fun onDestroy() {
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
        ExoPlayer.Builder(context).build().also { onPlayerCreated(it) }
    }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val library = remember { LibraryStore(context) }
    val reaSettings by library.settings.collectAsState(initial = ReaSettings())

    // Terapkan gaya subtitle pilihan pengguna (warna + ukuran) ke tampilan subtitle.
    LaunchedEffect(playerViewRef, reaSettings.subtitleSize, reaSettings.subtitleColor) {
        val sv = playerViewRef?.subtitleView ?: return@LaunchedEffect
        val fg = when (reaSettings.subtitleColor) {
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
        sv.setViewTextSize(
            SubtitleView.TEXT_SIZE_TYPE_FRACTIONAL,
            when (reaSettings.subtitleSize) {
                "kecil" -> 0.040f
                "besar" -> 0.075f
                else -> 0.0533f
            },
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
    var scrubPreview by remember { mutableStateOf<Long?>(null) }
    var postPlayIn by remember { mutableStateOf<Int?>(null) }

    val nextEp = episodes.firstOrNull { it.number == (episodes.filter { it.number > currentEp }.minOfOrNull { it.number } ?: -1) }

    fun loadEpisode(ep: Int, playlist: String, pos: Long) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (subtitleDefault == "off") {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                setPreferredTextLanguage(subtitleDefault)
            }
        }.build()
        player.setMediaItem(MediaItem.fromUri(playlist))
        player.prepare()
        if (pos > 0) player.seekTo(pos)
        player.playWhenReady = true
        ended = false
        errorMsg = null
    }

    // Muat episode awal
    LaunchedEffect(Unit) {
        if (currentPlaylist.isNotBlank()) loadEpisode(currentEp, currentPlaylist, startPos)
    }

    // Dengarkan keadaan pemutar
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    ended = true
                    if (nextEp != null && autoplayNext) postPlayIn = 10
                }
                if (state == Player.STATE_READY) {
                    durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                errorMsg = "Gagal memutar. Periksa koneksi lalu coba lagi."
                isBuffering = false
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
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
        val nx = nextEp
        if (nx != null && ended) {
            currentEp = nx.number
            onEpisodeChanged(nx.number)
            loadEpisode(nx.number, nx.playlist, 0L)
            postPlayIn = null
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
            // Lapisan gestur: ketuk & ketuk-dua-kali
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { toggleControls() },
                        onDoubleTap = { off ->
                            if (off.x < size.width / 2) seekBy(-10_000) else seekBy(10_000)
                        },
                    )
                }
            )
            // Lapisan gestur: seret (kecerahan/volume/loncat)
            var dragMode by remember { mutableStateOf(0) } // 0=belum, 1=geser posisi, 2=kecerahan, 3=volume
            var dragStartPos by remember { mutableLongStateOf(0L) }
            var dragAccumX by remember { mutableFloatStateOf(0f) }
            var dragAccumY by remember { mutableFloatStateOf(0f) }
            var startBrightness by remember { mutableFloatStateOf(0.5f) }
            var startVolume by remember { mutableIntStateOf(0) }
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { off ->
                            dragMode = 0
                            dragAccumX = 0f
                            dragAccumY = 0f
                            dragStartPos = player.currentPosition
                            val act = context as ComponentActivity
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
                                    val act = context as ComponentActivity
                                    val frac = (startBrightness - dragAccumY / size.height).coerceIn(0.05f, 1f)
                                    act.window.attributes = act.window.attributes.apply { screenBrightness = frac }
                                    gestureHint = "Kecerahan ${(frac * 100).toInt()}%"
                                }
                                3 -> {
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val frac = (startVolume.toFloat() / max - dragAccumY / size.height).coerceIn(0f, 1f)
                                    val vol = (frac * max).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                                    gestureHint = "Volume ${(frac * 100).toInt()}%"
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
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMsg!!, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = ReaAccent, shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        errorMsg = null
                        loadEpisode(currentEp, currentPlaylist, positionMs)
                    },
                ) {
                    Text("Coba Lagi", color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
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
            Text(
                gestureHint!!, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
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
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
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
                        Text("Episode $currentEp dari ${episodes.size}", color = Color(0xFFCCCCCC), fontSize = 12.sp)
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
                        Text(" / ", color = Color(0xFF999999), fontSize = 12.sp)
                        Text(formatRemaining(positionMs, durationMs), color = Color(0xFF999999), fontSize = 12.sp)
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
                            IconButton(onClick = {
                                currentEp = nextEp.number
                                onEpisodeChanged(nextEp.number)
                                loadEpisode(nextEp.number, nextEp.playlist, 0L)
                            }) {
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

        // Lewati Intro: muncul di awal episode (intro umumnya < 90 detik)
        if (!ended && !inPip && !locked && durationMs > 180_000 && positionMs > 0 && positionMs <= 85_000) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 108.dp)
                    .clickable {
                        val target = minOf(90_000L, (durationMs - 5_000).coerceAtLeast(0L))
                        player.seekTo(target)
                        showControls = false
                    },
            ) {
                Text(
                    "Lewati Intro", color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        // Episode Berikutnya: muncul menjelang akhir
        if (!ended && !inPip && !locked && nextEp != null && durationMs > 0 &&
            (durationMs - positionMs) > 0 && (durationMs - positionMs) <= 45_000
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 108.dp)
                    .clickable {
                        currentEp = nextEp.number
                        onEpisodeChanged(nextEp.number)
                        loadEpisode(nextEp.number, nextEp.playlist, 0L)
                    },
            ) {
                Text(
                    "Episode Berikutnya", color = Color.Black, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        // Post-play: episode berikutnya
        if (ended && nextEp != null && !inPip) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(22.dp).width(300.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Text("Episode Berikutnya", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    Text("Episode ${nextEp.number}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            postPlayIn != null -> "Diputar dalam $postPlayIn detik…"
                            autoplayNext -> ""
                            else -> "Autoplay nonaktif — putar manual bila siap."
                        },
                        color = Color(0xFFAAAAAA), fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = Color.White, shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                postPlayIn = null
                                currentEp = nextEp.number
                                onEpisodeChanged(nextEp.number)
                                loadEpisode(nextEp.number, nextEp.playlist, 0L)
                            },
                        ) {
                            Text("Putar Sekarang", color = Color.Black, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                        Surface(
                            color = Color(0xFF333333), shape = RoundedCornerShape(6.dp),
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
                        if (s == speed) Icon(Icons.Filled.Check, contentDescription = null, tint = ReaAccent)
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
                                if (ep.number != currentEp) {
                                    currentEp = ep.number
                                    onEpisodeChanged(ep.number)
                                    loadEpisode(ep.number, ep.playlist, 0L)
                                }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Episode ${ep.number}", color = if (ep.number == currentEp) ReaAccent else Color.White, modifier = Modifier.weight(1f))
                            if (ep.number == currentEp) Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ReaAccent)
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
                .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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
            Text("Tidak ada trek audio/subtitle tambahan pada episode ini.", color = Color(0xFFAAAAAA), fontSize = 13.sp)
        }
        if (audio.isNotEmpty()) {
            Text("Audio", color = ReaAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        if (g.isTrackSelected(i)) Icon(Icons.Filled.Check, contentDescription = null, tint = ReaAccent)
                    }
                }
            }
        }
        if (text.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Subtitle", color = ReaAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        if (g.isTrackSelected(i)) Icon(Icons.Filled.Check, contentDescription = null, tint = ReaAccent)
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
            Text("Hanya satu kualitas tersedia dari gudang untuk episode ini.", color = Color(0xFFAAAAAA), fontSize = 13.sp)
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
                    if (g.isTrackSelected(i)) Icon(Icons.Filled.Check, contentDescription = null, tint = ReaAccent)
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
        Modifier.fillMaxWidth().height(22.dp).pointerInput(durationMs) {
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
        }.clickable { /* ketuk ditangani lewat drag nol */ },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(buffered).height(4.dp).background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(scrubFrac ?: played).height(4.dp).background(ReaAccent, RoundedCornerShape(2.dp)))
        Box(
            Modifier.fillMaxWidth(scrubFrac ?: played),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.size(13.dp).background(ReaAccent, RoundedCornerShape(50)))
        }
    }
}
