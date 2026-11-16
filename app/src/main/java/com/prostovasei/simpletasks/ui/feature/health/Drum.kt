package com.prostovasei.simpletasks.ui.feature.health

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.filter

/** Сколько строк видно в барабане: выбранная посередине и по две сверху и снизу. */
private const val VISIBLE_ROWS = 5
private const val SIDE_ROWS = 2

/**
 * Барабан выбора числа (#257, макеты design/approved/257, решение PM 16.11.2026): значения [from]..[to] прокручиваются по вертикали,
 * выбранное — крупное посередине в серой плашке, соседние бледнее. Прокрутка прилипает к значению; [onSelect] зовётся, когда барабан остановился.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Drum(
    from: Int,
    to: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    tag: String,
    description: String,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val item = AppTheme.sizes.healthDrumItem
    val itemPx = with(LocalDensity.current) { item.toPx() }
    val count = to - from + 1
    val start = (selected - from).coerceIn(0, count - 1)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val center by remember { derivedStateOf { (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0).coerceIn(0, count - 1) } }

    // Барабан остановился на другом значении — сообщаем его.
    val current by rememberUpdatedState(selected)
    val report by rememberUpdatedState(onSelect)
    LaunchedEffect(state, from, count) {
        snapshotFlow { state.isScrollInProgress to center }
            .filter { !it.first }
            .collect { (_, index) -> if (from + index != current) report(from + index) }
    }
    // Значение изменили снаружи (например, подставили последний вес) — барабан переезжает к нему.
    LaunchedEffect(selected) {
        val index = (selected - from).coerceIn(0, count - 1)
        if (!state.isScrollInProgress && center != index) state.scrollToItem(index)
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .width(width)
            .height(item * VISIBLE_ROWS)
            .semantics { contentDescription = description; stateDescription = (from + center).toString() }
            .testTag(tag),
    ) {
        Box(Modifier.fillMaxWidth().height(item).align(Alignment.Center).clip(AppTheme.shapes.large).background(colors.selected))
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = item * SIDE_ROWS),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(List(count) { from + it }) { index, value ->
                val distance = kotlin.math.abs(index - center)
                Box(Modifier.height(item).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        value.toString(),
                        style = when (distance) {
                            0 -> MaterialTheme.typography.headlineMedium
                            1 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                        color = when (distance) {
                            0 -> colors.textPrimary
                            1 -> colors.textSecondary
                            else -> colors.inactive
                        },
                    )
                }
            }
        }
    }
}
