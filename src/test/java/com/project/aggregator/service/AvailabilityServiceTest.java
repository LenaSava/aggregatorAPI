package com.project.aggregator.service;

import com.project.aggregator.client.AvailabilityClient;
import com.project.aggregator.model.AvailabilityDto;
import com.project.aggregator.model.ProductCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityClient client;

    @InjectMocks
    private AvailabilityService availabilityService;

    private static final ProductCommand PRODUCT_COMMAND = new ProductCommand("PART-001", "nl-NL", null);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(availabilityService, "timeoutMillis", 100L);
    }

    @Test
    void fetch_success() {
        AvailabilityDto expected = new AvailabilityDto(42, "WAREHOUSE-EU", Instant.parse("2026-02-20T10:00:00Z"));
        when(client.fetch("PART-001", "nl-NL")).thenReturn(expected);

        AvailabilityDto future = availabilityService.fetch(PRODUCT_COMMAND).join();

        assertThat(future).isEqualTo(expected);
        assertThat(future.stock()).isEqualTo(42);
    }

    @Test
    void fetch_clientThrows_futureCompletesExceptionally() {
        when(client.fetch("PART-001", "nl-NL")).thenThrow(new RuntimeException("timeout"));

        CompletableFuture<AvailabilityDto> future = availabilityService.fetch(PRODUCT_COMMAND);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
