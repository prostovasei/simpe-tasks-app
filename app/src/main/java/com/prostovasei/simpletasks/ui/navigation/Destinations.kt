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
data class QuickAddDestination(val projectId: String? = null, val today: Boolean = false)

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

/** Задача «Не буду делать» — шторка только для чтения (итерация 49). Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class WontDoTaskDestination(val taskId: String)

/** Выполненные задачи (итерация 11). */
@Serializable
data object CompletedDestination

/** Экран задачи. */
@Serializable
data class TaskDetailDestination(val taskId: String)

/** Экран задач списка. Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class ProjectDestination(val projectId: String)
