package com.sasmit.researchpaperassistant.api.dto.response;


import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
public class QuestionResponse {
    private String questionId;
    private String arxivId;
    private String question;
    private String answer;
    private LocalDateTime timestamp;
}
