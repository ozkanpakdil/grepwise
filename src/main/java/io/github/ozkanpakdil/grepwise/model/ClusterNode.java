package io.github.ozkanpakdil.grepwise.model;

import java.util.Objects;

/**
 * Represents a node in the cluster.
 */
public class ClusterNode {
    private String id;
    private String url;
    private long lastHeartbeat;
    private boolean active;

    /**
     * Default constructor for serialization.
     */
    public ClusterNode() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id            The node ID
     * @param url           The node URL
     * @param lastHeartbeat The timestamp of the last heartbeat
     * @param active        Whether the node is active
     */
    public ClusterNode(String id, String url, long lastHeartbeat, boolean active) {
        this.id = id;
        this.url = url;
        this.lastHeartbeat = lastHeartbeat;
        this.active = active;
    }

    /**
     * Get the node ID.
     *
     * @return The node ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the node ID.
     *
     * @param id The node ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the node URL.
     *
     * @return The node URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the node URL.
     *
     * @param url The node URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the timestamp of the last heartbeat.
     *
     * @return The timestamp of the last heartbeat
     */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    /**
     * Set the timestamp of the last heartbeat.
     *
     * @param lastHeartbeat The timestamp of the last heartbeat
     */
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    /**
     * Check if the node is active.
     *
     * @return true if the node is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set whether the node is active.
     *
     * @param active Whether the node is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterNode that = (ClusterNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ClusterNode{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", lastHeartbeat=" + lastHeartbeat +
                ", active=" + active +
                '}';
    }
}