#!/usr/bin/env bash
# Smoke-launch the R8-optimized RELEASE APK using a disposable CI-only key.
# This never reads production signing secrets or publishes any build.
set -euo pipefail
: "${ANDROID_HOME:?ANDROID_HOME is required}"
: "${RUNNER_TEMP:?RUNNER_TEMP is required}"
# Unsigned AGP release builds are named app-release-unsigned.apk; an
# environment-signed release uses app-release.apk. Require exactly one.
shopt -s nullglob
RELEASE_APKS=(app/build/outputs/apk/release/*.apk)
if (( ${#RELEASE_APKS[@]} != 1 )); then
  printf 'Expected one unsigned release APK, found %s\\n' "${#RELEASE_APKS[@]}" >&2
  exit 1
fi
APK="${RELEASE_APKS[0]}"
SIGNED="$RUNNER_TEMP/parknote-ci-release-signed.apk"
KEY="$RUNNER_TEMP/parknote-ci-smoke.jks"
SIGNER="$ANDROID_HOME/build-tools/36.0.0/apksigner"
test -s "$APK" || { echo "Release APK was not built" >&2; exit 1; }
test -x "$SIGNER" || { echo "Android apksigner unavailable" >&2; exit 1; }

# Explicitly disposable public CI credentials - never use the user's JKS here.
keytool -genkeypair -noprompt -keystore "$KEY" -storetype PKCS12 \
  -storepass ci-test-not-secret -keypass ci-test-not-secret \
  -alias ci-smoke -keyalg RSA -keysize 2048 -validity 1 \
  -dname "CN=ParkNote temporary CI release smoke,O=CI" >/dev/null 2>&1
"$SIGNER" sign --ks "$KEY" --ks-key-alias ci-smoke \
  --ks-pass pass:ci-test-not-secret --key-pass pass:ci-test-not-secret \
  --out "$SIGNED" "$APK"
"$SIGNER" verify "$SIGNED"
adb uninstall com.sl.parknote >/dev/null 2>&1 || true
# Clearing logcat is best-effort: API 26 emulator may deny this operation.
adb install "$SIGNED"
adb shell am start -W -n com.sl.parknote/.MainActivity
sleep 3
PID="$(adb shell pidof com.sl.parknote | tr -d '\r' || true)"
if [[ -z "$PID" ]]; then
  echo "Optimized release APK crashed or failed to launch" >&2
  adb logcat -d -s AndroidRuntime:E ActivityTaskManager:E | tail -100 >&2 || true
  exit 1
fi
echo "PASS: R8-optimized signed release APK launched (pid=$PID)"
adb shell am force-stop com.sl.parknote
rm -f "$SIGNED" "$KEY"
