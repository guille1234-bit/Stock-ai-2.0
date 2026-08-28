package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.BusinessSettings
import com.example.domain.model.ThemeMode

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val businessName: String = "Mi Negocio",
    val businessType: String = "Panadería / Kiosco",
    val currencyCode: String = "ARS",
    val currencySymbol: String = "$",
    val lowStockAlertsEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val themeMode: String = "SYSTEM"
) {
    fun toDomain(): BusinessSettings = BusinessSettings(
        id = id,
        businessName = businessName,
        businessType = businessType,
        currencyCode = currencyCode,
        currencySymbol = currencySymbol,
        lowStockAlertsEnabled = lowStockAlertsEnabled,
        isOnboardingCompleted = isOnboardingCompleted,
        themeMode = try { ThemeMode.valueOf(themeMode) } catch (e: Exception) { ThemeMode.SYSTEM }
    )

    companion object {
        fun fromDomain(settings: BusinessSettings): BusinessSettingsEntity = BusinessSettingsEntity(
            id = settings.id,
            businessName = settings.businessName,
            businessType = settings.businessType,
            currencyCode = settings.currencyCode,
            currencySymbol = settings.currencySymbol,
            lowStockAlertsEnabled = settings.lowStockAlertsEnabled,
            isOnboardingCompleted = settings.isOnboardingCompleted,
            themeMode = settings.themeMode.name
        )
    }
}
