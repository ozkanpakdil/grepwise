package io.github.ozkanpakdil.grepwise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

/**
 * Configuration for LDAP authentication.
 * This class configures the LDAP connection and authentication providers.
 * LDAP authentication is only enabled if grepwise.ldap.enabled is set to true.
 */
@Configuration
public class LdapConfig {

    @Value("${grepwise.ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${grepwise.ldap.url:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${grepwise.ldap.base-dn:dc=example,dc=com}")
    private String ldapBaseDn;

    @Value("${grepwise.ldap.user-dn-pattern:uid={0},ou=people}")
    private String ldapUserDnPattern;

    @Value("${grepwise.ldap.manager-dn:}")
    private String ldapManagerDn;

    @Value("${grepwise.ldap.manager-password:}")
    private String ldapManagerPassword;

    @Value("${grepwise.ldap.user-search-base:ou=people}")
    private String ldapUserSearchBase;

    @Value("${grepwise.ldap.user-search-filter:(uid={0})}")
    private String ldapUserSearchFilter;

    @Value("${grepwise.ldap.group-search-base:ou=groups}")
    private String ldapGroupSearchBase;

    @Value("${grepwise.ldap.group-search-filter:(member={0})}")
    private String ldapGroupSearchFilter;

    @Value("${grepwise.ldap.group-role-attribute:cn}")
    private String ldapGroupRoleAttribute;

    /**
     * Creates an LDAP context source for connecting to the LDAP server.
     *
     * @return The LDAP context source
     */
    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldapUrl);
        contextSource.setBase(ldapBaseDn);
        
        // Set manager credentials if provided
        if (ldapManagerDn != null && !ldapManagerDn.isEmpty()) {
            contextSource.setUserDn(ldapManagerDn);
            contextSource.setPassword(ldapManagerPassword);
        }
        
        return contextSource;
    }

    /**
     * Creates an LDAP template for LDAP operations.
     *
     * @return The LDAP template
     */
    @Bean
    @ConditionalOnProperty(name = "grepwise.ldap.enabled", havingValue = "true")
    public LdapTemplate ldapTemplate() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBaseDn);
        
        // Set manager credentials if provided
        if (ldapManagerDn != null && !ldapManagerDn.isEmpty()) {
            contextSource.setUserDn(ldapManagerDn);
            contextSource.setPassword(ldapManagerPassword);
        }
        
        contextSource.afterPropertiesSet();
        
        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        return ldapTemplate;
    }

    /**
     * Creates an LDAP authenticator for authenticating users.
     *
     * @return The LDAP authenticator
     */
    @Bean
    public LdapAuthenticator ldapAuthenticator() {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource());
        
        // Configure user search
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
            ldapUserSearchBase, 
            ldapUserSearchFilter, 
            contextSource()
        );
        authenticator.setUserSearch(userSearch);
        
        return authenticator;
    }

    /**
     * Creates an LDAP authorities populator for retrieving user roles from LDAP.
     *
     * @return The LDAP authorities populator
     */
    @Bean
    public LdapAuthoritiesPopulator ldapAuthoritiesPopulator() {
        DefaultLdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
            contextSource(), 
            ldapGroupSearchBase
        );
        
        authoritiesPopulator.setGroupSearchFilter(ldapGroupSearchFilter);
        authoritiesPopulator.setGroupRoleAttribute(ldapGroupRoleAttribute);
        authoritiesPopulator.setRolePrefix("ROLE_");
        
        return authoritiesPopulator;
    }

    /**
     * Creates an LDAP authentication provider for authenticating users against LDAP.
     *
     * @return The LDAP authentication provider
     */
    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(
            ldapAuthenticator(), 
            ldapAuthoritiesPopulator()
        );
        
        return provider;
    }

    /**
     * Checks if LDAP authentication is enabled.
     *
     * @return true if LDAP authentication is enabled, false otherwise
     */
    public boolean isLdapEnabled() {
        return ldapEnabled;
    }
}