package com.example.data.local

import androidx.room.*
import com.example.data.model.HtmlProject
import kotlinx.coroutines.flow.Flow

@Dao
interface HtmlProjectDao {
    @Query("SELECT * FROM html_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<HtmlProject>>

    @Query("SELECT * FROM html_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): HtmlProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: HtmlProject): Long

    @Update
    suspend fun updateProject(project: HtmlProject)

    @Delete
    suspend fun deleteProject(project: HtmlProject)

    @Query("DELETE FROM html_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}
