package com.findit.campus.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findit.campus.R
import com.findit.campus.ui.components.*

@Composable
fun WelcomeScreen(onCreate: () -> Unit, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Brand()
        Spacer(Modifier.height(16.dp))
        Illustration(R.drawable.art_welcome, Modifier.fillMaxWidth().height(250.dp))
        Text("Your things.\nBack where they belong.", style = MaterialTheme.typography.headlineLarge)
        Text("Your campus lost and found, made simple. Report an item, connect with a student, and check it together before returning it.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Button(onCreate, Modifier.fillMaxWidth().height(54.dp).testTag("welcome_create")) { Text("Create account") }
        OutlinedButton(onLogin, Modifier.fillMaxWidth().height(52.dp).testTag("welcome_login")) { Text("I already have an account") }
        TrustNote("Private conversations. Thoughtful checks. Happier reunions.")
    }
}

@Composable
private fun AccountLayout(title: String, body: String, onBack: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(modifier, topBar = { PageTopBar("FindIt Campus", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            IllustratedHeading(title, body, R.drawable.art_welcome)
            FormCard(content = content)
            TrustNote("Use the same campus name as your classmates to share the same board.")
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasswordInput(value: String, onChange: (String) -> Unit, label: String, tag: String) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth().testTag(tag), label = { Text(label) }, singleLine = true,
        shape = MaterialTheme.shapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = { visible = !visible }) {
            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "Hide password" else "Show password")
        } })
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onSubmit: (String, String, String, String, String) -> Unit,
    onLogin: () -> Unit, error: String?, modifier: Modifier = Modifier, busy: Boolean = false) {
    var name by rememberSaveable { mutableStateOf("") }; var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }; var confirm by rememberSaveable { mutableStateOf("") }
    var campus by rememberSaveable { mutableStateOf("") }
    AccountLayout("Make yourself\nat home.", "Join the students bringing things back together.", onBack, modifier) {
        Text("Create account", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth().testTag("register_name"), label = { Text("Full name") }, singleLine = true, shape = MaterialTheme.shapes.small)
        OutlinedTextField(email, { email = it.take(254) }, Modifier.fillMaxWidth().testTag("register_email"), label = { Text("Email address") }, singleLine = true, shape = MaterialTheme.shapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        PasswordInput(password, { password = it }, "Password", "register_password")
        Text("At least 8 characters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PasswordInput(confirm, { confirm = it }, "Confirm password", "register_confirm")
        OutlinedTextField(campus, { campus = it.take(80) }, Modifier.fillMaxWidth().testTag("register_campus"), label = { Text("Campus name") }, singleLine = true, shape = MaterialTheme.shapes.small)
        error?.let { Notice(it, Modifier.testTag("register_error"), true) }
        Button({ onSubmit(name, email, password, confirm, campus) }, Modifier.fillMaxWidth().height(52.dp).testTag("register_submit"), enabled = !busy) {
            Text(if (busy) "Creating account…" else "Create account")
        }
        TextButton(onLogin, Modifier.fillMaxWidth().testTag("register_go_login"), enabled = !busy) { Text("Already a member? Log in") }
    }
}

@Composable
fun LoginScreen(onBack: () -> Unit, onSubmit: (String, String) -> Unit, onCreate: () -> Unit, onForgot: () -> Unit,
    error: String?, notice: String? = null, modifier: Modifier = Modifier, busy: Boolean = false) {
    var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }
    AccountLayout("Good to see\nyou again.", "Your campus is ready to help. Let's find what you're missing.", onBack, modifier) {
        Text("Log in", style = MaterialTheme.typography.titleLarge)
        notice?.let { Notice(it) }
        OutlinedTextField(email, { email = it.take(254) }, Modifier.fillMaxWidth().testTag("login_email"), label = { Text("Email address") }, singleLine = true,
            shape = MaterialTheme.shapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        PasswordInput(password, { password = it }, "Password", "login_password")
        TextButton(onForgot, Modifier.align(Alignment.End).testTag("login_forgot")) { Text("Forgot password?") }
        error?.let { Notice(it, Modifier.testTag("login_error"), true) }
        Button({ onSubmit(email, password) }, Modifier.fillMaxWidth().height(52.dp).testTag("login_submit"), enabled = !busy) { Text(if (busy) "Logging in…" else "Log in") }
        TextButton(onCreate, Modifier.fillMaxWidth().testTag("login_go_register"), enabled = !busy) { Text("New here? Create account") }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val model: SessionViewModel = viewModel()
    val busy by model.busy.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }; var notice by rememberSaveable { mutableStateOf<String?>(null) }
    AccountLayout("Let's get\nyou back in.", "Enter your account email to request a password reset.", onBack, modifier) {
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        notice?.let { Notice(it) }
        Button({ model.resetPassword(email) { notice = it } }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy && email.isNotBlank()) { Text(if (busy) "Sending…" else "Send reset email") }
    }
}
