package com.sasmit.researchpaperassistant.api.dto.response;


import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a question and its corresponding answer.
 * Contains the question ID, associated ArXiv ID, the question text, the answer text, and a timestamp.
 */
@Data
@Builder
public class QuestionResponse {
    private String questionId;
    private String arxivId;
    private String question;
    private String answer;
    private LocalDateTime timestamp;
}
