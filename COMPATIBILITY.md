# Toolchain and platform decisions (22 October 2026)

- Android Gradle Plugin 9.4.0: official maximum compile API 37; requires Gradle 9.6.0+, minimum JDK 17.
- Gradle 9.8.2 is the selected runtime distribution. The Gradle 9.6.0 wrapper bootstrap JAR/scripts remain committed; the ZIP download is SHA-256 pinned. Use JDK 25 for developers and hosted CI, target Java/JVM bytecode 17.
- AGP 9 includes built-in Kotlin; do not apply the old Kotlin Android plugin. Compose Compiler plugin is 2.4.21, used with AGP built-in Kotlin; compatibility is gated on hosted compile and tests.
- Stable Compose BOM `2026.09.00`, Activity Compose 1.13.0, AndroidX Core 1.19.1, Lifecycle 2.11.0, ExifInterface 1.4.2.
- Location Button: `androidx.core.locationbutton:locationbutton-compose:1.0.0-alpha01` is experimental and **has no stable release**. This is a narrow exception to stable-only policy because the Android 17 permission policy requires the component for this use case.
- Manifest has ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION with onlyForLocationButton, and USE_LOCATION_BUTTON. The official library provides compatibility through Android 16 (API 36).
- GitHub Actions emulator instrumentation passed on Android API 26 and 35 for the previous toolchain. Android 17 / API 37 runtime and physical device compatibility remain unverified. New toolchain changes require new CI.

Official references:
- https://developer.android.com/build/releases/agp-9-4-0-release-notes
- https://docs.gradle.org/9.8.2/userguide/compatibility.html
- https://gradle.org/release-checksums/
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/guide/topics/permissions/private-alternatives/location-button
- https://developer.android.com/jetpack/androidx/releases/core-locationbutton
- https://developer.android.com/develop/ui/compose/bom
