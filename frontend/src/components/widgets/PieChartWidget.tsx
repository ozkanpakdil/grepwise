import React from 'react';

interface Widget { id: string; title: string }
interface DataRow { [k: string]: number }
interface Data { results: DataRow[]; timeSlots?: any[]; total?: number }

export default function PieChartWidget({ data, widget }: { data: Data; widget: Widget }) {
  const isSmall = typeof window !== 'undefined' && window.innerWidth < 640;

  const entries = Object.entries(data.results?.[0] || {});

  return (
    <div className="w-full h-40 flex-1 flex items-center justify-center">
      <div className={`${isSmall ? 'flex flex-col space-y-4' : 'flex space-x-4'}`}>
        <div>
          <svg width="200" height="200" role="img" aria-label="pie-chart" />
        </div>
        <div className={`${isSmall ? 'grid grid-cols-2' : ''}`}>
          {entries.map(([k, v]) => (
            <div key={k} className="flex items-center space-x-2">
              <span className="inline-block w-3 h-3 bg-gray-400 rounded" />
              <span className="text-xs text-muted-foreground">{k}: {v}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
