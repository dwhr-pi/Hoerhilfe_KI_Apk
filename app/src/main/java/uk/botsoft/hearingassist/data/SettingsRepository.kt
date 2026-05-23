package uk.botsoft.hearingassist.data

import android.content.Context
import org.json.JSONArray
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
            val maintenance = root.optJSONObject("maintenance") ?: JSONObject()
            val runtime = root.optJSONObject("runtime") ?: JSONObject()
            val profilesJson = root.optJSONArray("savedProfiles") ?: JSONArray()
            val savedProfiles = buildList {
                for (index in 0 until profilesJson.length()) {
                    val entry = profilesJson.optJSONObject(index) ?: continue
                    val entryProfile = entry.optJSONObject("profile") ?: JSONObject()
                    add(
                        SavedProfile(
                            id = entry.optString("id"),
                            name = entry.optString("name"),
                            source = entry.optString("source", "Vorgabe"),
                            createdAtIso = entry.optString("createdAtIso", "2026-05-06T00:00:00"),
                            profile = HearingProfile(
                                gain = entryProfile.optDouble("gain", 1.15).toFloat(),
                                lowBand = entryProfile.optDouble("lowBand", 1.0).toFloat(),
                                midBand = entryProfile.optDouble("midBand", 1.25).toFloat(),
                                highBand = entryProfile.optDouble("highBand", 1.35).toFloat(),
                                noiseReduction = entryProfile.optDouble("noiseReduction", 0.35).toFloat(),
                                voiceFocus = entryProfile.optDouble("voiceFocus", 0.55).toFloat(),
                                preset = AudioPreset.valueOf(entryProfile.optString("preset", AudioPreset.Conversation.name)),
                            ),
                        ),
                    )
                }
            }.ifEmpty { AppSettings().savedProfiles }

            val activeProfile = HearingProfile(
                gain = profile.optDouble("gain", savedProfiles.first().profile.gain.toDouble()).toFloat(),
                lowBand = profile.optDouble("lowBand", savedProfiles.first().profile.lowBand.toDouble()).toFloat(),
                midBand = profile.optDouble("midBand", savedProfiles.first().profile.midBand.toDouble()).toFloat(),
                highBand = profile.optDouble("highBand", savedProfiles.first().profile.highBand.toDouble()).toFloat(),
                noiseReduction = profile.optDouble("noiseReduction", savedProfiles.first().profile.noiseReduction.toDouble()).toFloat(),
                voiceFocus = profile.optDouble("voiceFocus", savedProfiles.first().profile.voiceFocus.toDouble()).toFloat(),
                preset = AudioPreset.valueOf(profile.optString("preset", savedProfiles.first().profile.preset.name)),
            )

            AppSettings(
                profile = activeProfile,
                testResult = HearingTestResult(
                    leftThresholdByFrequency = test.optJSONObject("left")?.keys()?.asSequence()?.associate { key ->
                        key.toInt() to test.getJSONObject("left").optInt(key, 0)
                    } ?: emptyMap(),
                    rightThresholdByFrequency = test.optJSONObject("right")?.keys()?.asSequence()?.associate { key ->
                        key.toInt() to test.getJSONObject("right").optInt(key, 0)
                    } ?: emptyMap(),
                ),
                gatewaySettings = GatewaySettings(
                    baseUrl = gateway.optString("baseUrl", ""),
                    apiKey = gateway.optString("apiKey", ""),
                    model = gateway.optString("model", "llama3.2:1b"),
                    provider = GatewayProvider.valueOf(gateway.optString("provider", GatewayProvider.OpenClaw.name)),
                    requestPath = gateway.optString("requestPath", ""),
                    authHeader = gateway.optString("authHeader", "Authorization"),
                    authPrefix = gateway.optString("authPrefix", "Bearer"),
                    customHeaders = gateway.optString("customHeaders", ""),
                ),
                maintenanceSettings = MaintenanceSettings(
                    productId = maintenance.optString("productId", "hoerhilfe-ki.android.alpha"),
                    errorReportEmail = maintenance.optString("errorReportEmail", "ai-chat-to-markdown@web.de"),
                    updateManifestUrl = maintenance.optString("updateManifestUrl", ""),
                ),
                audioRuntimeSettings = AudioRuntimeSettings(
                    hearingAidEnabled = runtime.optBoolean("hearingAidEnabled", false),
                    micMonitoringEnabled = runtime.optBoolean("micMonitoringEnabled", false),
                    liveProcessingEnabled = runtime.optBoolean("liveProcessingEnabled", false),
                    preferredRoute = AudioRoutePreference.valueOf(
                        runtime.optString("preferredRoute", AudioRoutePreference.Default.name),
                    ),
                    enhancedDspEnabled = runtime.optBoolean("enhancedDspEnabled", true),
                    lowLatencyModeEnabled = runtime.optBoolean("lowLatencyModeEnabled", true),
                    realTimeHeadsetDspEnabled = runtime.optBoolean("realTimeHeadsetDspEnabled", true),
                    bluetoothLeAudioOptimizationEnabled = runtime.optBoolean("bluetoothLeAudioOptimizationEnabled", false),
                    nativeLowLatencyPipelineEnabled = runtime.optBoolean("nativeLowLatencyPipelineEnabled", false),
                    advancedDspFiltersEnabled = runtime.optBoolean("advancedDspFiltersEnabled", true),
                    streamingCaptionModeEnabled = runtime.optBoolean("streamingCaptionModeEnabled", true),
                    bluetoothCompatibilityModeEnabled = runtime.optBoolean("bluetoothCompatibilityModeEnabled", true),
                    noiseSuppressionMode = NoiseSuppressionMode.valueOf(
                        runtime.optString("noiseSuppressionMode", NoiseSuppressionMode.Basic.name),
                    ),
                    mediaMixModeEnabled = runtime.optBoolean("mediaMixModeEnabled", true),
                    microphoneNormalizationEnabled = runtime.optBoolean("microphoneNormalizationEnabled", true),
                    liveTranscriptionEnabled = runtime.optBoolean("liveTranscriptionEnabled", true),
                    assistantEnabled = runtime.optBoolean("assistantEnabled", true),
                    themeMode = ThemeMode.valueOf(runtime.optString("themeMode", ThemeMode.System.name)),
                    appLanguage = AppLanguage.valueOf(runtime.optString("appLanguage", AppLanguage.System.name)),
                    textScale = AppTextScale.valueOf(runtime.optString("textScale", AppTextScale.Default.name)),
                    masterVolumePercent = runtime.optInt("masterVolumePercent", 70),
                ),
                savedProfiles = savedProfiles,
                selectedProfileId = root.optString("selectedProfileId", savedProfiles.first().id),
            )
        }.getOrElse { AppSettings() }
    }

    fun save(settings: AppSettings) {
        val profile = hearingProfileToJson(settings.profile)

        val test = JSONObject()
            .put("left", mapToJson(settings.testResult.leftThresholdByFrequency))
            .put("right", mapToJson(settings.testResult.rightThresholdByFrequency))

        val gateway = JSONObject()
            .put("baseUrl", settings.gatewaySettings.baseUrl)
            .put("apiKey", settings.gatewaySettings.apiKey)
            .put("model", settings.gatewaySettings.model)
            .put("provider", settings.gatewaySettings.provider.name)
            .put("requestPath", settings.gatewaySettings.requestPath)
            .put("authHeader", settings.gatewaySettings.authHeader)
            .put("authPrefix", settings.gatewaySettings.authPrefix)
            .put("customHeaders", settings.gatewaySettings.customHeaders)

        val maintenance = JSONObject()
            .put("productId", settings.maintenanceSettings.productId)
            .put("errorReportEmail", settings.maintenanceSettings.errorReportEmail)
            .put("updateManifestUrl", settings.maintenanceSettings.updateManifestUrl)

        val runtime = JSONObject()
            .put("hearingAidEnabled", settings.audioRuntimeSettings.hearingAidEnabled)
            .put("micMonitoringEnabled", settings.audioRuntimeSettings.micMonitoringEnabled)
            .put("liveProcessingEnabled", settings.audioRuntimeSettings.liveProcessingEnabled)
            .put("preferredRoute", settings.audioRuntimeSettings.preferredRoute.name)
            .put("enhancedDspEnabled", settings.audioRuntimeSettings.enhancedDspEnabled)
            .put("lowLatencyModeEnabled", settings.audioRuntimeSettings.lowLatencyModeEnabled)
            .put("realTimeHeadsetDspEnabled", settings.audioRuntimeSettings.realTimeHeadsetDspEnabled)
            .put("bluetoothLeAudioOptimizationEnabled", settings.audioRuntimeSettings.bluetoothLeAudioOptimizationEnabled)
            .put("nativeLowLatencyPipelineEnabled", settings.audioRuntimeSettings.nativeLowLatencyPipelineEnabled)
            .put("advancedDspFiltersEnabled", settings.audioRuntimeSettings.advancedDspFiltersEnabled)
            .put("streamingCaptionModeEnabled", settings.audioRuntimeSettings.streamingCaptionModeEnabled)
            .put("bluetoothCompatibilityModeEnabled", settings.audioRuntimeSettings.bluetoothCompatibilityModeEnabled)
            .put("noiseSuppressionMode", settings.audioRuntimeSettings.noiseSuppressionMode.name)
            .put("mediaMixModeEnabled", settings.audioRuntimeSettings.mediaMixModeEnabled)
            .put("microphoneNormalizationEnabled", settings.audioRuntimeSettings.microphoneNormalizationEnabled)
            .put("liveTranscriptionEnabled", settings.audioRuntimeSettings.liveTranscriptionEnabled)
            .put("assistantEnabled", settings.audioRuntimeSettings.assistantEnabled)
            .put("themeMode", settings.audioRuntimeSettings.themeMode.name)
            .put("appLanguage", settings.audioRuntimeSettings.appLanguage.name)
            .put("textScale", settings.audioRuntimeSettings.textScale.name)
            .put("masterVolumePercent", settings.audioRuntimeSettings.masterVolumePercent)

        val savedProfiles = JSONArray()
        settings.savedProfiles.forEach { savedProfile ->
            savedProfiles.put(
                JSONObject()
                    .put("id", savedProfile.id)
                    .put("name", savedProfile.name)
                    .put("source", savedProfile.source)
                    .put("createdAtIso", savedProfile.createdAtIso)
                    .put("profile", hearingProfileToJson(savedProfile.profile)),
            )
        }

        val root = JSONObject()
            .put("profile", profile)
            .put("test", test)
            .put("gateway", gateway)
            .put("maintenance", maintenance)
            .put("runtime", runtime)
            .put("savedProfiles", savedProfiles)
            .put("selectedProfileId", settings.selectedProfileId)

        prefs.edit().putString(KEY_JSON, root.toString()).apply()
    }

    private fun hearingProfileToJson(profile: HearingProfile): JSONObject {
        return JSONObject()
            .put("gain", profile.gain.toDouble())
            .put("lowBand", profile.lowBand.toDouble())
            .put("midBand", profile.midBand.toDouble())
            .put("highBand", profile.highBand.toDouble())
            .put("noiseReduction", profile.noiseReduction.toDouble())
            .put("voiceFocus", profile.voiceFocus.toDouble())
            .put("preset", profile.preset.name)
    }

    private fun mapToJson(values: Map<Int, Int>): JSONObject {
        val json = JSONObject()
        values.forEach { (key, value) ->
            json.put(key.toString(), value)
        }
        return json
    }

    private companion object {
        const val KEY_JSON = "app_settings_json"
    }
}
