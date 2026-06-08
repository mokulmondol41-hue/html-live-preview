package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.HtmlProjectDao
import com.example.data.model.HtmlProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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

class HtmlProjectRepository(
    private val htmlProjectDao: HtmlProjectDao
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
    ): PublishResult = withContext(Dispatchers.IO) {

        val folderName = slugify(projectName)
        if (folderName.isEmpty()) {
            return@withContext PublishResult.Error("Project name cannot be empty.")
        }

        val cpanelHost = BuildConfig.CPANEL_HOST
        val cpanelUser = BuildConfig.CPANEL_USER
        val cpanelToken = BuildConfig.CPANEL_TOKEN
        val siteUrl = BuildConfig.SITE_URL

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()

        try {
            // Step 1: Create folder in public_html
            val mkdirUrl = "https://$cpanelHost/execute/Fileman/mkdir" +
                "?dir=/public_html&name=$folderName"

            val mkdirReq = Request.Builder()
                .url(mkdirUrl)
                .header("Authorization", "cpanel $cpanelUser:$cpanelToken")
                .post("".toRequestBody())
                .build()

            client.newCall(mkdirReq).execute().use { res ->
                Log.d("CPANEL", "mkdir: ${res.code} ${res.body?.string()?.take(100)}")
            }

            // Step 2: Upload index.html via save_file_content
            val htmlBase64 = Base64.encodeToString(
                htmlContent.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val uploadUrl = "https://$cpanelHost/execute/Fileman/save_file_content"
            val body = "dir=/public_html/$folderName&filename=index.html&content=$htmlBase64&from_encoding=base64&to_encoding=utf-8"

            val uploadReq = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "cpanel $cpanelUser:$cpanelToken")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val uploadRes = client.newCall(uploadReq).execute()
            val uploadBody = uploadRes.body?.string() ?: ""
            Log.d("CPANEL", "upload: ${uploadRes.code} $uploadBody")

            val json = JSONObject(uploadBody)
            val status = json.optInt("status", 0)

            if (status == 1) {
                val liveUrl = "$siteUrl/$folderName/"
                PublishResult.Success(liveUrl, folderName)
            } else {
                val errors = json.optJSONArray("errors")
                val errMsg = errors?.optString(0) ?: "Upload failed"
                PublishResult.Error("Hosting failed: $errMsg")
            }

        } catch (e: Exception) {
            Log.e("CPANEL", "Error", e)
            PublishResult.Error("Hosting failed: ${e.localizedMessage}")
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
