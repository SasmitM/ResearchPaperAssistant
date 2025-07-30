package com.sasmit.researchpaperassistant.api.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class AskQuestionRequest {
    @NotBlank(message = "Question is required")
    @Size(min = 3, max = 500, message = "Question must be 500 characters or less")
    private String question;
}


