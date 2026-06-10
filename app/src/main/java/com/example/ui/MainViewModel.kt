package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.HtmlProject
import com.example.data.repository.HtmlProjectRepository
import com.example.data.repository.PublishResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PublishUiState {
    object Idle : PublishUiState()
    data class Loading(val stage: String) : PublishUiState()
    data class Success(val url: String, val repoName: String) : PublishUiState()
    data class Error(val message: String) : PublishUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HtmlProjectRepository(db.htmlProjectDao())

    companion object { const val MAX_HOSTED_SITES = 5 }

    val savedProjects: StateFlow<List<HtmlProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _htmlCode = MutableStateFlow("")
    val htmlCode: StateFlow<String> = _htmlCode.asStateFlow()

    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    private val _publishState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val publishState: StateFlow<PublishUiState> = _publishState.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _isFullscreenPreviewActive = MutableStateFlow(false)
    val isFullscreenPreviewActive: StateFlow<Boolean> = _isFullscreenPreviewActive.asStateFlow()

    fun updateHtmlCode(code: String) { _htmlCode.value = code }
    fun updateProjectName(name: String) { _projectName.value = name }
    fun updateActiveTab(tab: Int) { _activeTab.value = tab }
    fun setFullscreenPreview(active: Boolean) { _isFullscreenPreviewActive.value = active }
    fun resetPublishState() { _publishState.value = PublishUiState.Idle }

    // Project select করলে edit mode
    fun selectProject(project: HtmlProject) {
        _projectName.value = project.name
        _htmlCode.value = project.htmlContent
    }

    // নতুন project শুরু করলে সব clear
    fun createNewProject() {
        _projectName.value = ""
        _htmlCode.value = ""
        _activeTab.value = 0
    }

    fun saveProjectLocally(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val name = _projectName.value.trim()
            if (name.isEmpty()) return@launch
            repository.insertProject(
                HtmlProject(name = name, htmlContent = _htmlCode.value)
            )
            onSuccess()
        }
    }

    fun deleteProject(project: HtmlProject) {
        viewModelScope.launch {
            // cPanel এ hosted থাকলে সেখান থেকেও delete
            if (!project.repoName.isNullOrEmpty()) {
                _publishState.value = PublishUiState.Loading("Deleting from server...")
                repository.deleteFromCPanel(project.repoName)
                _publishState.value = PublishUiState.Idle
            }
            repository.deleteProjectById(project.id)
        }
    }

    fun publishProject() {
        val name = _projectName.value.trim()
        val content = _htmlCode.value

        if (name.isEmpty()) {
            _publishState.value = PublishUiState.Error("Please enter a project name.")
            return
        }
        if (content.isEmpty()) {
            _publishState.value = PublishUiState.Error("HTML content cannot be empty.")
            return
        }

        viewModelScope.launch {
            val allProjects = savedProjects.first()

            // Slug name বানাই
            val slugName = slugify(name)

            // Duplicate name check
            val duplicate = allProjects.find { p ->
                !p.repoName.isNullOrEmpty() && p.repoName == slugName
            }
            if (duplicate != null) {
                _publishState.value = PublishUiState.Error(
                    "\"${duplicate.name}\" is already hosted. Please choose a different name."
                )
                return@launch
            }

            // Limit check
            val hostedCount = allProjects.count { !it.publishedUrl.isNullOrEmpty() }
            if (hostedCount >= MAX_HOSTED_SITES) {
                _publishState.value = PublishUiState.Error(
                    "Limit reached ($hostedCount/$MAX_HOSTED_SITES). Delete one from History first."
                )
                return@launch
            }

            _publishState.value = PublishUiState.Loading("Uploading to server...")

            when (val result = repository.publishToGitHub(name, content, "", "")) {
                is PublishResult.Success -> {
                    // সবসময় নতুন row insert করো — update না
                    repository.insertProject(HtmlProject(
                        name = name,
                        htmlContent = content,
                        publishedUrl = result.url,
                        repoName = result.repoName
                    ))
                    // Editor clear করি নতুন project এর জন্য
                    _projectName.value = ""
                    _htmlCode.value = ""
                    _publishState.value = PublishUiState.Success(result.url, result.repoName)
                }
                is PublishResult.Error -> {
                    _publishState.value = PublishUiState.Error(result.message)
                }
            }
        }
    }

    private fun slugify(name: String) = name.trim()
        .lowercase()
        .replace("[^a-z0-9\\-_]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .removePrefix("-").removeSuffix("-")

    fun copyToClipboard(context: Context, text: String, label: String = "Link") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
    }
}
