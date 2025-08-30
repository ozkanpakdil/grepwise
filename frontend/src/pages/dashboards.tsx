import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Share2, Trash2, BarChart3, PieChart, Table, Activity, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import { dashboardApi, Dashboard, DashboardRequest, WidgetRequest } from '@/api/dashboard';
import { formatDate } from '@/lib/utils';
import { notifyError, notifySuccess } from '@/components/ui/use-toast';

const DashboardsPage: React.FC = () => {
  const navigate = useNavigate();
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showWidgetModal, setShowWidgetModal] = useState(false);
  const [selectedDashboard, setSelectedDashboard] = useState<Dashboard | null>(null);
  const [newDashboard, setNewDashboard] = useState<DashboardRequest>({
    name: '',
    description: '',
    createdBy: 'current-user', // In a real app, this would come from auth
  });
  const [newWidget, setNewWidget] = useState<WidgetRequest>({
    title: '',
    type: 'chart',
    query: '',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'current-user',
  });

  const { toast } = useToast();

  useEffect(() => {
    loadDashboards();
  }, []);

  const loadDashboards = async () => {
    try {
      setLoading(true);
      const data = await dashboardApi.getDashboards('current-user');
      setDashboards(data);
    } catch (error) {
      notifyError(error, 'Error', 'Failed to load dashboards');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateDashboard = async () => {
    try {
      if (!newDashboard.name.trim()) {
        notifyError('Dashboard name is required', 'Error');
        return;
      }

      await dashboardApi.createDashboard(newDashboard);
      notifySuccess('Dashboard created successfully');
      
      setShowCreateModal(false);
      setNewDashboard({
        name: '',
        description: '',
        createdBy: 'current-user',
      });
      loadDashboards();
    } catch (error) {
      notifyError(error, 'Error', 'Failed to create dashboard');
    }
  };

  const handleDeleteDashboard = async (dashboard: Dashboard) => {
    if (!confirm(`Are you sure you want to delete "${dashboard.name}"?`)) {
      return;
    }

    try {
      await dashboardApi.deleteDashboard(dashboard.id, 'current-user');
      notifySuccess('Dashboard deleted successfully');
      loadDashboards();
    } catch (error) {
      notifyError(error, 'Error', 'Failed to delete dashboard');
    }
  };

  const handleShareDashboard = async (dashboard: Dashboard) => {
    try {
      await dashboardApi.shareDashboard(dashboard.id, !dashboard.isShared, 'current-user');
      notifySuccess(`Dashboard ${dashboard.isShared ? 'unshared' : 'shared'} successfully`);
      loadDashboards();
    } catch (error) {
      notifyError(error, 'Error', 'Failed to share dashboard');
    }
  };

  const handleAddWidget = async () => {
    if (!selectedDashboard) return;

    try {
      if (!newWidget.title.trim() || !newWidget.query.trim()) {
        toast({
          title: 'Error',
          description: 'Widget title and query are required',
          variant: 'destructive',
        });
        return;
      }

      await dashboardApi.addWidget(selectedDashboard.id, newWidget);
      toast({
        title: 'Success',
        description: 'Widget added successfully',
      });
      
      setShowWidgetModal(false);
      setNewWidget({
        title: '',
        type: 'chart',
        query: '',
        positionX: 0,
        positionY: 0,
        width: 4,
        height: 3,
        userId: 'current-user',
      });
      loadDashboards();
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Failed to add widget',
        variant: 'destructive',
      });
    }
  };

  const getWidgetIcon = (type: string) => {
    switch (type) {
      case 'chart':
        return <BarChart3 className="h-4 w-4" />;
      case 'pie':
        return <PieChart className="h-4 w-4" />;
      case 'table':
        return <Table className="h-4 w-4" />;
      case 'metric':
        return <Activity className="h-4 w-4" />;
      default:
        return <BarChart3 className="h-4 w-4" />;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">Loading dashboards...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">Dashboards</h1>
          <p className="text-muted-foreground">Create and manage your log analysis dashboards</p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>
          <Plus className="h-4 w-4 mr-2" />
          New Dashboard
        </Button>
      </div>

      {dashboards.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-muted-foreground mb-4">No dashboards found</div>
          <Button onClick={() => setShowCreateModal(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Create your first dashboard
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {dashboards.map((dashboard) => (
            <div key={dashboard.id} className="border rounded-lg p-4 hover:shadow-md transition-shadow">
              <div className="flex justify-between items-start mb-3">
                <div>
                  <h3 className="font-semibold text-lg">{dashboard.name}</h3>
                  {dashboard.description && (
                    <p className="text-sm text-muted-foreground">{dashboard.description}</p>
                  )}
                </div>
                <div className="flex space-x-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleShareDashboard(dashboard)}
                    className={dashboard.isShared ? 'text-blue-600' : ''}
                  >
                    <Share2 className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDeleteDashboard(dashboard)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>

              <div className="mb-4">
                <div className="text-sm text-muted-foreground mb-2">
                  {dashboard.widgets?.length || 0} widgets
                </div>
                {dashboard.widgets && dashboard.widgets.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {dashboard.widgets.slice(0, 3).map((widget) => (
                      <div
                        key={widget.id}
                        className="flex items-center space-x-1 bg-muted px-2 py-1 rounded text-xs"
                      >
                        {getWidgetIcon(widget.type)}
                        <span>{widget.title}</span>
                      </div>
                    ))}
                    {dashboard.widgets.length > 3 && (
                      <div className="bg-muted px-2 py-1 rounded text-xs">
                        +{dashboard.widgets.length - 3} more
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="flex justify-between items-center">
                <div className="text-xs text-muted-foreground">
                  {dashboard.isShared ? 'Shared' : 'Private'} • 
                  Created {formatDate(dashboard.createdAt)}
                </div>
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => navigate(`/dashboards/${dashboard.id}`)}
                  >
                    <Eye className="h-4 w-4 mr-1" />
                    View
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setSelectedDashboard(dashboard);
                      setShowWidgetModal(true);
                    }}
                  >
                    <Plus className="h-4 w-4 mr-1" />
                    Add Widget
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Dashboard Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-semibold mb-4">Create New Dashboard</h2>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Name *</label>
                <input
                  type="text"
                  className="w-full border rounded px-3 py-2"
                  value={newDashboard.name}
                  onChange={(e) => setNewDashboard({ ...newDashboard, name: e.target.value })}
                  placeholder="Enter dashboard name"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  className="w-full border rounded px-3 py-2"
                  rows={3}
                  value={newDashboard.description}
                  onChange={(e) => setNewDashboard({ ...newDashboard, description: e.target.value })}
                  placeholder="Enter dashboard description"
                />
              </div>
            </div>

            <div className="flex justify-end space-x-2 mt-6">
              <Button variant="outline" onClick={() => setShowCreateModal(false)}>
                Cancel
              </Button>
              <Button onClick={handleCreateDashboard}>
                Create Dashboard
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Add Widget Modal */}
      {showWidgetModal && selectedDashboard && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-semibold mb-4">Add Widget to {selectedDashboard.name}</h2>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Title *</label>
                <input
                  type="text"
                  className="w-full border rounded px-3 py-2"
                  value={newWidget.title}
                  onChange={(e) => setNewWidget({ ...newWidget, title: e.target.value })}
                  placeholder="Enter widget title"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Type</label>
                <select
                  className="w-full border rounded px-3 py-2"
                  value={newWidget.type}
                  onChange={(e) => setNewWidget({ ...newWidget, type: e.target.value })}
                >
                  <option value="chart">Bar Chart</option>
                  <option value="line">Line Chart</option>
                  <option value="area">Area Chart</option>
                  <option value="pie">Pie Chart</option>
                  <option value="scatter">Scatter Plot</option>
                  <option value="table">Table</option>
                  <option value="metric">Metric</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Query *</label>
                <textarea
                  className="w-full border rounded px-3 py-2"
                  rows={3}
                  value={newWidget.query}
                  onChange={(e) => setNewWidget({ ...newWidget, query: e.target.value })}
                  placeholder="Enter SPL query (e.g., search error | stats count by level)"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Width</label>
                  <input
                    type="number"
                    min="1"
                    max="12"
                    className="w-full border rounded px-3 py-2"
                    value={newWidget.width}
                    onChange={(e) => setNewWidget({ ...newWidget, width: parseInt(e.target.value) || 4 })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Height</label>
                  <input
                    type="number"
                    min="1"
                    max="12"
                    className="w-full border rounded px-3 py-2"
                    value={newWidget.height}
                    onChange={(e) => setNewWidget({ ...newWidget, height: parseInt(e.target.value) || 3 })}
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end space-x-2 mt-6">
              <Button variant="outline" onClick={() => setShowWidgetModal(false)}>
                Cancel
              </Button>
              <Button onClick={handleAddWidget}>
                Add Widget
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardsPage;