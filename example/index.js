import { AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';
import { NotificationListenerAndroidHeadlessJsName } from 'react-native-notification-listener-android';

AppRegistry.registerComponent(appName, () => App);

// Handles every posted status-bar notification, delivered by the native
// NotificationListenerAndroidService even when the app is backgrounded/killed.
const headlessNotificationListener = async ({ notification }) => {
  if (!notification) return;
  const payload = JSON.parse(notification);
  console.log('[notification-listener-android] received:', payload);
};

AppRegistry.registerHeadlessTask(
  NotificationListenerAndroidHeadlessJsName,
  () => headlessNotificationListener
);
