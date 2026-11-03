package com.app.anilover.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.anilover.data.AniloverSettings
import com.app.anilover.data.Title

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
    val type = MaterialTheme.typography
    var cleared by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Tanpa Firebase (onLogout == null) akun bersifat lokal; nama bawaan netral, bukan nama pribadi.
    val displayName = accountName ?: if (onLogout == null) "Pengguna" else "Tamu"
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(76.dp)
                        .background(
                            Brush.linearGradient(listOf(AniloverAccent, AniloverGold)),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        displayName.trim().take(1).uppercase().ifEmpty { "A" },
                        color = AniloverOnAccent, style = type.headlineLarge,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(displayName, color = AniloverTextPrimary, style = type.titleLarge)
                Text(accountEmail ?: "Akun lokal di perangkat ini", color = AniloverTextDim, style = type.bodySmall)
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
                        Text("Autoplay episode berikutnya", color = AniloverTextPrimary, style = type.bodyLarge)
                        Text("Hitung mundur lalu putar otomatis", color = AniloverTextDim, style = type.bodySmall)
                    }
                    Switch(
                        checked = settings.autoplayNext,
                        onCheckedChange = onAutoplayChange,
                        colors = aniloverSwitchColors(),
                    )
                }
                SettingsDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SettingsIcon(Icons.Filled.Subtitles)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Subtitle bawaan", color = AniloverTextPrimary, style = type.bodyLarge)
                        Text("Dipilih otomatis saat episode mulai diputar", color = AniloverTextDim, style = type.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        ChipChoices(
                            options = listOf("id" to "Indonesia", "en" to "Inggris", "off" to "Nonaktif"),
                            selected = settings.subtitleDefault,
                            onSelect = onSubtitleChange,
                        )
                    }
                }
                SettingsDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SettingsIcon(Icons.Filled.Subtitles)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tampilan Subtitle", color = AniloverTextPrimary, style = type.bodyLarge)
                        Text("Ukuran dan warna teks subtitle di pemutar", color = AniloverTextDim, style = type.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Ukuran", color = AniloverTextDim, style = type.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        ChipChoices(
                            options = listOf("kecil" to "Kecil", "sedang" to "Sedang", "besar" to "Besar"),
                            selected = settings.subtitleSize,
                            onSelect = { onSubtitleStyleChange(it, settings.subtitleColor) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Warna", color = AniloverTextDim, style = type.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        ChipChoices(
                            options = listOf("putih" to "Putih", "kuning" to "Kuning", "hijau" to "Hijau"),
                            selected = settings.subtitleColor,
                            onSelect = { onSubtitleStyleChange(settings.subtitleSize, it) },
                        )
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
                        color = AniloverTextDim, style = type.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bersihkan riwayat tonton", color = AniloverAccent, style = type.labelLarge,
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
                    OutlinedButton(
                        onClick = { if (isGuest) onLogout() else confirmLogout = true },
                        shape = AniloverShapes.Button,
                        border = BorderStroke(1.dp, if (isGuest) AniloverAccent else AniloverOutline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isGuest) AniloverAccent else AniloverTextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(
                            if (isGuest) "Masuk atau daftar" else "Keluar dari akun",
                            style = type.labelLarge,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Text("Anilover versi ${versionName ?: "-"}", color = AniloverTextDim, style = type.bodySmall)
                Spacer(Modifier.height(24.dp))
                if (confirmLogout) {
                    AlertDialog(
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
                            TextButton(onClick = {
                                confirmLogout = false
                                onLogout?.invoke()
                            }) { Text("Keluar", color = AniloverAccent, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmLogout = false }) {
                                Text("Batal", color = AniloverTextDim)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Baris pilihan berbentuk chip (satu terpilih) untuk pengaturan. */
@Composable
private fun ChipChoices(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) {
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = aniloverChipColors(),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = AniloverOutline)
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = AniloverAccent, style = MaterialTheme.typography.headlineLarge)
        Text(label, color = AniloverTextDim, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(
        Modifier.size(38.dp).background(AniloverSurfaceHigh, AniloverShapes.Chip),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

/** Keadaan kosong bergaya Netflix: ikon besar dalam lingkaran + ajakan (opsional: aksi di bawahnya). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    val type = MaterialTheme.typography
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).background(AniloverSurface, CircleShape)
                .border(1.dp, AniloverOutline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AniloverTextDim, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = AniloverTextPrimary, style = type.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = AniloverTextDim, style = type.bodyMedium, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(10.dp))
            action()
        }
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
