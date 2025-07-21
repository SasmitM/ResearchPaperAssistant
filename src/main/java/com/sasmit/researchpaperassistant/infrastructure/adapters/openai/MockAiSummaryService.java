package com.sasmit.researchpaperassistant.infrastructure.adapters.openai;

import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis.DifficultyLevel;
import com.sasmit.researchpaperassistant.core.ports.out.AiSummaryService;
import com.sasmit.researchpaperassistant.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "app.features.use-mock-openai", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MockAiSummaryService implements AiSummaryService {

    private final AppProperties properties;
    private final Random random = new Random();

    private final List<String> summaryTemplates = List.of(
            "This groundbreaking research explores %s with innovative approaches that could revolutionize the field.",
            "The authors present a novel framework for understanding %s, making complex concepts accessible to students.",
            "A comprehensive study on %s that bridges theoretical foundations with practical applications.",
            "This paper introduces cutting-edge techniques in %s, perfect for students beginning their research journey."
    );

    @Override
    public String summarizeAbstract(String abstractText) {
        log.info("🤖 MOCK: Generating abstract summary for {} characters", abstractText.length());
        simulateProcessing();

        String topic = extractTopic(abstractText);
        String template = summaryTemplates.get(random.nextInt(summaryTemplates.size()));
        String timestamp = properties.getMock().getResponses().isIncludeTimestamp()
                ? " [Generated: " + LocalDateTime.now().toLocalTime() + "]" : "";

        StringBuilder summary = new StringBuilder();
        summary.append("**Student-Friendly Summary**").append(timestamp).append("\n\n");
        summary.append(String.format(template, topic)).append("\n\n");
        summary.append("**Key Points:**\n");
        summary.append("• Easy-to-understand methodology\n");
        summary.append("• Clear practical applications\n");
        summary.append("• Well-structured arguments\n\n");

        if (properties.getMock().getResponses().isAddEmojis()) {
            summary.append("📚 Perfect for undergraduate students!\n");
        }

        return summary.toString();
    }

    @Override
    public String summarizePaper(String fullText) {
        log.info("🤖 MOCK: Generating full paper summary for {} characters", fullText.length());
        simulateProcessing();

        return """
                **Comprehensive Paper Summary**
                
                **Introduction & Background**
                The paper establishes fundamental concepts and provides historical context that helps readers understand the research motivation.
                
                **Methodology**
                The authors employ a systematic approach with clear experimental design, making it easy to follow their reasoning.
                
                **Key Findings**
                • Discovery 1: Significant improvement in efficiency (up to 45%)
                • Discovery 2: Novel theoretical framework validated
                • Discovery 3: Practical applications demonstrated
                
                **Implications**
                This work opens new avenues for future research and has immediate applications in educational settings.
                
                **Conclusion**
                An excellent paper for students looking to understand advanced concepts through clear explanations and practical examples.
                """;
    }

    @Override
    public DifficultyLevel estimateDifficulty(String text) {
        log.info("🤖 MOCK: Estimating difficulty level");

        // Simple heuristics for demo
        int length = text.length();
        long complexWords = text.split("\\s+").length;

        if (length < 10000 || complexWords < 2000) {
            return DifficultyLevel.BEGINNER;
        } else if (length < 20000 || complexWords < 4000) {
            return DifficultyLevel.INTERMEDIATE;
        } else if (length < 40000) {
            return DifficultyLevel.ADVANCED;
        } else {
            return DifficultyLevel.EXPERT;
        }
    }

    @Override
    public int estimateReadingTime(String text) {
        log.info("🤖 MOCK: Estimating reading time");

        // Assume 200 words per minute
        int wordCount = text.split("\\s+").length;
        int minutes = Math.max(5, wordCount / 200);

        // Round to nearest 5 minutes
        return ((minutes + 4) / 5) * 5;
    }

    private void simulateProcessing() {
        try {
            int delay = ThreadLocalRandom.current().nextInt(
                    properties.getMock().getDelays().getMinMs(),
                    properties.getMock().getDelays().getMaxMs()
            );
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractTopic(String text) {
        String lowerText = text.toLowerCase();
        if (lowerText.contains("neural")) return "neural networks";
        if (lowerText.contains("quantum")) return "quantum computing";
        if (lowerText.contains("machine learning")) return "machine learning";
        if (lowerText.contains("algorithm")) return "algorithmic optimization";
        if (lowerText.contains("data")) return "data science";
        return "computational research";
    }
}