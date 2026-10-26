package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Нажатие «Желе» (0.29, Н5 — выбор PM в прототипе; 0.29.1 — сильнее): под пальцем плитка или кнопка сжимается,
 * после — колышется (шире-уже, два раза), без подсветки и без вибрации. [jelly] ставится до фона, чтобы колыхалась вся кнопка,
 * [jellyClick] — вместо `clickable`.
 */
@Stable
class Jelly internal constructor() {
    internal val interaction = MutableInteractionSource()
    internal val x = Animatable(1f)
    internal val y = Animatable(1f)
}

@Composable
fun rememberJelly(): Jelly {
    val jelly = remember { Jelly() }
    LaunchedEffect(jelly) {
        coroutineScope {
            jelly.interaction.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        launch { jelly.x.animateTo(PRESSED, tween(PRESS_MS)) }
                        launch { jelly.y.animateTo(PRESSED, tween(PRESS_MS)) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        launch { jelly.x.animateTo(1f, wobble(1.16f, 0.92f, 1.06f, 0.98f)) }
                        launch { jelly.y.animateTo(1f, wobble(0.86f, 1.10f, 0.95f, 1.02f)) }
                    }
                }
            }
        }
    }
    return jelly
}

/** Шире — уже — снова шире — почти на месте — на месте. */
private fun wobble(vararg steps: Float) = keyframes {
    durationMillis = WOBBLE_MS
    steps.forEachIndexed { i, value -> value at WOBBLE_MS * (i + 1) / (steps.size + 1) }
}

fun Modifier.jelly(jelly: Jelly): Modifier = graphicsLayer {
    scaleX = jelly.x.value
    scaleY = jelly.y.value
}

fun Modifier.jellyClick(jelly: Jelly, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(interactionSource = jelly.interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)

private const val PRESS_MS = 80
private const val WOBBLE_MS = 520
private const val PRESSED = 0.88f
