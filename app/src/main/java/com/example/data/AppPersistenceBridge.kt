package com.example.data

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppPersistenceBridge(
    private val repository: PersistenceRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    @JavascriptInterface
    fun saveAuthToken(token: String?, email: String?) {
        coroutineScope.launch {
            repository.updateAuthToken(token, email)
        }
    }

    @JavascriptInterface
    fun getSavedToken(): String {
        return repository.getCachedToken() ?: ""
    }

    @JavascriptInterface
    fun saveVoicePreference(voiceId: String, voiceName: String?, provider: String?, lang: String?) {
        coroutineScope.launch {
            val pref = VoicePreference(
                selectedVoiceId = voiceId,
                customVoiceName = voiceName ?: repository.getCachedCustomVoiceName(),
                voiceProvider = provider ?: repository.getCachedVoiceProvider(),
                languageCode = lang ?: "auto",
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveVoicePreference(pref)
        }
    }

    @JavascriptInterface
    fun getSelectedVoiceId(): String {
        return repository.getCachedSelectedVoiceId()
    }

    @JavascriptInterface
    fun getCustomVoiceName(): String {
        return repository.getCachedCustomVoiceName()
    }

    @JavascriptInterface
    fun getVoiceProvider(): String {
        return repository.getCachedVoiceProvider()
    }

    @JavascriptInterface
    fun clearAuth() {
        coroutineScope.launch {
            repository.clearAuthState()
        }
    }
}
