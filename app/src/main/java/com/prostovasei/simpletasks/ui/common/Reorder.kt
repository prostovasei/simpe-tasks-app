package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Перетаскивание задач (#357, решение PM): нажал и повёл — задача переносится (при ручной сортировке) или спрашивается
 * «Переключить на ручную?»; нажал и отпустил — режим выбора, как раньше. Переносятся строки блока с ключами `[keyPrefix]:id`.
 */
@Stable
class ReorderController internal constructor(private val listState: LazyListState, private val scope: CoroutineScope) {
    /** Ручная сортировка: перенос разрешён; иначе — вопрос [onAsk]. */
    var manual by mutableStateOf(false)
    var keyPrefix = "flat"
    var ids: List<String> = emptyList()
    var onAsk: () -> Unit = {}
    var onDrop: (List<String>) -> Unit = {}

    var draggingId by mutableStateOf<String?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set
    private val order = mutableStateListOf<String>()

    fun start(id: String) {
        order.clear()
        order.addAll(ids)
        offset = 0f
        draggingId = id
    }

    fun drag(dy: Float) {
        val id = draggingId ?: return
        offset += dy
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == "$keyPrefix:$id" } ?: return
        val center = current.offset + offset + current.size / 2f
        val target = visible.firstOrNull { info ->
            val key = info.key as? String ?: return@firstOrNull false
            key != current.key && key.startsWith("$keyPrefix:") && center >= info.offset && center <= info.offset + info.size
        } ?: return
        val from = order.indexOf(id)
        val to = order.indexOf((target.key as String).removePrefix("$keyPrefix:"))
        if (from < 0 || to < 0) return
        // Ленту держит первая видимая строка: если двигаем её, прокрутку возвращаем, чтобы список не прыгал.
        val first = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        order.add(to, order.removeAt(from))
        offset += current.offset - target.offset
        if (current.index == first || target.index == first) scope.launch { listState.scrollToItem(first, firstOffset) }
    }

    fun end() {
        if (draggingId == null) return
        val result = order.toList()
        draggingId = null
        offset = 0f
        order.clear()
        if (result != ids) onDrop(result)
    }

    fun cancel() {
        draggingId = null
        offset = 0f
        order.clear()
    }

    /** Порядок для показа: во время переноса — живой. */
    fun <T> arrange(items: List<T>, id: (T) -> String): List<T> {
        if (draggingId == null) return items
        val byId = items.associateBy(id)
        return order.mapNotNull { byId[it] } + items.filter { id(it) !in order }
    }
}

@Composable
fun rememberReorder(listState: LazyListState): ReorderController {
    val scope = rememberCoroutineScope()
    return remember(listState) { ReorderController(listState, scope) }
}

/**
 * Нажатия строки с перетаскиванием (#357): короткое — [onClick], долгое и отпустил — [onLongClick] (выбор),
 * долгое и повёл — перенос или вопрос о ручной сортировке.
 */
internal fun Modifier.reorderGestures(
    id: String,
    reorder: ReorderController,
    onClick: State<() -> Unit>,
    onLongClick: State<(() -> Unit)?>,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    this
        .indication(interaction, LocalIndication.current)
        .semantics {
            onClick { onClick.value(); true }
            onLongClick.value?.let { long -> onLongClick { long(); true } }
        }
        .pointerInput(id, reorder) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val press = PressInteraction.Press(down.position)
                interaction.tryEmit(press)
                val long = awaitLongPressOrCancellation(down.id)
                if (long == null) {
                    val up = currentEvent.changes.firstOrNull { it.id == down.id }
                    val tapped = up != null && up.changedToUp()
                    interaction.tryEmit(if (tapped) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                    if (tapped) {
                        up?.consume()
                        onClick.value()
                    }
                    return@awaitEachGesture
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                var total = Offset.Zero
                var dragging = false
                var asked = false
                try {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            break
                        }
                        val delta = change.positionChange()
                        change.consume()
                        if (asked) continue
                        total += delta
                        if (!dragging && total.getDistance() > viewConfiguration.touchSlop) {
                            interaction.tryEmit(PressInteraction.Cancel(press))
                            if (reorder.manual) {
                                dragging = true
                                reorder.start(id)
                            } else {
                                asked = true
                                reorder.onAsk()
                            }
                        }
                        if (dragging) reorder.drag(delta.y)
                    }
                } catch (e: CancellationException) {
                    if (dragging) reorder.cancel()
                    throw e
                }
                when {
                    dragging -> reorder.end()
                    asked -> Unit
                    else -> {
                        interaction.tryEmit(PressInteraction.Release(press))
                        onLongClick.value?.invoke()
                    }
                }
            }
        }
}
