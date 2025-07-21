package com.sasmit.researchpaperassistant.core.ports.out;

public interface PdfExtractor {
    /**
     * Extract text content from a PDF
     *
     * @param arxivId The arXiv ID of the paper
     * @return Extracted text content
     */
    String extractText(String arxivId);
}