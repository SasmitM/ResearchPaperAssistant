package com.sasmit.researchpaperassistant.infrastructure.adapters.arxiv;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.ports.out.ArxivClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of ArxivClient for testing and development.
 * Generates synthetic paper metadata without making real network calls.
 */
@Service
@ConditionalOnProperty(name = "app.features.use-mock-arxiv", havingValue = "true")
@Slf4j
public class MockArxivClient implements ArxivClient {

    /**
     * Simulates fetching paper metadata by generating mock data.
     *
     * @param arxivId The arXiv ID of the paper.
     * @return An Optional containing the mock Paper metadata.
     */
    @Override
    public Optional<Paper> fetchPaperMetadata(String arxivId) {
        log.info("🤖 MOCK: Fetching paper metadata for {}", arxivId);

        // Simulate network delay
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 800));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate mock paper
        Paper paper = Paper.fromMetadata(
                arxivId,
                "Neural Networks for " + generateRandomTopic() + ": A Comprehensive Study",
                "John Doe, Jane Smith, Alice Johnson, Bob Wilson",
                generateMockAbstract(),
                LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 365))
        );

        log.info("🤖 MOCK: Generated paper - {}", paper.getTitle());
        return Optional.of(paper);
    }

    /**
     * Generates a random research topic for the mock paper title.
     *
     * @return A random topic string.
     */
    private String generateRandomTopic() {
        String[] topics = {
                "Pattern Recognition", "Quantum Computing", "Natural Language Processing",
                "Computer Vision", "Reinforcement Learning", "Graph Neural Networks",
                "Optimization Algorithms", "Distributed Systems", "Cryptography"
        };
        return topics[ThreadLocalRandom.current().nextInt(topics.length)];
    }

    /**
     * Generates a mock abstract for the paper.
     *
     * @return A synthetic abstract string.
     */
    private String generateMockAbstract() {
        return """
                We present a novel approach to solving complex computational problems using advanced machine learning techniques. 
                Our method achieves state-of-the-art performance on benchmark datasets while requiring significantly less 
                computational resources than traditional approaches. Through extensive experimentation, we demonstrate 
                the effectiveness of our approach across multiple domains. The key contributions of this work include: 
                (1) a new theoretical framework for understanding the problem space, (2) an efficient algorithm with 
                provable convergence guarantees, and (3) comprehensive empirical evaluation on real-world datasets. 
                Our results show improvements of up to 35% in accuracy and 50% reduction in training time compared 
                to existing methods. The implications of this work extend beyond the immediate application domain 
                and suggest new directions for future research in artificial intelligence and machine learning.
                """;
    }
}