package uk.botsoft.hearingassist.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.abs

class MicrophoneMonitor {
    @Volatile
    private var running = false

    private var recorder: AudioRecord? = null
    private var worker: Thread? = null

    fun start(onLevelChanged: (Float) -> Unit): Boolean {
        if (running) return true

        val sampleRate = 16_000
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        if (minBufferSize <= 0) return false

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }

        recorder = audioRecord
        running = true
        audioRecord.startRecording()

        worker = thread(start = true, isDaemon = true, name = "mic-monitor") {
            val buffer = ShortArray(1024)
            while (running) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var peak = 0
                    for (i in 0 until read) {
                        peak = maxOf(peak, abs(buffer[i].toInt()))
                    }
                    onLevelChanged((peak / Short.MAX_VALUE.toFloat()).coerceIn(0f, 1f))
                }
            }
        }

        return true
    }

    fun stop() {
        running = false
        worker?.join(300)
        worker = null
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
    }
}
