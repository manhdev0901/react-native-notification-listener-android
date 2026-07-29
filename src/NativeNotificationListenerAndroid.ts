import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// Native contract kept intentionally loose (plain `string` instead of a union
// literal, plain object instead of a typed shape) — TurboModule codegen has a
// history of being finicky with union literals nested inside Promise<T> across
// RN versions. The typed, ergonomic surface lives in `src/index.tsx`, which
// narrows these at the JS layer.
export interface NativeConnectionLogEntry {
  type: string;
  time: number;
}

export interface Spec extends TurboModule {
  requestPermission(): void;
  getPermissionStatus(): Promise<string>;
  requestRebind(): void;
  getConnectionLogs(): Promise<NativeConnectionLogEntry[]>;
  clearConnectionLogs(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'NotificationListenerAndroid'
);
