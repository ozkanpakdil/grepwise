import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import { useAuthStore } from '@/store/auth-store';
import Layout from '@/components/layout';
import LoginPage from '@/pages/login';
import SearchPage from '@/pages/search';
import AlarmsPage from '@/pages/alarms';
import AlarmMonitoringPage from '@/pages/alarm-monitoring';
import SettingsPage from '@/pages/settings';
import DashboardsPage from '@/pages/dashboards';
import DashboardView from '@/pages/dashboard-view';
import UsersPage from '@/pages/users';
import MonitoringPage from '@/pages/monitoring';
import NotFoundPage from '@/pages/not-found';

function App() {
  const { isAuthenticated } = useAuthStore();

  return (
    <>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" replace />} />

        {/* Protected routes */}
        <Route element={<Layout />}>
          <Route path="/" element={isAuthenticated ? <SearchPage /> : <Navigate to="/login" replace />} />
          <Route path="/search" element={isAuthenticated ? <SearchPage /> : <Navigate to="/login" replace />} />
          <Route path="/dashboards" element={isAuthenticated ? <DashboardsPage /> : <Navigate to="/login" replace />} />
          <Route path="/dashboards/:id" element={isAuthenticated ? <DashboardView /> : <Navigate to="/login" replace />} />
          <Route path="/alarms" element={isAuthenticated ? <AlarmsPage /> : <Navigate to="/login" replace />} />
          <Route path="/alarm-monitoring" element={isAuthenticated ? <AlarmMonitoringPage /> : <Navigate to="/login" replace />} />
          <Route path="/settings" element={isAuthenticated ? <SettingsPage /> : <Navigate to="/login" replace />} />
          <Route path="/monitoring" element={isAuthenticated ? <MonitoringPage /> : <Navigate to="/login" replace />} />
          <Route path="/users" element={isAuthenticated ? <UsersPage /> : <Navigate to="/login" replace />} />
        </Route>

        {/* 404 route */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>

      {/* Toast notifications */}
      <Toaster />
    </>
  );
}

export default App;
