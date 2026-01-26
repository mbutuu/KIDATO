package com.loki.kidato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loki.kidato.ui.theme.KidatoTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: AuthViewModel = viewModel()
            var showUpload by remember { mutableStateOf(false) }

            val isAuthed by vm.authState.collectAsState()
            val profileDone by vm.profileCompleted.collectAsState()

            KidatoTheme {
                when {
                    // 1️⃣ Not logged in
                    !isAuthed -> {
                        AuthScreen(
                            vm = vm,
                            onAuthed = {}
                        )
                    }

                    // 2️⃣ Logged in but profile not completed
                    !profileDone -> {
                        vm.observeProfile()

                        ProfileSetupScreen(
                            onSave = { name, regNo, school, course, year ->
                                vm.saveProfile(name, regNo, school, course, year)
                            },
                            onSkip = {
                                vm.saveProfile("", "", "", "", "")
                            }
                        )
                    }

                    // 3️⃣ Logged in + profile completed → HOME
                    else -> {
                        if (showUpload) {
                            UploadFileScreen(
                                vm = vm,
                                onDone = { showUpload = false }
                            )
                        } else {
                            HomeScreen(
                                vm = vm,
                                onUpload = { showUpload = true },
                                onLogout = { vm.logout(application) }
                            )
                        }

                    }
                }
            }
        }
    }
}
