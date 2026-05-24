package uk.botsoft.hearingassist.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import uk.botsoft.hearingassist.data.EarSide

class HearingTestPlayer {
    fun playTone(
        frequencyHz: Int,
        level: Float = 0.4f,
        durationMs: Int = 900,
        earSide: EarSide? = null,
    ) {
        val sampleRate = 22_050
        val sampleCount = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(sampleCount * 2)

        for (i in 0 until sampleCount) {
            val fade = when {
                i < sampleRate / 30 -> i.toFloat() / (sampleRate / 30).toFloat()
                i > sampleCount - sampleRate / 30 -> (sampleCount - i).toFloat() / (sampleRate / 30).toFloat()
                else -> 1f
            }.coerceIn(0f, 1f)

            val sample = (sin(2.0 * PI * i * frequencyHz / sampleRate) * Short.MAX_VALUE * level * fade).toInt().toShort()
            val left = if (earSide == null || earSide == EarSide.Left) sample else 0
            val right = if (earSide == null || earSide == EarSide.Right) sample else 0
            val base = i * 2
            pcm[base] = left
            pcm[base + 1] = right
        }

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
            pcm.size * 2,
            AudioTrack.MODE_STATIC,
            0,
        )

        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(sampleCount - 1)
        audioTrack.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.release()
                }

                override fun onPeriodicNotification(track: AudioTrack?) = Unit
            },
        )
    }
}
