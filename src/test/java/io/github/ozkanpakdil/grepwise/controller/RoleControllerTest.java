package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Permission;
import io.github.ozkanpakdil.grepwise.model.Role;
import io.github.ozkanpakdil.grepwise.repository.PermissionRepository;
import io.github.ozkanpakdil.grepwise.repository.RoleRepository;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RoleController class.
 */
public class RoleControllerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllRoles() {
        // Arrange
        List<Role> roles = new ArrayList<>();
        Role adminRole = new Role();
        adminRole.setId("1");
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        
        Role userRole = new Role();
        userRole.setId("2");
        userRole.setName("USER");
        userRole.setDescription("User role");
        
        roles.add(adminRole);
        roles.add(userRole);
        
        when(roleRepository.findAll()).thenReturn(roles);
        
        // Act
        ResponseEntity<List<Role>> response = roleController.getAllRoles();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("ADMIN", response.getBody().get(0).getName());
        assertEquals("USER", response.getBody().get(1).getName());
    }

    @Test
    public void testGetRoleById() {
        // Arrange
        Role role = new Role();
        role.setId("1");
        role.setName("ADMIN");
        role.setDescription("Administrator role");
        
        when(roleRepository.findById("1")).thenReturn(role);
        
        // Act
        ResponseEntity<Role> response = roleController.getRoleById("1");
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ADMIN", response.getBody().getName());
    }

    @Test
    public void testGetRoleByIdNotFound() {
        // Arrange
        when(roleRepository.findById("999")).thenReturn(null);
        
        // Act
        ResponseEntity<Role> response = roleController.getRoleById("999");
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testCreateRole() {
        // Arrange
        RoleController.RoleRequest roleRequest = new RoleController.RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");
        roleRequest.setPermissionIds(Arrays.asList("1", "2", "3"));
        
        Permission permission1 = new Permission();
        permission1.setId("1");
        permission1.setName("user:view");
        
        Permission permission2 = new Permission();
        permission2.setId("2");
        permission2.setName("dashboard:view");
        
        Permission permission3 = new Permission();
        permission3.setId("3");
        permission3.setName("alarm:view");
        
        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(permissionRepository.findById("1")).thenReturn(permission1);
        when(permissionRepository.findById("2")).thenReturn(permission2);
        when(permissionRepository.findById("3")).thenReturn(permission3);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId("3");
            return role;
        });
        
        // Act
        ResponseEntity<?> response = roleController.createRole(roleRequest);
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Role createdRole = (Role) response.getBody();
        assertEquals("MANAGER", createdRole.getName());
        assertEquals("Manager role", createdRole.getDescription());
        assertEquals(3, createdRole.getPermissions().size());
    }

    @Test
    public void testCreateRoleNameExists() {
        // Arrange
        RoleController.RoleRequest roleRequest = new RoleController.RoleRequest();
        roleRequest.setName("ADMIN");
        roleRequest.setDescription("Administrator role");
        
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);
        
        // Act
        ResponseEntity<?> response = roleController.createRole(roleRequest);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Role name already exists", errorResponse.get("error"));
    }

    @Test
    public void testUpdateRole() {
        // Arrange
        Role existingRole = new Role();
        existingRole.setId("1");
        existingRole.setName("ADMIN");
        existingRole.setDescription("Administrator role");
        
        RoleController.RoleRequest roleRequest = new RoleController.RoleRequest();
        roleRequest.setDescription("Updated administrator role");
        roleRequest.setPermissionIds(Arrays.asList("1", "2"));
        
        Permission permission1 = new Permission();
        permission1.setId("1");
        permission1.setName("user:view");
        
        Permission permission2 = new Permission();
        permission2.setId("2");
        permission2.setName("dashboard:view");
        
        when(roleRepository.findById("1")).thenReturn(existingRole);
        when(permissionRepository.findById("1")).thenReturn(permission1);
        when(permissionRepository.findById("2")).thenReturn(permission2);
        when(roleRepository.save(any(Role.class))).thenReturn(existingRole);
        
        // Act
        ResponseEntity<?> response = roleController.updateRole("1", roleRequest);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Role updatedRole = (Role) response.getBody();
        assertEquals("ADMIN", updatedRole.getName());
        assertEquals("Updated administrator role", updatedRole.getDescription());
    }

    @Test
    public void testUpdateRoleNotFound() {
        // Arrange
        RoleController.RoleRequest roleRequest = new RoleController.RoleRequest();
        roleRequest.setDescription("Updated role");
        
        when(roleRepository.findById("999")).thenReturn(null);
        
        // Act
        ResponseEntity<?> response = roleController.updateRole("999", roleRequest);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testDeleteRole() {
        // Arrange
        Role role = new Role();
        role.setId("1");
        role.setName("ADMIN");
        
        when(roleRepository.findById("1")).thenReturn(role);
        when(userRepository.findByRole("ADMIN")).thenReturn(new ArrayList<>());
        when(roleRepository.deleteById("1")).thenReturn(true);
        
        // Act
        ResponseEntity<?> response = roleController.deleteRole("1");
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> successResponse = (Map<String, String>) response.getBody();
        assertEquals("Role deleted successfully", successResponse.get("message"));
    }

    @Test
    public void testDeleteRoleInUse() {
        // Arrange
        Role role = new Role();
        role.setId("1");
        role.setName("ADMIN");
        
        when(roleRepository.findById("1")).thenReturn(role);
        
        // Create a non-empty list of users
        List<io.github.ozkanpakdil.grepwise.model.User> users = new ArrayList<>();
        users.add(new io.github.ozkanpakdil.grepwise.model.User());
        
        when(userRepository.findByRole("ADMIN")).thenReturn(users);
        
        // Act
        ResponseEntity<?> response = roleController.deleteRole("1");
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertEquals("Cannot delete role that is assigned to users", errorResponse.get("error"));
    }

    @Test
    public void testGetAllPermissions() {
        // Arrange
        List<Permission> permissions = new ArrayList<>();
        Permission permission1 = new Permission();
        permission1.setId("1");
        permission1.setName("user:view");
        permission1.setCategory("User Management");
        
        Permission permission2 = new Permission();
        permission2.setId("2");
        permission2.setName("dashboard:view");
        permission2.setCategory("Dashboard");
        
        permissions.add(permission1);
        permissions.add(permission2);
        
        when(permissionRepository.findAll()).thenReturn(permissions);
        
        // Act
        ResponseEntity<List<Permission>> response = roleController.getAllPermissions();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("user:view", response.getBody().get(0).getName());
        assertEquals("dashboard:view", response.getBody().get(1).getName());
    }

    @Test
    public void testGetPermissionsByCategory() {
        // Arrange
        List<Permission> permissions = new ArrayList<>();
        Permission permission1 = new Permission();
        permission1.setId("1");
        permission1.setName("user:view");
        permission1.setCategory("User Management");
        
        Permission permission2 = new Permission();
        permission2.setId("2");
        permission2.setName("user:create");
        permission2.setCategory("User Management");
        
        permissions.add(permission1);
        permissions.add(permission2);
        
        when(permissionRepository.findByCategory("User Management")).thenReturn(permissions);
        
        // Act
        ResponseEntity<List<Permission>> response = roleController.getPermissionsByCategory("User Management");
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("user:view", response.getBody().get(0).getName());
        assertEquals("user:create", response.getBody().get(1).getName());
    }
}