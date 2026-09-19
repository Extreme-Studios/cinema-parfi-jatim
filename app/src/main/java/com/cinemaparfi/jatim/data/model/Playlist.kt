package com.cinemaparfi.jatim.data.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String>
)
