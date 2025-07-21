package com.sasmit.researchpaperassistant.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaperAnalysisResponse {
    private String arxivId;
    private String title;
    private String authors;
    private String abstractText;
    private String abstractSummary;
    private String fullTextSummary;
    private DifficultyInfo difficulty;
    private int estimatedReadingTimeMinutes;
    private CitationInfo citations;
    private LocalDateTime publishedDate;
    private LocalDateTime analyzedAt;

    @Data
    @Builder
    public static class DifficultyInfo {
        private String level;
        private String description;
        private String emoji;
    }

    @Data
    @Builder
    public static class CitationInfo {
        private String apa;
        private String mla;
        private String chicago;
        private String bibtex;
    }
}