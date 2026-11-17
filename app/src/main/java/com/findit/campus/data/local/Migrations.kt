package com.findit.campus.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    const val ADD_COLOR = "ALTER TABLE items ADD COLUMN dominantColor TEXT"

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_COLOR)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE threads ADD COLUMN ownerChecked INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE threads ADD COLUMN guestChecked INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE items ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN hasCoordinates INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE items ADD COLUMN verificationSecret TEXT")
            db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, email TEXT NOT NULL, passwordHash TEXT NOT NULL, passwordSalt TEXT NOT NULL, campusName TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_email ON accounts(email)")
            db.execSQL("CREATE TABLE IF NOT EXISTS device_session (id INTEGER NOT NULL PRIMARY KEY, accountId INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, itemId INTEGER, `read` INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS threads (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemId INTEGER NOT NULL, ownerId INTEGER NOT NULL, guestId INTEGER NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_threads_itemId_guestId ON threads(itemId, guestId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, threadId INTEGER NOT NULL, senderId INTEGER NOT NULL, body TEXT NOT NULL, photoPath TEXT, createdAt INTEGER NOT NULL)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN remoteUid TEXT")
            db.execSQL("ALTER TABLE notifications ADD COLUMN threadId INTEGER")
            db.execSQL("ALTER TABLE items ADD COLUMN campusKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE items ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE threads ADD COLUMN ownerReturned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE threads ADD COLUMN guestReturned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE threads ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE threads ADD COLUMN guestUid TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (`key` TEXT NOT NULL PRIMARY KEY, accountUid TEXT NOT NULL, collection TEXT NOT NULL, documentId TEXT NOT NULL, payload TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("UPDATE items SET campusKey = COALESCE((SELECT lower(trim(campusName)) FROM accounts WHERE accounts.id = items.ownerId), '')")
            // A visual heuristic in the previous version was allowed to check a side automatically.
            // Preserve reports and chats but require a fresh, explicit ownership check.
            db.execSQL("UPDATE threads SET ownerChecked = 0, guestChecked = 0 WHERE status != 'RETURNED'")
        }
    }
}
