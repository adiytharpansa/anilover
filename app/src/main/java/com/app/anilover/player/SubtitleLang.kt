package com.app.anilover.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import java.util.Locale

/**
 * Logika bahasa subtitle/audio pemutar:
 *  - "auto" mengikuti bahasa aplikasi (lalu cadangan Inggris bila trek bahasa itu tidak ada),
 *  - kode trek ("ind", "id-ID", "in") dan label ("Indonesian") dinormalkan ke satu kode bahasa,
 *  - nama trek ditampilkan dalam bahasa aplikasi ("Bahasa Indonesia", "Inggris", ...).
 */
object SubtitleLang {
    const val AUTO = "auto"
    const val OFF = "off"
    private const val FALLBACK = "en"

    /** Satu baris pilihan trek di panel Audio & Subtitle. */
    data class Entry(
        val group: Tracks.Group,
        val index: Int,
        val title: String,
        val lang: String?,
        val selected: Boolean,
    )

    // ISO 639-2 (3 huruf) -> ISO 639-1 (2 huruf), mis. "ind" -> "id".
    private val iso3to2: Map<String, String> by lazy {
        Locale.getISOLanguages()
            .mapNotNull { c -> runCatching { Locale(c).isO3Language to c }.getOrNull() }
            .toMap()
    }

    // Nama bahasa (Inggris, Indonesia, dan bahasa itu sendiri) -> kode, untuk trek yang hanya punya label.
    private val nameToCode: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        for (c in Locale.getISOLanguages()) {
            val loc = Locale(c)
            for (shownIn in listOf(Locale.ENGLISH, Locale("id"), loc)) {
                val n = loc.getDisplayLanguage(shownIn).lowercase(Locale.ROOT)
                if (n.isNotBlank() && n != c) map.putIfAbsent(n, c)
            }
        }
        map["bahasa indonesia"] = "id"
        map
    }

    /** Kode bahasa 2 huruf, atau null bila tidak diketahui ("und", kosong). */
    fun normalize(code: String?): String? {
        if (code.isNullOrBlank()) return null
        val base = code.substringBefore('-').substringBefore('_').trim().lowercase(Locale.ROOT)
        if (base.isEmpty() || base == "und" || base == "zxx" || base == "mis") return null
        return when (base) {
            "in" -> "id"
            "iw" -> "he"
            "ji" -> "yi"
            else -> if (base.length == 3) iso3to2[base] ?: base else base
        }
    }

    /** Bahasa sebuah trek: dari atribut language, kalau kosong dari labelnya. */
    fun languageOf(f: Format): String? {
        normalize(f.language)?.let { return it }
        val label = f.label?.replace(Regex("[\\[(].*$"), "")?.trim()?.lowercase(Locale.ROOT)
        return if (label.isNullOrEmpty()) null else nameToCode[label]
    }

    /** Bahasa aplikasi saat ini (mengikuti pilihan bahasa per-aplikasi / bahasa perangkat). */
    fun appLocale(context: Context): Locale = context.resources.configuration.locales[0]

    fun appLanguage(context: Context): String = normalize(appLocale(context).language) ?: FALLBACK

    /** Bahasa subtitle yang diminta dari pengaturan; null = subtitle dimatikan. */
    fun resolve(setting: String, context: Context): String? = when (setting) {
        OFF -> null
        AUTO -> appLanguage(context)
        else -> normalize(setting) ?: appLanguage(context)
    }

    /** Nama bahasa dalam bahasa aplikasi, mis. "Bahasa Indonesia" / "Indonesian". */
    fun displayName(lang: String, locale: Locale): String {
        if (lang == "id" && locale.language == "id") return "Bahasa Indonesia"
        val name = Locale(lang).getDisplayLanguage(locale).ifBlank { lang }
        return name.replaceFirstChar { it.titlecase(locale) }
    }

    /**
     * Trek teks terbaik untuk [lang]; bila tidak ada dan [allowFallback], pakai Inggris.
     * Trek "forced" dan yang tidak didukung dilewati kecuali tidak ada pilihan lain.
     */
    fun pick(groups: List<Tracks.Group>, lang: String, allowFallback: Boolean = true): Pair<Tracks.Group, Int>? {
        fun find(target: String): Pair<Tracks.Group, Int>? {
            var best: Pair<Tracks.Group, Int>? = null
            var bestScore = -1
            for (g in groups) {
                if (g.type != C.TRACK_TYPE_TEXT) continue
                for (i in 0 until g.length) {
                    if (!g.isTrackSupported(i)) continue
                    val f = g.getTrackFormat(i)
                    if (languageOf(f) != target) continue
                    var score = 0
                    if ((f.selectionFlags and C.SELECTION_FLAG_FORCED) == 0) score += 2
                    if ((f.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0) score += 1
                    if (score > bestScore) { best = g to i; bestScore = score }
                }
            }
            return best
        }
        return find(lang) ?: if (allowFallback && lang != FALLBACK) find(FALLBACK) else null
    }

    /**
     * Daftar pilihan untuk panel: nama dilokalkan, duplikat digabung, diurutkan
     * (bahasa aplikasi -> Inggris -> lainnya -> tanpa nama).
     */
    fun entries(
        groups: List<Tracks.Group>,
        type: Int,
        locale: Locale,
        unnamed: (Int) -> String,
        forcedSuffix: String,
    ): List<Entry> {
        val appLang = normalize(locale.language) ?: FALLBACK
        val map = LinkedHashMap<String, Entry>()
        var n = 0
        for (g in groups) {
            if (g.type != type) continue
            for (i in 0 until g.length) {
                if (!g.isTrackSupported(i)) continue
                val f = g.getTrackFormat(i)
                n++
                val lang = languageOf(f)
                val forced = (f.selectionFlags and C.SELECTION_FLAG_FORCED) != 0
                var title = when {
                    lang != null -> displayName(lang, locale)
                    !f.label.isNullOrBlank() -> f.label!!.trim()
                    else -> unnamed(n)
                }
                if (forced) title += " ($forcedSuffix)"
                val key = (lang ?: "#${f.label ?: n}") + "|" + forced
                val selected = g.isTrackSelected(i)
                val old = map[key]
                if (old != null && !selected) continue // duplikat: pertahankan yang terpilih
                map[key] = Entry(g, i, title, lang, selected)
            }
        }
        fun rank(e: Entry) = when (e.lang) {
            null -> 3
            appLang -> 0
            FALLBACK -> 1
            else -> 2
        }
        return map.values.sortedWith(compareBy<Entry>({ rank(it) }, { it.title.lowercase(locale) }))
            .let { sorted -> if (sorted.all { it.lang == null }) map.values.toList() else sorted }
    }
}
