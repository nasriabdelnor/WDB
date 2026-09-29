package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IsometricDao {

    @Query("SELECT * FROM isometrics ORDER BY isoNumber ASC")
    fun getAllIsometrics(): Flow<List<Isometric>>

    @Query("SELECT * FROM isometrics WHERE id = :id LIMIT 1")
    fun getIsometricById(id: Long): Flow<Isometric?>

    @Query("SELECT * FROM isometrics WHERE isoNumber = :isoNumber LIMIT 1")
    suspend fun findByNumber(isoNumber: String): Isometric?

    @Query("SELECT DISTINCT isoNumber FROM isometrics ORDER BY isoNumber ASC")
    fun getDistinctIsoNumbers(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(isometric: Isometric): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(isometrics: List<Isometric>)

    @Query("DELETE FROM isometrics")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM isometrics")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM isometrics")
    suspend fun getCountOnce(): Int
}
