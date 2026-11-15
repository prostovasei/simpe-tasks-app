package com.prostovasei.simpletasks.designsystem.tokens

/**
 * Цвет для светлой и тёмной темы в формате ARGB (0xAARRGGBB).
 * Чистый Kotlin без Compose: из этих значений строятся и AppTheme, и тема виджета Glance.
 */
data class ThemedColor(val light: Long, val dark: Long)

/** Пара цветов плашки приоритета: фон и текст. */
data class PriorityColorTokens(val container: ThemedColor, val content: ThemedColor)

/** Базовая палитра приложения. Единственный источник цветов (см. docs/ARCHITECTURE.md §1.5). */
object ColorTokens {
    /** Фон экранов и виджета: нейтральный светло-серый (итерация 45: без зелёного оттенка; итерация 26 — почти белый). */
    val background = ThemedColor(light = 0xFFF5F5F5, dark = 0xFF121212)

    /** Белые плашки задач, меню, окна и нижняя панель поверх фона. */
    val surface = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF1E1E1E)
    val textPrimary = ThemedColor(light = 0xFF16171D, dark = 0xFFECECEC)
    val textSecondary = ThemedColor(light = 0xFF62656E, dark = 0xFFA3A3A3)

    /** Акцент (итерация 45, решение PM): чёрный; в тёмной теме — белый, иначе его не видно. До этого — зелёный. */
    val accent = ThemedColor(light = 0xFF16171D, dark = 0xFFF2F2F2)

    /** Бледный акцент: подсветка найденного в поиске (итерация 15), «+N» и выбранная неделя календаря — нейтральный серый. */
    val accentSoft = ThemedColor(light = 0xFFEAEAEA, dark = 0xFF2E2E2E)
    val onAccent = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF16171D)

    /** Значок поверх квадратика цвета списка (итерация 14). */
    val onListSquare = ThemedColor(light = 0xFFFFFFFF, dark = 0xFFFFFFFF)


    /** Рамка пустой галочки без приоритета и неактивные иконки нижней панели. */
    val inactive = ThemedColor(light = 0xFFA9A9A9, dark = 0xFF636363)
    val overdue = ThemedColor(light = 0xFFC62828, dark = 0xFFFF6B6B)
    val divider = ThemedColor(light = 0xFFE2E2E2, dark = 0xFF2E2E2E)


    /**
     * Мягкая подсветка выбранного (итерация 26, вариант PM «А»): выбранные плитки дней, кнопки шторки
     * и переключатель виджета — светлая подложка вместо сплошной заливки. С итерации 45 — нейтральный серый.
     */
    val selected = ThemedColor(light = 0xFFE8E8E8, dark = 0xFF2C2C2C)

    /** Опасные действия (удаление). Совпадает с [overdue], но означает другое — отдельная роль. */
    val danger = ThemedColor(light = 0xFFC62828, dark = 0xFFFF6B6B)

    /** Доходы в списке операций (#245): сумма со знаком «+». */
    val income = ThemedColor(light = 0xFF1F8F4E, dark = 0xFF4CC38A)

    /** Затемнение под всплывающим окном (выбор списка виджета). */
    val scrim = ThemedColor(light = 0x66000000, dark = 0x99000000)
}

/**
 * Цвета плашек приоритетов (SPEC.md §3). Итерация 47 (решение PM): уровней два — высокий (фиолетовый) и обычный.
 * Текст подобран с контрастом не ниже 4.5:1.
 */
object PriorityTokens {
    val high = PriorityColorTokens(
        container = ThemedColor(light = 0xFFEFE4FA, dark = 0xFF3A2A52),
        content = ThemedColor(light = 0xFF6B32A8, dark = 0xFFD4B8F5),
    )
    val none = PriorityColorTokens(
        container = ThemedColor(light = 0xFFEEEEEE, dark = 0xFF242424),
        content = ThemedColor(light = 0xFF424242, dark = 0xFFBDBDBD),
    )
}
/**
 * Круглая галочка виджета: приоритет — цветом обводки и лёгкой заливки (решение PM, редизайн виджета).
 * [ring] — обводка, [fill] — заливка: непрозрачный оттенок поверх плашки дня (полупрозрачный оттенок
 * виджеты рисуют неровно). У задач без приоритета заливки нет.
 */
data class CheckTokens(val ring: ThemedColor, val fill: ThemedColor, val done: ThemedColor)

/**
 * Итерация 47 (решение PM): высокий приоритет — фиолетовый (до этого красный; средний и низкий убраны). [CheckTokens.done] — заливка
 * кружка выполненной задачи: у высокого — тусклый фиолетовый, у обычного — цвет кольца.
 */
object WidgetCheckTokens {
    val high = CheckTokens(
        ring = ThemedColor(light = 0xFF8E4EC6, dark = 0xFFB48AE8),
        fill = ThemedColor(light = 0x00000000, dark = 0x00000000),
        done = ThemedColor(light = 0xFFCDB3E6, dark = 0xFF5E4A7A),
    )
    val none = CheckTokens(
        ring = ThemedColor(light = 0xFF8B949E, dark = 0xFF7D8590),
        fill = ThemedColor(light = 0x00000000, dark = 0x00000000),
        done = ThemedColor(light = 0xFF8B949E, dark = 0xFF7D8590),
    )
}

/**
 * Строки вида «Неделя» (итерация 45, макет `VariantK2`; итерация 47 — два приоритета): бледный фон высокого приоритета
 * (без приоритета — [ColorTokens.surface]) и серая отметка выполненной задачи без приоритета. Те же цвета у приложения и у виджета.
 */
object WeekRowTokens {
    val high = ThemedColor(light = 0xFFF1E8FA, dark = 0xFF2E2440)
    val doneMark = ThemedColor(light = 0xFFBDBDBD, dark = 0xFF5A5A5A)
}

/** Согласованный вариант А, #247: только выбранные экраны задач и виджеты. */
object TaskCanvasTokens {
    /** Неделя, #249: высокий приоритет и его выполненное состояние. */
    val priority = PriorityTokens.high.content
    val priorityDone = ThemedColor(light = 0xFFDED0EC, dark = 0xFFDED0EC)
    val done = ThemedColor(light = 0xFFC9CBD0, dark = 0xFFC9CBD0)
    const val DONE_EMOJI_ALPHA = 0.35f
    val background = ThemedColor(light = 0xFFFFFFFF, dark = 0xFFFFFFFF)
    val divider = ThemedColor(light = 0xFFE8E8EA, dark = 0xFFE8E8EA)
}
