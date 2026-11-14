package com.prostovasei.simpletasks.ui.feature.finance

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Operation
import java.time.LocalDate

@Composable
fun FinanceTrashRoute(viewModel: FinanceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceTrashScreen(state.trash, state.today, viewModel::onIntent, onBack)
}

/**
 * Корзина финансов (этап 4, #231; экраны 3.5–3.6): отдельный экран со стрелкой назад. У каждой операции — «восстановить»
 * и «удалить навсегда», сверху «Очистить». Оба удаления — с подтверждением; сами операции из корзины не исчезают (решение PM).
 */
@Composable
fun FinanceTrashScreen(trash: List<Operation>, today: LocalDate, onIntent: (FinanceIntent) -> Unit, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var clearing by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_FINANCE_TRASH)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = spacing.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary) }
            Text(stringResource(R.string.finance_trash), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
            if (trash.isNotEmpty()) {
                Text(
                    stringResource(R.string.trash_clear),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.clip(AppTheme.shapes.medium).clickable(role = Role.Button) { clearing = true }.padding(spacing.xs).testTag(TAG_TRASH_CLEAR),
                )
            }
        }
        if (trash.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl)) {
                Text(stringResource(R.string.trash_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(stringResource(R.string.trash_empty_text), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = spacing.xs))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.xs, bottom = spacing.lg)) {
                item(key = "list") {
                    Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                        trash.forEachIndexed { i, operation ->
                            TrashRow(operation, today, onRestore = { onIntent(FinanceIntent.RestoreOperation(operation.id)) }, onDelete = { deletingId = operation.id })
                            if (i != trash.lastIndex) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                        }
                    }
                }
                item(key = "note") {
                    Text(stringResource(R.string.trash_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(horizontal = spacing.xxs, vertical = spacing.sm))
                }
            }
        }
    }
    if (clearing) {
        ConfirmDialog(
            title = stringResource(R.string.trash_clear_title),
            text = stringResource(R.string.trash_clear_text, trash.size),
            confirm = stringResource(R.string.action_delete),
            tag = TAG_TRASH_CONFIRM,
            onConfirm = {
                onIntent(FinanceIntent.EmptyTrash)
                clearing = false
            },
            onDismiss = { clearing = false },
        )
    }
    deletingId?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.trash_delete_title),
            text = stringResource(R.string.trash_delete_text),
            confirm = stringResource(R.string.action_delete),
            tag = TAG_TRASH_CONFIRM,
            onConfirm = {
                onIntent(FinanceIntent.DeleteForever(id))
                deletingId = null
            },
            onDismiss = { deletingId = null },
        )
    }
}

@Composable
private fun TrashRow(operation: Operation, today: LocalDate, onRestore: () -> Unit, onDelete: () -> Unit) {
    val deleted = operation.deletedOn?.let { stringResource(R.string.finance_trash_deleted_on, shortDay(it, today)) }
    val details = listOfNotNull(plainDay(operation.date, today), deleted, operation.lineDetails()).joinToString(" · ")
    OperationLine(operation, details, Modifier.testTag("$TAG_FINANCE_TRASH:${operation.id}")) {
        MiniButton(R.drawable.ic_snack_restore, stringResource(R.string.finance_trash_restore), "$TAG_TRASH_RESTORE:${operation.id}", onRestore)
        MiniButton(R.drawable.ic_tool_delete, stringResource(R.string.finance_trash_delete_forever), "$TAG_TRASH_DELETE:${operation.id}", onDelete)
    }
}

@Composable
private fun MiniButton(@DrawableRes icon: Int, label: String, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AppTheme.sizes.financeClose)
            .clip(AppTheme.shapes.medium)
            .background(colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
    ) { Icon(painterResource(icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.accountAddIcon)) }
}

const val TAG_FINANCE_TRASH = "finance_trash"
const val TAG_TRASH_CLEAR = "finance_trash_clear"
const val TAG_TRASH_RESTORE = "finance_trash_restore"
const val TAG_TRASH_DELETE = "finance_trash_delete"
const val TAG_TRASH_CONFIRM = "finance_trash_confirm"
