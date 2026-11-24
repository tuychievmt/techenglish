package com.techenglish.app.widget

import com.techenglish.app.feature.widget.RefreshWidgetWordAction
import com.techenglish.app.feature.widget.resolveWidgetPalette
import com.techenglish.domain.model.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sp207GlanceWidgetThemeAndRefreshTest {

    private fun findWidgetFile(): File {
        val candidates = listOf(
            File("src/main/kotlin/com/techenglish/app/feature/widget/TechEnglishGlanceWidget.kt"),
            File("app/src/main/kotlin/com/techenglish/app/feature/widget/TechEnglishGlanceWidget.kt")
        )
        return candidates.first { it.isFile }
    }

    @Test
    fun `widget removes hardcoded Tokyo Night palette and follows active app ThemeMode`() {
        val src = findWidgetFile().readText()
        assertFalse("Widget must not hardcode Tokyo Night bg 0xFF1A1B26", src.contains("0xFF1A1B26"))
        assertFalse("Widget must not hardcode Tokyo Night accent 0xFF7AA2F7", src.contains("0xFF7AA2F7"))
        assertTrue("Widget must read SettingsRepository from DataStore", src.contains("settingsRepository"))
        assertTrue("Widget must wire RefreshWidgetWordAction button", src.contains("RefreshWidgetWordAction"))

        val lightPalette = resolveWidgetPalette(ThemeMode.LIGHT, systemDark = false)
        val draculaPalette = resolveWidgetPalette(ThemeMode.DRACULA, systemDark = true)
        val gruvboxPalette = resolveWidgetPalette(ThemeMode.GRUVBOX, systemDark = true)
        val tokyoPalette = resolveWidgetPalette(ThemeMode.TOKYO_NIGHT, systemDark = true)

        assertNotEquals("Light theme must differ from Tokyo Night", lightPalette.surfaceBg, tokyoPalette.surfaceBg)
        assertNotEquals("Dracula theme must differ from Gruvbox", draculaPalette.surfaceBg, gruvboxPalette.surfaceBg)
        assertNotEquals("Dracula accent must differ from Tokyo Night accent", draculaPalette.accentColor, tokyoPalette.accentColor)

        // Verify RefreshWidgetWordAction implements ActionCallback
        assertTrue(
            androidx.glance.appwidget.action.ActionCallback::class.java
                .isAssignableFrom(RefreshWidgetWordAction::class.java)
        )
    }
}
