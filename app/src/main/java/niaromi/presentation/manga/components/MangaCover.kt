package niaromi.presentation.manga.components

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import niaromi.R
import niaromi.app.di.appGraph
import niaromi.domain.manga.model.asMangaCover
import niaromi.presentation.util.rememberResourceBitmapPainter
import niaromi.domain.manga.model.MangaCover as CoverData

enum class MangaCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    ;

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
        onError: (() -> Unit)? = null,
        contentScale: ContentScale = ContentScale.Crop,
    ) {
        val cover = data as? CoverData
        val context = LocalContext.current
        var failed by remember(cover) { mutableStateOf(false) }
        var recovered by remember(cover) { mutableStateOf<CoverData?>(null) }

        LaunchedEffect(cover, failed) {
            if (cover == null || !failed) return@LaunchedEffect
            val graph = context.appGraph
            if (graph.sourceManager.get(cover.sourceId) is HttpSource) {
                recovered = coverRecoverySlots.withPermit {
                    val manga = graph.getManga.await(cover.mangaId) ?: return@withPermit null
                    val current = manga.asMangaCover()
                    if (current != cover && !current.url.isNullOrBlank()) {
                        current
                    } else if (shouldRecoverCover(cover.mangaId)) {
                        graph.updateMangaFromRemote(manga, fetchDetails = true, manualFetch = true)
                            .getOrNull()?.manga?.asMangaCover()
                    } else {
                        null
                    }
                }
            }
        }

        AsyncImage(
            model = recovered ?: data,
            placeholder = ColorPainter(CoverPlaceholderColor),
            error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
            contentDescription = contentDescription,
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            fallback = rememberResourceBitmapPainter(id = R.drawable.cover_error),
            onError = {
                failed = true
                onError?.invoke()
            },
            contentScale = contentScale,
        )
    }
}

private val CoverPlaceholderColor = Color(0x1F888888)

private val coverRecoverySlots = Semaphore(2)
private val recentCoverRecoveries = LinkedHashMap<Long, Long>()

private fun shouldRecoverCover(id: Long): Boolean {
    val now = SystemClock.elapsedRealtime()
    recentCoverRecoveries.entries.removeAll { now - it.value >= 300_000L }
    if (id in recentCoverRecoveries) return false
    recentCoverRecoveries[id] = now
    return true
}
