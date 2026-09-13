package com.example.data.model

import androidx.room.ColumnInfo

data class ClassStockSummary(
    val tagClass: String,
    val centralCount: Int,
    val masterCount: Int,
    val executiveCount: Int,
    val assignedCount: Int,
    val totalCount: Int
)

data class ExecutiveStockSummary(
    val executiveId: String,
    val executiveName: String,
    val region: String,
    val active: Boolean,
    val currentStock: Int
)

data class ClassCountRaw(
    val tag_class: String,
    val current_location: String,
    val count: Int
)

data class ExecutiveClassCountRaw(
    val tag_class: String,
    val count: Int
)

data class OverallStockMetrics(
    val centralStock: Int = 0,
    val masterStock: Int = 0,
    val executiveStock: Int = 0,
    val assignedStock: Int = 0,
    val totalActive: Int = 0
)

data class MovementWithProof(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
    @ColumnInfo(name = "movement_type") val movementType: String,
    @ColumnInfo(name = "previous_location") val previousLocation: String?,
    @ColumnInfo(name = "new_location") val newLocation: String,
    @ColumnInfo(name = "executive_id") val executiveId: String?,
    @ColumnInfo(name = "subagent_id") val subagentId: String,
    @ColumnInfo(name = "subagent_name") val subagentName: String,
    @ColumnInfo(name = "tag_class") val tagClass: String,
    @ColumnInfo(name = "tag_serial_number") val tagSerialNumber: String,
    @ColumnInfo(name = "proof_document_id") val proofDocumentId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "proofFilename") val proofFilename: String?,
    @ColumnInfo(name = "proofStorageKey") val proofStorageKey: String?
)
