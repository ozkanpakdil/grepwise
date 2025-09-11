package io.github.ozkanpakdil.grepwise.marketplace.service;

import io.github.ozkanpakdil.grepwise.marketplace.model.AppDependency;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackage;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageStatus;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageType;
import io.github.ozkanpakdil.grepwise.plugin.PluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AppMarketplaceService implementation.
 */
public class AppMarketplaceServiceTest {
    
    @Mock
    private PluginRegistry pluginRegistry;
    
    private AppMarketplaceService appMarketplaceService;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        appMarketplaceService = new AppMarketplaceServiceImpl(pluginRegistry);
    }
    
    @Test
    public void testSubmitPackage() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        
        // Act
        AppPackage submittedPackage = appMarketplaceService.submitPackage(appPackage);
        
        // Assert
        assertNotNull(submittedPackage);
        assertEquals("test-package", submittedPackage.getId());
        assertEquals("Test Package", submittedPackage.getName());
        assertEquals(AppPackageStatus.PENDING_REVIEW, submittedPackage.getStatus());
        assertNotNull(submittedPackage.getPublishedDate());
        assertNotNull(submittedPackage.getLastUpdatedDate());
    }
    
    @Test
    public void testSubmitPackageWithNullId() {
        // Arrange
        AppPackage appPackage = createTestAppPackage(null, "Test Package");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appMarketplaceService.submitPackage(appPackage));
    }
    
    @Test
    public void testSubmitPackageWithEmptyId() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("", "Test Package");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appMarketplaceService.submitPackage(appPackage));
    }
    
    @Test
    public void testSubmitPackageWithNullName() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appMarketplaceService.submitPackage(appPackage));
    }
    
    @Test
    public void testSubmitPackageWithEmptyName() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appMarketplaceService.submitPackage(appPackage));
    }
    
    @Test
    public void testSubmitDuplicatePackage() {
        // Arrange
        AppPackage appPackage1 = createTestAppPackage("test-package", "Test Package 1");
        AppPackage appPackage2 = createTestAppPackage("test-package", "Test Package 2");
        
        // Act
        appMarketplaceService.submitPackage(appPackage1);
        
        // Assert
        assertThrows(IllegalArgumentException.class, () -> appMarketplaceService.submitPackage(appPackage2));
    }
    
    @Test
    public void testGetPackage() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        appMarketplaceService.submitPackage(appPackage);
        
        // Act
        Optional<AppPackage> retrievedPackage = appMarketplaceService.getPackage("test-package");
        
        // Assert
        assertTrue(retrievedPackage.isPresent());
        assertEquals("test-package", retrievedPackage.get().getId());
        assertEquals("Test Package", retrievedPackage.get().getName());
    }
    
    @Test
    public void testGetNonExistentPackage() {
        // Act
        Optional<AppPackage> retrievedPackage = appMarketplaceService.getPackage("non-existent");
        
        // Assert
        assertFalse(retrievedPackage.isPresent());
    }
    
    @Test
    public void testGetAllPackages() {
        // Arrange
        AppPackage appPackage1 = createTestAppPackage("test-package-1", "Test Package 1");
        AppPackage appPackage2 = createTestAppPackage("test-package-2", "Test Package 2");
        appMarketplaceService.submitPackage(appPackage1);
        appMarketplaceService.submitPackage(appPackage2);
        
        // Act
        List<AppPackage> allPackages = appMarketplaceService.getAllPackages();
        
        // Assert
        assertEquals(2, allPackages.size());
        assertTrue(allPackages.stream().anyMatch(p -> p.getId().equals("test-package-1")));
        assertTrue(allPackages.stream().anyMatch(p -> p.getId().equals("test-package-2")));
    }
    
    @Test
    public void testGetPackagesByType() {
        // Arrange
        AppPackage appPackage1 = createTestAppPackage("test-package-1", "Test Package 1");
        appPackage1.setType(AppPackageType.PLUGIN);
        
        AppPackage appPackage2 = createTestAppPackage("test-package-2", "Test Package 2");
        appPackage2.setType(AppPackageType.VISUALIZATION);
        
        appMarketplaceService.submitPackage(appPackage1);
        appMarketplaceService.submitPackage(appPackage2);
        
        // Act
        List<AppPackage> pluginPackages = appMarketplaceService.getPackagesByType(AppPackageType.PLUGIN);
        List<AppPackage> visualizationPackages = appMarketplaceService.getPackagesByType(AppPackageType.VISUALIZATION);
        
        // Assert
        assertEquals(1, pluginPackages.size());
        assertEquals("test-package-1", pluginPackages.get(0).getId());
        
        assertEquals(1, visualizationPackages.size());
        assertEquals("test-package-2", visualizationPackages.get(0).getId());
    }
    
    @Test
    public void testGetPackagesByStatus() {
        // Arrange
        AppPackage appPackage1 = createTestAppPackage("test-package-1", "Test Package 1");
        AppPackage appPackage2 = createTestAppPackage("test-package-2", "Test Package 2");
        
        appMarketplaceService.submitPackage(appPackage1);
        appMarketplaceService.submitPackage(appPackage2);
        
        appMarketplaceService.changePackageStatus("test-package-2", AppPackageStatus.APPROVED);
        
        // Act
        List<AppPackage> pendingPackages = appMarketplaceService.getPackagesByStatus(AppPackageStatus.PENDING_REVIEW);
        List<AppPackage> approvedPackages = appMarketplaceService.getPackagesByStatus(AppPackageStatus.APPROVED);
        
        // Assert
        assertEquals(1, pendingPackages.size());
        assertEquals("test-package-1", pendingPackages.get(0).getId());
        
        assertEquals(1, approvedPackages.size());
        assertEquals("test-package-2", approvedPackages.get(0).getId());
    }
    
    @Test
    public void testSearchPackages() {
        // Arrange
        AppPackage appPackage1 = createTestAppPackage("test-package-1", "Test Package One");
        appPackage1.setDescription("This is a test package for testing");
        
        AppPackage appPackage2 = createTestAppPackage("test-package-2", "Another Package");
        appPackage2.setDescription("This is another test package");
        appPackage2.addTag("test");
        
        AppPackage appPackage3 = createTestAppPackage("visualization-package", "Visualization Package");
        appPackage3.setDescription("A package for visualizations");
        appPackage3.addCategory("test-category");
        
        appMarketplaceService.submitPackage(appPackage1);
        appMarketplaceService.submitPackage(appPackage2);
        appMarketplaceService.submitPackage(appPackage3);
        
        // Act
        List<AppPackage> testResults = appMarketplaceService.searchPackages("test");
        List<AppPackage> visualizationResults = appMarketplaceService.searchPackages("visualization");
        List<AppPackage> categoryResults = appMarketplaceService.searchPackages("category");
        
        // Assert
        assertEquals(3, testResults.size());
        assertTrue(testResults.stream().anyMatch(p -> p.getId().equals("test-package-1")));
        assertTrue(testResults.stream().anyMatch(p -> p.getId().equals("test-package-2")));
        
        assertEquals(1, visualizationResults.size());
        assertEquals("visualization-package", visualizationResults.get(0).getId());
        
        assertEquals(1, categoryResults.size());
        assertEquals("visualization-package", categoryResults.get(0).getId());
    }
    
    @Test
    public void testChangePackageStatus() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        appMarketplaceService.submitPackage(appPackage);
        
        // Act
        AppPackage updatedPackage = appMarketplaceService.changePackageStatus("test-package", AppPackageStatus.APPROVED);
        
        // Assert
        assertEquals(AppPackageStatus.APPROVED, updatedPackage.getStatus());
        
        Optional<AppPackage> retrievedPackage = appMarketplaceService.getPackage("test-package");
        assertTrue(retrievedPackage.isPresent());
        assertEquals(AppPackageStatus.APPROVED, retrievedPackage.get().getStatus());
    }
    
    @Test
    public void testChangePackageStatusNonExistent() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
                appMarketplaceService.changePackageStatus("non-existent", AppPackageStatus.APPROVED));
    }
    
    @Test
    public void testInstallPackage() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        appMarketplaceService.submitPackage(appPackage);
        appMarketplaceService.changePackageStatus("test-package", AppPackageStatus.APPROVED);
        
        // Act
        boolean result = appMarketplaceService.installPackage("test-package");
        
        // Assert
        assertTrue(result);
        assertTrue(appMarketplaceService.isPackageInstalled("test-package"));
        
        List<AppPackage> installedPackages = appMarketplaceService.getInstalledPackages();
        assertEquals(1, installedPackages.size());
        assertEquals("test-package", installedPackages.get(0).getId());
    }
    
    @Test
    public void testInstallNonExistentPackage() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> appMarketplaceService.installPackage("non-existent"));
    }
    
    @Test
    public void testInstallNonApprovedPackage() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        appMarketplaceService.submitPackage(appPackage);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> appMarketplaceService.installPackage("test-package"));
    }
    
    @Test
    public void testUninstallPackage() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        appMarketplaceService.submitPackage(appPackage);
        appMarketplaceService.changePackageStatus("test-package", AppPackageStatus.APPROVED);
        appMarketplaceService.installPackage("test-package");
        
        // Act
        boolean result = appMarketplaceService.uninstallPackage("test-package");
        
        // Assert
        assertTrue(result);
        assertFalse(appMarketplaceService.isPackageInstalled("test-package"));
        
        List<AppPackage> installedPackages = appMarketplaceService.getInstalledPackages();
        assertEquals(0, installedPackages.size());
    }
    
    @Test
    public void testUninstallNonInstalledPackage() {
        // Arrange
        AppPackage appPackage = createTestAppPackage("test-package", "Test Package");
        appMarketplaceService.submitPackage(appPackage);
        appMarketplaceService.changePackageStatus("test-package", AppPackageStatus.APPROVED);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> appMarketplaceService.uninstallPackage("test-package"));
    }
    
    /**
     * Creates a test app package with the specified ID and name.
     *
     * @param id The ID of the app package
     * @param name The name of the app package
     * @return The created app package
     */
    private AppPackage createTestAppPackage(String id, String name) {
        AppPackage appPackage = new AppPackage();
        appPackage.setId(id);
        appPackage.setName(name);
        appPackage.setDescription("Test description");
        appPackage.setVersion("1.0.0");
        appPackage.setAuthor("Test Author");
        appPackage.setType(AppPackageType.PLUGIN);
        return appPackage;
    }
}