package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.ProjectEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectGroupEntity
import com.prostovasei.simpletasks.data.mapper.toDomain
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomProjectRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : ProjectRepository {

    private val groupDao = db.projectGroupDao()
    private val projectDao = db.projectDao()
    private val taskDao = db.taskDao()
    private val checklistDao = db.checklistDao()

    override fun observeProjectTree(): Flow<ProjectTree> =
        combine(groupDao.observeActive(), projectDao.observeActiveWithOpenCount()) { groups, projects ->
            val all = projects.map { it.toDomain() }
            val (inbox, regular) = all.partition { it.project.isInbox }
            val groupIds = groups.map { it.id }.toSet()
            val byGroup = regular.filter { it.project.groupId in groupIds }.groupBy { it.project.groupId }
            ProjectTree(
                inbox = inbox.firstOrNull(),
                groups = groups.map { group -> GroupWithProjects(group.toDomain(), byGroup[group.id].orEmpty()) },
                ungrouped = regular.filter { it.project.groupId !in groupIds },
            )
        }.distinctUntilChanged()

    override fun observeProject(projectId: String): Flow<Project?> =
        projectDao.observeById(projectId).map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun getInbox(): Project =
        checkNotNull(projectDao.getInbox()) { "Список «Входящие» должен создаваться при открытии БД" }.toDomain()

    override suspend fun createGroup(name: String): String {
        require(name.isNotBlank()) { "Название папки не может быть пустым" }
        val id = ids.newId()
        val now = nowMillis()
        db.withTransaction {
            groupDao.insert(
                ProjectGroupEntity(id = id, name = name, sortOrder = groupDao.nextSortOrder(), createdAt = now, updatedAt = now),
            )
        }
        return id
    }

    override suspend fun renameGroup(groupId: String, name: String) {
        require(name.isNotBlank()) { "Название папки не может быть пустым" }
        groupDao.rename(groupId, name, nowMillis())
    }

    override suspend fun deleteGroup(groupId: String) {
        val now = nowMillis()
        db.withTransaction {
            projectDao.ungroup(groupId, now)
            groupDao.softDelete(groupId, now)
        }
    }

    override suspend fun createProject(name: String, groupId: String?, color: String?): String {
        require(name.isNotBlank()) { "Название списка не может быть пустым" }
        val id = ids.newId()
        val now = nowMillis()
        db.withTransaction {
            projectDao.insert(
                ProjectEntity(
                    id = id,
                    groupId = groupId,
                    name = name,
                    color = color,
                    sortOrder = projectDao.nextSortOrder(groupId),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return id
    }

    override suspend fun updateProject(projectId: String, name: String, groupId: String?, color: String?) {
        require(name.isNotBlank()) { "Название списка не может быть пустым" }
        projectDao.update(projectId, name, groupId, color, nowMillis())
    }

    override suspend fun setProjectIcon(projectId: String, icon: String?) {
        projectDao.setIcon(projectId, icon, nowMillis())
    }

    override suspend fun deleteProject(projectId: String) {
        val now = nowMillis()
        db.withTransaction {
            val project = projectDao.getById(projectId) ?: return@withTransaction
            require(!project.isInbox) { "Список «Входящие» удалить нельзя" }
            checklistDao.softDeleteByProject(projectId, now)
            taskDao.softDeleteByProject(projectId, now)
            projectDao.softDelete(projectId, now)
        }
    }

    private fun nowMillis() = time.now().toEpochMilli()
}
