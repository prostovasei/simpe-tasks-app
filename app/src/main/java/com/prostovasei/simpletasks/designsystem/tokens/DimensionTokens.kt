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
    /** Итерация 55: «+» посередине панели шире — 72×44 (было 48×40). */
    const val NAV_ADD_W = 72
    const val NAV_ADD_H = 44

    /** Итерация 55: вкладка панели как в нижней строке виджета — плашка 72×40, у выбранной фон `selected`. */
    const val NAV_TAB_W = 72
    const val NAV_TAB_H = 40

    /** Точка под значком «Предстоящее / Неделя»: по удержанию есть меню. */
    const val NAV_HINT_DOT = 4

    /** Поле «Поиск задач» на «Списках» (итерация 55). */
    const val SEARCH_ENTRY = 48

    /** Итерация 56: строка карточки «Списков» — не ниже. */
    const val LIST_ROW = 52

    /** Итерация 56: кнопка строки значков «Списков» (поиск, папка, список, настройки) — высота. */
    const val LISTS_TOOL = 44

    /** Итерация 56: плашка эмодзи слева от названия в окне создания списка и папки. */
    const val EMOJI_BUTTON = 56

    /** Итерация 56: эмодзи быстрого выбора в окне создания. */
    const val EMOJI_QUICK = 38

    /** Итерация 56: значок-стрелка на плашке эмодзи. */
    const val EMOJI_BUTTON_MARK = 20

    /**
     * Финансы, этап 1 (#221, макеты design/approved/217): верхний бар модулей — высота строки, кнопка модуля (ширина и высота),
     * значок в ней; карточка счёта в ряду, когда счетов больше трёх; маленький «+» у заголовка «Счета»; значок пустого экрана;
     * строка поля в окне счёта и кнопка «×» окна.
     */
    const val MODULE_BAR = 48
    const val MODULE_BUTTON_W = 44
    const val MODULE_BUTTON_H = 36
    const val MODULE_ICON = 20
    const val ACCOUNT_TILE = 108
    const val ACCOUNT_ADD = 32
    const val ACCOUNT_ADD_ICON = 18
    const val FINANCE_EMPTY_ICON = 40
    const val FINANCE_FIELD = 48
    const val FINANCE_CLOSE = 36
    const val FINANCE_ACTION = 46

    /** Финансы, этап 2 (#225): кнопка категории и часть переключателя «Расход / Доход / Перевод» — высота не меньше; строка операции в списке. */
    const val FINANCE_CHIP = 36

    /** Финансы, #237: плитка категории-эмодзи в окне операции, значок категории в списке, ширина боковых значков нижней строки окна (назад, корзина). */
    const val FINANCE_EMOJI = 44

    /** Единая нижняя панель (#241, решение PM 13.11.2026): высота 44, кнопки «+» и модуля 72×34, ширина кнопки страницы, обводка кнопки модуля. */
    const val APP_BAR = 44
    const val APP_BAR_BUTTON = 34
    const val APP_BAR_PAGE_W = 64
    const val APP_MODULE_TAB_W = 48
    const val APP_BAR_OUTLINE = 2
    const val FINANCE_MARK = 28
    /** Серый кружок «?» у операции без категории (#245). */
    const val FINANCE_UNKNOWN_MARK = 26
    const val FINANCE_BAR_SIDE = 56
    const val OPERATION_ROW = 52

    /**
     * Раздел «Здоровье» (#257, макеты design/approved/257): высота строки барабана и ширина барабанов «кг» / «десятые» / «рост», высота графика,
     * поле справа под подписи оси, радиусы точек, толщина линии, ячейка календаря, высота подсказки точки.
     */
    const val HEALTH_DRUM_ITEM = 40
    const val HEALTH_DRUM_WHOLE = 84
    const val HEALTH_DRUM_TENTH = 56
    const val FINANCE_CHART_HEIGHT = 138
    const val HEALTH_CHART_HEIGHT = 190
    const val HEALTH_CHART_AXIS = 40
    const val HEALTH_POINT = 3
    const val HEALTH_POINT_LAST = 5
    const val HEALTH_LINE = 2
    const val HEALTH_CAL_CELL = 40
    const val HEALTH_TIP_HEIGHT = 40
    const val HEALTH_TIP_WIDTH = 104
    const val HEALTH_DELTA_WIDTH = 48

    /**
     * Бюджет (#327), dp. [LIMIT_BAR] — толстая полоса лимита вверху «Финансов» (вариант Г); [BUDGET_BAR] — полоска строки окна «Бюджет»;
     * [BUDGET_MARK] — ширина отметки «сегодня», [BUDGET_MARK_OVER] — насколько она выступает над полоской; [BUDGET_TOTAL_RULE] — линия под итогом.
     */
    const val LIMIT_BAR = 26

    /** Хаб «Главная» (#329), dp: минимальная высота карточки новости в сетке 2 × 2 и точка страницы. */
    const val HUB_NEWS_CARD = 76
    const val HUB_DOT = 6
    const val BUDGET_BAR = 6
    const val BUDGET_MARK = 2
    const val BUDGET_MARK_OVER = 4
    const val BUDGET_TOTAL_RULE = 2

    /**
     * Окно голосового ввода (#305): высота как у окна новой задачи без срока — не растёт от клавиатуры и длины текста;
     * [VOICE_TIDE_PERCENT] — доля высоты окна под «водой» снизу; [VOICE_TIDE_RISE] — на сколько dp голос поднимает воду.
     */
    const val VOICE_HEIGHT = 212
    const val VOICE_TIDE_PERCENT = 58
    const val VOICE_TIDE_RISE = 16

    /** #310: строка задачи в списке над окном голоса, значок «×», кружок с числом задач на галочке. */
    const val VOICE_ROW = 36
    const val VOICE_REMOVE_ICON = 18
    const val VOICE_BADGE = 20
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
    const val WIDGET_COMPACT_BAR_THRESHOLD = 232
    const val WIDGET_FINANCE_CHART_COMPACT = 60
    const val WIDGET_BAR = 48

    /** Кнопка «+» в нижней строке виджета (итерация 43: 64×40; 0.46.0 — 64×32; 0.57 — плитка, той же ширины плитка даты, высота — у всех плиток строки). */
    const val WIDGET_ADD_W = 64
    const val WIDGET_ADD_H = 32
    const val WIDGET_ADD_ICON = 22
    /** Иконки «Все» и «Входящие» в переключателе. */
    const val WIDGET_TAB_ICON = 24

    /** Итерация 57: место значка в белом блоке вкладок нижней строки виджета (ширина; высота — [WIDGET_ADD_H]). */
    const val WIDGET_TAB_CELL = 36
    const val WIDGET_MODULE_COMPACT_CELL = 31
    const val WIDGET_MODULE_COMPACT_ADD = 44
    const val WIDGET_MODULE_DATE_W = 48
    const val WIDGET_OPEN_GAP_MIN = 16
    const val WIDGET_MODULE_BAR_THRESHOLD = 320

    /**
     * Микрофон рядом с «+» на вкладке «Задачи» (#95, вариант М1): «+» сужается до 40, микрофон 40, зазор 4 — вместе 84,
     * как дата с кнопкой обновления справа, вкладки остаются по центру. В компактной панели «+» прежний, микрофон — если влезает (К2).
     */
    const val WIDGET_VOICE_ADD_W = 40
    const val WIDGET_VOICE_W = 40
    const val WIDGET_VOICE_GAP = 4

    /** Итерация 57: поля по бокам внутри блока вкладок. */

    /** Итерация 57: дата в плитке — число и столбик «ВТ» / «ноя» (размеры шрифта, sp), промежуток между ними. */
    const val WIDGET_DATE_SMALL_SP = 8.5f
    const val WIDGET_DATE_GAP = 3

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
    /** Колонка кружка: кружок 20 dp и по 8 dp слева и справа (#331, решение PM 25.11.2026; было 26). */
    const val WIDGET_RAIL = 36

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

    /** Белая полоска (итерация 53, T-118): тень, чтобы не сливалась со светлым приложением под ней. */
    const val POPUP_SHADOW = 6

    /** Нажатая «Отложить» (меню открыто): белый зазор и чёрное кольцо вокруг кнопки. */
    const val POPUP_PRESSED_GAP = 3
    const val POPUP_PRESSED_RING = 2

    /** Меню «Отложить» над полоской: строка варианта. */
    const val POPUP_MENU_ROW = 48
    const val SHEET_REVEAL_BLUR = 8
    const val WIDGET_PROGRESS = 1

    /**
     * #314, анимации окна дня, dp. [DAY_CASCADE_RISE] — откуда поднимается строка в каскаде; [DAY_BOUNCE] — подскок окна
     * в «Празднике дня»; [DAY_SPARK] — искра, [DAY_SPARK_NEAR]…[DAY_SPARK_FAR] — дальность полёта, [DAY_SPARK_LIFT] — сколько
     * искры уходят вверх; [DAY_STRETCH] — предел растяжения резинки, [DAY_EXPAND_DRAG] — сколько протянуть вверх, чтобы окно
     * развернулось на весь экран.
     */
    const val DAY_CASCADE_RISE = 16
    const val DAY_BOUNCE = 8
    const val DAY_SPARK = 7
    const val DAY_SPARK_NEAR = 50
    const val DAY_SPARK_FAR = 140
    const val DAY_SPARK_LIFT = 40
    const val DAY_STRETCH = 120
    const val DAY_EXPAND_DRAG = 96

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
     * Итерация 48 (макет `week_tune.py`): строка даты — по высоте текста, [WIDGET_WEEK_HEADER] — её наименьшая высота; поле над датой
     * [WIDGET_WEEK_CELL_TOP], над верхним рядом [WIDGET_WEEK_TOP]; [WIDGET_WEEK_INLINE_GAP] — между эмодзи и временем и от времени до края
     * плашки; день календаря крупнее. Итерация 50 (макет `iteration50.py`): [WIDGET_WEEK_HIT] — невидимая зона «выполнить» поверх
     * кружка и начала названия на всю высоту плашки; кружок и название не сдвигаются.
     */
    const val WIDGET_WEEK_TASK = 18
    const val WIDGET_WEEK_HIT = 40
    const val WIDGET_WEEK_TASK_GAP = 2
    const val WIDGET_WEEK_CHECK = 14
    const val WIDGET_WEEK_TICK = 9
    const val WIDGET_WEEK_HEADER = 12
    const val WIDGET_WEEK_CELL_TOP = 1
    const val WIDGET_WEEK_TOP = 2
    const val WIDGET_WEEK_INLINE_GAP = 2

    /**
     * #313 «Всё листается»: строка задачи в списке дня (с промежутком 2 dp сверху), строка недели и названия месяца в ленте
     * календаря, затухание края списка; [WIDGET_WEEK_FILL_SLACK] — запас пустого места под задачами: рабочий стол сообщает
     * высоту виджета больше настоящей.
     */
    const val WIDGET_WEEK_ITEM = 22
    const val WIDGET_WEEK_CAL_ROW = 20
    const val WIDGET_WEEK_CAL_MONTH = 14
    const val WIDGET_WEEK_FADE = 8
    const val WIDGET_WEEK_FILL_SLACK = 8
    const val WEEK_MONTH_INLINE_CELL = 26
    const val WEEK_MONTH_INLINE_TEXT = 9
    const val WIDGET_WEEK_CAL_TEXT = 11
    const val WIDGET_WEEK_ROW_DEFAULT = 140
    const val WIDGET_WEEK_ROW_MIN = 96

    /**
     * Вид «Неделя» в приложении (итерация 44; 0.46.0 — компактные строки, `VariantK2`): плашка задачи (она же зона нажатия кружка)
     * и промежуток между плашками, кружок и галочка в нём, строка даты, мини-календарь (строка недели — наибольшая и наименьшая,
     * «пн…вс», кружок «сегодня» — он же кружок у числа в строке даты).
     */
    /** #253: минимальное поле и равные зазоры; ширина времени измеряется по HH:mm. */
    const val WEEK_EDGE = 2
    const val WEEK_CONTENT_GAP = 2

    /** Отступ слева и справа от кружка в списках задач на холсте (#331, решение PM 25.11.2026: 8 dp). */
    const val TIMELINE_CHECK_GAP = 8

    /**
     * Строка задачи (#334): кружок 18 → 10 → эмодзи списка 16 → 8 → название 14 sp полужирное → колокольчик 14 → 8 → время 13 sp.
     * Подпись дня и числа задач — 12 sp, полоса прогресса — 1 dp.
     */
    const val TASK_CHECK = 18

    /**
     * Плавающая панель (#336): капсула высотой 36 — кнопки 30 и поля 3, отступ от края экрана 12, значок 18, дата «25» 16 sp.
     * Выбор раздела — плитки 3 в ряд высотой 76.
     */
    const val NAV_BUTTON = 34
    const val NAV_PAD = 3
    const val NAV_EDGE = 12
    /** #338: место под капсулой в конце списков, которые идут под неё: 40 + 12 + 8. */
    const val NAV_BAR_SPACE = 60
    const val NAV_ICON = 20
    /** #338: «+» без круга — крупнее значков. */
    const val NAV_PLUS_ICON = 26
    /** #338: значки подписей полосы лимита в хабе. */
    const val LIMIT_NOTE_ICON = 14
    /** #340: привычки — круг отметки, точки недели, день календаря, эмодзи строки, высота строки. */
    const val HABIT_CHECK = 28
    const val HABIT_DOT = 8
    const val HABIT_DOT_GAP = 6
    const val HABIT_CAL_DAY = 34
    const val HABIT_EMOJI_SP = 20
    const val HABIT_ROW = 56
    const val HABIT_RING = 2
    /** #346: шаг штриховки прогноза и место под лимит у правого края полосы хаба. */
    const val LIMIT_HATCH = 6
    const val LIMIT_LABEL_RESERVE = 44
    /** #348: полоса хаба — толщина, риска плана, отступ до счетов. */
    const val HUB_LIMIT_BAR = 8
    const val HUB_LIMIT_TICK = 2
    const val HUB_FINANCE_GAP = 10
    const val PICKER_TILE = 76
    const val PICKER_ICON = 24
    const val TASK_CHECK_GAP = 10
    const val TASK_EMOJI = 16
    const val TASK_EMOJI_GAP = 8
    const val TASK_BELL = 14
    const val TASK_RIGHT_GAP = 8
    const val TASK_TITLE_SP = 14
    const val TASK_TIME_SP = 13
    const val TASK_EMOJI_SP = 13
    const val DAY_HEAD_SP = 12
    const val DAY_PROGRESS = 1
    const val WIDGET_WEEK_TIME_TEXT = 10
    const val CANVAS_TIME_TEXT = 13
    const val WEEKVIEW_TASK = 22
    const val WEEKVIEW_TASK_GAP = 3
    const val WEEKVIEW_CHECK = 14
    const val WEEKVIEW_CHECK_ICON = 9
    /** Итерация 55 (как в виджете): строка даты «ПН 9» по высоте текста — 20 (было 26). */
    const val WEEKVIEW_HEADER = 20
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

internal object FinanceShareSizes {
    const val accountMin = 68
    const val strip = 7
    const val gap = 4
    const val inset = 5
    const val label = 10
    const val percent = 11
}
