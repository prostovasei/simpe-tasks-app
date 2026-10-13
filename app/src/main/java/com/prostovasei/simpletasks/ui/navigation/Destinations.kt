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

/** Экран задач списка. Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class ProjectDestination(val projectId: String)
