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
import androidx.compose.ui.graphics.Color
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
 * Компактный верхний бар модулей (макет design/approved/217/compact-module-bar.html): слева белая плашка со значками модулей,
 * активный подсвечен серым. Заголовков модулей нет. Сам занимает место под строкой состояния.
 */
@Composable
fun ModuleBar(selected: AppModule, onSelect: (AppModule) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(sizes.moduleBar)
            .padding(horizontal = spacing.sm)
            .testTag(TAG_MODULE_BAR),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
            modifier = Modifier.clip(AppTheme.shapes.large).background(colors.surface).padding(spacing.xxs),
        ) {
            AppModule.entries.forEach { module ->
                val active = module == selected
                val label = stringResource(module.label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = sizes.moduleButtonWidth, height = sizes.moduleButtonHeight)
                        .clip(AppTheme.shapes.medium)
                        .background(if (active) colors.selected else Color.Transparent)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(module) })
                        .semantics { contentDescription = label }
                        .testTag("$TAG_MODULE:${module.name}"),
                ) {
                    Icon(painterResource(module.icon), contentDescription = null, tint = if (active) colors.textPrimary else colors.inactive, modifier = Modifier.size(sizes.moduleIcon))
                }
            }
        }
    }
}

/**
 * Нижняя панель финансов (макет compact-module-bar.html): аналитика, чёрный «+», операции — только значки, размеры как у панели задач.
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
            .background(if (active) colors.selected else Color.Transparent)
            .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) })
            .semantics { contentDescription = description }
            .testTag("$TAG_FINANCE_TAB:${tab.name}"),
    ) { Icon(painterResource(icon), contentDescription = null, tint = if (active) colors.accent else colors.textSecondary, modifier = Modifier.size(sizes.icon)) }
}

const val TAG_MODULE_BAR = "module_bar"
const val TAG_MODULE = "module"
const val TAG_FINANCE_BAR = "finance_bar"
const val TAG_FINANCE_TAB = "finance_tab"
const val TAG_FINANCE_ADD = "finance_add"
