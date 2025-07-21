package com.sasmit.researchpaperassistant.core.usecases;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.domain.model.PaperAnalysis;
import com.sasmit.researchpaperassistant.core.ports.in.AnalyzePaperUseCase;
import com.sasmit.researchpaperassistant.core.ports.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnalyzePaperService implements AnalyzePaperUseCase {

    private final ArxivClient arxivClient;
    private final PdfExtractor pdfExtractor;
    private final AiSummaryService aiSummaryService;
    private final PaperRepository paperRepository;

    // Simple in-memory job tracking for MVP
    private final Map<String, JobInfo> jobs = new ConcurrentHashMap<>();

    @Override
    public String submitPaperForAnalysis(String arxivId) {
        log.info("📋 Submitting paper for analysis: {}", arxivId);

        // Check if already analyzed
        Optional<PaperAnalysis> existing = paperRepository.findAnalysisByArxivId(arxivId);
        if (existing.isPresent() && existing.get().isFresh()) {
            log.info("✅ Using cached analysis for {}", arxivId);
            String jobId = UUID.randomUUID().toString();
            jobs.put(jobId, new JobInfo(jobId, arxivId, JobStatus.COMPLETED, null));
            return jobId;
        }

        // Create new job
        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new JobInfo(jobId, arxivId, JobStatus.PENDING, null));

        // Start async analysis
        analyzePaperAsync(jobId, arxivId);

        return jobId;
    }

    @Async
    protected void analyzePaperAsync(String jobId, String arxivId) {
        try {
            // Update status: Fetching metadata
            updateJobStatus(jobId, JobStatus.FETCHING_METADATA);
            Optional<Paper> paperOpt = arxivClient.fetchPaperMetadata(arxivId);

            if (paperOpt.isEmpty()) {
                throw new RuntimeException("Paper not found on arXiv: " + arxivId);
            }

            Paper paper = paperOpt.get();

            // Save paper
            paper = paperRepository.savePaper(paper);

            // Update status: Extracting PDF
            updateJobStatus(jobId, JobStatus.EXTRACTING_PDF);
            String fullText = pdfExtractor.extractText(arxivId);

            // Update status: Analyzing
            updateJobStatus(jobId, JobStatus.ANALYZING);

            // Generate summaries
            updateJobStatus(jobId, JobStatus.GENERATING_SUMMARY);
            String abstractSummary = aiSummaryService.summarizeAbstract(paper.getAbstractText());
            String fullSummary = aiSummaryService.summarizePaper(fullText);

            // Estimate difficulty and reading time
            PaperAnalysis.DifficultyLevel difficulty = aiSummaryService.estimateDifficulty(fullText);
            int readingTime = aiSummaryService.estimateReadingTime(fullText);

            // Generate citations
            updateJobStatus(jobId, JobStatus.FORMATTING_CITATIONS);
            PaperAnalysis.Citation citation = PaperAnalysis.Citation.generate(paper);

            // Create analysis result
            PaperAnalysis analysis = PaperAnalysis.builder()
                    .arxivId(paper.getArxivId())
                    .abstractSummary(abstractSummary)
                    .fullTextSummary(fullSummary)
                    .difficultyLevel(difficulty)
                    .estimatedReadingTimeMinutes(readingTime)
                    .citation(citation)
                    .analyzedAt(LocalDateTime.now())
                    .build();

            // Save analysis
            paperRepository.saveAnalysis(analysis);

            // Update status: Completed
            updateJobStatus(jobId, JobStatus.COMPLETED);
            log.info("✅ Analysis completed for {}", arxivId);

        } catch (Exception e) {
            log.error("❌ Analysis failed for {}", arxivId, e);
            updateJobStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }

    @Override
    public Optional<PaperAnalysis> getAnalysis(String arxivId) {
        return paperRepository.findAnalysisByArxivId(arxivId);
    }

    @Override
    public JobStatus getJobStatus(String jobId) {
        JobInfo job = jobs.get(jobId);
        return job != null ? job.status : JobStatus.FAILED;
    }

    // Helper method to get job info (for controller)
    public Optional<JobInfo> getJobInfo(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private void updateJobStatus(String jobId, JobStatus status) {
        updateJobStatus(jobId, status, null);
    }

    private void updateJobStatus(String jobId, JobStatus status, String error) {
        JobInfo job = jobs.get(jobId);
        if (job != null) {
            job.status = status;
            job.error = error;
        }
    }

    // Using a simple mutable class instead of record for job tracking
    public static class JobInfo {
        final String jobId;
        final String arxivId;
        JobStatus status;
        String error;

        JobInfo(String jobId, String arxivId, JobStatus status, String error) {
            this.jobId = jobId;
            this.arxivId = arxivId;
            this.status = status;
            this.error = error;
        }
    }
}