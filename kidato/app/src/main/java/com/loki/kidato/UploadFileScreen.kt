package com.loki.kidato

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun UploadFileScreen(
    vm: AuthViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("past_paper") } // past_paper | marking_scheme

    var progress by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Ready") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "Uploading..."
            progress = 0

            vm.uploadFileWithMeta(
                uri = uri,
                title = title.ifBlank { getFileName(context, uri) },
                type = type,
                courseCode = courseCode,
                unitName = unitName,
                onProgress = { progress = it },
                onSuccess = {
                    status = "Uploaded + saved to Firestore ✓"
                    onDone()
                },
                onError = { msg ->
                    status = "Error: $msg"
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Upload", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = courseCode,
            onValueChange = { courseCode = it },
            label = { Text("Course code (e.g. CCS3102)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = unitName,
            onValueChange = { unitName = it },
            label = { Text("Unit name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row {
            FilterChip(
                selected = type == "past_paper",
                onClick = { type = "past_paper" },
                label = { Text("Past paper") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = type == "marking_scheme",
                onClick = { type = "marking_scheme" },
                label = { Text("Marking scheme") }
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick file & Upload")
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(status)

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
