# Datenschutz und Sicherheit

## Grundsatz

Mikrofon- und Transkriptionsdaten sind sensibel. Die App darf keine heimliche Aufnahme durchführen und muss klar anzeigen, wenn Mikrofon, Live Assist oder Transkription aktiv sind.

## Datenverarbeitung

- Lokaler Modus bevorzugt.
- Keine Cloud-Übertragung ohne Zustimmung.
- Keine Audio-Speicherung als Standard.
- Temporäre Speicherung nur transparent.
- Verlauf löschen, Profile löschen, Gateway zurücksetzen und Datenexport sind in der Setup-UI vorbereitet.
- Export erfolgt als bewusst ausgelöster Android-Share-Intent, nicht automatisch im Hintergrund.

## Sichtbare Aktivität

Die UI muss klar zeigen:

- Mikrofon aktiv.
- Transkription aktiv.
- Cloud/KI aktiv oder Gateway nicht konfiguriert.
- Datenschutzmodus aktiv.

## Fehlerberichte

Fehlerberichte sollen produkt- und versionsbezogen sein. Passwörter oder private Schlüssel dürfen nicht in der App gespeichert werden. Ein Mail-Intent ist für Alpha-Tests akzeptabel; automatischer Versand sollte später über einen sicheren Server-Endpunkt erfolgen.

## Medizinische Abgrenzung

Diese App ist keine medizinische Diagnose, kein Ersatz für HNO-Arzt, Hörakustiker oder zugelassenes Hörgerät. Sie ist experimentell und nicht medizinisch zertifiziert.

## Haftungsausschluss

Nutzung auf eigene Verantwortung. Lautstärke immer niedrig beginnen. Bei Schmerzen, Pfeifen, Schwindel oder Unwohlsein sofort stoppen.
