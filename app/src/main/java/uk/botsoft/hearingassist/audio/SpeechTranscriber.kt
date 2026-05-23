package uk.botsoft.hearingassist.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechTranscriber(private val context: Context) {
    @Volatile
    private var running = false

    private var recognizer: SpeechRecognizer? = null
    private var lastCommittedText: String = ""
    private var restartRequested = false

    fun start(
        languageTag: String = "de-DE",
        streamingMode: Boolean = true,
        onStatusChanged: (String) -> Unit,
        onTranscriptChanged: (String) -> Unit,
    ): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatusChanged("Spracherkennung auf diesem Gerät nicht verfügbar")
            return false
        }

        if (running) {
            onStatusChanged("Live-Transkription läuft bereits")
            return true
        }

        running = true
        restartRequested = false
        lastCommittedText = ""
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStatusChanged("Live-Transkription hört zu")
                }

                override fun onBeginningOfSpeech() {
                    onStatusChanged("Sprache erkannt")
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    if (running) {
                        onStatusChanged("Verarbeite Spracheingabe")
                    }
                }

                override fun onError(error: Int) {
                    if (!running) return
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restartListening(languageTag)
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            onStatusChanged("Spracherkennung startet neu")
                            restartListening(languageTag)
                        }
                        else -> {
                            onStatusChanged("Transkriptionsfehler: $error")
                            restartListening(languageTag)
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results.bestText()
                    if (text.isNotBlank()) {
                        lastCommittedText = appendDistinct(lastCommittedText, text)
                        onTranscriptChanged(lastCommittedText)
                    }
                    if (running) restartListening(languageTag)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults.bestText()
                    if (text.isNotBlank()) {
                        if (streamingMode) {
                            onTranscriptChanged(
                                if (lastCommittedText.isBlank()) text else "$lastCommittedText\n$text",
                            )
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )

        restartListening(languageTag)
        return true
    }

    fun stop() {
        running = false
        restartRequested = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun restartListening(languageTag: String) {
        if (!running || restartRequested) return
        restartRequested = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
        }
        runCatching {
            recognizer?.cancel()
            recognizer?.startListening(intent)
        }.onFailure {
            restartRequested = false
            throw it
        }
        restartRequested = false
    }

    private fun Bundle?.bestText(): String {
        val results = this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        return results?.firstOrNull().orEmpty()
    }

    private fun appendDistinct(existing: String, next: String): String {
        val normalizedExisting = existing.trim()
        val normalizedNext = next.trim()
        if (normalizedExisting.isBlank()) return normalizedNext
        if (normalizedExisting.endsWith(normalizedNext, ignoreCase = true)) return normalizedExisting
        return "$normalizedExisting\n$normalizedNext"
    }
}
