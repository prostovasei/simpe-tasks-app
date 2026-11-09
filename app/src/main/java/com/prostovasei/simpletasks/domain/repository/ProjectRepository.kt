package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectTree
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjectTree(): Flow<ProjectTree>

    /** Список по id; null — удалён или не существует. */
    fun observeProject(projectId: String): Flow<Project?>

    suspend fun getInbox(): Project

    /** Новая папка; [emoji] (итерация 56) — эмодзи папки, null — 📁. */
    suspend fun createGroup(name: String, emoji: String? = null): String

    /** Название и эмодзи папки (итерация 56: окно «Папка»). */
    suspend fun updateGroup(groupId: String, name: String, emoji: String?)

    /** Удаляет папку. Её списки не удаляются, а переносятся на верхний уровень. */
    suspend fun deleteGroup(groupId: String)

    /** Новый список; [emoji] (итерация 56) — выбирается сразу в окне создания, null — 📁. */
    suspend fun createProject(name: String, groupId: String? = null, color: String? = null, emoji: String? = null): String

    suspend fun updateProject(projectId: String, name: String, groupId: String?, color: String?)

    /** Эмодзи списка (итерация 47; до этого — значок, итерация 14); null — по умолчанию. */
    suspend fun setProjectEmoji(projectId: String, emoji: String?)

    /** Мягкое удаление списка вместе с задачами и их чек-листами. «Входящие» удалить нельзя. */
    suspend fun deleteProject(projectId: String)
}
