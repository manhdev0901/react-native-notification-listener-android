import NativeNotificationListenerAndroid from './NativeNotificationListenerAndroid';

export type PermissionStatus = 'unknown' | 'authorized' | 'denied';

export type ConnectionLog = {
  type: 'connected' | 'disconnected';
  time: number;
};

// Matches the payload built natively in NotificationPayloadBuilder.kt from a
// StatusBarNotification. `icon`/`iconLarge`/`image` are base64 data URIs
// (icons are resized to a max of 128px before encoding to avoid
// TransactionTooLargeException on notifications with large avatars/images).
export type NotificationPayload = {
  app: string;
  title: string;
  titleBig: string;
  text: string;
  subText: string;
  summaryText: string;
  bigText: string;
  extraInfoText: string;
  icon: string;
  iconLarge: string;
  image: string;
  time: string;
};

// Name of the headless JS task the native NotificationListenerService starts
// for every posted status-bar notification. Register a handler with
// `AppRegistry.registerHeadlessTask(NotificationListenerAndroidHeadlessJsName, () => handler)`.
// The task receives `{ notification: string }` — `notification` is a JSON
// string (parse it into a `NotificationPayload`).
export const NotificationListenerAndroidHeadlessJsName =
  'NotificationListenerAndroidHeadlessTask';

const NotificationListenerAndroid = {
  /** Opens the system "Notification access" settings screen for this app. */
  requestPermission(): void {
    NativeNotificationListenerAndroid.requestPermission();
  },

  /** Resolves whether this app currently has notification-listener access. */
  getPermissionStatus(): Promise<PermissionStatus> {
    return NativeNotificationListenerAndroid.getPermissionStatus() as Promise<PermissionStatus>;
  },

  /**
   * Asks the OS to rebind the NotificationListenerService without sending
   * the user to Settings. Only works if permission is still granted but the
   * binding was dropped (e.g. the service crashed or was killed by the
   * OS/OEM battery manager) — a no-op if permission was actually revoked.
   */
  requestRebind(): void {
    NativeNotificationListenerAndroid.requestRebind();
  },

  /** Resolves the recent connect/disconnect history of the listener service, for diagnostics. */
  getConnectionLogs(): Promise<ConnectionLog[]> {
    return NativeNotificationListenerAndroid.getConnectionLogs() as unknown as Promise<
      ConnectionLog[]
    >;
  },

  /** Clears the stored connect/disconnect history. */
  clearConnectionLogs(): void {
    NativeNotificationListenerAndroid.clearConnectionLogs();
  },
};

export default NotificationListenerAndroid;
