package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "proof_documents",
    indices = [
        Index(value = ["proof_type"]),
        Index(value = ["uploaded_at"])
    ]
)
data class ProofDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "proof_type")
    val proofType: String,
    @ColumnInfo(name = "storage_provider")
    val storageProvider: String = "LOCAL_SECURE_STORAGE",
    @ColumnInfo(name = "storage_object_key")
    val storageObjectKey: String,
    @ColumnInfo(name = "original_filename")
    val originalFilename: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String = "image/jpeg",
    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,
    @ColumnInfo(name = "uploaded_at")
    val uploadedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "uploaded_by")
    val uploadedBy: String = "ADMIN",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
