package com.prostovasei.simpletasks.ui.common

import androidx.lifecycle.ViewModel
import com.prostovasei.simpletasks.domain.model.SortScope
import com.prostovasei.simpletasks.domain.model.TaskSort
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskSortStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Кнопка ↑↓ капсулы и шторка «Сортировка» (#357): сортировка открытого экрана и название списка для подписи. */
@HiltViewModel
class TaskSortViewModel @Inject constructor(
    private val store: TaskSortStore,
    private val projects: ProjectRepository,
) : ViewModel() {
    fun observe(scope: SortScope): Flow<TaskSort> = store.observe(scope)

    fun set(scope: SortScope, sort: TaskSort) = store.set(scope, sort)

    fun listName(scope: SortScope): Flow<String?> = when (scope) {
        is SortScope.Project -> projects.observeProject(scope.id).map { it?.name }
        else -> flowOf(null)
    }
}
