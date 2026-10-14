package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Цвет списка из БД («#RRGGBB» или «#AARRGGBB») → Color; некорректное значение — без цвета. */
fun parseListColor(hex: String?): Color? {
    val digits = hex?.trim()?.removePrefix("#") ?: return null
    val value = digits.toLongOrNull(radix = 16) ?: return null
    return when (digits.length) {
        6 -> Color(OPAQUE or value)
        8 -> Color(value)
        else -> null
    }
}

private const val OPAQUE = 0xFF000000L
