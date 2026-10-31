package com.app.anilover.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
 * Komentar per episode, disimpan di Cloud Firestore (koleksi "comments"), realtime.
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

    val isAvailable: Boolean get() = db() != null

    fun observe(thread: String): Flow<CommentsState> = callbackFlow {
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
        if (d == null || clean.isEmpty()) { onDone(false); return }
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
        val d = db() ?: return
        d.collection(COLLECTION).document(commentId).update(
            "likedBy",
            if (currentlyLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
        )
    }

    fun delete(commentId: String, onDone: (Boolean) -> Unit = {}) {
        val d = db() ?: run { onDone(false); return }
        d.collection(COLLECTION).document(commentId).delete()
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }
}
