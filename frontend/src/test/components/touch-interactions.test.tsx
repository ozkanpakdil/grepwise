import {beforeEach, describe, expect, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {act} from 'react-dom/test-utils';
import WidgetRenderer from '../../components/WidgetRenderer';
import {DashboardWidget} from '@/api/dashboard';
import {logSearchApi} from '@/api/logSearch';

// Mock the logSearchApi
vi.mock('@/api/logSearch', () => ({
    logSearchApi: {
        search: vi.fn().mockResolvedValue({
            results: [],
            totalHits: 0,
            timeTaken: 0,
        }),
    },
}));

// Mock the SSE client
vi.mock('@/utils/sseClient', () => ({
    getWidgetUpdateClient: vi.fn().mockReturnValue({
        on: vi.fn(),
        connect: vi.fn(),
        close: vi.fn(),
    }),
}));

describe('Touch Interactions', () => {
    // Sample widget for testing
    const mockWidget: DashboardWidget = {
        id: 'test-widget-1',
        dashboardId: 'test-dashboard',
        title: 'Test Widget',
        type: 'chart',
        query: 'test query',
        positionX: 0,
        positionY: 0,
        width: 4,
        height: 3,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    test('WidgetRenderer shows swipe hint', () => {
        render(<WidgetRenderer widget={mockWidget}/>);

        // Check if the swipe hint is displayed
        expect(screen.getByText('Swipe down to refresh')).toBeInTheDocument();
    });

    test('Refresh button has enhanced touch target', () => {
        render(<WidgetRenderer widget={mockWidget}/>);

        // Find the refresh button
        const refreshButton = screen.getByLabelText('Refresh widget');

        // Check if it has padding for larger touch target
        expect(refreshButton).toHaveClass('p-1');

        // Check if it has the refresh icon
        const refreshIcon = refreshButton.querySelector('svg');
        expect(refreshIcon).toBeInTheDocument();
    });

    test('Refresh button triggers data reload when clicked', async () => {
        render(<WidgetRenderer widget={mockWidget}/>);

        // Find and click the refresh button
        const refreshButton = screen.getByLabelText('Refresh widget');
        fireEvent.click(refreshButton);

        // Check if the search API was called
        expect(logSearchApi.search).toHaveBeenCalledWith({
            query: 'test query',
            timeRange: '24h',
            maxResults: 1000,
        });
    });

    // Note: Testing actual swipe gestures is challenging in JSDOM environment
    // as it doesn't fully support touch events. In a real environment,
    // we would use a tool like Cypress for E2E testing of touch interactions.
    test('Simulated swipe down triggers refresh', async () => {
        render(<WidgetRenderer widget={mockWidget}/>);

        // Get the widget container
        const widgetContainer = screen.getByText('Test Widget').closest('.widget-renderer');

        // Simulate a swipe down gesture by triggering touch events
        await act(async () => {
            // Start touch at the top
            fireEvent.touchStart(widgetContainer!, {touches: [{clientY: 100}]});

            // Move touch down
            fireEvent.touchMove(widgetContainer!, {touches: [{clientY: 200}]});

            // End touch
            fireEvent.touchEnd(widgetContainer!);
        });

        // Check if the search API was called (this may not work in JSDOM)
        // This is more of a placeholder for real E2E testing
        expect(logSearchApi.search).toHaveBeenCalled();
    });
});