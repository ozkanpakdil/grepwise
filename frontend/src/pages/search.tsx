import {useEffect, useMemo, useReducer, useRef} from 'react';
import {useUrlState} from '@/hooks/useUrlState';
import {useLocation, useNavigate} from 'react-router-dom';
import {useToast} from '@/components/ui/use-toast';
import {HistogramData, LogEntry, SearchParams, TimeSlot} from '@/api/logSearch';
import SearchHistogram from '@/components/search/SearchHistogram';
import {Monaco} from '@monaco-editor/react';
import {useTimeRange} from '@/hooks/useTimeRange';
import * as monaco from 'monaco-editor';
import {RefreshCw} from 'lucide-react';
import useLocalStorage from "@/components/LocalStorage.ts";
import {useAutoRefresh} from '@/hooks/useAutoRefresh';
import SearchResults from '@/components/search/SearchResults';
import TimeRangePicker from '@/components/search/TimeRangePicker';
import SearchBar from '@/components/search/SearchBar';
import {SearchService} from '@/services/SearchService';
import {apiUrl, config} from '@/config';
import {StreamingService} from '@/services/StreamingService';
import {initialSearchState, searchReducer} from '@/state/searchReducer';

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
    // Helper: serialize current UI state to URLSearchParams (moved to useUrlState hook)
    // Keeping a thin wrapper for backwards-compatibility within this file
    // URL state helpers from custom hook
    const {serialize: serializeToParams, parse: parseFromLocation, push: pushUrl} = useUrlState();
    const {computeRange, pickInterval} = useTimeRange();
    const [pageSize, setPageSize] = useLocalStorage<number>("grepwise.dashboard.pagesize", 100);
    const [s, dispatch] = useReducer(searchReducer, {...initialSearchState, pageSize});
    const query = s.query;
    const setQuery = (v: string) => dispatch({type: 'SET_QUERY', query: v});
    const isRegex = s.isRegex;
    const setIsRegex = (v: boolean) => dispatch({type: 'SET_REGEX', isRegex: v});
    const timeRange = s.timeRange;
    const setTimeRange = (v: SearchParams['timeRange']) => dispatch({type: 'SET_TIME_RANGE', timeRange: v});
    const customStartTime = s.customStartTime;
    const setCustomStartTime = (v?: number) => dispatch({type: 'SET_CUSTOM_RANGE', start: v, end: s.customEndTime});
    const customEndTime = s.customEndTime;
    const setCustomEndTime = (v?: number) => dispatch({type: 'SET_CUSTOM_RANGE', start: s.customStartTime, end: v});
    const isSearching = s.isSearching;
    const setIsSearching = (v: boolean) => dispatch({type: 'SET_SEARCHING', value: v});
    const searchResults = s.searchResults;
    const setSearchResults = (v: LogEntry[]) => dispatch({type: 'SET_RESULTS', results: v});
    const expandedLogId = s.expandedLogId;
    const setExpandedLogId = (v: string | null) => dispatch({type: 'SET_EXPANDED', id: v});
    const timeSlots = s.timeSlots;
    const setTimeSlots = (v: TimeSlot[]) => dispatch({type: 'SET_TIMESLOTS', slots: v});
    const histogramData = s.histogramData;
    const setHistogramData = (v: HistogramData[]) => dispatch({type: 'SET_HISTOGRAM', data: v});
    const isLoadingTimeSlots = s.isLoadingTimeSlots;
    const setIsLoadingTimeSlots = (v: boolean) => dispatch({type: 'SET_LOADING_TIMESLOTS', value: v});
    // Add state for editor loading
    const isEditorLoading = s.isEditorLoading;
    const setIsEditorLoading = (v: boolean) => dispatch({type: 'SET_EDITOR_LOADING', value: v});
    // Page size and totals
    const totalCount = s.totalCount;
    const setTotalCount = (v: number | null) => dispatch({type: 'SET_TOTAL', total: v});
    const currentPage = s.currentPage;
    const setCurrentPage = (v: number) => dispatch({type: 'SET_PAGE', page: v});
    const streamingRef = useRef<StreamingService | null>(null);
    const isStreaming = s.isStreaming;
    const setIsStreaming = (v: boolean) => dispatch({type: 'SET_STREAMING', value: v});

    // Auto-refresh settings
    const autoRefreshEnabled = s.autoRefreshEnabled;
    const setAutoRefreshEnabled = (v: boolean) => dispatch({type: 'SET_AUTO_REFRESH', enabled: v});
    const autoRefreshInterval = s.autoRefreshInterval;
    const setAutoRefreshInterval = (v: string) => dispatch({
        type: 'SET_AUTO_REFRESH',
        enabled: s.autoRefreshEnabled,
        interval: v
    });
    // Auto-refresh handled by custom hook
    const {timerRef: autoRefreshTimerRef} = useAutoRefresh(autoRefreshEnabled, autoRefreshInterval, () => {
        refreshData();
    });

    // Sorting state
    const sortColumn = s.sortColumn;
    const setSortColumn = (v: SortColumn) => dispatch({type: 'SET_SORT', column: v, direction: s.sortDirection});
    const sortDirection = s.sortDirection;
    const setSortDirection = (v: SortDirection) => dispatch({type: 'SET_SORT', column: s.sortColumn, direction: v});

    // Filtering state
    const filterValues = s.filterValues;
    const setFilterValues = (updater: any) => {
        // supports prev => ({...prev, ...}) and object directly
        if (typeof updater === 'function') {
            const next = updater(s.filterValues);
            dispatch({type: 'SET_FILTERS', filters: next});
        } else {
            dispatch({type: 'SET_FILTERS', filters: updater});
        }
    };
    const showFilters = s.showFilters;
    const setShowFilters = (vOrUpdater: any) => {
        if (typeof vOrUpdater === 'function') {
            const next = !!vOrUpdater(s.showFilters);
            if (next !== s.showFilters) dispatch({type: 'TOGGLE_FILTERS'});
        } else {
            const next = !!vOrUpdater;
            if (next !== s.showFilters) dispatch({type: 'TOGGLE_FILTERS'});
        }
    };

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
                if (streamingRef.current) {
                    streamingRef.current.stopAll();
                    streamingRef.current = null;
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
                setFilterValues({level: '', source: '', message: ''});
                setShowFilters(false);
                setIsSearching(false);
                setIsLoadingTimeSlots(false);
                setAutoRefreshEnabled(false);
                setAutoRefreshInterval('10s');
                // Clear editor content if mounted
                try {
                    editorRef.current?.setValue('');
                } catch {
                }

                // If we used a reset flag in the URL, clean it up to a nice URL
                if (resetFlag && (location.search?.length ?? 0) > 0) {
                    navigate(location.pathname, {replace: true});
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
            if (streamingRef.current) {
                streamingRef.current.stopAll();
                streamingRef.current = null;
            }
            setSearchResults([]);
            setTimeSlots([]);
            setHistogramData([]);
            setExpandedLogId(null);
            setTotalCount(null);
            setCurrentPage(1);

            // Calculate time range using hook util functions
            const {startTime, endTime} = computeRange(timeRange, customStartTime, customEndTime, overrideRange);
            // Determine appropriate interval based on time range
            const interval = pickInterval(startTime, endTime);

            // Start SSE stream
            const trimmed = currentEditorValue.trim();
            const params = SearchService.buildStreamParams({
                query: trimmed,
                isRegex,
                timeRange: (!overrideRange ? timeRange : 'custom') as SearchParams['timeRange'],
                startTime: overrideRange ? startTime : (timeRange === 'custom' ? startTime : undefined),
                endTime: overrideRange ? endTime : (timeRange === 'custom' ? endTime : undefined),
                interval: (overrideRange || timeRange === 'custom' || (timeRange && timeRange !== '24h')) ? interval : undefined,
                pageSize
            });

            const logsUrl = `${apiUrl(`${config.apiPaths.logs}/search/stream`)}?${params.toString()}`;
            const histParams = SearchService.buildHistogramParamsFrom(params);
            const histUrl = `${apiUrl(`${config.apiPaths.logs}/search/timetable/stream`)}?${histParams.toString()}`;
            setIsStreaming(true);
            const streaming = new StreamingService();
            streamingRef.current = streaming;
            streaming.start(logsUrl, histUrl, {
                onLogsPage: (json) => {
                    try {
                        const logs: LogEntry[] = JSON.parse(json);
                        setSearchResults(logs);
                        setCurrentPage(1);
                    } catch (e) {
                        console.error('page parse error', e);
                    }
                },
                onLogsDone: (json) => {
                    try {
                        const d = JSON.parse(json);
                        setTotalCount(d.total ?? null);
                    } catch (e) {
                        console.error('done parse error', e);
                    } finally {
                        setIsStreaming(false);
                    }
                },
                onLogsError: () => {
                    console.error('SSE error (logs stream)');
                },
                onHistInit: (json) => {
                    try {
                        const data = JSON.parse(json);
                        const buckets: { timestamp: string; count: number }[] = data.buckets || [];
                        setHistogramData(buckets);
                    } catch (e) {
                        console.error('timetable init parse error', e);
                    }
                },
                onHistUpdate: (json) => {
                    try {
                        const snapshot: HistogramData[] = JSON.parse(json);
                        setHistogramData(snapshot);
                    } catch (e) {
                        console.error('timetable hist parse error', e);
                    }
                },
                onHistDone: () => {
                    setIsStreaming(false);
                },
                onHistError: () => {
                    console.error('SSE error (timetable stream)');
                    setIsStreaming(false);
                }
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
                    // useUrlState push
                    pushUrl(sp);
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

    // Setup auto-refresh timer (moved to useAutoRefresh hook)
    const setupAutoRefresh = () => {
        // No-op: handled by useAutoRefresh
    };

    // Fetch a specific page from the server (non-SSE)
    const fetchPage = async (page: number) => {
        try {
            setExpandedLogId(null);
            if (totalCount === null) return;
            const trimmed = (editorRef.current?.getValue() || query).trim();
            const st = customStartTime;
            const et = customEndTime;
            const data = await SearchService.fetchPage<LogEntry>({
                query: trimmed,
                isRegex,
                timeRange,
                startTime: st,
                endTime: et,
                page,
                pageSize
            });
            setSearchResults(data.items || []);
            setTotalCount(data.total ?? totalCount);
            setCurrentPage(data.page || page);
        } catch (e) {
            console.error('Pagination error', e);
            toast({
                title: 'Pagination error',
                description: 'Failed to load the requested page',
                variant: 'destructive'
            });
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

    // Build current search params for export and other utilities
    const buildCurrentSearchParams = (): SearchParams => {
        const params: SearchParams = {
            query: query.trim() === '*' ? undefined : query,
            isRegex,
            timeRange,
        };
        if (timeRange === 'custom') {
            params.startTime = customStartTime;
            params.endTime = customEndTime;
        }
        return params;
    };

    const onExportCsv = (params: SearchParams) => {
        const exportUrl = SearchService.exportCsvUrl(params);
        window.open(exportUrl, '_blank');
    };

    const onExportJson = (params: SearchParams) => {
        const exportUrl = SearchService.exportJsonUrl(params);
        window.open(exportUrl, '_blank');
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

    const zoomStack = s.zoomStack;
    const setZoomStack = (updater: any) => {
        const next = typeof updater === 'function' ? updater(s.zoomStack) : updater;
        // figure out operation by comparing length change
        if (next.length === s.zoomStack.length + 1) {
            const last = next[next.length - 1];
            dispatch({type: 'PUSH_ZOOM', state: last});
        } else if (next.length === s.zoomStack.length - 1) {
            dispatch({type: 'POP_ZOOM'});
        } else {
            // bulk update
            dispatch({type: 'BULK_UPDATE', payload: {zoomStack: next}});
        }
    };

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

            <SearchBar
                query={query}
                setQuery={setQuery}
                isRegex={isRegex}
                setIsRegex={setIsRegex}
                timeRange={timeRange}
                setTimeRange={setTimeRange}
                pageSize={pageSize}
                setPageSize={setPageSize}
                isSearching={isSearching}
                onSearch={(e) => handleSearch(e, undefined, true)}
                onRefresh={(e) => handleSearch(e as any, undefined, true)}
                autoRefreshEnabled={autoRefreshEnabled}
                autoRefreshInterval={autoRefreshInterval}
                setAutoRefreshEnabled={setAutoRefreshEnabled}
                setAutoRefreshInterval={setAutoRefreshInterval}
                setupAutoRefresh={setupAutoRefresh}
                handleEditorDidMount={handleEditorDidMount}
                isEditorLoading={isEditorLoading}
            />

            <div className="flex flex-wrap gap-6">

                <TimeRangePicker
                    timeRange={timeRange}
                    setTimeRange={(tr) => setTimeRange(tr)}
                    customStartTime={customStartTime}
                    customEndTime={customEndTime}
                    setCustomStartTime={setCustomStartTime}
                    setCustomEndTime={setCustomEndTime}
                />
            </div>

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
                                        setZoomStack((z: string | any[]) => z.slice(0, z.length - 1));
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

                    <SearchHistogram
                        histogramData={histogramData}
                        timeSlots={timeSlots}
                        timeRange={timeRange}
                        isStreaming={isStreaming}
                        isLoading={isLoadingTimeSlots}
                        onZoom={(start, end) => {
                            setZoomStack((prev: any) => [
                                ...prev,
                                timeRange === 'custom' && customStartTime && customEndTime
                                    ? {timeRange: 'custom', startTime: customStartTime, endTime: customEndTime}
                                    : {timeRange}
                            ]);
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
                        onTimeSlotClick={handleTimeSlotClick}
                    />
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
                <SearchResults
                    results={searchResults}
                    processedResults={processedResults}
                    totalCount={totalCount}
                    pageSize={pageSize}
                    currentPage={currentPage}
                    sortColumn={sortColumn}
                    sortDirection={sortDirection}
                    onSort={(col) => handleSortClick(col)}
                    expandedLogId={expandedLogId}
                    onRowClick={(id) => setExpandedLogId(expandedLogId === id ? null : id)}
                    renderHighlighted={renderHighlighted}
                    getLevelClass={getLevelClass}
                    formatTimestamp={formatTimestamp}
                    showFilters={showFilters}
                    filterValues={filterValues as any}
                    onFilterChange={(field: any, value: string) => handleFilterChange(field, value)}
                    onToggleFilters={toggleFilters}
                    currentSearchParams={buildCurrentSearchParams()}
                    onExportCsv={onExportCsv}
                    onExportJson={onExportJson}
                    onPageChange={(page) => fetchPage(page)}
                />
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
