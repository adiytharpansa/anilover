package com.app.anilover.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ikon bilah navigasi bawah: garis tipis berujung bulat dengan gaya seragam
 * (kisi 24, goresan 1,8). Aktif = goresan sedikit lebih tebal + isian lembut
 * transparan (warna mengikuti tint aksen). Bentuk ikon mengacu ke set Lucide (ISC).
 */
object AniloverNavIcons {
    /** Satu bentuk: data path SVG + apakah area di dalamnya diberi isian saat aktif. */
    private class Shape(val d: String, val fill: Boolean)

    private fun build(name: String, shapes: List<Shape>, active: Boolean): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            for (s in shapes) {
                addPath(
                    pathData = addPathNodes(s.d),
                    fill = if (active && s.fill) SolidColor(Color.Black) else null,
                    fillAlpha = 0.22f,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = if (active) 2.1f else 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private val home = listOf(
        Shape("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", false),
        Shape(
            "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
            true,
        ),
    )
    private val search = listOf(
        Shape("M3 11a8 8 0 1 0 16 0a8 8 0 1 0-16 0", true),
        Shape("M21 21l-4.34-4.34", false),
    )
    private val sparkles = listOf(
        Shape(
            "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
            true,
        ),
        Shape("M20 3v4", false),
        Shape("M22 5h-4", false),
        Shape("M4 17v2", false),
        Shape("M5 18H3", false),
    )
    private val download = listOf(
        Shape("M12 15V3", false),
        Shape("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", true),
        Shape("M7 10l5 5 5-5", false),
    )
    private val user = listOf(
        Shape("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", true),
        Shape("M16 7a4 4 0 1 0-8 0a4 4 0 1 0 8 0", true),
    )

    val Home: ImageVector by lazy { build("NavHome", home, false) }
    val HomeActive: ImageVector by lazy { build("NavHomeActive", home, true) }
    val Search: ImageVector by lazy { build("NavSearch", search, false) }
    val SearchActive: ImageVector by lazy { build("NavSearchActive", search, true) }
    val New: ImageVector by lazy { build("NavNew", sparkles, false) }
    val NewActive: ImageVector by lazy { build("NavNewActive", sparkles, true) }
    val Downloads: ImageVector by lazy { build("NavDownloads", download, false) }
    val DownloadsActive: ImageVector by lazy { build("NavDownloadsActive", download, true) }
    val Profile: ImageVector by lazy { build("NavProfile", user, false) }
    val ProfileActive: ImageVector by lazy { build("NavProfileActive", user, true) }
}
