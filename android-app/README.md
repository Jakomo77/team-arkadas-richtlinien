# Brillensteuerung (GlassesControl)

Android-App zur Steuerung von Meta AI Glasses (Ray-Ban Meta / Meta Ray-Ban Display)
über das offizielle [Meta Wearables Device Access Toolkit](https://wearables.developer.meta.com/)
(DAT-SDK). Passend zur SDK-Version `0.8.0.31.0`, die im "App-Informationen"-Screen
der Meta AI App zu sehen ist (`Entwicklungsmodus` aktiviert).

## Was die App kann

- **Verbindung zur Brille**: Registrierung über die Meta AI App (`Wearables.startRegistration`).
- **Kamera-Steuerung**: Live-Videostream von der Brillenkamera anzeigen, Fotos auslösen.
- **KI-Interaktion (custom)**: Ein aufgenommenes Foto an Claude (Anthropic API,
  Vision) schicken und auf Deutsch beschreiben lassen.

## Wichtige Einschränkung

Das DAT-SDK erlaubt **keinen** Zugriff auf oder keine Steuerung der eingebauten
Meta-AI-Assistentin der Brille selbst - dafür gibt es (Stand SDK 0.8) keine
öffentliche API. Was das SDK freigibt, ist Kamera-Zugriff (Video-Stream, Foto)
und bei Meta Ray-Ban Display zusätzlich das Display. Die "KI-Interaktion" in
dieser App ist deshalb eine **eigene** Funktion obendrauf (Foto -> Claude Vision),
nicht eine Fernsteuerung von Metas Assistentin.

## Voraussetzungen zum Bauen

1. Android Studio Narwhal (2025.1.1)+, JDK 17+, Android SDK 36+.
2. `local.properties` aus `local.properties.example` kopieren und befüllen:
   - `github_token`: GitHub Personal Access Token (classic) mit `read:packages`-Scope,
     um die `mwdat-*`-Artefakte von GitHub Packages zu laden
     ([Anleitung](https://wearables.developer.meta.com/docs/develop/dat/build-integration-android)).
   - `mwdat_application_id` / `mwdat_client_token`: nur nötig, falls
     "Entwicklungsmodus" in der Meta AI App **aus** ist. Bei aktiviertem
     Entwicklungsmodus (wie im Screenshot) registriert sich die App automatisch.
   - `anthropic_api_key`: für die "KI beschreiben"-Funktion.
3. Projekt in Android Studio öffnen (erzeugt den Gradle Wrapper automatisch)
   oder manuell `gradle wrapper --gradle-version 8.14.3` ausführen, dann
   `./gradlew assembleDebug`. Der Wrapper ist nicht im Repo enthalten, da er
   in dieser Sandbox nicht ohne Zugriff auf `dl.google.com` erzeugt werden konnte.

## Nutzung

1. In der Meta AI App auf dem Handy: **Entwicklungsmodus** aktivieren (siehe
   App-Informationen-Screen).
2. App starten, alle Android-Berechtigungen erlauben (Bluetooth, Kamera, Internet).
3. "Mit Brille verbinden" antippen -> Registrierung über Meta AI App bestätigen.
4. Sobald die Brille erkannt ist: "Kamera starten" -> Kamera-Berechtigung erlauben.
5. Live-Bild wird angezeigt. "Foto aufnehmen" löst ein Foto auf der Brille aus.
6. Bei aufgenommenem Foto: "KI beschreiben" schickt es an Claude und zeigt die
   Beschreibung an.

## Bekannte Grenzen / mögliche Erweiterungen

- Fehlerbehandlung ist bewusst schlank gehalten (kein Firmware-/App-Update-Flow,
  keine feingranulare `DeviceSessionError`/`StreamError`-Auswertung wie im
  offiziellen `CameraAccess`-Sample von Meta). Für Produktionsreife sollte das
  ergänzt werden.
- Kein Mock-Device-Testmodus in der UI verdrahtet, obwohl `mwdat-mockdevice`
  als Abhängigkeit eingebunden ist (nützlich, um ohne physische Brille zu testen).
- Frame-Pacing ist vereinfacht (kein Presentation-Buffer wie im Originalbeispiel);
  bei Bedarf können Frames leicht ruckeln.

## Hinweis zur Verifikation

Dieses Projekt konnte in der aktuellen Sandbox **nicht** vollständig gebaut
werden: `dl.google.com` (Google Maven, benötigt für das Android Gradle Plugin)
ist über den Netzwerk-Proxy dieser Umgebung blockiert (403), und es liegt kein
GitHub-Token für die `mwdat-*`-Pakete vor. Der Code wurde stattdessen sorgfältig
gegen den echten Quellcode und Changelog von
[facebook/meta-wearables-dat-android](https://github.com/facebook/meta-wearables-dat-android)
abgeglichen (API-Namen, Enum-Werte, Signaturen). Baue und teste die App lokal
in Android Studio mit einer echten oder Mock-Brille, bevor du dich auf sie verlässt.
