package com.fiesta.filevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FileVaultApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVaultApp() {
    val context = LocalContext.current
    val store = remember { VaultStore(context) }
    val cloud = remember { SupabaseCloud(context) }
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf(store.items()) }
    var folders by remember { mutableStateOf(store.folders()) }
    var activeFolder by remember { mutableStateOf("All files") }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<VaultItem?>(null) }
    var previewText by remember { mutableStateOf<String?>(null) }
    var showAccount by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signUp by remember { mutableStateOf(false) }
    var exportItem by remember { mutableStateOf<VaultItem?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching { withContext(Dispatchers.IO) { store.import(uri, if (activeFolder == "All files") "General" else activeFolder) } }
                .onSuccess { files = store.items(); folders = store.folders(); message = "Encrypted and saved on this device." }
                .onFailure { message = it.message ?: "Import failed." }
            busy = false
        }
    }
    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val item = exportItem
        if (uri != null && item != null) scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val temp = File(context.cacheDir, "export-" + item.id)
                    try {
                        store.export(item, temp)
                        context.contentResolver.openOutputStream(uri)?.use { output -> temp.inputStream().use { it.copyTo(output) } }
                            ?: error("Could not write to the selected destination.")
                    } finally { temp.delete() }
                }
            }.onSuccess { message = "File exported." }.onFailure { message = it.message ?: "Export failed." }
            busy = false
            exportItem = null
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF91B8FF), secondary = Color(0xFF9ADBCB),
        background = Color(0xFF0B1020), surface = Color(0xFF151C2D),
        surfaceVariant = Color(0xFF202A3D)
    )) {
        Scaffold(containerColor = Color(0xFF0B1020), topBar = {
            TopAppBar(
                title = { Column {
                    Text("FileVault", fontWeight = FontWeight.Bold)
                    Text("PRIVATE STORAGE", fontSize = 10.sp, color = Color(0xFF9AA8C0), letterSpacing = 2.sp)
                } },
                actions = { IconButton(onClick = { showAccount = true }) { Icon(Icons.Default.Lock, "Cloud account") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1020), titleContentColor = Color.White)
            )
        }, floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(arrayOf("*/*")) }, containerColor = Color(0xFF91B8FF), contentColor = Color(0xFF10182A)) {
                Icon(Icons.Default.Add, "Add file")
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your private space", color = Color(0xFFB7C4DA), fontSize = 13.sp)
                            Text(files.size.toString() + " files secured", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("AES-GCM encrypted on this device", color = Color(0xFF9ADBCB), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF9ADBCB), modifier = Modifier.size(42.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search your vault") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF91B8FF), unfocusedBorderColor = Color(0xFF344158),
                        focusedContainerColor = Color(0xFF151C2D), unfocusedContainerColor = Color(0xFF151C2D)
                    ))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Folders", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showFolderDialog = true }) { Text("+ New folder") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("All files") + folders).distinct().forEach { folder ->
                        FilterChip(selected = activeFolder == folder, onClick = { activeFolder = folder },
                            label = { Text(folder) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (activeFolder == "All files") "My Vault" else activeFolder, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(files.size.toString() + " items", color = Color(0xFF9AA8C0), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                val filtered = files.filter {
                    (activeFolder == "All files" || it.folder == activeFolder) &&
                    (query.isBlank() || it.name.contains(query, ignoreCase = true))
                }
                if (filtered.isEmpty()) {
                    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Folder, null, tint = Color(0xFF71819D), modifier = Modifier.size(54.dp))
                        Text(if (query.isBlank()) "This folder is empty" else "No matching files", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Tap + to securely import a file", color = Color(0xFF9AA8C0), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        items(filtered, key = { it.id }) { item ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2D)), shape = RoundedCornerShape(18.dp)) {
                                Row(Modifier.fillMaxWidth().clickable {
                                    selected = item
                                    previewText = null
                                    if (item.mimeType.startsWith("text/") || item.name.endsWith(".json", true) || item.name.endsWith(".xml", true)) {
                                        scope.launch {
                                            previewText = runCatching {
                                                withContext(Dispatchers.IO) {
                                                    val temp = File(context.cacheDir, "preview-" + item.id)
                                                    try { store.export(item, temp); temp.readText().take(12000) } finally { temp.delete() }
                                                }
                                            }.getOrElse { "Preview unavailable: " + (it.message ?: "error") }
                                        }
                                    }
                                }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).background(Color(0xFF253653), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Folder, null, tint = Color(0xFF91B8FF))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(item.folder + " • " + formatBytes(item.size) + " • encrypted", color = Color(0xFF9AA8C0), fontSize = 11.sp, maxLines = 1)
                                    }
                                    IconButton(onClick = {
                                        scope.launch { busy = true; runCatching { withContext(Dispatchers.IO) { cloud.upload(item) } }.onSuccess { message = "Encrypted cloud backup uploaded." }.onFailure { message = it.message ?: "Upload failed." }; busy = false }
                                    }) { Icon(Icons.Default.CloudUpload, "Back up to cloud", tint = Color(0xFF9ADBCB)) }
                                    IconButton(onClick = { store.delete(item); files = store.items(); folders = store.folders(); message = "File removed from vault." }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFE59A9A))
                                    }
                                }
                            }
                        }
                    }
                }
                if (message.isNotBlank()) Text(message, color = Color(0xFF9ADBCB), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (busy) AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("Please wait") }, text = { LinearProgressIndicator(Modifier.fillMaxWidth()) })
        }

        if (selected != null) {
            val item = selected!!
            AlertDialog(onDismissRequest = { selected = null; previewText = null },
                title = { Text(item.name) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Folder: " + item.folder + "\nType: " + item.mimeType + "\nEncrypted size: " + formatBytes(item.size),
                            color = Color(0xFFCBD5E5))
                        if (previewText != null) Surface(color = Color(0xFF101827), shape = RoundedCornerShape(12.dp)) {
                            Text(previewText!!, color = Color(0xFFCBD5E5), modifier = Modifier.padding(10.dp), fontSize = 11.sp)
                        } else if (item.mimeType.startsWith("text/") || item.name.endsWith(".json", true) || item.name.endsWith(".xml", true)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else Text("Use Export to open this file in a compatible app.", color = Color(0xFFCBD5E5))
                    }
                },
                confirmButton = { TextButton(onClick = { exportItem = item; savePicker.launch(item.name); selected = null }) { Icon(Icons.Default.SaveAlt, null); Spacer(Modifier.width(4.dp)); Text("Export") } },
                dismissButton = { TextButton(onClick = { selected = null; previewText = null }) { Text("Close") } },
                containerColor = Color(0xFF192338), titleContentColor = Color.White)
        }
        if (showFolderDialog) {
            AlertDialog(onDismissRequest = { showFolderDialog = false },
                title = { Text("Create folder") },
                text = { OutlinedTextField(folderName, { folderName = it }, label = { Text("Folder name") }, singleLine = true) },
                confirmButton = { TextButton(onClick = {
                    runCatching { store.createFolder(folderName) }
                        .onSuccess { folders = store.folders(); activeFolder = folderName.trim(); folderName = ""; showFolderDialog = false }
                        .onFailure { message = it.message ?: "Could not create folder." }
                }) { Text("Create") } },
                dismissButton = { TextButton(onClick = { showFolderDialog = false }) { Text("Cancel") } },
                containerColor = Color(0xFF192338), titleContentColor = Color.White)
        }
        if (showAccount) {
            AlertDialog(onDismissRequest = { showAccount = false },
                title = { Text(if (cloud.signedIn) "Cloud account" else "Connect cloud storage") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!cloud.configured) Text("Set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties, then rebuild.", color = Color(0xFFCBD5E5))
                        if (cloud.signedIn) Text("Signed in. Cloud backups upload encrypted files only.", color = Color(0xFFCBD5E5))
                        else {
                            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
                            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true)
                            TextButton(onClick = { signUp = !signUp }) { Text(if (signUp) "Have an account? Sign in" else "Create an account") }
                        }
                    }
                },
                confirmButton = { TextButton(enabled = cloud.configured, onClick = {
                    if (cloud.signedIn) { cloud.signOut(); message = "Signed out."; showAccount = false }
                    else scope.launch {
                        busy = true
                        runCatching { withContext(Dispatchers.IO) { if (signUp) cloud.signUp(email.trim(), password) else cloud.signIn(email.trim(), password) } }
                            .onSuccess { message = "Cloud request completed. If email confirmation is enabled, verify your email and sign in."; showAccount = false }
                            .onFailure { message = it.message ?: "Authentication failed." }
                        busy = false
                    }
                }) { Text(if (cloud.signedIn) "Sign out" else if (signUp) "Create account" else "Sign in") } },
                dismissButton = { TextButton(onClick = { showAccount = false }) { Text("Cancel") } },
                containerColor = Color(0xFF192338), titleContentColor = Color.White)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return DecimalFormat("#,##0.0").format(kb) + " KB"
    return DecimalFormat("#,##0.0").format(kb / 1024.0) + " MB"
}