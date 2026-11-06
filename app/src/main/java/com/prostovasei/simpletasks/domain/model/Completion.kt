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

/** Что делать с незакрытыми повторами перед будущим разом, когда его отмечают «не буду делать» (итерация 49, как в TickTick). */
enum class EarlierRepeats {
    /** Пропустить: разы исчезают без записи. */
    SKIP,

    /** Выполнить: каждый раз становится выполненным. */
    COMPLETE,
}

/**
 * Итог «Не буду делать» (итерация 49) — с ним «Отмена» возвращает всё как было. [previous] — задача до изменения; [closedWhole] —
 * закрыта сама задача (обычная или последний раз серии), иначе серия переехала, а раз записан отдельной задачей. [copyIds] — созданные
 * записи (раз «не буду делать» и выполненные разы перед будущим); [checkedItemIds] — пункты чек-листа серии, с которых сняли галочки;
 * [occurrence] — дата закрытого раза; [skipped] и [completed] — сколько незакрытых повторов перед ним пропущено или выполнено.
 */
data class WontDo(
    val taskId: String,
    val previous: Task,
    val closedWhole: Boolean,
    val copyIds: List<String> = emptyList(),
    val checkedItemIds: List<String> = emptyList(),
    val occurrence: TaskDue? = null,
    val skipped: Int = 0,
    val completed: Int = 0,
)
