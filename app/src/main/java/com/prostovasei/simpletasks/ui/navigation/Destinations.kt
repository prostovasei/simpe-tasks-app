package com.prostovasei.simpletasks.ui.navigation

import kotlinx.serialization.Serializable

/** Маршруты приложения (типобезопасная навигация, docs/ARCHITECTURE.md §2.4). */
@Serializable
data object InboxDestination

@Serializable
data object AllDestination

@Serializable
data object ListsDestination

@Serializable
data object SettingsDestination

/**
 * Быстрое создание задачи. [projectId] — список по умолчанию (null — «Входящие»);
 * [today] — задача сразу на сегодня (открыто с экрана «Все»).
 */
@Serializable
data class QuickAddDestination(val projectId: String? = null, val today: Boolean = false)

/** Экран задачи. */
@Serializable
data class TaskDetailDestination(val taskId: String)

/** Экран задач списка. Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class ProjectDestination(val projectId: String)
