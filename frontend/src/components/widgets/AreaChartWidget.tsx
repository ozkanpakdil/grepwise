import React from 'react';

interface TimeSlot { time: number; count: number }
interface Data { timeSlots: TimeSlot[] }
interface Widget { id: string; title: string }

export default function AreaChartWidget({ data, widget }: { data: Data; widget: Widget }) {
  const isSmall = typeof window !== 'undefined' && window.innerWidth < 640;
  const textClass = isSmall ? 'text-[10px]' : 'text-xs';

  return (
    <div className="w-full">
      <div className="h-32 w-full bg-gray-100" aria-label="area-chart" />
      <div className={`mt-2 text-muted-foreground ${textClass}`}>{data.timeSlots.length} data points</div>
    </div>
  );
}
