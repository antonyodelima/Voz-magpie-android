package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoicePreferenceDao {
    @Query("SELECT * FROM voice_preferences WHERE id = 1")
    fun getVoicePreference(): Flow<VoicePreference?>

    @Query("SELECT * FROM voice_preferences WHERE id = 1")
    suspend fun getVoicePreferenceOnce(): VoicePreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVoicePreference(preference: VoicePreference)

    @Query("UPDATE voice_preferences SET selectedVoiceId = :voiceId, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateSelectedVoice(voiceId: String, timestamp: Long = System.currentTimeMillis())
}
