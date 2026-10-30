package com.prostovasei.simpletasks.widget

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import android.content.Context
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.ui.common.DayNames

/*
 * Лента виджета построчно (итерация 42). Каждая строка — отдельный элемент списка на обычных элементах Android ([WidgetRows]);
 * все строки уходят рабочему столу вместе с рамкой одним лёгким пакетом. Раньше (0.9.2–0.41) строки собирал Glance, пакет
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

/**
 * Список в рамке виджета: строки уходят рабочему столу сразу, вместе с рамкой ([WidgetRows], Android 12+), нажатия строк —
 * через общий шаблон. Не больше [MAX_LINES] строк: дальше 14 дней всё равно не показываем, предел — защита размера пакета.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun widgetListViews(context: Context, appWidgetId: Int, model: WidgetModel, liveClock: Boolean = true): RemoteViews {
    val all = model.lines(dayNames(context))
    val lines = if (all.size > MAX_LINES) all.take(MAX_LINES) + WidgetLine.Tail else all
    val items = RemoteViews.RemoteCollectionItems.Builder()
        .setHasStableIds(true)
        .setViewTypeCount(WidgetRows.VIEW_TYPES)
    lines.forEach { items.addItem(it.key.hashCode().toLong(), WidgetRows.views(context, it, model, liveClock)) }
    return RemoteViews(context.packageName, R.layout.widget_list).apply {
        setRemoteAdapter(R.id.widget_list, items.build())
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
    private const val SCHEME = "wt"

    fun uri(code: String, taskId: String?): Uri = Uri.fromParts(SCHEME, "$code/${taskId.orEmpty()}", null)

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
        WidgetLog.add(this, "нажатие в строке: $code")
        when (code) {
            WidgetClicks.OPEN -> taskId?.let { startActivity(openTaskIntent(this, it).apply { sourceBounds = intent.sourceBounds }) }
            WidgetClicks.MOVE_OVERDUE -> startActivity(moveOverdueIntent(this))
            WidgetClicks.COMPLETE -> taskId?.let { sendBroadcast(WidgetActions.complete(this, it)) }
            WidgetClicks.UNCOMPLETE -> taskId?.let { sendBroadcast(WidgetActions.uncomplete(this, it)) }
            WidgetClicks.TOGGLE_OVERDUE -> sendBroadcast(WidgetActions.toggleOverdue(this))
        }
        finish()
    }
}
