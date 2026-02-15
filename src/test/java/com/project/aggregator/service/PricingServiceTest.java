package com.project.aggregator.service;

import com.project.aggregator.client.PricingClient;
import com.project.aggregator.model.PricingDto;
import com.project.aggregator.model.ProductCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingClient client;

    @InjectMocks
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pricingService, "timeoutMillis", 80L);
    }

    @Test
    void fetch_success() {
        PricingDto expected = new PricingDto(
                new BigDecimal("30.00"), new BigDecimal("5.01"), new BigDecimal("24.99"));
        when(client.fetch("PART-001", "nl-NL", "DEALER-001")).thenReturn(expected);

        ProductCommand productCommand = new ProductCommand("PART-001", "nl-NL", "DEALER-001");
        CompletableFuture<PricingDto> future = pricingService.fetch(productCommand);

        assertThat(future.join()).isEqualTo(expected);
    }

    @Test
    void fetch_nullCustomer_passesNullCustomerId() {
        PricingDto expected = new PricingDto(
                new BigDecimal("30.00"), new BigDecimal("5.01"), new BigDecimal("24.99"));
        when(client.fetch("PART-001", "nl-NL", null)).thenReturn(expected);

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", null);
        CompletableFuture<PricingDto> future = pricingService.fetch(cmd);

        assertThat(future.join()).isEqualTo(expected);
        verify(client).fetch("PART-001", "nl-NL", null);
    }

    @Test
    void fetch_clientThrows_futureCompletesExceptionally() {
        when(client.fetch("PART-001", "nl-NL", null)).thenThrow(new RuntimeException("service down"));

        ProductCommand productCommand = new ProductCommand("PART-001", "nl-NL", null);
        CompletableFuture<PricingDto> future = pricingService.fetch(productCommand);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
