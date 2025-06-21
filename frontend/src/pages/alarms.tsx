import { useState } from 'react';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';

// Mock data for alarms
const mockAlarms = [
  {
    id: '1',
    name: 'Database Connection Failures',
    description: 'Alert when database connection errors occur',
    query: 'level:ERROR AND message:"Failed to connect to database"',
    condition: 'count > 5',
    threshold: 5,
    timeWindowMinutes: 15,
    enabled: true,
    createdAt: Date.now() - 1000 * 60 * 60 * 24 * 3, // 3 days ago
    notificationChannels: [
      { type: 'EMAIL', destination: 'admin@example.com' }
    ]
  },
  {
    id: '2',
    name: 'High Memory Usage',
    description: 'Alert when memory usage is consistently high',
    query: 'level:WARNING AND message:"High memory usage"',
    condition: 'count > 3',
    threshold: 3,
    timeWindowMinutes: 10,
    enabled: true,
    createdAt: Date.now() - 1000 * 60 * 60 * 24 * 2, // 2 days ago
    notificationChannels: [
      { type: 'EMAIL', destination: 'admin@example.com' },
      { type: 'SLACK', destination: '#alerts' }
    ]
  },
  {
    id: '3',
    name: 'API Rate Limit Alerts',
    description: 'Alert when API rate limits are exceeded',
    query: 'message:"rate limit exceeded"',
    condition: 'count > 10',
    threshold: 10,
    timeWindowMinutes: 5,
    enabled: false,
    createdAt: Date.now() - 1000 * 60 * 60 * 24, // 1 day ago
    notificationChannels: [
      { type: 'WEBHOOK', destination: 'https://example.com/webhook' }
    ]
  }
];

export default function AlarmsPage() {
  const [alarms, setAlarms] = useState(mockAlarms);
  const [isCreatingAlarm, setIsCreatingAlarm] = useState(false);
  const [selectedAlarm, setSelectedAlarm] = useState<(typeof mockAlarms)[0] | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const { toast } = useToast();

  // Form state for creating/editing alarms
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    query: '',
    condition: 'count >',
    threshold: 5,
    timeWindowMinutes: 15,
    enabled: true,
    notificationEmail: ''
  });

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      query: '',
      condition: 'count >',
      threshold: 5,
      timeWindowMinutes: 15,
      enabled: true,
      notificationEmail: ''
    });
  };

  const handleCreateAlarm = () => {
    setIsCreatingAlarm(true);
    setIsEditing(false);
    resetForm();
  };

  const handleEditAlarm = (alarm: (typeof mockAlarms)[0]) => {
    setSelectedAlarm(alarm);
    setIsEditing(true);
    setIsCreatingAlarm(true);
    
    // Populate form with alarm data
    setFormData({
      name: alarm.name,
      description: alarm.description,
      query: alarm.query,
      condition: `count > ${alarm.threshold}`,
      threshold: alarm.threshold,
      timeWindowMinutes: alarm.timeWindowMinutes,
      enabled: alarm.enabled,
      notificationEmail: alarm.notificationChannels.find(c => c.type === 'EMAIL')?.destination || ''
    });
  };

  const handleDeleteAlarm = (id: string) => {
    // In a real app, this would be an API call to delete the alarm
    const updatedAlarms = alarms.filter(alarm => alarm.id !== id);
    setAlarms(updatedAlarms);
    
    toast({
      title: 'Alarm deleted',
      description: 'The alarm has been deleted successfully',
    });
  };

  const handleToggleAlarm = (id: string, enabled: boolean) => {
    // In a real app, this would be an API call to update the alarm
    const updatedAlarms = alarms.map(alarm => 
      alarm.id === id ? { ...alarm, enabled } : alarm
    );
    setAlarms(updatedAlarms);
    
    toast({
      title: enabled ? 'Alarm enabled' : 'Alarm disabled',
      description: `The alarm has been ${enabled ? 'enabled' : 'disabled'} successfully`,
    });
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name || !formData.query) {
      toast({
        title: 'Error',
        description: 'Please fill in all required fields',
        variant: 'destructive',
      });
      return;
    }
    
    // In a real app, this would be an API call to create/update the alarm
    if (isEditing && selectedAlarm) {
      // Update existing alarm
      const updatedAlarms = alarms.map(alarm => 
        alarm.id === selectedAlarm.id 
          ? {
              ...alarm,
              name: formData.name,
              description: formData.description,
              query: formData.query,
              threshold: formData.threshold,
              timeWindowMinutes: formData.timeWindowMinutes,
              enabled: formData.enabled,
              notificationChannels: [
                { type: 'EMAIL', destination: formData.notificationEmail }
              ]
            } 
          : alarm
      );
      
      setAlarms(updatedAlarms);
      
      toast({
        title: 'Alarm updated',
        description: 'The alarm has been updated successfully',
      });
    } else {
      // Create new alarm
      const newAlarm = {
        id: `${Date.now()}`, // Generate a unique ID
        name: formData.name,
        description: formData.description,
        query: formData.query,
        condition: `count > ${formData.threshold}`,
        threshold: formData.threshold,
        timeWindowMinutes: formData.timeWindowMinutes,
        enabled: formData.enabled,
        createdAt: Date.now(),
        notificationChannels: [
          { type: 'EMAIL', destination: formData.notificationEmail }
        ]
      };
      
      setAlarms([...alarms, newAlarm]);
      
      toast({
        title: 'Alarm created',
        description: 'The alarm has been created successfully',
      });
    }
    
    // Reset form and close modal
    resetForm();
    setIsCreatingAlarm(false);
    setIsEditing(false);
    setSelectedAlarm(null);
  };

  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleDateString();
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Alarms</h1>
          <p className="text-muted-foreground">
            Create and manage alerts for important log events
          </p>
        </div>
        <Button onClick={handleCreateAlarm}>
          Create Alarm
        </Button>
      </div>
      
      {alarms.length > 0 ? (
        <div className="rounded-md border">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-4 py-2 text-left font-medium">Name</th>
                  <th className="px-4 py-2 text-left font-medium">Query</th>
                  <th className="px-4 py-2 text-left font-medium">Condition</th>
                  <th className="px-4 py-2 text-left font-medium">Status</th>
                  <th className="px-4 py-2 text-left font-medium">Created</th>
                  <th className="px-4 py-2 text-left font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {alarms.map((alarm) => (
                  <tr key={alarm.id} className="border-b">
                    <td className="px-4 py-2 text-sm font-medium">{alarm.name}</td>
                    <td className="px-4 py-2 text-sm max-w-xs truncate">{alarm.query}</td>
                    <td className="px-4 py-2 text-sm">
                      {alarm.condition} in {alarm.timeWindowMinutes} min
                    </td>
                    <td className="px-4 py-2 text-sm">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        alarm.enabled 
                          ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300' 
                          : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
                      }`}>
                        {alarm.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-sm">{formatDate(alarm.createdAt)}</td>
                    <td className="px-4 py-2 text-sm">
                      <div className="flex space-x-2">
                        <Button 
                          variant="outline" 
                          size="sm"
                          onClick={() => handleEditAlarm(alarm)}
                        >
                          Edit
                        </Button>
                        <Button 
                          variant={alarm.enabled ? "outline" : "default"} 
                          size="sm"
                          onClick={() => handleToggleAlarm(alarm.id, !alarm.enabled)}
                        >
                          {alarm.enabled ? 'Disable' : 'Enable'}
                        </Button>
                        <Button 
                          variant="destructive" 
                          size="sm"
                          onClick={() => handleDeleteAlarm(alarm.id)}
                        >
                          Delete
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="text-center py-8 border rounded-md">
          <p className="text-muted-foreground">No alarms have been created yet</p>
          <Button 
            variant="outline" 
            className="mt-4"
            onClick={handleCreateAlarm}
          >
            Create your first alarm
          </Button>
        </div>
      )}
      
      {isCreatingAlarm && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="bg-background p-6 rounded-lg shadow-lg w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">
              {isEditing ? 'Edit Alarm' : 'Create Alarm'}
            </h2>
            
            <form onSubmit={handleFormSubmit} className="space-y-4">
              <div>
                <label htmlFor="name" className="block text-sm font-medium">
                  Name *
                </label>
                <input
                  id="name"
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="Database Error Alert"
                  required
                />
              </div>
              
              <div>
                <label htmlFor="description" className="block text-sm font-medium">
                  Description
                </label>
                <textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="Alert when database errors occur"
                  rows={2}
                />
              </div>
              
              <div>
                <label htmlFor="query" className="block text-sm font-medium">
                  Search Query *
                </label>
                <input
                  id="query"
                  type="text"
                  value={formData.query}
                  onChange={(e) => setFormData({...formData, query: e.target.value})}
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="level:ERROR AND message:database"
                  required
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="threshold" className="block text-sm font-medium">
                    Threshold
                  </label>
                  <input
                    id="threshold"
                    type="number"
                    min="1"
                    value={formData.threshold}
                    onChange={(e) => setFormData({...formData, threshold: parseInt(e.target.value)})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  />
                </div>
                
                <div>
                  <label htmlFor="timeWindow" className="block text-sm font-medium">
                    Time Window (minutes)
                  </label>
                  <input
                    id="timeWindow"
                    type="number"
                    min="1"
                    value={formData.timeWindowMinutes}
                    onChange={(e) => setFormData({...formData, timeWindowMinutes: parseInt(e.target.value)})}
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  />
                </div>
              </div>
              
              <div>
                <label htmlFor="email" className="block text-sm font-medium">
                  Notification Email
                </label>
                <input
                  id="email"
                  type="email"
                  value={formData.notificationEmail}
                  onChange={(e) => setFormData({...formData, notificationEmail: e.target.value})}
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="admin@example.com"
                />
              </div>
              
              <div className="flex items-center">
                <input
                  id="enabled"
                  type="checkbox"
                  checked={formData.enabled}
                  onChange={(e) => setFormData({...formData, enabled: e.target.checked})}
                  className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                />
                <label htmlFor="enabled" className="ml-2 block text-sm">
                  Enable alarm
                </label>
              </div>
              
              <div className="flex justify-end space-x-2 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsCreatingAlarm(false);
                    setIsEditing(false);
                    setSelectedAlarm(null);
                  }}
                >
                  Cancel
                </Button>
                <Button type="submit">
                  {isEditing ? 'Update' : 'Create'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}