# Miiverse Plaza

An experimental game foundation for a StreetPass-style plaza where Android and modded consoles all act as game clients.

## Current Features

- game-style plaza setup with a mission and score system
- Android profile with nickname, favorite game, and status message
- Android app acting as the central source of all StreetPass data
- UDP beacon discovery on port `43555`
- encounter list for devices found on the same network
- local storage for collected StreetPass encounters on the phone
- test button to simulate StreetPass encounters without a console
- JSON payloads that modded DSi, modded 3DS, or modded Switch homebrew can send
- JSON sync packet that a homebrew bridge can inject into local system or save data
- sync sending to a specific LAN IP or by broadcast

## JSON Beacon Format

```json
{
  "type": "beacon",
  "deviceId": "unique-device-id",
  "nickname": "Doria",
  "favoriteGame": "Mario Kart DS",
  "statusMessage": "Looking for an encounter!",
  "platform": "Switch"
}
```

## JSON System Sync Format

```json
{
  "type": "system_sync",
  "deviceId": "unique-device-id",
  "profile": {
    "nickname": "Doria",
    "favoriteGame": "Mario Kart DS",
    "statusMessage": "Looking for an encounter!",
    "sourcePlatform": "Android"
  },
  "targets": ["DSi", "Switch", "3DS"],
  "instructions": {
    "mode": "inject_into_system",
    "updatedBy": "Miiverse Plaza Android"
  }
}
```

## Testing Idea For Modded Devices

- Let the homebrew app send a UDP broadcast to port `43555` every few seconds
- Use the same JSON format shown above
- Set `platform` to values like `DSi` or `Switch`
- Let the homebrew bridge also receive `system_sync` packets and write the fields into the correct save or system structure
- Use `255.255.255.255` to send to the whole local network, or a specific target such as `192.168.0.45`

## Important Limitation

This is not a real official Nintendo StreetPass implementation. It is an original plaza game and sync layer for homebrew devices and Android, where each device acts as a game client.
