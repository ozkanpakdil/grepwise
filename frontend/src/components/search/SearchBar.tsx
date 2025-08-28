import React from 'react';
import Editor, { Monaco } from '@monaco-editor/react';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { SearchParams } from '@/api/logSearch';
import { Clock, RefreshCw, Regex, Search } from 'lucide-react';

interface Props {
  query: string;
  setQuery: (q: string) => void;
  isRegex: boolean;
  setIsRegex: (v: boolean) => void;
  timeRange: SearchParams['timeRange'];
  setTimeRange: (tr: SearchParams['timeRange']) => void;
  pageSize: number;
  setPageSize: (n: number) => void;
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
}

export default function SearchBar({
  query,
  setQuery,
  isRegex,
  setIsRegex,
  timeRange,
  setTimeRange,
  pageSize,
  setPageSize,
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
}: Props) {
  return (
    <form onSubmit={(e) => onSearch(e)} className="h-[20px]">
      <div className="flex h-full gap-2">
        <div className="flex-1 h-full w-96">
          <Editor
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
            className="monaco-editor-container h-full overflow-hidden"
          />
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex items-center space-x-1">
            <Checkbox
              id="regex"
              checked={isRegex}
              onCheckedChange={(checked: boolean | 'indeterminate') => setIsRegex(!!checked)}
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
            <Select value={timeRange} onValueChange={(value) => setTimeRange(value as SearchParams['timeRange'])}>
              <SelectTrigger className="h-8 text-xs w-[120px]" id="timeRange">
                <SelectValue placeholder="Time range" />
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
              value={autoRefreshEnabled ? autoRefreshInterval : 'off'}
              onValueChange={(value) => {
                if (value === 'off') {
                  setAutoRefreshEnabled(false);
                } else {
                  setAutoRefreshEnabled(true);
                  setAutoRefreshInterval(value);
                  setupAutoRefresh();
                }
              }}
            >
              <SelectTrigger className="h-8 text-xs w-[80px]" id="autoRefresh">
                <SelectValue placeholder="Off" />
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
    </form>
  );
}
