// File: AppProperties.java
package com.sasmit.researchpaperassistant.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private Features features = new Features();
    private Arxiv arxiv = new Arxiv();
    private Mock mock = new Mock();

    @Data
    public static class Features {
        private boolean useMockAi;
        private boolean useMockArxiv;
        private boolean useGeminiAi;
        private boolean useOpenAi;
        private boolean enableCaching;
    }

    @Data
    public static class Arxiv {
        private String baseUrl = "https://export.arxiv.org/api/query";
        private String pdfBaseUrl = "https://arxiv.org/pdf/";
    }


    @Data
    public static class Mock {
        private Delays delays = new Delays();
        private Responses responses = new Responses();

        @Data
        public static class Delays {
            private int minMs = 500;
            private int maxMs = 2000;
        }

        @Data
        public static class Responses {
            private boolean addEmojis = true;
            private boolean includeTimestamp = true;
        }
    }
}