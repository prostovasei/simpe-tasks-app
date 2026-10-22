package com.prostovasei.simpletasks.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.BeforeLabel
import com.prostovasei.simpletasks.domain.model.DayPart
import com.prostovasei.simpletasks.domain.model.ReminderBadge

/**
 * Колокольчик напоминаний со значком (решение PM, итерация 23): в момент срока — просто колокольчик;
 * «за …» — серый значок с «30», «1ч», «1д», «3д» справа внизу; время суток у задачи на весь день — круг,
 * заливка которого растёт с верхней левой четверти (ночь — луна); вручную — часики; несколько — число справа вверху.
 *
 * Рисуется тремя белыми масками — колокольчик, подложка значка, знак на ней — и каждая красится своим цветом.
 * Так один рисунок работает и в приложении, и в виджете (там цвет подставляет система под светлую или тёмную тему).
 * Вокруг значка колокольчик вырезан — значок не сливается с ним на любом фоне.
 */
object ReminderIconMasks {
    data class Masks(val bell: Bitmap, val pill: Bitmap?, val glyph: Bitmap?)

    private val cache = LruCache<String, Masks>(CACHE_SIZE)

    fun of(context: Context, badge: ReminderBadge, sizePx: Int): Masks {
        val label = label(context, badge)
        val key = "$badge|$label|$sizePx"
        return cache.get(key) ?: render(badge, label, sizePx, typeface(context)).also { cache.put(key, it) }
    }

    /** Подпись значка: «30», «1ч», число напоминаний; у остальных значков — рисунок. */
    fun label(context: Context, badge: ReminderBadge): String? = when (badge) {
        is ReminderBadge.Before -> context.getString(
            when (badge.label) {
                BeforeLabel.MINUTES_30 -> R.string.reminder_badge_30m
                BeforeLabel.HOUR_1 -> R.string.reminder_badge_1h
                BeforeLabel.DAY_1 -> R.string.reminder_badge_1d
                BeforeLabel.DAYS_3 -> R.string.reminder_badge_3d
            },
        )
        is ReminderBadge.Several -> if (badge.count > MAX_COUNT) context.getString(R.string.reminder_badge_many, MAX_COUNT) else badge.count.toString()
        else -> null
    }

    private fun typeface(context: Context): Typeface =
        runCatching { ResourcesCompat.getFont(context, R.font.manrope_800) }.getOrNull() ?: Typeface.DEFAULT_BOLD

    private fun render(badge: ReminderBadge, label: String?, sizePx: Int, typeface: Typeface): Masks {
        val scale = sizePx / VIEWPORT
        fun bitmap() = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        fun stroke(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = WHITE
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE }

        val bell = bitmap()
        val bellCanvas = Canvas(bell)
        bellCanvas.drawPath(BELL_PATH.scaled(scale), stroke(BELL_STROKE))

        val pillRect = pillRect(badge, label) ?: return Masks(bell, null, null)
        val rect = RectF(pillRect.left * scale, pillRect.top * scale, pillRect.right * scale, pillRect.bottom * scale)
        // Вырез вокруг значка.
        val gap = GAP * scale
        val cut = RectF(rect.left - gap, rect.top - gap, rect.right + gap, rect.bottom + gap)
        bellCanvas.drawRoundRect(cut, cut.height() / 2, cut.height() / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })

        val pill = bitmap()
        Canvas(pill).drawRoundRect(rect, rect.height() / 2, rect.height() / 2, fill)

        val glyph = bitmap()
        val canvas = Canvas(glyph)
        val cx = rect.centerX()
        val cy = rect.centerY()
        when (badge) {
            is ReminderBadge.Before, is ReminderBadge.Several -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = TEXT_SIZE * scale
                    this.typeface = typeface
                }
                val metrics = paint.fontMetrics
                canvas.drawText(label.orEmpty(), cx, cy - (metrics.ascent + metrics.descent) / 2, paint)
            }
            ReminderBadge.Custom -> {
                canvas.drawCircle(cx, cy, CLOCK_RADIUS * scale, stroke(CLOCK_STROKE))
                val hands = Path().apply {
                    moveTo(cx, cy - CLOCK_HAND * scale)
                    lineTo(cx, cy)
                    lineTo(cx + CLOCK_HAND_X * scale, cy + CLOCK_HAND_Y * scale)
                }
                canvas.drawPath(hands, stroke(CLOCK_STROKE))
            }
            is ReminderBadge.AtDayPart -> drawDayPart(canvas, badge.part, cx, cy, scale, stroke(PART_STROKE), fill)
            ReminderBadge.AtDue -> Unit
        }
        return Masks(bell, pill, glyph)
    }

    /** Круг заполняется по часовой с верхней левой четверти (с «9 часов»): утро — ¼, обед — ½, день — ¾, вечер — весь. */
    private fun drawDayPart(canvas: Canvas, part: DayPart, cx: Float, cy: Float, scale: Float, ring: Paint, fill: Paint) {
        val r = PART_RADIUS * scale
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        when (part) {
            DayPart.NIGHT -> {
                val moon = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
                val shadow = Path().apply { addCircle(cx + MOON_SHIFT_X * scale, cy - MOON_SHIFT_Y * scale, r * MOON_SHADOW, Path.Direction.CW) }
                moon.op(shadow, Path.Op.DIFFERENCE)
                canvas.drawPath(moon, fill)
            }
            DayPart.EVENING -> canvas.drawCircle(cx, cy, r, fill)
            else -> {
                canvas.drawCircle(cx, cy, r, ring)
                val quarters = when (part) {
                    DayPart.MORNING -> 1
                    DayPart.NOON -> 2
                    else -> 3
                }
                canvas.drawArc(oval, START_AT_NINE, QUARTER * quarters, true, fill)
            }
        }
    }

    /** Значок: внизу справа, у «несколько» — вверху справа. Координаты — в сетке 24×24. */
    private fun pillRect(badge: ReminderBadge, label: String?): RectF? {
        val width = when (badge) {
            ReminderBadge.AtDue -> return null
            is ReminderBadge.AtDayPart -> PILL_ROUND
            ReminderBadge.Custom -> PILL_ROUND
            is ReminderBadge.Before, is ReminderBadge.Several -> when (label.orEmpty().length) {
                0, 1 -> PILL_HEIGHT
                2 -> PILL_WIDE
                else -> PILL_WIDER
            }
        }
        val height = if (badge is ReminderBadge.AtDayPart || badge == ReminderBadge.Custom) PILL_ROUND else PILL_HEIGHT
        val top = if (badge is ReminderBadge.Several) TOP_EDGE else VIEWPORT - BOTTOM_INSET - height
        return RectF(VIEWPORT - width, top, VIEWPORT, top + height)
    }

    private fun Path.scaled(scale: Float): Path = Path(this).apply { transform(Matrix().apply { setScale(scale, scale) }) }

    private val BELL_PATH: Path = PathParser.createPathFromPathData("M6,10a6,6 0 0 1 12,0c0,5 2,7 2,7H4c0,0 2,-2 2,-7 M10,20.5a2,2 0 0 0 4,0")

    private const val VIEWPORT = 24f
    private const val BELL_STROKE = 1.9f
    private const val GAP = 1.6f
    private const val PILL_HEIGHT = 11.5f
    private const val PILL_WIDE = 14.5f
    private const val PILL_WIDER = 16.5f
    private const val PILL_ROUND = 12.5f
    private const val TOP_EDGE = 0f
    private const val BOTTOM_INSET = 0.25f
    private const val TEXT_SIZE = 8.8f
    private const val CLOCK_RADIUS = 3.7f
    private const val CLOCK_STROKE = 1.4f
    private const val CLOCK_HAND = 2.1f
    private const val CLOCK_HAND_X = 1.4f
    private const val CLOCK_HAND_Y = 0.9f
    private const val PART_RADIUS = 3.6f
    private const val PART_STROKE = 1.1f
    private const val MOON_SHIFT_X = 1.9f
    private const val MOON_SHIFT_Y = 1.3f
    private const val MOON_SHADOW = 0.8f
    private const val START_AT_NINE = 180f
    private const val QUARTER = 90f
    private const val MAX_COUNT = 9
    private const val CACHE_SIZE = 64
    private const val WHITE = android.graphics.Color.WHITE
}

/**
 * Колокольчик со значком в Compose: [tint] — колокольчик, [badgeColor] — подложка значка, [glyphColor] — знак на ней.
 */
@Composable
fun ReminderIcon(
    badge: ReminderBadge,
    size: Dp,
    tint: Color,
    badgeColor: Color,
    glyphColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val masks = remember(badge, px) { ReminderIconMasks.of(context, badge, px) }
    Box(modifier.size(size)) {
        Image(masks.bell.asImageBitmap(), null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(size))
        masks.pill?.let { Image(it.asImageBitmap(), null, colorFilter = ColorFilter.tint(badgeColor), modifier = Modifier.size(size)) }
        masks.glyph?.let { Image(it.asImageBitmap(), null, colorFilter = ColorFilter.tint(glyphColor), modifier = Modifier.size(size)) }
    }
}
