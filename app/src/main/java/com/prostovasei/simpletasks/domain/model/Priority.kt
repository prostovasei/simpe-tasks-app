package com.prostovasei.simpletasks.domain.model

/** Приоритет задачи. [value] — значение в БД и в CSV TickTick (SPEC.md §2.3). */
enum class Priority(val value: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(3),
    HIGH(5),
    ;

    companion object {
        fun fromValue(value: Int): Priority = entries.firstOrNull { it.value == value } ?: NONE
    }
}
