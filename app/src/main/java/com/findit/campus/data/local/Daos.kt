package com.findit.campus.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.findit.campus.model.ItemStatus
import kotlinx.coroutines.flow.Flow

data class ItemWithCategory(
    @Embedded val item: ItemEntity,
    val categoryName: String,
)

data class CategoryWithItems(
    @Embedded val category: CategoryEntity,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val items: List<ItemEntity>,
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT name FROM categories ORDER BY name")
    fun observeNames(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeWithItems(): Flow<List<CategoryWithItems>>
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET status = :status, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ItemStatus, resolvedAt: Long?)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT items.*, categories.name AS categoryName
        FROM items INNER JOIN categories ON items.categoryId = categories.id
        ORDER BY items.createdAt DESC
        """,
    )
    fun observeAll(): Flow<List<ItemWithCategory>>

    @Query(
        """
        SELECT items.*, categories.name AS categoryName
        FROM items INNER JOIN categories ON items.categoryId = categories.id
        WHERE items.ownerId = :ownerId
        ORDER BY items.createdAt DESC
        """,
    )
    fun observeMine(ownerId: Long): Flow<List<ItemWithCategory>>

    @Query(
        """
        SELECT items.*, categories.name AS categoryName
        FROM items INNER JOIN categories ON items.categoryId = categories.id
        WHERE items.id = :id
        """,
    )
    fun observeById(id: Long): Flow<ItemWithCategory?>

    @Query("SELECT verificationSecret FROM items WHERE id = :id")
    suspend fun verificationSecret(id: Long): String?

    @Query(
        """
        SELECT items.*, categories.name AS categoryName
        FROM items INNER JOIN categories ON items.categoryId = categories.id
        """,
    )
    suspend fun listAll(): List<ItemWithCategory>

    @Query(
        """
        SELECT items.*, categories.name AS categoryName
        FROM items INNER JOIN categories ON items.categoryId = categories.id
        WHERE (
            :query = ''
            OR items.title LIKE '%' || :query || '%' ESCAPE '\'
            OR items.description LIKE '%' || :query || '%' ESCAPE '\'
            OR items.zoneName LIKE '%' || :query || '%' ESCAPE '\'
            OR items.placeLabel LIKE '%' || :query || '%' ESCAPE '\'
            OR items.scannedText LIKE '%' || :query || '%' ESCAPE '\'
            OR categories.name LIKE '%' || :query || '%' ESCAPE '\'
        )
        ORDER BY items.createdAt DESC
        """,
    )
    fun observeSearch(query: String): Flow<List<ItemWithCategory>>
}
