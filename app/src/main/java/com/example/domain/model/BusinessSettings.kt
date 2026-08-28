package com.example.domain.model

data class BusinessSettings(
    val id: Long = 1,
    val businessName: String = "Mi Negocio",
    val businessType: String = "Comercio",
    val currencyCode: String = "ARS",
    val currencySymbol: String = "$",
    val lowStockAlertsEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode(val label: String) {
    SYSTEM("Seguir sistema"),
    LIGHT("Claro"),
    DARK("Oscuro")
}
