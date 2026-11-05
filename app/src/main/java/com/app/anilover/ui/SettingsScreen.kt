package com.app.anilover.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.R
import com.app.anilover.Str
import com.app.anilover.data.AniloverSettings

// ---------------- Pengaturan (layar tersendiri) ----------------

@Composable
fun SettingsScreen(
    settings: AniloverSettings,
    historyCount: Int,
    accountEmail: String?,
    onBack: () -> Unit,
    onAutoplayChange: (Boolean) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onSubtitleStyleChange: (String, String) -> Unit,
    onSmartChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    /** Non-null bila Firebase aktif: tombol Keluar (akun) atau Masuk/Daftar (tamu). */
    onLogout: (() -> Unit)? = null,
) {
    var cleared by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isGuest = accountEmail == null
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) { null }
    }

    Column(Modifier.fillMaxSize()) {
        // Bilah atas: kembali + judul.
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Str.get(R.string.back), tint = AniloverTextPrimary)
            }
            Text(
                Str.get(R.string.settings_title),
                color = AniloverTextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
            )
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            // ── Pemutaran ──
            item {
                SectionLabel(Str.get(R.string.settings_playback))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingCard(
                        icon = Icons.Filled.PlayCircle,
                        title = Str.get(R.string.autoplay_next),
                        desc = Str.get(R.string.autoplay_next_desc),
                        trailing = {
                            Switch(
                                checked = settings.autoplayNext,
                                onCheckedChange = onAutoplayChange,
                                colors = aniloverSwitchColors(),
                            )
                        },
                    )
                    SettingCard(
                        icon = Icons.Filled.Subtitles,
                        title = Str.get(R.string.default_subtitle),
                        desc = Str.get(R.string.default_subtitle_desc),
                    ) {
                        Segmented(
                            options = listOf(
                                "id" to Str.get(R.string.lang_indonesian),
                                "en" to Str.get(R.string.lang_english),
                                "off" to Str.get(R.string.off),
                            ),
                            selected = settings.subtitleDefault,
                            onSelect = onSubtitleChange,
                        )
                    }
                    SettingCard(
                        icon = Icons.Filled.TextFields,
                        title = Str.get(R.string.subtitle_style),
                        desc = Str.get(R.string.subtitle_style_desc),
                    ) {
                        SubtitlePreview(settings.subtitleSize, settings.subtitleColor)
                        Spacer(Modifier.height(16.dp))
                        GroupLabel(Str.get(R.string.size))
                        Segmented(
                            options = listOf(
                                "kecil" to Str.get(R.string.small),
                                "sedang" to Str.get(R.string.medium),
                                "besar" to Str.get(R.string.large),
                            ),
                            selected = settings.subtitleSize,
                            onSelect = { onSubtitleStyleChange(it, settings.subtitleColor) },
                        )
                        Spacer(Modifier.height(16.dp))
                        GroupLabel(Str.get(R.string.color))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for ((value, label) in listOf(
                                "putih" to Str.get(R.string.white),
                                "kuning" to Str.get(R.string.yellow),
                                "hijau" to Str.get(R.string.green),
                            )) {
                                ColorSwatch(
                                    color = subtitleColorOf(value),
                                    label = label,
                                    selected = settings.subtitleColor == value,
                                    onClick = { onSubtitleStyleChange(settings.subtitleSize, value) },
                                )
                            }
                        }
                    }
                }
            }

            // ── Unduhan ──
            item {
                SectionLabel(Str.get(R.string.nav_downloads))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingCard(
                        icon = Icons.Filled.CloudDownload,
                        title = Str.get(R.string.smart_downloads),
                        desc = Str.get(R.string.smart_downloads_desc),
                        trailing = {
                            Switch(
                                checked = settings.smartDownloads,
                                onCheckedChange = onSmartChange,
                                colors = aniloverSwitchColors(),
                            )
                        },
                    )
                    AnimatedVisibility(visible = settings.smartDownloads) {
                        SettingCard(
                            icon = Icons.Filled.Wifi,
                            title = Str.get(R.string.wifi_only),
                            desc = Str.get(R.string.wifi_only_desc),
                            trailing = {
                                Switch(
                                    checked = settings.wifiOnlyDownloads,
                                    onCheckedChange = onWifiOnlyChange,
                                    colors = aniloverSwitchColors(),
                                )
                            },
                        )
                    }
                }
            }

            // ── Riwayat ──
            item {
                SectionLabel(Str.get(R.string.history))
                val canClear = historyCount > 0 && !cleared
                SettingCard(
                    icon = Icons.Filled.History,
                    title = when {
                        cleared -> Str.get(R.string.history_cleared)
                        historyCount == 0 -> Str.get(R.string.history_none)
                        else -> Str.plural(R.plurals.history_saved, historyCount)
                    },
                    desc = null,
                ) {
                    val tone = if (canClear) AniloverDanger else AniloverTextDim.copy(alpha = 0.5f)
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(AniloverShapes.Button)
                            .border(1.dp, tone.copy(alpha = 0.45f), AniloverShapes.Button)
                            .background(tone.copy(alpha = 0.08f))
                            .clickable(enabled = canClear) {
                                onClearHistory()
                                cleared = true
                            }
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null, tint = tone, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Str.get(R.string.clear_history), color = tone, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Akun ──
            if (onLogout != null) {
                item {
                    SectionLabel(Str.get(R.string.settings_account))
                    SettingCard(
                        icon = Icons.Filled.AccountCircle,
                        title = accountEmail ?: Str.get(R.string.local_account),
                        desc = null,
                    ) {
                        if (isGuest) {
                            Button(
                                onClick = onLogout,
                                shape = AniloverShapes.Button,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                                ),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Str.get(R.string.sign_in_or_up), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { confirmLogout = true },
                                shape = AniloverShapes.Button,
                                border = BorderStroke(1.dp, AniloverDanger.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AniloverDanger),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Str.get(R.string.sign_out), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── Tentang ──
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    Str.get(R.string.version_fmt, versionName ?: "-"),
                    color = AniloverTextDim.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = AniloverSurface,
            title = { Text(Str.get(R.string.sign_out_q), color = AniloverTextPrimary) },
            text = { Text(Str.get(R.string.sign_out_msg), color = AniloverTextDim) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout?.invoke()
                }) { Text(Str.get(R.string.sign_out_btn), color = AniloverDanger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(Str.get(R.string.cancel), color = AniloverTextDim)
                }
            },
        )
    }
}

/** Label bagian kecil berhuruf kapital, memisahkan kelompok pengaturan. */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = AniloverAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
    )
}

// ───────────────────────── Komponen kecil ─────────────────────────

/** Kartu pengaturan: ikon + judul + deskripsi (+ kontrol di kanan) dan isi opsional di bawahnya. */
@Composable
internal fun SettingCard(
    icon: ImageVector,
    title: String,
    desc: String?,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().panel().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIcon(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AniloverTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (desc != null) {
                    Text(desc, color = AniloverTextDim, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (content != null) {
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
internal fun SettingsIcon(icon: ImageVector) {
    Box(
        Modifier.size(40.dp).background(AniloverAccent.copy(alpha = 0.12f), AniloverShapes.Card),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AniloverAccent, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun GroupLabel(text: String) {
    Text(
        text, color = AniloverTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Pilihan segmen selebar penuh: semua opsi sama lebar dan satu baris (tidak ada teks yang terpotong). */
@Composable
internal fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(AniloverShapes.Button)
            .background(AniloverBackground)
            .border(1.dp, AniloverOutline, AniloverShapes.Button)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for ((value, label) in options) {
            val isSelected = value == selected
            val bg by animateColorAsState(
                if (isSelected) AniloverAccent else Color.Transparent, tween(180), label = "segBg",
            )
            val fg by animateColorAsState(
                if (isSelected) AniloverOnAccent else AniloverTextDim, tween(180), label = "segFg",
            )
            Box(
                Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, color = fg, fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun ColorSwatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(AniloverShapes.Card).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(46.dp).border(2.dp, if (selected) AniloverAccent else AniloverOutline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(32.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label, fontSize = 12.sp,
            color = if (selected) AniloverTextPrimary else AniloverTextDim,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Pratinjau langsung teks subtitle di atas "adegan" gelap, mengikuti ukuran & warna terpilih. */
@Composable
internal fun SubtitlePreview(size: String, colorKey: String) {
    Box(
        Modifier.fillMaxWidth().height(92.dp)
            .clip(AniloverShapes.Card)
            .background(Brush.verticalGradient(listOf(Color(0xFF2C2F36), Color(0xFF0B0B0B))))
            .border(1.dp, AniloverOutline, AniloverShapes.Card),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            Str.get(R.string.subtitle_preview),
            color = subtitleColorOf(colorKey),
            fontSize = when (size) { "kecil" -> 12.sp; "besar" -> 19.sp; else -> 15.sp },
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 2f), 6f)),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

internal fun subtitleColorOf(key: String): Color = when (key) {
    "kuning" -> Color(0xFFFFE066)
    "hijau" -> AniloverAccent
    else -> Color.White
}
