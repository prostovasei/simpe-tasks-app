package com.prostovasei.simpletasks.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.unit.DpSize
import androidx.core.net.toUri
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.MonthSpending
import com.prostovasei.simpletasks.ui.feature.finance.axisAmount
import com.prostovasei.simpletasks.ui.feature.finance.compactAmount
import com.prostovasei.simpletasks.ui.feature.finance.financeLimitPreferences
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal fun financeWidgetViews(context: Context, id: Int, bar: RemoteViews, size: DpSize): RemoteViews {
    val width = size.width.value.takeIf { it.isFinite() }?.toInt() ?: 320
    val height = size.height.value.takeIf { it.isFinite() }?.toInt() ?: 640
    val adapter = Intent(context, WidgetFinanceService::class.java)
        .setData("simpletasks://widget/$id/finance/$width/$height".toUri())
        .putExtra("width", width)
        .putExtra("height", height)
    return RemoteViews(context.packageName, R.layout.widget_finance_root).apply {
        setRemoteAdapter(R.id.widget_finance_list, adapter)
        removeAllViews(R.id.widget_finance_bar)
        addView(R.id.widget_finance_bar, bar)
    }
}

class WidgetFinanceService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        FinanceRows(applicationContext, intent.getIntExtra("width", 320), intent.getIntExtra("height", 640))
}

private class FinanceRows(private val context: Context, private val width: Int, private val height: Int) :
    RemoteViewsService.RemoteViewsFactory {
    private var rows = emptyList<RemoteViews>()
    override fun onCreate() = Unit
    override fun onDestroy() { rows = emptyList() }
    override fun getCount() = rows.size
    override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 3
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = false

    override fun onDataSetChanged() {
        rows = runBlocking(Dispatchers.IO) {
            val entry = context.widgetEntryPoint()
            val repository = entry.finance()
            val accounts = repository.observeAccounts().first()
            if (accounts.isEmpty()) return@runBlocking listOf(row("Начнём со счёта"))
            val today = entry.time().today()
            val month = YearMonth.from(today)
            val model = MonthSpending.of(repository.observeOperations().first(), month, today)
            val limit = financeLimitPreferences(context).getLong(month.toString(), 0L).takeIf { it > 0 }
            val amounts = RemoteViews(context.packageName, R.layout.widget_finance_summary).apply {
                setTextViewText(R.id.widget_finance_total, compactAmount(model.total))
                setTextViewText(R.id.widget_finance_remaining,
                    limit?.let { compactAmount((it - model.total).coerceAtLeast(0)) } ?: "—")
                setTextViewText(R.id.widget_finance_estimate, model.forecast?.let { compactAmount(it) }.orEmpty())
                if (model.forecast == null) setViewVisibility(R.id.widget_finance_estimate_group, android.view.View.INVISIBLE)
            }
            val compact = height <= SizeTokens.WIDGET_BAR + SizeTokens.WIDGET_FINANCE_CHART_COMPACT + SizeTokens.WIDGET_EDGE
            buildList {
                if (!compact) add(amounts)
                add(RemoteViews(context.packageName, R.layout.widget_finance_chart).apply {
                    setImageViewBitmap(R.id.widget_finance_graph, chart(model, compact))
                    setContentDescription(R.id.widget_finance_graph, "Накопленные расходы за месяц")
                })
                if (compact) add(amounts)
                if (model.total == 0L) add(row(context.getString(R.string.analytics_empty)))
                else if (width >= SizeTokens.WIDGET_COMPACT_BAR_THRESHOLD) {
                    add(row("Категории", "Места", leftAmount = "₽", rightAmount = "₽"))
                    repeat(maxOf(model.categories.size, model.places.size)) { idx ->
                        val category = model.categories.getOrNull(idx)
                        val place = model.places.getOrNull(idx)
                        add(row(rankLabel(idx, category), rankLabel(idx, place),
                            leftAmount = category?.let { Money.amount(it.amount) }.orEmpty(),
                            rightAmount = place?.let { Money.amount(it.amount) }.orEmpty(), paired = true))
                    }
                } else {
                    addAll(ranking("Категории", model.categories))
                    addAll(ranking("Места", model.places))
                }
                add(row("Счета", leftAmount = Money.format(accounts.sumOf { it.balance })))
                accounts.forEach { add(row("${it.emojiShown} ${it.name}", leftAmount = Money.format(it.balance))) }
            }
        }
    }

    private fun rankLabel(index: Int, rank: MonthSpending.Rank?) =
        rank?.let { "${index + 1} ${listOfNotNull(it.icon, it.name).joinToString(" ")}" }.orEmpty()

    private fun ranking(title: String, ranks: List<MonthSpending.Rank>) =
        listOf(row(title, leftAmount = "₽")) +
            ranks.mapIndexed { idx, rank -> row(rankLabel(idx, rank), leftAmount = Money.amount(rank.amount)) }

    private fun row(left: String, right: String = "", leftAmount: String = "", rightAmount: String = "", paired: Boolean = right.isNotEmpty()) =
        RemoteViews(context.packageName, R.layout.widget_finance_row).apply {
            setTextViewText(R.id.widget_finance_left, left)
            setTextViewText(R.id.widget_finance_left_amount, leftAmount)
            setTextViewText(R.id.widget_finance_right, right)
            setTextViewText(R.id.widget_finance_right_amount, rightAmount)
            setViewVisibility(R.id.widget_finance_right_column, if (paired) android.view.View.VISIBLE else android.view.View.GONE)
        }

    private fun chart(model: MonthSpending, compact: Boolean): Bitmap {
        val width = (width.coerceIn(180, 440) - 12) * 2
        val height = (if (compact) SizeTokens.WIDGET_FINANCE_CHART_COMPACT else SizeTokens.FINANCE_CHART_HEIGHT) * 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.density = android.util.DisplayMetrics.DENSITY_XHIGH
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val top = 5f
        val bottom = height - 23f
        val left = 7f
        val right = width - 7f
        val last = model.cumulative.lastIndex
        fun x(day: Int) = left + (right - left) * day / (model.days - 1)
        fun y(value: Long) = bottom - (bottom - top) * (value.toDouble() / model.ceiling).toFloat()
        paint.color = context.getColor(R.color.widget_flat_divider)
        listOf(top, (top + bottom) / 2, bottom).forEach { canvas.drawLine(left, it, right, it, paint) }
        paint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        canvas.drawLine(x(last), top, x(last), bottom, paint)
        paint.pathEffect = null
        paint.color = context.getColor(R.color.widget_flat_text_primary)
        paint.strokeWidth = 3f
        model.cumulative.zipWithNext().forEachIndexed { index, (a, b) ->
            canvas.drawLine(x(index), y(a), x(index + 1), y(b), paint)
        }
        canvas.drawCircle(x(last), y(model.total), 4f, paint)
        model.forecast?.let {
            paint.color = context.getColor(R.color.widget_flat_text_secondary)
            paint.pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
            canvas.drawLine(x(last), y(model.total), right, y(it), paint)
            paint.pathEffect = null
        }
        paint.color = context.getColor(R.color.widget_flat_text_secondary)
        paint.textSize = if (compact) 15f else 19f
        canvas.drawText(axisAmount(model.ceiling), left + 4, top + paint.textSize, paint)
        canvas.drawText(axisAmount(model.ceiling / 2), left + 4, (top + bottom) / 2 + paint.textSize, paint)
        canvas.drawText("01", left, height - 4f, paint)
        val end = model.days.toString()
        canvas.drawText(end, right - paint.measureText(end), height - 4f, paint)
        if (last in 2 until model.days - 2) {
            val day = (last + 1).toString().padStart(2, '0')
            canvas.drawText(day, x(last) - paint.measureText(day) / 2, height - 4f, paint)
        }
        return bitmap
    }
}
