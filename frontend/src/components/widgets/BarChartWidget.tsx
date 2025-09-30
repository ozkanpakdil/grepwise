interface TimeSlot { time: number; count: number }
interface Data { results?: any[]; timeSlots: TimeSlot[]; total?: number }

type Props = { data: Data; widget: unknown };

export default function BarChartWidget({ data }: Props) {
  const isSmall = typeof window !== 'undefined' && window.innerWidth < 640;
  const gapClass = isSmall ? 'gap-0.5' : 'gap-1';
  const labelClass = isSmall ? 'text-[10px]' : 'text-xs';

  // Basic relative bar heights based on max count
  const max = Math.max(1, ...data.timeSlots.map(t => t.count));

  return (
    <div className="w-full h-40">
      <div className={`flex items-end h-full ${gapClass}`}>
        {data.timeSlots.map((slot, idx) => (
          <div key={idx} className="flex flex-col items-center">
            <div
              className="bg-blue-500 w-3"
              style={{ height: `${(slot.count / max) * 100}%` }}
              aria-label={`bar-${idx}`}
            />
            <div className={`text-muted-foreground mt-1 ${labelClass}`}>{new Date(slot.time).getHours()}:00</div>
          </div>
        ))}
      </div>
    </div>
  );
}
