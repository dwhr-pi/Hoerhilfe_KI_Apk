package uk.botsoft.hearingassist.maintenance

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateCheckResult(
    val available: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val message: String,
)

class UpdateChecker(private val context: Context) {
    fun currentVersionCode(): Int {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }

    fun currentVersionName(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    }

    fun check(manifestUrl: String, productId: String): Result<UpdateCheckResult> = runCatching {
        require(manifestUrl.isNotBlank()) { "Keine Update-URL konfiguriert" }
        val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        val raw = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(raw)
        val manifestProduct = json.optString("productId", productId)
        require(manifestProduct == productId) {
            "Update-Datei gehoert zu '$manifestProduct', erwartet '$productId'"
        }
        val latestCode = json.optInt("versionCode", currentVersionCode())
        val latestName = json.optString("versionName", currentVersionName())
        val downloadUrl = json.optString("downloadUrl", "")
        val message = json.optString("message", "")
        UpdateCheckResult(
            available = latestCode > currentVersionCode(),
            latestVersionCode = latestCode,
            latestVersionName = latestName,
            downloadUrl = downloadUrl,
            message = message,
        )
    }
}
