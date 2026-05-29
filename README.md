# Höhrhilfe KI

Experimentelle Open-Source-Hörassistenz für Android mit Profilverwaltung, Audio-Dashboard, Hörtest, Live-Transkription und vorbereiteter KI-Gateway-Anbindung.

Wichtiger medizinischer Hinweis: Keine medizinische Diagnose, kein Ersatz für HNO-Arzt, Hörakustiker oder zugelassenes Hörgerät. Die App ist nicht medizinisch zertifiziert und darf nicht als Medizinprodukt beworben werden. Nutzung auf eigene Verantwortung.

## Projektidee

Aus dem Android-MVP „Hearing Assist KI Android“ entsteht eine offene Test-App für bessere Alltagshörbarkeit, Profile, Transkription und spätere Experimente mit Bluetooth, ESP32 und lokalen KI-Gateways wie Ollama/OpenClaw.

## Screenshots

Screenshots folgen. Vorgesehen sind:

- Start-Dashboard mit Profilen, Lautstärke und Live-Transkript.
- Hörtest mit linker, rechter und Gesamt-Kurve.
- Live-Ansicht für Monitor, Live Assist und Transkription.
- Setup mit Datenschutz, Audio, KI, Sprache und Dark Mode.

## Funktionen

- Kotlin + Jetpack Compose.
- Paketname `uk.botsoft.hearingassist`.
- minSdk 26, targetSdk/compileSdk 34.
- Audio-Dashboard mit Profilverwaltung.
- Vorgabeprofile: Gespräch, Draußen, TV und Musik.
- Adaptiver Hörtest mit synthetischen Sinus-Tönen.
- Getrennte Kurven für linkes und rechtes Ohr plus Gesamtkurve.
- Live-Audio-Grundlage mit `AudioRecord`/`AudioTrack`.
- Live-Transkription über Android-Spracherkennung.
- Setup-Schalter für Dark Mode, Sprache, Schriftgröße, DSP, Transkription und KI.
- Schwarz/orange Design angelehnt an die myExplorer-App: Orange `#FF7A18`, Amber `#FFAA33`, Dunkelgrau/Schwarz.
- OpenAI-kompatibles Gateway, Ollama/OpenClaw-Pfade vorbereitet.
- Fehlerprotokoll per Mail-Intent und Alpha-Update-Manifest vorbereitet.
- Datenschutzbereich im Setup für Verlauf löschen, Profile löschen, Gateway zurücksetzen und Daten exportieren.

## Was funktioniert aktuell?

- Die App kann lokal gebaut und als APK getestet werden.
- Profile können ausgewählt, angepasst und gespeichert werden.
- Der Hörtest erzeugt die Töne direkt per `AudioTrack`; es fehlen keine Audiodateien.
- Der Hörtest enthält aktuell diese Frequenzen:

`125, 250, 500, 750, 1000, 1250, 1500, 1750, 2000, 2250, 2500, 2750, 3000, 3250, 3500, 3750, 4000, 4250, 4500, 4750, 5000, 5250, 5500, 5750, 6000, 6250, 6500, 6750, 7000, 7250, 7500, 7750, 8000 Hz`

- Live Assist, Monitor und Transkription werden beim Hörteststart pausiert, damit der Test sich nicht selbst stört.
- Ungenau oder gar nicht gehörte Frequenzen werden begrenzt wiederholt und dann als „nicht sicher gehört“ gewertet, statt endlos hängen zu bleiben.

## Was ist geplant?

- Native Low-Latency-Pipeline mit Oboe/C++.
- Messbare Latenztests pro Gerät und Headset.
- Echte DSP-Kette mit EQ, Kompressor, Limiter und Feedback-Schutz.
- KI-Transkription über lokale oder OpenAI-kompatible Gateways.
- ESP32 als Fernbedienung, Profilumschalter oder experimentelles Gateway.
- Vollständiges Onboarding und Datenexport/Löschen.

## Installation auf dem Handy

1. Auf dem Android-Handy `Einstellungen > Über das Telefon` öffnen.
2. Mehrfach auf `Build-Nummer` tippen, bis Entwickleroptionen aktiv sind.
3. In den Entwickleroptionen `USB-Debugging` aktivieren.
4. Handy per USB verbinden und die Debugging-Rückfrage bestätigen.
5. APK installieren oder direkt aus Android Studio starten.

Android Studio muss installiert sein, aber nicht gestartet sein, damit die App über die Kommandozeile gebaut werden kann.

## Build

Voraussetzungen:

- Android Studio mit Android SDK.
- Java 17, zum Beispiel die von Android Studio installierte Runtime.
- Gradle Wrapper oder lokal bereitgestellte Gradle-Version.

Debug-Build mit installiertem Gradle oder Android-Studio-Gradle:

```powershell
gradle assembleDebug
```

Falls auf deinem Rechner zusätzlich ein lokales Gradle unter `tools/gradle-8.7` liegt:

```powershell
.\tools\gradle-8.7\bin\gradle.bat assembleDebug
```

Unit Tests:

```powershell
gradle testDebugUnitTest
```

Release-Build:

```powershell
.\tools\generate-release-keystore.ps1
.\tools\build-release.cmd
```

Keystore-Dateien, `release-signing.properties`, lokale Build-Ausgaben und Logs gehören nicht ins Repo.

## KI-Gateway

Die App ist für OpenAI-kompatible Gateways und lokale Ollama/OpenClaw-Setups vorbereitet.

Beispiele:

- OpenAI-kompatibel: `/v1/chat/completions`
- OpenClaw Proxy: `/openclaw/v1/chat/completions`
- Ollama Proxy: `/ollama/api/chat`

Details stehen in [docs/KI_GATEWAY.md](docs/KI_GATEWAY.md).

## ESP32 Remote

ESP32-Unterstützung ist als Remote-Konzept vorbereitet, aber noch keine echte Geräteverbindung. Geplant sind Profilumschaltung, Lauter/Leiser, Mute, Statusanzeige und optional ein Hörtest-Reaktionsbutton.

Details stehen in [docs/ESP32_REMOTE.md](docs/ESP32_REMOTE.md).

## Datenschutz

- Mikrofon ist sensibel.
- Keine heimliche Aufnahme.
- Cloud nur mit Zustimmung.
- Lokaler Modus bevorzugt.
- Keine Audio-Speicherung als Standard.
- Fehlerberichte dürfen keine Passwörter, API-Keys oder privaten Daten enthalten.

Details stehen in [docs/DATENSCHUTZ_UND_SICHERHEIT.md](docs/DATENSCHUTZ_UND_SICHERHEIT.md).

## Roadmap

- Level 1: sauberes MVP und stabiler Build.
- Level 2: echte Audioaufnahme + Playback.
- Level 3: DSP-Profile mit EQ, Kompressor und Schutzlogik.
- Level 4: KI offline/online mit Datenschutz-Schaltern.
- Level 5: Bluetooth, ESP32 und Cloud-Gateway.
- Level 6: experimentelle Hörassistenz mit ehrlicher Dokumentation.

Mehr Details stehen in [docs/REPO_BEWERTUNG.md](docs/REPO_BEWERTUNG.md).

## Troubleshooting Android Studio

- Wenn das Handy nicht erkannt wird: Datenkabel statt Ladekabel verwenden.
- USB-Modus am Handy auf Dateiübertragung stellen.
- Hersteller-USB-Treiber installieren, falls Windows das Gerät nicht erkennt.
- Bei Buildproblemen zuerst Gradle Sync in Android Studio laufen lassen.
- Wenn Benachrichtigungen den Hörtest stören: vor dem Test „Nicht stören“ am Handy aktivieren.
- Für den Hörtest Live Assist, Monitor und Transkription nicht parallel verwenden. Die App pausiert diese Funktionen beim Start automatisch.

## Mitmachen

Beiträge sind willkommen, besonders bei:

- Geräte- und Headset-Testberichten.
- DSP-Verbesserungen.
- Dokumentation.
- Datenschutzfreundlicher KI-Gateway-Anbindung.
- ESP32-Experimenten.

## Lizenz

Bitte Lizenzdatei ergänzen oder bestätigen. Bis dahin sollte das Projekt nicht ohne klare Lizenzannahme weiterveröffentlicht werden.
