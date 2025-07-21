package com.sasmit.researchpaperassistant.core.ports.out;

import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis.DifficultyLevel;

/**
 * Port for AI-powered text analysis and summarization
 */
public interface AiSummaryService {
    /**
     * Generate a student-friendly summary of an abstract
     *
     * @param abstractText The original abstract
     * @return Simplified summary
     */
    String summarizeAbstract(String abstractText);

    /**
     * Generate a comprehensive summary of the full paper
     *
     * @param fullText The full paper text
     * @return Comprehensive summary
     */
    String summarizePaper(String fullText);

    /**
     * Estimate the difficulty level of a paper
     *
     * @param text The paper text
     * @return Estimated difficulty level
     */
    DifficultyLevel estimateDifficulty(String text);

    /**
     * Estimate reading time in minutes
     *
     * @param text The paper text
     * @return Estimated reading time
     */
    int estimateReadingTime(String text);
}