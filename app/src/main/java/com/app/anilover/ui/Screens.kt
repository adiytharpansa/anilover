package com.app.anilover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.anilover.data.DownloadItem
import com.app.anilover.data.AniloverSettings
import com.app.anilover.data.SoonTitle
import com.app.anilover.data.Title

// ---------------- Cari ----------------

@Composable
fun SearchScreen(
    titles: List<Title>,
    onOpen: (Title) -> Unit,
    recents: List<String> = emptyList(),
    onSaveRecent: (String) -> Unit = {},
    onClearRecents: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // Pencarian suara bawaan Android (tanpa izin tambahan; lewat intent sistem).
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) query = spoken
        }
    }
    fun openSaving(t: Title) {
        if (query.isNotBlank()) onSaveRecent(query)
        onOpen(t)
    }
    val genres = remember(titles) { titles.flatMap { it.genres }.distinct() }
    val results = titles.filter { t ->
        (genre == null || t.genres.contains(genre)) &&
            (query.isBlank() || t.title.contains(query, ignoreCase = true) ||
                t.genres.any { it.contains(query, ignoreCase = true) })
    }
    // "Sedang Tren Sekarang" ala Netflix: tampil saat kolom masih kosong.
    val trending = remember(titles) {
        titles.sortedByDescending { it.score ?: 0.0 }.take(5)
    }
    // Saran untuk hasil kosong. WAJIB di tingkat atas: remember di dalam cabang
    // kondisional merusak tabel slot Compose dan membuat aplikasi keluar sendiri
    // saat hasil pencarian berubah kosong<->ada (perbaikan 2.6.1).
    val suggestions = remember(titles) {
        titles.sortedByDescending { it.score ?: 0.0 }.take(6)
    }
    val showTrending = query.isBlank() && genre == null && trending.size >= 3
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("Cari", color = AniloverTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Cari judul atau genre…", color = AniloverTextDim) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
            trailingIcon = {
                IconButton(onClick = {
                    try {
                        voiceLauncher.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan judul anime…")
                            },
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Pencarian suara tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = "Pencarian suara", tint = Color.White)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotBlank()) onSaveRecent(query)
            }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = AniloverShapes.Button,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = AniloverSurfaceHigh,
                unfocusedContainerColor = AniloverSurface,
                focusedBorderColor = AniloverAccent,
                unfocusedBorderColor = AniloverOutline,
                cursorColor = AniloverAccent,
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
                        colors = aniloverChipColors(),
                    )
                }
            }
        }
        if (query.isBlank() && genre == null && recents.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pencarian Terakhir", color = AniloverTextPrimary, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                )
                Text(
                    "Hapus", color = AniloverAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearRecents() }.padding(4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recents) { r ->
                    FilterChip(
                        selected = false,
                        onClick = { query = r },
                        label = { Text(r) },
                        leadingIcon = {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White,
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            }
        }
        if (showTrending) {
            Spacer(Modifier.height(14.dp))
            Text("Sedang Tren Sekarang", color = AniloverTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            for ((idx, t) in trending.withIndex()) {
                Row(
                    Modifier.fillMaxWidth().clickable { openSaving(t) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${idx + 1}", color = AniloverAccent, fontSize = 17.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.width(30.dp),
                    )
                    PosterImage(
                        model = t.poster ?: t.banner,
                        contentDescription = t.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(40.dp).aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(4.dp)).background(AniloverSurface),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        t.title, color = AniloverTextPrimary, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    t.matchPercent?.let {
                        Text("$it%", color = AniloverGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) {
            if (query.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada judul untuk filter ini.", color = AniloverTextDim, fontSize = 14.sp)
                }
            } else {
                // Hasil kosong yang membantu: pesan + saran judul (skor tertinggi).
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "Tidak ada hasil untuk \"$query\".",
                        color = AniloverTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Periksa ejaan, atau coba salah satu judul populer ini:",
                        color = AniloverTextDim, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(suggestions) { t -> GridPoster(t) { openSaving(t) } }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results) { t -> GridPoster(t) { openSaving(t) } }
            }
        }
    }
}

@Composable
private fun GridPoster(t: Title, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    Column(
        Modifier.pressScaleFx(pressSource)
            .clickable(interactionSource = pressSource, indication = LocalIndication.current, onClick = onClick),
    ) {
        PosterImage(
            model = t.poster ?: t.banner,
            contentDescription = t.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(AniloverShapes.Poster).background(AniloverSurface),
        )
        CardTitles(t)
    }
}

// ---------------- Kategori (halaman genre dari chip Beranda) ----------------

@Composable
fun CategoryScreen(
    genre: String,
    titles: List<Title>,
    onOpen: (Title) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text(genre, color = AniloverTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(
                "${titles.size} judul", color = AniloverTextDim, fontSize = 13.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(titles) { t -> GridPoster(t) { onOpen(t) } }
        }
    }
}

// ---------------- Baru ----------------

@Composable
fun NewScreen(
    titles: List<Title>,
    soon: List<SoonTitle>,
    reminders: Set<String>,
    onToggleReminder: (SoonTitle) -> Unit,
    onOpen: (Title) -> Unit,
) {
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
                        t.title, color = AniloverTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    t.matchPercent?.let {
                        Text("$it%", color = AniloverGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val meta = listOfNotNull(
                    t.year?.toString(), t.statusLabel,
                    if (t.episodes.isNotEmpty()) "${t.episodes.size} Episode" else null,
                    if (t.genres.isNotEmpty()) t.genres.joinToString(", ") else null,
                ).joinToString("  •  ")
                if (meta.isNotBlank()) Text(meta, color = AniloverTextDim, fontSize = 12.sp)
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
                "SEGERA", color = AniloverOnAccent, fontSize = 10.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.TopStart)
                    .background(AniloverGold, RoundedCornerShape(bottomEnd = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.title, color = AniloverTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            s.matchPercent?.let {
                Text("$it%", color = AniloverGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        val meta = listOfNotNull(
            s.year?.toString(),
            if (s.totalEpisodes > 0) "${s.totalEpisodes} Episode" else null,
            if (s.genres.isNotEmpty()) s.genres.joinToString(", ") else null,
        ).joinToString("  •  ")
        if (meta.isNotBlank()) Text(meta, color = AniloverTextDim, fontSize = 12.sp)
        if (!s.synopsis.isNullOrBlank()) {
            Text(
                s.synopsis, color = AniloverTextDim, fontSize = 12.sp,
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
                color = if (reminded) AniloverOnAccent else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ---------------- Unduhan ----------------

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    titles: List<Title>,
    settings: AniloverSettings,
    onPlayDownload: (DownloadItem) -> Unit,
    onDeleteDownload: (DownloadItem) -> Unit,
    onSmartChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onBrowse: () -> Unit,
) {
    val byId = titles.associateBy { it.id }
    val context = LocalContext.current
    val storage = remember {
        try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            val total = stat.totalBytes
            val free = stat.availableBytes
            total to free
        } catch (e: Exception) {
            0L to 0L
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenTitle("Unduhan")
            val done = downloads.filter { it.state == "selesai" }
            if (done.isNotEmpty()) {
                Text(
                    "${done.size} episode terunduh · " + formatSize(done.sumOf { it.sizeBytes }),
                    color = AniloverTextDim, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            if (storage.first > 0) {
                val usedFrac = 1f - storage.second.toFloat() / storage.first.toFloat()
                Text(
                    "Penyimpanan perangkat: " + formatSize(storage.second) + " bebas dari " +
                        formatSize(storage.first),
                    color = AniloverTextDim, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                )
                Box(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(6.dp)
                        .background(AniloverSurfaceHigh, AniloverShapes.Pill),
                ) {
                    Box(
                        Modifier.fillMaxWidth(usedFrac.coerceIn(0f, 1f)).height(6.dp)
                            .background(AniloverAccent, AniloverShapes.Pill),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().panel()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Unduhan Cerdas", color = AniloverTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Episode berikutnya terunduh otomatis; episode yang selesai ditonton terhapus sendiri.",
                            color = AniloverTextDim, fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = settings.smartDownloads,
                        onCheckedChange = onSmartChange,
                        colors = aniloverSwitchColors(),
                    )
                }
                if (settings.smartDownloads) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hanya lewat Wi-Fi", color = AniloverTextPrimary, fontSize = 13.sp)
                            Text("Unduhan Cerdas berhenti saat memakai data seluler.", color = AniloverTextDim, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.wifiOnlyDownloads,
                            onCheckedChange = onWifiOnlyChange,
                            colors = aniloverSwitchColors(),
                        )
                    }
                }
            }
        }
        if (downloads.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(88.dp).background(AniloverSurface, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, AniloverOutline, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = AniloverTextDim, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Belum ada unduhan", color = AniloverTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Unduh episode dari halaman detail judul untuk menonton tanpa internet.",
                        color = AniloverTextDim, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Jelajahi judul", color = AniloverAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
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
                PosterImage(
                    model = t?.poster ?: t?.banner,
                    contentDescription = d.titleName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(52.dp).aspectRatio(2f / 3f)
                        .clip(AniloverShapes.Chip).background(AniloverSurface),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        d.titleName, color = AniloverTextPrimary, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Episode ${d.episode} · " + when (d.state) {
                            "selesai" -> formatSize(d.sizeBytes) + " · terunduh"
                            "mengunduh" -> "mengunduh ${(d.fraction * 100).toInt()}%"
                            else -> "gagal — buka detail untuk coba lagi"
                        },
                        color = AniloverTextDim, fontSize = 12.sp,
                    )
                    if (d.state == "mengunduh") {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(5.dp)
                                .background(AniloverSurfaceHigh, AniloverShapes.Pill),
                        ) {
                            Box(Modifier.fillMaxWidth(d.fraction).height(5.dp).background(AniloverAccent, AniloverShapes.Pill))
                        }
                    }
                }
                if (d.state == "mengunduh") {
                    CircularProgressIndicator(color = AniloverAccent, modifier = Modifier.size(22.dp))
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
    finishedEpisodes: Int,
    watchedTitles: Int,
    settings: AniloverSettings,
    onOpenTitle: (Title) -> Unit,
    onAutoplayChange: (Boolean) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onSubtitleStyleChange: (String, String) -> Unit,
    onClearHistory: () -> Unit,
    accountName: String? = null,
    accountEmail: String? = null,
    /** Non-null bila Firebase aktif: tombol Keluar (akun) atau Masuk/Daftar (tamu). */
    onLogout: (() -> Unit)? = null,
) {
    var cleared by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val displayName = accountName ?: if (onLogout == null) "Adiyth" else "Tamu"
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(76.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(AniloverAccent, AniloverGold)
                            ),
                            RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        displayName.trim().take(1).uppercase().ifEmpty { "A" },
                        color = AniloverOnAccent, fontSize = 34.sp, fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(displayName, color = AniloverTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(accountEmail ?: "Akun lokal di perangkat ini", color = AniloverTextDim, fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).panel().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn("$finishedEpisodes", "Episode selesai")
                StatColumn("$watchedTitles", "Judul ditonton")
                StatColumn("${myListTitles.size}", "Di Daftarku")
            }
            Spacer(Modifier.height(8.dp))
        }
        item { PosterRowOrEmpty(myListTitles, onOpenTitle) }
        item {
            SectionTitle("Pengaturan Putar")
            Column(Modifier.padding(horizontal = 20.dp).panel()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Filled.PlayCircle)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Autoplay episode berikutnya", color = AniloverTextPrimary, fontSize = 14.sp)
                    Text("Hitung mundur lalu putar otomatis", color = AniloverTextDim, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.autoplayNext,
                    onCheckedChange = onAutoplayChange,
                    colors = aniloverSwitchColors(),
                )
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp, color = AniloverOutline,
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingsIcon(Icons.Filled.Subtitles)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Subtitle bawaan", color = AniloverTextPrimary, fontSize = 14.sp)
                    Text("Dipilih otomatis saat episode mulai diputar", color = AniloverTextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((value, label) in listOf("id" to "Indonesia", "en" to "Inggris", "off" to "Nonaktif")) {
                            FilterChip(
                                selected = settings.subtitleDefault == value,
                                onClick = { onSubtitleChange(value) },
                                label = { Text(label) },
                                colors = aniloverChipColors(),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp, color = AniloverOutline,
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingsIcon(Icons.Filled.Subtitles)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tampilan Subtitle", color = AniloverTextPrimary, fontSize = 14.sp)
                    Text("Ukuran dan warna teks subtitle di pemutar", color = AniloverTextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Ukuran", color = AniloverTextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((value, label) in listOf("kecil" to "Kecil", "sedang" to "Sedang", "besar" to "Besar")) {
                            FilterChip(
                                selected = settings.subtitleSize == value,
                                onClick = { onSubtitleStyleChange(value, settings.subtitleColor) },
                                label = { Text(label) },
                                colors = aniloverChipColors(),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Warna", color = AniloverTextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((value, label) in listOf("putih" to "Putih", "kuning" to "Kuning", "hijau" to "Hijau")) {
                            FilterChip(
                                selected = settings.subtitleColor == value,
                                onClick = { onSubtitleStyleChange(settings.subtitleSize, value) },
                                label = { Text(label) },
                                colors = aniloverChipColors(),
                            )
                        }
                    }
                }
            }
            }
        }
        item {
            SectionTitle("Riwayat")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingsIcon(Icons.Filled.History)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            cleared -> "Riwayat sudah dibersihkan."
                            historyCount == 0 -> "Belum ada riwayat tonton."
                            else -> "$historyCount riwayat tonton tersimpan."
                        },
                        color = AniloverTextDim, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bersihkan riwayat tonton", color = AniloverAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onClearHistory()
                            cleared = true
                        },
                    )
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(28.dp))
                val versionName = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (e: Exception) { null }
                }
                if (onLogout != null) {
                    val isGuest = accountEmail == null
                    androidx.compose.material3.OutlinedButton(
                        onClick = { if (isGuest) onLogout() else confirmLogout = true },
                        shape = AniloverShapes.Button,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, if (isGuest) AniloverAccent else AniloverOutline,
                        ),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isGuest) AniloverAccent else AniloverTextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(
                            if (isGuest) "Masuk atau daftar" else "Keluar dari akun",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Text("Anilover versi ${versionName ?: "-"}", color = AniloverTextDim, fontSize = 12.sp)
                Spacer(Modifier.height(24.dp))
                if (confirmLogout) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { confirmLogout = false },
                        containerColor = AniloverSurface,
                        title = { Text("Keluar dari akun?", color = AniloverTextPrimary) },
                        text = {
                            Text(
                                "Kamu bisa masuk lagi kapan saja dengan email dan kata sandi.",
                                color = AniloverTextDim,
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                confirmLogout = false
                                onLogout?.invoke()
                            }) { Text("Keluar", color = AniloverAccent, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { confirmLogout = false }) {
                                Text("Batal", color = AniloverTextDim)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = AniloverAccent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = AniloverTextDim, fontSize = 11.sp)
    }
}

@Composable
private fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier.size(38.dp).background(AniloverSurfaceHigh, AniloverShapes.Chip),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

/** Keadaan kosong bergaya Netflix: ikon besar dalam lingkaran + ajakan. */
@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).background(AniloverSurface, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, AniloverOutline, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AniloverTextDim, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = AniloverTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle, color = AniloverTextDim, fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun PosterRowOrEmpty(myListTitles: List<Title>, onOpenTitle: (Title) -> Unit) {
    if (myListTitles.isNotEmpty()) {
        PosterRow("Daftarku", myListTitles, onOpenTitle)
    } else {
        SectionTitle("Daftarku")
        EmptyState(
            icon = Icons.Filled.PlayCircle,
            title = "Daftarku masih kosong",
            subtitle = "Tambahkan judul dari tombol + di beranda atau halaman detail supaya mudah ditemukan lagi.",
        )
    }
}
