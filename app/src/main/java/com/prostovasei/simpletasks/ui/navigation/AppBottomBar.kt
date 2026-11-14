package com.prostovasei.simpletasks.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected as semanticsSelected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/** Модули приложения (решение PM 10.11.2026): новые добавляются сюда же. Выбор модуля — кнопка справа в нижней панели (#241). */
enum class AppModule(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    TASKS(R.string.module_tasks, R.drawable.ic_module_tasks),
    FINANCE(R.string.module_finance, R.drawable.ic_module_finance),
}

/** Страница модуля в нижней панели: [key] — для выбора, [tag] — для тестов. */
data class BarPage(val key: String, @param:DrawableRes val icon: Int, @param:StringRes val label: Int, val tag: String)

/**
 * Единая нижняя панель приложения (#241, макеты design/approved/241, решение PM 13.11.2026): слева чёрная «+» ([onAdd]), посередине
 * страницы модуля [pages] (выбранная чёрная, остальные серые, без плашек), справа кнопка модуля — той же ширины и со скруглениями, как у «+»,
 * но с обводкой. Нажатие на кнопку модуля открывает меню: «Задачи», «Финансы» ([onModule]) и «Настройки» ([onSettings]).
 * Верхнего бара нет. Панель низкая (44): нажимается всей высотой, видна только кнопка 34.
 */
@Composable
fun AppBottomBar(
    pages: List<BarPage>,
    selected: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    addTag: String,
    @StringRes addLabel: Int,
    module: AppModule,
    onModule: (AppModule) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onPageLongClick: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    var menu by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().background(colors.surface).testTag(TAG_APP_BAR)) {
        Box(Modifier.fillMaxWidth().height(sizes.hairline).background(colors.divider))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(sizes.appBar)
                .padding(horizontal = spacing.sm),
        ) {
            val add = stringResource(addLabel)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = sizes.navAddWidth, height = sizes.appBar)
                    .clip(AppTheme.shapes.extraLarge)
                    .clickable(role = Role.Button, onClick = onAdd)
                    .semantics { contentDescription = add }
                    .testTag(addTag),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(width = sizes.navAddWidth, height = sizes.appBarButton).clip(AppTheme.shapes.extraLarge).background(colors.accent),
                ) { Icon(Icons.Default.Add, contentDescription = null, tint = colors.onAccent) }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                pages.forEach { page ->
                    val active = page.key == selected
                    val label = stringResource(page.label)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = sizes.appBarPageWidth, height = sizes.appBar)
                            .clip(AppTheme.shapes.large)
                            .combinedClickable(role = Role.Tab, onClick = { onSelect(page.key) }, onLongClick = { onPageLongClick(page.key) })
                            .semantics { contentDescription = label; semanticsSelected = active }
                            .testTag(page.tag),
                    ) { Icon(painterResource(page.icon), contentDescription = null, tint = if (active) colors.accent else colors.inactive, modifier = Modifier.size(sizes.icon)) }
                }
            }
            val moduleLabel = stringResource(R.string.bar_module, stringResource(module.label))
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = sizes.navAddWidth, height = sizes.appBar)
                        .clip(AppTheme.shapes.extraLarge)
                        .clickable(role = Role.Button) { menu = true }
                        .semantics { contentDescription = moduleLabel }
                        .testTag(TAG_MODULE_BUTTON),
                ) {
                    val shape = AppTheme.shapes.extraLarge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = sizes.navAddWidth, height = sizes.appBarButton)
                            .clip(shape)
                            .then(if (menu) Modifier.background(colors.accent) else Modifier.border(sizes.appBarOutline, colors.accent, shape)),
                    ) { Icon(painterResource(module.icon), contentDescription = null, tint = if (menu) colors.onAccent else colors.accent, modifier = Modifier.size(sizes.moduleIcon)) }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface, modifier = Modifier.testTag(TAG_MODULE_MENU)) {
                    AppModule.entries.forEach { entry ->
                        val current = entry == module
                        DropdownMenuItem(
                            text = { Text(stringResource(entry.label), color = colors.textPrimary, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = { Icon(painterResource(entry.icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(sizes.moduleIcon)) },
                            trailingIcon = if (current) {
                                { Box(Modifier.size(spacing.xs).background(colors.accent, CircleShape)) }
                            } else {
                                null
                            },
                            onClick = {
                                menu = false
                                if (!current) onModule(entry)
                            },
                            modifier = Modifier.testTag("$TAG_MODULE:${entry.name}"),
                        )
                    }
                    HorizontalDivider(color = colors.divider, thickness = sizes.hairline)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tab_settings), color = colors.textPrimary, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_nav_settings), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(sizes.moduleIcon)) },
                        onClick = {
                            menu = false
                            onSettings()
                        },
                        modifier = Modifier.testTag(TAG_MODULE_SETTINGS),
                    )
                }
            }
        }
    }
}

const val TAG_APP_BAR = "app_bar"
const val TAG_MODULE_BUTTON = "module_button"
const val TAG_MODULE_MENU = "module_menu"
const val TAG_MODULE = "module"
const val TAG_MODULE_SETTINGS = "module_settings"
