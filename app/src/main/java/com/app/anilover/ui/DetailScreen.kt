package com.app.anilover.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var confirmAll by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val hasTrailer = !title.trailer.isNullOrBlank()
    val tabKeys = buildList {
        add("ep")
        if (hasTrailer) add("trailer")
        if (similar.isNotEmpty()) add("similar")
    }
    var tabKey by remember(title.id) { mutableStateOf("ep") }
    val activeTab = if (tabKeys.contains(tabKey)) tabKey else "ep"
    val pending = title.episodes.filter { downloadByEp[it.number] == null }
    // Tombol Putar lengket: muncul setelah hero + info tergeser lewat.
    val showSticky by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= 2 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 650)
        }
    }
    val playMain = {
        if (resume != null) onPlay(resume.episode, resume.positionMs)
        else onPlay(title.episodes.firstOrNull()?.number ?: 1, 0L)
    }

    Box(Modifier.fillMaxSize().background(AniloverBackground)) {
    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(bottom = 84.dp),
    ) {
        item(key = "hero") {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(AniloverSurface)) {
                if (hasTrailer) {
                    // Trailer inline: bisu, mutar otomatis di hero.
                    TrailerPlayer(title.trailer!!)
                } else {
                    PosterImage(
                        model = title.banner ?: title.poster,
                        contentDescription = title.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, AniloverBackground)
                        )
                    )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.statusBarsPadding().padding(12.dp).size(42.dp)
                        .background(Color.Black.copy(alpha = 0.5f), AniloverShapes.Pill),
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                // Tombol putar besar di tengah hero (gaya referensi).
                Box(
                    Modifier.align(Alignment.Center).size(58.dp)
                        .background(Color.Black.copy(alpha = 0.5f), AniloverShapes.Pill)
                        .clip(AniloverShapes.Pill)
                        .clickable(onClick = playMain),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow, contentDescription = "Putar", tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
        }
        item(key = "info") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    title.title, color = AniloverTextPrimary, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, lineHeight = 30.sp, maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                // Chip metadata
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    title.score?.takeIf { it > 0.0 }?.let {
                        MetaChip(
                            "★ " + String.format(java.util.Locale("id", "ID"), "%.1f", it),
                            textColor = AniloverGold,
                        )
                    }
                    title.matchPercent?.let { MetaChip("$it% cocok", textColor = AniloverAccent) }
                    title.year?.let { MetaChip(it.toString()) }
                    title.statusLabel?.let { MetaChip(it) }
                    MetaChip("HD")
                    if (title.episodes.isNotEmpty()) MetaChip("${title.episodes.size} Eps")
                    if (title.adult) MetaChip("18+")
                }
                if (title.genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(title.genres.joinToString(" · "), color = AniloverTextDim, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = playMain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                    ),
                    shape = AniloverShapes.Button,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (resume != null) "Lanjutkan E${resume.episode} · ${formatClock(resume.positionMs)}"
                        else "Putar",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DetailAction(
                        icon = if (inMyList) Icons.Filled.Check else Icons.Filled.Add,
                        label = if (inMyList) "Di Daftarku" else "Daftarku",
                        active = inMyList,
                        onClick = onToggleMyList,
                    )
                    DetailAction(
                        icon = Icons.Filled.ThumbUp,
                        label = when (rating) {
                            2 -> "Sangat Suka"
                            1 -> "Disukai"
                            else -> "Sukai"
                        },
                        active = rating > 0,
                        // Ketuk berurutan: Suka -> Sangat Suka -> batal.
                        onClick = {
                            onRate(
                                when (rating) {
                                    1 -> 2
                                    2 -> 0
                                    else -> 1
                                }
                            )
                        },
                    )
                    DetailAction(
                        icon = Icons.Filled.ThumbDown,
                        label = "Tidak Suka",
                        active = rating == -1,
                        onClick = { onRate(if (rating == -1) 0 else -1) },
                    )
                    DetailAction(
                        icon = Icons.Filled.Download,
                        label = if (pending.isEmpty()) "Terunduh" else "Unduh",
                        active = pending.isEmpty() && title.episodes.isNotEmpty(),
                        onClick = { if (pending.isNotEmpty()) confirmAll = true },
                    )
                    DetailAction(
                        icon = Icons.Filled.Share,
                        label = "Bagikan",
                        active = false,
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Tonton ${title.title} di Anilover")
                            }
                            context.startActivity(Intent.createChooser(send, "Bagikan"))
                        },
                    )
                }
                if (!title.synopsis.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        title.synopsis, color = AniloverTextDim, fontSize = 14.sp, lineHeight = 20.sp,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded },
                    )
                    Text(
                        if (synopsisExpanded) "Tutup" else "Selengkapnya",
                        color = AniloverAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                            .padding(vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                // Tab pil: yang aktif terisi aksen.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (key in tabKeys) {
                        val label = when (key) {
                            "ep" -> "Episode"
                            "trailer" -> "Trailer"
                            else -> "Serupa"
                        }
                        val on = activeTab == key
                        Box(
                            Modifier.clip(AniloverShapes.Pill)
                                .background(if (on) AniloverAccent else AniloverSurfaceHigh)
                                .clickable { tabKey = key }
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                        ) {
                            Text(
                                label,
                                color = if (on) AniloverOnAccent else AniloverTextDim,
                                fontSize = 13.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (activeTab == "ep" && title.episodes.isNotEmpty()) {
            // Chip nomor episode: ditonton = redup, dilanjutkan = aksen terisi.
            val rows = title.episodes.chunked(5)
            item(key = "chips-head") {
                Text(
                    "Pilih Episode", color = AniloverTextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                )
            }
            items(rows, key = { r -> "chips-" + r.first().number }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { ep ->
                        val prog = progressByEp[ep.number]
                        val isResume = resume?.episode == ep.number
                        val watched = prog != null && prog.isFinished
                        Box(
                            Modifier.weight(1f).height(42.dp)
                                .clip(AniloverShapes.Chip)
                                .background(if (isResume) AniloverAccent else AniloverSurfaceHigh)
                                .clickable {
                                    onPlay(
                                        ep.number,
                                        if (prog != null && prog.isStarted && !prog.isFinished) prog.positionMs else 0L,
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "%02d".format(ep.number),
                                color = when {
                                    isResume -> AniloverOnAccent
                                    watched -> AniloverTextDim.copy(alpha = 0.55f)
                                    else -> AniloverTextPrimary
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isResume) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item(key = "list-head") {
                Text(
                    "Daftar Episode", color = AniloverTextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                )
            }
        }
        if (activeTab == "ep") {
        items(title.episodes.size, key = { idx -> "ep" + title.episodes[idx].number }) { idx ->
            val ep = title.episodes[idx]
            val prog = progressByEp[ep.number]
            val dl = downloadByEp[ep.number]
            Column(
                Modifier.fillMaxWidth()
                    .clickable {
                        onPlay(ep.number, if (prog != null && prog.isStarted && !prog.isFinished) prog.positionMs else 0L)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.width(112.dp).aspectRatio(16f / 9f)
                            .clip(AniloverShapes.Poster).background(AniloverSurface),
                    ) {
                        PosterImage(
                            model = ep.thumb ?: title.banner ?: title.poster,
                            contentDescription = "Episode ${ep.number}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.align(Alignment.Center).size(30.dp)
                                .background(Color.Black.copy(alpha = 0.55f), AniloverShapes.Pill),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        if (prog != null && prog.isStarted && !prog.isFinished) {
                            Box(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                    .height(3.dp).background(Color.White.copy(alpha = 0.35f)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(prog.fraction).height(3.dp)
                                        .background(AniloverAccent),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${ep.number}. " + (ep.title ?: "Episode ${ep.number}"),
                            color = AniloverTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (!ep.synopsis.isNullOrBlank()) {
                            Text(
                                ep.synopsis, color = AniloverTextDim, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when {
                            prog != null && prog.isFinished ->
                                Text("Selesai ditonton", color = AniloverTextDim, fontSize = 12.sp)
                            prog != null && prog.isStarted ->
                                Text(
                                    formatClock(prog.durationMs) + " · sisa " +
                                        formatRemainingPlain(prog.positionMs, prog.durationMs),
                                    color = AniloverTextDim, fontSize = 12.sp,
                                )
                            dl?.state == "selesai" ->
                                Text("Terunduh · " + formatSize(dl.sizeBytes), color = AniloverTextDim, fontSize = 12.sp)
                        }
                    }
                    when (dl?.state) {
                        "mengunduh" -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { dl.fraction },
                                color = AniloverAccent, modifier = Modifier.size(26.dp),
                                trackColor = Color.White.copy(alpha = 0.25f),
                            )
                        }
                        "selesai" -> IconButton(onClick = { onDeleteDownload(dl) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus unduhan", tint = AniloverTextDim)
                        }
                        else -> IconButton(onClick = { onDownload(ep) }) {
                            Icon(Icons.Filled.Download, contentDescription = "Unduh episode", tint = AniloverTextPrimary)
                        }
                    }
                }
                if (dl?.state == "gagal") {
                    Text(
                        "Unduhan gagal — ketuk ikon unduh untuk mencoba lagi.",
                        color = AniloverAccent, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 124.dp, top = 2.dp),
                    )
                }
            }
        }
        if (similar.isNotEmpty()) {
            item(key = "reco") { PosterRow("Rekomendasi untukmu", similar, onOpenTitle) }
        }
        }
        if (activeTab == "trailer" && hasTrailer) {
            item(key = "trailer") {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .clip(AniloverShapes.Card).background(AniloverSurface),
                    ) {
                        TrailerPlayer(title.trailer!!, controls = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Trailer — ${title.title}", color = AniloverTextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Cuplikan resmi dari katalog gudang Anilover.",
                        color = AniloverTextDim, fontSize = 12.sp,
                    )
                }
            }
        }
        if (activeTab == "similar" && similar.isNotEmpty()) {
            items(similar.chunked(3), key = { r -> "sim-" + r.first().id }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { t ->
                        PosterCard(t = t, modifier = Modifier.weight(1f), onClick = { onOpenTitle(t) })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
    AnimatedVisibility(
        visible = showSticky,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it },
        exit = fadeOut(tween(120)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Row(
            Modifier.fillMaxWidth().background(AniloverSurface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title.title, color = AniloverTextPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (resume != null) "Lanjutkan E${resume.episode}" else "${title.episodes.size} episode",
                    color = AniloverTextDim, fontSize = 12.sp,
                )
            }
            Button(
                onClick = playMain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                ),
                shape = AniloverShapes.Button,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (resume != null) "Lanjutkan" else "Putar", fontWeight = FontWeight.Bold)
            }
        }
    }
    }
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Unduh semua episode?") },
            text = { Text("${pending.size} episode akan diunduh ke perangkat.") },
            confirmButton = {
                TextButton(onClick = {
                    pending.forEach { onDownload(it) }
                    confirmAll = false
                }) { Text("Unduh", color = AniloverAccent) }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun MetaChip(text: String, textColor: Color = AniloverTextPrimary) {
    Text(
        text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(AniloverSurfaceHigh, AniloverShapes.Pill)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

@Composable
private fun DetailAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(64.dp).clip(AniloverShapes.Card).clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            Modifier.size(44.dp)
                .background(if (active) AniloverAccent.copy(alpha = 0.18f) else AniloverSurfaceHigh, AniloverShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, contentDescription = label,
                tint = if (active) AniloverAccent else AniloverTextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label, color = if (active) AniloverAccent else AniloverTextDim, fontSize = 10.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
