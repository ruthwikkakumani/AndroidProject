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
}
