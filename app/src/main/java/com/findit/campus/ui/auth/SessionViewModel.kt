package com.findit.campus.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.Account
import com.findit.campus.data.AuthResult
import com.findit.campus.data.GoogleAuthResult
import com.findit.campus.data.GoogleProfileDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class SessionState(val loading: Boolean = true, val account: Account? = null)

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val accounts = getApplication<FindItApplication>().graph.accounts
    val busy = MutableStateFlow(false)
    val googleDraft = MutableStateFlow<GoogleProfileDraft?>(null)

    fun google(idToken: String, onResult: (String?) -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                when (val result = accounts.signInGoogle(idToken)) {
                    is GoogleAuthResult.SignedIn -> { googleDraft.value = null; onResult(null) }
                    is GoogleAuthResult.NeedsCampus -> { googleDraft.value = result.draft; onResult(null) }
                    is GoogleAuthResult.Rejected -> onResult(result.message)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onResult("Google sign-in could not be completed. Please try again.") }
            finally { busy.value = false }
        }
    }

    fun completeGoogle(name: String, campus: String, onResult: (String?) -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                when (val result = accounts.completeGoogleProfile(name, campus)) {
                    is AuthResult.SignedIn -> { googleDraft.value = null; onResult(null) }
                    is AuthResult.Rejected -> onResult(result.message)
                    AuthResult.Created -> Unit
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onResult("Your campus could not be saved. Please try again.") }
            finally { busy.value = false }
        }
    }

    fun cancelGoogle() {
        if (busy.value) return
        googleDraft.value = null
        accounts.cancelGoogleSetup()
    }
    // Resolve the saved Room session before deciding to display authentication.
    val state: StateFlow<SessionState> = accounts.observeAccount()
        .map { SessionState(loading = false, account = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState())

    fun register(
        name: String,
        email: String,
        password: String,
        confirm: String,
        campus: String,
        onResult: (String?) -> Unit,
    ) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { when (val result = accounts.register(name, email, password, confirm, campus)) {
                is AuthResult.Created -> onResult(null)
                is AuthResult.SignedIn -> onResult(null)
                is AuthResult.Rejected -> onResult(result.message)
            } } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onResult("Account could not be saved. Please try again.") }
            finally { busy.value = false }
        }
    }

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { when (val result = accounts.login(email, password)) {
                is AuthResult.SignedIn -> onResult(null)
                is AuthResult.Rejected -> onResult(result.message)
                AuthResult.Created -> onResult("Log in with the account you just created.")
            } } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onResult("Sign-in could not be completed. Please try again.") }
            finally { busy.value = false }
        }
    }

    fun resetPassword(email: String, onResult: (String) -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                val message = accounts.resetPassword(email)
                onResult(message ?: "Reset email requested. Check your inbox and spam folder.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onResult("Recovery email could not be requested. Please try again.") }
            finally { busy.value = false }
        }
    }

    fun connect(password: String, onResult: (String?) -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                val result = accounts.connectCurrentAccount(password)
                onResult((result as? AuthResult.Rejected)?.message)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onResult("Could not connect. Please try again.") }
            finally { busy.value = false }
        }
    }

    fun logout() {
        viewModelScope.launch { accounts.logout() }
    }
}
