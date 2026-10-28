package com.app.rea.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

const val API_BASE = "https://api-anime.adiyth-arpansa.workers.dev"

data class Episode(
    val number: Int,
    val playlist: String,
)

data class Title(
    val id: String,
    val title: String,
    val poster: String? = null,
    val banner: String? = null,
    val synopsis: String? = null,
    val year: Int? = null,
    val score: Double? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(),
) {
    /** Persentase kecocokan gaya Netflix, diturunkan dari skor (8,6 -> 86%). */
    val matchPercent: Int? get() = score?.let { (it * 10).toInt().coerceIn(0, 100) }
    /** Nama seri tanpa embel season, untuk baris pertama judul kartu. */
    val seriesName: String
        get() {
            val i = title.indexOf(" Season ")
            if (i > 0) return title.substring(0, i)
            val j = title.indexOf(": ")
            if (j > 0) return title.substring(0, j)
            return title
        }
    /** Sisa judul sesudah nama seri (season/bagian), untuk baris kedua kartu. */
    val titleSub: String?
        get() {
            val i = title.indexOf(" Season ")
            if (i > 0) return title.substring(i + 1)
            val j = title.indexOf(": ")
            if (j > 0) return title.substring(j + 2)
            return null
        }
    val statusLabel: String?
        get() = when (status) {
            "FINISHED" -> "Tamat"
            "RELEASING" -> "Sedang Tayang"
            "NOT_YET_RELEASED" -> "Segera"
            else -> status
        }
}

object CatalogApi {
    fun parseCatalog(body: String): List<Title> {
        val root = JSONObject(body)
        val arr = root.optJSONArray("titles") ?: JSONArray()
        val out = ArrayList<Title>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            val eps = ArrayList<Episode>()
            val ea = t.optJSONArray("episodes") ?: JSONArray()
            for (j in 0 until ea.length()) {
                val e = ea.getJSONObject(j)
                eps.add(Episode(e.optInt("number"), e.optString("playlist")))
            }
            val genres = ArrayList<String>()
            val ga = t.optJSONArray("genres") ?: JSONArray()
            for (j in 0 until ga.length()) genres.add(ga.optString(j))
            out.add(
                Title(
                    id = t.optString("id"),
                    title = t.optString("title"),
                    poster = t.optString("poster").ifBlank { null },
                    banner = t.optString("banner").ifBlank { null },
                    synopsis = t.optString("synopsis").ifBlank { null },
                    year = if (t.has("year") && !t.isNull("year")) t.optInt("year") else null,
                    score = if (t.has("score") && !t.isNull("score")) t.optDouble("score") else null,
                    status = t.optString("status").ifBlank { null },
                    genres = genres,
                    episodes = eps.sortedBy { it.number },
                )
            )
        }
        return out
    }

    suspend fun fetchRawCatalog(): String = withContext(Dispatchers.IO) {
        val conn = URL("$API_BASE/api/catalog").openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 20000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (REA Android)")
        conn.inputStream.bufferedReader().use { it.readText() }
    }

    suspend fun fetchCatalog(): List<Title> = parseCatalog(fetchRawCatalog())
}

/** Satu riwayat tonton per (judul, episode) — bahan baris "Lanjutkan Menonton". */
data class WatchEntry(
    val titleId: String,
    val episode: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val isStarted: Boolean get() = fraction > 0.02f
    val isFinished: Boolean get() = fraction > 0.95f
}

private val Context.dataStore by preferencesDataStore(name = "rea_store")
private val HISTORY_KEY = stringPreferencesKey("history_json")
private val MYLIST_KEY = stringPreferencesKey("mylist_json")
private val RATINGS_KEY = stringPreferencesKey("ratings_json")
private val HIDDEN_KEY = stringPreferencesKey("hidden_continue_json")
private val AUTOPLAY_KEY = booleanPreferencesKey("autoplay_next")
private val SUBTITLE_KEY = stringPreferencesKey("subtitle_default")
private val CACHE_KEY = stringPreferencesKey("catalog_cache")
private val DOWNLOADS_KEY = stringPreferencesKey("downloads_json")

class HistoryStore(private val context: Context) {
    val entries: Flow<List<WatchEntry>> = context.dataStore.data.map { prefs ->
        parse(prefs[HISTORY_KEY] ?: "[]")
    }

    suspend fun save(entry: WatchEntry) {
        context.dataStore.edit { prefs ->
            val list = parse(prefs[HISTORY_KEY] ?: "[]").toMutableList()
            list.removeAll { it.titleId == entry.titleId && it.episode == entry.episode }
            list.add(entry)
            prefs[HISTORY_KEY] = write(list)
        }
    }

    suspend fun remove(titleId: String, episode: Int) {
        context.dataStore.edit { prefs ->
            val list = parse(prefs[HISTORY_KEY] ?: "[]").toMutableList()
            list.removeAll { it.titleId == titleId && it.episode == episode }
            prefs[HISTORY_KEY] = write(list)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it[HISTORY_KEY] = "[]" }
    }

    suspend fun find(titleId: String, episode: Int): WatchEntry? {
        val prefs = context.dataStore.data.first()
        return parse(prefs[HISTORY_KEY] ?: "[]")
            .firstOrNull { it.titleId == titleId && it.episode == episode }
    }

    private fun parse(s: String): List<WatchEntry> = try {
        val arr = JSONArray(s)
        val out = ArrayList<WatchEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                WatchEntry(
                    o.optString("t"), o.optInt("e"),
                    o.optLong("p"), o.optLong("d"), o.optLong("u"),
                )
            )
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    private fun write(list: List<WatchEntry>): String {
        val arr = JSONArray()
        for (e in list) {
            arr.put(
                JSONObject()
                    .put("t", e.titleId).put("e", e.episode)
                    .put("p", e.positionMs).put("d", e.durationMs).put("u", e.updatedAt)
            )
        }
        return arr.toString()
    }
}

data class ReaSettings(
    val autoplayNext: Boolean = true,
    val subtitleDefault: String = "id",
)

/** Daftarku, nilai, sembunyikan-dari-Lanjutkan, pengaturan, cache katalog, unduhan. */
class LibraryStore(private val context: Context) {

    val myList: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        parseStringSet(prefs[MYLIST_KEY])
    }
    val ratings: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        parseRatings(prefs[RATINGS_KEY])
    }
    val hiddenContinue: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        parseStringSet(prefs[HIDDEN_KEY])
    }
    val settings: Flow<ReaSettings> = context.dataStore.data.map { prefs ->
        ReaSettings(
            autoplayNext = prefs[AUTOPLAY_KEY] ?: true,
            subtitleDefault = prefs[SUBTITLE_KEY] ?: "id",
        )
    }
    val downloads: Flow<List<DownloadItem>> = context.dataStore.data.map { prefs ->
        parseDownloads(prefs[DOWNLOADS_KEY])
    }

    suspend fun toggleMyList(id: String) {
        context.dataStore.edit { prefs ->
            val set = parseStringSet(prefs[MYLIST_KEY]).toMutableSet()
            if (!set.add(id)) set.remove(id)
            prefs[MYLIST_KEY] = writeStringSet(set)
        }
    }

    suspend fun setRating(id: String, value: Int) {
        context.dataStore.edit { prefs ->
            val map = parseRatings(prefs[RATINGS_KEY]).toMutableMap()
            if (value == 0) map.remove(id) else map[id] = value
            val obj = JSONObject()
            for ((k, v) in map) obj.put(k, v)
            prefs[RATINGS_KEY] = obj.toString()
        }
    }

    suspend fun hideContinue(titleId: String, episode: Int) {
        context.dataStore.edit { prefs ->
            val set = parseStringSet(prefs[HIDDEN_KEY]).toMutableSet()
            set.add("$titleId|$episode")
            prefs[HIDDEN_KEY] = writeStringSet(set)
        }
    }

    suspend fun setAutoplay(value: Boolean) {
        context.dataStore.edit { it[AUTOPLAY_KEY] = value }
    }

    suspend fun setSubtitleDefault(value: String) {
        context.dataStore.edit { it[SUBTITLE_KEY] = value }
    }

    suspend fun saveCatalogCache(raw: String) {
        context.dataStore.edit { it[CACHE_KEY] = raw }
    }

    suspend fun readCatalogCache(): List<Title> {
        val raw = context.dataStore.data.first()[CACHE_KEY] ?: return emptyList()
        return try { CatalogApi.parseCatalog(raw) } catch (e: Exception) { emptyList() }
    }

    suspend fun upsertDownload(item: DownloadItem) {
        context.dataStore.edit { prefs ->
            val list = parseDownloads(prefs[DOWNLOADS_KEY]).toMutableList()
            list.removeAll { it.titleId == item.titleId && it.episode == item.episode }
            list.add(item)
            prefs[DOWNLOADS_KEY] = writeDownloads(list)
        }
    }

    suspend fun removeDownload(titleId: String, episode: Int) {
        context.dataStore.edit { prefs ->
            val list = parseDownloads(prefs[DOWNLOADS_KEY]).toMutableList()
            list.removeAll { it.titleId == titleId && it.episode == episode }
            prefs[DOWNLOADS_KEY] = writeDownloads(list)
        }
    }

    suspend fun downloadsNow(): List<DownloadItem> =
        parseDownloads(context.dataStore.data.first()[DOWNLOADS_KEY])

    private fun parseStringSet(s: String?): Set<String> = try {
        if (s.isNullOrBlank()) emptySet()
        else {
            val arr = JSONArray(s)
            val out = LinkedHashSet<String>()
            for (i in 0 until arr.length()) out.add(arr.optString(i))
            out
        }
    } catch (e: Exception) { emptySet() }

    private fun writeStringSet(set: Set<String>): String {
        val arr = JSONArray()
        for (v in set) arr.put(v)
        return arr.toString()
    }

    private fun parseRatings(s: String?): Map<String, Int> = try {
        if (s.isNullOrBlank()) emptyMap()
        else {
            val obj = JSONObject(s)
            val out = HashMap<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optInt(k)
            }
            out
        }
    } catch (e: Exception) { emptyMap() }

    private fun parseDownloads(s: String?): List<DownloadItem> = try {
        if (s.isNullOrBlank()) emptyList()
        else {
            val arr = JSONArray(s)
            val out = ArrayList<DownloadItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    DownloadItem(
                        titleId = o.optString("t"),
                        titleName = o.optString("n"),
                        episode = o.optInt("e"),
                        playlist = o.optString("p"),
                        state = o.optString("s", "gagal"),
                        totalSegs = o.optInt("ts"),
                        doneSegs = o.optInt("ds"),
                        sizeBytes = o.optLong("b"),
                        updatedAt = o.optLong("u"),
                    )
                )
            }
            out.sortedByDescending { it.updatedAt }
        }
    } catch (e: Exception) { emptyList() }

    private fun writeDownloads(list: List<DownloadItem>): String {
        val arr = JSONArray()
        for (d in list) {
            arr.put(
                JSONObject()
                    .put("t", d.titleId).put("n", d.titleName).put("e", d.episode)
                    .put("p", d.playlist).put("s", d.state)
                    .put("ts", d.totalSegs).put("ds", d.doneSegs)
                    .put("b", d.sizeBytes).put("u", d.updatedAt)
            )
        }
        return arr.toString()
    }
}

/** state: antre | mengunduh | selesai | gagal */
data class DownloadItem(
    val titleId: String,
    val titleName: String,
    val episode: Int,
    val playlist: String,
    val state: String,
    val totalSegs: Int = 0,
    val doneSegs: Int = 0,
    val sizeBytes: Long = 0,
    val updatedAt: Long = 0,
) {
    val fraction: Float
        get() = if (totalSegs > 0) (doneSegs.toFloat() / totalSegs).coerceIn(0f, 1f) else 0f
}

/**
 * Unduhan offline sederhana: salin playlist + semua segmennya ke penyimpanan
 * aplikasi, lalu pemutar memutar playlist lokal yang sama (nama segmen relatif).
 */
class DownloadManager(private val context: Context, private val library: LibraryStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = HashSet<String>()

    fun dirFor(titleId: String, episode: Int): File =
        File(context.filesDir, "downloads/$titleId/ep$episode")

    fun localPlaylistUri(titleId: String, episode: Int): String? {
        val f = File(dirFor(titleId, episode), "playlist.m3u8")
        return if (f.exists() && f.length() > 0) "file://" + f.absolutePath else null
    }

    fun start(title: Title, episode: Episode) {
        val key = "${title.id}|${episode.number}"
        synchronized(running) { if (!running.add(key)) return }
        scope.launch {
            var item = DownloadItem(
                titleId = title.id, titleName = title.title, episode = episode.number,
                playlist = episode.playlist, state = "mengunduh",
                updatedAt = System.currentTimeMillis(),
            )
            try {
                library.upsertDownload(item)
                val dir = dirFor(title.id, episode.number)
                dir.mkdirs()
                val playlistText = httpGetText(episode.playlist)
                val base = episode.playlist.substringBeforeLast("/") + "/"
                val segLines = playlistText.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
                File(dir, "playlist.m3u8").writeText(playlistText)
                item = item.copy(totalSegs = segLines.size)
                library.upsertDownload(item)
                var bytes = 0L
                var done = 0
                for (line in segLines) {
                    val segUrl = when {
                        line.startsWith("http") -> line
                        line.startsWith("/") -> URL(episode.playlist).let { "${it.protocol}://${it.host}$line" }
                        else -> base + line
                    }
                    val name = line.substringBefore("?").substringAfterLast("/")
                    val outFile = File(dir, name)
                    if (!outFile.exists() || outFile.length() == 0L) {
                        httpDownload(segUrl, outFile)
                    }
                    bytes += outFile.length()
                    done++
                    item = item.copy(doneSegs = done, sizeBytes = bytes, updatedAt = System.currentTimeMillis())
                    library.upsertDownload(item)
                }
                item = item.copy(state = "selesai", updatedAt = System.currentTimeMillis())
                library.upsertDownload(item)
            } catch (e: Exception) {
                try {
                    library.upsertDownload(
                        item.copy(state = "gagal", updatedAt = System.currentTimeMillis())
                    )
                } catch (ignored: Exception) { }
            } finally {
                synchronized(running) { running.remove(key) }
            }
        }
    }

    fun delete(item: DownloadItem) {
        scope.launch {
            try { dirFor(item.titleId, item.episode).deleteRecursively() } catch (e: Exception) { }
            library.removeDownload(item.titleId, item.episode)
        }
    }

    private fun httpGetText(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (REA Android)")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun httpDownload(url: String, outFile: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 60000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (REA Android)")
        conn.inputStream.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
    }
}
