package com.project.aggregator.client.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.aggregator.client.PricingClient;
import com.project.aggregator.exception.UpstreamServiceException;
import com.project.aggregator.model.PricingDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;

/**
 * Simulates the Pricing Service.
 * Behaviour:
 * - Base latency: 80ms ± 15ms jitter
 * - Failure rate: 0.5% (reliability 99.5%)
 * - Returns market-specific currency
 * - Applies customer-segment discounts when customerId is provided
 */
@Component
@Slf4j
public class MockPricingClient implements PricingClient {

    private static final int MAX_DELAY_MS = 20;
    private static final String SERVICE = "PricingService";
    private static final long BASE_LATENCY_MS = 80;
    private static final double FAILURE_RATE = 0.005;

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Loaded from JSON
    private Map<String, BigDecimal> basePrices;
    private Map<String, MarketConfig> marketConfig;
    private Map<String, BigDecimal> segmentDiscounts;

    @PostConstruct
    public void loadPricingData() {
        try {
            ClassPathResource resource = new ClassPathResource("mock-data/pricing.json");
            PricingData data = objectMapper.readValue(resource.getInputStream(), PricingData.class);

            this.basePrices = data.basePrices();
            this.marketConfig = data.marketConfig();
            this.segmentDiscounts = data.segmentDiscounts();

            log.info("Loaded pricing data: {} products, {} markets, {} segments",
                    basePrices.size(), marketConfig.size(), segmentDiscounts.size());
        } catch (IOException e) {
            log.error("Failed to load pricing mock data", e);
            throw new IllegalStateException("Cannot initialize MockPricingClient", e);
        }
    }

    @Override
    public PricingDto fetch(String productId, String market, String customerId) {
        simulateLatency();
        simulateFailure();

        BigDecimal baseEur = basePrices.getOrDefault(productId, new BigDecimal("99.99"));
        MarketConfig mc = marketConfig.getOrDefault(market,
                new MarketConfig("EUR", BigDecimal.ONE));

        BigDecimal basePrice = baseEur.multiply(mc.exchangeRate())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = resolveDiscount(customerId);
        BigDecimal finalPrice = basePrice
                .multiply(BigDecimal.ONE.subtract(discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        return new PricingDto(basePrice, discount, finalPrice);
    }

    private BigDecimal resolveDiscount(String customerId) {
        if (customerId == null) return BigDecimal.ZERO;

        // Extract segment from customerId prefix (e.g., DEALER-001 → DEALER)
        String segment = customerId.contains("-")
                ? customerId.substring(0, customerId.indexOf('-'))
                : "STANDARD";

        return segmentDiscounts.getOrDefault(segment, BigDecimal.ZERO);
    }

    void simulateLatency() {
        long delay = (long) (random.nextDouble() * MAX_DELAY_MS);
        try {
            Thread.sleep(MockPricingClient.BASE_LATENCY_MS + delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void simulateFailure() {
        if (random.nextDouble() < MockPricingClient.FAILURE_RATE) {
            throw new UpstreamServiceException(MockPricingClient.SERVICE, "Simulated transient failure");
        }
    }

    private record PricingData(
            Map<String, BigDecimal> basePrices,
            Map<String, MarketConfig> marketConfig,
            Map<String, BigDecimal> segmentDiscounts
    ) {
    }

    private record MarketConfig(String currency, BigDecimal exchangeRate) {
    }
}
