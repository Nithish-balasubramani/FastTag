package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClassCountRaw
import com.example.data.model.ExecutiveClassCountRaw
import com.example.data.model.InventoryTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryTagDao {
    @Query("SELECT * FROM inventory_tags ORDER BY updated_at DESC")
    fun getAllTags(): Flow<List<InventoryTagEntity>>

    @Query("SELECT COUNT(*) FROM inventory_tags WHERE current_location = :location")
    fun countByLocation(location: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM inventory_tags")
    fun countTotal(): Flow<Int>

    @Query("SELECT tag_class, current_location, COUNT(*) as count FROM inventory_tags GROUP BY tag_class, current_location")
    fun getClassCounts(): Flow<List<ClassCountRaw>>

    @Query("SELECT tag_class, COUNT(*) as count FROM inventory_tags WHERE current_executive_id = :execId AND current_location = 'EXECUTIVE' GROUP BY tag_class")
    fun getExecutiveClassCounts(execId: String): Flow<List<ExecutiveClassCountRaw>>

    @Query("SELECT COUNT(*) FROM inventory_tags WHERE current_executive_id = :execId AND current_location = 'EXECUTIVE'")
    fun countExecutiveStock(execId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM inventory_tags WHERE current_executive_id = :execId AND current_location = 'EXECUTIVE'")
    suspend fun countExecutiveStockSync(execId: String): Int

    @Query("SELECT * FROM inventory_tags WHERE current_executive_id = :execId AND current_location = 'EXECUTIVE' ORDER BY tag_serial_number ASC")
    fun getTagsByExecutive(execId: String): Flow<List<InventoryTagEntity>>

    @Query("SELECT * FROM inventory_tags WHERE tag_serial_number = :serial LIMIT 1")
    fun getTagBySerial(serial: String): Flow<InventoryTagEntity?>

    @Query("SELECT * FROM inventory_tags WHERE tag_serial_number = :serial LIMIT 1")
    suspend fun getTagBySerialSync(serial: String): InventoryTagEntity?

    @Query("SELECT * FROM inventory_tags WHERE tag_serial_number IN (:serials)")
    suspend fun getTagsBySerialsSync(serials: List<String>): List<InventoryTagEntity>

    @Query("SELECT tag_serial_number FROM inventory_tags WHERE tag_serial_number IN (:serials)")
    suspend fun getExistingSerials(serials: List<String>): List<String>

    @Query("""
        SELECT * FROM inventory_tags 
        WHERE (:query = '' OR tag_serial_number LIKE '%' || :query || '%' OR subagent_name LIKE '%' || :query || '%' OR subagent_id LIKE '%' || :query || '%')
        AND (:location = '' OR current_location = :location)
        AND (:tagClass = '' OR tag_class = :tagClass)
        AND (:execId = '' OR current_executive_id = :execId)
        ORDER BY updated_at DESC
    """)
    fun searchTags(
        query: String,
        location: String,
        tagClass: String,
        execId: String
    ): Flow<List<InventoryTagEntity>>

    @Query("SELECT DISTINCT tag_class FROM inventory_tags ORDER BY tag_class ASC")
    fun getAllDiscoveredClasses(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: InventoryTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTags(tags: List<InventoryTagEntity>): List<Long>

    @Update
    suspend fun updateTag(tag: InventoryTagEntity): Int

    @Update
    suspend fun updateTags(tags: List<InventoryTagEntity>): Int

    @Query("DELETE FROM inventory_tags")
    suspend fun deleteAllTags(): Int
}
