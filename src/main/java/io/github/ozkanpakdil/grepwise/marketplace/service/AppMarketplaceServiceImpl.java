package io.github.ozkanpakdil.grepwise.marketplace.service;

import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackage;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageStatus;
import io.github.ozkanpakdil.grepwise.marketplace.model.AppPackageType;
import io.github.ozkanpakdil.grepwise.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the AppMarketplaceService interface.
 * This service manages the app marketplace, including package submission, approval, installation, and updates.
 */
@Service
public class AppMarketplaceServiceImpl implements AppMarketplaceService {

    private static final Logger logger = LoggerFactory.getLogger(AppMarketplaceServiceImpl.class);

    // Map of package ID to app package
    private final Map<String, AppPackage> packages = new ConcurrentHashMap<>();

    // Map of package ID to installation status
    private final Map<String, Boolean> installedPackages = new ConcurrentHashMap<>();

    private final PluginRegistry pluginRegistry;

    @Autowired
    public AppMarketplaceServiceImpl(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public AppPackage submitPackage(AppPackage appPackage) {
        if (appPackage == null) {
            throw new IllegalArgumentException("App package cannot be null");
        }

        if (appPackage.getId() == null || appPackage.getId().isEmpty()) {
            throw new IllegalArgumentException("App package ID cannot be null or empty");
        }

        if (appPackage.getName() == null || appPackage.getName().isEmpty()) {
            throw new IllegalArgumentException("App package name cannot be null or empty");
        }

        if (appPackage.getVersion() == null || appPackage.getVersion().isEmpty()) {
            throw new IllegalArgumentException("App package version cannot be null or empty");
        }

        if (packages.containsKey(appPackage.getId())) {
            throw new IllegalArgumentException("App package with ID " + appPackage.getId() + " already exists");
        }

        // Set metadata
        appPackage.setPublishedDate(LocalDateTime.now());
        appPackage.setLastUpdatedDate(LocalDateTime.now());
        appPackage.setStatus(AppPackageStatus.PENDING_REVIEW);

        // Store the package
        packages.put(appPackage.getId(), appPackage);

        logger.info("App package submitted: {} ({})", appPackage.getName(), appPackage.getId());

        return appPackage;
    }

    @Override
    public AppPackage updatePackage(AppPackage appPackage) {
        if (appPackage == null) {
            throw new IllegalArgumentException("App package cannot be null");
        }

        if (appPackage.getId() == null || appPackage.getId().isEmpty()) {
            throw new IllegalArgumentException("App package ID cannot be null or empty");
        }

        if (!packages.containsKey(appPackage.getId())) {
            throw new IllegalStateException("App package with ID " + appPackage.getId() + " does not exist");
        }

        // Get the existing package
        AppPackage existingPackage = packages.get(appPackage.getId());

        // Update metadata
        appPackage.setPublishedDate(existingPackage.getPublishedDate());
        appPackage.setLastUpdatedDate(LocalDateTime.now());
        appPackage.setDownloadCount(existingPackage.getDownloadCount());
        appPackage.setAverageRating(existingPackage.getAverageRating());

        // Store the updated package
        packages.put(appPackage.getId(), appPackage);

        logger.info("App package updated: {} ({})", appPackage.getName(), appPackage.getId());

        return appPackage;
    }

    @Override
    public Optional<AppPackage> getPackage(String packageId) {
        return Optional.ofNullable(packages.get(packageId));
    }

    @Override
    public List<AppPackage> getAllPackages() {
        return new ArrayList<>(packages.values());
    }

    @Override
    public List<AppPackage> getPackagesByType(AppPackageType type) {
        return packages.values().stream()
                .filter(p -> p.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppPackage> getPackagesByStatus(AppPackageStatus status) {
        return packages.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppPackage> searchPackages(String query) {
        if (query == null || query.isEmpty()) {
            return getAllPackages();
        }

        String lowerQuery = query.toLowerCase();

        return packages.values().stream()
                .filter(p -> {
                    // Search in name
                    if (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }

                    // Search in description
                    if (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }

                    // Search in tags
                    if (p.getTags() != null) {
                        for (String tag : p.getTags()) {
                            if (tag.toLowerCase().contains(lowerQuery)) {
                                return true;
                            }
                        }
                    }

                    // Search in categories
                    if (p.getCategories() != null) {
                        for (String category : p.getCategories()) {
                            if (category.toLowerCase().contains(lowerQuery)) {
                                return true;
                            }
                        }
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }

    @Override
    public AppPackage changePackageStatus(String packageId, AppPackageStatus status) {
        if (packageId == null || packageId.isEmpty()) {
            throw new IllegalArgumentException("Package ID cannot be null or empty");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (!packages.containsKey(packageId)) {
            throw new IllegalStateException("App package with ID " + packageId + " does not exist");
        }

        // Get the existing package
        AppPackage appPackage = packages.get(packageId);

        // Update status
        appPackage.setStatus(status);
        appPackage.setLastUpdatedDate(LocalDateTime.now());

        // Store the updated package
        packages.put(packageId, appPackage);

        logger.info("App package status changed: {} ({}) -> {}", appPackage.getName(), packageId, status);

        return appPackage;
    }

    @Override
    public boolean installPackage(String packageId) {
        if (packageId == null || packageId.isEmpty()) {
            throw new IllegalArgumentException("Package ID cannot be null or empty");
        }

        if (!packages.containsKey(packageId)) {
            throw new IllegalStateException("App package with ID " + packageId + " does not exist");
        }

        AppPackage appPackage = packages.get(packageId);

        if (appPackage.getStatus() != AppPackageStatus.APPROVED) {
            throw new IllegalStateException("App package is not approved for installation");
        }

        if (isPackageInstalled(packageId)) {
            logger.warn("App package is already installed: {} ({})", appPackage.getName(), packageId);
            return true;
        }

        try {
            // In a real implementation, this would download and install the package
            // For now, we'll just mark it as installed

            // Increment download count
            appPackage.incrementDownloadCount();

            // Mark as installed
            installedPackages.put(packageId, true);

            logger.info("App package installed: {} ({})", appPackage.getName(), packageId);

            return true;
        } catch (Exception e) {
            logger.error("Failed to install app package: {} ({}): {}", appPackage.getName(), packageId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean uninstallPackage(String packageId) {
        if (packageId == null || packageId.isEmpty()) {
            throw new IllegalArgumentException("Package ID cannot be null or empty");
        }

        if (!isPackageInstalled(packageId)) {
            throw new IllegalStateException("App package is not installed");
        }

        AppPackage appPackage = packages.get(packageId);

        try {
            // In a real implementation, this would uninstall the package
            // For now, we'll just mark it as not installed

            // Mark as not installed
            installedPackages.remove(packageId);

            logger.info("App package uninstalled: {} ({})", appPackage.getName(), packageId);

            return true;
        } catch (Exception e) {
            logger.error("Failed to uninstall app package: {} ({}): {}", appPackage.getName(), packageId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isPackageInstalled(String packageId) {
        return installedPackages.getOrDefault(packageId, false);
    }

    @Override
    public List<AppPackage> getInstalledPackages() {
        return installedPackages.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> packages.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppPackage> checkForUpdates() {
        // In a real implementation, this would check for updates from a remote repository
        // For now, we'll just return an empty list
        return Collections.emptyList();
    }

    @Override
    public boolean updateInstalledPackage(String packageId) {
        if (packageId == null || packageId.isEmpty()) {
            throw new IllegalArgumentException("Package ID cannot be null or empty");
        }

        if (!isPackageInstalled(packageId)) {
            throw new IllegalStateException("App package is not installed");
        }

        // In a real implementation, this would update the package
        // For now, we'll just return true
        logger.info("App package updated: {}", packageId);
        return true;
    }
}