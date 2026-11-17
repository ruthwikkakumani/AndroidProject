package com.findit.campus

import android.app.Application
import android.util.Log
import com.findit.campus.ai.InsightEngine
import com.findit.campus.ai.SmartMatchEngine
import com.findit.campus.ai.TextScanner
import com.findit.campus.ai.PhotoCheckEngine
import com.findit.campus.data.cloud.CloudServices
import com.findit.campus.data.cloud.CloudSync
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
    val cloud = CloudServices(context)
    val sync = CloudSync(cloud, database)
    val accounts = AccountRepository(database, cloud)
    val repository = ItemRepository(database, matcher, insights, accounts, sync)
    val photos = PhotoStore(context, cloud)
    val scanner = TextScanner()
    val checker = PhotoCheckEngine(scanner)
    val chat = ChatRepository(database, repository, photos, checker, sync)
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
        graph.sync.start(scope, graph.accounts)
        scope.launch {
            runCatching { graph.repository.ensureCategories() }
                .onFailure { FindItLog.e("Could not prepare the campus board", it) }
        }
        FindItLog.i("FindIt ready")
    }
}
