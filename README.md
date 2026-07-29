# react-native-notification-listener-android

Android `NotificationListenerService` bridge for React Native, built for
Android 13-16+. Written as a from-scratch replacement for
[`react-native-android-notification-listener`](https://github.com/leandrosimoes/react-native-android-notification-listener),
which hasn't been updated in years and crashes on Android 12+
(`ForegroundServiceDidNotStartInTimeException`, caused by its boot receiver
calling `startForegroundService()` on a service that never calls
`startForeground()`).

Android-only: `NotificationListenerService` has no iOS equivalent. On iOS
this module's methods are safe no-ops (`getPermissionStatus` resolves
`'denied'`, `getConnectionLogs` resolves `[]`).

## Why not just patch the old library

This library fixes the root causes instead of working around them:

- **No boot-time `startForegroundService()` at all.** Android automatically
  rebinds a `NotificationListenerService` after reboot as long as
  notification-listener permission is still granted — no foreground service
  is needed to "keep it alive". The optional `BOOT_COMPLETED` receiver only
  calls `NotificationListenerService.requestRebind()`, never starts a
  service.
- **Self-healing on disconnect.** `onListenerDisconnected()` immediately
  requests a rebind, in addition to the app itself being able to call
  `requestRebind()` on foreground.
- **Payload size guarded.** Notification icons/images are resized before
  base64-encoding, and delivery to the JS headless task is wrapped so a
  malformed/oversized notification can't crash (and unbind) the listener
  service.
- **Connection diagnostics built in.** `getConnectionLogs()` /
  `clearConnectionLogs()` expose a rolling connect/disconnect history for
  debugging permission or OEM-battery-killer issues in the field.

## Installation

```sh
yarn add react-native-notification-listener-android
```

No extra native setup needed — the service, headless-task service, and boot
receiver are all registered via the library's own `AndroidManifest.xml` and
merged in automatically through autolinking.

## Usage

```ts
import NotificationListenerAndroid, {
  NotificationListenerAndroidHeadlessJsName,
  type NotificationPayload,
} from 'react-native-notification-listener-android';

// 1. Ask the user to grant "Notification access" (opens system Settings).
NotificationListenerAndroid.requestPermission();

// 2. Check current status: 'unknown' | 'authorized' | 'denied'.
const status = await NotificationListenerAndroid.getPermissionStatus();

// 3. Register a headless task (e.g. in index.js) to receive notifications,
//    including while the app is backgrounded or killed.
AppRegistry.registerHeadlessTask(
  NotificationListenerAndroidHeadlessJsName,
  () => async ({ notification }) => {
    const payload: NotificationPayload = JSON.parse(notification);
    // payload.app / payload.title / payload.text / payload.time, etc.
  }
);

// 4. If the binding drops (service crash, OEM battery killer), recover
//    without sending the user back to Settings.
NotificationListenerAndroid.requestRebind();

// 5. Diagnostics.
const logs = await NotificationListenerAndroid.getConnectionLogs(); // {type: 'connected'|'disconnected', time: number}[]
NotificationListenerAndroid.clearConnectionLogs();
```

## API

| Method | Returns | Description |
|---|---|---|
| `requestPermission()` | `void` | Opens the system "Notification access" settings screen. |
| `getPermissionStatus()` | `Promise<'unknown' \| 'authorized' \| 'denied'>` | Current permission state. |
| `requestRebind()` | `void` | Asks the OS to rebind the listener service without going through Settings. No-op if permission is actually revoked. |
| `getConnectionLogs()` | `Promise<{type: 'connected'\|'disconnected', time: number}[]>` | Rolling history (last 50) of listener connect/disconnect events. |
| `clearConnectionLogs()` | `void` | Clears the stored history. |

## Migrating from `react-native-android-notification-listener`

The API mirrors the old library closely, but isn't a byte-for-byte drop-in:

- Import path changes: `react-native-android-notification-listener` → `react-native-notification-listener-android`.
- Headless task name export renamed: `RNAndroidNotificationListenerHeadlessJsName` → `NotificationListenerAndroidHeadlessJsName`.
- `requestRebind()` / `getConnectionLogs()` / `clearConnectionLogs()` are first-class here (they only existed as a local `patch-package` patch on the old library).
- Notification payload shape is unchanged (`app`, `title`, `text`, `time`, plus the same extra fields).

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
