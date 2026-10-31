package com.app.anilover.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sinkronisasi cloud (Cloud Firestore) untuk data yang selama ini tersimpan lokal:
 * riwayat tonton, Daftarku, nilai, sembunyikan-dari-Lanjutkan, pengingat "Segera Tayang",
 * dan pengaturan aplikasi.
 *
 * - Aktif HANYA saat pengguna MASUK (Firebase Auth). Tamu & build tanpa Firebase
 *   berjalan seperti sebelumnya: semua data tetap lokal di perangkat.
 * - Satu dokumen per pengguna: `users/{uid}` dengan field data + `clientId` (UUID
 *   perangkat) + `updatedAt` (server timestamp).
 * - Penggabungan saat masuk: dokumen remote dipakai bila lebih baru dari yang terakhir
 *   kita terima/kirim; jika tidak, data lokal yang diunggah.
 * - Selama sesi: perubahan lokal didorong ke cloud (debounce 1,5 dtk) dan perubahan
 *   dari perangkat lain diterapkan langsung (snapshot listener). Penulisan sendiri
 *   diabaikan lewat `clientId`, dan hash data mencegah ping-pong antar perangkat.
 * - Yang TIDAK disinkronkan (bersifat perangkat): unduhan (berkas), cache katalog,
 *   id katalog yang sudah diketahui, dan riwayat pencarian.
 */
object CloudSync {
    private const val COLLECTION = "users"
    private const val PREFS = "anilover_sync"
    private const val DEBOUNCE_MS = 1500L
    /** Batas riwayat yang disinkronkan agar dokumen tidak melampaui 1 MB. */
    private const val MAX_HISTORY = 500

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionJob: Job? = null

    private var appContext: Context? = null
    private var history: HistoryStore? = null
    private var library: LibraryStore? = null

    /** Panggil sekali dari MainActivity.onCreate setelah AuthRepository.init. */
    fun attach(context: Context, historyStore: HistoryStore, libraryStore: LibraryStore) {
        appContext = context.applicationContext
        history = historyStore
        library = libraryStore
        scope.launch {
            AuthRepository.user.collectLatest { user ->
                sessionJob?.cancel()
                sessionJob = null
                if (user != null && AuthRepository.isConfigured) {
                    sessionJob = scope.launch { runSession(user.uid) }
                }
            }
        }
    }

    /** Keadaan lokal lengkap yang ikut disinkronkan. */
    private data class SyncState(
        val history: List<WatchEntry>,
        val myList: Set<String>,
        val ratings: Map<String, Int>,
        val hidden: Set<String>,
        val reminders: Set<String>,
        val settings: AniloverSettings,
    ) {
        /** Hash dari JSON kanonik — sama dengan hash remote berarti "sudah tersinkron". */
        val hash: Int get() = toJson().hashCode()

        private fun canonicalHistory(): List<WatchEntry> =
            history.sortedByDescending { it.updatedAt }.take(MAX_HISTORY)

        fun toJson(): String {
            val o = JSONObject()
            val h = JSONArray()
            for (e in canonicalHistory().sortedWith(compareBy({ it.titleId }, { it.episode }))) {
                h.put(
                    JSONObject()
                        .put("t", e.titleId).put("e", e.episode)
                        .put("p", e.positionMs).put("d", e.durationMs).put("u", e.updatedAt)
                )
            }
            o.put("history", h)
            o.put("myList", JSONArray(myList.sorted()))
            o.put("ratings", JSONObject().apply { for ((k, v) in ratings.toSortedMap()) put(k, v) })
            o.put("hidden", JSONArray(hidden.sorted()))
            o.put("reminders", JSONArray(reminders.sorted()))
            o.put(
                "settings", JSONObject()
                    .put("autoplay", settings.autoplayNext)
                    .put("sub", settings.subtitleDefault)
                    .put("subSize", settings.subtitleSize)
                    .put("subColor", settings.subtitleColor)
                    .put("smart", settings.smartDownloads)
                    .put("wifiOnly", settings.wifiOnlyDownloads)
            )
            return o.toString()
        }

        /** Map untuk ditulis ke Firestore (dengan server timestamp). */
        fun toMap(): Map<String, Any> = mapOf(
            "history" to canonicalHistory().map {
                mapOf(
                    "t" to it.titleId, "e" to it.episode,
                    "p" to it.positionMs, "d" to it.durationMs, "u" to it.updatedAt,
                )
            },
            "myList" to myList.toList(),
            "ratings" to ratings.mapValues { it.value.toLong() },
            "hidden" to hidden.toList(),
            "reminders" to reminders.toList(),
            "settings" to mapOf(
                "autoplay" to settings.autoplayNext,
                "sub" to settings.subtitleDefault,
                "subSize" to settings.subtitleSize,
                "subColor" to settings.subtitleColor,
                "smart" to settings.smartDownloads,
                "wifiOnly" to settings.wifiOnlyDownloads,
            ),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }

    private suspend fun runSession(uid: String) {
        val ctx = appContext ?: return
        val h = history ?: return
        val l = library ?: return
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            return // Firebase tidak terpasang — tidak ada sinkronisasi
        }
        val docRef = db.collection(COLLECTION).document(uid)
        val clientId = clientId(ctx)
        // Penanda disimpan per uid supaya ganti akun di perangkat yang sama tidak tercampur.
        val keyHash = "applied_hash_$uid"
        val keyMs = "applied_ms_$uid"
        val keyPushed = "pushed_ms_$uid"
        var lastAppliedHash = prefs(ctx).getInt(keyHash, 0)
        var lastAppliedMs = prefs(ctx).getLong(keyMs, 0L)

        suspend fun snapshotLocal(): SyncState = SyncState(
            history = h.entries.first(),
            myList = l.myList.first(),
            ratings = l.ratings.first(),
            hidden = l.hiddenContinue.first(),
            reminders = l.reminders.first(),
            settings = l.settings.first(),
        )

        suspend fun applyRemote(remote: SyncState, ms: Long) {
            h.replaceAll(remote.history)
            l.replaceMyList(remote.myList)
            l.replaceRatings(remote.ratings)
            l.replaceHidden(remote.hidden)
            l.replaceReminders(remote.reminders)
            l.replaceSettings(remote.settings)
            prefs(ctx).edit()
                .putInt(keyHash, remote.hash)
                .putLong(keyMs, ms)
                .apply()
            lastAppliedHash = remote.hash
            lastAppliedMs = ms
        }

        suspend fun push(state: SyncState) {
            // Jangan tulis ulang data yang baru saja diterima dari cloud (anti ping-pong).
            if (state.hash == lastAppliedHash) return
            val data = state.toMap().toMutableMap()
            data["clientId"] = clientId
            docRef.set(data).await()
            prefs(ctx).edit().putLong(keyPushed, System.currentTimeMillis()).apply()
            lastAppliedHash = state.hash
        }

        // Skop sesi: ikut batal saat pengguna keluar / sesi diganti.
        val sessionScope = CoroutineScope(SupervisorJob(coroutineContext[Job]) + Dispatchers.IO)

        // 1) Penggabungan saat masuk: remote dipakai bila lebih baru dari yang terakhir
        //    kita terima (applied_ms) atau kirim (pushed_ms); selain itu, unggah lokal.
        try {
            val snap = docRef.get().await()
            val local = snapshotLocal()
            if (snap.exists()) {
                val remote = snap.toRemote()
                val remoteMs = snap.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                val pushedMs = prefs(ctx).getLong(keyPushed, 0L)
                if (remote != null && snap.getString("clientId") != clientId &&
                    remoteMs > maxOf(lastAppliedMs, pushedMs)
                ) {
                    applyRemote(remote, remoteMs)
                } else if (local.hash != lastAppliedHash) {
                    try { push(local) } catch (e: Exception) { /* coba lagi nanti */ }
                }
            } else if (local.hash != lastAppliedHash || lastAppliedHash == 0) {
                try { push(local) } catch (e: Exception) { /* coba lagi nanti */ }
            }
        } catch (e: Exception) {
            // Baca awal gagal — listener di bawah tetap mencoba saat jaringan pulih.
        }

        // 2) Dengarkan perubahan dari perangkat lain (real-time).
        val reg = docRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
            sessionScope.launch {
                try {
                    val remote = snap.toRemote() ?: return@launch
                    if (snap.getString("clientId") == clientId) return@launch // tulisan sendiri
                    val ms = snap.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    if (ms > lastAppliedMs && remote.hash != lastAppliedHash) {
                        applyRemote(remote, ms)
                    }
                } catch (e: Exception) { /* abaikan snapshot rusak */ }
            }
        }

        // 3) Dorong perubahan lokal ke cloud (debounce supaya tidak menulis tiap detik).
        sessionScope.launch {
            combine(
                listOf<Flow<Any?>>(
                    h.entries, l.myList, l.ratings, l.hiddenContinue, l.reminders, l.settings,
                ),
            ) { arr ->
                @Suppress("UNCHECKED_CAST")
                SyncState(
                    history = arr[0] as List<WatchEntry>,
                    myList = arr[1] as Set<String>,
                    ratings = arr[2] as Map<String, Int>,
                    hidden = arr[3] as Set<String>,
                    reminders = arr[4] as Set<String>,
                    settings = arr[5] as AniloverSettings,
                )
            }.collectLatest { state ->
                delay(DEBOUNCE_MS)
                try { push(state) } catch (e: Exception) { /* coba lagi saat perubahan berikutnya */ }
            }
        }

        try {
            awaitCancellation()
        } finally {
            reg.remove()
        }
    }

    /** Baca dokumen Firestore menjadi SyncState; null bila rusak. */
    private fun DocumentSnapshot.toRemote(): SyncState? = try {
        val hArr = get("history") as? List<*> ?: emptyList<Any?>()
        val history = hArr.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val t = m["t"] as? String ?: return@mapNotNull null
            WatchEntry(
                titleId = t,
                episode = (m["e"] as? Number)?.toInt() ?: 0,
                positionMs = (m["p"] as? Number)?.toLong() ?: 0L,
                durationMs = (m["d"] as? Number)?.toLong() ?: 0L,
                updatedAt = (m["u"] as? Number)?.toLong() ?: 0L,
            )
        }
        val myList = (get("myList") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        val hidden = (get("hidden") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        val reminders = (get("reminders") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        val ratings = (get("ratings") as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String
            val num = v as? Number
            if (key != null && num != null) key to num.toInt() else null
        }?.toMap() ?: emptyMap()
        val s = get("settings") as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val settings = AniloverSettings(
            autoplayNext = s["autoplay"] as? Boolean ?: true,
            subtitleDefault = s["sub"] as? String ?: "id",
            subtitleSize = s["subSize"] as? String ?: "sedang",
            subtitleColor = s["subColor"] as? String ?: "putih",
            smartDownloads = s["smart"] as? Boolean ?: false,
            wifiOnlyDownloads = s["wifiOnly"] as? Boolean ?: false,
        )
        SyncState(history, myList, ratings, hidden, reminders, settings)
    } catch (e: Exception) {
        null
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** UUID tetap per perangkat — dipakai membedakan tulisan sendiri vs perangkat lain. */
    private fun clientId(ctx: Context): String {
        val p = prefs(ctx)
        val existing = p.getString("client_id", null)
        if (!existing.isNullOrBlank()) return existing
        val id = java.util.UUID.randomUUID().toString()
        p.edit().putString("client_id", id).apply()
        return id
    }

    /** Menunggu Task Firebase selesai tanpa dependensi tambahan. */
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
