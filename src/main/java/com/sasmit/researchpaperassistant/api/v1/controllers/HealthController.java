package com.sasmit.researchpaperassistant.api.v1.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Simple health check controller to verify if the service is running.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Health check endpoint.
     *
     * @return A map containing the health status and application name.
     */

    @GetMapping
    @Operation(summary = "Health check", description = "Check if the service is running")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "application", applicationName,
                "timestamp", LocalDateTime.now()
        );
    }
}