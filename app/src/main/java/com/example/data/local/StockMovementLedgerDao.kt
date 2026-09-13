package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MovementWithProof
import com.example.data.model.StockMovementLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementLedgerDao {
    @Query("""
        SELECT m.*, p.original_filename as proofFilename, p.storage_object_key as proofStorageKey
        FROM stock_movement_ledger m
        LEFT JOIN proof_documents p ON m.proof_document_id = p.id
        ORDER BY m.created_at DESC
    """)
    fun getAllMovementsWithProof(): Flow<List<MovementWithProof>>

    @Query("""
        SELECT m.*, p.original_filename as proofFilename, p.storage_object_key as proofStorageKey
        FROM stock_movement_ledger m
        LEFT JOIN proof_documents p ON m.proof_document_id = p.id
        ORDER BY m.created_at DESC
        LIMIT :limit
    """)
    fun getRecentMovements(limit: Int = 30): Flow<List<MovementWithProof>>

    @Query("""
        SELECT m.*, p.original_filename as proofFilename, p.storage_object_key as proofStorageKey
        FROM stock_movement_ledger m
        LEFT JOIN proof_documents p ON m.proof_document_id = p.id
        WHERE m.tag_serial_number = :tagSerial
        ORDER BY m.created_at ASC
    """)
    fun getMovementsForTag(tagSerial: String): Flow<List<MovementWithProof>>

    @Query("""
        SELECT m.*, p.original_filename as proofFilename, p.storage_object_key as proofStorageKey
        FROM stock_movement_ledger m
        LEFT JOIN proof_documents p ON m.proof_document_id = p.id
        WHERE m.executive_id = :execId
        ORDER BY m.created_at DESC
    """)
    fun getMovementsForExecutive(execId: String): Flow<List<MovementWithProof>>

    @Query("SELECT COUNT(*) FROM stock_movement_ledger WHERE proof_document_id = :proofId")
    suspend fun countTagsForProofSync(proofId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovementLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovements(movements: List<StockMovementLedgerEntity>): List<Long>

    @Query("DELETE FROM stock_movement_ledger")
    suspend fun deleteAllMovements(): Int
}
