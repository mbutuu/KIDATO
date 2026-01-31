package com.loki.kidato

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ApiException



@Composable
fun RegisterScreenSimple(
    onRegister: (email: String, password: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onRegister(email.trim(), password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Back to login")
        }
    }
}

@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onAuthed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    var isRegister by remember { mutableStateOf(false) }

    // ---------------------------
    // GOOGLE SIGN-IN SETUP
    // ---------------------------
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) vm.signInWithGoogle(idToken)
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    val isAuthed by vm.authState.collectAsState()

    LaunchedEffect(isAuthed) {
        if (isAuthed) onAuthed()
    }

    // ---------------------------
    // YOUR ORIGINAL SWITCH
    // ---------------------------
    if (!isRegister) {
        // ✅ LOGIN SCREEN
        LoginScreenFancy(
            onLogin = { email, password ->
                vm.login(email, password) // must exist
            },
            onGoogle = {
                googleLauncher.launch(googleClient.signInIntent)
            },
            onRegister = {
                isRegister = true
            },
            onForgotPassword = {
                // if you have resetPassword(email), call it here
                // vm.resetPassword(email)
            }
        )
    } else {
        // ✅ YOUR EXISTING ELSE BLOCK (UNCHANGED)
        RegisterScreenSimple(
            onRegister = { email, password ->
                vm.register(email, password)
            },
            onBackToLogin = {
                isRegister = false
            }
        )
    }
}





