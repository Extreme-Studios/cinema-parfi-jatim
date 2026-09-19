package com.cinemaparfi.jatim.data.repository

import com.cinemaparfi.jatim.data.model.Song

class MovieRepository(
    private val api: ApiSongRepository = ApiSongRepository()
) {
    suspend fun fetchMovies(): Result<List<Song>> = api.fetchSongs()
}

