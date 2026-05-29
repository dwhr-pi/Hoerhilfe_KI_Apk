# Repo-Bewertung

## Medizinischer Hinweis

Diese App ist eine experimentelle Open-Source-Hörassistenz. Sie ist keine medizinische Diagnose, kein Ersatz für HNO-Arzt, Hörakustiker oder zugelassenes Hörgerät. Nutzung und Tests erfolgen auf eigene Verantwortung.

## Aktueller technischer Zustand

Das Repository enthält ein Kotlin-/Jetpack-Compose-Android-Projekt mit Paket `uk.botsoft.hearingassist`, minSdk 26 und target/compileSdk 34. Vorhanden sind ein Dashboard, Profilverwaltung, Live-Assist-Grundlage, Mikrofonmonitor, Android-Spracherkennung, ein KI-Gateway-Client, ein adaptiver Hörtest und lokale Persistenz.

Der lokale Stand ist nicht mehr nur ein README-MVP, sondern bereits eine lauffähige Alpha. Viele Produktideen sind jedoch bewusst erst vorbereitet: echte medizinisch belastbare Audiometrie, native Low-Latency-Audioverarbeitung, BLE-Audio-Optimierung und KI-Audio-Transkription über ein Gateway sind noch nicht produktionsreif.

## Was bereits gut ist

- Klare Compose-App-Struktur mit Tabs für Startseite, Hörtest, Live, KI und Setup.
- Persistente Profile inklusive Vorgaben für Gespräch, Draußen, TV und Musik.
- Runtime-Permissions für Mikrofon und Bluetooth sind vorbereitet.
- Live-Transkription nutzt Android-Spracherkennung statt eigene Cloud-Pflicht.
- Release-Signing ist lokal vorbereitet, ohne Keystore oder Passwörter ins Repo zu legen.
- Der Hörtest trennt linkes und rechtes Ohr und stellt Kurven dar.
- Update- und Fehlerprotokoll-Konzept sind als Alpha-Funktionen vorbereitet.

## Was noch Mock/MVP ist

- Der Hörtest ist ein technischer Reaktionstest, keine validierte Audiometrie.
- RNNoise, DeepFilterNet und Beamforming sind auswählbare Modi, aber noch keine eingebundenen nativen Modelle.
- Bluetooth-LE-Audio ist konzeptionell vorbereitet, aber nicht gerätespezifisch optimiert.
- ESP32-Unterstützung ist aktuell Konzept und Datenstruktur, noch keine echte Verbindung.
- KI-Audio-Transkription über `/v1/audio/transcriptions` ist dokumentiert, aber noch nicht eingebunden.
- Oboe/C++-Low-Latency-Pipeline ist vorbereitet, aber noch nicht implementiert.

## Fehlende Kernfunktionen

- Stabile Echtzeit-Mikrofon-zu-Headset-Pipeline mit messbarer Latenz.
- Geräteabhängiges Bluetooth-Routing und Headset-Kompatibilitätstests.
- Echte DSP-Kette mit parametrischem EQ, Kompressor, Limiter, Feedback-Erkennung und Messwerten.
- Robustes Audio-Fokus-Konzept für parallele Medienwiedergabe.
- Vollständiges Onboarding mit Datenschutz- und Lautstärkewarnungen.
- Export/Löschen aller Nutzerdaten ist in der UI vorbereitet, braucht aber später echte Datei- und Backup-Formate.
- Reproduzierbare CI-Builds sind vorbereitet; lokales Gradle bleibt dokumentiert, darf aber nicht als Build-Artefakt committed werden.

## Risiken

- Latenz: Android-Mikrofon zu Bluetooth-Headset kann schnell als Echo wahrgenommen werden.
- Feedback/Pfeifen: Mikrofon und Lautsprecher/Headset können sich gegenseitig aufschaukeln.
- Datenschutz: Mikrofon- und Transkriptionsdaten sind besonders sensibel.
- Akkuverbrauch: Dauerhafte Aufnahme, DSP und Transkription belasten CPU und Akku.
- Android-Audio-Routing: AudioFocus, Bluetooth und Medienwiedergabe unterscheiden sich stark je Gerät.
- Bluetooth-Latenz: Classic Bluetooth und BLE Audio haben unterschiedliche Verzögerungen und Herstellerprofile.
- Medizinische Einordnung: Die App darf nicht als Medizinprodukt oder zugelassenes Hörgerät beworben werden.

## Roadmap

### Level 1: Sauberes MVP

- Build stabilisieren.
- README, Datenschutz, medizinische Abgrenzung und Testanleitung pflegen.
- Hörtest gegen Endlosschleifen und Störungen absichern.
- Schwarz/orange Design analog zu myExplorer konsolidieren.

### Level 2: Echte Audioaufnahme + Playback

- `AudioEngine` als Interface stabilisieren.
- `AudioRecord`/`AudioTrack` messbar betreiben.
- AudioFocus und Medien-Mix je Android-Version testen.
- SampleRate, Buffergrößen, FrameSize und Fehlerstatus in der UI anzeigen.

### Level 3: DSP-Profile

- DSP-Datenmodell, Presets, EQ, Balance, Kompressor und Limiter ausbauen.
- Feedback-Schutz zunächst konservativ, später messbasiert.
- Unit Tests für DSP-Konfigurationen erweitern.

### Level 4: KI-Features offline/online

- Live-Transkript, Untertitel, Zusammenfassung und Übersetzung sauber trennen.
- Offline bevorzugen, Cloud nur mit Zustimmung.
- Ollama/OpenClaw/OpenAI-kompatible Gateways konfigurierbar halten.

### Level 5: Bluetooth/ESP32/Cloud-Gateway

- Headset-Routing gerätespezifisch testen.
- ESP32 als Fernbedienung, Profilumschalter oder Gateway vorbereiten.
- Alpha-Update-Manifest und Fehlerberichte über eigenen sicheren Server statt SMTP in der App.

### Level 6: Experimentelle Hörassistenz mit Dokumentation

- Latenz, Klirren, Pegel, Datenschutz und Grenzen offen dokumentieren.
- Keine medizinischen Versprechen.
- Nutzertests nur mit klarer Warnung und niedriger Anfangslautstärke.
