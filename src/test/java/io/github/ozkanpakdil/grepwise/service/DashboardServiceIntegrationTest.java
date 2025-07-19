package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Dashboard;
import io.github.ozkanpakdil.grepwise.model.DashboardWidget;
import io.github.ozkanpakdil.grepwise.repository.DashboardRepository;
import io.github.ozkanpakdil.grepwise.repository.DashboardWidgetRepository;
import io.github.ozkanpakdil.grepwise.service.SplQueryService.SplQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the DashboardService.
 * Tests the integration between DashboardService and its dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class DashboardServiceIntegrationTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DashboardWidgetRepository widgetRepository;

    @Mock
    private SplQueryService splQueryService;

    @Captor
    private ArgumentCaptor<Dashboard> dashboardCaptor;

    @Captor
    private ArgumentCaptor<DashboardWidget> widgetCaptor;

    private DashboardService dashboardService;

    // Test data
    private static final String USER_ID = "test-user";
    private static final String OTHER_USER_ID = "other-user";
    private static final String DASHBOARD_ID = "dashboard-123";
    private static final String WIDGET_ID = "widget-456";

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService();
        
        // Inject mocked dependencies using ReflectionTestUtils
        ReflectionTestUtils.setField(dashboardService, "dashboardRepository", dashboardRepository);
        ReflectionTestUtils.setField(dashboardService, "widgetRepository", widgetRepository);
        ReflectionTestUtils.setField(dashboardService, "splQueryService", splQueryService);
    }

    @Test
    void testCreateDashboard() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        when(dashboardRepository.existsByNameAndCreatedBy(anyString(), anyString())).thenReturn(false);
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> {
            Dashboard savedDashboard = invocation.getArgument(0);
            savedDashboard.setId(DASHBOARD_ID);
            return savedDashboard;
        });

        // Act
        Dashboard result = dashboardService.createDashboard(dashboard);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(DASHBOARD_ID, result.getId(), "Dashboard ID should be set");
        assertEquals("Test Dashboard", result.getName(), "Dashboard name should match");
        assertEquals(USER_ID, result.getCreatedBy(), "Creator should match");
        
        verify(dashboardRepository).existsByNameAndCreatedBy("Test Dashboard", USER_ID);
        verify(dashboardRepository).save(dashboardCaptor.capture());
        
        Dashboard capturedDashboard = dashboardCaptor.getValue();
        assertEquals("Test Dashboard", capturedDashboard.getName(), "Saved dashboard name should match");
        assertEquals("Test Description", capturedDashboard.getDescription(), "Saved dashboard description should match");
        assertEquals(USER_ID, capturedDashboard.getCreatedBy(), "Saved dashboard creator should match");
    }

    @Test
    void testCreateDashboardWithDuplicateName() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        when(dashboardRepository.existsByNameAndCreatedBy("Test Dashboard", USER_ID)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.createDashboard(dashboard);
        });
        
        assertTrue(exception.getMessage().contains("already exists"), "Exception message should indicate duplicate name");
        verify(dashboardRepository).existsByNameAndCreatedBy("Test Dashboard", USER_ID);
        verify(dashboardRepository, never()).save(any(Dashboard.class));
    }

    @Test
    void testUpdateDashboard() {
        // Arrange
        Dashboard dashboard = new Dashboard("Updated Dashboard", "Updated Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        Dashboard existingDashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        existingDashboard.setId(DASHBOARD_ID);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(existingDashboard);
        when(dashboardRepository.existsByNameAndCreatedBy("Updated Dashboard", USER_ID)).thenReturn(false);
        when(dashboardRepository.update(any(Dashboard.class))).thenReturn(dashboard);

        // Act
        Dashboard result = dashboardService.updateDashboard(dashboard);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals("Updated Dashboard", result.getName(), "Dashboard name should be updated");
        assertEquals("Updated Description", result.getDescription(), "Dashboard description should be updated");
        
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(dashboardRepository).existsByNameAndCreatedBy("Updated Dashboard", USER_ID);
        verify(dashboardRepository).update(dashboardCaptor.capture());
        
        Dashboard capturedDashboard = dashboardCaptor.getValue();
        assertEquals("Updated Dashboard", capturedDashboard.getName(), "Updated dashboard name should match");
        assertEquals("Updated Description", capturedDashboard.getDescription(), "Updated dashboard description should match");
    }

    @Test
    void testUpdateDashboardNotFound() {
        // Arrange
        Dashboard dashboard = new Dashboard("Updated Dashboard", "Updated Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.updateDashboard(dashboard);
        });
        
        assertTrue(exception.getMessage().contains("not found"), "Exception message should indicate dashboard not found");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(dashboardRepository, never()).update(any(Dashboard.class));
    }

    @Test
    void testGetDashboardsForUser() {
        // Arrange
        List<Dashboard> dashboards = new ArrayList<>();
        dashboards.add(new Dashboard("Dashboard 1", "Description 1", USER_ID));
        dashboards.add(new Dashboard("Dashboard 2", "Description 2", USER_ID));
        
        when(dashboardRepository.findAccessibleByUser(USER_ID)).thenReturn(dashboards);
        when(widgetRepository.findByDashboardId(any())).thenReturn(new ArrayList<>());

        // Act
        List<Dashboard> result = dashboardService.getDashboardsForUser(USER_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 dashboards");
        
        verify(dashboardRepository).findAccessibleByUser(USER_ID);
        verify(widgetRepository, times(2)).findByDashboardId(any());
    }

    @Test
    void testGetDashboardById() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        List<DashboardWidget> widgets = new ArrayList<>();
        DashboardWidget widget = new DashboardWidget(DASHBOARD_ID, "Test Widget", "chart", "search *");
        widget.setId(WIDGET_ID);
        widgets.add(widget);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);
        when(widgetRepository.findByDashboardId(DASHBOARD_ID)).thenReturn(widgets);

        // Act
        Dashboard result = dashboardService.getDashboardById(DASHBOARD_ID, USER_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(DASHBOARD_ID, result.getId(), "Dashboard ID should match");
        assertEquals("Test Dashboard", result.getName(), "Dashboard name should match");
        assertNotNull(result.getWidgets(), "Widgets should be loaded");
        assertEquals(1, result.getWidgets().size(), "Should have 1 widget");
        assertEquals(WIDGET_ID, result.getWidgets().get(0).getId(), "Widget ID should match");
        
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository).findByDashboardId(DASHBOARD_ID);
    }

    @Test
    void testGetDashboardByIdNotFound() {
        // Arrange
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.getDashboardById(DASHBOARD_ID, USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("not found"), "Exception message should indicate dashboard not found");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository, never()).findByDashboardId(any());
    }

    @Test
    void testGetDashboardByIdAccessDenied() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        dashboard.setShared(false);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.getDashboardById(DASHBOARD_ID, OTHER_USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("Access denied"), "Exception message should indicate access denied");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository, never()).findByDashboardId(any());
    }

    @Test
    void testDeleteDashboard() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);
        when(widgetRepository.deleteByDashboardId(DASHBOARD_ID)).thenReturn(2);
        when(dashboardRepository.deleteById(DASHBOARD_ID)).thenReturn(true);

        // Act
        boolean result = dashboardService.deleteDashboard(DASHBOARD_ID, USER_ID);

        // Assert
        assertTrue(result, "Delete should return true");
        
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository).deleteByDashboardId(DASHBOARD_ID);
        verify(dashboardRepository).deleteById(DASHBOARD_ID);
    }

    @Test
    void testDeleteDashboardNotFound() {
        // Arrange
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.deleteDashboard(DASHBOARD_ID, USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("not found"), "Exception message should indicate dashboard not found");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository, never()).deleteByDashboardId(any());
        verify(dashboardRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteDashboardAccessDenied() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.deleteDashboard(DASHBOARD_ID, OTHER_USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("Access denied"), "Exception message should indicate access denied");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository, never()).deleteByDashboardId(any());
        verify(dashboardRepository, never()).deleteById(any());
    }

    @Test
    void testShareDashboard() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        dashboard.setShared(false);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);
        when(dashboardRepository.update(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Dashboard result = dashboardService.shareDashboard(DASHBOARD_ID, true, USER_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isShared(), "Dashboard should be shared");
        
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(dashboardRepository).update(dashboardCaptor.capture());
        
        Dashboard capturedDashboard = dashboardCaptor.getValue();
        assertTrue(capturedDashboard.isShared(), "Updated dashboard should be shared");
    }

    @Test
    void testShareDashboardNotFound() {
        // Arrange
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.shareDashboard(DASHBOARD_ID, true, USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("not found"), "Exception message should indicate dashboard not found");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(dashboardRepository, never()).update(any(Dashboard.class));
    }

    @Test
    void testShareDashboardAccessDenied() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.shareDashboard(DASHBOARD_ID, true, OTHER_USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("Access denied"), "Exception message should indicate access denied");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(dashboardRepository, never()).update(any(Dashboard.class));
    }

    @Test
    void testAddWidget() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        DashboardWidget widget = new DashboardWidget(DASHBOARD_ID, "Test Widget", "chart", "search *");
        widget.setWidth(6);
        widget.setHeight(4);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);
        when(widgetRepository.existsByTitleAndDashboardId("Test Widget", DASHBOARD_ID)).thenReturn(false);
        when(widgetRepository.save(any(DashboardWidget.class))).thenAnswer(invocation -> {
            DashboardWidget savedWidget = invocation.getArgument(0);
            savedWidget.setId(WIDGET_ID);
            return savedWidget;
        });

        // Act
        DashboardWidget result = dashboardService.addWidget(widget, USER_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(WIDGET_ID, result.getId(), "Widget ID should be set");
        assertEquals("Test Widget", result.getTitle(), "Widget title should match");
        assertEquals(DASHBOARD_ID, result.getDashboardId(), "Dashboard ID should match");
        
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository).existsByTitleAndDashboardId("Test Widget", DASHBOARD_ID);
        verify(widgetRepository).save(widgetCaptor.capture());
        
        DashboardWidget capturedWidget = widgetCaptor.getValue();
        assertEquals("Test Widget", capturedWidget.getTitle(), "Saved widget title should match");
        assertEquals("chart", capturedWidget.getType(), "Saved widget type should match");
        assertEquals("search *", capturedWidget.getQuery(), "Saved widget query should match");
    }

    @Test
    void testAddWidgetDuplicateTitle() {
        // Arrange
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        
        DashboardWidget widget = new DashboardWidget(DASHBOARD_ID, "Test Widget", "chart", "search *");
        widget.setWidth(6);
        widget.setHeight(4);
        
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);
        when(widgetRepository.existsByTitleAndDashboardId("Test Widget", DASHBOARD_ID)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            dashboardService.addWidget(widget, USER_ID);
        });
        
        assertTrue(exception.getMessage().contains("already exists"), "Exception message should indicate duplicate title");
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(widgetRepository).existsByTitleAndDashboardId("Test Widget", DASHBOARD_ID);
        verify(widgetRepository, never()).save(any(DashboardWidget.class));
    }

    @Test
    void testExecuteWidgetQuery() throws Exception {
        // Arrange
        DashboardWidget widget = new DashboardWidget(DASHBOARD_ID, "Test Widget", "chart", "search *");
        widget.setId(WIDGET_ID);
        
        Dashboard dashboard = new Dashboard("Test Dashboard", "Test Description", USER_ID);
        dashboard.setId(DASHBOARD_ID);
        dashboard.setShared(true);
        
        SplQueryResult queryResult = new SplQueryResult();
        queryResult.setResultType(SplQueryResult.ResultType.STATISTICS);
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("count", 42);
        queryResult.setStatistics(statistics);
        
        when(widgetRepository.findById(WIDGET_ID)).thenReturn(widget);
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(dashboard);
        when(splQueryService.executeSplQuery("search *")).thenReturn(queryResult);

        // Act
        Map<String, Object> result = dashboardService.executeWidgetQuery(WIDGET_ID, OTHER_USER_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals("STATISTICS", result.get("resultType"), "Result type should match");
        assertNotNull(result.get("statistics"), "Statistics should be present");
        assertEquals(42, ((Map<String, Object>)result.get("statistics")).get("count"), "Count should match");
        
        verify(widgetRepository).findById(WIDGET_ID);
        verify(dashboardRepository).findById(DASHBOARD_ID);
        verify(splQueryService).executeSplQuery("search *");
    }

    @Test
    void testGetDashboardStatistics() {
        // Arrange
        when(dashboardRepository.count()).thenReturn(5);
        when(dashboardRepository.countShared()).thenReturn(2);
        when(widgetRepository.count()).thenReturn(15);

        // Act
        Map<String, Object> result = dashboardService.getDashboardStatistics();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(5, result.get("totalDashboards"), "Total dashboards should match");
        assertEquals(2, result.get("sharedDashboards"), "Shared dashboards should match");
        assertEquals(15, result.get("totalWidgets"), "Total widgets should match");
        
        verify(dashboardRepository).count();
        verify(dashboardRepository).countShared();
        verify(widgetRepository).count();
    }
}