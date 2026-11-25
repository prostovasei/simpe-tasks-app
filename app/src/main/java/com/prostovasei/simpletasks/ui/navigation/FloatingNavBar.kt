package com.prostovasei.simpletasks.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.NavBarTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import java.time.LocalDate

/*
 * Плавающая панель (#336, конструктор PM 25.11.2026): тёмная капсула справа внизу — дата «25 ср/ноя», хаб (сетка 2 × 2, открывает
 * выбор раздела), настройки (ползунки) и фиолетовый «+». Вкладок в панели больше нет: разделы — в окне выбора плитками.
 */

private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingNavBar(
    today: LocalDate,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    addTag: String,
    @StringRes addLabel: Int,
    modifier: Modifier = Modifier,
    onHubLongClick: (() -> Unit)? = null,
) {
    val background = Color(NavBarTokens.background.light)
    val icon = Color(NavBarTokens.icon.light)
    Row(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = SizeTokens.NAV_EDGE.dp, bottom = SizeTokens.NAV_EDGE.dp, top = AppTheme.spacing.xxs),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            Modifier.shadow(AppTheme.spacing.xs, CircleShape).clip(CircleShape).background(background)
                .padding(SizeTokens.NAV_PAD.dp).testTag(TAG_APP_BAR),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.sizes.hairline * 2),
        ) {
            NavDate(today)
            val hubLabel = stringResource(R.string.nav_sections)
            Box(
                Modifier.size(SizeTokens.NAV_BUTTON.dp).clip(CircleShape).background(Color(NavBarTokens.selected.light))
                    .combinedClickable(role = Role.Button, onClick = onHub, onLongClick = onHubLongClick)
                    .semantics { contentDescription = hubLabel }.testTag(TAG_NAV_HUB),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_nav_grid), null, tint = icon, modifier = Modifier.size(SizeTokens.NAV_ICON.dp)) }
            val settings = stringResource(R.string.settings_title)
            Box(
                Modifier.size(SizeTokens.NAV_BUTTON.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onSettings)
                    .semantics { contentDescription = settings }.testTag(TAG_MODULE_SETTINGS),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_nav_sliders), null, tint = icon, modifier = Modifier.size(SizeTokens.NAV_ICON.dp)) }
            val add = stringResource(addLabel)
            Box(
                Modifier.size(SizeTokens.NAV_BUTTON.dp).clip(CircleShape).background(Color(NavBarTokens.plus.light))
                    .clickable(role = Role.Button, onClick = onAdd).semantics { contentDescription = add }.testTag(addTag),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_nav_plus), null, tint = icon, modifier = Modifier.size(SizeTokens.NAV_ICON.dp)) }
        }
    }
}

/** «25» крупно и «ср / ноя» столбиком — как дата в нижней строке виджета. */
@Composable
private fun NavDate(today: LocalDate) {
    Row(
        Modifier.height(SizeTokens.NAV_BUTTON.dp).padding(start = AppTheme.spacing.xs, end = AppTheme.spacing.xxs).testTag(TAG_NAV_DATE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.sizes.hairline * 3),
    ) {
        Text(today.dayOfMonth.toString(), fontSize = SizeTokens.NAV_DATE_SP.sp, fontWeight = FontWeight.Bold, color = Color(NavBarTokens.icon.light))
        Column {
            val small = Modifier
            Text(WEEKDAYS[today.dayOfWeek.value - 1], small, fontSize = SizeTokens.NAV_DATE_SMALL_SP.sp, lineHeight = SizeTokens.NAV_DATE_SMALL_SP.sp,
                fontWeight = FontWeight.Bold, color = Color(NavBarTokens.muted.light))
            Text(MONTHS[today.monthValue - 1], small, fontSize = SizeTokens.NAV_DATE_SMALL_SP.sp, lineHeight = SizeTokens.NAV_DATE_SMALL_SP.sp,
                fontWeight = FontWeight.Bold, color = Color(NavBarTokens.muted.light))
        }
    }
}

/** Раздел в окне выбора: тег прежней вкладки панели (`module:HUB`, `tab:UPCOMING` …) — по нему переходят тесты. */
data class PickerEntry(val key: String, @DrawableRes val icon: Int, @StringRes val label: Int, val tag: String)

/** Разделы окна выбора (#336, выбор PM): «Предстоящее», «Неделя», «Списки» — отдельно; «Настройки» — последней плиткой. */
val PICKER_ENTRIES: List<PickerEntry> = listOf(
    PickerEntry("hub", R.drawable.ic_nav_grid, R.string.module_hub, "module:HUB"),
    PickerEntry(Tab.UPCOMING.name, Tab.UPCOMING.icon, Tab.UPCOMING.label, "tab:${Tab.UPCOMING.name}"),
    PickerEntry(Tab.WEEK.name, Tab.WEEK.icon, Tab.WEEK.label, "tab:${Tab.WEEK.name}"),
    PickerEntry(Tab.LISTS.name, Tab.LISTS.icon, Tab.LISTS.label, "tab:${Tab.LISTS.name}"),
    PickerEntry("finance", R.drawable.ic_module_finance, R.string.module_finance, "module:FINANCE"),
    PickerEntry("health", R.drawable.ic_module_health, R.string.module_health, "module:HEALTH"),
    PickerEntry("news", R.drawable.ic_module_news, R.string.module_news, "module:NEWS"),
    PickerEntry("settings", R.drawable.ic_nav_sliders, R.string.settings_title, TAG_PICKER_SETTINGS),
)

/** Выбор раздела плитками снизу (#336): затемнение, лист со скруглением, плитки по три в ряд; выбранный раздел — фиолетовым. */
@Composable
fun TabPickerSheet(selected: String?, onPick: (String) -> Unit, onDismiss: () -> Unit, entries: List<PickerEntry> = PICKER_ENTRIES) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss, indication = null, interactionSource = null), contentAlignment = Alignment.BottomCenter) {
            TabPickerTiles(selected, onPick, Modifier.windowInsetsPadding(WindowInsets.navigationBars), entries)
        }
    }
}

@Composable
fun TabPickerTiles(selected: String?, onPick: (String) -> Unit, modifier: Modifier = Modifier, entries: List<PickerEntry> = PICKER_ENTRIES) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        modifier.fillMaxWidth().padding(spacing.sm).clip(AppTheme.shapes.extraLarge).background(colors.surface)
            .clickable(enabled = false) {}.padding(spacing.sm).testTag(TAG_PICKER),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        entries.chunked(PICKER_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                row.forEach { entry ->
                    val on = entry.key == selected
                    val label = stringResource(entry.label)
                    Column(
                        Modifier.weight(1f).height(SizeTokens.PICKER_TILE.dp).clip(AppTheme.shapes.large)
                            .background(if (on) colors.priority.high.container else colors.selected)
                            .clickable(role = Role.Button) { onPick(entry.key) }
                            .semantics { contentDescription = label; this.selected = on }.testTag(entry.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(painterResource(entry.icon), null, tint = if (on) colors.priority.high.content else colors.textPrimary,
                            modifier = Modifier.size(SizeTokens.PICKER_ICON.dp))
                        Spacer(Modifier.height(spacing.xxs))
                        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center,
                            color = if (on) colors.priority.high.content else colors.textPrimary)
                    }
                }
                repeat(PICKER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val PICKER_COLUMNS = 3
const val TAG_NAV_HUB = "nav_hub"
const val TAG_NAV_DATE = "nav_date"
const val TAG_PICKER = "tab_picker"
const val TAG_PICKER_SETTINGS = "picker:settings"
