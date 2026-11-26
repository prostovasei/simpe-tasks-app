package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.SortScope
import com.prostovasei.simpletasks.domain.model.TaskSort
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.domain.repository.TaskSortStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Сортировка экрана списка и «Входящих» (#357): перетаскивание, вопрос «Переключить на ручную?» и переключение.
 * [items] — задачи на экране, [setItems] и [setAsk] — обновление состояния экрана.
 */
class ListSorting(
    private val scope: CoroutineScope,
    private val tasks: TaskRepository,
    private val store: TaskSortStore,
    private val sortScope: SortScope,
) {
    fun observe() = store.observe(sortScope)

    /** true — намерение обработано. */
    fun handle(
        intent: TaskListIntent,
        items: () -> List<TaskItem>,
        sort: () -> TaskSort,
        today: () -> LocalDate?,
        setItems: (List<TaskItem>) -> Unit,
        setAsk: (Boolean) -> Unit,
    ): Boolean {
        when (intent) {
            is TaskListIntent.Reorder -> {
                // Сразу на экране, потом в БД: иначе строка на миг вернулась бы на старое место.
                val byId = items().associateBy { it.id }
                val moved = intent.ids.mapNotNull { byId[it] }
                setItems(moved + items().filter { it.id !in intent.ids })
                scope.launch { tasks.reorderTasks(intent.ids) }
            }
            TaskListIntent.AskManualSort -> if (store.skipManualQuestion) switch(items(), sort(), today()) else setAsk(true)
            is TaskListIntent.SwitchToManual -> {
                if (intent.skipQuestion) store.skipManualQuestion = true
                setAsk(false)
                switch(items(), sort(), today())
            }
            TaskListIntent.DismissManualSort -> setAsk(false)
            else -> return false
        }
        return true
    }

    /** Нынешний порядок становится ручным. */
    private fun switch(items: List<TaskItem>, sort: TaskSort, today: LocalDate?) {
        val order = sortedTasks(items, sort, today).let { it.active + it.done }.filterNot { it.isGhost }.map { it.id }
        scope.launch {
            tasks.reorderTasks(order)
            store.set(sortScope, TaskSort(SortBy.MANUAL))
        }
    }
}
