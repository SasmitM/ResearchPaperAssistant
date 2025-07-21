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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.features.use-mock-arxiv", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ArxivRestClient implements ArxivClient {

    private final AppProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
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

            //Factory methods can provide input validation too, so this is used over constructors
            Paper paper = Paper.fromMetadata(arxivId, title, authors, abstractText, publishedDate);

            log.info("✅ Successfully parsed paper: {}", title);
            return Optional.of(paper);

        } catch (Exception e) {
            log.error("Error parsing arXiv XML", e);
            return Optional.empty();
        }
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

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