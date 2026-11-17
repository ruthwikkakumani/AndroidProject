package com.findit.campus.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Durable uploads survive process death and never require an online UI write. */
@Entity(tableName = "sync_queue")
data class PendingWrite(
    @PrimaryKey val key: String,
    val accountUid: String,
    val collection: String,
    val documentId: String,
    val payload: String,
    val createdAt: Long,
)

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(write: PendingWrite)

    @Query("SELECT * FROM sync_queue WHERE accountUid = :uid ORDER BY createdAt ASC")
    suspend fun pending(uid: String): List<PendingWrite>

    @Query("SELECT * FROM sync_queue WHERE accountUid = :uid ORDER BY createdAt ASC")
    fun observePending(uid: String): Flow<List<PendingWrite>>

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE `key` = :key")
    suspend fun hasPending(key: String): Int

    @Query("DELETE FROM sync_queue WHERE `key` = :key AND payload = :payload")
    suspend fun acknowledge(key: String, payload: String)
}
