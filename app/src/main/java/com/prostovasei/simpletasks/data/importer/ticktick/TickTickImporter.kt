package com.prostovasei.simpletasks.data.importer.ticktick

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectGroupEntity
import com.prostovasei.simpletasks.data.local.entity.TaskEntity
import com.prostovasei.simpletasks.domain.model.ImportProgress
import com.prostovasei.simpletasks.domain.model.ImportReport
import javax.inject.Inject

/**
 * Записывает строки TickTick в БД одной транзакцией: либо импорт целиком, либо ничего.
 * Повторный импорт пропускает задачи с уже известным taskId (колонка external_id).
 */
internal class TickTickImporter @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    private val groupDao = db.projectGroupDao()
    private val projectDao = db.projectDao()
    private val taskDao = db.taskDao()
    private val checklistDao = db.checklistDao()

    suspend fun import(rows: List<TickTickRow>, onProgress: (ImportProgress) -> Unit): ImportReport {
        val zone = time.zone()
        var failed = 0
        val tasks = rows.mapNotNull { row ->
            runCatching { TickTickRowMapper.map(row, zone) }.getOrElse {
                failed++
                null
            }
        }

        return db.withTransaction {
            val now = time.now().toEpochMilli()
            val lists = ListResolver(now)
            val ranks = ranksWithinList(tasks)
            val baseSortOrder = mutableMapOf<String, Int>()
            var added = 0
            var skipped = 0
            var checklistItems = 0

            tasks.forEachIndexed { index, task ->
                val projectId = lists.projectIdFor(task.listName, task.folderName)
                // Новые задачи встают после уже существующих в списке (важно при повторном импорте).
                val base = baseSortOrder.getOrPut(projectId) { taskDao.nextSortOrder(projectId) }
                val id = ids.newId()
                val entity = task.toEntity(id, projectId, base + ranks.getValue(task.externalId), now)
                val inserted = taskDao.insertOrIgnore(entity) != -1L
                if (inserted) {
                    added++
                    if (task.checklist.isNotEmpty()) {
                        checklistDao.insertAll(
                            task.checklist.mapIndexed { position, item ->
                                ChecklistItemEntity(
                                    id = ids.newId(),
                                    taskId = id,
                                    title = item.title,
                                    isDone = item.isDone,
                                    sortOrder = position,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            },
                        )
                        checklistItems += task.checklist.size
                    }
                } else {
                    skipped++
                }
                onProgress(ImportProgress(processed = index + 1, total = tasks.size))
            }

            // Подзадачи связываются вторым проходом: родитель может идти в файле после дочерней задачи.
            var linked = 0
            tasks.filter { it.parentExternalId != null }.forEach { task ->
                val childId = taskDao.getIdByExternalId(task.externalId)
                val parentId = taskDao.getIdByExternalId(task.parentExternalId!!)
                if (childId != null && parentId != null && childId != parentId) {
                    linked += taskDao.linkParent(childId, parentId)
                }
            }

            ImportReport(
                tasksAdded = added,
                tasksSkipped = skipped,
                listsCreated = lists.listsCreated,
                foldersCreated = lists.foldersCreated,
                checklistItems = checklistItems,
                subtasksLinked = linked,
                rowsFailed = failed,
            )
        }
    }

    /** Порядок TickTick (большие числа, меньше — выше) → позиции 0, 1, 2… внутри списка. Ключ — taskId. */
    private fun ranksWithinList(tasks: List<ImportedTask>): Map<String, Int> =
        buildMap {
            tasks.groupBy { it.listName.lowercase() }.values.forEach { listTasks ->
                listTasks.sortedWith(compareBy(nullsLast()) { it.order })
                    .forEachIndexed { rank, task -> put(task.externalId, rank) }
            }
        }

    private fun ImportedTask.toEntity(id: String, projectId: String, sortOrder: Int, now: Long) = TaskEntity(
        id = id,
        projectId = projectId,
        title = title,
        content = content,
        status = status.value,
        priority = priority.value,
        dueDate = dueDate,
        startDate = startDate,
        isAllDay = isAllDay,
        completedAt = completedAt,
        sortOrder = sortOrder,
        rrule = rrule,
        reminders = reminders,
        externalId = externalId,
        sourceData = sourceData,
        createdAt = createdAt ?: now,
        updatedAt = now,
    )

    /** Находит или создаёт папки и списки по имени. «Inbox» из TickTick — это наши «Входящие». */
    private inner class ListResolver(private val now: Long) {
        private val projectIds = mutableMapOf<String, String>()
        private val groupIds = mutableMapOf<String, String>()
        var listsCreated = 0
        var foldersCreated = 0

        suspend fun projectIdFor(listName: String, folderName: String?): String {
            if (listName.isEmpty() || listName.equals(TickTickFormat.INBOX, ignoreCase = true)) return inboxId()
            projectIds[listName]?.let { return it }
            val id = projectDao.getActiveByName(listName)?.id ?: run {
                val groupId = folderName?.let { groupIdFor(it) }
                val newId = ids.newId()
                projectDao.insert(
                    ProjectEntity(
                        id = newId,
                        groupId = groupId,
                        name = listName,
                        sortOrder = projectDao.nextSortOrder(groupId),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                listsCreated++
                newId
            }
            projectIds[listName] = id
            return id
        }

        private suspend fun inboxId(): String =
            projectIds.getOrPut(TickTickFormat.INBOX) { checkNotNull(projectDao.getInbox()) { "Нет «Входящих»" }.id }

        private suspend fun groupIdFor(name: String): String {
            groupIds[name]?.let { return it }
            val id = groupDao.getActiveByName(name)?.id ?: run {
                val newId = ids.newId()
                groupDao.insert(
                    ProjectGroupEntity(id = newId, name = name, sortOrder = groupDao.nextSortOrder(), createdAt = now, updatedAt = now),
                )
                foldersCreated++
                newId
            }
            groupIds[name] = id
            return id
        }
    }
}
