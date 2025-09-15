package com.sasmit.researchpaperassistant.infrastructure.adapters.gemini;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis.DifficultyLevel;
import com.sasmit.researchpaperassistant.core.ports.out.AiSummaryService;
import com.sasmit.researchpaperassistant.infrastructure.config.GeminiProperties;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Service to interact with Google's Gemini AI for summarizing research papers,
 * estimating difficulty, reading time, and answering questions.
 * Uses RestTemplate to call Gemini's REST API.
 * Circuit breaker pattern applied to handle API issues gracefully.
 */
@Service
@ConditionalOnProperty(name = "app.features.use-gemini-ai", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class GeminiAiSummaryService implements AiSummaryService {

    private final GeminiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    /**
     * Summarizes the abstract of a research paper using Gemini AI.
     * Caches results to avoid redundant API calls for the same abstract.
     *
     * @param abstractText The abstract text to summarize.
     * @return A student-friendly summary of the abstract.
     */
    @Override
    @Cacheable(value = "summaries", key = "#abstractText.hashCode()")
    @CircuitBreaker(name = "geminiService", fallbackMethod = "geminiAPICallFallback")
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

    /**
     * Summarizes the full text of a research paper using Gemini AI.
     * Caches results to avoid redundant API calls for the same full text.
     *
     * @param fullText The full text of the research paper to summarize.
     * @return A comprehensive summary of the paper.
     */
    @Override
    @Cacheable(value = "fullSummaries", key = "#fullText.hashCode()")
    @CircuitBreaker(name = "geminiService", fallbackMethod = "geminiAPICallFallback")
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

    /**
     * Estimates the difficulty level of a research paper using Gemini AI.
     * Uses a fallback method if the AI call fails or returns unexpected results.
     *
     * @param text The text of the research paper to analyze.
     * @return The estimated difficulty level.
     */
    @Override
    @CircuitBreaker(name = "geminiService", fallbackMethod = "estimateDifficultyFallback")
    public DifficultyLevel estimateDifficulty(String text) {
        log.info("🤖 Estimating paper difficulty with Gemini");

        String prompt = getString(text);

        try {
            String response = callGeminiApi(prompt).trim().toUpperCase();

            // Parse response
            if (response.contains("BEGINNER")) return DifficultyLevel.BEGINNER;
            if (response.contains("INTERMEDIATE")) return DifficultyLevel.INTERMEDIATE;
            if (response.contains("ADVANCED")) return DifficultyLevel.ADVANCED;
            if (response.contains("EXPERT")) return DifficultyLevel.EXPERT;

            // Default based on text complexity
            return estimateDifficultyFallback(text, new RuntimeException("Unexpected response: " + response));

        } catch (Exception e) {
            log.error("Failed to estimate difficulty", e);
            return estimateDifficultyFallback(text, e);
        }
    }

    /**
     * Constructs the prompt for estimating difficulty.
     * Truncates the text if it exceeds 5000 characters to fit within token limits.
     *
     * @param text The text of the research paper.
     * @return The constructed prompt string.
     */
    private static String getString(String text) {
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
        return prompt;
    }

    /**
     * Estimates the reading time for a research paper based on word count.
     * Rounds the estimate to the nearest 5 minutes.
     *
     * @param text The text of the research paper.
     * @return Estimated reading time in minutes, rounded to nearest 5.
     */
    @Override
    public int estimateReadingTime(String text) {
        // Simple calculation: average reading speed is 200-250 words per minute
        // Academic papers might be slower, so use 150 wpm
        int wordCount = text.split("\\s+").length;
        int minutes = Math.max(5, wordCount / 150);

        // Round to nearest 5 minutes
        return ((minutes + 4) / 5) * 5;
    }

    /**
     * Answers a specific question based on the provided paper context using Gemini AI.
     * Uses a fallback method if the AI call fails.
     *
     * @param paperContext The context of the paper to base the answer on.
     * @param question     The question to answer.
     * @return The answer to the question.
     */
    @Override
    @CircuitBreaker(name = "geminiService", fallbackMethod = "geminiAnswerQuestionFallback")
    public String answerQuestion(String paperContext, String question) {
        log.info("🤖 Answering question with Gemini: {}", question);

        // Truncate context if too long
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
            String response = callGeminiApi(prompt);
            log.info("✅ Successfully generated answer");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to generate answer", e);
            return "I'm sorry, I couldn't generate an answer to your question. Please try again.";
        }
    }

    /**
     * Makes a call to the Gemini API with the given prompt.
     * Handles request construction, response parsing, and error handling.
     *
     * @param prompt The prompt to send to the Gemini API.
     * @return The text response from the Gemini API.
     */
    private String callGeminiApi(String prompt) {
        String url = properties.getApiUrl() + properties.getModel() +
                ":generateContent?key=" + properties.getApiKey();


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

    /**
     * Fallback method for Gemini API calls.
     * Logs the error and returns a generic failure message.
     *
     * @param prompt The original prompt sent to the API.
     * @param t      The throwable that caused the fallback.
     * @return A generic error message.
     */
    private String geminiAPICallFallback(String prompt, Throwable t) {
        log.error("❌ Gemini API call failed, using fallback", t);
        return "I'm sorry, I couldn't process your request at the moment. Please try again later.";
    }

    /**
     * Fallback method for answering questions using Gemini AI.
     * Logs the error and returns a generic failure message.
     *
     * @param paperContext The context of the paper.
     * @param question     The question asked.
     * @param t            The throwable that caused the fallback.
     * @return A generic error message.
     */
    private String geminiAnswerQuestionFallback(String paperContext, String question, Throwable t) {
        log.error("❌ Gemini API call failed for question '{}', using fallback", question, t);
        return "I'm sorry, I couldn't answer your question at the moment. Please try again later.";
    }

    /**
     * Fallback method to estimate difficulty using a simple heuristic based on text length.
     * Logs the error that caused the fallback.
     *
     * @param text The text of the research paper.
     * @param t    The throwable that caused the fallback.
     * @return Estimated difficulty level based on text length.
     */
    private DifficultyLevel estimateDifficultyFallback(String text, Throwable t) {
        log.error("❌ Fallback triggered for estimateDifficulty due to exception: {}", t.getMessage(), t);

        // Simple heuristic fallback
        int length = text.length();
        if (length < 10000) return DifficultyLevel.BEGINNER;
        if (length < 20000) return DifficultyLevel.INTERMEDIATE;
        if (length < 40000) return DifficultyLevel.ADVANCED;
        return DifficultyLevel.EXPERT;
    }
}