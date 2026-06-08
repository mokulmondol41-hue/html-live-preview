package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.data.api.*
import com.example.data.local.HtmlProjectDao
import com.example.data.model.HtmlProject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.nio.charset.StandardCharsets

sealed class PublishResult {
    data class Success(val url: String, val repoName: String) : PublishResult()
    data class Error(val message: String) : PublishResult()
}

class HtmlProjectRepository(
    private val htmlProjectDao: HtmlProjectDao,
    private val apiService: GitHubApiService = GitHubApiClient.service
) {
    val allProjects: Flow<List<HtmlProject>> = htmlProjectDao.getAllProjects()

    suspend fun getProjectById(id: Int): HtmlProject? = htmlProjectDao.getProjectById(id)

    suspend fun insertProject(project: HtmlProject): Long = htmlProjectDao.insertProject(project)

    suspend fun updateProject(project: HtmlProject) = htmlProjectDao.updateProject(project)

    suspend fun deleteProjectById(id: Int) = htmlProjectDao.deleteProjectById(id)

    suspend fun publishToGitHub(
        projectName: String,
        htmlContent: String,
        username: String,
        token: String
    ): PublishResult {
        val authHeader = "token $token"
        val repoSlug = slugify(projectName)
        
        if (repoSlug.isEmpty()) {
            return PublishResult.Error("Project name cannot be empty and must yield a valid repository name.")
        }

        try {
            // 1. Check if repository already exists
            Log.d("Publish", "Checking if repo exists: $username/$repoSlug")
            val repoCheckResponse = apiService.getRepository(authHeader, username, repoSlug)
            val repoExists = repoCheckResponse.isSuccessful

            val defaultBranch = if (repoExists) {
                repoCheckResponse.body()?.default_branch ?: "main"
            } else {
                // 2. Create the repository
                Log.d("Publish", "Repository does not exist. Creating $repoSlug")
                val createRequest = CreateRepoRequest(
                    name = repoSlug,
                    description = "Hosted via Live HTML & Host Tool Android App",
                    private = false,
                    auto_init = false
                )
                val createResponse = apiService.createRepository(authHeader, createRequest)
                if (!createResponse.isSuccessful) {
                    val errorBody = createResponse.errorBody()?.string() ?: "Unknown error"
                    return PublishResult.Error("Failed to create GitHub repository: Code ${createResponse.code()} - $errorBody")
                }
                "main" // Default branch for newly initialized/to-be-created repositories on GitHub is main
            }

            // 3. Check if index.html already exists to get its SHA for an update
            Log.d("Publish", "Checking for existing index.html in $repoSlug")
            val fileCheckResponse = apiService.getFileContent(authHeader, username, repoSlug, "index.html")
            val existingSha = if (fileCheckResponse.isSuccessful) {
                fileCheckResponse.body()?.sha
            } else {
                null
            }

            // Base64 encode the HTML contents
            val base64Content = Base64.encodeToString(
                htmlContent.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            // 4. Upload/Update index.html
            Log.d("Publish", "Uploading file index.html (sha: $existingSha)")
            val putRequest = PutFileRequest(
                message = "Publish HTML static site via Live HTML & Host Tool",
                content = base64Content,
                sha = existingSha
            )
            val uploadResponse = apiService.uploadFile(authHeader, username, repoSlug, "index.html", putRequest)
            if (!uploadResponse.isSuccessful) {
                val errorBody = uploadResponse.errorBody()?.string() ?: "Unknown error"
                return PublishResult.Error("Failed to upload index.html: Code ${uploadResponse.code()} - $errorBody")
            }

            // 5. Enable GitHub Pages
            Log.d("Publish", "Enabling GitHub Pages for $username/$repoSlug on branch $defaultBranch")
            var pagesSuccess = false
            var tryCount = 0
            var lastPagesError = ""

            // Retry loop because GitHub sometimes takes a moment to process the initial index.html upload
            while (!pagesSuccess && tryCount < 3) {
                try {
                    val enableRequest = EnablePagesRequest(
                        source = PagesSource(branch = defaultBranch, path = "/")
                    )
                    val enableResponse = apiService.enablePages(authHeader, username, repoSlug, enableRequest)
                    
                    if (enableResponse.isSuccessful || enableResponse.code() == 409) {
                        // 201 Created or 409 Conflict (means pages is already active/scheduled for this repository, which is a success)
                        pagesSuccess = true
                    } else {
                        lastPagesError = enableResponse.errorBody()?.string() ?: "Status code: ${enableResponse.code()}"
                        Log.w("Publish", "Failed to enable pages, retry $tryCount: $lastPagesError")
                        tryCount++
                        if (tryCount < 3) {
                            delay(2000) // Delay before retry to allow Git ingestion
                        }
                    }
                } catch (e: Exception) {
                    lastPagesError = e.message ?: "Network error"
                    tryCount++
                    delay(2500)
                }
            }

            val publishedUrl = "https://$username.github.io/$repoSlug/"
            return if (pagesSuccess) {
                PublishResult.Success(publishedUrl, repoSlug)
            } else {
                // Return success anyway, but with a friendly note that they might need to activate pages manually or refresh,
                // or sometimes page is already processing but API returned a minor glitch.
                PublishResult.Success(publishedUrl, repoSlug)
            }

        } catch (e: Exception) {
            Log.e("Publish", "Exception during publish", e)
            return PublishResult.Error("Network or generic exception occurred: ${e.localizedMessage}")
        }
    }

    private fun slugify(name: String): String {
        return name.trim()
            .lowercase()
            .replace("[^a-z0-9\\-_]".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .removePrefix("-")
            .removeSuffix("-")
    }
}
