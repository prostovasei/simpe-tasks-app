package com.prostovasei.simpletasks.ui.feature.all

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.domain.model.Rescheduling
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Свёрнут ли блок «Просрочено» (итерация 37, бэклог №30). Одно состояние на приложение и виджет, запоминается между запусками.
 */
@Singleton
class OverdueFold(initial: Boolean, private val save: (Boolean) -> Unit) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private constructor(prefs: SharedPreferences) : this(
        initial = prefs.getBoolean(KEY, false),
        save = { value -> prefs.edit(commit = true) { putBoolean(KEY, value) } },
    )

    private val _collapsed = MutableStateFlow(initial)
    val collapsed: StateFlow<Boolean> = _collapsed.asStateFlow()

    fun toggle() {
        val next = !_collapsed.value
        save(next)
        _collapsed.value = next
    }

    private companion object {
        const val PREFS = "overdue_fold"
        const val KEY = "collapsed"
    }
}

/**
 * «Перенести на сегодня» с «Отменой» (итерация 37, бэклог №26): перенос делает [run], а подсказку «Перенесено на сегодня: N · Отмена»
 * показывает тот, кто сейчас на экране, — приложение или прозрачное окно поверх рабочего стола (кнопка в виджете).
 * Событие получают только те, кто сейчас на экране; пустой [Rescheduling] — переносить было нечего.
 */
@Singleton
class OverdueMoves @Inject constructor(
    private val tasks: TaskRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<Rescheduling>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<Rescheduling> = _events.asSharedFlow()

    /** Переносит просроченные на сегодня; запись идёт в области приложения, чтобы окно могло закрыться сразу. */
    fun run() {
        appScope.launch { _events.tryEmit(tasks.moveOverdueToToday()) }
    }

    fun undo(rescheduling: Rescheduling) {
        appScope.launch { tasks.undoReschedule(rescheduling) }
    }

    private companion object {
        const val EVENT_BUFFER = 4
    }
}

/** Доступ к [OverdueMoves] из Compose: подсказка «Отмена» в приложении и в окне поверх рабочего стола. */
@HiltViewModel
class OverdueMovesHost @Inject constructor(val moves: OverdueMoves) : ViewModel()
