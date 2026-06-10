package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.HtmlProjectDao
import com.example.data.model.HtmlProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class PublishResult {
    data class Success(val url: String, val repoName: String) : PublishResult()
    data class Error(val message: String) : PublishResult()
}

sealed class DeleteResult {
    object Success : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}

class HtmlProjectRepository(private val htmlProjectDao: HtmlProjectDao) {

    val allProjects: Flow<List<HtmlProject>> = htmlProjectDao.getAllProjects()

    suspend fun getProjectById(id: Int): HtmlProject? = htmlProjectDao.getProjectById(id)
    suspend fun insertProject(project: HtmlProject): Long = htmlProjectDao.insertProject(project)
    suspend fun updateProject(project: HtmlProject) = htmlProjectDao.updateProject(project)
    suspend fun deleteProjectById(id: Int) = htmlProjectDao.deleteProjectById(id)

    private fun buildClient() = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .hostnameVerifier { _, _ -> true }
        .build()

    private fun cpanelAuth() = "cpanel ${BuildConfig.CPANEL_USER}:${BuildConfig.CPANEL_TOKEN}"

    // cPanel UAPI - create directory
    private suspend fun createDir(client: OkHttpClient, folderName: String) {
        try {
            val url = "https://${BuildConfig.CPANEL_HOST}/execute/Fileman/mkdir" +
                "?dir=%2Fpublic_html&name=$folderName"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", cpanelAuth())
                .post("".toRequestBody())
                .build()
            client.newCall(req).execute().close()
        } catch (e: Exception) {
            Log.w("CPANEL", "mkdir: ${e.message}")
        }
    }

    suspend fun deleteFromCPanel(folderName: String): DeleteResult = withContext(Dispatchers.IO) {
        val client = buildClient()
        try {
            val body = "files[0]=/public_html/$folderName&recursive=1"
            val req = Request.Builder()
                .url("https://${BuildConfig.CPANEL_HOST}/execute/Fileman/delete_files")
                .header("Authorization", cpanelAuth())
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            val res = client.newCall(req).execute()
            val json = JSONObject(res.body?.string() ?: "{}")
            res.close()
            if (json.optInt("status", 0) == 1) DeleteResult.Success
            else DeleteResult.Error("Delete failed")
        } catch (e: Exception) {
            DeleteResult.Error(e.localizedMessage ?: "Error")
        }
    }

    suspend fun publishToGitHub(
        projectName: String,
        htmlContent: String,
        username: String,
        token: String
    ): PublishResult = withContext(Dispatchers.IO) {

        val folderName = slugify(projectName)
        if (folderName.isEmpty()) {
            return@withContext PublishResult.Error("Project name cannot be empty.")
        }

        val client = buildClient()

        try {
            // Step 1: Create folder
            createDir(client, folderName)

            // Step 2: Upload via cPanel UAPI save_file_content
            // Content must be plain string (not base64) for this endpoint
            val host = BuildConfig.CPANEL_HOST
            val auth = cpanelAuth()

            // Build form body manually
            val content = htmlContent
            val encodedContent = java.net.URLEncoder.encode(content, "UTF-8")
            val encodedDir = java.net.URLEncoder.encode("/public_html/$folderName", "UTF-8")
            val formBody = "dir=$encodedDir&filename=index.html&content=$encodedContent"

            Log.d("CPANEL", "Uploading to /public_html/$folderName/index.html")

            val res = client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/save_file_content")
                    .header("Authorization", auth)
                    .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build()
            ).execute()

            val resBody = res.body?.string() ?: ""
            val code = res.code
            res.close()

            Log.d("CPANEL", "Response $code: $resBody")

            if (code == 200) {
                val json = try { JSONObject(resBody) } catch (e: Exception) { JSONObject() }
                val status = json.optInt("status", 0)
                if (status == 1) {
                    val liveUrl = "${BuildConfig.SITE_URL}/$folderName/"
                    PublishResult.Success(liveUrl, folderName)
                } else {
                    val errors = json.optJSONArray("errors")
                    val errMsg = if (errors != null && errors.length() > 0)
                        errors.getString(0) else "Upload failed"
                    PublishResult.Error("Hosting failed: $errMsg")
                }
            } else {
                PublishResult.Error("Server error: HTTP $code")
            }

        } catch (e: Exception) {
            Log.e("CPANEL", "Error", e)
            PublishResult.Error("Hosting failed: ${e.localizedMessage}")
        }
    }

    private fun slugify(name: String) = name.trim()
        .lowercase()
        .replace("[^a-z0-9\\-_]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .removePrefix("-").removeSuffix("-")
}
