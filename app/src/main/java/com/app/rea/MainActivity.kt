package com.app.rea

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.rea.data.CatalogApi
import com.app.rea.data.HistoryStore
import com.app.rea.data.Title
import com.app.rea.data.WatchEntry
import com.app.rea.player.PlayerActivity
import com.app.rea.ui.DetailScreen
import com.app.rea.ui.HomeScreen
import com.app.rea.ui.ReaBackground
import com.app.rea.ui.ReaTheme
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = HistoryStore(applicationContext)
        setContent {
            ReaTheme {
                AppRoot(store) { title, episode, startPos ->
                    val eps = JSONArray()
                    for (e in title.episodes) {
                        eps.put(JSONObject().put("n", e.number).put("p", e.playlist))
                    }
                    startActivity(
                        Intent(this, PlayerActivity::class.java).apply {
                            putExtra(PlayerActivity.EXTRA_TITLE_ID, title.id)
                            putExtra(PlayerActivity.EXTRA_TITLE, title.title)
                            putExtra(PlayerActivity.EXTRA_EPISODE, episode)
                            putExtra(PlayerActivity.EXTRA_EPISODES, eps.toString())
                            putExtra(PlayerActivity.EXTRA_START_POS, startPos)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    store: HistoryStore,
    onPlay: (Title, Int, Long) -> Unit,
) {
    var titles by remember { mutableStateOf<List<Title>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Title?>(null) }
    val history by store.entries.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        try {
            titles = CatalogApi.fetchCatalog()
        } catch (e: Exception) {
            error = "Tidak bisa memuat katalog. Periksa koneksi internetmu."
        }
    }

    Box(Modifier.fillMaxSize().background(ReaBackground)) {
        when {
            error != null -> Text(
                error!!, color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
            titles == null -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
            selected != null -> DetailScreen(
                title = selected!!,
                history = history,
                onBack = { selected = null },
                onPlay = { ep, pos -> onPlay(selected!!, ep, pos) },
            )
            else -> HomeScreen(
                titles = titles!!,
                history = history,
                onOpen = { selected = it },
                onPlay = { t, ep, pos -> onPlay(t, ep, pos) },
            )
        }
    }
}
