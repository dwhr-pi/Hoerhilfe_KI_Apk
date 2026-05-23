package uk.botsoft.hearingassist.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.botsoft.hearingassist.audio.AdaptiveHearingTestEngine
import uk.botsoft.hearingassist.audio.AudioDeviceRouter
import uk.botsoft.hearingassist.audio.AudioOutputDevice
import uk.botsoft.hearingassist.audio.HearingTestPlayer
import uk.botsoft.hearingassist.audio.HearingTestUiState
import uk.botsoft.hearingassist.audio.LiveAudioEngine
import uk.botsoft.hearingassist.audio.MicrophoneMonitor
import uk.botsoft.hearingassist.audio.SpeechTranscriber
import uk.botsoft.hearingassist.audio.VolumeController
import uk.botsoft.hearingassist.audio.VoicePromptSpeaker
import uk.botsoft.hearingassist.data.AppLanguage
import uk.botsoft.hearingassist.data.AppSettings
import uk.botsoft.hearingassist.data.AppTextScale
import uk.botsoft.hearingassist.data.AudioPreset
import uk.botsoft.hearingassist.data.AudioRoutePreference
import uk.botsoft.hearingassist.data.AudioRuntimeSettings
import uk.botsoft.hearingassist.data.EarSide
import uk.botsoft.hearingassist.data.GatewayProvider
import uk.botsoft.hearingassist.data.GatewaySettings
import uk.botsoft.hearingassist.data.HearingProfile
import uk.botsoft.hearingassist.data.HearingTestResult
import uk.botsoft.hearingassist.data.MaintenanceSettings
import uk.botsoft.hearingassist.data.NoiseSuppressionMode
import uk.botsoft.hearingassist.data.SavedProfile
import uk.botsoft.hearingassist.data.SettingsRepository
import uk.botsoft.hearingassist.data.ThemeMode
import uk.botsoft.hearingassist.maintenance.ErrorReporter
import uk.botsoft.hearingassist.maintenance.UpdateChecker
import uk.botsoft.hearingassist.network.KiGatewayClient

private enum class Screen {
    Home,
    HearingTest,
    Live,
    Assistant,
    Settings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearingAssistApp() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val player = remember { HearingTestPlayer() }
    val hearingTestEngine = remember { AdaptiveHearingTestEngine(player) }
    val voicePromptSpeaker = remember { VoicePromptSpeaker(context) }
    val gatewayClient = remember { KiGatewayClient() }
    val audioRouter = remember { AudioDeviceRouter(context) }
    val microphoneMonitor = remember { MicrophoneMonitor() }
    val liveAudioEngine = remember { LiveAudioEngine() }
    val speechTranscriber = remember { SpeechTranscriber(context) }
    val volumeController = remember { VolumeController(context) }
    val errorReporter = remember { ErrorReporter(context) }
    val updateChecker = remember { UpdateChecker(context) }
    val scope = rememberCoroutineScope()

    var appSettings by remember { mutableStateOf(AppSettings()) }
    var selectedScreen by remember { mutableStateOf(Screen.Home) }
    var initialized by remember { mutableStateOf(false) }
    var micLevel by remember { mutableStateOf(0f) }
    var isMonitoring by remember { mutableStateOf(false) }
    var isLiveAudioRunning by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var audioStatus by remember { mutableStateOf("Noch kein Live-Audio aktiv") }
    var transcriptText by remember { mutableStateOf("Hier erscheint die Live-Transkription.") }
    var transcriptionStatus by remember { mutableStateOf("Transkription noch nicht gestartet") }
    var hearingTestState by remember { mutableStateOf(HearingTestUiState(result = HearingTestResult())) }
    var profileNameInput by remember { mutableStateOf("Standard Gespräch") }
    var maintenanceStatus by remember { mutableStateOf("Fehlerprotokoll und Update-Pruefung bereit") }

    fun activeLanguage(): AppLanguage = resolveAppLanguage(appSettings.audioRuntimeSettings.appLanguage)

    val hasRecordAudioPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val hasBluetoothConnectPermission = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasRecordAudioPermission.value = granted
        audioStatus = if (granted) "Mikrofonfreigabe erteilt" else "Mikrofonfreigabe fehlt"
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasBluetoothConnectPermission.value = granted
        audioStatus = if (granted) "Bluetooth-Berechtigung erteilt" else "Bluetooth-Berechtigung fehlt"
    }

    DisposableEffect(Unit) {
        onDispose {
            microphoneMonitor.stop()
            liveAudioEngine.stop()
            speechTranscriber.stop()
            hearingTestEngine.stop()
            voicePromptSpeaker.shutdown()
            audioRouter.releaseToSystemMix()
        }
    }

    LaunchedEffect(Unit) {
        val loaded = repository.load()
        val startupVolume = volumeController.currentVolumePercent()
        appSettings = loaded.copy(
            audioRuntimeSettings = loaded.audioRuntimeSettings.copy(
                hearingAidEnabled = false,
                micMonitoringEnabled = false,
                liveProcessingEnabled = false,
                masterVolumePercent = startupVolume,
            ),
        )
        val selectedProfile = loaded.savedProfiles.firstOrNull { it.id == loaded.selectedProfileId } ?: loaded.savedProfiles.first()
        profileNameInput = selectedProfile.name
        hearingTestState = HearingTestUiState(result = loaded.testResult, totalUnits = 64)
        initialized = true
        if (loaded.audioRuntimeSettings.preferredRoute != AudioRoutePreference.Default) {
            audioStatus = runCatching { audioRouter.applyRoute(loaded.audioRuntimeSettings.preferredRoute) }
                .getOrElse { "Gespeicherte Route konnte nicht aktiviert werden" }
        }
        volumeController.setVolumePercent(startupVolume)
    }

    LaunchedEffect(appSettings.profile) {
        if (isLiveAudioRunning) liveAudioEngine.updateProfile(appSettings.profile)
    }

    LaunchedEffect(appSettings.audioRuntimeSettings.enhancedDspEnabled) {
        if (isLiveAudioRunning) liveAudioEngine.updateProcessingMode(appSettings.audioRuntimeSettings.enhancedDspEnabled)
    }

    LaunchedEffect(
        appSettings.audioRuntimeSettings.lowLatencyModeEnabled,
        appSettings.audioRuntimeSettings.realTimeHeadsetDspEnabled,
        appSettings.audioRuntimeSettings.nativeLowLatencyPipelineEnabled,
        appSettings.audioRuntimeSettings.advancedDspFiltersEnabled,
        appSettings.audioRuntimeSettings.noiseSuppressionMode,
        appSettings.audioRuntimeSettings.mediaMixModeEnabled,
    ) {
        if (!initialized || !isLiveAudioRunning) return@LaunchedEffect
        liveAudioEngine.stop()
        val restarted = liveAudioEngine.start(
            profile = appSettings.profile,
            enhancedProcessing = appSettings.audioRuntimeSettings.enhancedDspEnabled,
            lowLatencyMode = appSettings.audioRuntimeSettings.lowLatencyModeEnabled,
            realTimeHeadsetDspEnabled = appSettings.audioRuntimeSettings.realTimeHeadsetDspEnabled,
            nativeLowLatencyPipelineEnabled = appSettings.audioRuntimeSettings.nativeLowLatencyPipelineEnabled,
            advancedDspFiltersEnabled = appSettings.audioRuntimeSettings.advancedDspFiltersEnabled,
            noiseSuppressionMode = appSettings.audioRuntimeSettings.noiseSuppressionMode,
            mediaMixMode = appSettings.audioRuntimeSettings.mediaMixModeEnabled,
            normalizationEnabled = appSettings.audioRuntimeSettings.microphoneNormalizationEnabled,
            onLevelChanged = { micLevel = it },
            onStatusChanged = { audioStatus = it },
        )
        isLiveAudioRunning = restarted
        val restartedSettings = appSettings.copy(
            audioRuntimeSettings = appSettings.audioRuntimeSettings.copy(
                hearingAidEnabled = restarted,
                liveProcessingEnabled = restarted,
            ),
        )
        appSettings = restartedSettings
        if (initialized) repository.save(restartedSettings)
    }

    LaunchedEffect(appSettings.audioRuntimeSettings.liveTranscriptionEnabled) {
        if (!appSettings.audioRuntimeSettings.liveTranscriptionEnabled && isTranscribing) {
            speechTranscriber.stop()
            isTranscribing = false
            transcriptionStatus = localized(
                activeLanguage(),
                "Live-Transkription in den Einstellungen deaktiviert",
                "Live transcription disabled in settings",
            )
            if (!isMonitoring && !isLiveAudioRunning && !isTranscribing) {
                audioRouter.releaseToSystemMix()
            }
        }
    }

    fun persist(newSettings: AppSettings) {
        appSettings = newSettings
        if (initialized) repository.save(newSettings)
    }

    fun prepareMediaFriendlyAudioState() {
        if (
            appSettings.audioRuntimeSettings.mediaMixModeEnabled &&
            appSettings.audioRuntimeSettings.preferredRoute != AudioRoutePreference.Bluetooth
        ) {
            audioRouter.releaseToSystemMix()
        }
    }

    fun refreshSystemAudioMixIfIdle() {
        if (!isMonitoring && !isLiveAudioRunning && !isTranscribing) {
            audioRouter.releaseToSystemMix()
        }
    }

    fun updateRuntime(transform: (AudioRuntimeSettings) -> AudioRuntimeSettings) {
        val updated = appSettings.copy(audioRuntimeSettings = transform(appSettings.audioRuntimeSettings))
        persist(updated)
    }

    fun selectProfile(profile: SavedProfile) {
        profileNameInput = profile.name
        persist(
            appSettings.copy(
                profile = profile.profile,
                selectedProfileId = profile.id,
            ),
        )
    }

    fun saveProfile(asNew: Boolean) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val trimmedName = profileNameInput.trim().ifBlank {
            localized(language = resolveAppLanguage(appSettings.audioRuntimeSettings.appLanguage), "Profil ${appSettings.savedProfiles.size + 1}", "Profile ${appSettings.savedProfiles.size + 1}")
        }
        val selected = appSettings.savedProfiles.firstOrNull { it.id == appSettings.selectedProfileId }
        val source = when {
            hearingTestState.result.leftThresholdByFrequency.isNotEmpty() || hearingTestState.result.rightThresholdByFrequency.isNotEmpty() ->
                localized(resolveAppLanguage(appSettings.audioRuntimeSettings.appLanguage), "Hörtest", "Hearing test")
            else -> localized(resolveAppLanguage(appSettings.audioRuntimeSettings.appLanguage), "Manuell", "Manual")
        }
        val overwriteSelected = !asNew && selected != null && selected.source != "Vorgabe"

        val selectedId = selected?.id
        val updatedProfiles = if (overwriteSelected && selectedId != null) {
            appSettings.savedProfiles.map {
                if (it.id == selectedId) {
                    it.copy(
                        name = trimmedName,
                        profile = appSettings.profile,
                        source = source,
                        createdAtIso = now,
                    )
                } else {
                    it
                }
            }
        } else {
            appSettings.savedProfiles + SavedProfile(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                profile = appSettings.profile,
                source = source,
                createdAtIso = now,
            )
        }

        val active = updatedProfiles.lastOrNull { it.name == trimmedName && it.profile == appSettings.profile } ?: updatedProfiles.first { it.name == trimmedName }
        persist(
            appSettings.copy(
                savedProfiles = updatedProfiles,
                selectedProfileId = active.id,
            ),
        )
    }

    fun toggleMonitoring() {
        if (!hasRecordAudioPermission.value) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (isMonitoring) {
            if (!isLiveAudioRunning) {
                microphoneMonitor.stop()
            }
            isMonitoring = false
            if (!isLiveAudioRunning) {
                micLevel = 0f
            }
            audioStatus = localized(activeLanguage(), "Mikrofonmonitor gestoppt", "Microphone monitor stopped")
            updateRuntime { it.copy(micMonitoringEnabled = false) }
            refreshSystemAudioMixIfIdle()
            return
        }

        if (isLiveAudioRunning) {
            isMonitoring = true
            audioStatus = localized(
                activeLanguage(),
                "Monitor aktiv. Die Pegelanzeige folgt jetzt Live Assist.",
                "Monitor active. The level meter now follows Live Assist.",
            )
            updateRuntime { it.copy(micMonitoringEnabled = true) }
            return
        }

        prepareMediaFriendlyAudioState()
        val started = runCatching {
            microphoneMonitor.start { level -> micLevel = level }
        }.getOrDefault(false)

        isMonitoring = started
        audioStatus = if (started) {
            localized(activeLanguage(), "Mikrofonmonitor aktiv", "Microphone monitor active")
        } else {
            localized(activeLanguage(), "Monitor konnte nicht gestartet werden", "Monitor could not be started")
        }
        updateRuntime { it.copy(micMonitoringEnabled = started) }
    }

    fun toggleLiveAudio() {
        if (!hasRecordAudioPermission.value) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val keepMonitoringEnabled = isMonitoring
        if (isMonitoring) {
            microphoneMonitor.stop()
        }

        if (isLiveAudioRunning) {
            liveAudioEngine.stop()
            isLiveAudioRunning = false
            if (isMonitoring) {
                val restartedMonitor = runCatching {
                    microphoneMonitor.start { level -> micLevel = level }
                }.getOrDefault(false)
                isMonitoring = restartedMonitor
                audioStatus = if (restartedMonitor) {
                    localized(
                        activeLanguage(),
                        "Live-Audio gestoppt. Mikrofonmonitor wieder aktiv.",
                        "Live audio stopped. Microphone monitor active again.",
                    )
                } else {
                    micLevel = 0f
                    localized(activeLanguage(), "Live-Audio gestoppt", "Live audio stopped")
                }
                updateRuntime {
                    it.copy(
                        hearingAidEnabled = false,
                        liveProcessingEnabled = false,
                        micMonitoringEnabled = restartedMonitor,
                    )
                }
            } else {
                micLevel = 0f
                audioStatus = localized(activeLanguage(), "Live-Audio gestoppt", "Live audio stopped")
                updateRuntime { it.copy(hearingAidEnabled = false, liveProcessingEnabled = false) }
            }
            refreshSystemAudioMixIfIdle()
            return
        }

        prepareMediaFriendlyAudioState()
        val started = liveAudioEngine.start(
            profile = appSettings.profile,
            enhancedProcessing = appSettings.audioRuntimeSettings.enhancedDspEnabled,
            lowLatencyMode = appSettings.audioRuntimeSettings.lowLatencyModeEnabled,
            realTimeHeadsetDspEnabled = appSettings.audioRuntimeSettings.realTimeHeadsetDspEnabled,
            nativeLowLatencyPipelineEnabled = appSettings.audioRuntimeSettings.nativeLowLatencyPipelineEnabled,
            advancedDspFiltersEnabled = appSettings.audioRuntimeSettings.advancedDspFiltersEnabled,
            noiseSuppressionMode = appSettings.audioRuntimeSettings.noiseSuppressionMode,
            mediaMixMode = appSettings.audioRuntimeSettings.mediaMixModeEnabled,
            normalizationEnabled = appSettings.audioRuntimeSettings.microphoneNormalizationEnabled,
            onLevelChanged = { micLevel = it },
            onStatusChanged = { audioStatus = it },
        )

        isLiveAudioRunning = started
        if (!started && keepMonitoringEnabled) {
            val restartedMonitor = runCatching {
                microphoneMonitor.start { level -> micLevel = level }
            }.getOrDefault(false)
            isMonitoring = restartedMonitor
        }
        audioStatus = when {
            started && isMonitoring -> localized(
                activeLanguage(),
                "Live Assist und Monitor aktiv. Die Pegelanzeige folgt Live Assist.",
                "Live Assist and monitor are active. The level meter follows Live Assist.",
            )
            started -> localized(activeLanguage(), "Live-Audio aktiv", "Live audio active")
            else -> localized(activeLanguage(), "Live-Audio konnte nicht gestartet werden", "Live audio could not be started")
        }
        updateRuntime {
            it.copy(
                hearingAidEnabled = started,
                liveProcessingEnabled = started,
                micMonitoringEnabled = isMonitoring,
            )
        }
    }

    fun toggleHearingAid() {
        toggleLiveAudio()
    }

    fun toggleTranscription() {
        if (!appSettings.audioRuntimeSettings.liveTranscriptionEnabled) {
            transcriptionStatus = localized(
                activeLanguage(),
                "Live-Transkription in den Einstellungen deaktiviert",
                "Live transcription disabled in settings",
            )
            return
        }

        if (!hasRecordAudioPermission.value) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (isTranscribing) {
            speechTranscriber.stop()
            isTranscribing = false
            transcriptionStatus = localized(
                activeLanguage(),
                "Live-Transkription gestoppt",
                "Live transcription stopped",
            )
            refreshSystemAudioMixIfIdle()
            return
        }

        prepareMediaFriendlyAudioState()
        val started = speechTranscriber.start(
            languageTag = if (resolveAppLanguage(appSettings.audioRuntimeSettings.appLanguage) == AppLanguage.German) "de-DE" else "en-US",
            streamingMode = appSettings.audioRuntimeSettings.streamingCaptionModeEnabled,
            onStatusChanged = { transcriptionStatus = it },
            onTranscriptChanged = { transcriptText = it },
        )
        isTranscribing = started
    }

    fun applyRoute(preference: AudioRoutePreference) {
        val needsBluetoothPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            preference == AudioRoutePreference.Bluetooth &&
            !hasBluetoothConnectPermission.value

        if (needsBluetoothPermission) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }

        audioStatus = runCatching { audioRouter.applyRoute(preference) }
            .getOrElse { localized(activeLanguage(), "Routing fehlgeschlagen", "Routing failed") + ": ${it.message ?: "unknown"}" }
        updateRuntime { it.copy(preferredRoute = preference) }
    }

    fun setMasterVolume(percent: Int) {
        volumeController.setVolumePercent(percent)
        updateRuntime { it.copy(masterVolumePercent = percent.coerceIn(0, 100)) }
    }

    fun updateMaintenance(transform: (MaintenanceSettings) -> MaintenanceSettings) {
        persist(appSettings.copy(maintenanceSettings = transform(appSettings.maintenanceSettings)))
    }

    fun sendErrorReport() {
        errorReporter.append(
            productId = appSettings.maintenanceSettings.productId,
            title = "Manuell gesendetes Fehlerprotokoll",
            detail = "AudioStatus=$audioStatus\nTranskription=$transcriptionStatus\nProfil=${appSettings.selectedProfileId}",
        )
        val result = errorReporter.sendByEmail(
            productId = appSettings.maintenanceSettings.productId,
            recipient = appSettings.maintenanceSettings.errorReportEmail,
        )
        maintenanceStatus = result.getOrElse { error ->
            "Fehlerprotokoll konnte nicht geoeffnet werden: ${error.message ?: "unbekannt"}"
        }
    }

    fun clearErrorReport() {
        errorReporter.clear()
        maintenanceStatus = localized(activeLanguage(), "Fehlerprotokoll geloescht", "Error log cleared")
    }

    fun checkForUpdate() {
        maintenanceStatus = localized(activeLanguage(), "Update-Pruefung laeuft ...", "Checking for update ...")
        scope.launch {
            val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                updateChecker.check(
                    manifestUrl = appSettings.maintenanceSettings.updateManifestUrl,
                    productId = appSettings.maintenanceSettings.productId,
                )
            }
            maintenanceStatus = result.fold(
                onSuccess = { update ->
                    if (update.available) {
                        localized(activeLanguage(), "Update verfuegbar", "Update available") +
                            ": ${update.latestVersionName} (${update.latestVersionCode}) ${update.downloadUrl}".trimEnd()
                    } else {
                        localized(activeLanguage(), "Kein Update verfuegbar", "No update available") +
                            ": ${updateChecker.currentVersionName()} (${updateChecker.currentVersionCode()})"
                    }
                },
                onFailure = { error ->
                    localized(activeLanguage(), "Update-Pruefung fehlgeschlagen", "Update check failed") +
                        ": ${error.message ?: "unknown"}"
                },
            )
        }
    }

    fun startAdaptiveHearingTest() {
        if (!hasRecordAudioPermission.value) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        hearingTestState = HearingTestUiState(
            isRunning = true,
            status = localized(
                activeLanguage(),
                "Sprachanweisung wird abgespielt",
                "Voice instructions are playing",
            ),
            result = hearingTestState.result,
            totalUnits = 64,
        )

        scope.launch {
            voicePromptSpeaker.speak(
                localized(
                    activeLanguage(),
                    "Sobald Sie den Ton hören können und wahrnehmen, klicken Sie auf den großen roten Button auf dem Bildschirm Ihres Geräts.",
                    "As soon as you can hear and notice the tone, tap the large red button on your device screen.",
                ),
                activeLanguage(),
            )
            delay(250)
            voicePromptSpeaker.speak(
                localized(
                    activeLanguage(),
                    "Sind Sie bereit? Dann klicken Sie zum Start des Hörtests auf den Button, ab jetzt!",
                    "Are you ready? Then tap the button to start the hearing test from now on.",
                ),
                activeLanguage(),
            )
            delay(500)

            hearingTestEngine.start(
                onStateChanged = { state -> hearingTestState = state },
                onCompleted = { result ->
                    val recommended = result.recommendedProfile()
                    hearingTestState = hearingTestState.copy(
                        result = result,
                        status = localized(
                            activeLanguage(),
                            "Hörtest abgeschlossen. Bitte empfohlenes Profil speichern.",
                            "Hearing test finished. Please save the recommended profile.",
                        ),
                    )
                    persist(appSettings.copy(testResult = result, profile = recommended))
                    profileNameInput = "Hörtest ${DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now())}"
                },
            )
        }
    }

    val language = resolveAppLanguage(appSettings.audioRuntimeSettings.appLanguage)
    val useDarkTheme = when (appSettings.audioRuntimeSettings.themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(localized(language, "Hörhilfe KI", "AI Hearing Aid"), fontWeight = FontWeight.SemiBold) },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedScreen == Screen.Home,
                        onClick = { selectedScreen = Screen.Home },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        label = { Text(localized(language, "Start", "Home")) },
                    )
                    NavigationBarItem(
                        selected = selectedScreen == Screen.HearingTest,
                        onClick = { selectedScreen = Screen.HearingTest },
                        icon = { Icon(Icons.Rounded.Hearing, contentDescription = null) },
                        label = { Text(localized(language, "Hörtest", "Hearing")) },
                    )
                    NavigationBarItem(
                        selected = selectedScreen == Screen.Live,
                        onClick = { selectedScreen = Screen.Live },
                        icon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                        label = { Text(localized(language, "Live", "Live")) },
                    )
                    NavigationBarItem(
                        selected = selectedScreen == Screen.Assistant,
                        onClick = { selectedScreen = Screen.Assistant },
                        icon = { Icon(Icons.Rounded.Psychology, contentDescription = null) },
                        label = { Text(localized(language, "KI", "AI")) },
                    )
                    NavigationBarItem(
                        selected = selectedScreen == Screen.Settings,
                        onClick = { selectedScreen = Screen.Settings },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text(localized(language, "Setup", "Setup")) },
                    )
                }
            },
        ) { innerPadding ->
            when (selectedScreen) {
                Screen.Home -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    language = language,
                    hearingAidEnabled = appSettings.audioRuntimeSettings.hearingAidEnabled,
                    textScale = appSettings.audioRuntimeSettings.textScale,
                    currentProfile = appSettings.profile,
                    savedProfiles = appSettings.savedProfiles,
                    selectedProfileId = appSettings.selectedProfileId,
                    profileName = profileNameInput,
                    transcriptText = transcriptText,
                    transcriptionStatus = transcriptionStatus,
                    masterVolumePercent = appSettings.audioRuntimeSettings.masterVolumePercent,
                    onProfileNameChange = { profileNameInput = it },
                    onSelectProfile = ::selectProfile,
                    onSaveCurrentProfile = { saveProfile(false) },
                    onSaveNewProfile = { saveProfile(true) },
                    onVolumeChange = ::setMasterVolume,
                    onToggleHearingAid = ::toggleHearingAid,
                )

                Screen.HearingTest -> HearingTestScreen(
                    modifier = Modifier.padding(innerPadding),
                    language = language,
                    textScale = appSettings.audioRuntimeSettings.textScale,
                    currentResult = appSettings.testResult,
                    testState = hearingTestState,
                    onStart = ::startAdaptiveHearingTest,
                    onStop = { hearingTestEngine.stop() },
                    onReact = { hearingTestEngine.registerReaction() },
                    onSaveRecommended = { saveProfile(true) },
                )

                Screen.Live -> LiveAudioScreen(
                    modifier = Modifier.padding(innerPadding),
                    profile = appSettings.profile,
                    runtimeSettings = appSettings.audioRuntimeSettings,
                    language = language,
                    textScale = appSettings.audioRuntimeSettings.textScale,
                    micPermissionGranted = hasRecordAudioPermission.value,
                    bluetoothPermissionGranted = hasBluetoothConnectPermission.value,
                    micLevel = micLevel,
                    isMonitoring = isMonitoring,
                    isLiveAudioRunning = isLiveAudioRunning,
                    isTranscribing = isTranscribing,
                    audioStatus = audioStatus,
                    currentRoute = audioRouter.currentRouteSummary(),
                    bluetoothAvailable = audioRouter.bluetoothAvailable(),
                    outputs = audioRouter.listOutputs(),
                    transcriptionStatus = transcriptionStatus,
                    transcriptText = transcriptText,
                    onRequestPermission = { recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestBluetoothPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    },
                    onToggleMonitoring = ::toggleMonitoring,
                    onToggleLiveAudio = ::toggleLiveAudio,
                    onToggleTranscription = ::toggleTranscription,
                    onApplyRoute = ::applyRoute,
                )

                Screen.Assistant -> AssistantScreen(
                    modifier = Modifier.padding(innerPadding),
                    settings = appSettings.gatewaySettings,
                    profile = appSettings.profile,
                    language = language,
                    enabled = appSettings.audioRuntimeSettings.assistantEnabled,
                    onSettingsChange = { persist(appSettings.copy(gatewaySettings = it)) },
                    onAsk = { prompt, translate, onResult ->
                        scope.launch {
                            val result = gatewayClient.askAssistant(
                                settings = appSettings.gatewaySettings,
                                userPrompt = prompt,
                                profile = appSettings.profile,
                                translateToEnglish = translate,
                            )
                            onResult(result.getOrElse { localized(language, "Fehler", "Error") + ": ${it.message ?: "unknown"}" })
                        }
                    },
                    onTestGateway = { onResult ->
                        scope.launch {
                            val result = gatewayClient.testGateway(appSettings.gatewaySettings)
                            onResult(result.getOrElse { localized(language, "Gateway-Test fehlgeschlagen", "Gateway test failed") + ": ${it.message ?: "unknown"}" })
                        }
                    },
                )

                Screen.Settings -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    runtimeSettings = appSettings.audioRuntimeSettings,
                    maintenanceSettings = appSettings.maintenanceSettings,
                    maintenanceStatus = maintenanceStatus,
                    language = language,
                    savedProfiles = appSettings.savedProfiles,
                    selectedProfileId = appSettings.selectedProfileId,
                    onSelectProfile = ::selectProfile,
                    onToggleEnhancedDsp = { enabled -> updateRuntime { it.copy(enhancedDspEnabled = enabled) } },
                    onToggleLowLatencyMode = { enabled -> updateRuntime { it.copy(lowLatencyModeEnabled = enabled) } },
                    onToggleRealTimeHeadsetDsp = { enabled -> updateRuntime { it.copy(realTimeHeadsetDspEnabled = enabled) } },
                    onToggleBluetoothLeAudioOptimization = { enabled -> updateRuntime { it.copy(bluetoothLeAudioOptimizationEnabled = enabled) } },
                    onToggleNativeLowLatencyPipeline = { enabled -> updateRuntime { it.copy(nativeLowLatencyPipelineEnabled = enabled) } },
                    onToggleAdvancedDspFilters = { enabled -> updateRuntime { it.copy(advancedDspFiltersEnabled = enabled) } },
                    onToggleStreamingCaptionMode = { enabled -> updateRuntime { it.copy(streamingCaptionModeEnabled = enabled) } },
                    onToggleBluetoothCompatibilityMode = { enabled -> updateRuntime { it.copy(bluetoothCompatibilityModeEnabled = enabled) } },
                    onNoiseSuppressionModeChange = { mode -> updateRuntime { it.copy(noiseSuppressionMode = mode) } },
                    onToggleMediaMixMode = { enabled -> updateRuntime { it.copy(mediaMixModeEnabled = enabled) } },
                    onToggleNormalization = { enabled -> updateRuntime { it.copy(microphoneNormalizationEnabled = enabled) } },
                    onToggleTranscription = { enabled -> updateRuntime { it.copy(liveTranscriptionEnabled = enabled) } },
                    onToggleAssistant = { enabled -> updateRuntime { it.copy(assistantEnabled = enabled) } },
                    onThemeModeChange = { mode -> updateRuntime { it.copy(themeMode = mode) } },
                    onLanguageChange = { appLanguage -> updateRuntime { it.copy(appLanguage = appLanguage) } },
                    onTextScaleChange = { textScale -> updateRuntime { it.copy(textScale = textScale) } },
                    onMaintenanceChange = { maintenance -> persist(appSettings.copy(maintenanceSettings = maintenance)) },
                    onSendErrorReport = ::sendErrorReport,
                    onClearErrorReport = ::clearErrorReport,
                    onCheckForUpdate = ::checkForUpdate,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    hearingAidEnabled: Boolean,
    textScale: AppTextScale,
    currentProfile: HearingProfile,
    savedProfiles: List<SavedProfile>,
    selectedProfileId: String,
    profileName: String,
    transcriptText: String,
    transcriptionStatus: String,
    masterVolumePercent: Int,
    onProfileNameChange: (String) -> Unit,
    onSelectProfile: (SavedProfile) -> Unit,
    onSaveCurrentProfile: () -> Unit,
    onSaveNewProfile: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onToggleHearingAid: () -> Unit,
) {
    val selectedProfile = savedProfiles.firstOrNull { it.id == selectedProfileId }
    val detailStyle = detailTextStyle(textScale)
    val metaStyle = metaTextStyle(textScale)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(localized(language, "Profile", "Profiles"), style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedProfiles.chunked(2).forEach { rowProfiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowProfiles.forEach { profile ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        FilterChip(
                                            selected = profile.id == selectedProfileId,
                                            onClick = { onSelectProfile(profile) },
                                            label = { Text(homeProfileLabel(profile, language)) },
                                        )
                                    }
                                }
                                if (rowProfiles.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = onProfileNameChange,
                        label = { Text(localized(language, "Profilname", "Profile name")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveCurrentProfile) {
                            Text(localized(language, "Aktuelles Profil speichern", "Save current profile"))
                        }
                        Button(onClick = onSaveNewProfile) {
                            Text(localized(language, "Als neues Profil speichern", "Save as new profile"))
                        }
                    }
                    selectedProfile?.let {
                        Text(localized(language, "Quelle", "Source") + ": ${it.source}", style = metaStyle)
                        Text(localized(language, "Zeitpunkt", "Timestamp") + ": ${it.createdAtIso.replace('T', ' ')}", style = metaStyle)
                    }
                    Text(
                        localized(
                            language,
                            "Vorgabe-Profile wie Gespräch, Draußen, TV und Musik bleiben erhalten. Eigene Anpassungen kannst du separat abspeichern.",
                            "Preset profiles like Conversation, Outdoor, TV, and Music stay available. Save your own adjustments separately.",
                        ),
                        style = detailStyle,
                    )
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleHearingAid,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (hearingAidEnabled) {
                                localized(language, "Hörhilfe ausschalten", "Turn hearing aid off")
                            } else {
                                localized(language, "Hörhilfe starten", "Start hearing aid")
                            },
                        )
                    }
                    Text(
                        localized(
                            language,
                            "Startet oder stoppt Live Assist als Hörhilfe.",
                            "Starts or stops Live Assist as hearing aid.",
                        ),
                        style = detailStyle,
                    )
                    Text(localized(language, "Aktives Profil", "Active profile"), style = MaterialTheme.typography.titleMedium)
                    Text("${presetLabel(currentProfile.preset, language)} | Gain ${"%.2f".format(currentProfile.gain)}")
                    Text("Low ${"%.2f".format(currentProfile.lowBand)} | Mid ${"%.2f".format(currentProfile.midBand)} | High ${"%.2f".format(currentProfile.highBand)}")
                    Text("Noise ${"%.2f".format(currentProfile.noiseReduction)} | Voice ${"%.2f".format(currentProfile.voiceFocus)}")
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Gesamtlautstärke", "Master volume"), style = MaterialTheme.typography.titleMedium)
                    Text("${masterVolumePercent}%")
                    Slider(
                        value = masterVolumePercent.toFloat(),
                        onValueChange = { onVolumeChange(it.toInt()) },
                        valueRange = 0f..100f,
                    )
                    Text(localized(language, "Handy-Lautstärke insgesamt.", "Overall phone volume."), style = detailStyle)
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Live-Transkription", "Live transcription"), style = MaterialTheme.typography.titleMedium)
                    Text(transcriptionStatus, style = detailStyle)
                    AutoScrollingTranscriptPanel(
                        text = transcriptText.ifBlank {
                            localized(language, "Noch kein erkannter Text", "No recognized text yet")
                        },
                        textScale = textScale,
                    )
                }
            }
        }
    }
}

@Composable
private fun HearingTestScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    textScale: AppTextScale,
    currentResult: HearingTestResult,
    testState: HearingTestUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReact: () -> Unit,
    onSaveRecommended: () -> Unit,
) {
    val detailStyle = detailTextStyle(textScale)
    val displayedResult = if (
        testState.result.leftThresholdByFrequency.isNotEmpty() ||
        testState.result.rightThresholdByFrequency.isNotEmpty()
    ) {
        testState.result
    } else {
        currentResult
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Getrennter Hörtest links / rechts", "Separated hearing test left / right"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        localized(
                            language,
                            "Der Test prüft das linke und rechte Ohr getrennt. Zusätzlich wird eine Gesamtkurve aus beiden Seiten als grobe Gesamtansicht berechnet.",
                            "The test checks the left and right ear separately. An overall curve is also derived from both sides as a rough overall view.",
                        ),
                        style = detailStyle,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStart, enabled = !testState.isRunning) {
                            Text(localized(language, "Hörtest starten", "Start hearing test"))
                        }
                        Button(onClick = onStop, enabled = testState.isRunning) {
                            Text(localized(language, "Hörtest stoppen", "Stop hearing test"))
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Teststatus", "Test status"), style = MaterialTheme.typography.titleMedium)
                    Text(testState.status.ifBlank { localized(language, "Noch kein Test aktiv", "No test running yet") }, style = detailStyle)
                    Text(localized(language, "Fortschritt", "Progress") + ": ${testState.completedUnits} / ${testState.totalUnits}", style = detailStyle)
                    testState.currentEar?.let {
                        Text(localized(language, "Aktuelles Ohr", "Current ear") + ": ${earLabel(it, language)}", style = detailStyle)
                    }
                    testState.currentFrequencyHz?.let {
                        Text(localized(language, "Aktuelle Frequenz", "Current frequency") + ": $it Hz", style = detailStyle)
                    }
                    testState.currentLevelPercent?.let {
                        Text(localized(language, "Aktuelle Lautstärke", "Current loudness") + ": $it%", style = detailStyle)
                    }
                    testState.lastReactionMs?.let {
                        Text(localized(language, "Letzte Reaktionszeit", "Last reaction time") + ": ${it} ms", style = detailStyle)
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(localized(language, "Reaktionsfeld", "Reaction field"), style = MaterialTheme.typography.titleMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (testState.canReact) Color(0xFFD32F2F) else Color(0xFF8D8D8D),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(10.dp),
                    ) {
                        Button(
                            onClick = onReact,
                            enabled = testState.canReact,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFD32F2F),
                                disabledContentColor = Color.White.copy(alpha = 0.88f),
                            ),
                    ) {
                            Text(localized(language, "TON HÖRBAR", "TONE HEARD"), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        localized(
                            language,
                            "Der rote Button bleibt statisch an derselben Stelle. Bitte sofort drücken, sobald du den Ton wahrnimmst.",
                            "The red button stays fixed in the same place. Press it immediately once you perceive the tone.",
                        ),
                        style = detailStyle,
                    )
                }
            }
        }

        item {
            EarCurveCard(
                title = localized(language, "Linkes Ohr", "Left ear"),
                values = displayedResult.leftThresholdByFrequency,
                language = language,
            )
        }
        item {
            EarCurveCard(
                title = localized(language, "Rechtes Ohr", "Right ear"),
                values = displayedResult.rightThresholdByFrequency,
                language = language,
            )
        }
        item {
            EarCurveCard(
                title = localized(language, "Gesamtkurve", "Overall curve"),
                values = displayedResult.overallThresholdByFrequency(),
                language = language,
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSaveRecommended,
                enabled = displayedResult.leftThresholdByFrequency.isNotEmpty() || displayedResult.rightThresholdByFrequency.isNotEmpty(),
            ) {
                Text(localized(language, "Empfohlenes Profil speichern", "Save recommended profile"))
            }
        }
    }
}

@Composable
private fun EarCurveCard(
    title: String,
    values: Map<Int, Int>,
    language: AppLanguage,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (values.isEmpty()) {
                Text(localized(language, "Noch keine Werte vorhanden", "No values yet"))
            } else {
                HearingCurveChart(values = values, language = language, modifier = Modifier.fillMaxWidth().height(220.dp))
                HorizontalDivider()
                values.toSortedMap().forEach { (frequency, db) ->
                    Text("$frequency Hz: $db dB")
                }
            }
        }
    }
}

@Composable
private fun LiveAudioScreen(
    modifier: Modifier = Modifier,
    profile: HearingProfile,
    runtimeSettings: AudioRuntimeSettings,
    language: AppLanguage,
    textScale: AppTextScale,
    micPermissionGranted: Boolean,
    bluetoothPermissionGranted: Boolean,
    micLevel: Float,
    isMonitoring: Boolean,
    isLiveAudioRunning: Boolean,
    isTranscribing: Boolean,
    audioStatus: String,
    currentRoute: String,
    bluetoothAvailable: Boolean,
    outputs: List<AudioOutputDevice>,
    transcriptionStatus: String,
    transcriptText: String,
    onRequestPermission: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onToggleMonitoring: () -> Unit,
    onToggleLiveAudio: () -> Unit,
    onToggleTranscription: () -> Unit,
    onApplyRoute: (AudioRoutePreference) -> Unit,
) {
    val detailStyle = detailTextStyle(textScale)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Live-Audio", "Live audio"), style = MaterialTheme.typography.titleMedium)
                    Text(audioStatus, style = detailStyle)
                    Text(localized(language, "Aktive Route", "Current route") + ": $currentRoute", style = detailStyle)
                    Text(localized(language, "Aktives Profil", "Active profile") + ": ${presetLabel(profile.preset, language)}", style = detailStyle)
                    if (!micPermissionGranted) {
                        Button(onClick = onRequestPermission) {
                            Text(localized(language, "Mikrofon freigeben", "Grant microphone"))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onToggleMonitoring) {
                                Text(if (isMonitoring) localized(language, "Monitor stoppen", "Stop monitor") else localized(language, "Monitor starten", "Start monitor"))
                            }
                            Button(onClick = onToggleLiveAudio) {
                                Text(if (isLiveAudioRunning) localized(language, "Live Assist stoppen", "Stop Live Assist") else localized(language, "Live Assist starten", "Start Live Assist"))
                            }
                        }
                    }
                    LinearProgressIndicator(progress = { micLevel.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Live-Transkription", "Live transcription"), style = MaterialTheme.typography.titleMedium)
                    Text(transcriptionStatus, style = detailStyle)
                    if (runtimeSettings.liveTranscriptionEnabled && micPermissionGranted) {
                        Button(onClick = onToggleTranscription) {
                            Text(if (isTranscribing) localized(language, "Transkription stoppen", "Stop transcription") else localized(language, "Transkription starten", "Start transcription"))
                        }
                    }
                    AutoScrollingTranscriptPanel(
                        text = transcriptText.ifBlank {
                            localized(language, "Noch kein erkannter Text", "No recognized text yet")
                        },
                        textScale = textScale,
                    )
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Audio-Routing", "Audio routing"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioRoutePreference.entries.forEach { route ->
                            FilterChip(
                                selected = runtimeSettings.preferredRoute == route,
                                onClick = { onApplyRoute(route) },
                                label = { Text(routeLabel(route, language)) },
                            )
                        }
                    }
                    Text(if (bluetoothAvailable) localized(language, "Bluetooth-Ausgabe erkannt", "Bluetooth output detected") else localized(language, "Aktuell kein Bluetooth-Audiogerät erkannt", "No Bluetooth audio device detected right now"))
                    if (!bluetoothPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Button(onClick = onRequestBluetoothPermission) {
                            Text(localized(language, "Bluetooth freigeben", "Grant Bluetooth"))
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Gefundene Ausgänge", "Detected outputs"), style = MaterialTheme.typography.titleMedium)
                    outputs.forEach { output -> Text("• ${output.name}") }
                }
            }
        }
    }
}

@Composable
private fun AssistantScreen(
    modifier: Modifier = Modifier,
    settings: GatewaySettings,
    profile: HearingProfile,
    language: AppLanguage,
    enabled: Boolean,
    onSettingsChange: (GatewaySettings) -> Unit,
    onAsk: (String, Boolean, (String) -> Unit) -> Unit,
    onTestGateway: ((String) -> Unit) -> Unit,
) {
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf(localized(language, "Hier erscheinen KI-Antworten oder Übersetzungen.", "AI answers or translations will appear here.")) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!enabled) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(localized(language, "KI-Bereich deaktiviert", "AI section disabled"), style = MaterialTheme.typography.titleMedium)
                        Text(localized(language, "Der KI-Assistent ist in den Einstellungen ausgeschaltet.", "The AI assistant is disabled in settings."))
                    }
                }
            }
            return@LazyColumn
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gateway", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GatewayProvider.entries.forEach { provider ->
                            FilterChip(
                                selected = settings.provider == provider,
                                onClick = {
                                    onSettingsChange(
                                        settings.copy(
                                            provider = provider,
                                            requestPath = provider.defaultPath,
                                            model = when (provider) {
                                                GatewayProvider.Ollama -> "llama3.2:latest"
                                                GatewayProvider.OpenClaw -> "openclaw"
                                                GatewayProvider.OpenAiCompatible -> settings.model
                                            },
                                        ),
                                    )
                                },
                                label = { Text(providerLabel(provider, language)) },
                            )
                        }
                    }
                    OutlinedTextField(value = settings.baseUrl, onValueChange = { onSettingsChange(settings.copy(baseUrl = it)) }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = settings.requestPath, onValueChange = { onSettingsChange(settings.copy(requestPath = it)) }, label = { Text(localized(language, "Pfad", "Path")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = settings.model, onValueChange = { onSettingsChange(settings.copy(model = it)) }, label = { Text(localized(language, "Modell", "Model")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = settings.apiKey, onValueChange = { onSettingsChange(settings.copy(apiKey = it)) }, label = { Text("Token / API Key") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = settings.authHeader, onValueChange = { onSettingsChange(settings.copy(authHeader = it)) }, label = { Text("Auth Header") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = settings.authPrefix, onValueChange = { onSettingsChange(settings.copy(authPrefix = it)) }, label = { Text("Auth Prefix") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = settings.customHeaders, onValueChange = { onSettingsChange(settings.copy(customHeaders = it)) }, label = { Text(localized(language, "Zusätzliche Header", "Additional headers")) }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onTestGateway { answer = it } }) {
                        Text(localized(language, "Gateway testen", "Test gateway"))
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(localized(language, "KI-Hilfe", "AI helper"), style = MaterialTheme.typography.titleMedium)
                    Text(localized(language, "Aktives Profil", "Active profile") + ": ${presetLabel(profile.preset, language)}")
                    OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text(localized(language, "Nachricht oder Satz", "Message or sentence")) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAsk(prompt, false) { answer = it } }, enabled = prompt.isNotBlank()) {
                            Text(localized(language, "Chat", "Chat"))
                        }
                        Button(onClick = { onAsk(prompt, true) { answer = it } }, enabled = prompt.isNotBlank()) {
                            Text(localized(language, "Übersetzen", "Translate"))
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized(language, "Antwort", "Answer"), style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(answer)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    runtimeSettings: AudioRuntimeSettings,
    maintenanceSettings: MaintenanceSettings,
    maintenanceStatus: String,
    language: AppLanguage,
    savedProfiles: List<SavedProfile>,
    selectedProfileId: String,
    onSelectProfile: (SavedProfile) -> Unit,
    onToggleEnhancedDsp: (Boolean) -> Unit,
    onToggleLowLatencyMode: (Boolean) -> Unit,
    onToggleRealTimeHeadsetDsp: (Boolean) -> Unit,
    onToggleBluetoothLeAudioOptimization: (Boolean) -> Unit,
    onToggleNativeLowLatencyPipeline: (Boolean) -> Unit,
    onToggleAdvancedDspFilters: (Boolean) -> Unit,
    onToggleStreamingCaptionMode: (Boolean) -> Unit,
    onToggleBluetoothCompatibilityMode: (Boolean) -> Unit,
    onNoiseSuppressionModeChange: (NoiseSuppressionMode) -> Unit,
    onToggleMediaMixMode: (Boolean) -> Unit,
    onToggleNormalization: (Boolean) -> Unit,
    onToggleTranscription: (Boolean) -> Unit,
    onToggleAssistant: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onTextScaleChange: (AppTextScale) -> Unit,
    onMaintenanceChange: (MaintenanceSettings) -> Unit,
    onSendErrorReport: () -> Unit,
    onClearErrorReport: () -> Unit,
    onCheckForUpdate: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Profile auswählen", "Choose profile"), style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedProfiles.chunked(2).forEach { rowProfiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowProfiles.forEach { profile ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        FilterChip(
                                            selected = profile.id == selectedProfileId,
                                            onClick = { onSelectProfile(profile) },
                                            label = { Text(profile.name) },
                                        )
                                    }
                                }
                                if (rowProfiles.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Schriftgröße", "Text size"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextScale.entries.forEach { scale ->
                            FilterChip(
                                selected = runtimeSettings.textScale == scale,
                                onClick = { onTextScaleChange(scale) },
                                label = { Text(textScaleLabel(scale, language)) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Design", "Design"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = runtimeSettings.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(themeModeLabel(mode, language)) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Sprache", "Language"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { appLanguage ->
                            FilterChip(
                                selected = runtimeSettings.appLanguage == appLanguage,
                                onClick = { onLanguageChange(appLanguage) },
                                label = { Text(languageLabel(appLanguage)) },
                            )
                        }
                    }
                    Text(
                        localized(
                            language,
                            "System nutzt die Gerätesprache automatisch. Deutsch und English überschreiben das Verhalten gezielt.",
                            "System follows the device language automatically. Deutsch and English override it explicitly.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Stärkerer DSP / EQ", "Stronger DSP / EQ"),
                description = localized(language, "Mehr Sprachbetonung und stärkere Filterung für Live Assist.", "More speech emphasis and stronger filtering for Live Assist."),
                impact = localized(language, "Auswirkung: kann Sprache klarer machen, aber auch künstlicher klingen.", "Effect: can make speech clearer, but also more artificial."),
                checked = runtimeSettings.enhancedDspEnabled,
                onCheckedChange = onToggleEnhancedDsp,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Niedrige Latenz testen", "Test low latency"),
                description = localized(language, "Aktiviert kleinere Audiopuffer und schnellere Wiedergabe für Live Assist.", "Enables smaller audio buffers and faster playback for Live Assist."),
                impact = localized(language, "Auswirkung: meist deutlich weniger Echo-Verzögerung, kann je nach Handy aber instabiler werden.", "Effect: usually much less echo-like delay, but may become less stable depending on the phone."),
                checked = runtimeSettings.lowLatencyModeEnabled,
                onCheckedChange = onToggleLowLatencyMode,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Echtzeit Mic -> Headset DSP", "Real-time mic -> headset DSP"),
                description = localized(language, "Schaltet die eigentliche Mikrofon-DSP-Headset-Strecke fuer Live Assist frei.", "Enables the actual microphone-DSP-headset path for Live Assist."),
                impact = localized(language, "Auswirkung: aus bedeutet, dass Live Assist nicht als Durchhoerhilfe startet.", "Effect: off means Live Assist will not start as a pass-through hearing aid."),
                checked = runtimeSettings.realTimeHeadsetDspEnabled,
                onCheckedChange = onToggleRealTimeHeadsetDsp,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Native Low-Latency Pipeline", "Native low-latency pipeline"),
                description = localized(language, "Bereitet den spaeteren Oboe/C++-Audiopfad vor und nutzt aktuell noch kleinere Frames.", "Prepares the later Oboe/C++ audio path and currently uses even smaller frames."),
                impact = localized(language, "Auswirkung: kann weniger Echo erzeugen, braucht aber Alltagstests pro Handy.", "Effect: can reduce echo, but needs real-device testing per phone."),
                checked = runtimeSettings.nativeLowLatencyPipelineEnabled,
                onCheckedChange = onToggleNativeLowLatencyPipeline,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Bluetooth-LE-Audio optimieren", "Optimize Bluetooth LE Audio"),
                description = localized(language, "Merkt sich einen experimentellen LE-Audio-Modus fuer kuenftige Headset-Feinabstimmung.", "Stores an experimental LE Audio mode for future headset tuning."),
                impact = localized(language, "Auswirkung: aktuell Konfigurationsschalter, spaeter Grundlage fuer LE-spezifisches Routing.", "Effect: currently a configuration switch, later the basis for LE-specific routing."),
                checked = runtimeSettings.bluetoothLeAudioOptimizationEnabled,
                onCheckedChange = onToggleBluetoothLeAudioOptimization,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Medien parallel zulassen", "Allow parallel media"),
                description = localized(language, "Versucht Musik und andere Audio-Apps weniger zu stören, während Hörhilfe oder Monitor aktiv sind.", "Tries to disturb music and other audio apps less while hearing aid or monitor are active."),
                impact = localized(language, "Auswirkung: bessere Koexistenz mit Musik-Apps, aber Routing kann etwas konservativer werden.", "Effect: better coexistence with music apps, but routing may behave a bit more conservatively."),
                checked = runtimeSettings.mediaMixModeEnabled,
                onCheckedChange = onToggleMediaMixMode,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Bluetooth-Kompatibilitaet", "Bluetooth compatibility"),
                description = localized(language, "Konservativeres Verhalten fuer unterschiedliche Headset-Hersteller im Alltag.", "More conservative behavior for different headset vendors in daily use."),
                impact = localized(language, "Auswirkung: weniger aggressives Routing, dafuer stabilere Koexistenz mit Medien.", "Effect: less aggressive routing, but more stable coexistence with media."),
                checked = runtimeSettings.bluetoothCompatibilityModeEnabled,
                onCheckedChange = onToggleBluetoothCompatibilityMode,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Echter EQ, Kompressor, Feedback-Schutz", "Real EQ, compressor, feedback protection"),
                description = localized(language, "Schaltet die staerkere DSP-Kette mit Kompressor und einfachem Feedback-Schutz.", "Enables the stronger DSP chain with compressor and simple feedback protection."),
                impact = localized(language, "Auswirkung: mehr Sprachkontrolle, kann aber je nach Geraet kuenstlicher klingen.", "Effect: more speech control, but may sound more artificial depending on the device."),
                checked = runtimeSettings.advancedDspFiltersEnabled,
                onCheckedChange = onToggleAdvancedDspFilters,
            )
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Rauschunterdrueckung", "Noise suppression"), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NoiseSuppressionMode.entries.forEach { mode ->
                            FilterChip(
                                selected = runtimeSettings.noiseSuppressionMode == mode,
                                onClick = { onNoiseSuppressionModeChange(mode) },
                                label = { Text(noiseSuppressionLabel(mode)) },
                            )
                        }
                    }
                    Text(
                        localized(
                            language,
                            "RNNoise, DeepFilterNet und Beamforming sind als waehlebare Betriebsarten vorbereitet; echte native Modelle muessen spaeter als Bibliothek eingebunden werden.",
                            "RNNoise, DeepFilterNet, and beamforming are prepared as selectable modes; real native models need to be added later as libraries.",
                        ),
                        style = detailTextStyle(runtimeSettings.textScale),
                    )
                }
            }
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Mikrofon normalisieren", "Normalize microphone"),
                description = localized(language, "Glättet leise und laute Mikrofonpegel für eine gleichmäßigere Hörhilfe.", "Smooths quiet and loud microphone levels for a more even hearing aid feed."),
                impact = localized(language, "Auswirkung: Sprache wird konstanter, kann aber etwas weniger natürlich klingen.", "Effect: speech becomes more consistent, but may sound slightly less natural."),
                checked = runtimeSettings.microphoneNormalizationEnabled,
                onCheckedChange = onToggleNormalization,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Streaming-Transkription", "Streaming transcription"),
                description = localized(language, "Nutzt Android-Spracherkennung, um Gesprochenes mitzuschreiben.", "Uses Android speech recognition to transcribe spoken content."),
                impact = localized(language, "Auswirkung: zusätzliche Hilfe beim Verstehen, aber mehr Akkuverbrauch.", "Effect: extra help with understanding, but more battery use."),
                checked = runtimeSettings.liveTranscriptionEnabled,
                onCheckedChange = onToggleTranscription,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "Streaming-Untertitel", "Streaming captions"),
                description = localized(language, "Zeigt Teilresultate der Spracherkennung direkt waehrend des Sprechens.", "Shows partial speech recognition results while speaking."),
                impact = localized(language, "Auswirkung: schneller lesbar, aber einzelne Woerter koennen sich waehrend der Erkennung noch aendern.", "Effect: faster to read, but individual words may still change during recognition."),
                checked = runtimeSettings.streamingCaptionModeEnabled,
                onCheckedChange = onToggleStreamingCaptionMode,
            )
        }

        item {
            FeatureToggleCard(
                title = localized(language, "KI-Assistent", "AI assistant"),
                description = localized(language, "Aktiviert Gateway-Test, Chat und Übersetzung.", "Enables gateway test, chat, and translation."),
                impact = localized(language, "Auswirkung: mehr Hilfsfunktionen, aber nur sinnvoll mit erreichbarem Gateway.", "Effect: more helper features, but only useful with a reachable gateway."),
                checked = runtimeSettings.assistantEnabled,
                onCheckedChange = onToggleAssistant,
            )
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Fehlerprotokoll", "Error log"), style = MaterialTheme.typography.titleMedium)
                    Text(localized(language, "Produktkennung", "Product id") + ": ${maintenanceSettings.productId}", style = detailTextStyle(runtimeSettings.textScale))
                    OutlinedTextField(
                        value = maintenanceSettings.errorReportEmail,
                        onValueChange = { onMaintenanceChange(maintenanceSettings.copy(errorReportEmail = it.trim())) },
                        label = { Text(localized(language, "Zieladresse", "Recipient")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSendErrorReport) {
                            Text(localized(language, "Protokoll senden", "Send log"))
                        }
                        Button(onClick = onClearErrorReport) {
                            Text(localized(language, "Protokoll leeren", "Clear log"))
                        }
                    }
                    Text(
                        localized(
                            language,
                            "SMTP-Passwoerter werden nicht in der App gespeichert. Der Versand nutzt den Mail-Client des Geraets.",
                            "SMTP passwords are not stored in the app. Sending uses the device mail client.",
                        ),
                        style = detailTextStyle(runtimeSettings.textScale),
                    )
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(min = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized(language, "Update-Pruefung", "Update check"), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = maintenanceSettings.updateManifestUrl,
                        onValueChange = { onMaintenanceChange(maintenanceSettings.copy(updateManifestUrl = it.trim())) },
                        label = { Text(localized(language, "Alpha Update-URL", "Alpha update URL")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = onCheckForUpdate) {
                        Text(localized(language, "Update pruefen", "Check update"))
                    }
                    Text(maintenanceStatus, style = detailTextStyle(runtimeSettings.textScale))
                    Text(
                        localized(
                            language,
                            "Fuer Alpha-Versionen reicht eine kleine JSON-Datei auf VPS oder GitHub mit productId, versionCode, versionName und downloadUrl.",
                            "For alpha versions, a small JSON file on VPS or GitHub with productId, versionCode, versionName, and downloadUrl is enough.",
                        ),
                        style = detailTextStyle(runtimeSettings.textScale),
                    )
                }
            }
        }
    }
}

@Composable
private fun HearingCurveChart(
    values: Map<Int, Int>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val points = values.keys.sorted()
    if (points.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxDb = 60f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(modifier = modifier) {
            val leftPad = 42f
            val rightPad = 12f
            val topPad = 16f
            val bottomPad = 24f
            val chartWidth = size.width - leftPad - rightPad
            val chartHeight = size.height - topPad - bottomPad

            for (db in 0..6) {
                val value = db * 10f
                val y = topPad + (value / maxDb) * chartHeight
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f,
                )
            }

            val xStep = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth / 2f
            val path = Path()
            points.forEachIndexed { index, frequency ->
                val dbValue = values[frequency]?.toFloat() ?: return@forEachIndexed
                val x = leftPad + index * xStep
                val y = topPad + ((dbValue / maxDb).coerceIn(0f, 1f) * chartHeight)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = lineColor, radius = 6.5f, center = Offset(x, y))
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { frequency ->
                Text(
                    text = frequency.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
            }
        }
        Text(
            localized(language, "Frequenzen in Hz, Schwellenwerte 0 bis 60 dB.", "Frequencies in Hz, thresholds 0 to 60 dB."),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

@Composable
private fun FeatureToggleCard(
    title: String,
    description: String,
    impact: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 148.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
            Text(description)
            Text(impact, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AutoScrollingTranscriptPanel(
    text: String,
    textScale: AppTextScale,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(12.dp)
                .verticalScroll(scrollState),
        ) {
            Text(text, style = detailTextStyle(textScale))
        }
    }
}

private fun resolveAppLanguage(language: AppLanguage): AppLanguage = when (language) {
    AppLanguage.System -> {
        val defaultLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
        if (defaultLanguage.startsWith("de")) AppLanguage.German else AppLanguage.English
    }
    else -> language
}

private fun localized(language: AppLanguage, german: String, english: String): String {
    return if (language == AppLanguage.German) german else english
}

private fun presetLabel(preset: AudioPreset, language: AppLanguage): String = when (preset) {
    AudioPreset.Conversation -> localized(language, "Gespräch", "Conversation")
    AudioPreset.Outdoor -> localized(language, "Draußen", "Outdoor")
    AudioPreset.Tv -> "TV"
    AudioPreset.Music -> localized(language, "Musik", "Music")
}

private fun providerLabel(provider: GatewayProvider, language: AppLanguage): String = when (provider) {
    GatewayProvider.OpenClaw -> "OpenClaw Proxy"
    GatewayProvider.Ollama -> "Ollama Proxy"
    GatewayProvider.OpenAiCompatible -> localized(language, "OpenAI-kompatibel", "OpenAI compatible")
}

private fun themeModeLabel(mode: ThemeMode, language: AppLanguage): String = when (mode) {
    ThemeMode.System -> localized(language, "System", "System")
    ThemeMode.Light -> localized(language, "Hell", "Light")
    ThemeMode.Dark -> localized(language, "Dunkel", "Dark")
}

private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.System -> "System"
    AppLanguage.German -> "Deutsch"
    AppLanguage.English -> "English"
}

private fun textScaleLabel(scale: AppTextScale, language: AppLanguage): String = when (scale) {
    AppTextScale.Small -> localized(language, "Klein", "Small")
    AppTextScale.Default -> localized(language, "Standard", "Default")
    AppTextScale.Large -> localized(language, "Groß", "Large")
}

private fun earLabel(ear: EarSide, language: AppLanguage): String = when (ear) {
    EarSide.Left -> localized(language, "Links", "Left")
    EarSide.Right -> localized(language, "Rechts", "Right")
}

private fun routeLabel(route: AudioRoutePreference, language: AppLanguage): String = when (route) {
    AudioRoutePreference.Default -> localized(language, "Auto", "Auto")
    AudioRoutePreference.Speaker -> localized(language, "Lautsprecher", "Speaker")
    AudioRoutePreference.Bluetooth -> "Bluetooth"
}

private fun noiseSuppressionLabel(mode: NoiseSuppressionMode): String = when (mode) {
    NoiseSuppressionMode.Off -> "Aus"
    NoiseSuppressionMode.Basic -> "Basic"
    NoiseSuppressionMode.RNNoise -> "RNNoise"
    NoiseSuppressionMode.DeepFilterNet -> "DeepFilterNet"
    NoiseSuppressionMode.Beamforming -> "Beamforming"
}

private fun homeProfileLabel(profile: SavedProfile, language: AppLanguage): String {
    val compactName = profile.name.removePrefix("Standard ").removePrefix("Standard")
    return if (compactName.isBlank()) localized(language, "Profil", "Profile") else compactName
}

@Composable
private fun detailTextStyle(scale: AppTextScale): TextStyle = when (scale) {
    AppTextScale.Small -> MaterialTheme.typography.labelSmall
    AppTextScale.Default -> MaterialTheme.typography.bodySmall
    AppTextScale.Large -> MaterialTheme.typography.bodyMedium
}

@Composable
private fun metaTextStyle(scale: AppTextScale): TextStyle = when (scale) {
    AppTextScale.Small -> MaterialTheme.typography.labelSmall
    AppTextScale.Default -> MaterialTheme.typography.labelMedium
    AppTextScale.Large -> MaterialTheme.typography.bodySmall
}
