package com.app.anilover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Pengganti AsyncImage untuk semua poster/banner:
 *  - memuat  -> kotak shimmer (bukan kotak gelap kosong),
 *  - gagal / URL kosong -> kartu cadangan berisi ikon + judul,
 *  - tampil dengan crossfade halus.
 * Nama parameter sengaja sama dengan AsyncImage agar mudah diganti.
 */
@Composable
fun PosterImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    val label = contentDescription ?: ""
    if (model == null || (model is String && model.isBlank())) {
        PosterFallback(label, modifier)
        return
    }
    val context = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(context).data(model).crossfade(300).build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        loading = { ShimmerBox(Modifier.fillMaxSize(), RectangleShape) },
        error = { PosterFallback(label, Modifier.fillMaxSize()) },
    )
}

/** Kartu cadangan saat gambar tidak ada / gagal dimuat. */
@Composable
fun PosterFallback(title: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier.background(
            Brush.verticalGradient(listOf(AniloverSurfaceHigh, AniloverSurface)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        val roomy = maxWidth > 72.dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(if (roomy) 10.dp else 2.dp),
        ) {
            Icon(
                Icons.Filled.Movie, contentDescription = null,
                tint = AniloverTextDim.copy(alpha = 0.6f),
                modifier = Modifier.size(if (roomy) 28.dp else 18.dp),
            )
            if (roomy && title.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    title, color = AniloverTextDim,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
