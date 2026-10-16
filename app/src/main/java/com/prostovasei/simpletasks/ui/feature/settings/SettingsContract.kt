package com.prostovasei.simpletasks.ui.feature.settings

import com.prostovasei.simpletasks.core.update.AvailableUpdate
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportReport

data class SettingsState(
    val stats: DataStats? = null,
    val import: ImportState = ImportState.Idle,
    val export: ExportState = ExportState.Idle,
    /** Имя файла, которое пикер предложит при сохранении резервной копии. */
    val backupFileName: String = "",
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
    val update: UpdateState = UpdateState.Idle,
) {
    val isBusy: Boolean get() = import is ImportState.InProgress || export is ExportState.InProgress || isClearing
}

/** Обновление по кнопке (итерация 10). */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val percent: Int) : UpdateState
    data class NeedsPermission(val update: AvailableUpdate) : UpdateState

    /** Android показывает окно «Обновить приложение?». */
    data class Confirming(val update: AvailableUpdate) : UpdateState
    data object CheckFailed : UpdateState
    data class DownloadFailed(val update: AvailableUpdate) : UpdateState
}

sealed interface ExportState {
    data object Idle : ExportState
    data object InProgress : ExportState
    data class Done(val tasks: Int) : ExportState
    data object Failed : ExportState
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
    data class ExportTo(val uri: String) : SettingsIntent
    data object ClearRequested : SettingsIntent
    data object ClearConfirmed : SettingsIntent
    data object ClearDismissed : SettingsIntent
    data object CheckUpdate : SettingsIntent
    data object InstallUpdate : SettingsIntent
    data object GrantInstallPermission : SettingsIntent
}

sealed interface SettingsEffect {
    data object DataCleared : SettingsEffect
}
