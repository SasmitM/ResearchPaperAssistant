package com.sasmit.researchpaperassistant.api.dto.response;

import lombok.Builder;
import lombok.Data;

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