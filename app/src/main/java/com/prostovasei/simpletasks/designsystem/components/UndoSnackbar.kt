package com.prostovasei.simpletasks.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens

/** Что произошло — от этого зависит значок слева в подсказке «Отмена» (итерация 38). */
enum class UndoKind(@DrawableRes val icon: Int) {
    Done(R.drawable.ic_node_check),
    Repeat(R.drawable.ic_tool_repeat),
    Deleted(R.drawable.ic_tool_delete),
    Moved(R.drawable.ic_move_today),
    Restored(R.drawable.ic_snack_restore),
    Tomorrow(R.drawable.ic_popup_tomorrow),
    Calendar(R.drawable.ic_widget_all),
    List(R.drawable.ic_list_inbox),
    Priority(R.drawable.ic_tool_priority),
}

/** Содержимое подсказки с кнопкой: текст, подпись кнопки и [kind] для значка. */
class UndoVisuals(
    override val message: String,
    override val actionLabel: String,
    val kind: UndoKind,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * Место подсказок: белая полупрозрачная карточка, как плашки и нижняя панель (итерация 38, решение PM). Без тени: сквозь полупрозрачную карточку она просвечивает. Отступы по бокам и снизу —
 * [AppTheme.spacing] `sm`; [modifier] добавляет отступ от края экрана (над нижней панелью Scaffold ставит её сам).
 */
@Composable
fun UndoSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val spacing = AppTheme.spacing
    SnackbarHost(state, modifier.padding(horizontal = spacing.sm, vertical = spacing.sm)) { data -> UndoSnackbar(data) }
}

@Composable
private fun UndoSnackbar(data: SnackbarData) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val kind = (data.visuals as? UndoVisuals)?.kind
    Surface(
        shape = AppTheme.shapes.extraLarge,
        color = colors.surface.copy(alpha = SNACKBAR_ALPHA),
        contentColor = colors.textPrimary,
        border = BorderStroke(AppTheme.sizes.hairline, colors.divider),
        modifier = Modifier.testTag(TAG_UNDO_SNACKBAR),
    ) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = SizeTokens.SNACKBAR_MIN_HEIGHT.dp).padding(start = spacing.md, end = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (kind != null) {
                Icon(painterResource(kind.icon), null, tint = colors.textSecondary, modifier = Modifier.size(SizeTokens.SNACKBAR_ICON.dp))
                Box(Modifier.size(spacing.sm))
            }
            Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(vertical = spacing.xs))
            data.visuals.actionLabel?.let { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    modifier = Modifier
                        .defaultMinSize(minHeight = SizeTokens.SNACKBAR_ACTION_MIN.dp)
                        .clickable(onClickLabel = label) { data.performAction() }
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                        .testTag(TAG_UNDO_ACTION),
                )
            }
        }
    }
}

/** Прозрачность белой карточки подсказки: сквозь неё виден список, но текст читается (88%). */
private const val SNACKBAR_ALPHA = 0.88f

const val TAG_UNDO_SNACKBAR = "undo_snackbar"
const val TAG_UNDO_ACTION = "undo_action"
