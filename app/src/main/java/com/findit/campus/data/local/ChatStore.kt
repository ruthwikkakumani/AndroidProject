package com.findit.campus.data.local

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

data class ConversationSummary(
    val id: Long,
    val itemId: Long,
    val title: String,
    val personName: String,
    val photoPath: String?,
    val preview: String?,
    val updatedAt: Long,
    val unread: Int,
    val ownerChecked: Boolean,
    val guestChecked: Boolean,
    val ownerReturned: Boolean,
    val guestReturned: Boolean,
)

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
    @ColumnInfo(defaultValue = "0") val ownerReturned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val guestReturned: Boolean = false,
    @ColumnInfo(defaultValue = "''") val ownerUid: String = "",
    @ColumnInfo(defaultValue = "''") val guestUid: String = "",
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
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessage(id: Long): MessageEntity?

    @Query("""
        SELECT t.id, t.itemId, COALESCE(i.title, 'Report removed') AS title,
        COALESCE(a.displayName, 'Campus student') AS personName, i.photoPath,
        (SELECT body FROM messages WHERE threadId = t.id ORDER BY createdAt DESC, id DESC LIMIT 1) AS preview,
        COALESCE((SELECT MAX(createdAt) FROM messages WHERE threadId = t.id), t.createdAt) AS updatedAt,
        (SELECT COUNT(*) FROM notifications WHERE threadId = t.id AND accountId = :accountId AND read = 0) AS unread,
        t.ownerChecked, t.guestChecked, t.ownerReturned, t.guestReturned
        FROM threads t LEFT JOIN items i ON t.itemId = i.id
        LEFT JOIN accounts a ON a.id = CASE WHEN t.ownerId = :accountId THEN t.guestId ELSE t.ownerId END
        WHERE t.ownerId = :accountId OR t.guestId = :accountId ORDER BY updatedAt DESC
    """)
    fun observeConversations(accountId: Long): Flow<List<ConversationSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: ThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: MessageEntity)

    @Query("UPDATE threads SET ownerReturned = :returned WHERE id = :id")
    suspend fun setOwnerReturned(id: Long, returned: Boolean)

    @Query("UPDATE threads SET guestReturned = :returned WHERE id = :id")
    suspend fun setGuestReturned(id: Long, returned: Boolean)

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
