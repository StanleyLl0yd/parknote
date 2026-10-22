# Toolchain and platform decisions (22 October 2026)

- Android Gradle Plugin 9.4.0: official maximum compile API 37; requires Gradle 9.6.0+, minimum JDK 17.
- Gradle 9.6 supports running on JDK 25. Use JDK 25 for developers and hosted CI, target Java/JVM bytecode 17.
- AGP 9 includes built-in Kotlin; do not simultaneously apply the old Kotlin Android plugin. Compose Compiler plugin matches Kotlin 2.2.10 chosen here, pending actual dependency resolution.
- Stable Compose BOM `2026.09.00`, Activity Compose 1.13.0, AndroidX Core 1.19.0, Lifecycle 2.11.0, ExifInterface 1.4.2.
- Location Button: `androidx.core.locationbutton:locationbutton-compose:1.0.0-alpha01` is experimental and **has no stable release**. This is a narrow exception to stable-only policy because the Android 17 permission policy requires the component for this use case.
- Manifest has ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION with onlyForLocationButton, and USE_LOCATION_BUTTON. The official library provides compatibility through Android 16 (API 36).
- Neither minimum nor target platform support has been exercised in an Android runtime yet; documentation compatibility is not device proof.

Official references:
- https://developer.android.com/build/releases/agp-9-4-0-release-notes
- https://docs.gradle.org/9.6.0/userguide/compatibility.html
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/guide/topics/permissions/private-alternatives/location-button
- https://developer.android.com/jetpack/androidx/releases/core-locationbutton
- https://developer.android.com/develop/ui/compose/bom
