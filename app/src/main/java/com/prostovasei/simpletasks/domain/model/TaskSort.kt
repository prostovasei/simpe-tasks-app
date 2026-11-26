package com.prostovasei.simpletasks.domain.model

import java.text.Collator
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Вариант сортировки задач (#357). [TIME] — только «Предстоящее» (порядок внутри дня), [MANUAL] — только списки. */
enum class SortBy { DUE, PRIORITY, MANUAL, CREATED, TIME }

/**
 * Сортировка экрана (#357, решение PM 27.11.2026): вариант и направление. [descending] = false — «↑»: ранние, обычные, старые;
 * у приоритета и даты создания стандартное направление — «↓» (сначала важные, сначала новые).
 */
data class TaskSort(val by: SortBy, val descending: Boolean = defaultDescending(by)) {
    fun flipped(): TaskSort = if (by == SortBy.MANUAL) this else copy(descending = !descending)

    fun encode(): String = "${by.name}|${if (descending) 1 else 0}"

    companion object {
        val LIST_DEFAULT = TaskSort(SortBy.DUE, descending = false)
        val DAY_DEFAULT = TaskSort(SortBy.TIME, descending = false)

        fun defaultDescending(by: SortBy): Boolean = by == SortBy.PRIORITY || by == SortBy.CREATED

        fun decode(value: String?): TaskSort? {
            val by = SortBy.entries.find { it.name == value?.substringBefore('|') } ?: return null
            return TaskSort(by, value?.substringAfter('|', "") == "1")
        }
    }
}

/** Чья сортировка (#357): у каждого списка своя, у «Входящих» и «Предстоящего» — свои. */
sealed interface SortScope {
    val key: String
    val default: TaskSort

    data object Upcoming : SortScope {
        override val key = "upcoming"
        override val default = TaskSort.DAY_DEFAULT
    }

    data object Inbox : SortScope {
        override val key = "inbox"
        override val default = TaskSort.LIST_DEFAULT
    }

    data class Project(val id: String) : SortScope {
        override val key = "project:$id"
        override val default = TaskSort.LIST_DEFAULT
    }
}

/** Порядок задач списка (#357): по сроку, приоритету, дате создания или вручную; ничьи — по названию. */
object TaskSorter {
    private val names: Collator = Collator.getInstance(Locale.forLanguageTag("ru")).apply { strength = Collator.SECONDARY }

    /** День задачи (по началу, иначе сроку); null — без даты. */
    fun day(task: Task, zone: ZoneId): LocalDate? = when (val anchor = task.start ?: task.due) {
        is TaskDue.AllDay -> anchor.date
        is TaskDue.AtTime -> anchor.instant.atZone(zone).toLocalDate()
        null -> null
    }

    /** Время внутри дня: «весь день» раньше задач со временем. */
    private fun timeKey(task: Task): Long = ((task.start ?: task.due) as? TaskDue.AtTime)?.instant?.toEpochMilli() ?: Long.MIN_VALUE

    private val byName: Comparator<Task> = Comparator { a, b -> names.compare(a.title, b.title) }

    /** Срок по возрастанию; задачи без даты — в конце. */
    private fun byDue(zone: ZoneId): Comparator<Task> = compareBy<Task>(
        { day(it, zone) == null }, { day(it, zone) }, { timeKey(it) },
    )

    fun sort(tasks: List<Task>, sort: TaskSort, zone: ZoneId): List<Task> {
        val comparator: Comparator<Task> = when (sort.by) {
            SortBy.DUE, SortBy.TIME -> {
                val due = byDue(zone)
                Comparator { a, b ->
                    val undatedA = day(a, zone) == null
                    val undatedB = day(b, zone) == null
                    when {
                        undatedA != undatedB -> if (undatedA) 1 else -1
                        else -> (if (sort.descending) -due.compare(a, b) else due.compare(a, b)).takeIf { it != 0 } ?: byName.compare(a, b)
                    }
                }
            }
            SortBy.PRIORITY -> Comparator<Task> { a, b ->
                val p = a.priority.value.compareTo(b.priority.value)
                if (p != 0) (if (sort.descending) -p else p) else 0
            }.then(byDue(zone)).then(byName)
            SortBy.CREATED -> Comparator<Task> { a, b ->
                val c = a.createdAt.compareTo(b.createdAt)
                if (sort.descending) -c else c
            }.then(byName)
            SortBy.MANUAL -> compareBy<Task>({ it.sortOrder }, { it.createdAt })
        }
        return tasks.sortedWith(comparator)
    }
}
