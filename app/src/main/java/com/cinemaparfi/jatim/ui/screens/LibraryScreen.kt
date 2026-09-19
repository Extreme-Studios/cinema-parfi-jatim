@file:OptIn(ExperimentalLayoutApi::class)

package com.cinemaparfi.jatim.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinemaparfi.jatim.R
import com.cinemaparfi.jatim.data.model.Song
import com.cinemaparfi.jatim.data.model.VideoCollection
import com.cinemaparfi.jatim.ui.components.SongCover
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val LibraryBlack = Color(0xFF030607)
private val LibraryGold = Color(0xFFE7B946)
private val LibraryPaper = Color(0xFFF5F2EB)
private val LibraryMuted = Color(0xFFB5B6B8)
private val LibraryPill = Color(0xFF161B1F)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    videoCollections: List<VideoCollection>,
    genres: List<String>,
    errorMessage: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayTrailer: (Song) -> Unit,
    onOpenPlaylist: (VideoCollection) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Semua") }
    val filteredSongs = remember(songs, query, selectedGenre) {
        songs.filter { song ->
            val matchesQuery = query.isBlank() || song.title.contains(query, true) || song.artist.contains(query, true) || song.genre.contains(query, true)
            val matchesGenre = selectedGenre == "Semua" || song.genre.equals(selectedGenre, true) || song.category.equals(selectedGenre, true)
            matchesQuery && matchesGenre
        }
    }
    val featured = remember(songs) { songs.firstOrNull { it.featured } ?: songs.firstOrNull() }
    val selectedCollections = remember(videoCollections, selectedGenre) {
        if (selectedGenre == "Semua") emptyList() else videoCollections.filter { it.category.equals(selectedGenre, ignoreCase = true) }
    }
    val listState = rememberLazyListState()
    val dragAccum = remember { floatArrayOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LibraryBlack)
            .pointerInput(isRefreshing) {
                detectVerticalDragGestures { _, dragAmount ->
                    val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (!isRefreshing && atTop && dragAmount > 0f) {
                        dragAccum[0] += dragAmount
                        if (dragAccum[0] > 120f) {
                            dragAccum[0] = 0f
                            onRefresh()
                        }
                    } else if (dragAmount < 0f) {
                        dragAccum[0] = 0f
                    }
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { LibraryHero(featured = featured) }
            item { LibrarySearch(query = query, onQueryChange = { query = it }, isRefreshing = isRefreshing, onRefresh = onRefresh) }
            item { LibraryGenres(genres = genres, collectionCategories = videoCollections.map { it.category }, selected = selectedGenre, onSelected = { selectedGenre = it }) }
            items(selectedCollections, key = { it.id }) { collection ->
                PlaylistCollection(title = collection.title, channelName = collection.channelName, onOpen = { onOpenPlaylist(collection) })
            }
            if (isLoading) item { LibraryMessage("Memuat daftar film...") }
            if (!isLoading && songs.isEmpty()) item { LibraryMessage(errorMessage ?: "Library masih kosong.") }
            if (!isLoading && songs.isNotEmpty() && filteredSongs.isEmpty()) item { LibraryMessage("Tidak ada film yang cocok dengan pencarian ini.") }
            if (filteredSongs.isNotEmpty()) item { TrailerRail(films = filteredSongs, onPlayTrailer = onPlayTrailer) }
        }
    }
}

@Composable
private fun LibraryHero(featured: Song?) {
    Box(modifier = Modifier.fillMaxWidth().height(305.dp).background(Color.Black)) {
        featured?.let { SongCover(song = it, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(LibraryBlack, LibraryBlack.copy(alpha = 0.76f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, LibraryBlack.copy(alpha = 0.95f)))) )
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.logo_parfi_asli), "PARFI Jawa Timur", Modifier.size(76.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Cinema", color = LibraryGold, fontSize = 27.sp, fontWeight = FontWeight.Light)
                Text("PARFI Jatim", color = LibraryPaper, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("FILM  •  SENI  •  KITA", color = LibraryMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Search, "Cari", tint = LibraryPaper, modifier = Modifier.size(28.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifikasi", tint = LibraryPaper, modifier = Modifier.size(28.dp)) }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 28.dp, vertical = 26.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Film", color = LibraryPaper, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(12.dp))
                Text("Library", color = LibraryGold, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Temukan, tonton, dan dukung karya\nsineas Jawa Timur.", color = LibraryPaper.copy(alpha = 0.82f), fontSize = 17.sp, lineHeight = 23.sp)
        }
    }
}

@Composable
private fun LibrarySearch(query: String, onQueryChange: (String) -> Unit, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Cari film, kreator, atau genre...", color = LibraryMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = LibraryGold) },
            shape = RoundedCornerShape(50),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LibraryGold,
                unfocusedBorderColor = LibraryPaper.copy(alpha = 0.23f),
                focusedContainerColor = LibraryPill,
                unfocusedContainerColor = LibraryPill,
                focusedTextColor = LibraryPaper,
                unfocusedTextColor = LibraryPaper
            )
        )
        Surface(modifier = Modifier.size(58.dp).clickable { onRefresh() }, color = LibraryPill, shape = RoundedCornerShape(50)) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (isRefreshing) "..." else "↻", color = LibraryGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibraryGenres(genres: List<String>, collectionCategories: List<String>, selected: String, onSelected: (String) -> Unit) {
    val categories = remember(genres, collectionCategories) {
        (genres + collectionCategories).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.let { listOf("Semua") + it.filterNot { item -> item.equals("Semua", ignoreCase = true) } }
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categories, key = { it }) { genre ->
            val active = selected == genre
            Surface(modifier = Modifier.clickable { onSelected(genre) }, color = if (active) LibraryGold else LibraryPill, shape = RoundedCornerShape(50)) {
                Text(genre, Modifier.padding(horizontal = 20.dp, vertical = 13.dp), color = if (active) Color.Black else LibraryPaper, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlaylistCollection(title: String, channelName: String, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = LibraryPill,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LibraryGold.copy(alpha = 0.46f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(color = LibraryGold, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(54.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(31.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LibraryPaper, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text(channelName, color = LibraryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text("Buka semua video dalam playlist ini", color = LibraryMuted, fontSize = 13.sp)
            }
            Text("›", color = LibraryGold, fontSize = 34.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun TrailerRail(films: List<Song>, onPlayTrailer: (Song) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Trailer",
            color = LibraryPaper,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(films, key = { it.id }) { song ->
                AdaptiveTrailerCard(song = song, onPlayTrailer = onPlayTrailer)
            }
        }
    }
}

@Composable
private fun AdaptiveTrailerCard(song: Song, onPlayTrailer: (Song) -> Unit) {
    val context = LocalContext.current
    var imageRatio by remember(song.id) { mutableFloatStateOf(2f / 3f) }
    val imageUrl = song.coverUrl.toLibraryPosterUrl()

    Box(
        modifier = Modifier
            .width(172.dp)
            .aspectRatio(imageRatio.coerceIn(0.52f, 1.78f))
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LibraryPaper.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
            .clickable { onPlayTrailer(song) }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
            contentDescription = "Putar trailer ${song.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.splash),
            error = painterResource(R.drawable.splash),
            onSuccess = { state ->
                val drawable = state.result.drawable
                if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                    imageRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
                }
            }
        )
    }
}

private fun String.toLibraryPosterUrl(): String {
    val rawUrl = trim()
    if (rawUrl.isBlank()) return ""
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return rawUrl
    val host = uri.host.orEmpty().lowercase()
    if (!host.contains("drive.google.com") && !host.contains("docs.google.com")) return rawUrl
    val fileId = uri.getQueryParameter("id") ?: uri.pathSegments.let { segments ->
        val index = segments.indexOf("d")
        if (index >= 0 && index + 1 < segments.size) segments[index + 1] else null
    }
    return fileId?.takeIf { it.isNotBlank() }?.let { "https://drive.google.com/thumbnail?id=$it&sz=w1200" } ?: rawUrl
}

@Composable
private fun LibraryMessage(message: String) {
    Text(message, color = LibraryMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp))
}
