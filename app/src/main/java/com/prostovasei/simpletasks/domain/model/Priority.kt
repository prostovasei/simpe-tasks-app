package com.prostovasei.simpletasks.domain.model

/**
 * Приоритет задачи. [value] — значение в БД и в CSV TickTick (SPEC.md §2.3).
 * Итерация 47 (решение PM): уровней два — высокий и обычный. Средний TickTick (3) читается как высокий, низкий (1) — как обычный;
 * миграция БД 2 → 3 переписала старые значения, импорт делает то же через [fromValue].
 */
enum class Priority(val value: Int) {
    NONE(0),
    HIGH(5),
    ;

    companion object {
        /** Средний (3) TickTick — высокий, низкий (1) и неизвестные — обычный. */
        fun fromValue(value: Int): Priority = if (value >= MEDIUM_TICKTICK) HIGH else NONE

        /** Средний приоритет TickTick: с него и выше — высокий. */
        const val MEDIUM_TICKTICK = 3
    }
}
