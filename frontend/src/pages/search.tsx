import {useEffect, useMemo, useRef, useState, Fragment} from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {useToast} from '@/components/ui/use-toast';
import {Button} from '@/components/ui/button';
import {exportLogsAsCsv, exportLogsAsJson, HistogramData, LogEntry, SearchParams, TimeSlot} from '@/api/logSearch';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Checkbox} from '@/components/ui/checkbox';
import {Label} from '@/components/ui/label';
import LogBarChart from '@/components/LogBarChart';
import MUIBarsChart from '@/components/MUIBarsChart';
import Editor, {Monaco} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import {Clock, RefreshCw, Regex, Search} from 'lucide-react';
import useLocalStorage from "@/components/LocalStorage.ts";

// Type definitions for sorting and filtering
type SortColumn = 'timestamp' | 'level' | 'message' | 'source' | null;
type SortDirection = 'asc' | 'desc';
type FilterValues = {
    level: string;
    source: string;
    message: string;
};

export default function SearchPage() {
    const location = useLocation();
    const navigate = useNavigate();
    // Helper: serialize current UI state to URLSearchParams
    const serializeToParams = (state: {
        query: string;
        isRegex: boolean;
        timeRange: SearchParams['timeRange'];
        startTime?: number;
        endTime?: number;
        pageSize: number;
        autoRefreshEnabled: boolean;
        autoRefreshInterval: string;
    }) => {
        const sp = new URLSearchParams();
        if (state.query?.trim()) sp.set('query', state.query.trim());
        if (state.isRegex) sp.set('isRegex', 'true');
        sp.set('pageSize', String(state.pageSize));
        if (state.autoRefreshEnabled) {
            sp.set('autoRefresh', 'on');
            sp.set('autoRefreshInterval', state.autoRefreshInterval);
        }
        if (state.timeRange === 'custom' && state.startTime && state.endTime) {
            sp.set('timeRange', 'custom');
            sp.set('startTime', String(state.startTime));
            sp.set('endTime', String(state.endTime));
        } else {
            sp.set('timeRange', state.timeRange || '24h');
        }
        return sp;
    };

    // Helper: parse URL into UI state
    const parseFromLocation = () => {
        const sp = new URLSearchParams(window.location.search);
        const parsed: Partial<SearchParams> & {
            pageSize?: number;
            autoRefreshEnabled?: boolean;
            autoRefreshInterval?: string;
            query?: string;
            isRegex?: boolean;
        } = {};
        const q = sp.get('query') || '';
        const isRegex = sp.get('isRegex') === 'true';
        const tr = (sp.get('timeRange') as SearchParams['timeRange']) || undefined;
        const st = sp.get('startTime');
        const et = sp.get('endTime');
        const ps = sp.get('pageSize');
        const ar = sp.get('autoRefresh');
        const ari = sp.get('autoRefreshInterval') || '10s';
        if (q) parsed.query = q;
        if (isRegex) parsed.isRegex = true;
        if (ps) parsed.pageSize = parseInt(ps, 10) || undefined;
        if (ar === 'on') parsed.autoRefreshEnabled = true;
        parsed.autoRefreshInterval = ari;
        if (tr === 'custom' && st && et) {
            parsed.timeRange = 'custom';
            parsed.startTime = Number(st);
            parsed.endTime = Number(et);
        } else if (tr) {
            parsed.timeRange = tr;
        }
        return parsed;
    };
    const [query, setQuery] = useState('');
    const [isRegex, setIsRegex] = useState(false);
    const [timeRange, setTimeRange] = useState<SearchParams['timeRange']>('24h');
    const [customStartTime, setCustomStartTime] = useState<number | undefined>(undefined);
    const [customEndTime, setCustomEndTime] = useState<number | undefined>(undefined);
    const [isSearching, setIsSearching] = useState(false);
    const [searchResults, setSearchResults] = useState<LogEntry[]>([]);
    const [expandedLogId, setExpandedLogId] = useState<string | null>(null);
    const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
    const [histogramData, setHistogramData] = useState<HistogramData[]>([]);
    const [isLoadingTimeSlots, setIsLoadingTimeSlots] = useState(false);
    // Add state for editor loading
    const [isEditorLoading, setIsEditorLoading] = useState(true);
    // Page size and totals
    const [pageSize, setPageSize] = useLocalStorage<number>("grepwise.dashboard.pagesize", 100);
    const [totalCount, setTotalCount] = useState<number | null>(null);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const eventSourceRef = useRef<EventSource | null>(null);
    const histEventSourceRef = useRef<EventSource | null>(null);
    const [isStreaming, setIsStreaming] = useState(false);

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
                handleSearch(undefined, undefined, true);
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

    // Initialize from URL and handle back/forward navigation
    useEffect(() => {
        try {
            const parsed = parseFromLocation();
            if (parsed.query !== undefined) setQuery(parsed.query);
            if (parsed.isRegex !== undefined) setIsRegex(parsed.isRegex);
            if (parsed.pageSize) {
                // pageSize is kept in local storage hook; update via setter
                // avoid 0/NaN
                // setPageSize is stable
                setPageSize(Math.max(1, parsed.pageSize));
            }
            if (parsed.autoRefreshEnabled) setAutoRefreshEnabled(true);
            if (parsed.autoRefreshInterval) setAutoRefreshInterval(parsed.autoRefreshInterval);

            if (parsed.timeRange === 'custom' && parsed.startTime && parsed.endTime) {
                setTimeRange('custom');
                setCustomStartTime(parsed.startTime);
                setCustomEndTime(parsed.endTime);
                // trigger initial search without adding a new history entry (we already have the URL)
                handleSearch(undefined, {start: parsed.startTime, end: parsed.endTime}, false);
            } else if (parsed.timeRange) {
                setTimeRange(parsed.timeRange);
                // If we have a query or regex flag, perform initial search without pushing
                if (parsed.query || parsed.isRegex) {
                    handleSearch(undefined, undefined, false);
                }
            }
        } catch (e) {
            console.warn('Failed to parse initial URL state', e);
        }

        const onPop = () => {
            // When user navigates back/forward, re-parse URL and search accordingly
            try {
                const parsed = parseFromLocation();
                if (parsed.query !== undefined) setQuery(parsed.query);
                else setQuery('');
                setIsRegex(!!parsed.isRegex);
                if (parsed.pageSize) setPageSize(Math.max(1, parsed.pageSize));
                setAutoRefreshEnabled(!!parsed.autoRefreshEnabled);
                if (parsed.autoRefreshInterval) setAutoRefreshInterval(parsed.autoRefreshInterval);

                if (parsed.timeRange === 'custom' && parsed.startTime && parsed.endTime) {
                    setTimeRange('custom');
                    setCustomStartTime(parsed.startTime);
                    setCustomEndTime(parsed.endTime);
                    handleSearch(undefined, {start: parsed.startTime, end: parsed.endTime}, false);
                } else {
                    setTimeRange((parsed.timeRange as any) || '24h');
                    handleSearch(undefined, undefined, false);
                }
            } catch (e) {
                console.warn('Failed to handle popstate', e);
            }
        };

        window.addEventListener('popstate', onPop);
        return () => window.removeEventListener('popstate', onPop);
    }, []);

    // Reset search state when navigating to home or when reset flag is present
    useEffect(() => {
        try {
            const sp = new URLSearchParams(location.search);
            const resetFlag = sp.get('home') === '1' || sp.get('reset') === '1';
            const hasSearchParams = !!(sp.get('query') || sp.get('isRegex') || sp.get('timeRange') || sp.get('startTime') || sp.get('endTime') || sp.get('pageSize') || sp.get('autoRefresh') || sp.get('autoRefreshInterval'));

            // When navigating to root/search without params or explicit reset requested, clear the UI state
            if ((location.pathname === '/' || location.pathname === '/search') && (resetFlag || !hasSearchParams)) {
                // Stop streaming and timers
                if (eventSourceRef.current) {
                    try { eventSourceRef.current.close(); } catch {}
                    eventSourceRef.current = null;
                }
                if (histEventSourceRef.current) {
                    try { histEventSourceRef.current.close(); } catch {}
                    histEventSourceRef.current = null;
                }
                if (autoRefreshTimerRef.current) {
                    clearInterval(autoRefreshTimerRef.current);
                    autoRefreshTimerRef.current = null;
                }
                setIsStreaming(false);

                // Clear inputs and state back to defaults
                setQuery('');
                setIsRegex(false);
                setTimeRange('24h');
                setCustomStartTime(undefined);
                setCustomEndTime(undefined);
                setSearchResults([]);
                setTimeSlots([]);
                setHistogramData([]);
                setExpandedLogId(null);
                setTotalCount(null);
                setCurrentPage(1);
                setSortColumn(null);
                setSortDirection('desc');
                setFilterValues({ level: '', source: '', message: '' });
                setShowFilters(false);
                setIsSearching(false);
                setIsLoadingTimeSlots(false);
                setAutoRefreshEnabled(false);
                setAutoRefreshInterval('10s');
                // Clear editor content if mounted
                try { editorRef.current?.setValue(''); } catch {}

                // If we used a reset flag in the URL, clean it up to a nice URL
                if (resetFlag && (location.search?.length ?? 0) > 0) {
                    navigate(location.pathname, { replace: true });
                }
            }
        } catch (e) {
            console.warn('Failed to handle location-driven reset', e);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.pathname, location.search]);

    const handleSearch = async (e?: React.FormEvent | React.MouseEvent, overrideRange?: {
        start: number;
        end: number
    }, pushHistory: boolean = true) => {
        // If this is a form submission event, prevent the default behavior
        if (e && 'preventDefault' in e) {
            e.preventDefault();
        }

        // Get the current value from the editor if available
        const currentEditorValue = editorRef.current?.getValue() || query;

        // Update the query state with the current editor value
        if (currentEditorValue !== query) {
            setQuery(currentEditorValue);
        }

        setIsSearching(true);
        setIsLoadingTimeSlots(true);

        try {
            // Close any previous stream
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
            }
            if (histEventSourceRef.current) {
                histEventSourceRef.current.close();
                histEventSourceRef.current = null;
            }
            setSearchResults([]);
            setTimeSlots([]);
            setHistogramData([]);
            setExpandedLogId(null);
            setTotalCount(null);
            setCurrentPage(1);

            // Calculate time range
            let startTime: number;
            let endTime: number;

            if (overrideRange) {
                startTime = overrideRange.start;
                endTime = overrideRange.end;
            } else if (timeRange === 'custom' && customStartTime && customEndTime) {
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

            // Determine appropriate interval based on time range
            let interval: string;
            const timeRangeMs = endTime - startTime;
            if (timeRangeMs <= 60 * 60 * 1000) interval = '1m';
            else if (timeRangeMs <= 3 * 60 * 60 * 1000) interval = '5m';
            else if (timeRangeMs <= 12 * 60 * 60 * 1000) interval = '15m';
            else interval = '30m';

            // Start SSE stream
            const params = new URLSearchParams();
            const trimmed = currentEditorValue.trim();
            // Treat "*" as match-all by omitting query param
            if (trimmed && trimmed !== '*') params.set('query', trimmed);
            if (isRegex) params.set('isRegex', 'true');
            if (!overrideRange && timeRange !== 'custom') {
                // Use backend default of last 30 days unless user chose a specific range different than 24h
                if (timeRange && timeRange !== '24h') {
                    params.set('timeRange', timeRange);
                }
            } else {
                params.set('startTime', String(startTime));
                params.set('endTime', String(endTime));
            }
            // Let backend pick interval (daily for 30d) when not using custom explicit range
            if (overrideRange || timeRange === 'custom' || (timeRange && timeRange !== '24h')) {
                params.set('interval', interval);
            }
            params.set('pageSize', String(pageSize));

            const url = `http://localhost:8080/api/logs/search/stream?${params.toString()}`;
            const es = new EventSource(url);
            eventSourceRef.current = es;

            // logs stream: only care about page and done
            es.addEventListener('page', (ev: MessageEvent) => {
                try {
                    const logs: LogEntry[] = JSON.parse((ev as any).data);
                    setSearchResults(logs);
                    setCurrentPage(1);
                } catch (e) {
                    console.error('page parse error', e);
                }
            });

            es.addEventListener('done', (ev: MessageEvent) => {
                try {
                    const d = JSON.parse((ev as any).data);
                    setTotalCount(d.total ?? null);
                } catch (e) {
                    console.error('done parse error', e);
                } finally {
                    setIsStreaming(false);
                    es.close();
                    eventSourceRef.current = null;
                }
            });

            es.addEventListener('error', () => {
                console.error('SSE error (logs stream)');
            });

            // Start separate histogram SSE
            const histParams = new URLSearchParams(params.toString());
            // Do not send pageSize for timetable
            histParams.delete('pageSize');
            const histUrl = `http://localhost:8080/api/logs/search/timetable/stream?${histParams.toString()}`;
            const esHist = new EventSource(histUrl);
            histEventSourceRef.current = esHist;
            setIsStreaming(true);

            esHist.addEventListener('init', (ev: MessageEvent) => {
                try {
                    const data = JSON.parse((ev as any).data);
                    const buckets: { timestamp: string; count: number }[] = data.buckets || [];
                    setHistogramData(buckets);
                } catch (e) {
                    console.error('timetable init parse error', e);
                }
            });

            esHist.addEventListener('hist', (ev: MessageEvent) => {
                try {
                    const snapshot: HistogramData[] = JSON.parse((ev as any).data);
                    setHistogramData(snapshot);
                } catch (e) {
                    console.error('timetable hist parse error', e);
                }
            });

            esHist.addEventListener('done', () => {
                setIsStreaming(false);
                try { esHist.close(); } catch {}
                histEventSourceRef.current = null;
            });

            esHist.addEventListener('error', () => {
                console.error('SSE error (timetable stream)');
                setIsStreaming(false);
                try { esHist.close(); } catch {}
                histEventSourceRef.current = null;
            });

            // Push URL state for history/share if requested
            try {
                if (pushHistory) {
                    const sp = serializeToParams({
                        query: currentEditorValue || '',
                        isRegex,
                        timeRange: (overrideRange ? 'custom' : timeRange) as SearchParams['timeRange'],
                        startTime: overrideRange ? startTime : (timeRange === 'custom' ? startTime : undefined),
                        endTime: overrideRange ? endTime : (timeRange === 'custom' ? endTime : undefined),
                        pageSize,
                        autoRefreshEnabled,
                        autoRefreshInterval
                    });
                    const newUrl = `${window.location.pathname}?${sp.toString()}`;
                    window.history.pushState({type: 'grepwise-search'}, '', newUrl);
                }
            } catch (e) {
                console.warn('URL history push failed', e);
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

    // Fetch a specific page from the server (non-SSE)
    const fetchPage = async (page: number) => {
        try {
            setExpandedLogId(null);
            if (totalCount === null) return;
            const trimmed = (editorRef.current?.getValue() || query).trim();
            const sp = new URLSearchParams();
            if (trimmed && trimmed !== '*') sp.set('query', trimmed);
            if (isRegex) sp.set('isRegex', 'true');
            if (timeRange !== 'custom' && (!customStartTime || !customEndTime)) {
                // Use backend default of last 30 days unless user chose a specific range different than 24h
                if (timeRange && timeRange !== '24h') {
                    sp.set('timeRange', timeRange);
                }
            } else {
                // ensure we pass concrete times
                const st = customStartTime ?? (Date.now() - 24 * 60 * 60 * 1000);
                const et = customEndTime ?? Date.now();
                sp.set('startTime', String(st));
                sp.set('endTime', String(et));
            }
            sp.set('page', String(page));
            sp.set('pageSize', String(pageSize));
            const res = await fetch(`http://localhost:8080/api/logs/search/page?${sp.toString()}`);
            if (!res.ok) throw new Error('Failed to fetch page');
            const data = await res.json();
            setSearchResults(data.items || []);
            setTotalCount(data.total ?? totalCount);
            setCurrentPage(data.page || page);
        } catch (e) {
            console.error('Pagination error', e);
            toast({ title: 'Pagination error', description: 'Failed to load the requested page', variant: 'destructive' });
        }
    };

    // Refresh data without changing search parameters
    const refreshData = async () => {
        if (isSearching) return; // Don't refresh if already searching

        // Get the current value from the editor if available
        const currentEditorValue = editorRef.current?.getValue() || query;

        // Update the query state with the current editor value
        if (currentEditorValue !== query) {
            setQuery(currentEditorValue);
        }

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

            // For auto-refresh, just restart the stream
            await handleSearch();

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

    // Build regex for highlighting based on query and isRegex
    const highlightRegex = useMemo(() => {
        if (!query || !query.trim()) return null;
        try {
            if (isRegex) {
                return new RegExp(query, 'gi');
            }
            // Escape special regex chars for plain text search
            const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            return new RegExp(escaped, 'gi');
        } catch (e) {
            // If regex is invalid, disable highlighting to avoid runtime errors
            return null;
        }
    }, [query, isRegex]);

    // Render helper to highlight matched parts in a given text
    const renderHighlighted = (text: string) => {
        if (!text) return '';
        if (!highlightRegex) return text;
        const parts: (string | JSX.Element)[] = [];
        let lastIndex = 0;
        let match: RegExpExecArray | null;
        // Reset lastIndex for safety when regex has global flag
        highlightRegex.lastIndex = 0;
        while ((match = highlightRegex.exec(text)) !== null) {
            const start = match.index;
            const end = start + (match[0]?.length || 0);
            if (start > lastIndex) {
                parts.push(text.slice(lastIndex, start));
            }
            if (end > start) {
                parts.push(
                    <mark key={parts.length} className="bg-yellow-200 dark:bg-yellow-700 px-0.5 rounded">
                        {text.slice(start, end)}
                    </mark>
                );
            }
            lastIndex = end;
            // Avoid infinite loop on zero-width matches
            if (match[0]?.length === 0) {
                highlightRegex.lastIndex++;
            }
        }
        if (lastIndex < text.length) {
            parts.push(text.slice(lastIndex));
        }
        return <>{parts}</>;
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
        setExpandedLogId((prev) => (prev === log.id ? null : log.id));
    };

    const [zoomStack, setZoomStack] = useState<{
        timeRange: SearchParams['timeRange'];
        startTime?: number;
        endTime?: number;
    }[]>([]);

    const handleTimeSlotClick = (slot: TimeSlot) => {
        // Double-click zoom handler (single click intentionally does nothing)
        const currentEditorValue = editorRef.current?.getValue() || query;
        if (currentEditorValue !== query) {
            setQuery(currentEditorValue);
        }

        // Determine slot size from neighboring buckets (UTC-aligned by timestamps)
        const idx = timeSlots.findIndex(s => s.time === slot.time);
        let slotSizeMs = 60 * 60 * 1000; // fallback 1h
        if (idx >= 0) {
            const prev = idx > 0 ? timeSlots[idx - 1] : undefined;
            const next = idx < timeSlots.length - 1 ? timeSlots[idx + 1] : undefined;
            if (next) slotSizeMs = Math.max(1000, next.time - slot.time);
            else if (prev) slotSizeMs = Math.max(1000, slot.time - prev.time);
        }

        // Stop further zooming if already at second-level granularity
        if (slotSizeMs <= 1000) {
            toast({title: 'Max zoom level', description: 'You are already at second-level resolution (UTC).'});
            return;
        }

        // Push current range to zoom stack for "Zoom out"
        setZoomStack(prev => [
            ...prev,
            timeRange === 'custom' && customStartTime && customEndTime
                ? {timeRange: 'custom', startTime: customStartTime, endTime: customEndTime}
                : {timeRange}
        ]);

        // Update to custom range for selected slot
        const newStart = slot.time;
        const newEnd = slot.time + slotSizeMs;
        setTimeRange('custom');
        setCustomStartTime(newStart);
        setCustomEndTime(newEnd);

        // Restart search/stream within the new range to keep auto-refresh behavior using override to avoid setState race
        handleSearch(undefined, {start: newStart, end: newEnd}, true);

        toast({
            title: 'Zoomed in',
            description: `UTC ${new Date(newStart).toUTCString()} → ${new Date(newEnd).toUTCString()}`,
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

            <form onSubmit={(e) => handleSearch(e, undefined, true)} className="h-[20px]">
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
                                <Regex className="h-4 w-4 mr-1"/>
                            </Label>
                        </div>
                    </div>
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center space-x-1">
                            <Label htmlFor="timeRange" className="text-sm flex items-center">
                                <Clock className="h-4 w-4"/>
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
                        <div className="flex items-center space-x-2">
                            <Label htmlFor="pageSize" className="text-sm">Page size</Label>
                            <input
                                id="pageSize"
                                type="number"
                                min={1}
                                value={pageSize}
                                onChange={(e) => setPageSize(Math.max(1, parseInt(e.target.value || '1', 10)))}
                                className="h-8 w-[90px] rounded-md border border-input bg-background px-2 text-sm"
                            />
                        </div>
                    </div>
                    <div className="flex flex-col gap-2">
                        <Button type="submit" size="sm" disabled={isSearching} className="px-3">
                            {isSearching ? 'Searching...' : <Search className="h-4 w-4"/>}
                        </Button>
                    </div>
                    <div className="flex flex-col gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={(e) => handleSearch(e, undefined, true)}
                            disabled={isSearching}
                            className="px-3"
                        >
                            <RefreshCw className="h-4 w-4"/>
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
                        <div className="flex items-center gap-3">
                            {autoRefreshEnabled && (
                                <div className="text-xs text-muted-foreground flex items-center">
                                    <RefreshCw className="h-3 w-3 mr-1 animate-spin"/>
                                    Auto-refreshing every {autoRefreshInterval}
                                </div>
                            )}
                            {zoomStack.length > 0 && (
                                <button
                                    className="text-xs text-blue-600 hover:underline"
                                    onClick={() => {
                                        const prev = zoomStack[zoomStack.length - 1];
                                        setZoomStack(z => z.slice(0, z.length - 1));
                                        if (prev.timeRange === 'custom' && prev.startTime && prev.endTime) {
                                            setTimeRange('custom');
                                            setCustomStartTime(prev.startTime);
                                            setCustomEndTime(prev.endTime);
                                        } else {
                                            setTimeRange(prev.timeRange || '24h');
                                        }
                                        handleSearch(undefined, undefined, true);
                                    }}
                                >
                                    Zoom out
                                </button>
                            )}
                            {isStreaming && (
                                <div className="text-xs text-muted-foreground">
                                    Computing histogram…
                                </div>
                            )}
                        </div>
                    </div>

                    {histogramData.length > 0 ? (
                        <div className="w-full h-64">
                            <MUIBarsChart
                                data={histogramData}
                                onBarDoubleClick={(start, end) => {
                                    // Push current to zoom stack
                                    setZoomStack(prev => [
                                        ...prev,
                                        timeRange === 'custom' && customStartTime && customEndTime
                                            ? {timeRange: 'custom', startTime: customStartTime, endTime: customEndTime}
                                            : {timeRange}
                                    ]);
                                    // Prevent zoom beyond seconds
                                    if (end - start <= 1000) return;
                                    setTimeRange('custom');
                                    setCustomStartTime(start);
                                    setCustomEndTime(end);
                                    handleSearch(undefined, {start, end}, true);
                                    toast({
                                        title: 'Zoomed in',
                                        description: `UTC ${new Date(start).toUTCString()} → ${new Date(end).toUTCString()}`
                                    });
                                }}
                            />
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
                Showing {processedResults.length} of {totalCount ?? '…'} logs
              </span>
                        </div>
                        <div className="flex items-center gap-2">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => {
                                    // Create search params from current state
                                    const params: SearchParams = {
                                        query: query.trim() === '*' ? undefined : query,
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
                                        query: query.trim() === '*' ? undefined : query,
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
                                    <Fragment key={log.id}>
                                        <tr
                                            className="border-b hover:bg-muted/50 cursor-pointer"
                                            onClick={() => handleLogClick(log)}
                                        >
                                            <td className="px-4 py-2 text-sm">{formatTimestamp(log.timestamp)}</td>
                                            <td className="px-4 py-2 text-sm">
                                                <span className={getLevelClass(log.level)}>
                                                    {log.level}
                                                </span>
                                            </td>
                                            <td className="px-4 py-2 text-sm">{renderHighlighted(log.message)}</td>
                                            <td className="px-4 py-2 text-sm">{log.source}</td>
                                        </tr>
                                        {expandedLogId === log.id && (
                                            <tr className="border-b bg-muted/20">
                                                <td colSpan={4} className="px-4 py-3">
                                                    <div className="flex justify-between items-start mb-2">
                                                        <h3 className="text-sm font-medium">Log Details</h3>
                                                        <Button variant="ghost" size="sm" onClick={() => setExpandedLogId(null)}>Close</Button>
                                                    </div>
                                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                        <div>
                                                            <p className="text-xs font-medium">Timestamp</p>
                                                            <p className="text-xs">{formatTimestamp(log.timestamp)}</p>
                                                        </div>
                                                        <div>
                                                            <p className="text-xs font-medium">Level</p>
                                                            <p className={`text-xs ${getLevelClass(log.level)}`}>{log.level}</p>
                                                        </div>
                                                        <div>
                                                            <p className="text-xs font-medium">Source</p>
                                                            <p className="text-xs">{log.source}</p>
                                                        </div>
                                                        <div>
                                                            <p className="text-xs font-medium">ID</p>
                                                            <p className="text-xs">{log.id}</p>
                                                        </div>
                                                    </div>
                                                    <div className="mt-3">
                                                        <p className="text-xs font-medium">Message</p>
                                                        <p className="text-xs mt-1 p-2 bg-background rounded-md">{log.message}</p>
                                                    </div>
                                                    <div className="mt-3">
                                                        <p className="text-xs font-medium">Metadata</p>
                                                        <pre className="text-xs mt-1 p-2 bg-background rounded-md overflow-x-auto">{JSON.stringify(log.metadata, null, 2)}</pre>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </Fragment>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Pagination controls */}
                    {totalCount !== null && totalCount > pageSize && (
                        <div className="flex items-center justify-between mt-3">
                            <div className="text-sm text-muted-foreground">
                                Page {currentPage} of {Math.ceil(totalCount / pageSize)}
                            </div>
                            <div className="flex gap-2">
                                <Button
                                    variant="outline"
                                    size="sm"
                                    disabled={currentPage <= 1}
                                    onClick={() => fetchPage(Math.max(1, currentPage - 1))}
                                >
                                    Previous
                                </Button>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    disabled={currentPage >= Math.ceil(totalCount / pageSize)}
                                    onClick={() => fetchPage(Math.min(Math.ceil(totalCount! / pageSize), currentPage + 1))}
                                >
                                    Next
                                </Button>
                            </div>
                        </div>
                    )}
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
