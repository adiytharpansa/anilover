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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.anilover.data.AniloverSettings
import com.app.anilover.data.DownloadItem
import com.app.anilover.data.Title

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
    val type = MaterialTheme.typography
    val byId = titles.associateBy { it.id }
    val context = LocalContext.current
    val storage = remember {
        try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            stat.totalBytes to stat.availableBytes
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
                    color = AniloverTextDim, style = type.bodyMedium,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                )
            }
            if (storage.first > 0) {
                val usedFrac = 1f - storage.second.toFloat() / storage.first.toFloat()
                Text(
                    "Penyimpanan perangkat: " + formatSize(storage.second) + " bebas dari " +
                        formatSize(storage.first),
                    color = AniloverTextDim, style = type.bodySmall,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
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
                        Text("Unduhan Cerdas", color = AniloverTextPrimary, style = type.titleSmall)
                        Text(
                            "Episode berikutnya terunduh otomatis; episode yang selesai ditonton terhapus sendiri.",
                            color = AniloverTextDim, style = type.bodySmall,
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
                            Text("Hanya lewat Wi-Fi", color = AniloverTextPrimary, style = type.bodyMedium)
                            Text("Unduhan Cerdas berhenti saat memakai data seluler.", color = AniloverTextDim, style = type.bodySmall)
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
                Box(Modifier.fillMaxWidth().padding(vertical = 36.dp)) {
                    EmptyState(
                        icon = Icons.Filled.Download,
                        title = "Belum ada unduhan",
                        subtitle = "Unduh episode dari halaman detail judul untuk menonton tanpa internet.",
                        action = {
                            Text(
                                "Jelajahi judul", color = AniloverAccent, style = type.labelLarge,
                                modifier = Modifier.clickable { onBrowse() }.padding(8.dp),
                            )
                        },
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
                        d.titleName, color = AniloverTextPrimary, style = type.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Episode ${d.episode} · " + when (d.state) {
                            "selesai" -> formatSize(d.sizeBytes) + " · terunduh"
                            "mengunduh" -> "mengunduh ${(d.fraction * 100).toInt()}%"
                            else -> "gagal — buka detail untuk coba lagi"
                        },
                        color = AniloverTextDim, style = type.bodySmall,
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
