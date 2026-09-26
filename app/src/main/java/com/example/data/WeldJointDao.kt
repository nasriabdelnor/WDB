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
}
