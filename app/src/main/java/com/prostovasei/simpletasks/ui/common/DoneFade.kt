package com.prostovasei.simpletasks.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Выполненная задача (#316, вариант PM 10 «Медленно тает»): без зачёркивания — задача и все её элементы полупрозрачные
 * ([DONE_ALPHA]) и стоят на месте. Переход: галочка появляется за [DONE_CHECK_MS], затем задача плавно тает за [DONE_FADE_MS].
 * Возврат в работу — быстро, за [UNDONE_MS]. Уже выполненная при показе экрана — сразу полупрозрачная, без анимации.
 */
const val DONE_ALPHA = 0.4f
const val DONE_CHECK_MS = 420
const val DONE_FADE_MS = 800
const val UNDONE_MS = 250

/** Сколько длится весь переход к выполненной — столько отмеченная задача держится на экране, где выполненные не показываются. */
const val DONE_TOTAL_MS = DONE_CHECK_MS + DONE_FADE_MS

/** Прозрачность задачи: 1 — обычная, [DONE_ALPHA] — выполненная; переход «медленно тает». */
@Composable
fun doneAlpha(done: Boolean, label: String = "doneAlpha"): Float {
    val alpha by animateFloatAsState(
        targetValue = if (done) DONE_ALPHA else 1f,
        animationSpec = if (done) tween(DONE_FADE_MS, delayMillis = DONE_CHECK_MS, easing = FastOutSlowInEasing) else tween(UNDONE_MS),
        label = label,
    )
    return alpha
}

/** Доля появления галочки 0..1 — за [DONE_CHECK_MS]; при возврате в работу — сразу. */
@Composable
fun doneCheck(done: Boolean, label: String = "doneCheck"): Float {
    val check by animateFloatAsState(if (done) 1f else 0f, tween(if (done) DONE_CHECK_MS else UNDONE_MS), label = label)
    return check
}
