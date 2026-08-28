package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.entity.BusinessSettingsEntity
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.ExpenseEntity
import com.example.data.database.entity.ProductEntity
import com.example.data.database.entity.PurchaseEntity
import com.example.data.database.entity.PurchaseItemEntity
import com.example.data.database.entity.SaleEntity
import com.example.data.database.entity.SaleItemEntity
import com.example.data.database.entity.StockMovementEntity
import com.example.data.database.entity.SupplierEntity
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val database: AppDatabase
) {
    suspend fun exportBackupJson(): String = exportJsonBackup()

    suspend fun importBackupJson(jsonString: String): Boolean = importJsonBackup(jsonString).isSuccess

    suspend fun clearAllDatabase(): Result<Unit> = wipeDatabase()

    suspend fun exportJsonBackup(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "Stock AI")

        // Settings
        val settings = database.settingsDao().getSettings()
        if (settings != null) {
            val settingsJson = JSONObject()
            settingsJson.put("businessName", settings.businessName)
            settingsJson.put("businessType", settings.businessType)
            settingsJson.put("currencyCode", settings.currencyCode)
            settingsJson.put("currencySymbol", settings.currencySymbol)
            settingsJson.put("lowStockAlertsEnabled", settings.lowStockAlertsEnabled)
            root.put("settings", settingsJson)
        }

        // Categories
        val categories = database.categoryDao().getAllCategories().first()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconEmoji", c.iconEmoji)
            obj.put("colorHex", c.colorHex)
            obj.put("isDefault", c.isDefault)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Suppliers
        val suppliers = database.supplierDao().getAllSuppliers().first()
        val supArray = JSONArray()
        for (s in suppliers) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("phone", s.phone)
            obj.put("description", s.description)
            obj.put("createdAt", s.createdAt)
            supArray.put(obj)
        }
        root.put("suppliers", supArray)

        // Products
        val products = database.productDao().getAllProducts().first()
        val prodArray = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("description", p.description)
            obj.put("categoryId", p.categoryId ?: JSONObject.NULL)
            obj.put("categoryName", p.categoryName)
            obj.put("sku", p.sku)
            obj.put("salePrice", p.salePrice)
            obj.put("costPrice", p.costPrice)
            obj.put("currentStock", p.currentStock)
            obj.put("minStock", p.minStock)
            obj.put("unit", p.unit)
            obj.put("supplierId", p.supplierId ?: JSONObject.NULL)
            obj.put("supplierName", p.supplierName ?: JSONObject.NULL)
            obj.put("createdAt", p.createdAt)
            obj.put("isActive", p.isActive)
            prodArray.put(obj)
        }
        root.put("products", prodArray)

        // Expenses
        val expenses = database.expenseDao().getAllExpenses().first()
        val expArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("timestamp", e.timestamp)
            obj.put("category", e.category)
            obj.put("amount", e.amount)
            obj.put("description", e.description)
            expArray.put(obj)
        }
        root.put("expenses", expArray)

        // Sales & Items
        val sales = database.saleDao().getAllSales().first()
        val salesArray = JSONArray()
        for (saleWithItems in sales) {
            val s = saleWithItems.sale
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("timestamp", s.timestamp)
            obj.put("totalAmount", s.totalAmount)
            obj.put("totalCost", s.totalCost)
            obj.put("profit", s.profit)
            obj.put("paymentMethod", s.paymentMethod)
            obj.put("customerNote", s.customerNote)

            val itemsArr = JSONArray()
            for (item in saleWithItems.items) {
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("productId", item.productId)
                itemObj.put("productName", item.productName)
                itemObj.put("unit", item.unit)
                itemObj.put("quantity", item.quantity)
                itemObj.put("unitPrice", item.unitPrice)
                itemObj.put("unitCost", item.unitCost)
                itemObj.put("subtotal", item.subtotal)
                itemObj.put("totalCost", item.totalCost)
                itemsArr.put(itemObj)
            }
            obj.put("items", itemsArr)
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        // Purchases & Items
        val purchases = database.purchaseDao().getAllPurchases().first()
        val purchasesArray = JSONArray()
        for (pWithItems in purchases) {
            val p = pWithItems.purchase
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("timestamp", p.timestamp)
            obj.put("supplierId", p.supplierId ?: JSONObject.NULL)
            obj.put("supplierName", p.supplierName ?: JSONObject.NULL)
            obj.put("totalAmount", p.totalAmount)
            obj.put("note", p.note)

            val itemsArr = JSONArray()
            for (item in pWithItems.items) {
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("productId", item.productId)
                itemObj.put("productName", item.productName)
                itemObj.put("unit", item.unit)
                itemObj.put("quantity", item.quantity)
                itemObj.put("unitCost", item.unitCost)
                itemObj.put("subtotal", item.subtotal)
                itemsArr.put(itemObj)
            }
            obj.put("items", itemsArr)
            purchasesArray.put(obj)
        }
        root.put("purchases", purchasesArray)

        // Stock Movements
        val movements = database.stockMovementDao().getAllMovements().first()
        val movArray = JSONArray()
        for (m in movements) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("timestamp", m.timestamp)
            obj.put("productId", m.productId)
            obj.put("productName", m.productName)
            obj.put("type", m.type)
            obj.put("quantityChange", m.quantityChange)
            obj.put("previousStock", m.previousStock)
            obj.put("newStock", m.newStock)
            obj.put("unit", m.unit)
            obj.put("reason", m.reason)
            obj.put("referenceId", m.referenceId ?: JSONObject.NULL)
            movArray.put(obj)
        }
        root.put("movements", movArray)

        return root.toString(2)
    }

    suspend fun importJsonBackup(jsonString: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("products") && !root.has("categories")) {
                return Result.failure(IllegalArgumentException("El archivo no tiene el formato de copia de seguridad de Stock AI."))
            }

            database.withTransaction {
                // Clear existing
                database.saleDao().deleteAllSales()
                database.purchaseDao().deleteAllPurchases()
                database.expenseDao().deleteAllExpenses()
                database.stockMovementDao().deleteAllMovements()
                database.productDao().deleteAllProducts()
                database.supplierDao().deleteAllSuppliers()

                // Settings
                if (root.has("settings")) {
                    val sJson = root.getJSONObject("settings")
                    database.settingsDao().insertOrUpdate(
                        BusinessSettingsEntity(
                            id = 1,
                            businessName = sJson.optString("businessName", "Mi Negocio"),
                            businessType = sJson.optString("businessType", "Comercio"),
                            currencyCode = sJson.optString("currencyCode", "ARS"),
                            currencySymbol = sJson.optString("currencySymbol", "$"),
                            lowStockAlertsEnabled = sJson.optBoolean("lowStockAlertsEnabled", true),
                            isOnboardingCompleted = true,
                            themeMode = "SYSTEM"
                        )
                    )
                }

                // Categories
                if (root.has("categories")) {
                    val catArr = root.getJSONArray("categories")
                    val catList = mutableListOf<CategoryEntity>()
                    for (i in 0 until catArr.length()) {
                        val obj = catArr.getJSONObject(i)
                        catList.add(
                            CategoryEntity(
                                id = obj.optLong("id", 0),
                                name = obj.getString("name"),
                                iconEmoji = obj.optString("iconEmoji", "📦"),
                                colorHex = obj.optString("colorHex", "#3B82F6"),
                                isDefault = obj.optBoolean("isDefault", false)
                            )
                        )
                    }
                    database.categoryDao().insertCategories(catList)
                }

                // Suppliers
                if (root.has("suppliers")) {
                    val supArr = root.getJSONArray("suppliers")
                    for (i in 0 until supArr.length()) {
                        val obj = supArr.getJSONObject(i)
                        database.supplierDao().insertSupplier(
                            SupplierEntity(
                                id = obj.optLong("id", 0),
                                name = obj.getString("name"),
                                phone = obj.optString("phone", ""),
                                description = obj.optString("description", ""),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Products
                if (root.has("products")) {
                    val prodArr = root.getJSONArray("products")
                    val prodList = mutableListOf<ProductEntity>()
                    for (i in 0 until prodArr.length()) {
                        val obj = prodArr.getJSONObject(i)
                        prodList.add(
                            ProductEntity(
                                id = obj.optLong("id", 0),
                                name = obj.getString("name"),
                                description = obj.optString("description", ""),
                                categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                                categoryName = obj.optString("categoryName", "Otros"),
                                sku = obj.optString("sku", ""),
                                salePrice = obj.getDouble("salePrice"),
                                costPrice = obj.getDouble("costPrice"),
                                currentStock = obj.getDouble("currentStock"),
                                minStock = obj.optDouble("minStock", 5.0),
                                unit = obj.optString("unit", "unidad"),
                                supplierId = if (obj.has("supplierId") && !obj.isNull("supplierId")) obj.getLong("supplierId") else null,
                                supplierName = if (obj.has("supplierName") && !obj.isNull("supplierName")) obj.getString("supplierName") else null,
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                isActive = obj.optBoolean("isActive", true)
                            )
                        )
                    }
                    database.productDao().insertProducts(prodList)
                }

                // Expenses
                if (root.has("expenses")) {
                    val expArr = root.getJSONArray("expenses")
                    for (i in 0 until expArr.length()) {
                        val obj = expArr.getJSONObject(i)
                        database.expenseDao().insertExpense(
                            ExpenseEntity(
                                id = obj.optLong("id", 0),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                category = obj.getString("category"),
                                amount = obj.getDouble("amount"),
                                description = obj.optString("description", "")
                            )
                        )
                    }
                }

                // Sales
                if (root.has("sales")) {
                    val salesArr = root.getJSONArray("sales")
                    for (i in 0 until salesArr.length()) {
                        val obj = salesArr.getJSONObject(i)
                        val saleEntity = SaleEntity(
                            id = obj.optLong("id", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            totalAmount = obj.getDouble("totalAmount"),
                            totalCost = obj.getDouble("totalCost"),
                            profit = obj.getDouble("profit"),
                            paymentMethod = obj.optString("paymentMethod", "EFECTIVO"),
                            customerNote = obj.optString("customerNote", "")
                        )
                        val saleId = database.saleDao().insertSale(saleEntity)

                        if (obj.has("items")) {
                            val itemsArr = obj.getJSONArray("items")
                            val itemList = mutableListOf<SaleItemEntity>()
                            for (j in 0 until itemsArr.length()) {
                                val itemObj = itemsArr.getJSONObject(j)
                                itemList.add(
                                    SaleItemEntity(
                                        id = itemObj.optLong("id", 0),
                                        saleId = saleId,
                                        productId = itemObj.getLong("productId"),
                                        productName = itemObj.getString("productName"),
                                        unit = itemObj.optString("unit", "unidad"),
                                        quantity = itemObj.getDouble("quantity"),
                                        unitPrice = itemObj.getDouble("unitPrice"),
                                        unitCost = itemObj.getDouble("unitCost"),
                                        subtotal = itemObj.getDouble("subtotal"),
                                        totalCost = itemObj.getDouble("totalCost")
                                    )
                                )
                            }
                            database.saleDao().insertSaleItems(itemList)
                        }
                    }
                }

                // Purchases
                if (root.has("purchases")) {
                    val purArr = root.getJSONArray("purchases")
                    for (i in 0 until purArr.length()) {
                        val obj = purArr.getJSONObject(i)
                        val purEntity = PurchaseEntity(
                            id = obj.optLong("id", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            supplierId = if (obj.has("supplierId") && !obj.isNull("supplierId")) obj.getLong("supplierId") else null,
                            supplierName = if (obj.has("supplierName") && !obj.isNull("supplierName")) obj.getString("supplierName") else null,
                            totalAmount = obj.getDouble("totalAmount"),
                            note = obj.optString("note", "")
                        )
                        val purId = database.purchaseDao().insertPurchase(purEntity)

                        if (obj.has("items")) {
                            val itemsArr = obj.getJSONArray("items")
                            val itemList = mutableListOf<PurchaseItemEntity>()
                            for (j in 0 until itemsArr.length()) {
                                val itemObj = itemsArr.getJSONObject(j)
                                itemList.add(
                                    PurchaseItemEntity(
                                        id = itemObj.optLong("id", 0),
                                        purchaseId = purId,
                                        productId = itemObj.getLong("productId"),
                                        productName = itemObj.getString("productName"),
                                        unit = itemObj.optString("unit", "unidad"),
                                        quantity = itemObj.getDouble("quantity"),
                                        unitCost = itemObj.getDouble("unitCost"),
                                        subtotal = itemObj.getDouble("subtotal")
                                    )
                                )
                            }
                            database.purchaseDao().insertPurchaseItems(itemList)
                        }
                    }
                }

                // Stock Movements
                if (root.has("movements")) {
                    val movArr = root.getJSONArray("movements")
                    val movList = mutableListOf<StockMovementEntity>()
                    for (i in 0 until movArr.length()) {
                        val obj = movArr.getJSONObject(i)
                        movList.add(
                            StockMovementEntity(
                                id = obj.optLong("id", 0),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                productId = obj.getLong("productId"),
                                productName = obj.getString("productName"),
                                type = obj.getString("type"),
                                quantityChange = obj.getDouble("quantityChange"),
                                previousStock = obj.getDouble("previousStock"),
                                newStock = obj.getDouble("newStock"),
                                unit = obj.optString("unit", "unidad"),
                                reason = obj.optString("reason", ""),
                                referenceId = if (obj.has("referenceId") && !obj.isNull("referenceId")) obj.getLong("referenceId") else null
                            )
                        )
                    }
                    database.stockMovementDao().insertMovements(movList)
                }
            }
            Result.success("Copia de seguridad restaurada correctamente.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadDemoData(): Result<String> {
        return try {
            database.withTransaction {
                // Clear existing
                wipeDatabase()

                // Settings
                database.settingsDao().insertOrUpdate(
                    BusinessSettingsEntity(
                        id = 1,
                        businessName = "Panadería & Kiosco Don Carlos",
                        businessType = "Panadería y Almacén",
                        currencyCode = "ARS",
                        currencySymbol = "$",
                        lowStockAlertsEnabled = true,
                        isOnboardingCompleted = true,
                        themeMode = "SYSTEM"
                    )
                )

                // Categories
                val panaderia = CategoryEntity(id = 1, name = "Panadería", iconEmoji = "🍞", colorHex = "#F59E0B", isDefault = true)
                val pizzas = CategoryEntity(id = 2, name = "Pizzas & Empanadas", iconEmoji = "🍕", colorHex = "#EF4444", isDefault = true)
                val bebidas = CategoryEntity(id = 3, name = "Bebidas", iconEmoji = "🥤", colorHex = "#3B82F6", isDefault = true)
                val comidas = CategoryEntity(id = 4, name = "Comidas & Minutas", iconEmoji = "🍔", colorHex = "#10B981", isDefault = true)
                val kiosco = CategoryEntity(id = 5, name = "Kiosco & Golosinas", iconEmoji = "🍬", colorHex = "#8B5CF6", isDefault = true)
                val otros = CategoryEntity(id = 6, name = "Insumos & Otros", iconEmoji = "📦", colorHex = "#64748B", isDefault = true)
                database.categoryDao().insertCategories(listOf(panaderia, pizzas, bebidas, comidas, kiosco, otros))

                // Suppliers
                val sup1Id = database.supplierDao().insertSupplier(
                    SupplierEntity(id = 1, name = "Distribuidora Mayorista Central", phone = "1144556677", description = "Harinas, levaduras y lácteos")
                )
                val sup2Id = database.supplierDao().insertSupplier(
                    SupplierEntity(id = 2, name = "Bebidas & Gaseosas Express", phone = "1188990011", description = "Gaseosas, aguas y jugos")
                )

                // Products
                val p1 = ProductEntity(
                    id = 1, name = "Pizza Muzzarella Artesanal", description = "Masa madre con doble muzzarella",
                    categoryId = 2, categoryName = "Pizzas & Empanadas", sku = "PIZ-01",
                    salePrice = 7500.0, costPrice = 3200.0, currentStock = 18.0, minStock = 5.0, unit = "unidad",
                    supplierId = sup1Id, supplierName = "Distribuidora Mayorista Central", createdAt = System.currentTimeMillis() - 7 * 86400000L
                )
                val p2 = ProductEntity(
                    id = 2, name = "Empanada de Carne Cortada a Cuchillo", description = "Relleno criollo horneado",
                    categoryId = 2, categoryName = "Pizzas & Empanadas", sku = "EMP-01",
                    salePrice = 1400.0, costPrice = 650.0, currentStock = 35.0, minStock = 10.0, unit = "unidad",
                    supplierId = sup1Id, supplierName = "Distribuidora Mayorista Central", createdAt = System.currentTimeMillis() - 7 * 86400000L
                )
                val p3 = ProductEntity(
                    id = 3, name = "Coca Cola 1.5L Original", description = "Botella descartable",
                    categoryId = 3, categoryName = "Bebidas", sku = "BEB-01",
                    salePrice = 2800.0, costPrice = 1750.0, currentStock = 4.0, minStock = 8.0, unit = "unidad", // LOW STOCK ALERT
                    supplierId = sup2Id, supplierName = "Bebidas & Gaseosas Express", createdAt = System.currentTimeMillis() - 7 * 86400000L
                )
                val p4 = ProductEntity(
                    id = 4, name = "Pan Francés Tradicional", description = "Crocante recién horneado",
                    categoryId = 1, categoryName = "Panadería", sku = "PAN-01",
                    salePrice = 2200.0, costPrice = 900.0, currentStock = 22.0, minStock = 5.0, unit = "kg",
                    supplierId = sup1Id, supplierName = "Distribuidora Mayorista Central", createdAt = System.currentTimeMillis() - 7 * 86400000L
                )
                val p5 = ProductEntity(
                    id = 5, name = "Medialunas de Manteca", description = "Docena de medialunas dulces",
                    categoryId = 1, categoryName = "Panadería", sku = "MED-01",
                    salePrice = 6000.0, costPrice = 2600.0, currentStock = 2.0, minStock = 6.0, unit = "docena", // LOW STOCK ALERT
                    supplierId = sup1Id, supplierName = "Distribuidora Mayorista Central", createdAt = System.currentTimeMillis() - 7 * 86400000L
                )
                val p6 = ProductEntity(
                    id = 6, name = "Alfajor Triple Chocolate", description = "Relleno con dulce de leche",
                    categoryId = 5, categoryName = "Kiosco & Golosinas", sku = "ALF-01",
                    salePrice = 1200.0, costPrice = 650.0, currentStock = 0.0, minStock = 10.0, unit = "unidad", // OUT OF STOCK
                    supplierId = sup2Id, supplierName = "Bebidas & Gaseosas Express", createdAt = System.currentTimeMillis() - 7 * 86400000L
                )

                database.productDao().insertProducts(listOf(p1, p2, p3, p4, p5, p6))

                // Initial Movements
                for (p in listOf(p1, p2, p3, p4, p5, p6)) {
                    database.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            timestamp = System.currentTimeMillis() - 7 * 86400000L,
                            productId = p.id,
                            productName = p.name,
                            type = MovementType.INICIAL.name,
                            quantityChange = p.currentStock + 15.0,
                            previousStock = 0.0,
                            newStock = p.currentStock + 15.0,
                            unit = p.unit,
                            reason = "Inventario inicial de prueba"
                        )
                    )
                }

                // Sample Sales over the last 5 days
                val now = System.currentTimeMillis()

                // Sale 1: Today
                val sale1 = SaleEntity(
                    id = 1, timestamp = now - 2 * 3600000L, totalAmount = 17900.0, totalCost = 8100.0, profit = 9800.0,
                    paymentMethod = PaymentMethod.EFECTIVO.name, customerNote = "Mesa 3 / Para llevar"
                )
                val s1Id = database.saleDao().insertSale(sale1)
                database.saleDao().insertSaleItems(
                    listOf(
                        SaleItemEntity(saleId = s1Id, productId = 1, productName = "Pizza Muzzarella Artesanal", unit = "unidad", quantity = 2.0, unitPrice = 7500.0, unitCost = 3200.0, subtotal = 15000.0, totalCost = 6400.0),
                        SaleItemEntity(saleId = s1Id, productId = 3, productName = "Coca Cola 1.5L Original", unit = "unidad", quantity = 1.0, unitPrice = 2800.0, unitCost = 1750.0, subtotal = 2800.0, totalCost = 1750.0)
                    )
                )

                // Sale 2: Yesterday
                val sale2 = SaleEntity(
                    id = 2, timestamp = now - 86400000L, totalAmount = 14400.0, totalCost = 6500.0, profit = 7900.0,
                    paymentMethod = PaymentMethod.TRANSFERENCIA.name, customerNote = "Cliente habitual"
                )
                val s2Id = database.saleDao().insertSale(sale2)
                database.saleDao().insertSaleItems(
                    listOf(
                        SaleItemEntity(saleId = s2Id, productId = 2, productName = "Empanada de Carne Cortada a Cuchillo", unit = "unidad", quantity = 6.0, unitPrice = 1400.0, unitCost = 650.0, subtotal = 8400.0, totalCost = 3900.0),
                        SaleItemEntity(saleId = s2Id, productId = 5, productName = "Medialunas de Manteca", unit = "docena", quantity = 1.0, unitPrice = 6000.0, unitCost = 2600.0, subtotal = 6000.0, totalCost = 2600.0)
                    )
                )

                // Sale 3: 3 days ago
                val sale3 = SaleEntity(
                    id = 3, timestamp = now - 3 * 86400000L, totalAmount = 21000.0, totalCost = 9100.0, profit = 11900.0,
                    paymentMethod = PaymentMethod.TARJETA.name, customerNote = "Pedido fin de semana"
                )
                val s3Id = database.saleDao().insertSale(sale3)
                database.saleDao().insertSaleItems(
                    listOf(
                        SaleItemEntity(saleId = s3Id, productId = 1, productName = "Pizza Muzzarella Artesanal", unit = "unidad", quantity = 2.0, unitPrice = 7500.0, unitCost = 3200.0, subtotal = 15000.0, totalCost = 6400.0),
                        SaleItemEntity(saleId = s3Id, productId = 5, productName = "Medialunas de Manteca", unit = "docena", quantity = 1.0, unitPrice = 6000.0, unitCost = 2600.0, subtotal = 6000.0, totalCost = 2600.0)
                    )
                )

                // Sample Expenses
                database.expenseDao().insertExpense(
                    ExpenseEntity(timestamp = now - 2 * 86400000L, category = ExpenseCategory.ELECTRICIDAD.name, amount = 12500.0, description = "Factura luz local comercial")
                )
                database.expenseDao().insertExpense(
                    ExpenseEntity(timestamp = now - 4 * 86400000L, category = ExpenseCategory.INSUMOS.name, amount = 4200.0, description = "Cajas de cartón para pizza y servilletas")
                )

                // Sample Movements
                database.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        timestamp = now - 2 * 3600000L, productId = 1, productName = "Pizza Muzzarella Artesanal",
                        type = MovementType.VENTA.name, quantityChange = -2.0, previousStock = 20.0, newStock = 18.0, unit = "unidad",
                        reason = "Venta #1", referenceId = s1Id
                    )
                )
                database.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        timestamp = now - 86400000L, productId = 3, productName = "Coca Cola 1.5L Original",
                        type = MovementType.PERDIDA.name, quantityChange = -1.0, previousStock = 5.0, newStock = 4.0, unit = "unidad",
                        reason = "Envase dañado durante descarga"
                    )
                )
            }
            Result.success("Datos de demostración cargados con éxito.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun wipeDatabase(): Result<Unit> {
        return try {
            database.withTransaction {
                database.saleDao().deleteAllSales()
                database.purchaseDao().deleteAllPurchases()
                database.expenseDao().deleteAllExpenses()
                database.stockMovementDao().deleteAllMovements()
                database.productDao().deleteAllProducts()
                database.supplierDao().deleteAllSuppliers()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
