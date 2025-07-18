import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { alarmApi, Alarm, AlarmStatistics, AlarmEvent } from '@/api/alarm';

export default function AlarmMonitoringPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [alarms, setAlarms] = useState<Alarm[]>([]);
  const [alarmEvents, setAlarmEvents] = useState<AlarmEvent[]>([]);
  const [statistics, setStatistics] = useState<AlarmStatistics | null>(null);
  const [activeTab, setActiveTab] = useState('active');
  const [searchQuery, setSearchQuery] = useState('');

  // Load alarms, events, and statistics on component mount
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        const [alarmsData, statsData, eventsData] = await Promise.all([
          alarmApi.getAllAlarms(),
          alarmApi.getStatistics(),
          alarmApi.getAlarmEvents()
        ]);
        
        setAlarms(alarmsData);
        setStatistics(statsData);
        setAlarmEvents(eventsData);
      } catch (error) {
        toast({
          title: 'Error',
          description: 'Failed to load alarm data',
          variant: 'destructive',
        });
      } finally {
        setLoading(false);
      }
    };

    loadData();
    
    // Set up a refresh interval (every 30 seconds)
    const intervalId = setInterval(() => {
      loadData();
    }, 30000);
    
    // Clean up interval on component unmount
    return () => clearInterval(intervalId);
  }, [toast]);

  // Filter alarm events based on search query and active tab
  const filteredEvents = alarmEvents.filter(event => {
    const matchesSearch = searchQuery === '' || 
      event.alarmName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      event.details?.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesTab = 
      (activeTab === 'active' && event.status === 'TRIGGERED') ||
      (activeTab === 'acknowledged' && event.status === 'ACKNOWLEDGED') ||
      (activeTab === 'resolved' && event.status === 'RESOLVED') ||
      (activeTab === 'all');
    
    return matchesSearch && matchesTab;
  });

  // Handle acknowledging an alarm
  const handleAcknowledge = async (eventId: string) => {
    try {
      // Call the API to acknowledge the alarm
      const updatedEvent = await alarmApi.acknowledgeAlarm(eventId);
      
      // Update the local state with the response from the API
      setAlarmEvents(prev => 
        prev.map(event => 
          event.id === eventId ? updatedEvent : event
        )
      );
      
      toast({
        title: 'Alarm acknowledged',
        description: 'The alarm has been acknowledged successfully',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to acknowledge alarm',
        variant: 'destructive',
      });
    }
  };

  // Handle resolving an alarm
  const handleResolve = async (eventId: string) => {
    try {
      // Call the API to resolve the alarm
      const updatedEvent = await alarmApi.resolveAlarm(eventId);
      
      // Update the local state with the response from the API
      setAlarmEvents(prev => 
        prev.map(event => 
          event.id === eventId ? updatedEvent : event
        )
      );
      
      toast({
        title: 'Alarm resolved',
        description: 'The alarm has been resolved successfully',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to resolve alarm',
        variant: 'destructive',
      });
    }
  };

  // Format timestamp to readable date/time
  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  // Calculate time elapsed since timestamp
  const getTimeElapsed = (timestamp: number) => {
    const now = Date.now();
    const elapsed = now - timestamp;
    
    const seconds = Math.floor(elapsed / 1000);
    if (seconds < 60) return `${seconds} seconds ago`;
    
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes} minutes ago`;
    
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} hours ago`;
    
    const days = Math.floor(hours / 24);
    return `${days} days ago`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">Loading alarm data...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Alarm Monitoring Dashboard</h1>
          <p className="text-muted-foreground">
            Monitor and manage active alarms and alerts
          </p>
        </div>
        <Button onClick={() => navigate('/alarms')}>
          Manage Alarms
        </Button>
      </div>

      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-card rounded-lg shadow p-4 border">
            <div className="text-sm text-muted-foreground">Total Alarms</div>
            <div className="text-2xl font-bold">{statistics.totalAlarms}</div>
          </div>
          <div className="bg-card rounded-lg shadow p-4 border">
            <div className="text-sm text-muted-foreground">Enabled Alarms</div>
            <div className="text-2xl font-bold">{statistics.enabledAlarms}</div>
          </div>
          <div className="bg-card rounded-lg shadow p-4 border">
            <div className="text-sm text-muted-foreground">Disabled Alarms</div>
            <div className="text-2xl font-bold">{statistics.disabledAlarms}</div>
          </div>
          <div className="bg-card rounded-lg shadow p-4 border">
            <div className="text-sm text-muted-foreground">Recently Triggered</div>
            <div className="text-2xl font-bold text-red-600">{statistics.recentlyTriggered}</div>
          </div>
        </div>
      )}

      {/* Search and Filter */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="flex-1">
          <Input
            placeholder="Search alarms..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full"
          />
        </div>
      </div>

      {/* Tabs for different alarm statuses */}
      <Tabs defaultValue="active" onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="active">
            Active
            <Badge variant="destructive" className="ml-2">
              {alarmEvents.filter(e => e.status === 'TRIGGERED').length}
            </Badge>
          </TabsTrigger>
          <TabsTrigger value="acknowledged">
            Acknowledged
            <Badge variant="outline" className="ml-2">
              {alarmEvents.filter(e => e.status === 'ACKNOWLEDGED').length}
            </Badge>
          </TabsTrigger>
          <TabsTrigger value="resolved">
            Resolved
          </TabsTrigger>
          <TabsTrigger value="all">All</TabsTrigger>
        </TabsList>

        {/* Tab content - same layout for all tabs, but filtered differently */}
        <TabsContent value="active" className="mt-4">
          <AlarmEventsList 
            events={filteredEvents} 
            onAcknowledge={handleAcknowledge} 
            onResolve={handleResolve}
            formatTimestamp={formatTimestamp}
            getTimeElapsed={getTimeElapsed}
          />
        </TabsContent>
        <TabsContent value="acknowledged" className="mt-4">
          <AlarmEventsList 
            events={filteredEvents} 
            onAcknowledge={handleAcknowledge} 
            onResolve={handleResolve}
            formatTimestamp={formatTimestamp}
            getTimeElapsed={getTimeElapsed}
          />
        </TabsContent>
        <TabsContent value="resolved" className="mt-4">
          <AlarmEventsList 
            events={filteredEvents} 
            onAcknowledge={handleAcknowledge} 
            onResolve={handleResolve}
            formatTimestamp={formatTimestamp}
            getTimeElapsed={getTimeElapsed}
          />
        </TabsContent>
        <TabsContent value="all" className="mt-4">
          <AlarmEventsList 
            events={filteredEvents} 
            onAcknowledge={handleAcknowledge} 
            onResolve={handleResolve}
            formatTimestamp={formatTimestamp}
            getTimeElapsed={getTimeElapsed}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// Component for displaying the list of alarm events
interface AlarmEventsListProps {
  events: AlarmEvent[];
  onAcknowledge: (eventId: string) => void;
  onResolve: (eventId: string) => void;
  formatTimestamp: (timestamp: number) => string;
  getTimeElapsed: (timestamp: number) => string;
}

function AlarmEventsList({ 
  events, 
  onAcknowledge, 
  onResolve,
  formatTimestamp,
  getTimeElapsed
}: AlarmEventsListProps) {
  if (events.length === 0) {
    return (
      <div className="text-center py-8 border rounded-md">
        <p className="text-muted-foreground">No alarms found</p>
      </div>
    );
  }

  return (
    <div className="rounded-md border">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b bg-muted/50">
              <th className="px-4 py-2 text-left font-medium">Status</th>
              <th className="px-4 py-2 text-left font-medium">Alarm Name</th>
              <th className="px-4 py-2 text-left font-medium">Triggered</th>
              <th className="px-4 py-2 text-left font-medium">Match Count</th>
              <th className="px-4 py-2 text-left font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.id} className="border-b">
                <td className="px-4 py-2 text-sm">
                  <StatusBadge status={event.status} />
                </td>
                <td className="px-4 py-2 text-sm font-medium">
                  <div>{event.alarmName}</div>
                  <div className="text-xs text-muted-foreground">{event.details}</div>
                </td>
                <td className="px-4 py-2 text-sm">
                  <div>{formatTimestamp(event.timestamp)}</div>
                  <div className="text-xs text-muted-foreground">{getTimeElapsed(event.timestamp)}</div>
                </td>
                <td className="px-4 py-2 text-sm">
                  {event.matchCount}
                </td>
                <td className="px-4 py-2 text-sm">
                  <div className="flex space-x-2">
                    {event.status === 'TRIGGERED' && (
                      <Button 
                        variant="outline" 
                        size="sm"
                        onClick={() => onAcknowledge(event.id)}
                      >
                        Acknowledge
                      </Button>
                    )}
                    {(event.status === 'TRIGGERED' || event.status === 'ACKNOWLEDGED') && (
                      <Button 
                        variant="default" 
                        size="sm"
                        onClick={() => onResolve(event.id)}
                      >
                        Resolve
                      </Button>
                    )}
                    <Button 
                      variant="outline" 
                      size="sm"
                      onClick={() => window.alert('View details - to be implemented')}
                    >
                      Details
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// Component for displaying status badges
function StatusBadge({ status }: { status: AlarmEvent['status'] }) {
  switch (status) {
    case 'TRIGGERED':
      return (
        <Badge variant="destructive">
          Triggered
        </Badge>
      );
    case 'ACKNOWLEDGED':
      return (
        <Badge variant="outline" className="bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900 dark:text-yellow-300 dark:border-yellow-800">
          Acknowledged
        </Badge>
      );
    case 'RESOLVED':
      return (
        <Badge variant="outline" className="bg-green-100 text-green-800 border-green-200 dark:bg-green-900 dark:text-green-300 dark:border-green-800">
          Resolved
        </Badge>
      );
    default:
      return null;
  }
}