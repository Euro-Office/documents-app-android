package app.editors.manager.managers.tools

import android.os.LocaleList
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppLocaleHelperTest {

    private fun localeList(vararg locales: Locale): LocaleList = mockk {
        every { size() } returns locales.size
        every { this@mockk[any()] } answers { locales[firstArg()] }
    }

    @Test
    fun `sorts the locale config's languages by their own names`() {
        // Deutsch, English, español, Français, Nederlands
        val supported = localeList(Locale("nl"), Locale("fr"), Locale("en"), Locale("es"), Locale("de"))
        assertEquals(
            listOf(Locale("de"), Locale("en"), Locale("es"), Locale("fr"), Locale("nl")),
            AppLocaleHelper.sortedLocales(supported)
        )
    }

    @Test
    fun `keeps the regional variants of a language`() {
        val supported = localeList(Locale("zh", "TW"), Locale("zh"), Locale("zh", "CN"))
        assertEquals(3, AppLocaleHelper.sortedLocales(supported).size)
    }

    @Test
    fun `lists no languages without a locale config`() {
        assertTrue(AppLocaleHelper.sortedLocales(null).isEmpty())
    }

    @Test
    fun `reads the locale config only when the languages are needed`() {
        // Creating the helper must not touch LocaleConfig, which needs a real device
        AppLocaleHelper(mockk(relaxed = true), mockk(relaxed = true))
    }
}
