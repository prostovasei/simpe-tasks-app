package com.prostovasei.simpletasks.domain.model.health

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Запись веса (#257): одна на дату; вес хранится целым числом десятых килограмма (824 = 82,4 кг). */
data class WeightEntry(val date: LocalDate, val tenths: Int, val comment: String? = null)

/** Настройки «Здоровья» (#257): цель и рост необязательны; напоминание выключено по умолчанию. */
data class HealthSettings(
    val goalTenths: Int? = null,
    val heightCm: Int? = null,
    val reminderOn: Boolean = false,
    val reminderMinute: Int = DEFAULT_REMINDER_MINUTE,
) {
    companion object {
        const val DEFAULT_REMINDER_MINUTE = 8 * 60
    }
}

/** Файл копии не подходит (#257): данные остаются как были. */
class InvalidHealthBackupException(message: String) : Exception(message)

/** Что лежит в файле резервной копии «Здоровья». */
data class HealthBackupInfo(val entries: Int, val from: LocalDate?, val to: LocalDate?, val settings: HealthSettings)

/** Границы и отображение веса. */
object Weight {
    const val MIN_TENTHS = 300
    const val MAX_TENTHS = 2500
    const val DEFAULT_TENTHS = 700
    const val MIN_HEIGHT = 100
    const val MAX_HEIGHT = 230
    const val DEFAULT_HEIGHT = 170

    fun clamp(tenths: Int): Int = tenths.coerceIn(MIN_TENTHS, MAX_TENTHS)

    /** 824 → «82,4». */
    fun format(tenths: Int): String = "${tenths / 10},${abs(tenths % 10)}"

    /** Разница: «−0,8», «+0,2», «±0». */
    fun formatDelta(deltaTenths: Int): String = when {
        deltaTenths == 0 -> "±0"
        deltaTenths < 0 -> "−" + format(-deltaTenths)
        else -> "+" + format(deltaTenths)
    }
}

/** Индекс массы тела: ниже нормы, норма, выше нормы, ожирение (границы 18,5 / 25 / 30). */
enum class BmiClass { UNDER, NORMAL, OVER, OBESE }

data class Bmi(val value: Double, val tenths: Int, val kind: BmiClass) {
    /** «24,1». */
    val text: String get() = "${tenths / 10},${tenths % 10}"
}

/** Период графика и изменения: месяц, три месяца, год, всё время. */
enum class WeightPeriod(val days: Int?) { MONTH(30), QUARTER(90), YEAR(365), ALL(null) }

/** Прогноз достижения цели. */
sealed interface Forecast {
    /** Цели нет. */
    data object NoGoal : Forecast

    /** Цель достигнута. */
    data object Reached : Forecast

    /** Мало записей: нужно [MIN_ENTRIES] за [MIN_SPAN_DAYS] дней. */
    data object NotEnoughData : Forecast

    /** Вес за последние 30 дней не движется к цели: [rising] — растёт, иначе снижается; null — стоит. */
    data class WrongWay(val rising: Boolean?) : Forecast

    /** Цель по текущему темпу — примерно к [date]. */
    data class At(val date: LocalDate) : Forecast

    companion object {
        const val WINDOW_DAYS = 30
        const val MIN_ENTRIES = 5
        const val MIN_SPAN_DAYS = 14
        const val MAX_DAYS = 3650
    }
}

/** Расчёты «Здоровья» — без Android, чтобы проверяться обычными тестами. */
object WeightMath {
    /** ИМТ = вес / рост², одна десятая; без роста — null. */
    fun bmi(tenths: Int, heightCm: Int?): Bmi? {
        if (heightCm == null || heightCm <= 0) return null
        val meters = heightCm / 100.0
        val value = tenths / 10.0 / (meters * meters)
        val rounded = (value * 10).roundToInt()
        val kind = when {
            value < 18.5 -> BmiClass.UNDER
            value < 25.0 -> BmiClass.NORMAL
            value < 30.0 -> BmiClass.OVER
            else -> BmiClass.OBESE
        }
        return Bmi(value, rounded, kind)
    }

    /** Записи периода: от [today] минус дни периода до сегодня (для «всё» — все). */
    fun inPeriod(entries: List<WeightEntry>, period: WeightPeriod, today: LocalDate): List<WeightEntry> {
        val days = period.days ?: return entries
        val from = today.minusDays(days.toLong())
        return entries.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
    }

    /** Изменение за период: последняя запись минус первая в периоде; меньше двух записей — null. */
    fun change(entries: List<WeightEntry>, period: WeightPeriod, today: LocalDate): Int? {
        val inside = inPeriod(entries, period, today).sortedBy { it.date }
        if (inside.size < 2) return null
        return inside.last().tenths - inside.first().tenths
    }

    /** Цель достигнута: вес пересёк цель с той стороны, с какой начинался (по самой ранней записи). */
    fun reached(entries: List<WeightEntry>, goalTenths: Int): Boolean {
        val sorted = entries.sortedBy { it.date }
        val first = sorted.firstOrNull() ?: return false
        val last = sorted.last()
        return if (first.tenths > goalTenths) last.tenths <= goalTenths else if (first.tenths < goalTenths) last.tenths >= goalTenths else true
    }

    /**
     * Прогноз по линейному тренду (метод наименьших квадратов) за последние [Forecast.WINDOW_DAYS] дней: нужно не меньше
     * [Forecast.MIN_ENTRIES] записей, растянутых не меньше чем на [Forecast.MIN_SPAN_DAYS] дней.
     */
    fun forecast(entries: List<WeightEntry>, goalTenths: Int?, today: LocalDate): Forecast {
        if (goalTenths == null) return Forecast.NoGoal
        val sorted = entries.sortedBy { it.date }
        if (sorted.isEmpty()) return Forecast.NotEnoughData
        if (reached(sorted, goalTenths)) return Forecast.Reached
        val from = today.minusDays(Forecast.WINDOW_DAYS.toLong())
        val window = sorted.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
        val span = if (window.size < 2) 0L else ChronoUnit.DAYS.between(window.first().date, window.last().date)
        if (window.size < Forecast.MIN_ENTRIES || span < Forecast.MIN_SPAN_DAYS) return Forecast.NotEnoughData
        val slope = slopePerDay(window)
        val current = sorted.last().tenths
        val needDown = current > goalTenths
        val moving = if (needDown) slope < -EPS else slope > EPS
        if (!moving) return Forecast.WrongWay(if (abs(slope) <= EPS) null else slope > 0)
        // Допуск гасит погрешность дробей: ровно 30 дней не должны превращаться в 31.
        val days = ceil(abs(current - goalTenths) / abs(slope) - DAYS_EPS).toLong()
        if (days > Forecast.MAX_DAYS) return Forecast.WrongWay(null)
        return Forecast.At(sorted.last().date.plusDays(days).let { if (it.isBefore(today)) today else it })
    }

    /** Наклон тренда: десятых килограмма в день. */
    internal fun slopePerDay(window: List<WeightEntry>): Double {
        val base = window.first().date
        val xs = window.map { ChronoUnit.DAYS.between(base, it.date).toDouble() }
        val ys = window.map { it.tenths.toDouble() }
        val mx = xs.average()
        val my = ys.average()
        val den = xs.sumOf { (it - mx) * (it - mx) }
        if (den == 0.0) return 0.0
        return xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / den
    }

    private const val EPS = 0.05
    private const val DAYS_EPS = 1e-9
}
