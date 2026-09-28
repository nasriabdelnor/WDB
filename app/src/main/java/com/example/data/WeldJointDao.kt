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
        WHERE jointNo LIKE '%' || :query || '%' 
           OR lineNo LIKE '%' || :query || '%' 
           OR welderId LIKE '%' || :query || '%' 
           OR welderName LIKE '%' || :query || '%' 
           OR spoolNo LIKE '%' || :query || '%' 
           OR drawingNo LIKE '%' || :query || '%'
        ORDER BY id DESC
    """)
    fun searchWelds(query: String): Flow<List<WeldJoint>>

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

    @Query("SELECT * FROM welds")
    suspend fun getAllWeldsSnapshot(): List<WeldJoint>

    // --- Search & Multi-parameter Filtering ---

    @Query("""
        SELECT * FROM welds
        WHERE (:lineNo IS NULL OR :lineNo = '' OR :lineNo = 'ALL' OR lineNo = :lineNo)
          AND (:spoolNo IS NULL OR :spoolNo = '' OR :spoolNo = 'ALL' OR spoolNo = :spoolNo)
          AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR status = :status)
          AND (:welderId IS NULL OR :welderId = '' OR :welderId = 'ALL' OR welderId = :welderId)
        ORDER BY lineNo ASC, spoolNo ASC, jointNo ASC
    """)
    fun filterWelds(
        lineNo: String? = null,
        spoolNo: String? = null,
        status: String? = null,
        welderId: String? = null
    ): Flow<List<WeldJoint>>

    @Query("SELECT * FROM welds WHERE status = :status ORDER BY id DESC")
    fun getWeldsByStatus(status: String): Flow<List<WeldJoint>>

    @Query("SELECT * FROM welds WHERE welderId = :welderId ORDER BY id DESC")
    fun getWeldsByWelder(welderId: String): Flow<List<WeldJoint>>

    // --- Isometric Data & Hierarchy Queries ---

    @Query("SELECT DISTINCT lineNo FROM welds WHERE lineNo != '' ORDER BY lineNo ASC")
    fun getDistinctLines(): Flow<List<String>>

    @Query("SELECT DISTINCT spoolNo FROM welds WHERE lineNo = :lineNo AND spoolNo != '' ORDER BY spoolNo ASC")
    fun getDistinctSpools(lineNo: String): Flow<List<String>>

    @Query("SELECT * FROM welds WHERE lineNo = :lineNo ORDER BY spoolNo ASC, jointNo ASC")
    fun getWeldsByIsometric(lineNo: String): Flow<List<WeldJoint>>

    @Query("SELECT * FROM welds WHERE lineNo = :lineNo AND spoolNo = :spoolNo ORDER BY jointNo ASC")
    fun getWeldsByIsometricAndSpool(lineNo: String, spoolNo: String): Flow<List<WeldJoint>>

    // --- Duplicate Detection Queries (App Metadata: Détection des doublons WDB) ---

    /**
     * Reactive stream of duplicate welds (records where lineNo, spoolNo, jointNo appears more than once)
     */
    @Query("""
        SELECT * FROM welds
        WHERE (lineNo || '___' || spoolNo || '___' || jointNo) IN (
            SELECT (lineNo || '___' || spoolNo || '___' || jointNo)
            FROM welds
            GROUP BY lineNo, spoolNo, jointNo
            HAVING COUNT(*) > 1
        )
        ORDER BY lineNo ASC, spoolNo ASC, jointNo ASC, id ASC
    """)
    fun getDuplicateWelds(): Flow<List<WeldJoint>>

    /**
     * Snapshot of all duplicate records for offline reports and exports
     */
    @Query("""
        SELECT * FROM welds
        WHERE (lineNo || '___' || spoolNo || '___' || jointNo) IN (
            SELECT (lineNo || '___' || spoolNo || '___' || jointNo)
            FROM welds
            GROUP BY lineNo, spoolNo, jointNo
            HAVING COUNT(*) > 1
        )
        ORDER BY lineNo ASC, spoolNo ASC, jointNo ASC, id ASC
    """)
    suspend fun getDuplicateWeldsSnapshot(): List<WeldJoint>

    /**
     * Reactive count of duplicate welds in the database
     */
    @Query("""
        SELECT COUNT(*) FROM welds
        WHERE (lineNo || '___' || spoolNo || '___' || jointNo) IN (
            SELECT (lineNo || '___' || spoolNo || '___' || jointNo)
            FROM welds
            GROUP BY lineNo, spoolNo, jointNo
            HAVING COUNT(*) > 1
        )
    """)
    fun getDuplicateCount(): Flow<Int>

    /**
     * Check if a specific weld joint number already exists for a line and spool
     */
    @Query("""
        SELECT * FROM welds 
        WHERE lineNo = :lineNo 
          AND spoolNo = :spoolNo 
          AND jointNo = :jointNo 
          AND id != :excludeId
    """)
    suspend fun findDuplicates(
        lineNo: String,
        spoolNo: String,
        jointNo: String,
        excludeId: Long = 0
    ): List<WeldJoint>
}
