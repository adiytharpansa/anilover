package com.app.rea.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
)

object CatalogApi {
    suspend fun fetchCatalog(): List<Title> = withContext(Dispatchers.IO) {
        val conn = URL("$API_BASE/api/catalog").openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 20000
        conn.setRequestProperty("Accept", "application/json")
        val body = conn.inputStream.bufferedReader().use { it.readText() }
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
                    year = if (t.has("year")) t.optInt("year") else null,
                    score = if (t.has("score")) t.optDouble("score") else null,
                    status = t.optString("status").ifBlank { null },
                    genres = genres,
                    episodes = eps.sortedBy { it.number },
                )
            )
        }
        out
    }
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
