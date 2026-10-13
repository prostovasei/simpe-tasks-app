package com.prostovasei.simpletasks.domain.model

/** Статус задачи. [value] — значение в БД (SPEC.md §2.3). */
enum class TaskStatus(val value: Int) {
    ACTIVE(0),
    COMPLETED(2),
    ;

    companion object {
        /** 0 — активна; любое другое значение (в TickTick 1 и 2) — выполнена. */
        fun fromValue(value: Int): TaskStatus = if (value == ACTIVE.value) ACTIVE else COMPLETED
    }
}
