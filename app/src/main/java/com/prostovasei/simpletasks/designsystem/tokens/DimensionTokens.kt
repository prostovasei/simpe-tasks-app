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

    /** Место под эмодзи списка перед названием задачи и размер эмодзи (итерация 47; раньше — квадратик со значком). */
    const val LIST_SQUARE = 16
    const val LIST_GLYPH = 14

    /** Эмодзи списка на экране «Списки» и в заголовке списка (без подложки, проверка PM 0.47.0). */
    const val LIST_SQUARE_LARGE = 24
    const val LIST_GLYPH_LARGE = 20

    /** Плитка значка в окне выбора значка. */
    const val ICON_TILE = 44

    /**
     * Нижняя панель (итерация 44, решение PM: не парит, а закреплена внизу на всю ширину): высота, наибольшая ширина кнопки
     * вкладки, кнопка «+» (ширина и высота); [NAV_SHADOW] — тень шторки и плашки сетки.
     */
    const val NAV_BAR = 56
    const val NAV_TAB = 48
    const val NAV_ADD_W = 48
    const val NAV_ADD_H = 40
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
     * Нижняя панель виджета (итерация 25, вариант PM «А + Г»; итерация 43 — кнопки ниже и шире, как клавиша ввода, перехода
     * к фону нет): место под панелью в конце ленты, строка кнопок, кнопка переключателя «Все» / «Входящие» и поле капсулы.
     */
    const val WIDGET_TAIL = 60
    // 0.46.0 (проверка PM): нижняя строка ниже — 40 вместо 48, кнопки 30–32 вместо 36–40.
    const val WIDGET_BAR = 40
    const val WIDGET_SWITCH_W = 50
    const val WIDGET_SWITCH_H = 30
    const val WIDGET_SWITCH_PAD = 4

    /** Кнопка «+» в нижней строке виджета (итерация 43: 64×40; 0.46.0 — 64×32). */
    const val WIDGET_ADD_W = 64
    const val WIDGET_ADD_H = 32
    const val WIDGET_ADD_ICON = 22
    /** Иконки «Все» и «Входящие» в переключателе. */
    const val WIDGET_TAB_ICON = 22

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

    /**
     * Колонка дня и времени в плоских списках приложения (итерация 26). Итерация 41: 48 → 56 и растёт вместе с системным шрифтом
     * (умножается на его масштаб) — «Сегодня» на телефоне PM обрезалось до «Сегодн».
     */
    const val TIMELINE_DAY_COLUMN = 56
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

    /**
     * Кнопка «открыть приложение» в нижней строке виджета (итерация 43, вместо кнопки обновления из итерации 32): белая
     * зона 52×32 с галочкой значка приложения 24×20 (0.46.0: без капсулы, ниже) и щель до «+».
     */
    const val WIDGET_OPEN_W = 52
    const val WIDGET_OPEN_H = 32
    const val WIDGET_OPEN_ICON_W = 24
    const val WIDGET_OPEN_ICON_H = 20
    const val WIDGET_UPDATE_GAP = 8

    /** Режим выбора (итерация 40): кнопка ленты действий (ширина не меньше, высота) и высота верхней строки «Выбрано: N». */
    const val RIBBON_BUTTON = 76
    const val RIBBON_BUTTON_HEIGHT = 56
    const val SELECTION_BAR = 56

    /** Чип срока из названия под полем названия (итерация 41): высота, значок календаря, кружок с крестиком. */
    const val SMART_CHIP = 32
    const val SMART_CHIP_ICON = 18
    const val SMART_CHIP_CLOSE = 20
    const val SMART_CHIP_CLOSE_ICON = 12

    /** Корзина (итерация 39): строка с подписью «удалена …» выше обычной на это число dp. */
    /** Вид «Неделя» (итерация 44): наименьшая высота ячейки дня. */
    const val WEEKVIEW_CELL_MIN = 112

    /**
     * Вид «Неделя» в виджете (итерация 45, макет `VariantK1`; 0.46.0 — `VariantK2`, компактные строки): наименьшая строка задачи
     * (настоящая делит высоту ячейки поровну и обычно выше — ~20; по наименьшей считается, сколько строк влезает, решение PM — до 6) и промежуток
     * между строками, кружок и галочка в нём, строка даты, мини-календарь («пн…вс», день) и высота строки
     * недели, пока размер виджета неизвестен (по ней считается только число строк задач: сами строки делят высоту виджета поровну).
     */
    const val WIDGET_WEEK_TASK = 18
    const val WIDGET_WEEK_TASK_GAP = 2
    const val WIDGET_WEEK_CHECK = 13
    const val WIDGET_WEEK_TICK = 9
    const val WIDGET_WEEK_HEADER = 18
    const val WIDGET_WEEK_CAL_WEEKDAYS = 12
    const val WIDGET_WEEK_CAL_DAY = 14
    const val WIDGET_WEEK_ROW_DEFAULT = 140
    const val WIDGET_WEEK_ROW_MIN = 96

    /**
     * Вид «Неделя» в приложении (итерация 44; 0.46.0 — компактные строки, `VariantK2`): плашка задачи (она же зона нажатия кружка)
     * и промежуток между плашками, кружок и галочка в нём, строка даты, мини-календарь (строка недели — наибольшая и наименьшая,
     * «пн…вс», кружок «сегодня» — он же кружок у числа в строке даты).
     */
    const val WEEKVIEW_TASK = 22
    const val WEEKVIEW_TASK_GAP = 3
    const val WEEKVIEW_CHECK = 14
    const val WEEKVIEW_CHECK_ICON = 9
    const val WEEKVIEW_HEADER = 26
    const val WEEKVIEW_CAL_ROW = 28
    const val WEEKVIEW_CAL_ROW_MIN = 16
    const val WEEKVIEW_CAL_WEEKDAYS = 14
    const val WEEKVIEW_CAL_TODAY = 22

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
