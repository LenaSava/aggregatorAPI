package com.project.aggregator.client.mock;

import com.project.aggregator.client.AvailabilityClient;
import com.project.aggregator.exception.UpstreamServiceException;
import com.project.aggregator.model.AvailabilityDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;

/**
 * Simulates the Availability Service.
 * Behaviour:
 * - Base latency: 100ms ± 20ms delay (highest latency upstream)
 * - Failure rate: 2% (reliability 98%)
 * - Returns the closest warehouse to the requested market
 * - Stock levels randomised per request to simulate real-time inventory
 */
@Component
public class MockAvailabilityClient implements AvailabilityClient {

    private static final int MAX_DELAY_MS = 25;
    private static final long BASE_LATENCY_MS = 100;
    private static final double FAILURE_RATE = 0.02;

    private final Random random = new Random();

    private static final Map<String, String> MARKET_WAREHOUSE = Map.of(
            "nl-NL", "Amsterdam-WH",
            "de-DE", "Hamburg-WH",
            "pl-PL", "Warsaw-WH",
            "en-GB", "Manchester-WH",
            "fr-FR", "Paris-WH"
    );

    @Override
    public AvailabilityDto fetch(String productId, String market) {
        simulateLatency();
        simulateFailure();

        String warehouse = MARKET_WAREHOUSE.getOrDefault(market, "Central-WH");

        // Simulate varying stock levels — in reality this hits a live inventory system
        int stockLevel = random.nextInt(0, 250);
        boolean inStock = stockLevel > 0;

        // Delivery estimate: 1 day if in stock at local warehouse, 3-5 otherwise
        Instant delivery = inStock
                ? Instant.now().plus(1, ChronoUnit.DAYS)
                : Instant.now().plus(3 + random.nextInt(3), ChronoUnit.DAYS);

        return new AvailabilityDto(stockLevel, warehouse, delivery);
    }

    private void simulateLatency() {
        long delay = (long) (random.nextDouble() * MAX_DELAY_MS);
        try {
            Thread.sleep(MockAvailabilityClient.BASE_LATENCY_MS + delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateFailure() {
        if (random.nextDouble() < MockAvailabilityClient.FAILURE_RATE) {
            throw new UpstreamServiceException("AvailabilityService", "Simulated transient failure");
        }
    }
}
