package app.editors.manager.viewModels.main

import android.content.Context
import app.editors.manager.managers.tools.FontManager
import app.editors.manager.managers.tools.PreferenceTool
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import lib.toolkit.base.managers.tools.ResourcesProvider
import lib.toolkit.base.managers.utils.EditorDictionaries
import lib.toolkit.base.managers.utils.addDictionary
import lib.toolkit.base.managers.utils.contextWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val main = UnconfinedTestDispatcher()
    private val locale = Locale.getDefault()

    private lateinit var context: Context
    private lateinit var dir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        Locale.setDefault(Locale.ENGLISH)
        context = contextWith(folder.root)
        dir = EditorDictionaries.of(context).dir
    }

    @After
    fun tearDown() {
        Locale.setDefault(locale)
        Dispatchers.resetMain()
    }

    private fun viewModel(io: TestDispatcher = main): AppSettingsViewModel {
        val resourcesProvider = mockk<ResourcesProvider>(relaxed = true)
        every { resourcesProvider.context } returns context
        every { resourcesProvider.getCacheDir(any()) } returns null
        return AppSettingsViewModel(mockk(relaxed = true), resourcesProvider, mockk<PreferenceTool>(relaxed = true), mockk<FontManager>(relaxed = true), io)
    }

    @Test
    fun `lists the downloaded dictionaries by their language`() {
        dir.addDictionary("de_DE", size = 5)
        dir.addDictionary("fr_FR", size = 7)

        assertEquals(
            listOf(SpellingDictionary("fr_FR", "French (France)", 7), SpellingDictionary("de_DE", "German (Germany)", 5)),
            viewModel().settingsState.value.dictionaries
        )
    }

    @Test
    fun `reads the dictionaries off the main thread`() {
        dir.addDictionary("de_DE")
        val io = StandardTestDispatcher(main.scheduler)

        val viewModel = viewModel(io)
        assertEquals(emptyList<SpellingDictionary>(), viewModel.settingsState.value.dictionaries)

        io.scheduler.advanceUntilIdle()
        assertEquals(listOf("de_DE"), viewModel.settingsState.value.dictionaries.map { it.name })
    }

    @Test
    fun `deleting a dictionary removes it and remembers that`() {
        dir.addDictionary("de_DE")
        dir.addDictionary("fr_FR")
        val viewModel = viewModel()

        viewModel.deleteDictionary(viewModel.settingsState.value.dictionaries.first { it.name == "de_DE" })

        assertFalse(File(dir, "de_DE").exists())
        assertEquals(setOf("de_DE"), EditorDictionaries.removed(context))
        assertEquals(listOf("fr_FR"), viewModel.settingsState.value.dictionaries.map { it.name })
    }

    @Test
    fun `deletes a dictionary off the main thread`() {
        dir.addDictionary("de_DE")
        val io = StandardTestDispatcher(main.scheduler)
        val viewModel = viewModel(io)
        io.scheduler.advanceUntilIdle()

        viewModel.deleteDictionary(viewModel.settingsState.value.dictionaries.single())
        assertTrue(File(dir, "de_DE").exists())

        io.scheduler.advanceUntilIdle()
        assertFalse(File(dir, "de_DE").exists())
        assertEquals(emptyList<SpellingDictionary>(), viewModel.settingsState.value.dictionaries)
    }

    @Test
    fun `deleting a dictionary keeps what changed meanwhile`() {
        dir.addDictionary("de_DE")
        val io = StandardTestDispatcher(main.scheduler)
        val viewModel = viewModel(io)
        io.scheduler.advanceUntilIdle()

        viewModel.deleteDictionary(viewModel.settingsState.value.dictionaries.single())
        viewModel.setWifiState(true)
        io.scheduler.advanceUntilIdle()

        assertTrue(viewModel.settingsState.value.wifi)
    }
}
