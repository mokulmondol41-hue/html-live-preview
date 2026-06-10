package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.HtmlProjectDao
import com.example.data.model.HtmlProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
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

    private fun cpanelAuth() =
        "cpanel ${BuildConfig.CPANEL_USER}:${BuildConfig.CPANEL_TOKEN}"

    suspend fun deleteFromCPanel(folderName: String): DeleteResult = withContext(Dispatchers.IO) {
        val client = buildClient()
        try {
            val body = FormBody.Builder()
                .add("files[0]", "/public_html/$folderName")
                .add("recursive", "1")
                .build()
            val res = client.newCall(
                Request.Builder()
                    .url("https://${BuildConfig.CPANEL_HOST}/execute/Fileman/delete_files")
                    .header("Authorization", cpanelAuth())
                    .post(body)
                    .build()
            ).execute()
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
        val host = BuildConfig.CPANEL_HOST
        val auth = cpanelAuth()

        try {
            // Step 1: Create directory
            client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/mkdir")
                    .header("Authorization", auth)
                    .post(FormBody.Builder()
                        .add("dir", "/public_html")
                        .add("name", folderName)
                        .build())
                    .build()
            ).execute().close()

            // Step 2: Upload file as multipart with "file" parameter
            val htmlBytes = htmlContent.toByteArray(Charsets.UTF_8)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("dir", "/public_html/$folderName")
                .addFormDataPart(
                    "file",
                    "index.html",
                    htmlBytes.toRequestBody("text/html; charset=utf-8".toMediaType())
                )
                .build()

            val res = client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/upload_files")
                    .header("Authorization", auth)
                    .post(multipart)
                    .build()
            ).execute()

            val resBody = res.body?.string() ?: ""
            val code = res.code
            res.close()

            Log.d("CPANEL", "HTTP $code: $resBody")

            val json = try { JSONObject(resBody) } catch (e: Exception) { JSONObject() }
            val status = json.optInt("status", 0)

            if (status == 1) {
                PublishResult.Success("${BuildConfig.SITE_URL}/$folderName/", folderName)
            } else {
                val errors = json.optJSONArray("errors")
                val msg = if (errors != null && errors.length() > 0)
                    errors.getString(0) else "Upload failed (HTTP $code)"
                PublishResult.Error("Hosting failed: $msg")
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
