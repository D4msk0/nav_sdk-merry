# Navigation to LED demo

A portfolio-friendly Android demo that uses Google Navigation SDK to generate real-time turn guidance, forwards that guidance over Bluetooth to an ESP32, and renders the instruction on an LED strip.

## What it does

- Launches a Google Navigation view on Android
- Lets the user simulate or live-route navigation
- Converts navigation updates into a structured Bluetooth payload
- Sends the payload to an ESP32 over RFCOMM
- Drives LED behavior for turn guidance and arrival feedback

## Architecture

### Android app
- `MainActivity` handles lifecycle and permission flow
- `NavigationController` owns navigation setup and route behavior
- `BluetoothTransport` owns the Bluetooth connection and message sending
- `BluetoothCommandSerializer` defines the serial protocol
- `NavInstructionMapper` converts raw navigation maneuvers into app-level models

### ESP32
- The embedded side is responsible for parsing the command payload and rendering LED output
- The command format is intentionally structured so the mobile app and the device logic stay readable and extensible

## Demo flow

1. Open the app and allow the required permissions
2. Tap the map to choose a destination
3. The navigation SDK computes turn-by-turn guidance
4. The phone sends a structured command to the ESP32 over Bluetooth
5. The LED strip shows the current instruction and arrival state

## Notes for portfolio use

This project is a strong showcase because it combines:
- mobile development
- Bluetooth communication
- embedded hardware interaction
- data modeling and protocol design
- real-time system integration

## References

- Google Navigation SDK standalone guide
- Google Navigation SDK getting started guide

## Current status

- Android-side refactor completed
- Structured Bluetooth command format added
- UI and README polished for presentation
- Full Gradle verification is blocked until an Android SDK is configured in this environment
