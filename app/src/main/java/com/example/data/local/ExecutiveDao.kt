package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExecutiveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutiveDao {
    @Query("SELECT * FROM executives ORDER BY executive_id ASC")
    fun getAllExecutives(): Flow<List<ExecutiveEntity>>

    @Query("SELECT * FROM executives WHERE active = 1 ORDER BY executive_id ASC")
    fun getActiveExecutives(): Flow<List<ExecutiveEntity>>

    @Query("SELECT * FROM executives WHERE executive_id = :execId LIMIT 1")
    fun getExecutiveByExecId(execId: String): Flow<ExecutiveEntity?>

    @Query("SELECT * FROM executives WHERE executive_id = :execId LIMIT 1")
    suspend fun getExecutiveByExecIdSync(execId: String): ExecutiveEntity?

    @Query("SELECT COUNT(*) FROM executives")
    suspend fun countExecutives(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExecutive(executive: ExecutiveEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(executives: List<ExecutiveEntity>)

    @Update
    suspend fun updateExecutive(executive: ExecutiveEntity): Int

    @Query("UPDATE executives SET active = :active, updated_at = :updatedAt WHERE executive_id = :execId")
    suspend fun setExecutiveActive(execId: String, active: Boolean, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE executives SET executive_name = :name, region = :region, active = :active, updated_at = :updatedAt WHERE executive_id = :execId")
    suspend fun updateExecutiveDetails(execId: String, name: String, region: String, active: Boolean, updatedAt: Long = System.currentTimeMillis()): Int
}
