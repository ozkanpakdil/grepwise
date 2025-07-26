package io.github.ozkanpakdil.grepwise.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the state of the cluster.
 */
public class ClusterState {
    private String leaderId;
    private List<ClusterNode> nodes;

    /**
     * Default constructor.
     */
    public ClusterState() {
        this.nodes = new ArrayList<>();
    }

    /**
     * Constructor with all fields.
     *
     * @param leaderId The ID of the leader node
     * @param nodes The list of nodes in the cluster
     */
    public ClusterState(String leaderId, List<ClusterNode> nodes) {
        this.leaderId = leaderId;
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    /**
     * Get the ID of the leader node.
     *
     * @return The ID of the leader node
     */
    public String getLeaderId() {
        return leaderId;
    }

    /**
     * Set the ID of the leader node.
     *
     * @param leaderId The ID of the leader node
     */
    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    /**
     * Get the list of nodes in the cluster.
     *
     * @return The list of nodes in the cluster
     */
    public List<ClusterNode> getNodes() {
        return nodes;
    }

    /**
     * Set the list of nodes in the cluster.
     *
     * @param nodes The list of nodes in the cluster
     */
    public void setNodes(List<ClusterNode> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    /**
     * Get the number of active nodes in the cluster.
     *
     * @return The number of active nodes
     */
    public int getActiveNodeCount() {
        if (nodes == null) {
            return 0;
        }
        return (int) nodes.stream().filter(ClusterNode::isActive).count();
    }

    /**
     * Check if the cluster has a leader.
     *
     * @return true if the cluster has a leader, false otherwise
     */
    public boolean hasLeader() {
        return leaderId != null && !leaderId.isEmpty();
    }

    /**
     * Get the leader node.
     *
     * @return The leader node, or null if there is no leader
     */
    public ClusterNode getLeaderNode() {
        if (!hasLeader() || nodes == null) {
            return null;
        }
        return nodes.stream()
                .filter(node -> Objects.equals(node.getId(), leaderId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterState that = (ClusterState) o;
        return Objects.equals(leaderId, that.leaderId) &&
                Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leaderId, nodes);
    }

    @Override
    public String toString() {
        return "ClusterState{" +
                "leaderId='" + leaderId + '\'' +
                ", nodes=" + nodes +
                ", activeNodeCount=" + getActiveNodeCount() +
                '}';
    }
}