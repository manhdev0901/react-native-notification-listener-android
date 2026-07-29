import NotificationListenerAndroid from './NativeNotificationListenerAndroid';

export function multiply(a: number, b: number): number {
  return NotificationListenerAndroid.multiply(a, b);
}
