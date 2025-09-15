package com.sasmit.researchpaperassistant.infrastructure.adapters.arxiv;

import com.sasmit.researchpaperassistant.core.domain.model.Paper;
import com.sasmit.researchpaperassistant.core.ports.out.ArxivClient;
import com.sasmit.researchpaperassistant.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Real implementation of ArxivClient using arXiv's REST API.
 * Parses XML responses to extract paper metadata.
 * Circuit breaker pattern applied to handle arXiv service issues gracefully.
 */
@Service
@ConditionalOnProperty(name = "app.features.use-mock-arxiv", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ArxivRestClient implements ArxivClient {

    private final AppProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();


    /**
     * Fetches paper metadata from arXiv given an arXiv ID.
     * Uses circuit breaker to handle potential failures.
     *
     * @param arxivId The arXiv ID of the paper.
     * @return An Optional containing the Paper metadata if found, else empty.
     */
    @Override
    @CircuitBreaker(name = "arxivService", fallbackMethod = "fallbackFetchPaperMetadata")
    public Optional<Paper> fetchPaperMetadata(String arxivId) {
        log.info("📚 Fetching metadata from arXiv for ID: {}", arxivId);

        try {
            String url = properties.getArxiv().getBaseUrl() + "?id_list=" + arxivId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "ResearchPaperAssistant/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            //The actual get request to arXiv API
            String xmlResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.warn("Empty response from arXiv API");
                return Optional.empty();
            }

            return parseArxivXml(xmlResponse, arxivId);

        } catch (Exception e) {
            log.error("Error fetching from arXiv API", e);
            return Optional.empty();
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Returns empty Optional and logs the error.
     *
     * @param arxivId The arXiv ID of the paper.
     * @param t       The throwable that caused the fallback.
     * @return An empty Optional.
     */
    public Optional<Paper> fallbackFetchPaperMetadata(String arxivId, Throwable t) {
        log.error("❌ Circuit breaker triggered. Falling back for arXiv ID: {}", arxivId);
        return Optional.empty();
    }

    /**
     * Parses the XML response from arXiv to extract paper metadata.
     *
     * @param xml     The XML response as a string.
     * @param arxivId The arXiv ID of the paper.
     * @return An Optional containing the Paper metadata if parsing is successful, else empty.
     */
    private Optional<Paper> parseArxivXml(String xml, String arxivId) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

            NodeList entries = doc.getElementsByTagName("entry");
            if (entries.getLength() == 0) {
                log.warn("No entries found in arXiv response");
                return Optional.empty();
            }

            Element entry = (Element) entries.item(0);

            String title = getTextContent(entry, "title").trim();
            String abstractText = getTextContent(entry, "summary").trim();
            String authors = extractAuthors(entry);
            LocalDateTime publishedDate = parseDate(getTextContent(entry, "published"));

            Paper paper = Paper.fromMetadata(arxivId, title, authors, abstractText, publishedDate);

            log.info("✅ Successfully parsed paper: {}", title);
            return Optional.of(paper);

        } catch (Exception e) {
            log.error("Error parsing arXiv XML", e);
            return Optional.empty();
        }
    }

    /**
     * Helper method to get text content of a tag.
     *
     * @param parent  The parent XML element.
     * @param tagName The tag name to extract text from.
     * @return The text content of the tag, or empty string if not found.
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

    /**
     * Extracts authors from the XML entry.
     *
     * @param entry The XML entry element.
     * @return A comma-separated string of authors.
     */
    private String extractAuthors(Element entry) {
        NodeList authors = entry.getElementsByTagName("author");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < authors.getLength(); i++) {
            Element author = (Element) authors.item(i);
            String name = getTextContent(author, "name");
            if (i > 0) sb.append(", ");
            sb.append(name);
        }

        return sb.toString();
    }

    /**
     * Parses date string from arXiv format to LocalDateTime.
     * Falls back to current-time if parsing fails.
     *
     * @param dateStr The date string from arXiv.
     * @return The parsed LocalDateTime, or current time if parsing fails.
     */
    private LocalDateTime parseDate(String dateStr) {
        try {
            // Handle arXiv date format: 2023-01-15T10:30:00Z
            if (dateStr.endsWith("Z")) {
                dateStr = dateStr.substring(0, dateStr.length() - 1);
            }
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            log.warn("Could not parse date: {}, using current time", dateStr);
            return LocalDateTime.now();
        }
    }
}