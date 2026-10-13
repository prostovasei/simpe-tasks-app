package com.prostovasei.simpletasks.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Основа MVI-экрана: экран отправляет [Intent], ViewModel меняет [State] (всё, что рисуется)
 * и иногда шлёт одноразовый [Effect] (Snackbar, навигация).
 */
abstract class MviViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    abstract fun onIntent(intent: Intent)

    protected val currentState: State get() = _state.value

    protected fun setState(reducer: State.() -> State) = _state.update(reducer)

    protected fun sendEffect(effect: Effect) {
        _effects.trySend(effect)
    }
}
