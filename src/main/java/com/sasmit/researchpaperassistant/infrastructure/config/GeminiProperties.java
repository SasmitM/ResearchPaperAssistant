package com.sasmit.researchpaperassistant.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Gemini AI integration
 * Loaded from application.yml or application.properties with prefix "app.gemini"
 */
@Component
@ConfigurationProperties(prefix = "app.gemini")
@Data
public class GeminiProperties {
    private String apiKey;
    private String model;
    private int maxTokens;
    private double temperature;
    private String apiUrl;
}