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

    /** Квадратик списка «Входящие» (у него нет своего цвета). */
    val inboxList = ThemedColor(light = 0xFF3D8BFD, dark = 0xFF6AA8FF)

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

    /** Затемнение под всплывающим окном (выбор списка виджета). */
    val scrim = ThemedColor(light = 0x66000000, dark = 0x99000000)
}

/**
 * Готовые цвета для списков (выбор в меню «Цвет»). Хранятся в БД как HEX-строка — это данные пользователя,
 * а набор вариантов — часть дизайн-системы.
 */
object ListColorTokens {
    val presets: List<String> = listOf(
        "#E5484D", "#F76B15", "#FFC53D", "#46A758", "#12A594", "#3D8BFD", "#8E4EC6", "#D6409F", "#8D8D8D",
    )
}

/**
 * Цвета плашек приоритетов (SPEC.md §3). С итерации 46 (решение PM, «очень цветасто») — графит: высокий почти чёрный с белым текстом,
 * средний и низкий — серые, в тёмной теме наоборот. Текст подобран с контрастом не ниже 4.5:1.
 */
object PriorityTokens {
    val high = PriorityColorTokens(
        container = ThemedColor(light = 0xFF2E2F33, dark = 0xFFE4E5E7),
        content = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF16171D),
    )
    val medium = PriorityColorTokens(
        container = ThemedColor(light = 0xFFD0D1D4, dark = 0xFF4A4C51),
        content = ThemedColor(light = 0xFF16171D, dark = 0xFFF2F2F2),
    )
    val low = PriorityColorTokens(
        container = ThemedColor(light = 0xFFEAEBEC, dark = 0xFF2E3034),
        content = ThemedColor(light = 0xFF3A3C42, dark = 0xFFD0D1D4),
    )
    val none = PriorityColorTokens(
        container = ThemedColor(light = 0xFFEEEEEE, dark = 0xFF242424),
        content = ThemedColor(light = 0xFF424242, dark = 0xFFBDBDBD),
    )
}
/**
 * Круглая галочка виджета: приоритет — цветом обводки и лёгкой заливки (решение PM, редизайн виджета).
 * [ring] — обводка, [fill] — заливка: непрозрачный оттенок поверх плашки дня (полупрозрачный оттенок
 * виджеты рисуют неровно). У задач без приоритета заливки нет. [check] — галочка выполненной на кружке, залитом [ring]
 * (итерация 46: на светлом графите тёмной темы — тёмная, иначе белая).
 */
data class CheckTokens(val ring: ThemedColor, val fill: ThemedColor, val check: ThemedColor)

object WidgetCheckTokens {
    // Итерация 46 (вариант А, решение PM): приоритет — темнотой кружка, без цвета: высокий — чёрный (и толще, CHECKBOX_STROKE_HIGH),
    // средний — тёмно-серый, низкий — светло-серый, без приоритета — бледный. В тёмной теме наоборот. Заливки нет.
    val high = CheckTokens(ring = ThemedColor(light = 0xFF16171D, dark = 0xFFF2F2F2), fill = ThemedColor(light = 0x00000000, dark = 0x00000000), check = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF16171D))
    val medium = CheckTokens(ring = ThemedColor(light = 0xFF5F626B, dark = 0xFFB3B5BA), fill = ThemedColor(light = 0x00000000, dark = 0x00000000), check = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF16171D))
    val low = CheckTokens(ring = ThemedColor(light = 0xFF9A9DA4, dark = 0xFF7A7D84), fill = ThemedColor(light = 0x00000000, dark = 0x00000000), check = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF16171D))
    val none = CheckTokens(ring = ThemedColor(light = 0xFFC4C6CA, dark = 0xFF55575C), fill = ThemedColor(light = 0x00000000, dark = 0x00000000), check = ThemedColor(light = 0xFFFFFFFF, dark = 0xFFFFFFFF))
}

/**
 * Строки вида «Неделя» (итерация 45; итерация 46 — вариант 2 «Контрастный графит» из рефа PM): фон по приоритету — высокий почти чёрный
 * с белым текстом и кружком, средний — средне-серый, низкий — светло-серый, без приоритета — [ColorTokens.surface]; в тёмной теме наоборот.
 * У высокого и среднего свои цвета текста (`*Content`), времени (`*Time`) и просрочки (`*Overdue`, на тёмной строке — светло-красный).
 * Серая отметка выполненной задачи. Те же цвета у приложения и у виджета.
 */
object WeekRowTokens {
    val high = ThemedColor(light = 0xFF2E2F33, dark = 0xFFE4E5E7)
    val highContent = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF16171D)
    val highTime = ThemedColor(light = 0xFFD8D9DC, dark = 0xFF4B4E56)
    val highOverdue = ThemedColor(light = 0xFFFF8A80, dark = 0xFFC62828)
    val medium = ThemedColor(light = 0xFFD0D1D4, dark = 0xFF4A4C51)
    val mediumContent = ThemedColor(light = 0xFF16171D, dark = 0xFFF2F2F2)
    val mediumTime = ThemedColor(light = 0xFF4B4E56, dark = 0xFFC9CBCF)
    val mediumOverdue = ThemedColor(light = 0xFFC62828, dark = 0xFFFF8A80)
    val low = ThemedColor(light = 0xFFEAEBEC, dark = 0xFF2E3034)
    val doneMark = ThemedColor(light = 0xFFBDBDBD, dark = 0xFF5A5A5A)
}
