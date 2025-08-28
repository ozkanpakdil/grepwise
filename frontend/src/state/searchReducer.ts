import { HistogramData, LogEntry, SearchParams, TimeSlot } from '@/api/logSearch';

export type SortColumn = 'timestamp' | 'level' | 'message' | 'source' | null;
export type SortDirection = 'asc' | 'desc';

export type FilterValues = {
  level: string;
  source: string;
  message: string;
};

export interface SearchState {
  // query + config
  query: string;
  isRegex: boolean;
  timeRange: SearchParams['timeRange'];
  customStartTime?: number;
  customEndTime?: number;

  // async flags
  isSearching: boolean;
  isEditorLoading: boolean;
  isStreaming: boolean;
  isLoadingTimeSlots: boolean;

  // data
  searchResults: LogEntry[];
  totalCount: number | null;
  currentPage: number;
  pageSize: number;
  histogramData: HistogramData[];
  timeSlots: TimeSlot[];

  // ui
  expandedLogId: string | null;
  sortColumn: SortColumn;
  sortDirection: SortDirection;
  filterValues: FilterValues;
  showFilters: boolean;

  // auto refresh
  autoRefreshEnabled: boolean;
  autoRefreshInterval: string; // '5s' | '10s' | '30s'

  // zoom
  zoomStack: { timeRange: SearchParams['timeRange']; startTime?: number; endTime?: number; }[];
}

export const initialSearchState: SearchState = {
  query: '',
  isRegex: false,
  timeRange: '24h',
  customStartTime: undefined,
  customEndTime: undefined,

  isSearching: false,
  isEditorLoading: true,
  isStreaming: false,
  isLoadingTimeSlots: false,

  searchResults: [],
  totalCount: null,
  currentPage: 1,
  pageSize: 100,
  histogramData: [],
  timeSlots: [],

  expandedLogId: null,
  sortColumn: null,
  sortDirection: 'desc',
  filterValues: { level: '', source: '', message: '' },
  showFilters: false,

  autoRefreshEnabled: false,
  autoRefreshInterval: '10s',

  zoomStack: [],
};

export type SearchAction =
  | { type: 'SET_QUERY'; query: string }
  | { type: 'SET_REGEX'; isRegex: boolean }
  | { type: 'SET_TIME_RANGE'; timeRange: SearchParams['timeRange'] }
  | { type: 'SET_CUSTOM_RANGE'; start?: number; end?: number }
  | { type: 'SET_SEARCHING'; value: boolean }
  | { type: 'SET_EDITOR_LOADING'; value: boolean }
  | { type: 'SET_STREAMING'; value: boolean }
  | { type: 'SET_LOADING_TIMESLOTS'; value: boolean }
  | { type: 'SET_RESULTS'; results: LogEntry[] }
  | { type: 'SET_TOTAL'; total: number | null }
  | { type: 'SET_PAGE'; page: number }
  | { type: 'SET_PAGE_SIZE'; pageSize: number }
  | { type: 'SET_HISTOGRAM'; data: HistogramData[] }
  | { type: 'SET_TIMESLOTS'; slots: TimeSlot[] }
  | { type: 'SET_EXPANDED'; id: string | null }
  | { type: 'SET_SORT'; column: SortColumn; direction: SortDirection }
  | { type: 'SET_FILTERS'; filters: Partial<FilterValues> }
  | { type: 'TOGGLE_FILTERS' }
  | { type: 'SET_AUTO_REFRESH'; enabled: boolean; interval?: string }
  | { type: 'PUSH_ZOOM'; state: { timeRange: SearchParams['timeRange']; startTime?: number; endTime?: number } }
  | { type: 'POP_ZOOM' }
  | { type: 'RESET_ALL' }
  | { type: 'BULK_UPDATE'; payload: Partial<SearchState> };

export function searchReducer(state: SearchState, action: SearchAction): SearchState {
  switch (action.type) {
    case 'SET_QUERY': return { ...state, query: action.query };
    case 'SET_REGEX': return { ...state, isRegex: action.isRegex };
    case 'SET_TIME_RANGE': return { ...state, timeRange: action.timeRange };
    case 'SET_CUSTOM_RANGE': return { ...state, customStartTime: action.start, customEndTime: action.end };
    case 'SET_SEARCHING': return { ...state, isSearching: action.value };
    case 'SET_EDITOR_LOADING': return { ...state, isEditorLoading: action.value };
    case 'SET_STREAMING': return { ...state, isStreaming: action.value };
    case 'SET_LOADING_TIMESLOTS': return { ...state, isLoadingTimeSlots: action.value };
    case 'SET_RESULTS': return { ...state, searchResults: action.results };
    case 'SET_TOTAL': return { ...state, totalCount: action.total };
    case 'SET_PAGE': return { ...state, currentPage: action.page };
    case 'SET_PAGE_SIZE': return { ...state, pageSize: action.pageSize };
    case 'SET_HISTOGRAM': return { ...state, histogramData: action.data };
    case 'SET_TIMESLOTS': return { ...state, timeSlots: action.slots };
    case 'SET_EXPANDED': return { ...state, expandedLogId: action.id };
    case 'SET_SORT': return { ...state, sortColumn: action.column, sortDirection: action.direction };
    case 'SET_FILTERS': return { ...state, filterValues: { ...state.filterValues, ...action.filters } };
    case 'TOGGLE_FILTERS': return { ...state, showFilters: !state.showFilters };
    case 'SET_AUTO_REFRESH': return { ...state, autoRefreshEnabled: action.enabled, autoRefreshInterval: action.interval ?? state.autoRefreshInterval };
    case 'PUSH_ZOOM': return { ...state, zoomStack: [...state.zoomStack, action.state] };
    case 'POP_ZOOM': return { ...state, zoomStack: state.zoomStack.slice(0, Math.max(0, state.zoomStack.length - 1)) };
    case 'RESET_ALL': return { ...initialSearchState, pageSize: state.pageSize };
    case 'BULK_UPDATE': return { ...state, ...action.payload };
    default: return state;
  }
}
