package com.project.aggregator.client.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.aggregator.client.CatalogClient;
import com.project.aggregator.exception.ProductNotFoundException;
import com.project.aggregator.exception.UpstreamServiceException;
import com.project.aggregator.model.CatalogDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simulates the Catalog Service.
 * Behaviour:
 * - Base latency: 50ms ± 15ms delay
 * - Failure rate: 0.1% (reliability 99.9%)
 * - Returns market-localised product names/descriptions
 * - Throws ProductNotFoundException for unknown product IDs
 */
@Component
@Slf4j
public class MockCatalogClient implements CatalogClient {

    private static final int MAX_DELAY_MS = 20;
    private static final long BASE_LATENCY_MS = 50;
    private static final double FAILURE_RATE = 0.001;

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record CatalogTemplate(
            String productId,
            Map<String, String> names,
            Map<String, String> descriptions,
            Map<String, String> specs,
            List<String> images
    ) {}

    private Map<String, CatalogTemplate> catalogue;

    @PostConstruct
    public void loadCatalogData() {
        try {
            ClassPathResource resource = new ClassPathResource("mock-data/catalog.json");
            List<CatalogTemplate> products = objectMapper.readValue(
                    resource.getInputStream(), new TypeReference<List<CatalogTemplate>>() {});

            catalogue = products.stream()
                    .collect(Collectors.toMap(CatalogTemplate::productId, Function.identity()));

            log.info("Loaded {} products from catalog.json", catalogue.size());
        } catch (IOException e) {
            log.error("Failed to load catalog mock data", e);
            throw new IllegalStateException("Cannot initialize MockCatalogClient", e);
        }
    }

    @Override
    public CatalogDto fetch(String productId, String market) {
        simulateLatency();
        simulateFailure();

        CatalogTemplate template = catalogue.get(productId);
        if (template == null) {
            throw new ProductNotFoundException(productId);
        }

        return new CatalogDto(
                productId,
                localize(template.names(), market),
                localize(template.descriptions(), market),
                template.specs(),
                template.images()
        );
    }

    private String localize(Map<String, String> localeMap, String market) {
        if (localeMap == null) return null;
        return localeMap.getOrDefault(market, localeMap.getOrDefault("en-GB", localeMap.values().iterator().next()));
    }

    private void simulateLatency() {
        long delay = (long) (random.nextDouble() * MAX_DELAY_MS);
        try {
            Thread.sleep(MockCatalogClient.BASE_LATENCY_MS + delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateFailure() {
        if (random.nextDouble() < MockCatalogClient.FAILURE_RATE) {
            throw new UpstreamServiceException("CatalogService", "Simulated transient failure");
        }
    }
}