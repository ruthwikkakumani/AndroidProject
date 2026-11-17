package com.findit.campus.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.findit.campus.BuildConfig
import com.findit.campus.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInButton(enabled: Boolean, onToken: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requesting by remember { mutableStateOf(false) }
    OutlinedButton({
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank() || BuildConfig.FIREBASE_PROJECT_ID.isBlank()) {
            onError("Google sign-in has not been connected yet. You can create an account with email and password.")
        } else scope.launch {
            requesting = true
            try {
                val request = GetCredentialRequest.Builder().addCredentialOption(
                    GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()).build()
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                    onToken(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                else onError("Google could not verify this account. Please try again.")
            } catch (cancelled: GetCredentialCancellationException) { /* The student dismissed the chooser. */ }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (missing: NoCredentialException) { onError("Add a Google account to this device, then try again.") }
            catch (error: Exception) { onError("Google sign-in is unavailable. Check your connection and account setup.") }
            finally { requesting = false }
        }
    }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled && !requesting) {
        Icon(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp), tint = Color.Unspecified)
        Spacer(Modifier.width(12.dp))
        Text(if (requesting) "Opening Google…" else "Continue with Google")
    }
}
