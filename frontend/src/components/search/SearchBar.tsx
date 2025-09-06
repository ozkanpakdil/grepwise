import Editor, { Monaco } from '@monaco-editor/react';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import type React from 'react';
import { Button } from '@/components/ui/button';
import { SearchParams } from '@/api/logSearch';
import { Clock, Pause, Play, RefreshCw, Regex, Search } from 'lucide-react';

interface Props {
  query: string;
  setQuery: (q: string) => void;
  isRegex: boolean;
  setIsRegex: (v: boolean) => void;
  timeRange: SearchParams['timeRange'];
  setTimeRange: (tr: SearchParams['timeRange']) => void;
  isSearching: boolean;
  onSearch: (e?: React.FormEvent | React.MouseEvent) => void;
  onRefresh: (e?: React.FormEvent | React.MouseEvent) => void;
  autoRefreshEnabled: boolean;
  autoRefreshInterval: string;
  setAutoRefreshEnabled: (b: boolean) => void;
  setAutoRefreshInterval: (s: string) => void;
  setupAutoRefresh: () => void;
  handleEditorDidMount: (editor: any, monaco: Monaco) => void;
  isEditorLoading: boolean;
  // new: external control of the time range panel
  showTimePanel: boolean;
  setShowTimePanel: (b: boolean) => void;
}

export default function SearchBar({
  query,
  setQuery,
  isRegex,
  setIsRegex,
  timeRange,
  setTimeRange,
  isSearching,
  onSearch,
  onRefresh,
  autoRefreshEnabled,
  autoRefreshInterval,
  setAutoRefreshEnabled,
  setAutoRefreshInterval,
  setupAutoRefresh,
  handleEditorDidMount,
  isEditorLoading,
  showTimePanel,
  setShowTimePanel,
}: Props) {
  return (
    <form onSubmit={(e) => onSearch(e)} data-testid="search-form">
      <div className="flex gap-2 items-center">
        <div className="flex-1" data-testid="query-editor">
          <Editor
            height="20px"
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
              readOnly: isEditorLoading,
            }}
            className="monaco-editor-container overflow-hidden"
          />
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex items-center space-x-1">
            <Checkbox
              id="regex"
              checked={isRegex}
              onCheckedChange={(checked: boolean | 'indeterminate') => setIsRegex(!!checked)}
              className="h-4 w-4"
              data-testid="regex-toggle"
            />
            <Label htmlFor="regex" className="text-sm flex items-center">
              <Regex className="h-4 w-4 mr-1" />
            </Label>
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <div className="relative flex items-center space-x-1">
            <Label className="text-sm flex items-center">
              <Clock className="h-4 w-4" />
            </Label>
            <button
              type="button"
              className="h-8 text-xs w-[160px] text-left rounded-md border border-input bg-background px-2"
              onClick={(e) => {
                // store a marker attribute so parent can query position
                (e.currentTarget as HTMLElement).setAttribute('data-time-range-trigger', 'true');
                setShowTimePanel(!showTimePanel);
              }}
              data-testid="time-range"
              data-time-range-trigger
            >
              Time range:{' '}
              {timeRange === 'custom'
                ? 'Custom'
                : timeRange === '1h'
                  ? 'Last 1 hour'
                  : timeRange === '3h'
                    ? 'Last 3 hours'
                    : timeRange === '12h'
                      ? 'Last 12 hours'
                      : timeRange === '24h'
                        ? 'Last 24 hours'
                        : timeRange === '7d'
                          ? 'Last 7 days'
                          : 'Last 30 days'}
            </button>
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <Button type="submit" size="sm" disabled={isSearching} className="px-3" data-testid="run-search">
            {isSearching ? 'Searching...' : <Search className="h-4 w-4" />}
          </Button>
        </div>
        <div className="flex flex-col gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={(e) => onRefresh(e)}
            disabled={isSearching}
            className="px-3"
            data-testid="refresh"
          >
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex items-center space-x-1">
            <Label className="text-sm flex items-center">Live</Label>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                if (autoRefreshEnabled) {
                  setAutoRefreshEnabled(false);
                } else {
                  if (!autoRefreshInterval) setAutoRefreshInterval('5s');
                  setAutoRefreshEnabled(true);
                  setupAutoRefresh();
                }
              }}
              className="px-3"
              data-testid="auto-refresh-toggle"
            >
              {autoRefreshEnabled ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            </Button>
          </div>
        </div>
      </div>
    </form>
  );
}
