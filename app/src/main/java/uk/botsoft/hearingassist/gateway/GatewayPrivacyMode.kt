package uk.botsoft.hearingassist.gateway

data class GatewayPrivacyMode(
    val preferOffline: Boolean = true,
    val allowCloud: Boolean = false,
    val storeAudio: Boolean = false,
    val temporaryStorage: Boolean = false,
) {
    val canSendAudioToCloud: Boolean
        get() = allowCloud && !storeAudio
}
