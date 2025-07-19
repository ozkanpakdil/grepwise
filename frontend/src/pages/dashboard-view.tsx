import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Edit, Plus, Trash2, Share2, Copy, Check, Download, FileText, Image, Database, Save, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import { dashboardApi, Dashboard, DashboardWidget } from '@/api/dashboard';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { Responsive, WidthProvider } from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';

const ResponsiveGridLayout = WidthProvider(Responsive);

// Widget components
import WidgetRenderer from '@/components/WidgetRenderer';

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
  const [layouts, setLayouts] = useState<{ [key: string]: Array<any> }>({});
  const [layoutChanged, setLayoutChanged] = useState(false);
  const [mobileActionsOpen, setMobileActionsOpen] = useState(false);
  
  // Reference to the dashboard content for export
  const dashboardRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (id) {
      loadDashboard();
    }
  }, [id]);
  
  // Initialize responsive layouts from dashboard widgets
  useEffect(() => {
    if (dashboard && dashboard.widgets) {
      // Create base layout
      const baseLayout = dashboard.widgets.map(widget => ({
        i: widget.id,
        x: widget.positionX || 0,
        y: widget.positionY || 0,
        w: widget.width || 4,
        h: widget.height || 3,
        minW: 2,
        minH: 2
      }));
      
      // Create responsive layouts for different breakpoints
      const responsiveLayouts = {
        lg: baseLayout,
        md: baseLayout.map(item => ({
          ...item,
          w: Math.min(item.w, 6), // Limit width on medium screens
          x: 0 // Stack widgets vertically on medium screens
        })),
        sm: baseLayout.map(item => ({
          ...item,
          w: 6, // Full width on small screens
          x: 0 // Stack widgets vertically on small screens
        })),
        xs: baseLayout.map(item => ({
          ...item,
          w: 4, // Full width on extra small screens
          x: 0 // Stack widgets vertically on extra small screens
        }))
      };
      
      setLayouts(responsiveLayouts);
    }
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
      const updatedDashboard = await dashboardApi.shareDashboard(
        dashboard.id, 
        !dashboard.isShared, 
        'current-user'
      );
      
      setDashboard(updatedDashboard);
      
      toast({
        title: 'Success',
        description: updatedDashboard.isShared 
          ? 'Dashboard is now shared' 
          : 'Dashboard is no longer shared',
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
    navigator.clipboard.writeText(shareUrl)
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
      await new Promise(resolve => setTimeout(resolve, 100));
      
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
      await new Promise(resolve => setTimeout(resolve, 100));
      
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
  
  // Handle responsive layout changes
  const handleLayoutChange = (currentLayout: any, allLayouts: any) => {
    setLayouts(allLayouts);
    setLayoutChanged(true);
  };
  
  // Toggle mobile actions menu
  const toggleMobileActions = () => {
    setMobileActionsOpen(!mobileActionsOpen);
  };
  
  // Save layout changes to backend
  const saveLayout = async () => {
    if (!dashboard || !layouts.lg || !layouts.lg.length) return;
    
    try {
      // Use the large screen layout for saving positions
      const widgetPositions: Record<string, Record<string, number>> = {};
      
      layouts.lg.forEach(item => {
        widgetPositions[item.i] = {
          positionX: item.x,
          positionY: item.y,
          width: item.w,
          height: item.h
        };
      });
      
      await dashboardApi.updateWidgetPositions(dashboard.id, widgetPositions, 'current-user');
      
      toast({
        title: 'Success',
        description: 'Dashboard layout saved successfully',
      });
      
      setLayoutChanged(false);
      loadDashboard(); // Reload to get updated positions
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to save dashboard layout',
        variant: 'destructive',
      });
      console.error('Layout save error:', error);
    }
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
    <div className="p-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-6 space-y-4 md:space-y-0">
        <div className="flex items-center space-x-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/dashboards')}
            className="p-1 md:p-2"
          >
            <ArrowLeft className="h-4 w-4 md:mr-2" />
            <span className="hidden md:inline">Back to Dashboards</span>
          </Button>
          <div>
            <h1 className="text-xl md:text-2xl font-bold">{dashboard.name}</h1>
            {dashboard.description && (
              <p className="text-sm text-muted-foreground">{dashboard.description}</p>
            )}
          </div>
        </div>
        
        {/* Desktop actions */}
        <div className="hidden md:flex items-center space-x-2">
          <Button
            variant="outline"
            onClick={() => setEditMode(!editMode)}
          >
            <Edit className="h-4 w-4 mr-2" />
            {editMode ? 'Exit Edit' : 'Edit'}
          </Button>
          <Button
            variant="outline"
            onClick={() => setShowShareModal(true)}
            className={dashboard.isShared ? "bg-green-50" : ""}
          >
            <Share2 className="h-4 w-4 mr-2" />
            {dashboard.isShared ? 'Shared' : 'Share'}
          </Button>
          {layoutChanged && (
            <Button
              variant="default"
              onClick={saveLayout}
              className="bg-green-600 hover:bg-green-700"
            >
              <Save className="h-4 w-4 mr-2" />
              Save Layout
            </Button>
          )}
          <Button
            variant="outline"
            onClick={() => setShowExportModal(true)}
          >
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
          <Button
            onClick={() => navigate(`/dashboards/${dashboard.id}/add-widget`)}
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Widget
          </Button>
        </div>
        
        {/* Mobile actions */}
        <div className="flex md:hidden items-center justify-between">
          <div className="flex space-x-2">
            {layoutChanged && (
              <Button
                variant="default"
                onClick={saveLayout}
                size="sm"
                className="bg-green-600 hover:bg-green-700"
              >
                <Save className="h-4 w-4" />
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={() => setEditMode(!editMode)}
            >
              <Edit className="h-4 w-4" />
            </Button>
          </div>
          
          <Button
            variant="outline"
            size="sm"
            onClick={toggleMobileActions}
            aria-label="More actions"
          >
            <Menu className="h-4 w-4" />
          </Button>
          
          {/* Mobile actions dropdown */}
          {mobileActionsOpen && (
            <div className="absolute right-6 mt-10 z-10 bg-white dark:bg-gray-800 shadow-lg rounded-md border p-2 w-48">
              <div className="space-y-1">
                <button
                  className="flex w-full items-center px-3 py-2 text-sm rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                  onClick={() => {
                    setShowShareModal(true);
                    setMobileActionsOpen(false);
                  }}
                >
                  <Share2 className="h-4 w-4 mr-2" />
                  {dashboard.isShared ? 'Shared' : 'Share'}
                </button>
                <button
                  className="flex w-full items-center px-3 py-2 text-sm rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                  onClick={() => {
                    setShowExportModal(true);
                    setMobileActionsOpen(false);
                  }}
                >
                  <Download className="h-4 w-4 mr-2" />
                  Export
                </button>
                <button
                  className="flex w-full items-center px-3 py-2 text-sm rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                  onClick={() => {
                    navigate(`/dashboards/${dashboard.id}/add-widget`);
                    setMobileActionsOpen(false);
                  }}
                >
                  <Plus className="h-4 w-4 mr-2" />
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
          <ResponsiveGridLayout
            className="layout"
            layouts={layouts}
            breakpoints={{ lg: 1200, md: 996, sm: 768, xs: 480 }}
            cols={{ lg: 12, md: 8, sm: 6, xs: 4 }}
            rowHeight={100}
            margin={[16, 16]}
            containerPadding={[8, 8]}
            onLayoutChange={handleLayoutChange}
            isDraggable={editMode}
            isResizable={editMode}
            compactType="vertical"
            useCSSTransforms={true}
          >
            {dashboard.widgets.map((widget) => (
              <div
                key={widget.id}
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
            ))}
          </ResponsiveGridLayout>
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
          <div className="bg-white dark:bg-gray-800 rounded-lg p-4 md:p-6 w-full max-w-md max-h-[90vh] overflow-auto">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg md:text-xl font-bold">Share Dashboard</h2>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0"
                onClick={() => setShowShareModal(false)}
              >
                <XIcon className="h-4 w-4" />
                <span className="sr-only">Close</span>
              </Button>
            </div>
            
            <div className="mb-6">
              <p className="text-sm text-muted-foreground mb-3">
                {dashboard.isShared 
                  ? 'This dashboard is currently shared. Anyone with the link can view it.' 
                  : 'This dashboard is currently private. Enable sharing to generate a link.'}
              </p>
              
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <span className="font-medium">Sharing Status</span>
                <Button 
                  variant={dashboard.isShared ? "destructive" : "default"}
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
                  <Button
                    className="w-full sm:w-auto sm:rounded-l-none"
                    onClick={copyShareLink}
                  >
                    {copied ? <Check className="h-4 w-4 mr-2" /> : <Copy className="h-4 w-4 mr-2" />}
                    {copied ? 'Copied' : 'Copy'}
                  </Button>
                </div>
              </div>
            )}
            
            <div className="flex justify-end mt-4">
              <Button
                variant="outline"
                onClick={() => setShowShareModal(false)}
                className="w-full sm:w-auto"
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
      
      {/* Export Modal */}
      {showExportModal && dashboard && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-4 md:p-6 w-full max-w-md max-h-[90vh] overflow-auto">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg md:text-xl font-bold">Export Dashboard</h2>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0"
                onClick={() => setShowExportModal(false)}
                disabled={exporting}
              >
                <XIcon className="h-4 w-4" />
                <span className="sr-only">Close</span>
              </Button>
            </div>
            
            <p className="text-sm text-muted-foreground mb-4">
              Choose a format to export your dashboard
            </p>
            
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