package com.sasmit.researchpaperassistant.core.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Core domain entity representing a research paper.
 * This is a pure domain object with no framework dependencies.
 */
@Getter
@Builder
@ToString
public class Paper {
    private final ArxivId arxivId;
    private final String title;
    private final String authors;
    private final String abstractText;
    private final LocalDateTime publishedDate;

    /**
     * Value object for ArXiv ID
     */
    public record ArxivId(String value) {
        public ArxivId {
            Objects.requireNonNull(value, "ArXiv ID cannot be null");
            if (!value.matches("^(\\d{4}\\.\\d{4,5}|[a-z\\-]+/\\d{7})$")) {
                throw new IllegalArgumentException("Invalid arXiv ID format: " + value);
            }
        }


        @Override //have to override toString, as Lombok's @ToString does not work with Java records
        public String toString() {
            return value;
        }
    }

    /**
     * Factory method to create a Paper from metadata
     */
    public static Paper fromMetadata(String arxivId, String title, String authors,
                                     String abstractText, LocalDateTime publishedDate) {
        return Paper.builder()
                .arxivId(new ArxivId(arxivId))
                .title(Objects.requireNonNull(title, "Title cannot be null"))
                .authors(Objects.requireNonNull(authors, "Authors cannot be null"))
                .abstractText(Objects.requireNonNull(abstractText, "Abstract cannot be null"))
                .publishedDate(publishedDate)
                .build();
    }

    /**
     * Business logic: Check if paper is recent (published within last year)
     */
    public boolean isRecent() {
        return publishedDate != null &&
                publishedDate.isAfter(LocalDateTime.now().minusYears(1));
    }


}