import { Label } from '@/components/ui/label';
import { SearchParams } from '@/api/logSearch';

interface Props {
  timeRange: SearchParams['timeRange'];
  setTimeRange: (tr: SearchParams['timeRange']) => void;
  customStartTime?: number;
  customEndTime?: number;
  setCustomStartTime: (ms: number | undefined) => void;
  setCustomEndTime: (ms: number | undefined) => void;
}

export default function TimeRangePicker({
  timeRange,
  setTimeRange,
  customStartTime,
  customEndTime,
  setCustomStartTime,
  setCustomEndTime,
}: Props) {
  // reference setTimeRange to satisfy noUnusedLocals rule without changing behavior
  void setTimeRange;
  return (
    <>
      {timeRange === 'custom' && (
        <div className="flex items-center space-x-2">
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
      )}
    </>
  );
}
