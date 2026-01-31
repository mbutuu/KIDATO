package com.loki.kidato

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AuthViewModel,
    onUpload: () -> Unit,
    onLogout: () -> Unit
) {
    val filesVm: FilesViewModel = viewModel()
    val files by filesVm.files.collectAsState()
    val error by filesVm.error.collectAsState()

    // Drawer header data from VM
    val profileName by vm.profileName.collectAsState()
    val role by vm.role.collectAsState()

    // DeKUT-ish palette
    val dekutGreen = Color(0xFF0B5D3B)
    val dekutGold = Color(0xFFF2B705)

    val context = LocalContext.current
    fun openUrl(url: String) {
        if (url.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // start realtime listener
        filesVm.startListening()

        // ensure profile observer is active (so name/role updates)
        vm.observeProfile()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Drawer header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = (profileName ?: "User"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Divider()

                // Logout item (kept inside drawer)
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    icon = { Icon(Icons.Filled.Logout, contentDescription = "Logout") },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Kidato Papers",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = dekutGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },

            // Upload becomes a plus FAB (conspicuous but not screaming)
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onUpload,
                    containerColor = dekutGold,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Upload")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {

                // Error message
                if (error != null) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }

                if (files.isEmpty()) {
                    Text("No files yet. Tap + to upload.")
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
                                    .clickable { openUrl(f.downloadUrl) },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        f.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        "${f.courseCode} • ${
                                            if (f.type == "marking_scheme") "Marking scheme" else "Past paper"
                                        }",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    if (f.unitName.isNotBlank()) {
                                        Text(f.unitName, style = MaterialTheme.typography.bodySmall)
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    val timeText = f.uploadedAt?.let { df.format(Date(it)) } ?: "Unknown time"
                                    Text(
                                        "Uploaded by ${f.uploadedByName.ifBlank { "Unknown" }} • $timeText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = { openUrl(f.downloadUrl) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = dekutGreen
                                        )
                                    ) {
                                        Text("Open / Download")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
