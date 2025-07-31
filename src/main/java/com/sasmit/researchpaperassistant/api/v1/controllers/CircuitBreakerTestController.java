package com.sasmit.researchpaperassistant.api.v1.controllers;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/circuitbreaker")
@RequiredArgsConstructor
public class CircuitBreakerTestController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping
    public Map<String, Object> getCircuitBreakerDetails() {
        Map<String, Object> response = new HashMap<>();

        // Fetch all CircuitBreakers from the registry
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            Map<String, Object> circuitBreakerDetails = new HashMap<>();

            // Fetch name and state
            CircuitBreaker.State state = circuitBreaker.getState();
            circuitBreakerDetails.put("state", state.name());

            // Fetch Metrics
            Metrics metrics = circuitBreaker.getMetrics();
            circuitBreakerDetails.put("failureRate", metrics.getFailureRate());
            circuitBreakerDetails.put("slowCallRate", metrics.getSlowCallRate());
            circuitBreakerDetails.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
            circuitBreakerDetails.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            circuitBreakerDetails.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
            circuitBreakerDetails.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());

            // Add to response
            response.put(circuitBreaker.getName(), circuitBreakerDetails);
        });

        return response;
    }
}