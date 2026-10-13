package com.prostovasei.simpletasks.ui.feature.settings

import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportReport

data class SettingsState(
    val stats: DataStats? = null,
    val import: ImportState = ImportState.Idle,
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
) {
    val isBusy: Boolean get() = import is ImportState.InProgress || isClearing
}

sealed interface ImportState {
    data object Idle : ImportState
    data class InProgress(val processed: Int, val total: Int) : ImportState
    data class Done(val report: ImportReport) : ImportState
    data class Failed(val reason: ImportFailure) : ImportState
}

enum class ImportFailure {
    /** Файл не похож на резервную копию TickTick. */
    NOT_TICKTICK_FILE,

    /** Файл не удалось прочитать. */
    READ_ERROR,
}

sealed interface SettingsIntent {
    data class FileSelected(val uri: String) : SettingsIntent
    data object ClearRequested : SettingsIntent
    data object ClearConfirmed : SettingsIntent
    data object ClearDismissed : SettingsIntent
}

sealed interface SettingsEffect {
    data object DataCleared : SettingsEffect
}
