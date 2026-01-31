package com.loki.kidato

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFileScreen(
    vm: AuthViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    // DeKUT-ish palette
    val dekutGreen = Color(0xFF0B5D3B)
    val dekutGreenLight = Color(0xFFE6F2ED)
    val dekutGold = Color(0xFFF2B705)

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf("") }

    var title by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("") }

    // "past_paper" or "marking_scheme"
    var fileType by remember { mutableStateOf("past_paper") }

    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        pickedUri = uri
        pickedName = uri?.lastPathSegment ?: ""
    }

    val canUpload =
        pickedUri != null &&
                title.trim().isNotEmpty() &&
                courseCode.trim().isNotEmpty() &&
                !uploading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Description, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dekutGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = dekutGreenLight
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Share a paper with your classmates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = dekutGreen
                    )
                    Text(
                        text = "Upload past papers or marking schemes. Keep it clean and correctly labeled.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // File picker
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select file", fontWeight = FontWeight.SemiBold)

                    OutlinedButton(
                        onClick = { picker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, dekutGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (pickedUri == null) "Choose a file" else "Change file"
                        )
                    }

                    if (pickedUri != null) {
                        Text(
                            text = "Selected: ${pickedName.ifBlank { "file" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF3B3B3B)
                        )
                    }
                }
            }

            // Type selector
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Type", fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = fileType == "past_paper",
                            onClick = { fileType = "past_paper" },
                            label = { Text("Past Paper") }
                        )
                        FilterChip(
                            selected = fileType == "marking_scheme",
                            onClick = { fileType = "marking_scheme" },
                            label = { Text("Marking Scheme") }
                        )
                    }
                }
            }

            // Details form
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Details", fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g., CAT 2 2023)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it.uppercase() },
                        label = { Text("Course code (e.g., CCS3102)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        label = { Text("Unit name (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Upload action
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (uploading) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Uploading… $progress%", style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            error = null
                            val uri = pickedUri ?: run {
                                error = "Pick a file first."
                                return@Button
                            }

                            uploading = true
                            progress = 0

                            vm.uploadFileWithMeta(
                                uri = uri,
                                title = title.trim(),
                                type = fileType,
                                courseCode = courseCode.trim(),
                                unitName = unitName.trim(),
                                onProgress = { pct -> progress = pct },
                                onSuccess = {
                                    uploading = false
                                    onDone()
                                },
                                onError = { msg ->
                                    uploading = false
                                    error = msg
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canUpload,
                        colors = ButtonDefaults.buttonColors(containerColor = dekutGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Upload", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // little accent line
                    Divider(color = dekutGold.copy(alpha = 0.7f))
                    Text(
                        text = "Tip: Use correct course code so others can find it easily.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3B3B3B)
                    )
                }
            }
        }
    }
}
