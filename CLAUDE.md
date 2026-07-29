# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android `NotificationListenerService` bridge for React Native (TurboModule, Old + New Architecture). Built from scratch as a modern replacement for `react-native-android-notification-listener`, which crashes on Android 12+ (`ForegroundServiceDidNotStartInTimeException`) because its boot receiver calls `startForegroundService()` on a service that never calls `startForeground()`. This library's entire design avoids that failure mode: it never starts the listener service via `startForegroundService()`/`startService()` anywhere, relying instead on Android's automatic `NotificationListenerService` rebinding after reboot plus an explicit `requestRebind()` API.

Android-only — `NotificationListenerService` has no iOS equivalent. The iOS native module (`ios/NotificationListenerAndroid.mm`) exists only so the package doesn't break iOS builds; every method is a safe no-op/empty-resolve stub.

Node version: see `.nvmrc`. Package manager: Yarn (monorepo via Yarn workspaces — do not use npm, see CONTRIBUTING.md).

## Commands

```bash
# Install deps for both the library and example app (run from repo root)
yarn

# Typecheck / lint the library
yarn typecheck
yarn lint
yarn lint --fix

# Build the library (babel + tsc -> lib/module, lib/typescript)
# Required at least once before Metro can resolve the package via its
# "main" field fallback (the package.json exports also declare a
# react-native-notification-listener-android-source condition so Metro can
# resolve straight to src/ during development if that condition is wired up
# in the consumer's metro.config.js — the example app's config already does
# this via react-native-monorepo-config).
yarn prepare

# Example app (from repo root; example/ is a Yarn workspace)
yarn example start           # Metro, from example/
yarn example android         # build + install + run on connected device/emulator
yarn example build:android   # release-style Android build, arm64-v8a only
```

No test runner is configured in this repo.

### Local dev-loop gotcha (multiple Metro instances on one machine/emulator)

Android emulators resolve a RN debug app's default dev-server host (`10.0.2.2:8081`) straight to the **host machine's real port 8081**, bypassing `adb reverse` entirely. If another Metro instance for a different project is already running on host port 8081, this project's example app will silently load *that other project's bundle* instead of failing to connect — the symptom is a redbox for a native module that doesn't exist in this repo at all (e.g. an unrelated Firebase module). If you hit that, either free port 8081 or point this app's dev server explicitly at a different port: run Metro with `--port <N>`, `adb reverse tcp:<N> tcp:<N>`, and override the app's `debug_http_host` (SharedPreferences key read by `PackagerConnectionSettings.kt`) to `localhost:<N>` — either via the in-app Dev Menu ("Debug server host & port for device") or by writing it directly for a debug build via `adb shell run-as <pkg> ...` into `shared_prefs/<pkg>_preferences.xml`.

## Architecture

### Two-layer JS API (`src/`)

- `NativeNotificationListenerAndroid.ts` — the raw TurboModule `Spec`. Deliberately typed loosely (`Promise<string>` instead of a union literal, plain object instead of a typed shape) because TurboModule codegen has historically been unreliable with union literals nested inside `Promise<T>`.
- `index.tsx` — the public API. Wraps the raw native module and narrows return types to the real ergonomic types (`PermissionStatus`, `ConnectionLog`, `NotificationPayload`). Also exports `NotificationListenerAndroidHeadlessJsName`, the string every consumer must pass to `AppRegistry.registerHeadlessTask`.

Always add new native methods to both files: the loose contract in `NativeNotificationListenerAndroid.ts`, the typed wrapper in `index.tsx`.

### Native Android (`android/src/main/java/com/notificationlistenerandroid/`)

- `NotificationListenerAndroidModule.kt` — the TurboModule (`requestPermission`, `getPermissionStatus`, `requestRebind`, `getConnectionLogs`, `clearConnectionLogs`). Extends the codegen-generated `NativeNotificationListenerAndroidSpec` abstract class (regenerated on every build from `NativeNotificationListenerAndroid.ts` — do not hand-edit generated code).
- `NotificationListenerAndroidService.kt` — the actual `NotificationListenerService`. On `onNotificationPosted`, builds a payload via `NotificationPayloadBuilder` and forwards it to the headless task service; wrapped in try/catch so a bad payload (e.g. `TransactionTooLargeException` from an oversized icon) can't crash and unbind the whole listener. On `onListenerConnected`/`onListenerDisconnected`, appends to a rolling connect/disconnect log (SharedPreferences, capped at 50 entries) and self-heals via `requestRebind()` on disconnect.
- `NotificationListenerAndroidHeadlessTaskService.kt` — delivers one notification payload to the JS headless task (`HeadlessJsTaskConfig`, task name must match `NotificationListenerAndroidHeadlessJsName` in `index.tsx`).
- `NotificationPayloadBuilder.kt` — builds the JSON payload from a `StatusBarNotification`. Resizes icons to a max of 128px before base64-encoding, specifically to avoid `TransactionTooLargeException` on notifications with large avatars/images.
- `BootReceiver.kt` — on `BOOT_COMPLETED`, calls `NotificationListenerService.requestRebind()` only. **Never** add a `startForegroundService()`/`startService()` call here — that is the exact bug this library was written to avoid (see Project Overview).
- `AndroidManifest.xml` — intentionally declares no `FOREGROUND_SERVICE` permission or `foregroundServiceType`, because the service is never started as a foreground service.

Connection-log SharedPreferences constants (`CONNECTION_LOG_PREFS`, `CONNECTION_LOG_KEY`) live on `NotificationListenerAndroidService`'s companion object; the module reads/writes the same keys rather than duplicating them.

### iOS (`ios/`)

`NotificationListenerAndroid.mm` implements the same TurboModule protocol as Android but every method is a no-op or resolves an empty/default value. Keep this in sync with the `Spec` interface whenever a method is added, purely so iOS builds of consumer apps don't break — no real behavior is expected here.
