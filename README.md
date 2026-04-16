# Hearing Assist KI Android

Android-MVP fuer die Idee aus `dwhr-pi/H-rger-te_App_mit_KI`, umgesetzt als importierbares Android-Studio-Projekt in Kotlin + Jetpack Compose.

## Enthalten

- Audio-Dashboard mit anpassbaren Hoerprofilen
- Hoertest light mit Sinus-Toenen und automatischer Profil-Empfehlung
- KI-Tab fuer Chat oder Uebersetzung ueber ein OpenAI-kompatibles Gateway
- Persistenz fuer Profil-, Test- und Gateway-Einstellungen

## Nicht enthalten

- Echtzeit-Mikrofon-zu-Headset-DSP
- medizinische Diagnostik
- Bluetooth-LE-Audio-Optimierung
- RNNoise, DeepFilterNet oder Beamforming

Das ist bewusst ein MVP, der die Produktidee als Android-App nutzbar macht und sauber an die vorhandene KI-Infrastruktur anschliessen kann.

## Build

1. Projektordner `android-hearing-assist` in Android Studio oeffnen.
2. Gradle Sync ausfuehren.
3. App auf Emulator oder Geraet starten.

## Gateway

Der KI-Bereich erwartet ein OpenAI-kompatibles Endpoint-Muster:

- Base URL: `https://dein-host`
- Request: `POST /v1/chat/completions`
- Auth: Bearer API Key optional

## Nächste sinnvolle Schritte

1. Audio-Capture und Low-Latency-Playback per Oboe oder nativer AudioTrack-Pipeline ergaenzen.
2. DSP-Filter in ein eigenes Audio-Modul auslagern.
3. Live-Transkription und Untertitel auf Streaming umstellen.
4. Bluetooth-Headset-Routing und Mikrofonberechtigungen sauber integrieren.
