import React from 'react';
import LogBarChart from '@/components/LogBarChart';
import MUIBarsChart from '@/components/MUIBarsChart';
import { HistogramData, TimeSlot, SearchParams } from '@/api/logSearch';

interface Props {
  histogramData: HistogramData[];
  timeSlots: TimeSlot[];
  timeRange: SearchParams['timeRange'];
  isStreaming: boolean;
  isLoading: boolean;
  onZoom: (start: number, end: number) => void;
  onTimeSlotClick: (slotTime: number) => void;
}

export default function SearchHistogram({ histogramData, timeSlots, timeRange, isStreaming, isLoading, onZoom, onTimeSlotClick }: Props) {
  return (
    <div className="mt-6">
      <div className="flex items-center justify-between mb-2">
        <div className="text-sm font-medium">Time distribution</div>
        <div className="text-xs text-muted-foreground">
          {isStreaming ? 'Computing histogram…' : null}
        </div>
      </div>

      {histogramData && histogramData.length > 0 ? (
        <div className="w-full h-64">
          <MUIBarsChart data={histogramData} onBarDoubleClick={onZoom} />
        </div>
      ) : timeSlots && timeSlots.length > 0 ? (
        <LogBarChart timeSlots={timeSlots} timeRange={timeRange} onTimeSlotClick={onTimeSlotClick} />
      ) : isLoading ? (
        <div className="mt-6 mb-8 text-center py-4 border border-input rounded-md bg-background">
          <p className="text-muted-foreground">Loading time distribution...</p>
        </div>
      ) : null}
    </div>
  );
}
