package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {
    @Query("SELECT * FROM user_auth_state WHERE id = 1")
    fun getAuthState(): Flow<UserAuthState?>

    @Query("SELECT * FROM user_auth_state WHERE id = 1")
    suspend fun getAuthStateOnce(): UserAuthState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAuthState(authState: UserAuthState)

    @Query("UPDATE user_auth_state SET token = :token, isLoggedIn = :isLoggedIn, lastLoginTimestamp = :timestamp WHERE id = 1")
    suspend fun updateToken(token: String?, isLoggedIn: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_auth_state")
    suspend fun clearAuthState()
}
