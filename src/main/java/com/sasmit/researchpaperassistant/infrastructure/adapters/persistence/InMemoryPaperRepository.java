package com.sasmit.researchpaperassistant.infrastructure.adapters.persistence;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis;
import com.sasmit.researchpaperassistant.core.ports.out.PaperRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PaperRepository for MVP.
 * This will be replaced with JPA implementation later.
 */
@Repository
@Slf4j
public class InMemoryPaperRepository implements PaperRepository {

    private final Map<String, Paper> papers = new ConcurrentHashMap<>();
    private final Map<String, PaperAnalysis> analyses = new ConcurrentHashMap<>();

    @Override
    public Paper savePaper(Paper paper) {
        log.debug("💾 Saving paper: {}", paper.getArxivId()); //using log.debug for detailed logging, as it deals with internal interactions with in-memory storage
        papers.put(paper.getArxivId().value(), paper);
        return paper;
    }

    @Override
    public Optional<Paper> findByArxivId(String arxivId) {
        log.debug("🔍 Finding paper by arXiv ID: {}", arxivId);
        return Optional.ofNullable(papers.get(arxivId));
    }

    @Override
    public PaperAnalysis saveAnalysis(PaperAnalysis analysis) {
        log.debug("💾 Saving analysis for: {}", analysis.getArxivId());
        analyses.put(analysis.getArxivId().value(), analysis);
        return analysis;
    }

    @Override
    public Optional<PaperAnalysis> findAnalysisByArxivId(String arxivId) {
        log.debug("🔍 Finding analysis by arXiv ID: {}", arxivId);
        PaperAnalysis analysis = analyses.get(arxivId);

        // Check if analysis exists and is still fresh
        if (analysis != null && analysis.isFresh()) {
            return Optional.of(analysis);
        }

        // Remove stale analysis
        if (analysis != null && !analysis.isFresh()) {
            log.info("🗑️ Removing stale analysis for: {}", arxivId);
            analyses.remove(arxivId);
        }

        return Optional.empty();
    }

    /**
     * Utility method to clear all data (useful for testing)
     */
    public void clearAll() {
        papers.clear();
        analyses.clear();
        log.info("🧹 Cleared all papers and analyses from memory");
    }

    /**
     * Get current repository statistics
     */
    public Map<String, Integer> getStats() {
        return Map.of(
                "papers", papers.size(),
                "analyses", analyses.size()
        );
    }
}