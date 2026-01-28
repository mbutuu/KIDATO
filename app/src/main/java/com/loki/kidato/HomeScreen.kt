package com.loki.kidato

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    vm: AuthViewModel,
    onUpload: () -> Unit,
    onLogout: () -> Unit
) {
    val filesVm: FilesViewModel = viewModel()
    val files by filesVm.files.collectAsState()
    val error by filesVm.error.collectAsState()

    LaunchedEffect(Unit) {
        filesVm.startListening()

        // DEBUG: one-time fetch to confirm we can read Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("files")
            .get()
            .addOnSuccessListener { snap ->
                android.util.Log.d("FILES_DEBUG", "files count = ${snap.size()}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FILES_DEBUG", "read failed", e)
            }
    }


    val context = LocalContext.current
    fun openUrl(url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Kidato Papers", style = MaterialTheme.typography.headlineMedium)

            Row {
                Button(onClick = { onUpload() }) { Text("Upload") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onLogout) { Text("Logout") }
            }
        }


        Spacer(Modifier.height(8.dp))

        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        if (files.isEmpty()) {
            Text("No files yet. Upload something.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(files) { f ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { openUrl(f.downloadUrl) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(f.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))

                            Text(
                                "${f.courseCode} • ${if (f.type == "marking_scheme") "Marking scheme" else "Past paper"}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (f.unitName.isNotBlank()) {
                                Text(f.unitName, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(6.dp))

                            val timeText = f.uploadedAt?.let { df.format(Date(it)) } ?: "Unknown time"
                            Text(
                                "Uploaded by ${f.uploadedByName.ifBlank { "Unknown" }} • $timeText",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(onClick = { openUrl(f.downloadUrl) }) {
                                Text("Open / Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
