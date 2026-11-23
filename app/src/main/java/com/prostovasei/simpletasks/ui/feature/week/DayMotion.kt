package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.ui.common.DONE_CHECK_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * #314 — анимации окна дня поверх рабочего стола (идеи PM 1, 2, 4, 8, 11, 12; макет design/approved/314).
 * Длительности взяты из согласованного макета.
 */

/** 1 «Рост из ячейки»: затемнение, рост плашки из ячейки виджета, затем проявление содержимого. */
internal const val DAY_SCRIM_MS = 300
internal const val DAY_GROW_MS = 520
internal const val DAY_REVEAL_DELAY_MS = 380
internal const val DAY_REVEAL_MS = 250
internal val DAY_GROW_EASING = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)

/** Без места ячейки (открыли не из виджета) окно выезжает снизу и уезжает вниз, как окно задачи. */
internal const val DAY_SLIDE_MS = 240
internal const val DAY_SLIDE_CLOSE_MS = 160
internal val DAY_SLIDE_EASING = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** 11 «Закрытие в ячейку»: содержимое гаснет, окно сжимается в ячейку, затемнение растворяется, затем плашка исчезает. */
internal const val DAY_HIDE_MS = 160
internal const val DAY_SHRINK_MS = 460
internal const val DAY_SCRIM_OUT_DELAY_MS = 120
internal const val DAY_SCRIM_OUT_MS = 420
internal const val DAY_SHRINK_FADE_MS = 120
internal const val DAY_SHRINK_ALPHA = 0.6f
internal val DAY_SHRINK_EASING = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** 2 «Каскад задач»: строки поднимаются по очереди с лёгкой пружиной, шаг 40 мс. */
internal const val CASCADE_STEP_MS = 40
internal const val CASCADE_WINDOW_MS = 600
private const val CASCADE_DAMPING = 0.6f

/** 4 «Праздник дня». */
private const val SPARK_COUNT = 26
private const val SPARK_MS = 900
private const val SPARK_SPREAD_MS = 400
private const val SPARK_TURN = 360f
private const val SPARK_CORNER = 0.3f
private val SPARK_EASING = CubicBezierEasing(0.1f, 0.7f, 0.3f, 1f)
private const val BOUNCE_UP_MS = 140
private const val BOUNCE_DAMPING = 0.4f

/** 12 «Резинка»: сопротивление растяжению (как у прокрутки iOS). */
private const val RUBBER = 0.55f

/** Ожидание, которое, как и анимации, ускоряется настройкой «Анимация» Android; при выключенных анимациях — без ожидания. */
internal suspend fun motionDelay(ms: Long) {
    val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    if (scale > 0f && ms > 0) delay((ms * scale).toLong())
}

/** Растяжение резинки: чем дальше тянут, тем меньше окно поддаётся; не больше [limit]. */
internal fun rubber(pulled: Float, limit: Float): Float =
    if (pulled <= 0f || limit <= 0f) 0f else limit * (1f - 1f / (pulled * RUBBER / limit + 1f))

/** Каскад одного дня: пока [open], новые строки появляются по очереди, после — сразу (прокрутка, новые задачи). */
@Stable
internal class Cascade(val startMs: Int) {
    var open by mutableStateOf(true)
}

/** Строка, которая поднимается снизу на [SizeTokens.DAY_CASCADE_RISE] и проявляется в свою очередь [index]. */
@Composable
internal fun CascadeRow(cascade: Cascade, index: Int, content: @Composable () -> Unit) {
    val rise = with(LocalDensity.current) { SizeTokens.DAY_CASCADE_RISE.dp.toPx() }
    val shown = remember { Animatable(if (cascade.open) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (shown.value < 1f) {
            motionDelay((cascade.startMs + index * CASCADE_STEP_MS).toLong())
            shown.animateTo(1f, spring(dampingRatio = CASCADE_DAMPING, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Box(Modifier.graphicsLayer { alpha = shown.value.coerceIn(0f, 1f); translationY = (1f - shown.value) * rise }) { content() }
}

/** «Праздник дня»: откуда летят искры ([at] — в координатах окна) и номер праздника — новый номер запускает новые искры. */
@Stable
internal class Celebration {
    var at by mutableStateOf<Offset?>(null)
        private set
    var count by mutableIntStateOf(0)
        private set

    fun fire(from: Offset) {
        at = from
        count++
    }

    fun done() {
        at = null
    }
}

private class Spark(val angle: Float, val distance: Float, val turn: Float, val life: Float, val color: Color)

/**
 * Искры из кружка последней задачи дня (идея 4): после того как дорисовалась галочка, [SPARK_COUNT] искр цветов приложения
 * разлетаются и гаснут, окно подпрыгивает ([bounce] 0 → 1 → 0).
 */
@Composable
internal fun Sparks(celebration: Celebration, bounce: Animatable<Float, *>) {
    val at = celebration.at ?: return
    val count = celebration.count
    val density = LocalDensity.current
    val near = with(density) { SizeTokens.DAY_SPARK_NEAR.dp.toPx() }
    val far = with(density) { SizeTokens.DAY_SPARK_FAR.dp.toPx() }
    val lift = with(density) { SizeTokens.DAY_SPARK_LIFT.dp.toPx() }
    val side = with(density) { SizeTokens.DAY_SPARK.dp.toPx() }
    val sparks = remember(count) {
        val random = Random(count)
        val colors = TaskCanvasTokens.celebration
        List(SPARK_COUNT) { i ->
            Spark(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                distance = near + random.nextFloat() * (far - near),
                turn = random.nextFloat() * SPARK_TURN,
                life = (SPARK_MS + random.nextFloat() * SPARK_SPREAD_MS) / (SPARK_MS + SPARK_SPREAD_MS),
                color = Color(colors[i % colors.size]),
            )
        }
    }
    val time = remember(count) { Animatable(0f) }
    LaunchedEffect(count) {
        motionDelay(DONE_CHECK_MS.toLong())
        launch {
            bounce.animateTo(1f, tween(BOUNCE_UP_MS, easing = FastOutSlowInEasing))
            bounce.animateTo(0f, spring(dampingRatio = BOUNCE_DAMPING, stiffness = Spring.StiffnessMedium))
        }
        time.animateTo(1f, tween(SPARK_MS + SPARK_SPREAD_MS, easing = LinearEasing))
        celebration.done()
    }
    Canvas(Modifier.fillMaxSize().testTag(TAG_DAY_CELEBRATION)) {
        val now = time.value
        if (now <= 0f) return@Canvas
        sparks.forEach { spark ->
            val t = (now / spark.life).coerceIn(0f, 1f)
            if (t >= 1f) return@forEach
            val e = SPARK_EASING.transform(t)
            val center = Offset(at.x + cos(spark.angle) * spark.distance * e, at.y + (sin(spark.angle) * spark.distance - lift) * e)
            rotate(spark.turn * e, center) {
                drawRoundRect(
                    color = spark.color,
                    topLeft = center - Offset(side / 2f, side / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(side * SPARK_CORNER),
                    alpha = 1f - t,
                )
            }
        }
    }
}

const val TAG_DAY_CELEBRATION = "day_sheet_celebration"
