package com.sasmit.researchpaperassistant.core.ports.out;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;

import java.util.Optional;

/**
 * Port for fetching paper metadata from arXiv
 */
public interface ArxivClient {
    /**
     * Fetch paper metadata from arXiv
     *
     * @param arxivId The arXiv ID
     * @return Paper metadata if found
     */
    Optional<Paper> fetchPaperMetadata(String arxivId);
}