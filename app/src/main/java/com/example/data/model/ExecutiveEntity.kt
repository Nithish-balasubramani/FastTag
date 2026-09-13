package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "executives",
    indices = [
        Index(value = ["executive_id"], unique = true)
    ]
)
data class ExecutiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "executive_id")
    val executiveId: String,
    @ColumnInfo(name = "executive_name")
    val executiveName: String,
    @ColumnInfo(name = "region")
    val region: String = "Regional Hub",
    @ColumnInfo(name = "active")
    val active: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
