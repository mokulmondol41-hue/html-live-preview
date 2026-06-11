package com.example.ui

import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FabPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
    val isFullscreen by viewModel.isFullscreenPreviewActive.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val content = stream?.bufferedReader()?.readText() ?: ""
                stream?.close()
                viewModel.updateHtmlCode(content)
                getFileName(context, it)?.let { name ->
                    viewModel.updateProjectName(name.removeSuffix(".html").removeSuffix(".htm"))
                }
                Toast.makeText(context, "File imported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (isFullscreen) {
        FullscreenPreview(htmlCode = htmlCode, onClose = { viewModel.setFullscreenPreview(false) })
        return
    }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A56DB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Live HTML & Host",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827))
                        Text("Build & publish websites",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280))
                    }
                    Spacer(modifier = Modifier.weight(1f))

                }
            }
        },
        floatingActionButton = {
            if (activeTab != 2) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.publishProject() },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("Publish to Web", fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFF1A56DB),
                    contentColor = Color.White,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color(0xFFF0F4FF)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Project name + import
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { viewModel.updateProjectName(it) },
                        label = { Text("Project Name") },
                        placeholder = { Text("my-website") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.FolderOpen, contentDescription = null,
                                tint = Color(0xFF1A56DB))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A56DB),
                            focusedLabelColor = Color(0xFF1A56DB)
                        )
                    )
                    Button(
                        onClick = { filePickerLauncher.launch("text/*") },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tabs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .shadow(3.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A56DB),

                ) {
                    listOf(
                        Pair(Icons.Default.Code, "Editor"),
                        Pair(Icons.Default.Visibility, "Preview"),
                        Pair(Icons.Default.History, "History")
                    ).forEachIndexed { index, (icon, label) ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { viewModel.updateActiveTab(index) },
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text(label, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                            selectedContentColor = Color(0xFF1A56DB),
                            unselectedContentColor = Color(0xFF6B7280)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (activeTab) {
                0 -> CodeEditorPane(htmlCode = htmlCode,
                    onCodeChange = { viewModel.updateHtmlCode(it) },
                    onSave = { viewModel.saveProjectLocally() })
                1 -> LivePreviewPane(htmlCode = htmlCode,
                    onFullscreen = { viewModel.setFullscreenPreview(true) })
                2 -> HistoryLayout(
                    savedProjects = savedProjects,
                    onSelectProject = { viewModel.selectProject(it) },
                    onDeleteProject = { viewModel.deleteProject(it) },
                    onCopyLink = { project ->
                        project.publishedUrl?.let { url ->
                            viewModel.copyToClipboard(context, url)
                        }
                    }
                )
            }
        }

        // Publish state overlays
        when (val state = publishState) {
            is PublishUiState.Loading -> {
                Dialog(onDismissRequest = {}) {
                    Card(shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.shadow(16.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF1A56DB), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.stage, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                        }
                    }
                }
            }
            is PublishUiState.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetPublishState() },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White,
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = Color(0xFF1A56DB), modifier = Modifier.size(28.dp))
                        }
                    },
                    title = { Text("Website is Live!", fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)) },
                    text = {
                        Column {
                            Text("Your website has been published successfully.",
                                color = Color(0xFF6B7280), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDBEAFE))) {
                                Text(state.url, modifier = Modifier.padding(10.dp),
                                    color = Color(0xFF1A56DB), fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE3F2FD))
                                    .clickable {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://t.me/shuvo_bhai11"))
                                        context.startActivity(intent)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null,
                                    tint = Color(0xFF0088CC), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Join our Telegram channel",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0088CC))
                            }
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                viewModel.copyToClipboard(context, state.url)
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB)),
                                shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(state.url))
                                context.startActivity(intent)
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.resetPublishState() }) {
                            Text("Close", color = Color(0xFF6B7280))
                        }
                    }
                )
            }
            is PublishUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetPublishState() },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White,
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null,
                                tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp))
                        }
                    },
                    title = { Text("Hosting Failed", fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)) },
                    text = { Text(state.message, color = Color(0xFF6B7280), fontSize = 13.sp) },
                    confirmButton = {
                        Button(onClick = { viewModel.resetPublishState() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun CodeEditorPane(htmlCode: String, onCodeChange: (String) -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://t.me/shuvo_bhai11"))
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Send, contentDescription = "Telegram",
                    tint = Color(0xFF0088CC), modifier = Modifier.size(20.dp))
            }
            TextButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = null,
                    modifier = Modifier.size(16.dp), tint = Color(0xFF1A56DB))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Draft", color = Color(0xFF1A56DB), fontWeight = FontWeight.SemiBold)
            }
        }
        Card(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)) {
            OutlinedTextField(
                value = htmlCode,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                placeholder = { Text("Write your HTML code here...",
                    color = Color(0xFF6B7280), fontSize = 13.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFF111827)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun LivePreviewPane(htmlCode: String, onFullscreen: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFullscreen) {
                Icon(Icons.Default.Fullscreen, contentDescription = null,
                    modifier = Modifier.size(16.dp), tint = Color(0xFF1A56DB))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fullscreen", color = Color(0xFF1A56DB), fontWeight = FontWeight.SemiBold)
            }
        }
        Card(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)) {
            WebViewContainer(htmlCode = htmlCode, modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
fun WebViewContainer(htmlCode: String, modifier: Modifier = Modifier) {
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
            webView.loadDataWithBaseURL("https://localhost/", htmlCode, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun FullscreenPreview(htmlCode: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        WebViewContainer(htmlCode = htmlCode, modifier = Modifier.fillMaxSize())
        FloatingActionButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp),
            containerColor = Color(0xFF1A56DB),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun HistoryLayout(
    savedProjects: List<HtmlProject>,
    onSelectProject: (HtmlProject) -> Unit,
    onDeleteProject: (HtmlProject) -> Unit,
    onCopyLink: (HtmlProject) -> Unit = {}
) {
    val context = LocalContext.current
    val hostedCount = savedProjects.count { !it.publishedUrl.isNullOrEmpty() }

    Column(modifier = Modifier.fillMaxSize()) {
        if (savedProjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null,
                            tint = Color(0xFF1A56DB), modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Projects Yet", fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp, color = Color(0xFF111827))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Create your first website and publish it to the web.",
                        color = Color(0xFF6B7280), fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD))
                            .clickable {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://t.me/shuvo_bhai11"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null,
                            tint = Color(0xFF0088CC), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join Telegram for tips & updates",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0088CC))
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hostedCount >= 5) Color(0xFFFEE2E2) else Color(0xFFDBEAFE)
                )
            ) {
                Row(modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hostedCount >= 5) Icons.Default.Warning else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (hostedCount >= 5) Color(0xFFDC2626) else Color(0xFF1A56DB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (hostedCount >= 5)
                            "Limit reached ($hostedCount/5) — Delete one to host new"
                        else
                            "Hosted websites: $hostedCount / 5",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (hostedCount >= 5) Color(0xFFDC2626) else Color(0xFF1342B0)
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()
                .padding(horizontal = 12.dp)) {
                items(savedProjects, key = { it.id }) { project ->
                    ProjectCard(project = project,
                        onSelect = { onSelectProject(project) },
                        onDelete = { onDeleteProject(project) })
                }
            }
        }
    }
}

@Composable
fun ProjectCard(project: HtmlProject, onSelect: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isHosted = !project.publishedUrl.isNullOrEmpty()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .shadow(5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onSelect
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (isHosted) Color(0xFFDBEAFE) else Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center) {
                Icon(
                    if (isHosted) Icons.Default.Language else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isHosted) Color(0xFF1A56DB) else Color(0xFF6B7280),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = Color(0xFF111827))
                Spacer(modifier = Modifier.height(3.dp))
                if (isHosted) {
                    Text(project.publishedUrl ?: "", fontSize = 11.sp,
                        color = Color(0xFF1A56DB), fontWeight = FontWeight.Medium)
                } else {
                    Text("Draft — ${dateFormat.format(Date(project.updatedAt))}",
                        fontSize = 11.sp, color = Color(0xFF6B7280))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isHosted) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDBEAFE)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A56DB))
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            IconButton(onClick = onDelete,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEE2E2))) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use {
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
