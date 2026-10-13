package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectTree
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjectTree(): Flow<ProjectTree>

    /** Список по id; null — удалён или не существует. */
    fun observeProject(projectId: String): Flow<Project?>

    suspend fun getInbox(): Project

    suspend fun createGroup(name: String): String

    suspend fun renameGroup(groupId: String, name: String)

    /** Удаляет папку. Её списки не удаляются, а переносятся на верхний уровень. */
    suspend fun deleteGroup(groupId: String)

    suspend fun createProject(name: String, groupId: String? = null, color: String? = null): String

    suspend fun updateProject(projectId: String, name: String, groupId: String?, color: String?)

    /** Мягкое удаление списка вместе с задачами и их чек-листами. «Входящие» удалить нельзя. */
    suspend fun deleteProject(projectId: String)
}
