package com.prostovasei.simpletasks.ui.feature.update

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.update.AppUpdater
import com.prostovasei.simpletasks.core.update.AvailableUpdate
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Окно обновления с виджета (итерация 32, макет `VariantU1`): проверка → найдено → загрузка → «Обновить». */
sealed interface UpdateDialogState {
    data object Checking : UpdateDialogState
    data object UpToDate : UpdateDialogState
    data object CheckFailed : UpdateDialogState
    data class Available(val update: AvailableUpdate) : UpdateDialogState
    data class Downloading(val update: AvailableUpdate, val percent: Int) : UpdateDialogState
    data class DownloadFailed(val update: AvailableUpdate) : UpdateDialogState

    /** Приложению ещё не разрешено устанавливать обновления (один раз). */
    data class NeedsPermission(val update: AvailableUpdate) : UpdateDialogState

    /** Скачано: «Обновить» или «Не сейчас». */
    data class Ready(val update: AvailableUpdate, val apk: File) : UpdateDialogState

    /** Файл у установщика Android — он показывает своё окно «Обновить приложение?». */
    data class Installing(val update: AvailableUpdate) : UpdateDialogState
}

sealed interface UpdateDialogIntent {
    data object Download : UpdateDialogIntent

    /** «Повторить» после ошибки: проверку или скачивание, смотря где споткнулись. */
    data object Retry : UpdateDialogIntent
    data object Install : UpdateDialogIntent
    data object GrantPermission : UpdateDialogIntent

    /** Окно снова на экране (вернулись из настроек телефона). */
    data object Resume : UpdateDialogIntent

    /** «Закрыть», «Не сейчас», «Отмена»: останавливает проверку или скачивание и закрывает окно. */
    data object Close : UpdateDialogIntent
}

sealed interface UpdateDialogEffect {
    data object Close : UpdateDialogEffect
}

@HiltViewModel
class UpdateDialogViewModel @Inject constructor(
    private val updater: AppUpdater,
    savedState: androidx.lifecycle.SavedStateHandle,
) :
    MviViewModel<UpdateDialogState, UpdateDialogIntent, UpdateDialogEffect>(UpdateDialogState.Checking) {

    private var job: Job? = null
    private var installing: UpdateDialogState.Ready? = null

    /** #354: открыто кнопкой «Обновить приложение» — найденная версия сразу скачивается и передаётся установщику. */
    private val auto: Boolean = savedState.get<Boolean>(com.prostovasei.simpletasks.UpdateActivity.EXTRA_AUTO) == true

    init {
        check()
    }

    override fun onIntent(intent: UpdateDialogIntent) {
        when (intent) {
            UpdateDialogIntent.Download -> (currentState as? UpdateDialogState.Available)?.let { download(it.update) }
            UpdateDialogIntent.Retry -> when (val state = currentState) {
                UpdateDialogState.CheckFailed -> check()
                is UpdateDialogState.DownloadFailed -> download(state.update)
                else -> Unit
            }
            UpdateDialogIntent.Install -> (currentState as? UpdateDialogState.Ready)?.let(::install)
            UpdateDialogIntent.GrantPermission -> updater.openInstallPermissionSettings()
            UpdateDialogIntent.Resume -> when (val state = currentState) {
                is UpdateDialogState.NeedsPermission -> if (updater.canInstall()) setState { UpdateDialogState.Available(state.update) } else Unit
                is UpdateDialogState.Installing -> installing?.let { ready -> setState { ready } } ?: Unit
                else -> Unit
            }
            UpdateDialogIntent.Close -> {
                job?.cancel()
                discardDownloadedFile()
                sendEffect(UpdateDialogEffect.Close)
            }
        }
    }

    private fun check() {
        setState { UpdateDialogState.Checking }
        job = viewModelScope.launch {
            val result = try {
                updater.check()?.let { UpdateDialogState.Available(it) } ?: UpdateDialogState.UpToDate
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                UpdateDialogState.CheckFailed
            }
            setState { result }
            if (auto && result is UpdateDialogState.Available) download(result.update)
        }
    }

    private fun download(update: AvailableUpdate) {
        setState { UpdateDialogState.Downloading(update, 0) }
        job = viewModelScope.launch {
            val result = try {
                val apk = updater.download(update) { percent -> setState { UpdateDialogState.Downloading(update, percent) } }
                if (apk == null) UpdateDialogState.NeedsPermission(update) else UpdateDialogState.Ready(update, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                UpdateDialogState.DownloadFailed(update)
            }
            setState { result }
            if (auto && result is UpdateDialogState.Ready) install(result)
        }
    }

    private fun install(ready: UpdateDialogState.Ready) {
        installing = ready
        setState { UpdateDialogState.Installing(ready.update) }
        job = viewModelScope.launch {
            try {
                updater.commit(ready.apk)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                discardDownloadedFile()
                setState { UpdateDialogState.DownloadFailed(ready.update) }
            }
        }
    }
    override fun onCleared() {
        discardDownloadedFile()
        super.onCleared()
    }

    private fun discardDownloadedFile() {
        ((currentState as? UpdateDialogState.Ready) ?: installing)?.apk?.delete()
        installing = null
    }
}
