package com.sasmit.researchpaperassistant.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.gemini")
@Data
public class GeminiProperties {
    private String apiKey;
    private String model = "gemini-pro";
    private int maxTokens = 2048;
    private double temperature = 0.7;
    private String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/";
}