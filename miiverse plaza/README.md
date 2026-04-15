# Miiverse Plaza

Een experimentele game-basis voor een StreetPass-achtige plaza waarbij Android en modded consoles allemaal spelclients zijn.

## Wat er nu in zit

- game-achtige plaza-opzet met missie en punten
- Android-profiel met nickname, favoriete game en status
- Android-app als centrale bron van alle StreetPass-data
- UDP beacon discovery op poort `43555`
- Encounter-lijst voor gevonden apparaten op hetzelfde netwerk
- lokale opslag van verzamelde StreetPasses op de telefoon
- testknop om zonder console alvast StreetPasses te simuleren
- JSON payload die een modded DSi, modded 3DS of modded Switch homebrew kan uitsturen
- JSON sync-pakket dat door een homebrew-bridge in systeemdata geïnjecteerd kan worden
- sync verzenden naar een specifiek LAN-IP of via broadcast

## JSON beacon-formaat

```json
{
  "type": "beacon",
  "deviceId": "unique-device-id",
  "nickname": "Doria",
  "favoriteGame": "Mario Kart DS",
  "statusMessage": "Op zoek naar een encounter!",
  "platform": "Switch"
}
```

## JSON system-sync formaat

```json
{
  "type": "system_sync",
  "deviceId": "unique-device-id",
  "profile": {
    "nickname": "Doria",
    "favoriteGame": "Mario Kart DS",
    "statusMessage": "Op zoek naar een encounter!",
    "sourcePlatform": "Android"
  },
  "targets": ["DSi", "Switch", "3DS"],
  "instructions": {
    "mode": "inject_into_system",
    "updatedBy": "Miiverse Plaza Android"
  }
}
```

## Testidee voor modded apparaten

- Laat de homebrew-app elke paar seconden een UDP broadcast sturen naar poort `43555`
- Gebruik hetzelfde JSON-formaat als hierboven
- Laat `platform` bijvoorbeeld `DSi` of `Switch` zijn
- Laat de homebrew-bridge daarnaast `system_sync` ontvangen en de velden wegschrijven naar de juiste save- of systeemstructuur
- Gebruik `255.255.255.255` om op het hele lokale netwerk te zenden, of een concreet doeladres zoals `192.168.0.45`

## Belangrijke beperking

Dit is geen echte officiële Nintendo StreetPass-implementatie. Het is een eigen plaza-game en eigen sync-laag voor homebrew-toestellen en Android, waarbij elk apparaat als game-client werkt.
