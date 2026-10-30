package com.prostovasei.simpletasks.widget

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.ui.common.DayNames
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.graphics.Typeface

/*
 * Классический список виджета (итерация 42). Рамка виджета (нижняя строка, фон) приходит рабочему столу картинкой, а строки ленты
 * он запрашивает сам у [WidgetListService] — по мере прокрутки. После изменений [WidgetRenderer] говорит «перечитай строки»
 * (notifyAppWidgetViewDataChanged). Раньше (0.9.2–0.41) вся лента уходила одной картинкой, и Nova Launcher на 27+ задачах её
 * не применял до перезапуска виджетов.
 *
 * Нажатия: в строках списка Android игнорирует обычные нажатия, поэтому строка — картинка Glance без нажатий, а поверх неё
 * прозрачный слой с зонами (кружок, плашка; у «Просрочено» — подпись и значок переноса). Зона несёт «добавку» к общему
 * шаблону списка: что сделать и с какой задачей; выполняет [WidgetClickActivity].
 */

/** Строка ленты — отдельный элемент списка. */
sealed interface WidgetLine {
    val key: String

    data class Overdue(val entry: WidgetEntry.Overdue, val isFirst: Boolean) : WidgetLine {
        override val key get() = "overdue"
    }

    data class Title(val day: WidgetDay, val isFirst: Boolean, val progress: DayProgress?) : WidgetLine {
        override val key get() = "title:${day.key}"
    }

    data class Task(val row: WidgetRow.Task, val line: Line, val overdue: Boolean, val day: WidgetDay) : WidgetLine {
        override val key get() = "task:${row.item.id}:${day.key}"
    }

    data class Now(val line: Line, val nowMinute: Int?, val day: WidgetDay) : WidgetLine {
        override val key get() = "now:${day.key}"
    }

    data object Tail : WidgetLine {
        override val key get() = "tail"
    }
}

private val WidgetDay.key: String
    get() = when (this) {
        WidgetDay.NoDate -> "none"
        is WidgetDay.On -> date.toString()
    }

/**
 * Лента построчно, как раньше рисовал блок дня: подпись дня, строки «время | линия | плашка» и «сейчас»; линия шкалы сплошная
 * от первой строки дня до последней. [withTail] — место под нижней панелью в конце (только в прокручиваемом списке).
 */
fun WidgetModel.lines(days: DayNames, withTail: Boolean = true): List<WidgetLine> {
    val entries = entries()
    val lines = entries.flatMapIndexed { index, entry ->
        val isFirst = index == 0
        when (entry) {
            is WidgetEntry.Overdue -> listOf(WidgetLine.Overdue(entry, isFirst))
            is WidgetEntry.Day -> {
                val block = entry.block
                // Прошедший день (итерация 27): красные подпись, время и кружки, раскладка — как у остальных дней.
                val overdue = (block.day as? WidgetDay.On)?.date?.let { it < today } == true
                val nowMinute = nowMinute.takeIf { block.day == WidgetDay.On(today) }
                val rows = WidgetTimeline.rows(block.items, overdue = false, nowMinute = nowMinute, days = days)
                val progress = DayProgress.of(block.items).takeIf { block.day == WidgetDay.On(today) }
                listOf(WidgetLine.Title(block.day, isFirst, progress)) + rows.mapIndexed { i, row ->
                    val line = Line(top = i > 0, bottom = i < rows.lastIndex)
                    when (row) {
                        is WidgetRow.Task -> WidgetLine.Task(row, line, overdue, block.day)
                        WidgetRow.Now -> WidgetLine.Now(line, nowMinute, block.day)
                    }
                }
            }
        }
    }
    return if (withTail && lines.isNotEmpty()) lines + WidgetLine.Tail else lines
}

/** Список в рамке виджета: адаптер — свой сервис на каждый виджет, нажатия строк — через общий шаблон. */
fun widgetListViews(context: Context, appWidgetId: Int): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_list).apply {
        @Suppress("DEPRECATION")
        setRemoteAdapter(
            R.id.widget_list,
            Intent(context, WidgetListService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                // Свой адрес у каждого виджета — иначе Android отдаст двум виджетам одни строки.
                .setData("simpletasks://widget/$appWidgetId/list".toUri()),
        )
        setPendingIntentTemplate(
            R.id.widget_list,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, WidgetClickActivity::class.java),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    }

class WidgetListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        WidgetListFactory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
}

/** Строки одного виджета: данные читаются заново по «перечитай»; картинка строки собирается при первом запросе и запоминается. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WidgetListFactory(private val context: Context, private val appWidgetId: Int) : RemoteViewsService.RemoteViewsFactory {
    private var model: WidgetModel? = null
    private var lines: List<WidgetLine> = emptyList()
    private val cache = HashMap<Int, RemoteViews>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        try {
            val filter = WidgetSelection.get(context, appWidgetId) ?: WidgetFilter.All
            val fresh = runBlocking { context.widgetEntryPoint().observeModel(filter).first() }
            model = fresh
            lines = fresh.lines(dayNames(context))
            cache.clear()
            WidgetLog.add(context, "строки $appWidgetId: ${lines.size} (Android ${Build.VERSION.RELEASE})")
        } catch (e: Exception) {
            WidgetLog.error(context, "строки виджета $appWidgetId", e)
        }
    }

    override fun onDestroy() {
        cache.clear()
    }

    override fun getCount(): Int = lines.size

    override fun getViewAt(position: Int): RemoteViews? {
        val line = lines.getOrNull(position) ?: return null
        val current = model ?: return null
        return cache.getOrPut(position) {
            try {
                itemViews(line, current)
            } catch (e: Exception) {
                WidgetLog.error(context, "строка $position виджета $appWidgetId", e)
                RemoteViews(context.packageName, R.layout.widget_list_item)
            }
        }
    }

    private fun itemViews(line: WidgetLine, model: WidgetModel): RemoteViews = widgetLineViews(context, line, model, widgetSize(context, appWidgetId))

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = lines.getOrNull(position)?.key?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

/** Строка списка: картинка строки (Glance, без нажатий) и поверх — зоны нажатий. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
fun widgetLineViews(context: Context, line: WidgetLine, model: WidgetModel, size: DpSize, liveClock: Boolean = true): RemoteViews {
    val content = runBlocking {
        GlanceRemoteViews().compose(context, size, appWidgetOptions = Bundle()) {
            CompositionLocalProvider(LocalWidgetClicks provides false) { WidgetLineContent(line, model, liveClock) }
        }.remoteViews
    }
    return RemoteViews(context.packageName, R.layout.widget_list_item).apply {
        removeAllViews(R.id.item_content)
        addView(R.id.item_content, content)
        WidgetHits.apply(this, context, line)
    }
}

/** Зоны нажатий поверх строки: слева направо отступ, первая зона, промежуток, вторая зона, остаток. */
object WidgetHits {
    const val OPEN = "com.prostovasei.simpletasks.widget.OPEN"
    const val MOVE_OVERDUE = "com.prostovasei.simpletasks.widget.MOVE_OVERDUE"

    fun apply(views: RemoteViews, context: Context, line: WidgetLine) {
        when (line) {
            is WidgetLine.Task -> {
                val item = line.row.item
                // Кружок — колонка времени и шкалы; у будущего повтора нажатия нет (как раньше).
                width(views, R.id.hit_one, SizeTokens.WIDGET_TIME + SizeTokens.WIDGET_RAIL)
                if (!item.isGhost) {
                    val action = if (item.showsDone) WidgetActions.UNCOMPLETE else WidgetActions.COMPLETE
                    views.setOnClickFillInIntent(R.id.hit_one, fillIn(action, item.id))
                }
                views.setOnClickFillInIntent(R.id.hit_rest, fillIn(OPEN, item.taskId))
            }
            is WidgetLine.Overdue -> {
                val textWidth = overdueTextWidth(context, line.entry)
                width(views, R.id.hit_lead, SizeTokens.WIDGET_EDGE)
                width(views, R.id.hit_one, textWidth + SpacingTokens.XXS + SizeTokens.WIDGET_FOLD_ICON)
                views.setOnClickFillInIntent(R.id.hit_one, fillIn(WidgetActions.TOGGLE_OVERDUE, null))
                if (line.entry.count > 0) {
                    width(views, R.id.hit_gap, SpacingTokens.SM)
                    width(views, R.id.hit_two, SizeTokens.WIDGET_FOLD_ICON)
                    views.setOnClickFillInIntent(R.id.hit_two, fillIn(MOVE_OVERDUE, null))
                }
            }
            else -> Unit
        }
    }

    /** Ширина зоны в dp — только Android 12+; на 10–11 вся строка — плашка (решение PM: старые версии не важны). */
    private fun width(views: RemoteViews, id: Int, dp: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) views.setViewLayoutWidth(id, dp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }

    private fun fillIn(action: String, taskId: String?): Intent = Intent()
        .setAction(action)
        .setData("simpletasks://widget/hit/$action/${taskId.orEmpty()}".toUri())
        .apply { taskId?.let { putExtra(WidgetActions.EXTRA_TASK_ID, it) } }

    /** Ширина подписи «Просрочено · N» в dp: тем же шрифтом, что рисует Glance (системный, жирный, 14 sp). */
    private fun overdueTextWidth(context: Context, entry: WidgetEntry.Overdue): Int {
        val text = if (entry.count > 0) context.getString(R.string.overdue_count, entry.count) else context.getString(R.string.overdue_title)
        val metrics = context.resources.displayMetrics
        val paint = TextPaint().apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, OVERDUE_TEXT_SP, metrics)
        }
        return (paint.measureText(text) / metrics.density).toInt() + 1
    }

    private const val OVERDUE_TEXT_SP = 14f
}

/**
 * Невидимое окно-переходник для нажатий в строках списка (итерация 42): открыть задачу поверх рабочего стола или перенести
 * просроченные — окно; галочка и «свернуть» — сразу приёмнику виджета. Закрывается до показа.
 */
class WidgetClickActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getStringExtra(WidgetActions.EXTRA_TASK_ID)
        WidgetLog.add(this, "нажатие в строке: ${intent.action?.substringAfterLast('.')}")
        when (intent.action) {
            WidgetHits.OPEN -> taskId?.let { startActivity(openTaskIntent(this, it).apply { sourceBounds = intent.sourceBounds }) }
            WidgetHits.MOVE_OVERDUE -> startActivity(moveOverdueIntent(this))
            WidgetActions.COMPLETE, WidgetActions.UNCOMPLETE, WidgetActions.TOGGLE_OVERDUE ->
                sendBroadcast(
                    Intent(this, WidgetActionReceiver::class.java)
                        .setAction(intent.action)
                        .setData(intent.data)
                        .apply { taskId?.let { putExtra(WidgetActions.EXTRA_TASK_ID, it) } },
                )
        }
        finish()
    }
}

/** Размер виджета из системы (портрет: ширина — минимальная, высота — максимальная). */
internal fun widgetSize(context: Context, appWidgetId: Int): DpSize {
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else DpSize.Unspecified
}
