package com.prostovasei.simpletasks.ui.feature.settings

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.IoDispatcher
import com.prostovasei.simpletasks.core.files.DocumentOpener
import com.prostovasei.simpletasks.domain.model.InvalidImportFileException
import com.prostovasei.simpletasks.domain.repository.ImportRepository
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ImportRepository,
    private val documents: DocumentOpener,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        viewModelScope.launch {
            repository.observeStats().collect { stats -> setState { copy(stats = stats) } }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.FileSelected -> importFile(intent.uri)
            SettingsIntent.ClearRequested -> if (!currentState.isBusy) setState { copy(showClearConfirmation = true) }
            SettingsIntent.ClearDismissed -> setState { copy(showClearConfirmation = false) }
            SettingsIntent.ClearConfirmed -> clearAll()
        }
    }

    private fun importFile(uri: String) {
        if (currentState.isBusy) return
        setState { copy(import = ImportState.InProgress(processed = 0, total = 0)) }
        viewModelScope.launch {
            val result = try {
                val input = withContext(ioDispatcher) { documents.open(uri) }
                val report = repository.importTickTick(input) { progress ->
                    setState { copy(import = ImportState.InProgress(progress.processed, progress.total)) }
                }
                ImportState.Done(report)
            } catch (e: CancellationException) {
                throw e
            } catch (_: InvalidImportFileException) {
                ImportState.Failed(ImportFailure.NOT_TICKTICK_FILE)
            } catch (_: IOException) {
                ImportState.Failed(ImportFailure.READ_ERROR)
            } catch (_: SecurityException) {
                ImportState.Failed(ImportFailure.READ_ERROR)
            }
            setState { copy(import = result) }
        }
    }

    private fun clearAll() {
        if (currentState.isBusy) return
        setState { copy(showClearConfirmation = false, isClearing = true) }
        viewModelScope.launch {
            try {
                repository.clearAllData()
                setState { copy(import = ImportState.Idle) }
                sendEffect(SettingsEffect.DataCleared)
            } finally {
                setState { copy(isClearing = false) }
            }
        }
    }
}
