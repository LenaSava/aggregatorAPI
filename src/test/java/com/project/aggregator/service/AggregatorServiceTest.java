package com.project.aggregator.service;

import com.project.aggregator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregatorServiceTest {

    private static final ProductCommand PRODUCT_COMMAND = new ProductCommand("PART-001", "nl-NL", "DEALER-001");

    private static final PricingDto PRICING = new PricingDto(new BigDecimal("30.00"),
            new BigDecimal("7.51"), new BigDecimal("22.49"));
    private static final AvailabilityDto AVAILABILITY = new AvailabilityDto(42, "WAREHOUSE-EU", Instant.parse("2026-02-20T10:00:00Z"));
    private static final CustomerDto CUSTOMER = new CustomerDto("DEALER-001", "DEALER", List.of("fast-delivery"));

    @Mock
    private FetchService<PricingDto> pricingService;

    @Mock
    private FetchService<AvailabilityDto> availabilityService;

    @Mock
    private FetchService<CustomerDto> customerService;

    private AggregatorService aggregatorService;

    @BeforeEach
    void setUp() {
        aggregatorService = new AggregatorService(List.of(pricingService, availabilityService, customerService));
    }

    @Test
    void fetch_allServicesSucceed_returnsAllDtos() {
        when(pricingService.fetch(any())).thenReturn(CompletableFuture.completedFuture(PRICING));
        when(availabilityService.fetch(any())).thenReturn(CompletableFuture.completedFuture(AVAILABILITY));
        when(customerService.fetch(any())).thenReturn(CompletableFuture.completedFuture(CUSTOMER));

        Map<String, FetchResult> results = aggregatorService.fetch(PRODUCT_COMMAND);

        assertThat(results).hasSize(3)
                .containsEntry(PricingDto.RESPONSE_TYPE, PRICING)
                .containsEntry(AvailabilityDto.RESPONSE_TYPE, AVAILABILITY)
                .containsEntry(CustomerDto.RESPONSE_TYPE, CUSTOMER);
    }

    @Test
    void fetch_pricingFailsAsync_othersReturned() {
        when(pricingService.fetch(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("pricing down")));
        when(availabilityService.fetch(any())).thenReturn(CompletableFuture.completedFuture(AVAILABILITY));
        when(customerService.fetch(any())).thenReturn(CompletableFuture.completedFuture(CUSTOMER));

        Map<String, FetchResult> results = aggregatorService.fetch(PRODUCT_COMMAND);

        assertThat(results).hasSize(2)
                .containsEntry(AvailabilityDto.RESPONSE_TYPE, AVAILABILITY)
                .containsEntry(CustomerDto.RESPONSE_TYPE, CUSTOMER)
                .doesNotContainKey(PricingDto.RESPONSE_TYPE);
    }

    @Test
    void fetch_customerThrowsSync_othersReturned() {
        when(pricingService.fetch(any())).thenReturn(CompletableFuture.completedFuture(PRICING));
        when(availabilityService.fetch(any())).thenReturn(CompletableFuture.completedFuture(AVAILABILITY));
        when(customerService.fetch(any())).thenThrow(new RuntimeException("customer down"));

        Map<String, FetchResult> results = aggregatorService.fetch(PRODUCT_COMMAND);

        assertThat(results).hasSize(2)
                .containsEntry(PricingDto.RESPONSE_TYPE, PRICING)
                .containsEntry(AvailabilityDto.RESPONSE_TYPE, AVAILABILITY)
                .doesNotContainKey(CustomerDto.RESPONSE_TYPE);
    }

    @Test
    void fetch_serviceReturnsNull_filteredOut() {
        when(pricingService.fetch(any())).thenReturn(CompletableFuture.completedFuture(PRICING));
        when(availabilityService.fetch(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(customerService.fetch(any())).thenReturn(CompletableFuture.completedFuture(CUSTOMER));

        Map<String, FetchResult> results = aggregatorService.fetch(PRODUCT_COMMAND);

        assertThat(results).hasSize(2)
                .containsEntry(PricingDto.RESPONSE_TYPE, PRICING)
                .containsEntry(CustomerDto.RESPONSE_TYPE, CUSTOMER);
    }

    @Test
    void fetch_allServicesFail_returnsEmptyMap() {
        when(pricingService.fetch(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("err1")));
        when(availabilityService.fetch(any())).thenThrow(new RuntimeException("err2"));
        when(customerService.fetch(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("err3")));

        Map<String, FetchResult> results = aggregatorService.fetch(PRODUCT_COMMAND);

        assertThat(results).isEmpty();
    }

}
