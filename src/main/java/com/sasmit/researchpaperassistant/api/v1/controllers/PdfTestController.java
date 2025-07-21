package com.sasmit.researchpaperassistant.api.v1.controllers;

import com.sasmit.researchpaperassistant.core.ports.out.PdfExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test/pdf")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDF Test", description = "Test endpoints for PDF extraction")
@CrossOrigin(origins = "*")
public class PdfTestController {

    private final PdfExtractor pdfExtractor;

    @GetMapping("/extract/{arxivId}")
    @Operation(summary = "Test PDF extraction",
            description = "Extract and return raw text from an arXiv PDF using Tika")
    public ResponseEntity<Map<String, Object>> extractPdfText(@PathVariable String arxivId) {
        log.info("🧪 Testing PDF extraction for arXiv ID: {}", arxivId);

        Map<String, Object> response = new HashMap<>();

        try {
            // Start timing
            long startTime = System.currentTimeMillis();

            // Extract text using Tika
            String extractedText = pdfExtractor.extractText(arxivId);

            // Calculate extraction time
            long extractionTime = System.currentTimeMillis() - startTime;

            // Prepare response with metadata
            response.put("success", true);
            response.put("arxivId", arxivId);
            response.put("pdfUrl", "https://arxiv.org/pdf/" + arxivId + ".pdf");
            response.put("extractionTimeMs", extractionTime);
            response.put("textLength", extractedText.length());
            response.put("wordCount", extractedText.split("\\s+").length);
            response.put("lineCount", extractedText.split("\n").length);

            // Add text samples
            response.put("first500Chars", extractedText.substring(0,
                    Math.min(500, extractedText.length())));
            response.put("last500Chars", extractedText.substring(
                    Math.max(0, extractedText.length() - 500)));

            // Add the full text (be careful with large PDFs!)
            response.put("fullText", extractedText);

            // Check what sections were likely found
            Map<String, Boolean> sectionsFound = new HashMap<>();
            String lowerText = extractedText.toLowerCase();
            sectionsFound.put("hasAbstract", lowerText.contains("abstract"));
            sectionsFound.put("hasIntroduction", lowerText.contains("introduction"));
            sectionsFound.put("hasConclusion", lowerText.contains("conclusion"));
            sectionsFound.put("hasReferences", lowerText.contains("references"));
            response.put("sectionsFound", sectionsFound);

            log.info("✅ Successfully extracted {} characters", extractedText.length());

        } catch (Exception e) {
            log.error("❌ Failed to extract PDF", e);
            response.put("success", false);
            response.put("error", e.getClass().getSimpleName());
            response.put("message", e.getMessage());
            response.put("arxivId", arxivId);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/extract-stats/{arxivId}")
    @Operation(summary = "Get PDF extraction statistics",
            description = "Extract PDF and return only statistics (no full text)")
    public ResponseEntity<Map<String, Object>> extractPdfStats(@PathVariable String arxivId) {
        log.info("📊 Getting PDF extraction stats for arXiv ID: {}", arxivId);

        Map<String, Object> response = new HashMap<>();

        try {
            String extractedText = pdfExtractor.extractText(arxivId);

            response.put("success", true);
            response.put("arxivId", arxivId);
            response.put("statistics", Map.of(
                    "totalCharacters", extractedText.length(),
                    "totalWords", extractedText.split("\\s+").length,
                    "totalLines", extractedText.split("\n").length,
                    "totalParagraphs", extractedText.split("\n\n").length,
                    "estimatedPages", extractedText.length() / 3000 // Rough estimate
            ));

            // Sample from different parts of the document
            int quarterLength = extractedText.length() / 4;
            response.put("textSamples", Map.of(
                    "beginning", extractedText.substring(0, Math.min(200, extractedText.length())),
                    "quarter", extractedText.substring(quarterLength, Math.min(quarterLength + 200, extractedText.length())),
                    "middle", extractedText.substring(quarterLength * 2, Math.min(quarterLength * 2 + 200, extractedText.length())),
                    "threeQuarters", extractedText.substring(quarterLength * 3, Math.min(quarterLength * 3 + 200, extractedText.length()))
            ));

        } catch (Exception e) {
            log.error("Failed to get PDF stats", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}