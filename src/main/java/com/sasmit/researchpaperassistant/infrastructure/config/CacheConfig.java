package com.sasmit.researchpaperassistant.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "summaries",           // For abstract summaries
                "fullSummaries",       // For full paper summaries (was missing!)
                "pdfText",            // For PDF text
                "paperMetadata"       // For paper metadata
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.DAYS)
                .maximumSize(100)
                .recordStats());

        return cacheManager;
    }
}