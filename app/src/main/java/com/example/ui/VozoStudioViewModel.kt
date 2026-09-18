package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PersistenceRepository
import com.example.data.UserAuthState
import com.example.data.VoicePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VozoStudioViewModel(application: Application) : AndroidViewModel(application) {

    val repository = PersistenceRepository(
        context = application.applicationContext,
        appDatabase = AppDatabase.getInstance(application.applicationContext)
    )

    val authState: StateFlow<UserAuthState> = repository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserAuthState(
                token = repository.getCachedToken(),
                isLoggedIn = !repository.getCachedToken().isNullOrBlank()
            )
        )

    val voicePreference: StateFlow<VoicePreference> = repository.voicePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VoicePreference(
                selectedVoiceId = repository.getCachedSelectedVoiceId(),
                customVoiceName = repository.getCachedCustomVoiceName(),
                voiceProvider = repository.getCachedVoiceProvider()
            )
        )

    fun saveAuthToken(token: String?, email: String? = null) {
        viewModelScope.launch {
            repository.updateAuthToken(token, email)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearAuthState()
        }
    }

    fun selectVoice(voiceId: String, provider: String? = null) {
        viewModelScope.launch {
            repository.updateSelectedVoice(voiceId, provider)
        }
    }

    fun updateVoicePreference(preference: VoicePreference) {
        viewModelScope.launch {
            repository.saveVoicePreference(preference)
        }
    }
}
