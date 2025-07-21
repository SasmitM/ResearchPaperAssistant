package com.sasmit.researchpaperassistant.api.v1.controllers;

import com.sasmit.researchpaperassistant.api.dto.request.AnalyzePaperRequest;
import com.sasmit.researchpaperassistant.api.dto.response.JobStatusResponse;
import com.sasmit.researchpaperassistant.api.dto.response.PaperAnalysisResponse;
import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis;
import com.sasmit.researchpaperassistant.core.ports.in.AnalyzePaperUseCase;
import com.sasmit.researchpaperassistant.core.ports.out.PaperRepository;
import com.sasmit.researchpaperassistant.core.ports.out.ArxivClient;
import com.sasmit.researchpaperassistant.core.ports.out.PdfExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Papers", description = "Paper analysis endpoints")
@CrossOrigin(origins = "*") // Configure properly for production
public class PaperController {

    private final AnalyzePaperUseCase analyzePaperUseCase;
    private final PaperRepository paperRepository;
    private final ArxivClient arxivClient;
    private final PdfExtractor pdfExtractor;

    @PostMapping("/analyze")
    @Operation(summary = "Submit a paper for analysis",
            description = "Submit an arXiv paper for AI-powered analysis")
    public ResponseEntity<Map<String, String>> analyzePaper(
            @Valid @RequestBody AnalyzePaperRequest request) {

        log.info("📥 Received analysis request for arXiv ID: {}", request.getArxivId());

        String jobId = analyzePaperUseCase.submitPaperForAnalysis(request.getArxivId());

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "Paper submitted for analysis",
                "statusUrl", "/api/v1/papers/jobs/" + jobId
        ));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Check job status",
            description = "Get the status of an analysis job")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {

        AnalyzePaperUseCase.JobStatus status = analyzePaperUseCase.getJobStatus(jobId);

        JobStatusResponse.JobStatusResponseBuilder response = JobStatusResponse.builder()
                .jobId(jobId)
                .status(status.name())
                .description(status.getDescription())
                .progressPercentage(status.getProgressPercentage());

        // If completed, include the result
        if (status == AnalyzePaperUseCase.JobStatus.COMPLETED) {
            // Note: In a real implementation, we'd store the arxivId with the job
            // For now, we'll leave the result null
            log.info("✅ Job {} completed", jobId);
        }

        return ResponseEntity.ok(response.build());
    }

    @GetMapping("/{arxivId}")
    @Operation(summary = "Get paper analysis",
            description = "Retrieve the analysis results for a paper")
    public ResponseEntity<?> getPaperAnalysis(@PathVariable String arxivId) {

        log.info("🔍 Fetching analysis for arXiv ID: {}", arxivId);

        Optional<PaperAnalysis> analysisOpt = analyzePaperUseCase.getAnalysis(arxivId);

        if (analysisOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Analysis not found",
                    "arxivId", arxivId,
                    "message", "Please submit this paper for analysis first"
            ));
        }

        // Get the paper details
        Optional<Paper> paperOpt = paperRepository.findByArxivId(arxivId);
        if (paperOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Paper not found",
                    "arxivId", arxivId
            ));
        }

        Paper paper = paperOpt.get();
        PaperAnalysis analysis = analysisOpt.get();

        // Map to response DTO
        PaperAnalysisResponse response = PaperAnalysisResponse.builder()
                .arxivId(arxivId)
                .title(paper.getTitle())
                .authors(paper.getAuthors())
                .abstractText(paper.getAbstractText())
                .abstractSummary(analysis.getAbstractSummary())
                .fullTextSummary(analysis.getFullTextSummary())
                .difficulty(PaperAnalysisResponse.DifficultyInfo.builder()
                        .level(analysis.getDifficultyLevel().name())
                        .description(analysis.getDifficultyLevel().getDescription())
                        .emoji(analysis.getDifficultyLevel().getEmoji())
                        .build())
                .estimatedReadingTimeMinutes(analysis.getEstimatedReadingTimeMinutes())
                .citations(PaperAnalysisResponse.CitationInfo.builder()
                        .apa(analysis.getCitation().getApa())
                        .mla(analysis.getCitation().getMla())
                        .chicago(analysis.getCitation().getChicago())
                        .bibtex(analysis.getCitation().getBibtex())
                        .build())
                .publishedDate(paper.getPublishedDate())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/{arxivId}/exists")
    @Operation(summary = "Test endpoint",
            description = "Check if a paper exists in our cache")
    public ResponseEntity<Map<String, Object>> checkPaperExists(@PathVariable String arxivId) {
        boolean paperExists = paperRepository.findByArxivId(arxivId).isPresent();
        boolean analysisExists = paperRepository.findAnalysisByArxivId(arxivId).isPresent();

        return ResponseEntity.ok(Map.of(
                "arxivId", arxivId,
                "paperCached", paperExists,
                "analysisExists", analysisExists
        ));
    }

    @GetMapping("/{arxivId}/raw-text")
    @Operation(summary = "Get raw PDF text",
            description = "Extract and return the raw text content from the PDF without AI analysis")
    public ResponseEntity<Map<String, Object>> getRawPdfText(@PathVariable String arxivId) {
        log.info("📄 Extracting raw PDF text for arXiv ID: {}", arxivId);

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

            // Extract raw text using Tika
            String rawText = pdfExtractor.extractText(arxivId);

            Paper paper = paperOpt.get();
            return ResponseEntity.ok(Map.of(
                    "arxivId", arxivId,
                    "title", paper.getTitle(),
                    "rawText", rawText,
                    "extractedAt", LocalDateTime.now(),
                    "textLength", rawText.length()
            ));

        } catch (Exception e) {
            log.error("Error extracting PDF text for {}", arxivId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to extract PDF text",
                    "arxivId", arxivId,
                    "message", e.getMessage()
            ));
        }
    }
}