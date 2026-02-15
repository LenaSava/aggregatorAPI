package com.project.aggregator.service;

import com.project.aggregator.exception.CatalogUnavailableException;
import com.project.aggregator.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFacadeTest {

    @Mock
    private CatalogService catalogService;
    @Mock
    private AggregatorService aggregatorService;

    @InjectMocks
    private ProductFacade productFacade;

    @Test
    void aggregate_allServicesSucceed() {
        when(catalogService.fetch("PART-001", "nl-NL")).thenReturn(CATALOG);
        stubAggregator(Map.of(CustomerDto.RESPONSE_TYPE, CUSTOMER, PricingDto.RESPONSE_TYPE, PRICING, AvailabilityDto.RESPONSE_TYPE, AVAILABILITY));

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", "DEALER-001");
        ProductResponse response = productFacade.aggregate(cmd);

        // catalog
        assertThat(response.getCatalog().getProductId()).isEqualTo("PART-001");
        assertThat(response.getCatalog().getName()).isEqualTo("Oil Filter");
        assertThat(response.getCatalog().getDescription()).isEqualTo("High-quality oil filter");
        // pricing
        assertThat(response.getPrice().getFinalPrice()).isEqualTo(new BigDecimal("22.49"));
        assertThat(response.getPrice().getBasePrice()).isEqualTo(new BigDecimal("30.00"));
        assertThat(response.getPrice().getStatus()).isEqualTo(ProductResponse.PriceResponse.Status.AVAILABLE);
        // availability
        assertThat(response.getAvailability().getStock()).isEqualTo(42);
        assertThat(response.getAvailability().getWarehouse()).isEqualTo("WAREHOUSE-EU");
        assertThat(response.getAvailability().getStatus()).isEqualTo(ProductResponse.AvailabilityResponse.Status.IN_STOCK);
        // customer
        assertThat(response.getCustomer().getCustomerId()).isEqualTo("DEALER-001");
        assertThat(response.getCustomer().getSegment()).isEqualTo("DEALER");
        assertThat(response.getCustomer().getStatus()).isEqualTo(ProductResponse.CustomerResponse.Status.PERSONALIZED);
    }

    @Test
    void aggregate_withoutCustomer_nonPersonalized() {
        when(catalogService.fetch("PART-001", "nl-NL")).thenReturn(CATALOG);
        stubAggregator(Map.of(PricingDto.RESPONSE_TYPE, PRICING, AvailabilityDto.RESPONSE_TYPE, AVAILABILITY));

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", null);
        ProductResponse response = productFacade.aggregate(cmd);

        assertThat(response.getCustomer().getStatus())
                .isEqualTo(ProductResponse.CustomerResponse.Status.NON_PERSONALIZED);
        assertThat(response.getCustomer().getCustomerId()).isNull();
        assertThat(response.getCustomer().getSegment()).isNull();
        assertThat(response.getPrice().getStatus()).isEqualTo(ProductResponse.PriceResponse.Status.AVAILABLE);
        assertThat(response.getAvailability().getStatus()).isEqualTo(ProductResponse.AvailabilityResponse.Status.IN_STOCK);
    }

    @Test
    void aggregate_catalogFails_throws() {
        when(catalogService.fetch("PART-001", "nl-NL"))
                .thenThrow(new CatalogUnavailableException(new RuntimeException("down")));

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", null);

        assertThatThrownBy(() -> productFacade.aggregate(cmd))
                .isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void aggregate_pricingFails_priceUnavailable() {
        when(catalogService.fetch("PART-001", "nl-NL")).thenReturn(CATALOG);
        stubAggregator(Map.of(AvailabilityDto.RESPONSE_TYPE, AVAILABILITY));

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", null);
        ProductResponse response = productFacade.aggregate(cmd);

        assertThat(response.getPrice().getStatus())
                .isEqualTo(ProductResponse.PriceResponse.Status.UNAVAILABLE);
        assertThat(response.getPrice().getFinalPrice()).isNull();
        assertThat(response.getAvailability().getStock()).isEqualTo(42);
        assertThat(response.getAvailability().getStatus()).isEqualTo(ProductResponse.AvailabilityResponse.Status.IN_STOCK);
    }

    @Test
    void aggregate_availabilityFails_stockUnknown() {
        when(catalogService.fetch("PART-001", "nl-NL")).thenReturn(CATALOG);
        stubAggregator(Map.of(PricingDto.RESPONSE_TYPE, PRICING));

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", null);
        ProductResponse response = productFacade.aggregate(cmd);

        assertThat(response.getAvailability().getStatus())
                .isEqualTo(ProductResponse.AvailabilityResponse.Status.UNKNOWN_STOCK);
        assertThat(response.getAvailability().getStock()).isNull();
        assertThat(response.getPrice().getFinalPrice()).isEqualTo(new BigDecimal("22.49"));
        assertThat(response.getPrice().getStatus()).isEqualTo(ProductResponse.PriceResponse.Status.AVAILABLE);
    }

    @Test
    void aggregate_customerFails_nonPersonalized() {
        when(catalogService.fetch("PART-001", "nl-NL")).thenReturn(CATALOG);
        stubAggregator(Map.of(PricingDto.RESPONSE_TYPE, PRICING, AvailabilityDto.RESPONSE_TYPE, AVAILABILITY));

        ProductCommand cmd = new ProductCommand("PART-001", "nl-NL", "DEALER-001");
        ProductResponse response = productFacade.aggregate(cmd);

        assertThat(response.getCustomer().getStatus())
                .isEqualTo(ProductResponse.CustomerResponse.Status.NON_PERSONALIZED);
        assertThat(response.getCustomer().getSegment()).isNull();
        assertThat(response.getPrice().getStatus()).isEqualTo(ProductResponse.PriceResponse.Status.AVAILABLE);
        assertThat(response.getAvailability().getStatus()).isEqualTo(ProductResponse.AvailabilityResponse.Status.IN_STOCK);
    }

    private static final CatalogDto CATALOG = new CatalogDto(
            "PART-001", "Oil Filter", "High-quality oil filter",
            Map.of("type", "oil"), List.of("img1.png")
    );
    private static final CustomerDto CUSTOMER = new CustomerDto(
            "DEALER-001", "DEALER", List.of("fast-delivery")
    );
    private static final PricingDto PRICING = new PricingDto(
            new BigDecimal("30.00"), new BigDecimal("7.51"), new BigDecimal("22.49")
    );
    private static final AvailabilityDto AVAILABILITY = new AvailabilityDto(
            42, "WAREHOUSE-EU", Instant.parse("2026-02-20T10:00:00Z")
    );

    private void stubAggregator(Map<String, FetchResult> results) {
        when(aggregatorService.fetch(any())).thenReturn(results);
    }
}
