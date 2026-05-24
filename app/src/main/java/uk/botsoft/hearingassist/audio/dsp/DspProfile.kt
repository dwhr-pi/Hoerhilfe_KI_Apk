package uk.botsoft.hearingassist.audio.dsp

enum class DspPreset(val label: String) {
    Everyday("Alltag"),
    Speech("Sprache"),
    Tv("TV"),
    Restaurant("Restaurant"),
    Outdoor("Draußen"),
    Music("Musik"),
    Night("Nachtmodus"),
    ExperimentalTinnitusMasking("Tinnitus-Maskierung experimentell"),
}

data class EqualizerBands(
    val bass: Float = 1.0f,
    val speech: Float = 1.0f,
    val treble: Float = 1.0f,
) {
    fun normalized(): EqualizerBands = copy(
        bass = bass.coerceIn(0.5f, 2.0f),
        speech = speech.coerceIn(0.5f, 2.0f),
        treble = treble.coerceIn(0.5f, 2.0f),
    )
}

data class CompressorSettings(
    val enabled: Boolean = true,
    val thresholdDb: Float = -18f,
    val ratio: Float = 2.2f,
    val attackMs: Float = 12f,
    val releaseMs: Float = 120f,
) {
    fun normalized(): CompressorSettings = copy(
        thresholdDb = thresholdDb.coerceIn(-45f, -3f),
        ratio = ratio.coerceIn(1f, 8f),
        attackMs = attackMs.coerceIn(1f, 80f),
        releaseMs = releaseMs.coerceIn(40f, 800f),
    )
}

data class FeedbackProtectionSettings(
    val enabled: Boolean = true,
    val notchStrength: Float = 0.35f,
    val highFrequencyGuard: Float = 0.25f,
) {
    fun normalized(): FeedbackProtectionSettings = copy(
        notchStrength = notchStrength.coerceIn(0f, 1f),
        highFrequencyGuard = highFrequencyGuard.coerceIn(0f, 1f),
    )
}

data class DspProfile(
    val preset: DspPreset = DspPreset.Everyday,
    val gain: Float = 1.0f,
    val balance: Float = 0f,
    val equalizer: EqualizerBands = EqualizerBands(),
    val compressor: CompressorSettings = CompressorSettings(),
    val feedbackProtection: FeedbackProtectionSettings = FeedbackProtectionSettings(),
    val limiterCeiling: Float = 0.92f,
) {
    fun normalized(): DspProfile = copy(
        gain = gain.coerceIn(0.1f, 2.5f),
        balance = balance.coerceIn(-1f, 1f),
        equalizer = equalizer.normalized(),
        compressor = compressor.normalized(),
        feedbackProtection = feedbackProtection.normalized(),
        limiterCeiling = limiterCeiling.coerceIn(0.4f, 0.98f),
    )
}

object DspPresets {
    val all: List<DspProfile> = listOf(
        DspProfile(DspPreset.Everyday, gain = 1.05f, equalizer = EqualizerBands(1.0f, 1.12f, 1.08f)),
        DspProfile(DspPreset.Speech, gain = 1.12f, equalizer = EqualizerBands(0.92f, 1.32f, 1.16f)),
        DspProfile(DspPreset.Tv, gain = 1.08f, equalizer = EqualizerBands(0.95f, 1.26f, 1.12f)),
        DspProfile(DspPreset.Restaurant, gain = 1.06f, equalizer = EqualizerBands(0.88f, 1.35f, 1.05f)),
        DspProfile(DspPreset.Outdoor, gain = 1.04f, equalizer = EqualizerBands(0.9f, 1.22f, 1.1f)),
        DspProfile(DspPreset.Music, gain = 1.0f, equalizer = EqualizerBands(1.18f, 1.02f, 1.18f), compressor = CompressorSettings(ratio = 1.4f)),
        DspProfile(DspPreset.Night, gain = 0.86f, equalizer = EqualizerBands(0.92f, 1.05f, 0.9f), limiterCeiling = 0.72f),
        DspProfile(DspPreset.ExperimentalTinnitusMasking, gain = 0.9f, equalizer = EqualizerBands(0.85f, 1.0f, 1.22f)),
    ).map { it.normalized() }

    fun byPreset(preset: DspPreset): DspProfile = all.first { it.preset == preset }
}
