describe('Dashboard Management Flow', () => {
  beforeEach(() => {
    // Login before each test
    cy.login('admin', 'password');
    cy.navigateToDashboards();
  });

  it('should display dashboards page with list of dashboards', () => {
    cy.get('h1').should('contain', 'Dashboards');
    cy.get('[data-testid="create-dashboard-button"]').should('exist');
    cy.get('[data-testid="dashboards-list"]').should('exist');
  });

  it('should create a new dashboard', () => {
    const dashboardName = `Test Dashboard ${Date.now()}`;
    const dashboardDescription = 'This is a test dashboard created by Cypress';
    
    // Click create dashboard button
    cy.get('[data-testid="create-dashboard-button"]').click();
    
    // Fill dashboard form
    cy.get('[data-testid="dashboard-name-input"]').type(dashboardName);
    cy.get('[data-testid="dashboard-description-input"]').type(dashboardDescription);
    
    // Submit form
    cy.get('[data-testid="create-dashboard-submit"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Dashboard created successfully');
    
    // Verify dashboard is in the list
    cy.get('[data-testid="dashboards-list"]').should('contain', dashboardName);
  });

  it('should view dashboard details', () => {
    // Find and click on a dashboard
    cy.get('[data-testid="dashboard-item"]').first().click();
    
    // Verify dashboard view page
    cy.url().should('include', '/dashboards/');
    cy.get('[data-testid="dashboard-title"]').should('exist');
    cy.get('[data-testid="add-widget-button"]').should('exist');
    cy.get('[data-testid="edit-dashboard-button"]').should('exist');
  });

  it('should add a widget to a dashboard', () => {
    // Find and click on a dashboard
    cy.get('[data-testid="dashboard-item"]').first().click();
    
    // Click add widget button
    cy.get('[data-testid="add-widget-button"]').click();
    
    // Fill widget form
    const widgetTitle = `Test Widget ${Date.now()}`;
    cy.get('[data-testid="widget-title-input"]').type(widgetTitle);
    cy.get('[data-testid="widget-type-select"]').click();
    cy.get('[data-testid="widget-type-option-chart"]').click();
    cy.get('[data-testid="widget-query-input"]').type('source="nginx" level="error" | stats count by host');
    
    // Submit form
    cy.get('[data-testid="create-widget-submit"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Widget added successfully');
    
    // Verify widget is displayed on dashboard
    cy.get('[data-testid="widget-title"]').should('contain', widgetTitle);
  });

  it('should edit dashboard', () => {
    // Find and click on a dashboard
    cy.get('[data-testid="dashboard-item"]').first().click();
    
    // Click edit button
    cy.get('[data-testid="edit-dashboard-button"]').click();
    
    // Update dashboard name
    const updatedName = `Updated Dashboard ${Date.now()}`;
    cy.get('[data-testid="dashboard-name-input"]').clear().type(updatedName);
    
    // Submit form
    cy.get('[data-testid="update-dashboard-submit"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Dashboard updated successfully');
    
    // Verify dashboard title is updated
    cy.get('[data-testid="dashboard-title"]').should('contain', updatedName);
  });

  it('should delete a widget from dashboard', () => {
    // Find and click on a dashboard
    cy.get('[data-testid="dashboard-item"]').first().click();
    
    // Enter edit mode
    cy.get('[data-testid="edit-dashboard-button"]').click();
    
    // Get widget count before deletion
    cy.get('[data-testid="widget"]').then($widgets => {
      const widgetCount = $widgets.length;
      
      // Click delete button on first widget
      cy.get('[data-testid="delete-widget-button"]').first().click();
      
      // Confirm deletion
      cy.on('window:confirm', () => true);
      
      // Verify success message
      cy.get('[data-testid="toast-success"]').should('exist');
      cy.get('[data-testid="toast-success"]').should('contain', 'Widget deleted successfully');
      
      // Verify widget count decreased
      cy.get('[data-testid="widget"]').should('have.length', widgetCount - 1);
    });
  });

  it('should delete a dashboard', () => {
    // Create a new dashboard to delete
    const dashboardName = `Dashboard to Delete ${Date.now()}`;
    
    // Click create dashboard button
    cy.get('[data-testid="create-dashboard-button"]').click();
    
    // Fill dashboard form
    cy.get('[data-testid="dashboard-name-input"]').type(dashboardName);
    cy.get('[data-testid="dashboard-description-input"]').type('This dashboard will be deleted');
    
    // Submit form
    cy.get('[data-testid="create-dashboard-submit"]').click();
    
    // Wait for dashboard to be created
    cy.get('[data-testid="dashboards-list"]').should('contain', dashboardName);
    
    // Get dashboard count before deletion
    cy.get('[data-testid="dashboard-item"]').then($dashboards => {
      const dashboardCount = $dashboards.length;
      
      // Find and click delete button for the new dashboard
      cy.get('[data-testid="dashboard-item"]').contains(dashboardName)
        .parent().find('[data-testid="delete-dashboard-button"]').click();
      
      // Confirm deletion
      cy.on('window:confirm', () => true);
      
      // Verify success message
      cy.get('[data-testid="toast-success"]').should('exist');
      cy.get('[data-testid="toast-success"]').should('contain', 'Dashboard deleted successfully');
      
      // Verify dashboard count decreased
      cy.get('[data-testid="dashboard-item"]').should('have.length', dashboardCount - 1);
      
      // Verify dashboard is no longer in the list
      cy.get('[data-testid="dashboards-list"]').should('not.contain', dashboardName);
    });
  });

  it('should share a dashboard', () => {
    // Find and click on a dashboard
    cy.get('[data-testid="dashboard-item"]').first().click();
    
    // Click share button
    cy.get('[data-testid="share-dashboard-button"]').click();
    
    // Toggle sharing on
    cy.get('[data-testid="share-dashboard-toggle"]').click();
    
    // Copy share link
    cy.get('[data-testid="copy-share-link-button"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Share link copied to clipboard');
    
    // Close share dialog
    cy.get('[data-testid="close-share-dialog-button"]').click();
    
    // Verify dashboard is marked as shared
    cy.get('[data-testid="shared-indicator"]').should('exist');
  });

  it('should export dashboard as PDF', () => {
    // Find and click on a dashboard
    cy.get('[data-testid="dashboard-item"]').first().click();
    
    // Click export button
    cy.get('[data-testid="export-dashboard-button"]').click();
    
    // Select PDF format
    cy.get('[data-testid="export-format-pdf"]').click();
    
    // Click export
    cy.get('[data-testid="confirm-export-button"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Dashboard exported successfully');
  });
});