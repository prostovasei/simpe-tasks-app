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

/** Результат удаления задачи: [at] — момент, которым помечено всё удалённое (задача, подзадачи, чек-листы); с ним возвращают именно это. */
data class Deletion(val taskId: String, val at: Long)

/** Результат «Перенести на сегодня» (итерация 37): задачи такими, какими они были до переноса, — для «Отмены». */
data class Rescheduling(val previous: List<Task>) {
    val count: Int get() = previous.size
}

/** Результат восстановления из корзины (итерация 39): по одной записи на каждую вернувшуюся задачу — для «Отмены». */
data class Restoration(val deletions: List<Deletion>) {
    val count: Int get() = deletions.size
}
