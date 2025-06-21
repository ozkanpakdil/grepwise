import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import { useAuthStore } from '@/store/auth-store';
import Layout from '@/components/layout';
import LoginPage from '@/pages/login';
import SearchPage from '@/pages/search';
import AlarmsPage from '@/pages/alarms';
import SettingsPage from '@/pages/settings';
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
          <Route path="/alarms" element={isAuthenticated ? <AlarmsPage /> : <Navigate to="/login" replace />} />
          <Route path="/settings" element={isAuthenticated ? <SettingsPage /> : <Navigate to="/login" replace />} />
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