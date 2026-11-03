package com.app.anilover.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val type = MaterialTheme.typography
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
    // Chip genre memakai daftar baku yang sama dengan Beranda (tanpa tag seperti "Earth").
    val genres = remember(titles) { cleanGenres(titles.flatMap { it.genres }.distinct()) }
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
        Text("Cari", color = AniloverTextPrimary, style = type.headlineLarge)
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
                    "Pencarian Terakhir", color = AniloverTextPrimary,
                    style = type.titleMedium, modifier = Modifier.weight(1f),
                )
                Text(
                    "Hapus", color = AniloverAccent, style = type.labelLarge,
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
            Text("Sedang Tren Sekarang", color = AniloverTextPrimary, style = type.titleMedium)
            Spacer(Modifier.height(6.dp))
            for ((idx, t) in trending.withIndex()) {
                Row(
                    Modifier.fillMaxWidth().clickable { openSaving(t) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${idx + 1}", color = AniloverAccent,
                        style = type.titleMedium.copy(fontWeight = FontWeight.Black),
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
                        t.title, color = AniloverTextPrimary, style = type.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    t.matchPercent?.let {
                        Text("$it%", color = AniloverGreen, style = type.labelLarge)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) {
            if (query.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada judul untuk filter ini.", color = AniloverTextDim, style = type.bodyLarge)
                }
            } else {
                // Hasil kosong yang membantu: pesan + saran judul (skor tertinggi).
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "Tidak ada hasil untuk \"$query\".",
                        color = AniloverTextPrimary, style = type.titleMedium,
                    )
                    Text(
                        "Periksa ejaan, atau coba salah satu judul populer ini:",
                        color = AniloverTextDim, style = type.bodyMedium,
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
internal fun GridPoster(t: Title, onClick: () -> Unit) {
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
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text(genre, color = AniloverTextPrimary, style = type.titleLarge)
            Spacer(Modifier.width(6.dp))
            Text(
                "${titles.size} judul", color = AniloverTextDim, style = type.bodyMedium,
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
