package uk.botsoft.hearingassist.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import uk.botsoft.hearingassist.data.GatewaySettings
import uk.botsoft.hearingassist.data.HearingProfile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class KiGatewayClient {
    suspend fun askAssistant(
        settings: GatewaySettings,
        userPrompt: String,
        profile: HearingProfile,
        translateToEnglish: Boolean,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (settings.baseUrl.isBlank()) {
            return@withContext Result.success(buildOfflineAnswer(userPrompt, profile, translateToEnglish))
        }

        runCatching {
            val connection = (URL(settings.baseUrl.trimEnd('/') + "/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                if (settings.apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                }
                doOutput = true
                connectTimeout = 12_000
                readTimeout = 25_000
            }

            val systemPrompt = if (translateToEnglish) {
                "You are a hearing-assistance translator. Translate the user's sentence to concise natural English and preserve intent."
            } else {
                "You are a hearing-assistance coach. Give short, practical answers in German. Use the provided hearing profile for advice when relevant."
            }

            val payload = JSONObject()
                .put("model", settings.model)
                .put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject().put("role", "system").put(
                                "content",
                                "$systemPrompt Current profile: gain=${profile.gain}, low=${profile.lowBand}, mid=${profile.midBand}, high=${profile.highBand}, noise=${profile.noiseReduction}, voice=${profile.voiceFocus}.",
                            ),
                        )
                        .put(JSONObject().put("role", "user").put("content", userPrompt)),
                )
                .put("temperature", 0.4)

            connection.outputStream.use { it.write(payload.toString().toByteArray()) }

            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { reader -> reader.readText() }
            val root = JSONObject(body)
            root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

    private fun buildOfflineAnswer(prompt: String, profile: HearingProfile, translateToEnglish: Boolean): String {
        if (translateToEnglish) {
            return "Offline-Modus: Bitte Gateway konfigurieren. Beispieltext: \"$prompt\""
        }

        val emphasis = when {
            profile.highBand > 1.5f -> "Höhen sind schon recht stark betont."
            profile.midBand > 1.4f -> "Mitten sind gut für Sprachverständlichkeit angehoben."
            else -> "Mitten und Höhen könnten noch etwas Feintuning vertragen."
        }

        return "Offline-Empfehlung: Profil ${profile.preset.label}, Gain ${"%.2f".format(profile.gain)}. $emphasis Für kurze Gespräche zuerst Voice-Fokus erhöhen und Noise Reduction nur moderat einsetzen. Deine Eingabe war: \"$prompt\""
    }
}
