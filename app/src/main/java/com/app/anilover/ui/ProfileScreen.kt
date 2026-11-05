package com.app.anilover.ui

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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.R
import com.app.anilover.Str
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
    /** Status verifikasi email (hanya bermakna bila [showEmailStatus] true). */
    accountVerified: Boolean = true,
    showEmailStatus: Boolean = false,
    /** Non-null bila email belum terverifikasi: kirim ulang email verifikasi. */
    onResendVerification: (() -> Unit)? = null,
    /** Non-null bila Firebase aktif: tombol Keluar (akun) atau Masuk/Daftar (tamu). */
    onLogout: (() -> Unit)? = null,
) {
    var cleared by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val displayName = accountName ?: if (onLogout == null) "Adiyth" else Str.get(R.string.guest)
    val isGuest = accountEmail == null
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) { null }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            ProfileHeader(
                displayName = displayName,
                accountEmail = accountEmail,
                accountVerified = accountVerified,
                showEmailStatus = showEmailStatus,
                onResendVerification = onResendVerification,
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).appearFx(60),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(Icons.Filled.CheckCircle, "$finishedEpisodes", Str.get(R.string.stat_episodes), Modifier.weight(1f))
                StatCard(Icons.Filled.Visibility, "$watchedTitles", Str.get(R.string.stat_titles), Modifier.weight(1f))
                StatCard(Icons.Filled.Bookmark, "${myListTitles.size}", Str.get(R.string.in_my_list), Modifier.weight(1f))
            }
        }
        item { MyListSection(myListTitles, onOpenTitle) }

        // ── Pengaturan putar: tiga kartu terpisah, masing-masing satu keputusan ──
        item {
            SectionTitle(Str.get(R.string.playback_settings))
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

        // ── Riwayat ──
        item {
            SectionTitle(Str.get(R.string.history))
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

        // ── Akun & versi ──
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(28.dp))
                if (onLogout != null) {
                    if (isGuest) {
                        Button(
                            onClick = onLogout,
                            shape = AniloverShapes.Button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
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
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Str.get(R.string.sign_out), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
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

// ───────────────────────── Bagian-bagian layar ─────────────────────────

@Composable
private fun ProfileHeader(
    displayName: String,
    accountEmail: String?,
    accountVerified: Boolean,
    showEmailStatus: Boolean,
    onResendVerification: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(AniloverAccent.copy(alpha = 0.18f), Color.Transparent)))
            .padding(top = 32.dp, bottom = 24.dp)
            .appearFx(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar: cincin gradasi hijau + inisial.
        Box(
            Modifier.size(104.dp)
                .border(
                    3.dp,
                    Brush.linearGradient(listOf(AniloverAccent, AniloverAccentSoft.copy(alpha = 0.25f))),
                    CircleShape,
                )
                .padding(6.dp)
                .background(
                    Brush.linearGradient(listOf(AniloverAccent, AniloverAccentSoft)),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                displayName.trim().take(1).uppercase().ifEmpty { "A" },
                color = AniloverOnAccent, fontSize = 40.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            displayName, color = AniloverTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            accountEmail ?: Str.get(R.string.local_account),
            color = AniloverTextDim, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
        )
        if (showEmailStatus && accountEmail != null) {
            Spacer(Modifier.height(10.dp))
            if (accountVerified) {
                StatusPill(Icons.Filled.Verified, Str.get(R.string.verify_verified), AniloverAccent)
            } else {
                StatusPill(Icons.Filled.ErrorOutline, Str.get(R.string.verify_unverified), AniloverDanger)
                if (onResendVerification != null) {
                    Text(
                        Str.get(R.string.verify_resend),
                        color = AniloverAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(AniloverShapes.Pill)
                            .clickable { onResendVerification() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String, tone: Color) {
    Row(
        Modifier.clip(AniloverShapes.Pill)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.35f), AniloverShapes.Pill)
            .padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = tone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.panel().padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(36.dp).background(AniloverAccent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = AniloverAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, color = AniloverTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            label, color = AniloverTextDim, fontSize = 11.sp, lineHeight = 14.sp,
            textAlign = TextAlign.Center, minLines = 2, maxLines = 2,
        )
    }
}

@Composable
private fun MyListSection(myListTitles: List<Title>, onOpenTitle: (Title) -> Unit) {
    if (myListTitles.isNotEmpty()) {
        PosterRow(Str.get(R.string.my_list), myListTitles, onOpenTitle)
    } else {
        SectionTitle(Str.get(R.string.my_list))
        Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().panel()) {
            EmptyState(
                icon = Icons.Filled.Bookmark,
                title = Str.get(R.string.my_list_empty),
                subtitle = Str.get(R.string.my_list_empty_hint),
            )
        }
    }
}

// ───────────────────────── Komponen kecil ─────────────────────────

/** Kartu pengaturan: ikon + judul + deskripsi (+ kontrol di kanan) dan isi opsional di bawahnya. */
@Composable
private fun SettingCard(
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
private fun SettingsIcon(icon: ImageVector) {
    Box(
        Modifier.size(40.dp).background(AniloverAccent.copy(alpha = 0.12f), AniloverShapes.Card),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AniloverAccent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text, color = AniloverTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Pilihan segmen selebar penuh: semua opsi sama lebar dan satu baris (tidak ada teks yang terpotong). */
@Composable
private fun Segmented(
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
private fun ColorSwatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun SubtitlePreview(size: String, colorKey: String) {
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

private fun subtitleColorOf(key: String): Color = when (key) {
    "kuning" -> Color(0xFFFFE066)
    "hijau" -> AniloverAccent
    else -> Color.White
}
