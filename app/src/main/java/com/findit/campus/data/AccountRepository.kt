package com.findit.campus.data

import com.findit.campus.auth.PasswordHasher
import com.findit.campus.auth.isValidEmail
import com.findit.campus.auth.validateNewAccount
import com.findit.campus.data.local.AccountEntity
import com.findit.campus.data.local.DeviceSessionEntity
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.NotificationEntity
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class Account(
    val id: Long,
    val displayName: String,
    val email: String,
    val campusName: String,
)

sealed interface AuthResult {
    data class SignedIn(val account: Account) : AuthResult
    data class Rejected(val message: String) : AuthResult
}

class AccountRepository(private val database: FindItDatabase) {
    private val accounts = database.accountDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAccount(): Flow<Account?> = accounts.observeSession().flatMapLatest { session ->
        if (session == null) {
            flowOf(null)
        } else {
            accounts.observeById(session.accountId).map { entity -> entity?.toAccount() }
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        confirm: String,
        campus: String,
    ): AuthResult {
        validateNewAccount(name, email, password, confirm, campus)?.let { return AuthResult.Rejected(it) }
        val normalized = email.trim().lowercase()
        if (accounts.findByEmail(normalized) != null) {
            return AuthResult.Rejected("An account with that email already exists.")
        }
        val salt = PasswordHasher.salt()
        val id = accounts.insert(
            AccountEntity(
                displayName = name.trim(),
                email = normalized,
                passwordHash = PasswordHasher.hash(password, salt),
                passwordSalt = Base64.getEncoder().encodeToString(salt),
                campusName = campus.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        accounts.saveSession(DeviceSessionEntity(accountId = id))
        val created = accounts.findByEmail(normalized)!!.toAccount()
        return AuthResult.SignedIn(created)
    }

    suspend fun login(email: String, password: String): AuthResult {
        if (!isValidEmail(email) || password.isEmpty()) {
            return AuthResult.Rejected("Enter the email and password for your account.")
        }
        val entity = accounts.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Rejected("Those details do not match an account.")
        if (!PasswordHasher.verify(password, entity.passwordSalt, entity.passwordHash)) {
            return AuthResult.Rejected("Those details do not match an account.")
        }
        accounts.saveSession(DeviceSessionEntity(accountId = entity.id))
        return AuthResult.SignedIn(entity.toAccount())
    }

    suspend fun logout() {
        accounts.clearSession()
    }

    fun observeNotifications(accountId: Long): Flow<List<NotificationEntity>> =
        accounts.observeNotifications(accountId)

    suspend fun markNotificationsRead(accountId: Long) {
        accounts.markRead(accountId)
    }
}

private fun AccountEntity.toAccount() = Account(
    id = id,
    displayName = displayName,
    email = email,
    campusName = campusName,
)
