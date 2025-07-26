package io.github.ozkanpakdil.grepwise.marketplace.service;

import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackage;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageStatus;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageType;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing the app marketplace.
 * This service provides methods for submitting, approving, searching, and installing app packages.
 */
public interface AppMarketplaceService {
    
    /**
     * Submits a new app package to the marketplace.
     * The package will be in PENDING_REVIEW status until approved.
     *
     * @param appPackage The app package to submit
     * @return The submitted app package with updated metadata
     * @throws IllegalArgumentException if the app package is invalid
     */
    AppPackage submitPackage(AppPackage appPackage);
    
    /**
     * Updates an existing app package in the marketplace.
     *
     * @param appPackage The app package to update
     * @return The updated app package
     * @throws IllegalArgumentException if the app package is invalid
     * @throws IllegalStateException if the app package does not exist
     */
    AppPackage updatePackage(AppPackage appPackage);
    
    /**
     * Retrieves an app package by its ID.
     *
     * @param packageId The ID of the app package to retrieve
     * @return The app package, or empty if not found
     */
    Optional<AppPackage> getPackage(String packageId);
    
    /**
     * Lists all app packages in the marketplace.
     *
     * @return A list of all app packages
     */
    List<AppPackage> getAllPackages();
    
    /**
     * Lists app packages of a specific type.
     *
     * @param type The type of app packages to list
     * @return A list of app packages of the specified type
     */
    List<AppPackage> getPackagesByType(AppPackageType type);
    
    /**
     * Lists app packages with a specific status.
     *
     * @param status The status of app packages to list
     * @return A list of app packages with the specified status
     */
    List<AppPackage> getPackagesByStatus(AppPackageStatus status);
    
    /**
     * Searches for app packages by name, description, or tags.
     *
     * @param query The search query
     * @return A list of app packages matching the query
     */
    List<AppPackage> searchPackages(String query);
    
    /**
     * Changes the status of an app package.
     *
     * @param packageId The ID of the app package
     * @param status The new status
     * @return The updated app package
     * @throws IllegalStateException if the app package does not exist
     */
    AppPackage changePackageStatus(String packageId, AppPackageStatus status);
    
    /**
     * Installs an app package.
     * This will download and install the package, and register any plugins it contains.
     *
     * @param packageId The ID of the app package to install
     * @return true if the installation was successful, false otherwise
     * @throws IllegalStateException if the app package does not exist or is not approved
     */
    boolean installPackage(String packageId);
    
    /**
     * Uninstalls an app package.
     * This will unregister any plugins it contains and remove the package files.
     *
     * @param packageId The ID of the app package to uninstall
     * @return true if the uninstallation was successful, false otherwise
     * @throws IllegalStateException if the app package is not installed
     */
    boolean uninstallPackage(String packageId);
    
    /**
     * Checks if an app package is installed.
     *
     * @param packageId The ID of the app package
     * @return true if the app package is installed, false otherwise
     */
    boolean isPackageInstalled(String packageId);
    
    /**
     * Lists all installed app packages.
     *
     * @return A list of installed app packages
     */
    List<AppPackage> getInstalledPackages();
    
    /**
     * Checks for updates to installed app packages.
     *
     * @return A list of app packages that have updates available
     */
    List<AppPackage> checkForUpdates();
    
    /**
     * Updates an installed app package to the latest version.
     *
     * @param packageId The ID of the app package to update
     * @return true if the update was successful, false otherwise
     * @throws IllegalStateException if the app package is not installed
     */
    boolean updateInstalledPackage(String packageId);
}