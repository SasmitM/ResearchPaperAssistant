package com.sasmit.researchpaperassistant.core.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Value object representing the analysis results of a paper
 */
@Getter
@Builder
@ToString
public class PaperAnalysis {
    private final Paper.ArxivId arxivId;
    private final String abstractSummary;
    private final String fullTextSummary;
    private final DifficultyLevel difficultyLevel;
    private final int estimatedReadingTimeMinutes;
    private final Citation citation;
    private final LocalDateTime analyzedAt;

    /**
     * Difficulty levels for papers
     */
    public enum DifficultyLevel {
        BEGINNER("Suitable for beginners", "🟢"),
        INTERMEDIATE("Requires some background knowledge", "🟡"),
        ADVANCED("Requires significant expertise", "🔴"),
        EXPERT("Cutting-edge research level", "🟣");

        private final String description;
        private final String emoji;

        DifficultyLevel(String description, String emoji) {
            this.description = description;
            this.emoji = emoji;
        }

        public String getDescription() {
            return description;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    /**
     * Value object for citation
     */
    @Getter
    @Builder
    @ToString
    public static class Citation {
        private final String apa;
        private final String mla;
        private final String chicago;
        private final String bibtex;

        public static Citation generate(Paper paper) {
            // Simplified citation generation
            String year = paper.getPublishedDate() != null ?
                    String.valueOf(paper.getPublishedDate().getYear()) : "n.d.";

            String authorLastNames = extractLastNames(paper.getAuthors());

            return Citation.builder()
                    .apa(String.format("%s (%s). %s. arXiv:%s",
                            authorLastNames, year, paper.getTitle(), paper.getArxivId()))
                    .mla(String.format("%s. \"%s.\" arXiv preprint arXiv:%s (%s).",
                            authorLastNames, paper.getTitle(), paper.getArxivId(), year))
                    .chicago(String.format("%s. \"%s.\" arXiv preprint arXiv:%s (%s).",
                            authorLastNames, paper.getTitle(), paper.getArxivId(), year))
                    .bibtex(String.format("""
                                    @article{%s%s,
                                      title={%s},
                                      author={%s},
                                      journal={arXiv preprint arXiv:%s},
                                      year={%s}
                                    }""",
                            extractFirstAuthorLastName(paper.getAuthors()), year,
                            paper.getTitle(), paper.getAuthors(), paper.getArxivId(), year))
                    .build();
        }

        private static String extractLastNames(String authors) {
            // Simple extraction - in production, use a proper name parser
            String[] authorList = authors.split(",");
            if (authorList.length == 1) {
                return extractLastName(authorList[0].trim());
            } else if (authorList.length == 2) {
                return extractLastName(authorList[0].trim()) + " & " +
                        extractLastName(authorList[1].trim());
            } else {
                return extractLastName(authorList[0].trim()) + " et al.";
            }
        }

        private static String extractFirstAuthorLastName(String authors) {
            String[] authorList = authors.split(",");
            return extractLastName(authorList[0].trim()).replaceAll("[^a-zA-Z]", "");
        }

        private static String extractLastName(String fullName) {
            String[] parts = fullName.trim().split("\\s+");
            return parts[parts.length - 1];
        }
    }

    /**
     * Check if this analysis is still fresh (less than 30 days old)
     */
    public boolean isFresh() {
        return analyzedAt != null &&
                analyzedAt.isAfter(LocalDateTime.now().minusDays(30));
    }
}