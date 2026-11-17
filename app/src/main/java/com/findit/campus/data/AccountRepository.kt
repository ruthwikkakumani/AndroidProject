package com.findit.campus.data

import com.findit.campus.auth.PasswordHasher
import com.findit.campus.auth.isValidEmail
import com.findit.campus.auth.validateNewAccount
import com.findit.campus.data.local.AccountEntity
import com.findit.campus.data.local.DeviceSessionEntity
import com.findit.campus.data.local.FindItDatabase
import com.findit.campus.data.local.NotificationEntity
import com.findit.campus.data.cloud.CloudServices
import com.findit.campus.data.cloud.awaitResult
import com.findit.campus.data.cloud.campusKey
import com.findit.campus.data.cloud.stableId
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.Locale
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
    val remoteUid: String? = null,
)

sealed interface AuthResult {
    data object Created : AuthResult
    data class SignedIn(val account: Account) : AuthResult
    data class Rejected(val message: String) : AuthResult
}

data class GoogleProfileDraft(val name: String, val email: String)
sealed interface GoogleAuthResult {
    data class SignedIn(val account: Account) : GoogleAuthResult
    data class NeedsCampus(val draft: GoogleProfileDraft) : GoogleAuthResult
    data class Rejected(val message: String) : GoogleAuthResult
}

class AccountRepository(private val database: FindItDatabase, private val cloud: CloudServices) {
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
        val normalized = email.trim().lowercase(Locale.ROOT)
        if (cloud.configured) {
            return try {
                val user = cloud.auth!!.createUserWithEmailAndPassword(normalized, password).awaitResult().user
                    ?: return AuthResult.Rejected("Account creation could not be completed.")
                val id = stableId(user.uid)
                cloud.store!!.collection("profiles").document(user.uid).set(mapOf(
                    "uid" to user.uid,
                    "localId" to id, "displayName" to name.trim(), "campusName" to campus.trim(),
                    "campusKey" to campusKey(campus),
                )).awaitResult()
                storeCredentials(id, name.trim(), normalized, password, campus.trim(), user.uid)
                cloud.auth.signOut()
                AuthResult.Created
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                FindItLog.e("Cloud registration failed", error)
                AuthResult.Rejected("Account could not be created. Check your connection and Firebase setup; this email may already be registered.")
            }
        }
        if (accounts.findByEmail(normalized) != null) {
            return AuthResult.Rejected("An account with that email already exists.")
        }
        val salt = PasswordHasher.salt()
        val id = accounts.insert(
            AccountEntity(
                displayName = name.trim(),
                email = normalized,
                passwordHash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) },
                passwordSalt = Base64.getEncoder().encodeToString(salt),
                campusName = campus.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        return if (id > 0) AuthResult.Created else AuthResult.Rejected("The account could not be created.")
    }

    suspend fun login(email: String, password: String): AuthResult {
        if (!isValidEmail(email) || password.isEmpty()) {
            return AuthResult.Rejected("Enter the email and password for your account.")
        }
        if (cloud.configured) {
            try {
                val user = cloud.auth!!.signInWithEmailAndPassword(email.trim(), password).awaitResult().user
                    ?: return AuthResult.Rejected("Sign-in could not be completed.")
                val profile = cloud.store!!.collection("profiles").document(user.uid).get().awaitResult()
                if (!profile.exists()) return AuthResult.Rejected("Your campus profile is missing. Please finish account setup.")
                val id = stableId(user.uid)
                storeCredentials(id, profile.getString("displayName").orEmpty(), email.trim().lowercase(),
                    password, profile.getString("campusName").orEmpty(), user.uid)
                accounts.saveSession(DeviceSessionEntity(accountId = id))
                return AuthResult.SignedIn(accounts.getById(id)!!.toAccount())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: FirebaseNetworkException) {
                // Only a previously successful login can unlock a locally saved credential offline.
                FindItLog.i("Using saved credentials while offline")
            } catch (error: Exception) {
                FindItLog.e("Cloud sign-in failed", error)
                return AuthResult.Rejected("Sign-in failed. Check your email, password, connection, and Firebase setup.")
            }
        }
        val entity = accounts.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Rejected("Those details do not match an account.")
        if (entity.passwordHash.isBlank()) return AuthResult.Rejected("Use Google sign-in for this account.")
        if (!withContext(Dispatchers.Default) { PasswordHasher.verify(password, entity.passwordSalt, entity.passwordHash) }) {
            return AuthResult.Rejected("Those details do not match an account.")
        }
        accounts.saveSession(DeviceSessionEntity(accountId = entity.id))
        return AuthResult.SignedIn(entity.toAccount())
    }

    suspend fun logout() {
        accounts.clearSession()
        cloud.auth?.signOut()
    }

    suspend fun signInGoogle(idToken: String): GoogleAuthResult {
        if (!cloud.configured) return GoogleAuthResult.Rejected("Google sign-in needs campus account services to be configured.")
        if (idToken.isBlank()) return GoogleAuthResult.Rejected("Google could not verify your account. Please try again.")
        return try {
            val user = cloud.auth!!.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitResult().user
                ?: return GoogleAuthResult.Rejected("Google sign-in could not be completed.")
            val email = user.email.orEmpty().trim().lowercase(Locale.ROOT)
            if (!isValidEmail(email)) return GoogleAuthResult.Rejected("Your Google account needs an email address.")
            val local = accounts.findByEmail(email)
            if (local != null && local.remoteUid != user.uid) {
                cloud.auth.signOut()
                return GoogleAuthResult.Rejected("This email belongs to a local demo account. Use its password, or choose a different Google account.")
            }
            val profile = cloud.store!!.collection("profiles").document(user.uid).get().awaitResult()
            if (!profile.exists()) GoogleAuthResult.NeedsCampus(GoogleProfileDraft(user.displayName.orEmpty(), email))
            else GoogleAuthResult.SignedIn(saveGoogleAccount(user.uid, email, profile))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            FindItLog.e("Google sign-in failed", error)
            GoogleAuthResult.Rejected("Google sign-in failed. Check your connection and Firebase Google provider setup.")
        }
    }

    suspend fun completeGoogleProfile(name: String, campus: String): AuthResult {
        if (name.trim().length !in 2..80) return AuthResult.Rejected("Enter your full name (2–80 characters).")
        if (campus.trim().length !in 2..80) return AuthResult.Rejected("Choose or enter your college (2–80 characters).")
        val user = cloud.auth?.currentUser
            ?: return AuthResult.Rejected("Please sign in with Google again.")
        if (user.providerData.none { it.providerId == GoogleAuthProvider.PROVIDER_ID })
            return AuthResult.Rejected("Please sign in with Google again.")
        return try {
            val reference = cloud.store!!.collection("profiles").document(user.uid)
            var profile = reference.get().awaitResult()
            if (!profile.exists()) {
                reference.set(mapOf("uid" to user.uid, "localId" to stableId(user.uid),
                    "displayName" to name.trim(), "campusName" to campus.trim(), "campusKey" to campusKey(campus))).awaitResult()
                profile = reference.get().awaitResult()
            }
            AuthResult.SignedIn(saveGoogleAccount(user.uid, user.email.orEmpty().trim().lowercase(Locale.ROOT), profile))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            FindItLog.e("Google campus setup failed", error)
            AuthResult.Rejected("Your campus could not be saved. Check your connection and try again.")
        }
    }

    fun cancelGoogleSetup() { cloud.auth?.signOut() }

    private suspend fun saveGoogleAccount(uid: String, email: String, profile: DocumentSnapshot): Account {
        val id = stableId(uid)
        val existing = accounts.getById(id)
        // OAuth tokens are never persisted in Room or used as passwords.
        val entity = AccountEntity(id, profile.getString("displayName").orEmpty(), email,
            existing?.passwordHash.orEmpty(), existing?.passwordSalt.orEmpty(),
            profile.getString("campusName").orEmpty(), existing?.createdAt ?: System.currentTimeMillis(), uid)
        accounts.upsert(entity)
        accounts.saveSession(DeviceSessionEntity(accountId = id))
        return entity.toAccount()
    }

    suspend fun resetPassword(email: String): String? {
        if (!isValidEmail(email)) return "Enter a valid email address."
        if (!cloud.configured) return "Email recovery needs campus account services to be connected."
        return try {
            cloud.auth!!.sendPasswordResetEmail(email.trim()).awaitResult()
            null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { "Recovery email could not be sent. Please check your connection." }
    }

    private suspend fun storeCredentials(id: Long, name: String, email: String, password: String, campus: String, uid: String) {
        val salt = PasswordHasher.salt()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) }
        accounts.upsert(AccountEntity(id, name, email, hash, Base64.getEncoder().encodeToString(salt), campus,
            System.currentTimeMillis(), uid))
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
    remoteUid = remoteUid,
)
