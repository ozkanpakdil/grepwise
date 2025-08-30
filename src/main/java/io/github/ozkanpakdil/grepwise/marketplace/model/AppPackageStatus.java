package io.github.ozkanpakdil.grepwise.marketplace.model;

/**
 * Enum representing the different statuses of app packages in the marketplace.
 */
public enum AppPackageStatus {
    /**
     * The app package has been submitted but is awaiting review.
     */
    PENDING_REVIEW,

    /**
     * The app package has been reviewed and approved for the marketplace.
     */
    APPROVED,

    /**
     * The app package has been reviewed and rejected from the marketplace.
     */
    REJECTED,

    /**
     * The app package has been temporarily suspended from the marketplace.
     */
    SUSPENDED,

    /**
     * The app package has been permanently removed from the marketplace.
     */
    REMOVED,

    /**
     * The app package is in draft state and not yet submitted for review.
     */
    DRAFT,

    /**
     * The app package has been deprecated and will be removed in the future.
     */
    DEPRECATED
}