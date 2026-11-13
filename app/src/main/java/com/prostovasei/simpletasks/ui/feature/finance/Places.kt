package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Place

@Composable
fun PlacesRoute(viewModel: FinanceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlacesScreen(state.places, viewModel::onIntent, onBack)
}

/**
 * Справочник мест (#239, макеты 1.2, 1.4, 2.2–2.4, 3.2): как категории, но без эмодзи и без деления на расходы и доходы.
 * По алфавиту, архив внизу, «+» — новое место. Нажатие на место — правка названия и «В архив» (с предупреждением);
 * на архивное — «Вернуть из архива» или «Удалить навсегда» (с предупреждением и числом затронутых операций).
 */
@Composable
fun PlacesScreen(places: List<Place>, onIntent: (FinanceIntent) -> Unit, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var archivingId by rememberSaveable { mutableStateOf<String?>(null) }
    var archivedId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val (archived, active) = places.sortedByName { it.name }.partition { it.archived }
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_PLACES)) {
        ServiceHeader(stringResource(R.string.places_title), onBack) {
            val add = stringResource(R.string.place_add)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppTheme.sizes.financeClose)
                    .clip(AppTheme.shapes.large)
                    .background(colors.accent)
                    .clickable(role = Role.Button) { creating = true }
                    .semantics { contentDescription = add }
                    .testTag(TAG_PLACE_ADD),
            ) { Icon(Icons.Default.Add, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(AppTheme.sizes.accountAddIcon)) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = spacing.md, end = spacing.md, bottom = spacing.lg)) {
            if (active.isEmpty()) {
                item(key = "empty") { Text(stringResource(R.string.places_empty), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(spacing.md)) }
            } else {
                item(key = "active") { PlaceCard(active) { editingId = it.id } }
            }
            if (archived.isNotEmpty()) {
                item(key = "archive-title") {
                    Text(stringResource(R.string.categories_archive), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(start = spacing.xxs, top = spacing.md, bottom = spacing.xs))
                }
                item(key = "archive") { PlaceCard(archived) { archivedId = it.id } }
            }
            item(key = "note") { Text(stringResource(R.string.places_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(horizontal = spacing.xxs, vertical = spacing.sm)) }
        }
    }
    if (creating) {
        PlaceNameDialog(stringResource(R.string.place_new), "", stringResource(R.string.action_create), onSave = { name ->
            onIntent(FinanceIntent.CreatePlace(name))
            creating = false
        }, onDismiss = { creating = false })
    }
    places.firstOrNull { it.id == editingId }?.let { place ->
        PlaceNameDialog(stringResource(R.string.place_title), place.name, stringResource(R.string.action_save), onSave = { name ->
            onIntent(FinanceIntent.RenamePlace(place.id, name))
            editingId = null
        }, onDismiss = { editingId = null }, onArchive = { archivingId = place.id })
    }
    places.firstOrNull { it.id == archivingId }?.let { place ->
        ConfirmDialog(
            title = stringResource(R.string.archive_confirm_title),
            text = stringResource(R.string.archive_confirm_place, place.name),
            confirm = stringResource(R.string.category_archive),
            tag = TAG_ARCHIVE_CONFIRM,
            onConfirm = {
                onIntent(FinanceIntent.ArchivePlace(place.id, archived = true))
                archivingId = null
                editingId = null
            },
            onDismiss = { archivingId = null },
        )
    }
    places.firstOrNull { it.id == archivedId }?.let { place ->
        ArchivedDialog(
            title = place.name,
            uses = place.uses,
            tag = TAG_ARCHIVED_WINDOW,
            onRestore = {
                onIntent(FinanceIntent.ArchivePlace(place.id, archived = false))
                archivedId = null
            },
            onDelete = { deletingId = place.id },
            onDismiss = { archivedId = null },
        )
    }
    places.firstOrNull { it.id == deletingId }?.let { place ->
        ConfirmDialog(
            title = stringResource(R.string.delete_forever_title),
            text = stringResource(R.string.delete_forever_place, place.name, place.uses),
            confirm = stringResource(R.string.action_delete),
            tag = TAG_DELETE_CONFIRM,
            danger = true,
            onConfirm = {
                onIntent(FinanceIntent.DeletePlace(place.id))
                deletingId = null
                archivedId = null
            },
            onDismiss = { deletingId = null },
        )
    }
}

@Composable
private fun PlaceCard(places: List<Place>, onClick: (Place) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
        places.forEachIndexed { i, place ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppTheme.sizes.operationRow)
                    .clickable(role = Role.Button) { onClick(place) }
                    .padding(horizontal = spacing.md)
                    .testTag("$TAG_PLACE:${place.id}"),
            ) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (place.archived) colors.textSecondary else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.inactive)
            }
            if (i != places.lastIndex) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
        }
    }
}

/** Окно места: новое (только «Создать») или правка («В архив» и «Сохранить»). Переименование меняет место во всех операциях. */
@Composable
fun PlaceNameDialog(title: String, initialName: String, saveLabel: String, onSave: (String) -> Unit, onDismiss: () -> Unit, onArchive: (() -> Unit)? = null) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focus = remember { FocusRequester() }
    if (initialName.isEmpty()) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    FinanceWindow(
        title = title,
        tag = TAG_PLACE_WINDOW,
        onDismiss = onDismiss,
        actions = {
            if (onArchive != null) SecondaryAction(stringResource(R.string.category_archive), TAG_PLACE_ARCHIVE, onArchive)
            PrimaryAction(saveLabel, TAG_PLACE_SAVE, onClick = { onSave(name.trim()) }, enabled = name.isNotBlank())
        },
    ) {
        FinanceField(stringResource(R.string.account_name)) {
            FieldInput(name, { name = it }, stringResource(R.string.place_name_hint), TAG_PLACE_NAME, Modifier.focusRequester(focus), bold = true)
        }
    }
}

const val TAG_PLACES = "finance_places"
const val TAG_PLACE = "place"
const val TAG_PLACE_ADD = "place_add"
const val TAG_PLACE_WINDOW = "place_window"
const val TAG_PLACE_NAME = "place_name"
const val TAG_PLACE_SAVE = "place_save"
const val TAG_PLACE_ARCHIVE = "place_archive"
