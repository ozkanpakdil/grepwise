import { useState, useEffect } from 'react';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { roleApi, Role, RoleRequest } from '@/api/role';
import { Permission } from '@/api/permission';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';

export default function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [permissionCategories, setPermissionCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCreatingRole, setIsCreatingRole] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const { toast } = useToast();

  // Load roles and permissions on component mount
  useEffect(() => {
    loadRolesAndPermissions();
  }, []);

  const loadRolesAndPermissions = async () => {
    try {
      setLoading(true);
      const [rolesData, permissionsData, categoriesData] = await Promise.all([
        roleApi.getAllRoles(),
        roleApi.getAllPermissions(),
        roleApi.getAllPermissionCategories()
      ]);
      setRoles(rolesData);
      setPermissions(permissionsData);
      setPermissionCategories(categoriesData);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to load roles and permissions',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Form state for creating/editing roles
  const [formData, setFormData] = useState<RoleRequest>({
    name: '',
    description: '',
    permissionIds: [],
  });

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      permissionIds: [],
    });
  };

  const handleCreateRole = () => {
    setIsCreatingRole(true);
    setIsEditing(false);
    resetForm();
  };

  const handleEditRole = (role: Role) => {
    setSelectedRole(role);
    setIsEditing(true);
    setIsCreatingRole(true);

    // Populate form with role data
    setFormData({
      name: role.name,
      description: role.description,
      permissionIds: role.permissions.map(p => p.id),
    });
  };

  const handleDeleteRole = async (id: string) => {
    try {
      await roleApi.deleteRole(id);
      await loadRolesAndPermissions(); // Reload roles after deletion
      toast({
        title: 'Role deleted',
        description: 'The role has been deleted successfully',
      });
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.message || 'Failed to delete role',
        variant: 'destructive',
      });
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePermissionChange = (permissionId: string, checked: boolean) => {
    setFormData(prev => {
      if (checked) {
        return {
          ...prev,
          permissionIds: [...prev.permissionIds, permissionId]
        };
      } else {
        return {
          ...prev,
          permissionIds: prev.permissionIds.filter(id => id !== permissionId)
        };
      }
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      if (isEditing && selectedRole) {
        await roleApi.updateRole(selectedRole.id, formData);
        toast({
          title: 'Role updated',
          description: 'The role has been updated successfully',
        });
      } else {
        await roleApi.createRole(formData);
        toast({
          title: 'Role created',
          description: 'The role has been created successfully',
        });
      }
      
      setIsCreatingRole(false);
      resetForm();
      await loadRolesAndPermissions(); // Reload roles after creation/update
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.message || 'Failed to save role',
        variant: 'destructive',
      });
    }
  };

  const handleCancel = () => {
    setIsCreatingRole(false);
    resetForm();
  };

  // Group permissions by category for better display
  const getPermissionsByCategory = (category: string) => {
    return permissions.filter(p => p.category === category);
  };

  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Role Management</h1>
        <Button onClick={handleCreateRole}>Create Role</Button>
      </div>

      {isCreatingRole ? (
        <div className="bg-card rounded-lg shadow p-6 mb-6">
          <h2 className="text-2xl font-bold mb-4">{isEditing ? 'Edit Role' : 'Create Role'}</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4">
              <div className="space-y-2">
                <label htmlFor="name" className="block text-sm font-medium">
                  Role Name
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  required
                  value={formData.name}
                  onChange={handleInputChange}
                  className="w-full rounded-md border border-input bg-background px-3 py-2"
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="description" className="block text-sm font-medium">
                  Description
                </label>
                <textarea
                  id="description"
                  name="description"
                  rows={3}
                  value={formData.description}
                  onChange={handleInputChange}
                  className="w-full rounded-md border border-input bg-background px-3 py-2"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-sm font-medium mb-2">
                  Permissions
                </label>
                <div className="border border-input rounded-md p-4 max-h-96 overflow-y-auto">
                  {permissionCategories.map(category => (
                    <div key={category} className="mb-4">
                      <h3 className="font-medium text-sm mb-2">{category}</h3>
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
                        {getPermissionsByCategory(category).map(permission => (
                          <div key={permission.id} className="flex items-center space-x-2">
                            <Checkbox
                              id={`permission-${permission.id}`}
                              checked={formData.permissionIds.includes(permission.id)}
                              onCheckedChange={(checked) => 
                                handlePermissionChange(permission.id, checked as boolean)
                              }
                            />
                            <Label
                              htmlFor={`permission-${permission.id}`}
                              className="text-sm cursor-pointer"
                              title={permission.description}
                            >
                              {permission.name}
                            </Label>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex justify-end space-x-2">
              <Button type="button" variant="outline" onClick={handleCancel}>
                Cancel
              </Button>
              <Button type="submit">
                {isEditing ? 'Update Role' : 'Create Role'}
              </Button>
            </div>
          </form>
        </div>
      ) : null}

      <div className="bg-card rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium">Role Name</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Description</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Permissions</th>
                <th className="px-4 py-3 text-right text-sm font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={4} className="px-4 py-3 text-center">Loading roles...</td>
                </tr>
              ) : roles.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-4 py-3 text-center">No roles found</td>
                </tr>
              ) : (
                roles.map(role => (
                  <tr key={role.id} className="hover:bg-muted/50">
                    <td className="px-4 py-3 text-sm">{role.name}</td>
                    <td className="px-4 py-3 text-sm">{role.description}</td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex flex-wrap gap-1">
                        {role.permissions.slice(0, 3).map(permission => (
                          <span 
                            key={permission.id} 
                            className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-primary/10 text-primary"
                            title={permission.description}
                          >
                            {permission.name}
                          </span>
                        ))}
                        {role.permissions.length > 3 && (
                          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-muted">
                            +{role.permissions.length - 3} more
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-right">
                      <div className="flex justify-end space-x-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleEditRole(role)}
                        >
                          Edit
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteRole(role.id)}
                        >
                          Delete
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}