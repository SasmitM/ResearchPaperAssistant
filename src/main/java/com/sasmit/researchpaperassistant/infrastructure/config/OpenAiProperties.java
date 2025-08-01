package com.sasmit.researchpaperassistant.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.openai") // Map these properties from "application.yml"
@Data
public class OpenAiProperties {
    private String apiKey; // OpenAI API key
    private String model; // Default model
    private int maxTokens; // Default max tokens
    private double temperature;
    private String apiUrl;
}