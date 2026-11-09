package com.prostovasei.simpletasks.ui.feature.lists

import com.prostovasei.simpletasks.domain.model.ProjectTree

/** Экран «Списки» (SPEC.md §4.3): аккордеон папок со списками и счётчиками открытых задач. */
data class ListsState(
    /** Сколько задач в корзине (итерация 39): число у строки «Корзина», при нуле не показывается. */
    val trashCount: Int = 0,
    /** Сколько задач «Не буду делать» (итерация 49): число у строки, при нуле не показывается. */
    val wontDoCount: Int = 0,
    /** «Когда-нибудь» и «Без списка» (итерация 52): числа открытых задач у строк. */
    /** Итерация 55: «Входящие» — первой строкой «Списков», с числом неразобранных. */
    val inboxCount: Int = 0,
    val somedayCount: Int = 0,
    val noListCount: Int = 0,
    val tree: ProjectTree? = null,
    val collapsedFolders: Set<String> = emptySet(),
    val dialog: ListsDialog = ListsDialog.None,
)

sealed interface ListsDialog {
    data object None : ListsDialog
    data object CreateList : ListsDialog
    data object CreateFolder : ListsDialog
    /** Итерация 56: «⋮» папки → «Изменить» — название и эмодзи одним окном. */
    data class EditFolder(val folderId: String, val name: String, val emoji: String?) : ListsDialog
    data class DeleteFolder(val folderId: String, val name: String) : ListsDialog
}

sealed interface ListsIntent {
    data class ToggleFolder(val folderId: String) : ListsIntent
    data class OpenDialog(val dialog: ListsDialog) : ListsIntent
    data object DismissDialog : ListsIntent
    data class CreateList(val name: String, val folderId: String?, val emoji: String? = null) : ListsIntent
    data class CreateFolder(val name: String, val emoji: String? = null) : ListsIntent
    data class UpdateFolder(val name: String, val emoji: String?) : ListsIntent
    data object ConfirmDeleteFolder : ListsIntent
}

sealed interface ListsEffect {
    /** Открыть только что созданный список. */
    data class OpenProject(val projectId: String) : ListsEffect
}
