import * as React from 'react';
import { BarChart } from '@mui/x-charts/BarChart';
import { HistogramData } from '@/api/logSearch';

interface Props {
  data: HistogramData[];
}

// Convert ISO timestamp to a readable label (UTC)
const formatLabel = (iso: string) => {
  const d = new Date(iso);
  return d.toUTCString().split(' ')[4]; // HH:MM:SS
};

export const MUIBarsChart: React.FC<Props> = ({ data }) => {
  // Prepare x axis categories and y values
  const categories = data.map(d => new Date(d.timestamp).toISOString());
  const values = data.map(d => d.count);

  const max = Math.max(1, ...values);

  return (
    <BarChart
      height={240}
      series={[{ data: values, label: 'Logs', color: '#3b82f6' }]}
      xAxis={[{ scaleType: 'band', data: categories, valueFormatter: v => formatLabel(String(v)) }]}
      yAxis={[{ min: 0, max }]}
      slotProps={{ legend: { hidden: true } }}
      margin={{ top: 10, right: 10, bottom: 40, left: 50 }}
    />
  );
};

export default MUIBarsChart;
