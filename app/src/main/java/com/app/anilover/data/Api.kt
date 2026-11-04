package com.app.anilover.data

import android.content.Context
import com.app.anilover.BuildConfig
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
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request


// Sumber katalog utama: Cloudflare Pages (aset statis — tanpa jatah harian
// seperti Worker; katalog disinkronkan otomatis dari repo xjatxz2upew4).
private const val CATALOG_PAGES =
    "https://anilover-catalog.pages.dev/da163150ab26d6059ec10153/lib.dat"
private const val SOON_PAGES =
    "https://anilover-catalog.pages.dev/da163150ab26d6059ec10153/soon.dat"
// Cermin katalog gudang (dipakai bila API utama kena batas kuota Cloudflare).
private const val CATALOG_MIRROR =
    "https://cdn.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/catalog.json"
private const val CATALOG_MIRROR_GCORE =
    "https://gcore.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/catalog.json"
private const val CATALOG_MIRROR_FASTLY =
    "https://fastly.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/catalog.json"
private const val CATALOG_MIRROR2 =
    "https://raw.githubusercontent.com/adiytharpansa/xjatxz2upew4/main/catalog.json"
private const val SOON_MIRROR =
    "https://cdn.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/coming-soon.json"
private const val PROJECT_REPO = "adiytharpansa/xjatxz2upew4"

data class Episode(
    val number: Int,
    val playlist: String,
    /** Judul episode dari API bila ada (data opsional, tampil bila terisi). */
    val title: String? = null,
    /** Gambar cuplikan episode dari API bila ada. */
    val thumb: String? = null,
    /** Sinopsis episode dari API bila ada. */
    val synopsis: String? = null,
    /** Detik akhir intro dari API bila ada; null = heuristik Lewati Intro 90 dtk. */
    val introEnd: Long? = null,
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
    /** Konten khusus dewasa dari API bila datanya ada; menggerakkan lencana 18+ di halaman detail. */
    val adult: Boolean = false,
    /** URL trailer dari API bila ada; billboard memutarnya otomatis bila terisi. */
    val trailer: String? = null,
    /** Cap waktu terbit (epoch ms) bila katalog menyediakannya; null = fallback ke urutan katalog. */
    val addedAt: Long? = null,
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

/**
 * Urutan "terbaru lebih dulu": bila ada judul yang membawa cap waktu terbit
 * ([Title.addedAt]), urutkan menurunnya; bila tidak, balik urutan katalog
 * (asumsi gudang: yang terakhir masuk = yang terbaru terbit).
 */
fun List<Title>.newestFirst(): List<Title> =
    if (any { it.addedAt != null }) sortedByDescending { it.addedAt ?: 0L }
    else reversed()

object CatalogApi {
    /** Id katalog terakhir yang berhasil dibaca (untuk menyaring Segera Tayang). */
    @Volatile
    var lastCatalogIds: Set<String> = emptySet()

    // OkHttp (tumpukan jaringan yang sama dengan pemutar & poster — terbukti
    // jalan di HP pengguna) menggantikan HttpURLConnection untuk pengambilan
    // teks katalog/soon.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun httpGetText(url: String, timeoutMs: Int = 15000): String {
        val client = httpClient.newBuilder()
            .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Anilover Android)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw java.io.IOException("HTTP " + resp.code + " dari " + url)
            }
            return resp.body?.string() ?: throw java.io.IOException("respons kosong dari " + url)
        }
    }

    private fun httpGetBytes(url: String, timeoutMs: Int = 15000): ByteArray {
        val client = httpClient.newBuilder()
            .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Anilover Android)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw java.io.IOException("HTTP " + resp.code + " dari " + url)
            }
            return resp.body?.bytes() ?: throw java.io.IOException("respons kosong dari " + url)
        }
    }

    /**
     * Buka isi katalog: teks JSON apa adanya (dari cermin gudang), atau
     * berkas terenkripsi AES-GCM dari Pages (nonce 12 byte di depan,
     * sisanya ciphertext+tag). Kunci tiba tersamar lewat BuildConfig
     * (rahasia CI) dan dibuka dengan topeng turunan konstanta kode;
     * tanpa kunci yang sah fungsi ini melempar dan pemanggil jatuh ke
     * sumber berikutnya.
     */
    private fun decodeCatalogBody(bytes: ByteArray): String {
        if (bytes.isNotEmpty() && (bytes[0] == '{'.code.toByte() || bytes[0] == '['.code.toByte())) {
            return String(bytes, Charsets.UTF_8)
        }
        return String(decryptCatalog(bytes), Charsets.UTF_8)
    }

    private fun decryptCatalog(data: ByteArray): ByteArray {
        require(data.size > 28) { "berkas terenkripsi terlalu pendek" }
        val masked = android.util.Base64.decode(BuildConfig.CATALOG_KEY_X, android.util.Base64.DEFAULT)
        require(masked.size == 32) { "kunci katalog tidak tersedia" }
        val mask = java.security.MessageDigest.getInstance("SHA-256")
            .digest("anilover-katalog-v1".toByteArray(Charsets.UTF_8))
        val key = ByteArray(32) { i -> (masked[i].toInt() xor mask[i].toInt()).toByte() }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            javax.crypto.Cipher.DECRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.GCMParameterSpec(128, data.copyOfRange(0, 12)),
        )
        return cipher.doFinal(data.copyOfRange(12, data.size))
    }

    fun parseCatalog(body: String): List<Title> {
        val root = JSONObject(body)
        val rootPin = root.optString("pin")
        val arr = root.optJSONArray("titles") ?: JSONArray()
        val out = ArrayList<Title>()
        for (i in 0 until arr.length()) {
          try {
            val t = arr.getJSONObject(i)
            // Basis media per judul (meniru Worker api.anime): repo+pin judul,
            // atau repo proyek + pin katalog global. Respons API sudah absolut.
            val tRepo = t.optString("repo")
            val tPin = t.optString("pin")
            val base = when {
                tRepo.isNotBlank() && tPin.isNotBlank() ->
                    "https://cdn.jsdelivr.net/gh/$tRepo@$tPin"
                rootPin.isNotBlank() ->
                    "https://cdn.jsdelivr.net/gh/$PROJECT_REPO@$rootPin"
                else -> null
            }
            fun absolut(path: String): String =
                if (path.startsWith("http") || base == null) path else "$base/$path"
            val eps = ArrayList<Episode>()
            val ea = t.optJSONArray("episodes") ?: JSONArray()
            for (j in 0 until ea.length()) {
                // Gudang menulis episode dalam dua bentuk: objek lengkap
                // {number, playlist, ...} ATAU angka saja (jalur robot) —
                // untuk angka, jalur playlist mengikuti pola gudang
                // <id>/ep<N>/playlist.m3u8 dari basis repo+pin judul.
                when (val item = ea.opt(j)) {
                    is JSONObject -> eps.add(
                        Episode(
                            number = item.optInt("number"),
                            playlist = absolut(item.optString("playlist")),
                            title = item.optString("title").ifBlank { null },
                            thumb = item.optString("thumb").ifBlank { null }?.let { absolut(it) },
                            synopsis = item.optString("synopsis").ifBlank { null },
                            introEnd = if (item.has("introEnd") && !item.isNull("introEnd")) item.optLong("introEnd") else null,
                        )
                    )
                    is Number -> {
                        val n = item.toInt()
                        eps.add(
                            Episode(
                                number = n,
                                playlist = absolut(t.optString("id") + "/ep" + n + "/playlist.m3u8"),
                            )
                        )
                    }
                }
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
                    adult = t.optBoolean("adult", false),
                    trailer = t.optString("trailer").ifBlank { null },
                    addedAt = run {
                        val v = when {
                            t.has("addedAt") && !t.isNull("addedAt") -> t.optLong("addedAt")
                            t.has("added") && !t.isNull("added") -> t.optLong("added")
                            else -> 0L
                        }
                        // Terima epoch detik maupun milidetik.
                        if (v <= 0L) null else if (v < 100_000_000_000L) v * 1000 else v
                    },
                )
            )
          } catch (e: Exception) {
            // Satu judul rusak tidak boleh menggagalkan seluruh katalog.
            continue
          }
        }
        lastCatalogIds = out.map { it.id }.toSet()
        return out
    }

    /** Rincian kegagalan terakhir tiap sumber katalog (untuk diagnosis di layar). */
    @Volatile
    var lastCatalogError: String? = null

    /**
     * Ambil katalog mentah: Cloudflare Pages dulu (tanpa jatah harian),
     * lalu API Worker; bila gagal (mis. kuota Worker Cloudflare habis —
     * galat 1027/429), jatuh ke cermin-cermin jsDelivr (edge berbeda) lalu
     * raw.githubusercontent dari repo gudang yang sama. Seluruh rantai
     * dicoba dua putaran sebelum menyerah; tiap kegagalan dicatat di
     * [lastCatalogError] agar penyebab aslinya terlihat di layar galat.
     */
    suspend fun fetchRawCatalog(): String = withContext(Dispatchers.IO) {
        val sources = listOf(
            "Pages" to CATALOG_PAGES,
            "jsDelivr" to CATALOG_MIRROR,
            "gcore" to CATALOG_MIRROR_GCORE,
            "raw" to CATALOG_MIRROR2,
            "fastly" to CATALOG_MIRROR_FASTLY,
        )
        val errs = ArrayList<String>()
        var last: Exception? = null
        for (round in 1..2) {
            for ((name, u) in sources) {
                try {
                    val body = decodeCatalogBody(httpGetBytes(u, 30000))
                    if (!body.trimStart().startsWith("{")) {
                        throw java.io.IOException("isi katalog tidak dikenal dari " + name)
                    }
                    lastCatalogError = null
                    return@withContext body
                } catch (e: Exception) {
                    last = e
                    if (round == 2) {
                        errs.add("$name: ${e.javaClass.simpleName}: ${e.message?.take(90) ?: "?"}")
                    }
                }
            }
        }
        lastCatalogError = errs.joinToString(" · ")
        throw last ?: java.io.IOException("semua sumber katalog gagal")
    }

    suspend fun fetchCatalog(): List<Title> = parseCatalog(fetchRawCatalog())

    /**
     * Pintu-pintu CDN untuk satu URL playlist gudang: berkas yang sama di
     * GitHub bisa diambil lewat banyak pintu gratis (keluarga jsDelivr,
     * cermin jsDelivr, GitHack, raw GitHub, proksi, Statically). Urutan
     * sesuai uji hidup 31 Okt 2026 atas berkas gudang bernama baru.
     * URL non-jsDelivr dikembalikan apa adanya (satu pintu).
     */
    fun doorVariants(url: String): List<String> {
        val m = Regex("""^https://[A-Za-z0-9.-]+/gh/([^/]+)/([^/@]+)@([^/]+)/(.+)$""").find(url)
            ?: return listOf(url)
        val (owner, repo, pin, path) = m.destructured
        val gh = "$owner/$repo@$pin/$path"
        val raw = "$owner/$repo/$pin/$path"
        return listOf(
            "https://cdn.jsdelivr.net/gh/$gh",
            "https://fastly.jsdelivr.net/gh/$gh",
            "https://gcore.jsdelivr.net/gh/$gh",
            "https://quantil.jsdelivr.net/gh/$gh",
            "https://testingcf.jsdelivr.net/gh/$gh",
            "https://jsdelivr.b-cdn.net/gh/$gh",
            "https://cdn.jsdmirror.com/gh/$gh",
            "https://raw.githack.com/$raw",
            "https://raw.githubusercontent.com/$raw",
            "https://ghproxy.net/https://raw.githubusercontent.com/$raw",
            "https://cdn.statically.io/gh/$gh",
        )
    }

    /** Pilih pintu pertama yang menjawab playlist valid (#EXTM3U). */
    suspend fun pickDoor(url: String): String = withContext(Dispatchers.IO) {
        val doors = doorVariants(url)
        if (doors.size == 1) return@withContext url
        for (d in doors) {
            try {
                if (httpGetText(d, timeoutMs = 5000).trimStart().startsWith("#EXTM3U")) {
                    return@withContext d
                }
            } catch (e: Exception) {
                // pintu ini gagal -> coba pintu berikutnya
            }
        }
        url
    }

    /** Daftar "Segera Tayang": judul antrean gudang yang belum terbit. */
    suspend fun fetchSoon(): List<SoonTitle> = withContext(Dispatchers.IO) {
        val body = try {
            decodeCatalogBody(httpGetBytes(SOON_PAGES))
        } catch (e: Exception) {
            httpGetText(SOON_MIRROR)
        }
        // API membungkus {titles:[...]}; berkas cermin adalah array telanjang.
        val trimmed = body.trimStart()
        val arr = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            JSONObject(trimmed).optJSONArray("titles") ?: JSONArray()
        }
        val out = ArrayList<SoonTitle>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            val genres = ArrayList<String>()
            val ga = t.optJSONArray("genres") ?: JSONArray()
            for (j in 0 until ga.length()) genres.add(ga.optString(j))
            out.add(
                SoonTitle(
                    id = t.optString("id"),
                    title = t.optString("title"),
                    poster = t.optString("poster").ifBlank { null },
                    banner = t.optString("banner").ifBlank { null },
                    synopsis = t.optString("synopsis").ifBlank { null },
                    year = if (t.has("year") && !t.isNull("year")) t.optInt("year") else null,
                    score = if (t.has("score") && !t.isNull("score")) t.optDouble("score") else null,
                    genres = genres,
                    totalEpisodes = t.optInt("totalEpisodes"),
                )
            )
        }
        // Judul yang sudah tayang tidak ditampilkan sebagai "segera tayang"
        // (penyaringan ini biasanya dilakukan Worker; cermin perlu disaring di sini).
        out.filter { !lastCatalogIds.contains(it.id) }
    }
}

/** Judul yang sedang disalin ke gudang dan akan segera tayang (tab Baru). */
data class SoonTitle(
    val id: String,
    val title: String,
    val poster: String? = null,
    val banner: String? = null,
    val synopsis: String? = null,
    val year: Int? = null,
    val score: Double? = null,
    val genres: List<String> = emptyList(),
    val totalEpisodes: Int = 0,
) {
    val matchPercent: Int? get() = score?.let { (it * 10).toInt().coerceIn(0, 100) }
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

private val Context.dataStore by preferencesDataStore(name = "anilover_store")
private val HISTORY_KEY = stringPreferencesKey("history_json")
private val MYLIST_KEY = stringPreferencesKey("mylist_json")
private val RATINGS_KEY = stringPreferencesKey("ratings_json")
private val HIDDEN_KEY = stringPreferencesKey("hidden_continue_json")
private val AUTOPLAY_KEY = booleanPreferencesKey("autoplay_next")
private val SUBTITLE_KEY = stringPreferencesKey("subtitle_default")
private val SUBSIZE_KEY = stringPreferencesKey("subtitle_size")
private val SUBCOLOR_KEY = stringPreferencesKey("subtitle_color")
private val REMINDERS_KEY = stringPreferencesKey("reminders_json")
private val KNOWN_IDS_KEY = stringPreferencesKey("known_catalog_ids")
private val SMART_KEY = booleanPreferencesKey("smart_downloads")
private val WIFIONLY_KEY = booleanPreferencesKey("wifi_only_downloads")
private val CACHE_KEY = stringPreferencesKey("catalog_cache")
private val RECENT_KEY = stringPreferencesKey("recent_searches_json")
private val DOWNLOADS_KEY = stringPreferencesKey("downloads_json")

/**
 * Penangkap galat tak tertangani: jejak tumpukan disimpan ke berkas agar bisa
 * ditampilkan & dibagikan pada pembukaan berikutnya (diagnosis keluar sendiri).
 */
object CrashReporter {
    private const val NAME = "last-crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = java.io.StringWriter()
                error.printStackTrace(java.io.PrintWriter(sw))
                val text = sw.toString()
                File(app.filesDir, NAME)
                    .writeText(text.takeLast(7000))
            } catch (e: Throwable) { /* abaikan */ }
            prev?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = try {
        val f = File(context.filesDir, NAME)
        if (f.exists()) f.readText().ifBlank { null } else null
    } catch (e: Throwable) { null }

    fun clear(context: Context) {
        try { File(context.filesDir, NAME).delete() } catch (e: Throwable) { }
    }
}

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

    /** Ganti SELURUH riwayat sekaligus (dipakai sinkronisasi cloud saat data remote lebih baru). */
    suspend fun replaceAll(entries: List<WatchEntry>) {
        context.dataStore.edit { it[HISTORY_KEY] = write(entries) }
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

data class AniloverSettings(
    val autoplayNext: Boolean = true,
    val subtitleDefault: String = "id",
    /** Ukuran teks subtitle: kecil | sedang | besar. */
    val subtitleSize: String = "sedang",
    /** Warna teks subtitle: putih | kuning | hijau. */
    val subtitleColor: String = "putih",
    /** Unduhan Cerdas: unduh episode berikutnya otomatis + hapus yang sudah ditonton. */
    val smartDownloads: Boolean = false,
    /** Unduhan Cerdas hanya berjalan saat Wi-Fi. */
    val wifiOnlyDownloads: Boolean = false,
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
    val settings: Flow<AniloverSettings> = context.dataStore.data.map { prefs ->
        AniloverSettings(
            autoplayNext = prefs[AUTOPLAY_KEY] ?: true,
            subtitleDefault = prefs[SUBTITLE_KEY] ?: "id",
            subtitleSize = prefs[SUBSIZE_KEY] ?: "sedang",
            subtitleColor = prefs[SUBCOLOR_KEY] ?: "putih",
            smartDownloads = prefs[SMART_KEY] ?: false,
            wifiOnlyDownloads = prefs[WIFIONLY_KEY] ?: false,
        )
    }
    val reminders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        parseStringSet(prefs[REMINDERS_KEY])
    }
    val downloads: Flow<List<DownloadItem>> = context.dataStore.data.map { prefs ->
        parseDownloads(prefs[DOWNLOADS_KEY])
    }
    /** Riwayat pencarian, terbaru di depan (maks 8). */
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        parseStringList(prefs[RECENT_KEY])
    }

    suspend fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        context.dataStore.edit { prefs ->
            val list = parseStringList(prefs[RECENT_KEY])
                .filter { !it.equals(q, ignoreCase = true) }
                .toMutableList()
            list.add(0, q)
            val arr = JSONArray()
            for (v in list.take(8)) arr.put(v)
            prefs[RECENT_KEY] = arr.toString()
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(RECENT_KEY) }
    }

    suspend fun toggleMyList(id: String) {
        context.dataStore.edit { prefs ->
            val set = parseStringSet(prefs[MYLIST_KEY]).toMutableSet()
            if (!set.add(id)) set.remove(id)
            prefs[MYLIST_KEY] = writeStringSet(set)
        }
    }

    // --- Pengganti massal untuk sinkronisasi cloud (CloudSync) ---

    suspend fun replaceMyList(ids: Set<String>) {
        context.dataStore.edit { it[MYLIST_KEY] = writeStringSet(ids) }
    }

    suspend fun replaceHidden(set: Set<String>) {
        context.dataStore.edit { it[HIDDEN_KEY] = writeStringSet(set) }
    }

    suspend fun replaceReminders(set: Set<String>) {
        context.dataStore.edit { it[REMINDERS_KEY] = writeStringSet(set) }
    }

    suspend fun replaceRatings(map: Map<String, Int>) {
        context.dataStore.edit { prefs ->
            val obj = JSONObject()
            for ((k, v) in map) obj.put(k, v)
            prefs[RATINGS_KEY] = obj.toString()
        }
    }

    suspend fun replaceSettings(s: AniloverSettings) {
        context.dataStore.edit { prefs ->
            prefs[AUTOPLAY_KEY] = s.autoplayNext
            prefs[SUBTITLE_KEY] = s.subtitleDefault
            prefs[SUBSIZE_KEY] = s.subtitleSize
            prefs[SUBCOLOR_KEY] = s.subtitleColor
            prefs[SMART_KEY] = s.smartDownloads
            prefs[WIFIONLY_KEY] = s.wifiOnlyDownloads
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

    suspend fun setSubtitleStyle(size: String, color: String) {
        context.dataStore.edit { prefs ->
            prefs[SUBSIZE_KEY] = size
            prefs[SUBCOLOR_KEY] = color
        }
    }

    suspend fun toggleReminder(id: String) {
        context.dataStore.edit { prefs ->
            val set = parseStringSet(prefs[REMINDERS_KEY]).toMutableSet()
            if (!set.add(id)) set.remove(id)
            prefs[REMINDERS_KEY] = writeStringSet(set)
        }
    }

    suspend fun remindersNow(): Set<String> =
        parseStringSet(context.dataStore.data.first()[REMINDERS_KEY])

    suspend fun removeReminders(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            val set = parseStringSet(prefs[REMINDERS_KEY]).toMutableSet()
            set.removeAll(ids)
            prefs[REMINDERS_KEY] = writeStringSet(set)
        }
    }

    /** Id katalog yang terakhir diketahui pemeriksa latar; null = belum pernah jalan. */
    suspend fun knownIdsNow(): Set<String>? {
        val raw = context.dataStore.data.first()[KNOWN_IDS_KEY] ?: return null
        return parseStringSet(raw)
    }

    suspend fun setKnownIds(ids: Set<String>) {
        context.dataStore.edit { it[KNOWN_IDS_KEY] = writeStringSet(ids) }
    }

    suspend fun setSmartDownloads(value: Boolean) {
        context.dataStore.edit { it[SMART_KEY] = value }
    }

    suspend fun setWifiOnlyDownloads(value: Boolean) {
        context.dataStore.edit { it[WIFIONLY_KEY] = value }
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

    private fun parseStringList(s: String?): List<String> = try {
        if (s.isNullOrBlank()) emptyList()
        else {
            val arr = JSONArray(s)
            val out = ArrayList<String>()
            for (i in 0 until arr.length()) out.add(arr.optString(i))
            out
        }
    } catch (e: Exception) { emptyList() }

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

private val URI_ATTR = Regex("URI=\"([^\"]+)\"")
private val BANDWIDTH_ATTR = Regex("BANDWIDTH=(\\d+)")

/**
 * Unduhan offline: salin playlist HLS + semua berkasnya (segmen, init fMP4, kunci)
 * ke penyimpanan aplikasi. Playlist lokal ditulis ULANG dengan nama berkas
 * lokal dan baru disimpan setelah SEMUA berkas selesai, sehingga episode yang
 * belum lengkap tidak pernah diputar dari berkas (dulu playlist ditulis di
 * awal -> episode setengah unduh/gagal diputar lokal lalu error).
 */
class DownloadManager(private val context: Context, private val library: LibraryStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = HashSet<String>()

    init {
        // Unduhan yang tertinggal berstatus "mengunduh" karena aplikasi dimatikan
        // paksa ditandai gagal agar bisa dicoba lagi (bukan berputar selamanya).
        scope.launch {
            try {
                for (d in library.downloadsNow()) {
                    val key = "${d.titleId}|${d.episode}"
                    val active = synchronized(running) { running.contains(key) }
                    if (d.state == "mengunduh" && !active) {
                        library.upsertDownload(d.copy(state = "gagal"))
                    }
                }
            } catch (e: Exception) { /* abaikan */ }
        }
    }

    fun dirFor(titleId: String, episode: Int): File =
        File(context.filesDir, "downloads/$titleId/ep$episode")

    /** URI playlist lokal hanya bila SEMUA berkas yang dirujuknya ada dan tidak kosong. */
    fun localPlaylistUri(titleId: String, episode: Int): String? {
        val dir = dirFor(titleId, episode)
        val f = File(dir, "playlist.m3u8")
        if (!f.exists() || f.length() == 0L) return null
        return try {
            fun present(name: String): Boolean {
                val x = File(dir, name)
                return x.exists() && x.length() > 0
            }
            val ok = f.readLines().all { raw ->
                val line = raw.trim()
                when {
                    line.isEmpty() -> true
                    line.startsWith("#") -> {
                        val m = URI_ATTR.find(line)
                        m == null || present(m.groupValues[1])
                    }
                    else -> present(line)
                }
            }
            if (ok) "file://" + f.absolutePath else null
        } catch (e: Exception) {
            null
        }
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
                File(dir, "playlist.m3u8").delete()

                var playlistUrl = episode.playlist
                var text = httpGetText(playlistUrl)
                // Master playlist: ambil satu varian (bitrate tertinggi).
                if (text.contains("#EXT-X-STREAM-INF")) {
                    playlistUrl = pickVariant(playlistUrl, text)
                    text = httpGetText(playlistUrl)
                }

                val urlToName = LinkedHashMap<String, String>()
                fun nameFor(ref: String): String {
                    val abs = resolveUrl(playlistUrl, ref)
                    return urlToName.getOrPut(abs) {
                        val path = abs.substringBefore("?").substringBefore("#")
                        val last = path.substringAfterLast("/")
                        val ext = if (last.contains(".")) last.substringAfterLast(".").take(5) else ""
                        "f" + "%05d".format(urlToName.size) + if (ext.isNotEmpty()) ".$ext" else ""
                    }
                }

                val rewritten = StringBuilder()
                for (raw in text.lines()) {
                    val line = raw.trim()
                    when {
                        line.isEmpty() -> Unit
                        line.startsWith("#") -> {
                            val m = URI_ATTR.find(line)
                            val out = if (m != null) {
                                line.replace(m.value, "URI=\"" + nameFor(m.groupValues[1]) + "\"")
                            } else line
                            rewritten.append(out).append('\n')
                        }
                        else -> rewritten.append(nameFor(line)).append('\n')
                    }
                }

                val total = urlToName.size
                item = item.copy(totalSegs = total)
                library.upsertDownload(item)
                var bytes = 0L
                var done = 0
                for ((url, name) in urlToName) {
                    val outFile = File(dir, name)
                    if (!outFile.exists() || outFile.length() == 0L) httpDownload(url, outFile)
                    bytes += outFile.length()
                    done++
                    if (done % 3 == 0 || done == total) {
                        item = item.copy(doneSegs = done, sizeBytes = bytes, updatedAt = System.currentTimeMillis())
                        library.upsertDownload(item)
                    }
                }

                // Playlist lokal ditulis TERAKHIR (atomik lewat berkas sementara).
                val tmp = File(dir, "playlist.tmp")
                tmp.writeText(rewritten.toString())
                if (!tmp.renameTo(File(dir, "playlist.m3u8"))) {
                    File(dir, "playlist.m3u8").writeText(rewritten.toString())
                    tmp.delete()
                }
                item = item.copy(state = "selesai", doneSegs = total, sizeBytes = bytes, updatedAt = System.currentTimeMillis())
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

    private fun resolveUrl(base: String, ref: String): String {
        if (ref.startsWith("http://") || ref.startsWith("https://")) return ref
        return try {
            java.net.URI(base).resolve(ref).toString()
        } catch (e: Exception) {
            if (ref.startsWith("/")) URL(base).let { "${it.protocol}://${it.host}$ref" }
            else base.substringBeforeLast("/") + "/" + ref
        }
    }

    private fun pickVariant(masterUrl: String, master: String): String {
        var bestBw = -1L
        var best: String? = null
        var pendingBw = 0L
        var expectUri = false
        for (raw in master.lines()) {
            val line = raw.trim()
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                pendingBw = BANDWIDTH_ATTR.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                expectUri = true
            } else if (expectUri && line.isNotEmpty() && !line.startsWith("#")) {
                if (pendingBw > bestBw) { bestBw = pendingBw; best = line }
                expectUri = false
            }
        }
        return resolveUrl(masterUrl, best ?: throw java.io.IOException("master playlist tanpa varian"))
    }

    private fun connect(url: String, readTimeout: Int): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = readTimeout
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Anilover Android)")
        if (conn.responseCode !in 200..299) {
            val code = conn.responseCode
            conn.disconnect()
            throw java.io.IOException("HTTP $code dari $url")
        }
        return conn
    }

    private fun httpGetText(url: String): String {
        val conn = connect(url, 30000)
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Unduh ke berkas .part lalu ganti nama: berkas terpotong tak dianggap utuh. */
    private fun httpDownload(url: String, outFile: File) {
        val part = File(outFile.parentFile, outFile.name + ".part")
        val conn = connect(url, 60000)
        try {
            conn.inputStream.use { input ->
                part.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            }
        } finally {
            conn.disconnect()
        }
        if (!part.renameTo(outFile)) {
            part.copyTo(outFile, overwrite = true)
            part.delete()
        }
    }
}
