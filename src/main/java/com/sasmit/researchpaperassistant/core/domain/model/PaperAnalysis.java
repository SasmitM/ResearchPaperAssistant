package com.sasmit.researchpaperassistant.core.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Domain model representing the analysis of a research paper.
 * Encapsulates summaries, difficulty assessment, reading time estimate, citations, and timestamps.
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
     * Enum representing difficulty levels with descriptions and emojis.
     */
    public enum DifficultyLevel {
        BEGINNER("Suitable for beginners", "🟢"),
        INTERMEDIATE("Requires some background knowledge", "🟡"),
        ADVANCED("Requires significant expertise", "🔴"),
        EXPERT("Cutting-edge research level", "🟣");

        private final String description;
        private final String emoji;

        /**
         * Constructor for DifficultyLevel enum.
         *
         * @param description Description of the difficulty level.
         * @param emoji       Emoji representing the difficulty level.
         */
        DifficultyLevel(String description, String emoji) {
            this.description = description;
            this.emoji = emoji;
        }

        /**
         * Get the description of the difficulty level.
         *
         * @return Description string.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Get the emoji representing the difficulty level.
         *
         * @return Emoji string.
         */
        public String getEmoji() {
            return emoji;
        }
    }

    /**
     * Citation formats for the paper.
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

        /**
         * Extract last names from a comma-separated author string.
         * Handles different cases for single, two, or multiple authors.
         *
         * @param authors Comma-separated author names.
         * @return Formatted string of last names.
         */
        private static String extractLastNames(String authors) {
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

        /**
         * Extract the last name of the first author from a comma-separated author string.
         *
         * @param authors Comma-separated author names.
         * @return Last name of the first author.
         */
        private static String extractFirstAuthorLastName(String authors) {
            String[] authorList = authors.split(",");
            return extractLastName(authorList[0].trim()).replaceAll("[^a-zA-Z]", "");
        }

        /**
         * Helper method to extract the last name from a full name string.
         *
         * @param fullName Full name of an author.
         * @return Last name of the author.
         */
        private static String extractLastName(String fullName) {
            String[] parts = fullName.trim().split("\\s+");
            return parts[parts.length - 1];
        }
    }

    /**
     * Business logic to determine if the analysis is considered fresh.
     * An analysis is fresh if it was done within the last 30 days.
     *
     * @return true if the analysis is fresh, false otherwise.
     */
    public boolean isFresh() {
        return analyzedAt != null &&
                analyzedAt.isAfter(LocalDateTime.now().minusDays(30));
    }
}