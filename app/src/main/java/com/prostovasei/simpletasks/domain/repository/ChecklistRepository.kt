package com.prostovasei.simpletasks.domain.repository

interface ChecklistRepository {
    /** Добавляет пункт в конец чек-листа. Возвращает id пункта. */
    suspend fun addItem(taskId: String, title: String): String

    suspend fun renameItem(itemId: String, title: String)

    suspend fun setDone(itemId: String, done: Boolean)

    suspend fun deleteItem(itemId: String)

    /** Задаёт порядок пунктов по списку id (позиция в списке = новый порядок). */
    suspend fun reorder(taskId: String, orderedItemIds: List<String>)
}
