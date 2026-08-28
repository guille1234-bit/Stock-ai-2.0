package com.example.domain.model

data class Supplier(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
