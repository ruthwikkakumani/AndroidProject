package com.findit.campus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ItemEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        DeviceSessionEntity::class,
        NotificationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FindItDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun categoryDao(): CategoryDao

    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var instance: FindItDatabase? = null

        fun create(context: Context): FindItDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FindItDatabase::class.java,
                    "campus-board.db",
                ).addMigrations(Migrations.MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
