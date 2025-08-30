import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setAuthState } from '@/api/http';
import { apiUrl } from '@/config';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { notifyError, notifySuccess } from '@/lib/errorHandler';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { toast } = useToast();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!username || !password) {
      notifyError('Please enter both username and password', 'Error');
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch(apiUrl('/api/auth/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || 'Login failed');
      }
      const data = await res.json();
      setAuthState({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
        isAuthenticated: true,
      });
      notifySuccess('You have successfully logged in');
      navigate('/');
    } catch (error: any) {
      notifyError(error, 'Error', 'An error occurred during login');
      console.error('Login error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-md space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold">GrepWise</h1>
          <p className="mt-2 text-sm text-muted-foreground">An open-source alternative to Splunk</p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6" data-testid="login-form">
          <div className="space-y-4 rounded-md shadow-sm">
            <div>
              <label htmlFor="username" className="block text-sm font-medium">
                Username
              </label>
              <input
                id="username"
                data-testid="username"
                name="username"
                type="text"
                autoComplete="username"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="admin"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium">
                Password
              </label>
              <input
                id="password"
                data-testid="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="admin"
              />
            </div>
          </div>

          <div>
            <Button type="submit" className="w-full" disabled={isLoading} data-testid="sign-in">
              {isLoading ? 'Signing in...' : 'Sign in'}
            </Button>
          </div>

          <div className="text-center text-sm">
            <p className="text-muted-foreground">Default credentials: admin / admin</p>
          </div>
        </form>
      </div>
    </div>
  );
}
