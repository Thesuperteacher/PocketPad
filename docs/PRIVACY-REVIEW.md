# Application release privacy review

The public repository contains the Android and Windows application, setup helpers, licenses, and documentation. Release downloads contain the APK, portable Windows package, application source, and checksums.

## Excluded material

Website source, video projects, finished videos, recording scripts, captions, and production tools are stored separately. Local sessions, pairing secrets and QR codes, device identifiers, personal paths, logs, account metadata, credentials, signing stores, and debug symbols are excluded from the application release.

## Checks

The application release was scanned for known private identifiers, personal paths, credential patterns, and private-key blocks. Checks covered tracked source, commit identity, nested release archives, APK contents, and Windows binaries. Runtime code that generates pairing secrets is intentional and contains no live pairing credentials. Third-party license notices retain their authors' public attribution.

## App behavior

USB and Wi-Fi connections use TLS with certificate pinning and a fresh pairing token. Automatic keyboard opening sends only an editable yes/no signal. Normal operation does not record input or capture the desktop. Developer test and diagnostic modes can write local files; keep those outputs private.

GitHub processes connection information when serving this repository and its downloads. This review is not an independent security certification. Report reproducible issues without posting live pairing codes or personal content.

## Publication boundary

The September 2026 replacement uses fresh application-only history and a GitHub noreply commit identity. An explicit file allowlist and a local pre-push check help prevent production material and common credentials from entering future uploads. Checks for known personal identifiers are kept outside the repository. The Android APK and Windows ZIP are unchanged from the audited 0.3.1 beta; the source ZIP adds the publication check and documentation.
