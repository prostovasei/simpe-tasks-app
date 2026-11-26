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
import com.prostovasei.simpletasks.ui.feature.finance.monthLimit
import com.prostovasei.simpletasks.ui.feature.finance.paceText
import com.prostovasei.simpletasks.domain.model.finance.BudgetPace
import com.prostovasei.simpletasks.domain.model.finance.BudgetRules
import com.prostovasei.simpletasks.domain.model.finance.MonthBudget
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
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
            val budgets = repository.observeBudgets().first()
            val categories = repository.observeCategories().first()
            val operations = repository.observeOperations().first()
            val budget = MonthBudget.of(BudgetRules.effective(budgets, month, month), categories, operations, month, today)
            val limit = monthLimit(context, budget)
            val amounts = limitViews(model, limit, month, today)
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
    /** [large] (#336, хаб): плашки крупнее — 14 sp и поля 10 × 7. */
    internal fun accountRow(accounts: List<Account>, columns: Int, large: Boolean = false) =
        RemoteViews(context.packageName, R.layout.widget_finance_accounts).apply {
            removeAllViews(R.id.widget_finance_accounts)
            repeat(columns) { index ->
                val account = accounts.getOrNull(index)
                addView(R.id.widget_finance_accounts, RemoteViews(context.packageName, R.layout.widget_finance_account).apply {
                    if (account == null) setViewVisibility(R.id.widget_finance_account, android.view.View.INVISIBLE)
                    else {
                        setTextViewText(R.id.widget_finance_account, "${account.emojiShown}  ${financeThousands(account.balance)}")
                        if (large) {
                            setTextViewTextSize(R.id.widget_finance_account, android.util.TypedValue.COMPLEX_UNIT_SP, ACCOUNT_LARGE_SP)
                            val density = context.resources.displayMetrics.density
                            setViewPadding(R.id.widget_finance_account, (10 * density).toInt(), (7 * density).toInt(), (10 * density).toInt(), (7 * density).toInt())
                        }
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

    /** Полоса лимита (#327, вариант Г): картинка полосы и строка «осталось · прогноз»; [daily] — полоса хаба (#346). */
    internal fun limitViews(model: MonthSpending, limit: Long?, month: YearMonth, today: java.time.LocalDate, daily: Boolean = false): RemoteViews {
        if (daily) return RemoteViews(context.packageName, R.layout.widget_hub_limit).apply {
            val pace = limit?.let { BudgetRules.pace(it, model.total, month, today) }
            setImageViewBitmap(R.id.widget_finance_limit_bar, hubLimitBar(model.total, limit, pace, model.forecast))
            setContentDescription(R.id.widget_finance_limit_bar, listOfNotNull(
                limit?.let { "Потрачено ${Money.format(model.total)} из ${Money.format(it)}" } ?: "Потрачено ${Money.format(model.total)}, бюджет не задан",
                model.forecast?.let { "прогноз ${Money.format(it)}" },
            ).joinToString(", "))
        }
        return RemoteViews(context.packageName, R.layout.widget_finance_limit).apply {
            val pace = limit?.let { BudgetRules.pace(it, model.total, month, today) }
            setImageViewBitmap(R.id.widget_finance_limit_bar, limitBar(model.total, limit, pace, BudgetRules.todayShare(month, today)))
            setContentDescription(R.id.widget_finance_limit_bar,
                limit?.let { "Потрачено ${Money.format(model.total)} из ${Money.format(it)}" } ?: "Потрачено ${Money.format(model.total)}, бюджет не задан")
            setTextViewText(R.id.widget_finance_limit_status,
                if (limit == null || pace == null) "Бюджет не задан" else paceText(pace, limit, model.total, ::compactAmount))
            val status = when (pace) {
                BudgetPace.AHEAD -> ColorTokens.warning.light
                BudgetPace.OVER -> ColorTokens.danger.light
                else -> ColorTokens.textSecondary.light
            }
            setTextColor(R.id.widget_finance_limit_status, status.toInt())
            setTextViewText(R.id.widget_finance_estimate, model.forecast?.let { "прогноз ${compactAmount(it)}" }.orEmpty())
            if (model.forecast == null) setViewVisibility(R.id.widget_finance_estimate_group, android.view.View.INVISIBLE)
        }
    }

    /**
     * Полоса хаба (#346, как в приложении): трек на всю ширину, штриховка от потраченного до прогноза, заливка потраченного;
     * слева «42k», у конца штриховки — прогноз (больше лимита — красный у лимита), справа внутри — лимит. Рисуется в 2×.
     */
    private fun hubLimitBar(spent: Long, limit: Long?, pace: BudgetPace?, forecast: Long?): Bitmap {
        val scale = 2f
        val width = ((width.coerceIn(180, 440) - 12) * scale).toInt()
        val height = (SizeTokens.LIMIT_BAR * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.density = android.util.DisplayMetrics.DENSITY_XHIGH
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = height / 2f
        val clip = android.graphics.Path().apply { addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, android.graphics.Path.Direction.CW) }
        canvas.clipPath(clip)
        paint.color = ColorTokens.accentSoft.light.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val fill = if (limit == null) 0f else (spent.toFloat() / limit).coerceIn(0f, 1f)
        val share = if (limit == null || forecast == null) null else (forecast.toFloat() / limit).coerceAtLeast(fill)
        share?.let {
            val end = it.coerceAtMost(1f) * width
            paint.color = ColorTokens.textSecondary.light.toInt()
            paint.alpha = (255 * 0.4f).toInt()
            paint.strokeWidth = SizeTokens.LIMIT_HATCH * scale / 3
            canvas.save()
            canvas.clipRect(fill * width, 0f, end, height.toFloat())
            var x = fill * width - height
            while (x < end) { canvas.drawLine(x, height.toFloat(), x + height, 0f, paint); x += SizeTokens.LIMIT_HATCH * scale }
            canvas.restore()
            paint.alpha = 255
        }
        if (fill > 0f) {
            paint.color = when (pace) {
                BudgetPace.AHEAD -> ColorTokens.warning.light
                BudgetPace.OVER -> ColorTokens.danger.light
                else -> ColorTokens.textPrimary.light
            }.toInt()
            canvas.drawRoundRect(0f, 0f, maxOf(width * fill, height.toFloat()), height.toFloat(), radius, radius, paint)
        }
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = 13 * scale
        val baseline = height / 2f - (paint.descent() + paint.ascent()) / 2
        val pad = SizeTokens.BUDGET_BAR * 2 * scale
        paint.color = (if (fill >= 0.3f) ColorTokens.onAccent.light else ColorTokens.textPrimary.light).toInt()
        canvas.drawText(financeThousands(spent), pad, baseline, paint)
        val limitText = limit?.let(::financeThousands) ?: "—"
        paint.color = ColorTokens.textPrimary.light.toInt()
        val limitX = width - pad - paint.measureText(limitText)
        canvas.drawText(limitText, limitX, baseline, paint)
        if (share != null && forecast != null) {
            val text = financeThousands(forecast)
            paint.textSize = 12 * scale
            val w = paint.measureText(text)
            val over = share > 1f
            val right = if (over) limitX - pad / 2 else minOf(share * width - pad / 2, limitX - pad / 2)
            paint.color = (if (over) ColorTokens.danger.light else ColorTokens.textSecondary.light).toInt()
            canvas.drawText(text, right - w, baseline, paint)
        }
        return bitmap
    }

    /** Толстая полоса: внутри потрачено, отметка «сегодня», справа лимит. Рисуется в 2× (как график). */
    private fun limitBar(spent: Long, limit: Long?, pace: BudgetPace?, todayShare: Float?): Bitmap {
        val scale = 2f
        val width = ((width.coerceIn(180, 440) - 12) * scale).toInt()
        val bar = SizeTokens.LIMIT_BAR * scale
        val over = SizeTokens.BUDGET_MARK_OVER * scale
        val height = (bar + 2 * over).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.density = android.util.DisplayMetrics.DENSITY_XHIGH
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = 15 * scale
        val limitText = limit?.let { compactAmount(it).removeSuffix("\u00a0₽").removeSuffix(" ₽") } ?: "—"
        val right = width - paint.measureText(limitText) - SizeTokens.BUDGET_BAR * scale
        val top = over
        val radius = bar / 2
        paint.color = ColorTokens.accentSoft.light.toInt()
        canvas.drawRoundRect(0f, top, right, top + bar, radius, radius, paint)
        val fill = if (limit == null) 0f else (spent.toFloat() / limit).coerceIn(0f, 1f)
        if (fill > 0f) {
            paint.color = when (pace) {
                BudgetPace.AHEAD -> ColorTokens.warning.light
                BudgetPace.OVER -> ColorTokens.danger.light
                else -> ColorTokens.textPrimary.light
            }.toInt()
            canvas.drawRoundRect(0f, top, maxOf(right * fill, bar), top + bar, radius, radius, paint)
        }
        if (limit != null && todayShare != null) {
            val x = right * todayShare
            paint.color = ColorTokens.surface.light.toInt()
            canvas.drawRect(x - SizeTokens.BUDGET_MARK * scale, 0f, x + SizeTokens.BUDGET_MARK * scale, height.toFloat(), paint)
            paint.color = ColorTokens.textSecondary.light.toInt()
            canvas.drawRect(x - SizeTokens.BUDGET_MARK * scale / 2, 0f, x + SizeTokens.BUDGET_MARK * scale / 2, height.toFloat(), paint)
        }
        paint.textSize = 13 * scale
        val baseline = top + bar / 2 - (paint.descent() + paint.ascent()) / 2
        paint.color = (if (fill >= 0.3f) ColorTokens.onAccent.light else ColorTokens.textPrimary.light).toInt()
        canvas.drawText(compactAmount(spent), SizeTokens.BUDGET_BAR * 2 * scale, baseline, paint)
        paint.textSize = 15 * scale
        paint.color = ColorTokens.textPrimary.light.toInt()
        canvas.drawText(limitText, width - paint.measureText(limitText), baseline, paint)
        return bitmap
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

private const val ACCOUNT_LARGE_SP = 14f
