package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Отметка задачи с отменой (SPEC.md §4.1, аудит N6). Пока плашка «рассыпается в пыль» ([HIDE_DELAY_MS],
 * итерация 15), задача остаётся в списке отмеченной ([pending]), затем пропадает.
 * - Обычная задача пишется в БД сразу (виджет и закрытие приложения ничего не теряют).
 * - Повторяющаяся переезжает на следующий раз после анимации — иначе плашка перепрыгнет на новый день
 *   посреди «распада»; затем подсказка «Следующий раз: …».
 */
class CompletionTracker(
    private val scope: CoroutineScope,
    private val repository: TaskRepository,
    private val time: TimeProvider,
) {
    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    val pending: StateFlow<Set<String>> = _pending.asStateFlow()
    private val timers = mutableMapOf<String, Job>()
    private val writes = mutableMapOf<String, Job>()
    private val results = mutableMapOf<String, Completion>()

    /**
     * Отмечает задачу; [onCompleted] получает Snackbar с «Отменой». Повторное нажатие на ещё не скрытую
     * задачу — отмена.
     */
    fun toggle(taskId: String, recurring: Boolean, onCompleted: (TaskListEffect.ShowUndo) -> Unit) {
        if (taskId in _pending.value) {
            undo(taskId)
            return
        }
        _pending.update { it + taskId }
        if (recurring) {
            timers[taskId] = scope.launch {
                delay(HIDE_DELAY_MS)
                timers.remove(taskId)
                writes[taskId] = scope.launch {
                    val result = repository.complete(taskId)
                    results[taskId] = result
                    _pending.update { it - taskId }
                    onCompleted(TaskListEffect.ShowUndo(taskId, next = result.nextDueText()))
                }
            }
            return
        }
        timers[taskId] = scope.launch {
            delay(HIDE_DELAY_MS)
            _pending.update { it - taskId }
            timers.remove(taskId)
        }
        onCompleted(TaskListEffect.ShowUndo(taskId))
        writes[taskId] = scope.launch { results[taskId] = repository.complete(taskId) }
    }

    /**
     * Возвращает задачу как было — и пока она ещё видна, и после того как скрылась или переехала.
     * Повторяющаяся, отменённая до конца анимации, в БД не менялась — возвращать нечего.
     */
    fun undo(taskId: String) {
        timers.remove(taskId)?.cancel()
        _pending.update { it - taskId }
        val write = writes.remove(taskId)
        scope.launch {
            write?.join()
            results.remove(taskId)?.let { repository.undo(it) }
        }
    }

    private fun Completion.nextDueText(): DueText? = (this as? Completion.Rescheduled)?.next?.let {
        DueFormatter.format(it.due, it.start, time.today(), time.zone())
    }

    companion object {
        /** Длительность «распада в пыль» в [com.prostovasei.simpletasks.designsystem.components.TaskRow]. */
        const val HIDE_DELAY_MS = 1_100L
    }
}
