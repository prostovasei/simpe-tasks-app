package com.prostovasei.simpletasks.ui.feature.lists

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val projects: ProjectRepository,
    tasks: TaskRepository,
) : MviViewModel<ListsState, ListsIntent, ListsEffect>(ListsState()) {

    init {
        viewModelScope.launch {
            projects.observeProjectTree().collect { tree -> setState { copy(tree = tree) } }
        }
        viewModelScope.launch {
            tasks.observeTrashCount().collect { count -> setState { copy(trashCount = count) } }
        }
        viewModelScope.launch {
            tasks.observeWontDoCount().collect { count -> setState { copy(wontDoCount = count) } }
        viewModelScope.launch {
            tasks.observeSortCounts().collect { counts -> setState { copy(inboxCount = counts.inbox, somedayCount = counts.someday, noListCount = counts.noList) } }
        }
        }
    }

    override fun onIntent(intent: ListsIntent) {
        when (intent) {
            is ListsIntent.ToggleFolder -> setState {
                val collapsed = if (intent.folderId in collapsedFolders) collapsedFolders - intent.folderId else collapsedFolders + intent.folderId
                copy(collapsedFolders = collapsed)
            }
            is ListsIntent.OpenDialog -> setState { copy(dialog = intent.dialog) }
            ListsIntent.DismissDialog -> closeDialog()
            is ListsIntent.CreateList -> {
                closeDialog()
                viewModelScope.launch {
                    val id = projects.createProject(intent.name, groupId = intent.folderId)
                    sendEffect(ListsEffect.OpenProject(id))
                }
            }
            is ListsIntent.CreateFolder -> {
                closeDialog()
                viewModelScope.launch { projects.createGroup(intent.name) }
            }
            is ListsIntent.RenameFolder -> {
                val dialog = currentState.dialog as? ListsDialog.RenameFolder ?: return
                closeDialog()
                viewModelScope.launch { projects.renameGroup(dialog.folderId, intent.name) }
            }
            ListsIntent.ConfirmDeleteFolder -> {
                val dialog = currentState.dialog as? ListsDialog.DeleteFolder ?: return
                closeDialog()
                viewModelScope.launch { projects.deleteGroup(dialog.folderId) }
            }
        }
    }

    private fun closeDialog() = setState { copy(dialog = ListsDialog.None) }
}
