package com.example.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconEmoji: String = "📦",
    val colorHex: String = "#3B82F6",
    val isDefault: Boolean = false
)
