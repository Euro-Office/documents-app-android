package lib.toolkit.base.managers.utils

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * The spell checking dictionaries the editors download when a document needs them (English comes
 * with them): one folder each in [dir], named after the dictionary ("de_DE"). The app's settings
 * list and remove them; a removed one isn't downloaded for the device's language any more.
 */
class EditorDictionaries(val dir: File) {

    /** The downloaded dictionaries, also ones of older versions of the app. */
    fun downloaded(): List<String> =
        dir.listFiles { file -> file.isDirectory && File(file, "${file.name}.dic").exists() }?.map { it.name }.orEmpty()

    /** The size of a downloaded dictionary's files, in bytes. */
    fun size(name: String): Long = File(dir, name).listFiles()?.sumOf { it.length() } ?: 0

    /** Removes a downloaded dictionary; it is downloaded again when a document needs it. */
    fun remove(name: String): Boolean = name in downloaded() && File(dir, name).deleteRecursively()

    companion object {

        private const val PREFERENCES = "lib.editors.spelling"
        private const val KEY_REMOVED = "removed_dictionaries"

        fun of(context: Context) = EditorDictionaries(File(context.filesDir, "editors/dictionaries"))

        /** The dictionaries the user removed. */
        fun removed(context: Context): Set<String> =
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getStringSet(KEY_REMOVED, null).orEmpty()

        fun setRemoved(context: Context, name: String, removed: Boolean) {
            val names = removed(context).let { if (removed) it + name else it - name }
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putStringSet(KEY_REMOVED, names).apply()
        }

        /**
         * A language's name in [locale]'s language ("German (Germany)"), by its tag ("de-DE") or
         * a dictionary's name ("de_DE").
         */
        fun languageName(tag: String, locale: Locale = Locale.getDefault()): String =
            Locale.forLanguageTag(tag.replace('_', '-')).getDisplayName(locale).ifEmpty { tag }.replaceFirstChar { it.titlecase(locale) }
    }
}
