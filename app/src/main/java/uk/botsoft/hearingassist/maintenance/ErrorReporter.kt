package uk.botsoft.hearingassist.maintenance

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ErrorReporter(private val context: Context) {
    private val prefs = context.getSharedPreferences("hearing_assist_error_log", Context.MODE_PRIVATE)

    fun installCrashHandler(productId: String) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            append(
                productId = productId,
                title = "Uncaught crash on ${thread.name}",
                detail = throwable.stackTraceText(),
            )
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun append(productId: String, title: String, detail: String) {
        val entry = buildString {
            appendLine("[$productId]")
            appendLine("time=${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
            appendLine("locale=${Locale.getDefault()}")
            appendLine("title=$title")
            appendLine(detail.trim())
            appendLine()
        }
        val next = (entry + prefs.getString(KEY_LOG, "").orEmpty()).take(MAX_LOG_LENGTH)
        prefs.edit().putString(KEY_LOG, next).apply()
    }

    fun sendByEmail(productId: String, recipient: String): Result<String> {
        val logText = prefs.getString(KEY_LOG, "").orEmpty().ifBlank {
            "Noch kein technischer Fehler im lokalen Protokoll vorhanden."
        }
        val subject = "[$productId] Fehlerprotokoll Hoerhilfe KI"
        val body = buildString {
            appendLine("Produkt: $productId")
            appendLine("App: Hoerhilfe KI")
            appendLine("Hinweis: Der Versand erfolgt bewusst ueber den Mail-Client des Geraets.")
            appendLine()
            appendLine(logText)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Fehlerprotokoll senden").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Mail-App fuer Fehlerprotokoll geoeffnet"
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_LOG).apply()
    }

    private fun Throwable.stackTraceText(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private companion object {
        const val KEY_LOG = "error_log"
        const val MAX_LOG_LENGTH = 48_000
    }
}
