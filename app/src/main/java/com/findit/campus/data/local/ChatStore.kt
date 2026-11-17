package com.findit.campus.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "threads",
    indices = [Index(value = ["itemId", "guestId"], unique = true)],
)
data class ThreadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val ownerId: Long,
    val guestId: Long,
    val status: String,
    val createdAt: Long,
    val ownerChecked: Boolean = false,
    val guestChecked: Boolean = false,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long,
    val senderId: Long,
    val body: String,
    val photoPath: String?,
    val createdAt: Long,
)

@Dao
interface ChatDao {
    @Insert
    suspend fun insertThread(thread: ThreadEntity): Long

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM threads WHERE itemId = :itemId AND guestId = :guestId LIMIT 1")
    suspend fun findThread(itemId: Long, guestId: Long): ThreadEntity?

    @Query("SELECT * FROM threads WHERE id = :id")
    fun observeThread(id: Long): Flow<ThreadEntity?>

    @Query(
        """
        SELECT * FROM threads
        WHERE ownerId = :accountId OR guestId = :accountId
        ORDER BY createdAt DESC
        """,
    )
    fun observeThreads(accountId: Long): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun observeMessages(threadId: Long): Flow<List<MessageEntity>>

    @Query("UPDATE threads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE threads SET ownerChecked = :checked WHERE id = :id")
    suspend fun setOwnerChecked(id: Long, checked: Boolean)

    @Query("UPDATE threads SET guestChecked = :checked WHERE id = :id")
    suspend fun setGuestChecked(id: Long, checked: Boolean)

    @Query("SELECT * FROM threads WHERE id = :id")
    suspend fun getThread(id: Long): ThreadEntity?
}
