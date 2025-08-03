import {useEffect, useMemo, useRef, useState} from 'react';
import {useToast} from '@/components/ui/use-toast';
import {Button} from '@/components/ui/button';
import {
    exportLogsAsCsv,
    exportLogsAsJson,
    getTimeAggregation,
    getHistogram,
    HistogramData,
    LogEntry,
    searchLogs,
    SearchParams,
    TimeSlot
} from '@/api/logSearch';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Checkbox} from '@/components/ui/checkbox';
import {Label} from '@/components/ui/label';
import LogBarChart from '@/components/LogBarChart';
import Editor, {Monaco} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import {Search, RefreshCw, Clock, Regex} from 'lucide-react';

// Type definitions for sorting and filtering
type SortColumn = 'timestamp' | 'level' | 'message' | 'source' | null;
type SortDirection = 'asc' | 'desc';
type FilterValues = {
    level: string;
    source: string;
    message: string;
};

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
    const [histogramData, setHistogramData] = useState<HistogramData[]>([]);
    const [isLoadingTimeSlots, setIsLoadingTimeSlots] = useState(false);
    // Add state for editor loading
    const [isEditorLoading, setIsEditorLoading] = useState(true);
    
    // Auto-refresh settings
    const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(false);
    const [autoRefreshInterval, setAutoRefreshInterval] = useState<string>("10s");
    const autoRefreshTimerRef = useRef<NodeJS.Timeout | null>(null);
    
    // Effect to handle auto-refresh changes and cleanup
    useEffect(() => {
        // Setup auto-refresh when enabled
        if (autoRefreshEnabled) {
            setupAutoRefresh();
        } else if (autoRefreshTimerRef.current) {
            // Clear timer when disabled
            clearInterval(autoRefreshTimerRef.current);
            autoRefreshTimerRef.current = null;
        }
        
        // Cleanup function to clear timer when component unmounts
        return () => {
            if (autoRefreshTimerRef.current) {
                clearInterval(autoRefreshTimerRef.current);
                autoRefreshTimerRef.current = null;
            }
        };
    }, [autoRefreshEnabled, autoRefreshInterval]);

    // Sorting state
    const [sortColumn, setSortColumn] = useState<SortColumn>(null);
    const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

    // Filtering state
    const [filterValues, setFilterValues] = useState<FilterValues>({
        level: '',
        source: '',
        message: ''
    });
    const [showFilters, setShowFilters] = useState(false);

    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
    const {toast} = useToast();

    // Function to configure Monaco Editor with SPL syntax highlighting
    const handleEditorDidMount = (editor: monaco.editor.IStandaloneCodeEditor, monaco: Monaco) => {
        editorRef.current = editor;

        try {
            // Register a new language
            monaco.languages.register({id: 'spl'});

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
            if (editor.getModel()) {
                monaco.editor.setModelLanguage(editor.getModel()!, 'spl');
            }

            // Add key binding for Enter to trigger search and Ctrl+Enter for new line
            editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
                // Do nothing, this allows the default behavior of inserting a new line
            });
            
            editor.addCommand(monaco.KeyCode.Enter, () => {
                // Trigger search when Enter is pressed
                handleSearch();
            });

            // Focus the editor to ensure it's visible and ready for input
            editor.focus();

            // Set loading state to false once editor is mounted
            setIsEditorLoading(false);

            console.log('Monaco Editor initialized successfully');
        } catch (error) {
            console.error('Error initializing Monaco Editor:', error);
            // Set loading state to false even on error to avoid UI being stuck
            setIsEditorLoading(false);

            // Show error toast
            toast({
                title: 'Editor Error',
                description: 'There was an issue initializing the editor. Please refresh the page.',
                variant: 'destructive',
            });
        }
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
            // Calculate time range
            let startTime: number;
            let endTime: number;
            
            if (timeRange === 'custom' && customStartTime && customEndTime) {
                startTime = customStartTime;
                endTime = customEndTime;
            } else {
                endTime = Date.now();
                
                switch (timeRange) {
                    case '1h':
                        startTime = endTime - (60 * 60 * 1000);
                        break;
                    case '3h':
                        startTime = endTime - (3 * 60 * 60 * 1000);
                        break;
                    case '12h':
                        startTime = endTime - (12 * 60 * 60 * 1000);
                        break;
                    case '24h':
                    default:
                        startTime = endTime - (24 * 60 * 60 * 1000);
                        break;
                }
            }

            // Build search parameters
            const searchParams: SearchParams = {
                query: query.trim(),
                isRegex,
                timeRange: timeRange === 'custom' ? undefined : timeRange,
                startTime,
                endTime
            };

            // Fetch logs
            const results = await searchLogs(searchParams);
            setSearchResults(results);

            // Determine appropriate interval based on time range
            let interval: string;
            const timeRangeMs = endTime - startTime;
            
            if (timeRangeMs <= 60 * 60 * 1000) { // 1 hour or less
                interval = '1m';
            } else if (timeRangeMs <= 3 * 60 * 60 * 1000) { // 3 hours or less
                interval = '5m';
            } else if (timeRangeMs <= 12 * 60 * 60 * 1000) { // 12 hours or less
                interval = '15m';
            } else { // 24 hours or less
                interval = '30m';
            }

            // Fetch histogram data
            const histogramResult = await getHistogram({
                query: query.trim(),
                isRegex,
                from: startTime,
                to: endTime,
                interval
            });
            setHistogramData(histogramResult);

            // Also fetch time slots for backward compatibility
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
            
            // Setup auto-refresh if enabled
            setupAutoRefresh();
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
    
    // Setup auto-refresh timer
    const setupAutoRefresh = () => {
        // Clear any existing timer
        if (autoRefreshTimerRef.current) {
            clearInterval(autoRefreshTimerRef.current);
            autoRefreshTimerRef.current = null;
        }
        
        // If auto-refresh is enabled, set up a new timer
        if (autoRefreshEnabled) {
            const intervalMs = autoRefreshInterval === '5s' ? 5000 : 
                              autoRefreshInterval === '10s' ? 10000 : 
                              autoRefreshInterval === '30s' ? 30000 : 10000;
            
            autoRefreshTimerRef.current = setInterval(() => {
                refreshData();
            }, intervalMs);
        }
    };
    
    // Refresh data without changing search parameters
    const refreshData = async () => {
        if (isSearching) return; // Don't refresh if already searching
        
        setIsSearching(true);
        
        try {
            // Calculate time range
            let startTime: number;
            let endTime: number = Date.now();
            
            if (timeRange === 'custom' && customStartTime && customEndTime) {
                // For custom range, keep the same range length but slide it to now
                const rangeLength = customEndTime - customStartTime;
                startTime = endTime - rangeLength;
            } else {
                switch (timeRange) {
                    case '1h':
                        startTime = endTime - (60 * 60 * 1000);
                        break;
                    case '3h':
                        startTime = endTime - (3 * 60 * 60 * 1000);
                        break;
                    case '12h':
                        startTime = endTime - (12 * 60 * 60 * 1000);
                        break;
                    case '24h':
                    default:
                        startTime = endTime - (24 * 60 * 60 * 1000);
                        break;
                }
            }
            
            // Update custom time range if it was being used
            if (timeRange === 'custom') {
                setCustomStartTime(startTime);
                setCustomEndTime(endTime);
            }
            
            // Fetch new logs
            const results = await searchLogs({
                query: query.trim(),
                isRegex,
                startTime,
                endTime
            });
            
            // Determine appropriate interval based on time range
            let interval: string;
            const timeRangeMs = endTime - startTime;
            
            if (timeRangeMs <= 60 * 60 * 1000) { // 1 hour or less
                interval = '1m';
            } else if (timeRangeMs <= 3 * 60 * 60 * 1000) { // 3 hours or less
                interval = '5m';
            } else if (timeRangeMs <= 12 * 60 * 60 * 1000) { // 12 hours or less
                interval = '15m';
            } else { // 24 hours or less
                interval = '30m';
            }
            
            // Fetch new histogram data
            const histogramResult = await getHistogram({
                query: query.trim(),
                isRegex,
                from: startTime,
                to: endTime,
                interval
            });
            
            // Update state with new data
            setSearchResults(results);
            setHistogramData(histogramResult);
            
            // Also update time slots for backward compatibility
            const slots = await getTimeAggregation({
                query: query.trim(),
                isRegex,
                startTime,
                endTime,
                slots: timeRange === '24h' ? 24 : timeRange === '12h' ? 12 : timeRange === '3h' ? 6 : timeRange === '1h' ? 6 : 24
            });
            setTimeSlots(slots);
            
            console.log('Auto-refreshed data at', new Date().toLocaleTimeString());
        } catch (error) {
            console.error('Error refreshing data:', error);
        } finally {
            setIsSearching(false);
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

    // Handle sorting when a column header is clicked
    const handleSortClick = (column: SortColumn) => {
        if (sortColumn === column) {
            // Toggle direction if clicking the same column
            setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        } else {
            // Set new column and default to descending for timestamp, ascending for others
            setSortColumn(column);
            setSortDirection(column === 'timestamp' ? 'desc' : 'asc');
        }
    };

    // Handle filter value changes
    const handleFilterChange = (field: keyof FilterValues, value: string) => {
        setFilterValues(prev => ({
            ...prev,
            [field]: value
        }));
    };

    // Toggle filter visibility
    const toggleFilters = () => {
        setShowFilters(prev => !prev);
    };

    // Apply sorting and filtering to search results
    const processedResults = useMemo(() => {
        let results = [...searchResults];

        // Apply filters
        if (filterValues.level) {
            results = results.filter(log =>
                log.level.toLowerCase().includes(filterValues.level.toLowerCase())
            );
        }

        if (filterValues.source) {
            results = results.filter(log =>
                log.source.toLowerCase().includes(filterValues.source.toLowerCase())
            );
        }

        if (filterValues.message) {
            results = results.filter(log =>
                log.message.toLowerCase().includes(filterValues.message.toLowerCase())
            );
        }

        // Apply sorting
        if (sortColumn) {
            results.sort((a, b) => {
                let valueA: any;
                let valueB: any;

                // Extract values based on sort column
                switch (sortColumn) {
                    case 'timestamp':
                        valueA = a.timestamp;
                        valueB = b.timestamp;
                        break;
                    case 'level':
                        valueA = a.level.toLowerCase();
                        valueB = b.level.toLowerCase();
                        break;
                    case 'message':
                        valueA = a.message.toLowerCase();
                        valueB = b.message.toLowerCase();
                        break;
                    case 'source':
                        valueA = a.source.toLowerCase();
                        valueB = b.source.toLowerCase();
                        break;
                    default:
                        return 0;
                }

                // Compare values based on sort direction
                if (valueA < valueB) {
                    return sortDirection === 'asc' ? -1 : 1;
                }
                if (valueA > valueB) {
                    return sortDirection === 'asc' ? 1 : -1;
                }
                return 0;
            });
        }

        return results;
    }, [searchResults, sortColumn, sortDirection, filterValues]);

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

            <form onSubmit={handleSearch} className="h-[20px]">
                <div className="flex h-full gap-2">
                    <div className="flex-1 h-full w-96">
                        <Editor
                            defaultLanguage="spl"
                            defaultValue={query}
                            onChange={(value) => setQuery(value || '')}
                            onMount={handleEditorDidMount}
                            options={{
                                minimap: {enabled: false},
                                lineNumbers: 'off',
                                folding: false,
                                scrollBeyondLastLine: false,
                                wordWrap: 'on',
                                automaticLayout: true,
                                suggestOnTriggerCharacters: true,
                                fontSize: 14,
                                tabSize: 2,
                                readOnly: isEditorLoading
                            }}
                            className="monaco-editor-container h-full overflow-hidden"
                        />
                    </div>
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center space-x-1">
                            <Checkbox
                                id="regex"
                                checked={isRegex}
                                onCheckedChange={(checked: boolean | 'indeterminate') => setIsRegex(checked as boolean)}
                                className="h-4 w-4"
                            />
                            <Label htmlFor="regex" className="text-sm flex items-center">
                                <Regex className="h-4 w-4 mr-1" />
                            </Label>
                        </div>
                    </div>
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center space-x-1">
                            <Label htmlFor="timeRange" className="text-sm flex items-center">
                                <Clock className="h-4 w-4" />
                            </Label>
                            <Select value={timeRange}
                                    onValueChange={(value) => setTimeRange(value as SearchParams['timeRange'])}>
                                <SelectTrigger className="h-8 text-xs w-[120px]" id="timeRange">
                                    <SelectValue placeholder="Time range"/>
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
                    </div>
                    <div className="flex flex-col gap-2">
                        <Button type="submit" size="sm" disabled={isSearching} className="px-3">
                            {isSearching ? 'Searching...' : <Search className="h-4 w-4" />}
                        </Button>
                    </div>
                    <div className="flex flex-col gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={handleSearch}
                            disabled={isSearching}
                            className="px-3"
                        >
                            <RefreshCw className="h-4 w-4" />
                        </Button>
                    </div>
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center space-x-1">
                            <Label htmlFor="autoRefresh" className="text-sm flex items-center">
                                Auto-refresh
                            </Label>
                            <Select 
                                value={autoRefreshEnabled ? autoRefreshInterval : "off"}
                                onValueChange={(value) => {
                                    if (value === "off") {
                                        setAutoRefreshEnabled(false);
                                    } else {
                                        setAutoRefreshEnabled(true);
                                        setAutoRefreshInterval(value);
                                        // Trigger setup of auto-refresh
                                        setupAutoRefresh();
                                    }
                                }}
                            >
                                <SelectTrigger className="h-8 text-xs w-[80px]" id="autoRefresh">
                                    <SelectValue placeholder="Off"/>
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="off">Off</SelectItem>
                                    <SelectItem value="5s">5s</SelectItem>
                                    <SelectItem value="10s">10s</SelectItem>
                                    <SelectItem value="30s">30s</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                </div>

                <div className="flex flex-wrap gap-6">

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
            {(histogramData.length > 0 || timeSlots.length > 0) && !isLoadingTimeSlots ? (
                <div className="mt-6 mb-8 border border-input rounded-md p-4 bg-background">
                    <div className="flex justify-between items-center mb-4">
                        <h3 className="text-lg font-medium">Time Distribution</h3>
                        {autoRefreshEnabled && (
                            <div className="text-xs text-muted-foreground flex items-center">
                                <RefreshCw className="h-3 w-3 mr-1 animate-spin" />
                                Auto-refreshing every {autoRefreshInterval}
                            </div>
                        )}
                    </div>
                    
                    {histogramData.length > 0 ? (
                        <div>
                            <div className="flex flex-wrap gap-2 mb-4">
                                {/* Histogram data visualization */}
                                <div className="w-full h-64 relative">
                                    <div className="absolute inset-0 flex items-end">
                                        {histogramData.map((item, index) => {
                                            // Find the maximum count for normalization
                                            const maxCount = Math.max(...histogramData.map(d => d.count));
                                            // Calculate height percentage
                                            const heightPercent = maxCount > 0 ? (item.count / maxCount) * 100 : 0;
                                            // Parse the timestamp
                                            const timestamp = new Date(item.timestamp);
                                            
                                            return (
                                                <div 
                                                    key={index} 
                                                    className="flex-1 flex flex-col items-center group"
                                                    title={`${timestamp.toLocaleString()}: ${item.count} logs`}
                                                >
                                                    <div className="relative w-full">
                                                        {/* Tooltip */}
                                                        <div className="absolute bottom-full mb-1 left-1/2 transform -translate-x-1/2 bg-background border rounded-md p-1 text-xs opacity-0 group-hover:opacity-100 transition-opacity z-10 whitespace-nowrap">
                                                            <div>{timestamp.toLocaleString()}</div>
                                                            <div className="font-bold">{item.count} logs</div>
                                                        </div>
                                                        
                                                        {/* Bar */}
                                                        <div 
                                                            className={`w-full ${heightPercent > 0 ? 'bg-primary/80 group-hover:bg-primary transition-colors' : ''}`}
                                                            style={{ height: `${heightPercent}%` }}
                                                        />
                                                    </div>
                                                    
                                                    {/* Only show some x-axis labels to avoid overcrowding */}
                                                    {(index === 0 || index === histogramData.length - 1 || index % Math.max(1, Math.floor(histogramData.length / 8)) === 0) && (
                                                        <div className="text-xs text-muted-foreground mt-1 transform -rotate-45 origin-top-left whitespace-nowrap">
                                                            {timestamp.toLocaleTimeString()}
                                                        </div>
                                                    )}
                                                </div>
                                            );
                                        })}
                                    </div>
                                    
                                    {/* Y-axis labels */}
                                    <div className="absolute -left-8 bottom-0 h-full flex flex-col justify-between text-xs text-muted-foreground">
                                        <div>Max: {Math.max(...histogramData.map(d => d.count))}</div>
                                        <div>0</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <LogBarChart
                            timeSlots={timeSlots}
                            timeRange={timeRange}
                            onTimeSlotClick={handleTimeSlotClick}
                        />
                    )}
                </div>
            ) : isLoadingTimeSlots ? (
                <div className="mt-6 mb-8 text-center py-4 border border-input rounded-md bg-background">
                    <p className="text-muted-foreground">Loading time distribution...</p>
                </div>
            ) : searchResults.length > 0 ? (
                <div className="mt-6 mb-8 text-center py-4 border border-input rounded-md bg-background">
                    <p className="text-muted-foreground">No time distribution data available</p>
                </div>
            ) : null}

            {searchResults.length > 0 && (
                <div className="space-y-2">
                    <div className="flex justify-between items-center">
                        <div>
              <span className="text-sm text-muted-foreground">
                Showing {processedResults.length} of {searchResults.length} logs
              </span>
                        </div>
                        <div className="flex items-center gap-2">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => {
                                    // Create search params from current state
                                    const params: SearchParams = {
                                        query: query,
                                        isRegex: isRegex,
                                        timeRange: timeRange
                                    };

                                    // Add custom time range if selected
                                    if (timeRange === 'custom') {
                                        params.startTime = customStartTime;
                                        params.endTime = customEndTime;
                                    }

                                    // Generate export URL and open in new tab
                                    const exportUrl = exportLogsAsCsv(params);
                                    window.open(exportUrl, '_blank');
                                }}
                                className="flex items-center gap-1"
                            >
                                Export CSV
                            </Button>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => {
                                    // Create search params from current state
                                    const params: SearchParams = {
                                        query: query,
                                        isRegex: isRegex,
                                        timeRange: timeRange
                                    };

                                    // Add custom time range if selected
                                    if (timeRange === 'custom') {
                                        params.startTime = customStartTime;
                                        params.endTime = customEndTime;
                                    }

                                    // Generate export URL and open in new tab
                                    const exportUrl = exportLogsAsJson(params);
                                    window.open(exportUrl, '_blank');
                                }}
                                className="flex items-center gap-1"
                            >
                                Export JSON
                            </Button>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={toggleFilters}
                                className="flex items-center gap-1"
                            >
                                {showFilters ? 'Hide Filters' : 'Show Filters'}
                            </Button>
                        </div>
                    </div>

                    {showFilters && (
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-muted/20 rounded-md border">
                            <div>
                                <Label htmlFor="levelFilter" className="text-sm">Filter by Level</Label>
                                <input
                                    id="levelFilter"
                                    type="text"
                                    value={filterValues.level}
                                    onChange={(e) => handleFilterChange('level', e.target.value)}
                                    placeholder="Filter by level..."
                                    className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
                                />
                            </div>
                            <div>
                                <Label htmlFor="messageFilter" className="text-sm">Filter by Message</Label>
                                <input
                                    id="messageFilter"
                                    type="text"
                                    value={filterValues.message}
                                    onChange={(e) => handleFilterChange('message', e.target.value)}
                                    placeholder="Filter by message..."
                                    className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
                                />
                            </div>
                            <div>
                                <Label htmlFor="sourceFilter" className="text-sm">Filter by Source</Label>
                                <input
                                    id="sourceFilter"
                                    type="text"
                                    value={filterValues.source}
                                    onChange={(e) => handleFilterChange('source', e.target.value)}
                                    placeholder="Filter by source..."
                                    className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
                                />
                            </div>
                        </div>
                    )}

                    <div className="rounded-md border">
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead>
                                <tr className="border-b bg-muted/50">
                                    <th
                                        className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                                        onClick={() => handleSortClick('timestamp')}
                                    >
                                        <div className="flex items-center gap-1">
                                            Timestamp
                                            {sortColumn === 'timestamp' && (
                                                <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>
                                            )}
                                        </div>
                                    </th>
                                    <th
                                        className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                                        onClick={() => handleSortClick('level')}
                                    >
                                        <div className="flex items-center gap-1">
                                            Level
                                            {sortColumn === 'level' && (
                                                <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>
                                            )}
                                        </div>
                                    </th>
                                    <th
                                        className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                                        onClick={() => handleSortClick('message')}
                                    >
                                        <div className="flex items-center gap-1">
                                            Message
                                            {sortColumn === 'message' && (
                                                <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>
                                            )}
                                        </div>
                                    </th>
                                    <th
                                        className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                                        onClick={() => handleSortClick('source')}
                                    >
                                        <div className="flex items-center gap-1">
                                            Source
                                            {sortColumn === 'source' && (
                                                <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>
                                            )}
                                        </div>
                                    </th>
                                </tr>
                                </thead>
                                <tbody>
                                {processedResults.map((log) => (
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
                    <p className="text-muted-foreground">Enter a search query and click Search, or click Refresh to see
                        the latest logs</p>
                </div>
            )}
        </div>
    );
}
