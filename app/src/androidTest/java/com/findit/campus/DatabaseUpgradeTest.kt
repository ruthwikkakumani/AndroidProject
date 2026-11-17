package com.findit.campus

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.Migrations
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DatabaseUpgradeTest {
    @Test fun everyPublishedSchemaUpgradesWithoutLosingReportsOrChats() {
        for (version in 2..5) upgrade(version)
    }

    private fun upgrade(version: Int) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val schema = JSONObject(assets.open("com.findit.campus.data.local.FindItDatabase/$version.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (index in 0 until entities.length()) {
                        val e = entities.getJSONObject(index)
                        db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                        val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
                        for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql")
                            .replace("\${TABLE_NAME}", e.getString("tableName")))
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        try {
            helper.writableDatabase.apply {
                execSQL("INSERT INTO categories (id, name, symbol) VALUES (1, 'Bottles', 'bottle')")
                val ownerColumns = if (version >= 3) ", ownerId, hasCoordinates" else ""
                val ownerValues = if (version >= 3) ", 1, 1" else ""
                execSQL("INSERT INTO items (id, title, description, categoryId, type, status, latitude, longitude, placeLabel, zoneName, scannedText, contact, createdAt, isSample$ownerColumns) VALUES (7, 'Keep this report', 'Keep its description', 1, 'LOST', 'OPEN', 1.0, 2.0, 'Library', 'Library', '', '', 10, 0$ownerValues)")
                if (version >= 3) execSQL("INSERT INTO accounts (id, displayName, email, passwordHash, passwordSalt, campusName, createdAt) VALUES (1, 'Student', 'student@example.edu', 'hash', 'salt', 'City College', 1)")
                if (version >= 4) {
                    val flags = if (version == 5) ", ownerChecked, guestChecked" else ""
                    val values = if (version == 5) ", 1, 1" else ""
                    execSQL("INSERT INTO threads (id, itemId, ownerId, guestId, status, createdAt$flags) VALUES (8, 7, 1, 2, 'OPEN', 10$values)")
                    execSQL("INSERT INTO messages (id, threadId, senderId, body, createdAt) VALUES (9, 8, 2, 'Keep this message', 11)")
                }
            }
            helper.close()
            val db = Room.databaseBuilder(context, FindItDatabase::class.java, name)
                .addMigrations(Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5, Migrations.MIGRATION_5_6).build()
            try {
                db.openHelper.writableDatabase.query("SELECT title, campusKey FROM items WHERE id = 7").use {
                    assertTrue("Report retained from v$version", it.moveToFirst())
                    assertEquals("Keep this report", it.getString(0))
                    if (version >= 3) assertEquals("city college", it.getString(1))
                }
                if (version >= 4) db.openHelper.writableDatabase.query("SELECT body FROM messages WHERE id = 9").use {
                    assertTrue(it.moveToFirst()); assertEquals("Keep this message", it.getString(0))
                }
                db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM sync_queue").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
            } finally { db.close() }
        } finally { helper.close(); context.deleteDatabase(name) }
    }
}
