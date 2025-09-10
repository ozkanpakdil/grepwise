package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.repository.RedactionConfigRepository;
import io.github.ozkanpakdil.grepwise.service.RedactionConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "Redaction Config", description = "Manage redaction keywords and regex patterns")
@RestController
@RequestMapping("/api/redaction")
@CrossOrigin(origins = "*")
public class RedactionController {
    private final RedactionConfigRepository repo;
    private final RedactionConfigService service;

    public RedactionController(RedactionConfigRepository repo, RedactionConfigService service) {
        this.repo = repo;
        this.service = service;
    }

    @Operation(summary = "Get redaction keys")
    @GetMapping("/keys")
    public ResponseEntity<Set<String>> getKeys() {
        return ResponseEntity.ok(repo.getKeys());
    }

    @Operation(summary = "Set redaction keys (will persist and include defaults)")
    @PostMapping("/keys")
    public ResponseEntity<Set<String>> setKeys(@RequestBody Map<String, List<String>> body) {
        List<String> keys = body.get("keys");
        repo.setConfig(keys, repo.getPatterns());
        service.reload();
        return ResponseEntity.ok(repo.getKeys());
    }

    @Operation(summary = "Get redaction config (both flat and grouped)")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "keys", repo.getKeys(),
                "patterns", repo.getPatterns(),
                "groups", repo.getGroupedConfig()
        ));
    }

    @Operation(summary = "Set redaction config (grouped format only)")
    @PostMapping("/config")
    public ResponseEntity<?> setConfig(@RequestBody Map<String, Object> body) {
        repo.setGroupedConfig(body);
        service.reload();
        return getConfig();
    }

    @Operation(summary = "Reload redaction config from file")
    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reload() {
        service.reload();
        return ResponseEntity.ok(Map.of("status", "reloaded"));
    }
}
