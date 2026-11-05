package com.app.anilover.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Satu komentar (atau balasan bila [parentId] terisi). */
data class Comment(
    val id: String,
    val parentId: String?,
    val uid: String,
    val name: String,
    val text: String,
    val createdAt: Long,
    val likedBy: Set<String>,
) {
    val likes: Int get() = likedBy.size
}

/** Keadaan daftar komentar untuk UI. */
sealed interface CommentsState {
    data object Loading : CommentsState
    data object Unavailable : CommentsState // Firebase belum dipasang
    data class Error(val message: String) : CommentsState
    data class Ready(val items: List<Comment>) : CommentsState
}

/**
 * Komentar per episode. Dengan Firebase: disimpan di Cloud Firestore (koleksi "comments"), realtime.
 * Tanpa Firebase: disimpan lokal di perangkat (SharedPreferences) dengan fitur yang sama.
 * Satu koleksi datar: komentar utama punya parentId = null, balasan menunjuk ke id induknya.
 * Kueri hanya memakai satu filter persamaan (thread) sehingga TIDAK butuh indeks gabungan;
 * pengurutan (Teratas/Terbaru) dilakukan di sisi aplikasi.
 */
object CommentRepository {
    const val MAX_LENGTH = 500
    private const val COLLECTION = "comments"
    private const val LIMIT = 300L

    fun threadId(titleId: String, episode: Int) = "${titleId}_$episode"

    private fun db(): FirebaseFirestore? =
        if (AuthRepository.isConfigured) {
            try { FirebaseFirestore.getInstance() } catch (e: Throwable) { null }
        } else null

    val isAvailable: Boolean get() = true

    fun observe(thread: String): Flow<CommentsState> =
        if (db() != null) observeCloud(thread) else observeLocal(thread)

    private fun observeCloud(thread: String): Flow<CommentsState> = callbackFlow {
        val d = db()
        if (d == null) {
            trySend(CommentsState.Unavailable)
            awaitClose { }
            return@callbackFlow
        }
        trySend(CommentsState.Loading)
        val reg = d.collection(COLLECTION)
            .whereEqualTo("thread", thread)
            .limit(LIMIT)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(
                        CommentsState.Error(
                            "Komentar tidak bisa dimuat. Periksa koneksi atau aturan Firestore."
                        )
                    )
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toComment() } ?: emptyList()
                trySend(CommentsState.Ready(list))
            }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toComment(): Comment? {
        val text = getString("text") ?: return null
        val uid = getString("uid") ?: return null
        val ts = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        @Suppress("UNCHECKED_CAST")
        val liked = (get("likedBy") as? List<String>)?.toSet() ?: emptySet()
        return Comment(
            id = id,
            parentId = getString("parentId")?.takeIf { it.isNotBlank() },
            uid = uid,
            name = getString("name") ?: "Pengguna",
            text = text,
            createdAt = ts?.toDate()?.time ?: System.currentTimeMillis(),
            likedBy = liked,
        )
    }

    /** Kirim komentar / balasan. [onDone] dipanggil dengan true bila berhasil. */
    fun post(
        thread: String,
        text: String,
        parentId: String?,
        user: AuthUser,
        onDone: (Boolean) -> Unit,
    ) {
        val d = db()
        val clean = text.trim().take(MAX_LENGTH)
        if (clean.isEmpty()) { onDone(false); return }
        if (d == null) { postLocal(thread, clean, parentId, user, onDone); return }
        val data = hashMapOf<String, Any>(
            "thread" to thread,
            "uid" to user.uid,
            "name" to user.name.ifBlank { user.email?.substringBefore('@') ?: "Pengguna" },
            "text" to clean,
            "likedBy" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        if (parentId != null) data["parentId"] = parentId
        d.collection(COLLECTION).add(data).addOnCompleteListener { onDone(it.isSuccessful) }
    }

    fun toggleLike(commentId: String, uid: String, currentlyLiked: Boolean) {
        val d = db() ?: run { toggleLikeLocal(commentId, uid, currentlyLiked); return }
        d.collection(COLLECTION).document(commentId).update(
            "likedBy",
            if (currentlyLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
        )
    }

    fun delete(commentId: String, onDone: (Boolean) -> Unit = {}) {
        val d = db() ?: run { deleteLocal(commentId, onDone); return }
        d.collection(COLLECTION).document(commentId).delete()
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }

    // ---- Mode lokal (tanpa Firebase) ----

    private data class LocalRow(val thread: String, val comment: Comment)

    private const val LOCAL_MAX = 1500

    @Volatile private var localPrefs: SharedPreferences? = null
    @Volatile private var localRows: List<LocalRow> = emptyList()
    private val localTick = MutableStateFlow(0)

    /** Panggil sekali dari Activity (idempotent). */
    @Synchronized
    fun init(context: Context) {
        if (localPrefs != null) return
        val p = context.applicationContext.getSharedPreferences("anilover_comments_local", Context.MODE_PRIVATE)
        localPrefs = p
        localRows = try { parseLocal(p.getString("rows", "[]") ?: "[]") } catch (e: Exception) { emptyList() }
        localTick.value = localTick.value + 1
    }

    private fun observeLocal(thread: String): Flow<CommentsState> =
        localTick.map {
            CommentsState.Ready(localRows.filter { r -> r.thread == thread }.map { r -> r.comment })
        }

    private fun parseLocal(json: String): List<LocalRow> {
        val arr = JSONArray(json)
        val out = ArrayList<LocalRow>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val likes = o.optJSONArray("likes")
            val liked = HashSet<String>()
            if (likes != null) for (j in 0 until likes.length()) liked.add(likes.optString(j))
            out.add(
                LocalRow(
                    thread = o.optString("t"),
                    comment = Comment(
                        id = o.optString("id"),
                        parentId = o.optString("p").takeIf { it.isNotBlank() },
                        uid = o.optString("uid"),
                        name = o.optString("name").ifBlank { "Pengguna" },
                        text = o.optString("text"),
                        createdAt = o.optLong("at"),
                        likedBy = liked,
                    ),
                )
            )
        }
        return out
    }

    private fun persistLocal(rows: List<LocalRow>) {
        val arr = JSONArray()
        for (r in rows) {
            val c = r.comment
            arr.put(
                JSONObject()
                    .put("t", r.thread).put("id", c.id).put("p", c.parentId ?: "")
                    .put("uid", c.uid).put("name", c.name).put("text", c.text)
                    .put("at", c.createdAt).put("likes", JSONArray(c.likedBy.toList()))
            )
        }
        localPrefs?.edit()?.putString("rows", arr.toString())?.apply()
    }

    /** Ubah daftar lokal secara atomik, simpan, lalu beri tahu pengamat. */
    @Synchronized
    private fun mutateLocal(block: (List<LocalRow>) -> List<LocalRow>): Boolean {
        if (localPrefs == null) return false
        var next = block(localRows)
        if (next.size > LOCAL_MAX) next = next.sortedBy { it.comment.createdAt }.takeLast(LOCAL_MAX)
        localRows = next
        persistLocal(next)
        localTick.value = localTick.value + 1
        return true
    }

    private fun postLocal(thread: String, clean: String, parentId: String?, user: AuthUser, onDone: (Boolean) -> Unit) {
        val row = LocalRow(
            thread,
            Comment(
                id = UUID.randomUUID().toString(),
                parentId = parentId,
                uid = user.uid,
                name = user.name.ifBlank { user.email?.substringBefore('@') ?: "Pengguna" },
                text = clean,
                createdAt = System.currentTimeMillis(),
                likedBy = emptySet(),
            ),
        )
        onDone(mutateLocal { it + row })
    }

    private fun toggleLikeLocal(commentId: String, uid: String, currentlyLiked: Boolean) {
        mutateLocal { rows ->
            rows.map { r ->
                if (r.comment.id != commentId) r
                else r.copy(
                    comment = r.comment.copy(
                        likedBy = if (currentlyLiked) r.comment.likedBy - uid else r.comment.likedBy + uid,
                    )
                )
            }
        }
    }

    private fun deleteLocal(commentId: String, onDone: (Boolean) -> Unit) {
        onDone(mutateLocal { rows -> rows.filter { it.comment.id != commentId && it.comment.parentId != commentId } })
    }
}
