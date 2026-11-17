package com.findit.campus.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Private to this device and account; never included in cloud synchronization. */
@Entity(tableName = "walking_trail", indices = [Index("accountId")])
data class TrailPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val segmentId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAt: Long,
)

@Dao
interface TrailDao {
    @Query("SELECT * FROM walking_trail WHERE accountId = :accountId ORDER BY capturedAt, id")
    fun observe(accountId: Long): Flow<List<TrailPoint>>

    @Query("SELECT * FROM walking_trail WHERE accountId = :accountId ORDER BY capturedAt DESC, id DESC LIMIT 1")
    suspend fun last(accountId: Long): TrailPoint?

    @Query("SELECT COUNT(*) FROM walking_trail WHERE accountId = :accountId")
    suspend fun count(accountId: Long): Int

    @Insert
    suspend fun insert(point: TrailPoint)

    @Query("DELETE FROM walking_trail WHERE accountId = :accountId")
    suspend fun clear(accountId: Long)
}
