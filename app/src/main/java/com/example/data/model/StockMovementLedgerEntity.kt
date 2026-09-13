package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movement_ledger",
    indices = [
        Index(value = ["tag_serial_number"]),
        Index(value = ["proof_document_id"]),
        Index(value = ["executive_id"]),
        Index(value = ["movement_type"]),
        Index(value = ["created_at"])
    ]
)
data class StockMovementLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "tag_id")
    val tagId: Long,
    @ColumnInfo(name = "movement_type")
    val movementType: String,
    @ColumnInfo(name = "previous_location")
    val previousLocation: String?,
    @ColumnInfo(name = "new_location")
    val newLocation: String,
    @ColumnInfo(name = "executive_id")
    val executiveId: String?,
    @ColumnInfo(name = "subagent_id")
    val subagentId: String,
    @ColumnInfo(name = "subagent_name")
    val subagentName: String,
    @ColumnInfo(name = "tag_class")
    val tagClass: String,
    @ColumnInfo(name = "tag_serial_number")
    val tagSerialNumber: String,
    @ColumnInfo(name = "proof_document_id")
    val proofDocumentId: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_by")
    val createdBy: String = "ADMIN"
)
