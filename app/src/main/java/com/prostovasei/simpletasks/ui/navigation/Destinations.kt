package com.prostovasei.simpletasks.ui.navigation

import kotlinx.serialization.Serializable

/** Маршруты приложения (типобезопасная навигация, docs/ARCHITECTURE.md §2.4). */
@Serializable
data object InboxDestination

@Serializable
data object AllDestination

/** Вид «Неделя» (итерация 44). */
@Serializable
data object WeekDestination

/** Полный список дня недели (итерация 44): [date] — день в виде «2026-10-30». Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class DayDestination(val date: String)

@Serializable
data object ListsDestination

@Serializable
data object SearchDestination

@Serializable
data object SettingsDestination

/**
 * Быстрое создание задачи. [projectId] — список по умолчанию (null — «Входящие»);
 * [today] — задача сразу на сегодня (открыто с экрана «Все»).
 */
@Serializable
data class QuickAddDestination(
    val projectId: String? = null,
    val today: Boolean = false,
    val date: String? = null,
    /** Из «Когда-нибудь» (итерация 52): сразу открыть выбор списка — задача без даты со списком. */
    val pickList: Boolean = false,
)

/** Прозрачное окно «Перенести просроченные на сегодня» поверх рабочего стола (итерация 37, из виджета). */
@Serializable
data object MoveOverdueDestination

/** Корзина (итерация 39). */
@Serializable
data object TrashDestination

/** Задача в корзине — шторка только для чтения (итерация 39). Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class TrashTaskDestination(val taskId: String)

/** «Не буду делать» (итерация 49). */
@Serializable
data object WontDoDestination

/** «Когда-нибудь» (итерация 52): задачи со списком, но без даты. */
@Serializable
data object SomedayDestination

/** «Без списка» (итерация 52): задачи без списка, но с датой. */
@Serializable
data object NoListDestination

/** Задача «Не буду делать» — шторка только для чтения (итерация 49). Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class WontDoTaskDestination(val taskId: String)

/** Выполненные задачи (итерация 11). */
@Serializable
data object CompletedDestination

/** Экран задачи; [openDate] — сразу окно даты и времени (итерация 53: «Отложить» → «Выбрать дату и время…»). */
@Serializable
data class TaskDetailDestination(val taskId: String, val openDate: Boolean = false)

/** Окно «Отложить» поверх экрана — из кнопки уведомления (итерация 53). */
@Serializable
data class SnoozeDestination(val taskId: String)

/** Экран задач списка. Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class ProjectDestination(val projectId: String)

/** Раздел «Финансы» (#217, этап 1 — #221): открывается значком «₽» верхнего бара модулей. */
@Serializable
data object FinanceDestination

/** Корзина финансов (этап 4, #231): из меню шестерёнки. */
@Serializable
data object FinanceTrashDestination

/** Категории финансов (этап 2, #225): из меню шестерёнки финансов. */
@Serializable
data object FinanceCategoriesDestination
