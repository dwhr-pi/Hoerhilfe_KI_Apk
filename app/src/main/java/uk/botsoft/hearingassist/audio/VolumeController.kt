package uk.botsoft.hearingassist.audio

import android.content.Context
import android.media.AudioManager

class VolumeController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun currentVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max) * 100f).toInt().coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((clamped / 100f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}
