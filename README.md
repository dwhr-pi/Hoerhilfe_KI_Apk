# Hearing Assist KI Android

Android-MVP fuer die Idee aus `dwhr-pi/H-rger-te_App_mit_KI`, umgesetzt als importierbares Android-Studio-Projekt in Kotlin + Jetpack Compose.

## Enthalten

- Audio-Dashboard mit anpassbaren Hoerprofilen
- Hoertest light mit Sinus-Toenen und automatischer Profil-Empfehlung
- KI-Tab fuer Chat oder Uebersetzung ueber OpenClaw, Ollama oder ein OpenAI-kompatibles Gateway
- Live-Audio mit Mikrofonmonitor, einfacher Sprachbetonung, Noise-Gate und Android-Audio-Routing
- eigener Setup-Tab zum Ein- und Ausschalten zentraler Funktionen
- Dark Mode im Setup-Tab
- vorbereitete Sprachumschaltung Deutsch / Englisch
- Live-Transkription ueber Android-Spracherkennung
- adaptiver Hoertest mit Sprachanweisung, Reaktionsbutton und Lautstaerke-Anpassung
- grafische Kurve der ermittelten Hoerschwellen im Hoertest
- getrennte Hoerkurven fuer linkes und rechtes Ohr plus Gesamtkurve
- Startfenster mit Profilauswahl, Live-Transkription und Gesamtlautstaerke
- Profilverwaltung mit Namen, Quelle sowie Datum/Uhrzeit
- Persistenz fuer Profil-, Test- und Gateway-Einstellungen

## Nicht enthalten

- Echtzeit-Mikrofon-zu-Headset-DSP
- medizinische Diagnostik
- Bluetooth-LE-Audio-Optimierung
- RNNoise, DeepFilterNet oder Beamforming

Das ist bewusst ein MVP, der die Produktidee als Android-App nutzbar macht und sauber an die vorhandene KI-Infrastruktur anschliessen kann.

## Build

Android Studio muss auf dem Rechner installiert sein, aber es muss dafuer nicht gestartet sein. Ich kann die mit Android Studio installierte Java-Runtime, das Android-SDK und die Build-Tools auch direkt ueber die Kommandozeile benutzen.

1. Projektordner `android-hearing-assist` in Android Studio oeffnen.
2. Gradle Sync ausfuehren.
3. App auf Emulator oder Geraet starten.

Fuer einen reinen Kommandozeilen-Build reicht daher in der Praxis oft schon:

1. Android Studio einmal installieren.
2. Das Android-SDK ueber Android Studio mitinstallieren lassen.
3. Danach kann das Projekt auch ohne geoeffnetes Android Studio gebaut werden.

## Release-APK

Der Release-Pfad ist jetzt im Projekt vorbereitet. Die Signaturdaten liegen bewusst nicht fest im Repo, sondern lokal auf deinem Rechner.

### Variante A: Keystore automatisch anlegen

1. PowerShell im Repo-Ordner `android-hearing-assist` oeffnen.
2. Das Skript [generate-release-keystore.ps1](C:/Users/danie/Documents/GitHub/Ultimate_KI_Setup/android-hearing-assist/tools/generate-release-keystore.ps1) starten.
3. Passwort und Zertifikatsdaten eingeben.
4. Das Skript erzeugt:
   - `keystore/hearing-assist-release.jks`
   - `release-signing.properties`

### Variante B: Vorhandenen Keystore eintragen

1. Die Vorlage [release-signing.properties.example](C:/Users/danie/Documents/GitHub/Ultimate_KI_Setup/android-hearing-assist/release-signing.properties.example) nach `release-signing.properties` kopieren.
2. Dort `storeFile`, `storePassword`, `keyAlias` und `keyPassword` eintragen.

### Release bauen

1. Das Skript [build-release.cmd](C:/Users/danie/Documents/GitHub/Ultimate_KI_Setup/android-hearing-assist/tools/build-release.cmd) ausfuehren.
2. Das Release-Artefakt liegt danach unter `app/build/outputs/apk/release/`.

### Wichtig zu Release-Signing

- `release-signing.properties`
- `*.jks`
- `*.keystore`

werden bewusst nicht mit ins Repo eingecheckt.

Wenn `release-signing.properties` vorhanden ist und der Keystore existiert, signiert Gradle den Release-Build automatisch. Wenn noch kein Keystore vorhanden ist, kann der Release-Build zwar vorbereitet sein, aber noch nicht als sauber signierte Verteilungs-APK gelten.

## App Aufs Handy Testen

### Variante A: Direkt aus Android Studio

1. Auf dem Android-Handy `Einstellungen > Ueber das Telefon` oeffnen.
2. Mehrfach auf `Build-Nummer` tippen, bis die Entwickleroptionen aktiv sind.
3. Unter `Entwickleroptionen` `USB-Debugging` aktivieren.
4. Handy per USB mit dem Rechner verbinden.
5. Die Rueckfrage auf dem Handy zum USB-Debugging mit `Zulassen` bestaetigen.
6. Das Projekt in Android Studio oeffnen.
7. Oben dein Geraet auswaehlen und `Run` starten.

### Variante B: APK manuell installieren

1. Das Debug-APK bauen.
2. Das APK liegt dann unter `app/build/outputs/apk/debug/app-debug.apk`.
3. Die Datei aufs Handy kopieren.
4. Auf dem Handy die Installation aus unbekannter Quelle fuer die verwendete Datei-App oder den Browser erlauben, falls Android danach fragt.
5. `app-debug.apk` auf dem Handy oeffnen und installieren.

### Wenn das Handy vom Rechner nicht erkannt wird

1. Ein USB-Datenkabel verwenden, nicht nur ein Ladekabel.
2. Im USB-Menue des Handys `Dateiuebertragung` oder `Datenuebertragung` aktivieren.
3. Falls noetig den passenden Windows-USB-Treiber des Herstellers installieren.
4. Ein neues PowerShell-Fenster oeffnen und mit `adb devices` pruefen, ob das Geraet sichtbar ist.

### Was du fuer einen Test wirklich brauchst

- Android Studio muss installiert sein.
- Android Studio muss nicht geoeffnet sein, damit ich hier ueber die lokale Toolchain bauen kann.
- Fuer einen echten Start auf dem Handy brauchst du nur ein aktiviertes USB-Debugging oder das fertige APK zur manuellen Installation.

### Was du in der App zuerst testen solltest

1. Im Tab `Audio` ein vorsichtiges Profil waehlen, am besten `Gespraech`.
2. Im Tab `Setup` festlegen, ob du staerkeren DSP, Live-Transkription und den KI-Assistenten aktiv haben willst.
3. Im Tab `Live` zuerst nur den `Monitor` starten und den Mikrofonpegel pruefen.
4. Danach `Live Assist starten`, aber die Geraetelaustarke zuerst niedrig halten.
5. Falls du ein Headset oder Bluetooth-Geraet nutzen willst, im selben Tab die Route auf `Bluetooth` oder `Auto` stellen.
6. Wenn `Live-Transkription` aktiviert ist, dort auch die Transkription starten.
7. Im Tab `KI` dein Gateway eintragen und mit `Gateway testen` pruefen, ob dein VPS-/OpenClaw-/Ollama-Stack erreichbar ist.

## Setup-Schalter

Im Tab `Setup` kannst du die wichtigsten Produktfunktionen gezielt vergleichen:

- `Dark Mode`
  Wirkung: wechselt zwischen `System`, `Hell` und `Dunkel`.
  Nachteil: kein funktionaler Nachteil, aber gut fuer Lesbarkeitstests unter verschiedenen Lichtbedingungen.

- `Sprache`
  Wirkung: die Haupt-UI und die Sprachanweisungen des Hoertests koennen zwischen `Deutsch` und `English` umgeschaltet werden.
  Nachteil: die App ist aktuell in den zentralen Bereichen vorbereitet, aber noch nicht als vollstaendig internationalisierte Produktiv-App ausgebaut.

- `Staerkerer DSP / EQ`
  Wirkung: mehr Sprachfokus, staerkeres Noise-Gate, hoehere Verstaendlichkeitschance bei schwierigen Umgebungen.
  Nachteil: kann kuenstlicher, schraeger oder schaerfer klingen.

- `Streaming-Transkription`
  Wirkung: Sprache wird im Live-Tab mitgeschrieben, was beim Verstehen hilft.
  Nachteil: braucht einen funktionierenden Android-Spracherkennungsdienst und verbraucht zusaetzlich Akku.

- `KI-Assistent`
  Wirkung: Chat, Uebersetzung und Gateway-Test fuer OpenClaw/Ollama werden eingeblendet.
  Nachteil: ohne erreichbares Gateway bringt die Funktion im Alltag wenig.

- `Echtzeit Mic -> Headset DSP`
  Wirkung: schaltet die eigentliche Live-Assist-Durchhoerstrecke frei.
  Nachteil: ausgeschaltet startet Live Assist bewusst nicht als Hoerhilfe.

- `Native Low-Latency Pipeline`
  Wirkung: bereitet den spaeteren Oboe/C++-Pfad vor und nutzt aktuell kleinere Audiobloecke.
  Nachteil: echte Oboe-Anbindung braucht spaeter native Bibliotheken und Geraetetests.

- `Bluetooth-LE-Audio optimieren`
  Wirkung: speichert einen experimentellen LE-Audio-Modus fuer spaetere Headset-Feinabstimmung.
  Nachteil: aktuell noch vorbereitender Schalter, weil Android-LE-Audio je nach Geraet stark variiert.

- `Bluetooth-Kompatibilitaet`
  Wirkung: markiert konservativeres Routing fuer mehr Headset-Hersteller im Alltag.
  Nachteil: muss mit echten Geraeten feinjustiert werden.

- `Echter EQ, Kompressor, Feedback-Schutz`
  Wirkung: aktiviert die staerkere DSP-Kette mit Kompressor und einfachem Feedback-Schutz.
  Nachteil: kann Sprache kontrollierter, aber auch kuenstlicher klingen lassen.

- `Rauschunterdrueckung`
  Wirkung: Modi fuer `Aus`, `Basic`, `RNNoise`, `DeepFilterNet` und `Beamforming`.
  Nachteil: RNNoise, DeepFilterNet und Beamforming sind aktuell als waehlebare Betriebsarten vorbereitet; echte native Modelle muessen spaeter als Bibliothek eingebunden werden.

- `Streaming-Untertitel`
  Wirkung: Teilresultate der Android-Spracherkennung werden schon waehrend des Sprechens angezeigt.
  Nachteil: einzelne Woerter koennen sich waehrend der Erkennung noch aendern.

Wichtig: `Release-Signing` ist absichtlich kein Schalter in der App. Das ist kein Laufzeit-Feature, sondern ein Build-Schritt fuer eine spaetere Release-APK.

## Neuer Hoertest

Der Hoertest ist jetzt nicht mehr nur ein einfacher Ton-Player:

1. Zu Beginn werden zwei Sprachanweisungen vorgelesen.
2. Danach werden unterschiedliche Frequenzen in zufaelliger Reihenfolge abgespielt.
   Aktuell: `125, 250, 500, 750, 1000, 1250, 1500, 1750, 2000, 2250, 2500, 2750, 3000, 3250, 3500, 3750, 4000, 4250, 4500, 5000, 5250, 5500, 5750, 6000, 6250, 6500, 6750, 7000, 7250, 7500, 7750, 8000 Hz`
3. Die Lautstaerke beginnt zunaechst eher hoeher und wird dann adaptiv leiser.
4. Sobald der Tester den Ton hoert, drueckt er den grossen roten Button.
5. Reagiert der Tester nicht innerhalb des Reaktionsfensters, wird spaeter wieder mit etwas lauterem Ton nachgesteuert.
6. Linkes und rechtes Ohr werden getrennt ausgewertet.
7. Aus den Reaktionen entsteht pro Frequenz eine erste Schaetzung der Hoerschwelle.
8. Die Schwellen werden als linke Kurve, rechte Kurve und Gesamtkurve direkt auf dem Bildschirm dargestellt.

## Startfenster

Das Startfenster dient jetzt als Hauptfenster:

1. Auswahl zwischen vorhandenen Profilen
2. Speichern des aktuellen Profils unter eigenem Namen
3. Anzeige von Quelle sowie Datum/Uhrzeit des Profils
4. Anzeige der Live-Transkription
5. Gesamtlautstaerke-Regler fuer die Handy-Medienlautstaerke

Das ist noch kein medizinisch validierter Audiometrie-Test, aber deutlich naeher an einem echten Schwellen-/Reaktionstest als die fruehere Demo-Version.

## Gateway

Der KI-Bereich ist jetzt individuell anpassbar fuer drei Modi:

- `OpenClaw Proxy`
- `Ollama Proxy`
- `OpenAI-kompatibel`

Passend zu deinem Reverse-Proxy-Setup in diesem Repo sind insbesondere diese Kombinationen sinnvoll:

- Base URL: `https://dein-host`
- OpenClaw Pfad: `/openclaw/v1/chat/completions`
- Ollama Pfad: `/ollama/api/chat`

Bei direkter LAN-Nutzung kannst du statt einer Domain auch eine lokale URL wie `http://192.168.x.x` eintragen. Die App erlaubt deshalb bewusst auch Cleartext-HTTP fuer private Netze.

Neu ist ausserdem:

- ein `Gateway testen`-Button fuer einen echten Probe-Request
- zusaetzliche freie Header als Mehrzeilenfeld
- Presets fuer `OpenClaw`, `Ollama` und `OpenAI-kompatibel`

## Fehlerprotokoll und Alpha-Updates

Im Tab `Setup` gibt es jetzt einen Wartungsbereich:

- `Fehlerprotokoll senden` oeffnet den Mail-Client des Geraets und adressiert den Bericht an `ai-chat-to-markdown@web.de`.
- Die Produktkennung ist `hoerhilfe-ki.android.alpha`, damit Protokolle eindeutig von anderen Produkten unterschieden werden koennen.
- SMTP-Passwoerter werden bewusst nicht in der APK gespeichert. Auch verschluesselte Zugangsdaten waeren in einer Android-App prinzipiell auslesbar. Fuer automatischen Hintergrundversand ist spaeter ein kleiner VPS-Endpunkt sinnvoller.
- `Update pruefen` liest eine frei konfigurierbare JSON-Datei. Das funktioniert auch fuer Alpha-Versionen ohne Play Store.

Beispiel fuer eine Alpha-Update-Datei:

```json
{
  "productId": "hoerhilfe-ki.android.alpha",
  "versionCode": 3,
  "versionName": "0.1.2-alpha",
  "downloadUrl": "https://example.com/hoerhilfe-ki/app-debug-latest.apk",
  "message": "Neue Alpha-Version mit Fehlerprotokoll und Update-Pruefung."
}
```

Die Beispiel-Datei liegt im Repo als [update-manifest.example.json](C:/Users/danie/Documents/GitHub/Ultimate_KI_Setup/android-hearing-assist/update-manifest.example.json). Du kannst so eine Datei auf dem VPS oder in GitHub ablegen und die URL in der App unter `Setup > Update-Pruefung > Alpha Update-URL` eintragen.

## Nächste sinnvolle Schritte

1. Low-Latency Mic->DSP->Headset Pipeline spaeter auf Oboe oder nativen C++-Audio-Stack umstellen.
2. DSP-Filter um echten EQ, Kompressor und Feedback-Schutz erweitern.
3. Live-Transkription und Untertitel auf Streaming umstellen.
4. Bluetooth-Headset-Routing fuer mehr Herstellergeraete im Alltag testen und feinjustieren.
