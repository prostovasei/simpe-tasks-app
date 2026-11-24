package com.prostovasei.simpletasks.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.finance.BudgetLine
import com.prostovasei.simpletasks.domain.model.finance.BudgetRules
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.MonthBudget
import com.prostovasei.simpletasks.domain.repository.FinanceRepository
import com.prostovasei.simpletasks.ui.feature.finance.monthNominative
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Уведомления бюджета (#327, решения PM 24.11.2026):
 * - «Продукты: 84% бюджета» — расходы категории текущего месяца впервые дошли до 80% бюджета; один раз на категорию,
 *   месяц и сумму бюджета (после изменения бюджета может прийти снова). Состояние при первом запуске запоминается без уведомлений.
 * - «Бюджет на декабрь» — за 7 дней до 1-го числа в 19:00, если у текущего месяца есть бюджеты, а следующий не заведён:
 *   «Оставить» копирует бюджеты, «Изменить» открывает окно подготовки следующего месяца.
 */
@Singleton
class BudgetAlerts @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val finance: FinanceRepository,
    private val time: TimeProvider,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            combine(finance.observeBudgets(), finance.observeCategories(), finance.observeOperations()) { budgets, categories, operations ->
                val today = time.today()
                val month = YearMonth.from(today)
                MonthBudget.of(BudgetRules.effective(budgets, month, month), categories, operations, month, today)
            }.debounce(DEBOUNCE_MS).collect { check(it) }
        }
        schedule()
    }

    /** Сверяет категории текущего месяца с порогом 80% и показывает уведомления о новых. */
    internal fun check(budget: MonthBudget) {
        val crossed = budget.expenses.filter { line ->
            val plan = line.budget ?: return@filter false
            BudgetRules.percent(plan, line.fact) >= BudgetRules.ALERT_PERCENT
        }
        val keys = crossed.associateBy { "${budget.month}:${it.categoryId}:${it.budget}" }
        val firstRun = !prefs.contains(KEY_SEEN)
        val seen = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty()
        val fresh = keys.filterKeys { it !in seen }
        val month = budget.month.toString()
        prefs.edit().putStringSet(KEY_SEEN, seen.filter { it.startsWith(month) }.toSet() + keys.keys).apply()
        if (!firstRun) fresh.values.forEach { notifyAlert(it, budget) }
    }

    private fun notifyAlert(line: BudgetLine, budget: MonthBudget) {
        val plan = line.budget ?: return
        if (!canPost()) return
        ensureChannel()
        val daysLeft = budget.month.lengthOfMonth() - budget.today.dayOfMonth + 1
        val text = "Потрачено ${Money.amount(line.fact)} из ${Money.format(plan)} · " +
            if (line.fact >= plan) "перерасход ${Money.format(line.fact - plan)}" else "осталось ${Money.format(plan - line.fact)} на $daysLeft ${days(daysLeft)}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_finance)
            .setContentTitle("${line.name}: ${BudgetRules.percent(plan, line.fact)}% бюджета")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openBudget(budget.month, prepare = false, request = REQUEST_OPEN_ALERT))
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        NotificationManagerCompat.from(context).notify(ALERT_TAG, (line.categoryId ?: "").hashCode(), notification)
    }

    /** Ставит будильник «Бюджет на следующий месяц»: за [DAYS_BEFORE] дней до 1-го числа в [HOUR]:00. */
    fun schedule() {
        val now = LocalDateTime.ofInstant(time.now(), time.zone())
        val at = nextMonthReminder(now).atZone(time.zone()).toInstant().toEpochMilli()
        val intent = monthIntent()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    /** Будильник сработал: показать «Бюджет на …», если есть что подтверждать, и поставить следующий. */
    suspend fun fireMonth() {
        val today = time.today()
        val current = YearMonth.from(today)
        val next = current.plusMonths(1)
        val budgets = finance.observeBudgets().first()
        val categories = finance.observeCategories().first()
        val expenses = BudgetRules.effective(budgets, current, current)
            .filterKeys { id -> categories.any { it.id == id && it.kind == com.prostovasei.simpletasks.domain.model.finance.OperationKind.EXPENSE } }
        if (expenses.isNotEmpty() && !BudgetRules.isSet(budgets, next)) showMonth(current, next, expenses.values.sum())
        schedule()
    }

    /** «Оставить» в уведомлении: следующий месяц заводится копией текущего. */
    suspend fun keep(month: YearMonth) {
        finance.setBudgets(month, emptyMap())
        NotificationManagerCompat.from(context).cancel(MONTH_NOTIFICATION_ID)
    }

    private fun showMonth(current: YearMonth, next: YearMonth, plan: Long) {
        if (!canPost()) return
        ensureChannel()
        val text = "Оставить как в ${monthPrepositional(current)} — ${Money.format(plan)} расходов — или поправить по средним тратам?"
        val keep = PendingIntent.getBroadcast(
            context, REQUEST_KEEP,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_BUDGET_KEEP).putExtra(EXTRA_MONTH, next.toString()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val change = openBudget(next, prepare = true, request = REQUEST_CHANGE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_finance)
            .setContentTitle("Бюджет на ${monthNominative(next)}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(change)
            .setAutoCancel(true)
            .addAction(0, "Оставить", keep)
            .addAction(0, "Изменить", change)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        NotificationManagerCompat.from(context).notify(MONTH_NOTIFICATION_ID, notification)
    }

    private fun openBudget(month: YearMonth, prepare: Boolean, request: Int): PendingIntent = PendingIntent.getActivity(
        context, request,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_BUDGET_MONTH, month.toString())
            .putExtra(MainActivity.EXTRA_BUDGET_PREPARE, prepare)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun monthIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_MONTH,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_BUDGET_MONTH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Бюджет", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "budget"
        const val EXTRA_MONTH = "budget_month"
        const val DAYS_BEFORE = 7L
        const val HOUR = 19
        private const val PREFS = "budget_alerts"
        private const val KEY_SEEN = "seen"
        private const val ALERT_TAG = "budget_alert"
        private const val MONTH_NOTIFICATION_ID = Int.MAX_VALUE - 9
        private const val REQUEST_MONTH = 327
        private const val REQUEST_KEEP = 328
        private const val REQUEST_CHANGE = 329
        private const val REQUEST_OPEN_ALERT = 330
        private const val DEBOUNCE_MS = 1_000L

        /** Ближайшее время уведомления о следующем месяце: 1-е число следующего месяца минус 7 дней, 19:00; прошло — через месяц. */
        fun nextMonthReminder(now: LocalDateTime): LocalDateTime {
            fun at(month: YearMonth): LocalDateTime = month.plusMonths(1).atDay(1).minusDays(DAYS_BEFORE).atTime(LocalTime.of(HOUR, 0))
            val current = YearMonth.from(now)
            val candidate = at(current)
            return if (now.isBefore(candidate)) candidate else at(current.plusMonths(1))
        }

        private fun days(count: Int): String = when {
            count % 100 in 11..14 -> "дней"
            count % 10 == 1 -> "день"
            count % 10 in 2..4 -> "дня"
            else -> "дней"
        }

        /** «в ноябре». */
        private fun monthPrepositional(month: YearMonth): String =
            listOf("январе", "феврале", "марте", "апреле", "мае", "июне", "июле", "августе", "сентябре", "октябре", "ноябре", "декабре")[month.monthValue - 1]
    }
}
