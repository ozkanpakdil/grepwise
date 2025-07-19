describe('Log Search Flow', () => {
  beforeEach(() => {
    // Login before each test
    cy.login('admin', 'password');
    cy.navigateToSearch();
  });

  it('should display search page with search input', () => {
    cy.get('textarea[aria-label="Search query"]').should('exist');
    cy.get('button').contains('Search').should('exist');
    cy.get('[data-testid="time-range-selector"]').should('exist');
  });

  it('should perform basic search and display results', () => {
    const searchQuery = 'error';
    
    // Enter search query
    cy.get('textarea[aria-label="Search query"]').clear().type(searchQuery);
    
    // Click search button
    cy.get('button').contains('Search').click();
    
    // Verify search results are displayed
    cy.get('[data-testid="search-results"]').should('exist');
    cy.get('[data-testid="search-results-count"]').should('exist');
    
    // Verify search query is displayed in results
    cy.get('[data-testid="search-query-display"]').should('contain', searchQuery);
  });

  it('should filter search results by time range', () => {
    // Open time range selector
    cy.get('[data-testid="time-range-selector"]').click();
    
    // Select last 15 minutes
    cy.get('[data-testid="time-range-option-15m"]').click();
    
    // Perform search
    cy.get('textarea[aria-label="Search query"]').clear().type('*');
    cy.get('button').contains('Search').click();
    
    // Verify time range is applied
    cy.get('[data-testid="applied-time-range"]').should('contain', '15 minutes');
    
    // Verify search results are displayed
    cy.get('[data-testid="search-results"]').should('exist');
  });

  it('should use advanced query syntax', () => {
    const advancedQuery = 'source="nginx" level="error" | stats count by host';
    
    // Enter advanced search query
    cy.get('textarea[aria-label="Search query"]').clear().type(advancedQuery);
    
    // Click search button
    cy.get('button').contains('Search').click();
    
    // Verify search results are displayed
    cy.get('[data-testid="search-results"]').should('exist');
    
    // Verify stats results are displayed
    cy.get('[data-testid="stats-results"]').should('exist');
    cy.get('[data-testid="stats-results"]').find('table').should('exist');
    cy.get('[data-testid="stats-results"]').find('th').should('contain', 'host');
    cy.get('[data-testid="stats-results"]').find('th').should('contain', 'count');
  });

  it('should save search query', () => {
    const searchQuery = 'level="error"';
    const savedQueryName = 'Test Saved Query';
    
    // Enter search query
    cy.get('textarea[aria-label="Search query"]').clear().type(searchQuery);
    
    // Click save button
    cy.get('[data-testid="save-query-button"]').click();
    
    // Enter saved query name
    cy.get('[data-testid="saved-query-name-input"]').type(savedQueryName);
    
    // Save the query
    cy.get('[data-testid="confirm-save-query-button"]').click();
    
    // Verify success message
    cy.get('[data-testid="toast-success"]').should('exist');
    cy.get('[data-testid="toast-success"]').should('contain', 'Query saved successfully');
    
    // Verify saved query appears in saved queries list
    cy.get('[data-testid="saved-queries-dropdown"]').click();
    cy.get('[data-testid="saved-query-item"]').should('contain', savedQueryName);
  });

  it('should load saved search query', () => {
    const savedQueryName = 'Test Saved Query';
    
    // Open saved queries dropdown
    cy.get('[data-testid="saved-queries-dropdown"]').click();
    
    // Click on saved query
    cy.get('[data-testid="saved-query-item"]').contains(savedQueryName).click();
    
    // Verify query is loaded in search input
    cy.get('textarea[aria-label="Search query"]').should('have.value', 'level="error"');
    
    // Verify search is executed
    cy.get('[data-testid="search-results"]').should('exist');
  });

  it('should display log details when clicking on a log entry', () => {
    // Perform search
    cy.get('textarea[aria-label="Search query"]').clear().type('error');
    cy.get('button').contains('Search').click();
    
    // Wait for results
    cy.get('[data-testid="search-results"]').should('exist');
    
    // Click on first log entry
    cy.get('[data-testid="log-entry"]').first().click();
    
    // Verify log details panel is displayed
    cy.get('[data-testid="log-details-panel"]').should('exist');
    cy.get('[data-testid="log-details-panel"]').should('contain', 'Log Details');
    
    // Verify fields are displayed
    cy.get('[data-testid="log-field-timestamp"]').should('exist');
    cy.get('[data-testid="log-field-level"]').should('exist');
    cy.get('[data-testid="log-field-message"]').should('exist');
  });
});