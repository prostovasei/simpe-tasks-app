package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.domain.repository.ChecklistRepository
import javax.inject.Inject

class RoomChecklistRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : ChecklistRepository {

    private val dao = db.checklistDao()

    override suspend fun addItem(taskId: String, title: String): String {
        require(title.isNotBlank()) { "Пункт чек-листа не может быть пустым" }
        val id = ids.newId()
        val now = nowMillis()
        db.withTransaction {
            dao.insert(
                ChecklistItemEntity(
                    id = id,
                    taskId = taskId,
                    title = title,
                    sortOrder = dao.nextSortOrder(taskId),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return id
    }

    override suspend fun renameItem(itemId: String, title: String) {
        require(title.isNotBlank()) { "Пункт чек-листа не может быть пустым" }
        dao.rename(itemId, title, nowMillis())
    }

    override suspend fun setDone(itemId: String, done: Boolean) {
        dao.setDone(itemId, done, nowMillis())
    }

    override suspend fun deleteItem(itemId: String) {
        dao.softDelete(itemId, nowMillis())
    }

    override suspend fun reorder(taskId: String, orderedItemIds: List<String>) {
        val now = nowMillis()
        db.withTransaction {
            // Игнорируем чужие и удалённые пункты: порядок меняется только внутри этой задачи.
            val own = dao.getActiveIds(taskId).toSet()
            orderedItemIds.filter { it in own }.forEachIndexed { index, id ->
                dao.setSortOrder(id, index, now)
            }
        }
    }

    private fun nowMillis() = time.now().toEpochMilli()
}
