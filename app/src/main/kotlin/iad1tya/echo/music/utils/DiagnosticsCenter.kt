package iad1tya.echo.music.utils

import android.content.Context
import android.os.Build
import iad1tya.echo.music.BuildConfig
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsCenter {
    private const val MAX_LINES = 400
    private val lock = Any()
    private val lines = ArrayDeque<String>(MAX_LINES)

    fun addLine(priority: Int, tag: String?, message: String) {
        val level = when (priority) {
            android.util.Log.VERBOSE -> "V"
            android.util.Log.DEBUG -> "D"
            android.util.Log.INFO -> "I"
            android.util.Log.WARN -> "W"
            android.util.Log.ERROR -> "E"
            else -> "?"
        }
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$timestamp $level/${tag ?: "Echo"}: $message"
        synchronized(lock) {
            if (lines.size >= MAX_LINES) {
                lines.removeFirst()
            }
            lines.addLast(line)
        }
    }

    fun snapshot(): String = synchronized(lock) {
        lines.joinToString(separator = "\n")
    }

    fun buildReport(context: Context, extraSections: List<Pair<String, String>> = emptyList()): String {
        val header = buildString {
            appendLine("Echo Music Diagnostics Report")
            appendLine("Generated: ${Date()}")
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}")
            appendLine("Architecture: ${BuildConfig.ARCHITECTURE}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Package: ${context.packageName}")
        }

        val sections = buildString {
            extraSections.forEach { (title, content) ->
                appendLine()
                appendLine("[$title]")
                appendLine(content.ifBlank { "n/a" })
            }
            appendLine()
            appendLine("[Recent Logs]")
            appendLine(snapshot().ifBlank { "No captured logs yet." })
        }

        return header + sections
    }
}

class DiagnosticsLogTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val throwableInfo = t?.let { "\n${it.stackTraceToString()}" } ?: ""
        DiagnosticsCenter.addLine(priority, tag, message + throwableInfo)
    }
}
