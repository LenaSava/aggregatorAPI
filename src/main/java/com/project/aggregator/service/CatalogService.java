package com.project.aggregator.service;

import com.project.aggregator.client.CatalogClient;
import com.project.aggregator.exception.CatalogUnavailableException;
import com.project.aggregator.model.CatalogDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CatalogClient client;

    @CircuitBreaker(name = "catalogCB", fallbackMethod = "fallback")
    public CatalogDto fetch(String productId, String locale) {
        log.info("Executing Catalog info");
        return client.fetch(productId, locale);
    }

    private CatalogDto fallback(String productId, String locale, Throwable throwable) {
        log.error("Catalog service failed for product {}", productId, throwable);
        throw new CatalogUnavailableException(throwable);
    }
}
