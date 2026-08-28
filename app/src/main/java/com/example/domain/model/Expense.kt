package com.example.domain.model

data class Expense(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val category: ExpenseCategory,
    val amount: Double,
    val description: String = ""
)

enum class ExpenseCategory(val label: String, val emoji: String) {
    ALQUILER("Alquiler", "🏢"),
    ELECTRICIDAD("Electricidad", "⚡"),
    GAS("Gas", "🔥"),
    TRANSPORTE("Transporte", "🚚"),
    INSUMOS("Insumos", "📦"),
    PUBLICIDAD("Publicidad", "📢"),
    MANTENIMIENTO("Mantenimiento", "🔧"),
    SALARIOS("Salarios", "👥"),
    OTROS("Otros", "💸");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: OTROS
        }
    }
}
