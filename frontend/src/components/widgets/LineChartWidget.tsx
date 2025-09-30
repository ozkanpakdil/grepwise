interface TimeSlot { time: number; count: number }
interface Data { timeSlots: TimeSlot[] }

type Props = { data: Data; widget: unknown };

export default function LineChartWidget({ data }: Props) {
  const isSmall = typeof window !== 'undefined' && window.innerWidth < 640;
  const textClass = isSmall ? 'text-[10px]' : 'text-xs';

  return (
    <div className="w-full">
      <div className="h-32 w-full bg-gray-100" aria-label="line-chart" />
      <div className={`mt-2 text-muted-foreground ${textClass}`}>{data.timeSlots.length} data points</div>
    </div>
  );
}
