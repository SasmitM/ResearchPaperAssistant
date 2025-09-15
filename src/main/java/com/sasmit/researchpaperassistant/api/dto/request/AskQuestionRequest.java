package com.sasmit.researchpaperassistant.api.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for asking a question.
 * Includes validation to ensure the question is not blank and within a reasonable length.
 */
@Data
public class AskQuestionRequest {
    @NotBlank(message = "Question is required")
    @Size(min = 3, max = 500, message = "Question must be 500 characters or less")
    private String question;
}


