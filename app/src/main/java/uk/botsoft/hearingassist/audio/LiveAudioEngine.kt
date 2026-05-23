package uk.botsoft.hearingassist.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import uk.botsoft.hearingassist.data.HearingProfile
import uk.botsoft.hearingassist.data.NoiseSuppressionMode

class LiveAudioEngine {
    @Volatile
    private var running = false

    @Volatile
    private var activeProfile: HearingProfile = HearingProfile()

    @Volatile
    private var enhancedDspEnabled: Boolean = true

    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var worker: Thread? = null

    fun start(
        profile: HearingProfile,
        enhancedProcessing: Boolean,
        lowLatencyMode: Boolean,
        realTimeHeadsetDspEnabled: Boolean,
        nativeLowLatencyPipelineEnabled: Boolean,
        advancedDspFiltersEnabled: Boolean,
        noiseSuppressionMode: NoiseSuppressionMode,
        mediaMixMode: Boolean,
        normalizationEnabled: Boolean,
        onLevelChanged: (Float) -> Unit,
        onStatusChanged: (String) -> Unit,
    ): Boolean {
        if (running) {
            activeProfile = profile
            enhancedDspEnabled = enhancedProcessing
            onStatusChanged("Live-Audio läuft bereits")
            return true
        }

        if (!realTimeHeadsetDspEnabled) {
            onStatusChanged("Echtzeit-Mikrofon-zu-Headset-DSP ist im Setup deaktiviert")
            return false
        }

        val sampleRate = 16_000
        val minRecordBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val minTrackBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        if (minRecordBuffer <= 0 || minTrackBuffer <= 0) {
            onStatusChanged("Audiopuffer konnten nicht initialisiert werden")
            return false
        }

        val frameSize = when {
            nativeLowLatencyPipelineEnabled -> 96
            lowLatencyMode -> 128
            else -> 384
        }
        val recordBufferSize = if (lowLatencyMode) {
            max(minRecordBuffer, frameSize * 2)
        } else {
            max(minRecordBuffer * 2, frameSize * 4)
        }
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufferSize,
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            onStatusChanged("Mikrofon konnte nicht initialisiert werden")
            return false
        }

        val trackBufferSize = if (lowLatencyMode) {
            max(minTrackBuffer, frameSize * 2)
        } else {
            max(minTrackBuffer * 2, frameSize * 4)
        }
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(
                        if (mediaMixMode) {
                            AudioAttributes.USAGE_MEDIA
                        } else {
                            AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                        },
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(trackBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(
                if (lowLatencyMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
                } else {
                    AudioTrack.PERFORMANCE_MODE_NONE
                },
            )
            .build()

        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioRecord.release()
            audioTrack.release()
            onStatusChanged("Audioausgabe konnte nicht initialisiert werden")
            return false
        }

        recorder = audioRecord
        player = audioTrack
        activeProfile = profile
        enhancedDspEnabled = enhancedProcessing
        running = true

        runCatching {
            audioRecord.startRecording()
            audioTrack.play()
        }.onFailure {
            stop()
            onStatusChanged("Live-Audio konnte nicht gestartet werden: ${it.message ?: "unbekannt"}")
            return false
        }

        worker = thread(start = true, isDaemon = true, name = "live-audio-engine") {
            val inputBuffer = ShortArray(frameSize)
            val outputBuffer = ShortArray(frameSize)
            var previousInput = 0f
            var previousHigh = 0f
            var compressorGain = 1f

            while (running) {
                val read = audioRecord.read(inputBuffer, 0, inputBuffer.size)
                if (read <= 0) continue

                val profileSnapshot = activeProfile
                val enhanced = enhancedDspEnabled
                val voiceMix = profileSnapshot.voiceFocus.coerceIn(0f, 1f)
                val noiseStrength = when (noiseSuppressionMode) {
                    NoiseSuppressionMode.Off -> 0f
                    NoiseSuppressionMode.Basic -> 1f
                    NoiseSuppressionMode.RNNoise -> 1.18f
                    NoiseSuppressionMode.DeepFilterNet -> 1.28f
                    NoiseSuppressionMode.Beamforming -> 1.08f
                }
                val gateThreshold = if (enhanced) {
                    (0.008f + (profileSnapshot.noiseReduction * 0.03f * noiseStrength)).coerceIn(0.004f, 0.048f)
                } else {
                    0.004f
                }
                val overallGain = if (enhanced) {
                    (profileSnapshot.gain * (0.95f + profileSnapshot.midBand * 0.12f)).coerceIn(0.85f, 2.25f)
                } else {
                    profileSnapshot.gain.coerceIn(0.85f, 1.7f)
                }
                val highBoost = if (enhanced) {
                    ((profileSnapshot.highBand - 1f) * 0.18f).coerceIn(0f, 0.18f)
                } else {
                    ((profileSnapshot.highBand - 1f) * 0.08f).coerceIn(0f, 0.08f)
                }
                val gateAttenuation = if (enhanced && noiseSuppressionMode != NoiseSuppressionMode.Off) {
                    (0.75f - profileSnapshot.noiseReduction * 0.18f * noiseStrength).coerceIn(0.48f, 0.88f)
                } else {
                    0.96f
                }
                val saturation = if (enhanced) 0.94f else 0.98f
                val compressorThreshold = if (advancedDspFiltersEnabled && enhanced) 0.18f else 0.28f
                val compressorRatio = if (advancedDspFiltersEnabled && enhanced) 2.4f else 1.25f
                val attack = if (enhanced) 0.2f else 0.12f
                val release = if (enhanced) 0.035f else 0.02f
                val normalizationTarget = if (enhanced) 0.16f else 0.19f
                var peak = 0f

                for (i in 0 until read) {
                    val dry = inputBuffer[i] / Short.MAX_VALUE.toFloat()
                    val high = (0.965f * previousHigh) + dry - previousInput
                    previousInput = dry
                    previousHigh = high

                    val voiced = dry + high * if (enhanced) (voiceMix * 0.38f + highBoost) else (voiceMix * 0.18f + highBoost)
                    val gated = if (abs(voiced) < gateThreshold) {
                        voiced * gateAttenuation
                    } else {
                        voiced
                    }
                    val feedbackProtected = if (advancedDspFiltersEnabled && abs(high) > 0.32f && abs(dry) > 0.28f) {
                        gated * 0.82f
                    } else {
                        gated
                    }
                    val compressed = applyCompressor(
                        sample = feedbackProtected * overallGain,
                        threshold = compressorThreshold,
                        ratio = compressorRatio,
                        attack = attack,
                        release = release,
                        currentGain = compressorGain,
                        normalizationEnabled = normalizationEnabled,
                        targetAmplitude = normalizationTarget,
                    )
                    compressorGain = compressed.second
                    val boosted = softClip(compressed.first, saturation)

                    peak = max(peak, abs(boosted))
                    outputBuffer[i] = (boosted * Short.MAX_VALUE).roundToInt().toShort()
                }

                onLevelChanged(peak.coerceIn(0f, 1f))
                audioTrack.write(outputBuffer, 0, read)
            }
        }

        onStatusChanged(
            if (nativeLowLatencyPipelineEnabled) {
                "Live-Audio aktiv (Native/Oboe-Modus vorbereitet)"
            } else {
                "Live-Audio aktiv"
            },
        )
        return true
    }

    fun updateProfile(profile: HearingProfile) {
        activeProfile = profile
    }

    fun updateProcessingMode(enabled: Boolean) {
        enhancedDspEnabled = enabled
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        runCatching { player?.stop() }
        player?.release()
        player = null
    }

    private fun softClip(value: Float, saturation: Float): Float {
        return (value / (1f + abs(value) / saturation)).coerceIn(-1f, 1f)
    }

    private fun applyCompressor(
        sample: Float,
        threshold: Float,
        ratio: Float,
        attack: Float,
        release: Float,
        currentGain: Float,
        normalizationEnabled: Boolean,
        targetAmplitude: Float,
    ): Pair<Float, Float> {
        val amplitude = abs(sample).coerceAtLeast(0.0001f)
        val compressionGain = if (amplitude <= threshold) {
            1f
        } else {
            val over = amplitude / threshold
            over.pow(-(ratio - 1f) / ratio).coerceIn(0.45f, 1f)
        }
        val normalizationGain = if (normalizationEnabled) {
            (targetAmplitude / amplitude).coerceIn(0.82f, 1.22f)
        } else {
            1f
        }
        val targetGain = (compressionGain * normalizationGain).coerceIn(0.42f, 1.22f)
        val smoothing = if (targetGain < currentGain) attack else release
        val smoothedGain = currentGain + ((targetGain - currentGain) * smoothing)
        return (sample * smoothedGain) to smoothedGain
    }
}
