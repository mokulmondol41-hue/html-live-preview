package com.example.ui

import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HtmlProject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val htmlCode by viewModel.htmlCode.collectAsStateWithLifecycle()
    val projectName by viewModel.projectName.collectAsStateWithLifecycle()
    val publishState by viewModel.publishState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedLocalProject.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreenPreviewActive.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                viewModel.updateHtmlCode(content)
                val fileName = getFileName(context, it)
                if (fileName != null) {
                    val name = fileName.removeSuffix(".html").removeSuffix(".htm")
                    viewModel.updateProjectName(name)
                }
                Toast.makeText(context, "File imported!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (isFullscreen) {
        FullscreenPreview(
            htmlCode = htmlCode,
            onClose = { viewModel.setFullscreenPreview(false) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Live HTML & Host",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    if (selectedProject != null) {
                        IconButton(onClick = { viewModel.createNewProject() }) {
                            Icon(Icons.Default.Add, contentDescription = "New Project")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (activeTab != 2) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.publishProject() },
                    icon = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
                    text = { Text("Publish to Web") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Project name + Import button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { viewModel.updateProjectName(it) },
                    label = { Text("Project Name") },
                    placeholder = { Text("my-website") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.List, contentDescription = null)
                    }
                )
                Button(
                    onClick = { filePickerLauncher.launch("text/*") },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import")
                }
            }

            // Tabs
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { viewModel.updateActiveTab(0) },
                    text = { Text("Editor") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { viewModel.updateActiveTab(1) },
                    text = { Text("Preview") }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { viewModel.updateActiveTab(2) },
                    text = { Text("History") }
                )
            }

            // Tab content
            when (activeTab) {
                0 -> CodeEditorPane(
                    htmlCode = htmlCode,
                    onCodeChange = { viewModel.updateHtmlCode(it) },
                    onSave = { viewModel.saveProjectLocally() }
                )
                1 -> LivePreviewPane(
                    htmlCode = htmlCode,
                    onFullscreen = { viewModel.setFullscreenPreview(true) }
                )
                2 -> HistoryLayout(
                    savedProjects = savedProjects,
                    onSelectProject = { viewModel.selectProject(it) },
                    onDeleteProject = { viewModel.deleteProject(it) }
                )
            }
        }

        // Publish state dialogs
        when (val state = publishState) {
            is PublishUiState.Loading -> {
                Dialog(onDismissRequest = {}) {
                    Card(shape = MaterialTheme.shapes.large) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.stage, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            is PublishUiState.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetPublishState() },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    title = { Text("Hosted Successfully!") },
                    text = {
                        Column {
                            Text("Your website is live:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.url,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.copyToClipboard(context, state.url)
                            viewModel.resetPublishState()
                        }) {
                            Text("Copy Link")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.resetPublishState() }) {
                            Text("Close")
                        }
                    }
                )
            }
            is PublishUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetPublishState() },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    title = { Text("Hosting Failed") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetPublishState() }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun CodeEditorPane(
    htmlCode: String,
    onCodeChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Draft")
            }
        }
        OutlinedTextField(
            value = htmlCode,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text("Write HTML code here...") },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun LivePreviewPane(
    htmlCode: String,
    onFullscreen: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onFullscreen) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Fullscreen")
            }
        }
        WebViewContainer(htmlCode = htmlCode, modifier = Modifier.weight(1f))
    }
}

@Composable
fun WebViewContainer(
    htmlCode: String,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun FullscreenPreview(htmlCode: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        WebViewContainer(htmlCode = htmlCode, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
fun HistoryLayout(
    savedProjects: List<HtmlProject>,
    onSelectProject: (HtmlProject) -> Unit,
    onDeleteProject: (HtmlProject) -> Unit
) {
    val hostedCount = savedProjects.count { it.publishedUrl != null }

    Column(modifier = Modifier.fillMaxSize()) {
        if (savedProjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No projects yet",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // Hosted site count
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hostedCount >= 5)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Hosted sites: $hostedCount / 5" +
                        if (hostedCount >= 5) " — Delete one to host new" else "",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Medium,
                    color = if (hostedCount >= 5)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(savedProjects) { project ->
                    ProjectCard(
                        project = project,
                        onSelect = { onSelectProject(project) },
                        onDelete = { onDeleteProject(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: HtmlProject,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (project.publishedUrl != null) {
                    Text(
                        text = project.publishedUrl,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Draft — ${dateFormat.format(Date(project.updatedAt))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) result = it.getString(idx)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}
