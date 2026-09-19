package com.cinemaparfi.jatim.data.local

import android.content.Context
import com.cinemaparfi.jatim.data.model.PortalUserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class PortalSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("portal_session_store", Context.MODE_PRIVATE)

    private val _sessionFlow = MutableStateFlow(loadSession())
    val sessionFlow: StateFlow<PortalUserSession?> = _sessionFlow.asStateFlow()

    private val _pendingWhatsappFlow = MutableStateFlow(prefs.getString("pending_whatsapp", null))
    val pendingWhatsappFlow: StateFlow<String?> = _pendingWhatsappFlow.asStateFlow()

    private val _lastHubWhatsappFlow = MutableStateFlow(prefs.getString("last_hub_whatsapp", null))
    val lastHubWhatsappFlow: StateFlow<String?> = _lastHubWhatsappFlow.asStateFlow()

    fun saveSession(session: PortalUserSession) {
        val json = JSONObject().apply {
            put("userId", session.userId)
            put("bandName", session.bandName)
            put("genre", session.genre)
            put("city", session.city)
            put("whatsappNormalized", session.whatsappNormalized)
            put("createdAt", session.createdAt)
            put("status", session.status)
            put("username", session.username)
            put("password", session.password)
        }.toString()

        prefs.edit().apply {
            putString("session_json", json)
            putString("last_hub_whatsapp", session.whatsappNormalized)
            apply()
        }
        _sessionFlow.value = session
        _lastHubWhatsappFlow.value = session.whatsappNormalized
    }

    fun clearSession() {
        prefs.edit().remove("session_json").apply()
        _sessionFlow.value = null
    }

    fun savePendingWhatsapp(whatsapp: String) {
        prefs.edit().apply {
            putString("pending_whatsapp", whatsapp)
            putString("last_hub_whatsapp", whatsapp)
            apply()
        }
        _pendingWhatsappFlow.value = whatsapp
        _lastHubWhatsappFlow.value = whatsapp
    }

    fun clearPendingWhatsapp() {
        prefs.edit().remove("pending_whatsapp").apply()
        _pendingWhatsappFlow.value = null
    }

    fun clearLastHubWhatsapp() {
        prefs.edit().remove("last_hub_whatsapp").apply()
        _lastHubWhatsappFlow.value = null
    }

    private fun loadSession(): PortalUserSession? {
        val json = prefs.getString("session_json", null) ?: return null
        return try {
            val obj = JSONObject(json)
            PortalUserSession(
                userId = obj.getString("userId"),
                bandName = obj.getString("bandName"),
                genre = obj.getString("genre"),
                city = obj.getString("city"),
                whatsappNormalized = obj.getString("whatsappNormalized"),
                createdAt = obj.getString("createdAt"),
                status = obj.getString("status"),
                username = obj.optString("username").ifBlank { obj.getString("bandName") },
                password = obj.optString("password").ifBlank { obj.getString("genre") }
            )
        } catch (e: Exception) {
            null
        }
    }
}
