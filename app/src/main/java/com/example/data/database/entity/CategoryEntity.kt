package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconEmoji: String = "📦",
    val colorHex: String = "#3B82F6",
    val isDefault: Boolean = false
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        iconEmoji = iconEmoji,
        colorHex = colorHex,
        isDefault = isDefault
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            iconEmoji = category.iconEmoji,
            colorHex = category.colorHex,
            isDefault = category.isDefault
        )
    }
}
