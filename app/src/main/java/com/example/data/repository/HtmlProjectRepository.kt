package com.example.data.repository

import android.util.Log
import com.example.util.SecureConfig
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
        "cpanel ${SecureConfig.USER}:${SecureConfig.TOKEN}"

    suspend fun deleteFromCPanel(folderName: String): DeleteResult = withContext(Dispatchers.IO) {
        val client = buildClient()
        val host = SecureConfig.HOST
        val auth = cpanelAuth()

        try {
            // Step 1: Delete index.html inside folder
            val deleteFileBody = FormBody.Builder()
                .add("files-0-path", "/public_html/$folderName")
                .add("files-0-name", "index.html")
                .build()

            client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/delete_files")
                    .header("Authorization", auth)
                    .post(deleteFileBody)
                    .build()
            ).execute().use { res ->
                Log.d("CPANEL_DELETE", "Delete file: ${res.code} ${res.body?.string()}")
            }

            // Step 2: Delete the folder itself
            val deleteFolderBody = FormBody.Builder()
                .add("files-0-path", "/public_html")
                .add("files-0-name", folderName)
                .build()

            val res = client.newCall(
                Request.Builder()
                    .url("https://$host/execute/Fileman/delete_files")
                    .header("Authorization", auth)
                    .post(deleteFolderBody)
                    .build()
            ).execute()

            val resBody = res.body?.string() ?: "{}"
            val code = res.code
            res.close()

            Log.d("CPANEL_DELETE", "Delete folder: $code $resBody")

            val json = try { JSONObject(resBody) } catch (e: Exception) { JSONObject() }
            if (json.optInt("status", 0) == 1) {
                DeleteResult.Success
            } else {
                val err = json.optJSONArray("errors")?.optString(0) ?: "Delete failed"
                DeleteResult.Error(err)
            }
        } catch (e: Exception) {
            Log.e("CPANEL_DELETE", "Error", e)
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
        val host = SecureConfig.HOST
        val auth = cpanelAuth()

        try {
            // Create directory
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

            // Upload file as multipart
            val htmlBytes = htmlContent.toByteArray(Charsets.UTF_8)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("dir", "/public_html/$folderName")
                .addFormDataPart(
                    "file", "index.html",
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

            Log.d("CPANEL", "Upload: $code $resBody")

            val json = try { JSONObject(resBody) } catch (e: Exception) { JSONObject() }
            if (json.optInt("status", 0) == 1) {
                PublishResult.Success("${SecureConfig.SITE}/$folderName/", folderName)
            } else {
                val err = json.optJSONArray("errors")?.optString(0) ?: "Upload failed (HTTP $code)"
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
