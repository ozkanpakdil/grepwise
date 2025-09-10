import { Navigate, Route, Routes } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import { getAuthState } from '@/api/http';
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
import RolesPage from '@/pages/roles';
import RedactionEditorPage from '@/pages/redaction-editor';

import { useEffect, useState } from 'react';

function App() {
  const [token, setToken] = useState<string | null>(getAuthState()?.state?.accessToken || null);
  const isAuthenticated = !!token;
  const user: any = getAuthState()?.state?.user || null;
  const roleNames: string[] = Array.isArray((user as any)?.roleNames)
    ? (user as any).roleNames
    : Array.isArray((user as any)?.roles)
    ? (user as any).roles
    : [];
  const isAdmin = roleNames.includes('ADMIN');

  useEffect(() => {
    const update = () => setToken(getAuthState()?.state?.accessToken || null);
    window.addEventListener('grepwise-auth-changed', update);
    window.addEventListener('storage', update);
    return () => {
      window.removeEventListener('grepwise-auth-changed', update);
      window.removeEventListener('storage', update);
    };
  }, []);

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
          <Route
            path="/dashboards/:id"
            element={isAuthenticated ? <DashboardView /> : <Navigate to="/login" replace />}
          />
          <Route path="/alarms" element={isAuthenticated ? <AlarmsPage /> : <Navigate to="/login" replace />} />
          <Route
            path="/alarm-monitoring"
            element={isAuthenticated ? <AlarmMonitoringPage /> : <Navigate to="/login" replace />}
          />
          <Route path="/settings" element={isAuthenticated ? (isAdmin ? <SettingsPage /> : <Navigate to="/search" replace />) : <Navigate to="/login" replace />} />
          <Route path="/monitoring" element={isAuthenticated ? <MonitoringPage /> : <Navigate to="/login" replace />} />
          <Route path="/users" element={isAuthenticated ? (isAdmin ? <UsersPage /> : <Navigate to="/search" replace />) : <Navigate to="/login" replace />} />
          <Route path="/roles" element={isAuthenticated ? (isAdmin ? <RolesPage /> : <Navigate to="/search" replace />) : <Navigate to="/login" replace />} />
          <Route path="/admin/redaction" element={isAuthenticated ? (isAdmin ? <RedactionEditorPage /> : <Navigate to="/search" replace />) : <Navigate to="/login" replace />} />
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
