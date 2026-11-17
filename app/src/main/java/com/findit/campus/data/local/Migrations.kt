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
}
