package com.prostovasei.simpletasks.ui.feature.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.BuildConfig
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.update.AvailableUpdate
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.feature.task.DialogText
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog
import java.util.Locale

/** Окно обновления поверх рабочего стола (кнопка на виджете): состояние из [UpdateDialogViewModel], «Закрыть» — [onClose]. */
@Composable
fun UpdateDialogRoute(onClose: () -> Unit, viewModel: UpdateDialogViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { if (it == UpdateDialogEffect.Close) onClose() }
    }
    // Вернулись из настройки «Установка неизвестных приложений» — если разрешили, снова можно «Загрузить».
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(UpdateDialogIntent.Resume) }
    UpdateDialog(state, viewModel::onIntent)
}

/**
 * Окно по макету `VariantU1`: заголовок, короткий текст и одна-две кнопки. «Назад» и нажатие мимо окна — «Закрыть»,
 * кроме загрузки: её останавливает только «Отмена» (чтобы не оборвать случайно).
 */
@Composable
fun UpdateDialog(state: UpdateDialogState, onIntent: (UpdateDialogIntent) -> Unit) {
    val close = { onIntent(UpdateDialogIntent.Close) }
    val current = stringResource(R.string.update_version_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    when (state) {
        UpdateDialogState.Checking -> SheetDialog(
            title = stringResource(R.string.update_dialog_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = null,
            dismissLabel = R.string.update_dialog_close,
        ) {
            DialogText(stringResource(R.string.update_dialog_checking))
            LinearProgressIndicator(
                color = AppTheme.colors.accent,
                trackColor = AppTheme.colors.divider,
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
            )
        }
        UpdateDialogState.UpToDate -> SheetDialog(
            title = stringResource(R.string.update_dialog_none_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = null,
            dismissLabel = R.string.update_dialog_close,
            icon = { Badge(Icons.Default.Check) },
        ) {
            DialogText(stringResource(R.string.update_dialog_none_text))
            Strong(current)
        }
        is UpdateDialogState.Available -> SheetDialog(
            title = stringResource(R.string.update_dialog_found_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = { onIntent(UpdateDialogIntent.Download) },
            dismissLabel = R.string.update_dialog_close,
            doneLabel = R.string.update_dialog_download,
            icon = { Badge(painterResource(R.drawable.ic_widget_update)) },
        ) {
            DialogText(stringResource(R.string.update_dialog_found_new))
            Strong(versionText(state.update))
            val now = stringResource(R.string.update_dialog_found_now, current)
            val megabytes = megabytes(state.update.sizeBytes)
            Note(if (megabytes > 0) stringResource(R.string.update_dialog_found_size, now, megabytes) else now)
        }
        is UpdateDialogState.Downloading -> SheetDialog(
            title = stringResource(R.string.update_dialog_download_title),
            tag = TAG_UPDATE_DIALOG,
            // Загрузку обрывает только «Отмена»: «Назад» и нажатие мимо окна её не трогают.
            onDismiss = {},
            onCancel = close,
            onDone = null,
        ) {
            DialogText(stringResource(R.string.update_dialog_download_version, state.update.versionName))
            LinearProgressIndicator(
                progress = { state.percent / PERCENT_MAX },
                color = AppTheme.colors.accent,
                trackColor = AppTheme.colors.divider,
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm).testTag(TAG_UPDATE_PROGRESS),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg), horizontalArrangement = Arrangement.SpaceBetween) {
                val size = state.update.sizeBytes
                Small(if (size > 0) stringResource(R.string.update_dialog_download_progress, downloaded(size, state.percent), megabytes(size)) else "")
                Small(stringResource(R.string.update_dialog_percent, state.percent), Modifier.testTag(TAG_UPDATE_PERCENT))
            }
        }
        is UpdateDialogState.Ready -> SheetDialog(
            title = stringResource(R.string.update_dialog_ready_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = { onIntent(UpdateDialogIntent.Install) },
            dismissLabel = R.string.update_dialog_later,
            doneLabel = R.string.update_install,
            icon = { Badge(Icons.Default.Check) },
        ) {
            DialogText(stringResource(R.string.update_dialog_ready_text, state.update.versionName))
        }
        is UpdateDialogState.Installing -> SheetDialog(
            title = stringResource(R.string.update_dialog_installing_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = null,
            dismissLabel = R.string.update_dialog_close,
        ) {
            DialogText(stringResource(R.string.update_confirm))
        }
        UpdateDialogState.CheckFailed -> SheetDialog(
            title = stringResource(R.string.update_dialog_check_failed_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = { onIntent(UpdateDialogIntent.Retry) },
            dismissLabel = R.string.update_dialog_close,
            doneLabel = R.string.update_dialog_retry,
            icon = { Badge(Icons.Default.Warning, error = true) },
        ) {
            DialogText(stringResource(R.string.update_dialog_retry_text))
        }
        is UpdateDialogState.DownloadFailed -> SheetDialog(
            title = stringResource(R.string.update_dialog_download_failed_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = { onIntent(UpdateDialogIntent.Retry) },
            dismissLabel = R.string.update_dialog_close,
            doneLabel = R.string.update_dialog_retry,
            icon = { Badge(Icons.Default.Warning, error = true) },
        ) {
            DialogText(stringResource(R.string.update_dialog_retry_text))
        }
        is UpdateDialogState.NeedsPermission -> SheetDialog(
            title = stringResource(R.string.update_dialog_permission_title),
            tag = TAG_UPDATE_DIALOG,
            onDismiss = close,
            onDone = { onIntent(UpdateDialogIntent.GrantPermission) },
            dismissLabel = R.string.update_dialog_close,
            doneLabel = R.string.update_grant,
            icon = { Badge(Icons.Default.Lock) },
        ) {
            DialogText(stringResource(R.string.update_dialog_permission_text))
        }
    }
}

@Composable
private fun versionText(update: AvailableUpdate) = stringResource(R.string.update_version_build, update.versionName, update.build)

private fun megabytes(bytes: Long): Int = ((bytes + BYTES_IN_MB / 2) / BYTES_IN_MB).toInt()

/** «7,2» из «16 МБ»: сколько уже скачано по проценту (процент считается от того же размера). */
private fun downloaded(size: Long, percent: Int): String = String.format(RU, "%.1f", size * percent / PERCENT_MAX / BYTES_IN_MB)

@Composable
private fun Strong(text: String) {
    Text(
        text,
        color = AppTheme.colors.textPrimary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
    )
}

@Composable
private fun Small(text: String, modifier: Modifier = Modifier) {
    Text(text, color = AppTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/** Мелкая пометка под основным текстом (какая сейчас версия, сколько весит новая). */
@Composable
private fun Note(text: String) {
    Text(
        text,
        color = AppTheme.colors.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xs),
    )
}

/** Круглый значок над заголовком: зелёный (всё хорошо) или красный (ошибка). */
@Composable
private fun Badge(icon: ImageVector, error: Boolean = false) = BadgeBox(error) {
    Icon(icon, null, tint = badgeTint(error), modifier = Modifier.size(AppTheme.sizes.dialogBadgeIcon))
}

@Composable
private fun Badge(icon: androidx.compose.ui.graphics.painter.Painter) = BadgeBox(false) {
    Icon(icon, null, tint = badgeTint(false), modifier = Modifier.size(AppTheme.sizes.dialogBadgeIcon))
}

@Composable
private fun badgeTint(error: Boolean): Color = if (error) AppTheme.colors.danger else AppTheme.colors.accent

@Composable
private fun BadgeBox(error: Boolean, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(AppTheme.sizes.dialogBadge)
            .clip(CircleShape)
            .background(if (error) colors.danger.copy(alpha = BADGE_ERROR_ALPHA) else colors.selected),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private const val BYTES_IN_MB = 1024f * 1024f
private const val PERCENT_MAX = 100f
private const val BADGE_ERROR_ALPHA = 0.12f
private val RU = Locale.forLanguageTag("ru")

const val TAG_UPDATE_DIALOG = "update_dialog"
const val TAG_UPDATE_PROGRESS = "update_progress"
const val TAG_UPDATE_PERCENT = "update_percent"
