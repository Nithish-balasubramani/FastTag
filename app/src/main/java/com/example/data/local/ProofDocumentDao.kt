package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ProofDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProofDocumentDao {
    @Query("SELECT * FROM proof_documents ORDER BY uploaded_at DESC")
    fun getAllProofs(): Flow<List<ProofDocumentEntity>>

    @Query("SELECT * FROM proof_documents WHERE proof_type = :proofType ORDER BY uploaded_at DESC")
    fun getProofsByType(proofType: String): Flow<List<ProofDocumentEntity>>

    @Query("SELECT * FROM proof_documents WHERE id = :id LIMIT 1")
    fun getProofById(id: Long): Flow<ProofDocumentEntity?>

    @Query("SELECT * FROM proof_documents WHERE id = :id LIMIT 1")
    suspend fun getProofByIdSync(id: Long): ProofDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProof(proof: ProofDocumentEntity): Long

    @Query("SELECT COUNT(*) FROM proof_documents")
    fun countProofs(): Flow<Int>

    @Query("DELETE FROM proof_documents")
    suspend fun deleteAllProofs(): Int
}
