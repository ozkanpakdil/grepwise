import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Edit, Plus, Trash2, Share2, Copy, Check, Download, FileText, Image, Database, Save } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import { dashboardApi, Dashboard, DashboardWidget } from '@/api/dashboard';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import GridLayout from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';

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
  const [layout, setLayout] = useState<Array<any>>([]);
  const [layoutChanged, setLayoutChanged] = useState(false);
  
  // Reference to the dashboard content for export
  const dashboardRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (id) {
      loadDashboard();
    }
  }, [id]);
  
  // Initialize layout from dashboard widgets
  useEffect(() => {
    if (dashboard && dashboard.widgets) {
      const initialLayout = dashboard.widgets.map(widget => ({
        i: widget.id,
        x: widget.positionX || 0,
        y: widget.positionY || 0,
        w: widget.width || 4,
        h: widget.height || 3,
        minW: 2,
        minH: 2
      }));
      setLayout(initialLayout);
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
  
  // Handle layout changes
  const handleLayoutChange = (newLayout: any[]) => {
    setLayout(newLayout);
    setLayoutChanged(true);
  };
  
  // Save layout changes to backend
  const saveLayout = async () => {
    if (!dashboard || !layout.length) return;
    
    try {
      // Convert layout to widget positions format
      const widgetPositions: Record<string, Record<string, number>> = {};
      
      layout.forEach(item => {
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
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/dashboards')}
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Dashboards
          </Button>
          <div>
            <h1 className="text-2xl font-bold">{dashboard.name}</h1>
            {dashboard.description && (
              <p className="text-muted-foreground">{dashboard.description}</p>
            )}
          </div>
        </div>
        
        <div className="flex items-center space-x-2">
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
      </div>

      {/* Dashboard Grid */}
      {dashboard.widgets && dashboard.widgets.length > 0 ? (
        <div ref={dashboardRef} className="w-full h-full">
          <GridLayout
            className="layout"
            layout={layout}
            cols={12}
            rowHeight={100}
            width={1200}
            margin={[16, 16]}
            containerPadding={[0, 0]}
            onLayoutChange={handleLayoutChange}
            isDraggable={editMode}
            isResizable={editMode}
            compactType="vertical"
          >
            {dashboard.widgets.map((widget) => (
              <div
                key={widget.id}
                className={`relative border border-gray-200 rounded-lg bg-white overflow-hidden ${
                  editMode ? 'border-2 border-dashed border-blue-500' : ''
                }`}
              >
                {editMode && (
                  <div className="absolute top-2 right-2 z-10 bg-white bg-opacity-90 rounded p-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDeleteWidget(widget.id)}
                      className="text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                )}
                
                <WidgetRenderer widget={widget} />
              </div>
            ))}
          </GridLayout>
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
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">Share Dashboard</h2>
            
            <div className="mb-6">
              <p className="text-sm text-muted-foreground mb-2">
                {dashboard.isShared 
                  ? 'This dashboard is currently shared. Anyone with the link can view it.' 
                  : 'This dashboard is currently private. Enable sharing to generate a link.'}
              </p>
              
              <div className="flex items-center justify-between">
                <span className="font-medium">Sharing Status</span>
                <Button 
                  variant={dashboard.isShared ? "destructive" : "default"}
                  size="sm"
                  onClick={handleShareDashboard}
                >
                  {dashboard.isShared ? 'Disable Sharing' : 'Enable Sharing'}
                </Button>
              </div>
            </div>
            
            {dashboard.isShared && (
              <div className="mb-6">
                <label className="block text-sm font-medium mb-2">Share Link</label>
                <div className="flex">
                  <input
                    type="text"
                    readOnly
                    value={`${window.location.origin}/dashboards/${dashboard.id}`}
                    className="flex-1 border rounded-l-md px-3 py-2 text-sm bg-gray-50"
                  />
                  <Button
                    className="rounded-l-none"
                    onClick={copyShareLink}
                  >
                    {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                  </Button>
                </div>
              </div>
            )}
            
            <div className="flex justify-end">
              <Button
                variant="outline"
                onClick={() => setShowShareModal(false)}
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
      
      {/* Export Modal */}
      {showExportModal && dashboard && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">Export Dashboard</h2>
            
            <p className="text-sm text-muted-foreground mb-4">
              Choose a format to export your dashboard
            </p>
            
            <div className="space-y-4 mb-6">
              <button
                onClick={exportAsPDF}
                disabled={exporting}
                className="w-full flex items-center p-3 border rounded-md hover:bg-gray-50 transition-colors"
              >
                <div className="bg-red-100 p-2 rounded-md mr-3">
                  <FileText className="h-5 w-5 text-red-600" />
                </div>
                <div className="text-left">
                  <div className="font-medium">PDF Document</div>
                  <div className="text-xs text-muted-foreground">Export as a printable PDF document</div>
                </div>
              </button>
              
              <button
                onClick={exportAsImage}
                disabled={exporting}
                className="w-full flex items-center p-3 border rounded-md hover:bg-gray-50 transition-colors"
              >
                <div className="bg-blue-100 p-2 rounded-md mr-3">
                  <Image className="h-5 w-5 text-blue-600" />
                </div>
                <div className="text-left">
                  <div className="font-medium">PNG Image</div>
                  <div className="text-xs text-muted-foreground">Export as a high-resolution image</div>
                </div>
              </button>
              
              <button
                onClick={exportAsData}
                disabled={exporting}
                className="w-full flex items-center p-3 border rounded-md hover:bg-gray-50 transition-colors"
              >
                <div className="bg-green-100 p-2 rounded-md mr-3">
                  <Database className="h-5 w-5 text-green-600" />
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
            
            <div className="flex justify-end">
              <Button
                variant="outline"
                onClick={() => setShowExportModal(false)}
                disabled={exporting}
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