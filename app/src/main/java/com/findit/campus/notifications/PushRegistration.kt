package com.findit.campus.notifications

import com.findit.campus.data.AccountRepository
import com.findit.campus.data.FindItLog
import com.findit.campus.data.cloud.CloudServices
import com.findit.campus.data.cloud.awaitResult
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Tokens belong to the signed-in student, never to the whole campus. */
class PushRegistration(private val cloud: CloudServices, private val accounts: AccountRepository) {
    fun start(scope: CoroutineScope) {
        if (!cloud.configured) return
        scope.launch {
            accounts.observeAccount().distinctUntilChangedBy { it?.remoteUid }.collectLatest { account ->
                val uid = account?.remoteUid ?: return@collectLatest
                if (cloud.auth?.currentUser?.uid != uid) return@collectLatest
                try {
                    withTimeout(15_000) {
                        cloud.registerPushToken(uid, FirebaseMessaging.getInstance().token.awaitResult())
                    }
                } catch (timeout: TimeoutCancellationException) {
                    FindItLog.e("Push registration timed out", timeout)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { FindItLog.e("Push registration unavailable", error) }
            }
        }
    }
}
