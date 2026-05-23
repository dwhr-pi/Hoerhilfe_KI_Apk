package uk.botsoft.hearingassist.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import uk.botsoft.hearingassist.data.AppLanguage

class VoicePromptSpeaker(context: Context) {
    private val ready = AtomicBoolean(false)
    private var tts: TextToSpeech? = null
    private var pendingUtterance: CompletableDeferred<Boolean>? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready.set(status == TextToSpeech.SUCCESS)
        }.apply {
            setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        pendingUtterance?.complete(true)
                        pendingUtterance = null
                    }

                    override fun onError(utteranceId: String?) {
                        pendingUtterance?.complete(false)
                        pendingUtterance = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        pendingUtterance?.complete(false)
                        pendingUtterance = null
                    }
                },
            )
        }
    }

    suspend fun speak(text: String, language: AppLanguage): Boolean = withContext(Dispatchers.Main) {
        val engine = tts ?: return@withContext false
        if (!ready.get()) return@withContext false

        val locale = when (language) {
            AppLanguage.System -> Locale.getDefault()
            AppLanguage.German -> Locale.GERMAN
            AppLanguage.English -> Locale.ENGLISH
        }
        engine.language = locale

        val utteranceId = UUID.randomUUID().toString()
        val completed = CompletableDeferred<Boolean>()
        pendingUtterance = completed

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            pendingUtterance = null
            return@withContext false
        }

        completed.await()
    }

    fun shutdown() {
        pendingUtterance?.complete(false)
        pendingUtterance = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
