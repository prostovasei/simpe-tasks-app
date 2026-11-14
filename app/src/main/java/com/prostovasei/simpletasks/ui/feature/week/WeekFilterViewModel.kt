package com.prostovasei.simpletasks.ui.feature.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.WeekFilter
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Окно фильтра страницы «Неделя» (#243): дерево списков и текущий фильтр приложения. */
@HiltViewModel
class WeekFilterViewModel @Inject constructor(
    projects: ProjectRepository,
    private val store: AppWeekFilter,
) : ViewModel() {
    val tree: StateFlow<ProjectTree?> = projects.observeProjectTree().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)
    val filter: StateFlow<WeekFilter> = store.filter

    fun apply(filter: WeekFilter) = store.set(filter)

    private companion object {
        const val STOP_MS = 5_000L
    }
}
