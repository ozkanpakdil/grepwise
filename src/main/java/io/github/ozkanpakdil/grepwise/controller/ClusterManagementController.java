package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.ClusterState;
import io.github.ozkanpakdil.grepwise.service.HighAvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for cluster management operations.
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ClusterManagementController.class);

    @Autowired
    private HighAvailabilityService highAvailabilityService;

    /**
     * Get the current cluster state.
     *
     * @return The cluster state
     */
    @GetMapping("/state")
    public ResponseEntity<ClusterState> getClusterState() {
        if (!highAvailabilityService.isHighAvailabilityEnabled()) {
            return ResponseEntity.ok(new ClusterState());
        }
        return ResponseEntity.ok(highAvailabilityService.getClusterState());
    }

    /**
     * Enable or disable high availability.
     *
     * @param enabled Whether high availability should be enabled
     * @return A response with the current status
     */
    @PostMapping("/high-availability")
    public ResponseEntity<Map<String, Object>> setHighAvailabilityEnabled(@RequestParam boolean enabled) {
        highAvailabilityService.setHighAvailabilityEnabled(enabled);

        Map<String, Object> response = new HashMap<>();
        response.put("highAvailabilityEnabled", highAvailabilityService.isHighAvailabilityEnabled());

        return ResponseEntity.ok(response);
    }

    /**
     * Handle a heartbeat from another node.
     *
     * @param heartbeatData The heartbeat data
     * @return A response indicating success
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> handleHeartbeat(@RequestBody Map<String, Object> heartbeatData) {
        if (!highAvailabilityService.isHighAvailabilityEnabled()) {
            return ResponseEntity.ok().build();
        }

        String nodeId = (String) heartbeatData.get("nodeId");
        String nodeUrl = (String) heartbeatData.get("nodeUrl");
        long timestamp = ((Number) heartbeatData.get("timestamp")).longValue();
        boolean isLeader = (boolean) heartbeatData.get("isLeader");

        highAvailabilityService.handleHeartbeat(nodeId, nodeUrl, timestamp, isLeader);

        return ResponseEntity.ok().build();
    }

    /**
     * Handle a notification that a node is leaving the cluster.
     *
     * @param data The data containing the leaving node ID
     * @return A response indicating success
     */
    @PostMapping("/node-leaving")
    public ResponseEntity<Void> handleNodeLeaving(@RequestBody Map<String, String> data) {
        if (!highAvailabilityService.isHighAvailabilityEnabled()) {
            return ResponseEntity.ok().build();
        }

        String nodeId = data.get("nodeId");
        highAvailabilityService.handleNodeLeaving(nodeId);

        return ResponseEntity.ok().build();
    }

    /**
     * Handle a leader change notification.
     *
     * @param clusterState The new cluster state
     * @return A response indicating success
     */
    @PostMapping("/leader-change")
    public ResponseEntity<Void> handleLeaderChange(@RequestBody ClusterState clusterState) {
        if (!highAvailabilityService.isHighAvailabilityEnabled()) {
            return ResponseEntity.ok().build();
        }

        highAvailabilityService.handleLeaderChange(clusterState);

        return ResponseEntity.ok().build();
    }

    /**
     * Get statistics about the cluster.
     *
     * @return A map of statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getClusterStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("highAvailabilityEnabled", highAvailabilityService.isHighAvailabilityEnabled());
        stats.put("nodeId", highAvailabilityService.getNodeId());
        stats.put("isLeader", highAvailabilityService.isLeader());
        stats.put("leaderId", highAvailabilityService.getCurrentLeaderId());
        stats.put("nodeCount", highAvailabilityService.getClusterNodes().size());

        return ResponseEntity.ok(stats);
    }
}