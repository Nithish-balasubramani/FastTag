package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_tags",
    indices = [
        Index(value = ["tag_serial_number"], unique = true),
        Index(value = ["tag_class"]),
        Index(value = ["current_location"]),
        Index(value = ["current_executive_id"]),
        Index(value = ["subagent_id"])
    ]
)
data class InventoryTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "tag_serial_number")
    val tagSerialNumber: String,
    @ColumnInfo(name = "tag_class")
    val tagClass: String,
    @ColumnInfo(name = "subagent_id")
    val subagentId: String,
    @ColumnInfo(name = "subagent_name")
    val subagentName: String,
    @ColumnInfo(name = "current_location")
    val currentLocation: String, // CENTRAL, MASTER, EXECUTIVE, ASSIGNED
    @ColumnInfo(name = "current_executive_id")
    val currentExecutiveId: String? = null,
    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // ACTIVE or ASSIGNED
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
