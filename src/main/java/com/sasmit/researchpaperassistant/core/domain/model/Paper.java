package com.sasmit.researchpaperassistant.core.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a research paper from arXiv.
 * Encapsulates metadata and provides business logic related to the paper.
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
     * Value object representing a valid ArXiv ID.
     * Ensures the ID conforms to arXiv's formatting rules.
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
     * Factory method to create a Paper instance from metadata.
     * Validates required fields and constructs the Paper object.
     *
     * @param arxivId       The arXiv ID of the paper.
     * @param title         The title of the paper.
     * @param authors       The authors of the paper.
     * @param abstractText  The abstract of the paper.
     * @param publishedDate The publication date of the paper.
     * @return A new Paper instance.
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
     * Business logic to determine if the paper is considered recent.
     * A paper is recent if it was published within the last year.
     *
     * @return true if the paper is recent, false otherwise.
     */
    public boolean isRecent() {
        return publishedDate != null &&
                publishedDate.isAfter(LocalDateTime.now().minusYears(1));
    }


}