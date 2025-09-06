import { Label } from '@/components/ui/label';
import { SearchParams } from '@/api/logSearch';

interface Props {
  timeRange: SearchParams['timeRange'];
  setTimeRange: (tr: SearchParams['timeRange']) => void;
  customStartTime?: number;
  customEndTime?: number;
  setCustomStartTime: (ms: number | undefined) => void;
  setCustomEndTime: (ms: number | undefined) => void;
  onClose?: () => void;
}

export default function TimeRangePicker({
  timeRange,
  setTimeRange,
  customStartTime,
  customEndTime,
  setCustomStartTime,
  setCustomEndTime,
}: Props) {
  const presets: { label: string; value: SearchParams['timeRange'] }[] = [
    { label: 'Last 1 hour', value: '1h' },
    { label: 'Last 3 hours', value: '3h' },
    { label: 'Last 12 hours', value: '12h' },
    { label: 'Last 24 hours', value: '24h' },
    { label: 'Last 7 days', value: '7d' },
    { label: 'Last 30 days', value: '30d' },
  ];

  return (
    <>
      <div className="border border-input bg-popover text-popover-foreground shadow-md rounded-md p-3 w-full max-w-[640px]">
        <div className="flex flex-wrap gap-2 mb-3">
          {presets.map((p) => (
            <button
              key={p.label}
              type="button"
              className="text-xs px-2 py-1 rounded-md border hover:bg-accent hover:text-accent-foreground"
              onClick={() => { setTimeRange(p.value); onClose && onClose(); }}
            >
              {p.label}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <Label htmlFor="startTime">From:</Label>
          <input
            type="datetime-local"
            id="startTime"
            value={customStartTime ? new Date(customStartTime).toISOString().slice(0, 16) : ''}
            onChange={(e) => {
              const date = new Date(e.target.value);
              if (isNaN(date.getTime())) setCustomStartTime(undefined);
              else setCustomStartTime(date.getTime());
            }}
            className="rounded-md border border-input bg-background px-2 py-1 text-sm"
          />
          <Label htmlFor="endTime">To:</Label>
          <input
            type="datetime-local"
            id="endTime"
            value={customEndTime ? new Date(customEndTime).toISOString().slice(0, 16) : ''}
            onChange={(e) => {
              const date = new Date(e.target.value);
              if (isNaN(date.getTime())) setCustomEndTime(undefined);
              else setCustomEndTime(date.getTime());
            }}
            className="rounded-md border border-input bg-background px-2 py-1 text-sm"
          />
        </div>
        <div className="text-xs text-muted-foreground mt-2">Choose a preset above or enter a custom From/To range, then run the search.</div>
      </div>
    </>
  );
}
