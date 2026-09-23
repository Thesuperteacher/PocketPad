# PocketPad

**Your Android phone, your Windows trackpad.** Built by The SuperTeacher.

Move the pointer with one finger, scroll with two, tap to click, or type with your phone's keyboard. PocketPad connects locally over USB or Wi-Fi. It also includes an experimental Bluetooth mouse and keyboard mode.

[Website](https://pocketpad-superteacher.netlify.app/en/) · [Download the beta](https://github.com/Thesuperteacher/PocketPad/releases/tag/v0.3.1) · [Nederlands](docs/QUICKSTART-NL.md) · [Español](docs/QUICKSTART-ES.md)

## Before you install

PocketPad 0.3.1 is an **experimental beta**, released under the MIT license. The Android APK is debug-signed; the Windows executable has no publisher signature. They are not app-store releases. Download only from this repository's releases and compare the files with `SHA256SUMS.txt`. Windows or Android may show an installation warning. Do not disable your security software.

| Component | Requirement |
|---|---|
| Windows companion | Windows 10/11, x64; portable and includes the .NET runtime |
| Android, USB or Wi-Fi | Android 7.0 or later |
| Android, direct Bluetooth | Android 9.0 or later, compatible Bluetooth HID firmware and PC adapter |
| USB debugging route | Data cable, Android Platform Tools, and an approved debugging connection |
| USB tethering route | Data cable and Android USB tethering support |

USB cursor control, click haptics, and automatic keyboard opening have previously been confirmed on a Galaxy Z Fold5. Other devices may behave differently. Direct Bluetooth, USB tethering, and broad Wi-Fi compatibility still need physical testing. See [verification](docs/VERIFICATION.md) for the boundary between tests and hardware checks.

The app interface is currently English. The website and quick-start guides are available in English, Dutch, and Spanish.

## Download and install

1. Open the [0.3.1 beta release](https://github.com/Thesuperteacher/PocketPad/releases/tag/v0.3.1).
2. Install `PocketPad-Android.apk` on the phone. Allow installation for the app opening that APK only if you trust the download.
3. Extract `PocketPad-Windows.zip` on the PC. Keep `PocketPad.exe` and its companion DLLs together.
4. Open the Windows companion and choose a connection method below.

There is no account to create. Connections and pairing credentials stay local. Closing either app ends the connection; restarting requires pairing again.

## Connect over Wi-Fi

1. Put the phone and PC on the same trusted local network.
2. Open `PocketPad.exe`. Select the Wi-Fi or Ethernet adapter for that network, then choose **Start connection**. The default adapter option is local-only USB, so change it for Wi-Fi.
3. If Windows asks about network access, allow the private network you intend to use.
4. On the phone, open the connection control, choose **Scan PC code · Wi-Fi / USB tethering**, and scan the PC's QR code.

The QR code authorizes control of the PC. Keep it out of screenshots and recordings. **Stop & disconnect** closes the listener and invalidates the current code.

If a firewall rule is needed, `Enable-WiFi.ps1` creates one for this executable, TCP port 19876, private profiles, and local-subnet traffic. It requires administrator permission and does not run automatically. Run it with `-Remove` to remove that rule.

## Connect with a USB cable

### USB debugging: local-only forwarding

1. Install [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools).
2. Enable Android Developer options and USB debugging. Connect a data cable, unlock the phone, and accept the PC's debugging prompt.
3. Run `Start-USB.ps1` from the extracted Windows package. If script policy blocks it, use a process-scoped command:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\Start-USB.ps1
   ```

4. Select **USB cable / this PC only** in the companion, then **Start connection**.
5. On the phone choose **Scan PC code · USB debugging cable** and scan the code.

Unplugging removes the forwarding connection. Run the helper again after reconnecting. `Start-USB.ps1 -Disconnect` removes the forwarding rule; turn off USB debugging when you no longer need it. The helper does not enable wireless debugging.

### USB tethering: no debugging required

Enable USB tethering in Android's hotspot settings. Reopen the PC companion so it discovers the USB network adapter, select that adapter, and scan using **Wi-Fi / USB tethering**. Tethering may also share the phone's internet connection. Turn it off when finished.

PocketPad does not make the phone act as a wired USB HID mouse; these cable options use a local network connection.

## Direct Bluetooth: experimental

Enable Bluetooth on both devices. In PocketPad choose **Bluetooth mouse & keyboard**, allow Nearby devices access, and select a paired PC. To pair a new PC, make the phone visible through PocketPad and add it from Windows Bluetooth settings. Confirm the matching pairing prompts.

The Windows companion can stay closed in this mode. Firmware support varies, so Android 9+ alone does not guarantee compatibility. This route has not been verified on physical hardware.

Bluetooth text assumes an **English (United States) PC keyboard layout**, supports printable ASCII, and is limited to 180 characters per send. Use USB or Wi-Fi for accents, emoji, or other writing systems. Bluetooth has no return channel for text-field focus, so open Keyboard mode manually.

## Gestures and controls

| Action | Gesture |
|---|---|
| Move the pointer | Slide one finger |
| Click / double-click | Tap once / twice |
| Drag | Tap, lift, then touch again nearby and hold while moving; lift to release |
| Scroll | Slide two fingers vertically |
| Right-click | Tap with two fingers, or use Right |
| Keep dragging without holding a finger down | Turn on Drag; tap it again to release |
| Type | Open Keyboard, compose text, then choose Send text |
| Presentation and editing keys | Open Shortcuts |

The second touch for tap-drag must land within Android's double-tap interval. Switching modes, cancelling, leaving the app, or disconnecting releases held input. The manual Drag toggle remains available.

Clicks and taps have optional haptic feedback. Sliding and scrolling do not vibrate. Settings controls pointer speed, haptics, automatic keyboard opening, and the gesture guide.

## Automatic keyboard

On USB and Wi-Fi, the companion detects whether a Windows control exposes an editable field. Selecting it opens Keyboard mode and the phone keyboard. This waits until an active drag or touch gesture ends.

Type a draft on the phone and tap **Send text** to insert it into the focused PC field. Typing is not streamed key by key. Phone backspace edits your draft; PocketPad's Backspace button edits the PC field. Switching tabs preserves the draft.

Only an editable **yes/no** notification goes to the phone. Field names, identities, and contents are not transmitted by this feature. Custom editors may need manual Keyboard mode. Disable automatic opening in Settings if you prefer.

## Privacy and connection behavior

- USB/Wi-Fi traffic uses TLS. Scanning pins the companion's certificate and provides a fresh 192-bit authorization token.
- One phone can control the receiver at a time. A five-second network timeout releases held mouse buttons when the connection disappears.
- Normal operation does not record input, capture the desktop, use analytics, or send commands to a cloud service. This is an input-control app, not remote screen sharing.
- Camera permission scans pairing codes. Nearby devices permission supports Bluetooth. Android backups are disabled.
- Optional developer diagnostics and test modes can write local metadata or test pairing files. Keep those files private. They are excluded from the public release.
- Administrator windows, UAC prompts, and the Windows sign-in screen are outside the companion's normal input permissions.

Read the [public-release privacy review](docs/PRIVACY-REVIEW.md). These checks reduce accidental disclosure; they are not an independent security certification.

## Troubleshooting

| Symptom | Check |
|---|---|
| USB phone not detected | Unlock it, accept the debugging prompt, try a data cable, and check Platform Tools' device list |
| USB stopped after unplugging | Rerun `Start-USB.ps1`, confirm the receiver is started, then reconnect |
| Wi-Fi timeout | Confirm the selected adapter, same network, private firewall rule, and absence of guest-network client isolation |
| Keyboard does not open | Use USB/Wi-Fi, enable automatic keyboard, and try a standard editable control; use the Keyboard tab for unsupported editors |
| Text does not appear on the PC | Focus an ordinary PC field and tap Send text; the phone textbox is a draft |
| Bluetooth accents fail | Use USB/Wi-Fi; Bluetooth currently supports printable ASCII with US layout |
| Pointer cannot control an administrator dialog | Return to an ordinary desktop application |

## Build from source

Install .NET SDK 10, Android SDK platform 35, and JDK 17 or a compatible Android Studio JBR. Set `JAVA_HOME` and `ANDROID_HOME` to your installations, then run:

```powershell
.\Build.ps1
```

The script creates the self-contained Windows app and debug APK in `dist/`. Android uses the included Gradle wrapper, AGP 8.6.1, and Java 17 source compatibility. A locally generated debug certificate may differ from the downloadable beta's certificate; Android will not accept an update signed with a different key.

| Directory | Contents |
|---|---|
| `android/` | Native Java interface, gestures, TLS transport, Bluetooth HID, tests |
| `windows/` | .NET companion, input delivery, accessibility focus detection, protocol tests |
| `docs/` | Quick starts, verification, and privacy review |

Run Android unit tests with `android\gradlew.bat -p android testDebugUnitTest`. Instrumentation tests require an emulator or phone: `android\gradlew.bat -p android connectedDebugAndroidTest`. Run `PocketPad.exe --self-test` for protocol tests and `--focus-test` for native focus checks. Those commands write local reports beside the executable.

## Publication check

The public repository is limited to the files listed in `publication-files.txt`. Website and video work belongs outside this checkout.

With Python 3 installed, run `python scripts/verify_publication.py --staged` before committing and `python scripts/verify_publication.py` before pushing. The second command checks every locally reachable commit for unexpected files and common credential patterns, including nested archives. To run it automatically before pushes from this checkout:

```powershell
git config core.hooksPath .githooks
```

When adding an application file, review it and add its exact path to the allowlist. The check reports filenames and issue types without printing matching secrets. It cannot recognize every private value or inspect images visually. Review changed files and release contents yourself; keep any checks for personal identifiers outside the public repository. Downloaded source ZIPs have no Git history, so run this check from a Git clone.

## Contributing and license

Report reproducible problems in GitHub Issues with Android/Windows versions, connection type, and steps. Remove pairing codes, device serials, personal text, and local paths from attachments. Never post a live QR code.

PocketPad is an independent project inspired by phone-as-trackpad tools such as Mousely; it is not affiliated with them. Source code is MIT-licensed. Dependencies retain their licenses: see [third-party notices](THIRD-PARTY.md).
