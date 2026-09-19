package com.cinemaparfi.jatim.data.repository

import android.util.Log
import com.cinemaparfi.jatim.data.model.PortalPost
import com.cinemaparfi.jatim.data.model.PortalUserSession
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PortalRepository {
    fun getCategories(): List<String> = listOf(
        "All",
        "Event",
        "Promo Film",
        "Announcement",
        "Rilis Baru"
    )

    fun getSubmissionCategories(): List<String> = getCategories().filter { it != "All" }

    fun normalizeWhatsapp(rawInput: String): Result<String> = runCatching {
        val compact = rawInput
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("+", "")
            .trim()

        val normalized = when {
            compact.startsWith("0") -> "62${compact.drop(1)}"
            compact.startsWith("8") -> "62$compact"
            compact.startsWith("62") -> compact
            else -> throw IllegalStateException("Nomor WhatsApp tidak valid. Gunakan nomor Indonesia aktif.")
        }

        if (!normalized.all { it.isDigit() } || normalized.length !in 10..15) {
            throw IllegalStateException("Nomor WhatsApp tidak valid. Gunakan nomor Indonesia aktif.")
        }
        normalized
    }

    suspend fun registerHubBand(
        username: String,
        password: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "hub_update")
                put("username", username)
                put("password", password)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(
                    response.optString("message").ifBlank { "Gagal mendaftarkan kreator." }
                )
            }
            username.ifBlank { "admin" }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal mendaftarkan kreator.")
        }
    }

    suspend fun loginHubBand(
        usernameRaw: String,
        passwordRaw: String,
    ): Result<PortalUserSession> = withContext(Dispatchers.IO) {
        runCatching {
            requestHubAccount(type = "hub_login", usernameRaw = usernameRaw, passwordRaw = passwordRaw)
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal login PARFI.")
        }
    }

    suspend fun checkHubStatus(
        usernameRaw: String,
        passwordRaw: String,
    ): Result<PortalUserSession> = withContext(Dispatchers.IO) {
        runCatching {
            requestHubAccount(type = "hub_check_status", usernameRaw = usernameRaw, passwordRaw = passwordRaw)
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal mengecek status PARFI.")
        }
    }

    suspend fun fetchActivePosts(): Result<List<PortalPost>> = withContext(Dispatchers.IO) {
        runCatching {
            val requestUrl = "$GOOGLE_APPS_SCRIPT_ENDPOINT?type=portal_posts&ts=${System.currentTimeMillis()}"
            Log.d("API_DEBUG", "URL=" + requestUrl)
            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code saat mengambil data portal.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            Log.d("API_DEBUG", "RAW_RESPONSE=" + body)
            val normalizedBody = body.trimStart()
            if (normalizedBody.startsWith("<!DOCTYPE", ignoreCase = true) || normalizedBody.startsWith("<html", ignoreCase = true)) {
                throw IllegalStateException("Endpoint belum mengembalikan JSON. Deploy ulang Apps Script sebagai Web App (Who has access: Anyone).")
            }

            val root = JSONObject(body)
            val candidates = listOfNotNull(
                root.optJSONArray("portal_posts"),
                root.optJSONArray("posts"),
                root.optJSONArray("portalPosts")
            )
            val postsArray = candidates.firstOrNull() ?: JSONArray()
            buildList {
                for (i in 0 until postsArray.length()) {
                    val item = postsArray.optJSONObject(i) ?: continue
                    val status = item.optString("status").ifBlank { "PENDING" }
                    if (!status.equals("ACTIVE", ignoreCase = true) && !status.equals("active", ignoreCase = true)) continue
                    val posterUrl = item.optString("poster_url")
                    if (posterUrl.isBlank()) continue
                    add(
                        PortalPost(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = item.optString("title").ifBlank {
                                item.optString("band_name").ifBlank { item.optString("category") }
                            },
                            submitterName = item.optString("submitterName").ifBlank {
                                item.optString("band_name").ifBlank {
                                    item.optString("submitter_name").ifBlank { "Poster Board" }
                                }
                            },
                            category = item.optString("category"),
                            posterUrl = posterUrl,
                            targetUrl = item.optString("target_url"),
                            shortCaption = item.optString("short_caption").ifBlank { item.optString("caption") },
                            eventDate = item.optString("event_date"),
                            status = status,
                            createdAt = item.optString("created_at")
                        )
                    )
                }
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal memuat Poster Board.")
        }
    }

    suspend fun reportPoster(post: PortalPost, reason: String, reporterWhatsapp: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "portal_report")
                put("postId", post.id)
                put("reason", reason)
                put("reporterWhatsapp", reporterWhatsapp)
                put("reportedLink", post.posterUrl)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Laporan gagal dikirim." })
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Laporan gagal dikirim.")
        }
    }

    suspend fun deletePoster(
        session: PortalUserSession,
        postId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "portal_post_delete")
                put("post_id", postId)
                put("username", session.username)
                put("password", session.password)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Gagal menghapus poster." })
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal menghapus poster.")
        }
    }

    suspend fun deleteFilm(
        session: PortalUserSession,
        filmId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "film_delete")
                put("film_id", filmId)
                put("username", session.username)
                put("password", session.password)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Gagal menghapus film." })
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal menghapus film.")
        }
    }

    suspend fun updateFilm(
        session: PortalUserSession,
        filmId: String,
        title: String,
        release: String,
        category: String,
        kreator: String,
        synopsis: String,
        posterUrl: String,
        trailerUrl: String,
        movieUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "film_update")
                put("film_id", filmId)
                put("title", title)
                put("release", release)
                put("category", category)
                put("kreator", kreator)
                put("synopsis", synopsis)
                put("poster_url", posterUrl)
                put("trailer_url", trailerUrl)
                put("movie_url", movieUrl)
                put("username", session.username)
                put("password", session.password)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Gagal mengupdate film." })
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal mengupdate film.")
        }
    }

    suspend fun submitPoster(
        session: PortalUserSession?,
        category: String,
        posterImageBase64: String,
        posterImageFileName: String,
        posterImageMimeType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            submitPortalMetadata(
                session = session,
                category = category,
                posterImageBase64 = posterImageBase64,
                posterImageFileName = posterImageFileName,
                posterImageMimeType = posterImageMimeType
            )
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal mengirim poster.")
        }
    }

    private fun submitPortalMetadata(
        session: PortalUserSession?,
        category: String,
        posterImageBase64: String,
        posterImageFileName: String,
        posterImageMimeType: String
    ) {
        val submitterId = session?.userId?.ifBlank { null } ?: "guest_${System.currentTimeMillis()}"
        val submitterName = session?.username?.ifBlank { null } ?: session?.bandName?.ifBlank { null } ?: "Guest"
        val payload = JSONObject().apply {
            put("type", "portal_post")
            put("userId", submitterId)
            put("bandName", submitterName)
            if (!session?.username.isNullOrBlank()) put("username", session.username)
            if (!session?.password.isNullOrBlank()) put("password", session.password)
            put("category", category)
            put("posterImageBase64", posterImageBase64)
            put("posterImageFileName", posterImageFileName)
            put("posterImageMimeType", posterImageMimeType)
        }
        Log.d(
            "PortalKirim",
            "payload={type=portal_post,userId=$submitterId,username=${session?.username.orEmpty()},category=$category,fileName=$posterImageFileName,mimeType=$posterImageMimeType,base64Length=${posterImageBase64.length}}"
        )
        val response = postJson(payload)
        Log.d("PortalKirim", "raw response=${response.toString()}")
        if (!response.optBoolean("success", false)) {
            throw IllegalStateException(
                response.optString("message").ifBlank { "Metadata poster gagal disimpan." }
            )
        }
    }

    private fun postJson(payload: JSONObject): JSONObject {
        val payloadType = payload.optString("type")
        if (payloadType == "hub_register" || payloadType == "hub_login" || payloadType == "hub_check_status") {
            Log.d("HubAuth", "payload=${payload.toString()}")
        }
        val url = GOOGLE_APPS_SCRIPT_ENDPOINT
        Log.d("API_DEBUG", "URL=" + url)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }

        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()

        Log.d("API_DEBUG", "RAW_RESPONSE=" + responseBody)

        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP $responseCode saat mengirim poster. $responseBody")
        }
        val normalizedBody = responseBody.trimStart()
        if (normalizedBody.startsWith("<!DOCTYPE", ignoreCase = true) || normalizedBody.startsWith("<html", ignoreCase = true)) {
            throw IllegalStateException("Endpoint belum mengembalikan JSON. Deploy ulang Apps Script sebagai Web App (Who has access: Anyone).")
        }
        return JSONObject(responseBody.ifBlank { "{\"success\":false}" })
    }

    private fun requestHubAccount(type: String, usernameRaw: String, passwordRaw: String): PortalUserSession {
        val username = usernameRaw.trim()
        val password = passwordRaw.trim()
        val payload = JSONObject().apply {
            put("type", type)
            put("username", username)
            put("password", password)
        }
        val response = postJson(payload)
        val success = response.optBoolean("success", false)
        if (!success) {
            throw IllegalStateException(
                response.optString("message").ifBlank {
                    when (type) {
                        "hub_login" -> "Gagal login admin."
                        "hub_check_status" -> "Info akun admin tidak ditemukan."
                        else -> "Gagal mengecek status PARFI."
                    }
                }
            )
        }

        val user = response.optJSONObject("user") ?: response.optJSONObject("member")
        val rawStatus = when {
            response.has("status") -> response.optString("status")
            response.optJSONObject("admin") != null -> response.optJSONObject("admin")!!.optString("status")
            user != null -> user.optString("status")
            else -> ""
        }
        val normalizedStatus = normalizeHubStatus(rawStatus)
        val isActive = normalizedStatus.equals("ACTIVE", ignoreCase = true)
        Log.d("HubAuth", "parsed success=" + success + ", status=" + normalizedStatus)
        Log.d("HubAuth", "isActive=" + isActive)

        if (type == "hub_login" && !isActive) {
            throw IllegalStateException("Akun admin tidak aktif.")
        }

        val parsedUsername = response.optString("username").ifBlank {
            user?.optString("username").orEmpty().ifBlank { username }
        }
        val parsedPassword = response.optString("password").ifBlank {
            user?.optString("password").orEmpty().ifBlank { password }
        }
        val parsedUserId = response.optString("userId").ifBlank {
            user?.optString("userId").orEmpty().ifBlank { parsedUsername.ifBlank { "admin" } }
        }
        Log.d("HubAuth", "parsed user id=$parsedUserId")
        Log.d("HubAuth", "parsed status=$normalizedStatus")
        Log.d("HubAuth", "login status decision=${if (isActive) "accepted" else "not_active"}")
        return PortalUserSession(
            userId = parsedUserId,
            bandName = parsedUsername,
            genre = parsedPassword,
            city = "",
            whatsappNormalized = "",
            createdAt = response.optString("createdAt").ifBlank { user?.optString("createdAt").orEmpty() },
            status = normalizedStatus,
            username = parsedUsername,
            password = parsedPassword
        )
    }

    private fun parseHubUserId(user: JSONObject, normalizedStatus: String): String {
        val rawId = user.opt("id")?.toString()?.trim().orEmpty()
        if (rawId.isBlank()) {
            Log.e("HubAuth", "Invalid HUB user id from backend. rawId=$rawId status=$normalizedStatus")
            if (normalizedStatus.equals("ACTIVE", ignoreCase = true)) {
                throw IllegalStateException("ID akun PARFI tidak valid.")
            }
        }
        return rawId
    }

    private fun normalizeHubStatus(rawStatus: String): String = when (rawStatus.trim().uppercase()) {
        "ACTIVE" -> "ACTIVE"
        "ADMIN" -> "ACTIVE"
        "BLOCKED" -> "BLOCKED"
        else -> "PENDING"
    }

    companion object {
        private const val NETWORK_TIMEOUT_MS = 30_000
        private const val GOOGLE_APPS_SCRIPT_ENDPOINT =
            "https://script.google.com/macros/s/AKfycbxQogA-cfhzFLv93JWblbuDH-b1preG1wRiRP4ubUjdHO3aarN3_9NZWtfFxLBJtAQ_Rw/exec"
    }
}

