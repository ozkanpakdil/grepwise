describe('Authentication Flow', () => {
  beforeEach(() => {
    // Clear cookies and local storage before each test
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  it('should display login page', () => {
    cy.visit('/login');
    cy.get('h1').should('contain', 'Login');
    cy.get('input[name="username"]').should('exist');
    cy.get('input[name="password"]').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });

  it('should login successfully with valid credentials', () => {
    cy.visit('/login');
    cy.get('input[name="username"]').type('admin');
    cy.get('input[name="password"]').type('password');
    cy.get('button[type="submit"]').click();
    
    // Verify successful login
    cy.url().should('not.include', '/login');
    cy.get('[data-testid="user-menu"]').should('exist');
  });

  it('should show error with invalid credentials', () => {
    cy.visit('/login');
    cy.get('input[name="username"]').type('invalid');
    cy.get('input[name="password"]').type('invalid');
    cy.get('button[type="submit"]').click();
    
    // Verify error message
    cy.get('[data-testid="login-error"]').should('exist');
    cy.get('[data-testid="login-error"]').should('contain', 'Invalid username or password');
    
    // Verify still on login page
    cy.url().should('include', '/login');
  });

  it('should redirect to login page when accessing protected route while not authenticated', () => {
    cy.visit('/search');
    
    // Should redirect to login
    cy.url().should('include', '/login');
  });

  it('should logout successfully', () => {
    // Login first
    cy.login('admin', 'password');
    
    // Verify logged in
    cy.get('[data-testid="user-menu"]').should('exist');
    
    // Logout
    cy.get('[data-testid="user-menu"]').click();
    cy.get('[data-testid="logout-button"]').click();
    
    // Verify logged out
    cy.url().should('include', '/login');
  });
});