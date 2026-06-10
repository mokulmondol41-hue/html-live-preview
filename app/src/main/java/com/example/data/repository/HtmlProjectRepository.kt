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
import java.net.URLEncoder
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

    suspend fun deleteFromCPanel(folderName: String): DeleteResult = withContext(Dispatchers.IO) {
        val host = BuildConfig.CPANEL_HOST
        val user = BuildConfig.CPANEL_USER
        val token = BuildConfig.CPANEL_TOKEN
        val auth = "cpanel $user:$token"
        val client = buildClient()
        try {
            val body = "files[0]=/public_html/$folderName&recursive=1"
            val req = Request.Builder()
                .url("https://$host/execute/Fileman/delete_files")
                .header("Authorization", auth)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            val res = client.newCall(req).execute()
            val json = JSONObject(res.body?.string() ?: "{}")
            res.close()
            if (json.optInt("status", 0) == 1) DeleteResult.Success
            else DeleteResult.Error(json.optJSONArray("errors")?.optString(0) ?: "Delete failed")
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
        if (folderName.isEmpty()) return@withContext PublishResult.Error("Project name cannot be empty.")

        val host = BuildConfig.CPANEL_HOST
        val user = BuildConfig.CPANEL_USER
        val cpToken = BuildConfig.CPANEL_TOKEN
        val siteUrl = BuildConfig.SITE_URL
        val auth = "cpanel $user:$cpToken"
        val client = buildClient()

        try {
            // Step 1: Create folder
            client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/mkdir?dir=%2Fpublic_html&name=$folderName")
                    .header("Authorization", auth)
                    .post("".toRequestBody())
                    .build()
            ).execute().close()

            // Step 2: Save file using save_file_content with base64
            val htmlBase64 = Base64.encodeToString(
                htmlContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
            )
            val encodedContent = URLEncoder.encode(htmlBase64, "UTF-8")
            val body = "dir=%2Fpublic_html%2F$folderName&filename=index.html&content=$encodedContent&from_encoding=base64&to_encoding=utf-8"

            val res = client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/save_file_content")
                    .header("Authorization", auth)
                    .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build()
            ).execute()

            val resBody = res.body?.string() ?: ""
            res.close()
            Log.d("CPANEL", "save_file: $resBody")

            val json = JSONObject(resBody)
            if (json.optInt("status", 0) == 1) {
                PublishResult.Success("$siteUrl/$folderName/", folderName)
            } else {
                val err = json.optJSONArray("errors")?.optString(0) ?: "Upload failed"
                PublishResult.Error("Hosting failed: $err")
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
