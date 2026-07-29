import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import NotificationListenerAndroid, {
  type ConnectionLog,
  type PermissionStatus,
} from 'react-native-notification-listener-android';

export default function App() {
  const [status, setStatus] = useState<PermissionStatus>('unknown');
  const [logs, setLogs] = useState<ConnectionLog[]>([]);

  const refresh = useCallback(async () => {
    setStatus(await NotificationListenerAndroid.getPermissionStatus());
    setLogs(await NotificationListenerAndroid.getConnectionLogs());
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Notification Listener Android</Text>
      <Text style={styles.status}>Permission status: {status}</Text>

      <View style={styles.buttonRow}>
        <Button
          title="Request permission"
          onPress={NotificationListenerAndroid.requestPermission}
        />
        <Button
          title="Request rebind"
          onPress={NotificationListenerAndroid.requestRebind}
        />
      </View>
      <View style={styles.buttonRow}>
        <Button title="Refresh" onPress={refresh} />
        <Button
          title="Clear logs"
          onPress={async () => {
            NotificationListenerAndroid.clearConnectionLogs();
            await refresh();
          }}
        />
      </View>

      <Text style={styles.sectionTitle}>Connection logs</Text>
      <FlatList
        data={logs}
        keyExtractor={(_, index) => String(index)}
        renderItem={({ item }) => (
          <Text style={styles.logRow}>
            {new Date(item.time).toLocaleTimeString()} — {item.type}
          </Text>
        )}
        ListEmptyComponent={<Text style={styles.logRow}>No logs yet</Text>}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16 },
  title: { fontSize: 18, fontWeight: '600', marginBottom: 8 },
  status: { fontSize: 14, marginBottom: 16 },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
  },
  logRow: { fontSize: 13, paddingVertical: 4 },
});
