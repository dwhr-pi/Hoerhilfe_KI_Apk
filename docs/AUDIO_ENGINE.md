# Audio-Engine

## Ziel

Die App soll UI, Audio-Engine, DSP und KI-Gateway sauber trennen. Aktuell nutzt die App Android `AudioRecord` und `AudioTrack` für erste Live-Audio-Funktionen. Für eine spätere echte Low-Latency-Pipeline ist ein Interface `AudioEngine` vorbereitet.

## Aktueller Stand

- `LiveAudioEngine` nutzt `AudioRecord` für Mikrofonaufnahme.
- `AudioTrack` gibt bearbeitete Audiodaten wieder.
- `HearingTestPlayer` erzeugt Sinustöne synthetisch mit `AudioTrack`; es werden keine einzelnen Audiodateien benötigt.
- Der Hörtest pausiert Live Assist, Monitor und Transkription beim Start, damit sich die App nicht selbst stört.

## Architektur

- UI: Compose-Screens, Statusanzeige, Buttons, Profile.
- Audio Engine: Start/Stop, Routing, Puffergrößen, Latenzmodus.
- DSP: Gain, Balance, EQ, Kompressor, Limiter, Feedback-Schutz.
- KI-Gateway: Chat, Zusammenfassung, Übersetzung, spätere Audio-Transkription.

## Messpunkte

Die Audio-Schicht soll bei jedem Start diese Werte erfassen und später im Setup sichtbar machen:

- SampleRate, aktuell meist `16000 Hz` im MVP.
- RecordBufferSize und TrackBufferSize.
- FrameSize je Latenzmodus.
- Startzeit und Fehlerstatus von `AudioRecord`/`AudioTrack`.
- Geschätzte Roundtrip-Latenz, sobald ein Messverfahren vorhanden ist.

Echo, Rückkopplung/Pfeifen und zu hohe Lautstärke müssen als Sicherheitswarnung in UI und Dokumentation sichtbar bleiben.

## Oboe/C++-Pfad

Oboe ist sinnvoll, sobald die Java/Kotlin-Pipeline nicht mehr reicht. Der native Pfad sollte später als `NativeAudioEngine` hinter `AudioEngine` eingebunden werden.

Geplantes Vorgehen:

1. Latenz der aktuellen `AudioRecord`/`AudioTrack`-Pipeline messen.
2. DSP-Datenklassen stabil halten.
3. Native Engine nur hinter demselben Interface einbauen.
4. Geräte- und Headset-Tests dokumentieren.

## Bekannte Grenzen

- Bluetooth kann die Latenz stark erhöhen.
- Benachrichtigungen anderer Apps können Tests stören.
- Android-Geräte verhalten sich bei AudioFocus und Routing unterschiedlich.
- Die App ist keine medizinische Diagnose und kein zugelassenes Hörgerät.
