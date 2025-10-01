package com.sasmit.researchpaperassistant.infrastructure.adapters.openai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis.DifficultyLevel;
import com.sasmit.researchpaperassistant.core.ports.out.AiSummaryService;
import com.sasmit.researchpaperassistant.infrastructure.config.OpenAiProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of AiSummaryService using OpenAI's API.
 * Provides functionalities to summarize abstracts and full papers,
 * estimate difficulty levels, reading times, and answer questions.
 * Circuit breaker pattern applied to handle OpenAI service issues gracefully.
 */
@Service
@ConditionalOnProperty(name = "app.features.use-openai", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OpenAiSummaryService implements AiSummaryService {

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    /**
     * Generates a student-friendly summary of the given abstract text using OpenAI.
     *
     * @param abstractText The original abstract
     * @return String, Simplified summary
     */
    @Override
    @Cacheable(value = "summaries", key = "#abstractText.hashCode()")
    @CircuitBreaker(name = "openAiService", fallbackMethod = "summaryFallback")
    public String summarizeAbstract(String abstractText) {
        log.info("🤖 Calling OpenAI API to summarize abstract");

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
            String response = callOpenAiApi(prompt);
            log.info("✅ Successfully generated abstract summary");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to generate summary", e);
            return summaryFallback(abstractText, e);
        }
    }

    /**
     * Generates a comprehensive summary of the full research paper text using OpenAI.
     *
     * @param fullText The full text of the research paper
     * @return String, Detailed summary
     */
    @Override
    @Cacheable(value = "fullSummaries", key = "#fullText.hashCode()")
    @CircuitBreaker(name = "openAiService", fallbackMethod = "summaryFallback")
    public String summarizePaper(String fullText) {
        log.info("🤖 Calling OpenAI API to summarize full paper");


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
            String response = callOpenAiApi(prompt);
            log.info("✅ Successfully generated full paper summary");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to generate summary", e);
            return summaryFallback(fullText, e);
        }
    }

    /**
     * Estimates the difficulty level of a research paper using OpenAI.
     *
     * @param text The paper text
     * @return DifficultyLevel, Estimated difficulty level
     */
    @Override
    @CircuitBreaker(name = "openAiService", fallbackMethod = "difficultyFallback")
    public DifficultyLevel estimateDifficulty(String text) {
        log.info("🤖 Estimating paper difficulty with OpenAI");

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
            String response = callOpenAiApi(prompt).trim().toUpperCase();
            return mapDifficulty(response);
        } catch (Exception e) {
            log.error("Failed to estimate difficulty", e);
            return difficultyFallback(text, e);
        }
    }

    /**
     * Estimates the reading time for a research paper based on word count.
     * Assumes average reading speed of 150 words per minute.
     *
     * @param text The full text of the research paper
     * @return int, Estimated reading time in minutes (rounded to nearest 5)
     */
    @Override
    public int estimateReadingTime(String text) {
        int wordCount = text.split("\\s+").length;
        int minutes = Math.max(5, wordCount / 150);
        return ((minutes + 4) / 5) * 5; // Round to nearest 5 minutes
    }

    /**
     * Answers a specific question based on the provided paper context using OpenAI.
     *
     * @param paperContext The context of the paper
     * @param question     The question to answer
     * @return String, Answer to the question
     */
    @Override
    @CircuitBreaker(name = "openAiService", fallbackMethod = "questionFallback")
    public String answerQuestion(String paperContext, String question) {
        log.info("🤖 Answering question with OpenAI: {}", question);

        String truncatedContext = paperContext.length() > 10000 ?
                paperContext.substring(0, 10000) + "..." : paperContext;

        String prompt = """
                You are an AI assistant helping students understand research papers.
                Based on the paper content below, answer the student's question clearly and concisely.
                If the answer is not in the paper, say so politely.
                Use simple language and explain technical terms.
                
                Paper Content:
                %s
                
                Student's Question: %s
                
                Answer:
                """.formatted(truncatedContext, question);

        try {
            String response = callOpenAiApi(prompt);
            log.info("✅ Successfully generated answer");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to generate answer", e);
            return questionFallback(paperContext, question, e);
        }
    }

    /**
     * Helper method to call OpenAI API with the given prompt.
     *
     * @param prompt The prompt to send to OpenAI
     * @return String, The response from OpenAI
     */
    private String callOpenAiApi(String prompt) {
        String apiUrl = properties.getApiUrl();
        String apiKey = properties.getApiKey();

        if (apiKey == null || apiKey.isBlank() || "mock-key".equals(apiKey)) {
            throw new IllegalStateException("OpenAI API key is missing or invalid");
        }

        // Build request using Gson (safer than string formatting)
        JsonObject requestBody = getJsonObject(prompt);

        // Make request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); // This is cleaner than "Bearer " + apiKey

        HttpEntity<String> request = new HttpEntity<>(gson.toJson(requestBody), headers);

        log.debug("Calling OpenAI API with model: {}", properties.getModel());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("OpenAI API error: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("OpenAI API error: " + response.getStatusCode());
            }

            // Parse response
            JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);

            return jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString()
                    .trim();

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    /**
     * Constructs the JSON request body for OpenAI API.
     *
     * @param prompt The prompt to include in the request
     * @return JsonObject, The request body
     */
    private JsonObject getJsonObject(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", properties.getModel());
        requestBody.addProperty("max_tokens", properties.getMaxTokens());
        requestBody.addProperty("temperature", properties.getTemperature());

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful AI assistant that helps students understand research papers.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);
        return requestBody;
    }

    /**
     * Maps OpenAI response string to DifficultyLevel enum.
     *
     * @param response The response string from OpenAI
     * @return DifficultyLevel, Mapped difficulty level
     */
    private DifficultyLevel mapDifficulty(String response) {
        if (response.contains("BEGINNER")) return DifficultyLevel.BEGINNER;
        if (response.contains("INTERMEDIATE")) return DifficultyLevel.INTERMEDIATE;
        if (response.contains("ADVANCED")) return DifficultyLevel.ADVANCED;
        if (response.contains("EXPERT")) return DifficultyLevel.EXPERT;
        return DifficultyLevel.INTERMEDIATE; // Default
    }


    /**
     * Fallback method for summary generation when OpenAI API fails.
     *
     * @param text The original text
     * @param t    The throwable that caused the fallback
     * @return String, Fallback message
     */
    private String summaryFallback(String text, Throwable t) {
        log.error("OpenAI API call failed, using fallback", t);
        return "Unable to generate summary at this time. The OpenAI service is temporarily unavailable.";
    }

    /**
     * Fallback method for difficulty estimation when OpenAI API fails.
     * Uses simple heuristics based on text length.
     *
     * @param text The original text
     * @param t    The throwable that caused the fallback
     * @return DifficultyLevel, Estimated difficulty level
     */
    private DifficultyLevel difficultyFallback(String text, Throwable t) {
        log.error("OpenAI difficulty estimation failed, using fallback", t);
        int length = text.length();
        if (length < 10000) return DifficultyLevel.BEGINNER;
        if (length < 20000) return DifficultyLevel.INTERMEDIATE;
        if (length < 40000) return DifficultyLevel.ADVANCED;
        return DifficultyLevel.EXPERT;
    }

    /**
     * Fallback method for question answering when OpenAI API fails.
     *
     * @param context  The paper context
     * @param question The original question
     * @param t        The throwable that caused the fallback
     * @return String, Fallback message
     */
    private String questionFallback(String context, String question, Throwable t) {
        log.error("OpenAI question answering failed, using fallback", t);
        return "Unable to answer your question at this time. The OpenAI service is temporarily unavailable.";
    }
}