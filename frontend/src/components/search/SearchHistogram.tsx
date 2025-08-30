import MUIBarsChart from '@/components/MUIBarsChart';
import { HistogramData, SearchParams, TimeSlot } from '@/api/logSearch';

interface Props {
  histogramData: HistogramData[];
  timeSlots: TimeSlot[];
  timeRange: SearchParams['timeRange'];
  isStreaming: boolean;
  isLoading: boolean;
  onZoom: (start: number, end: number) => void;
  onTimeSlotClick: (slotTime: number) => void;
}

export default function SearchHistogram({ histogramData, isLoading, onZoom }: Props) {
  return (
    <div data-testid="histogram-section">
      <div className="text-sm font-medium">Time distribution</div>
      {histogramData && histogramData.length > 0 ? (
        <div className="w-full h-64">
          <MUIBarsChart data={histogramData} onBarDoubleClick={onZoom} />
        </div>
      ) : isLoading ? (
        <div className="mt-6 mb-8 text-center py-4 border border-input rounded-md bg-background">
          <p className="text-muted-foreground">Loading time distribution...</p>
        </div>
      ) : null}
    </div>
  );
}
