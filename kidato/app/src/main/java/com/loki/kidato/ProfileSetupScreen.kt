package com.loki.kidato

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSetupScreen(
    onSave: (name: String, regNo: String, school: String, course: String, year: String) -> Unit,
    onSkip: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Complete your profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(name, { name = it }, label = { Text("Full Name") })
        OutlinedTextField(regNo, { regNo = it }, label = { Text("Registration Number") })
        OutlinedTextField(school, { school = it }, label = { Text("School / Faculty") })
        OutlinedTextField(course, { course = it }, label = { Text("Course") })
        OutlinedTextField(year, { year = it }, label = { Text("Year of Study") })

        Spacer(Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSave(name, regNo, school, course, year) }
        ) {
            Text("Save Profile")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSkip
        ) {
            Text("Skip for now")
        }
    }
}
