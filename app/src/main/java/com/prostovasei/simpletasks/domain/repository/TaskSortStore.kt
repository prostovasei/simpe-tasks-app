package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.SortScope
import com.prostovasei.simpletasks.domain.model.TaskSort
import kotlinx.coroutines.flow.Flow

/** Сортировки экранов задач (#357): у каждого списка своя, хранятся на устройстве. */
interface TaskSortStore {
    fun observe(scope: SortScope): Flow<TaskSort>

    fun get(scope: SortScope): TaskSort

    fun set(scope: SortScope, sort: TaskSort)

    /** Галочка «Больше не спрашивать» в окне «Переключить на ручную сортировку?». */
    var skipManualQuestion: Boolean
}
