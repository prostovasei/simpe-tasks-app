package com.prostovasei.simpletasks.domain.model.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Расчёты «Здоровья» (#257): вес, ИМТ, изменение за период, достижение цели и прогноз. */
class WeightMathTest {
    private val today = LocalDate.of(2026, 11, 16)

    /** Записи: пары «дней назад» → вес в десятых. */
    private fun entries(vararg points: Pair<Int, Int>) = points.map { (ago, tenths) -> WeightEntry(today.minusDays(ago.toLong()), tenths) }.sortedBy { it.date }

    @Test
    fun weightIsFormattedWithCommaAndSign() {
        assertEquals("82,4", Weight.format(824))
        assertEquals("70,0", Weight.format(700))
        assertEquals("−0,8", Weight.formatDelta(-8))
        assertEquals("+0,2", Weight.formatDelta(2))
        assertEquals("±0", Weight.formatDelta(0))
        assertEquals(300, Weight.clamp(10))
        assertEquals(2500, Weight.clamp(9000))
    }

    @Test
    fun bmiIsRoundedToTenthAndClassified() {
        // 82,4 кг при 180 см: 25,43 → 25,4 «выше нормы»; 78,0 → 24,07 → 24,1 «норма».
        assertEquals(254, WeightMath.bmi(824, 180)!!.tenths)
        assertEquals("25,4", WeightMath.bmi(824, 180)!!.text)
        assertEquals(BmiClass.OVER, WeightMath.bmi(824, 180)!!.kind)
        assertEquals("24,1", WeightMath.bmi(780, 180)!!.text)
        assertEquals(BmiClass.NORMAL, WeightMath.bmi(780, 180)!!.kind)
        assertEquals(BmiClass.UNDER, WeightMath.bmi(500, 180)!!.kind)
        assertEquals(BmiClass.OBESE, WeightMath.bmi(1100, 180)!!.kind)
        // Границы: 18,5 — уже норма, 25,0 — уже «выше нормы».
        assertEquals(BmiClass.NORMAL, WeightMath.bmi(600, 180)!!.kind) // 18,52
        assertEquals(BmiClass.OVER, WeightMath.bmi(810, 180)!!.kind) // 25,0
        assertNull(WeightMath.bmi(824, null))
    }

    @Test
    fun changeIsLastMinusFirstWithinPeriod() {
        val all = entries(40 to 840, 30 to 832, 10 to 826, 0 to 824)
        assertEquals(-8, WeightMath.change(all, WeightPeriod.MONTH, today)) // 832 → 824, запись 40 дней назад в месяц не входит
        assertEquals(-16, WeightMath.change(all, WeightPeriod.ALL, today))
        assertEquals(-16, WeightMath.change(all, WeightPeriod.QUARTER, today))
        assertNull(WeightMath.change(entries(0 to 824), WeightPeriod.MONTH, today))
        assertNull(WeightMath.change(emptyList(), WeightPeriod.ALL, today))
    }

    @Test
    fun periodIncludesBoundaryDay() {
        val all = entries(30 to 830, 31 to 835, 0 to 824)
        assertEquals(listOf(30L, 0L), WeightMath.inPeriod(all, WeightPeriod.MONTH, today).map { java.time.temporal.ChronoUnit.DAYS.between(it.date, today) })
    }

    @Test
    fun goalIsReachedFromAboveWhenWeightDropsToIt() {
        val down = entries(30 to 830, 0 to 780)
        assertTrue(WeightMath.reached(down, 780))
        assertEquals(Forecast.Reached, WeightMath.forecast(down, 780, today))
        assertEquals(false, WeightMath.reached(entries(30 to 830, 0 to 790), 780))
        // Набор веса: начинали ниже цели.
        assertTrue(WeightMath.reached(entries(30 to 700, 0 to 720), 720))
        assertEquals(false, WeightMath.reached(emptyList(), 780))
    }

    @Test
    fun noGoalMeansNoForecast() {
        assertEquals(Forecast.NoGoal, WeightMath.forecast(entries(0 to 824), null, today))
    }

    @Test
    fun forecastNeedsFiveEntriesOverTwoWeeks() {
        // Мало записей.
        assertEquals(Forecast.NotEnoughData, WeightMath.forecast(entries(20 to 830, 10 to 828, 0 to 824), 780, today))
        // Записей достаточно, но растянуты меньше чем на 14 дней.
        assertEquals(Forecast.NotEnoughData, WeightMath.forecast(entries(10 to 830, 8 to 829, 6 to 828, 4 to 827, 0 to 826), 780, today))
        assertEquals(Forecast.NotEnoughData, WeightMath.forecast(emptyList(), 780, today))
    }

    @Test
    fun forecastFollowsLinearTrend() {
        // Ровно −0,1 кг в день (−1 десятая): было 83,0, стало 82,0 за 10 дней... берём 30 дней: −3 кг, до цели 78,0 ещё 4,0 кг.
        val trend = entries(30 to 850, 24 to 844, 18 to 838, 12 to 832, 6 to 826, 0 to 820)
        val forecast = WeightMath.forecast(trend, 780, today)
        // Наклон −1 десятая в день, осталось 40 десятых → 40 дней.
        assertEquals(Forecast.At(today.plusDays(40)), forecast)
    }

    @Test
    fun forecastSaysWrongWayWhenWeightMovesAwayFromGoal() {
        val up = entries(30 to 820, 24 to 822, 18 to 826, 12 to 830, 6 to 832, 0 to 836)
        assertEquals(Forecast.WrongWay(rising = true), WeightMath.forecast(up, 780, today))
        val flat = entries(30 to 824, 24 to 824, 18 to 824, 12 to 824, 6 to 824, 0 to 824)
        assertEquals(Forecast.WrongWay(rising = null), WeightMath.forecast(flat, 780, today))
    }

    @Test
    fun forecastWorksForGainingToo() {
        val gain = entries(30 to 700, 24 to 706, 18 to 712, 12 to 718, 6 to 724, 0 to 730)
        // Первая запись ниже цели 76,0 → набираем; +1 десятая в день, осталось 30 десятых.
        assertEquals(Forecast.At(today.plusDays(30)), WeightMath.forecast(gain, 760, today))
        // Если при этом вес падает — «не туда».
        val lose = entries(30 to 730, 24 to 724, 18 to 718, 12 to 712, 6 to 706, 0 to 700)
        // Начинали выше цели 72,0, дошли до 70,0 — цель достигнута (пересекли).
        assertEquals(Forecast.Reached, WeightMath.forecast(lose, 720, today))
    }

    @Test
    fun veryFarForecastIsNotShown() {
        val slow = entries(30 to 900, 24 to 900, 18 to 900, 12 to 900, 6 to 900, 0 to 899)
        assertEquals(Forecast.WrongWay(rising = null), WeightMath.forecast(slow, 600, today))
    }
}
