package com.example.data.api

import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<RepoResponse>

    @POST("user/repos")
    suspend fun createRepository(
        @Header("Authorization") authorization: String,
        @Body request: CreateRepoRequest
    ): Response<RepoResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): Response<FileContentResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun uploadFile(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: PutFileRequest
    ): Response<PutFileResponse>

    @Headers("Accept: application/vnd.github+json")
    @POST("repos/{owner}/{repo}/pages")
    suspend fun enablePages(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: EnablePagesRequest
    ): Response<PagesResponse>
}
