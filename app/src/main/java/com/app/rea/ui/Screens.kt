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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.rea.data.DownloadItem
import com.app.rea.data.ReaSettings
import com.app.rea.data.Title

// ---------------- Cari ----------------

@Composable
fun SearchScreen(
    titles: List<Title>,
    onOpen: (Title) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf<String?>(null) }
    val genres = remember(titles) { titles.flatMap { it.genres }.distinct() }
    val results = titles.filter { t ->
        (genre == null || t.genres.contains(genre)) &&
            (query.isBlank() || t.title.contains(query, ignoreCase = true) ||
                t.genres.any { it.contains(query, ignoreCase = true) })
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("Cari", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Cari judul atau genre…", color = ReaTextDim) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ReaAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                cursorColor = ReaAccent,
            ),
        )
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres) { g ->
                    FilterChip(
                        selected = genre == g,
                        onClick = { genre = if (genre == g) null else g },
                        label = { Text(g) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ReaAccent,
                            selectedLabelColor = Color.White,
                            labelColor = Color.White,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Tidak ada hasil untuk \"$query\".\nCoba kata kunci lain.",
                    color = ReaTextDim, fontSize = 14.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results) { t -> GridPoster(t) { onOpen(t) } }
            }
        }
    }
}

@Composable
private fun GridPoster(t: Title, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
        )
        Text(
            t.title, color = Color.White, fontSize = 11.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------------- Baru ----------------

@Composable
fun NewScreen(
    titles: List<Title>,
    onOpen: (Title) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Baru & Populer", color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        items(titles) { t ->
            Column(
                Modifier.fillMaxWidth().clickable { onOpen(t) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)).background(ReaSurface),
                ) {
                    AsyncImage(
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
                        t.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    t.matchPercent?.let {
                        Text("$it%", color = Color(0xFF46D369), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val meta = listOfNotNull(
                    t.year?.toString(), t.statusLabel,
                    if (t.episodes.isNotEmpty()) "${t.episodes.size} Episode" else null,
                    if (t.genres.isNotEmpty()) t.genres.joinToString(", ") else null,
                ).joinToString("  •  ")
                if (meta.isNotBlank()) Text(meta, color = ReaTextDim, fontSize = 12.sp)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------- Unduhan ----------------

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    titles: List<Title>,
    onPlayDownload: (DownloadItem) -> Unit,
    onDeleteDownload: (DownloadItem) -> Unit,
    onBrowse: () -> Unit,
) {
    val byId = titles.associateBy { it.id }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Unduhan", color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            val done = downloads.filter { it.state == "selesai" }
            if (done.isNotEmpty()) {
                Text(
                    "${done.size} episode terunduh · " + formatSize(done.sumOf { it.sizeBytes }),
                    color = ReaTextDim, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
        }
        if (downloads.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = ReaTextDim, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada unduhan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Unduh episode dari halaman detail judul untuk menonton tanpa internet.",
                        color = ReaTextDim, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Jelajahi judul", color = ReaAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBrowse() },
                    )
                }
            }
        }
        items(downloads) { d ->
            val t = byId[d.titleId]
            Row(
                Modifier.fillMaxWidth()
                    .clickable(enabled = d.state == "selesai") { onPlayDownload(d) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = t?.poster ?: t?.banner,
                    contentDescription = d.titleName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(52.dp).aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(6.dp)).background(ReaSurface),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        d.titleName, color = Color.White, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Episode ${d.episode} · " + when (d.state) {
                            "selesai" -> formatSize(d.sizeBytes) + " · terunduh"
                            "mengunduh" -> "mengunduh ${(d.fraction * 100).toInt()}%"
                            else -> "gagal — buka detail untuk coba lagi"
                        },
                        color = ReaTextDim, fontSize = 12.sp,
                    )
                    if (d.state == "mengunduh") {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(4.dp)
                                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                        ) {
                            Box(Modifier.fillMaxWidth(d.fraction).height(4.dp).background(ReaAccent, RoundedCornerShape(2.dp)))
                        }
                    }
                }
                if (d.state == "mengunduh") {
                    CircularProgressIndicator(color = ReaAccent, modifier = Modifier.size(22.dp))
                } else {
                    IconButton(onClick = { onDeleteDownload(d) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus unduhan", tint = Color.White)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------- Profil ----------------

@Composable
fun ProfileScreen(
    myListTitles: List<Title>,
    historyCount: Int,
    settings: ReaSettings,
    onOpenTitle: (Title) -> Unit,
    onAutoplayChange: (Boolean) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    var cleared by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(72.dp).background(ReaAccent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                Text("Adiyth", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Akun lokal di perangkat ini", color = ReaTextDim, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (myListTitles.isNotEmpty()) {
            item { PosterRow("Daftarku", myListTitles, onOpenTitle) }
        }
        item {
            SectionTitle("Pengaturan Putar")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Autoplay episode berikutnya", color = Color.White, fontSize = 14.sp)
                    Text("Hitung mundur lalu putar otomatis", color = ReaTextDim, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.autoplayNext,
                    onCheckedChange = onAutoplayChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ReaAccent),
                )
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("Subtitle bawaan", color = Color.White, fontSize = 14.sp)
                Text("Dipilih otomatis saat episode mulai diputar", color = ReaTextDim, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((value, label) in listOf("id" to "Indonesia", "en" to "Inggris", "off" to "Nonaktif")) {
                        FilterChip(
                            selected = settings.subtitleDefault == value,
                            onClick = { onSubtitleChange(value) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ReaAccent,
                                selectedLabelColor = Color.White,
                                labelColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }
        item {
            SectionTitle("Riwayat")
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    if (cleared) "Riwayat sudah dibersihkan."
                    else "$historyCount entri riwayat tonton tersimpan di perangkat ini.",
                    color = ReaTextDim, fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bersihkan riwayat tonton", color = ReaAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onClearHistory()
                        cleared = true
                    },
                )
                Spacer(Modifier.height(28.dp))
                Text("REA versi 2.0.0", color = ReaTextDim, fontSize = 12.sp)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
