package uk.botsoft.hearingassist.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import uk.botsoft.hearingassist.data.AudioRoutePreference

data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val isSink: Boolean,
)

class AudioDeviceRouter(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun listOutputs(): List<AudioOutputDevice> {
        return availableOutputDevices().map { device ->
            AudioOutputDevice(
                id = device.id,
                name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: device.typeLabel(),
                isSink = device.isSink,
            )
        }
    }

    fun applyRoute(preference: AudioRoutePreference): String {
        return when (preference) {
            AudioRoutePreference.Default -> {
                clearRouting()
                audioManager.mode = AudioManager.MODE_NORMAL
                "Automatische Ausgabe aktiv"
            }

            AudioRoutePreference.Speaker -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                routeToDevice(
                    preferredTypes = intArrayOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                    fallback = {
                        @Suppress("DEPRECATION")
                        audioManager.isSpeakerphoneOn = true
                    },
                )
                "Lautsprecher aktiviert"
            }

            AudioRoutePreference.Bluetooth -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                val routed = routeToDevice(
                    preferredTypes = intArrayOf(
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    ),
                    fallback = {
                        @Suppress("DEPRECATION")
                        runCatching { audioManager.startBluetoothSco() }
                        @Suppress("DEPRECATION")
                        runCatching { audioManager.isSpeakerphoneOn = false }
                    },
                )
                if (routed) "Bluetooth-Audio aktiviert" else "Kein Bluetooth-Audiogerät gefunden"
            }
        }
    }

    fun currentRouteSummary(): String {
        val communicationDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice
        } else {
            null
        }
        val active = communicationDevice ?: availableOutputDevices().firstOrNull { it.type.isBluetoothType() }
        return when {
            active?.type?.isBluetoothType() == true -> active.productName?.toString()?.ifBlank { "Bluetooth" } ?: "Bluetooth"
            communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Lautsprecher"
            isSpeakerEnabled() -> "Lautsprecher"
            else -> "Standard / Headset"
        }
    }

    fun bluetoothAvailable(): Boolean {
        return availableOutputDevices().any { it.type.isBluetoothType() }
    }

    fun releaseToSystemMix() {
        clearRouting()
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun availableOutputDevices(): List<AudioDeviceInfo> {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { it.isSink }
    }

    private fun clearRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.mode = AudioManager.MODE_NORMAL
        @Suppress("DEPRECATION")
        runCatching { audioManager.stopBluetoothSco() }
        @Suppress("DEPRECATION")
        runCatching { audioManager.isSpeakerphoneOn = false }
    }

    private fun routeToDevice(preferredTypes: IntArray, fallback: () -> Unit): Boolean {
        clearRouting()
        val target = availableOutputDevices().firstOrNull { device -> preferredTypes.any { it == device.type } }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && target != null) {
            return audioManager.setCommunicationDevice(target)
        }
        fallback()
        return target != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }

    private fun isSpeakerEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    private fun AudioDeviceInfo.typeLabel(): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE Headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE Lautsprecher"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Gerätelautsprecher"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Hörer"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Kabel-Headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Kopfhörer"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB-Headset"
        else -> "Ausgabegerät $type"
    }

    private fun Int.isBluetoothType(): Boolean = this == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        this == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        this == AudioDeviceInfo.TYPE_BLE_HEADSET ||
        this == AudioDeviceInfo.TYPE_BLE_SPEAKER
}
