package com.prostovasei.simpletasks.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightMath
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import java.time.LocalDate
import android.graphics.DashPathEffect
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import androidx.compose.ui.geometry.Size
import com.prostovasei.simpletasks.ui.feature.health.ChartGeometry
import com.prostovasei.simpletasks.ui.feature.health.healthGoalText
import com.prostovasei.simpletasks.ui.feature.health.dayMonth
import com.prostovasei.simpletasks.ui.feature.health.dayMonthYear
import com.prostovasei.simpletasks.ui.feature.health.dayMonthShort
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.first

internal suspend fun healthWidgetViews(context: Context, today: LocalDate, bar: RemoteViews, size: DpSize): RemoteViews {
    val settings = context.widgetEntryPoint().healthSettings().settings.value
    val goal = settings.goalTenths
    val entries = context.widgetEntryPoint().health().observeEntries().first()
    val latest = entries.lastOrNull()
    val period = WeightPeriod.MONTH
    val widgetHeight = size.height.value.takeIf { it.isFinite() } ?: 280f
    val weightSize = if (widgetHeight < SizeTokens.WIDGET_MODULE_BAR_THRESHOLD / 2) 17f else 32f
    val points = WeightMath.inPeriod(entries, period, today)
    val (goalTitle, goalSubtitle) = if (latest == null) "" to null else
        healthGoalText(context, entries, settings, today) { if (it.year == today.year) dayMonth(it) else dayMonthYear(it) }
    val title = goalTitle.takeIf { goal != null && latest != null }
    val details = if (widgetHeight >= 160) (if (goal == null && settings.heightCm != null) goalTitle else goalSubtitle.takeIf { goal != null }) else null
    return RemoteViews(context.packageName, R.layout.widget_health_root).apply {
        removeAllViews(R.id.widget_health_bar)
        addView(R.id.widget_health_bar, bar)
        setTextViewTextSize(R.id.widget_health_weight, TypedValue.COMPLEX_UNIT_SP, weightSize)
        setTextViewText(R.id.widget_health_weight, latest?.let {
            val value = Weight.format(it.tenths)
            SpannableString("$value кг").apply { setSpan(RelativeSizeSpan(.56f), value.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
            ?: context.getString(R.string.health_empty_title))
        setTextViewText(R.id.widget_health_goal, title)
        setViewVisibility(R.id.widget_health_goal, if (title == null) View.GONE else View.VISIBLE)
        setTextViewText(R.id.widget_health_details, details)
        setViewVisibility(R.id.widget_health_details, if (details.isNullOrBlank()) View.GONE else View.VISIBLE)
        val chartVisibility = if (points.isEmpty()) View.GONE else View.VISIBLE
        setViewVisibility(R.id.widget_health_chart, chartVisibility)
        val showDates = widgetHeight >= 120f || goal == null
        setViewVisibility(R.id.widget_health_dates, if (showDates) chartVisibility else View.GONE)
        if (points.isEmpty()) {
            if (latest != null) setTextViewText(R.id.widget_health_weight, "${Weight.format(latest.tenths)} кг · за месяц записей нет")
        } else {
            val metrics = context.resources.displayMetrics
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, weightSize, metrics)
            val weightMetrics = paint.fontMetricsInt
            paint.typeface = Typeface.DEFAULT
            paint.textSize = context.resources.getDimension(R.dimen.widget_health_date_text)
            val dateMetrics = paint.fontMetricsInt
            val widgetWidth = size.width.value.takeIf { it.isFinite() } ?: 320f
            val detailLayout = details?.takeUnless { it.isBlank() }?.let {
                android.text.StaticLayout.Builder.obtain(it, 0, it.length, android.text.TextPaint(paint),
                    ((widgetWidth - 2 * SizeTokens.WIDGET_EDGE) * metrics.density).roundToInt().coerceAtLeast(1))
                    .setMaxLines(2).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
            }
            val dateHeight = if (showDates) dateMetrics.bottom - dateMetrics.top else 0
            val titleHeight = if (title == null) 0 else {
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.fontMetricsInt.let { it.bottom - it.top }
            }
            val labelsHeight = (weightMetrics.bottom - weightMetrics.top + dateHeight + titleHeight + (detailLayout?.height ?: 0)) / metrics.density
            val chartWidth = (widgetWidth - 2 * SizeTokens.WIDGET_EDGE).coerceAtLeast(1f)
            val chartHeight = (widgetHeight - SizeTokens.WIDGET_BAR - labelsHeight).coerceAtLeast(1f)
            // Ограничиваем число пикселей, сохраняя пропорции и высоту графика на рабочем столе.
            val scale = sqrt(250_000f / (chartWidth * chartHeight)).coerceAtMost(2f)
            val width = (chartWidth * scale).roundToInt().coerceAtLeast(1)
            val height = (chartHeight * scale).roundToInt().coerceAtLeast(1)
            val chart = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            chart.density = Bitmap.DENSITY_NONE
            val canvas = Canvas(chart)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = (context.resources.getDimension(R.dimen.widget_health_date_text) / metrics.density) * scale
            val labels = points.map { it.tenths } + listOfNotNull(goal)
            val axis = labels.maxOf { paint.measureText(Weight.format(it)) } + 8f * scale
            val radius = minOf(2.5f * scale, height / 2f)
            val yPadding = minOf(paint.textSize * .6f, height / 3f)
            val geometry = ChartGeometry(Size(width.toFloat(), height.toFloat()), points, period, today,
                goal, left = radius, right = axis, top = yPadding, bottom = yPadding)
            val right = width - axis
            val goalY = goal?.let { geometry.y(it.toFloat()) }
            val goalColor = com.prostovasei.simpletasks.designsystem.tokens.ColorTokens.goal.light.toInt()
            var previousY: Float? = null
            fun axisLabel(label: String, y: Float, color: Int) {
                paint.color = color
                canvas.drawText(label, right + 4f * scale, y - (paint.ascent() + paint.descent()) / 2f, paint)
            }
            geometry.gridKg().forEach { kg ->
                val y = geometry.y(kg * 10f)
                if (goalY?.let { abs(it - y) < paint.textSize } == true ||
                    previousY?.let { abs(it - y) < paint.textSize } == true) return@forEach
                paint.color = context.getColor(R.color.widget_flat_divider)
                canvas.drawLine(radius, y, right, y, paint)
                axisLabel(kg.toString(), y, context.getColor(R.color.widget_flat_text_secondary))
                previousY = y
            }
            if (goal != null && goalY != null) {
                paint.color = goalColor
                paint.strokeWidth = scale
                paint.pathEffect = DashPathEffect(floatArrayOf(4f * scale, 4f * scale), 0f)
                canvas.drawLine(radius, goalY, right, goalY, paint)
                paint.pathEffect = null
                axisLabel(Weight.format(goal), goalY, goalColor)
            }
            paint.color = context.getColor(R.color.widget_flat_text_primary)
            paint.strokeWidth = 1.5f * scale
            paint.strokeCap = Paint.Cap.ROUND
            val positions = geometry.points.map { it.at }
            positions.zipWithNext().forEach { (a, b) -> canvas.drawLine(a.x, a.y, b.x, b.y, paint) }
            positions.forEach { canvas.drawCircle(it.x, it.y, radius, paint) }
            setImageViewBitmap(R.id.widget_health_chart, chart)
            setTextViewText(R.id.widget_health_from, dayMonthShort(geometry.from))
            setTextViewText(R.id.widget_health_to, dayMonthShort(today))
            setViewPadding(R.id.widget_health_dates, (radius / scale * metrics.density).roundToInt(), 0,
                (axis / scale * metrics.density).roundToInt(), 0)
            setContentDescription(R.id.widget_health_chart, "График веса за месяц. Последняя запись ${Weight.format(latest!!.tenths)} кг")
        }
    }
}
