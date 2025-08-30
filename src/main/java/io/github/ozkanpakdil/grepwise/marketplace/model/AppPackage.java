package io.github.ozkanpakdil.grepwise.marketplace.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an application package in the marketplace.
 * An app package contains metadata about a plugin or extension that can be installed.
 */
public class AppPackage {

    private String id;
    private String name;
    private String description;
    private String version;
    private String author;
    private String website;
    private String repositoryUrl;
    private String iconUrl;
    private LocalDateTime publishedDate;
    private LocalDateTime lastUpdatedDate;
    private long downloadCount;
    private double averageRating;
    private Set<String> tags = new HashSet<>();
    private Set<String> categories = new HashSet<>();
    private Set<AppDependency> dependencies = new HashSet<>();
    private AppPackageType type;
    private AppPackageStatus status;

    /**
     * Default constructor.
     */
    public AppPackage() {
    }

    /**
     * Constructor with essential fields.
     *
     * @param id          The unique identifier for this app package
     * @param name        The name of this app package
     * @param description A description of this app package
     * @param version     The version of this app package
     * @param author      The author of this app package
     */
    public AppPackage(String id, String name, String description, String version, String author) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.publishedDate = LocalDateTime.now();
        this.lastUpdatedDate = LocalDateTime.now();
        this.type = AppPackageType.PLUGIN;
        this.status = AppPackageStatus.PENDING_REVIEW;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public Set<AppDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<AppDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public AppPackageType getType() {
        return type;
    }

    public void setType(AppPackageType type) {
        this.type = type;
    }

    public AppPackageStatus getStatus() {
        return status;
    }

    public void setStatus(AppPackageStatus status) {
        this.status = status;
    }

    /**
     * Increments the download count by 1.
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    /**
     * Adds a tag to this app package.
     *
     * @param tag The tag to add
     * @return true if the tag was added, false if it was already present
     */
    public boolean addTag(String tag) {
        return tags.add(tag);
    }

    /**
     * Adds a category to this app package.
     *
     * @param category The category to add
     * @return true if the category was added, false if it was already present
     */
    public boolean addCategory(String category) {
        return categories.add(category);
    }

    /**
     * Adds a dependency to this app package.
     *
     * @param dependency The dependency to add
     * @return true if the dependency was added, false if it was already present
     */
    public boolean addDependency(AppDependency dependency) {
        return dependencies.add(dependency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppPackage that = (AppPackage) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AppPackage{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}