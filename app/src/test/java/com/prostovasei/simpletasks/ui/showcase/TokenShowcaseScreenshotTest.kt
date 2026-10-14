package com.prostovasei.simpletasks.ui.showcase

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Скриншоты витрины токенов в обеих темах; сохраняются в app/screenshots командой ./gradlew recordRoborazziDebug. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1100dp-xhdpi")
class TokenShowcaseScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightTheme() = capture(darkTheme = false, name = "token_showcase_light")

    @Test
    fun darkTheme() = capture(darkTheme = true, name = "token_showcase_dark")

    private fun capture(darkTheme: Boolean, name: String) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) { TokenShowcaseScreen() }
        }
        composeRule.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
