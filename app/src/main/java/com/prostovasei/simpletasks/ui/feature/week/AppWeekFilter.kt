package com.prostovasei.simpletasks.ui.feature.week

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.prostovasei.simpletasks.domain.model.WeekFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Фильтр страницы «Неделя» в приложении (#243): один на приложение, не связан с фильтрами виджетов, запоминается между запусками. */
@Singleton
class AppWeekFilter(initial: WeekFilter, private val save: (WeekFilter) -> Unit) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private constructor(prefs: SharedPreferences) : this(
        initial = WeekFilter.decode(prefs.getString(KEY, null)),
        save = { value -> prefs.edit(commit = true) { putString(KEY, value.encode()) } },
    )

    private val _filter = MutableStateFlow(initial)
    val filter: StateFlow<WeekFilter> = _filter.asStateFlow()

    fun set(value: WeekFilter) {
        save(value)
        _filter.value = value
    }

    private companion object {
        const val PREFS = "week_filter"
        const val KEY = "filter"
    }
}
