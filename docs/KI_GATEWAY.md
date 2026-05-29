# KI-Gateway

## Ziel

Die KI-Funktionen sollen als Assistenz dienen: Live-Transkript, Untertitel, Übersetzung, Gesprächszusammenfassung und die Frage „Was wurde gerade gesagt?“. Cloud-Funktionen dürfen nur mit ausdrücklicher Zustimmung genutzt werden.

## OpenAI-kompatibler Chat

Ein kompatibles Gateway kann `/v1/chat/completions` anbieten. Beispiel:

```json
{
  "model": "llama3.2:1b",
  "messages": [
    {"role": "system", "content": "Du bist ein knapper Hörassistenz-Helfer."},
    {"role": "user", "content": "Fasse das Gespräch zusammen."}
  ]
}
```

## Spätere Audio-Transkription

Für echte Gateway-Transkription ist später ein Endpunkt wie `/v1/audio/transcriptions` sinnvoll. Bis dahin nutzt die App bevorzugt Android-Spracherkennung lokal bzw. systemnah.

## Ollama/OpenClaw

Mögliche lokale Konfigurationen:

- OpenClaw Proxy: `https://dein-host/openclaw/v1/chat/completions`
- Ollama Proxy: `https://dein-host/ollama/api/chat`
- Lokales LAN: `http://192.168.x.x:11434`

## Gateway-Status

Die App unterscheidet im UI-Grundzustand:

- `offline / nicht konfiguriert`, wenn keine Base URL eingetragen ist.
- `konfiguriert, Test empfohlen`, wenn ein Gateway eingetragen ist.
- Fehlertext aus dem Gateway-Test, wenn der Request fehlschlägt.
- Datenschutzmodus aktiv, solange keine automatische Audio-Cloud-Übertragung implementiert ist.

## Datenschutz-Schalter

- Offline bevorzugen.
- Cloud erlauben nur aktiv einschalten.
- Keine Audio-Speicherung als Standard.
- Temporäre Speicherung nur transparent und löschbar.
- Verlauf löschen sichtbar anbieten.

## Sicherheitswarnungen

Keine API-Keys oder SMTP-Passwörter ins Repo committen. Auch „verschlüsselte“ Secrets in einer APK sind prinzipiell extrahierbar. Für automatischen Versand oder Cloud-Protokolle sollte später ein eigener Server-Endpunkt genutzt werden.
