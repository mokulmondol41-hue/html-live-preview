package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "html_projects")
data class HtmlProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val htmlContent: String,
    val publishedUrl: String? = null,
    val repoName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
