package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.database.dao.CategoryDao
import com.example.data.database.dao.ExpenseDao
import com.example.data.database.dao.ProductDao
import com.example.data.database.dao.PurchaseDao
import com.example.data.database.dao.SaleDao
import com.example.data.database.dao.SettingsDao
import com.example.data.database.dao.StockMovementDao
import com.example.data.database.dao.SupplierDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        ExpenseEntity::class,
        SupplierEntity::class,
        StockMovementEntity::class,
        BusinessSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun supplierDao(): SupplierDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock_ai_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val settingsDao = database.settingsDao()

            if (categoryDao.getCategoryCount() == 0) {
                val defaultCategories = listOf(
                    CategoryEntity(id = 1, name = "Panadería", iconEmoji = "🍞", colorHex = "#F59E0B", isDefault = true),
                    CategoryEntity(id = 2, name = "Pizzas & Empanadas", iconEmoji = "🍕", colorHex = "#EF4444", isDefault = true),
                    CategoryEntity(id = 3, name = "Bebidas", iconEmoji = "🥤", colorHex = "#3B82F6", isDefault = true),
                    CategoryEntity(id = 4, name = "Comidas & Minutas", iconEmoji = "🍔", colorHex = "#10B981", isDefault = true),
                    CategoryEntity(id = 5, name = "Kiosco & Golosinas", iconEmoji = "🍬", colorHex = "#8B5CF6", isDefault = true),
                    CategoryEntity(id = 6, name = "Insumos & Almacén", iconEmoji = "📦", colorHex = "#64748B", isDefault = true)
                )
                categoryDao.insertCategories(defaultCategories)
            }

            if (settingsDao.getSettings() == null) {
                settingsDao.insertOrUpdate(
                    BusinessSettingsEntity(
                        id = 1,
                        businessName = "Mi Negocio",
                        businessType = "Panadería / Kiosco",
                        currencyCode = "ARS",
                        currencySymbol = "$",
                        lowStockAlertsEnabled = true,
                        isOnboardingCompleted = false,
                        themeMode = "SYSTEM"
                    )
                )
            }
        }
    }
}
