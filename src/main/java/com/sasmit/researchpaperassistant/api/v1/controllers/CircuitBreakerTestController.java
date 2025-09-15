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

/**
 * Controller to expose circuit breaker details for monitoring and debugging.
 */
@RestController
@RequestMapping("/api/v1/circuitbreaker")
@RequiredArgsConstructor
public class CircuitBreakerTestController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get details of all circuit breakers.
     *
     * @return A map containing the state and metrics of each circuit breaker.
     */
    @GetMapping
    public Map<String, Object> getCircuitBreakerDetails() {
        Map<String, Object> response = new HashMap<>();


        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            Map<String, Object> circuitBreakerDetails = new HashMap<>();


            CircuitBreaker.State state = circuitBreaker.getState();
            circuitBreakerDetails.put("state", state.name());


            Metrics metrics = circuitBreaker.getMetrics();
            circuitBreakerDetails.put("failureRate", metrics.getFailureRate());
            circuitBreakerDetails.put("slowCallRate", metrics.getSlowCallRate());
            circuitBreakerDetails.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
            circuitBreakerDetails.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            circuitBreakerDetails.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
            circuitBreakerDetails.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());


            response.put(circuitBreaker.getName(), circuitBreakerDetails);
        });

        return response;
    }
}