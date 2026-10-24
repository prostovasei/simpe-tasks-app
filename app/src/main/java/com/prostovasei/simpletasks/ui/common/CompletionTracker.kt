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
 * Отметка задачи с отменой (SPEC.md §4.1, аудит N6). Итерация 27 (решение PM): выполненная не исчезает — остаётся
 * на своём месте до полуночи, с галочкой и зачёркнутая; повторное нажатие на галочку возвращает её.
 * - Обычная задача пишется в БД сразу; пока БД не ответила, она в [pending] — показывается выполненной.
 * - Повторяющаяся сначала показывается выполненной ([RECURRING_DELAY_MS] — время перехода), потом переезжает
 *   на следующий раз, а на её месте остаётся выполненный раз; подсказка «Следующий повтор: …».
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
     * Нажатие на галочку: [done] = false — отметить, [onCompleted] получает Snackbar с «Отменой»;
     * [done] = true — вернуть выполненную задачу (ещё отмечаемую — тоже).
     */
    fun toggle(taskId: String, recurring: Boolean, done: Boolean = false, onCompleted: (TaskListEffect.ShowUndo) -> Unit) {
        if (done || taskId in _pending.value) {
            undo(taskId)
            return
        }
        _pending.update { it + taskId }
        if (recurring) {
            timers[taskId] = scope.launch {
                delay(RECURRING_DELAY_MS)
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
        onCompleted(TaskListEffect.ShowUndo(taskId))
        writes[taskId] = scope.launch {
            results[taskId] = repository.complete(taskId)
            _pending.update { it - taskId }
        }
    }

    /**
     * Возвращает задачу как было. [taskId] — отмеченная задача или выполненный раз повторяющейся (его id другой).
     * Отмеченная в этом сеансе отменяется точно (повторяющаяся — на прежний срок); выполненная раньше —
     * снова становится активной. Повторяющаяся, отменённая до записи, в БД не менялась — возвращать нечего.
     */
    fun undo(taskId: String) {
        val timer = timers.remove(taskId)
        timer?.cancel()
        _pending.update { it - taskId }
        val key = results.entries.find { (id, result) -> id == taskId || (result as? Completion.Rescheduled)?.doneCopyId == taskId }?.key ?: taskId
        val write = writes.remove(key)
        if (timer != null && write == null) return
        scope.launch {
            write?.join()
            val result = results.remove(key)
            if (result != null) repository.undo(result) else repository.setCompleted(taskId, completed = false)
        }
    }

    private fun Completion.nextDueText(): DueText? = (this as? Completion.Rescheduled)?.next?.let {
        DueFormatter.format(it.due, it.start, time.today(), time.zone())
    }

    companion object {
        /** Повторяющаяся успевает показаться выполненной ([DONE_FADE_MS]), прежде чем переехать на следующий раз. */
        const val RECURRING_DELAY_MS = DONE_FADE_MS + 100L
    }
}
