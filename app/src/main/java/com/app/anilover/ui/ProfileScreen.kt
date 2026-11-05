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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.R
import com.app.anilover.Str
import com.app.anilover.data.Title

// ---------------- Profil ----------------
// Pengaturan ada di layar tersendiri (SettingsScreen.kt); Profil hanya memuat identitas,
// statistik, Daftarku, dan pintu masuk ke Pengaturan.

@Composable
fun ProfileScreen(
    myListTitles: List<Title>,
    finishedEpisodes: Int,
    watchedTitles: Int,
    onOpenTitle: (Title) -> Unit,
    onOpenSettings: () -> Unit,
    accountName: String? = null,
    accountEmail: String? = null,
    /** Status verifikasi email (hanya bermakna bila [showEmailStatus] true). */
    accountVerified: Boolean = true,
    showEmailStatus: Boolean = false,
    /** Non-null bila email belum terverifikasi: kirim ulang email verifikasi. */
    onResendVerification: (() -> Unit)? = null,
    /** Non-null bila Firebase aktif: untuk tamu menjadi tombol Masuk/Daftar. */
    onLogout: (() -> Unit)? = null,
) {
    val displayName = accountName ?: if (onLogout == null) "Adiyth" else Str.get(R.string.guest)
    val isGuest = accountEmail == null

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth()) {
                ProfileHeader(
                    displayName = displayName,
                    accountEmail = accountEmail,
                    accountVerified = accountVerified,
                    showEmailStatus = showEmailStatus,
                    onResendVerification = onResendVerification,
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = Str.get(R.string.settings_title), tint = AniloverTextPrimary)
                }
            }
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
        item {
            Spacer(Modifier.height(24.dp))
            // Pintu masuk ke halaman Pengaturan.
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .panel()
                    .clickable(onClick = onOpenSettings)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Filled.Settings)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(Str.get(R.string.settings_title), color = AniloverTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(Str.get(R.string.settings_hint), color = AniloverTextDim, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AniloverTextDim)
            }
            if (isGuest && onLogout != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onLogout,
                    shape = AniloverShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Str.get(R.string.sign_in_or_up), fontWeight = FontWeight.Bold)
                }
            }
        }
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
