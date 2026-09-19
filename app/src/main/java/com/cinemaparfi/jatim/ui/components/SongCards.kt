package com.cinemaparfi.jatim.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cinemaparfi.jatim.R
import com.cinemaparfi.jatim.data.model.Song

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongCover(song = song, modifier = Modifier.size(width = 72.dp, height = 104.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Kreator: ${song.director.ifBlank { song.artist }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onArtistClick != null) Modifier.clickable { onArtistClick(song.artist) } else Modifier,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.releaseYear.isNotBlank()) {
                    Text(
                        text = "Rilis: ${song.releaseYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(song.genre) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (onAddToPlaylist != null) {
                    TextButton(onClick = onAddToPlaylist) {
                        Text("+Playlist")
                    }
                }
            }
        }
    }
}

@Composable
fun MoviePosterCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(132.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SongCover(
            song = song,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (song.director.isNotBlank()) {
            Text(
                text = "Kreator: ${song.director}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (song.releaseYear.isNotBlank()) {
            Text(
                text = "Rilis: ${song.releaseYear}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = song.genre,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongCover(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
    val fallbackPainter = painterResource(id = R.drawable.splash)
    val safeCoverUrl = song.coverUrl.trim()
    val imageModelUrl = safeCoverUrl.toDisplayImageUrl()
    val isRemoteUrl = imageModelUrl.startsWith("http://", ignoreCase = true) || imageModelUrl.startsWith("https://", ignoreCase = true)
    Log.d(
        "SongCover",
        "title=${song.title} cover_url=$safeCoverUrl imageModel=$imageModelUrl audio_url=${song.audioUrl} isRemote=$isRemoteUrl"
    )

    if (isRemoteUrl) {
        AsyncImage(
            model = imageModelUrl,
            contentDescription = "${song.title} poster",
            modifier = modifier
                .clip(shape)
                .background(Color(0x33222222)),
            contentScale = ContentScale.Fit,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter,
            onError = { state ->
                Log.e(
                    "SongCover",
                    "Failed loading cover for title=${song.title}, cover_url=$safeCoverUrl, imageModel=$imageModelUrl",
                    state.result.throwable
                )
            },
            onSuccess = {
                Log.d("SongCover", "Loaded cover for title=${song.title}, cover_url=$safeCoverUrl, imageModel=$imageModelUrl")
            }
        )
        return
    }

    Image(
        painter = fallbackPainter,
        contentDescription = "${song.title} poster fallback",
        modifier = modifier
            .clip(shape)
            .background(Color(0x33222222)),
        contentScale = ContentScale.Fit
    )
}

private fun String.toDisplayImageUrl(): String {
    val rawUrl = trim()
    if (rawUrl.isBlank()) return ""
    val fileId = rawUrl.extractGoogleDriveFileId() ?: return rawUrl
    return "https://drive.google.com/thumbnail?id=$fileId&sz=w1200"
}

private fun String.extractGoogleDriveFileId(): String? {
    return runCatching {
        val uri = Uri.parse(this)
        val host = uri.host.orEmpty().lowercase()
        if (!host.contains("drive.google.com") && !host.contains("docs.google.com")) return@runCatching null

        uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }?.let { return@runCatching it }

        val segments = uri.pathSegments
        val fileIndex = segments.indexOf("d")
        if (fileIndex >= 0 && fileIndex + 1 < segments.size) {
            return@runCatching segments[fileIndex + 1].takeIf { it.isNotBlank() }
        }

        null
    }.getOrNull()
}
