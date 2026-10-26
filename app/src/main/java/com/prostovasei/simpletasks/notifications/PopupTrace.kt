package com.prostovasei.simpletasks.notifications

import android.app.ActivityManager
import android.content.Context
import com.prostovasei.simpletasks.widget.WidgetLog

/**
 * Диагностика полоски (0.30.2): у PM полоска появляется только после касания её места. Каждый шаг — будильник,
 * показ, окно, кадры, анимация, сервис, касание — пишется с точным временем в журнал («Настройки», долгое нажатие
 * на строку версии → «Отправить»). По нему видно, на каком шаге она застревает.
 */
object PopupTrace {
    fun add(context: Context, event: String) = WidgetLog.add(context, "Полоска: $event")

    /** Насколько система считает приложение важным (100 — на экране, 125 — сервис, 400 и больше — фон). */
    fun importance(): Int = ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState).importance
}
