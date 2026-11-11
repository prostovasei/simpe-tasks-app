package com.prostovasei.simpletasks.ui.feature.finance

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/** Модули приложения в верхнем баре (решение PM 10.11.2026): новые добавляются сюда же, справа. */
enum class AppModule(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    TASKS(R.string.module_tasks, R.drawable.ic_module_tasks),
    FINANCE(R.string.module_finance, R.drawable.ic_module_finance),
}

/**
 * Верхний бар модулей (0.59.1, решение PM; было — плашка слева по макету compact-module-bar.html): белый фон на всю ширину,
 * значки модулей по центру без плашки выделения — активный чёрный, остальные серые. Настройки модуля — справа.
 * Заголовков модулей нет. Фон заходит под строку состояния.
 */
@Composable
fun ModuleBar(selected: AppModule, onSelect: (AppModule) -> Unit, modifier: Modifier = Modifier, onSettings: (() -> Unit)? = null) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(sizes.moduleBar)
            .padding(horizontal = spacing.sm)
            .testTag(TAG_MODULE_BAR),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            AppModule.entries.forEach { module ->
                val active = module == selected
                val label = stringResource(module.label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = sizes.moduleButtonWidth, height = sizes.moduleButtonHeight)
                        .clip(AppTheme.shapes.medium)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(module) })
                        .semantics { contentDescription = label }
                        .testTag("$TAG_MODULE:${module.name}"),
                ) {
                    Icon(painterResource(module.icon), contentDescription = null, tint = if (active) colors.accent else colors.inactive, modifier = Modifier.size(sizes.icon))
                }
            }
        }
        // Настройки модуля — справа в той же строке (решение PM 10.11.2026); у задач их здесь нет.
        if (onSettings != null) {
            val label = stringResource(R.string.finance_settings)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(sizes.moduleButtonWidth)
                    .clip(AppTheme.shapes.large)
                    .clickable(role = Role.Button, onClick = onSettings)
                    .semantics { contentDescription = label }
                    .testTag(TAG_MODULE_SETTINGS),
            ) { Icon(painterResource(R.drawable.ic_nav_settings), contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(sizes.moduleIcon)) }
        }
    }
}

/**
 * Нижняя панель финансов (макет compact-module-bar.html): аналитика, чёрный «+», операции — только значки, размеры как у панели задач.
 * 0.59.1 (решение PM): без плашки выделения — выбранная вкладка чёрная, другая серая.
 */
@Composable
fun FinanceBottomBar(selected: FinanceTab, onSelect: (FinanceTab) -> Unit, onAdd: () -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    Column(Modifier.fillMaxWidth().background(colors.surface).testTag(TAG_FINANCE_BAR)) {
        Box(Modifier.fillMaxWidth().height(sizes.hairline).background(colors.divider))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(sizes.navBar)
                .padding(horizontal = AppTheme.spacing.xs),
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FinanceNavTab(FinanceTab.ANALYTICS, R.drawable.ic_finance_analytics, R.string.finance_tab_analytics, selected, onSelect)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = sizes.navAddWidth, height = sizes.navAddHeight)
                    .clip(AppTheme.shapes.extraLarge)
                    .background(colors.accent)
                    .clickable(role = Role.Button, onClick = onAdd)
                    .testTag(TAG_FINANCE_ADD),
            ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.finance_add), tint = colors.onAccent) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FinanceNavTab(FinanceTab.OPERATIONS, R.drawable.ic_finance_operations, R.string.finance_tab_operations, selected, onSelect)
            }
        }
    }
}

@Composable
private fun FinanceNavTab(tab: FinanceTab, @DrawableRes icon: Int, @StringRes label: Int, selected: FinanceTab, onSelect: (FinanceTab) -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val active = tab == selected
    val description = stringResource(label)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = sizes.navTabWidth, height = sizes.navTabHeight)
            .clip(AppTheme.shapes.large)
            .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) })
            .semantics { contentDescription = description }
            .testTag("$TAG_FINANCE_TAB:${tab.name}"),
    ) { Icon(painterResource(icon), contentDescription = null, tint = if (active) colors.accent else colors.inactive, modifier = Modifier.size(sizes.icon)) }
}

const val TAG_MODULE_BAR = "module_bar"
const val TAG_MODULE = "module"
const val TAG_MODULE_SETTINGS = "module_settings"
const val TAG_FINANCE_BAR = "finance_bar"
const val TAG_FINANCE_TAB = "finance_tab"
const val TAG_FINANCE_ADD = "finance_add"
