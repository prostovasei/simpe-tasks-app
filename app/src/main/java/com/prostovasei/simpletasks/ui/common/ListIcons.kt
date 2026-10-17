package com.prostovasei.simpletasks.ui.common

import androidx.annotation.DrawableRes
import com.prostovasei.simpletasks.R

/**
 * Значки списков (итерация 14): ключ хранится в БД (`projects.icon`), картинка — белый контур
 * поверх квадратика цвета списка. Одни и те же ресурсы в приложении и в виджете.
 */
object ListIcons {
    /** Значок «Входящих» — всегда, выбрать другой нельзя. */
    const val INBOX = "inbox"

    /** Варианты в меню списка «Значок», по порядку показа. */
    val choices: List<String> = listOf("work", "home", "health", "shopping", "study", "money", "travel", "sport", "idea", "star")

    @DrawableRes
    fun drawable(key: String?): Int? = when (key) {
        INBOX -> R.drawable.ic_list_inbox
        "work" -> R.drawable.ic_list_work
        "home" -> R.drawable.ic_list_home
        "health" -> R.drawable.ic_list_health
        "shopping" -> R.drawable.ic_list_shopping
        "study" -> R.drawable.ic_list_study
        "money" -> R.drawable.ic_list_money
        "travel" -> R.drawable.ic_list_travel
        "sport" -> R.drawable.ic_list_sport
        "idea" -> R.drawable.ic_list_idea
        "star" -> R.drawable.ic_list_star
        else -> null
    }
}
