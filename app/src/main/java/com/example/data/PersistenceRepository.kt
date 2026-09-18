package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersistenceRepository(
    private val context: Context,
    private val appDatabase: AppDatabase = AppDatabase.getInstance(context)
) {
    private val authDao = appDatabase.authDao()
    private val voicePreferenceDao = appDatabase.voicePreferenceDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vozo_persistence_prefs", Context.MODE_PRIVATE)

    val authState: Flow<UserAuthState> = authDao.getAuthState().map { it ?: UserAuthState() }
    val voicePreference: Flow<VoicePreference> =
        voicePreferenceDao.getVoicePreference().map { it ?: VoicePreference() }

    suspend fun saveAuthState(state: UserAuthState) = withContext(Dispatchers.IO) {
        authDao.saveAuthState(state)
        prefs.edit()
            .putString("auth_token", state.token)
            .putString("user_email", state.userEmail)
            .putString("user_id", state.userId)
            .putBoolean("is_logged_in", state.isLoggedIn)
            .putLong("last_login", state.lastLoginTimestamp)
            .apply()
    }

    suspend fun updateAuthToken(token: String?, userEmail: String? = null) = withContext(Dispatchers.IO) {
        val current = authDao.getAuthStateOnce() ?: UserAuthState()
        val updated = current.copy(
            token = token,
            userEmail = userEmail ?: current.userEmail,
            isLoggedIn = !token.isNullOrBlank(),
            lastLoginTimestamp = System.currentTimeMillis()
        )
        saveAuthState(updated)
    }

    suspend fun clearAuthState() = withContext(Dispatchers.IO) {
        authDao.clearAuthState()
        prefs.edit()
            .remove("auth_token")
            .remove("user_email")
            .remove("user_id")
            .putBoolean("is_logged_in", false)
            .apply()
    }

    suspend fun saveVoicePreference(pref: VoicePreference) = withContext(Dispatchers.IO) {
        voicePreferenceDao.saveVoicePreference(pref)
        prefs.edit()
            .putString("selected_voice_id", pref.selectedVoiceId)
            .putString("custom_voice_name", pref.customVoiceName)
            .putString("voice_provider", pref.voiceProvider)
            .putString("language_code", pref.languageCode)
            .apply()
    }

    suspend fun updateSelectedVoice(voiceId: String, provider: String? = null) = withContext(Dispatchers.IO) {
        val current = voicePreferenceDao.getVoicePreferenceOnce() ?: VoicePreference()
        val updated = current.copy(
            selectedVoiceId = voiceId,
            voiceProvider = provider ?: current.voiceProvider,
            lastUpdated = System.currentTimeMillis()
        )
        saveVoicePreference(updated)
    }

    suspend fun getCurrentAuthState(): UserAuthState = withContext(Dispatchers.IO) {
        authDao.getAuthStateOnce() ?: UserAuthState(
            token = prefs.getString("auth_token", null),
            userEmail = prefs.getString("user_email", null),
            userId = prefs.getString("user_id", null),
            isLoggedIn = prefs.getBoolean("is_logged_in", false)
        )
    }

    suspend fun getCurrentVoicePreference(): VoicePreference = withContext(Dispatchers.IO) {
        voicePreferenceDao.getVoicePreferenceOnce() ?: VoicePreference(
            selectedVoiceId = prefs.getString("selected_voice_id", "minha_voz") ?: "minha_voz",
            customVoiceName = prefs.getString("custom_voice_name", "Minha Voz (Clone)") ?: "Minha Voz (Clone)",
            voiceProvider = prefs.getString("voice_provider", "gemini") ?: "gemini",
            languageCode = prefs.getString("language_code", "auto") ?: "auto"
        )
    }

    // Synchronous reads for fast WebView bootstrapping
    fun getCachedToken(): String? = prefs.getString("auth_token", null)
    fun getCachedSelectedVoiceId(): String = prefs.getString("selected_voice_id", "minha_voz") ?: "minha_voz"
    fun getCachedCustomVoiceName(): String = prefs.getString("custom_voice_name", "Minha Voz (Clone)") ?: "Minha Voz (Clone)"
    fun getCachedVoiceProvider(): String = prefs.getString("voice_provider", "gemini") ?: "gemini"
}
