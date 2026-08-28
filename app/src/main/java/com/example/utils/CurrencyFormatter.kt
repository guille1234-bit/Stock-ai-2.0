package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "$", currencyCode: String = "ARS"): String {
        val locale = when (currencyCode.uppercase()) {
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "BRL" -> Locale("pt", "BR")
            "MXN" -> Locale("es", "MX")
            "COP", "CLP", "PEN", "UYU" -> Locale("es", "AR")
            else -> Locale("es", "AR")
        }

        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }

        val formattedNumber = numberFormat.format(amount)
        return when (currencyCode.uppercase()) {
            "EUR" -> "$formattedNumber €"
            "USD" -> "US$ $formattedNumber"
            else -> "$symbol $formattedNumber"
        }
    }

    fun formatPercent(percent: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return "${format.format(percent)}%"
    }

    fun formatQuantity(quantity: Double, unit: String): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = if (quantity % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 3
        }
        val formatted = format.format(quantity)
        return "$formatted $unit"
    }
}
