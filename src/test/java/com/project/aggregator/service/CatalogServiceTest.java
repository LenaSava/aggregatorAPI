package com.project.aggregator.service;

import com.project.aggregator.client.CatalogClient;
import com.project.aggregator.exception.ProductNotFoundException;
import com.project.aggregator.exception.UpstreamServiceException;
import com.project.aggregator.model.CatalogDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogClient client;

    @InjectMocks
    private CatalogService catalogService;

    private static final CatalogDto CATALOG = new CatalogDto("PART-001", "Oil Filter", "High-quality oil filter",
            Map.of("type", "oil"), List.of("img1.png"));

    @Test
    void fetch_success() {
        when(client.fetch("PART-001", "nl-NL")).thenReturn(CATALOG);

        CatalogDto result = catalogService.fetch("PART-001", "nl-NL");

        assertThat(result).isEqualTo(CATALOG);
    }

    @Test
    void fetch_productNotFound_propagates() {
        when(client.fetch("UNKNOWN", "nl-NL"))
                .thenThrow(new ProductNotFoundException("UNKNOWN"));

        assertThatThrownBy(() -> catalogService.fetch("UNKNOWN", "nl-NL"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void fetch_upstreamFailure_propagates() {
        when(client.fetch("PART-001", "nl-NL"))
                .thenThrow(new UpstreamServiceException("CatalogService", "Connection refused"));

        assertThatThrownBy(() -> catalogService.fetch("PART-001", "nl-NL"))
                .isInstanceOf(UpstreamServiceException.class);
    }
}
