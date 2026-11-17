package com.findit.campus

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.location.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class WalkingTrailStorageTest {
    @Test fun trailPersistsAcrossDatabaseReopenAndClearIsAccountScoped() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "trail-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, FindItDatabase::class.java, name).build()
        try {
            var repository = WalkingTrailRepository(db.trailDao())
            val fix = DeviceFix(13.0, 80.0, 5f)
            repository.append(1, "first", fix)
            repository.append(2, "second", fix)
            db.close()
            db = Room.databaseBuilder(context, FindItDatabase::class.java, name).build()
            repository = WalkingTrailRepository(db.trailDao())
            assertEquals(1, repository.observe(1).first().size)
            assertEquals(1L, repository.observe(1).first().single().accountId)
            repository.clear(1)
            assertTrue(repository.observe(1).first().isEmpty())
            assertEquals(1, repository.observe(2).first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
