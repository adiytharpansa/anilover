package com.app.rea.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.rea.data.Title
import com.app.rea.data.WatchEntry

@Composable
fun DetailScreen(
    title: Title,
    history: List<WatchEntry>,
    onBack: () -> Unit,
    onPlay: (Int, Long) -> Unit,
) {
    val progressByEp = history.filter { it.titleId == title.id }
        .associateBy { it.episode }
    val resume = progressByEp.values
        .filter { it.isStarted && !it.isFinished }
        .maxByOrNull { it.updatedAt }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(ReaSurface)) {
                AsyncImage(
                    model = title.banner ?: title.poster,
                    contentDescription = title.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, ReaBackground),
                        )
                    )
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text(
                    title.title, color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                val meta = listOfNotNull(
                    title.score?.let { "★ " + String.format("%.1f", it) },
                    title.year?.toString(),
                    title.status,
                    if (title.genres.isNotEmpty()) title.genres.joinToString(", ") else null,
                ).joinToString("  •  ")
                if (meta.isNotBlank()) Text(meta, color = ReaTextDim, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
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
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!title.synopsis.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(title.synopsis, color = Color(0xFFDDDDDD), fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("Episode", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
            }
        }
        items(title.episodes) { ep ->
            val prog = progressByEp[ep.number]
            Row(
                Modifier.fillMaxWidth().clickable {
                    onPlay(ep.number, if (prog != null && prog.isStarted && !prog.isFinished) prog.positionMs else 0L)
                }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.width(40.dp).height(40.dp)
                        .background(ReaSurface, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Episode ${ep.number}", color = Color.White, fontSize = 15.sp)
                    if (prog != null && prog.isFinished) {
                        Text("Selesai ditonton", color = ReaTextDim, fontSize = 12.sp)
                    } else if (prog != null && prog.isStarted) {
                        Text(
                            "Lanjutkan dari " + formatClock(prog.positionMs) +
                                " · sisa " + formatRemaining(prog.positionMs, prog.durationMs),
                            color = ReaTextDim, fontSize = 12.sp,
                        )
                    }
                }
                if (prog != null && prog.isStarted && !prog.isFinished) {
                    Box(
                        Modifier.width(54.dp).height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(prog.fraction).height(4.dp)
                                .background(ReaAccent, RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
