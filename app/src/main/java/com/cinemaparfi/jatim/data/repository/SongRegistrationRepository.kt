package com.cinemaparfi.jatim.data.repository
import android.util.Log
import com.cinemaparfi.jatim.data.model.SongRegistrationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class SongRegistrationRepository {
    companion object {
        const val GOOGLE_APPS_SCRIPT_ENDPOINT = "https://script.google.com/macros/s/AKfycbxQogA-cfhzFLv93JWblbuDH-b1preG1wRiRP4ubUjdHO3aarN3_9NZWtfFxLBJtAQ_Rw/exec"
        private const val NETWORK_TIMEOUT_MS = 30_000
    }

    suspend fun submitRegistration(payload: SongRegistrationPayload): Result<Unit> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val url = GOOGLE_APPS_SCRIPT_ENDPOINT
            Log.d("API_DEBUG", "URL=" + url)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put("type", "film_registration")
                put("title", payload.songTitle)
                put("judul", payload.songTitle)
                put("songTitle", payload.songTitle)
                put("release", payload.release)
                put("rilis", payload.release)
                put("category", payload.category)
                put("kategori", payload.category)
                put("synopsis", payload.synopsis)
                put("copyrightAgreement", payload.copyrightAgreement)
                put("movie_url", payload.movieLink)
                put("movieLink", payload.movieLink)
                if (payload.trailerLink.isNotBlank()) put("trailer_url", payload.trailerLink)
                if (payload.posterLink.isNotBlank()) {
                    put("cover_url", payload.posterLink)
                    put("poster_url", payload.posterLink) // keep compatibility with old backend mapping
                }
                if (!payload.coverImageBase64.isNullOrBlank()) {
                    put("coverImageBase64", payload.coverImageBase64)
                    put("coverImageFileName", payload.coverImageFileName.orEmpty())
                    put("coverImageMimeType", payload.coverImageMimeType.orEmpty())
                }
                if (!payload.trailerFileBase64.isNullOrBlank()) {
                    put("trailerFileBase64", payload.trailerFileBase64)
                    put("trailerFileName", payload.trailerFileName.orEmpty())
                    put("trailerFileMimeType", payload.trailerFileMimeType.orEmpty())
                }
            }.toString()

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                throw IllegalStateException("Gagal kirim data (HTTP $responseCode). $errorBody")
            }
            val responseBody = connection.inputStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            connection.disconnect()
            Log.d("API_DEBUG", "RAW_RESPONSE=" + responseBody)
            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            val success = responseJson.optBoolean("success", false)

            if (!success) {
                val message = responseJson.optString("message").ifBlank {
                    "Registrasi film gagal diproses."
                }
                throw IllegalStateException(message)
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat mengirim registrasi.")
        }
    }
}
