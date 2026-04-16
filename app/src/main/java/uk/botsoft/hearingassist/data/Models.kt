package uk.botsoft.hearingassist.data

enum class AudioPreset(val label: String) {
    Conversation("Gespräch"),
    Outdoor("Draußen"),
    Tv("TV"),
    Music("Musik"),
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
    val thresholdByFrequency: Map<Int, Int> = emptyMap(),
) {
    fun recommendedProfile(): HearingProfile {
        val lowLoss = normalizeLoss(listOf(250, 500))
        val midLoss = normalizeLoss(listOf(1000, 2000))
        val highLoss = normalizeLoss(listOf(4000, 6000))

        return HearingProfile(
            gain = (1.05f + (lowLoss + midLoss + highLoss) / 9f).coerceIn(1.0f, 2.0f),
            lowBand = (1.0f + lowLoss / 4f).coerceIn(0.9f, 1.8f),
            midBand = (1.15f + midLoss / 3.3f).coerceIn(1.0f, 2.1f),
            highBand = (1.2f + highLoss / 3f).coerceIn(1.0f, 2.2f),
            noiseReduction = (0.25f + highLoss / 10f).coerceIn(0.2f, 0.75f),
            voiceFocus = (0.4f + midLoss / 8f).coerceIn(0.35f, 0.9f),
        )
    }

    private fun normalizeLoss(frequencies: List<Int>): Float {
        val relevant = frequencies.mapNotNull { thresholdByFrequency[it] }
        if (relevant.isEmpty()) return 0f
        return relevant.average().toFloat() / 60f
    }
}

data class GatewaySettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
)

data class AppSettings(
    val profile: HearingProfile = HearingProfile(),
    val testResult: HearingTestResult = HearingTestResult(),
    val gatewaySettings: GatewaySettings = GatewaySettings(),
)
