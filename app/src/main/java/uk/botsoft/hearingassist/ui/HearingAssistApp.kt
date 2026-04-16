package uk.botsoft.hearingassist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.botsoft.hearingassist.audio.HearingTestPlayer
import uk.botsoft.hearingassist.data.AppSettings
import uk.botsoft.hearingassist.data.AudioPreset
import uk.botsoft.hearingassist.data.GatewaySettings
import uk.botsoft.hearingassist.data.HearingProfile
import uk.botsoft.hearingassist.data.HearingTestResult
import uk.botsoft.hearingassist.data.SettingsRepository
import uk.botsoft.hearingassist.network.KiGatewayClient

private enum class Screen {
    Dashboard,
    HearingTest,
    Assistant,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearingAssistApp() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val player = remember { HearingTestPlayer() }
    val gatewayClient = remember { KiGatewayClient() }
    val scope = rememberCoroutineScope()

    var appSettings by remember { mutableStateOf(AppSettings()) }
    var selectedScreen by remember { mutableStateOf(Screen.Dashboard) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appSettings = repository.load()
        initialized = true
    }

    fun persist(newSettings: AppSettings) {
        appSettings = newSettings
        if (initialized) {
            repository.save(newSettings)
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Hearing Assist KI", fontWeight = FontWeight.SemiBold) },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedScreen == Screen.Dashboard,
                        onClick = { selectedScreen = Screen.Dashboard },
                        icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        label = { Text("Audio") },
                    )
                    NavigationBarItem(
                        selected = selectedScreen == Screen.HearingTest,
                        onClick = { selectedScreen = Screen.HearingTest },
                        icon = { Icon(Icons.Rounded.Hearing, contentDescription = null) },
                        label = { Text("Hörtest") },
                    )
                    NavigationBarItem(
                        selected = selectedScreen == Screen.Assistant,
                        onClick = { selectedScreen = Screen.Assistant },
                        icon = { Icon(Icons.Rounded.Psychology, contentDescription = null) },
                        label = { Text("KI") },
                    )
                }
            },
        ) { innerPadding ->
            when (selectedScreen) {
                Screen.Dashboard -> DashboardScreen(
                    modifier = Modifier.padding(innerPadding),
                    profile = appSettings.profile,
                    onProfileChange = { persist(appSettings.copy(profile = it)) },
                )

                Screen.HearingTest -> HearingTestScreen(
                    modifier = Modifier.padding(innerPadding),
                    result = appSettings.testResult,
                    onPlay = { player.playTone(it) },
                    onResultChange = { persist(appSettings.copy(testResult = it)) },
                    onApplyRecommendation = {
                        persist(appSettings.copy(profile = appSettings.testResult.recommendedProfile()))
                    },
                )

                Screen.Assistant -> AssistantScreen(
                    modifier = Modifier.padding(innerPadding),
                    settings = appSettings.gatewaySettings,
                    profile = appSettings.profile,
                    onSettingsChange = { persist(appSettings.copy(gatewaySettings = it)) },
                    onAsk = { prompt, translate, onResult ->
                        scope.launch {
                            val result = gatewayClient.askAssistant(
                                settings = appSettings.gatewaySettings,
                                userPrompt = prompt,
                                profile = appSettings.profile,
                                translateToEnglish = translate,
                            )
                            onResult(result.getOrElse { "Fehler: ${it.message ?: "unbekannt"}" })
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    modifier: Modifier = Modifier,
    profile: HearingProfile,
    onProfileChange: (HearingProfile) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Schnellprofile", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = profile.preset == preset,
                                onClick = { onProfileChange(profileForPreset(preset)) },
                                label = { Text(preset.label) },
                            )
                        }
                    }
                }
            }
        }
        item { AudioSliderCard("Verstärkung", profile.gain, 1f..2f) { onProfileChange(profile.copy(gain = it)) } }
        item { AudioSliderCard("Bass", profile.lowBand, 0.8f..2.2f) { onProfileChange(profile.copy(lowBand = it)) } }
        item { AudioSliderCard("Mitten / Sprache", profile.midBand, 0.8f..2.2f) { onProfileChange(profile.copy(midBand = it)) } }
        item { AudioSliderCard("Höhen", profile.highBand, 0.8f..2.2f) { onProfileChange(profile.copy(highBand = it)) } }
        item { AudioSliderCard("Noise Reduction", profile.noiseReduction, 0f..1f) { onProfileChange(profile.copy(noiseReduction = it)) } }
        item { AudioSliderCard("Voice Focus", profile.voiceFocus, 0f..1f) { onProfileChange(profile.copy(voiceFocus = it)) } }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MVP-Hinweis", style = MaterialTheme.typography.titleMedium)
                    Text("Diese Version verwaltet Hörprofile, macht einen einfachen Hörtest und bindet KI an. Sie ist noch keine latenzoptimierte Live-DSP-Pipeline und kein Medizinprodukt.")
                }
            }
        }
    }
}

@Composable
private fun HearingTestScreen(
    modifier: Modifier = Modifier,
    result: HearingTestResult,
    onPlay: (Int) -> Unit,
    onResultChange: (HearingTestResult) -> Unit,
    onApplyRecommendation: () -> Unit,
) {
    val frequencies = listOf(250, 500, 1000, 2000, 4000, 6000)
    val localValues = remember(result.thresholdByFrequency) {
        mutableStateMapOf<Int, Int>().apply { putAll(result.thresholdByFrequency) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hörtest light", style = MaterialTheme.typography.titleMedium)
                    Text("Spiele jeden Ton ab und schätze die Lautstärke, ab der du ihn sicher wahrnimmst. Die Werte dienen nur zur Profilbildung.")
                }
            }
        }

        items(frequencies) { frequency ->
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("$frequency Hz", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { onPlay(frequency) }) {
                            Text("Ton abspielen")
                        }
                    }

                    val current = localValues[frequency] ?: 20
                    Text("Schwelle: $current dB")
                    Slider(
                        value = current.toFloat(),
                        onValueChange = {
                            val updated = it.toInt()
                            localValues[frequency] = updated
                            onResultChange(HearingTestResult(localValues.toMap()))
                        },
                        valueRange = 0f..60f,
                    )
                }
            }
        }

        item {
            Button(modifier = Modifier.fillMaxWidth(), onClick = onApplyRecommendation) {
                Text("Empfohlenes Profil übernehmen")
            }
        }
    }
}

@Composable
private fun AssistantScreen(
    modifier: Modifier = Modifier,
    settings: GatewaySettings,
    profile: HearingProfile,
    onSettingsChange: (GatewaySettings) -> Unit,
    onAsk: (String, Boolean, (String) -> Unit) -> Unit,
) {
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Hier erscheinen KI-Antworten oder Übersetzungen.") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gateway", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = settings.baseUrl,
                        onValueChange = { onSettingsChange(settings.copy(baseUrl = it)) },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://dein-gateway.example") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.apiKey,
                        onValueChange = { onSettingsChange(settings.copy(apiKey = it)) },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.model,
                        onValueChange = { onSettingsChange(settings.copy(model = it)) },
                        label = { Text("Modell") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Erwartet ein OpenAI-kompatibles `/v1/chat/completions`-Gateway. Ohne URL läuft die App im Offline-Demo-Modus.")
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("KI-Hilfe", style = MaterialTheme.typography.titleMedium)
                    Text("Aktives Profil: ${profile.preset.label}, Gain ${"%.2f".format(profile.gain)}, Voice ${"%.2f".format(profile.voiceFocus)}")
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Nachricht oder Satz") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAsk(prompt, false) { answer = it } }, enabled = prompt.isNotBlank()) {
                            Text("Chat")
                        }
                        Button(onClick = { onAsk(prompt, true) { answer = it } }, enabled = prompt.isNotBlank()) {
                            Text("Übersetzen")
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Antwort", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(answer)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { prompt = "Wie stelle ich Sprache im Restaurant klarer ein?" }, label = { Text("Restaurant") })
                    AssistChip(onClick = { prompt = "Kannst du diesen Satz ins Englische übersetzen?" }, label = { Text("Translate") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { prompt = "Welche Werte passen für TV und Stimmen?" }, label = { Text("TV") })
                    AssistChip(onClick = { prompt = "Wie reduziere ich blechernen Klang?" }, label = { Text("Klang") })
                }
            }
        }
    }
}

@Composable
private fun AudioSliderCard(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("%.2f".format(value))
            Slider(value = value, onValueChange = onChange, valueRange = range)
        }
    }
}

private fun profileForPreset(preset: AudioPreset): HearingProfile = when (preset) {
    AudioPreset.Conversation -> HearingProfile(1.2f, 1.0f, 1.35f, 1.45f, 0.35f, 0.6f, preset)
    AudioPreset.Outdoor -> HearingProfile(1.15f, 0.95f, 1.4f, 1.35f, 0.55f, 0.72f, preset)
    AudioPreset.Tv -> HearingProfile(1.1f, 1.0f, 1.45f, 1.5f, 0.25f, 0.52f, preset)
    AudioPreset.Music -> HearingProfile(1.0f, 1.2f, 1.05f, 1.2f, 0.1f, 0.2f, preset)
}
