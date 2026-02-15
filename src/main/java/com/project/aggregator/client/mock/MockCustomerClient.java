package com.project.aggregator.client.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.aggregator.client.CustomerClient;
import com.project.aggregator.exception.UpstreamServiceException;
import com.project.aggregator.model.CustomerDto;
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
 * Simulates the Customer Service.
 * <p>
 * Behaviour:
 * - Base latency: 60ms ± 15ms jitter
 * - Failure rate: 1% (reliability 99%)
 * - Only called when a customerId is provided
 * - Customer segment is derived from the ID prefix for demo purposes
 */
@Component
@Slf4j
public class MockCustomerClient implements CustomerClient {

    private static final int MAX_DELAY_MS = 15;
    private static final long BASE_LATENCY_MS = 60;
    private static final double FAILURE_RATE = 0.01;

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, CustomerDto> customers;

    @PostConstruct
    public void loadCustomerData() {
        try {
            ClassPathResource resource = new ClassPathResource("mock-data/customers.json");
            List<CustomerDto> customerList = objectMapper.readValue(
                    resource.getInputStream(), new TypeReference<>() {
                    }
            );

            customers = customerList.stream()
                    .collect(Collectors.toMap(CustomerDto::customerId, Function.identity()));

            log.info("Loaded {} customers from customers.json", customers.size());
        } catch (IOException e) {
            log.error("Failed to load customer mock data", e);
            throw new IllegalStateException("Cannot initialize MockCustomerClient", e);
        }
    }

    @Override
    public CustomerDto fetch(String customerId) {
        simulateLatency();
        simulateFailure();

        return customers.get(customerId);
    }

    private void simulateLatency() {
        long delay = (long) (random.nextDouble() * MAX_DELAY_MS);
        try {
            Thread.sleep(MockCustomerClient.BASE_LATENCY_MS + delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateFailure() {
        if (random.nextDouble() < MockCustomerClient.FAILURE_RATE) {
            throw new UpstreamServiceException("CustomerService", "Simulated transient failure");
        }
    }
}
