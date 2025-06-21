import { useState, useEffect } from 'react';
import { useTheme } from '@/components/theme-provider';
import { useAuthStore } from '@/store/auth-store';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { 
  getLogDirectoryConfigs, 
  createLogDirectoryConfig, 
  updateLogDirectoryConfig, 
  deleteLogDirectoryConfig,
  scanLogDirectory,
  scanAllLogDirectories,
  LogDirectoryConfig
} from '@/api/logDirectoryConfig';

export default function SettingsPage() {
  const { theme, setTheme } = useTheme();
  const { user, updateUser } = useAuthStore();
  const { toast } = useToast();

  // User profile form state
  const [profileForm, setProfileForm] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
  });

  // Password change form state
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // Loading states
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isLoadingConfigs, setIsLoadingConfigs] = useState(false);
  const [isSavingConfig, setIsSavingConfig] = useState(false);
  const [isScanning, setIsScanning] = useState(false);

  // Log directory configurations
  const [logDirectoryConfigs, setLogDirectoryConfigs] = useState<LogDirectoryConfig[]>([]);
  const [newConfig, setNewConfig] = useState<LogDirectoryConfig>({
    directoryPath: '',
    enabled: true,
    filePattern: '*.log',
    scanIntervalSeconds: 60
  });

  // Load log directory configurations
  useEffect(() => {
    const fetchConfigs = async () => {
      setIsLoadingConfigs(true);
      try {
        const configs = await getLogDirectoryConfigs();
        setLogDirectoryConfigs(configs);
      } catch (error) {
        console.error('Error fetching log directory configs:', error);
        toast({
          title: 'Error',
          description: 'Failed to load log directory configurations',
          variant: 'destructive',
        });
      } finally {
        setIsLoadingConfigs(false);
      }
    };

    fetchConfigs();
  }, []);

  // Handle creating a new log directory configuration
  const handleCreateConfig = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newConfig.directoryPath) {
      toast({
        title: 'Error',
        description: 'Please enter a directory path',
        variant: 'destructive',
      });
      return;
    }

    setIsSavingConfig(true);

    try {
      const createdConfig = await createLogDirectoryConfig(newConfig);
      setLogDirectoryConfigs([...logDirectoryConfigs, createdConfig]);
      setNewConfig({
        directoryPath: '',
        enabled: true,
        filePattern: '*.log',
        scanIntervalSeconds: 60
      });

      toast({
        title: 'Configuration created',
        description: 'Log directory configuration has been created successfully',
      });
    } catch (error) {
      console.error('Error creating log directory config:', error);
      toast({
        title: 'Error',
        description: 'Failed to create log directory configuration',
        variant: 'destructive',
      });
    } finally {
      setIsSavingConfig(false);
    }
  };

  // Handle updating a log directory configuration
  const handleUpdateConfig = async (config: LogDirectoryConfig) => {
    if (!config.id) return;

    try {
      const updatedConfig = await updateLogDirectoryConfig(config.id, config);
      setLogDirectoryConfigs(logDirectoryConfigs.map(c => c.id === config.id ? updatedConfig : c));

      toast({
        title: 'Configuration updated',
        description: 'Log directory configuration has been updated successfully',
      });
    } catch (error) {
      console.error('Error updating log directory config:', error);
      toast({
        title: 'Error',
        description: 'Failed to update log directory configuration',
        variant: 'destructive',
      });
    }
  };

  // Handle deleting a log directory configuration
  const handleDeleteConfig = async (id: string) => {
    try {
      await deleteLogDirectoryConfig(id);
      setLogDirectoryConfigs(logDirectoryConfigs.filter(c => c.id !== id));

      toast({
        title: 'Configuration deleted',
        description: 'Log directory configuration has been deleted successfully',
      });
    } catch (error) {
      console.error('Error deleting log directory config:', error);
      toast({
        title: 'Error',
        description: 'Failed to delete log directory configuration',
        variant: 'destructive',
      });
    }
  };

  // Handle scanning a log directory
  const handleScanDirectory = async (id: string) => {
    setIsScanning(true);

    try {
      const processedCount = await scanLogDirectory(id);

      toast({
        title: 'Scan completed',
        description: `Processed ${processedCount} log entries`,
      });
    } catch (error) {
      console.error('Error scanning log directory:', error);
      toast({
        title: 'Error',
        description: 'Failed to scan log directory',
        variant: 'destructive',
      });
    } finally {
      setIsScanning(false);
    }
  };

  // Handle scanning all log directories
  const handleScanAllDirectories = async () => {
    setIsScanning(true);

    try {
      const scannedCount = await scanAllLogDirectories();

      toast({
        title: 'Scan completed',
        description: `Scanned ${scannedCount} log directories`,
      });
    } catch (error) {
      console.error('Error scanning all log directories:', error);
      toast({
        title: 'Error',
        description: 'Failed to scan log directories',
        variant: 'destructive',
      });
    } finally {
      setIsScanning(false);
    }
  };

  // Handle profile form submission
  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!profileForm.firstName || !profileForm.lastName || !profileForm.email) {
      toast({
        title: 'Error',
        description: 'Please fill in all required fields',
        variant: 'destructive',
      });
      return;
    }

    setIsUpdatingProfile(true);

    try {
      // In a real app, this would be an API call to update the user profile
      // For now, we'll just simulate a successful update
      await new Promise(resolve => setTimeout(resolve, 500));

      updateUser({
        firstName: profileForm.firstName,
        lastName: profileForm.lastName,
        email: profileForm.email,
      });

      toast({
        title: 'Profile updated',
        description: 'Your profile has been updated successfully',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'An error occurred while updating your profile',
        variant: 'destructive',
      });
      console.error('Profile update error:', error);
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  // Handle password form submission
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
      toast({
        title: 'Error',
        description: 'Please fill in all password fields',
        variant: 'destructive',
      });
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast({
        title: 'Error',
        description: 'New password and confirmation do not match',
        variant: 'destructive',
      });
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      toast({
        title: 'Error',
        description: 'New password must be at least 8 characters long',
        variant: 'destructive',
      });
      return;
    }

    setIsChangingPassword(true);

    try {
      // In a real app, this would be an API call to change the password
      // For now, we'll just simulate a successful password change
      await new Promise(resolve => setTimeout(resolve, 500));

      // Reset the password form
      setPasswordForm({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });

      toast({
        title: 'Password changed',
        description: 'Your password has been changed successfully',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'An error occurred while changing your password',
        variant: 'destructive',
      });
      console.error('Password change error:', error);
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="space-y-10">
      <div>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="text-muted-foreground">
          Manage your account settings and preferences
        </p>
      </div>

      {/* Theme Settings */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Appearance</h2>
        <div className="border rounded-md p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-medium">Theme</h3>
              <p className="text-sm text-muted-foreground">
                Choose your preferred theme
              </p>
            </div>
            <div className="flex space-x-2">
              <Button
                variant={theme === 'light' ? 'default' : 'outline'}
                onClick={() => setTheme('light')}
              >
                Light
              </Button>
              <Button
                variant={theme === 'dark' ? 'default' : 'outline'}
                onClick={() => setTheme('dark')}
              >
                Dark
              </Button>
              <Button
                variant={theme === 'system' ? 'default' : 'outline'}
                onClick={() => setTheme('system')}
              >
                System
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Profile Settings */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Profile</h2>
        <div className="border rounded-md p-4">
          <form onSubmit={handleProfileSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="firstName" className="block text-sm font-medium">
                  First Name
                </label>
                <input
                  id="firstName"
                  type="text"
                  value={profileForm.firstName}
                  onChange={(e) => setProfileForm({...profileForm, firstName: e.target.value})}
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  required
                />
              </div>
              <div>
                <label htmlFor="lastName" className="block text-sm font-medium">
                  Last Name
                </label>
                <input
                  id="lastName"
                  type="text"
                  value={profileForm.lastName}
                  onChange={(e) => setProfileForm({...profileForm, lastName: e.target.value})}
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  required
                />
              </div>
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={profileForm.email}
                onChange={(e) => setProfileForm({...profileForm, email: e.target.value})}
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                required
              />
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={isUpdatingProfile}>
                {isUpdatingProfile ? 'Saving...' : 'Save Changes'}
              </Button>
            </div>
          </form>
        </div>
      </div>

      {/* Password Settings */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Password</h2>
        <div className="border rounded-md p-4">
          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div>
              <label htmlFor="currentPassword" className="block text-sm font-medium">
                Current Password
              </label>
              <input
                id="currentPassword"
                type="password"
                value={passwordForm.currentPassword}
                onChange={(e) => setPasswordForm({...passwordForm, currentPassword: e.target.value})}
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                required
              />
            </div>
            <div>
              <label htmlFor="newPassword" className="block text-sm font-medium">
                New Password
              </label>
              <input
                id="newPassword"
                type="password"
                value={passwordForm.newPassword}
                onChange={(e) => setPasswordForm({...passwordForm, newPassword: e.target.value})}
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                required
                minLength={8}
              />
              <p className="mt-1 text-xs text-muted-foreground">
                Password must be at least 8 characters long
              </p>
            </div>
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium">
                Confirm New Password
              </label>
              <input
                id="confirmPassword"
                type="password"
                value={passwordForm.confirmPassword}
                onChange={(e) => setPasswordForm({...passwordForm, confirmPassword: e.target.value})}
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                required
              />
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={isChangingPassword}>
                {isChangingPassword ? 'Changing...' : 'Change Password'}
              </Button>
            </div>
          </form>
        </div>
      </div>

      {/* Account Information */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Account Information</h2>
        <div className="border rounded-md p-4">
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-sm font-medium">Username</span>
              <span className="text-sm">{user?.username}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm font-medium">Role</span>
              <span className="text-sm">{user?.roles?.join(', ')}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Log Directory Configuration */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Log Directory Configuration</h2>
        <div className="border rounded-md p-4">
          <div className="space-y-6">
            {/* Add new configuration */}
            <div>
              <h3 className="text-lg font-medium mb-4">Add New Log Directory</h3>
              <form onSubmit={handleCreateConfig} className="space-y-4">
                <div>
                  <label htmlFor="directoryPath" className="block text-sm font-medium">
                    Directory Path
                  </label>
                  <input
                    id="directoryPath"
                    type="text"
                    value={newConfig.directoryPath}
                    onChange={(e) => setNewConfig({...newConfig, directoryPath: e.target.value})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    placeholder="C:\logs"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="filePattern" className="block text-sm font-medium">
                    File Pattern
                  </label>
                  <input
                    id="filePattern"
                    type="text"
                    value={newConfig.filePattern}
                    onChange={(e) => setNewConfig({...newConfig, filePattern: e.target.value})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    placeholder="*.log"
                  />
                </div>
                <div>
                  <label htmlFor="scanInterval" className="block text-sm font-medium">
                    Scan Interval (seconds)
                  </label>
                  <input
                    id="scanInterval"
                    type="number"
                    value={newConfig.scanIntervalSeconds}
                    onChange={(e) => setNewConfig({...newConfig, scanIntervalSeconds: parseInt(e.target.value)})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    min="10"
                    max="3600"
                  />
                </div>
                <div className="flex items-center">
                  <input
                    id="enabled"
                    type="checkbox"
                    checked={newConfig.enabled}
                    onChange={(e) => setNewConfig({...newConfig, enabled: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                  />
                  <label htmlFor="enabled" className="ml-2 block text-sm font-medium">
                    Enabled
                  </label>
                </div>
                <div className="flex justify-end">
                  <Button type="submit" disabled={isSavingConfig}>
                    {isSavingConfig ? 'Saving...' : 'Add Directory'}
                  </Button>
                </div>
              </form>
            </div>

            {/* Existing configurations */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-medium">Configured Directories</h3>
                <Button 
                  onClick={handleScanAllDirectories}
                  disabled={isScanning}
                >
                  {isScanning ? 'Scanning...' : 'Scan All Directories'}
                </Button>
              </div>
              {isLoadingConfigs ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground">Loading configurations...</p>
                </div>
              ) : logDirectoryConfigs.length === 0 ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground">No log directories configured yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {logDirectoryConfigs.map((config) => (
                    <div key={config.id} className="border rounded-md p-4">
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <h4 className="font-medium">{config.directoryPath}</h4>
                          <p className="text-sm text-muted-foreground">
                            Pattern: {config.filePattern} | Scan interval: {config.scanIntervalSeconds}s | 
                            Status: {config.enabled ? 'Enabled' : 'Disabled'}
                          </p>
                        </div>
                        <div className="flex space-x-2">
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => handleScanDirectory(config.id!)}
                            disabled={isScanning}
                          >
                            {isScanning ? 'Scanning...' : 'Scan Now'}
                          </Button>
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => handleUpdateConfig({
                              ...config,
                              enabled: !config.enabled
                            })}
                          >
                            {config.enabled ? 'Disable' : 'Enable'}
                          </Button>
                          <Button 
                            variant="destructive" 
                            size="sm"
                            onClick={() => handleDeleteConfig(config.id!)}
                          >
                            Delete
                          </Button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
