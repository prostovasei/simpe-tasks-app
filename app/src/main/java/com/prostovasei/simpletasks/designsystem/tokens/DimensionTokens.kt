package com.prostovasei.simpletasks.designsystem.tokens

/** Шкала отступов в dp. */
object SpacingTokens {
    const val XXS = 4
    const val XS = 8
    const val SM = 12
    const val MD = 16
    const val LG = 24
    const val XL = 32
}

/** Радиусы скругления в dp. */
object RadiusTokens {
    const val SM = 4
    const val MD = 8
    const val LG = 12
    const val XL = 16
}

/** Фиксированные размеры элементов в dp. */
object SizeTokens {
    const val HAIRLINE = 1
    const val CHECKBOX = 18
    const val CHECKBOX_STROKE = 2
    const val SWATCH = 32
    const val PROGRESS_HEIGHT = 6
    const val LIST_DOT = 10
    const val META_ICON = 14
    const val PICKER_MAX_HEIGHT = 440

    /** Ширина галочки в виджете Glance — под неё отступ у будущих повторов. */
    const val WIDGET_CHECKBOX = 32

    /** Нижняя строка виджета: список, дата и «+». */
    const val WIDGET_BAR = 40

    /** Кнопка «+» в нижней строке виджета. */
    const val WIDGET_ADD = 30
    const val WIDGET_ADD_ICON = 20

    /** Отступ между строками задач в блоке дня виджета. */
    const val WIDGET_ROW_GAP = 3

    /** Подпись дня почти вплотную к плашке. */
    const val WIDGET_DAY_GAP = 2

    /** Поля ленты виджета от краёв. */
    const val WIDGET_EDGE = 6
    const val COLOR_SWATCH = 40
    const val ICON = 24
    const val CHECKBOX_TOUCH = 40
}
