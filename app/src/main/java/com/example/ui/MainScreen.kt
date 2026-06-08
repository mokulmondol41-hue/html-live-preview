package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HtmlProject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // View state flow observers
    val htmlCode by viewModel.htmlCode.collectAsStateWithLifecycle()
    val projectName by viewModel.projectName.collectAsStateWithLifecycle()
    val publishState by viewModel.publishState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val selectedLocalProject by viewModel.selectedLocalProject.collectAsStateWithLifecycle()
    val isFullscreenActive by viewModel.isFullscreenPreviewActive.collectAsStateWithLifecycle()

    // Single HTML file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileContent = readTextFromUri(context, uri)
            if (fileContent != null) {
                viewModel.updateHtmlCode(fileContent)
                
                // Set name based on actual file name if possible, or fallback
                val fileName = getFileName(context, uri) ?: "uploaded-site"
                val cleanedName = fileName.replace(".html", "").replace(".htm", "")
                viewModel.updateProjectName(cleanedName)
                
                viewModel.updateActiveTab(0) // Focus code editor
                Toast.makeText(context, "HTML Loaded Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to read HTML file content.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "App logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live HTML & Host",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        floatingActionButton = {
            if (activeTab != 2) {
                ExtendedFloatingActionButton(
                    text = { Text("Publish to Web", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Send, contentDescription = null) },
                    onClick = {
                        viewModel.publishProject()
                    },
                    modifier = Modifier.testTag("publish_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Workspace Header: Name configuration and templates
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Project Name Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { viewModel.updateProjectName(it) },
                            label = { Text("Project Name") },
                            placeholder = { Text("my-cool-website") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("project_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Import physical file button
                        FilledTonalButton(
                            onClick = { filePickerLauncher.launch("text/html") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .height(56.dp)
                                .testTag("upload_file_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload HTML")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", fontSize = 13.sp)
                        }
                    }

            }

            // Tabs Selector
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { viewModel.updateActiveTab(0) },
                    text = { Text("Editor", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_editor")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { viewModel.updateActiveTab(1) },
                    text = { Text("Preview", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_preview")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { viewModel.updateActiveTab(2) },
                    text = { Text("History", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            // Current Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> {
                        // CODE EDITOR SCREEN
                        CodeEditorPane(
                            htmlCode = htmlCode,
                            onCodeChange = { viewModel.updateHtmlCode(it) },
                            onSaveDraft = {
                                viewModel.saveProjectLocally {
                                    Toast.makeText(context, "Draft Saved Locally!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    1 -> {
                        // LIVE PREVIEW SCREEN
                        LivePreviewPane(
                            htmlCode = htmlCode,
                            onRefresh = { viewModel.updateHtmlCode(htmlCode) }, // Triggers reload
                            onFullscreen = { viewModel.setFullscreenPreview(true) }
                        )
                    }
                    2 -> {
                        // HISTORY OF WEBSITES
                        HistoryLayout(
                            savedProjects = savedProjects,
                            selectedProject = selectedLocalProject,
                            onSelect = { viewModel.selectProject(it) },
                            onDelete = { viewModel.deleteProject(it) },
                            onLaunch = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open project URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCopy = { url ->
                                viewModel.copyToClipboard(context, url)
                            },
                            onCreateNew = {
                                viewModel.createNewProject()
                            }
                        )
                    }
                }
            }
        }
    }

    // FULLSCREEN PREVIEW MODAL DIALOG
    if (isFullscreenActive) {
        Dialog(
            onDismissRequest = { viewModel.setFullscreenPreview(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.setFullscreenPreview(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Fullscreen")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fullscreen Preview",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.updateHtmlCode(htmlCode) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WebViewContainer(htmlCode = htmlCode)
                    }
                }
            }
        }
    }

        // PUBLISH PROCESS / OVERLAY FEEDBACK STATUS WINDOW TYPE
    when (val state = publishState) {
        is PublishUiState.Loading -> {
            Dialog(
                onDismissRequest = {}, // Disallow cancellation to avoid network splits
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 4.dp)
                        Text(
                            text = "Uploading to Web Server...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = state.stage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        is PublishUiState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetPublishState() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = { Text("Website Live & Hosted!", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your website is live! Visit the link below:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = state.url,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.primary,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                        }
                        Text(
                            text = "Your site is ready instantly!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.copyToClipboard(context, state.url)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not launch system browser", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.resetPublishState()
                        }
                    ) {
                        Text("Open Online")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
        is PublishUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetPublishState() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = { Text("Hosting Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetPublishState() }
                    ) {
                        Text("Dismiss")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
        else -> {}
    }
}

// ---------------- SUBPANE COMPOSABLES ----------------

@Composable
fun CodeEditorPane(
    htmlCode: String,
    onCodeChange: (String) -> Unit,
    onSaveDraft: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workspace Source Code",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onSaveDraft,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_draft_button")
            ) {
                Icon(Icons.Default.Create, contentDescription = "Save Draft", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Draft", fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Fully styled code area
        OutlinedTextField(
            value = htmlCode,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("html_code_input"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Write html code here...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun LivePreviewPane(
    htmlCode: String,
    onRefresh: () -> Unit,
    onFullscreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Sandbox Preview",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Preview")
                    }
                    IconButton(onClick = onFullscreen, modifier = Modifier.testTag("fullscreen_button")) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Immersive Preview")
                    }
                }
            }
            
            // WebView container
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                WebViewContainer(htmlCode = htmlCode)
            }
        }
    }
}

@Composable
fun WebViewContainer(htmlCode: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://localhost/",
                htmlCode,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = Modifier.fillMaxSize().testTag("preview_webview")
    )
}

@Composable
fun HistoryLayout(
    savedProjects: List<HtmlProject>,
    selectedProject: HtmlProject?,
    onSelect: (HtmlProject) -> Unit,
    onDelete: (HtmlProject) -> Unit,
    onLaunch: (String) -> Unit,
    onCopy: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Offline Drafts & Hosted Projects",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ElevatedButton(
                onClick = onCreateNew,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Draft")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (savedProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Your dynamic site lists are empty.",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Type or import any HTML code and hit 'Save Draft' or 'Publish to Web' to keep it here!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedProjects, key = { it.id }) { project ->
                    val isSelected = selectedProject?.id == project.id
                    val dateFormatted = remember(project.updatedAt) {
                        SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(project.updatedAt))
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(project) }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = if (isSelected) 
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else 
                            null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Modified: $dateFormatted",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDelete(project) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete project",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            if (project.publishedUrl != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = project.publishedUrl,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    IconButton(
                                        onClick = { onCopy(project.publishedUrl) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Copy link", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { onLaunch(project.publishedUrl) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = "Open web site", modifier = Modifier.size(14.dp))
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

// ---------------- STORAGE HELPERS ----------------

fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    } catch (e: Exception) {
        null
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
}
