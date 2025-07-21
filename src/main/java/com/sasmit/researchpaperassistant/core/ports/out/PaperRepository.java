package com.sasmit.researchpaperassistant.core.ports.out;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis;

import java.util.Optional;

/**
 * Port for persisting and retrieving papers and analyses
 */
public interface PaperRepository {
    /**
     * Save a paper
     *
     * @param paper The paper to save
     * @return The saved paper
     */
    Paper savePaper(Paper paper);

    /**
     * Find a paper by arXiv ID
     *
     * @param arxivId The arXiv ID
     * @return The paper if found
     */
    Optional<Paper> findByArxivId(String arxivId);

    /**
     * Save analysis results
     *
     * @param analysis The analysis to save
     * @return The saved analysis
     */
    PaperAnalysis saveAnalysis(PaperAnalysis analysis);

    /**
     * Find analysis by arXiv ID
     *
     * @param arxivId The arXiv ID
     * @return The analysis if found
     */
    Optional<PaperAnalysis> findAnalysisByArxivId(String arxivId);
}