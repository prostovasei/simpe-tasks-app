package com.prostovasei.simpletasks.domain.model

/** Режим фильтра недели: показать только отмеченные списки или скрыть отмеченные (#243). */
enum class WeekFilterMode { SHOW, HIDE }

/**
 * Фильтр вида «Неделя» по спискам (#243). [ids] — id отмеченных списков и [NO_LIST] («Без списка»: задачи с датой во «Входящих»);
 * ничего не отмечено — фильтра нет в любом режиме. Папки в хранилище не попадают: отметка папки — это отметки всех её списков.
 */
data class WeekFilter(val mode: WeekFilterMode = WeekFilterMode.SHOW, val ids: Set<String> = emptySet()) {
    val isActive: Boolean get() = ids.isNotEmpty()

    /** Попадает ли задача списка [projectId] (во «Входящих» — [isInbox]) в неделю. */
    fun allows(projectId: String, isInbox: Boolean): Boolean {
        if (!isActive) return true
        val marked = (if (isInbox) NO_LIST else projectId) in ids
        return if (mode == WeekFilterMode.SHOW) marked else !marked
    }

    fun encode(): String = "${mode.name}|${ids.sorted().joinToString(",")}"

    companion object {
        /** Пункт «Без списка» — задачи служебного списка «Входящие», у которых есть дата. */
        const val NO_LIST = "none"

        fun decode(value: String?): WeekFilter {
            if (value.isNullOrBlank()) return WeekFilter()
            val mode = WeekFilterMode.entries.find { it.name == value.substringBefore('|') } ?: WeekFilterMode.SHOW
            val ids = value.substringAfter('|', "").split(',').filter { it.isNotBlank() }.toSet()
            return WeekFilter(mode, ids)
        }
    }
}
