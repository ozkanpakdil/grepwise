import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Edit,
  Plus,
  Trash2,
  Share2,
  Copy,
  Check,
  Download,
  FileText,
  Image,
  Database,
  Menu,
  X as XIcon,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import { dashboardApi, Dashboard } from '@/api/dashboard';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
// Using MUI Grid instead of react-grid-layout
import Grid from '@mui/material/Grid';
import { useSwipeable } from 'react-swipeable';

const DashboardView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const [copied, setCopied] = useState(false);
  const [exporting, setExporting] = useState(false);
  // Using simple MUI Grid layout (no drag/drop), so no layouts state
  const [mobileActionsOpen, setMobileActionsOpen] = useState(false);

  // Reference to the dashboard content for export
  const dashboardRef = useRef<HTMLDivElement>(null);

  // Swipe handlers for mobile menu
  const mobileMenuSwipeHandlers = useSwipeable({
    onSwipedLeft: () => {
      if (mobileActionsOpen) {
        setMobileActionsOpen(false);
      }
    },
    onSwipedRight: () => {
      if (!mobileActionsOpen) {
        setMobileActionsOpen(true);
      }
    },
    trackMouse: false,
    swipeDuration: 500,
    preventScrollOnSwipe: true,
  });

  // Swipe handlers for share modal
  const shareModalSwipeHandlers = useSwipeable({
    onSwipedDown: () => {
      setShowShareModal(false);
    },
    trackMouse: false,
    swipeDuration: 500,
    preventScrollOnSwipe: true,
  });

  // Swipe handlers for export modal
  const exportModalSwipeHandlers = useSwipeable({
    onSwipedDown: () => {
      if (!exporting) {
        setShowExportModal(false);
      }
    },
    trackMouse: false,
    swipeDuration: 500,
    preventScrollOnSwipe: true,
  });

  useEffect(() => {
    if (id) {
      loadDashboard();
    }
  }, [id]);

  // Using MUI Grid: no responsive layout calculation needed
  useEffect(() => {
    // placeholder effect to keep dependency parity if needed
  }, [dashboard]);

  const loadDashboard = async () => {
    if (!id) return;

    try {
      setLoading(true);
      const data = await dashboardApi.getDashboard(id, 'current-user');
      setDashboard(data);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to load dashboard',
        variant: 'destructive',
      });
      navigate('/dashboards');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteWidget = async (widgetId: string) => {
    if (!dashboard || !confirm('Are you sure you want to delete this widget?')) {
      return;
    }

    try {
      await dashboardApi.deleteWidget(dashboard.id, widgetId, 'current-user');
      toast({
        title: 'Success',
        description: 'Widget deleted successfully',
      });
      loadDashboard();
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to delete widget',
        variant: 'destructive',
      });
    }
  };

  const handleShareDashboard = async () => {
    if (!dashboard) return;

    try {
      const updatedDashboard = await dashboardApi.shareDashboard(dashboard.id, !dashboard.isShared, 'current-user');

      setDashboard(updatedDashboard);

      toast({
        title: 'Success',
        description: updatedDashboard.isShared ? 'Dashboard is now shared' : 'Dashboard is no longer shared',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to update sharing settings',
        variant: 'destructive',
      });
    }
  };

  const copyShareLink = () => {
    if (!dashboard) return;

    const shareUrl = `${window.location.origin}/dashboards/${dashboard.id}`;
    navigator.clipboard
      .writeText(shareUrl)
      .then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);

        toast({
          title: 'Success',
          description: 'Share link copied to clipboard',
        });
      })
      .catch(() => {
        toast({
          title: 'Error',
          description: 'Failed to copy link',
          variant: 'destructive',
        });
      });
  };

  // Export dashboard as PDF
  const exportAsPDF = async () => {
    if (!dashboardRef.current || !dashboard) return;

    try {
      setExporting(true);

      // Temporarily remove any edit controls for clean export
      const currentEditMode = editMode;
      setEditMode(false);

      // Wait for re-render
      await new Promise((resolve) => setTimeout(resolve, 100));

      const canvas = await html2canvas(dashboardRef.current, {
        scale: 2, // Higher quality
        logging: false,
        useCORS: true,
        allowTaint: true,
      });

      const imgData = canvas.toDataURL('image/jpeg', 0.95);
      const pdf = new jsPDF({
        orientation: 'landscape',
        unit: 'mm',
      });

      // Add dashboard title
      pdf.setFontSize(16);
      pdf.text(dashboard.name, 14, 15);

      // Add timestamp
      pdf.setFontSize(10);
      pdf.text(`Exported on ${new Date().toLocaleString()}`, 14, 22);

      // Calculate dimensions to fit the page
      const imgWidth = 280;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      // Add the image
      pdf.addImage(imgData, 'JPEG', 14, 30, imgWidth, imgHeight);

      // Save the PDF
      pdf.save(`dashboard-${dashboard.name.toLowerCase().replace(/\s+/g, '-')}.pdf`);

      // Restore edit mode
      setEditMode(currentEditMode);

      toast({
        title: 'Success',
        description: 'Dashboard exported as PDF',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to export dashboard as PDF',
        variant: 'destructive',
      });
      console.error('PDF export error:', error);
    } finally {
      setExporting(false);
      setShowExportModal(false);
    }
  };

  // Export dashboard as image
  const exportAsImage = async () => {
    if (!dashboardRef.current || !dashboard) return;

    try {
      setExporting(true);

      // Temporarily remove any edit controls for clean export
      const currentEditMode = editMode;
      setEditMode(false);

      // Wait for re-render
      await new Promise((resolve) => setTimeout(resolve, 100));

      const canvas = await html2canvas(dashboardRef.current, {
        scale: 2, // Higher quality
        logging: false,
        useCORS: true,
        allowTaint: true,
      });

      // Create download link
      const link = document.createElement('a');
      link.download = `dashboard-${dashboard.name.toLowerCase().replace(/\s+/g, '-')}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();

      // Restore edit mode
      setEditMode(currentEditMode);

      toast({
        title: 'Success',
        description: 'Dashboard exported as image',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to export dashboard as image',
        variant: 'destructive',
      });
      console.error('Image export error:', error);
    } finally {
      setExporting(false);
      setShowExportModal(false);
    }
  };

  // Export dashboard data as JSON
  const exportAsData = () => {
    if (!dashboard) return;

    try {
      setExporting(true);

      // Create a JSON representation of the dashboard
      const dashboardData = {
        ...dashboard,
        exportedAt: new Date().toISOString(),
        exportedBy: 'current-user',
      };

      // Convert to JSON string
      const jsonString = JSON.stringify(dashboardData, null, 2);

      // Create download link
      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.download = `dashboard-${dashboard.name.toLowerCase().replace(/\s+/g, '-')}.json`;
      link.href = url;
      link.click();

      // Clean up
      URL.revokeObjectURL(url);

      toast({
        title: 'Success',
        description: 'Dashboard data exported as JSON',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to export dashboard data',
        variant: 'destructive',
      });
      console.error('Data export error:', error);
    } finally {
      setExporting(false);
      setShowExportModal(false);
    }
  };

  // Toggle mobile actions menu
  const toggleMobileActions = () => {
    setMobileActionsOpen(!mobileActionsOpen);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">Loading dashboard...</div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">Dashboard not found</div>
      </div>
    );
  }

  return (
    <div className="p-6" {...mobileMenuSwipeHandlers}>
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-6 space-y-4 md:space-y-0">
        <div className="flex items-center space-x-4">
          <Button variant="ghost" size="sm" onClick={() => navigate('/dashboards')} className="p-1 md:p-2">
            <ArrowLeft className="h-4 w-4 md:mr-2" />
            <span className="hidden md:inline">Back to Dashboards</span>
          </Button>
          <div>
            <h1 className="text-xl md:text-2xl font-bold">{dashboard.name}</h1>
            {dashboard.description && <p className="text-sm text-muted-foreground">{dashboard.description}</p>}
          </div>
        </div>

        {/* Desktop actions */}
        <div className="hidden md:flex items-center space-x-2">
          <Button variant="outline" onClick={() => setEditMode(!editMode)}>
            <Edit className="h-4 w-4 mr-2" />
            {editMode ? 'Exit Edit' : 'Edit'}
          </Button>
          <Button
            variant="outline"
            onClick={() => setShowShareModal(true)}
            className={dashboard.isShared ? 'bg-green-50' : ''}
          >
            <Share2 className="h-4 w-4 mr-2" />
            {dashboard.isShared ? 'Shared' : 'Share'}
          </Button>
          <Button variant="outline" onClick={() => setShowExportModal(true)}>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
          <Button onClick={() => navigate(`/dashboards/${dashboard.id}/add-widget`)}>
            <Plus className="h-4 w-4 mr-2" />
            Add Widget
          </Button>
        </div>

        {/* Mobile actions */}
        <div className="flex md:hidden items-center justify-between">
          <div className="flex space-x-2">
            <Button variant="outline" size="sm" onClick={() => setEditMode(!editMode)} className="p-2">
              <Edit className="h-5 w-5" />
            </Button>
          </div>

          <Button variant="outline" size="sm" onClick={toggleMobileActions} aria-label="More actions" className="p-2">
            <Menu className="h-5 w-5" />
          </Button>

          {/* Mobile actions dropdown with swipe to dismiss */}
          {mobileActionsOpen && (
            <div className="absolute right-6 mt-10 z-10 bg-white dark:bg-gray-800 shadow-lg rounded-md border p-2 w-48">
              <div className="flex justify-between items-center mb-1 pb-1 border-b">
                <span className="text-xs text-muted-foreground">Swipe left to close</span>
                <button
                  className="text-muted-foreground hover:text-foreground"
                  onClick={() => setMobileActionsOpen(false)}
                >
                  <XIcon className="h-4 w-4" />
                </button>
              </div>
              <div className="space-y-1 pt-1">
                <button
                  className="flex w-full items-center px-3 py-3 text-sm rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                  onClick={() => {
                    setShowShareModal(true);
                    setMobileActionsOpen(false);
                  }}
                >
                  <Share2 className="h-5 w-5 mr-3" />
                  {dashboard.isShared ? 'Shared' : 'Share'}
                </button>
                <button
                  className="flex w-full items-center px-3 py-3 text-sm rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                  onClick={() => {
                    setShowExportModal(true);
                    setMobileActionsOpen(false);
                  }}
                >
                  <Download className="h-5 w-5 mr-3" />
                  Export
                </button>
                <button
                  className="flex w-full items-center px-3 py-3 text-sm rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                  onClick={() => {
                    navigate(`/dashboards/${dashboard.id}/add-widget`);
                    setMobileActionsOpen(false);
                  }}
                >
                  <Plus className="h-5 w-5 mr-3" />
                  Add Widget
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Dashboard Grid */}
      {dashboard.widgets && dashboard.widgets.length > 0 ? (
        <div ref={dashboardRef} className="w-full h-full">
          <div data-testid="grid-layout">
            <Grid container spacing={2}>
              {dashboard.widgets.map((widget) => {
                const lg = Math.min(12, widget.width || 4);
                const md = Math.min(12, Math.max(6, widget.width || 4));
                return (
                  <Grid key={widget.id} item xs={12} sm={12} md={md} lg={lg}>
                    <div
                      className={`relative border border-gray-200 rounded-lg bg-white dark:bg-gray-800 overflow-hidden shadow-sm ${
                        editMode ? 'border-2 border-dashed border-blue-500' : ''
                      }`}
                    >
                      {editMode && (
                        <div className="absolute top-2 right-2 z-10 bg-white dark:bg-gray-800 bg-opacity-90 rounded p-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeleteWidget(widget.id)}
                            className="text-red-600 hover:text-red-700 p-1"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      )}
                      <div className="h-full overflow-auto p-1">
                        <WidgetRenderer widget={widget} />
                      </div>
                    </div>
                  </Grid>
                );
              })}
            </Grid>
          </div>
        </div>
      ) : (
        <div ref={dashboardRef} className="text-center py-12">
          <div className="text-muted-foreground mb-4">No widgets in this dashboard</div>
          <Button onClick={() => navigate(`/dashboards/${dashboard.id}/add-widget`)}>
            <Plus className="h-4 w-4 mr-2" />
            Add your first widget
          </Button>
        </div>
      )}

      {/* Share Modal */}
      {showShareModal && dashboard && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div
            {...shareModalSwipeHandlers}
            className="bg-white dark:bg-gray-800 rounded-lg p-4 md:p-6 w-full max-w-md max-h-[90vh] overflow-auto"
          >
            {/* Swipe indicator */}
            <div className="flex justify-center mb-2">
              <div className="w-10 h-1 bg-gray-300 dark:bg-gray-600 rounded-full"></div>
            </div>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg md:text-xl font-bold">Share Dashboard</h2>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0" onClick={() => setShowShareModal(false)}>
                <XIcon className="h-5 w-5" />
                <span className="sr-only">Close</span>
              </Button>
            </div>
            <div className="text-xs text-muted-foreground text-center mb-4">Swipe down to close</div>

            <div className="mb-6">
              <p className="text-sm text-muted-foreground mb-3">
                {dashboard.isShared
                  ? 'This dashboard is currently shared. Anyone with the link can view it.'
                  : 'This dashboard is currently private. Enable sharing to generate a link.'}
              </p>

              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <span className="font-medium">Sharing Status</span>
                <Button
                  variant={dashboard.isShared ? 'destructive' : 'default'}
                  size="sm"
                  onClick={handleShareDashboard}
                  className="w-full sm:w-auto"
                >
                  {dashboard.isShared ? 'Disable Sharing' : 'Enable Sharing'}
                </Button>
              </div>
            </div>

            {dashboard.isShared && (
              <div className="mb-6">
                <label className="block text-sm font-medium mb-2">Share Link</label>
                <div className="flex flex-col sm:flex-row gap-2">
                  <input
                    type="text"
                    readOnly
                    value={`${window.location.origin}/dashboards/${dashboard.id}`}
                    className="w-full border rounded-md sm:rounded-l-md sm:rounded-r-none px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700"
                  />
                  <Button className="w-full sm:w-auto sm:rounded-l-none" onClick={copyShareLink}>
                    {copied ? <Check className="h-4 w-4 mr-2" /> : <Copy className="h-4 w-4 mr-2" />}
                    {copied ? 'Copied' : 'Copy'}
                  </Button>
                </div>
              </div>
            )}

            <div className="flex justify-end mt-4">
              <Button variant="outline" onClick={() => setShowShareModal(false)} className="w-full sm:w-auto">
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Export Modal */}
      {showExportModal && dashboard && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div
            {...exportModalSwipeHandlers}
            className="bg-white dark:bg-gray-800 rounded-lg p-4 md:p-6 w-full max-w-md max-h-[90vh] overflow-auto"
          >
            {/* Swipe indicator */}
            <div className="flex justify-center mb-2">
              <div className="w-10 h-1 bg-gray-300 dark:bg-gray-600 rounded-full"></div>
            </div>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg md:text-xl font-bold">Export Dashboard</h2>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0"
                onClick={() => setShowExportModal(false)}
                disabled={exporting}
              >
                <XIcon className="h-5 w-5" />
                <span className="sr-only">Close</span>
              </Button>
            </div>

            {!exporting && <div className="text-xs text-muted-foreground text-center mb-4">Swipe down to close</div>}

            <p className="text-sm text-muted-foreground mb-4">Choose a format to export your dashboard</p>

            <div className="space-y-3 mb-6">
              <button
                onClick={exportAsPDF}
                disabled={exporting}
                className="w-full flex items-center p-2 md:p-3 border rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                <div className="bg-red-100 dark:bg-red-900/30 p-2 rounded-md mr-3">
                  <FileText className="h-5 w-5 text-red-600 dark:text-red-400" />
                </div>
                <div className="text-left">
                  <div className="font-medium">PDF Document</div>
                  <div className="text-xs text-muted-foreground">Export as a printable PDF document</div>
                </div>
              </button>

              <button
                onClick={exportAsImage}
                disabled={exporting}
                className="w-full flex items-center p-2 md:p-3 border rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                <div className="bg-blue-100 dark:bg-blue-900/30 p-2 rounded-md mr-3">
                  <Image className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div className="text-left">
                  <div className="font-medium">PNG Image</div>
                  <div className="text-xs text-muted-foreground">Export as a high-resolution image</div>
                </div>
              </button>

              <button
                onClick={exportAsData}
                disabled={exporting}
                className="w-full flex items-center p-2 md:p-3 border rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                <div className="bg-green-100 dark:bg-green-900/30 p-2 rounded-md mr-3">
                  <Database className="h-5 w-5 text-green-600 dark:text-green-400" />
                </div>
                <div className="text-left">
                  <div className="font-medium">JSON Data</div>
                  <div className="text-xs text-muted-foreground">Export dashboard configuration and data</div>
                </div>
              </button>
            </div>

            {exporting && (
              <div className="text-center py-2 mb-4">
                <div className="inline-block h-4 w-4 border-2 border-t-blue-500 rounded-full animate-spin"></div>
                <span className="ml-2 text-sm">Exporting...</span>
              </div>
            )}

            <div className="flex justify-end mt-4">
              <Button
                variant="outline"
                onClick={() => setShowExportModal(false)}
                disabled={exporting}
                className="w-full sm:w-auto"
              >
                Cancel
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardView;
