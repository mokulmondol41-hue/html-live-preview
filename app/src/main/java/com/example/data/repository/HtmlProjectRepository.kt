package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.HtmlProjectDao
import com.example.data.model.HtmlProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayInputStream

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

        val ftpHost = BuildConfig.FTP_HOST
        val ftpUser = BuildConfig.FTP_USER
        val ftpPass = BuildConfig.FTP_PASS
        val siteUrl = BuildConfig.SITE_URL

        val ftp = FTPClient()
        try {
            Log.d("FTP", "Connecting to $ftpHost")
            ftp.connect(ftpHost, 21)
            ftp.login(ftpUser, ftpPass)
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            // public_html এ folder বানাও
            val remotePath = "/public_html/$folderName"
            ftp.makeDirectory(remotePath)

            // index.html upload করো
            val htmlBytes = htmlContent.toByteArray(Charsets.UTF_8)
            val inputStream = ByteArrayInputStream(htmlBytes)
            val uploaded = ftp.storeFile("$remotePath/index.html", inputStream)
            inputStream.close()

            ftp.logout()
            ftp.disconnect()

            if (uploaded) {
                val liveUrl = "$siteUrl/$folderName/"
                Log.d("FTP", "Upload success: $liveUrl")
                PublishResult.Success(liveUrl, folderName)
            } else {
                PublishResult.Error("File upload failed. Check FTP permissions.")
            }

        } catch (e: Exception) {
            Log.e("FTP", "FTP error", e)
            try { ftp.disconnect() } catch (_: Exception) {}
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
