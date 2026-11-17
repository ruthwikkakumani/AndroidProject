package com.findit.campus.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findit.campus.FindItApplication
import com.findit.campus.data.Account
import com.findit.campus.data.AuthResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val accounts = getApplication<FindItApplication>().graph.accounts
    val account: StateFlow<Account?> = accounts.observeAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun register(
        name: String,
        email: String,
        password: String,
        confirm: String,
        campus: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            when (val result = accounts.register(name, email, password, confirm, campus)) {
                is AuthResult.Created -> onResult(null)
                is AuthResult.SignedIn -> onResult(null)
                is AuthResult.Rejected -> onResult(result.message)
            }
        }
    }

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = accounts.login(email, password)) {
                is AuthResult.SignedIn -> onResult(null)
                is AuthResult.Rejected -> onResult(result.message)
                AuthResult.Created -> onResult("Log in with the account you just created.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { accounts.logout() }
    }
}
