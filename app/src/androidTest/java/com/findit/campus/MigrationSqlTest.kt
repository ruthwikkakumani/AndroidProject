package com.findit.campus

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.findit.campus.data.local.Migrations
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationSqlTest {
    @Test
    fun colorColumnIsAdded() {
        val database = SQLiteDatabase.create(null)
        database.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY NOT NULL, title TEXT NOT NULL)")
        database.execSQL(Migrations.ADD_COLOR)
        val names = mutableListOf<String>()
        database.rawQuery("PRAGMA table_info(items)", null).use { cursor ->
            while (cursor.moveToNext()) names += cursor.getString(1)
        }
        database.close()
        assertTrue(names.contains("dominantColor"))
    }
}
