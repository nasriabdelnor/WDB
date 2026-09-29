package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeldJointDao {

    @Query("SELECT * FROM welds ORDER BY id DESC")
    fun getAllWelds(): Flow<List<WeldJoint>>

    @Query("SELECT * FROM welds WHERE id = :id LIMIT 1")
    fun getWeldById(id: Long): Flow<WeldJoint?>

    @Query("SELECT * FROM welds WHERE id = :id LIMIT 1")
    suspend fun getWeldByIdOnce(id: Long): WeldJoint?

    @Query("""
        SELECT * FROM welds 
        WHERE jointNumber LIKE '%' || :query || '%' 
           OR isoNumber LIKE '%' || :query || '%' 
           OR spoolNumber LIKE '%' || :query || '%' 
           OR welder LIKE '%' || :query || '%' 
           OR welderName LIKE '%' || :query || '%' 
           OR part1HeatNumber LIKE '%' || :query || '%' 
           OR part2HeatNumber LIKE '%' || :query || '%' 
           OR drawingNo LIKE '%' || :query || '%'
        ORDER BY id DESC
    """)
    fun searchWelds(query: String): Flow<List<WeldJoint>>

    @Query("""
        SELECT * FROM welds 
        WHERE part1HeatNumber LIKE '%' || :heatNumber || '%' 
           OR part2HeatNumber LIKE '%' || :heatNumber || '%'
        ORDER BY isoNumber ASC, spoolNumber ASC, jointNumber ASC
    """)
    fun searchByHeatNumber(heatNumber: String): Flow<List<WeldJoint>>

    @Query("""
        SELECT * FROM welds 
        WHERE welder LIKE '%' || :welder || '%' 
           OR welderName LIKE '%' || :welder || '%'
        ORDER BY date DESC, id DESC
    """)
    fun searchByWelder(welder: String): Flow<List<WeldJoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeld(weld: WeldJoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(welds: List<WeldJoint>)

    @Update
    suspend fun updateWeld(weld: WeldJoint)

    @Delete
    suspend fun deleteWeld(weld: WeldJoint)

    @Query("DELETE FROM welds WHERE id = :id")
    suspend fun deleteWeldById(id: Long)

    @Query("DELETE FROM welds")
    suspend fun clearAllWelds()

    @Query("SELECT COUNT(*) FROM welds")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM welds")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT * FROM welds")
    suspend fun getAllWeldsSnapshot(): List<WeldJoint>

    // --- Cascade Filtering: ISO -> SPOOL -> JOINTS ---

    @Query("SELECT DISTINCT isoNumber FROM welds WHERE isoNumber != '' ORDER BY isoNumber ASC")
    fun getDistinctLines(): Flow<List<String>>

    @Query("SELECT DISTINCT spoolNumber FROM welds WHERE isoNumber = :isoNumber AND spoolNumber != '' ORDER BY spoolNumber ASC")
    fun getDistinctSpools(isoNumber: String): Flow<List<String>>

    @Query("SELECT * FROM welds WHERE isoNumber = :isoNumber ORDER BY spoolNumber ASC, jointNumber ASC")
    fun getWeldsByIsometric(isoNumber: String): Flow<List<WeldJoint>>

    @Query("SELECT * FROM welds WHERE isoNumber = :isoNumber AND spoolNumber = :spoolNumber ORDER BY jointNumber ASC")
    fun getWeldsByIsometricAndSpool(isoNumber: String, spoolNumber: String): Flow<List<WeldJoint>>

    @Query("""
        SELECT * FROM welds
        WHERE (:isoNumber IS NULL OR :isoNumber = '' OR :isoNumber = 'ALL' OR isoNumber = :isoNumber)
          AND (:spoolNumber IS NULL OR :spoolNumber = '' OR :spoolNumber = 'ALL' OR spoolNumber = :spoolNumber)
          AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR status = :status)
          AND (:welder IS NULL OR :welder = '' OR :welder = 'ALL' OR welder = :welder)
        ORDER BY isoNumber ASC, spoolNumber ASC, jointNumber ASC
    """)
    fun filterWelds(
        isoNumber: String? = null,
        spoolNumber: String? = null,
        status: String? = null,
        welder: String? = null
    ): Flow<List<WeldJoint>>

    // --- Duplicate Detection (Quality Control) ---

    @Query("""
        SELECT * FROM welds
        WHERE (isoNumber || '___' || spoolNumber || '___' || jointNumber) IN (
            SELECT (isoNumber || '___' || spoolNumber || '___' || jointNumber)
            FROM welds
            GROUP BY isoNumber, spoolNumber, jointNumber
            HAVING COUNT(*) > 1
        )
        ORDER BY isoNumber ASC, spoolNumber ASC, jointNumber ASC, id ASC
    """)
    fun getDuplicateWelds(): Flow<List<WeldJoint>>

    @Query("""
        SELECT * FROM welds
        WHERE (isoNumber || '___' || spoolNumber || '___' || jointNumber) IN (
            SELECT (isoNumber || '___' || spoolNumber || '___' || jointNumber)
            FROM welds
            GROUP BY isoNumber, spoolNumber, jointNumber
            HAVING COUNT(*) > 1
        )
        ORDER BY isoNumber ASC, spoolNumber ASC, jointNumber ASC, id ASC
    """)
    suspend fun getDuplicateWeldsSnapshot(): List<WeldJoint>

    @Query("""
        SELECT COUNT(*) FROM welds
        WHERE (isoNumber || '___' || spoolNumber || '___' || jointNumber) IN (
            SELECT (isoNumber || '___' || spoolNumber || '___' || jointNumber)
            FROM welds
            GROUP BY isoNumber, spoolNumber, jointNumber
            HAVING COUNT(*) > 1
        )
    """)
    fun getDuplicateCount(): Flow<Int>

    @Query("""
        SELECT * FROM welds 
        WHERE isoNumber = :isoNumber 
          AND spoolNumber = :spoolNumber 
          AND jointNumber = :jointNumber 
          AND id != :excludeId
    """)
    suspend fun findDuplicates(
        isoNumber: String,
        spoolNumber: String,
        jointNumber: String,
        excludeId: Long = 0
    ): List<WeldJoint>
}
