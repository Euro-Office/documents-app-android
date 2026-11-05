package lib.toolkit.base.managers.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

/** A context with [filesDir] and preferences kept in memory. */
private fun contextWith(filesDir: File): Context {
    var names: Set<String>? = null
    val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    every { editor.putStringSet(any(), any()) } answers {
        names = secondArg()
        editor
    }
    val preferences = mockk<SharedPreferences>()
    every { preferences.getStringSet(any(), any()) } answers { names ?: secondArg() }
    every { preferences.edit() } returns editor
    val context = mockk<Context>(relaxed = true)
    every { context.filesDir } returns filesDir
    every { context.getSharedPreferences(any(), any()) } returns preferences
    return context
}

/** A downloaded dictionary with [size] bytes in its .dic file. */
private fun File.addDictionary(name: String, size: Int = 3) {
    File(this, name).mkdirs()
    File(this, "$name/$name.dic").writeBytes(ByteArray(size))
}

class EditorDictionariesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var dictionaries: EditorDictionaries

    @Before
    fun setUp() {
        dir = folder.newFolder("dictionaries")
        dictionaries = EditorDictionaries(dir)
    }

    @Test
    fun `lists only folders with a dictionary`() {
        dir.addDictionary("de_DE")
        File(dir, "fr_FR.partial").mkdirs()
        File(dir, "empty").mkdirs()
        File(dir, "user-words.txt").writeText("word\n")

        assertEquals(listOf("de_DE"), dictionaries.downloaded())
    }

    @Test
    fun `lists nothing before the first download`() {
        assertEquals(emptyList<String>(), EditorDictionaries(File(dir, "missing")).downloaded())
    }

    @Test
    fun `size adds up a dictionary's files`() {
        dir.addDictionary("de_DE", size = 5)
        File(dir, "de_DE/de_DE.aff").writeBytes(ByteArray(2))

        assertEquals(7L, dictionaries.size("de_DE"))
        assertEquals(0L, dictionaries.size("fr_FR"))
    }

    @Test
    fun `remove deletes only that dictionary`() {
        dir.addDictionary("de_DE")
        dir.addDictionary("fr_FR")

        assertTrue(dictionaries.remove("de_DE"))

        assertEquals(listOf("fr_FR"), dictionaries.downloaded())
    }

    @Test
    fun `remove leaves what isn't a dictionary`() {
        File(dir, "user-words.txt").writeText("word\n")

        assertFalse(dictionaries.remove("user-words.txt"))

        assertTrue(File(dir, "user-words.txt").exists())
    }

    @Test
    fun `remove takes the folder of a running download away`() {
        dir.addDictionary("de_DE")
        File(dir, "de_DE.partial").mkdirs()
        File(dir, "de_DE.partial/de_DE.aff").writeText("SET UTF-8\n")

        dictionaries.remove("de_DE")

        assertFalse(File(dir, "de_DE.partial").exists())
    }

    @Test
    fun `removed dictionaries are remembered until they are back`() {
        val context = contextWith(folder.root)

        EditorDictionaries.setRemoved(context, "de_DE", true)
        EditorDictionaries.setRemoved(context, "fr_FR", true)
        assertEquals(setOf("de_DE", "fr_FR"), EditorDictionaries.removed(context))

        EditorDictionaries.setRemoved(context, "de_DE", false)
        assertEquals(setOf("fr_FR"), EditorDictionaries.removed(context))
    }

    @Test
    fun `the folder is in the app's files`() {
        assertEquals(File(folder.root, "editors/dictionaries"), EditorDictionaries.of(contextWith(folder.root)).dir)
    }

    @Test
    fun `language names come from tags and dictionary names`() {
        assertEquals("German (Germany)", EditorDictionaries.languageName("de-DE", Locale.ENGLISH))
        assertEquals("German (Germany)", EditorDictionaries.languageName("de_DE", Locale.ENGLISH))
        assertEquals("Deutsch (Deutschland)", EditorDictionaries.languageName("de_DE", Locale.GERMAN))
    }
}
