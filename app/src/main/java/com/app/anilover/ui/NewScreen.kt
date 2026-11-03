package com.app.anilover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.anilover.data.SoonTitle
import com.app.anilover.data.Title

// ---------------- Baru ----------------

@Composable
fun NewScreen(
    titles: List<Title>,
    soon: List<SoonTitle>,
    reminders: Set<String>,
    onToggleReminder: (SoonTitle) -> Unit,
    onOpen: (Title) -> Unit,
) {
    val type = MaterialTheme.typography
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenTitle("Baru & Populer", "Judul terbaru dan yang segera tayang")
        }
        if (soon.isNotEmpty()) {
            item { SectionTitle("Segera Tayang") }
            items(soon) { s ->
                SoonCard(
                    s = s,
                    reminded = reminders.contains(s.id),
                    onToggleReminder = { onToggleReminder(s) },
                )
            }
            item { SectionTitle("Baru Ditambahkan") }
        }
        items(titles.reversed()) { t ->
            Column(
                Modifier.fillMaxWidth().clickable { onOpen(t) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(AniloverShapes.Card).background(AniloverSurface),
                ) {
                    PosterImage(
                        model = t.banner ?: t.poster,
                        contentDescription = t.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.align(Alignment.Center).size(46.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.title, color = AniloverTextPrimary,
                        style = type.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    t.matchPercent?.let {
                        Text("$it%", color = AniloverGreen, style = type.labelLarge)
                    }
                }
                val meta = listOfNotNull(
                    t.year?.toString(), t.statusLabel,
                    if (t.episodes.isNotEmpty()) "${t.episodes.size} Episode" else null,
                    if (t.genres.isNotEmpty()) t.genres.joinToString(", ") else null,
                ).joinToString("  •  ")
                if (meta.isNotBlank()) Text(meta, color = AniloverTextDim, style = type.bodySmall)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Kartu "Segera Tayang": judul masih disalin ke gudang; tombol Ingatkan Saya. */
@Composable
private fun SoonCard(
    s: SoonTitle,
    reminded: Boolean,
    onToggleReminder: () -> Unit,
) {
    val type = MaterialTheme.typography
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(AniloverShapes.Card).background(AniloverSurface),
        ) {
            PosterImage(
                model = s.banner ?: s.poster,
                contentDescription = s.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                "SEGERA", color = AniloverOnAccent,
                style = type.labelSmall.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.align(Alignment.TopStart)
                    .background(AniloverGold, RoundedCornerShape(bottomEnd = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.title, color = AniloverTextPrimary,
                style = type.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            s.matchPercent?.let {
                Text("$it%", color = AniloverGreen, style = type.labelLarge)
            }
        }
        val meta = listOfNotNull(
            s.year?.toString(),
            if (s.totalEpisodes > 0) "${s.totalEpisodes} Episode" else null,
            if (s.genres.isNotEmpty()) s.genres.joinToString(", ") else null,
        ).joinToString("  •  ")
        if (meta.isNotBlank()) Text(meta, color = AniloverTextDim, style = type.bodySmall)
        if (!s.synopsis.isNullOrBlank()) {
            Text(
                s.synopsis, color = AniloverTextDim, style = type.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.clip(AniloverShapes.Button)
                .background(if (reminded) AniloverAccent else AniloverSurfaceHigh)
                .clickable(onClick = onToggleReminder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (reminded) Icons.Filled.Check else Icons.Filled.Notifications,
                contentDescription = null, tint = if (reminded) AniloverOnAccent else Color.White, modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (reminded) "Diingatkan" else "Ingatkan Saya",
                color = if (reminded) AniloverOnAccent else Color.White, style = type.labelLarge,
            )
        }
    }
}
