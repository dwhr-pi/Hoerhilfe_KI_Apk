package uk.botsoft.hearingassist.data

enum class AudioPreset(val label: String) {
    Conversation("Gespräch"),
    Outdoor("Draußen"),
    Tv("TV"),
    Music("Musik"),
}

enum class EarSide {
    Left,
    Right,
}

data class HearingProfile(
    val gain: Float = 1.15f,
    val lowBand: Float = 1.0f,
    val midBand: Float = 1.25f,
    val highBand: Float = 1.35f,
    val noiseReduction: Float = 0.35f,
    val voiceFocus: Float = 0.55f,
    val preset: AudioPreset = AudioPreset.Conversation,
)

data class HearingTestResult(
    val leftThresholdByFrequency: Map<Int, Int> = emptyMap(),
    val rightThresholdByFrequency: Map<Int, Int> = emptyMap(),
) {
    fun overallThresholdByFrequency(): Map<Int, Int> {
        val keys = (leftThresholdByFrequency.keys + rightThresholdByFrequency.keys).toSortedSet()
        return keys.associateWith { frequency ->
            val values = listOfNotNull(
                leftThresholdByFrequency[frequency],
                rightThresholdByFrequency[frequency],
            )
            if (values.isEmpty()) 60 else values.average().toInt()
        }
    }

    fun recommendedProfile(): HearingProfile {
        val overall = overallThresholdByFrequency()
        val lowLoss = normalizeLoss(overall, listOf(125, 250, 500, 750))
        val midLoss = normalizeLoss(overall, listOf(1000, 1500, 2000, 3000))
        val highLoss = normalizeLoss(overall, listOf(4000, 6000, 8000))

        return HearingProfile(
            gain = (1.02f + (lowLoss + midLoss + highLoss) / 10f).coerceIn(1.0f, 1.9f),
            lowBand = (0.98f + lowLoss / 5f).coerceIn(0.9f, 1.55f),
            midBand = (1.08f + midLoss / 4.2f).coerceIn(1.0f, 1.7f),
            highBand = (1.08f + highLoss / 4f).coerceIn(1.0f, 1.8f),
            noiseReduction = (0.12f + highLoss / 14f).coerceIn(0.08f, 0.45f),
            voiceFocus = (0.18f + midLoss / 10f).coerceIn(0.15f, 0.5f),
        )
    }

    fun sortedFrequencies(): List<Int> {
        return (leftThresholdByFrequency.keys + rightThresholdByFrequency.keys).distinct().sorted()
    }

    fun forEar(earSide: EarSide): Map<Int, Int> = when (earSide) {
        EarSide.Left -> leftThresholdByFrequency
        EarSide.Right -> rightThresholdByFrequency
    }

    private fun normalizeLoss(values: Map<Int, Int>, frequencies: List<Int>): Float {
        val relevant = frequencies.mapNotNull { values[it] }
        if (relevant.isEmpty()) return 0f
        return relevant.average().toFloat() / 60f
    }
}

data class SavedProfile(
    val id: String,
    val name: String,
    val profile: HearingProfile,
    val source: String,
    val createdAtIso: String,
)

data class GatewaySettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "llama3.2:1b",
    val provider: GatewayProvider = GatewayProvider.OpenClaw,
    val requestPath: String = "",
    val authHeader: String = "Authorization",
    val authPrefix: String = "Bearer",
    val customHeaders: String = "",
)

data class AppSettings(
    val profile: HearingProfile = HearingProfile(),
    val testResult: HearingTestResult = HearingTestResult(),
    val gatewaySettings: GatewaySettings = GatewaySettings(),
    val maintenanceSettings: MaintenanceSettings = MaintenanceSettings(),
    val audioRuntimeSettings: AudioRuntimeSettings = AudioRuntimeSettings(),
    val savedProfiles: List<SavedProfile> = defaultProfiles(),
    val selectedProfileId: String = defaultProfiles().first().id,
)

data class MaintenanceSettings(
    val productId: String = "hoerhilfe-ki.android.alpha",
    val errorReportEmail: String = "ai-chat-to-markdown@web.de",
    val updateManifestUrl: String = "",
)

enum class GatewayProvider(val label: String, val defaultPath: String) {
    OpenClaw("OpenClaw Proxy", "/openclaw/v1/chat/completions"),
    Ollama("Ollama Proxy", "/ollama/api/chat"),
    OpenAiCompatible("OpenAI-kompatibel", "/v1/chat/completions"),
}

data class AudioRuntimeSettings(
    val hearingAidEnabled: Boolean = false,
    val micMonitoringEnabled: Boolean = false,
    val liveProcessingEnabled: Boolean = false,
    val preferredRoute: AudioRoutePreference = AudioRoutePreference.Default,
    val enhancedDspEnabled: Boolean = true,
    val lowLatencyModeEnabled: Boolean = true,
    val realTimeHeadsetDspEnabled: Boolean = true,
    val bluetoothLeAudioOptimizationEnabled: Boolean = false,
    val nativeLowLatencyPipelineEnabled: Boolean = false,
    val advancedDspFiltersEnabled: Boolean = true,
    val streamingCaptionModeEnabled: Boolean = true,
    val bluetoothCompatibilityModeEnabled: Boolean = true,
    val noiseSuppressionMode: NoiseSuppressionMode = NoiseSuppressionMode.Basic,
    val mediaMixModeEnabled: Boolean = true,
    val microphoneNormalizationEnabled: Boolean = true,
    val liveTranscriptionEnabled: Boolean = true,
    val assistantEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val appLanguage: AppLanguage = AppLanguage.System,
    val textScale: AppTextScale = AppTextScale.Default,
    val masterVolumePercent: Int = 70,
)

enum class AudioRoutePreference(val label: String) {
    Default("Automatisch"),
    Speaker("Lautsprecher"),
    Bluetooth("Bluetooth"),
}

enum class NoiseSuppressionMode {
    Off,
    Basic,
    RNNoise,
    DeepFilterNet,
    Beamforming,
}

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class AppLanguage {
    System,
    German,
    English,
}

enum class AppTextScale {
    Small,
    Default,
    Large,
}

private fun defaultProfiles(): List<SavedProfile> {
    return listOf(
        SavedProfile(
            id = "default-conversation",
            name = "Standard Gespräch",
            profile = HearingProfile(1.08f, 1.0f, 1.16f, 1.12f, 0.18f, 0.28f, AudioPreset.Conversation),
            source = "Vorgabe",
            createdAtIso = "2026-05-06T00:00:00",
        ),
        SavedProfile(
            id = "default-outdoor",
            name = "Standard Draußen",
            profile = HearingProfile(1.1f, 0.98f, 1.2f, 1.14f, 0.28f, 0.34f, AudioPreset.Outdoor),
            source = "Vorgabe",
            createdAtIso = "2026-05-06T00:00:00",
        ),
        SavedProfile(
            id = "default-tv",
            name = "Standard TV",
            profile = HearingProfile(1.06f, 1.0f, 1.22f, 1.18f, 0.14f, 0.24f, AudioPreset.Tv),
            source = "Vorgabe",
            createdAtIso = "2026-05-06T00:00:00",
        ),
        SavedProfile(
            id = "default-music",
            name = "Standard Musik",
            profile = HearingProfile(1.0f, 1.2f, 1.05f, 1.2f, 0.1f, 0.2f, AudioPreset.Music),
            source = "Vorgabe",
            createdAtIso = "2026-05-06T00:00:00",
        ),
    )
}
