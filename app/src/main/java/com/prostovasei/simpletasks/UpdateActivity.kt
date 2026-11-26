package com.prostovasei.simpletasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.feature.update.UpdateDialogRoute
import dagger.hilt.android.AndroidEntryPoint

/**
 * Окно обновления поверх рабочего стола (итерация 32): открывается кнопкой в нижней строке виджета, приложение
 * не запускается. Проверка новой версии, загрузка и «Обновить» — в одном окне по центру. Закрытие окна возвращает на рабочий стол.
 */
@AndroidEntryPoint
class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme { UpdateDialogRoute(onClose = ::finish) }
        }
    }

    companion object {
        /** #354: кнопка «Обновить приложение» в «Главной» — новая версия скачивается и ставится сразу, без «Загрузить». */
        const val EXTRA_AUTO = "update_auto"
    }
}
