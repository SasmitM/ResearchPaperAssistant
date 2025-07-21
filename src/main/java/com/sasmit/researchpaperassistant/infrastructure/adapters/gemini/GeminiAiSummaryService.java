package com.sasmit.researchpaperassistant.infrastructure.adapters.gemini;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis.DifficultyLevel;
import com.sasmit.researchpaperassistant.core.ports.out.AiSummaryService;
import com.sasmit.researchpaperassistant.infrastructure.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "app.features.use-mock-openai", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class GeminiAiSummaryService implements AiSummaryService {

    private final GeminiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    @Override
    @Cacheable(value = "summaries", key = "#abstractText.hashCode()")
    public String summarizeAbstract(String abstractText) {
        log.info("🤖 Calling Gemini API to summarize abstract");

        String prompt = """
                You are an AI assistant helping students understand research papers.
                
                Summarize the following research paper abstract in a way that's easy for undergraduate students to understand.
                Use simple language, explain technical terms, and highlight the main contributions.
                Keep it under 200 words.
                
                Abstract:
                %s
                
                Student-Friendly Summary:
                """.formatted(abstractText);

        try {
            String response = callGeminiApi(prompt);
            log.info("✅ Successfully generated abstract summary");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to generate summary", e);
            return "Error generating summary: " + e.getMessage();
        }
    }

    @Override
    @Cacheable(value = "fullSummaries", key = "#fullText.hashCode()")
    public String summarizePaper(String fullText) {
        log.info("🤖 Calling Gemini API to summarize full paper");

        // Truncate if too long (Gemini has token limits)
        String truncatedText = fullText.length() > 15000 ?
                fullText.substring(0, 15000) + "..." : fullText;

        String prompt = """
                You are an AI assistant helping students understand research papers.
                
                Create a comprehensive summary of this research paper for students.
                Structure it with:
                1. Main Idea (1-2 sentences)
                2. Key Contributions (bullet points)
                3. Methodology (simplified explanation)
                4. Results (what they found)
                5. Why It Matters (real-world impact)
                
                Keep the language accessible to undergraduate students.
                
                Paper Text:
                %s
                
                Structured Summary:
                """.formatted(truncatedText);

        try {
            String response = callGeminiApi(prompt);
            log.info("✅ Successfully generated full paper summary");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to generate summary", e);
            return "Error generating summary: " + e.getMessage();
        }
    }

    @Override
    public DifficultyLevel estimateDifficulty(String text) {
        log.info("🤖 Estimating paper difficulty with Gemini");

        String truncatedText = text.length() > 5000 ?
                text.substring(0, 5000) + "..." : text;

        String prompt = """
                Analyze the difficulty level of this research paper for students.
                Consider: mathematical complexity, required background knowledge, technical jargon, and concept density.
                
                Respond with ONLY ONE of these levels:
                - BEGINNER (undergraduate can understand with basic knowledge)
                - INTERMEDIATE (requires some domain knowledge)
                - ADVANCED (requires significant expertise)
                - EXPERT (cutting-edge research level)
                
                Paper excerpt:
                %s
                
                Difficulty Level:
                """.formatted(truncatedText);

        try {
            String response = callGeminiApi(prompt).trim().toUpperCase();

            // Parse response
            if (response.contains("BEGINNER")) return DifficultyLevel.BEGINNER;
            if (response.contains("INTERMEDIATE")) return DifficultyLevel.INTERMEDIATE;
            if (response.contains("ADVANCED")) return DifficultyLevel.ADVANCED;
            if (response.contains("EXPERT")) return DifficultyLevel.EXPERT;

            // Default based on text complexity
            return estimateDifficultyFallback(text);

        } catch (Exception e) {
            log.error("Failed to estimate difficulty", e);
            return estimateDifficultyFallback(text);
        }
    }

    @Override
    public int estimateReadingTime(String text) {
        // Simple calculation: average reading speed is 200-250 words per minute
        // Academic papers might be slower, so use 150 wpm
        int wordCount = text.split("\\s+").length;
        int minutes = Math.max(5, wordCount / 150);

        // Round to nearest 5 minutes
        return ((minutes + 4) / 5) * 5;
    }

    private String callGeminiApi(String prompt) {
        String url = properties.getApiUrl() + properties.getModel() +
                ":generateContent?key=" + properties.getApiKey();

        // Build request body
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", properties.getTemperature());
        generationConfig.addProperty("maxOutputTokens", properties.getMaxTokens());
        requestBody.add("generationConfig", generationConfig);

        // Make request using RestTemplate
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(gson.toJson(requestBody), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Gemini API error: " + response.getStatusCode());
            }

            // Parse response
            JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);

            return jsonResponse
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }

    private DifficultyLevel estimateDifficultyFallback(String text) {
        // Simple heuristic fallback
        int length = text.length();
        if (length < 10000) return DifficultyLevel.BEGINNER;
        if (length < 20000) return DifficultyLevel.INTERMEDIATE;
        if (length < 40000) return DifficultyLevel.ADVANCED;
        return DifficultyLevel.EXPERT;
    }
}