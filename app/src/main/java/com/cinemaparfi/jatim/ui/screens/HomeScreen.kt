package com.cinemaparfi.jatim.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinemaparfi.jatim.R
import com.cinemaparfi.jatim.data.model.Song
import com.cinemaparfi.jatim.ui.components.SongCover

private val CinemaBlack = Color(0xFF030607)
private val CinemaGold = Color(0xFFE7B946)
private val CinemaPaper = Color(0xFFF5F2EB)
private val CinemaMuted = Color(0xFFB5B6B8)
private val CinemaPill = Color(0xFF161B1F)

@Composable
fun HomeScreen(
    songs: List<Song>,
    artists: List<String>,
    errorMessage: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val dragAccum = remember { floatArrayOf(0f) }
    val featured = remember(songs) { songs.firstOrNull { it.featured } ?: songs.firstOrNull() }
    val recommendations = remember(songs, featured) { songs.filter { it.id != featured?.id }.ifEmpty { songs }.take(8) }
    val categories = remember(songs) {
        listOf("Semua") + songs.map { it.category.ifBlank { it.genre } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
    }
    var selectedCategory by remember(songs) { mutableStateOf("Semua") }
    val filteredFilms = remember(songs, selectedCategory) {
        if (selectedCategory == "Semua") songs else songs.filter {
            it.category.equals(selectedCategory, ignoreCase = true) || it.genre.equals(selectedCategory, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { CinemaHeader() }
            item { CinemaHero(featured = featured, onPlay = { featured?.let(onSongClick) }) }
            item { CategoryRail(categories = categories, selected = selectedCategory, onSelected = { selectedCategory = it }) }
            featured?.let { item { FeaturedFilmCard(song = it, onClick = { onSongClick(it) }) } }
            if (isLoading) item { HomeMessage("Memuat katalog film...") }
            if (!isLoading && songs.isEmpty()) item { HomeMessage(errorMessage ?: "Katalog film belum tersedia.") }
            if (recommendations.isNotEmpty()) item { RecommendationRail(films = recommendations, onSongClick = onSongClick) }
            if (filteredFilms.isNotEmpty()) item { CuratedCategories(films = filteredFilms, onSongClick = onSongClick) }
        }
    }
}

@Composable
private fun CinemaHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) { Icon(Icons.Default.Menu, "Menu", tint = CinemaPaper, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.width(12.dp))
        Image(painterResource(R.drawable.logo_parfi_asli), "PARFI Jawa Timur", Modifier.size(56.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cinema", color = CinemaGold, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text("PARFI Jatim", color = CinemaPaper, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("FILM  •  SENI  •  KITA", color = CinemaMuted, fontSize = 9.sp, letterSpacing = 1.4.sp)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Search, "Cari", tint = CinemaPaper, modifier = Modifier.size(27.dp)) }
        IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifikasi", tint = CinemaPaper, modifier = Modifier.size(27.dp)) }
    }
}

@Composable
private fun CinemaHero(featured: Song?, onPlay: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(342.dp).background(Color.Black)) {
        featured?.let { SongCover(song = it, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(CinemaBlack, CinemaBlack.copy(alpha = 0.82f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, CinemaBlack.copy(alpha = 0.92f)))) )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ceritakan", color = CinemaPaper, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp)
            Text("Duniamu", color = CinemaGold, fontSize = 47.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 45.sp)
            Text("Film adalah jembatan antara kita\ndan masa depan.", color = CinemaPaper, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 5.dp))
            Button(
                onClick = onPlay,
                enabled = featured != null,
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Tonton Sekarang", fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 31.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) { index ->
                Box(Modifier.size(if (index == 0) 18.dp else 9.dp, 8.dp).clip(RoundedCornerShape(8.dp)).background(if (index == 0) CinemaGold else CinemaMuted.copy(alpha = 0.65f)))
            }
        }
    }
}

@Composable
private fun CategoryRail(categories: List<String>, selected: String, onSelected: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categories, key = { it }) { category ->
            val active = category == selected
            Surface(modifier = Modifier.clickable { onSelected(category) }, color = if (active) CinemaGold else CinemaPill, shape = RoundedCornerShape(50)) {
                Text(category, Modifier.padding(horizontal = 18.dp, vertical = 13.dp), color = if (active) Color.Black else CinemaPaper, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FeaturedFilmCard(song: Song, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CinemaSectionTitle("Film Unggulan")
        Box(
            modifier = Modifier.fillMaxWidth().height(252.dp).clip(RoundedCornerShape(16.dp))
                .border(1.dp, CinemaPaper.copy(alpha = 0.36f), RoundedCornerShape(16.dp)).clickable(onClick = onClick)
        ) {
            SongCover(song = song, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(CinemaBlack.copy(alpha = 0.94f), CinemaBlack.copy(alpha = 0.48f), Color.Transparent))))
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(24.dp).fillMaxWidth(0.72f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(song.category.ifBlank { "FILM PILIHAN" }.uppercase(), color = CinemaGold, fontSize = 12.sp, letterSpacing = 1.1.sp)
                Text(song.title, color = CinemaGold, fontSize = 34.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 37.sp)
                Text(song.description.ifBlank { "Karya pilihan Cinema PARFI Jawa Timur." }, color = CinemaPaper.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black), shape = RoundedCornerShape(50)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("Tonton", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RecommendationRail(films: List<Song>, onSongClick: (Song) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        CinemaSectionTitle("Rekomendasi untuk Kamu", Modifier.padding(horizontal = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(films, key = { it.id }) { song ->
                Column(modifier = Modifier.width(151.dp).clickable { onSongClick(song) }) {
                    SongCover(song = song, modifier = Modifier.fillMaxWidth().height(210.dp))
                    Text(song.title, color = CinemaPaper, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(listOf(song.category.ifBlank { song.genre }, song.releaseYear).filter { it.isNotBlank() }.joinToString("  •  "), color = CinemaMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun CuratedCategories(films: List<Song>, onSongClick: (Song) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        CinemaSectionTitle("Kategori Pilihan", Modifier.padding(horizontal = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(films.take(5), key = { "category_${it.id}" }) { song ->
                Box(
                    modifier = Modifier.width(210.dp).height(116.dp).clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CinemaPaper.copy(alpha = 0.32f), RoundedCornerShape(12.dp)).clickable { onSongClick(song) }
                ) {
                    SongCover(song = song, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, CinemaBlack.copy(alpha = 0.94f)))))
                    Row(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(song.category.ifBlank { song.genre }.ifBlank { "Film PARFI Jatim" }, color = CinemaPaper, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, "Buka", tint = CinemaGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CinemaSectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = CinemaPaper, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(34.dp).height(3.dp).background(CinemaGold, RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun HomeMessage(message: String) {
    Text(text = message, color = CinemaMuted, modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodyMedium)
}
