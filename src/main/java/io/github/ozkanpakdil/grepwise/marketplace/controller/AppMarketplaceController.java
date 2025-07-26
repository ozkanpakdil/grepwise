package io.github.ozkanpakdil.grepwise.marketplace.controller;

import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackage;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageStatus;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageType;
import io.github.ozkanpakdil.grepwise.marketplace.service.AppMarketplaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the app marketplace.
 * This controller exposes endpoints for managing app packages in the marketplace.
 */
@RestController
@RequestMapping("/api/marketplace")
public class AppMarketplaceController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppMarketplaceController.class);
    
    private final AppMarketplaceService appMarketplaceService;
    
    @Autowired
    public AppMarketplaceController(AppMarketplaceService appMarketplaceService) {
        this.appMarketplaceService = appMarketplaceService;
    }
    
    /**
     * Submits a new app package to the marketplace.
     *
     * @param appPackage The app package to submit
     * @return The submitted app package with updated metadata
     */
    @PostMapping("/packages")
    public ResponseEntity<AppPackage> submitPackage(@RequestBody AppPackage appPackage) {
        try {
            AppPackage submittedPackage = appMarketplaceService.submitPackage(appPackage);
            return ResponseEntity.status(HttpStatus.CREATED).body(submittedPackage);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to submit app package: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error submitting app package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Updates an existing app package in the marketplace.
     *
     * @param packageId The ID of the app package to update
     * @param appPackage The updated app package
     * @return The updated app package
     */
    @PutMapping("/packages/{packageId}")
    public ResponseEntity<AppPackage> updatePackage(@PathVariable String packageId, @RequestBody AppPackage appPackage) {
        try {
            if (!packageId.equals(appPackage.getId())) {
                return ResponseEntity.badRequest().build();
            }
            
            AppPackage updatedPackage = appMarketplaceService.updatePackage(appPackage);
            return ResponseEntity.ok(updatedPackage);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to update app package: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to update app package: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating app package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves an app package by its ID.
     *
     * @param packageId The ID of the app package to retrieve
     * @return The app package, or 404 if not found
     */
    @GetMapping("/packages/{packageId}")
    public ResponseEntity<AppPackage> getPackage(@PathVariable String packageId) {
        return appMarketplaceService.getPackage(packageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lists all app packages in the marketplace.
     *
     * @return A list of all app packages
     */
    @GetMapping("/packages")
    public ResponseEntity<List<AppPackage>> getAllPackages() {
        return ResponseEntity.ok(appMarketplaceService.getAllPackages());
    }
    
    /**
     * Lists app packages of a specific type.
     *
     * @param type The type of app packages to list
     * @return A list of app packages of the specified type
     */
    @GetMapping("/packages/type/{type}")
    public ResponseEntity<List<AppPackage>> getPackagesByType(@PathVariable AppPackageType type) {
        return ResponseEntity.ok(appMarketplaceService.getPackagesByType(type));
    }
    
    /**
     * Lists app packages with a specific status.
     *
     * @param status The status of app packages to list
     * @return A list of app packages with the specified status
     */
    @GetMapping("/packages/status/{status}")
    public ResponseEntity<List<AppPackage>> getPackagesByStatus(@PathVariable AppPackageStatus status) {
        return ResponseEntity.ok(appMarketplaceService.getPackagesByStatus(status));
    }
    
    /**
     * Searches for app packages by name, description, or tags.
     *
     * @param query The search query
     * @return A list of app packages matching the query
     */
    @GetMapping("/packages/search")
    public ResponseEntity<List<AppPackage>> searchPackages(@RequestParam String query) {
        return ResponseEntity.ok(appMarketplaceService.searchPackages(query));
    }
    
    /**
     * Changes the status of an app package.
     *
     * @param packageId The ID of the app package
     * @param status The new status
     * @return The updated app package
     */
    @PatchMapping("/packages/{packageId}/status")
    public ResponseEntity<AppPackage> changePackageStatus(@PathVariable String packageId, @RequestParam AppPackageStatus status) {
        try {
            AppPackage updatedPackage = appMarketplaceService.changePackageStatus(packageId, status);
            return ResponseEntity.ok(updatedPackage);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to change app package status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to change app package status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error changing app package status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Installs an app package.
     *
     * @param packageId The ID of the app package to install
     * @return 200 if successful, error status otherwise
     */
    @PostMapping("/packages/{packageId}/install")
    public ResponseEntity<Void> installPackage(@PathVariable String packageId) {
        try {
            boolean success = appMarketplaceService.installPackage(packageId);
            return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            logger.error("Failed to install app package: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to install app package: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error installing app package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Uninstalls an app package.
     *
     * @param packageId The ID of the app package to uninstall
     * @return 200 if successful, error status otherwise
     */
    @PostMapping("/packages/{packageId}/uninstall")
    public ResponseEntity<Void> uninstallPackage(@PathVariable String packageId) {
        try {
            boolean success = appMarketplaceService.uninstallPackage(packageId);
            return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            logger.error("Failed to uninstall app package: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to uninstall app package: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error uninstalling app package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Lists all installed app packages.
     *
     * @return A list of installed app packages
     */
    @GetMapping("/packages/installed")
    public ResponseEntity<List<AppPackage>> getInstalledPackages() {
        return ResponseEntity.ok(appMarketplaceService.getInstalledPackages());
    }
    
    /**
     * Checks for updates to installed app packages.
     *
     * @return A list of app packages that have updates available
     */
    @GetMapping("/packages/updates")
    public ResponseEntity<List<AppPackage>> checkForUpdates() {
        return ResponseEntity.ok(appMarketplaceService.checkForUpdates());
    }
    
    /**
     * Updates an installed app package to the latest version.
     *
     * @param packageId The ID of the app package to update
     * @return 200 if successful, error status otherwise
     */
    @PostMapping("/packages/{packageId}/update")
    public ResponseEntity<Void> updateInstalledPackage(@PathVariable String packageId) {
        try {
            boolean success = appMarketplaceService.updateInstalledPackage(packageId);
            return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            logger.error("Failed to update app package: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to update app package: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error updating app package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}