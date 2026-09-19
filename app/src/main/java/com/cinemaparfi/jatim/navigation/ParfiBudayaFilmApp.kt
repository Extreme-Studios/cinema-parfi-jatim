package com.cinemaparfi.jatim.navigation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cinemaparfi.jatim.data.local.PortalSessionStore
import com.cinemaparfi.jatim.data.model.PortalPost
import com.cinemaparfi.jatim.data.model.Song
import com.cinemaparfi.jatim.data.model.VideoCollection
import com.cinemaparfi.jatim.data.repository.ApiSongRepository
import com.cinemaparfi.jatim.data.repository.PortalRepository
import com.cinemaparfi.jatim.ui.components.TopBannerAd
import com.cinemaparfi.jatim.ui.screens.AboutScreen
import com.cinemaparfi.jatim.ui.screens.AlbumDetailScreen
import com.cinemaparfi.jatim.ui.screens.ArtistDetailScreen
import com.cinemaparfi.jatim.ui.screens.HomeScreen
import com.cinemaparfi.jatim.ui.screens.LibraryScreen
import com.cinemaparfi.jatim.ui.screens.MovieDetailScreen
import com.cinemaparfi.jatim.ui.screens.PortalKitaScreen
import com.cinemaparfi.jatim.ui.screens.PortalPostDetailScreen
import com.cinemaparfi.jatim.ui.screens.PortalPosterSubmissionScreen
import com.cinemaparfi.jatim.ui.screens.VideoPlayerScreen
import com.cinemaparfi.jatim.ui.screens.YouTubePlaylistScreen
import kotlinx.coroutines.launch

sealed class AppScreen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : AppScreen("home", "Home", Icons.Default.Home)
    data object Library : AppScreen("library", "Library", Icons.AutoMirrored.Filled.List)
    data object PortalKita : AppScreen("portal_kita", "Poster Board", Icons.Default.AddCircle)
    data object About : AppScreen("about", "About", Icons.Default.Info)
}

@Composable
fun CinemaParfiJatimApp(repository: ApiSongRepository = ApiSongRepository()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val portalRepository = remember { PortalRepository() }
    val portalSessionStore = remember { PortalSessionStore(context) }
    val portalSession by portalSessionStore.sessionFlow.collectAsState(initial = null)

    val tabs = listOf(
        AppScreen.Home,
        AppScreen.Library,
        AppScreen.PortalKita,
        AppScreen.About
    )

    var currentTab by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val tabHistory = remember { mutableStateListOf<AppScreen>().apply { add(AppScreen.Home) } }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedMovie by remember { mutableStateOf<Song?>(null) }
    var activeVideoSong by rememberSaveable { mutableStateOf<Song?>(null) }
    var activeIsTrailer by rememberSaveable { mutableStateOf(false) }
    var activeVideoCollection by remember { mutableStateOf<VideoCollection?>(null) }
    var movieToEdit by remember { mutableStateOf<Song?>(null) }
    var showEditMovieDialog by remember { mutableStateOf(false) }
    var editMovieTitle by remember { mutableStateOf("") }
    var editMovieRelease by remember { mutableStateOf("") }
    var editMovieCategory by remember { mutableStateOf("") }
    var editMovieCreator by remember { mutableStateOf("") }
    var editMovieSynopsis by remember { mutableStateOf("") }
    var editMovieTrailerUrl by remember { mutableStateOf("") }
    var editMovieVideoUrl by remember { mutableStateOf("") }
    var editMoviePosterUrl by remember { mutableStateOf("") }
    var selectedPortalPost by remember { mutableStateOf<PortalPost?>(null) }
    var showPortalSubmission by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var videoCollections by remember { mutableStateOf<List<VideoCollection>>(emptyList()) }
    var isLoadingSongs by remember { mutableStateOf(true) }
    var isRefreshingSongs by remember { mutableStateOf(false) }
    var songsError by remember { mutableStateOf<String?>(null) }
    val genres = remember(songs) { repository.getGenres(songs) }
    val artists = remember(songs) { repository.getArtists(songs) }
    var portalPosts by remember { mutableStateOf<List<PortalPost>>(emptyList()) }
    var isLoadingPortal by remember { mutableStateOf(true) }
    var portalError by remember { mutableStateOf<String?>(null) }
    val portalCategories = remember { portalRepository.getCategories() }

    fun refreshSongs(showBlocking: Boolean) {
        scope.launch {
            if (showBlocking) isLoadingSongs = true else isRefreshingSongs = true
            val activeLikeUserId = portalSession?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) }?.userId
            repository.fetchSongs(activeLikeUserId)
                .onSuccess { apiSongs ->
                    songs = apiSongs
                    songsError = null
                    Log.d("CinemaParfiJatimApp", "API success. films count=${apiSongs.size}")
                }
                .onFailure { err ->
                    songsError = err.message ?: "Gagal mengambil data film dari server."
                    Log.d("CinemaParfiJatimApp", "API failed. Keeping current in-memory list. count=${songs.size}")
                }
            repository.fetchVideoCollections()
                .onSuccess { collections -> videoCollections = collections }
                .onFailure { error -> Log.w("CinemaParfiJatimApp", "Koleksi video gagal dimuat.", error) }
            isLoadingSongs = false
            isRefreshingSongs = false
        }
    }

    fun refreshPortalPosts(showBlocking: Boolean = true) {
        scope.launch {
            if (showBlocking) isLoadingPortal = true
            portalRepository.fetchActivePosts()
                .onSuccess { posts ->
                    portalPosts = posts
                    portalError = null
                }
                .onFailure { error ->
                    portalError = error.message ?: "Gagal memuat Poster Board."
                    if (showBlocking && portalPosts.isEmpty()) portalPosts = emptyList()
                }
            isLoadingPortal = false
        }
    }

    fun syncHubAccessState() {
        val currentAdmin = portalSession ?: return
        scope.launch {
            portalRepository.checkHubStatus(currentAdmin.username, currentAdmin.password)
                .onSuccess { account ->
                    if (account.status.equals("ACTIVE", ignoreCase = true)) {
                        val merged = if (account.userId.isBlank() && portalSession?.userId?.isNotBlank() == true) {
                            account.copy(userId = portalSession?.userId.orEmpty())
                        } else account
                        portalSessionStore.saveSession(merged)
                        Log.d("HubAuth", "saved session userId=${merged.userId}, status=ACTIVE")
                    } else {
                        Log.d("HubAuth", "status=${account.status}; clearing admin session")
                        portalSessionStore.clearSession()
                    }
                }
                .onFailure { error ->
                    val message = error.message.orEmpty()
                    if (message.contains("admin", ignoreCase = true) ||
                        message.contains("tidak ditemukan", ignoreCase = true) ||
                        message.contains("tidak aktif", ignoreCase = true)
                    ) {
                        Log.d("HubAuth", "status check failed; clearing admin session: $message")
                        portalSessionStore.clearSession()
                    }
                }
        }
    }

    fun navigateTo(screen: AppScreen) {
        selectedArtist = null
        selectedAlbum = null
        selectedMovie = null
        activeVideoCollection = null
        selectedPortalPost = null
        showPortalSubmission = false
        if (currentTab != screen) {
            currentTab = screen
            tabHistory.add(screen)
        }
    }

    fun beginEditMovie(movie: Song) {
        movieToEdit = movie
        editMovieTitle = movie.title
        editMovieRelease = movie.releaseYear
        editMovieCategory = movie.category
        editMovieCreator = movie.director
        editMovieSynopsis = movie.description
        editMovieTrailerUrl = movie.trailerUrl
        editMovieVideoUrl = movie.movieUrl.ifBlank { movie.audioUrl }
        editMoviePosterUrl = movie.coverUrl
        showEditMovieDialog = true
    }

    fun deleteMovie(movie: Song) {
        val admin = portalSession ?: return
        scope.launch {
            portalRepository.deleteFilm(admin, movie.id)
                .onSuccess {
                    if (selectedMovie?.id == movie.id) {
                        selectedMovie = null
                    }
                    refreshSongs(showBlocking = false)
                }
                .onFailure { error ->
                    Log.e("CinemaParfiJatimApp", error.message ?: "Gagal menghapus film.")
                }
        }
    }

    fun openVideoInApp(song: Song, isTrailer: Boolean) {
        selectedMovie = null
        activeVideoCollection = null
        activeVideoSong = song
        activeIsTrailer = isTrailer
    }

    fun openPlaylist(collection: VideoCollection) {
        selectedMovie = null
        activeVideoSong = null
        activeVideoCollection = collection
    }

    fun openSong(song: Song) {
        selectedArtist = null
        selectedAlbum = null
        selectedMovie = song
    }

    LaunchedEffect(Unit) {
        refreshSongs(showBlocking = true)
        refreshPortalPosts(showBlocking = true)
        syncHubAccessState()
    }

    LaunchedEffect(currentTab, songs.size) {
        Log.d("CinemaParfiJatimApp", "Selected tab=${currentTab.route}, songsCount=${songs.size}")
    }

    LaunchedEffect(portalSession?.userId, portalSession?.status) {
        refreshSongs(showBlocking = false)
    }

    DisposableEffect(lifecycleOwner, portalSession?.username, portalSession?.password) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncHubAccessState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when {
            activeVideoCollection != null -> activeVideoCollection = null
            activeVideoSong != null -> {
                activeVideoSong = null
                activeIsTrailer = false
            }
            selectedMovie != null -> selectedMovie = null
            showEditMovieDialog -> showEditMovieDialog = false
            showPortalSubmission -> showPortalSubmission = false
            selectedPortalPost != null -> selectedPortalPost = null
            selectedAlbum != null -> selectedAlbum = null
            selectedArtist != null -> selectedArtist = null
            tabHistory.size > 1 -> {
                tabHistory.removeAt(tabHistory.lastIndex)
                currentTab = tabHistory.last()
            }
            else -> activity?.moveTaskToBack(true)
        }
    }

    Scaffold(
        bottomBar = {
            if (activeVideoSong == null && activeVideoCollection == null) {
                Column {
                    TopBannerAd()
                    NavigationBar(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp
                    ) {
                        tabs.forEach { screen ->
                            NavigationBarItem(
                                selected = currentTab == screen,
                                onClick = { navigateTo(screen) },
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = {
                                    Text(
                                        text = screen.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val modifier = if (activeVideoSong != null || activeVideoCollection != null) Modifier else Modifier.padding(innerPadding)

        if (showEditMovieDialog && movieToEdit != null) {
            EditMovieDialog(
                title = editMovieTitle,
                release = editMovieRelease,
                category = editMovieCategory,
                creator = editMovieCreator,
                synopsis = editMovieSynopsis,
                trailerUrl = editMovieTrailerUrl,
                movieUrl = editMovieVideoUrl,
                posterUrl = editMoviePosterUrl,
                onTitleChange = { editMovieTitle = it },
                onReleaseChange = { editMovieRelease = it },
                onCategoryChange = { editMovieCategory = it },
                onCreatorChange = { editMovieCreator = it },
                onSynopsisChange = { editMovieSynopsis = it },
                onTrailerUrlChange = { editMovieTrailerUrl = it },
                onMovieUrlChange = { editMovieVideoUrl = it },
                onPosterUrlChange = { editMoviePosterUrl = it },
                onDismiss = { showEditMovieDialog = false },
                onSave = {
                    val movie = movieToEdit ?: return@EditMovieDialog
                    val admin = portalSession ?: return@EditMovieDialog
                    scope.launch {
                        portalRepository.updateFilm(
                            session = admin,
                            filmId = movie.id,
                            title = editMovieTitle,
                            release = editMovieRelease,
                            category = editMovieCategory,
                            kreator = editMovieCreator,
                            synopsis = editMovieSynopsis,
                            posterUrl = editMoviePosterUrl,
                            trailerUrl = editMovieTrailerUrl,
                            movieUrl = editMovieVideoUrl
                        ).onSuccess {
                            showEditMovieDialog = false
                            movieToEdit = null
                            refreshSongs(showBlocking = false)
                        }.onFailure { error ->
                            Log.e("CinemaParfiJatimApp", error.message ?: "Gagal mengupdate film.")
                        }
                    }
                }
            )
        }

        activeVideoCollection?.let { collection ->
            YouTubePlaylistScreen(
                playlistId = collection.playlistUrl.youtubePlaylistId(),
                title = collection.title,
                channelName = collection.channelName,
                modifier = modifier,
                onBack = { activeVideoCollection = null }
            )
        } ?: activeVideoSong?.let { video ->
            VideoPlayerScreen(
                video = video,
                isTrailer = activeIsTrailer,
                modifier = modifier,
                onBack = {
                    activeVideoSong = null
                    activeIsTrailer = false
                }
            )
        } ?: if (selectedMovie != null) {
            MovieDetailScreen(
                movie = selectedMovie ?: return@Scaffold,
                modifier = modifier,
                onWatchTrailer = { _ ->
                    selectedMovie?.let { openVideoInApp(it, true) }
                },
                onWatchMovie = { _ ->
                    selectedMovie?.let { openVideoInApp(it, false) }
                },
                onEditFilm = if (portalSession != null) ({ movie -> beginEditMovie(movie) }) else null,
                onDeleteFilm = if (portalSession != null) ({ selectedMovie?.let { deleteMovie(it) } }) else null
            )
        } else if (showPortalSubmission) {
            PortalPosterSubmissionScreen(
                modifier = modifier,
                onBack = { showPortalSubmission = false },
                onSubmitted = {
                    showPortalSubmission = false
                    refreshPortalPosts(showBlocking = true)
                }
            )
        } else selectedPortalPost?.let { post ->
            PortalPostDetailScreen(
                post = post,
                modifier = modifier,
                onBack = { selectedPortalPost = null }
            )
        } ?: selectedAlbum?.let { (artistName, albumName) ->
            AlbumDetailScreen(
                artistName = artistName,
                albumName = albumName,
                songs = songs,
                modifier = modifier,
                onBack = { selectedAlbum = null },
                onSongClick = { song, _ -> openSong(song) }
            )
        } ?: selectedArtist?.let { artistName ->
            ArtistDetailScreen(
                artistName = artistName,
                songs = songs,
                errorMessage = songsError,
                modifier = modifier,
                onBack = { selectedArtist = null },
                onSongClick = { openSong(it) },
                onAlbumClick = { albumName -> selectedAlbum = artistName to albumName }
            )
        } ?: when (currentTab) {
            AppScreen.Home -> HomeScreen(
                songs = songs,
                artists = artists,
                errorMessage = songsError,
                isLoading = isLoadingSongs,
                isRefreshing = isRefreshingSongs,
                onRefresh = { refreshSongs(showBlocking = false) },
                modifier = modifier,
                onSongClick = { openSong(it) },
                onArtistClick = { selectedArtist = it }
            )
            AppScreen.Library -> LibraryScreen(
                songs = songs,
                videoCollections = videoCollections,
                genres = genres,
                errorMessage = songsError,
                isLoading = isLoadingSongs,
                isRefreshing = isRefreshingSongs,
                onRefresh = { refreshSongs(showBlocking = false) },
                modifier = modifier,
                onPlayTrailer = { openVideoInApp(it, true) },
                onOpenPlaylist = { openPlaylist(it) }
            )
            AppScreen.PortalKita -> PortalKitaScreen(
                posts = portalPosts,
                categories = portalCategories,
                isLoading = isLoadingPortal,
                errorMessage = portalError,
                modifier = modifier,
                onRefresh = { refreshPortalPosts(showBlocking = true) },
                onPostPreview = { selectedPortalPost = it },
                onOpenPosterSubmission = { showPortalSubmission = true }
            )
            AppScreen.About -> AboutScreen(modifier = modifier)
        }
    }
}

@Composable
private fun EditMovieDialog(
    title: String,
    release: String,
    category: String,
    creator: String,
    synopsis: String,
    trailerUrl: String,
    movieUrl: String,
    posterUrl: String,
    onTitleChange: (String) -> Unit,
    onReleaseChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCreatorChange: (String) -> Unit,
    onSynopsisChange: (String) -> Unit,
    onTrailerUrlChange: (String) -> Unit,
    onMovieUrlChange: (String) -> Unit,
    onPosterUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Film") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Judul") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = release, onValueChange = onReleaseChange, label = { Text("Rilis") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = onCategoryChange, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = creator, onValueChange = onCreatorChange, label = { Text("Kreator") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = synopsis, onValueChange = onSynopsisChange, label = { Text("Synopsis") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = trailerUrl, onValueChange = onTrailerUrlChange, label = { Text("Trailer URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = movieUrl, onValueChange = onMovieUrlChange, label = { Text("Video URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = posterUrl, onValueChange = onPosterUrlChange, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun String.youtubePlaylistId(): String {
    val url = trim()
    if (url.isBlank()) return ""
    return runCatching { android.net.Uri.parse(url).getQueryParameter("list").orEmpty() }
        .getOrDefault("")
}
