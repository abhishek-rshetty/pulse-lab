# Pulse Lab

An experimental, local-first Android companion prototype for WHOOP 5.0.

**Status:** Demo mode works from synthetic values. Live mode scans and inspects Bluetooth LE services. It does not decode proprietary WHOOP data, compute recovery, or sync sleep. Not intended to replace the WHOOP app yet.

## Build

Open with Android Studio or run `gradle :core:test :app:assembleDebug :app:lintDebug` using JDK 17 and Android SDK 35. GitHub Actions runs the checks and uploads an APK on success.

## Safety and privacy

- No INTERNET permission.
- Bluetooth requires user approval and is only started on button press.
- Device addresses are not shown or retained.
- Displayed demo data is labelled **SYNTHETIC**.
- Debug builds may be flagged by Android; do not disable Play Protect.
- No authentication, cloud storage, or access to WHOOP credentials.

## Known missing validation

WHOOP firmware compatibility, overnight measurements, background scanning, and on-device integration tests are not yet verified. Passing unit tests and CI builds does not validate these functions.
