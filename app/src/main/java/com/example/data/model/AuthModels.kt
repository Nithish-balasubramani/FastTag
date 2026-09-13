package com.example.data.model

enum class UserRole {
    ADMIN,
    USER
}

data class LoggedInUser(
    val role: UserRole,
    val userId: String,
    val displayName: String,
    val region: String = ""
)
