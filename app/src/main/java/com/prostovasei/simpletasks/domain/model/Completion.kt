package com.prostovasei.simpletasks.domain.model

/** Результат отметки задачи — с ним можно отменить отметку. */
sealed interface Completion {
    val taskId: String

    /** Обычная задача (или последний повтор) стала выполненной. */
    data class Done(override val taskId: String) : Completion

    /**
     * Повторяющаяся задача перенесена на следующий раз ([next]). Выполненный раз сохранён отдельной
     * выполненной задачей [doneCopyId] (как в TickTick: история и резервная копия его видят).
     * [checkedItemIds] — пункты чек-листа, с которых сняли галочки.
     */
    data class Rescheduled(
        override val taskId: String,
        val previous: Task,
        val next: Task,
        val doneCopyId: String,
        val checkedItemIds: List<String>,
    ) : Completion
}
