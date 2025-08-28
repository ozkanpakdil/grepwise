import React from 'react';
import { Label } from '@/components/ui/label';

export type FilterValues = {
  level: string;
  source: string;
  message: string;
};

interface Props {
  visible: boolean;
  values: FilterValues;
  onChange: (field: keyof FilterValues, value: string) => void;
}

export default function SearchFilters({ visible, values, onChange }: Props) {
  if (!visible) return null;
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-muted/20 rounded-md border" data-testid="filters-panel">
      <div>
        <Label htmlFor="levelFilter" className="text-sm">Filter by Level</Label>
        <input
          id="levelFilter"
          type="text"
          value={values.level}
          onChange={(e) => onChange('level', e.target.value)}
          placeholder="Filter by level..."
          className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
          data-testid="filter-level"
        />
      </div>
      <div>
        <Label htmlFor="messageFilter" className="text-sm">Filter by Message</Label>
        <input
          id="messageFilter"
          type="text"
          value={values.message}
          onChange={(e) => onChange('message', e.target.value)}
          placeholder="Filter by message..."
          className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
          data-testid="filter-message"
        />
      </div>
      <div>
        <Label htmlFor="sourceFilter" className="text-sm">Filter by Source</Label>
        <input
          id="sourceFilter"
          type="text"
          value={values.source}
          onChange={(e) => onChange('source', e.target.value)}
          placeholder="Filter by source..."
          className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
          data-testid="filter-source"
        />
      </div>
    </div>
  );
}
