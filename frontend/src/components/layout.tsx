import { Link, Outlet, useLocation } from 'react-router-dom';
import { useTheme } from '@/components/theme-provider';
import { clearAuthState, getAuthState } from '@/api/http';
import { Button } from '@/components/ui/button';
import {
  ActivityIcon,
  BellIcon,
  LayoutDashboard,
  LogOutIcon,
  MenuIcon,
  MoonIcon,
  SearchIcon,
  SettingsIcon,
  ShieldIcon,
  SunIcon,
  UsersIcon,
  XIcon,
} from 'lucide-react';
import { useState } from 'react';

export default function Layout() {
  const { theme, setTheme } = useTheme();
  const auth = getAuthState();
  const user: any = auth?.state?.user || null;
  const logout = () => {
    clearAuthState();
    window.location.href = '/login';
  };
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname === path + '/';
  };

  const toggleMobileMenu = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b bg-background sticky top-0 z-20">
        <div className="container flex h-16 items-center justify-between py-4">
          <div className="flex items-center gap-4">
            <div className="flex items-center md:hidden">
              <Button variant="ghost" size="icon" onClick={toggleMobileMenu} className="mr-2" aria-label="Toggle menu">
                <MenuIcon className="h-5 w-5" />
              </Button>
            </div>
            <Link to="/?home=1" className="flex items-center gap-2">
              <span className="font-bold text-xl">GrepWise</span>
            </Link>
            <nav className="hidden md:flex items-center gap-6 text-sm">
              <Link
                to="/search"
                className={`flex items-center gap-2 ${
                  isActive('/search') || isActive('/') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <SearchIcon className="h-4 w-4" />
                Search
              </Link>
              <Link
                to="/dashboards"
                className={`flex items-center gap-2 ${
                  isActive('/dashboards') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <LayoutDashboard className="h-4 w-4" />
                Dashboards
              </Link>
              <Link
                to="/alarms"
                className={`flex items-center gap-2 ${
                  isActive('/alarms') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <BellIcon className="h-4 w-4" />
                Alarms
              </Link>
              <Link
                to="/settings"
                className={`flex items-center gap-2 ${
                  isActive('/settings') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <SettingsIcon className="h-4 w-4" />
                Settings
              </Link>
              <Link
                to="/monitoring"
                className={`flex items-center gap-2 ${
                  isActive('/monitoring') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <ActivityIcon className="h-4 w-4" />
                Monitoring
              </Link>
              <Link
                to="/users"
                className={`flex items-center gap-2 ${
                  isActive('/users') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <UsersIcon className="h-4 w-4" />
                Users
              </Link>
              <Link
                to="/roles"
                className={`flex items-center gap-2 ${
                  isActive('/roles') ? 'text-foreground font-medium' : 'text-muted-foreground'
                } transition-colors hover:text-foreground`}
              >
                <ShieldIcon className="h-4 w-4" />
                Roles
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
              {theme === 'dark' ? <SunIcon className="h-5 w-5" /> : <MoonIcon className="h-5 w-5" />}
              <span className="sr-only">Toggle theme</span>
            </Button>
            {user && (
              <div className="flex items-center gap-4">
                <div className="hidden md:block text-sm">
                  <div className="font-medium">{user.username}</div>
                  <div className="text-muted-foreground">{user.email}</div>
                </div>
                <Button variant="ghost" size="icon" onClick={logout} title="Logout" data-testid="logout">
                  <LogOutIcon className="h-5 w-5" />
                  <span className="sr-only">Logout</span>
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Mobile menu */}
      <div
        className={`fixed inset-0 bg-black bg-opacity-50 z-10 transition-opacity duration-300 md:hidden ${
          mobileMenuOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
        onClick={toggleMobileMenu}
      >
        <div
          className={`fixed top-0 left-0 h-full w-3/4 max-w-xs bg-background shadow-lg transform transition-transform duration-300 ease-in-out ${
            mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
          }`}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex justify-between items-center p-4 border-b">
            <span className="font-bold text-lg">Menu</span>
            <Button variant="ghost" size="icon" onClick={toggleMobileMenu} aria-label="Close menu">
              <XIcon className="h-5 w-5" />
            </Button>
          </div>

          <nav className="p-4">
            <ul className="space-y-4">
              <li>
                <Link
                  to="/search"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/search') || isActive('/') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <SearchIcon className="h-5 w-5" />
                  <span>Search</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/dashboards"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/dashboards') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <LayoutDashboard className="h-5 w-5" />
                  <span>Dashboards</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/alarms"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/alarms') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <BellIcon className="h-5 w-5" />
                  <span>Alarms</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/settings"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/settings') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <SettingsIcon className="h-5 w-5" />
                  <span>Settings</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/monitoring"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/monitoring') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <ActivityIcon className="h-5 w-5" />
                  <span>Monitoring</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/users"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/users') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <UsersIcon className="h-5 w-5" />
                  <span>Users</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/roles"
                  className={`flex items-center gap-3 p-2 rounded-md ${
                    isActive('/roles') ? 'bg-muted font-medium' : ''
                  }`}
                  onClick={toggleMobileMenu}
                >
                  <ShieldIcon className="h-5 w-5" />
                  <span>Roles</span>
                </Link>
              </li>
            </ul>
          </nav>

          {user && (
            <div className="absolute bottom-0 left-0 right-0 border-t p-4">
              <div className="font-medium">{user.username}</div>
              <div className="text-muted-foreground text-sm">{user.email}</div>
            </div>
          )}
        </div>
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
          <p className="text-sm text-muted-foreground">An open-source alternative to Splunk</p>
        </div>
      </footer>
    </div>
  );
}
