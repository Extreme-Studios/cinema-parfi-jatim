package com.cinemaparfi.jatim.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cinemaparfi.jatim.data.model.Song
import com.cinemaparfi.jatim.ui.components.SongCover

@Composable
fun MovieDetailScreen(
    movie: Song,
    modifier: Modifier = Modifier,
    onWatchTrailer: (String) -> Unit,
    onWatchMovie: (String) -> Unit,
    onEditFilm: ((Song) -> Unit)? = null,
    onDeleteFilm: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SongCover(
                song = movie,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Kreator: ${movie.director.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                Text("Rilis: ${movie.releaseYear.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                Text("Kategori: ${movie.category.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Synopsis: ${movie.description.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (movie.trailerUrl.isNotBlank()) {
            item {
                Button(
                    onClick = { onWatchTrailer(movie.trailerUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Watch Trailer")
                }
            }
        }
        if (movie.movieUrl.isNotBlank() || movie.audioUrl.isNotBlank()) {
            item {
                val fullMovieUrl = movie.movieUrl.ifBlank { movie.audioUrl }
                Button(
                    onClick = { onWatchMovie(fullMovieUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Play")
                }
            }
        }
        if (onEditFilm != null || onDeleteFilm != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (onEditFilm != null) {
                        IconActionButton(
                            text = "Edit",
                            icon = Icons.Filled.Edit,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditFilm(movie) }
                        )
                    }
                    if (onDeleteFilm != null) {
                        IconActionButton(
                            text = "Hapus",
                            icon = Icons.Filled.Delete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = { showDeleteConfirm = true }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && onDeleteFilm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Film?") },
            text = { Text("Tindakan ini akan menghapus film dari daftar. Lanjutkan?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteFilm()
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun IconActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors
    ) {
        Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}
