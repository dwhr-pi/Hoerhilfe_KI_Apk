# ESP32 Remote

## Ziel

Ein ESP32 kann später als kleine Fernbedienung für Höhrhilfe KI dienen. Das ist vorbereitetes Konzept, noch keine echte Verbindung im App-Code.

## Mögliche Funktionen

- Profil umschalten.
- Lauter/leiser.
- Mute oder Verstärkung sofort aus.
- Statusanzeige per LED oder kleinem Display.
- Hörtest-Reaktionsbutton als optionales Eingabegerät.

## BLE-Protokollidee

JSON-Variante:

```json
{"type":"profile","id":"default-conversation"}
{"type":"volume","delta":-5}
{"type":"mute","enabled":true}
{"type":"status","mic":true,"liveAssist":false,"transcription":true}
```

Einfache Command-Variante:

```text
PROFILE default-conversation
VOL +5
VOL -5
MUTE ON
MUTE OFF
STOP
STATUS?
```

## Sicherheit

- Pairing nur sichtbar und bewusst.
- Keine heimliche Aufnahme.
- Kein automatischer Cloud-Zugriff.
- Kein medizinisches Versprechen.

## Nächste Schritte

1. BLE-Service-UUID festlegen.
2. Android-Scanner nur mit Berechtigung starten.
3. Profil- und Lautstärke-Commands lokal testen.
4. Erst danach Firmware und App fest koppeln.
