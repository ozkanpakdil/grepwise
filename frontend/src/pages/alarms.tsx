import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { alarmApi, Alarm, AlarmRequest, NotificationChannel } from '@/api/alarm';
import NotificationPreferences from '@/components/NotificationPreferences';

export default function AlarmsPage() {
  const navigate = useNavigate();
  const [alarms, setAlarms] = useState<Alarm[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCreatingAlarm, setIsCreatingAlarm] = useState(false);
  const [selectedAlarm, setSelectedAlarm] = useState<Alarm | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const { toast } = useToast();

  // Load alarms on component mount
  useEffect(() => {
    loadAlarms();
  }, []);

  const loadAlarms = async () => {
    try {
      setLoading(true);
      const data = await alarmApi.getAllAlarms();
      setAlarms(data);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to load alarms',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Form state for creating/editing alarms
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    query: '',
    condition: 'count >',
    threshold: 5,
    timeWindowMinutes: 15,
    enabled: true,
    notificationChannels: [] as NotificationChannel[]
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
      notificationChannels: []
    });
  };

  const handleCreateAlarm = () => {
    setIsCreatingAlarm(true);
    setIsEditing(false);
    resetForm();
  };

  const handleEditAlarm = (alarm: Alarm) => {
    setSelectedAlarm(alarm);
    setIsEditing(true);
    setIsCreatingAlarm(true);

    // Populate form with alarm data
    setFormData({
      name: alarm.name,
      description: alarm.description || '',
      query: alarm.query,
      condition: alarm.condition,
      threshold: alarm.threshold,
      timeWindowMinutes: alarm.timeWindowMinutes,
      enabled: alarm.enabled,
      notificationChannels: alarm.notificationChannels || []
    });
  };

  const handleDeleteAlarm = async (id: string) => {
    try {
      await alarmApi.deleteAlarm(id);
      await loadAlarms(); // Reload alarms after deletion
      toast({
        title: 'Alarm deleted',
        description: 'The alarm has been deleted successfully',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to delete alarm',
        variant: 'destructive',
      });
    }
  };

  const handleToggleAlarm = async (id: string, enabled: boolean) => {
    try {
      await alarmApi.toggleAlarm(id);
      await loadAlarms(); // Reload alarms after toggle
      toast({
        title: enabled ? 'Alarm enabled' : 'Alarm disabled',
        description: `The alarm has been ${enabled ? 'enabled' : 'disabled'} successfully`,
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to toggle alarm',
        variant: 'destructive',
      });
    }
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name || !formData.query) {
      toast({
        title: 'Error',
        description: 'Please fill in all required fields',
        variant: 'destructive',
      });
      return;
    }

    try {
      const alarmRequest: AlarmRequest = {
        name: formData.name,
        description: formData.description,
        query: formData.query,
        condition: formData.condition,
        threshold: formData.threshold,
        timeWindowMinutes: formData.timeWindowMinutes,
        enabled: formData.enabled,
        notificationChannels: formData.notificationChannels
      };

      if (isEditing && selectedAlarm) {
        // Update existing alarm
        await alarmApi.updateAlarm(selectedAlarm.id, alarmRequest);
        toast({
          title: 'Alarm updated',
          description: 'The alarm has been updated successfully',
        });
      } else {
        // Create new alarm
        await alarmApi.createAlarm(alarmRequest);
        toast({
          title: 'Alarm created',
          description: 'The alarm has been created successfully',
        });
      }

      // Reload alarms and reset form
      await loadAlarms();
      resetForm();
      setIsCreatingAlarm(false);
      setIsEditing(false);
      setSelectedAlarm(null);
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Failed to save alarm',
        variant: 'destructive',
      });
    }
  };

  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleDateString();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">Loading alarms...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Alarms</h1>
          <p className="text-muted-foreground">
            Create and manage alerts for important log events
          </p>
        </div>
        <div className="flex space-x-2">
          <Button 
            variant="outline"
            onClick={() => navigate('/alarm-monitoring')}
          >
            Monitoring Dashboard
          </Button>
          <Button onClick={handleCreateAlarm}>
            Create Alarm
          </Button>
        </div>
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
                <NotificationPreferences
                  channels={formData.notificationChannels}
                  onChange={(channels) => setFormData({...formData, notificationChannels: channels})}
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
