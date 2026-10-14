package com.prostovasei.simpletasks.ui.common

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
 * Отметка задачи с отменой (SPEC.md §4.1, аудит N6): статус пишется в БД сразу (виджет и закрытие приложения
 * ничего не теряют), а на экране задача остаётся зачёркнутой [HIDE_DELAY_MS] мс.
 */
class CompletionTracker(
    private val scope: CoroutineScope,
    private val repository: TaskRepository,
) {
    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    val pending: StateFlow<Set<String>> = _pending.asStateFlow()
    private val timers = mutableMapOf<String, Job>()

    /** Отмечает задачу. Повторное нажатие на ещё не скрытую задачу — отмена. Возвращает true, если задача отмечена. */
    fun toggle(taskId: String): Boolean {
        if (taskId in _pending.value) {
            undo(taskId)
            return false
        }
        _pending.update { it + taskId }
        scope.launch { repository.setCompleted(taskId, completed = true) }
        timers[taskId] = scope.launch {
            delay(HIDE_DELAY_MS)
            _pending.update { it - taskId }
            timers.remove(taskId)
        }
        return true
    }

    /** Возвращает задачу в активные — и пока она ещё видна, и после того как скрылась. */
    fun undo(taskId: String) {
        timers.remove(taskId)?.cancel()
        _pending.update { it - taskId }
        scope.launch { repository.setCompleted(taskId, completed = false) }
    }

    companion object {
        const val HIDE_DELAY_MS = 2_500L
    }
}
