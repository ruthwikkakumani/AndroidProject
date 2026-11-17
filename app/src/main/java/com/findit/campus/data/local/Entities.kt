package com.findit.campus.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val symbol: String,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("status"),
        Index("createdAt"),
    ],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val categoryId: Long,
    val type: ItemType,
    val status: ItemStatus,
    val latitude: Double,
    val longitude: Double,
    val placeLabel: String,
    val zoneName: String,
    val photoPath: String?,
    val scannedText: String,
    val dominantColor: String?,
    val contact: String,
    val createdAt: Long,
    val resolvedAt: Long?,
    val isSample: Boolean,
    val ownerId: Long = 0,
    val hasCoordinates: Boolean = true,
    val verificationSecret: String? = null,
    @ColumnInfo(defaultValue = "''") val campusKey: String = "",
    @ColumnInfo(defaultValue = "''") val ownerUid: String = "",
)
