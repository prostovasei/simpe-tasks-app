package com.prostovasei.simpletasks.ui.common

import androidx.annotation.DrawableRes
import com.prostovasei.simpletasks.R

/** Одна геометрия значков Недели для Compose и RemoteViews (#253). */
@DrawableRes
fun TaskItem.weekNodeIcon(): Int = when {
    isRecurring -> R.drawable.ic_repeat_round
    showsDone -> R.drawable.ic_week_node_fill
    else -> R.drawable.ic_week_node_ring
}
