package uk.botsoft.hearingassist.audio

import uk.botsoft.hearingassist.data.HearingProfile

data class AudioEngineMetrics(
    val sampleRateHz: Int = 0,
    val recordBufferBytes: Int = 0,
    val playbackBufferBytes: Int = 0,
    val frameSizeSamples: Int = 0,
    val estimatedLatencyMs: Int? = null,
    val lastError: String? = null,
)

interface AudioEngine {
    val isRunning: Boolean
    val metrics: AudioEngineMetrics

    fun start(
        profile: HearingProfile,
        onLevelChanged: (Float) -> Unit,
        onStatusChanged: (String) -> Unit,
    ): Boolean

    fun updateProfile(profile: HearingProfile)

    fun stop()
}

class MockAudioEngine : AudioEngine {
    override var isRunning: Boolean = false
        private set
    override var metrics: AudioEngineMetrics = AudioEngineMetrics(lastError = "Mock engine")
        private set

    private var activeProfile: HearingProfile = HearingProfile()

    override fun start(
        profile: HearingProfile,
        onLevelChanged: (Float) -> Unit,
        onStatusChanged: (String) -> Unit,
    ): Boolean {
        activeProfile = profile
        isRunning = true
        metrics = AudioEngineMetrics(
            sampleRateHz = 16_000,
            frameSizeSamples = 0,
            lastError = null,
        )
        onLevelChanged(0f)
        onStatusChanged("Mock-AudioEngine aktiv. Native Low-Latency-Engine ist vorbereitet, aber noch nicht eingebunden.")
        return true
    }

    override fun updateProfile(profile: HearingProfile) {
        activeProfile = profile
    }

    override fun stop() {
        isRunning = false
    }
}
