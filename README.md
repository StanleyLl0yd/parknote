# ParkNote

ParkNote remembers where you parked - even underground. Android 8+ (minSdk 26).

- Single active parking record, no history, accounts, ads, analytics, telemetry, or network permission.
- Offline parking details: floor (`-2`, `0`, `P1`), sector, space, landmark, and two optional private photos.
- Optional one-shot location via Android 17 Location Button, with the library's Android 8-16 permission fallback.
- Location, when present, can be passed to any compatible external map app (`geo:` intent).
- Russian UI, English translation, light/dark/Material You themes.

## Developer setup

- Android Studio supporting AGP 9.4; Android SDK Platform 37; SDK Build Tools 36.0.0.
- Gradle 9.8.2 and JDK 25 to run builds (JVM target 17).
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

**Build status:** GitHub Actions has passed Android build, unit tests, lint and emulator instrumentation on API 26 and 35. The committed Gradle wrapper bootstrap JAR/scripts are retained from 9.6.0; the Gradle runtime distribution is pinned to 9.8.2 with its official SHA-256. Every toolchain update must pass fresh CI before merge. Physical camera, GPS, process-death and backup verification remain mandatory before release.

## Storage integrity

`ParkingRepository` uses `AtomicFile` for one JSON schema-1 record. New JPEGs are written into app-private storage before a record points at them; old JPEGs are only pruned after the replacement record is persisted. Photos are re-encoded with orientation correction, up to 2048 px per dimension, without source EXIF metadata. The app excludes its private records from automatic cloud/device-transfer backups and does not request INTERNET, media-library or camera permissions.

## Required acceptance before 1.0

1. Hosted exact-head compile, Android Lint, and JVM tests in CI.
2. Android emulator instrumentation and rotation/process-death scenarios, including in-progress camera capture and denied permissions.
3. Real Android 8, 14-16 and Android 17 hardware tests for Location Button, system camera, Photo Picker, and maps; test without GMS and network.
4. Crash-injection tests of atomic save and photo replacement, orphan cleanup, bad EXIF and low-disk errors.
5. Merged manifest validation, cloud-backup/device-transfer validation, APK/AAB inspection and signature setup outside Git.
6. Resolve remaining issue: camera URI cleanup on abnormal process death, recover uncommitted photo data securely; test with large font and accessibility services.

**Publication gate:** Do not upload the app to RuStore or Google Play without explicit authorization.

No license is included until explicitly chosen.
