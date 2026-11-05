package com.prostovasei.simpletasks.widget

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import android.content.Context
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.ui.common.DayNames

/*
 * Лента виджета построчно (итерация 42). Каждая строка — отдельный элемент списка на обычных элементах Android ([WidgetRows]);
 * все строки уходят рабочему столу вместе с рамкой одним лёгким пакетом ([widgetRootViews]). Раньше (0.9.2–0.41) строки собирал Glance, пакет
 * на 100+ задачах весил ~760 КБ, и Nova Launcher его не применял до возврата из приложения.
 *
 * Нажатия: в строках списка Android понимает только «добавки» к общему шаблону списка — что сделать и с какой задачей;
 * выполняет их [WidgetClickActivity].
 */

/** Строка ленты — отдельный элемент списка. */
sealed interface WidgetLine {
    val key: String

    data class Overdue(val entry: WidgetEntry.Overdue, val isFirst: Boolean) : WidgetLine {
        override val key get() = "overdue"
    }

    /** [count] — все задачи дня (с выполненными и будущими повторами): число справа у подписи; у «Сегодня» вместо него [progress]. */
    data class Title(val day: WidgetDay, val isFirst: Boolean, val progress: DayProgress?, val count: Int) : WidgetLine {
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
                listOf(WidgetLine.Title(block.day, isFirst, progress, block.items.size)) + rows.mapIndexed { i, row ->
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

/**
 * Весь виджет на Android 12+: фон и лента — обычные элементы прямо в корне ([WidgetRows]), поверх — нижняя панель от Glance
 * ([overlay], [WidgetContent] с scrollable = true). Ленту нельзя вкладывать в картинку Glance: рабочий стол, применяющий виджет
 * в фоне (Nova, Android 16–17), оставляет такой список пустым. Нажатия строк — через общий шаблон списка.
 * Не больше [MAX_LINES] строк: дальше 14 дней всё равно не показываем, предел — защита размера пакета.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun widgetRootViews(
    context: Context,
    appWidgetId: Int,
    model: WidgetModel,
    overlay: RemoteViews,
    size: DpSize = DpSize.Unspecified,
    liveClock: Boolean = true,
): RemoteViews {
    val week = model.week
    if (week != null) {
        // Вид «Неделя» (итерация 45; 0.46.0 — не лента): четыре строки делят настоящую высоту виджета поровну над нижней панелью.
        return RemoteViews(context.packageName, R.layout.widget_root_week).apply {
            val click = WidgetWeekRows.Click { viewId, code, id -> setOnClickPendingIntent(viewId, WidgetClicks.pending(context, appWidgetId, code, id)) }
            WidgetWeekRows.views(context, week, size, click).forEach { addView(R.id.week_rows, it) }
            addView(R.id.widget_overlay, overlay)
        }
    }
    return RemoteViews(context.packageName, R.layout.widget_root).apply {
        val items = RemoteViews.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(WidgetRows.VIEW_TYPES)
        val all = if (model.isEmpty) emptyList() else model.lines(dayNames(context))
        val lines = if (all.size > MAX_LINES) all.take(MAX_LINES) + WidgetLine.Tail else all
        lines.forEach { items.addItem(it.key.hashCode().toLong(), WidgetRows.views(context, it, model, liveClock)) }
        setRemoteAdapter(R.id.widget_list, items.build())
        setPendingIntentTemplate(
            R.id.widget_list,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, WidgetClickActivity::class.java).putExtra(WidgetActions.EXTRA_WIDGET_ID, appWidgetId),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        addView(R.id.widget_overlay, overlay)
    }
}

/** Предел строк ленты (~150 строк — ~150 КБ пакета). */
const val MAX_LINES = 150

/** Нажатия в строках ленты: код действия и задача в коротком адресе добавки ([WidgetRows.fillIn]). */
object WidgetClicks {
    const val COMPLETE = "c"
    const val UNCOMPLETE = "u"
    const val TOGGLE_OVERDUE = "t"
    const val OPEN = "o"
    const val MOVE_OVERDUE = "m"

    /** Вид «Неделя» в виджете (итерация 45): строка даты открывает день ([DAY]), строка мини-календаря выбирает неделю ([PICK_WEEK]) и с ней месяц (0.46.0, стрелок нет). */
    const val DAY = "d"
    const val PICK_WEEK = "k"

    /** Пустое нажатие: кружок будущего повтора и т. п. — у перекрашенной строки затирает прежнее. */
    const val NONE = "n"
    private const val SCHEME = "wt"

    fun uri(code: String, taskId: String?): Uri = Uri.fromParts(SCHEME, "$code/${taskId.orEmpty()}", null)

    /**
     * Своё намерение у элемента вне ленты (вид «Неделя», 0.46.0). Номер виджета — и в добавке, и во фрагменте адреса: намерения
     * с одинаковым адресом Android считает одним, и выбор недели ушёл бы в чужой виджет.
     */
    fun pending(context: Context, appWidgetId: Int, code: String, taskId: String?): PendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        Intent(context, WidgetClickActivity::class.java)
            .setData(Uri.fromParts(SCHEME, "$code/${taskId.orEmpty()}", appWidgetId.toString()))
            .putExtra(WidgetActions.EXTRA_WIDGET_ID, appWidgetId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Код и задача из адреса; null — адрес не наш. */
    fun parse(uri: Uri?): Pair<String, String?>? {
        if (uri?.scheme != SCHEME) return null
        val part = uri.schemeSpecificPart ?: return null
        return part.substringBefore('/') to part.substringAfter('/', "").ifEmpty { null }
    }
}

/**
 * Невидимое окно-переходник для нажатий в строках списка (итерация 42): открыть задачу поверх рабочего стола или перенести
 * просроченные — окно; галочка и «свернуть» — сразу приёмнику виджета. Закрывается до показа.
 */
class WidgetClickActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (code, taskId) = WidgetClicks.parse(intent.data) ?: (null to null)
        val widgetId = intent.getIntExtra(WidgetActions.EXTRA_WIDGET_ID, -1)
        WidgetLog.add(this, "нажатие в строке: $code")
        when (code) {
            WidgetClicks.OPEN -> taskId?.let { startActivity(openTaskIntent(this, it).apply { sourceBounds = intent.sourceBounds }) }
            WidgetClicks.MOVE_OVERDUE -> startActivity(moveOverdueIntent(this))
            WidgetClicks.COMPLETE -> taskId?.let { sendBroadcast(WidgetActions.complete(this, it)) }
            WidgetClicks.UNCOMPLETE -> taskId?.let { sendBroadcast(WidgetActions.uncomplete(this, it)) }
            WidgetClicks.TOGGLE_OVERDUE -> sendBroadcast(WidgetActions.toggleOverdue(this))
            WidgetClicks.DAY -> taskId?.let { startActivity(openDayIntent(this, it)) }
            WidgetClicks.PICK_WEEK -> taskId?.let { sendBroadcast(WidgetActions.pickWeek(this, widgetId, it)) }
        }
        finish()
    }
}
