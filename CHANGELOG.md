# 0.9.0-beta.1 - private Android test build (2026-10-22)

- Offline, single active parking note: floor, sector, space number and optional landmark.
- Camera and system photo picker, with up to two private, re-encoded photos and stripped source EXIF.
- Optional one-shot location with bounded GPS/network provider fallback; no startup/background tracking.
- Open an explicitly saved location using an external maps app.
- Atomic local save, delete/finish, corrupt-record fail-closed behavior and photo lifecycle cleanup.
- Russian and English translations; light/dark and Material You color support.
- CI regression tests on Android 8 (API 26) and Android 15 (API 35), APK/AAB signing and native 16 KiB alignment checks.
- Release candidate is **not** certified on physical camera, Photo Picker, GPS, app process death,
  accessibility/large text or an actual 16 KiB-page device. These remain acceptance gates.

This is a **private beta APK/AAB**, not a store publication or a stable 1.0 release.

# 0.1.0 - initial development build

- Initial app architecture, Compose UI, local parking repository, permissions model, and CI.
