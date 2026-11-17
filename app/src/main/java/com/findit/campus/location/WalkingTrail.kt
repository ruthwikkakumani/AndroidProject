package com.findit.campus.location

import com.findit.campus.data.local.TrailDao
import com.findit.campus.data.local.TrailPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TrailPolicy {
    const val MAX_POINTS = 5_000
    const val MAX_ACCURACY_METERS = 60f
    const val MIN_MOVEMENT_METERS = 5.0
    const val MAX_SPEED_METERS_PER_SECOND = 15.0
    const val MAX_FIX_AGE_MILLIS = 20_000L
}

fun acceptsTrailFix(previous: TrailPoint?, segmentId: String, fix: DeviceFix, now: Long): Boolean {
    if (!fix.latitude.isFinite() || !fix.longitude.isFinite() ||
        fix.latitude !in -90.0..90.0 || fix.longitude !in -180.0..180.0 ||
        !fix.accuracyMeters.isFinite() || fix.accuracyMeters <= 0f ||
        fix.accuracyMeters > TrailPolicy.MAX_ACCURACY_METERS ||
        fix.capturedAtMillis > now || now - fix.capturedAtMillis > TrailPolicy.MAX_FIX_AGE_MILLIS) return false
    if (previous == null) return true
    if (fix.capturedAtMillis <= previous.capturedAt) return false
    if (previous.segmentId != segmentId) return true
    val distance = distanceMeters(previous.latitude, previous.longitude, fix.latitude, fix.longitude)
    val seconds = (fix.capturedAtMillis - previous.capturedAt) / 1_000.0
    return distance >= TrailPolicy.MIN_MOVEMENT_METERS && distance / seconds <= TrailPolicy.MAX_SPEED_METERS_PER_SECOND
}

fun trailDistanceMeters(points: List<TrailPoint>): Double = points.zipWithNext().sumOf { (a, b) ->
    if (a.segmentId == b.segmentId) distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) else 0.0
}

class WalkingTrailRepository(private val dao: TrailDao) {
    private val mutex = Mutex()
    fun observe(accountId: Long): Flow<List<TrailPoint>> = dao.observe(accountId)

    /** False only when storage is full; noisy or stale GPS fixes are quietly ignored. */
    suspend fun append(accountId: Long, segmentId: String, fix: DeviceFix): Boolean = mutex.withLock {
        require(accountId > 0)
        if (dao.count(accountId) >= TrailPolicy.MAX_POINTS) return@withLock false
        if (acceptsTrailFix(dao.last(accountId), segmentId, fix, System.currentTimeMillis())) {
            dao.insert(TrailPoint(accountId = accountId, segmentId = segmentId,
                latitude = fix.latitude, longitude = fix.longitude, accuracyMeters = fix.accuracyMeters,
                capturedAt = fix.capturedAtMillis))
        }
        true
    }

    suspend fun clear(accountId: Long) = mutex.withLock { dao.clear(accountId) }
}
