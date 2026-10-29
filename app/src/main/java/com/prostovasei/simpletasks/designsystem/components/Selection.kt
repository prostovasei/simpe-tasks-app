package com.prostovasei.simpletasks.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens

/** Верхняя строка режима выбора (итерация 40): «✕ Выбрано: N» и «Выбрать все». */
@Composable
fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier.fillMaxWidth().background(colors.surface).testTag(TAG_SELECTION_BAR)) {
        Row(Modifier.fillMaxWidth().height(SizeTokens.SELECTION_BAR.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.testTag(TAG_SELECTION_CLOSE)) {
                Icon(Icons.Default.Close, stringResource(R.string.select_close), tint = colors.textPrimary)
            }
            Text(
                stringResource(R.string.select_count, count),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).testTag(TAG_SELECTION_COUNT),
            )
            TextButton(onClick = onSelectAll, modifier = Modifier.testTag(TAG_SELECTION_ALL)) {
                Text(stringResource(R.string.select_all), color = colors.accent, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = colors.divider)
    }
}

/** Кнопка ленты действий: значок и подпись; [enabled] = false — бледная, нажатие ничего не делает; [danger] — красная. */
data class RibbonAction(
    @DrawableRes val icon: Int,
    val label: String,
    val tag: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Лента действий режима выбора (итерация 40, решение PM): на месте нижней панели, все действия в один ряд, ряд прокручивается пальцем;
 * у правого края лёгкое затухание — видно, что дальше есть ещё.
 */
@Composable
fun ActionRibbon(actions: List<RibbonAction>, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Box(
        modifier
            .fillMaxWidth()
            .padding(spacing.sm)
            .height(sizes.navBar)
            .shadow(sizes.navShadow, AppTheme.shapes.nav)
            .clip(AppTheme.shapes.nav)
            .background(colors.surface)
            .testTag(TAG_ACTION_RIBBON),
    ) {
        Row(
            Modifier.matchParentSize().horizontalScroll(rememberScrollState()).padding(horizontal = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.HAIRLINE.dp),
        ) {
            actions.forEach { action ->
                val tint = if (action.danger) colors.danger else colors.textPrimary
                Column(
                    Modifier
                        .defaultMinSize(minWidth = SizeTokens.RIBBON_BUTTON.dp)
                        .height(SizeTokens.RIBBON_BUTTON_HEIGHT.dp)
                        .clip(AppTheme.shapes.large)
                        .clickable(enabled = action.enabled, onClick = action.onClick)
                        .alpha(if (action.enabled) 1f else DISABLED_ALPHA)
                        .padding(horizontal = spacing.xs)
                        .testTag(action.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(action.icon), null, tint = tint, modifier = Modifier.size(sizes.icon))
                    Text(action.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tint, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(spacing.lg)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(colors.surface.copy(alpha = 0f), colors.surface))),
        )
    }
}

/** Недоступное действие (нет подходящих задач среди выбранных) — бледное. */
private const val DISABLED_ALPHA = 0.35f

const val TAG_SELECTION_BAR = "selection_bar"
const val TAG_SELECTION_CLOSE = "selection_close"
const val TAG_SELECTION_COUNT = "selection_count"
const val TAG_SELECTION_ALL = "selection_all"
const val TAG_ACTION_RIBBON = "action_ribbon"
