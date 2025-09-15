package com.sasmit.researchpaperassistant.api.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for job status inquiries.
 * Contains details about the job's current state, progress, and results if available.
 */
@Data
@Builder
public class JobStatusResponse {
    private String jobId;
    private String arxivId;
    private String status;
    private String description;
    private int progressPercentage;
    private String error;
    private PaperAnalysisResponse result;
}