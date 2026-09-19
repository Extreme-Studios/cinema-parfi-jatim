package com.cinemaparfi.jatim.data.repository

import android.util.Log
import com.cinemaparfi.jatim.data.model.Song
import com.cinemaparfi.jatim.data.model.VideoCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ApiSongRepository {
    private var cachedSongs: List<Song> = emptyList()
    private var lastFetchAtMs: Long = 0L
    private var cachedLikeUserId: String? = null

    suspend fun fetchSongs(likeUserId: String? = null): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val normalizedLikeUserId = likeUserId?.trim().orEmpty().ifBlank { null }
            if (cachedSongs.isNotEmpty() && cachedLikeUserId == normalizedLikeUserId && now - lastFetchAtMs < CACHE_TTL_MS) {
                Log.d(TAG, "Using in-memory cache. count=${cachedSongs.size}")
                return@runCatching cachedSongs
            }
            var activeCount = 0
            var appsScriptFailure: Throwable? = null
            val songs = try {
                val requestUrl = buildString {
                append(SONGS_ENDPOINT)
                append("?type=get_films&ts=${System.currentTimeMillis()}")
                if (!normalizedLikeUserId.isNullOrBlank()) {
                    append("&user_id=${java.net.URLEncoder.encode(normalizedLikeUserId, "UTF-8")}")
                }
            }
            Log.d("HubAuth", "url=" + requestUrl)
            Log.d("API_DEBUG", "URL=" + requestUrl)
            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = FILMS_FETCH_TIMEOUT_MS
                readTimeout = FILMS_FETCH_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code saat mengambil data film.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            Log.d("API_DEBUG", "RAW_RESPONSE=" + body)
            val root = JSONObject(body)
            if (!root.optBoolean("success", false)) throw IllegalStateException("Response API tidak sukses.")
            val songsArray = root.optJSONArray("films")
                ?: root.optJSONArray("movies")
                ?: root.optJSONArray("songs")
                ?: root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("films")
                ?: root.optJSONObject("data")?.optJSONArray("songs")
                ?: throw IllegalStateException("Field films tidak ditemukan.")

            Log.d(TAG, "Raw films from API: ${songsArray.length()}")
            buildList {
                for (i in 0 until songsArray.length()) {
                    val item = songsArray.optJSONObject(i) ?: continue
                    val title = readString(item, "title", "film_title", "movie_title", "judul_film", "song_title", "judul_lagu")
                    val artist = readString(item, "kreator", "creator", "director", "sutradara", "artist", "cast_name", "band_name", "artist_name", "bandName", "submitterName")
                        .ifBlank { "Cinema PARFI Jatim" }
                    val status = readString(item, "status").ifBlank { "approved" }
                    val isActive = status.equals("approved", ignoreCase = true)
                    if (!isActive) continue
                    activeCount++

                    val rawId = readString(item, "id", "film_id", "movie_id", "song_id")
                    val createdAt = readString(item, "created_at", "uploaded_at", "uploadedAt", "updated_at")
                    val coverUrl = readString(item, "cover_url", "poster_url", "coverUrl", "cover")
                    val movieUrl = readString(item, "movie_url", "video_url", "videoUrl", "media_url", "audio_url", "audioUrl", "audio")
                    val trailerUrl = readString(item, "trailer_url", "trailerUrl")
                    val albumName = readString(item, "collection_name", "album_name", "albumName")
                    val trackNumber = readString(item, "episode_number", "track_number", "trackNumber").toIntOrNull()
                    val resolvedId = rawId.ifBlank {
                        listOf(title, artist, createdAt, i.toString()).joinToString("#")
                    }
                    Log.d(TAG, "Film from API title=$title poster_url=$coverUrl movie_url=$movieUrl category=${readString(item, "category")}")

                    add(
                        Song(
                            id = resolvedId,
                            title = title,
                            artist = artist,
                            genre = readString(item, "genre"),
                            category = readString(item, "category"),
                            duration = readString(item, "duration"),
                            director = readString(item, "kreator", "creator", "director", "sutradara", "cast_name", "artist"),
                            description = readString(item, "synopsis", "description", "desc", "notes"),
                            coverUrl = coverUrl,
                            trailerUrl = trailerUrl,
                            movieUrl = movieUrl,
                            audioUrl = movieUrl,
                            uploadedAt = createdAt,
                            status = status,
                            featured = readString(item, "featured").equals("yes", ignoreCase = true),
                            views = readString(item, "views").toIntOrNull() ?: 0,
                            likes = readString(item, "likes").toIntOrNull() ?: 0,
                            alreadyLiked = item.optBoolean("already_liked", false),
                            releaseYear = readString(item, "release_year", "year", "releaseYear", "duration", "Rilis", "rilis"),
                            albumName = albumName,
                            trackNumber = trackNumber
                        )
                    )
                }
            }.filter { it.title.isNotBlank() }
            } catch (error: Throwable) {
                appsScriptFailure = error
                Log.w(TAG, "Apps Script film feed unavailable; using website trailer feed.", error)
                emptyList()
            }
            val websiteTrailers = fetchWebsiteTrailers()
            if (songs.isEmpty() && websiteTrailers.isEmpty()) {
                throw IllegalStateException(
                    "Data film tidak dapat dimuat dari server.",
                    appsScriptFailure
                )
            }
            val mergedSongs = (websiteTrailers + songs)
                .distinctBy { youtubeIdFromUrl(it.trailerUrl.ifBlank { it.movieUrl }).ifBlank { it.id } }
                .sortedWith(
                    compareByDescending<Song> { parseCreatedAtMillis(it.uploadedAt) }
                        .thenBy { it.title.lowercase(Locale.US) }
                )

            Log.d(TAG, "Active films after status filter: $activeCount")
            Log.d(TAG, "Website trailers: ${websiteTrailers.size}; merged films: ${mergedSongs.size}")
            cachedSongs = mergedSongs
            lastFetchAtMs = now
            cachedLikeUserId = normalizedLikeUserId
            mergedSongs
        }
    }

    fun getGenres(songs: List<Song>): List<String> = listOf("Semua") + songs.map { it.genre }.filter { it.isNotBlank() }.distinct().sorted()
    fun getArtists(songs: List<Song>): List<String> = songs.map { it.artist }.filter { it.isNotBlank() }.distinct().sorted()

    fun getFallbackSongs(): List<Song> = emptyList()

    suspend fun fetchVideoCollections(): Result<List<VideoCollection>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$SONGS_ENDPOINT?type=video_collections&ts=${System.currentTimeMillis()}"
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = VIDEO_COLLECTION_TIMEOUT_MS
                readTimeout = VIDEO_COLLECTION_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode} saat memuat koleksi video.")
                val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                if (!root.optBoolean("success", false)) throw IllegalStateException(root.optString("message", "Koleksi video gagal dimuat."))
                val items = root.optJSONArray("collections") ?: return@runCatching emptyList()
                buildList {
                    for (index in 0 until items.length()) {
                        val item = items.optJSONObject(index) ?: continue
                        val playlistUrl = readString(item, "playlist_url", "playlistUrl", "url")
                        if (playlistUrl.isBlank()) continue
                        add(
                            VideoCollection(
                                id = readString(item, "id").ifBlank { "collection_$index" },
                                title = readString(item, "title", "judul").ifBlank { "Koleksi Video" },
                                category = readString(item, "category", "kategori").ifBlank { "Lainnya" },
                                channelName = readString(item, "channel_name", "channelName", "channel").ifBlank { "YouTube" },
                                playlistUrl = playlistUrl
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun fetchWebsiteTrailers(): List<Song> {
        WEBSITE_FILM_ENDPOINTS.forEach { endpoint ->
            val result = runCatching {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = WEBSITE_TRAILER_TIMEOUT_MS
                    readTimeout = WEBSITE_TRAILER_TIMEOUT_MS
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (connection.responseCode !in 200..299) return@runCatching emptyList()
                    val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val items = root.optJSONArray("items") ?: return@runCatching emptyList()
                    buildList {
                        for (index in 0 until items.length()) {
                            val item = items.optJSONObject(index) ?: continue
                            if (!readString(item, "status").equals("PUBLISH", ignoreCase = true)) continue
                            val videoUrl = readString(item, "video_url", "source_url")
                            if (youtubeIdFromUrl(videoUrl).isBlank()) continue
                            val title = readString(item, "judul", "title").ifBlank { "Trailer PARFI Jawa Timur" }
                            add(
                                Song(
                                    id = "web_${readString(item, "id", "slug").ifBlank { title }.hashCode()}",
                                    title = title,
                                    artist = readString(item, "kreator", "creator").ifBlank { "PARFI Jawa Timur" },
                                    genre = "Trailer Resmi",
                                    category = readString(item, "kategori", "category").ifBlank { "Trailer Resmi" },
                                    description = readString(item, "sinopsis", "ringkasan", "summary", "description"),
                                    coverUrl = readString(item, "gambar_url", "poster_url", "cover_url"),
                                    trailerUrl = videoUrl,
                                    movieUrl = videoUrl,
                                    audioUrl = videoUrl,
                                    uploadedAt = readString(item, "tanggal", "published_at", "created_at"),
                                    status = "approved",
                                    featured = true,
                                    releaseYear = readString(item, "tahun", "year")
                                )
                            )
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse {
                Log.w(TAG, "Website trailer feed unavailable: $endpoint", it)
                emptyList()
            }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    suspend fun updateFilmLike(filmId: String, username: String, password: String): Result<FilmLikeResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (filmId.trim().isBlank()) throw IllegalStateException("film_id kosong.")
            if (username.trim().isBlank() || password.trim().isBlank()) {
                throw IllegalStateException("Login diperlukan untuk Like.")
            }
            val url = SONGS_ENDPOINT
            Log.d("API_DEBUG", "URL=" + url)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "film_like")
                put("film_id", filmId)
                put("username", username)
                put("password", password)
            }.toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            Log.d("API_DEBUG", "RAW_RESPONSE=" + responseBody)
            val responseJson = JSONObject(responseBody)
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Gagal update like." })
            }
            cachedSongs = emptyList()
            lastFetchAtMs = 0L
            FilmLikeResult(
                totalLikes = responseJson.optInt("total_likes", responseJson.optInt("likes", 0)),
                alreadyLiked = responseJson.optBoolean("already_liked", true)
            )
        }
    }

    suspend fun updateFilmView(filmId: String?, movieUrl: String?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val url = SONGS_ENDPOINT
            Log.d("API_DEBUG", "URL=" + url)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "film_view")
                if (!filmId.isNullOrBlank()) put("film_id", filmId)
                if (!movieUrl.isNullOrBlank()) put("movie_url", movieUrl)
            }.toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            Log.d("API_DEBUG", "RAW_RESPONSE=" + responseBody)
            val responseJson = JSONObject(responseBody)
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Gagal update view." })
            }
            responseJson.optInt("views", 0)
        }
    }

    companion object {
        private const val TAG = "ApiSongRepository"
        private const val CACHE_TTL_MS = 60_000L
        private const val NETWORK_TIMEOUT_MS = 30_000
        // Apps Script can stall while its spreadsheet is busy. Website trailers are the fast fallback.
        private const val FILMS_FETCH_TIMEOUT_MS = 4_000
        private const val WEBSITE_TRAILER_TIMEOUT_MS = 4_000
        private const val VIDEO_COLLECTION_TIMEOUT_MS = 10_000
        private const val SONGS_ENDPOINT = "https://script.google.com/macros/s/AKfycbxQogA-cfhzFLv93JWblbuDH-b1preG1wRiRP4ubUjdHO3aarN3_9NZWtfFxLBJtAQ_Rw/exec"
        private val WEBSITE_FILM_ENDPOINTS = listOf(
            "https://parfijatim.com/api/cms?action=public&type=film",
            "https://www.parfijatim.com/api/cms?action=public&type=film"
        )
    }
}

data class FilmLikeResult(
    val totalLikes: Int,
    val alreadyLiked: Boolean
)

private fun readString(item: JSONObject, vararg keys: String): String {
    keys.forEach { key ->
        if (item.has(key)) {
            val value = item.opt(key)
            if (value != null && value != JSONObject.NULL) {
                val text = value.toString().trim()
                if (text.isNotBlank()) return text
            }
        }
    }
    return ""
}

private fun parseCreatedAtMillis(value: String): Long {
    if (value.isBlank()) return Long.MIN_VALUE
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)?.time
        }.getOrNull()
    } ?: Long.MIN_VALUE
}

private fun youtubeIdFromUrl(value: String): String {
    val url = value.trim()
    if (url.isBlank()) return ""
    val match = Regex("(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:watch\\?v=|embed/|shorts/|live/))([A-Za-z0-9_-]{6,})")
        .find(url)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    return match.substringBefore('?').substringBefore('&')
}
