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

    /** Плавающая нижняя панель (итерация 13) и шторка новой задачи (итерация 16). */
    const val NAV = 22
}

/** Фиксированные размеры элементов в dp. */
object SizeTokens {
    const val HAIRLINE = 1

    /** Щель между плашками задач (итерация 12). */
    const val CARD_GAP = 2

    /** Квадратик списка перед названием задачи и значок в нём (итерации 13–14). */
    const val LIST_SQUARE = 16
    const val LIST_GLYPH = 12

    /** Квадратик списка на экране «Списки» и в заголовке списка. */
    const val LIST_SQUARE_LARGE = 24
    const val LIST_GLYPH_LARGE = 16

    /** Плитка значка в окне выбора значка. */
    const val ICON_TILE = 44

    /** Плавающая нижняя панель: высота, кнопка вкладки, кнопка «+», тень. */
    const val NAV_BAR = 64
    const val NAV_TAB = 48
    const val NAV_ADD = 52
    const val NAV_SHADOW = 10
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

    /** Вкладка списка внизу виджета: ширина, иконка и точка выбранной. */
    const val WIDGET_TAB = 28
    const val WIDGET_TAB_ICON = 20
    const val WIDGET_TAB_DOT = 4

    /** Поля строки задачи внутри её плашки в виджете. */
    const val WIDGET_ROW_GAP = 4

    /** Подпись дня почти вплотную к плашке. */
    const val WIDGET_DAY_GAP = 2

    /** Поля ленты виджета от краёв. */
    const val WIDGET_EDGE = 6

    /** Свободная полоска справа у плашек: в ней системная линия прокрутки списка, а не поверх времени. */
    const val WIDGET_SCROLL_GUTTER = 10
    const val COLOR_SWATCH = 40
    const val ICON = 24
    const val CHECKBOX_TOUCH = 40

    /** Частица «распада в пыль» при выполнении задачи (итерация 15). */
    const val DUST_PARTICLE = 3

    /** Пустой экран (итерация 15): пунктир подсказки, высота бледной плашки, стрелка к «+». */
    const val EMPTY_DASH = 6
    const val GHOST_ROW = 44
    const val GHOST_BAR = 8
    const val EMPTY_ARROW = 64

    /** Шторка новой задачи (итерация 16): ручка сверху, кнопки нижней панели и их значки. */
    const val SHEET_HANDLE_WIDTH = 36
    const val SHEET_HANDLE_HEIGHT = 4
    const val TOOL_BUTTON = 40
    const val TOOL_ICON = 20

    /** Сетка недели: высота часа, колонка часов слева, шапка дней, крестик «убрать время». */
    const val WEEK_HOUR = 44
    const val WEEK_GUTTER = 26
    const val WEEK_HEADER = 36
    const val WEEK_CLEAR = 18
    const val WEEK_KNOB = 10
}
