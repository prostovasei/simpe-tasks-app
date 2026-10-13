package com.prostovasei.simpletasks.ui.feature.project

import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent

/** Экран задач одного списка с меню «Переименовать / Цвет / Удалить» (решение PM, аудит N1). */
data class ProjectState(
    val name: String = "",
    val groupId: String? = null,
    val color: String? = null,
    val isInbox: Boolean = false,
    val tasks: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
    val dialog: ProjectDialog = ProjectDialog.None,
)

enum class ProjectDialog { None, Rename, Color, Delete }

sealed interface ProjectIntent : TaskListIntent {
    data class OpenDialog(val dialog: ProjectDialog) : ProjectIntent
    data object DismissDialog : ProjectIntent
    data class Rename(val name: String) : ProjectIntent
    data class SetColor(val hex: String?) : ProjectIntent
    data object ConfirmDelete : ProjectIntent
}

sealed interface ProjectEffect : TaskListEffect {
    /** Список удалён (или исчез) — экран закрывается. */
    data object Close : ProjectEffect
}
