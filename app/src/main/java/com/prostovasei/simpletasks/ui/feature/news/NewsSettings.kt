package com.prostovasei.simpletasks.ui.feature.news

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.notifications.NewsAlertPrefs
import com.prostovasei.simpletasks.notifications.NewsAlertSettings
import com.prostovasei.simpletasks.ui.feature.finance.ServiceHeader
import com.prostovasei.simpletasks.ui.feature.settings.clockText

/** Страница «Новости» единых настроек (#296, макеты design/approved/296): уведомления и период тишины. */
@Composable
fun NewsSettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(NewsAlertPrefs.settings(context)) }
    val save: (NewsAlertSettings) -> Unit = {
        NewsAlertPrefs.set(context, it)
        settings = it
    }
    fun pick(minute: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(context, { _, hour, min -> onPicked(hour * MINUTES_PER_HOUR + min) }, minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR, true).show()
    }
    NewsSettingsScreen(
        settings = settings,
        onBack = onBack,
        onChange = save,
        onPickFrom = { pick(settings.quietFrom) { save(settings.copy(quietFrom = it)) } },
        onPickTo = { pick(settings.quietTo) { save(settings.copy(quietTo = it)) } },
    )
}

@Composable
fun NewsSettingsScreen(
    settings: NewsAlertSettings,
    onBack: () -> Unit,
    onChange: (NewsAlertSettings) -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val quietActive = settings.enabled
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_NEWS_SETTINGS)) {
        ServiceHeader(stringResource(R.string.news_settings_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Section(stringResource(R.string.news_settings_alerts_section))
            Card {
                SwitchRow(
                    title = stringResource(R.string.news_settings_alerts),
                    hint = stringResource(if (settings.enabled) R.string.news_settings_alerts_hint else R.string.news_settings_alerts_off_hint),
                    checked = settings.enabled,
                    enabled = true,
                    tag = TAG_NEWS_ALERTS_SWITCH,
                ) { onChange(settings.copy(enabled = it)) }
            }
            Section(stringResource(R.string.news_settings_quiet_section))
            Card {
                SwitchRow(
                    title = stringResource(R.string.news_settings_quiet),
                    hint = stringResource(if (quietActive) R.string.news_settings_quiet_hint else R.string.news_settings_quiet_off_hint),
                    checked = settings.quietOn && quietActive,
                    enabled = quietActive,
                    tag = TAG_NEWS_QUIET_SWITCH,
                ) { onChange(settings.copy(quietOn = it)) }
                val timesActive = quietActive && settings.quietOn
                Divider()
                TimeRow(stringResource(R.string.news_settings_quiet_from), settings.quietFrom, timesActive, TAG_NEWS_QUIET_FROM, onPickFrom)
                Divider()
                TimeRow(stringResource(R.string.news_settings_quiet_to), settings.quietTo, timesActive, TAG_NEWS_QUIET_TO, onPickTo)
            }
            if (settings.enabled) {
                Text(
                    stringResource(R.string.news_settings_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(AppTheme.colors.surface)) { content() }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = AppTheme.colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = AppTheme.spacing.md))
}

@Composable
private fun Section(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(top = AppTheme.spacing.md, bottom = AppTheme.spacing.xs),
    )
}

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, enabled: Boolean, tag: String, onCheckedChange: (Boolean) -> Unit) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (enabled) colors.textPrimary else colors.inactive)
            Text(hint, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun TimeRow(title: String, minute: Int, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.inactive
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizes.operationRow)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)
            .testTag(tag),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
        Text(clockText(minute), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

private const val MINUTES_PER_HOUR = 60
const val TAG_NEWS_SETTINGS = "news_settings"
const val TAG_NEWS_ALERTS_SWITCH = "news_alerts_switch"
const val TAG_NEWS_QUIET_SWITCH = "news_quiet_switch"
const val TAG_NEWS_QUIET_FROM = "news_quiet_from"
const val TAG_NEWS_QUIET_TO = "news_quiet_to"
