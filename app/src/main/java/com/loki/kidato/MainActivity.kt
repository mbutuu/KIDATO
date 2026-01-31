package com.loki.kidato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loki.kidato.ui.theme.KidatoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: AuthViewModel = viewModel()
            val catalogVm: CatalogViewModel = viewModel()

            var showUpload by remember { mutableStateOf(false) }

            val isAuthed by vm.authState.collectAsState()
            val profileDone by vm.profileCompleted.collectAsState()

            val schools by catalogVm.schools.collectAsState()
            val courses by catalogVm.courses.collectAsState()
            val catalogError by catalogVm.error.collectAsState()

            // Load schools once when app starts
            LaunchedEffect(Unit) {
                catalogVm.loadSchools()
            }

            KidatoTheme {
                when {
                    // 1) Not logged in
                    !isAuthed -> {
                        AuthScreen(vm = vm, onAuthed = {})
                    }

                    // 2) Logged in but profile not completed
                    !profileDone -> {
                        // Ensure we’re listening for profile changes
                        LaunchedEffect(Unit) {
                            vm.observeProfile()
                        }

                        // Optional: show catalog errors (helps during setup)
                        if (catalogError != null) {
                            // you can also show a snackbar; keep it simple for now
                            androidx.compose.material3.Text("Catalog error: $catalogError")
                        }

                        ProfileSetupScreen(
                            schools = schools,
                            courses = courses,
                            onSchoolSelected = { schoolId ->
                                catalogVm.loadCourses(schoolId)
                            },
                            onSave = { name, regNo, schoolId, courseId, year, semester, semesterKey, completed ->
                                // IMPORTANT: this must save to Firestore using the NEW fields
                                vm.saveProfileV2(
                                    name = name,
                                    regNo = regNo,
                                    schoolId = schoolId,
                                    courseId = courseId,
                                    year = year,
                                    semester = semester,
                                    semesterKey = semesterKey,
                                    profileCompleted = completed
                                )
                            },

                        )
                    }

                    // 3) Logged in + profile completed → HOME
                    else -> {
                        if (showUpload) {
                            UploadFileScreen(
                                vm = vm,
                                onDone = { showUpload = false },
                                onBack = { showUpload = false } // optional
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

private fun MainActivity.UploadFileScreen(vm: AuthViewModel, onDone: () -> Unit) {}

private fun AuthViewModel.saveProfileV2(
    name: String,
    regNo: String,
    schoolId: String,
    courseId: String,
    year: Int,
    semester: Int,
    semesterKey: String,
    profileCompleted: Boolean
) {
}
