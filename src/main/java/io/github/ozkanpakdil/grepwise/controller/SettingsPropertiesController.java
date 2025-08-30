package io.github.ozkanpakdil.grepwise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes selected application properties needed by the frontend settings page.
 */
@RestController
@RequestMapping("/api/settings/properties")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class SettingsPropertiesController {

    @Autowired
    private Environment environment;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettingsProperties() {
        boolean ldapEnabled = Boolean.parseBoolean(environment.getProperty("grepwise.ldap.enabled", "false"));
        return ResponseEntity.ok(Map.of(
                "ldapEnabled", ldapEnabled
        ));
    }
}
