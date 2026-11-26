package com.prostovasei.simpletasks.data.repository

import android.content.Context
import androidx.core.content.edit
import com.prostovasei.simpletasks.domain.model.SortScope
import com.prostovasei.simpletasks.domain.model.TaskSort
import com.prostovasei.simpletasks.domain.repository.TaskSortStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Сортировки экранов в настройках устройства (#357): ключ — экран («upcoming», «inbox», «project:<id>»), значение — «DUE|0». */
@Singleton
class PrefsTaskSortStore @Inject constructor(@ApplicationContext context: Context) : TaskSortStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override fun observe(scope: SortScope): Flow<TaskSort> = state.map { it.sortOf(scope) }.distinctUntilChanged()

    override fun get(scope: SortScope): TaskSort = state.value.sortOf(scope)

    @Synchronized
    override fun set(scope: SortScope, sort: TaskSort) {
        prefs.edit { if (sort == scope.default) remove(scope.key) else putString(scope.key, sort.encode()) }
        state.value = read()
    }

    override var skipManualQuestion: Boolean
        get() = prefs.getBoolean(KEY_SKIP_QUESTION, false)
        set(value) = prefs.edit { putBoolean(KEY_SKIP_QUESTION, value) }

    private fun Map<String, String>.sortOf(scope: SortScope): TaskSort = TaskSort.decode(this[scope.key]) ?: scope.default

    private fun read(): Map<String, String> = prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()

    private companion object {
        const val PREFS = "task_sort"
        const val KEY_SKIP_QUESTION = "skip_manual_question"
    }
}
