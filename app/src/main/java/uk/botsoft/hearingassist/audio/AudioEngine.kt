package uk.botsoft.hearingassist.audio

import uk.botsoft.hearingassist.data.HearingProfile

interface AudioEngine {
    val isRunning: Boolean

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

    private var activeProfile: HearingProfile = HearingProfile()

    override fun start(
        profile: HearingProfile,
        onLevelChanged: (Float) -> Unit,
        onStatusChanged: (String) -> Unit,
    ): Boolean {
        activeProfile = profile
        isRunning = true
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
