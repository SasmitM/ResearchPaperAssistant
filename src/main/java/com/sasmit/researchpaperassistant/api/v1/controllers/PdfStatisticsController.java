package com.sasmit.researchpaperassistant.api.v1.controllers;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.ports.out.ArxivClient;
import com.sasmit.researchpaperassistant.core.ports.out.PaperRepository;
import com.sasmit.researchpaperassistant.core.ports.out.PdfExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller to handle PDF text extraction and statistics.
 * Uses Apache Tika under the hood for text extraction.
 * Endpoints:
 * - GET /api/v1/test/pdf/extract-stats/{arxivId}: Extracts text statistics from the PDF of the given arXiv ID.
 * - GET /api/v1/test/pdf/extract/{arxivId}: Extracts full text from the PDF of the given arXiv ID.
 */
@RestController
@RequestMapping("/api/v1/test/pdf-stats")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure properly for production
public class PdfStatisticsController {

    private final PdfExtractor pdfExtractor;
    private final PaperRepository paperRepository;
    private final ArxivClient arxivClient;

    /**
     * Endpoint to extract text statistics from a PDF given its arXiv ID.
     *
     * @param arxivId The arXiv ID of the paper.
     * @return A response entity containing the statistics or an error message.
     */
    @GetMapping("/extract-stats/{arxivId}")
    public ResponseEntity<Map<String, Object>> extractPdfStats(@PathVariable String arxivId) {
        log.info("📊 Extracting PDF statistics for arXiv ID: {}", arxivId);

        try {
            // Get the paper metadata first to validate the arXiv ID
            Optional<Paper> paperOpt = paperRepository.findByArxivId(arxivId);
            if (paperOpt.isEmpty()) {
                // Try to fetch from arXiv if not in cache
                Optional<Paper> fetchedPaper = arxivClient.fetchPaperMetadata(arxivId);
                if (fetchedPaper.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "error", "Paper not found",
                            "arxivId", arxivId,
                            "message", "Could not find paper on arXiv"
                    ));
                }
                // Save the paper for future use
                paperRepository.savePaper(fetchedPaper.get());
                paperOpt = fetchedPaper;
            }

            // Extract text using Tika to calculate statistics
            String extractedText = pdfExtractor.extractText(arxivId);

            // Calculate statistics
            int totalCharacters = extractedText.length();
            int totalWords = extractedText.trim().isEmpty() ? 0 : extractedText.trim().split("\\s+").length;
            int totalLines = extractedText.isEmpty() ? 0 : extractedText.split("\n").length;
            int totalParagraphs = extractedText.isEmpty() ? 0 : extractedText.split("\n\n").length;
            int estimatedPages = Math.max(1, totalWords / 250); // Rough estimate: 250 words per page

            Paper paper = paperOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("arxivId", arxivId);
            response.put("title", paper.getTitle());
            response.put("statistics", Map.of(
                    "totalCharacters", totalCharacters,
                    "totalWords", totalWords,
                    "totalLines", totalLines,
                    "totalParagraphs", totalParagraphs,
                    "estimatedPages", estimatedPages
            ));
            response.put("extractedAt", LocalDateTime.now());

            log.info("✅ Successfully extracted PDF statistics for {}", arxivId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error extracting PDF statistics for {}", arxivId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to extract PDF statistics",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Endpoint to extract full text from a PDF given its arXiv ID.
     *
     * @param arxivId The arXiv ID of the paper.
     * @return A response entity containing the full text or an error message.
     */
    @GetMapping("/extract/{arxivId}")
    public ResponseEntity<Map<String, Object>> extractFullText(@PathVariable String arxivId) {
        log.info("📄 Extracting full PDF text for arXiv ID: {}", arxivId);

        try {
            // Get the paper metadata first to validate the arXiv ID
            Optional<Paper> paperOpt = paperRepository.findByArxivId(arxivId);
            if (paperOpt.isEmpty()) {
                // Try to fetch from arXiv if not in cache
                Optional<Paper> fetchedPaper = arxivClient.fetchPaperMetadata(arxivId);
                if (fetchedPaper.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "error", "Paper not found",
                            "arxivId", arxivId,
                            "message", "Could not find paper on arXiv"
                    ));
                }
                // Save the paper for future use
                paperRepository.savePaper(fetchedPaper.get());
                paperOpt = fetchedPaper;
            }

            // Extract full text using Tika
            String extractedText = pdfExtractor.extractText(arxivId);

            Paper paper = paperOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("arxivId", arxivId);
            response.put("title", paper.getTitle());
            response.put("fullText", extractedText);
            response.put("textLength", extractedText.length());
            response.put("extractedAt", LocalDateTime.now());

            log.info("✅ Successfully extracted full PDF text for {}", arxivId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error extracting PDF text for {}", arxivId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to extract PDF text",
                    "message", e.getMessage()
            ));
        }
    }
}
