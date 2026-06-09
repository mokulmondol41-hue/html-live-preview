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
import com.example.data.repository.DeleteResult
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

    private val _selectedLocalProject = MutableStateFlow<HtmlProject?>(null)
    val selectedLocalProject: StateFlow<HtmlProject?> = _selectedLocalProject.asStateFlow()

    private val _isFullscreenPreviewActive = MutableStateFlow(false)
    val isFullscreenPreviewActive: StateFlow<Boolean> = _isFullscreenPreviewActive.asStateFlow()

    fun updateHtmlCode(code: String) { _htmlCode.value = code }
    fun updateProjectName(name: String) { _projectName.value = name }
    fun updateActiveTab(tab: Int) { _activeTab.value = tab }
    fun setFullscreenPreview(active: Boolean) { _isFullscreenPreviewActive.value = active }
    fun resetPublishState() { _publishState.value = PublishUiState.Idle }

    fun selectProject(project: HtmlProject) {
        _selectedLocalProject.value = project
        _projectName.value = project.name
        _htmlCode.value = project.htmlContent
    }

    fun createNewProject() {
        _selectedLocalProject.value = null
        _projectName.value = ""
        _htmlCode.value = ""
        _activeTab.value = 0
    }

    fun saveProjectLocally(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val current = _selectedLocalProject.value
            if (current != null) {
                val updated = current.copy(
                    name = _projectName.value,
                    htmlContent = _htmlCode.value,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateProject(updated)
                _selectedLocalProject.value = updated
            } else {
                val id = repository.insertProject(
                    HtmlProject(name = _projectName.value, htmlContent = _htmlCode.value)
                )
                _selectedLocalProject.value = HtmlProject(
                    id = id.toInt(),
                    name = _projectName.value,
                    htmlContent = _htmlCode.value
                )
            }
            onSuccess()
        }
    }

    fun deleteProject(project: HtmlProject) {
        viewModelScope.launch {
            // If hosted, delete from cPanel first
            if (!project.repoName.isNullOrEmpty()) {
                _publishState.value = PublishUiState.Loading("Deleting from server...")
                when (val result = repository.deleteFromCPanel(project.repoName)) {
                    is DeleteResult.Success -> {
                        repository.deleteProjectById(project.id)
                        if (_selectedLocalProject.value?.id == project.id) {
                            _selectedLocalProject.value = null
                        }
                        _publishState.value = PublishUiState.Idle
                    }
                    is DeleteResult.Error -> {
                        // Delete locally anyway
                        repository.deleteProjectById(project.id)
                        if (_selectedLocalProject.value?.id == project.id) {
                            _selectedLocalProject.value = null
                        }
                        _publishState.value = PublishUiState.Idle
                    }
                }
            } else {
                repository.deleteProjectById(project.id)
                if (_selectedLocalProject.value?.id == project.id) {
                    _selectedLocalProject.value = null
                }
            }
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
            val hostedCount = savedProjects.first().count { !it.publishedUrl.isNullOrEmpty() }
            val isUpdate = !_selectedLocalProject.value?.publishedUrl.isNullOrEmpty()

            if (!isUpdate && hostedCount >= MAX_HOSTED_SITES) {
                _publishState.value = PublishUiState.Error(
                    "Hosting limit reached ($MAX_HOSTED_SITES websites). Delete one from History to continue."
                )
                return@launch
            }

            _publishState.value = PublishUiState.Loading("Uploading to server...")

            when (val result = repository.publishToGitHub(name, content, "", "")) {
                is PublishResult.Success -> {
                    val current = _selectedLocalProject.value
                    if (current != null) {
                        repository.updateProject(current.copy(
                            publishedUrl = result.url,
                            repoName = result.repoName,
                            updatedAt = System.currentTimeMillis()
                        ))
                    } else {
                        val id = repository.insertProject(HtmlProject(
                            name = name,
                            htmlContent = content,
                            publishedUrl = result.url,
                            repoName = result.repoName
                        ))
                        _selectedLocalProject.value = HtmlProject(
                            id = id.toInt(), name = name, htmlContent = content,
                            publishedUrl = result.url, repoName = result.repoName
                        )
                    }
                    _publishState.value = PublishUiState.Success(result.url, result.repoName)
                }
                is PublishResult.Error -> {
                    _publishState.value = PublishUiState.Error(result.message)
                }
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Link") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
    }
}
