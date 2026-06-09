package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.HtmlProjectDao
import com.example.data.model.HtmlProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
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
        val cpanelHost = BuildConfig.CPANEL_HOST
        val cpanelUser = BuildConfig.CPANEL_USER
        val cpanelToken = BuildConfig.CPANEL_TOKEN
        val authHeader = "cpanel $cpanelUser:$cpanelToken"
        val client = buildClient()

        try {
            val url = "https://$cpanelHost/execute/Fileman/delete_files"
            val body = "files[0]=/public_html/$folderName&recursive=1"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val res = client.newCall(req).execute()
            val resBody = res.body?.string() ?: ""
            Log.d("CPANEL", "delete: ${res.code} $resBody")

            val json = JSONObject(resBody)
            if (json.optInt("status", 0) == 1) {
                DeleteResult.Success
            } else {
                val err = json.optJSONArray("errors")?.optString(0) ?: "Delete failed"
                DeleteResult.Error(err)
            }
        } catch (e: Exception) {
            Log.e("CPANEL", "Delete error", e)
            DeleteResult.Error(e.localizedMessage ?: "Unknown error")
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

        val cpanelHost = BuildConfig.CPANEL_HOST
        val cpanelUser = BuildConfig.CPANEL_USER
        val cpanelToken = BuildConfig.CPANEL_TOKEN
        val siteUrl = BuildConfig.SITE_URL
        val authHeader = "cpanel $cpanelUser:$cpanelToken"
        val client = buildClient()

        try {
            // Create folder
            val mkdirUrl = "https://$cpanelHost/execute/Fileman/mkdir?dir=%2Fpublic_html&name=$folderName"
            client.newCall(
                Request.Builder().url(mkdirUrl).header("Authorization", authHeader)
                    .post("".toRequestBody()).build()
            ).execute().close()

            // Upload file
            val tmpFile = File.createTempFile("index", ".html")
            tmpFile.writeText(htmlContent, Charsets.UTF_8)

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("dir", "/public_html/$folderName")
                .addFormDataPart("file", "index.html", tmpFile.asRequestBody("text/html".toMediaType()))
                .build()

            val uploadRes = client.newCall(
                Request.Builder()
                    .url("https://$cpanelHost/execute/Fileman/upload_files")
                    .header("Authorization", authHeader)
                    .post(multipart).build()
            ).execute()

            val uploadBody = uploadRes.body?.string() ?: ""
            tmpFile.delete()

            val json = JSONObject(uploadBody)
            if (json.optInt("status", 0) == 1) {
                PublishResult.Success("$siteUrl/$folderName/", folderName)
            } else {
                val err = json.optJSONArray("errors")?.optString(0) ?: "Upload failed"
                PublishResult.Error("Hosting failed: $err")
            }
        } catch (e: Exception) {
            Log.e("CPANEL", "Upload error", e)
            PublishResult.Error("Hosting failed: ${e.localizedMessage}")
        }
    }

    private fun slugify(name: String) = name.trim()
        .lowercase()
        .replace("[^a-z0-9\\-_]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .removePrefix("-").removeSuffix("-")
}
