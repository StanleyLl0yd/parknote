# ParkNote 0.9 beta: physical device acceptance (DEFERRED)

This list is intentionally **not** claimed as passed by emulator or CI.
No store publication is authorized until an explicit separate approval.

## Installer and upgrade

- [ ] Install the release-signed 0.9 beta APK on a real phone.
- [ ] Install over a signed 0.1.0 build and verify Android accepts the update
      with the same signing identity (versionCode 2).
- [ ] Confirm the previously saved parking note and both photos survive the upgrade.
- [ ] Confirm installed version name/code and package `com.sl.parknote`.
- [ ] Check a supported device with a 16 KiB memory page size installs and starts the APK.

## No connectivity, no surveillance

- [ ] In airplane mode, create an underground parking note with floor, sector and space.
- [ ] Relaunch offline and verify the record survives. No Google Play Services should be required.
- [ ] Confirm no Internet, camera or media-read runtime permissions were granted to ParkNote.
- [ ] Deny location and ensure manual entry, save, edit and finish still work.
- [ ] Grant approximate and fine location separately, verifying a single explicit location request.
- [ ] Ensure there is no background location acquisition or startup GPS request.
- [ ] Test an external maps app, and graceful behavior without any map handler.

## OEM camera / Photo Picker

- [ ] Take an in-app photo with a real system camera and save it.
- [ ] Add a second photo via Photo Picker, then edit/replace/remove one photo.
- [ ] Photograph a rotated document: preview should display upright after EXIF normalization.
- [ ] Check that imported private JPEGs contain no source GPS/camera metadata.
- [ ] Deny/cancel camera and Photo Picker, then confirm the previous record is still intact.
- [ ] Rotate screen and background/foreground the app during camera/picker handoff.
- [ ] Kill the app process during capture/picker and verify recovery does not erase saved data.
- [ ] Test inaccessible/corrupt pictures and an empty/low-storage device safely.

## Lifecycle, accessibility and recovery

- [ ] Create, edit, cancel an edit, finish and create a new record.
- [ ] Rotate device repeatedly during editor entry, save and finish confirmations.
- [ ] Use largest font/display scaling, screen reader, landscape, light/dark modes.
- [ ] Force-stop/restart and reboot; verify active parking persists and deleted parking stays deleted.
- [ ] Confirm damaged storage produces a clear error without silently overwriting original bytes.
- [ ] Confirm the app's cloud/device-transfer backup exclusion on representative devices.

## Gate

Physical tests are planned for the first beta feedback cycle, not required to
continue development. Any failures must be fixed and retested before version 1.0.
