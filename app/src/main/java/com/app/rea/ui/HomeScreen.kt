package com.app.rea.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.rea.data.Title
import com.app.rea.data.WatchEntry

@Composable
fun HomeScreen(
    titles: List<Title>,
    history: List<WatchEntry>,
    myList: Set<String>,
    hiddenContinue: Set<String>,
    onOpen: (Title) -> Unit,
    onPlay: (Title, Int, Long) -> Unit,
    onToggleMyList: (Title) -> Unit,
    onRemoveContinue: (WatchEntry) -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
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

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "REA", color = ReaAccent, fontSize = 26.sp,
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

        if (featured != null) {
            item {
                Billboard(
                    t = featured,
                    history = history,
                    inMyList = myList.contains(featured.id),
                    onOpen = { onOpen(featured) },
                    onPlay = { ep, pos -> onPlay(featured, ep, pos) },
                    onToggleMyList = { onToggleMyList(featured) },
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

        if (myListTitles.isNotEmpty()) {
            item { PosterRow("Daftarku", myListTitles, onOpen) }
        }

        if (titles.isNotEmpty()) {
            item { PosterRow("Di Gudang", titles, onOpen) }
        }

        if (titles.size >= 2) {
            val topRated = titles.sortedByDescending { it.score ?: 0.0 }
            if (topRated != titles) {
                item { PosterRow("Skor Tertinggi", topRated, onOpen) }
            }
            val releasing = titles.filter { it.status == "RELEASING" }
            if (releasing.isNotEmpty()) {
                item { PosterRow("Sedang Tayang", releasing, onOpen) }
            }
            val finished = titles.filter { it.status == "FINISHED" }
            if (finished.isNotEmpty() && finished.size != titles.size) {
                item { PosterRow("Serial Tamat", finished, onOpen) }
            }
            val genres = titles.flatMap { it.genres }.distinct().take(4)
            for (g in genres) {
                val inGenre = titles.filter { it.genres.contains(g) }
                if (inGenre.isNotEmpty()) {
                    item { PosterRow(g, inGenre, onOpen) }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
fun PosterRow(title: String, list: List<Title>, onOpen: (Title) -> Unit) {
    SectionTitle(title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(list) { t -> PosterCard(t) { onOpen(t) } }
    }
}

@Composable
private fun Billboard(
    t: Title,
    history: List<WatchEntry>,
    inMyList: Boolean,
    onOpen: () -> Unit,
    onPlay: (Int, Long) -> Unit,
    onToggleMyList: () -> Unit,
) {
    val resume = history
        .filter { it.titleId == t.id && it.isStarted && !it.isFinished }
        .maxByOrNull { it.updatedAt }
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 11f)
            .background(ReaSurface),
    ) {
        AsyncImage(
            model = t.banner ?: t.poster,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent,
                        ReaBackground.copy(alpha = 0.55f),
                        ReaBackground,
                    )
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
        ) {
            Text(
                t.title, color = Color.White, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                t.matchPercent?.let {
                    Text("$it% Kecocokan", color = Color(0xFF46D369), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                t.year?.let { Text(it.toString(), color = Color.White, fontSize = 13.sp) }
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
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                if (t.episodes.isNotEmpty()) {
                    Text("${t.episodes.size} Episode", color = Color.White, fontSize = 13.sp)
                }
            }
            if (!t.synopsis.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    t.synopsis, color = Color(0xFFE8E8E8), fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
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
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (resume != null) "Lanjutkan" else "Putar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                BillboardAction(
                    icon = if (inMyList) Icons.Filled.Check else Icons.Filled.Add,
                    label = "Daftarku",
                    onClick = onToggleMyList,
                )
                BillboardAction(
                    icon = Icons.Filled.Info,
                    label = "Detail",
                    onClick = onOpen,
                )
            }
        }
    }
}

@Composable
private fun BillboardAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun PosterCard(t: Title, onClick: () -> Unit) {
    Column(Modifier.width(112.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
        )
        Box(Modifier.height(34.dp)) {
            Text(
                t.title, color = Color.White, fontSize = 12.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun LandscapeCard(t: Title, onClick: () -> Unit) {
    Column(Modifier.width(190.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = t.banner ?: t.poster,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
        )
        Box(Modifier.height(34.dp)) {
            Text(
                t.title, color = Color.White, fontSize = 12.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
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
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Pilihan", tint = Color.White, modifier = Modifier.size(18.dp))
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
