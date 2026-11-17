package com.findit.campus.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["email"], unique = true)],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val campusName: String,
    val createdAt: Long,
    val remoteUid: String? = null,
)

@Entity(tableName = "device_session")
data class DeviceSessionEntity(
    @PrimaryKey val id: Int = 1,
    val accountId: Long,
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val body: String,
    val itemId: Long?,
    val read: Boolean,
    val createdAt: Long,
    val threadId: Long? = null,
)

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: DeviceSessionEntity)

    @Query("DELETE FROM device_session")
    suspend fun clearSession()

    @Query("SELECT * FROM device_session WHERE id = 1")
    fun observeSession(): Flow<DeviceSessionEntity?>

    @Insert
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeNotifications(accountId: Long): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET read = 1 WHERE accountId = :accountId AND read = 0")
    suspend fun markRead(accountId: Long)

    @Query("UPDATE notifications SET read = 1 WHERE accountId = :accountId AND threadId = :threadId")
    suspend fun markThreadRead(accountId: Long, threadId: Long)
}
