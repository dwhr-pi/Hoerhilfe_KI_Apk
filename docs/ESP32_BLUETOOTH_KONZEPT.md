# ESP32- und Bluetooth-Konzept

## Was Android allein kann

Android kann Mikrofon aufnehmen, Audio ausgeben, Bluetooth-Routing anstoßen, Profile speichern und KI-Gateways ansprechen. Die tatsächliche Headset-Latenz und Routing-Qualität hängen stark vom Gerät, Android-Version und Headset ab.

## Normales Bluetooth-Headset

Normale Bluetooth-Headsets sind für Medien und Telefonie optimiert, aber nicht automatisch für latenzarme Mikrofon-Durchleitung. Für Hörassistenz kann die Verzögerung störend sein.

## Classic Bluetooth, BLE und BLE Audio

- Classic Bluetooth: weit verbreitet, oft hohe Latenz bei Medienprofilen.
- BLE: gut für Steuerdaten, nicht automatisch für hochwertiges Audio.
- BLE Audio/LC3: vielversprechend, aber abhängig von Android-Version, Chipset und Headset.

## ESP32-Rollen

Ein ESP32 kann sinnvoll sein als:

- Fernbedienung für Profilwechsel.
- Externer Button für Hörtest-Reaktion.
- Kleines Mikrofonmodul für Experimente.
- Gateway-Statusanzeige.
- Profil-Sync-Gerät im lokalen Netz.

## Mögliche Firmware-Struktur

- BLE-Service für Button/Profilwechsel.
- Optional WLAN-Konfiguration.
- JSON-Protokoll für Profil-ID, Lautstärke und Status.
- Keine heimliche Aufnahme, klare LED-Anzeige bei aktivem Mikrofon.

## Grenzen

Ein ESP32 macht die App nicht automatisch zu einem medizinischen Hörgerät. Echtzeit-Audio mit guter Qualität ist anspruchsvoll und muss getrennt getestet werden.
