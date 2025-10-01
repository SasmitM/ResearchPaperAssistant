// File: AppProperties.java
package com.sasmit.researchpaperassistant.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application configuration properties
 * Loaded from application.yml or application.properties
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private Features features = new Features();
    private Arxiv arxiv = new Arxiv();
    private Mock mock = new Mock();

    /**
     * Feature toggles for enabling/disabling certain functionalities
     */
    @Data
    public static class Features {
        private boolean useMockAi;
        private boolean useMockArxiv;
        private boolean useGeminiAi;
        private boolean useOpenAi;
        private boolean enableCaching;
    }

    /**
     * Configuration properties for arXiv API
     */
    @Data
    public static class Arxiv {
        private String baseUrl = "https://export.arxiv.org/api/query";
        private String pdfBaseUrl = "https://arxiv.org/pdf/";
    }


    /**
     * Mock settings for simulating delays and customizing mock responses
     */
    @Data
    public static class Mock {
        private Delays delays = new Delays();
        private Responses responses = new Responses();

        /**
         * Delay settings for simulating network/API latency
         */
        @Data
        public static class Delays {
            private int minMs = 500;
            private int maxMs = 2000;
        }

        /**
         * Settings for customizing mock AI responses
         */
        @Data
        public static class Responses {
            private boolean addEmojis = true;
            private boolean includeTimestamp = true;
        }
    }
}