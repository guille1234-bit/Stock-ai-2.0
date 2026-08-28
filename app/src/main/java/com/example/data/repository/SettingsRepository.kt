package com.example.data.repository

import com.example.data.database.dao.SettingsDao
import com.example.data.database.entity.BusinessSettingsEntity
import com.example.domain.model.BusinessSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDao: SettingsDao
) {
    val settingsFlow: Flow<BusinessSettings> = settingsDao.getSettingsFlow()
        .map { it?.toDomain() ?: BusinessSettings() }

    suspend fun getSettings(): BusinessSettings {
        return settingsDao.getSettings()?.toDomain() ?: BusinessSettings()
    }

    suspend fun updateSettings(settings: BusinessSettings) {
        settingsDao.insertOrUpdate(BusinessSettingsEntity.fromDomain(settings))
    }

    suspend fun completeOnboarding(businessName: String, businessType: String, currencyCode: String, currencySymbol: String) {
        val current = getSettings()
        val updated = current.copy(
            businessName = businessName,
            businessType = businessType,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            isOnboardingCompleted = true
        )
        updateSettings(updated)
    }
}
