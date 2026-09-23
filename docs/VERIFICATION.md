# Verification and known limits

## Public release checks, 19 September 2026

- Android APK rebuilt from this source; four Bluetooth key-map unit tests passed.
- All 17 instrumented Android tests passed on an Android 15 emulator. They cover gestures, tap-drag ordering and release, lifecycle cancellation, keyboard opening, focus deferral, and draft preservation.
- Android lint completed. Existing warnings do not establish compatibility with all devices.
- Windows x64 self-contained build passed. Eight protocol checks passed: rejected invalid credentials, certificate pinning, exclusive controller, ordered input, disconnect release, packet bounds, shutdown, and minimal focus notifications.
- Native focus test passed: editable TextBox accepted; read-only TextBox and Button rejected.
- The fresh build exposed missing native Windows Desktop dependencies. The publish target now copies the matching runtime's native DLLs, and the native focus test passes with the distributed folder layout.

## Earlier physical checks

USB cursor control, click haptics, and automatic keyboard opening were confirmed on a Galaxy Z Fold5. Earlier instrumented tests also passed on that phone. These are historical device checks, not fresh tests of every public-release binary.

## Still unverified

- Physical Bluetooth HID registration, pairing, cursor control, and typing.
- Broad Wi-Fi and USB-tethering acceptance; other Android manufacturers and Android 7/8 devices.
- Fold/unfold continuity and all custom desktop editors.
- End-to-end physical tap-drag with the release APK after pairing.

Bluetooth has no automatic-keyboard return channel and uses printable ASCII with a US PC keyboard layout. Administrator windows, UAC, and sign-in screens are outside normal input permissions.

## Repeating the checks

Use the commands in the README. Run tests in a disposable build directory: test modes can create pairing credentials and metadata beside the executable. Do not publish those generated files.
