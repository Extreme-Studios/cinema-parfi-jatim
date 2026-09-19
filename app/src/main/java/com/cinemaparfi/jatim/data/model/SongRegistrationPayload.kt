package com.cinemaparfi.jatim.data.model

data class SongRegistrationPayload(
    val songTitle: String,
    val release: String = "",
    val category: String = "",
    val synopsis: String = "",
    val movieLink: String,
    val trailerLink: String = "",
    val posterLink: String = "",
    val copyrightAgreement: Boolean,
    val coverImageBase64: String? = null,
    val coverImageFileName: String? = null,
    val coverImageMimeType: String? = null,
    val trailerFileBase64: String? = null,
    val trailerFileName: String? = null,
    val trailerFileMimeType: String? = null
)
