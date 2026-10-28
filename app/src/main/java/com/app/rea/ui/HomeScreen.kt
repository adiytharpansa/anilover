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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onOpen: (Title) -> Unit,
    onPlay: (Title, Int, Long) -> Unit,
) {
    val byId = titles.associateBy { it.id }
    val continueWatching = history
        .filter { it.isStarted && !it.isFinished && byId.containsKey(it.titleId) }
        .sortedByDescending { it.updatedAt }
    val featured = titles.firstOrNull()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "REA", color = ReaAccent, fontSize = 26.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                )
                Spacer(Modifier.weight(1f))
                Text("Gudang project-X", color = ReaTextDim, fontSize = 12.sp)
            }
        }

        if (featured != null) {
            item { Billboard(featured, history, onOpen, onPlay) }
        }

        if (continueWatching.isNotEmpty()) {
            item {
                RowTitle("Lanjutkan Menonton")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(continueWatching) { entry ->
                        val t = byId.getValue(entry.titleId)
                        ContinueCard(t, entry) { onPlay(t, entry.episode, entry.positionMs) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (titles.isNotEmpty()) {
            item {
                RowTitle("Di Gudang")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(titles) { t -> PosterCard(t) { onOpen(t) } }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Billboard(
    t: Title,
    history: List<WatchEntry>,
    onOpen: (Title) -> Unit,
    onPlay: (Title, Int, Long) -> Unit,
) {
    val resume = history
        .filter { it.titleId == t.id && it.isStarted && !it.isFinished }
        .maxByOrNull { it.updatedAt }
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 10f)
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
                    listOf(Color.Transparent, Color.Transparent, ReaBackground),
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) {
            Text(
                t.title, color = Color.White, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                t.score?.let { "★ " + String.format("%.1f", it) },
                t.year?.toString(),
                t.status,
                t.episodes.size.let { "$it Episode" },
            ).joinToString("  •  ")
            if (meta.isNotBlank()) {
                Text(meta, color = ReaTextDim, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (resume != null) onPlay(t, resume.episode, resume.positionMs)
                        else onPlay(t, t.episodes.firstOrNull()?.number ?: 1, 0L)
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
                OutlinedButton(
                    onClick = { onOpen(t) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Detail")
                }
            }
        }
    }
}

@Composable
private fun RowTitle(text: String) {
    Text(
        text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
    )
}

@Composable
private fun PosterCard(t: Title, onClick: () -> Unit) {
    Column(Modifier.width(112.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
        )
        Text(
            t.title, color = Color.White, fontSize = 12.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ContinueCard(t: Title, entry: WatchEntry, onClick: () -> Unit) {
    Column(Modifier.width(190.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
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
        Text(
            "${t.title} · E${entry.episode}", color = Color.White, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "Sisa " + formatRemaining(entry.positionMs, entry.durationMs),
            color = ReaTextDim, fontSize = 11.sp,
        )
    }
}
