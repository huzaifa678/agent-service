package com.project.agent.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Kubernetes liveness/readiness probe endpoints under {@code /healthz}.
 * The Deployment/Rollout probes hit {@code /healthz/live} and {@code /healthz/ready}.
 */
@RestController
@RequestMapping("/healthz")
public class HealthController {

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
