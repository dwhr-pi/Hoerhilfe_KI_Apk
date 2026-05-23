package uk.botsoft.hearingassist.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import uk.botsoft.hearingassist.data.GatewayProvider
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
            val endpoint = settings.resolvedEndpoint()
            val connection = settings.openConnection(endpoint)

            val systemPrompt = if (translateToEnglish) {
                "You are a hearing-assistance translator. Translate the user's sentence to concise natural English and preserve intent."
            } else {
                "You are a hearing-assistance coach. Give short, practical answers in German. Use the provided hearing profile for advice when relevant."
            }

            val payload = when (settings.provider) {
                GatewayProvider.Ollama -> buildOllamaPayload(settings, userPrompt, systemPrompt, profile)
                GatewayProvider.OpenClaw, GatewayProvider.OpenAiCompatible -> buildOpenAiPayload(settings, userPrompt, systemPrompt, profile)
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray()) }

            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { reader -> reader.readText() }

            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode}: $body")
            }

            when (settings.provider) {
                GatewayProvider.Ollama -> parseOllamaResponse(body)
                GatewayProvider.OpenClaw, GatewayProvider.OpenAiCompatible -> parseOpenAiResponse(body)
            }
        }
    }

    suspend fun testGateway(settings: GatewaySettings): Result<String> = withContext(Dispatchers.IO) {
        if (settings.baseUrl.isBlank()) {
            return@withContext Result.success("Noch keine Base URL gesetzt")
        }

        runCatching {
            val endpoint = settings.resolvedEndpoint()
            val probeProfile = HearingProfile()
            val payload = when (settings.provider) {
                GatewayProvider.Ollama -> buildOllamaPayload(
                    settings = settings,
                    userPrompt = "Antwort nur mit OK.",
                    systemPrompt = "You are a connectivity check endpoint.",
                    profile = probeProfile,
                )
                GatewayProvider.OpenClaw, GatewayProvider.OpenAiCompatible -> buildOpenAiPayload(
                    settings = settings,
                    userPrompt = "Antworte nur mit OK.",
                    systemPrompt = "You are a connectivity check endpoint.",
                    profile = probeProfile,
                )
            }

            val connection = settings.openConnection(endpoint)
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { reader -> reader.readText() }

            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode}: $body")
            }

            val parsed = when (settings.provider) {
                GatewayProvider.Ollama -> parseOllamaResponse(body)
                GatewayProvider.OpenClaw, GatewayProvider.OpenAiCompatible -> parseOpenAiResponse(body)
            }

            "Gateway erreichbar: ${settings.provider.label} @ $endpoint\nAntwort: ${parsed.ifBlank { "OK" }}"
        }
    }

    private fun buildOpenAiPayload(
        settings: GatewaySettings,
        userPrompt: String,
        systemPrompt: String,
        profile: HearingProfile,
    ): JSONObject {
        return JSONObject()
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
        }

    private fun buildOllamaPayload(
        settings: GatewaySettings,
        userPrompt: String,
        systemPrompt: String,
        profile: HearingProfile,
    ): JSONObject {
        return JSONObject()
            .put("model", settings.model)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject().put(
                            "role",
                            "system",
                        ).put(
                            "content",
                            "$systemPrompt Current profile: gain=${profile.gain}, low=${profile.lowBand}, mid=${profile.midBand}, high=${profile.highBand}, noise=${profile.noiseReduction}, voice=${profile.voiceFocus}.",
                        ),
                    )
                    .put(JSONObject().put("role", "user").put("content", userPrompt)),
            )
            .put("stream", false)
            .put(
                "options",
                JSONObject()
                    .put("temperature", 0.4),
            )
    }

    private fun parseOpenAiResponse(body: String): String {
        val root = JSONObject(body)
        return root.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    private fun parseOllamaResponse(body: String): String {
        val root = JSONObject(body)
        return when {
            root.has("message") -> root.getJSONObject("message").optString("content", "").trim()
            root.has("response") -> root.getString("response").trim()
            else -> body
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

    private fun GatewaySettings.resolvedEndpoint(): String {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        val path = requestPath.trim().ifBlank { provider.defaultPath }
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return normalizedBase + normalizedPath
    }

    private fun GatewaySettings.openConnection(endpoint: String): HttpURLConnection {
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")

            val token = apiKey.trim()
            if (token.isNotEmpty()) {
                val prefix = authPrefix.trim()
                val headerValue = if (prefix.isEmpty()) token else "$prefix $token"
                setRequestProperty(authHeader.trim().ifBlank { "Authorization" }, headerValue)
            }

            parseCustomHeaders(customHeaders).forEach { (key, value) ->
                setRequestProperty(key, value)
            }

            doOutput = true
            connectTimeout = 12_000
            readTimeout = 35_000
        }
    }

    private fun parseCustomHeaders(raw: String): Map<String, String> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(':') }
            .associate { line ->
                val parts = line.split(':', limit = 2)
                parts[0].trim() to parts[1].trim()
            }
    }
}
