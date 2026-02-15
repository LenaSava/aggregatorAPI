package com.project.aggregator.service;

import com.project.aggregator.client.CustomerClient;
import com.project.aggregator.model.CustomerDto;
import com.project.aggregator.model.ProductCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerClient client;

    @InjectMocks
    private CustomerService customerService;

    private static final ProductCommand PRODUCT_COMMAND = new ProductCommand("PART-001", "nl-NL", "DEALER-001");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(customerService, "timeoutMillis", 60L);
    }

    @Test
    void fetch_success() {
        CustomerDto expected = new CustomerDto("DEALER-001", "DEALER", List.of("fast-delivery"));
        when(client.fetch("DEALER-001")).thenReturn(expected);

        CustomerDto result = customerService.fetch(PRODUCT_COMMAND).join();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void fetch_clientThrows_futureCompletesExceptionally() {
        when(client.fetch("DEALER-001")).thenThrow(new RuntimeException("timeout"));

        CompletableFuture<CustomerDto> future = customerService.fetch(PRODUCT_COMMAND);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
