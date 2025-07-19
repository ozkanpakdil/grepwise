describe('Alarm Management Flow', () => {
  beforeEach(() => {
    // Login before each test
    cy.login('admin', 'password');
    cy.visit('/alarms');
  });

  it('should display alarms page with list of alarms', () => {
    cy.get('h1').should('contain', 'Alarms');
    cy.get('[data-testid="create-alarm-button"]').should('exist');
    cy.get('[data-testid="alarms-list"]').should('exist');
  });

  it('should create a new alarm', () => {
    const alarmName = `Test Alarm ${Date.now()}`;
    const alarmDescription = 'This is a test alarm created by Cypress';
    const searchQuery = 'level="error"';
    
    // Click create alarm button
    cy.get('[data-testid="create-alarm-button"]').click();
    
    // Fill alarm form
    cy.get('[data-testid="alarm-name-input"]').type(alarmName);
    cy.get('[data-testid="alarm-description-input"]').type(alarmDescription);
    cy.get('[data-testid="alarm-query-input"]').type(searchQuery);
    
    // Set threshold
    cy.get('[data-testid="alarm-threshold-input"]').clear().type('5');
    
    // Set time window
    cy.get('[data-testid="alarm-time-window-select"]').click();
    cy.get('[data-testid="alarm-time-window-option-15m"]').click();
    
    // Set notification channel
    cy.get('[data-testid="alarm-notification-channel-select"]').click();
    cy.get('[data-testid="alarm-notification-channel-option-email"]').click();
    
    // Submit form
    cy.get('[data-testid="create-alarm-submit"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Alarm created successfully');
    
    // Verify alarm is in the list
    cy.get('[data-testid="alarms-list"]').should('contain', alarmName);
  });

  it('should view alarm details', () => {
    // Find and click on an alarm
    cy.get('[data-testid="alarm-item"]').first().click();
    
    // Verify alarm details page
    cy.url().should('include', '/alarms/');
    cy.get('[data-testid="alarm-details-title"]').should('exist');
    cy.get('[data-testid="alarm-details-description"]').should('exist');
    cy.get('[data-testid="alarm-details-query"]').should('exist');
    cy.get('[data-testid="alarm-details-threshold"]').should('exist');
    cy.get('[data-testid="alarm-details-time-window"]').should('exist');
    cy.get('[data-testid="alarm-details-notification-channel"]').should('exist');
    cy.get('[data-testid="edit-alarm-button"]').should('exist');
  });

  it('should edit an alarm', () => {
    // Find and click on an alarm
    cy.get('[data-testid="alarm-item"]').first().click();
    
    // Click edit button
    cy.get('[data-testid="edit-alarm-button"]').click();
    
    // Update alarm name
    const updatedName = `Updated Alarm ${Date.now()}`;
    cy.get('[data-testid="alarm-name-input"]').clear().type(updatedName);
    
    // Update threshold
    cy.get('[data-testid="alarm-threshold-input"]').clear().type('10');
    
    // Submit form
    cy.get('[data-testid="update-alarm-submit"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Alarm updated successfully');
    
    // Verify alarm details are updated
    cy.get('[data-testid="alarm-details-title"]').should('contain', updatedName);
    cy.get('[data-testid="alarm-details-threshold"]').should('contain', '10');
  });

  it('should delete an alarm', () => {
    // Create a new alarm to delete
    const alarmName = `Alarm to Delete ${Date.now()}`;
    
    // Click create alarm button
    cy.get('[data-testid="create-alarm-button"]').click();
    
    // Fill alarm form
    cy.get('[data-testid="alarm-name-input"]').type(alarmName);
    cy.get('[data-testid="alarm-description-input"]').type('This alarm will be deleted');
    cy.get('[data-testid="alarm-query-input"]').type('level="error"');
    cy.get('[data-testid="alarm-threshold-input"]').clear().type('5');
    
    // Set time window
    cy.get('[data-testid="alarm-time-window-select"]').click();
    cy.get('[data-testid="alarm-time-window-option-15m"]').click();
    
    // Set notification channel
    cy.get('[data-testid="alarm-notification-channel-select"]').click();
    cy.get('[data-testid="alarm-notification-channel-option-email"]').click();
    
    // Submit form
    cy.get('[data-testid="create-alarm-submit"]').click();
    
    // Wait for alarm to be created
    cy.get('[data-testid="alarms-list"]').should('contain', alarmName);
    
    // Get alarm count before deletion
    cy.get('[data-testid="alarm-item"]').then($alarms => {
      const alarmCount = $alarms.length;
      
      // Find and click delete button for the new alarm
      cy.get('[data-testid="alarm-item"]').contains(alarmName)
        .parent().find('[data-testid="delete-alarm-button"]').click();
      
      // Confirm deletion
      cy.on('window:confirm', () => true);
      
      // Verify success message
      cy.get('[data-testid="toast-success"]').should('exist');
      cy.get('[data-testid="toast-success"]').should('contain', 'Alarm deleted successfully');
      
      // Verify alarm count decreased
      cy.get('[data-testid="alarm-item"]').should('have.length', alarmCount - 1);
      
      // Verify alarm is no longer in the list
      cy.get('[data-testid="alarms-list"]').should('not.contain', alarmName);
    });
  });

  it('should test an alarm', () => {
    // Find and click on an alarm
    cy.get('[data-testid="alarm-item"]').first().click();
    
    // Click test button
    cy.get('[data-testid="test-alarm-button"]').click();
    
    // Verify test results are displayed
    cy.get('[data-testid="alarm-test-results"]').should('exist');
    cy.get('[data-testid="alarm-test-results-count"]').should('exist');
    cy.get('[data-testid="alarm-test-results-status"]').should('exist');
  });

  it('should navigate to alarm monitoring page', () => {
    // Click on alarm monitoring tab/button
    cy.get('[data-testid="alarm-monitoring-link"]').click();
    
    // Verify alarm monitoring page
    cy.url().should('include', '/alarm-monitoring');
    cy.get('h1').should('contain', 'Alarm Monitoring');
    cy.get('[data-testid="active-alarms-section"]').should('exist');
    cy.get('[data-testid="alarm-history-section"]').should('exist');
  });

  it('should acknowledge an active alarm', () => {
    // Navigate to alarm monitoring page
    cy.get('[data-testid="alarm-monitoring-link"]').click();
    
    // Check if there are active alarms
    cy.get('body').then($body => {
      if ($body.find('[data-testid="active-alarm-item"]').length > 0) {
        // Click acknowledge button on first active alarm
        cy.get('[data-testid="acknowledge-alarm-button"]').first().click();
        
        // Verify success message
        cy.get('[data-testid="toast-success"]').should('exist');
        cy.get('[data-testid="toast-success"]').should('contain', 'Alarm acknowledged');
        
        // Verify alarm is marked as acknowledged
        cy.get('[data-testid="alarm-status"]').first().should('contain', 'Acknowledged');
      } else {
        // Skip test if no active alarms
        cy.log('No active alarms to acknowledge');
      }
    });
  });

  it('should configure notification preferences', () => {
    // Navigate to notification preferences
    cy.get('[data-testid="notification-preferences-link"]').click();
    
    // Verify notification preferences page
    cy.url().should('include', '/notification-preferences');
    cy.get('h1').should('contain', 'Notification Preferences');
    
    // Toggle email notifications
    cy.get('[data-testid="email-notification-toggle"]').click();
    
    // Enter email address if not already set
    cy.get('body').then($body => {
      if ($body.find('[data-testid="email-address-input"]').length > 0) {
        cy.get('[data-testid="email-address-input"]').clear().type('test@example.com');
      }
    });
    
    // Save preferences
    cy.get('[data-testid="save-preferences-button"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Notification preferences saved');
  });
});