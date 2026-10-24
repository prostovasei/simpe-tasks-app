package com.prostovasei.simpletasks.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.PathParser
import com.prostovasei.simpletasks.domain.model.ReminderMark

/**
 * Колокольчик напоминаний (итерация 27, решение PM): без цифр и значков. В момент срока — просто колокольчик,
 * заранее — с точкой справа вверху.
 *
 * Рисуется двумя белыми масками — колокольчик и точка — и каждая красится своим цветом. Так один рисунок работает
 * и в приложении, и в виджете (там цвет подставляет система под светлую или тёмную тему). Вокруг точки колокольчик
 * вырезан — точка не сливается с ним на любом фоне.
 */
object ReminderIconMasks {
    data class Masks(val bell: Bitmap, val dot: Bitmap?)

    private val cache = LruCache<String, Masks>(CACHE_SIZE)

    fun of(mark: ReminderMark, sizePx: Int): Masks {
        val key = "$mark|$sizePx"
        return cache.get(key) ?: render(mark, sizePx).also { cache.put(key, it) }
    }

    private fun render(mark: ReminderMark, sizePx: Int): Masks {
        val scale = sizePx / VIEWPORT
        fun bitmap() = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val bell = bitmap()
        val bellCanvas = Canvas(bell)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = BELL_STROKE * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = WHITE
        }
        bellCanvas.drawPath(Path(BELL_PATH).apply { transform(Matrix().apply { setScale(scale, scale) }) }, stroke)
        if (mark == ReminderMark.AT_DUE) return Masks(bell, null)

        val cx = DOT_X * scale
        val cy = DOT_Y * scale
        bellCanvas.drawCircle(cx, cy, (DOT_RADIUS + GAP) * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
        val dot = bitmap()
        Canvas(dot).drawCircle(cx, cy, DOT_RADIUS * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE })
        return Masks(bell, dot)
    }

    private val BELL_PATH: Path = PathParser.createPathFromPathData("M6,10a6,6 0 0 1 12,0c0,5 2,7 2,7H4c0,0 2,-2 2,-7 M10,20.5a2,2 0 0 0 4,0")

    private const val VIEWPORT = 24f
    private const val BELL_STROKE = 1.9f
    private const val DOT_X = 19f
    private const val DOT_Y = 5f
    private const val DOT_RADIUS = 3.6f
    private const val GAP = 1.6f
    private const val CACHE_SIZE = 16
    private const val WHITE = android.graphics.Color.WHITE
}

/** Колокольчик в Compose: [tint] — колокольчик, [dotColor] — точка «заранее». */
@Composable
fun ReminderIcon(
    mark: ReminderMark,
    size: Dp,
    tint: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val masks = remember(mark, px) { ReminderIconMasks.of(mark, px) }
    Box(modifier.size(size)) {
        Image(masks.bell.asImageBitmap(), null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(size))
        masks.dot?.let { Image(it.asImageBitmap(), null, colorFilter = ColorFilter.tint(dotColor), modifier = Modifier.size(size)) }
    }
}
