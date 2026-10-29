package com.prostovasei.simpletasks.ui.feature.search

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Раздел «Поиск» (итерация 15): результаты меняются сразу при вводе. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    private val recents: RecentSearches,
) : MviViewModel<SearchState, SearchIntent, Nothing>(SearchState()) {

    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch { recents.items.collect { list -> setState { copy(recents = list) } } }
        viewModelScope.launch {
            combine(tasks.observeSearchable(), projects.observeProjectTree(), dayTicker.today(), query) { all, tree, day, q ->
                val byId = tree.projectsById()
                val (active, done) = searchTasks(all, q)
                fun SearchHit.toResult(isDone: Boolean) = SearchResult(
                    item = entry.task.toItem(byId, day, time.zone(), emptySet()).copy(isCompleting = isDone),
                    isDone = isDone,
                    match = match,
                )
                active.map { it.toResult(false) } to done.map { it.toResult(true) }
            }.collect { (active, done) ->
                // Строку запроса не трогаем: её держит поле ввода, иначе при быстром наборе буквы «отскакивают».
                setState { copy(active = active, done = done, isLoading = false) }
            }
        }
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.Query -> {
                setState { copy(query = intent.text) }
                query.value = intent.text
            }
            SearchIntent.Submit -> recents.add(currentState.query)
            is SearchIntent.PickRecent -> {
                setState { copy(query = intent.text) }
                query.value = intent.text
                recents.add(intent.text)
            }
            is SearchIntent.RemoveRecent -> recents.remove(intent.text)
            SearchIntent.ClearRecents -> recents.clear()
            is SearchIntent.Toggle -> viewModelScope.launch {
                if (intent.isDone) tasks.restore(intent.taskId) else tasks.complete(intent.taskId)
            }
        }
    }
}
