// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

// Declare global Cypress namespace to add custom commands
declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to login to the application
       * @example cy.login('admin', 'password')
       */
      login(username: string, password: string): Chainable<Element>;
      
      /**
       * Custom command to navigate to the search page
       * @example cy.navigateToSearch()
       */
      navigateToSearch(): Chainable<Element>;
      
      /**
       * Custom command to navigate to the dashboards page
       * @example cy.navigateToDashboards()
       */
      navigateToDashboards(): Chainable<Element>;
      
      /**
       * Custom command to navigate to the alarms page
       * @example cy.navigateToAlarms()
       */
      navigateToAlarms(): Chainable<Element>;
      
      /**
       * Custom command to perform a log search
       * @example cy.performSearch('error')
       */
      performSearch(query: string): Chainable<Element>;
    }
  }
}

// Login command
Cypress.Commands.add('login', (username: string, password: string) => {
  cy.visit('/login');
  cy.get('input[name="username"]').type(username);
  cy.get('input[name="password"]').type(password);
  cy.get('button[type="submit"]').click();
  // Wait for login to complete and redirect
  cy.url().should('not.include', '/login');
});

// Navigation commands
Cypress.Commands.add('navigateToSearch', () => {
  cy.visit('/search');
  cy.url().should('include', '/search');
  // Wait for page to load
  cy.get('h1').should('contain', 'Search');
});

Cypress.Commands.add('navigateToDashboards', () => {
  cy.visit('/dashboards');
  cy.url().should('include', '/dashboards');
  // Wait for page to load
  cy.get('h1').should('contain', 'Dashboards');
});

Cypress.Commands.add('navigateToAlarms', () => {
  cy.visit('/alarms');
  cy.url().should('include', '/alarms');
  // Wait for page to load
  cy.get('h1').should('contain', 'Alarms');
});

// Search command
Cypress.Commands.add('performSearch', (query: string) => {
  cy.navigateToSearch();
  cy.get('textarea[aria-label="Search query"]').clear().type(query);
  cy.get('button').contains('Search').click();
  // Wait for search results to load
  cy.get('[data-testid="search-results"]').should('exist');
});