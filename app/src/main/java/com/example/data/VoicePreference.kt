package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_preferences")
data class VoicePreference(
    @PrimaryKey val id: Int = 1,
    val selectedVoiceId: String = "minha_voz",
    val customVoiceName: String = "Minha Voz (Clone)",
    val voiceProvider: String = "gemini",
    val languageCode: String = "auto",
    val lastUpdated: Long = System.currentTimeMillis()
)
