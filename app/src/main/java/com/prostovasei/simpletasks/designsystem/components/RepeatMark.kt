package com.prostovasei.simpletasks.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens

/**
 * Знак повтора вместо кружка задачи (итерация 47, макет `VariantH2`, решения PM): у задачи без длительности — круг из двух половин
 * ([tall] = false, рисунок `ic_repeat_round`), у задачи с длительностью — вытянутая на высоту капсулы ([tall] = true): верхняя половина
 * идёт от середины левой стороны вверх и вниз по правой (стрелка вниз), нижняя — от середины правой вниз и вверх по левой (стрелка вверх).
 * Цвет — приоритет. Нажатие обрабатывает родитель — «выполнено», как у кружка.
 */
@Composable
fun RepeatMark(color: Color, modifier: Modifier = Modifier, tall: Boolean = false) {
    if (!tall) {
        Icon(painterResource(R.drawable.ic_task_repeat_round), null, tint = color, modifier = modifier)
        return
    }
    Canvas(modifier) {
        // Пропорции — как у `ic_widget_period_repeat` (20 в ширину): края 3 и 17, радиус 7, разрыв у середины 2.5, стрелка 2.
        val unit = size.width / REPEAT_GRID
        val left = REPEAT_EDGE * unit
        val right = size.width - left
        val radius = REPEAT_RADIUS * unit
        val topCenter = radius + REPEAT_INSET * unit
        val bottomCenter = size.height - topCenter
        val mid = size.height / 2
        val gap = REPEAT_GAP * unit
        val arrow = REPEAT_ARROW * unit
        val path = Path().apply {
            moveTo(left, mid - gap)
            lineTo(left, topCenter)
            arcTo(Rect(left, topCenter - radius, right, topCenter + radius), HALF_TURN, HALF_TURN, false)
            lineTo(right, mid - gap)
            moveTo(right - arrow, mid - gap - arrow)
            lineTo(right, mid - gap)
            lineTo(right + arrow, mid - gap - arrow)
            moveTo(right, mid + gap)
            lineTo(right, bottomCenter)
            arcTo(Rect(left, bottomCenter - radius, right, bottomCenter + radius), 0f, HALF_TURN, false)
            lineTo(left, mid + gap)
            moveTo(left - arrow, mid + gap + arrow)
            lineTo(left, mid + gap)
            lineTo(left + arrow, mid + gap + arrow)
        }
        drawPath(path, color, style = Stroke(width = SizeTokens.CHECKBOX_STROKE.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private const val REPEAT_GRID = 20f
private const val REPEAT_EDGE = 3f
private const val REPEAT_RADIUS = 7f
private const val REPEAT_INSET = 2f
private const val REPEAT_GAP = 2.5f
private const val REPEAT_ARROW = 2f
private const val HALF_TURN = 180f
