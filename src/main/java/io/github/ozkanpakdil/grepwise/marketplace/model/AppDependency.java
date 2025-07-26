package io.github.ozkanpakdil.grepwise.marketplace.model;

import java.util.Objects;

/**
 * Represents a dependency between app packages in the marketplace.
 * An app package can depend on other app packages with specific version requirements.
 */
public class AppDependency {
    
    private String packageId;
    private String versionRequirement;
    private boolean optional;
    
    /**
     * Default constructor.
     */
    public AppDependency() {
    }
    
    /**
     * Constructor with essential fields.
     * 
     * @param packageId The ID of the package that is required
     * @param versionRequirement The version requirement (e.g., ">=1.0.0", "1.2.x")
     * @param optional Whether this dependency is optional
     */
    public AppDependency(String packageId, String versionRequirement, boolean optional) {
        this.packageId = packageId;
        this.versionRequirement = versionRequirement;
        this.optional = optional;
    }
    
    /**
     * Constructor for a required dependency.
     * 
     * @param packageId The ID of the package that is required
     * @param versionRequirement The version requirement (e.g., ">=1.0.0", "1.2.x")
     */
    public AppDependency(String packageId, String versionRequirement) {
        this(packageId, versionRequirement, false);
    }
    
    /**
     * Gets the ID of the package that is required.
     * 
     * @return The package ID
     */
    public String getPackageId() {
        return packageId;
    }
    
    /**
     * Sets the ID of the package that is required.
     * 
     * @param packageId The package ID
     */
    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }
    
    /**
     * Gets the version requirement.
     * 
     * @return The version requirement
     */
    public String getVersionRequirement() {
        return versionRequirement;
    }
    
    /**
     * Sets the version requirement.
     * 
     * @param versionRequirement The version requirement
     */
    public void setVersionRequirement(String versionRequirement) {
        this.versionRequirement = versionRequirement;
    }
    
    /**
     * Checks if this dependency is optional.
     * 
     * @return true if this dependency is optional, false otherwise
     */
    public boolean isOptional() {
        return optional;
    }
    
    /**
     * Sets whether this dependency is optional.
     * 
     * @param optional Whether this dependency is optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    /**
     * Checks if the given version satisfies the version requirement.
     * 
     * @param version The version to check
     * @return true if the version satisfies the requirement, false otherwise
     */
    public boolean isSatisfiedBy(String version) {
        // Simple implementation for now
        // In a real implementation, this would use semantic versioning rules
        if (versionRequirement == null || version == null) {
            return false;
        }
        
        // Handle wildcard versions (e.g., "1.2.x")
        if (versionRequirement.endsWith(".x")) {
            String prefix = versionRequirement.substring(0, versionRequirement.length() - 2);
            return version.startsWith(prefix);
        }
        
        // Handle greater than or equal to (e.g., ">=1.0.0")
        if (versionRequirement.startsWith(">=")) {
            String minVersion = versionRequirement.substring(2);
            return compareVersions(version, minVersion) >= 0;
        }
        
        // Handle less than or equal to (e.g., "<=1.0.0")
        if (versionRequirement.startsWith("<=")) {
            String maxVersion = versionRequirement.substring(2);
            return compareVersions(version, maxVersion) <= 0;
        }
        
        // Handle exact version match
        return versionRequirement.equals(version);
    }
    
    /**
     * Compares two version strings.
     * 
     * @param version1 The first version
     * @param version2 The second version
     * @return A negative integer, zero, or a positive integer as the first version is less than, equal to, or greater than the second
     */
    private int compareVersions(String version1, String version2) {
        // Simple implementation for now
        // In a real implementation, this would use semantic versioning rules
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int length = Math.min(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int part1 = Integer.parseInt(parts1[i]);
            int part2 = Integer.parseInt(parts2[i]);
            
            if (part1 < part2) {
                return -1;
            }
            if (part1 > part2) {
                return 1;
            }
        }
        
        return Integer.compare(parts1.length, parts2.length);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AppDependency that = (AppDependency) o;
        
        if (optional != that.optional) return false;
        if (!Objects.equals(packageId, that.packageId)) return false;
        return Objects.equals(versionRequirement, that.versionRequirement);
    }
    
    @Override
    public int hashCode() {
        int result = packageId != null ? packageId.hashCode() : 0;
        result = 31 * result + (versionRequirement != null ? versionRequirement.hashCode() : 0);
        result = 31 * result + (optional ? 1 : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "AppDependency{" +
                "packageId='" + packageId + '\'' +
                ", versionRequirement='" + versionRequirement + '\'' +
                ", optional=" + optional +
                '}';
    }
}