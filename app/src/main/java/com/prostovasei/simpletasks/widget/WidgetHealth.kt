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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.first

internal suspend fun healthWidgetViews(context: Context, today: LocalDate, bar: RemoteViews, size: DpSize): RemoteViews {
    val entries = context.widgetEntryPoint().health().observeEntries().first()
    val latest = entries.lastOrNull()
    val period = WeightPeriod.MONTH
    val widgetHeight = size.height.value.takeIf { it.isFinite() } ?: 280f
    val weightSize = if (widgetHeight < SizeTokens.WIDGET_MODULE_BAR_THRESHOLD / 2) 17f else 32f
    val points = WeightMath.inPeriod(entries, period, today)
    return RemoteViews(context.packageName, R.layout.widget_health_root).apply {
        removeAllViews(R.id.widget_health_bar)
        addView(R.id.widget_health_bar, bar)
        setTextViewTextSize(R.id.widget_health_weight, TypedValue.COMPLEX_UNIT_SP, weightSize)
        setTextViewText(R.id.widget_health_weight, latest?.let { "${Weight.format(it.tenths)} кг" }
            ?: context.getString(R.string.health_empty_title))
        val chartVisibility = if (points.isEmpty()) View.GONE else View.VISIBLE
        setViewVisibility(R.id.widget_health_chart, chartVisibility)
        setViewVisibility(R.id.widget_health_dates, chartVisibility)
        if (points.isEmpty()) {
            if (latest != null) setTextViewText(R.id.widget_health_weight, "${Weight.format(latest.tenths)} кг · за месяц записей нет")
        } else {
            val from = today.minusDays(requireNotNull(period.days).toLong())
            val metrics = context.resources.displayMetrics
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, weightSize, metrics)
            val weightMetrics = paint.fontMetricsInt
            paint.typeface = Typeface.DEFAULT
            paint.textSize = context.resources.getDimension(R.dimen.widget_health_date_text)
            val dateMetrics = paint.fontMetricsInt
            val labelsHeight = (weightMetrics.bottom - weightMetrics.top + dateMetrics.bottom - dateMetrics.top) / metrics.density
            val widgetWidth = size.width.value.takeIf { it.isFinite() } ?: 320f
            val chartWidth = (widgetWidth - 2 * SizeTokens.WIDGET_EDGE).coerceAtLeast(1f)
            val chartHeight = (widgetHeight - SizeTokens.WIDGET_BAR - labelsHeight).coerceAtLeast(1f)
            // Ограничиваем число пикселей, сохраняя пропорции и высоту графика на рабочем столе.
            val scale = sqrt(250_000f / (chartWidth * chartHeight)).coerceAtMost(2f)
            val width = (chartWidth * scale).roundToInt().coerceAtLeast(1)
            val height = (chartHeight * scale).roundToInt().coerceAtLeast(1)
            val chart = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            chart.density = Bitmap.DENSITY_NONE
            val canvas = Canvas(chart)
            paint.color = context.getColor(R.color.widget_flat_divider)
            repeat(3) { line ->
                val y = (line + 1) * height / 4f
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
            }
            paint.color = context.getColor(R.color.widget_flat_text_primary)
            paint.strokeWidth = 1.5f * scale
            paint.strokeCap = Paint.Cap.ROUND
            val min = points.minOf { it.tenths }
            val range = points.maxOf { it.tenths } - min
            val radius = minOf(2.5f * scale, width / 2f, height / 2f)
            val positions = points.map {
                val x = radius + (width - 2 * radius) * (it.date.toEpochDay() - from.toEpochDay()) / requireNotNull(period.days)
                val y = if (range == 0) height / 2f else height - radius - (height - 2 * radius) * (it.tenths - min) / range
                x to y
            }
            positions.zipWithNext().forEach { (a, b) -> canvas.drawLine(a.first, a.second, b.first, b.second, paint) }
            val end = positions.last()
            canvas.drawCircle(end.first, end.second, radius, paint)
            setImageViewBitmap(R.id.widget_health_chart, chart)
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))
            setTextViewText(R.id.widget_health_from, from.format(formatter))
            setTextViewText(R.id.widget_health_to, today.format(formatter))
            setContentDescription(R.id.widget_health_chart, "График веса за месяц. Последняя запись ${Weight.format(latest!!.tenths)} кг")
        }
    }
}
