package com.example.utils

import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.Purchase
import com.example.domain.model.Sale
import com.example.domain.model.StockMovement

object CsvExporter {

    fun exportProductsToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("ID,Nombre,Categoria,SKU,Precio_Venta,Costo,Stock_Actual,Stock_Minimo,Unidad,Proveedor,Estado\n")
        for (p in products) {
            val status = if (p.isActive) "Activo" else "Inactivo"
            sb.append("${p.id},\"${escape(p.name)}\",\"${escape(p.categoryName)}\",\"${escape(p.sku)}\",${p.salePrice},${p.costPrice},${p.currentStock},${p.minStock},\"${p.unit}\",\"${escape(p.supplierName ?: "")}\",\"$status\"\n")
        }
        return sb.toString()
    }

    fun exportSalesToCsv(sales: List<Sale>): String {
        val sb = StringBuilder()
        sb.append("ID_Venta,Fecha,Metodo_Pago,Total_Venta,Costo_Total,Ganancia,Items_Detalle,Nota\n")
        for (s in sales) {
            val dateStr = DateUtils.formatFullDateTime(s.timestamp)
            val itemsSummary = s.items.joinToString(" | ") { "${it.productName} (${it.quantity} ${it.unit} x $${it.unitPrice})" }
            sb.append("${s.id},\"$dateStr\",\"${s.paymentMethod.label}\",${s.totalAmount},${s.totalCost},${s.profit},\"${escape(itemsSummary)}\",\"${escape(s.customerNote)}\"\n")
        }
        return sb.toString()
    }

    fun exportPurchasesToCsv(purchases: List<Purchase>): String {
        val sb = StringBuilder()
        sb.append("ID_Compra,Fecha,Proveedor,Total_Compra,Items_Detalle,Nota\n")
        for (p in purchases) {
            val dateStr = DateUtils.formatFullDateTime(p.timestamp)
            val itemsSummary = p.items.joinToString(" | ") { "${it.productName} (${it.quantity} ${it.unit} x $${it.unitCost})" }
            sb.append("${p.id},\"$dateStr\",\"${escape(p.supplierName ?: "Sin proveedor")}\",${p.totalAmount},\"${escape(itemsSummary)}\",\"${escape(p.note)}\"\n")
        }
        return sb.toString()
    }

    fun exportExpensesToCsv(expenses: List<Expense>): String {
        val sb = StringBuilder()
        sb.append("ID_Gasto,Fecha,Categoria,Monto,Descripcion\n")
        for (e in expenses) {
            val dateStr = DateUtils.formatFullDateTime(e.timestamp)
            sb.append("${e.id},\"$dateStr\",\"${e.category.label}\",${e.amount},\"${escape(e.description)}\"\n")
        }
        return sb.toString()
    }

    fun exportMovementsToCsv(movements: List<StockMovement>): String {
        val sb = StringBuilder()
        sb.append("ID_Movimiento,Fecha,Producto,Tipo,Cambio_Cantidad,Stock_Previo,Stock_Nuevo,Unidad,Motivo\n")
        for (m in movements) {
            val dateStr = DateUtils.formatFullDateTime(m.timestamp)
            sb.append("${m.id},\"$dateStr\",\"${escape(m.productName)}\",\"${m.type.label}\",${m.quantityChange},${m.previousStock},${m.newStock},\"${m.unit}\",\"${escape(m.reason)}\"\n")
        }
        return sb.toString()
    }

    private fun escape(text: String): String = text.replace("\"", "\"\"")
}
