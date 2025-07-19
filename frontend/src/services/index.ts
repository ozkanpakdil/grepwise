// Export all service classes and instances

// Log Service
export { default as LogService } from './LogService';
export { logService } from './LogService';

// Alarm Service
export { default as AlarmService } from './AlarmService';
export { alarmService } from './AlarmService';

// Auth Service
export { default as AuthService } from './AuthService';
export { authService } from './AuthService';

// Re-export for convenience
export * from '../generated/logservice_pb';
export * from '../generated/alarmservice_pb';
export * from '../generated/authservice_pb';