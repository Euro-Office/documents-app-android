package lib.toolkit.base.managers.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import lib.toolkit.base.R
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash reports kept on the device instead of a crash reporting service. Crashes of the app
 * (uncaught exceptions) are written to its files; crashes in native code and freezes (ANRs) are
 * read from the system's record of how the app last ended. At the next start the user is asked
 * whether to send them ([offerReports]): nothing leaves the device unless the user shares it.
 *
 * Reports include recent errors the app noted ([note]).
 */
object CrashLog {

    private const val TAG = "CrashLog"
    private const val DIRECTORY = "crash-reports"
    private const val PREFERENCES = "lib.toolkit.crash_log"
    private const val KEY_LAST_EXIT = "last_exit"
    private const val MAX_NOTES = 30
    private const val MAX_REPORTS = 5
    private const val MAX_TRACE_CHARS = 20_000

    private val notes = ArrayDeque<String>()
    private var directory: File? = null
    private var appInfo = ""
    private var offered = false

    /** Starts recording crashes; call once, when the app starts. */
    fun install(context: Context) {
        val app = context.applicationContext
        directory = File(app.filesDir, DIRECTORY).apply { mkdirs() }
        appInfo = appInfo(app)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write("Crash in thread ${thread.name}", stackTrace(error)) }
            previous?.uncaughtException(thread, error)
        }
        runCatching { readExits(app) }.onFailure { Log.w(TAG, "can't read how the app ended", it) }
    }

    /** An error that didn't crash the app, for the next report. */
    @JvmStatic
    fun note(message: String) {
        Log.w(TAG, message)
        synchronized(notes) {
            notes.addLast("${time()} $message")
            while (notes.size > MAX_NOTES) notes.removeFirst()
        }
    }

    @JvmStatic
    fun note(error: Throwable) = note(stackTrace(error).lineSequence().take(8).joinToString("\n"))

    /** Reports not sent or discarded yet, the newest first. */
    fun reports(): List<File> =
        directory?.listFiles { file -> file.name.endsWith(".txt") }.orEmpty().sortedByDescending { it.name }

    /**
     * Asks whether to send the reports of earlier crashes, once per start of the app; sending
     * shares them as text.
     */
    fun offerReports(activity: Activity) {
        val reports = reports()
        if (reports.isEmpty() || offered) return
        offered = true
        AlertDialog.Builder(activity)
            .setTitle(R.string.crash_log_title)
            .setMessage(R.string.crash_log_message)
            .setPositiveButton(R.string.crash_log_send) { _, _ ->
                val text = reports.joinToString("\n\n") { it.readText() }
                val intent = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.crash_log_subject))
                    .putExtra(Intent.EXTRA_TEXT, text)
                runCatching { activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.crash_log_send))) }
                reports.forEach { it.delete() }
            }
            .setNegativeButton(R.string.crash_log_discard) { _, _ -> reports.forEach { it.delete() } }
            .setNeutralButton(R.string.crash_log_later, null)
            .show()
    }

    // Native crashes and ANRs since the last start (Android 11 and later)
    private fun readExits(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val lastSeen = preferences.getLong(KEY_LAST_EXIT, 0)
        val exits = context.getSystemService(ActivityManager::class.java)
            ?.getHistoricalProcessExitReasons(context.packageName, 0, 10).orEmpty()
            .filter { it.timestamp > lastSeen }
        for (exit in exits) {
            val kind = when (exit.reason) {
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "Crash in native code"
                ApplicationExitInfo.REASON_ANR -> "Not responding (ANR)"
                else -> continue
            }
            // ANRs come with the threads' stacks as text; native crashes with a binary tombstone
            val trace = if (exit.reason == ApplicationExitInfo.REASON_ANR) {
                runCatching { exit.traceInputStream?.bufferedReader()?.use { it.readText().take(MAX_TRACE_CHARS) } }.getOrNull()
            } else {
                null
            }
            write("$kind at ${format(exit.timestamp)} in ${exit.processName}", listOfNotNull(exit.description, trace).joinToString("\n"))
        }
        exits.maxOfOrNull { it.timestamp }?.let { preferences.edit().putLong(KEY_LAST_EXIT, it).apply() }
    }

    private fun write(title: String, details: String) {
        val dir = directory ?: return
        val recent = synchronized(notes) { notes.toList() }
        val text = buildString {
            appendLine(title)
            appendLine(appInfo)
            appendLine()
            appendLine(details)
            if (recent.isNotEmpty()) {
                appendLine()
                appendLine("Recent errors:")
                recent.forEach { appendLine(it) }
            }
        }
        File(dir, "crash-${System.currentTimeMillis()}.txt").writeText(text)
        // Only the latest ones
        reports().drop(MAX_REPORTS).forEach { it.delete() }
    }

    private fun appInfo(context: Context): String {
        val version = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()})"
        }.getOrDefault("?")
        return "${context.packageName} $version, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
            "${Build.MANUFACTURER} ${Build.MODEL}, ${Build.SUPPORTED_ABIS.firstOrNull()}"
    }

    private fun stackTrace(error: Throwable): String =
        StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()

    private fun time() = format(System.currentTimeMillis())

    private fun format(time: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(time))
}
