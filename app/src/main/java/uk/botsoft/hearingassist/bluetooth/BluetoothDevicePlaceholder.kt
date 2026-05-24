package uk.botsoft.hearingassist.bluetooth

data class BluetoothDevicePlaceholder(
    val id: String,
    val displayName: String,
    val supportsClassicAudio: Boolean,
    val supportsBleAudio: Boolean,
    val notes: String = "",
)

class BluetoothDeviceRepository {
    fun knownPlaceholders(): List<BluetoothDevicePlaceholder> = listOf(
        BluetoothDevicePlaceholder(
            id = "system-default",
            displayName = "Android-Systemauswahl",
            supportsClassicAudio = true,
            supportsBleAudio = false,
            notes = "Routing wird aktuell durch Android verwaltet.",
        ),
        BluetoothDevicePlaceholder(
            id = "ble-audio-future",
            displayName = "BLE Audio / LC3 vorbereitet",
            supportsClassicAudio = false,
            supportsBleAudio = true,
            notes = "Erfordert spätere Gerätetests und Android-Versionen mit stabiler BLE-Audio-Unterstützung.",
        ),
    )
}
