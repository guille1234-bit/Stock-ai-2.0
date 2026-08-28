package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsByProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE type = :type ORDER BY timestamp DESC")
    fun getMovementsByType(type: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getMovementsByDateRange(startTime: Long, endTime: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productName LIKE '%' || :query || '%' OR reason LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMovements(query: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements")
    suspend fun getAllMovementsSync(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements WHERE id = :id")
    suspend fun deleteMovement(id: Long)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAllMovements()
}
