import { useState, useRef } from 'react';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { 
  searchLogs, 
  getTimeAggregation, 
  LogEntry, 
  SearchParams, 
  TimeSlot 
} from '@/api/logSearch';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import LogBarChart from '@/components/LogBarChart';
import Editor, { Monaco } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';

export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [isRegex, setIsRegex] = useState(false);
  const [timeRange, setTimeRange] = useState<SearchParams['timeRange']>('24h');
  const [customStartTime, setCustomStartTime] = useState<number | undefined>(undefined);
  const [customEndTime, setCustomEndTime] = useState<number | undefined>(undefined);
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<LogEntry[]>([]);
  const [selectedLog, setSelectedLog] = useState<LogEntry | null>(null);
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [isLoadingTimeSlots, setIsLoadingTimeSlots] = useState(false);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const { toast } = useToast();
  
  // Function to configure Monaco Editor with SPL syntax highlighting
  const handleEditorDidMount = (editor: monaco.editor.IStandaloneCodeEditor, monaco: Monaco) => {
    editorRef.current = editor;
    
    // Register a new language
    monaco.languages.register({ id: 'spl' });
    
    // Define the token rules for syntax highlighting
    monaco.languages.setMonarchTokensProvider('spl', {
      tokenizer: {
        root: [
          // Commands
          [/\b(search|where|stats|sort|head|tail|eval)\b/, 'keyword'],
          
          // Pipes
          [/\|/, 'operator'],
          
          // Field names and operators
          [/\b([a-zA-Z0-9_]+)(=)/, ['variable', 'operator']],
          
          // Operators
          [/[=<>!]/, 'operator'],
          [/-/, 'operator'],
          
          // Strings
          [/".*?"/, 'string'],
          [/'.*?'/, 'string'],
          
          // Numbers
          [/\b\d+\b/, 'number'],
          
          // Comments
          [/#.*$/, 'comment'],
        ]
      }
    });
    
    // Set the language for the editor
    monaco.editor.setModelLanguage(editor.getModel()!, 'spl');
  };

  // We don't load initial data on component mount anymore
  // This prevents the infinite API requests
  // The user needs to explicitly search to see results

  const handleSearch = async (e?: React.FormEvent | React.MouseEvent) => {
    // If this is a form submission event, prevent the default behavior
    if (e && 'preventDefault' in e) {
      e.preventDefault();
    }

    setIsSearching(true);
    setIsLoadingTimeSlots(true);

    try {
      // Build search parameters
      const searchParams: SearchParams = {
        query: query.trim(),
        isRegex,
        timeRange: timeRange === 'custom' ? undefined : timeRange,
      };

      // Add custom time range if selected
      if (timeRange === 'custom') {
        searchParams.startTime = customStartTime;
        searchParams.endTime = customEndTime;
      }

      // Fetch logs
      const results = await searchLogs(searchParams);
      setSearchResults(results);

      // Fetch time slots for the bar chart
      const slots = await getTimeAggregation({
        ...searchParams,
        slots: timeRange === '24h' ? 24 : timeRange === '12h' ? 12 : timeRange === '3h' ? 6 : timeRange === '1h' ? 6 : 24
      });
      setTimeSlots(slots);

      if (results.length === 0) {
        toast({
          title: 'No results',
          description: `No logs found matching your search criteria`,
        });
      } else {
        toast({
          title: 'Search completed',
          description: `Found ${results.length} matching logs`,
        });
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: 'An error occurred during search',
        variant: 'destructive',
      });
      console.error('Search error:', error);
    } finally {
      setIsSearching(false);
      setIsLoadingTimeSlots(false);
    }
  };

  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  const getLevelClass = (level: string) => {
    switch (level.toUpperCase()) {
      case 'ERROR':
        return 'log-level-error';
      case 'WARNING':
        return 'log-level-warning';
      case 'INFO':
        return 'log-level-info';
      case 'DEBUG':
        return 'log-level-debug';
      case 'TRACE':
        return 'log-level-trace';
      default:
        return '';
    }
  };

  const handleLogClick = (log: LogEntry) => {
    setSelectedLog(log);
  };

  const handleTimeSlotClick = (slot: TimeSlot) => {
    // Calculate the time range for this slot
    const slotSizeMs = timeRange === '24h' ? 60 * 60 * 1000 : // 1 hour
                       timeRange === '12h' ? 60 * 60 * 1000 : // 1 hour
                       timeRange === '3h' ? 30 * 60 * 1000 : // 30 minutes
                       timeRange === '1h' ? 10 * 60 * 1000 : // 10 minutes
                       60 * 60 * 1000; // default to 1 hour

    // Set custom time range
    setTimeRange('custom');
    setCustomStartTime(slot.time);
    setCustomEndTime(slot.time + slotSizeMs);

    // Trigger search with the new time range
    const searchParams: SearchParams = {
      query: query.trim(),
      isRegex,
      startTime: slot.time,
      endTime: slot.time + slotSizeMs
    };

    setIsSearching(true);
    setIsLoadingTimeSlots(true);

    // Fetch logs with the new time range
    searchLogs(searchParams)
      .then(results => {
        setSearchResults(results);
        return getTimeAggregation({
          ...searchParams,
          slots: 6 // Use 6 slots for the zoomed-in view
        });
      })
      .then(slots => {
        setTimeSlots(slots);
        toast({
          title: 'Time range updated',
          description: `Showing logs from ${new Date(slot.time).toLocaleTimeString()} to ${new Date(slot.time + slotSizeMs).toLocaleTimeString()}`,
        });
      })
      .catch(error => {
        console.error('Error updating time range:', error);
        toast({
          title: 'Error',
          description: 'Failed to update time range',
          variant: 'destructive',
        });
      })
      .finally(() => {
        setIsSearching(false);
        setIsLoadingTimeSlots(false);
      });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Log Search</h1>
        <p className="text-muted-foreground">
          Search for logs using simple queries or advanced syntax
        </p>
      </div>

      <form onSubmit={handleSearch} className="space-y-4">
        <div className="flex flex-col gap-2">
          <div className="flex-1 h-[150px]">
            <Editor
              height="100%"
              defaultLanguage="spl"
              defaultValue={query}
              onChange={(value) => setQuery(value || '')}
              onMount={handleEditorDidMount}
              options={{
                minimap: { enabled: false },
                lineNumbers: 'off',
                folding: false,
                scrollBeyondLastLine: false,
                wordWrap: 'on',
                automaticLayout: true,
                suggestOnTriggerCharacters: true,
                fontSize: 14,
                tabSize: 2,
              }}
            />
          </div>
          <div className="flex gap-2">
            <Button type="submit" disabled={isSearching}>
              {isSearching ? 'Searching...' : 'Search'}
            </Button>
            <Button 
              type="button" 
              variant="outline" 
              onClick={handleSearch} 
              disabled={isSearching}
            >
              Refresh
            </Button>
          </div>
        </div>

        <div className="flex flex-wrap gap-6">
          <div className="flex items-center space-x-2">
            <Checkbox 
              id="regex" 
              checked={isRegex} 
              onCheckedChange={(checked: boolean | 'indeterminate') => setIsRegex(checked as boolean)}
            />
            <Label htmlFor="regex">Use regex</Label>
          </div>

          <div className="flex items-center space-x-2">
            <Label htmlFor="timeRange">Time range:</Label>
            <Select value={timeRange} onValueChange={(value) => setTimeRange(value as SearchParams['timeRange'])}>
              <SelectTrigger className="w-[180px]" id="timeRange">
                <SelectValue placeholder="Select time range" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1h">Last 1 hour</SelectItem>
                <SelectItem value="3h">Last 3 hours</SelectItem>
                <SelectItem value="12h">Last 12 hours</SelectItem>
                <SelectItem value="24h">Last 24 hours</SelectItem>
                <SelectItem value="custom">Custom range</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {timeRange === 'custom' && (
            <div className="flex items-center space-x-2">
              <Label htmlFor="startTime">From:</Label>
              <input
                type="datetime-local"
                id="startTime"
                value={customStartTime ? new Date(customStartTime).toISOString().slice(0, 16) : ''}
                onChange={(e) => {
                  const date = new Date(e.target.value);
                  setCustomStartTime(date.getTime());
                }}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />

              <Label htmlFor="endTime">To:</Label>
              <input
                type="datetime-local"
                id="endTime"
                value={customEndTime ? new Date(customEndTime).toISOString().slice(0, 16) : ''}
                onChange={(e) => {
                  const date = new Date(e.target.value);
                  setCustomEndTime(date.getTime());
                }}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </div>
          )}
        </div>
      </form>

      {/* Bar chart visualization */}
      {timeSlots.length > 0 && !isLoadingTimeSlots && (
        <LogBarChart 
          timeSlots={timeSlots} 
          timeRange={timeRange} 
          onTimeSlotClick={handleTimeSlotClick} 
        />
      )}

      {isLoadingTimeSlots && (
        <div className="mt-6 mb-8 text-center py-4">
          <p className="text-muted-foreground">Loading time distribution...</p>
        </div>
      )}

      {searchResults.length > 0 && (
        <div className="rounded-md border">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-4 py-2 text-left font-medium">Timestamp</th>
                  <th className="px-4 py-2 text-left font-medium">Level</th>
                  <th className="px-4 py-2 text-left font-medium">Message</th>
                  <th className="px-4 py-2 text-left font-medium">Source</th>
                </tr>
              </thead>
              <tbody>
                {searchResults.map((log) => (
                  <tr 
                    key={log.id} 
                    className="border-b hover:bg-muted/50 cursor-pointer"
                    onClick={() => handleLogClick(log)}
                  >
                    <td className="px-4 py-2 text-sm">{formatTimestamp(log.timestamp)}</td>
                    <td className="px-4 py-2 text-sm">
                      <span className={getLevelClass(log.level)}>
                        {log.level}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-sm">{log.message}</td>
                    <td className="px-4 py-2 text-sm">{log.source}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selectedLog && (
        <div className="rounded-md border p-4 bg-muted/20">
          <div className="flex justify-between items-start mb-4">
            <h3 className="text-lg font-medium">Log Details</h3>
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={() => setSelectedLog(null)}
            >
              Close
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p className="text-sm font-medium">Timestamp</p>
              <p className="text-sm">{formatTimestamp(selectedLog.timestamp)}</p>
            </div>
            <div>
              <p className="text-sm font-medium">Level</p>
              <p className={`text-sm ${getLevelClass(selectedLog.level)}`}>
                {selectedLog.level}
              </p>
            </div>
            <div>
              <p className="text-sm font-medium">Source</p>
              <p className="text-sm">{selectedLog.source}</p>
            </div>
            <div>
              <p className="text-sm font-medium">ID</p>
              <p className="text-sm">{selectedLog.id}</p>
            </div>
          </div>

          <div className="mt-4">
            <p className="text-sm font-medium">Message</p>
            <p className="text-sm mt-1 p-2 bg-background rounded-md">
              {selectedLog.message}
            </p>
          </div>

          <div className="mt-4">
            <p className="text-sm font-medium">Metadata</p>
            <pre className="text-sm mt-1 p-2 bg-background rounded-md overflow-x-auto">
              {JSON.stringify(selectedLog.metadata, null, 2)}
            </pre>
          </div>
        </div>
      )}

      {searchResults.length === 0 && !isSearching && query && (
        <div className="text-center py-8">
          <p className="text-muted-foreground">No logs found matching your query</p>
        </div>
      )}

      {!query && searchResults.length === 0 && (
        <div className="text-center py-8">
          <p className="text-muted-foreground">Enter a search query and click Search, or click Refresh to see the latest logs</p>
        </div>
      )}
    </div>
  );
}
