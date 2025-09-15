package com.sasmit.researchpaperassistant.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for analyzing a research paper by its ArXiv ID.
 * Includes validation to ensure the ArXiv ID is in a correct format.
 */
@Data
public class AnalyzePaperRequest {
    @NotBlank(message = "ArXiv ID is required")
    @Pattern(
            regexp = "^(\\d{4}\\.\\d{4,5}|[a-z\\-]+/\\d{7})$",
            message = "Invalid arXiv ID format. Examples: 2301.00001 or cs/0301001"
    )
    private String arxivId;
}