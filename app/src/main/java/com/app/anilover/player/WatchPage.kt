package com.app.anilover.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.data.AuthUser
import com.app.anilover.data.Comment
import com.app.anilover.data.CommentRepository
import com.app.anilover.data.CommentsState
import com.app.anilover.ui.AniloverAccent
import com.app.anilover.ui.AniloverBackground
import com.app.anilover.ui.AniloverOnAccent
import com.app.anilover.ui.AniloverOutline
import com.app.anilover.ui.AniloverShapes
import com.app.anilover.ui.AniloverSurface
import com.app.anilover.ui.AniloverSurfaceHigh
import com.app.anilover.ui.AniloverTextDim
import com.app.anilover.ui.AniloverTextPrimary

private enum class CommentSort { Top, New }

/**
 * Halaman di bawah video (mode potret): judul + info, aksi, daftar episode, lalu komentar
 * dengan balasan, suka, urutan Teratas/Terbaru, dan kolom tulis tersemat di bawah.
 */
@Composable
internal fun WatchPage(
    title: String,
    meta: String,
    currentEp: Int,
    episodes: List<EpRef>,
    onSelectEpisode: (EpRef) -> Unit,
    onSeeAllEpisodes: () -> Unit,
    inMyList: Boolean,
    rating: Int,
    onToggleMyList: () -> Unit,
    onRate: (Int) -> Unit,
    onShare: () -> Unit,
    thread: String,
    commentsState: CommentsState,
    user: AuthUser?,
    onNeedLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sort by rememberSaveable { mutableStateOf(CommentSort.Top) }
    var replyTo by remember { mutableStateOf<Comment?>(null) }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var toDelete by remember { mutableStateOf<Comment?>(null) }

    // Ganti episode -> thread komentar berganti; bersihkan keadaan balasan.
    LaunchedEffect(thread) { replyTo = null; expanded = emptySet() }

    val all = (commentsState as? CommentsState.Ready)?.items ?: emptyList()
    val tops = all.filter { it.parentId == null }.let { list ->
        when (sort) {
            CommentSort.Top -> list.sortedWith(
                compareByDescending<Comment> { it.likes }.thenByDescending { it.createdAt }
            )
            CommentSort.New -> list.sortedByDescending { it.createdAt }
        }
    }
    val topIds = tops.map { it.id }.toSet()
    val repliesByParent = all.filter { it.parentId != null && it.parentId in topIds }
        .groupBy { it.parentId!! }
        .mapValues { (_, v) -> v.sortedBy { it.createdAt } }
    val totalCount = tops.size + repliesByParent.values.sumOf { it.size }

    Column(modifier.background(AniloverBackground)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item(key = "info") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        title, color = AniloverTextPrimary, fontSize = 19.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Episode $currentEp" + if (episodes.size > 1) " dari ${episodes.size}" else "",
                        color = AniloverAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    )
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(meta, color = AniloverTextDim, fontSize = 12.sp, maxLines = 2)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        WatchAction(
                            icon = if (inMyList) Icons.Filled.Check else Icons.Filled.Add,
                            label = if (inMyList) "Di Daftarku" else "Daftarku",
                            active = inMyList, onClick = onToggleMyList,
                        )
                        WatchAction(
                            icon = Icons.Filled.ThumbDown, label = "Tidak Suka",
                            active = rating == -1,
                            onClick = { onRate(if (rating == -1) 0 else -1) },
                        )
                        WatchAction(
                            icon = Icons.Filled.ThumbUp,
                            label = when (rating) { 2 -> "Sangat Suka"; 1 -> "Disukai"; else -> "Sukai" },
                            active = rating > 0,
                            onClick = { onRate(when (rating) { 1 -> 2; 2 -> 0; else -> 1 }) },
                        )
                        WatchAction(Icons.Filled.Share, "Bagikan", false, onShare)
                    }
                }
            }

            if (episodes.size > 1) {
                item(key = "episodes") {
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Episode", color = AniloverTextPrimary, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Semua", color = AniloverTextDim, fontSize = 13.sp,
                                modifier = Modifier.clickable(onClick = onSeeAllEpisodes)
                                    .padding(vertical = 6.dp),
                            )
                        }
                        val rowState = rememberLazyListState()
                        LaunchedEffect(currentEp) {
                            val idx = episodes.indexOfFirst { it.number == currentEp }
                            if (idx >= 0) rowState.animateScrollToItem((idx - 1).coerceAtLeast(0))
                        }
                        LazyRow(
                            state = rowState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(episodes, key = { it.number }) { ep ->
                                val cur = ep.number == currentEp
                                Box(
                                    Modifier.size(width = 64.dp, height = 40.dp)
                                        .clip(AniloverShapes.Chip)
                                        .background(if (cur) AniloverAccent else AniloverSurfaceHigh)
                                        .clickable { if (!cur) onSelectEpisode(ep) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${ep.number}",
                                        color = if (cur) AniloverOnAccent else AniloverTextPrimary,
                                        fontWeight = if (cur) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "divider") {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(AniloverSurface))
            }

            // Kepala komentar + urutan
            item(key = "comments-head") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Komentar", color = AniloverTextPrimary, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (commentsState is CommentsState.Ready) {
                        Spacer(Modifier.width(8.dp))
                        Text("$totalCount", color = AniloverTextDim, fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    SortChip("Teratas", sort == CommentSort.Top) { sort = CommentSort.Top }
                    Spacer(Modifier.width(6.dp))
                    SortChip("Terbaru", sort == CommentSort.New) { sort = CommentSort.New }
                }
            }

            when (commentsState) {
                CommentsState.Loading -> item(key = "c-loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = AniloverAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp),
                        )
                    }
                }
                CommentsState.Unavailable -> item(key = "c-off") {
                    CommentNotice("Komentar belum aktif. Pasang Firebase (lihat PANDUAN-FIREBASE.md) untuk mengaktifkannya.")
                }
                is CommentsState.Error -> item(key = "c-err") { CommentNotice(commentsState.message) }
                is CommentsState.Ready -> {
                    if (tops.isEmpty()) {
                        item(key = "c-empty") {
                            CommentNotice("Belum ada komentar. Jadilah yang pertama berkomentar!")
                        }
                    } else {
                        items(tops, key = { it.id }) { c ->
                            val replies = repliesByParent[c.id].orEmpty()
                            val open = c.id in expanded
                            Column(Modifier.fillMaxWidth()) {
                                CommentRow(
                                    c = c, me = user, isReply = false,
                                    onLike = { u -> CommentRepository.toggleLike(c.id, u.uid, u.uid in c.likedBy) },
                                    onReply = {
                                        if (user == null) onNeedLogin() else replyTo = c
                                    },
                                    onNeedLogin = onNeedLogin,
                                    onDelete = { toDelete = c },
                                )
                                if (replies.isNotEmpty()) {
                                    Row(
                                        Modifier.padding(start = 64.dp, bottom = 4.dp)
                                            .clickable {
                                                expanded = if (open) expanded - c.id else expanded + c.id
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            if (open) "Sembunyikan balasan" else "Lihat ${replies.size} balasan",
                                            color = AniloverAccent, fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Icon(
                                            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null, tint = AniloverAccent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    if (open) {
                                        replies.forEach { r ->
                                            CommentRow(
                                                c = r, me = user, isReply = true,
                                                onLike = { u -> CommentRepository.toggleLike(r.id, u.uid, u.uid in r.likedBy) },
                                                onReply = {
                                                    if (user == null) onNeedLogin() else replyTo = c
                                                },
                                                onNeedLogin = onNeedLogin,
                                                onDelete = { toDelete = r },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(12.dp)) }
        }

        if (commentsState !is CommentsState.Unavailable) {
            CommentInputBar(
                user = user,
                replyTo = replyTo,
                onCancelReply = { replyTo = null },
                onNeedLogin = onNeedLogin,
                onSend = { text, done ->
                    val u = user
                    if (u == null) { onNeedLogin(); done(false) } else {
                        val parent = replyTo
                        CommentRepository.post(thread, text, parent?.id, u) { ok ->
                            if (ok && parent != null) expanded = expanded + parent.id
                            if (ok) replyTo = null
                            done(ok)
                        }
                    }
                },
            )
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus komentar?") },
            text = { Text("Komentar ini akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { CommentRepository.delete(c.id); toDelete = null }) {
                    Text("Hapus", color = AniloverAccent)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun WatchAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.clip(AniloverShapes.Chip).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (active) AniloverAccent else AniloverTextPrimary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label, fontSize = 11.sp,
            color = if (active) AniloverAccent else AniloverTextPrimary,
        )
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(AniloverShapes.Pill)
            .background(if (selected) AniloverTextPrimary else AniloverSurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (selected) AniloverBackground else AniloverTextPrimary,
        )
    }
}

@Composable
private fun CommentNotice(text: String) {
    Text(
        text, color = AniloverTextDim, fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun Avatar(name: String, uid: String, size: androidx.compose.ui.unit.Dp) {
    val palette = listOf(
        Color(0xFFE5557D), Color(0xFF7C5CFF), Color(0xFF2E9BFF),
        Color(0xFF1FB58F), Color(0xFFE88A2E), Color(0xFFB04FD6),
    )
    val bg = palette[(uid.hashCode() and 0x7fffffff) % palette.size]
    Box(
        Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
private fun CommentRow(
    c: Comment,
    me: AuthUser?,
    isReply: Boolean,
    onLike: (AuthUser) -> Unit,
    onReply: () -> Unit,
    onNeedLogin: () -> Unit,
    onDelete: () -> Unit,
) {
    val liked = me != null && me.uid in c.likedBy
    val own = me != null && me.uid == c.uid
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .padding(start = if (isReply) 64.dp else 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
    ) {
        Avatar(c.name, c.uid, if (isReply) 26.dp else 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.name, color = AniloverTextPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text("  ·  " + timeAgo(c.createdAt), color = AniloverTextDim, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
            Text(c.text, color = AniloverTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(AniloverShapes.Chip)
                        .clickable { if (me == null) onNeedLogin() else onLike(me) }
                        .padding(end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Suka",
                        tint = if (liked) AniloverAccent else AniloverTextDim,
                        modifier = Modifier.size(17.dp),
                    )
                    if (c.likes > 0) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${c.likes}", fontSize = 12.sp,
                            color = if (liked) AniloverAccent else AniloverTextDim,
                        )
                    }
                }
                Text(
                    "Balas", color = AniloverTextDim, fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(AniloverShapes.Chip).clickable(onClick = onReply)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        if (own) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.MoreVert, contentDescription = "Opsi",
                        tint = AniloverTextDim, modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }
    }
}

@Composable
private fun CommentInputBar(
    user: AuthUser?,
    replyTo: Comment?,
    onCancelReply: () -> Unit,
    onNeedLogin: () -> Unit,
    onSend: (String, (Boolean) -> Unit) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank() && !sending

    Column(
        Modifier.fillMaxWidth()
            .background(AniloverSurface)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AniloverOutline.copy(alpha = 0.6f)))
        if (replyTo != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Membalas ${replyTo.name}", color = AniloverTextDim, fontSize = 12.sp,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onCancelReply, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Batal membalas",
                        tint = AniloverTextDim, modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (user == null) {
            Text(
                "Masuk untuk menulis komentar…", color = AniloverTextDim, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNeedLogin)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Avatar(user.name, user.uid, 34.dp)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.weight(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .background(AniloverSurfaceHigh)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            if (replyTo != null) "Tulis balasan…" else "Tambahkan komentar…",
                            color = AniloverTextDim, fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(CommentRepository.MAX_LENGTH) },
                        textStyle = TextStyle(color = AniloverTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(AniloverAccent),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(
                    enabled = canSend,
                    onClick = {
                        sending = true
                        onSend(text) { ok ->
                            sending = false
                            if (ok) text = ""
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = if (canSend) AniloverAccent else AniloverTextDim.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

private fun timeAgo(ms: Long): String {
    val s = ((System.currentTimeMillis() - ms) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "baru saja"
        s < 3600 -> "${s / 60} mnt lalu"
        s < 86_400 -> "${s / 3600} jam lalu"
        s < 86_400 * 30 -> "${s / 86_400} hari lalu"
        s < 86_400L * 365 -> "${s / (86_400 * 30)} bln lalu"
        else -> "${s / (86_400L * 365)} thn lalu"
    }
}
