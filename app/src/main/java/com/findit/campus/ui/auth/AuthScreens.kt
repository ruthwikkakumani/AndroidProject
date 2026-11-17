package com.findit.campus.ui.auth

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(onCreate: () -> Unit, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FindIt Campus", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text(
            "Report lost and found items for the campus you choose. Nothing here is tied to a preset city.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().testTag("welcome_create")) {
            Text("Create account")
        }
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().testTag("welcome_login")) {
            Text("Log in")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSubmit: (name: String, email: String, password: String, confirm: String, campus: String) -> Unit,
    onLogin: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var campus by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create account") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 20.dp).imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Join FindIt Campus", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().testTag("register_name"), label = { Text("Full name") }, singleLine = true)
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().testTag("register_email"), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(
                password,
                { password = it },
                Modifier.fillMaxWidth().testTag("register_password"),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "Toggle password visibility")
                    }
                },
            )
            OutlinedTextField(
                confirm,
                { confirm = it },
                Modifier.fillMaxWidth().testTag("register_confirm"),
                label = { Text("Confirm password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            OutlinedTextField(campus, { campus = it }, Modifier.fillMaxWidth().testTag("register_campus"), label = { Text("Campus name") }, supportingText = { Text("The school or campus this account belongs to.") }, singleLine = true)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("register_error"))
            Button(
                onClick = { onSubmit(name, email, password, confirm, campus) },
                modifier = Modifier.fillMaxWidth().testTag("register_submit"),
            ) { Text("Create account") }
            TextButton(onClick = onLogin, modifier = Modifier.testTag("register_go_login")) { Text("Log in") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    onCreate: () -> Unit,
    onForgot: () -> Unit,
    error: String?,
    notice: String? = null,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Log in") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 20.dp).imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("FindIt Campus", style = MaterialTheme.typography.labelLarge)
            Text("Welcome back", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            if (notice != null) {
                Text(notice, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().testTag("login_email"), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(
                password,
                { password = it },
                Modifier.fillMaxWidth().testTag("login_password"),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "Toggle password visibility")
                    }
                },
            )
            TextButton(onClick = onForgot, modifier = Modifier.testTag("login_forgot")) { Text("Forgot password") }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("login_error"))
            Button(onClick = { onSubmit(email, password) }, modifier = Modifier.fillMaxWidth().testTag("login_submit")) { Text("Log in") }
            TextButton(onClick = onCreate, modifier = Modifier.testTag("login_go_register")) { Text("Create account") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Forgot password") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Password email is not configured for this install, so a reset link cannot be sent. Sign in with the password you created on this device.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun ProfileScreen(name: String, email: String, campus: String, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(name, style = MaterialTheme.typography.titleLarge)
        Text(email)
        Text(campus)
        Button(onClick = onLogout, modifier = Modifier.testTag("logout")) { Text("Log out") }
    }
}
