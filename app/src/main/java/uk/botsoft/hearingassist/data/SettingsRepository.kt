package uk.botsoft.hearingassist.data

import android.content.Context
import org.json.JSONObject

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("hearing_assist", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val raw = prefs.getString(KEY_JSON, null) ?: return AppSettings()
        return runCatching {
            val root = JSONObject(raw)
            val profile = root.optJSONObject("profile") ?: JSONObject()
            val test = root.optJSONObject("test") ?: JSONObject()
            val gateway = root.optJSONObject("gateway") ?: JSONObject()

            AppSettings(
                profile = HearingProfile(
                    gain = profile.optDouble("gain", 1.15).toFloat(),
                    lowBand = profile.optDouble("lowBand", 1.0).toFloat(),
                    midBand = profile.optDouble("midBand", 1.25).toFloat(),
                    highBand = profile.optDouble("highBand", 1.35).toFloat(),
                    noiseReduction = profile.optDouble("noiseReduction", 0.35).toFloat(),
                    voiceFocus = profile.optDouble("voiceFocus", 0.55).toFloat(),
                    preset = AudioPreset.valueOf(profile.optString("preset", AudioPreset.Conversation.name)),
                ),
                testResult = HearingTestResult(
                    thresholdByFrequency = test.keys().asSequence().associate { key ->
                        key.toInt() to test.optInt(key, 0)
                    },
                ),
                gatewaySettings = GatewaySettings(
                    baseUrl = gateway.optString("baseUrl", ""),
                    apiKey = gateway.optString("apiKey", ""),
                    model = gateway.optString("model", "gpt-4o-mini"),
                ),
            )
        }.getOrElse { AppSettings() }
    }

    fun save(settings: AppSettings) {
        val profile = JSONObject()
            .put("gain", settings.profile.gain.toDouble())
            .put("lowBand", settings.profile.lowBand.toDouble())
            .put("midBand", settings.profile.midBand.toDouble())
            .put("highBand", settings.profile.highBand.toDouble())
            .put("noiseReduction", settings.profile.noiseReduction.toDouble())
            .put("voiceFocus", settings.profile.voiceFocus.toDouble())
            .put("preset", settings.profile.preset.name)

        val test = JSONObject()
        settings.testResult.thresholdByFrequency.forEach { (key, value) ->
            test.put(key.toString(), value)
        }

        val gateway = JSONObject()
            .put("baseUrl", settings.gatewaySettings.baseUrl)
            .put("apiKey", settings.gatewaySettings.apiKey)
            .put("model", settings.gatewaySettings.model)

        val root = JSONObject()
            .put("profile", profile)
            .put("test", test)
            .put("gateway", gateway)

        prefs.edit().putString(KEY_JSON, root.toString()).apply()
    }

    private companion object {
        const val KEY_JSON = "app_settings_json"
    }
}
