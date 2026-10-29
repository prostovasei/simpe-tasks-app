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

    /** Колокольчик напоминаний со значком в строке задачи (итерация 23). */
    const val REMINDER_ICON = 18
    const val PICKER_MAX_HEIGHT = 440

    /**
     * Нижняя панель виджета (итерация 25, вариант PM «А + Г»): высота перехода от прозрачного к фону, строка кнопок,
     * кнопка переключателя «Все» / «Входящие» и поле капсулы.
     */
    const val WIDGET_FADE = 104
    const val WIDGET_BAR = 58
    const val WIDGET_SWITCH_W = 46
    const val WIDGET_SWITCH_H = 42
    const val WIDGET_SWITCH_PAD = 4

    /** Кнопка «+» в нижней строке виджета. */
    const val WIDGET_ADD = 46
    const val WIDGET_ADD_ICON = 24
    /** Иконки «Все» и «Входящие» в переключателе. */
    const val WIDGET_TAB_ICON = 26

    /** Поля строки задачи внутри её плашки в виджете. */
    const val WIDGET_ROW_GAP = 4

    /** Подпись дня почти вплотную к плашке. */
    const val WIDGET_DAY_GAP = 2

    /** Поля ленты виджета от краёв. */
    const val WIDGET_EDGE = 6

    /** Шкала дня в виджете (итерация 20): поле ленты у левого и правого краёв — минимальное. */
    const val WIDGET_LIST_EDGE = 3

    /** Колонка времени слева и колонка шкалы с кружками. */
    const val WIDGET_TIME = 38

    /** Колонка дня и времени в плоских списках приложения (итерация 26): «Завтра» помещается целиком. */
    const val TIMELINE_DAY_COLUMN = 48
    const val WIDGET_RAIL = 26

    /** Высота строки: задача со временем или на весь день, период; отметка «сейчас». */
    const val WIDGET_ROW = 46
    const val WIDGET_PERIOD_ROW = 64
    const val WIDGET_NOW_ROW = 14

    /**
     * Кружок приоритета на шкале (итерация 27: 14 → 16; 27.1: 20 — решение PM, заметно крупнее), хвостики линии
     * у капсулы периода (= поле плашки — капсула ровно с плашку, решение PM), точка «сейчас», значки в плашке.
     */
    const val WIDGET_NODE = 20
    const val WIDGET_PERIOD_STUB = 3
    const val WIDGET_NOW_DOT = 7
    const val WIDGET_META_ICON = 18

    /** Белая галочка в залитом кружке выполненной задачи (итерация 27). */
    const val NODE_CHECK = 12

    /** Колокольчик напоминаний в плашке виджета (итерация 23). */
    const val WIDGET_REMINDER_ICON = 22

    /** Квадратик списка в плашке виджета и значок в нём (итерация 25 — крупнее, чем в приложении). */
    const val WIDGET_LIST_SQUARE = 18
    const val WIDGET_LIST_GLYPH = 14

    /** Щель между плашками — внутри строки (по 3 dp сверху и снизу), чтобы линия шкалы не рвалась. */
    const val WIDGET_CARD_INSET = 3
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
    const val TOOL_BUTTON = 50
    const val TOOL_ICON = 25

    /** Сетка недели: высота часа, колонка часов слева, шапка дней, крестик «убрать время». */
    const val WEEK_HOUR = 44
    const val WEEK_GUTTER = 26
    const val WEEK_HEADER = 36
    const val WEEK_CLEAR = 18

    /** Видимый крестик выбранного (итерация 26, вариант PM «4»): серая таблетка с белым ×; нажимать — по-прежнему по [WEEK_CLEAR]. */
    const val CLEAR_DOT = 14
    const val CLEAR_GLYPH = 10
    const val WEEK_KNOB = 10

    /**
     * Полоска напоминания поверх других приложений (T-69, вариант PM «В»): высота, круглые кнопки и значки в них,
     * поля от краёв экрана и между полосками стопки, на сколько смахнуть вправо, чтобы закрыть.
     */
    const val POPUP_STRIP = 68
    const val POPUP_BUTTON = 48
    const val POPUP_ICON = 22
    const val POPUP_EDGE = 12
    const val POPUP_GAP = 8
    const val POPUP_SWIPE = 56
    const val POPUP_UNDO_LINE = 3
    const val SHEET_REVEAL_BLUR = 8
    const val WIDGET_PROGRESS = 4

    /** Квадратная плитка дня в шторке (итерация 22): «чт / 22 / окт» и время. */
    const val DAY_TILE = 72

    /**
     * Поля даты и времени в шторке (итерации 33–34): высота строки и значок.
     * Окно даты: ячейка дня в блоке «2 недели», кружок дня, круглые стрелки листания и наименьший час сетки.
     */
    const val STRIP_HEIGHT = 54
    const val STRIP_ICON = 22
    const val DATE_CELL = 40
    const val DATE_DAY = 34
    const val DATE_ARROW = 28
    const val WEEK_HOUR_MIN = 32

    /** Ширина подписи «26 окт – 08 ноя» со стрелками в нижней строке окна даты при обычном шрифте (итерация 34); при крупном растёт. */
    const val DATE_RANGE_MIN = 150

    /** Наименьшая кнопка нижней строки окна даты: на узком экране с крупным шрифтом кнопки сжимаются, но не меньше. */
    const val DATE_BUTTON_MIN = 36

    /** Круглая отметка «выполнено» у названия задачи (0.31.2) и галочка в ней. */
    const val DONE_CIRCLE = 22
    const val DONE_CHECK = 14

    /** Окна шторки задачи (итерация 31): наибольшая ширина (планшет, телефон боком) и высота строки варианта. */
    const val DIALOG_MAX_WIDTH = 420
    const val DIALOG_ROW = 52

    /** Круглый значок над заголовком окна обновления (итерация 32) и сам значок. */
    const val DIALOG_BADGE = 48
    const val DIALOG_BADGE_ICON = 26

    /** Кнопка обновления в нижней строке виджета (итерация 32): сторона, значок в ней и щель до «+». */
    const val WIDGET_UPDATE = 44
    const val WIDGET_UPDATE_ICON = 22
    const val WIDGET_UPDATE_GAP = 8

    /** Режим выбора (итерация 40): кнопка ленты действий (ширина не меньше, высота) и высота верхней строки «Выбрано: N». */
    const val RIBBON_BUTTON = 76
    const val RIBBON_BUTTON_HEIGHT = 56
    const val SELECTION_BAR = 56

    /** Корзина (итерация 39): строка с подписью «удалена …» выше обычной на это число dp. */
    const val TRASH_ROW_EXTRA = 14

    /** Строка недавнего запроса в поиске (итерация 38): высота, значок «часы» слева, зона нажатия крестика справа. */
    const val RECENT_ROW = 48
    const val RECENT_ICON = 22
    const val RECENT_CLEAR_TOUCH = 48

    /** Подсказка «Отмена» (итерация 38): наименьшая высота, значок слева и зона нажатия кнопки. */
    const val SNACKBAR_MIN_HEIGHT = 56
    const val SNACKBAR_ICON = 22
    const val SNACKBAR_ACTION_MIN = 40

    /** Стрелка «свернуть» и значок «перенести на сегодня» у подписи «Просрочено» в виджете (итерация 37). */
    const val WIDGET_FOLD_ICON = 20
}
