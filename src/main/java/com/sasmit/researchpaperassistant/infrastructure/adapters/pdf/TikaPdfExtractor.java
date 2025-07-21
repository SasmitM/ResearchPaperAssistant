package com.sasmit.researchpaperassistant.infrastructure.adapters.pdf;

import com.sasmit.researchpaperassistant.core.ports.out.PdfExtractor;
import com.sasmit.researchpaperassistant.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@RequiredArgsConstructor
@Slf4j
public class TikaPdfExtractor implements PdfExtractor {

    private final AppProperties properties;
    private static final int MAX_CONTENT_SIZE = 10 * 1024 * 1024; // 10MB limit

    @Override
    @Cacheable(value = "pdfText", key = "#arxivId")
    public String extractText(String arxivId) {
        log.info("📄 Extracting text from PDF for arXiv ID: {}", arxivId);

        try {
            HttpURLConnection connection = getHttpURLConnection(arxivId, properties);

            try (InputStream inputStream = connection.getInputStream()) {
                // Parse PDF with Tika
                BodyContentHandler handler = new BodyContentHandler(MAX_CONTENT_SIZE);
                Metadata metadata = new Metadata();
                PDFParser parser = new PDFParser();
                ParseContext context = new ParseContext();

                parser.parse(inputStream, handler, metadata, context);

                String extractedText = handler.toString();
                log.info("✅ Extracted {} characters from PDF", extractedText.length());

                return extractedText;
            }

        } catch (Exception e) {
            log.error("Error extracting PDF text", e);
            // Return a fallback message instead of throwing
            return "Unable to extract PDF content. Error: " + e.getMessage();
        }
    }

    private static HttpURLConnection getHttpURLConnection(String arxivId, AppProperties properties) throws IOException {
        String pdfUrl = properties.getArxiv().getPdfBaseUrl() + arxivId + ".pdf";

        // Download PDF
        HttpURLConnection connection = (HttpURLConnection) new URL(pdfUrl).openConnection();
        connection.setRequestProperty("User-Agent", "ResearchPaperAssistant/1.0");
        connection.setInstanceFollowRedirects(true);

        // Handle redirects
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            String newUrl = connection.getHeaderField("Location");
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestProperty("User-Agent", "ResearchPaperAssistant/1.0");
        }
        return connection;
    }
}