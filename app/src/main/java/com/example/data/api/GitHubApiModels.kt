package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String,
    val private: Boolean = false,
    val auto_init: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RepoResponse(
    val name: String,
    val html_url: String,
    val default_branch: String? = "main"
)

@JsonClass(generateAdapter = true)
data class FileContentResponse(
    val sha: String,
    val path: String
)

@JsonClass(generateAdapter = true)
data class PutFileRequest(
    val message: String,
    val content: String, // Base64 encoded HTML
    val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class PutFileResponse(
    val content: FileInfo?
)

@JsonClass(generateAdapter = true)
data class FileInfo(
    val name: String,
    val path: String,
    val sha: String
)

@JsonClass(generateAdapter = true)
data class EnablePagesRequest(
    val source: PagesSource = PagesSource()
)

@JsonClass(generateAdapter = true)
data class PagesSource(
    val branch: String = "main",
    val path: String = "/"
)

@JsonClass(generateAdapter = true)
data class PagesResponse(
    val html_url: String?,
    val status: String?
)
