import React from 'react';
import { DashboardWidget } from '@/api/dashboard';

interface Props {
  widget: DashboardWidget;
}

// Minimal placeholder renderer to satisfy build. Replace with real implementations per widget.type.
const WidgetRenderer: React.FC<Props> = ({ widget }) => {
  return (
    <div className="p-2">
      <div className="text-sm font-medium">{widget.title || 'Widget'}</div>
      <pre className="text-xs text-muted-foreground overflow-auto">
        {JSON.stringify({ type: widget.type, query: widget.query, configuration: widget.configuration }, null, 2)}
      </pre>
    </div>
  );
};

export default WidgetRenderer;
