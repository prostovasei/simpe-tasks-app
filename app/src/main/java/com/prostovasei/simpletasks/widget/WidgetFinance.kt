package com.prostovasei.simpletasks.widget

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import com.prostovasei.simpletasks.designsystem.tokens.FinanceShareSizes
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.financeThousands
import com.prostovasei.simpletasks.ui.feature.finance.sharesBitmap
import com.prostovasei.simpletasks.ui.feature.finance.sharesDescription
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
    val open = Intent(context, MainActivity::class.java)
        .setData("simpletasks://widget/$id/open-finance-purchase".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MainActivity.EXTRA_BANK_QUEUE, true)
    val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    return RemoteViews(context.packageName, R.layout.widget_finance_root).apply {
        setPendingIntentTemplate(R.id.widget_finance_list, PendingIntent.getActivity(context, id, open, mutable))
        setRemoteAdapter(R.id.widget_finance_list, adapter)
        removeAllViews(R.id.widget_finance_bar)
        addView(R.id.widget_finance_bar, bar)
    }
}

class WidgetFinanceService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        FinanceRows(applicationContext, intent.getIntExtra("width", 320), intent.getIntExtra("height", 640))
}

internal class FinanceRows(private val context: Context, private val width: Int, private val height: Int) :
    RemoteViewsService.RemoteViewsFactory {
    private var rows = emptyList<RemoteViews>()
    override fun onCreate() = Unit
    override fun onDestroy() { rows = emptyList() }
    override fun getCount() = rows.size
    override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 5
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = false

    override fun onDataSetChanged() {
        rows = runBlocking(Dispatchers.IO) {
            val entry = context.widgetEntryPoint()
            val repository = entry.finance()
            val accounts = repository.observeAccounts().first()
            val pending = entry.bankPurchases().observePending().first()
            val pendingRows = pendingRows(pending, entry.time().zone())
            if (accounts.isEmpty()) return@runBlocking listOf(row("Начнём со счёта")) + pendingRows
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
                else {
                    add(shares("Категории", model.categories, model.total))
                    add(shares("Места", model.places, model.total))
                }
                val balance = accounts.sumOf { it.balance }
                add(row("${financeThousands(balance)} ₽", Money.format(balance)))
                val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
                val available = width - 2 * SizeTokens.WIDGET_EDGE
                val columns = (available / (FinanceShareSizes.accountMin * fontScale)).toInt().coerceIn(1, 4)
                accounts.chunked(columns).forEach { add(accountRow(it, columns)) }
                addAll(pendingRows)
            }
        }
    }

    private fun pendingRows(purchases: List<PendingPurchase>, zone: ZoneId): List<RemoteViews> {
        if (purchases.isEmpty()) return emptyList()
        val heading = context.getString(R.string.widget_bank_pending_count, context.getString(R.string.bank_unsorted), purchases.size)
        val timeFormat = DateTimeFormatter.ofPattern("dd.MM · HH:mm").withZone(zone)
        return buildList {
            add(RemoteViews(context.packageName, R.layout.widget_bank_purchase).apply {
                setTextViewText(R.id.widget_bank_label, heading)
                setViewVisibility(R.id.widget_bank_time, android.view.View.GONE)
                setViewVisibility(R.id.widget_bank_amount, android.view.View.GONE)
                setOnClickFillInIntent(R.id.widget_bank_row, Intent())
            })
            purchases.take(3).forEach { purchase ->
                add(RemoteViews(context.packageName, R.layout.widget_bank_purchase).apply {
                    setTextViewText(R.id.widget_bank_label, purchase.accountName)
                    setTextViewText(R.id.widget_bank_time, timeFormat.format(purchase.postedAt))
                    setTextViewText(R.id.widget_bank_amount, "−${Money.format(purchase.amount)}")
                    setOnClickFillInIntent(R.id.widget_bank_row, Intent().putExtra(MainActivity.EXTRA_BANK_PURCHASE, purchase.id))
                })
            }
        }
    }

    private fun shares(title: String, entries: List<MonthSpending.Rank>, total: Long): RemoteViews {
        val density = 2f
        val widthPx = ((width.coerceIn(180, 440) - 2 * SizeTokens.WIDGET_EDGE) * density).toInt()
        val image = sharesBitmap(entries, total, widthPx, density, context.resources.configuration.fontScale,
            context.getColor(R.color.widget_flat_text_primary))
        image.density = android.util.DisplayMetrics.DENSITY_XHIGH
        return RemoteViews(context.packageName, R.layout.widget_finance_chart).apply {
            setImageViewBitmap(R.id.widget_finance_graph, image)
            setContentDescription(R.id.widget_finance_graph, sharesDescription(title, entries, total))
        }
    }

    /**
     * Строка счетов. #317: лента виджета переиспользует строки и при обновлении доприменяет команды к уже показанной —
     * без очистки каждый `addView` добавлял ещё столько же счетов (у PM — 12 кружков без сумм вместо 4 счетов).
     */
    internal fun accountRow(accounts: List<Account>, columns: Int) =
        RemoteViews(context.packageName, R.layout.widget_finance_accounts).apply {
            removeAllViews(R.id.widget_finance_accounts)
            repeat(columns) { index ->
                val account = accounts.getOrNull(index)
                addView(R.id.widget_finance_accounts, RemoteViews(context.packageName, R.layout.widget_finance_account).apply {
                    if (account == null) setViewVisibility(R.id.widget_finance_account, android.view.View.INVISIBLE)
                    else {
                        setTextViewText(R.id.widget_finance_account, "${account.emojiShown}  ${financeThousands(account.balance)}")
                        setContentDescription(R.id.widget_finance_account, "${account.name}, ${Money.format(account.balance)}")
                    }
                })
            }
        }

    private fun row(text: String, description: String = text) =
        RemoteViews(context.packageName, R.layout.widget_finance_row).apply {
            setTextViewText(R.id.widget_finance_left, text)
            setContentDescription(R.id.widget_finance_left, description)
            setViewVisibility(R.id.widget_finance_right_column, android.view.View.GONE)
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
