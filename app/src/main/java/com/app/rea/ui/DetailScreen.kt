package com.app.rea.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.rea.data.DownloadItem
import com.app.rea.data.Episode
import com.app.rea.data.Title
import com.app.rea.data.WatchEntry

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
    val listState = rememberLazyListState()
    val hasTrailer = !title.trailer.isNullOrBlank()
    val tabKeys = buildList {
        add("ep")
        if (hasTrailer) add("trailer")
        if (similar.isNotEmpty()) add("similar")
    }
    var tabKey by remember(title.id) { mutableStateOf("ep") }
    if (!tabKeys.contains(tabKey)) tabKey = "ep"
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
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(ReaSurface)) {
                if (hasTrailer) {
                    // Trailer inline ala Netflix: bisu, mutar otomatis di hero.
                    TrailerPlayer(title.trailer!!)
                } else {
                    AsyncImage(
                        model = title.banner ?: title.poster,
                        contentDescription = title.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, ReaBackground)
                        )
                    )
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text(
                    title.title, color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    title.matchPercent?.let {
                        Text("$it% Kecocokan", color = Color(0xFF46D369), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    title.score?.let {
                        Text("★ " + String.format("%.1f", it), color = Color.White, fontSize = 13.sp)
                    }
                    title.year?.let { Text(it.toString(), color = Color.White, fontSize = 13.sp) }
                    title.statusLabel?.let {
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
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    if (title.adult) {
                        Text(
                            "18+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (title.genres.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(title.genres.joinToString(" · "), color = ReaTextDim, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (resume != null) onPlay(resume.episode, resume.positionMs)
                        else onPlay(title.episodes.firstOrNull()?.number ?: 1, 0L)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (resume != null) "Lanjutkan E${resume.episode} · ${formatClock(resume.positionMs)}"
                        else "Putar",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DetailAction(
                        icon = if (inMyList) Icons.Filled.Check else Icons.Filled.Add,
                        label = if (inMyList) "Di Daftarku" else "Daftarku",
                        active = inMyList,
                        onClick = onToggleMyList,
                    )
                    DetailAction(
                        icon = Icons.Filled.ThumbDown,
                        label = "Tidak Suka",
                        active = rating == -1,
                        onClick = { onRate(if (rating == -1) 0 else -1) },
                    )
                    DetailAction(
                        icon = Icons.Filled.ThumbUp,
                        label = when (rating) {
                            2 -> "Sangat Suka"
                            1 -> "Disukai"
                            else -> "Sukai"
                        },
                        active = rating > 0,
                        // Ketuk berurutan ala Netflix: Suka -> Sangat Suka -> batal.
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
                        icon = Icons.Filled.Share,
                        label = "Bagikan",
                        active = false,
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Tonton ${title.title} di REA")
                            }
                            context.startActivity(Intent.createChooser(send, "Bagikan"))
                        },
                    )
                }
                if (!title.synopsis.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        title.synopsis, color = Color(0xFFDDDDDD), fontSize = 14.sp,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded },
                    )
                    Text(
                        if (synopsisExpanded) "Tutup" else "Lainnya",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                            .padding(vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (key in tabKeys) {
                        val label = when (key) {
                            "ep" -> "Episode"
                            "trailer" -> "Trailer & Lainnya"
                            else -> "Lainnya Seperti Ini"
                        }
                        Column(
                            Modifier.weight(1f).clickable { tabKey = key },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (tabKey == key) {
                                Box(Modifier.fillMaxWidth().height(3.dp).background(ReaAccent))
                            } else {
                                Spacer(Modifier.height(3.dp))
                            }
                            Text(
                                label,
                                color = if (tabKey == key) Color.White else ReaTextDim,
                                fontSize = 13.sp,
                                fontWeight = if (tabKey == key) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        if (tabKey == "ep") {
        items(title.episodes.size) { idx ->
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
                    Text(
                        "${ep.number}", color = Color.White, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(30.dp),
                    )
                    Box(
                        Modifier.width(118.dp).aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
                    ) {
                        AsyncImage(
                            model = ep.thumb ?: title.banner ?: title.poster,
                            contentDescription = "Episode ${ep.number}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.align(Alignment.Center).size(34.dp)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        if (prog != null && prog.isStarted && !prog.isFinished) {
                            Box(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                    .height(3.dp).background(Color.White.copy(alpha = 0.35f)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(prog.fraction).height(3.dp)
                                        .background(ReaAccent),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ep.title ?: "Episode ${ep.number}",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (!ep.synopsis.isNullOrBlank()) {
                            Text(
                                ep.synopsis, color = ReaTextDim, fontSize = 12.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when {
                            prog != null && prog.isFinished ->
                                Text("Selesai ditonton", color = ReaTextDim, fontSize = 12.sp)
                            prog != null && prog.isStarted ->
                                Text(
                                    formatClock(prog.durationMs) + " · sisa " +
                                        formatRemainingPlain(prog.positionMs, prog.durationMs),
                                    color = ReaTextDim, fontSize = 12.sp,
                                )
                            dl?.state == "selesai" ->
                                Text("Terunduh · " + formatSize(dl.sizeBytes), color = ReaTextDim, fontSize = 12.sp)
                        }
                    }
                    when (dl?.state) {
                        "mengunduh" -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { dl.fraction },
                                color = ReaAccent, modifier = Modifier.size(26.dp),
                                trackColor = Color.White.copy(alpha = 0.25f),
                            )
                        }
                        "selesai" -> IconButton(onClick = { onDeleteDownload(dl) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus unduhan", tint = Color.White)
                        }
                        else -> IconButton(onClick = { onDownload(ep) }) {
                            Icon(Icons.Filled.Download, contentDescription = "Unduh episode", tint = Color.White)
                        }
                    }
                }
                if (dl?.state == "gagal") {
                    Text(
                        "Unduhan gagal — ketuk ikon unduh untuk mencoba lagi.",
                        color = ReaAccent, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 42.dp, top = 2.dp),
                    )
                }
            }
        }
        }
        if (tabKey == "trailer" && hasTrailer) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp)).background(ReaSurface),
                    ) {
                        TrailerPlayer(title.trailer!!, controls = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Trailer — ${title.title}", color = Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Cuplikan resmi dari katalog gudang REA.",
                        color = ReaTextDim, fontSize = 12.sp,
                    )
                }
            }
        }
        if (tabKey == "similar" && similar.isNotEmpty()) {
            item { PosterRow("Judul Serupa", similar, onOpenTitle) }
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
            Modifier.fillMaxWidth().background(Color(0xFF161616))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title.title, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (resume != null) "Lanjutkan E${resume.episode}" else "${title.episodes.size} episode",
                    color = ReaTextDim, fontSize = 12.sp,
                )
            }
            Button(
                onClick = {
                    if (resume != null) onPlay(resume.episode, resume.positionMs)
                    else onPlay(title.episodes.firstOrNull()?.number ?: 1, 0L)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(6.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (resume != null) "Lanjutkan" else "Putar", fontWeight = FontWeight.Bold)
            }
        }
    }
    }
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
        modifier = Modifier.width(76.dp).height(58.dp).clickable(onClick = onClick),
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (active) ReaAccent else Color.White,
            modifier = Modifier.size(26.dp),
        )
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}
