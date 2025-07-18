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
import {
  getRetentionPolicies,
  createRetentionPolicy,
  updateRetentionPolicy,
  deleteRetentionPolicy,
  applyRetentionPolicy,
  applyAllRetentionPolicies,
  RetentionPolicy
} from '@/api/retentionPolicy';
import {
  fieldConfigurationApi,
  FieldConfiguration
} from '@/api/fieldConfiguration';

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
  const [isLoadingPolicies, setIsLoadingPolicies] = useState(false);
  const [isSavingPolicy, setIsSavingPolicy] = useState(false);
  const [isApplyingPolicy, setIsApplyingPolicy] = useState(false);
  const [isLoadingFieldConfigs, setIsLoadingFieldConfigs] = useState(false);
  const [isSavingFieldConfig, setIsSavingFieldConfig] = useState(false);
  const [isTestingFieldConfig, setIsTestingFieldConfig] = useState(false);

  // Log directory configurations
  const [logDirectoryConfigs, setLogDirectoryConfigs] = useState<LogDirectoryConfig[]>([]);
  const [newConfig, setNewConfig] = useState<LogDirectoryConfig>({
    directoryPath: '',
    enabled: true,
    filePattern: '*.log',
    scanIntervalSeconds: 60
  });
  
  // Retention policies
  const [retentionPolicies, setRetentionPolicies] = useState<RetentionPolicy[]>([]);
  const [newPolicy, setNewPolicy] = useState<RetentionPolicy>({
    name: '',
    maxAgeDays: 30,
    enabled: true,
    applyToSources: []
  });
  const [availableSources, setAvailableSources] = useState<string[]>([]);
  
  // Field configurations
  const [fieldConfigurations, setFieldConfigurations] = useState<FieldConfiguration[]>([]);
  const [newFieldConfig, setNewFieldConfig] = useState<FieldConfiguration>({
    name: '',
    description: '',
    fieldType: 'STRING',
    extractionPattern: null,
    sourceField: 'message',
    isStored: true,
    isIndexed: true,
    isTokenized: false,
    enabled: true
  });
  const [testSampleString, setTestSampleString] = useState('');
  const [testResult, setTestResult] = useState<string | null>(null);

  // Load log directory configurations, retention policies, and field configurations
  useEffect(() => {
    const fetchData = async () => {
      setIsLoadingConfigs(true);
      setIsLoadingPolicies(true);
      setIsLoadingFieldConfigs(true);
      
      try {
        // Fetch log directory configurations
        const configs = await getLogDirectoryConfigs();
        setLogDirectoryConfigs(configs);
        
        // Extract unique sources for retention policy source selection
        const sources = configs.map(config => {
          // Extract filename from path
          const parts = config.directoryPath.split(/[\/\\]/);
          return parts[parts.length - 1];
        });
        setAvailableSources([...new Set(sources)]);
        
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
      
      try {
        // Fetch retention policies
        const policies = await getRetentionPolicies();
        setRetentionPolicies(policies);
      } catch (error) {
        console.error('Error fetching retention policies:', error);
        toast({
          title: 'Error',
          description: 'Failed to load retention policies',
          variant: 'destructive',
        });
      } finally {
        setIsLoadingPolicies(false);
      }
      
      try {
        // Fetch field configurations
        const fieldConfigs = await fieldConfigurationApi.getFieldConfigurations();
        setFieldConfigurations(fieldConfigs);
      } catch (error) {
        console.error('Error fetching field configurations:', error);
        toast({
          title: 'Error',
          description: 'Failed to load field configurations',
          variant: 'destructive',
        });
      } finally {
        setIsLoadingFieldConfigs(false);
      }
    };

    fetchData();
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
  
  // Handle creating a new retention policy
  const handleCreatePolicy = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newPolicy.name) {
      toast({
        title: 'Error',
        description: 'Please enter a policy name',
        variant: 'destructive',
      });
      return;
    }

    setIsSavingPolicy(true);

    try {
      const createdPolicy = await createRetentionPolicy(newPolicy);
      setRetentionPolicies([...retentionPolicies, createdPolicy]);
      setNewPolicy({
        name: '',
        maxAgeDays: 30,
        enabled: true,
        applyToSources: []
      });

      toast({
        title: 'Policy created',
        description: 'Retention policy has been created successfully',
      });
    } catch (error) {
      console.error('Error creating retention policy:', error);
      toast({
        title: 'Error',
        description: 'Failed to create retention policy',
        variant: 'destructive',
      });
    } finally {
      setIsSavingPolicy(false);
    }
  };

  // Handle updating a retention policy
  const handleUpdatePolicy = async (policy: RetentionPolicy) => {
    if (!policy.id) return;

    try {
      const updatedPolicy = await updateRetentionPolicy(policy.id, policy);
      setRetentionPolicies(retentionPolicies.map(p => p.id === policy.id ? updatedPolicy : p));

      toast({
        title: 'Policy updated',
        description: 'Retention policy has been updated successfully',
      });
    } catch (error) {
      console.error('Error updating retention policy:', error);
      toast({
        title: 'Error',
        description: 'Failed to update retention policy',
        variant: 'destructive',
      });
    }
  };

  // Handle deleting a retention policy
  const handleDeletePolicy = async (id: string) => {
    try {
      await deleteRetentionPolicy(id);
      setRetentionPolicies(retentionPolicies.filter(p => p.id !== id));

      toast({
        title: 'Policy deleted',
        description: 'Retention policy has been deleted successfully',
      });
    } catch (error) {
      console.error('Error deleting retention policy:', error);
      toast({
        title: 'Error',
        description: 'Failed to delete retention policy',
        variant: 'destructive',
      });
    }
  };

  // Handle applying a retention policy
  const handleApplyPolicy = async (id: string) => {
    setIsApplyingPolicy(true);

    try {
      const deletedCount = await applyRetentionPolicy(id);

      toast({
        title: 'Policy applied',
        description: `Deleted ${deletedCount} log entries`,
      });
    } catch (error) {
      console.error('Error applying retention policy:', error);
      toast({
        title: 'Error',
        description: 'Failed to apply retention policy',
        variant: 'destructive',
      });
    } finally {
      setIsApplyingPolicy(false);
    }
  };

  // Handle applying all retention policies
  const handleApplyAllPolicies = async () => {
    setIsApplyingPolicy(true);

    try {
      const deletedCount = await applyAllRetentionPolicies();

      toast({
        title: 'All policies applied',
        description: `Deleted ${deletedCount} log entries`,
      });
    } catch (error) {
      console.error('Error applying all retention policies:', error);
      toast({
        title: 'Error',
        description: 'Failed to apply retention policies',
        variant: 'destructive',
      });
    } finally {
      setIsApplyingPolicy(false);
    }
  };
  
  // Handle creating a new field configuration
  const handleCreateFieldConfig = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newFieldConfig.name) {
      toast({
        title: 'Error',
        description: 'Please enter a field name',
        variant: 'destructive',
      });
      return;
    }

    if (!newFieldConfig.sourceField) {
      toast({
        title: 'Error',
        description: 'Please select a source field',
        variant: 'destructive',
      });
      return;
    }

    setIsSavingFieldConfig(true);

    try {
      const createdConfig = await fieldConfigurationApi.createFieldConfiguration(newFieldConfig);
      setFieldConfigurations([...fieldConfigurations, createdConfig]);
      setNewFieldConfig({
        name: '',
        description: '',
        fieldType: 'STRING',
        extractionPattern: null,
        sourceField: 'message',
        isStored: true,
        isIndexed: true,
        isTokenized: false,
        enabled: true
      });
      setTestSampleString('');
      setTestResult(null);

      toast({
        title: 'Field configuration created',
        description: 'Field configuration has been created successfully',
      });
    } catch (error) {
      console.error('Error creating field configuration:', error);
      toast({
        title: 'Error',
        description: 'Failed to create field configuration',
        variant: 'destructive',
      });
    } finally {
      setIsSavingFieldConfig(false);
    }
  };

  // Handle updating a field configuration
  const handleUpdateFieldConfig = async (config: FieldConfiguration) => {
    if (!config.id) return;

    try {
      const updatedConfig = await fieldConfigurationApi.updateFieldConfiguration(config.id, config);
      setFieldConfigurations(fieldConfigurations.map(c => c.id === config.id ? updatedConfig : c));

      toast({
        title: 'Field configuration updated',
        description: 'Field configuration has been updated successfully',
      });
    } catch (error) {
      console.error('Error updating field configuration:', error);
      toast({
        title: 'Error',
        description: 'Failed to update field configuration',
        variant: 'destructive',
      });
    }
  };

  // Handle deleting a field configuration
  const handleDeleteFieldConfig = async (id: string) => {
    try {
      await fieldConfigurationApi.deleteFieldConfiguration(id);
      setFieldConfigurations(fieldConfigurations.filter(c => c.id !== id));

      toast({
        title: 'Field configuration deleted',
        description: 'Field configuration has been deleted successfully',
      });
    } catch (error) {
      console.error('Error deleting field configuration:', error);
      toast({
        title: 'Error',
        description: 'Failed to delete field configuration',
        variant: 'destructive',
      });
    }
  };

  // Handle testing a field configuration
  const handleTestFieldConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!testSampleString) {
      toast({
        title: 'Error',
        description: 'Please enter a sample string',
        variant: 'destructive',
      });
      return;
    }

    setIsTestingFieldConfig(true);

    try {
      const result = await fieldConfigurationApi.testFieldConfiguration(newFieldConfig, testSampleString);
      setTestResult(result);
    } catch (error) {
      console.error('Error testing field configuration:', error);
      toast({
        title: 'Error',
        description: 'Failed to test field configuration',
        variant: 'destructive',
      });
      setTestResult(null);
    } finally {
      setIsTestingFieldConfig(false);
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
      
      {/* Retention Policy Configuration */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Log Retention Policy</h2>
        <div className="border rounded-md p-4">
          <div className="space-y-6">
            {/* Add new retention policy */}
            <div>
              <h3 className="text-lg font-medium mb-4">Add New Retention Policy</h3>
              <form onSubmit={handleCreatePolicy} className="space-y-4">
                <div>
                  <label htmlFor="policyName" className="block text-sm font-medium">
                    Policy Name
                  </label>
                  <input
                    id="policyName"
                    type="text"
                    value={newPolicy.name}
                    onChange={(e) => setNewPolicy({...newPolicy, name: e.target.value})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    placeholder="30-day retention"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="maxAgeDays" className="block text-sm font-medium">
                    Maximum Age (days)
                  </label>
                  <input
                    id="maxAgeDays"
                    type="number"
                    value={newPolicy.maxAgeDays}
                    onChange={(e) => setNewPolicy({...newPolicy, maxAgeDays: parseInt(e.target.value)})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    min="1"
                    max="365"
                  />
                </div>
                <div>
                  <label htmlFor="applySources" className="block text-sm font-medium">
                    Apply to Sources (optional)
                  </label>
                  <select
                    id="applySources"
                    multiple
                    value={newPolicy.applyToSources || []}
                    onChange={(e) => {
                      const selectedOptions = Array.from(e.target.selectedOptions, option => option.value);
                      setNewPolicy({...newPolicy, applyToSources: selectedOptions});
                    }}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    size={3}
                  >
                    {availableSources.map((source) => (
                      <option key={source} value={source}>
                        {source}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Leave empty to apply to all sources. Hold Ctrl/Cmd to select multiple sources.
                  </p>
                </div>
                <div className="flex items-center">
                  <input
                    id="policyEnabled"
                    type="checkbox"
                    checked={newPolicy.enabled}
                    onChange={(e) => setNewPolicy({...newPolicy, enabled: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                  />
                  <label htmlFor="policyEnabled" className="ml-2 block text-sm font-medium">
                    Enabled
                  </label>
                </div>
                <div className="flex justify-end">
                  <Button type="submit" disabled={isSavingPolicy}>
                    {isSavingPolicy ? 'Saving...' : 'Add Policy'}
                  </Button>
                </div>
              </form>
            </div>

            {/* Existing retention policies */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-medium">Configured Retention Policies</h3>
                <Button 
                  onClick={handleApplyAllPolicies}
                  disabled={isApplyingPolicy}
                >
                  {isApplyingPolicy ? 'Applying...' : 'Apply All Policies'}
                </Button>
              </div>
              {isLoadingPolicies ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground">Loading retention policies...</p>
                </div>
              ) : retentionPolicies.length === 0 ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground">No retention policies configured yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {retentionPolicies.map((policy) => (
                    <div key={policy.id} className="border rounded-md p-4">
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <h4 className="font-medium">{policy.name}</h4>
                          <p className="text-sm text-muted-foreground">
                            Max Age: {policy.maxAgeDays} days | 
                            Status: {policy.enabled ? 'Enabled' : 'Disabled'} |
                            Sources: {policy.applyToSources && policy.applyToSources.length > 0 
                              ? policy.applyToSources.join(', ') 
                              : 'All sources'}
                          </p>
                        </div>
                        <div className="flex space-x-2">
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => handleApplyPolicy(policy.id!)}
                            disabled={isApplyingPolicy}
                          >
                            {isApplyingPolicy ? 'Applying...' : 'Apply Now'}
                          </Button>
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => handleUpdatePolicy({
                              ...policy,
                              enabled: !policy.enabled
                            })}
                          >
                            {policy.enabled ? 'Disable' : 'Enable'}
                          </Button>
                          <Button 
                            variant="destructive" 
                            size="sm"
                            onClick={() => handleDeletePolicy(policy.id!)}
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
      
      {/* Field Configuration */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold">Field Configuration</h2>
        <div className="border rounded-md p-4">
          <div className="space-y-6">
            {/* Add new field configuration */}
            <div>
              <h3 className="text-lg font-medium mb-4">Add New Field Configuration</h3>
              <form onSubmit={handleCreateFieldConfig} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="fieldName" className="block text-sm font-medium">
                      Field Name
                    </label>
                    <input
                      id="fieldName"
                      type="text"
                      value={newFieldConfig.name}
                      onChange={(e) => setNewFieldConfig({...newFieldConfig, name: e.target.value})}
                      className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                      placeholder="ip_address"
                      required
                    />
                  </div>
                  <div>
                    <label htmlFor="fieldType" className="block text-sm font-medium">
                      Field Type
                    </label>
                    <select
                      id="fieldType"
                      value={newFieldConfig.fieldType}
                      onChange={(e) => setNewFieldConfig({...newFieldConfig, fieldType: e.target.value as any})}
                      className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    >
                      <option value="STRING">String</option>
                      <option value="NUMBER">Number</option>
                      <option value="DATE">Date</option>
                      <option value="BOOLEAN">Boolean</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label htmlFor="fieldDescription" className="block text-sm font-medium">
                    Description
                  </label>
                  <input
                    id="fieldDescription"
                    type="text"
                    value={newFieldConfig.description}
                    onChange={(e) => setNewFieldConfig({...newFieldConfig, description: e.target.value})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    placeholder="IP address extracted from log message"
                  />
                </div>
                <div>
                  <label htmlFor="sourceField" className="block text-sm font-medium">
                    Source Field
                  </label>
                  <select
                    id="sourceField"
                    value={newFieldConfig.sourceField}
                    onChange={(e) => setNewFieldConfig({...newFieldConfig, sourceField: e.target.value})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    required
                  >
                    <option value="message">Message</option>
                    <option value="level">Level</option>
                    <option value="source">Source</option>
                    <option value="rawContent">Raw Content</option>
                  </select>
                  <p className="mt-1 text-xs text-muted-foreground">
                    The field from which to extract the value
                  </p>
                </div>
                <div>
                  <label htmlFor="extractionPattern" className="block text-sm font-medium">
                    Extraction Pattern (Regular Expression)
                  </label>
                  <input
                    id="extractionPattern"
                    type="text"
                    value={newFieldConfig.extractionPattern || ''}
                    onChange={(e) => setNewFieldConfig({...newFieldConfig, extractionPattern: e.target.value || null})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    placeholder="\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
                  />
                  <p className="mt-1 text-xs text-muted-foreground">
                    Regular expression pattern to extract the field value. Leave empty to use the entire source field.
                  </p>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="flex items-center">
                    <input
                      id="isStored"
                      type="checkbox"
                      checked={newFieldConfig.isStored}
                      onChange={(e) => setNewFieldConfig({...newFieldConfig, isStored: e.target.checked})}
                      className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                    />
                    <label htmlFor="isStored" className="ml-2 block text-sm font-medium">
                      Stored
                    </label>
                  </div>
                  <div className="flex items-center">
                    <input
                      id="isIndexed"
                      type="checkbox"
                      checked={newFieldConfig.isIndexed}
                      onChange={(e) => setNewFieldConfig({...newFieldConfig, isIndexed: e.target.checked})}
                      className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                    />
                    <label htmlFor="isIndexed" className="ml-2 block text-sm font-medium">
                      Indexed
                    </label>
                  </div>
                  <div className="flex items-center">
                    <input
                      id="isTokenized"
                      type="checkbox"
                      checked={newFieldConfig.isTokenized}
                      onChange={(e) => setNewFieldConfig({...newFieldConfig, isTokenized: e.target.checked})}
                      className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                    />
                    <label htmlFor="isTokenized" className="ml-2 block text-sm font-medium">
                      Tokenized
                    </label>
                  </div>
                </div>
                <div className="flex items-center">
                  <input
                    id="fieldEnabled"
                    type="checkbox"
                    checked={newFieldConfig.enabled}
                    onChange={(e) => setNewFieldConfig({...newFieldConfig, enabled: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                  />
                  <label htmlFor="fieldEnabled" className="ml-2 block text-sm font-medium">
                    Enabled
                  </label>
                </div>
                
                {/* Test field configuration */}
                <div className="border-t pt-4 mt-4">
                  <h4 className="text-md font-medium mb-2">Test Extraction Pattern</h4>
                  <div className="space-y-2">
                    <div>
                      <label htmlFor="testSampleString" className="block text-sm font-medium">
                        Sample String
                      </label>
                      <textarea
                        id="testSampleString"
                        value={testSampleString}
                        onChange={(e) => setTestSampleString(e.target.value)}
                        className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                        placeholder="Enter a sample log message to test the extraction pattern"
                        rows={3}
                      />
                    </div>
                    <div className="flex justify-end">
                      <Button 
                        type="button" 
                        variant="outline" 
                        onClick={handleTestFieldConfig}
                        disabled={isTestingFieldConfig || !testSampleString}
                        className="mr-2"
                      >
                        {isTestingFieldConfig ? 'Testing...' : 'Test Pattern'}
                      </Button>
                    </div>
                    {testResult !== null && (
                      <div className="mt-2 p-2 border rounded-md">
                        <p className="text-sm font-medium">Extraction Result:</p>
                        <p className="text-sm">{testResult}</p>
                      </div>
                    )}
                  </div>
                </div>
                
                <div className="flex justify-end">
                  <Button type="submit" disabled={isSavingFieldConfig}>
                    {isSavingFieldConfig ? 'Saving...' : 'Add Field Configuration'}
                  </Button>
                </div>
              </form>
            </div>

            {/* Existing field configurations */}
            <div>
              <h3 className="text-lg font-medium mb-4">Configured Fields</h3>
              {isLoadingFieldConfigs ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground">Loading field configurations...</p>
                </div>
              ) : fieldConfigurations.length === 0 ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground">No field configurations configured yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {fieldConfigurations.map((config) => (
                    <div key={config.id} className="border rounded-md p-4">
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <h4 className="font-medium">{config.name}</h4>
                          <p className="text-sm text-muted-foreground">
                            Type: {config.fieldType} | 
                            Source: {config.sourceField} | 
                            Status: {config.enabled ? 'Enabled' : 'Disabled'}
                          </p>
                          {config.description && (
                            <p className="text-sm text-muted-foreground mt-1">
                              Description: {config.description}
                            </p>
                          )}
                          {config.extractionPattern && (
                            <p className="text-sm text-muted-foreground mt-1">
                              Pattern: {config.extractionPattern}
                            </p>
                          )}
                          <p className="text-sm text-muted-foreground mt-1">
                            Properties: 
                            {config.isStored ? ' Stored' : ''} 
                            {config.isIndexed ? ' Indexed' : ''} 
                            {config.isTokenized ? ' Tokenized' : ''}
                          </p>
                        </div>
                        <div className="flex space-x-2">
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => handleUpdateFieldConfig({
                              ...config,
                              enabled: !config.enabled
                            })}
                          >
                            {config.enabled ? 'Disable' : 'Enable'}
                          </Button>
                          <Button 
                            variant="destructive" 
                            size="sm"
                            onClick={() => handleDeleteFieldConfig(config.id!)}
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
