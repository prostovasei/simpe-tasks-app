package com.prostovasei.simpletasks.ui.common

import androidx.annotation.DrawableRes
import com.prostovasei.simpletasks.R

/** Одна геометрия значков Недели для Compose и RemoteViews (#253). */
@DrawableRes
fun TaskItem.weekNodeIcon(): Int = when {
    isRecurring -> R.drawable.ic_task_repeat_round
    showsDone -> R.drawable.ic_week_node_fill
    else -> R.drawable.ic_week_node_ring
}

/** Те же роли для вытянутого интервала Предстоящих. */
@DrawableRes
fun TaskItem.canvasNodeIcon(period: Boolean): Int = if (!period) weekNodeIcon() else when {
    isRecurring -> R.drawable.ic_task_period_repeat
    showsDone -> R.drawable.ic_task_period_fill
    else -> R.drawable.ic_task_period_ring
}

/** Цвет текста задачи на холсте; [done] = false — как у невыполненной (#316: в приложении бледность даёт прозрачность строки). */
fun TaskItem.canvasTextColor(past: Boolean = false, done: Boolean = showsDone): com.prostovasei.simpletasks.designsystem.tokens.ThemedColor {
    val colors = com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
    return when {
        priority == com.prostovasei.simpletasks.domain.model.Priority.HIGH -> if (done) colors.priorityDone else colors.priority
        done -> colors.done
        past -> com.prostovasei.simpletasks.designsystem.tokens.ColorTokens.textSecondary
        else -> com.prostovasei.simpletasks.designsystem.tokens.ColorTokens.textPrimary
    }
}
