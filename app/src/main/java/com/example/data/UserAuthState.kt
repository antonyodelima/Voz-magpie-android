package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_auth_state")
data class UserAuthState(
    @PrimaryKey val id: Int = 1,
    val token: String? = null,
    val refreshToken: String? = null,
    val userEmail: String? = null,
    val userId: String? = null,
    val isLoggedIn: Boolean = false,
    val apiKey: String? = null,
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)
