package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpoolDao {

    @Query("SELECT * FROM spools WHERE isoId = :isoId ORDER BY spoolNumber ASC")
    fun getSpoolsForIsoId(isoId: Long): Flow<List<Spool>>

    @Query("SELECT * FROM spools WHERE isoNumber = :isoNumber ORDER BY spoolNumber ASC")
    fun getSpoolsForIsoNumber(isoNumber: String): Flow<List<Spool>>

    @Query("SELECT DISTINCT spoolNumber FROM spools WHERE isoNumber = :isoNumber ORDER BY spoolNumber ASC")
    fun getDistinctSpoolNumbersForIso(isoNumber: String): Flow<List<String>>

    @Query("SELECT * FROM spools WHERE isoNumber = :isoNumber AND spoolNumber = :spoolNumber LIMIT 1")
    suspend fun findSpool(isoNumber: String, spoolNumber: String): Spool?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spool: Spool): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(spools: List<Spool>)

    @Query("DELETE FROM spools")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM spools")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM spools")
    suspend fun getCountOnce(): Int
}
