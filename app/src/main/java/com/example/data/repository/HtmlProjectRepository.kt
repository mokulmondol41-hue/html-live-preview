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
        val authHeader = "cpanel $cpanelUser:$cpanelToken"

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()

        try {
            // Step 1: Create folder
            val mkdirUrl = "https://$cpanelHost/execute/Fileman/mkdir?" +
                "dir=%2Fpublic_html&name=$folderName"
            val mkdirReq = Request.Builder()
                .url(mkdirUrl)
                .header("Authorization", authHeader)
                .post("".toRequestBody())
                .build()
            client.newCall(mkdirReq).execute().use { res ->
                Log.d("CPANEL", "mkdir: ${res.code}")
            }

            // Step 2: Upload via multipart - cPanel /execute/Fileman/upload_files
            val tmpFile = File.createTempFile("index", ".html")
            tmpFile.writeText(htmlContent, Charsets.UTF_8)

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("dir", "/public_html/$folderName")
                .addFormDataPart(
                    "file",
                    "index.html",
                    tmpFile.asRequestBody("text/html".toMediaType())
                )
                .build()

            val uploadUrl = "https://$cpanelHost/execute/Fileman/upload_files"
            val uploadReq = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", authHeader)
                .post(multipart)
                .build()

            val uploadRes = client.newCall(uploadReq).execute()
            val uploadBody = uploadRes.body?.string() ?: ""
            tmpFile.delete()

            Log.d("CPANEL", "upload: ${uploadRes.code} $uploadBody")

            val json = JSONObject(uploadBody)
            val status = json.optInt("status", 0)

            if (status == 1) {
                val liveUrl = "$siteUrl/$folderName/"
                PublishResult.Success(liveUrl, folderName)
            } else {
                val errors = json.optJSONArray("errors")
                val errMsg = if (errors != null && errors.length() > 0)
                    errors.getString(0) else "Upload failed. Check permissions."
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
