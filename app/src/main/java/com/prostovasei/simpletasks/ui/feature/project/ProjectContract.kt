package com.prostovasei.simpletasks.ui.feature.project

import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent

/** Экран задач одного списка с меню «Переименовать / Эмодзи / Удалить» (решение PM, аудит N1; итерация 47 — эмодзи вместо цвета и значка). */
data class ProjectState(
    val name: String = "",
    val groupId: String? = null,
    val color: String? = null,
    /** Эмодзи списка (итерация 47). */
    val emoji: String = "",
    val isInbox: Boolean = false,
    val tasks: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
    val dialog: ProjectDialog = ProjectDialog.None,
)

enum class ProjectDialog { None, Rename, Emoji, Delete }

sealed interface ProjectIntent : TaskListIntent {
    data class OpenDialog(val dialog: ProjectDialog) : ProjectIntent
    data object DismissDialog : ProjectIntent
    data class Rename(val name: String) : ProjectIntent
    data class SetEmoji(val emoji: String) : ProjectIntent
    data object ConfirmDelete : ProjectIntent
}

sealed interface ProjectEffect : TaskListEffect {
    /** Список удалён (или исчез) — экран закрывается. */
    data object Close : ProjectEffect
}
