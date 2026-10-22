package com.prostovasei.simpletasks.ui.feature.task

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter

/**
 * Высота клавиатуры в px для окна задачи (доработка 0.23.3, макет PM «А»): окно стоит сразу над клавиатурой
 * и не двигается, когда она открывается и закрывается. Высота запоминается после того, как клавиатура встала
 * на место, и хранится между запусками; пока клавиатура ни разу не открывалась — [fallbackPx].
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberKeyboardHeight(fallbackPx: Int): Int {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var saved by remember { mutableIntStateOf(prefs.getInt(KEY_HEIGHT, 0)) }
    val ime = WindowInsets.ime
    LaunchedEffect(ime, density) {
        snapshotFlow { ime.getBottom(density) }
            .debounce(SETTLE_MS)
            .filter { it > 0 && it != saved }
            .collect {
                saved = it
                prefs.edit().putInt(KEY_HEIGHT, it).apply()
            }
    }
    return if (saved > 0) saved else fallbackPx
}

private const val PREFS = "task_sheet"
private const val KEY_HEIGHT = "keyboard_height"
private const val SETTLE_MS = 300L
