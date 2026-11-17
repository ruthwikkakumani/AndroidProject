package com.findit.campus

import android.app.Application
import android.util.Log
import com.findit.campus.ai.InsightEngine
import com.findit.campus.ai.SmartMatchEngine
import com.findit.campus.ai.TextScanner
import com.findit.campus.data.FindItLog
import com.findit.campus.data.AccountRepository
import com.findit.campus.data.ChatRepository
import com.findit.campus.data.ItemRepository
import com.findit.campus.data.PhotoStore
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.location.LocationDataSource
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppGraph(context: Application) {
    val database: FindItDatabase = FindItDatabase.create(context)
    val matcher = SmartMatchEngine()
    val insights = InsightEngine()
    val repository = ItemRepository(database, matcher, insights)
    val accounts = AccountRepository(database)
    val chat = ChatRepository(database, repository)
    val photos = PhotoStore(context)
    val scanner = TextScanner()
    val location = LocationDataSource(context)
}

class FindItApplication : Application() {
    lateinit var graph: AppGraph
        private set

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            FindItLog.e("Background task failed", error)
        },
    )

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Log.e("FindIt", "Uncaught on ${thread.name}", error)
            previous?.uncaughtException(thread, error)
        }
        graph = AppGraph(this)
        scope.launch {
            runCatching { graph.repository.ensureCategories() }
                .onFailure { FindItLog.e("Could not prepare the campus board", it) }
        }
        FindItLog.i("FindIt ready")
    }
}
