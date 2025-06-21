import { Outlet } from 'react-router-dom';
import { useTheme } from '@/components/theme-provider';
import { useAuthStore } from '@/store/auth-store';
import { Button } from '@/components/ui/button';
import { MoonIcon, SunIcon, LogOutIcon, SettingsIcon, SearchIcon, BellIcon } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';

export default function Layout() {
  const { theme, setTheme } = useTheme();
  const { user, logout } = useAuthStore();
  const location = useLocation();

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname === path + '/';
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b bg-background sticky top-0 z-10">
        <div className="container flex h-16 items-center justify-between py-4">
          <div className="flex items-center gap-4">
            <Link to="/" className="flex items-center gap-2">
              <span className="font-bold text-xl">GrepWise</span>
            </Link>
            <nav className="hidden md:flex items-center gap-6 text-sm">
              <Link
                to="/search"
                className={`flex items-center gap-2 ${
                  isActive('/search') || isActive('/')
                    ? 'text-foreground font-medium'
                    : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <SearchIcon className="h-4 w-4" />
                Search
              </Link>
              <Link
                to="/alarms"
                className={`flex items-center gap-2 ${
                  isActive('/alarms')
                    ? 'text-foreground font-medium'
                    : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <BellIcon className="h-4 w-4" />
                Alarms
              </Link>
              <Link
                to="/settings"
                className={`flex items-center gap-2 ${
                  isActive('/settings')
                    ? 'text-foreground font-medium'
                    : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <SettingsIcon className="h-4 w-4" />
                Settings
              </Link>
            </nav>
          </div>
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
              title={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            >
              {theme === 'dark' ? (
                <SunIcon className="h-5 w-5" />
              ) : (
                <MoonIcon className="h-5 w-5" />
              )}
              <span className="sr-only">Toggle theme</span>
            </Button>
            {user && (
              <div className="flex items-center gap-4">
                <div className="hidden md:block text-sm">
                  <div className="font-medium">{user.username}</div>
                  <div className="text-muted-foreground">{user.email}</div>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={logout}
                  title="Logout"
                >
                  <LogOutIcon className="h-5 w-5" />
                  <span className="sr-only">Logout</span>
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Mobile navigation */}
      <div className="md:hidden border-b bg-background">
        <nav className="flex justify-between px-4 py-2">
          <Link
            to="/search"
            className={`flex flex-col items-center gap-1 px-4 py-2 ${
              isActive('/search') || isActive('/')
                ? 'text-foreground font-medium'
                : 'text-muted-foreground'
            } transition-colors hover:text-foreground`}
          >
            <SearchIcon className="h-5 w-5" />
            <span className="text-xs">Search</span>
          </Link>
          <Link
            to="/alarms"
            className={`flex flex-col items-center gap-1 px-4 py-2 ${
              isActive('/alarms')
                ? 'text-foreground font-medium'
                : 'text-muted-foreground'
            } transition-colors hover:text-foreground`}
          >
            <BellIcon className="h-5 w-5" />
            <span className="text-xs">Alarms</span>
          </Link>
          <Link
            to="/settings"
            className={`flex flex-col items-center gap-1 px-4 py-2 ${
              isActive('/settings')
                ? 'text-foreground font-medium'
                : 'text-muted-foreground'
            } transition-colors hover:text-foreground`}
          >
            <SettingsIcon className="h-5 w-5" />
            <span className="text-xs">Settings</span>
          </Link>
        </nav>
      </div>

      {/* Main content */}
      <main className="flex-1 container py-6">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t py-6 md:py-0">
        <div className="container flex flex-col md:flex-row items-center justify-between gap-4 md:h-16">
          <p className="text-sm text-muted-foreground">
            &copy; {new Date().getFullYear()} GrepWise. All rights reserved.
          </p>
          <p className="text-sm text-muted-foreground">
            An open-source alternative to Splunk
          </p>
        </div>
      </footer>
    </div>
  );
}