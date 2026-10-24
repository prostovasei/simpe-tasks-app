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
 * Колокольчик напоминаний (итерация 27, решение PM): без цифр. В момент срока — просто колокольчик,
 * заранее — со стрелкой назад справа вверху (0.27.1, было — точка).
 *
 * Рисуется двумя белыми масками — колокольчик и стрелка — и каждая красится своим цветом. Так один рисунок работает
 * и в приложении, и в виджете (там цвет подставляет система под светлую или тёмную тему). Вокруг стрелки колокольчик
 * вырезан — она не сливается с ним на любом фоне.
 */
object ReminderIconMasks {
    data class Masks(val bell: Bitmap, val early: Bitmap?)

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
        fun stroke(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = WHITE
        }
        fun Path.scaled() = Path(this).apply { transform(Matrix().apply { setScale(scale, scale) }) }
        bellCanvas.drawPath(BELL_PATH.scaled(), stroke(BELL_STROKE))
        if (mark == ReminderMark.AT_DUE) return Masks(bell, null)

        // Вырез — та же стрелка, только толще: щель вокруг неё одинаковая.
        val arrow = ARROW_PATH.scaled()
        val clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        bellCanvas.drawPath(arrow, stroke(ARROW_STROKE + GAP * 2).apply { xfermode = clear })
        // И всё внутри загиба — без обрывка колокольчика между стрелкой и хвостом.
        bellCanvas.drawRect(HOOK_LEFT * scale, HOOK_TOP * scale, HOOK_RIGHT * scale, HOOK_BOTTOM * scale, Paint().apply { xfermode = clear })
        val early = bitmap()
        Canvas(early).drawPath(arrow, stroke(ARROW_STROKE))
        return Masks(bell, early)
    }

    private val BELL_PATH: Path = PathParser.createPathFromPathData("M6,10a6,6 0 0 1 12,0c0,5 2,7 2,7H4c0,0 2,-2 2,-7 M10,20.5a2,2 0 0 0 4,0")

    /** Стрелка назад справа вверху: уголок влево и хвост, загнутый вниз (как «отменить»). Сетка 24×24. */
    private val ARROW_PATH: Path = PathParser.createPathFromPathData("M15.4,2.4L13.2,4.6L15.4,6.8 M13.6,4.6H18.8a2.9,2.9 0 0 1 0,5.8H17.6")

    private const val VIEWPORT = 24f
    private const val BELL_STROKE = 1.9f
    private const val ARROW_STROKE = 1.8f
    private const val GAP = 1.4f
    private const val HOOK_LEFT = 13.6f
    private const val HOOK_TOP = 4.6f
    private const val HOOK_RIGHT = 21.7f
    private const val HOOK_BOTTOM = 10.4f
    private const val CACHE_SIZE = 16
    private const val WHITE = android.graphics.Color.WHITE
}

/** Колокольчик в Compose: [tint] — колокольчик, [earlyColor] — стрелка «заранее». */
@Composable
fun ReminderIcon(
    mark: ReminderMark,
    size: Dp,
    tint: Color,
    earlyColor: Color,
    modifier: Modifier = Modifier,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val masks = remember(mark, px) { ReminderIconMasks.of(mark, px) }
    Box(modifier.size(size)) {
        Image(masks.bell.asImageBitmap(), null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(size))
        masks.early?.let { Image(it.asImageBitmap(), null, colorFilter = ColorFilter.tint(earlyColor), modifier = Modifier.size(size)) }
    }
}
