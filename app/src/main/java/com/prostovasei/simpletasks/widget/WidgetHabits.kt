package com.prostovasei.simpletasks.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitMath
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Привычки на сегодня (#340): активные и нужные в [today] (ТЗ §3), в порядке списка. */
internal fun List<Habit>.dueToday(today: LocalDate): List<Habit> = filter { !it.archived && HabitMath.dueOn(it, today) }

/** Виджет «Привычки» (#340, ТЗ §7): «N из M сегодня» и строки с отметкой прямо на рабочем столе. Android 12+. */
@RequiresApi(Build.VERSION_CODES.S)
internal suspend fun habitsWidgetViews(context: Context, id: Int, bar: RemoteViews): RemoteViews {
    val entry = context.widgetEntryPoint()
    return habitsViews(context, id, entry.habits().observeHabits().first(), entry.time().today(), bar)
}

/** Сборка без чтения данных — её проверяют тесты. */
@RequiresApi(Build.VERSION_CODES.S)
internal fun habitsViews(context: Context, id: Int, habits: List<Habit>, today: LocalDate, bar: RemoteViews?): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_habits_root).apply {
        val due = habits.dueToday(today)
        val done = due.count { it.isDone(today) }
        setTextViewText(R.id.widget_habits_summary, if (due.isEmpty()) context.getString(R.string.module_habits) else context.getString(R.string.habits_today, done, due.size))
        setOnClickPendingIntent(R.id.widget_habits_summary, openHabits(context, id))
        val empty = when {
            habits.none { !it.archived } -> context.getString(R.string.habits_empty_title)
            due.isEmpty() -> context.getString(R.string.habits_widget_none_today)
            else -> null
        }
        setViewVisibility(R.id.widget_habits_list, if (empty == null) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.widget_habits_empty, if (empty == null) View.GONE else View.VISIBLE)
        setTextViewText(R.id.widget_habits_empty, empty.orEmpty())
        setOnClickPendingIntent(R.id.widget_habits_empty, openHabits(context, id))
        val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
        due.forEach { habit -> items.addItem(habit.id.hashCode().toLong(), habitRow(context, habit, today)) }
        setRemoteAdapter(R.id.widget_habits_list, items.build())
        setPendingIntentTemplate(R.id.widget_habits_list, PendingIntent.getBroadcast(
            context, id,
            Intent(context, WidgetActionReceiver::class.java).setAction(WidgetActions.TOGGLE_HABIT).setData("simpletasks://widget/$id/habits".toUri()),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ))
        removeAllViews(R.id.widget_habits_bar)
        bar?.let { addView(R.id.widget_habits_bar, it) }
    }

private fun habitRow(context: Context, habit: Habit, today: LocalDate): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_habit_row).apply {
        val done = habit.isDone(today)
        setTextViewText(R.id.widget_habit_emoji, habit.emojiShown)
        setTextViewText(R.id.widget_habit_name, habit.name)
        val streak = HabitMath.streakLabel(habit, today)
        setTextViewText(R.id.widget_habit_streak, streak)
        setViewVisibility(R.id.widget_habit_streak, if (streak.isEmpty()) View.GONE else View.VISIBLE)
        setImageViewResource(R.id.widget_habit_check, if (done) R.drawable.widget_habit_check_on else R.drawable.widget_habit_check_off)
        setContentDescription(R.id.widget_habit_row, context.getString(if (done) R.string.habit_uncheck else R.string.habit_check, habit.name))
        setOnClickFillInIntent(R.id.widget_habit_row, Intent().putExtra(WidgetActions.EXTRA_HABIT_ID, habit.id))
    }

/** Плашки привычек виджета «Главная» (#340): до [HUB_HABITS] нужных сегодня; нажатие отмечает. */
internal fun hubHabitViews(views: RemoteViews, context: Context, id: Int, habits: List<Habit>, today: LocalDate) {
    val due = habits.dueToday(today).take(HUB_HABITS)
    val visibility = if (due.isEmpty()) View.GONE else View.VISIBLE
    views.setViewVisibility(R.id.widget_hub_title_habits, View.GONE) // #346: заголовков разделов нет
    views.setViewVisibility(R.id.widget_hub_habits, visibility)
    views.removeAllViews(R.id.widget_hub_habits)
    views.setOnClickPendingIntent(R.id.widget_hub_title_habits, PendingIntent.getBroadcast(context, id, WidgetActions.module(context, id, WidgetModule.HABITS),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    due.forEach { habit ->
        val done = habit.isDone(today)
        val chip = RemoteViews(context.packageName, R.layout.widget_hub_habit_chip).apply {
            setTextViewText(R.id.widget_hub_habit, "${habit.emojiShown} ${habit.name}${if (done) " ✓" else ""}")
            if (done) {
                setInt(R.id.widget_hub_habit, "setBackgroundResource", R.drawable.widget_habit_chip_on)
                setTextColor(R.id.widget_hub_habit, context.getColor(R.color.widget_habit_accent))
            }
            setContentDescription(R.id.widget_hub_habit, context.getString(if (done) R.string.habit_uncheck else R.string.habit_check, habit.name))
            setOnClickPendingIntent(R.id.widget_hub_habit, PendingIntent.getBroadcast(context, id, WidgetActions.toggleHabit(context, id, habit.id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        views.addView(R.id.widget_hub_habits, chip)
    }
}

private fun openHabits(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
    context, id,
    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_HABITS, true)
        .setData("simpletasks://widget/$id/open-habits".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

internal const val HUB_HABITS = 3
