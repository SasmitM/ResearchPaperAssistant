package com.sasmit.researchpaperassistant.core.ports.in;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis;

import java.util.Optional;

/**
 * Primary port for analyzing research papers.
 * This interface defines the use cases available to the application.
 */
public interface AnalyzePaperUseCase {

    /**
     * Submit a paper for analysis
     *
     * @param arxivId The arXiv ID of the paper
     * @return Job ID for tracking the analysis progress
     */
    String submitPaperForAnalysis(String arxivId);

    /**
     * Get the analysis results for a paper
     *
     * @param arxivId The arXiv ID of the paper
     * @return The analysis results if available
     */
    Optional<PaperAnalysis> getAnalysis(String arxivId);

    /**
     * Check the status of an analysis job
     *
     * @param jobId The job ID returned from submitPaperForAnalysis
     * @return The current job status
     */
    JobStatus getJobStatus(String jobId);

    /**
     * Job status enumeration
     */
    enum JobStatus {
        PENDING("Analysis is queued", 0),
        FETCHING_METADATA("Fetching paper metadata", 10),
        EXTRACTING_PDF("Extracting PDF content", 30),
        ANALYZING("Analyzing paper content", 50),
        GENERATING_SUMMARY("Generating summaries", 70),
        FORMATTING_CITATIONS("Formatting citations", 90),
        COMPLETED("Analysis complete", 100),
        FAILED("Analysis failed", -1);

        private final String description;
        private final int progressPercentage;

        JobStatus(String description, int progressPercentage) {
            this.description = description;
            this.progressPercentage = progressPercentage;
        }

        public String getDescription() {
            return description;
        }

        public int getProgressPercentage() {
            return progressPercentage;
        }
    }
}