package com.prostovasei.simpletasks.domain.model

/** Статус задачи. [value] — значение в БД, совпадает с TickTick. */
enum class TaskStatus(val value: Int) {
    ACTIVE(0),
    COMPLETED(2),
    ABANDONED(-1),
    ;

    companion object {
        /** 0 — активна, -1 — отменена, остальное (в TickTick 1 и 2) — выполнена. */
        fun fromValue(value: Int): TaskStatus = when (value) {
            ACTIVE.value -> ACTIVE
            ABANDONED.value -> ABANDONED
            else -> COMPLETED
        }
    }
}
