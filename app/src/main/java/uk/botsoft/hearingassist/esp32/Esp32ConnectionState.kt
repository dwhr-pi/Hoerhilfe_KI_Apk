package uk.botsoft.hearingassist.esp32

enum class Esp32ConnectionState {
    Disabled,
    Searching,
    Connected,
    Error,
}

data class Esp32GatewayConfig(
    val enabled: Boolean = false,
    val deviceNamePrefix: String = "HoerhilfeKI",
    val profileSyncEnabled: Boolean = false,
    val remoteButtonEnabled: Boolean = true,
)
