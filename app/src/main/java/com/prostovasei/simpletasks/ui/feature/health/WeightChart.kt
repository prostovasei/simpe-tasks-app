package com.prostovasei.simpletasks.ui.feature.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Точка графика: запись и её место на холсте. */
internal data class ChartPoint(val entry: WeightEntry, val at: Offset)

/** Область графика и отображение «дата, вес → пиксели»; одна и та же для рисования и для нажатий. */
internal class ChartGeometry(
    size: Size,
    entries: List<WeightEntry>,
    period: WeightPeriod,
    today: LocalDate,
    goalTenths: Int?,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
) {
    val from: LocalDate = when (val days = period.days) {
        null -> entries.minOfOrNull { it.date } ?: today
        else -> today.minusDays(days.toLong())
    }
    private val spanDays = max(1L, ChronoUnit.DAYS.between(from, today)).toFloat()
    val minTenths: Float
    val maxTenths: Float
    val plot = Size(size.width - left - right, size.height - top - bottom)
    private val origin = Offset(left, top)

    init {
        val values = entries.map { it.tenths } + listOfNotNull(goalTenths)
        var lo = (values.minOrNull() ?: Weight.DEFAULT_TENTHS) - PAD_TENTHS
        var hi = (values.maxOrNull() ?: Weight.DEFAULT_TENTHS) + PAD_TENTHS
        if (hi - lo < MIN_RANGE) {
            val mid = (hi + lo) / 2
            lo = mid - MIN_RANGE / 2
            hi = mid + MIN_RANGE / 2
        }
        minTenths = lo.toFloat()
        maxTenths = hi.toFloat()
    }

    fun x(date: LocalDate): Float = origin.x + plot.width * (ChronoUnit.DAYS.between(from, date) / spanDays)

    fun y(tenths: Float): Float = origin.y + plot.height * ((maxTenths - tenths) / (maxTenths - minTenths))

    val points: List<ChartPoint> = entries.map { ChartPoint(it, Offset(x(it.date), y(it.tenths.toFloat()))) }

    /** Подписи оси: целые килограммы, шаг подбирается по размаху. */
    fun gridKg(): List<Int> {
        val range = (maxTenths - minTenths) / 10f
        val step = when {
            range <= 6 -> 1
            range <= 12 -> 2
            range <= 30 -> 5
            else -> 10
        }
        val first = ceil(minTenths / 10f / step).toInt() * step
        val last = floor(maxTenths / 10f / step).toInt() * step
        return (first..last step step).toList()
    }

    private companion object {
        const val PAD_TENTHS = 4
        const val MIN_RANGE = 20
    }
}

/**
 * График веса (#257, макет 1.1): линия по записям периода, последняя точка закрашена, цель — пунктир с подписью справа, подписи оси
 * по килограммам и три даты внизу. Нажатие на точку — подсказка «вес · дата» (макет 1.2); повторное — правка записи.
 */
@Composable
fun WeightChart(
    entries: List<WeightEntry>,
    period: WeightPeriod,
    today: LocalDate,
    goalTenths: Int?,
    tip: LocalDate?,
    onTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelSmall
    val tipValue = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val description = stringResource(R.string.health_chart)
    val kgText = stringResource(R.string.health_kg)
    val axis = with(density) { sizes.healthChartAxis.toPx() }
    val big = with(density) { sizes.healthPointLast.toPx() }
    val small = with(density) { sizes.healthPoint.toPx() }
    val line = with(density) { sizes.healthLine.toPx() }
    val pad = with(density) { AppTheme.spacing.xs.toPx() }
    val labelRoom = with(density) { AppTheme.spacing.md.toPx() }
    val tipHeight = with(density) { sizes.healthTipHeight.toPx() }
    val tipWidth = with(density) { sizes.healthTipWidth.toPx() }

    fun geometry(size: Size) = ChartGeometry(size, entries, period, today, goalTenths, left = big + pad, right = axis, top = big + pad, bottom = labelRoom + pad)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(sizes.healthChartHeight)
            .semantics { contentDescription = description }
            .testTag(TAG_HEALTH_CHART)
            .pointerInput(entries, period, goalTenths) {
                detectTapGestures { offset ->
                    val g = geometry(Size(this.size.width.toFloat(), this.size.height.toFloat()))
                    val hit = g.points.minByOrNull { abs(it.at.x - offset.x) }
                    if (hit != null && abs(hit.at.x - offset.x) <= TAP_RADIUS_DP * density.density) onTap(hit.entry.date)
                }
            },
    ) {
        val g = geometry(size)
        val right = size.width - axis
        // Сетка и подписи оси.
        g.gridKg().forEach { kg ->
            val y = g.y(kg * 10f)
            drawLine(colors.divider, Offset(big + pad, y), Offset(right, y), strokeWidth = sizes.hairline.toPx())
            drawText(measurer, kg.toString(), Offset(right + pad, y - measurer.measure(kg.toString(), label).size.height / 2f), style = label.copy(color = colors.inactive))
        }
        // Цель — пунктир и подпись цветом цели.
        goalTenths?.let {
            val y = g.y(it.toFloat())
            drawLine(colors.goal, Offset(big + pad, y), Offset(right, y), strokeWidth = line * GOAL_LINE, pathEffect = PathEffect.dashPathEffect(floatArrayOf(pad, pad)))
            val text = Weight.format(it)
            drawText(measurer, text, Offset(right + pad, y - measurer.measure(text, label).size.height / 2f), style = label.copy(color = colors.goal, fontWeight = FontWeight.Bold))
        }
        // Линия и точки.
        if (g.points.size > 1) {
            val path = Path()
            g.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.at.x, p.at.y) else path.lineTo(p.at.x, p.at.y) }
            drawPath(path, colors.textPrimary, style = Stroke(width = line, join = StrokeJoin.Round))
        }
        g.points.forEachIndexed { i, p ->
            val lastPoint = i == g.points.lastIndex
            drawCircle(colors.background, if (lastPoint) big else small, p.at)
            if (lastPoint) drawCircle(colors.textPrimary, big, p.at) else drawCircle(colors.textPrimary, small, p.at, style = Stroke(width = line * DOT_STROKE))
        }
        // Подписи дат: начало, середина, конец периода.
        val baseline = size.height - labelRoom
        listOf(g.from, g.from.plusDays(ChronoUnit.DAYS.between(g.from, today) / 2), today).distinct().forEachIndexed { i, date ->
            val text = dayMonthShort(date)
            val m = measurer.measure(text, label)
            val x = (g.x(date) - m.size.width / 2f).coerceIn(big + pad - m.size.width / 2f, right - m.size.width.toFloat())
            drawText(m, color = colors.inactive, topLeft = Offset(if (i == 0) big + pad else x, baseline + pad))
        }
        // Подсказка у выбранной точки.
        g.points.firstOrNull { it.entry.date == tip }?.let { p ->
            drawLine(colors.inactive, Offset(p.at.x, big + pad), Offset(p.at.x, size.height - labelRoom - pad), strokeWidth = sizes.hairline.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(small, small)))
            drawCircle(colors.textPrimary, big + small, p.at)
            drawCircle(colors.background, small, p.at)
            drawTip(measurer, "${Weight.format(p.entry.tenths)} $kgText", dayMonth(p.entry.date), p.at, tipWidth, tipHeight, pad, colors.accent, colors.onAccent, tipValue, label, CornerRadius(pad))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTip(
    measurer: TextMeasurer,
    value: String,
    date: String,
    at: Offset,
    width: Float,
    height: Float,
    pad: Float,
    background: androidx.compose.ui.graphics.Color,
    onBackground: androidx.compose.ui.graphics.Color,
    valueStyle: androidx.compose.ui.text.TextStyle,
    dateStyle: androidx.compose.ui.text.TextStyle,
    corner: CornerRadius,
) {
    val v: TextLayoutResult = measurer.measure(value, valueStyle.copy(color = onBackground))
    val d: TextLayoutResult = measurer.measure(date, dateStyle.copy(color = onBackground))
    val w = max(width, max(v.size.width, d.size.width) + pad * 2)
    val x = (at.x - w / 2).coerceIn(0f, max(0f, size.width - w))
    val above = at.y - height - pad
    val y = if (above >= 0f) above else at.y + pad
    drawRoundRect(background, Offset(x, y), Size(w, height), corner)
    drawText(v, topLeft = Offset(x + (w - v.size.width) / 2, y + pad / 2))
    drawText(d, topLeft = Offset(x + (w - d.size.width) / 2, y + pad / 2 + v.size.height))
}

private const val TAP_RADIUS_DP = 24f
private const val GOAL_LINE = 0.7f
private const val DOT_STROKE = 0.8f
const val TAG_HEALTH_CHART = "health_chart"
