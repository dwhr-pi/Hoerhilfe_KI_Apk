package uk.botsoft.hearingassist.audio.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspProfileTest {
    @Test
    fun presetsContainExpectedEverydayModes() {
        val labels = DspPresets.all.map { it.preset }

        assertTrue(DspPreset.Everyday in labels)
        assertTrue(DspPreset.Speech in labels)
        assertTrue(DspPreset.Tv in labels)
        assertTrue(DspPreset.Restaurant in labels)
        assertTrue(DspPreset.ExperimentalTinnitusMasking in labels)
    }

    @Test
    fun normalizationKeepsValuesInSafeTechnicalBounds() {
        val profile = DspProfile(
            gain = 99f,
            balance = -3f,
            equalizer = EqualizerBands(bass = 9f, speech = -2f, treble = 4f),
            parametricEq = listOf(ParametricEqBand(22_000f, 30f, 99f)),
            limiterCeiling = 1.5f,
        ).normalized()

        assertEquals(2.5f, profile.gain, 0.001f)
        assertEquals(-1f, profile.balance, 0.001f)
        assertEquals(2.0f, profile.equalizer.bass, 0.001f)
        assertEquals(0.5f, profile.equalizer.speech, 0.001f)
        assertEquals(12_000f, profile.parametricEq.first().frequencyHz, 0.001f)
        assertEquals(12f, profile.parametricEq.first().gainDb, 0.001f)
        assertEquals(8f, profile.parametricEq.first().q, 0.001f)
        assertEquals(0.98f, profile.limiterCeiling, 0.001f)
    }
}
