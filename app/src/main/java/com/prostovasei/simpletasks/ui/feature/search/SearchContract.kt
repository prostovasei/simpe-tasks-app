package com.prostovasei.simpletasks.ui.feature.search

import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.ui.common.TaskItem

/** Найденная задача: [match] — строка описания или чек-листа, где нашлось слово (если не в названии). */
data class SearchResult(val item: TaskItem, val isDone: Boolean, val match: String?)

data class SearchState(
    val query: String = "",
    val active: List<SearchResult> = emptyList(),
    val done: List<SearchResult> = emptyList(),
    val isLoading: Boolean = true,
    /** Недавние запросы (итерация 38): показываются, пока поле пустое. */
    val recents: List<String> = emptyList(),
) {
    val found: Int get() = active.size + done.size
}

sealed interface SearchIntent {
    data class Query(val text: String) : SearchIntent

    /** Запрос закончен: нажали «поиск» на клавиатуре или открыли найденную задачу — он запоминается в недавних. */
    data object Submit : SearchIntent

    /** Нажали недавний запрос: он встаёт в поле и сразу ищется. */
    data class PickRecent(val text: String) : SearchIntent
    data class RemoveRecent(val text: String) : SearchIntent
    data object ClearRecents : SearchIntent

    /** Выполнить найденную задачу или вернуть выполненную. */
    data class Toggle(val taskId: String, val isDone: Boolean) : SearchIntent
}

/** Задача, подошедшая под запрос; [match] — строка описания или чек-листа, если слова нет в названии. */
data class SearchHit(val entry: SearchableTask, val match: String?)

/**
 * Поиск по названию, описанию и пунктам чек-листа, без учёта регистра (итерация 15). Возвращает активные
 * (по сроку, без срока — в конце) и выполненные — недавние сверху, не больше [DONE_LIMIT].
 */
fun searchTasks(all: List<SearchableTask>, query: String): Pair<List<SearchHit>, List<SearchHit>> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList<SearchHit>() to emptyList()
    val hits = all.mapNotNull { entry ->
        if (entry.task.title.contains(q, ignoreCase = true)) {
            SearchHit(entry, null)
        } else {
            (entry.task.content.lines() + entry.checklist).firstOrNull { it.contains(q, ignoreCase = true) }?.let { SearchHit(entry, it.trim()) }
        }
    }
    // «Не буду делать» (итерация 49) — тоже закрытые: вместе с выполненными, не среди активных.
    val (done, active) = hits.partition { it.entry.task.status != TaskStatus.ACTIVE }
    return active.sortedWith(compareBy<SearchHit>({ it.entry.task.due == null }, { it.entry.task.due?.sortKey() }, { it.entry.task.createdAt })) to
        done.sortedByDescending { it.entry.task.completedAt ?: it.entry.task.updatedAt }.take(DONE_LIMIT)
}

private fun TaskDue.sortKey(): Long = when (this) {
    is TaskDue.AllDay -> date.toEpochDay() * MILLIS_PER_DAY
    is TaskDue.AtTime -> instant.toEpochMilli()
}

private const val MILLIS_PER_DAY = 86_400_000L
const val DONE_LIMIT = 50
